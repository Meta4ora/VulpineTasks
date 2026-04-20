package com.example.vulpinetasks.room

import androidx.room.*
import kotlinx.coroutines.flow.Flow

@Dao
interface NoteDao {

    @Query("SELECT * FROM notes WHERE userId = :userId ORDER BY updatedAt DESC")
    fun getNotes(userId: String): Flow<List<NoteEntity>>

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insert(note: NoteEntity)

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertAll(notes: List<NoteEntity>)

    @Query("DELETE FROM notes WHERE id = :id AND userId = :userId")
    suspend fun delete(id: String, userId: String)

    @Query("SELECT * FROM notes WHERE isSynced = 0 AND userId = :userId")
    suspend fun getUnsynced(userId: String): List<NoteEntity>
}