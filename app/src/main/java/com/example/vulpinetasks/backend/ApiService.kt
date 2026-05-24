package com.example.vulpinetasks.backend

import retrofit2.Response
import retrofit2.http.*
import org.json.JSONArray

data class AuthRequest(
    val email: String,
    val password: String
)

data class AuthResponse(
    val token: String,
    val userId: String,
    val email: String? = null,
    val createdAt: String? = null
)

data class UserInfo(
    val userId: String,
    val email: String,
    val createdAt: String? = null,
    val updatedAt: String? = null
)

data class CreateNoteRequest(
    val title: String,
    val type: String,
    val parentIds: List<String> = emptyList()
)

data class UpdateNoteRequest(
    val title: String? = null,
    val type: String? = null,
    val content: String? = null,
    val isDeleted: Boolean? = null,
    val parentIds: List<String>? = null
)

data class NoteDto(
    val id: String,
    val userId: String,
    val title: String,
    val type: String,
    val content: String = "",
    val parentIds: List<String> = emptyList(),
    val filePath: String? = null,
    val createdAt: Long,
    val updatedAt: Long,
    val isDeleted: Boolean = false
) {
    // Метод для подсчета символов (без HTML тегов)
    fun getCharacterCount(): Int {
        return if (type == "task") {
            // Для задач считаем подзадачи
            try {
                val jsonArray = JSONArray(content)
                jsonArray.length()
            } catch (e: Exception) {
                0
            }
        } else {
            // Для заметок считаем символы без HTML тегов
            content
                .replace(Regex("<[^>]*>"), "") // Удаляем HTML теги
                .replace(Regex("\\s+"), " ") // Заменяем множественные пробелы
                .trim()
                .length
        }
    }

    // Метод для подсчета слов
    fun getWordCount(): Int {
        if (type == "task") return 0

        val cleanText = content
            .replace(Regex("<[^>]*>"), "")
            .replace(Regex("[#*_>`~\\[\\](){}]"), "")
            .trim()

        if (cleanText.isEmpty()) return 0
        return cleanText.split(Regex("\\s+")).filter { it.isNotEmpty() }.size
    }

    // Метод для получения статистики подзадач
    fun getSubtaskStats(): Pair<Int, Int> {
        if (type != "task") return Pair(0, 0)

        return try {
            val jsonArray = JSONArray(content)
            var completed = 0
            for (i in 0 until jsonArray.length()) {
                val obj = jsonArray.getJSONObject(i)
                if (obj.getBoolean("isCompleted")) {
                    completed++
                }
            }
            Pair(completed, jsonArray.length())
        } catch (e: Exception) {
            Pair(0, 0)
        }
    }

    // Форматированный вывод количества слов
    fun getFormattedWordCount(): String {
        val count = getWordCount()
        return when {
            count == 0 -> "нет слов"
            count % 10 == 1 && count % 100 != 11 -> "$count слово"
            count % 10 in 2..4 && count % 100 !in 12..14 -> "$count слова"
            else -> "$count слов"
        }
    }

    // Форматированный вывод количества символов
    fun getFormattedCharacterCount(): String {
        val count = getCharacterCount()
        return when {
            count == 0 -> "пусто"
            count < 1000 -> "$count симв."
            count < 10000 -> "${count / 1000}.${(count % 1000) / 100} тыс."
            else -> "${count / 1000} тыс."
        }
    }

    // Получить превью текста
    fun getPreview(maxLength: Int = 100): String {
        return if (type == "task") {
            val (completed, total) = getSubtaskStats()
            if (total > 0) {
                "📋 $completed из $total подзадач выполнено"
            } else {
                "Нет подзадач"
            }
        } else {
            val cleanText = getCleanContent()
            if (cleanText.length > maxLength) {
                cleanText.substring(0, maxLength) + "..."
            } else if (cleanText.isNotEmpty()) {
                cleanText
            } else {
                "Нет содержания"
            }
        }
    }

    // Получить очищенный контент без HTML и маркдауна
    fun getCleanContent(): String {
        return content
            .replace(Regex("<[^>]*>"), "") // HTML теги
            .replace(Regex("[#*_>`~\\[\\](){}]"), "") // Маркдаун символы
            .replace(Regex("\\s+"), " ") // Множественные пробелы
            .trim()
    }
}

data class SubTaskDto(
    val id: String,
    val title: String,
    val isCompleted: Boolean = false
)

data class NoteRelationDto(
    val noteId: String,
    val parentNoteId: String
)

data class NoteWithRelationsDto(
    val id: String,
    val userId: String,
    val title: String,
    val type: String,
    val content: String = "",
    val parentIds: List<String> = emptyList(),
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

    @GET("api/users/{userId}")
    suspend fun getUserInfo(@Path("userId") userId: String): UserInfo
}