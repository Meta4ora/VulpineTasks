package com.example.vulpinetasks.mappers

import com.example.vulpinetasks.backend.NoteDto

fun com.example.vulpinetasks.room.NoteEntity.toDto() =
    NoteDto(
        id = id,
        userId = userId,
        title = title,
        type = type,
        parentId = null,
        filePath = "",
        createdAt = updatedAt,
        updatedAt = updatedAt
    )