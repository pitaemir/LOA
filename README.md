# Legends of the Ascended (LOA)

![Hytale](https://img.shields.io/badge/Game-Hytale-purple)
![Server](https://img.shields.io/badge/Server-0.6.8-lightgrey)
![Language](https://img.shields.io/badge/Language-Java_25-orange)
![Status](https://img.shields.io/badge/Status-Prototype-yellow)

**A MOBA game mode for Hytale** — two teams, lanes, creep waves pushing toward the enemy base — inspired by *Deadlock* and *Dota 2*.

LOA is a solo project and a learning journey: I'm building a full game mode on top of Hytale's server API, one small playable slice at a time, and documenting what I learn about the engine along the way.

<!-- TODO: add a GIF of two creep waves clashing in the lane -->

---

## Where it is now

The **creep wave** slice is playable:

- Two teams. Players join the blue team; creeps are **green** (allies) and **red** (enemies).
- Every wave spawns **3 melee + 2 ranged creeps per side**. They walk the lane toward each other, fight in the middle and keep pushing.
- After a fight, creeps **resume the lane forward** instead of wandering off.
- At the end of the lane they **hold position**, patrolling side to side and attacking whatever arrives.
- **No friendly fire** — neither damage nor aggro between teammates.
- Custom creeps: the Kweebec warrior, recolored per team, with its own AI template.

**Next:** base with health (first win/lose condition) → towers → gold & last hits → match flow → heroes.

---

## How it's built

LOA uses Hytale's systems the way the game itself does, instead of working around them. Each feature below maps to an engine mechanism:

| Feature | Hytale mechanism used |
|---|---|
| Teams decide who NPCs attack | A custom `IAttitudeProvider` registered on the NPC `Blackboard`'s `AttitudeView` (priority 10, ahead of reputation at 100) |
| No friendly fire | `DamageEventSystem` in the **filter damage group** that cancels same-team `Damage` (covers `ProjectileSource`) |
| No aggro between teammates | `EntityTickingSystem` that clears teammate refs from `MarkedEntitySupport` |
| Creeps walk the lane | `TransientPath` assigned through `NPCEntity.getPathManager()`, followed by the role's `Path` body motion |
| Creeps hold the end of the lane | `EntityTickingSystem` that swaps the lane path for a short perpendicular patrol path on arrival |
| Equal creep stats | Overriding the role's `NPC_Max` health modifier; fixing hit damage in the filter damage group |
| Creep AI | Own **role template** (`Template_LOA_Creep`) — `Search` state removed, lost target → back to the lane |
| Archers that never retreat | `Component_Instruction_Target_Adjusted_Attack_Melee` with `MaintainDistance: false`, chase `StopDistance` at attack range |
| Team colors | Model `Parent` inheritance: the Kweebec warrior with three textures swapped, shipped in the mod's asset pack |
| The map | An **instance template** (flat world gen) edited with `/instances edit`, one disposable copy per match later |

Everything gameplay-tunable (health, speed, ranges, spacing) lives in JSON assets, so balancing doesn't need recompiling.

---

## What I learned about the engine

Notes from reading the server jar and the game's assets while building this. Shared in case they're useful to other modders — or to the Hytale team.

**Things that worked really well**
- **Role `Variant` + `Reference` inheritance** made custom creeps almost free: one template, then per-team roles that only change `Appearance`.
- **The attitude provider chain** is a clean extension point — one lambda turned the whole NPC AI team-aware.
- **Damage system groups** (gather → filter → inspect) make it obvious where to hook, and later systems already skip cancelled events.
- **Instances + `/instances edit`** are a great fit for match-based modes: build the map once, spawn a fresh copy per match.

**Possible issues found in vanilla assets**
- `Template_Kweebec_Razorleaf` doesn't define the `Melee_Damage` interaction var (`Template_Intelligent` and `Trork_*` roles do). Its spear hits log `Missing replacement interactions for interaction ... for var Melee_Damage` and seem to deal no damage. LOA's template adds the var.

**Would love to have in the API**
- **Projectile collision filtering** — projectiles only ignore their creator; a hook to let arrows pass through teammates would make ranged units in team modes much cleaner.
- **A path shape that stops at the last node** — `LINE` ping-pongs and `LOOP` restarts; lane-walking units need "go to the end and stay".
- **Team-aware separation** — `ApplySeparation` pushes away from any nearby entity, including enemies in melee range.

---

## Try it

**Requirements:** Java 25, Hytale installed via the official launcher.

```bash
./run-server.sh
```

The script syncs `libs/HytaleServer.jar` with your Hytale install, builds the mod, copies it to `../server/mods/` and starts a local server. Connect to `localhost`.

First run, in the server console:
```
/auth login device
/auth persistence Encrypted
op add <your-username>
```

In game:
```
/loa wave setspawn     # stand where the enemy creeps should come from
/loa wave setbase      # stand at your base
/loa wave now          # spawn a wave on both sides
/loa wave start        # ... or every 60 seconds
```

<details>
<summary><b>All wave commands and options</b></summary>

| Command | Description |
|---|---|
| `/loa wave setspawn` / `setbase` | Set the enemy spawn / your base to your position |
| `/loa wave now` | Spawn one wave |
| `/loa wave start` / `stop` | Start / stop the wave timer |
| `/loa wave clear` | Remove all wave creeps |

| Option (`now` / `start`) | Default | |
|---|---|---|
| `--count N` | 3 | Melee creeps per side |
| `--ranged N` | 2 | Archers per side |
| `--sides both\|enemies\|allies` | both | Which sides spawn (`clear` accepts it too) |
| `--type` / `--allytype ROLE` | `Creep_Melee_Red` / `_Green` | Melee NPC role per side |
| `--interval S` | 60 | Seconds between waves (`start`) |

Debug: `/loa spawn`, `/loa xp`, `/loa stats`, `/loa resetlevel`.
</details>

<details>
<summary><b>Editing the map</b></summary>

The map template lives in `server/mods/LOA_Maps/Server/Instances/MobaMap/` — an editable folder asset pack (packs inside `.jar`/`.zip` are read-only). Flat terrain, no natural spawns, frozen daytime, creative mode.

| | |
|---|---|
| Edit | `/instances edit load MobaMap` |
| Save | `/world save` |
| Leave | `/tp world default` |
| Play a copy | `/instances spawn MobaMap` → `/instances exit` |
</details>

---

## Code map

```
src/main/java/com/troubledev/
├── LOASystems.java            Plugin entry: registers everything
├── team/                      Teams: attitude provider, friendly-fire filter, teammate-target cleanup
├── waves/                     Wave spawning, lane paths, end-of-lane patrol, shared creep stats
├── commands/                  /loa and subcommands
├── ui/                        XP HUD
└── components/ level/ systems/ events/ handlers/
                               Player level & weapon mastery — from the RPG prototype this
                               project started as; will become per-match progression

src/main/resources/            The mod's asset pack
├── Server/NPC/Roles/LOA/      Creep template, lane-follow and bow components, creep roles
├── Server/Models/LOA/         Team-colored creep appearances
└── Common/NPC/LOA/Creep/      Team textures
```

| To tweak | Edit |
|---|---|
| Creep health / damage | `waves/CreepStats.java` |
| View range, speed, weapon, attack range | `Template_LOA_Creep.json` parameters or a creep role |
| Archer stop distance / spacing | `ChaseStopDistance`, `SeparationDistance` |
| End-of-lane arrival and patrol width | `waves/CreepLaneSystem.java` |
| Wave size and timing | `waves/WaveManager.java` |

---

## Project history

LOA started as an **RPG progression tutorial** (XP, levels 1–30, weapon mastery, XP HUD). Once the ECS basics clicked, the goal grew into a full MOBA mode. The progression systems are still in the codebase and will be reworked so that everyone starts each match at level 1.

---

## Contact

Built by **pitaemir** — feedback, ideas and bug reports are very welcome through GitHub issues.

> *Forge your legend.*
