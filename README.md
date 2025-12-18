# Android Car Game

## Overview
This Android mini-game is a simple lane dodger built with **Kotlin** and **Android Studio** (API 30 / Android 11).  
You control a **biker** at the bottom of the road and avoid incoming obstacles that fall from the top.

## Gameplay
- The road is split into **3 lanes**.
- Obstacles spawn at the top in random lanes and move downward continuously.
- The biker can move **left / right** between lanes.
- You start with **3 lives** (hearts). Each collision removes one life.
- When lives reach **0**, the game **stops** and a dialog appears:
  - **Restart**: starts a new run
  - **Exit**: closes the app

## Controls
- **Left button**: move biker one lane left  
- **Right button**: move biker one lane right

## Technical Notes (What’s inside)
- **No Canvas**
- Lane positioning is done with **ConstraintLayout guidelines**
- Falling motion is done by updating **LayoutParams topMargin** per frame (no `x/y` usage)
- Main loop uses **Choreographer** for smooth frame timing
- Collision detection uses `getHitRect()` + `Rect.intersects()`
- Vibration feedback on crash and game over

## Project Structure (Key Files)
- `app/src/main/java/com/example/hw1/MainActivity.kt`  
  Game loop, lanes, spawning, collisions, game over dialog
- `app/src/main/res/layout/activity_main.xml`  
  UI layout (road area + controls + hearts)
- `app/src/main/res/drawable/`
  - `ic_biker.xml` (player)
  - `ic_block.xml` (obstacle)
  - `ic_left.xml`, `ic_right.xml` (buttons)
  - `ic_heart.xml` (lives)
  - `bg_road.xml` (roa_
