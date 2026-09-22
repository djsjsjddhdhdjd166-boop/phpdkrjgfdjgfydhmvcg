package com.example.stepwalker.data

import androidx.room.Database
import androidx.room.RoomDatabase

@Database(entities = [WalkEntity::class], version = 1, exportSchema = false)
abstract class AppDatabase : RoomDatabase() {
    abstract fun walkDao(): WalkDao
}
