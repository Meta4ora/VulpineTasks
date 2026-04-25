package com.example.vulpinetasks.backend

import retrofit2.Response
import retrofit2.http.*

data class AuthRequest(
    val email: String,
    val password: String
)

data class AuthResponse(
    val token: String,
    val userId: String
)

// CreateNoteRequest с parentId (как на сервере)
data class CreateNoteRequest(
    val title: String,
    val type: String,
    val parentId: String? = null
)

data class UpdateNoteRequest(
    val title: String? = null,
    val type: String? = null,
    val content: String? = null,
    val isDeleted: Boolean? = null
)

data class NoteDto(
    val id: String,
    val userId: String,
    val title: String,
    val type: String,
    val content: String = "",
    val parentId: String? = null,
    val filePath: String? = null,
    val createdAt: Long,
    val updatedAt: Long,
    val isDeleted: Boolean = false
)

interface ApiService {

    // Auth endpoints
    @POST("auth/login")
    suspend fun login(@Body request: AuthRequest): AuthResponse

    @POST("auth/register")
    suspend fun register(@Body request: AuthRequest): AuthResponse

    // Notes endpoints
    @GET("notes")
    suspend fun getNotes(@Header("Authorization") token: String): List<NoteDto>

    @GET("notes/{id}")
    suspend fun getNote(
        @Path("id") id: String,
        @Header("Authorization") token: String
    ): NoteDto

    @GET("notes/{id}/content")
    suspend fun getNoteContent(
        @Path("id") id: String,
        @Header("Authorization") token: String
    ): String

    @POST("notes")
    suspend fun createNote(
        @Header("Authorization") token: String,
        @Body request: CreateNoteRequest
    ): NoteDto

    @PUT("notes/{id}")
    suspend fun updateNote(
        @Path("id") id: String,
        @Header("Authorization") token: String,
        @Body request: UpdateNoteRequest
    ): NoteDto

    @PUT("notes/{id}/content")
    suspend fun updateNoteContent(
        @Path("id") id: String,
        @Header("Authorization") token: String,
        @Body content: String
    ): Response<Unit>

    @DELETE("notes/{id}")
    suspend fun deleteNote(
        @Path("id") id: String,
        @Header("Authorization") token: String
    )
}