package com.example.vulpinetasks.unittests

import android.content.Context
import com.example.vulpinetasks.backend.ApiService
import com.example.vulpinetasks.backend.CreateNoteRequest
import com.example.vulpinetasks.backend.NoteDto
import com.example.vulpinetasks.backend.TokenManager
import com.example.vulpinetasks.room.NoteDao
import com.example.vulpinetasks.room.NoteEntity
import com.example.vulpinetasks.room.NotesRepository
import kotlinx.coroutines.runBlocking
import org.junit.Assert.*
import org.junit.Before
import org.junit.Test
import org.junit.runner.RunWith
import org.mockito.Mock
import org.mockito.Mockito.*
import org.mockito.junit.MockitoJUnitRunner

@RunWith(MockitoJUnitRunner::class)
class NotesRepositoryTest {

    @Mock
    private lateinit var mockContext: Context

    @Mock
    private lateinit var mockDao: NoteDao

    @Mock
    private lateinit var mockApi: ApiService

    @Mock
    private lateinit var mockTokenManager: TokenManager

    private lateinit var repository: NotesRepository

    @Before
    fun setUp() {
        repository = NotesRepository(mockContext, mockDao, mockApi, mockTokenManager)
    }

    @Test
    fun `createNote should insert local note when offline`() = runBlocking {
        val title = "Test Note"
        val type = "note"
        val userId = "user-123"
        val isOnline = false

        `when`(mockTokenManager.isGuest()).thenReturn(true)

        repository.createNote(title, type, userId, isOnline)

        verify(mockDao).insert(any(NoteEntity::class.java))
        verify(mockApi, never()).createNote(anyString(), any(CreateNoteRequest::class.java))
    }

    @Test
    fun `moveToTrash should update isDeleted flag`() = runBlocking {
        val noteId = "note-123"
        val mockNote = NoteEntity(
            id = noteId,
            userId = "user-123",
            title = "Test Note",
            type = "note",
            content = "Content",
            createdAt = System.currentTimeMillis(),
            updatedAt = System.currentTimeMillis(),
            isDeleted = false
        )

        `when`(mockDao.getActiveNoteById(noteId)).thenReturn(mockNote)
        `when`(mockTokenManager.isGuest()).thenReturn(true)

        repository.moveToTrash(noteId)

        verify(mockDao).moveToTrash(eq(noteId), anyLong())
    }

    @Test
    fun `restoreFromTrash should remove isDeleted flag`() = runBlocking {
        val noteId = "note-123"
        val mockNote = NoteEntity(
            id = noteId,
            userId = "user-123",
            title = "Test Note",
            type = "note",
            content = "Content",
            createdAt = System.currentTimeMillis(),
            updatedAt = System.currentTimeMillis(),
            isDeleted = true
        )

        `when`(mockDao.getTrashNoteById(noteId)).thenReturn(mockNote)
        `when`(mockTokenManager.isGuest()).thenReturn(true)

        repository.restoreFromTrash(noteId)

        verify(mockDao).restoreFromTrash(eq(noteId), anyLong())
    }

    @Test
    fun `getAllNotes should return all active notes`() = runBlocking {
        val userId = "user-123"
        val expectedNotes = listOf(
            NoteEntity("1", userId, "Note 1", "note", "", 100L, 100L),
            NoteEntity("2", userId, "Note 2", "task", "", 200L, 200L)
        )

        `when`(mockDao.getAllActiveNotes(userId)).thenReturn(expectedNotes)

        val result = repository.getAllNotes(userId)

        assertEquals(2, result.size)
        assertEquals("Note 1", result[0].title)
        assertEquals("task", result[1].type)
        verify(mockDao).getAllActiveNotes(userId)
    }

    @Test
    fun `updateNoteTitle should update title locally and sync to server`() = runBlocking {
        val noteId = "note-123"
        val newTitle = "Updated Title"
        val existingNote = NoteEntity(
            id = noteId,
            userId = "user-123",
            title = "Old Title",
            type = "note",
            content = "Content",
            createdAt = 100L,
            updatedAt = 100L
        )

        `when`(mockDao.getNoteById(noteId)).thenReturn(existingNote)
        `when`(mockTokenManager.isGuest()).thenReturn(false)
        `when`(mockTokenManager.getToken()).thenReturn("fake-token")
        `when`(mockApi.updateNote(eq(noteId), anyString(), any())).thenReturn(NoteDto(
            id = noteId,
            userId = "user-123",
            title = newTitle,
            type = "note",
            content = "Content",
            createdAt = 100L,
            updatedAt = System.currentTimeMillis()
        ))

        repository.updateNoteTitle(noteId, newTitle)

        verify(mockDao).insert(any(NoteEntity::class.java))
        verify(mockApi).updateNote(eq(noteId), anyString(), any())
    }
}