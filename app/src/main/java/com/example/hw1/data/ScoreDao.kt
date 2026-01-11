package com.example.hw1.data

import androidx.room.Dao
import androidx.room.Insert
import androidx.room.Query

@Dao
interface ScoreDao {
    @Insert
    suspend fun insert(score: ScoreEntity): Long

    @Query("SELECT * FROM scores WHERE mode = :mode ORDER BY distance DESC, timestamp DESC LIMIT 10")
    suspend fun getTopScores(mode: String): List<ScoreEntity>
}
