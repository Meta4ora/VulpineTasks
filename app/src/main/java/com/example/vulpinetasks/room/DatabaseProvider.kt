package com.example.vulpinetasks.room

import android.content.Context
import androidx.room.Room

object DatabaseProvider {

    lateinit var db: AppDatabase

    fun init(context: Context) {
        db = Room.databaseBuilder(
            context,
            AppDatabase::class.java,
            "notes.db"
        )
            .fallbackToDestructiveMigration()
            .build()
    }
}