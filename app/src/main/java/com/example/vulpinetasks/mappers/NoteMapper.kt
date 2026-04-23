package com.example.vulpinetasks.mappers

import com.example.vulpinetasks.backend.NoteDto
import com.example.vulpinetasks.room.NoteEntity

fun NoteDto.toEntity(): NoteEntity {
    return NoteEntity(
        id = id,
        userId = userId,
        title = title,
        type = type,
        updatedAt = updatedAt,
        isSynced = true,
        isDeleted = false
    )
}