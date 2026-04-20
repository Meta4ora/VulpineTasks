package com.example.vulpinetasks.room

import com.example.vulpinetasks.backend.RetrofitClient
import com.example.vulpinetasks.backend.TokenManager
import com.example.vulpinetasks.room.DatabaseProvider
import com.example.vulpinetasks.room.NotesRepository

object AppGraph {

    lateinit var notesRepository: NotesRepository

    fun init(context: android.content.Context) {

        DatabaseProvider.init(context)

        notesRepository = NotesRepository(
            dao = DatabaseProvider.db.noteDao(),
            api = RetrofitClient.api,
            tokenManager = TokenManager(context)
        )
    }
}