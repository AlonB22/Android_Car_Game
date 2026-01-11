package com.example.hw1.data

import android.content.Context
import com.example.hw1.game.GameMode

class ScoreRepository private constructor(context: Context) {
    private val dao = AppDatabase.getInstance(context).scoreDao()

    suspend fun addScore(score: ScoreEntity) {
        dao.insert(score)
    }

    suspend fun getTopScores(mode: GameMode): List<ScoreEntity> {
        return dao.getTopScores(mode.name)
    }

    companion object {
        @Volatile
        private var INSTANCE: ScoreRepository? = null

        fun getInstance(context: Context): ScoreRepository {
            return INSTANCE ?: synchronized(this) {
                INSTANCE ?: ScoreRepository(context).also { INSTANCE = it }
            }
        }
    }
}
