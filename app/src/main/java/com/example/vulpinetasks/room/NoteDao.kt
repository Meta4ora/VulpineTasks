package com.example.vulpinetasks.room

import androidx.room.*
import kotlinx.coroutines.flow.Flow

@Dao
interface NoteDao {

    @Query("""
        SELECT * FROM notes 
        WHERE userId = :userId AND isDeleted = 0 ORDER BY updatedAt DESC
    """)
    fun getNotes(userId: String): Flow<List<NoteEntity>>

    @Query("""
        SELECT * FROM notes 
        WHERE userId = :userId AND isDeleted = 1 
        ORDER BY updatedAt DESC
    """)
    fun getTrash(userId: String): Flow<List<NoteEntity>>

    @Query("SELECT * FROM notes WHERE id = :id AND isDeleted = 0 LIMIT 1")
    suspend fun getActiveNoteById(id: String): NoteEntity?

    @Query("SELECT * FROM notes WHERE id = :id AND isDeleted = 1 LIMIT 1")
    suspend fun getTrashNoteById(id: String): NoteEntity?

    @Query("SELECT * FROM notes WHERE id = :id LIMIT 1")
    suspend fun getNoteById(id: String): NoteEntity?

    @Query("SELECT * FROM notes WHERE userId = :userId AND isDeleted = 0")
    suspend fun getAllActiveNotes(userId: String): List<NoteEntity>

    @Query("SELECT * FROM notes WHERE userId = :userId")
    suspend fun getAllOnce(userId: String): List<NoteEntity>

    @Query("SELECT * FROM notes WHERE userId = :userId AND serverId IS NULL")
    suspend fun getUnsyncedNotes(userId: String): List<NoteEntity>

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insert(note: NoteEntity)

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertAll(notes: List<NoteEntity>)

    @Update
    suspend fun update(note: NoteEntity)

    @Query("UPDATE notes SET isDeleted = 1, updatedAt = :time WHERE id = :id AND isDeleted = 0")
    suspend fun moveToTrash(id: String, time: Long)

    @Query("UPDATE notes SET isDeleted = 0, updatedAt = :time WHERE id = :id AND isDeleted = 1")
    suspend fun restoreFromTrash(id: String, time: Long)

    @Query("DELETE FROM notes WHERE id = :id")
    suspend fun delete(id: String)

    @Query("DELETE FROM notes WHERE userId = :userId")
    suspend fun clearAll(userId: String)

    @Query("UPDATE notes SET userId = :newUserId WHERE userId = :oldUserId")
    suspend fun migrateUserId(oldUserId: String, newUserId: String)

    @Query("""
        INSERT OR REPLACE INTO notes (id, userId, title, type, createdAt, updatedAt, isDeleted, serverId, filePath)
        SELECT 
            id, 
            :newUserId AS userId, 
            title, 
            type, 
            createdAt, 
            updatedAt, 
            isDeleted, 
            NULL AS serverId,
            NULL AS filePath
        FROM notes 
        WHERE userId = :sourceUserId AND isDeleted = 0
    """)
    suspend fun copyNotesToUser(sourceUserId: String, newUserId: String)

    // ========== Методы для работы со связями (many-to-many) ==========

    @Query("""
        SELECT n.* FROM notes n
        INNER JOIN note_relations nr ON n.id = nr.parentNoteId
        WHERE nr.noteId = :noteId AND n.isDeleted = 0
        ORDER BY n.title ASC
    """)
    suspend fun getParentsForNote(noteId: String): List<NoteEntity>

    @Query("""
        SELECT n.* FROM notes n
        INNER JOIN note_relations nr ON n.id = nr.noteId
        WHERE nr.parentNoteId = :parentId AND n.isDeleted = 0
        ORDER BY n.title ASC
    """)
    suspend fun getChildrenForNote(parentId: String): List<NoteEntity>

    @Query("SELECT parentNoteId FROM note_relations WHERE noteId = :noteId")
    suspend fun getParentIdsForNote(noteId: String): List<String>

    @Query("SELECT noteId FROM note_relations WHERE parentNoteId = :parentId")
    suspend fun getChildrenIdsForNote(parentId: String): List<String>

    @Insert(onConflict = OnConflictStrategy.IGNORE)
    suspend fun addRelation(relation: NoteRelationEntity)

    @Query("DELETE FROM note_relations WHERE noteId = :noteId AND parentNoteId = :parentId")
    suspend fun removeRelation(noteId: String, parentId: String)

    @Query("DELETE FROM note_relations WHERE noteId = :noteId")
    suspend fun removeAllRelationsForNote(noteId: String)

    @Query("SELECT COUNT(*) > 0 FROM note_relations WHERE noteId = :noteId AND parentNoteId = :parentId")
    suspend fun hasRelation(noteId: String, parentId: String): Boolean

    @Query("DELETE FROM note_relations")
    suspend fun clearAllRelations()

    @Query("""
    SELECT n.* FROM notes n
    INNER JOIN note_relations nr ON n.id = nr.noteId
    WHERE nr.parentNoteId = :parentId
""")
    suspend fun getNotesReferencingParentId(parentId: String): List<NoteEntity>
}