# Borderbound

A Minecraft manhunt-style PvP game mod where players fight to be the last one standing while racing against a shrinking world border!

## Features

### Core Gameplay
- **Shrinking World Border**: Customizable starting and ending sizes with configurable shrink time
- **Strategic Player Placement**: Players spawn at evenly distributed positions around the border edge
- **Slow Falling Start**: 1-minute slow falling effect at game start for safe descent from 200-block spawn height
- **Dynamic Respawning**: Players respawn at their designated edge, 100 blocks inside the current border
- **PvP Control**: PvP automatically enables when the border reaches its final size (or can be enabled from start)
- **Pause/Resume**: Take breaks during gameplay with `/bbpause` command

### HUD & UI
- **Border Distance Indicator**: Real-time display showing distance to world border
- **Color-Coded Warnings**:
  - 🟢 Green: Safe (160m+)
  - 🟡 Yellow: Caution (100-160m)
  - 🟠 Orange: Warning (50-100m)
  - 🔴 Red: Danger (< 50m)

## Commands

### `/bbstart`
Start a new Borderbound game.

**Usage:**
```
/bbstart [startsize] [finishsize] [time] [enablepvp]
```

**Parameters:**
- `startsize` (optional): Initial border distance from center in blocks. Default: `10000`
- `finishsize` (optional): Final border distance from center in blocks. Default: `100`
- `time` (optional): Time in seconds for border to shrink. Default: `7200` (2 hours)
- `enablepvp` (optional): Enable PvP from game start. Default: `false`

**Examples:**
```
/bbstart
/bbstart 5000 50 3600
/bbstart 10000 100 7200 true
```

**Permissions:** Requires OP level 2

### `/bbpause`
Pause or resume the current game.

**Usage:**
```
/bbpause
```

When paused:
- World border stops shrinking
- Remaining time is preserved
- Run `/bbpause` again to resume

**Permissions:** Requires OP level 2

## Gameplay Mechanics

### Game Start
1. All online players are teleported to starting positions around the border
2. Players receive 1 minute of slow falling
3. World border begins shrinking from `startsize` to `finishsize`
4. PvP is disabled (unless `enablepvp` is true)

### Player Positioning
- **1-2 players**: East/West positions
- **3 players**: East, West, North
- **4 players**: Cardinal directions (N, S, E, W)
- **5-8 players**: Cardinal directions + diagonal corners

All positions are 100 blocks inside the starting border.

### Death & Respawning
- Players respawn at their designated edge direction
- Respawn position is 100 blocks inside the current border
- Respawning only works while border is still shrinking

### PvP Activation
- By default, PvP is disabled at game start
- PvP automatically enables when border reaches `finishsize`
- Can be enabled from start with `enablepvp true` parameter

## Technical Details

### Requirements
- **Minecraft Version**: 1.21.10
- **Mod Loader**: Fabric 0.17.2+
- **Fabric Language Kotlin**: 1.13.6+kotlin.2.2.20
- **Fabric API**: 0.136.0+1.21.10

### Installation
1. Install [Fabric Loader](https://fabricmc.net/use/)
2. Download [Fabric API](https://www.curseforge.com/minecraft/mc-mods/fabric-api)
3. Download [Fabric Language Kotlin](https://www.curseforge.com/minecraft/mc-mods/fabric-language-kotlin)
4. Place `borderbound-1.0-SNAPSHOT.jar` in your `mods` folder
5. Launch Minecraft

### Building from Source
```bash
./gradlew build
```

The compiled mod will be in `build/libs/borderbound-1.0-SNAPSHOT.jar`

## License

MIT License - See LICENSE.txt for details

## Credits

Built with [Fabric](https://fabricmc.net/) and [Kotlin](https://kotlinlang.org/)
