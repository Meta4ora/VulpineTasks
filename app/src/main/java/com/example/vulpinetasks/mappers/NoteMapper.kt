package com.example.vulpinetasks.mappers

import com.example.vulpinetasks.backend.NoteDto
import com.example.vulpinetasks.room.NoteEntity

fun NoteEntity.toDto(): NoteDto {
    return NoteDto(
        id = this.id,
        userId = this.userId,
        title = this.title,
        type = this.type,
        content = this.content,
        parentIds = emptyList(),  // Связи теперь в отдельной таблице
        filePath = this.filePath,
        createdAt = this.createdAt,
        updatedAt = this.updatedAt,
        isDeleted = this.isDeleted
    )
}

fun NoteDto.toEntity(): NoteEntity {
    return NoteEntity(
        id = this.id,
        userId = this.userId,
        title = this.title,
        type = this.type,
        content = this.content,
        createdAt = this.createdAt,
        updatedAt = this.updatedAt,
        isDeleted = this.isDeleted,
        serverId = this.id,
        filePath = this.filePath
    )
}