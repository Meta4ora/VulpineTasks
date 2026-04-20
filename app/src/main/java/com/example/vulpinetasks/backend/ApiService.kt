package com.example.vulpinetasks.backend

import retrofit2.http.*

data class AuthRequest(val email: String, val password: String)
data class AuthResponse(val token: String, val userId: String)
data class RegisterResponse(val id: String, val email: String)

data class CreateNoteRequest(
    val title: String,
    val type: String,
    val parentId: String? = null
)

data class NoteDto(
    val id: String,
    val userId: String,
    val title: String,
    val type: String,
    val parentId: String?,
    val filePath: String,
    val createdAt: Long,
    val updatedAt: Long
)

interface ApiService {

    @POST("auth/register")
    suspend fun register(@Body body: AuthRequest): RegisterResponse

    @POST("auth/login")
    suspend fun login(@Body body: AuthRequest): AuthResponse

    @GET("notes")
    suspend fun getNotes(
        @Header("Authorization") token: String,
        @Query("parentId") parentId: String? = null
    ): List<NoteDto>

    @POST("notes")
    suspend fun createNote(
        @Header("Authorization") token: String,
        @Body body: CreateNoteRequest
    )

    @GET("notes/{id}")
    suspend fun getNote(
        @Path("id") id: String,
        @Header("Authorization") token: String
    ): NoteDto
}