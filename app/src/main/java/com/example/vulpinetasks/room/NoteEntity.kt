package com.example.vulpinetasks.room

import androidx.room.Entity
import androidx.room.PrimaryKey

@Entity(tableName = "notes")
data class NoteEntity(
    @PrimaryKey val id: String,
    val userId: String,
    val title: String,
    val type: String,
    val updatedAt: Long,
    val isSynced: Boolean = false
)