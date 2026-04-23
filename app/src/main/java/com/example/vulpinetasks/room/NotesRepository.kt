package com.example.vulpinetasks.room

import android.util.Log
import com.example.vulpinetasks.backend.*
import com.example.vulpinetasks.mappers.toDto
import com.example.vulpinetasks.mappers.toEntity
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.map
import java.util.UUID

class NotesRepository(
    private val dao: NoteDao,
    private val api: ApiService,
    private val tokenManager: TokenManager
) {

    fun observeNotes(userId: String): Flow<List<NoteDto>> =
        dao.getNotes(userId).map { it.map { e -> e.toDto() } }

    fun observeTrash(userId: String): Flow<List<NoteDto>> =
        dao.getTrash(userId).map { it.map { e -> e.toDto() } }

    suspend fun createNote(
        title: String,
        type: String,
        userId: String,
        isOnline: Boolean
    ) {
        if (!isOnline || tokenManager.isGuest()) {
            val local = NoteEntity(
                id = UUID.randomUUID().toString(),
                userId = userId,
                title = title,
                type = type,
                updatedAt = System.currentTimeMillis()
            )
            dao.insert(local)
            return
        }

        val token = tokenManager.getToken() ?: return

        try {
            api.createNote(
                "Bearer $token",
                CreateNoteRequest(title, type)
            )
            fetchFromServer(userId)
        } catch (e: Exception) {
            Log.e("Repo", "create failed", e)
        }
    }

    suspend fun fetchFromServer(userId: String) {
        if (tokenManager.isGuest()) return

        val token = tokenManager.getToken() ?: return

        try {
            val serverNotes = api.getNotes("Bearer $token")

            val serverIds = serverNotes.map { it.id }.toSet()

            val local = dao.getAllOnce(userId)

            // 1. обновляем/добавляем серверные
            val entities = serverNotes.map { it.toEntity() }
            dao.insertAll(entities)

            // 2. НЕ УДАЛЯЕМ локальные изменения без серверного аналога
            local.forEach { localNote ->
                if (!serverIds.contains(localNote.id) && localNote.isDeleted) {
                    // оставляем в trash, не удаляем
                    return@forEach
                }
            }

        } catch (e: Exception) {
            Log.e("Repo", "fetch failed", e)
        }
    }

    suspend fun moveToTrash(note: NoteDto) {
        dao.moveToTrash(note.id)

        val token = tokenManager.getToken() ?: return

        try {
            api.deleteNote(note.id, "Bearer $token")
        } catch (e: Exception) {
            Log.e("Repo", "delete failed", e)
        }
    }

    suspend fun restore(note: NoteDto) {
        dao.restore(note.id, System.currentTimeMillis())

        val token = tokenManager.getToken() ?: return

        try {
            api.createNote(
                "Bearer $token",
                CreateNoteRequest(note.title, note.type)
            )
        } catch (e: Exception) {
            Log.e("Repo", "restore sync failed", e)
        }
    }

    suspend fun delete(note: NoteDto) {
        dao.delete(note.id)

        val token = tokenManager.getToken() ?: return

        try {
            api.deleteNote(note.id, "Bearer $token")
        } catch (e: Exception) {
            Log.e("Repo", "delete failed", e)
        }
    }
}