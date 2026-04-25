package com.example.vulpinetasks.room

import com.example.vulpinetasks.backend.RetrofitClient
import com.example.vulpinetasks.backend.TokenManager

object AppGraph {

    lateinit var notesRepository: NotesRepository

    fun init(context: android.content.Context) {
        DatabaseProvider.init(context)

        notesRepository = NotesRepository(
            context = context,
            dao = DatabaseProvider.db.noteDao(),
            api = RetrofitClient.api,
            tokenManager = TokenManager(context)
        )

        notesRepository.initFileStorage()
    }
}