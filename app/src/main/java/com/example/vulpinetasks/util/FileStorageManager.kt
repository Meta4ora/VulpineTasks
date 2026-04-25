package com.example.vulpinetasks.util

import android.content.Context
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import java.io.File
import java.io.FileOutputStream

class FileStorageManager(private val context: Context) {

    companion object {
        private const val NOTES_DIR = "notes"
    }

    private fun getNotesDirectory(userId: String): File {
        return File(context.filesDir, "$NOTES_DIR/$userId").apply {
            if (!exists()) mkdirs()
        }
    }

    private fun getNoteFile(userId: String, noteId: String): File {
        return File(getNotesDirectory(userId), "$noteId.md")
    }

    suspend fun saveNoteContent(userId: String, noteId: String, content: String) {
        withContext(Dispatchers.IO) {
            try {
                val file = getNoteFile(userId, noteId)
                file.writeText(content)
            } catch (e: Exception) {
                e.printStackTrace()
            }
        }
    }

    suspend fun loadNoteContent(userId: String, noteId: String): String? {
        return withContext(Dispatchers.IO) {
            try {
                val file = getNoteFile(userId, noteId)
                if (file.exists()) {
                    file.readText()
                } else {
                    null
                }
            } catch (e: Exception) {
                e.printStackTrace()
                null
            }
        }
    }

    suspend fun deleteNoteFile(userId: String, noteId: String) {
        withContext(Dispatchers.IO) {
            try {
                getNoteFile(userId, noteId).delete()
            } catch (e: Exception) {
                e.printStackTrace()
            }
        }
    }

    suspend fun noteFileExists(userId: String, noteId: String): Boolean {
        return withContext(Dispatchers.IO) {
            getNoteFile(userId, noteId).exists()
        }
    }
}