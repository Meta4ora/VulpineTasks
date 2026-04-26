package com.example.vulpinetasks

import android.view.LayoutInflater
import android.view.ViewGroup
import androidx.recyclerview.widget.DiffUtil
import androidx.recyclerview.widget.ListAdapter
import androidx.recyclerview.widget.RecyclerView
import com.example.vulpinetasks.backend.NoteDto
import com.example.vulpinetasks.databinding.ItemChildNoteBinding

class ChildNotesAdapter(
    private val onNoteClick: (NoteDto) -> Unit,
    private val onUnlinkClick: (NoteDto) -> Unit
) : ListAdapter<NoteDto, ChildNotesAdapter.ChildNoteViewHolder>(Diff) {

    object Diff : DiffUtil.ItemCallback<NoteDto>() {
        override fun areItemsTheSame(oldItem: NoteDto, newItem: NoteDto): Boolean {
            return oldItem.id == newItem.id
        }

        override fun areContentsTheSame(oldItem: NoteDto, newItem: NoteDto): Boolean {
            return oldItem == newItem
        }
    }

    inner class ChildNoteViewHolder(
        private val binding: ItemChildNoteBinding
    ) : RecyclerView.ViewHolder(binding.root) {

        fun bind(note: NoteDto) {
            // Устанавливаем заголовок
            binding.noteTitle.text = note.title

            // Обработчик клика по всей карточке
            binding.root.setOnClickListener {
                onNoteClick(note)
            }

            // Обработчик клика по кнопке удаления
            binding.removeButton.setOnClickListener {
                onUnlinkClick(note)
            }
        }
    }

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): ChildNoteViewHolder {
        val binding = ItemChildNoteBinding.inflate(
            LayoutInflater.from(parent.context),
            parent,
            false
        )
        return ChildNoteViewHolder(binding)
    }

    override fun onBindViewHolder(holder: ChildNoteViewHolder, position: Int) {
        holder.bind(getItem(position))
    }
}