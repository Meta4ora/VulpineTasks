package com.example.vulpinetasks.room

import android.content.Context
import androidx.room.Room
import androidx.room.migration.Migration
import androidx.sqlite.db.SupportSQLiteDatabase

object DatabaseProvider {

    lateinit var db: AppDatabase

    private val MIGRATION_4_5 = object : Migration(4, 5) {
        override fun migrate(database: SupportSQLiteDatabase) {
            // Создаем таблицу связей
            database.execSQL("""
                CREATE TABLE IF NOT EXISTS `note_relations` (
                    `relationId` INTEGER PRIMARY KEY AUTOINCREMENT NOT NULL,
                    `noteId` TEXT NOT NULL,
                    `parentNoteId` TEXT NOT NULL
                )
            """)

            // Создаем индексы для ускорения поиска
            database.execSQL("CREATE INDEX IF NOT EXISTS `index_note_relations_noteId` ON `note_relations` (`noteId`)")
            database.execSQL("CREATE INDEX IF NOT EXISTS `index_note_relations_parentNoteId` ON `note_relations` (`parentNoteId`)")
        }
    }

    fun init(context: Context) {
        db = Room.databaseBuilder(
            context,
            AppDatabase::class.java,
            "notes.db"
        )
            .addMigrations(MIGRATION_4_5)
            .fallbackToDestructiveMigration()
            .build()
    }
}