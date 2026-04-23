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

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insert(note: NoteEntity)

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertAll(list: List<NoteEntity>)

    @Query("DELETE FROM notes WHERE userId = :userId")
    suspend fun clearAll(userId: String)

    @Query("UPDATE notes SET isDeleted = 1 WHERE id = :id")
    suspend fun moveToTrash(id: String)

    @Query("UPDATE notes SET isDeleted = 0, updatedAt = :time WHERE id = :id")
    suspend fun restore(id: String, time: Long)

    @Query("DELETE FROM notes WHERE id = :id")
    suspend fun delete(id: String)

    @Query("SELECT * FROM notes WHERE userId = :userId")
    suspend fun getAllOnce(userId: String): List<NoteEntity>
}