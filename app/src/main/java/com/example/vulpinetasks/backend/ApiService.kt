package com.example.vulpinetasks.backend

import retrofit2.http.*

data class AuthRequest(
    val email: String,
    val password: String
)

data class AuthResponse(
    val token: String,
    val userId: String
)

interface ApiService {

    // Auth endpoints
    @POST("auth/login")
    suspend fun login(
        @Body request: AuthRequest
    ): AuthResponse

    @POST("auth/register")
    suspend fun register(
        @Body request: AuthRequest
    ): AuthResponse

    // Notes endpoints
    @GET("notes")
    suspend fun getNotes(
        @Header("Authorization") token: String
    ): List<NoteDto>

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

    @DELETE("notes/{id}")
    suspend fun deleteNote(
        @Path("id") id: String,
        @Header("Authorization") token: String
    )
}