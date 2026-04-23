package com.example.vulpinetasks.room

import androidx.room.*
import kotlinx.coroutines.flow.Flow

@Dao
interface NoteDao {

    @Query("""
        SELECT * FROM notes 
        WHERE userId = :userId AND isDeleted = 0 
        ORDER BY updatedAt DESC
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
}