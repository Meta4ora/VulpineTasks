package com.example.vulpinetasks

import android.content.Intent
import android.os.Bundle
import android.widget.Toast
import androidx.appcompat.app.AppCompatActivity
import androidx.core.view.GravityCompat
import androidx.lifecycle.lifecycleScope
import androidx.recyclerview.widget.LinearLayoutManager
import com.example.vulpinetasks.backend.TokenManager
import com.example.vulpinetasks.databinding.ActivityMainBinding
import com.example.vulpinetasks.room.AppGraph
import kotlinx.coroutines.launch

class MainActivity : AppCompatActivity() {

    private lateinit var binding: ActivityMainBinding
    private lateinit var adapter: NotesAdapter

    private val repo = AppGraph.notesRepository
    private lateinit var userId: String
    private lateinit var tokenManager: TokenManager

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)

        binding = ActivityMainBinding.inflate(layoutInflater)
        setContentView(binding.root)

        tokenManager = TokenManager(this)

        userId = tokenManager.getUserId() ?: run {
            startActivity(Intent(this, LoginActivity::class.java))
            finish()
            return
        }

        setupRecycler()
        observeNotes()
        setupDrawer()

        lifecycleScope.launch {
            repo.fetchNotesFromServer(userId)
        }

        binding.menuButton.setOnClickListener {
            binding.drawerLayout.openDrawer(GravityCompat.START)
        }
    }

    override fun onStart() {
        super.onStart()

        lifecycleScope.launch {
            repo.syncAll(userId)
        }
    }

    private fun observeNotes() {
        lifecycleScope.launch {
            repo.getNotes(userId).collect { list ->
                adapter.updateNotes(
                    list.map {
                        com.example.vulpinetasks.backend.NoteDto(
                            id = it.id,
                            userId = it.userId,
                            title = it.title,
                            type = it.type,
                            parentId = null,
                            filePath = "",
                            createdAt = it.updatedAt,
                            updatedAt = it.updatedAt
                        )
                    }
                )
            }
        }
    }

    private fun setupRecycler() {
        adapter = NotesAdapter(emptyList()) {}

        binding.notesRecyclerView.layoutManager =
            LinearLayoutManager(this)

        binding.notesRecyclerView.adapter = adapter
    }

    private fun showCreateDialog() {
        val input = android.widget.EditText(this)

        androidx.appcompat.app.AlertDialog.Builder(this)
            .setTitle("Новая заметка")
            .setView(input)
            .setPositiveButton("Создать") { _, _ ->
                lifecycleScope.launch {
                    repo.createNote(
                        input.text.toString(),
                        "note",
                        userId
                    )
                }
            }
            .show()
    }

    private fun setupDrawer() {
        binding.navView.setNavigationItemSelectedListener { item ->

            when (item.itemId) {

                R.id.nav_login -> {
                    startActivity(Intent(this, LoginActivity::class.java))
                }

                R.id.nav_guest -> {
                    tokenManager.clear()
                    startActivity(Intent(this, LoginActivity::class.java))
                    finish()
                }
            }

            binding.drawerLayout.closeDrawer(GravityCompat.START)
            true
        }
    }

    private fun toast(msg: String) {
        Toast.makeText(this, msg, Toast.LENGTH_SHORT).show()
    }
}