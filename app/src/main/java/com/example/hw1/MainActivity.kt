package com.example.hw1

import android.graphics.Rect
import android.os.Build
import android.os.Bundle
import android.os.SystemClock
import android.os.VibrationEffect
import android.os.Vibrator
import android.view.Choreographer
import android.view.View
import android.widget.ImageView
import android.widget.Toast
import androidx.appcompat.app.AlertDialog
import androidx.appcompat.app.AppCompatActivity
import androidx.constraintlayout.widget.ConstraintLayout
import androidx.constraintlayout.widget.ConstraintSet
import androidx.constraintlayout.widget.Guideline
import com.example.hw1.databinding.ActivityMainBinding
import kotlin.math.max
import kotlin.math.min
import kotlin.random.Random

class MainActivity : AppCompatActivity() {

    private lateinit var b: ActivityMainBinding

    private val LANE_COUNT = 3
    private val SPAWN_EVERY_MS = 650L
    private val SPEED_PX_PER_SEC = 820f
    private val CRASH_COOLDOWN_MS = 350L

    private var laneIndex = LANE_COUNT / 2
    private var lives = 3

    private lateinit var car: ImageView
    private lateinit var laneGuides: IntArray

    private data class Obstacle(val view: ImageView, var lane: Int)
    private val obstacles = ArrayList<Obstacle>(16)

    private var lastSpawnMs = 0L
    private var lastCrashMs = 0L

    private var running = false
    private var lastFrameNanos = 0L

    private var isGameOver = false
    private var gameOverDialog: AlertDialog? = null

    private val frameCallback = object : Choreographer.FrameCallback {
        override fun doFrame(frameTimeNanos: Long) {
            if (!running) return
            if (lastFrameNanos == 0L) lastFrameNanos = frameTimeNanos
            val dtSec = (frameTimeNanos - lastFrameNanos) / 1_000_000_000f
            lastFrameNanos = frameTimeNanos
            tick(dtSec)
            Choreographer.getInstance().postFrameCallback(this)
        }
    }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        b = ActivityMainBinding.inflate(layoutInflater)
        setContentView(b.root)

        b.btnLeft.setOnClickListener { moveCarBy(-1) }
        b.btnRight.setOnClickListener { moveCarBy(+1) }

        b.road.post {
            buildLanes(b.road as ConstraintLayout, LANE_COUNT)
            buildCar(b.road as ConstraintLayout)
            resetGame()
            startLoop()
        }
    }

    override fun onResume() {
        super.onResume()
        b.road.post {
            if (!isGameOver && ::laneGuides.isInitialized && ::car.isInitialized) startLoop()
        }
    }

    override fun onPause() {
        super.onPause()
        gameOverDialog?.dismiss()
        stopLoop()
    }

    private fun startLoop() {
        if (running || isGameOver) return
        running = true
        lastFrameNanos = 0L
        Choreographer.getInstance().postFrameCallback(frameCallback)
    }

    private fun stopLoop() {
        running = false
        lastFrameNanos = 0L
        Choreographer.getInstance().removeFrameCallback(frameCallback)
    }

    private fun tick(dtSec: Float) {
        if (isGameOver) return
        if (!::laneGuides.isInitialized || !::car.isInitialized) return

        val now = SystemClock.uptimeMillis()

        if (now - lastSpawnMs >= SPAWN_EVERY_MS) {
            lastSpawnMs = now
            spawnObstacle()
        }

        val dy = (SPEED_PX_PER_SEC * dtSec).toInt()
        val roadH = b.road.height

        var i = 0
        while (i < obstacles.size) {
            val o = obstacles[i]
            val lp = o.view.layoutParams as ConstraintLayout.LayoutParams
            lp.topMargin = lp.topMargin + dy
            o.view.layoutParams = lp

            if (lp.topMargin > roadH + o.view.height) {
                (b.road as ConstraintLayout).removeView(o.view)
                obstacles.removeAt(i)
                continue
            }

            if (isColliding(car, o.view)) {
                handleCrash(o, now)
                continue
            }

            i++
        }
    }

    private fun handleCrash(o: Obstacle, nowMs: Long) {
        if (nowMs - lastCrashMs < CRASH_COOLDOWN_MS) return
        lastCrashMs = nowMs

        (b.road as ConstraintLayout).removeView(o.view)
        obstacles.remove(o)

        lives--
        updateHearts()

        vibrate(60)
        Toast.makeText(this, "Crash!", Toast.LENGTH_SHORT).show()

        if (lives <= 0) {
            vibrate(140)
            showGameOverDialog()
        }
    }

    private fun showGameOverDialog() {
        if (isGameOver) return
        isGameOver = true
        stopLoop()

        gameOverDialog?.dismiss()
        gameOverDialog = AlertDialog.Builder(this)
            .setTitle("Game Over")
            .setMessage("Restart a new game or exit?")
            .setCancelable(false)
            .setPositiveButton("Restart") { _, _ ->
                isGameOver = false
                resetGame()
                startLoop()
            }
            .setNegativeButton("Exit") { _, _ ->
                finish()
            }
            .create()

        gameOverDialog?.show()
    }

    private fun resetGame() {
        val road = b.road as ConstraintLayout
        for (o in obstacles) road.removeView(o.view)
        obstacles.clear()

        isGameOver = false

        lives = 3
        updateHearts()

        laneIndex = LANE_COUNT / 2
        placeCarAtLane(laneIndex)

        lastSpawnMs = SystemClock.uptimeMillis()
        lastCrashMs = 0L
    }

    private fun updateHearts() {
        b.heart1.alpha = if (lives >= 1) 1f else 0.2f
        b.heart2.alpha = if (lives >= 2) 1f else 0.2f
        b.heart3.alpha = if (lives >= 3) 1f else 0.2f
    }

    private fun moveCarBy(delta: Int) {
        if (isGameOver) return
        val next = min(LANE_COUNT - 1, max(0, laneIndex + delta))
        if (next == laneIndex) return
        laneIndex = next
        placeCarAtLane(laneIndex)
    }

    private fun placeCarAtLane(lane: Int) {
        val road = b.road as ConstraintLayout
        val cs = ConstraintSet()
        cs.clone(road)

        val leftId = if (lane == 0) ConstraintSet.PARENT_ID else laneGuides[lane - 1]
        val rightId = if (lane == LANE_COUNT - 1) ConstraintSet.PARENT_ID else laneGuides[lane]

        cs.clear(car.id, ConstraintSet.START)
        cs.clear(car.id, ConstraintSet.END)

        cs.connect(car.id, ConstraintSet.START, leftId, ConstraintSet.START, 0)
        cs.connect(car.id, ConstraintSet.END, rightId, ConstraintSet.END, 0)
        cs.setHorizontalBias(car.id, 0.5f)

        cs.applyTo(road)
    }

    private fun spawnObstacle() {
        val road = b.road as ConstraintLayout
        val lane = Random.nextInt(LANE_COUNT)

        val v = ImageView(this).apply {
            id = View.generateViewId()
            setImageResource(R.drawable.ic_block)
        }

        val size = dp(40)

        val leftId = if (lane == 0) ConstraintSet.PARENT_ID else laneGuides[lane - 1]
        val rightId = if (lane == LANE_COUNT - 1) ConstraintSet.PARENT_ID else laneGuides[lane]

        val lp = ConstraintLayout.LayoutParams(size, size).apply {
            topToTop = ConstraintSet.PARENT_ID
            topMargin = -size
            startToStart = leftId
            endToEnd = rightId
            horizontalBias = 0.5f
        }

        road.addView(v, lp)
        obstacles.add(Obstacle(v, lane))
    }

    private fun isColliding(a: View, b: View): Boolean {
        val ra = Rect()
        val rb = Rect()
        a.getHitRect(ra)
        b.getHitRect(rb)
        return Rect.intersects(ra, rb)
    }

    private fun vibrate(ms: Long) {
        val vib = getSystemService(VIBRATOR_SERVICE) as Vibrator
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            vib.vibrate(VibrationEffect.createOneShot(ms, VibrationEffect.DEFAULT_AMPLITUDE))
        } else {
            @Suppress("DEPRECATION")
            vib.vibrate(ms)
        }
    }

    private fun buildCar(road: ConstraintLayout) {
        car = ImageView(this).apply {
            id = View.generateViewId()
            setImageResource(R.drawable.ic_biker)
        }

        val size = dp(44)

        val leftId = if (laneIndex == 0) ConstraintSet.PARENT_ID else laneGuides[laneIndex - 1]
        val rightId = if (laneIndex == LANE_COUNT - 1) ConstraintSet.PARENT_ID else laneGuides[laneIndex]

        val lp = ConstraintLayout.LayoutParams(size, size).apply {
            bottomToBottom = ConstraintSet.PARENT_ID
            bottomMargin = dp(14)
            startToStart = leftId
            endToEnd = rightId
            horizontalBias = 0.5f
        }

        road.addView(car, lp)
    }

    private fun buildLanes(road: ConstraintLayout, lanes: Int) {
        road.removeAllViews()

        laneGuides = IntArray(lanes - 1)

        for (i in 1 until lanes) {
            val g = Guideline(this).apply { id = View.generateViewId() }
            val glp = ConstraintLayout.LayoutParams(
                ConstraintLayout.LayoutParams.WRAP_CONTENT,
                ConstraintLayout.LayoutParams.WRAP_CONTENT
            ).apply {
                orientation = ConstraintLayout.LayoutParams.VERTICAL
                guidePercent = i / lanes.toFloat()
            }
            road.addView(g, glp)
            laneGuides[i - 1] = g.id

            val line = View(this).apply {
                id = View.generateViewId()
                setBackgroundColor(0x55FFFFFF.toInt())
            }
            val lp = ConstraintLayout.LayoutParams(dp(2), 0).apply {
                topToTop = ConstraintSet.PARENT_ID
                bottomToBottom = ConstraintSet.PARENT_ID
                startToStart = g.id
                endToEnd = g.id
            }
            road.addView(line, lp)
        }
    }

    private fun dp(v: Int): Int =
        (v * resources.displayMetrics.density).toInt()
}
