package com.example.vulpinetasks.backend

data class NoteDto(
    val id: String,
    val userId: String,
    val title: String,
    val type: String,
    val parentId: String? = null,
    val filePath: String? = null,
    val createdAt: Long,
    val updatedAt: Long,
    val isDeleted: Boolean = false
)

data class CreateNoteRequest(
    val title: String,
    val type: String
)

data class UpdateNoteRequest(
    val title: String? = null,
    val type: String? = null,
    val isDeleted: Boolean? = null
)