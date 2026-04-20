package com.example.vulpinetasks.room

import com.example.vulpinetasks.backend.*
import kotlinx.coroutines.flow.Flow
import java.util.UUID

class NotesRepository(
    private val dao: NoteDao,
    private val api: ApiService,
    private val tokenManager: TokenManager
) {

    fun getNotes(userId: String): Flow<List<NoteEntity>> {
        return dao.getNotes(userId)
    }

    suspend fun createNote(title: String, type: String, userId: String) {

        val note = NoteEntity(
            id = UUID.randomUUID().toString(),
            userId = userId,
            title = title,
            type = type,
            updatedAt = System.currentTimeMillis(),
            isSynced = false
        )

        dao.insert(note)

        val token = tokenManager.getToken() ?: return

        try {
            api.createNote(
                token = "Bearer $token",
                body = CreateNoteRequest(
                    title = title,
                    type = type,
                    parentId = null
                )
            )
        } catch (e: Exception) {
            e.printStackTrace()
        }
    }

    suspend fun syncAll(userId: String) {

        val token = tokenManager.getToken() ?: return
        val unsynced = dao.getUnsynced(userId)

        unsynced.forEach { note ->
            try {
                api.createNote(
                    token = "Bearer $token",
                    body = CreateNoteRequest(
                        title = note.title,
                        type = note.type,
                        parentId = null
                    )
                )
            } catch (e: Exception) {
                e.printStackTrace()
            }
        }
    }

    suspend fun fetchNotesFromServer(userId: String) {

        val token = tokenManager.getToken() ?: return

        try {
            val notes = api.getNotes(
                token = "Bearer $token",
                parentId = null
            )

            val entities = notes.map {
                NoteEntity(
                    id = it.id,
                    userId = it.userId,
                    title = it.title,
                    type = it.type,
                    updatedAt = it.updatedAt,
                    isSynced = true
                )
            }

            dao.insertAll(entities)

        } catch (e: Exception) {
            e.printStackTrace()
        }
    }
}