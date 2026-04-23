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

    companion object {
        private const val TAG = "VULPINE_REPO"
    }

    fun observeNotes(userId: String): Flow<List<NoteDto>> =
        dao.getNotes(userId).map { entities ->
            entities.map { it.toDto() }
        }

    fun observeTrash(userId: String): Flow<List<NoteDto>> =
        dao.getTrash(userId).map { entities ->
            entities.map { it.toDto() }
        }

    suspend fun createNote(
        title: String,
        type: String,
        userId: String,
        isOnline: Boolean
    ) {
        val localId = UUID.randomUUID().toString()
        val now = System.currentTimeMillis()

        val localNote = NoteEntity(
            id = localId,
            userId = userId,
            title = title,
            type = type,
            createdAt = now,
            updatedAt = now,
            isDeleted = false,
            serverId = null
        )

        dao.insert(localNote)

        if (isOnline && !tokenManager.isGuest()) {
            syncCreateNoteToServer(localNote)
        }
    }

    private suspend fun syncCreateNoteToServer(localNote: NoteEntity) {
        try {
            val token = tokenManager.getToken() ?: return

            val serverNote = api.createNote(
                "Bearer $token",
                CreateNoteRequest(localNote.title, localNote.type)
            )

            val updatedNote = NoteEntity(
                id = serverNote.id,
                userId = localNote.userId,
                title = serverNote.title,
                type = serverNote.type,
                createdAt = serverNote.createdAt,
                updatedAt = serverNote.updatedAt,
                isDeleted = false,
                serverId = serverNote.id
            )

            dao.delete(localNote.id)
            dao.insert(updatedNote)

        } catch (e: Exception) {
            Log.e(TAG, "SYNC FAILED", e)
        }
    }

    /**
     * Копирует заметки пользователя гостю при выходе из аккаунта
     */
    suspend fun copyUserNotesToGuest(userUserId: String, guestUserId: String) {
        try {
            Log.d(TAG, "COPY NOTES from user=$userUserId to guest=$guestUserId")

            // Удаляем ВСЕ старые заметки гостя
            dao.clearAll(guestUserId)
            Log.d(TAG, "CLEARED old guest notes")

            // Получаем активные заметки пользователя
            val userNotes = dao.getAllActiveNotes(userUserId)
            Log.d(TAG, "USER NOTES count=${userNotes.size}")

            if (userNotes.isEmpty()) {
                Log.d(TAG, "NO NOTES TO COPY")
                return
            }

            // Копируем заметки гостю
            userNotes.forEach { note ->
                val guestNote = NoteEntity(
                    id = UUID.randomUUID().toString(),
                    userId = guestUserId,
                    title = note.title,
                    type = note.type,
                    createdAt = note.createdAt,
                    updatedAt = note.updatedAt,
                    isDeleted = false,
                    serverId = null
                )
                dao.insert(guestNote)
            }

            Log.d(TAG, "COPIED ${userNotes.size} notes to guest")

        } catch (e: Exception) {
            Log.e(TAG, "COPY NOTES FAILED", e)
        }
    }

    /**
     * Миграция заметок гостя в аккаунт пользователя при входе
     * Переносит только те заметки, которых еще нет у пользователя
     */
    suspend fun migrateGuestNotesToUser(guestUserId: String, userUserId: String) {
        try {
            Log.d(TAG, "MIGRATE guest=$guestUserId to user=$userUserId")

            // Получаем заметки гостя
            val guestNotes = dao.getAllActiveNotes(guestUserId)
            Log.d(TAG, "GUEST NOTES count=${guestNotes.size}")

            if (guestNotes.isEmpty()) {
                Log.d(TAG, "NO GUEST NOTES TO MIGRATE")
                return
            }

            // Получаем существующие заметки пользователя
            val userNotes = dao.getAllActiveNotes(userUserId)
            Log.d(TAG, "USER EXISTING NOTES count=${userNotes.size}")

            // Создаем множество заголовков существующих заметок пользователя
            val userNoteTitles = userNotes.map { it.title.lowercase().trim() }.toSet()

            var migratedCount = 0

            // Переносим только НОВЫЕ заметки (которых нет у пользователя)
            guestNotes.forEach { note ->
                val noteTitle = note.title.lowercase().trim()

                // Проверяем, есть ли уже такая заметка у пользователя
                if (noteTitle !in userNoteTitles) {
                    val userNote = NoteEntity(
                        id = UUID.randomUUID().toString(),
                        userId = userUserId,
                        title = note.title,
                        type = note.type,
                        createdAt = note.createdAt,
                        updatedAt = note.updatedAt,
                        isDeleted = false,
                        serverId = null
                    )
                    dao.insert(userNote)
                    migratedCount++
                    Log.d(TAG, "MIGRATED: ${note.title}")
                } else {
                    Log.d(TAG, "SKIPPED DUPLICATE: ${note.title}")
                }
            }

            // Удаляем ВСЕ заметки гостя после миграции
            dao.clearAll(guestUserId)

            Log.d(TAG, "MIGRATED $migratedCount notes, skipped ${guestNotes.size - migratedCount} duplicates")

            // Синхронизируем только если были новые заметки
            if (migratedCount > 0) {
                syncUnsyncedNotes(userUserId)
            }

        } catch (e: Exception) {
            Log.e(TAG, "MIGRATION FAILED", e)
        }
    }

    suspend fun syncUnsyncedNotes(userId: String) {
        if (tokenManager.isGuest()) return

        val token = tokenManager.getToken() ?: return

        try {
            val unsyncedNotes = dao.getUnsyncedNotes(userId)
            Log.d(TAG, "SYNC unsynced count=${unsyncedNotes.size}")

            unsyncedNotes.forEach { note ->
                try {
                    val serverNote = api.createNote(
                        "Bearer $token",
                        CreateNoteRequest(note.title, note.type)
                    )

                    val updatedNote = NoteEntity(
                        id = serverNote.id,
                        userId = userId,
                        title = serverNote.title,
                        type = serverNote.type,
                        createdAt = serverNote.createdAt,
                        updatedAt = serverNote.updatedAt,
                        isDeleted = note.isDeleted,
                        serverId = serverNote.id
                    )

                    dao.delete(note.id)
                    dao.insert(updatedNote)

                } catch (e: Exception) {
                    Log.e(TAG, "SYNC FAILED for note ${note.id}", e)
                }
            }

        } catch (e: Exception) {
            Log.e(TAG, "SYNC FAILED", e)
        }
    }

    suspend fun moveToTrash(noteId: String) {
        val note = dao.getActiveNoteById(noteId) ?: return

        val now = System.currentTimeMillis()
        dao.moveToTrash(note.id, now)

        if (!tokenManager.isGuest() && note.serverId != null) {
            try {
                val token = tokenManager.getToken() ?: return
                api.deleteNote(note.serverId, "Bearer $token")
            } catch (e: Exception) {
                Log.e(TAG, "API DELETE FAILED", e)
            }
        }
    }

    suspend fun restoreFromTrash(noteId: String) {
        val note = dao.getTrashNoteById(noteId) ?: return

        val now = System.currentTimeMillis()
        dao.restoreFromTrash(note.id, now)

        if (!tokenManager.isGuest()) {
            syncRestoreToServer(note, now)
        }
    }

    private suspend fun syncRestoreToServer(note: NoteEntity, restoreTime: Long) {
        try {
            val token = tokenManager.getToken() ?: return

            if (note.serverId != null) {
                try {
                    api.updateNote(
                        note.serverId,
                        "Bearer $token",
                        UpdateNoteRequest(isDeleted = false)
                    )
                    return
                } catch (e: Exception) {
                    Log.w(TAG, "UPDATE RESTORE FAILED", e)
                }
            }

            val serverNote = api.createNote(
                "Bearer $token",
                CreateNoteRequest(note.title, note.type)
            )

            val updatedNote = NoteEntity(
                id = serverNote.id,
                userId = note.userId,
                title = serverNote.title,
                type = serverNote.type,
                createdAt = serverNote.createdAt,
                updatedAt = restoreTime,
                isDeleted = false,
                serverId = serverNote.id
            )

            dao.delete(note.id)
            dao.insert(updatedNote)

        } catch (e: Exception) {
            Log.e(TAG, "RESTORE SYNC FAILED", e)
        }
    }

    suspend fun deletePermanently(noteId: String) {
        val note = dao.getNoteById(noteId) ?: return
        dao.delete(noteId)

        if (!tokenManager.isGuest() && note.serverId != null) {
            try {
                val token = tokenManager.getToken() ?: return
                api.deleteNote(note.serverId, "Bearer $token")
            } catch (e: Exception) {
                Log.e(TAG, "DELETE PERMANENTLY FAILED", e)
            }
        }
    }

    suspend fun fetchFromServer(userId: String) {
        if (tokenManager.isGuest()) return

        val token = tokenManager.getToken() ?: return

        try {
            val serverNotes = api.getNotes("Bearer $token")
            val localNotes = dao.getAllOnce(userId)

            // Удаляем локальные синхронизированные заметки, которых нет на сервере
            localNotes.forEach { local ->
                if (local.serverId != null) {
                    val existsOnServer = serverNotes.any { it.id == local.serverId }
                    if (!existsOnServer) {
                        dao.delete(local.id)
                    }
                }
            }

            // Добавляем новые заметки с сервера, которых нет локально
            serverNotes.forEach { server ->
                val existsLocally = localNotes.any {
                    it.serverId == server.id || it.id == server.id
                }
                if (!existsLocally) {
                    dao.insert(server.toEntity())
                }
            }

        } catch (e: Exception) {
            Log.e(TAG, "FETCH FAILED", e)
        }
    }
}