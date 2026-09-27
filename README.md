# Legends of the Ascended (LOA)

![Hytale](https://img.shields.io/badge/Game-Hytale-purple)
![Framework](https://img.shields.io/badge/Framework-ECS-blue)
![Language](https://img.shields.io/badge/Language-Java_25-orange)
![Status](https://img.shields.io/badge/Status-In_Development-yellow)

An RPG progression mod for *Hytale*. Players earn XP by defeating enemies, level up from 1 to 30, and build **mastery with each weapon** they fight with — all tracked by a live HUD and saved with the player.

Built on Hytale's **Entity Component System (ECS)** so each piece of the progression (data, rules, feedback) lives in its own module and new systems can be added without touching the existing ones.

---

## Features

- **Player leveling** — 30 levels with a hand-tuned XP curve (`XPTable`). Each kill grants 50 XP.
- **Weapon mastery** — every weapon type (e.g. `Weapon_Sword_Iron`, `Weapon_Axe_Copper`) has its own mastery level, raised by killing with it. Each kill grants 25 mastery XP.
- **XP HUD** — level and XP bar in the bottom-left corner, updated in real time, with a *LEVEL UP!* banner.
- **Persistent progress** — level and mastery are stored as ECS components and saved with the player.
- **Event-driven** — XP and level-ups flow through `GiveXPEvent` / `LevelUpEvent`, so other systems can hook in.
- **Debug commands** — grant XP, inspect stats, spawn enemies, reset progress.

---

## Commands

All commands are under `/loa` and require operator permission on the server.

| Command | Description |
|---|---|
| `/loa stats` | Show level, total XP and progress to the next level |
| `/loa xp [--amount N]` | Give yourself XP (default 50) |
| `/loa spawn [--type NPC] [--count N]` | Spawn NPCs around you to test kills (default 5 `Skeleton`, max 50) |
| `/loa resetlevel` | Reset your level back to 1 |

Tip: use Hytale's built-in `/give Weapon_Sword_Iron` to grab weapons for testing mastery.

---

## Architecture

```
src/main/java/com/troubledev/
├── LOASystems.java          Plugin entry point: registers components, systems, events, commands
├── components/
│   ├── PlayerLOAComponent       Player total XP (level is derived from it)
│   └── WeaponMasteryComponent   Mastery XP per weapon ID
├── level/
│   ├── XPTable                  Player level thresholds (1–30)
│   └── WeaponMasteryTable       Weapon mastery thresholds
├── systems/
│   ├── PlayerJoinSystem         Creates player data on first join and attaches the HUD
│   └── XPGainSystem             On enemy death, awards player XP and weapon mastery XP
├── events/
│   ├── GiveXPEvent
│   └── LevelUpEvent
├── handlers/
│   ├── GiveXPHandler            Applies XP, refreshes the HUD, fires LevelUpEvent
│   └── LevelUpHandler           Level-up message and HUD banner
├── commands/                    /loa and its subcommands
└── ui/
    └── LOAXPHud                 Custom HUD (layout in resources/Common/UI/Custom/LOAXPHud.ui)
```

**Flow of a kill:** `XPGainSystem` detects the death → dispatches `GiveXPEvent` → `GiveXPHandler` adds XP and refreshes the HUD → on level change, `LevelUpEvent` → `LevelUpHandler` shows the banner.

---

## Development

### Requirements

- Java 25
- Hytale installed via the official launcher (the server jar and assets are taken from the install)

### Project layout

The mod expects a `server/` folder next to the project, used as the local server's working directory:

```
HytaleModding/
├── first_try/     ← this repository
└── server/        ← local test server (world, config, mods/)
```

### Run a local test server

```bash
./run-server.sh
```

The script:

1. Copies the installed `HytaleServer.jar` into `libs/` if Hytale was updated (keeps the mod compiled against the current API)
2. Builds the mod with Gradle
3. Copies `LOASystems-*.jar` into `../server/mods/`
4. Starts the server

Then open Hytale and connect to `localhost`. After changing code, stop the server with **Ctrl+C** and run the script again.

### First run only

In the server console:

```
/auth login device          # authorize the server with your Hytale account
/auth persistence Encrypted # keep the login across restarts
op add <your-username>      # allow yourself to use /loa commands
```

### Build only

```bash
./gradlew build
```

The jar is written to `build/libs/`.

---

## Roadmap

- Rewards per level (stats, abilities)
- Weapon mastery bonuses and a mastery HUD
- Configurable XP per enemy type

---

> *Forge your legend.*
