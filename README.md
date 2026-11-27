
# Mga Nilalang — Filipino Folklore Dungeon

A Java 17+ Swing game built from the supplied `map.json`, `spritesheet.png`, player sprite strips, and `EnemiesAnimation.zip`.

## Run on Windows
Open PowerShell in this folder and run:

```powershell
gradle run
```

If Gradle is installed in a custom folder, add its `bin` folder to the current PowerShell session first, for example:

```powershell
$env:Path += ";C:\Users\Lenovo\Documents\assets\Gradle\gradle-9.9.2\bin"
gradle run
```

Java 17 or newer is required. Java 26 is supported by this source code when Gradle runs on it.

## Controls
- **WASD / Arrow keys** — walk
- **Space** — sword attack
- **E** — open the nearest nearby door
- **Mouse wheel / + / -** — zoom in and out
- **R** — restart after winning or dying

## Five stages
1. Tiyanak — starts in the smallest upper-left chamber (smallest active play area)
2. Aswang — upper hall
3. Tikbalang — west middle chamber
4. Manananggal — east middle chamber
5. Kapre — lower chamber

Clear all enemies in a stage to add that creature to your allies and advance. Enemies wake when the player is close, chase, attack, and use their walk-left/right, attack, and dying animations from the supplied GIFs. Player and enemy movement is restricted to cells in the map base-floor layer. Feet colliders also block walls, furniture/prop layers, and traps; invalid legacy spawn points are moved to the nearest valid floor cell in that stage. The camera smoothly follows the player and clamps to the map edges at the selected zoom. Doors use the supplied nine-frame DoorAnimation.png strip and remain blocked until the opening animation finishes.

## Asset preservation
The newly supplied map.json and spritesheet.png are installed as the active map and tile atlas. The supplied DoorAnimation.png strip is installed as the runtime door animation. The original player and enemy ZIP archives are also retained, and the supplied `Player.java` is kept in `reference/` as-is. Extracted animation frames are additional runtime assets; they do not replace or delete the originals.

# ⚔️ Dungeonbound: The Filipino Labyrinth

> *Explore the maze. Fight the creatures of Philippine folklore. Learn their stories.*

**🎮 Genre:** Dungeon crawler &nbsp;|&nbsp; **💻 Built with:** Java (GUI) &nbsp;|&nbsp; **👤 Mode:** Single-player, offline  
**📚 Course:** OOP 101 &nbsp;|&nbsp; **👥 Team:** NPC (Team 4, CS-2103)

---

## About

Dungeonbound is a single-player Java game with a graphical interface. Players explore randomly generated labyrinths, defeat enemies inspired by Philippine folklore, and earn coins to upgrade their character before facing a final boss.

There are plenty of dungeon and fantasy games, but few are built around Filipino culture and mythology. This project aims to be both a fun challenge and a way to introduce Filipino mythical creatures to players.

---

## Who It's For

| Player | What they get |
|---|---|
| **Casual gamers & fantasy fans** | A dungeon adventure with exploration, combat, and resource management leading to a final boss. |
| **Students & cultural enthusiasts** | An in-game wiki, story, and themed items that teach Filipino mythology and folklore. |

---

## Features

**🔐 Accounts**
Register and log in so your progress is saved.

**🗺️ Labyrinth Exploration**
Move through generated labyrinths with wall collision.

**👹 Folklore Enemies**
Each enemy has its own stats and skills, and is placed randomly in the maze.

**🪙 Coins & Shop**
Collect coins and loot, then spend them on HP, weapon damage, and skills.

**📖 Game Wiki**
Defeat a creature to unlock its entry and read about its place in Philippine folklore.

**☠️ Final Boss**
Conquer every labyrinth to face the boss. Lose and you can retry or end the game.

---

## How the Game Flows

```
Login / Register
      │
      ▼
Enter the dungeon ──► Explore the labyrinth
      │
      ▼
Battle enemies ──► Unlock wiki entries ──► Collect coins
      │
      ▼
Upgrade at the shop
      │
      ▼
All labyrinths conquered?
   │              │
   no             yes
   │              │
   ▼              ▼
Next labyrinth    Final boss
                  │         │
                 lose      win
                  │         │
          Retry / Game Over  Dungeon conquered
```

---


## 🚀 How to Run the Game (Paano Patakbuhin)

### System Requirements
- **Java Development Kit (JDK):** Version 17 or higher
- **Operating System:** Windows, macOS, or Linux

### Quick Start
Clone the repository and run the desktop application using the included Gradle wrapper:

```bash
# 1. Clone the repository
git clone https://github.com/neonekiii/OOP101-FinalProject-CS2103-Team04.git
cd OOP101-FinalProject-CS2103-Team04

# 2. Run the game
# On Windows (PowerShell / Command Prompt / Git Bash):
.\gradlew.bat lwjgl3:run

# On macOS / Linux:
./gradlew lwjgl3:run
```

### Controls (Mga Kontrol sa Laro)

| Action | Keyboard | Mouse / Touch |
|---|---|---|
| **Movement (Galaw)** | `W`, `A`, `S`, `D` or `Arrow Keys` | On-screen Joystick (bottom-left) |
| **Attack (Atake)** | `SPACE` bar (Sword swing) | Screen / Space |
| **Pause / Menu** | `ESC` key | In-game buttons |
| **Navigate Menus** | `W`/`S` or `Up`/`Down`, `Enter` | Mouse Hover / Click |

---


## OOP Concepts

| Concept | Where it applies |
|---|---|
| **Classes & Objects** | `Player`, `Enemy`, `Labyrinth`, `Shop`, `Account`. Every enemy in the maze is its own object. |
| **Encapsulation** | HP, coins, level, weapon, skills, and progress are private and changed only through methods (e.g. spending coins on an upgrade). |
| **Inheritance** | `Character` is the parent of `Player` and `Enemy`. Each folklore enemy extends `Enemy`. |
| **Polymorphism** | `attack()` and `useSkill()` act differently per enemy type: fast but weak, or slow but heavy-hitting. |
| **Abstraction** | The player picks "Upgrade Weapon"; the stat calculations stay hidden in the background. |
| **Exception Handling** | Covers invalid logins, unavailable menu options, and spending more coins than owned. |
| **Arrays / Lists** | Store the labyrinth layout, enemies, inventory, and wiki entries. |

---

## Team NPC

| Member | Role |
|---|---|
| **Charmaine T. Argona** ([@eniaxen](https://github.com/eniaxen)) | Character Designer, Repository Manager, Programmer, Documentor |
| **Dhymple Norshe F. Carandang** ([@ajax1114](https://github.com/ajax1114)) | Enemy Designer, 2D Background Artist, Programmer, Documentor |
| **Phoebe Leonides M. Dolor** ([@neonekiii](https://github.com/neonekiii)) | GUI & Item Designer, Programmer, Documentor |


---

## 🛡️ Intellectual Property & Assets Notice

All original pixel art, character animations, folklore monster cards, UI layouts, and audio design in this repository were created specifically for **DungeonBound** (OOP101 Final Project, CS2103 Team 04).
- **GUI, Item Designs, Wiki Cards & Sound Effects:** Created and owned by **Phoebe Leonides M. Dolor** ([@neonekiii](https://github.com/neonekiii)).
- **Character Designs:** Created and owned by **Charmaine T. Argona** ([@eniaxen](https://github.com/eniaxen)).
- **Enemy Designs & 2D Backgrounds:** Created and owned by **Dhymple Norshe F. Carandang** ([@ajax1114](https://github.com/ajax1114)).

> **Notice:** All rights reserved. Unauthorized reproduction, extraction, redistribution, or reuse of these creative assets in other academic submissions or third-party projects without explicit permission from the authors is strictly prohibited.

