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
