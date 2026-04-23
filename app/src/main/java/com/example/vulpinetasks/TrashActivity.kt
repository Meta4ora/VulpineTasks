package com.example.vulpinetasks

import android.app.AlertDialog
import android.os.Bundle
import android.view.View
import android.widget.Toast
import androidx.appcompat.app.AppCompatActivity
import androidx.lifecycle.lifecycleScope
import androidx.recyclerview.widget.LinearLayoutManager
import com.example.vulpinetasks.backend.NoteDto
import com.example.vulpinetasks.backend.TokenManager
import com.example.vulpinetasks.databinding.ActivityTrashBinding
import com.example.vulpinetasks.room.AppGraph
import kotlinx.coroutines.launch

class TrashActivity : AppCompatActivity() {

    private lateinit var binding: ActivityTrashBinding
    private lateinit var adapter: TrashAdapter

    private val repo get() = AppGraph.notesRepository
    private lateinit var userId: String
    private lateinit var tokenManager: TokenManager

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)

        binding = ActivityTrashBinding.inflate(layoutInflater)
        setContentView(binding.root)

        tokenManager = TokenManager(this)
        userId = tokenManager.getUserId() ?: ""

        setupRecycler()
        observeTrash()
    }

    private fun setupRecycler() {
        adapter = TrashAdapter(
            emptyList(),
            onRestore = {
                lifecycleScope.launch {
                    repo.restore(it)
                }
            },
            onDelete = {
                showDeleteDialog(it)
            }
        )

        binding.recycler.layoutManager =
            LinearLayoutManager(this)

        binding.recycler.adapter = adapter
    }

    private fun observeTrash() {
        lifecycleScope.launch {
            repo.observeTrash(userId).collect { list ->
                adapter.update(list)

                binding.emptyText.visibility =
                    if (list.isEmpty()) View.VISIBLE else View.GONE
            }
        }
    }

    private fun showDeleteDialog(note: NoteDto) {
        AlertDialog.Builder(this)
            .setTitle("Удалить заметку?")
            .setPositiveButton("Удалить") { _, _ ->
                lifecycleScope.launch {
                    repo.delete(note)
                }
            }
            .setNegativeButton("Отмена", null)
            .show()
    }

    private fun toast(msg: String) {
        Toast.makeText(this, msg, Toast.LENGTH_SHORT).show()
    }
}