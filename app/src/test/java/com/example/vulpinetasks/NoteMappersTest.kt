package com.example.vulpinetasks.unittests

import com.example.vulpinetasks.backend.NoteDto
import com.example.vulpinetasks.mappers.toDto
import com.example.vulpinetasks.mappers.toEntity
import com.example.vulpinetasks.room.NoteEntity
import org.junit.Assert.*
import org.junit.Test

class NoteMappersTest {

    @Test
    fun `toDto should map entity to dto correctly`() {
        val entity = NoteEntity(
            id = "1",
            userId = "user1",
            title = "Test Note",
            type = "note",
            content = "Content",
            createdAt = 100L,
            updatedAt = 200L,
            isDeleted = false,
            serverId = "server1",
            filePath = "/path"
        )

        val dto = entity.toDto()

        assertEquals(entity.id, dto.id)
        assertEquals(entity.userId, dto.userId)
        assertEquals(entity.title, dto.title)
        assertEquals(entity.type, dto.type)
        assertEquals(entity.content, dto.content)
        assertEquals(entity.createdAt, dto.createdAt)
        assertEquals(entity.updatedAt, dto.updatedAt)
        assertEquals(entity.isDeleted, dto.isDeleted)
        assertEquals(entity.filePath, dto.filePath)
    }

    @Test
    fun `toEntity should map dto to entity correctly`() {
        val dto = NoteDto(
            id = "1",
            userId = "user1",
            title = "Test Note",
            type = "note",
            content = "Content",
            createdAt = 100L,
            updatedAt = 200L,
            isDeleted = false,
            filePath = "/path"
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
    fun `deleted note should preserve deletion flag`() {
        val dto = NoteDto(
            id = "1",
            userId = "user1",
            title = "Deleted",
            type = "note",
            content = "",
            createdAt = 100L,
            updatedAt = 100L,
            isDeleted = true
        )

        val entity = dto.toEntity()
        assertTrue(entity.isDeleted)

        val backToDto = entity.toDto()
        assertTrue(backToDto.isDeleted)
    }
}