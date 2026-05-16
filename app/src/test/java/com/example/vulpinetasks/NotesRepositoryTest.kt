package com.example.vulpinetasks.unittests

import com.example.vulpinetasks.backend.ApiService
import com.example.vulpinetasks.backend.TokenManager
import com.example.vulpinetasks.room.NoteDao
import com.example.vulpinetasks.room.NotesRepository
import kotlinx.coroutines.runBlocking
import org.junit.Before
import org.junit.Test
import org.junit.runner.RunWith
import org.mockito.Mock
import org.mockito.Mockito.*
import org.mockito.junit.MockitoJUnitRunner

@RunWith(MockitoJUnitRunner::class)
class NotesRepositoryTest {

    @Mock
    private lateinit var mockContext: android.content.Context

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
    fun `repository should be created successfully`() {
        assert(repository != null)
    }
}