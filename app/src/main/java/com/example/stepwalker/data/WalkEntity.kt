package com.example.stepwalker.data

import androidx.room.Entity
import androidx.room.PrimaryKey

@Entity(tableName = "walks")
data class WalkEntity(
    @PrimaryKey(autoGenerate = true) val id: Long = 0,
    val startedAt: Long,
    val endedAt: Long,
    val steps: Int,
    val distanceKm: Double,
    val calories: Int,
    val avgSpeedKmh: Double
)
