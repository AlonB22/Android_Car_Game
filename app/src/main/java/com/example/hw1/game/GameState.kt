package com.example.hw1.game

data class GameState(
    val lanes: Int,
    val rows: Int,
    val playerLane: Int,
    val lives: Int,
    val distance: Int,
    val coins: Int,
    val obstacles: Array<BooleanArray>,
    val coinGrid: Array<BooleanArray>,
    val isGameOver: Boolean
)
