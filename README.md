# Android Car Game

## Overview
An Android mini-game built with Kotlin and Android Studio. You drive a biker down a lane-based road, dodge obstacles, collect coins, and rack up distance. The game targets API 30 compatibility while running on newer SDKs.

## Gameplay
- 5 lanes, 12 rows, grid-based movement (no Canvas, no coordinate movement).
- Obstacles and coins spawn over time; visibility is toggled on a pre-built board.
- You start with 3 lives; collisions reduce lives and trigger a crash effect.
- Game ends at 0 lives with a restart/exit dialog.

## Modes and Controls
- **SLOW** and **FAST**: use on-screen left/right buttons.
- **SENSOR**: tilt to change lanes; tilt pitch controls speed.

## High Scores
- Top 10 scores per mode are stored via Room.
- Each score shows distance, coins, time, and location when available.
- A small map (lite mode) renders the score location per entry.
- City names are resolved from GPS coordinates when possible.

## Audio and Haptics
- Crash feedback uses vibration and a deeper, layered crash sound.
- Game over also triggers haptic feedback.

## Setup Notes
- Set a valid Maps API key in `app/src/main/res/values/strings.xml` as `google_maps_key` (do not commit real keys).
- Location permission is required to store coordinates for high score maps.

## Key Files
- `app/src/main/java/com/example/hw1/MainActivity.kt`: game loop, input, crash/game-over handling.
- `app/src/main/java/com/example/hw1/HighScoresActivity.kt`: score list + per-entry map.
- `app/src/main/java/com/example/hw1/game/`: game engine and state.
- `app/src/main/java/com/example/hw1/data/`: Room database and repository.

## Build and Run
- `./gradlew assembleDebug`
- `./gradlew test`
- `./gradlew connectedAndroidTest`
- `./gradlew lint`
