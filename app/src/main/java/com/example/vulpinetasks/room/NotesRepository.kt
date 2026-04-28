package com.example.vulpinetasks.room

import android.content.Context
import android.util.Log
import com.example.vulpinetasks.backend.*
import com.example.vulpinetasks.mappers.toDto
import com.example.vulpinetasks.mappers.toEntity
import com.example.vulpinetasks.util.FileStorageManager
import com.example.vulpinetasks.util.NetworkUtil
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.map
import java.util.UUID

class NotesRepository(
    private val context: Context,
    private val dao: NoteDao,
    private val api: ApiService,
    private val tokenManager: TokenManager
) {

    companion object {
        private const val TAG = "VULPINE_REPO"
    }

    private lateinit var fileStorage: FileStorageManager

    fun initFileStorage() {
        fileStorage = FileStorageManager(context)
    }

    fun observeNotes(userId: String): Flow<List<NoteDto>> =
        dao.getNotes(userId).map { entities ->
            entities.map { it.toDto() }
        }

    fun observeTrash(userId: String): Flow<List<NoteDto>> =
        dao.getTrash(userId).map { entities ->
            entities.map { it.toDto() }
        }

    suspend fun getNoteContent(noteId: String, userId: String): String {
        val localContent = fileStorage.loadNoteContent(userId, noteId)
        if (localContent != null) {
            Log.d(TAG, "Loaded content from LOCAL file for note $noteId")
            return localContent
        }

        if (!tokenManager.isGuest() && NetworkUtil.isOnline(context)) {
            try {
                val token = tokenManager.getToken()
                if (token != null) {
                    val content = api.getNoteContent(noteId, "Bearer $token")
                    Log.d(TAG, "Loaded content from SERVER for note $noteId")

                    fileStorage.saveNoteContent(userId, noteId, content)

                    val note = dao.getNoteById(noteId)
                    if (note != null && note.content != content) {
                        dao.insert(note.copy(content = content))
                    }

                    return content
                }
            } catch (e: Exception) {
                Log.e(TAG, "Failed to load from server", e)
            }
        }

        val note = dao.getNoteById(noteId)
        if (note != null && note.content.isNotEmpty()) {
            Log.d(TAG, "Loaded content from DATABASE for note $noteId")
            return note.content
        }

        return generateDefaultContent(note?.title ?: "Заметка")
    }

    suspend fun updateNoteContent(
        noteId: String,
        userId: String,
        newContent: String
    ) {
        fileStorage.saveNoteContent(userId, noteId, newContent)

        val note = dao.getNoteById(noteId)
        if (note != null) {
            val updatedNote = note.copy(
                content = newContent,
                updatedAt = System.currentTimeMillis()
            )
            dao.insert(updatedNote)
        }

        if (!tokenManager.isGuest() && NetworkUtil.isOnline(context)) {
            try {
                val token = tokenManager.getToken()
                if (token != null) {
                    api.updateNoteContent(noteId, "Bearer $token", newContent)
                    Log.d(TAG, "Content synced to server for note $noteId")
                }
            } catch (e: Exception) {
                Log.e(TAG, "Failed to sync content to server", e)
                markNoteAsDirty(noteId)
            }
        } else {
            markNoteAsDirty(noteId)
        }
    }

    private suspend fun markNoteAsDirty(noteId: String) {
        Log.d(TAG, "Note $noteId marked as dirty (needs sync)")
    }

    private fun generateDefaultContent(title: String): String {
        return "# $title\n\nНапишите здесь содержание заметки..."
    }

    suspend fun createNote(
        title: String,
        type: String,
        userId: String,
        isOnline: Boolean
    ) {
        val localId = UUID.randomUUID().toString()
        val now = System.currentTimeMillis()
        val defaultContent = generateDefaultContent(title)

        val localNote = NoteEntity(
            id = localId,
            userId = userId,
            title = title,
            type = type,
            content = defaultContent,
            createdAt = now,
            updatedAt = now,
            isDeleted = false,
            serverId = null,
            filePath = null
        )

        dao.insert(localNote)
        fileStorage.saveNoteContent(userId, localId, defaultContent)

        if (isOnline && !tokenManager.isGuest()) {
            syncCreateNoteToServer(localNote)
        }
    }

    private suspend fun syncCreateNoteToServer(localNote: NoteEntity) {
        try {
            val token = tokenManager.getToken() ?: return

            val serverNote = api.createNote(
                "Bearer $token",
                CreateNoteRequest(localNote.title, localNote.type, emptyList())
            )

            val updatedNote = localNote.copy(
                id = serverNote.id,
                serverId = serverNote.id,
                filePath = serverNote.filePath
            )

            dao.delete(localNote.id)
            dao.insert(updatedNote)

            if (localNote.content.isNotEmpty()) {
                api.updateNoteContent(serverNote.id, "Bearer $token", localNote.content)
            }

            fileStorage.deleteNoteFile(localNote.userId, localNote.id)
            fileStorage.saveNoteContent(localNote.userId, serverNote.id, localNote.content)

        } catch (e: Exception) {
            Log.e(TAG, "SYNC CREATE FAILED", e)
        }
    }

    suspend fun copyUserNotesToGuest(userUserId: String, guestUserId: String) {
        try {
            Log.d(TAG, "COPY NOTES from user=$userUserId to guest=$guestUserId")

            dao.clearAll(guestUserId)
            Log.d(TAG, "CLEARED old guest notes")

            val userNotes = dao.getAllActiveNotes(userUserId)
            Log.d(TAG, "USER NOTES count=${userNotes.size}")

            if (userNotes.isEmpty()) {
                Log.d(TAG, "NO NOTES TO COPY")
                return
            }

            userNotes.forEach { note ->
                val newGuestId = UUID.randomUUID().toString()

                val guestNote = NoteEntity(
                    id = newGuestId,
                    userId = guestUserId,
                    title = note.title,
                    type = note.type,
                    content = note.content,
                    createdAt = note.createdAt,
                    updatedAt = note.updatedAt,
                    isDeleted = false,
                    serverId = null,
                    filePath = null
                )
                dao.insert(guestNote)

                val content = fileStorage.loadNoteContent(userUserId, note.id)
                if (content != null && content.isNotEmpty()) {
                    fileStorage.saveNoteContent(guestUserId, newGuestId, content)
                } else if (note.content.isNotEmpty()) {
                    fileStorage.saveNoteContent(guestUserId, newGuestId, note.content)
                }
            }

            Log.d(TAG, "COPIED ${userNotes.size} notes to guest")

        } catch (e: Exception) {
            Log.e(TAG, "COPY NOTES FAILED", e)
        }
    }

    suspend fun migrateGuestNotesToUser(guestUserId: String, userUserId: String) {
        try {
            Log.d(TAG, "MIGRATE guest=$guestUserId to user=$userUserId")

            val guestNotes = dao.getAllActiveNotes(guestUserId)
            Log.d(TAG, "GUEST NOTES count=${guestNotes.size}")

            if (guestNotes.isEmpty()) {
                Log.d(TAG, "NO GUEST NOTES TO MIGRATE")
                return
            }

            val userNotes = dao.getAllActiveNotes(userUserId)

            val userNotesByTitle = userNotes.associateBy {
                it.title.lowercase().trim()
            }

            var mergedCount = 0
            var skippedCount = 0

            guestNotes.forEach { guestNote ->
                val titleKey = guestNote.title.lowercase().trim()
                val existingNote = userNotesByTitle[titleKey]

                if (existingNote == null) {
                    val newNote = NoteEntity(
                        id = UUID.randomUUID().toString(),
                        userId = userUserId,
                        title = guestNote.title,
                        type = guestNote.type,
                        content = guestNote.content,
                        createdAt = guestNote.createdAt,
                        updatedAt = guestNote.updatedAt,
                        isDeleted = false,
                        serverId = null,
                        filePath = null
                    )
                    dao.insert(newNote)

                    val content = fileStorage.loadNoteContent(guestUserId, guestNote.id)
                    if (content != null && content.isNotEmpty()) {
                        fileStorage.saveNoteContent(userUserId, newNote.id, content)
                    }

                    mergedCount++
                    Log.d(TAG, "MERGED NEW: ${guestNote.title}")
                } else {
                    if (guestNote.updatedAt > existingNote.updatedAt) {
                        val updatedNote = existingNote.copy(
                            title = guestNote.title,
                            content = guestNote.content,
                            updatedAt = guestNote.updatedAt
                        )
                        dao.insert(updatedNote)

                        fileStorage.saveNoteContent(userUserId, existingNote.id, guestNote.content)

                        mergedCount++
                        Log.d(TAG, "MERGED UPDATED: ${guestNote.title} (guest newer)")
                    } else {
                        skippedCount++
                        Log.d(TAG, "SKIPPED: ${guestNote.title} (already exists, local older)")
                    }
                }
            }

            dao.clearAll(guestUserId)
            guestNotes.forEach { guestNote ->
                fileStorage.deleteNoteFile(guestUserId, guestNote.id)
            }

            Log.d(TAG, "MIGRATION COMPLETE: merged=$mergedCount, skipped=$skippedCount")

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
                    if (note.serverId == null) {
                        val serverNote = api.createNote(
                            "Bearer $token",
                            CreateNoteRequest(note.title, note.type, emptyList())
                        )

                        if (note.content.isNotEmpty()) {
                            api.updateNoteContent(serverNote.id, "Bearer $token", note.content)
                        }

                        val updatedNote = note.copy(
                            id = serverNote.id,
                            serverId = serverNote.id,
                            filePath = serverNote.filePath
                        )

                        dao.delete(note.id)
                        dao.insert(updatedNote)

                        fileStorage.saveNoteContent(userId, updatedNote.id, note.content)
                        if (note.id != updatedNote.id) {
                            fileStorage.deleteNoteFile(userId, note.id)
                        }
                        Log.d(TAG, "SYNCED new note ${note.id} -> ${serverNote.id}")

                    } else {
                        if (note.content.isNotEmpty()) {
                            api.updateNoteContent(note.serverId, "Bearer $token", note.content)
                        }

                        val parentIds = dao.getParentIdsForNote(note.id)

                        api.updateNote(
                            note.serverId,
                            "Bearer $token",
                            UpdateNoteRequest(title = note.title, content = note.content, parentIds = parentIds)
                        )

                        val updatedNote = note.copy(updatedAt = System.currentTimeMillis())
                        dao.insert(updatedNote)
                        Log.d(TAG, "SYNCED existing note ${note.id}")
                    }

                } catch (e: Exception) {
                    Log.e(TAG, "SYNC FAILED for note ${note.id}", e)
                }
            }

        } catch (e: Exception) {
            Log.e(TAG, "SYNC FAILED", e)
        }
    }

    // ==================== КОРЗИНА ====================

    // Временное хранилище для связей удаленных заметок (на случай восстановления)
    private val deletedRelationsStore = mutableMapOf<String, Triple<List<String>, List<String>, String?>>()

    private suspend fun saveRelationsForNote(noteId: String, parentIds: List<String>, childIds: List<String>, serverId: String?) {
        deletedRelationsStore[noteId] = Triple(parentIds, childIds, serverId)
    }

    private suspend fun clearSavedRelationsForNote(noteId: String) {
        deletedRelationsStore.remove(noteId)
    }

    suspend fun clearDeletedRelations() {
        deletedRelationsStore.clear()
    }

    /**
     * Перемещение заметки в корзину
     * НЕ удаляем связи, только помечаем заметку как isDeleted = true
     */
    suspend fun moveToTrash(noteId: String) {
        val note = dao.getActiveNoteById(noteId) ?: return

        // Сохраняем связи на случай восстановления (но НЕ удаляем их!)
        val parentIds = dao.getParentIdsForNote(noteId)
        val childIds = dao.getChildrenIdsForNote(noteId)
        saveRelationsForNote(noteId, parentIds, childIds, note.serverId)

        val now = System.currentTimeMillis()
        dao.moveToTrash(note.id, now)

        // ВАЖНО: НЕ удаляем связи! Они остаются в таблице note_relations

        if (!tokenManager.isGuest() && note.serverId != null) {
            try {
                val token = tokenManager.getToken() ?: return
                // На сервере тоже просто помечаем как удаленную
                api.updateNote(
                    note.serverId,
                    "Bearer $token",
                    UpdateNoteRequest(isDeleted = true)
                )
                Log.d(TAG, "Note moved to trash on server: ${note.title}")
            } catch (e: Exception) {
                Log.e(TAG, "Failed to move note to trash on server", e)
            }
        }
    }

    /**
     * Восстановление заметки из корзины
     */
    suspend fun restoreFromTrash(noteId: String) {
        val note = dao.getTrashNoteById(noteId) ?: return

        val now = System.currentTimeMillis()

        // Просто снимаем флаг isDeleted (связи уже существуют)
        dao.restoreFromTrash(note.id, now)

        // Очищаем временное хранилище
        clearSavedRelationsForNote(noteId)

        if (!tokenManager.isGuest()) {
            syncRestoreToServer(note, now)
        }
    }

    private suspend fun syncRestoreToServer(note: NoteEntity, restoreTime: Long) {
        try {
            val token = tokenManager.getToken() ?: return

            if (note.serverId != null) {
                // Получаем актуальные parentIds (они не удалялись)
                val parentIds = dao.getParentIdsForNote(note.id)

                // Просто снимаем флаг isDeleted
                api.updateNote(
                    note.serverId,
                    "Bearer $token",
                    UpdateNoteRequest(isDeleted = false, parentIds = parentIds)
                )
                Log.d(TAG, "Note restored on server: ${note.title} with parentIds: $parentIds")

                // Обновляем локальную заметку
                val updatedNote = note.copy(
                    updatedAt = restoreTime,
                    isDeleted = false
                )
                dao.insert(updatedNote)
            }
        } catch (e: Exception) {
            Log.e(TAG, "RESTORE SYNC FAILED", e)
        }
    }

    suspend fun syncLocalChangesToServer(userId: String) {
        if (tokenManager.isGuest()) return

        val token = tokenManager.getToken() ?: return
        val localNotes = dao.getAllOnce(userId)

        localNotes.forEach { localNote ->
            if (localNote.serverId != null) {
                val localContent = fileStorage.loadNoteContent(userId, localNote.id)
                if (localContent != null && localContent != localNote.content) {
                    try {
                        api.updateNoteContent(localNote.serverId, "Bearer $token", localContent)
                        val updatedNote = localNote.copy(
                            content = localContent,
                            updatedAt = System.currentTimeMillis()
                        )
                        dao.insert(updatedNote)
                        Log.d(TAG, "SYNCED local changes to server: ${localNote.title}")
                    } catch (e: Exception) {
                        Log.e(TAG, "Failed to sync local changes", e)
                    }
                }
            }
        }
    }

    /**
     * ПОЛНОЕ удаление заметки (без возможности восстановления)
     * ТОЛЬКО ЗДЕСЬ удаляем все связи
     */
    suspend fun deletePermanently(noteId: String) {
        val note = dao.getNoteById(noteId) ?: return

        // НАХОДИМ ВСЕ ЗАМЕТКИ, КОТОРЫЕ ССЫЛАЮТСЯ НА ЭТУ
        val referencingNotes = dao.getNotesReferencingParentId(noteId)

        referencingNotes.forEach { refNote ->
            val parentIds = dao.getParentIdsForNote(refNote.id)
            val updatedParentIds = parentIds.filter { it != noteId }

            if (updatedParentIds.size != parentIds.size) {
                // Удаляем старые связи и добавляем обновленные
                dao.removeAllRelationsForNote(refNote.id)
                updatedParentIds.forEach { parentId ->
                    val relation = NoteRelationEntity(
                        noteId = refNote.id,
                        parentNoteId = parentId
                    )
                    dao.addRelation(relation)
                }

                // Синхронизируем с сервером
                if (!tokenManager.isGuest() && refNote.serverId != null) {
                    try {
                        val token = tokenManager.getToken() ?: return
                        api.updateNote(
                            refNote.serverId,
                            "Bearer $token",
                            UpdateNoteRequest(parentIds = updatedParentIds)
                        )
                        Log.d(TAG, "Removed reference to deleted note ${noteId} from ${refNote.id}")
                    } catch (e: Exception) {
                        Log.e(TAG, "Failed to sync reference removal", e)
                    }
                }
            }
        }

        // Удаляем все связи самой заметки
        dao.removeAllRelationsForNote(noteId)

        // Удаляем файл содержимого
        fileStorage.deleteNoteFile(note.userId, note.id)

        // Удаляем заметку
        dao.delete(noteId)

        // Очищаем временное хранилище
        clearSavedRelationsForNote(noteId)

        // Удаляем с сервера
        if (!tokenManager.isGuest() && note.serverId != null) {
            try {
                val token = tokenManager.getToken() ?: return
                api.deleteNote(note.serverId, "Bearer $token")
                Log.d(TAG, "Note permanently deleted from server: ${note.title}")
            } catch (e: Exception) {
                Log.e(TAG, "DELETE PERMANENTLY FAILED", e)
            }
        }
    }

    private suspend fun downloadNoteContent(
        userId: String,
        noteId: String,
        token: String
    ) {
        try {
            val content = api.getNoteContent(noteId, "Bearer $token")
            fileStorage.saveNoteContent(userId, noteId, content)

            val note = dao.getNoteById(noteId)
            if (note != null && note.content != content) {
                dao.insert(note.copy(content = content))
            }
        } catch (e: Exception) {
            Log.e(TAG, "Failed to download content for $noteId", e)
        }
    }

    suspend fun fetchFromServer(userId: String) {
        if (tokenManager.isGuest()) return

        val token = tokenManager.getToken() ?: return

        try {
            val serverNotes = api.getNotes("Bearer $token")
            val localNotes = dao.getAllOnce(userId)

            serverNotes.forEach { serverNote ->
                val existingLocal = localNotes.find { local ->
                    local.serverId == serverNote.id
                }

                if (existingLocal == null && !serverNote.isDeleted) {
                    val newNote = NoteEntity(
                        id = serverNote.id,
                        userId = serverNote.userId,
                        title = serverNote.title,
                        type = serverNote.type,
                        content = "",
                        createdAt = serverNote.createdAt,
                        updatedAt = serverNote.updatedAt,
                        isDeleted = false,
                        serverId = serverNote.id,
                        filePath = serverNote.filePath
                    )
                    dao.insert(newNote)
                    downloadNoteContent(userId, serverNote.id, token)
                    Log.d(TAG, "ADDED new server note: ${serverNote.title}")
                } else if (existingLocal != null && serverNote.updatedAt > existingLocal.updatedAt && !serverNote.isDeleted) {
                    val updatedNote = existingLocal.copy(
                        title = serverNote.title,
                        type = serverNote.type,
                        updatedAt = serverNote.updatedAt,
                        filePath = serverNote.filePath,
                        isDeleted = false
                    )
                    dao.insert(updatedNote)
                    Log.d(TAG, "UPDATED existing note: ${serverNote.title}")
                } else if (existingLocal != null && serverNote.isDeleted && !existingLocal.isDeleted) {
                    dao.moveToTrash(existingLocal.id, System.currentTimeMillis())
                    Log.d(TAG, "Note moved to trash due to server deletion: ${serverNote.title}")
                }
            }

            syncRelationsFromServer(userId)

        } catch (e: Exception) {
            Log.e(TAG, "FETCH FAILED", e)
        }
    }

    suspend fun getAllNotes(userId: String): List<NoteDto> {
        return dao.getAllActiveNotes(userId).map { it.toDto() }
    }

    // ========== МЕТОДЫ ДЛЯ РАБОТЫ СО СВЯЗЯМИ ==========

    suspend fun getChildNotes(parentId: String, userId: String): List<NoteDto> {
        Log.d(TAG, "getChildNotes: parentId=$parentId, userId=$userId")
        val childNotes = dao.getChildrenForNote(parentId).filter { it.userId == userId && !it.isDeleted }
        Log.d(TAG, "Found ${childNotes.size} child notes")
        return childNotes.map { it.toDto() }
    }

    suspend fun getChildNotesIds(parentId: String): List<String> {
        Log.d(TAG, "getChildNotesIds: parentId=$parentId")
        return dao.getChildrenIdsForNote(parentId)
    }

    suspend fun getParentNotes(noteId: String, userId: String): List<NoteDto> {
        Log.d(TAG, "getParentNotes: noteId=$noteId, userId=$userId")
        val parentNotes = dao.getParentsForNote(noteId).filter { it.userId == userId && !it.isDeleted }
        Log.d(TAG, "Found ${parentNotes.size} parent notes")
        parentNotes.forEach { note ->
            Log.d(TAG, "  Parent: ${note.title} (${note.id})")
        }
        return parentNotes.map { it.toDto() }
    }

    suspend fun addParentRelation(childNoteId: String, parentNoteId: String) {
        Log.d(TAG, "addParentRelation: $childNoteId -> $parentNoteId")
        if (childNoteId == parentNoteId) {
            Log.w(TAG, "Cannot add self as parent")
            return
        }

        if (dao.hasRelation(childNoteId, parentNoteId)) {
            Log.w(TAG, "Relation already exists")
            return
        }

        val relation = NoteRelationEntity(
            noteId = childNoteId,
            parentNoteId = parentNoteId
        )
        dao.addRelation(relation)
        Log.d(TAG, "Parent relation added locally")

        if (!tokenManager.isGuest() && NetworkUtil.isOnline(context)) {
            try {
                val token = tokenManager.getToken() ?: return
                val allParentIds = dao.getParentIdsForNote(childNoteId)

                api.updateNote(
                    childNoteId,
                    "Bearer $token",
                    UpdateNoteRequest(parentIds = allParentIds)
                )
                Log.d(TAG, "ParentIds synced to server: $childNoteId -> $allParentIds")
            } catch (e: Exception) {
                Log.e(TAG, "Failed to sync parentIds to server", e)
            }
        }
    }

    suspend fun removeParentRelation(childNoteId: String, parentNoteId: String) {
        Log.d(TAG, "removeParentRelation: $childNoteId -> $parentNoteId")

        dao.removeRelation(childNoteId, parentNoteId)
        Log.d(TAG, "Parent relation removed locally")

        if (!tokenManager.isGuest() && NetworkUtil.isOnline(context)) {
            try {
                val token = tokenManager.getToken() ?: return
                val allParentIds = dao.getParentIdsForNote(childNoteId)

                api.updateNote(
                    childNoteId,
                    "Bearer $token",
                    UpdateNoteRequest(parentIds = allParentIds)
                )
                Log.d(TAG, "ParentIds updated on server: $childNoteId -> $allParentIds")
            } catch (e: Exception) {
                Log.e(TAG, "Failed to update parentIds on server", e)
            }
        }
    }

    suspend fun syncRelationsFromServer(userId: String) {
        if (tokenManager.isGuest()) return

        val token = tokenManager.getToken() ?: return

        try {
            val serverNotes = api.getNotes("Bearer $token")

            dao.clearAllRelations()

            serverNotes.forEach { serverNote ->
                if (serverNote.isDeleted) return@forEach

                serverNote.parentIds.forEach { parentId ->
                    var cleanParentId = parentId.removeSurrounding("{", "}")
                    if (cleanParentId.isNotEmpty() && cleanParentId != "[]") {
                        val relation = NoteRelationEntity(
                            noteId = serverNote.id,
                            parentNoteId = cleanParentId
                        )
                        dao.addRelation(relation)
                        Log.d(TAG, "Added relation: ${serverNote.id} -> $cleanParentId")
                    }
                }
            }

            Log.d(TAG, "Relations synced from server")
        } catch (e: Exception) {
            Log.e(TAG, "Failed to sync relations", e)
        }
    }
}