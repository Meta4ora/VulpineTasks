package com.example.vulpinetasks.unittests

import com.example.vulpinetasks.backend.NoteDto
import com.example.vulpinetasks.mappers.toDto
import com.example.vulpinetasks.mappers.toEntity
import com.example.vulpinetasks.room.NoteEntity
import org.junit.Assert.*
import org.junit.Test

class NoteMappersTest {

    @Test
    fun `toDto should convert NoteEntity to NoteDto correctly`() {
        val entity = NoteEntity(
            id = "note-123",
            userId = "user-456",
            title = "Test Note",
            type = "note",
            content = "Test content",
            createdAt = 123456789L,
            updatedAt = 123456790L,
            isDeleted = false,
            serverId = "server-789",
            filePath = "/storage/note-123.md"
        )

        val dto = entity.toDto()

        assertEquals(entity.id, dto.id)
        assertEquals(entity.userId, dto.userId)
        assertEquals(entity.title, dto.title)
        assertEquals(entity.type, dto.type)
        assertEquals(entity.createdAt, dto.createdAt)
        assertEquals(entity.updatedAt, dto.updatedAt)
        assertEquals(entity.isDeleted, dto.isDeleted)
        assertEquals(entity.filePath, dto.filePath)
    }

    @Test
    fun `toEntity should convert NoteDto to NoteEntity correctly`() {
        val dto = NoteDto(
            id = "note-123",
            userId = "user-456",
            title = "Test Note",
            type = "note",
            content = "Test content",
            createdAt = 123456789L,
            updatedAt = 123456790L,
            isDeleted = false,
            filePath = "/storage/note-123.md"
        )

        val entity = dto.toEntity()

        assertEquals(dto.id, entity.id)
        assertEquals(dto.userId, entity.userId)
        assertEquals(dto.title, entity.title)
        assertEquals(dto.type, entity.type)
        assertEquals(dto.content, entity.content)
        assertEquals(dto.createdAt, entity.createdAt)
        assertEquals(dto.updatedAt, entity.updatedAt)
        assertEquals(dto.isDeleted, entity.isDeleted)
        assertEquals(dto.id, entity.serverId)
        assertEquals(dto.filePath, entity.filePath)
    }

    @Test
    fun `toDto should handle empty content correctly`() {
        val entity = NoteEntity(
            id = "note-123",
            userId = "user-456",
            title = "Empty Note",
            type = "note",
            content = "",
            createdAt = 123456789L,
            updatedAt = 123456790L,
            isDeleted = false
        )

        val dto = entity.toDto()

        assertEquals("", dto.content)
    }

    @Test
    fun `toEntity should handle deleted notes correctly`() {
        val dto = NoteDto(
            id = "note-123",
            userId = "user-456",
            title = "Deleted Note",
            type = "note",
            content = "This note is deleted",
            createdAt = 123456789L,
            updatedAt = 123456790L,
            isDeleted = true
        )

        val entity = dto.toEntity()

        assertTrue(entity.isDeleted)
    }
}