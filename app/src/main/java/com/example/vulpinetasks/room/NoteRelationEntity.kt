package com.example.vulpinetasks.room

import androidx.room.Entity
import androidx.room.PrimaryKey

@Entity(tableName = "note_relations")
data class NoteRelationEntity(
    @PrimaryKey(autoGenerate = true)
    val relationId: Int = 0,
    val noteId: String,        // ID дочерней заметки
    val parentNoteId: String   // ID родительской заметки
)