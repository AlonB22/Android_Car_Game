package com.example.hw1.game

import kotlin.math.max
import kotlin.math.min
import kotlin.random.Random

class GameEngine(
    private val config: GameConfig,
    private val rng: Random = Random.Default
) {
    interface Listener {
        fun onStateChanged(state: GameState)
        fun onCrash(livesLeft: Int)
        fun onGameOver(finalState: GameState)
    }

    var listener: Listener? = null

    private var obstacles = Array(config.rows) { BooleanArray(config.lanes) }
    private var coins = Array(config.rows) { BooleanArray(config.lanes) }
    private var playerLane = config.lanes / 2
    private var lives = config.lives
    private var distance = 0
    private var coinsCollected = 0
    private var ticksSinceObstacle = 0
    private var ticksSinceCoin = 0
    private var isGameOver = false

    fun reset() {
        obstacles = Array(config.rows) { BooleanArray(config.lanes) }
        coins = Array(config.rows) { BooleanArray(config.lanes) }
        playerLane = config.lanes / 2
        lives = config.lives
        distance = 0
        coinsCollected = 0
        ticksSinceObstacle = 0
        ticksSinceCoin = 0
        isGameOver = false
        emitState()
    }

    fun moveLeft() {
        if (isGameOver) return
        playerLane = max(0, playerLane - 1)
        emitState()
    }

    fun moveRight() {
        if (isGameOver) return
        playerLane = min(config.lanes - 1, playerLane + 1)
        emitState()
    }

    fun tick() {
        if (isGameOver) return

        // Collision is checked after shifting obstacles into the player row.
        shiftDown()
        ticksSinceObstacle++
        ticksSinceCoin++

        if (ticksSinceObstacle >= config.spawnEveryTicks) {
            spawnObstacle()
            ticksSinceObstacle = 0
        }

        if (ticksSinceCoin >= config.coinEveryTicks) {
            spawnCoin()
            ticksSinceCoin = 0
        }

        checkCollisions()
        distance += config.distancePerTick

        val state = emitState()
        if (isGameOver) {
            listener?.onGameOver(state)
        }
    }

    private fun shiftDown() {
        for (row in config.rows - 1 downTo 1) {
            for (lane in 0 until config.lanes) {
                obstacles[row][lane] = obstacles[row - 1][lane]
                coins[row][lane] = coins[row - 1][lane]
            }
        }
        for (lane in 0 until config.lanes) {
            obstacles[0][lane] = false
            coins[0][lane] = false
        }
    }

    private fun spawnObstacle() {
        val lane = rng.nextInt(config.lanes)
        obstacles[0][lane] = true
    }

    private fun spawnCoin() {
        var lane = rng.nextInt(config.lanes)
        var tries = 0
        while (tries < config.lanes && obstacles[0][lane]) {
            lane = (lane + 1) % config.lanes
            tries++
        }
        if (!obstacles[0][lane]) {
            coins[0][lane] = true
        }
    }

    private fun checkCollisions() {
        val playerRow = config.rows - 1

        if (obstacles[playerRow][playerLane]) {
            obstacles[playerRow][playerLane] = false
            lives--
            listener?.onCrash(lives)
            if (lives <= 0) {
                isGameOver = true
            }
        }

        if (coins[playerRow][playerLane]) {
            coins[playerRow][playerLane] = false
            coinsCollected++
        }
    }

    private fun emitState(): GameState {
        val obstacleCopy = Array(config.rows) { row -> obstacles[row].clone() }
        val coinCopy = Array(config.rows) { row -> coins[row].clone() }
        val state = GameState(
            lanes = config.lanes,
            rows = config.rows,
            playerLane = playerLane,
            lives = lives,
            distance = distance,
            coins = coinsCollected,
            obstacles = obstacleCopy,
            coinGrid = coinCopy,
            isGameOver = isGameOver
        )
        listener?.onStateChanged(state)
        return state
    }
}
