package com.example.hw1.data

import androidx.room.Entity
import androidx.room.PrimaryKey

@Entity(tableName = "scores")
data class ScoreEntity(
    @PrimaryKey(autoGenerate = true) val id: Long = 0,
    val mode: String,
    val distance: Int,
    val coins: Int,
    val timestamp: Long,
    val latitude: Double?,
    val longitude: Double?
)
