package com.example.vulpinetasks

import android.content.Intent
import android.os.Bundle
import android.widget.EditText
import android.widget.Toast
import androidx.appcompat.app.AlertDialog
import androidx.appcompat.app.AppCompatActivity
import androidx.core.view.GravityCompat
import androidx.lifecycle.lifecycleScope
import androidx.recyclerview.widget.LinearLayoutManager
import com.example.vulpinetasks.backend.TokenManager
import com.example.vulpinetasks.databinding.ActivityMainBinding
import com.example.vulpinetasks.room.AppGraph
import com.example.vulpinetasks.util.NetworkUtil
import kotlinx.coroutines.launch

class MainActivity : AppCompatActivity() {

    private lateinit var binding: ActivityMainBinding
    private lateinit var adapter: NotesAdapter
    private lateinit var tokenManager: TokenManager

    private val repo get() = AppGraph.notesRepository
    private lateinit var userId: String

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)

        binding = ActivityMainBinding.inflate(layoutInflater)
        setContentView(binding.root)

        tokenManager = TokenManager(this)

        userId = tokenManager.getUserId()
            ?: "guest_${System.currentTimeMillis()}"

        tokenManager.saveUserId(userId)

        setupRecycler()
        observeNotes()
        setupDrawer()

        lifecycleScope.launch {
            repo.fetchFromServer(userId)
        }

        binding.addNoteButton.setOnClickListener {
            showCreateDialog()
        }
    }

    private fun setupRecycler() {
        adapter = NotesAdapter(
            onOpen = {
                toast("Open: ${it.title}")
            },
            onTrash = {
                lifecycleScope.launch {
                    repo.moveToTrash(it)
                }
            }
        )

        binding.notesRecyclerView.layoutManager =
            LinearLayoutManager(this)
        binding.notesRecyclerView.adapter = adapter
    }

    private fun observeNotes() {
        lifecycleScope.launch {
            repo.observeNotes(userId).collect {
                adapter.submitList(it)
            }
        }
    }

    private fun setupDrawer() {
        binding.navView.setNavigationItemSelectedListener { item ->
            when (item.itemId) {
                R.id.nav_login -> {
                    startActivity(Intent(this, LoginActivity::class.java))
                }
                R.id.nav_trash -> {
                    startActivity(Intent(this, TrashActivity::class.java))
                }
            }
            binding.drawerLayout.closeDrawer(GravityCompat.START)
            true
        }

        binding.menuButton.setOnClickListener {
            binding.drawerLayout.openDrawer(GravityCompat.START)
        }
    }

    private fun showCreateDialog() {
        val input = EditText(this)

        AlertDialog.Builder(this)
            .setTitle("Новая заметка")
            .setView(input)
            .setPositiveButton("Создать") { _, _ ->
                val text = input.text.toString().trim()
                if (text.isEmpty()) return@setPositiveButton

                lifecycleScope.launch {
                    repo.createNote(
                        text,
                        "note",
                        userId,
                        NetworkUtil.isOnline(this@MainActivity)
                    )
                }
            }
            .setNegativeButton("Отмена", null)
            .show()
    }

    private fun toast(msg: String) {
        Toast.makeText(this, msg, Toast.LENGTH_SHORT).show()
    }
}