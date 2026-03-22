# Legends of the Ascended (LOA)

![Hytale](https://img.shields.io/badge/Game-Hytale-purple)
![Framework](https://img.shields.io/badge/Framework-ECS-blue)
![Language](https://img.shields.io/badge/Language-Java-orange)
![Status](https://img.shields.io/badge/Status-In_Development-yellow)

A modular **loa progression system** for *Hytale*, focused on enhancing gameplay through player leveling and systemic progression.

Built using an **Entity Component System (ECS)** architecture for scalability, organization, and maintainability.

---

## Overview

**Legends of the Ascended** introduces a structured progression system where players evolve through gameplay.

The current implementation focuses on:

- XP-based leveling
- persistent player progression
- real-time feedback through UI
- modular systems for future expansion

---

## Features

✔ XP and leveling system  
✔ Configurable XP progression (XPTable)  
✔ Custom XP HUD  
✔ Persistent player data (component-based)  
✔ Event-driven architecture (ECS)  
✔ Debug and testing commands  

---

## Architecture

The project follows an **ECS (Entity Component System)** structure:

```
src/
│
├── components
│   playerLOAComponent
│
├── systems
│   XPGainSystem
│   PlayerJoinSystem
│
├── events
│   GiveXPEvent
│   LevelUpEvent
│
├── handlers
│   LevelUpHandler
│
├── commands
│   loa commands
│
├── ui
│   XP HUD
│
└── util
    XPTable
```

Each module is responsible for a specific part of the system, ensuring clean separation of concerns.

---

## Commands

- `/loaxp` → grant XP  
- `/loastats` → view player stats  
- `/loa` → base command  

---

## Setup

### 1. Clone the repository

```bash
git clone https://github.com/your-username/legends-of-the-ascended.git
```

### 2. Configure environment

- Import using Gradle  
- Ensure `HytaleServer.jar` is available in `/libs`  

### 3. Run

Start the Hytale modding environment with the project configured.

---

## Author

Project focused on:

- ECS Architecture  
- loa Progression Systems  
- Hytale Mod Development  

---

> *Forge your legend.*