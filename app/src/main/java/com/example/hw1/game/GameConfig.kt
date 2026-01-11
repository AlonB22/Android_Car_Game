package com.example.hw1.game

enum class GameMode {
    SLOW,
    FAST,
    SENSOR
}

data class GameConfig(
    val lanes: Int = 5,
    val rows: Int = 12,
    val lives: Int = 3,
    val spawnEveryTicks: Int,
    val coinEveryTicks: Int,
    val distancePerTick: Int
)
