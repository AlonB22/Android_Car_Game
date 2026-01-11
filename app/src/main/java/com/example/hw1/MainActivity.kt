package com.example.hw1

import android.Manifest
import android.content.Context
import android.content.pm.PackageManager
import android.hardware.Sensor
import android.hardware.SensorEvent
import android.hardware.SensorEventListener
import android.hardware.SensorManager
import android.os.Bundle
import android.os.Handler
import android.os.Looper
import android.os.SystemClock
import android.view.Gravity
import android.view.View
import android.animation.ObjectAnimator
import android.widget.FrameLayout
import android.widget.ImageView
import android.widget.LinearLayout
import androidx.activity.result.contract.ActivityResultContracts
import androidx.appcompat.app.AlertDialog
import androidx.appcompat.app.AppCompatActivity
import androidx.core.content.ContextCompat
import androidx.lifecycle.lifecycleScope
import com.example.hw1.data.ScoreEntity
import com.example.hw1.data.ScoreRepository
import com.example.hw1.databinding.ActivityMainBinding
import com.example.hw1.game.GameConfig
import com.example.hw1.game.GameEngine
import com.example.hw1.game.GameMode
import com.example.hw1.game.GameState
import com.example.hw1.services.AndroidHaptics
import com.example.hw1.services.AndroidSfx
import com.example.hw1.services.Haptics
import com.example.hw1.services.Sfx
import com.google.android.gms.location.FusedLocationProviderClient
import com.google.android.gms.location.LocationServices
import com.google.android.gms.location.Priority
import com.google.android.gms.tasks.CancellationTokenSource
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import kotlin.math.abs

class MainActivity : AppCompatActivity(), GameEngine.Listener, SensorEventListener {

    companion object {
        const val EXTRA_MODE = "extra_mode"
    }

    private data class ModeSettings(
        val tickMs: Long,
        val spawnEveryTicks: Int,
        val coinEveryTicks: Int,
        val distancePerTick: Int,
        val enablePitchSpeed: Boolean
    )

    private lateinit var b: ActivityMainBinding
    private lateinit var engine: GameEngine
    private lateinit var haptics: Haptics
    private lateinit var sfx: Sfx
    private lateinit var repo: ScoreRepository
    private lateinit var fusedLocationClient: FusedLocationProviderClient

    private val handler = Handler(Looper.getMainLooper())
    private var running = false
    private var isGameOver = false
    private var gameOverDialog: AlertDialog? = null
    private var boardReady = false

    private lateinit var obstacleCells: Array<Array<ImageView>>
    private lateinit var coinCells: Array<Array<ImageView>>
    private lateinit var playerCells: Array<ImageView>

    private var currentTickMs = 300L
    private var baseTickMs = 300L

    private var mode: GameMode = GameMode.SLOW
    private lateinit var config: GameConfig
    private lateinit var settings: ModeSettings

    private val sensorManager by lazy {
        getSystemService(Context.SENSOR_SERVICE) as SensorManager
    }
    private var sensor: Sensor? = null
    private var tiltArmed = true
    private var lastTiltMoveMs = 0L

    private val tiltThresholdHigh = 3.2f
    private val tiltThresholdLow = 1.6f
    private val tiltDebounceMs = 180L

    private val pitchMin = -6f
    private val pitchMax = 6f
    private val minTickMs = 150L
    private val maxTickMs = 420L

    private var pendingFinalState: GameState? = null
    private var crashShake: ObjectAnimator? = null

    private val locationPermissionLauncher = registerForActivityResult(
        ActivityResultContracts.RequestPermission()
    ) { granted ->
        val state = pendingFinalState ?: return@registerForActivityResult
        if (granted) {
            fetchLocationAndSave(state)
        } else {
            saveScore(state, null, null)
        }
        pendingFinalState = null
    }

    private val tickRunnable = object : Runnable {
        override fun run() {
            if (!running) return
            engine.tick()
            handler.postDelayed(this, currentTickMs)
        }
    }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        b = ActivityMainBinding.inflate(layoutInflater)
        setContentView(b.root)

        requestLocationPermissionIfNeeded()

        mode = parseMode(intent.getStringExtra(EXTRA_MODE))
        settings = settingsForMode(mode)
        config = GameConfig(
            lanes = 5,
            rows = 12,
            lives = 3,
            spawnEveryTicks = settings.spawnEveryTicks,
            coinEveryTicks = settings.coinEveryTicks,
            distancePerTick = settings.distancePerTick
        )

        baseTickMs = settings.tickMs
        currentTickMs = baseTickMs

        haptics = AndroidHaptics(this)
        sfx = AndroidSfx(this)
        repo = ScoreRepository.getInstance(applicationContext)
        fusedLocationClient = LocationServices.getFusedLocationProviderClient(this)

        engine = GameEngine(config).also { it.listener = this }

        b.btnLeft.setOnClickListener { engine.moveRight() }
        b.btnRight.setOnClickListener { engine.moveLeft() }

        applyModeUi()

        b.road.post {
            buildBoard()
            engine.reset()
            startLoop()
        }
    }

    override fun onResume() {
        super.onResume()
        if (mode == GameMode.SENSOR) registerSensor()
        if (!isGameOver && boardReady) startLoop()
    }

    override fun onPause() {
        super.onPause()
        if (mode == GameMode.SENSOR) sensorManager.unregisterListener(this)
        gameOverDialog?.dismiss()
        stopLoop()
    }

    override fun onDestroy() {
        super.onDestroy()
        sfx.release()
    }

    private fun startLoop() {
        if (running || isGameOver || !boardReady) return
        running = true
        handler.post(tickRunnable)
    }

    private fun stopLoop() {
        running = false
        handler.removeCallbacks(tickRunnable)
    }

    private fun buildBoard() {
        boardReady = false
        val road = b.road
        road.removeAllViews()

        obstacleCells = Array(config.rows) { Array(config.lanes) { ImageView(this) } }
        coinCells = Array(config.rows) { Array(config.lanes) { ImageView(this) } }
        playerCells = Array(config.lanes) { ImageView(this) }

        val obstacleSize = dp(40)
        val coinSize = dp(26)
        val playerSize = dp(44)
        val playerRow = config.rows - 1

        for (row in 0 until config.rows) {
            val rowLayout = LinearLayout(this).apply {
                orientation = LinearLayout.HORIZONTAL
                layoutParams = LinearLayout.LayoutParams(
                    LinearLayout.LayoutParams.MATCH_PARENT,
                    0,
                    1f
                )
            }

            for (lane in 0 until config.lanes) {
                val cell = FrameLayout(this).apply {
                    layoutParams = LinearLayout.LayoutParams(
                        0,
                        LinearLayout.LayoutParams.MATCH_PARENT,
                        1f
                    )
                }

                val obstacle = ImageView(this).apply {
                    setImageResource(R.drawable.ic_block)
                    alpha = 0f
                }
                val obstacleLp = FrameLayout.LayoutParams(
                    obstacleSize,
                    obstacleSize,
                    Gravity.CENTER
                )
                cell.addView(obstacle, obstacleLp)
                obstacleCells[row][lane] = obstacle

                val coin = ImageView(this).apply {
                    setImageResource(R.drawable.ic_coin)
                    alpha = 0f
                }
                val coinLp = FrameLayout.LayoutParams(
                    coinSize,
                    coinSize,
                    Gravity.CENTER
                )
                cell.addView(coin, coinLp)
                coinCells[row][lane] = coin

                if (row == playerRow) {
                    val player = ImageView(this).apply {
                        setImageResource(R.drawable.ic_biker)
                        alpha = 0f
                    }
                    val playerLp = FrameLayout.LayoutParams(
                        playerSize,
                        playerSize,
                        Gravity.CENTER
                    )
                    cell.addView(player, playerLp)
                    playerCells[lane] = player
                }

                rowLayout.addView(cell)

                if (lane < config.lanes - 1) {
                    val divider = View(this).apply {
                        setBackgroundColor(0x80FFFFFF.toInt())
                        alpha = if (row % 2 == 0) 0.6f else 0.2f
                    }
                    val dividerLp = LinearLayout.LayoutParams(
                        dp(2),
                        LinearLayout.LayoutParams.MATCH_PARENT
                    )
                    rowLayout.addView(divider, dividerLp)
                }
            }

            road.addView(rowLayout)
        }
        boardReady = true
    }

    private fun renderState(state: GameState) {
        for (row in 0 until state.rows) {
            for (lane in 0 until state.lanes) {
                obstacleCells[row][lane].alpha = if (state.obstacles[row][lane]) 1f else 0f
                coinCells[row][lane].alpha = if (state.coinGrid[row][lane]) 1f else 0f
            }
        }

        for (lane in 0 until state.lanes) {
            playerCells[lane].alpha = if (lane == state.playerLane) 1f else 0f
        }

        b.heart1.alpha = if (state.lives >= 1) 1f else 0.2f
        b.heart2.alpha = if (state.lives >= 2) 1f else 0.2f
        b.heart3.alpha = if (state.lives >= 3) 1f else 0.2f

        b.distanceText.text = "Distance: ${state.distance}"
        b.coinsText.text = "Coins: ${state.coins}"
    }

    private fun applyModeUi() {
        b.modeText.text = "Mode: ${mode.name}"
        b.controlBar.visibility = if (mode == GameMode.SENSOR) View.GONE else View.VISIBLE
    }

    private fun registerSensor() {
        sensor = sensorManager.getDefaultSensor(Sensor.TYPE_GRAVITY)
            ?: sensorManager.getDefaultSensor(Sensor.TYPE_ACCELEROMETER)
        val activeSensor = sensor
        if (activeSensor == null) {
            b.controlBar.visibility = View.VISIBLE
            return
        }
        sensorManager.registerListener(this, activeSensor, SensorManager.SENSOR_DELAY_GAME)
    }

    override fun onSensorChanged(event: SensorEvent) {
        if (mode != GameMode.SENSOR) return
        val x = event.values[0]
        val y = event.values[1]
        val now = SystemClock.uptimeMillis()

        if (tiltArmed && now - lastTiltMoveMs > tiltDebounceMs) {
            when {
                x > tiltThresholdHigh -> {
                    engine.moveRight()
                    tiltArmed = false
                    lastTiltMoveMs = now
                }
                x < -tiltThresholdHigh -> {
                    engine.moveLeft()
                    tiltArmed = false
                    lastTiltMoveMs = now
                }
            }
        }

        if (!tiltArmed && abs(x) < tiltThresholdLow) {
            tiltArmed = true
        }

        if (settings.enablePitchSpeed) {
            val normalized = ((-y - pitchMin) / (pitchMax - pitchMin)).coerceIn(0f, 1f)
            currentTickMs = (maxTickMs - (maxTickMs - minTickMs) * normalized).toLong()
        } else {
            currentTickMs = baseTickMs
        }
    }

    override fun onAccuracyChanged(sensor: Sensor?, accuracy: Int) = Unit

    override fun onStateChanged(state: GameState) {
        if (!boardReady) return
        renderState(state)
    }

    override fun onCrash(livesLeft: Int) {
        haptics.vibrateCrash()
        sfx.playCrash()
        shakeOnCrash()
    }

    override fun onGameOver(finalState: GameState) {
        if (isGameOver) return
        isGameOver = true
        haptics.vibrateGameOver()
        stopLoop()
        saveScoreWithLocation(finalState)
        showGameOverDialog()
    }

    private fun saveScoreWithLocation(state: GameState) {
        if (ContextCompat.checkSelfPermission(
                this,
                Manifest.permission.ACCESS_FINE_LOCATION
            ) == PackageManager.PERMISSION_GRANTED
        ) {
            fetchLocationAndSave(state)
        } else {
            pendingFinalState = state
            locationPermissionLauncher.launch(Manifest.permission.ACCESS_FINE_LOCATION)
        }
    }

    private fun requestLocationPermissionIfNeeded() {
        if (ContextCompat.checkSelfPermission(
                this,
                Manifest.permission.ACCESS_FINE_LOCATION
            ) == PackageManager.PERMISSION_GRANTED
        ) return
        locationPermissionLauncher.launch(Manifest.permission.ACCESS_FINE_LOCATION)
    }

    private fun fetchLocationAndSave(state: GameState) {
        val tokenSource = CancellationTokenSource()
        var handled = false
        val timeout = Runnable {
            if (handled) return@Runnable
            handled = true
            tokenSource.cancel()
            fetchLastLocationAndSave(state)
        }

        handler.postDelayed(timeout, 2000L)
        fusedLocationClient.getCurrentLocation(
            Priority.PRIORITY_BALANCED_POWER_ACCURACY,
            tokenSource.token
        ).addOnSuccessListener { location ->
            if (handled) return@addOnSuccessListener
            handled = true
            handler.removeCallbacks(timeout)
            if (location != null) {
                saveScore(state, location.latitude, location.longitude)
            } else {
                fetchLastLocationAndSave(state)
            }
        }.addOnFailureListener {
            if (handled) return@addOnFailureListener
            handled = true
            handler.removeCallbacks(timeout)
            fetchLastLocationAndSave(state)
        }
    }

    private fun fetchLastLocationAndSave(state: GameState) {
        fusedLocationClient.lastLocation
            .addOnSuccessListener { location ->
                saveScore(state, location?.latitude, location?.longitude)
            }
            .addOnFailureListener {
                saveScore(state, null, null)
            }
    }

    private fun saveScore(state: GameState, latitude: Double?, longitude: Double?) {
        val score = ScoreEntity(
            mode = mode.name,
            distance = state.distance,
            coins = state.coins,
            timestamp = System.currentTimeMillis(),
            latitude = latitude,
            longitude = longitude
        )
        lifecycleScope.launch(Dispatchers.IO) {
            repo.addScore(score)
        }
    }

    private fun showGameOverDialog() {
        gameOverDialog?.dismiss()
        gameOverDialog = AlertDialog.Builder(this)
            .setTitle("Game Over")
            .setMessage("Restart a new game or exit?")
            .setCancelable(false)
            .setPositiveButton("Restart") { _, _ ->
                isGameOver = false
                currentTickMs = baseTickMs
                engine.reset()
                startLoop()
            }
            .setNegativeButton("Exit") { _, _ ->
                finish()
            }
            .create()

        gameOverDialog?.show()
    }

    private fun parseMode(value: String?): GameMode {
        return try {
            GameMode.valueOf(value ?: GameMode.SLOW.name)
        } catch (_: IllegalArgumentException) {
            GameMode.SLOW
        }
    }

    private fun settingsForMode(mode: GameMode): ModeSettings {
        return when (mode) {
            GameMode.SLOW -> ModeSettings(
                tickMs = 360L,
                spawnEveryTicks = 4,
                coinEveryTicks = 3,
                distancePerTick = 1,
                enablePitchSpeed = false
            )
            GameMode.FAST -> ModeSettings(
                tickMs = 220L,
                spawnEveryTicks = 3,
                coinEveryTicks = 2,
                distancePerTick = 2,
                enablePitchSpeed = false
            )
            GameMode.SENSOR -> ModeSettings(
                tickMs = 280L,
                spawnEveryTicks = 3,
                coinEveryTicks = 2,
                distancePerTick = 2,
                enablePitchSpeed = true
            )
        }
    }

    private fun shakeOnCrash() {
        val dx = dp(8).toFloat()
        crashShake?.cancel()
        crashShake = ObjectAnimator.ofFloat(
            b.road,
            "translationX",
            0f,
            -dx,
            dx,
            -dx * 0.7f,
            dx * 0.7f,
            0f
        ).apply {
            duration = 180L
            start()
        }
    }

    private fun dp(v: Int): Int =
        (v * resources.displayMetrics.density).toInt()
}
