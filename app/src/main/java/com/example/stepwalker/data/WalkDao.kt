package com.example.stepwalker.data

import androidx.room.Dao
import androidx.room.Insert
import androidx.room.Query
import kotlinx.coroutines.flow.Flow

@Dao
interface WalkDao {
    @Insert suspend fun insert(walk: WalkEntity)
    @Query("SELECT * FROM walks ORDER BY startedAt DESC")
    fun observeAll(): Flow<List<WalkEntity>>
}
