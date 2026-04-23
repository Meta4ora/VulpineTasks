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
            Log.d(TAG, "FLOW NOTES EMIT size=${entities.size}")
            entities.map { it.toDto() }
        }

    fun observeTrash(userId: String): Flow<List<NoteDto>> =
        dao.getTrash(userId).map { entities ->
            Log.d(TAG, "FLOW TRASH EMIT size=${entities.size}")
            entities.map { it.toDto() }
        }

    suspend fun createNote(
        title: String,
        type: String,
        userId: String,
        isOnline: Boolean
    ) {
        Log.d(TAG, "createNote() title=$title type=$type userId=$userId online=$isOnline")

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

        // Сохраняем локально
        dao.insert(localNote)
        Log.d(TAG, "LOCAL NOTE CREATED id=$localId")

        // Если онлайн - синхронизируем с сервером
        if (isOnline && !tokenManager.isGuest()) {
            syncCreateNoteToServer(localNote)
        }
    }

    private suspend fun syncCreateNoteToServer(localNote: NoteEntity) {
        try {
            val token = tokenManager.getToken() ?: return
            Log.d(TAG, "API CREATE NOTE request for local id=${localNote.id}")

            val serverNote = api.createNote(
                "Bearer $token",
                CreateNoteRequest(localNote.title, localNote.type)
            )

            Log.d(TAG, "API CREATE NOTE SUCCESS serverId=${serverNote.id}")

            // Обновляем локальную заметку - заменяем локальный ID на серверный
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

            // Удаляем старую запись с локальным ID
            dao.delete(localNote.id)
            // Вставляем новую с серверным ID
            dao.insert(updatedNote)

            Log.d(TAG, "LOCAL NOTE UPDATED WITH SERVER ID old=${localNote.id} new=${serverNote.id}")

        } catch (e: Exception) {
            Log.e(TAG, "API CREATE FAILED for local id=${localNote.id}", e)
        }
    }

    suspend fun moveToTrash(noteId: String) {
        Log.d(TAG, "moveToTrash() noteId=$noteId")

        val note = dao.getActiveNoteById(noteId)
        if (note == null) {
            Log.e(TAG, "NOTE NOT FOUND id=$noteId")
            return
        }

        Log.d(TAG, "MOVE TO TRASH note id=${note.id} title=${note.title}")

        val now = System.currentTimeMillis()

        // Локально помечаем как удаленную
        dao.moveToTrash(note.id, now)
        Log.d(TAG, "LOCAL MOVE TO TRASH OK id=${note.id}")

        // Синхронизируем с сервером
        if (!tokenManager.isGuest()) {
            syncMoveToTrashToServer(note)
        }
    }

    private suspend fun syncMoveToTrashToServer(note: NoteEntity) {
        try {
            val token = tokenManager.getToken() ?: return
            val serverId = note.serverId ?: note.id

            Log.d(TAG, "API MOVE TO TRASH serverId=$serverId")

            // Пробуем удалить с сервера
            api.deleteNote(serverId, "Bearer $token")
            Log.d(TAG, "API MOVE TO TRASH OK id=$serverId")

        } catch (e: Exception) {
            Log.e(TAG, "API MOVE TO TRASH FAILED", e)
        }
    }

    suspend fun restoreFromTrash(noteId: String) {
        Log.d(TAG, "restoreFromTrash() noteId=$noteId")

        val note = dao.getTrashNoteById(noteId)
        if (note == null) {
            Log.e(TAG, "TRASH NOTE NOT FOUND id=$noteId")
            return
        }

        Log.d(TAG, "RESTORE NOTE id=${note.id} title=${note.title}")

        val now = System.currentTimeMillis()

        // Локально восстанавливаем
        dao.restoreFromTrash(note.id, now)
        Log.d(TAG, "LOCAL RESTORE OK id=${note.id}")

        // Синхронизируем с сервером
        if (!tokenManager.isGuest()) {
            syncRestoreToServer(note, now)
        }
    }

    private suspend fun syncRestoreToServer(note: NoteEntity, restoreTime: Long) {
        try {
            val token = tokenManager.getToken() ?: return
            val serverId = note.serverId ?: note.id

            Log.d(TAG, "API RESTORE - recreating note id=$serverId")

            // Создаем заметку заново на сервере
            val newServerNote = api.createNote(
                "Bearer $token",
                CreateNoteRequest(note.title, note.type)
            )

            Log.d(TAG, "API RESTORE SUCCESS newId=${newServerNote.id}")

            // Обновляем локальную заметку с новым серверным ID
            val updatedNote = NoteEntity(
                id = newServerNote.id,
                userId = note.userId,
                title = newServerNote.title,
                type = newServerNote.type,
                createdAt = newServerNote.createdAt,
                updatedAt = restoreTime,
                isDeleted = false,
                serverId = newServerNote.id
            )

            // Удаляем старую запись
            dao.delete(note.id)
            // Вставляем новую
            dao.insert(updatedNote)

            Log.d(TAG, "LOCAL NOTE UPDATED AFTER RESTORE oldId=${note.id} newId=${newServerNote.id}")

        } catch (e: Exception) {
            Log.e(TAG, "API RESTORE FAILED", e)
        }
    }

    suspend fun deletePermanently(noteId: String) {
        Log.d(TAG, "deletePermanently() noteId=$noteId")

        val note = dao.getNoteById(noteId)
        if (note == null) {
            Log.e(TAG, "NOTE NOT FOUND FOR DELETE id=$noteId")
            return
        }

        // Удаляем локально
        dao.delete(noteId)
        Log.d(TAG, "LOCAL DELETE OK id=$noteId")

        // Удаляем с сервера
        if (!tokenManager.isGuest()) {
            syncDeleteToServer(note)
        }
    }

    private suspend fun syncDeleteToServer(note: NoteEntity) {
        try {
            val token = tokenManager.getToken() ?: return
            val serverId = note.serverId ?: note.id

            Log.d(TAG, "API DELETE PERMANENTLY serverId=$serverId")
            api.deleteNote(serverId, "Bearer $token")
            Log.d(TAG, "API DELETE PERMANENTLY OK id=$serverId")
        } catch (e: Exception) {
            Log.e(TAG, "API DELETE PERMANENTLY FAILED", e)
        }
    }

    suspend fun fetchFromServer(userId: String) {
        if (tokenManager.isGuest()) {
            Log.d(TAG, "FETCH SKIPPED - guest mode")
            return
        }

        val token = tokenManager.getToken() ?: run {
            Log.d(TAG, "FETCH SKIPPED - no token")
            return
        }

        try {
            Log.d(TAG, "FETCH FROM SERVER userId=$userId")

            // Получаем только активные заметки
            val serverNotes = api.getNotes("Bearer $token")
            Log.d(TAG, "SERVER NOTES size=${serverNotes.size}")

            // Получаем локальные заметки (все, включая удаленные)
            val localNotes = dao.getAllOnce(userId)
            Log.d(TAG, "LOCAL NOTES size=${localNotes.size}")

            // Синхронизируем только активные заметки
            val serverActiveIds = serverNotes.map { it.id }.toSet()

            // Удаляем локальные активные заметки, которых нет на сервере
            localNotes.forEach { local ->
                if (!local.isDeleted && local.id !in serverActiveIds) {
                    Log.d(TAG, "REMOVING LOCAL NOTE not on server id=${local.id}")
                    dao.delete(local.id)
                }
            }

            // Добавляем/обновляем заметки с сервера
            serverNotes.forEach { server ->
                val existingLocal = localNotes.find { it.id == server.id }

                if (existingLocal == null || !existingLocal.isDeleted) {
                    val entity = server.toEntity()
                    Log.d(TAG, "UPSERTING SERVER NOTE id=${entity.id}")
                    dao.insert(entity)
                } else {
                    Log.d(TAG, "SKIPPING SERVER NOTE (locally deleted) id=${server.id}")
                }
            }

            Log.d(TAG, "FETCH COMPLETED SUCCESSFULLY")

        } catch (e: Exception) {
            Log.e(TAG, "FETCH FAILED: ${e.message}", e)
        }
    }

    suspend fun syncAll(userId: String) {
        if (tokenManager.isGuest()) {
            Log.d(TAG, "SYNC SKIPPED - guest mode")
            return
        }

        // Загружаем свежие данные с сервера
        fetchFromServer(userId)
    }
}