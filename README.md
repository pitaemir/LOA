# Legends of the Ascended (LOA)

![Hytale](https://img.shields.io/badge/Game-Hytale-purple)
![Framework](https://img.shields.io/badge/Framework-ECS-blue)
![Language](https://img.shields.io/badge/Language-Java_25-orange)
![Status](https://img.shields.io/badge/Status-Prototype-yellow)

A **PvP MOBA mod for *Hytale***, inspired by *Deadlock* and *Dota 2*. Two teams, lanes, creep waves marching toward the enemy base — built in small, playable steps on top of Hytale's **Entity Component System (ECS)**.

> **Current stage:** creep-wave prototype. Allied and enemy waves spawn on a timer, walk down a lane, fight each other and push to the other side. Structures (towers, base) and heroes come next.

---

## What works today

### Teams
- Every player and creep belongs to a team (`TeamComponent`). Players join the **blue** team automatically.
- **Same team = allies, different team = enemies.** A custom attitude rule tells NPC AI who to attack, overriding the game's default relations.
- **No friendly fire:** damage between teammates is cancelled (melee and projectiles), and a creep can never keep a teammate as its target.

### Creep waves
- Every *N* seconds each side spawns a wave: **3 melee + 2 ranged creeps** by default.
- Enemy creeps (red) walk from the enemy spawn to your base; allied creeps (green) walk the opposite way. They meet in the middle and fight.
- After a fight, creeps **resume the lane forward** from the nearest point — no wandering around.
- On reaching the end of the lane they **patrol side to side** there, attacking anything that shows up.
- All creeps share the same stats (100 HP, 10 damage per hit), regardless of NPC type.

### Custom creeps
- `Creep_Melee_Red` / `Creep_Melee_Green` — spear fighters.
- `Creep_Ranged_Red` / `Creep_Ranged_Green` — archers: stop at ~11 blocks and shoot, **never back away**, keep a small gap between each other.
- Based on the Kweebec warrior model, recolored per team (red autumn leaves / green leaves).
- Behavior comes from the mod's own NPC template (`Template_LOA_Creep`).

### Progression (from the original RPG prototype)
- Player leveling (1–30) with an XP HUD and *LEVEL UP!* banner.
- Weapon mastery per weapon type.
- These will be reworked into **per-match** progression (everyone starts at level 1 each match).

---

## Commands

All commands are under `/loa` and require operator permission (`op add <name>` in the server console).

### Waves

| Command | Description |
|---|---|
| `/loa wave setspawn` | Set the **enemy spawn** to your position |
| `/loa wave setbase` | Set **your base** (allied spawn) to your position |
| `/loa wave now` | Spawn one wave right away |
| `/loa wave start` | Spawn a wave every 60 s |
| `/loa wave stop` | Stop the timer |
| `/loa wave clear` | Remove all wave creeps |

Options for `now` / `start`:

| Option | Default | Description |
|---|---|---|
| `--count N` | 3 | Melee creeps per side (0–20) |
| `--ranged N` | 2 | Archers per side (0–10) |
| `--sides both\|enemies\|allies` | both | Which side(s) spawn |
| `--type ROLE` | `Creep_Melee_Red` | Enemy melee NPC role |
| `--allytype ROLE` | `Creep_Melee_Green` | Allied melee NPC role |
| `--interval S` | 60 | Seconds between waves (`start` only, ≥ 5) |

`/loa wave clear` also accepts `--sides`.

Examples:
```
/loa wave now --count 0 --ranged 4 --sides enemies   # only enemy archers
/loa wave start --interval 30                         # faster waves
```

### Debug

| Command | Description |
|---|---|
| `/loa spawn [--type NPC] [--count N]` | Spawn any NPC around you |
| `/loa xp [--amount N]` | Give yourself XP |
| `/loa stats` | Show level and XP |
| `/loa resetlevel` | Reset your level to 1 |

> Spawn and base points are kept in memory only — set them again after a server restart.

---

## Project structure

### Code — `src/main/java/com/troubledev/`

```
LOASystems.java              Plugin entry point: registers components, systems, events, commands
team/
├── Team                     BLUE / RED
├── TeamComponent            Which team an entity belongs to (saved)
├── TeamAttitudeSystem       NPC AI: same team -> friendly, other team -> hostile
├── TeamDamageSystem         Cancels damage between teammates
└── TeamTargetSystem         Drops a creep's target if it is a teammate
waves/
├── WaveManager              Wave timer, spawning, lane paths, /loa wave clear
├── CreepLaneComponent       Where a creep came from and where it is going
├── CreepLaneSystem          Switches creeps to side-to-side patrol at the end of the lane
├── CreepStats               Shared creep health and damage values
└── CreepDamageSystem        Every creep hit deals CreepStats.DAMAGE
commands/                    /loa and its subcommands (LOAWaveCommand, ...)
components/, level/,         Player level and weapon mastery (RPG prototype)
systems/, events/, handlers/
ui/LOAXPHud                  XP bar HUD
```

### Assets — `src/main/resources/` (the mod's asset pack)

```
Server/NPC/Roles/LOA/
├── Templates/Template_LOA_Creep.json            Creep AI (state machine) — based on the Kweebec warrior
├── Components/Component_LOA_Creep_Follow_Lane    Walk the lane forward, resume from nearest node
├── Components/Component_LOA_Attack_Sequence_Bow  Archer shot
├── Creep_Melee_{Red,Green}.json                  Melee creep roles
└── Creep_Ranged_{Red,Green}.json                 Archer roles
Server/Models/LOA/Creep_{Red,Green}.json          Appearance (Kweebec warrior + team textures)
Common/NPC/LOA/Creep/*.png                         Team-colored textures
Common/UI/Custom/LOAXPHud.ui                       HUD layout
```

### Where to tweak creeps

| To change | Edit |
|---|---|
| Health / damage of all creeps | `CreepStats.java` |
| View range, speed, weapon, attack range | `Parameters` in `Template_LOA_Creep.json` or a creep role |
| Archer stop distance / spacing | `ChaseStopDistance`, `SeparationDistance` in the ranged roles / template |
| "Arrived" distance and patrol width | `ARRIVE_DISTANCE`, `PATROL_HALF_WIDTH` in `CreepLaneSystem.java` |
| Wave size and interval | Constants at the top of `WaveManager.java` |

---

## Development

### Requirements

- Java 25
- Hytale installed via the official launcher (server jar and assets are taken from the install)

### Folder layout

```
HytaleModding/
├── first_try/            ← this repository (the mod)
├── server/               ← local test server (world, config, mods/)
│   └── mods/LOA_Maps/    ← editable asset pack with the map template (MobaMap)
└── art/                  ← reference models and texture work (not shipped)
```

### Run a local test server

```bash
./run-server.sh
```

The script updates `libs/HytaleServer.jar` if Hytale was updated, builds the mod, copies it to `../server/mods/` and starts the server. Connect to `localhost` from Hytale. After changing code: **Ctrl+C** and run the script again.

First run only, in the server console:
```
/auth login device            # authorize the server with your Hytale account
/auth persistence Encrypted   # keep the login across restarts
op add <your-username>        # allow /loa commands
```

### Build only

```bash
./gradlew build
```

The jar is written to `build/libs/`.

### Editing the map

The map is a Hytale **instance template** stored in `server/mods/LOA_Maps/Server/Instances/MobaMap/` (flat terrain, no natural mob spawns, time frozen at day, creative mode).

| Action | Command |
|---|---|
| Edit the map | `/instances edit load MobaMap` |
| Save | `/world save` |
| Back to the main world | `/tp world default` |
| Play a disposable copy | `/instances spawn MobaMap` (leave with `/instances exit`) |

The `server/` folder is not part of this repository — back up `LOA_Maps` regularly.

---

## Roadmap

1. **Map points** — save spawn/base per map so waves work right after loading it
2. **Base with health** — enemy creeps reaching the end damage the base; first win/lose condition
3. **Towers** — static defenders on each side of the lane
4. **Economy** — gold for last hits, simple item shop
5. **Match manager** — lobby → match → end, teams, respawn timers, per-match progression, one instance per match
6. **Heroes** — abilities with cooldowns

---

> *Forge your legend.*
