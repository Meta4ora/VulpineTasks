package com.example.vulpinetasks.mappers

import android.util.Log
import com.example.vulpinetasks.backend.NoteDto
import com.example.vulpinetasks.room.NoteEntity

private const val TAG = "VULPINE_MAPPER"

fun NoteEntity.toDto(): NoteDto {
    Log.d(TAG, "ENTITY -> DTO | id=$id userId=$userId title=$title")
    return NoteDto(
        id = id,
        userId = userId,
        title = title,
        type = type,
        content = content,
        parentId = null,
        filePath = filePath,
        createdAt = createdAt,
        updatedAt = updatedAt,
        isDeleted = isDeleted
    )
}

fun NoteDto.toEntity(): NoteEntity {
    Log.d(TAG, "DTO -> ENTITY | id=$id title=$title")
    return NoteEntity(
        id = id,
        userId = userId,
        title = title,
        type = type,
        content = content ?: "",
        createdAt = createdAt,
        updatedAt = updatedAt,
        isDeleted = isDeleted,
        serverId = id,
        filePath = filePath
    )
}