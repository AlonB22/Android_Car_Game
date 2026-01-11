package com.example.hw1

import android.content.Intent
import android.os.Bundle
import androidx.appcompat.app.AppCompatActivity
import com.example.hw1.databinding.ActivityMenuBinding
import com.example.hw1.game.GameMode

class MenuActivity : AppCompatActivity() {

    private lateinit var b: ActivityMenuBinding

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        b = ActivityMenuBinding.inflate(layoutInflater)
        setContentView(b.root)

        b.btnSlow.setOnClickListener { launchGame(GameMode.SLOW) }
        b.btnFast.setOnClickListener { launchGame(GameMode.FAST) }
        b.btnSensor.setOnClickListener { launchGame(GameMode.SENSOR) }
        b.btnHighScores.setOnClickListener {
            startActivity(Intent(this, HighScoresActivity::class.java))
        }
    }

    private fun launchGame(mode: GameMode) {
        val intent = Intent(this, MainActivity::class.java)
            .putExtra(MainActivity.EXTRA_MODE, mode.name)
        startActivity(intent)
    }
}
