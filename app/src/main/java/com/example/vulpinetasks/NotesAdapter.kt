package com.example.vulpinetasks

import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.PopupMenu
import androidx.recyclerview.widget.DiffUtil
import androidx.recyclerview.widget.ListAdapter
import androidx.recyclerview.widget.RecyclerView
import com.example.vulpinetasks.backend.NoteDto
import com.example.vulpinetasks.databinding.ItemNoteBinding

class NotesAdapter(
    private val onOpen: (NoteDto) -> Unit,
    private val onTrash: (NoteDto) -> Unit,
    private val onInfo: (NoteDto) -> Unit,
    private val onRename: (NoteDto) -> Unit  // Добавляем колбэк для переименования
) : ListAdapter<NoteDto, NotesAdapter.VH>(Diff) {

    object Diff : DiffUtil.ItemCallback<NoteDto>() {
        override fun areItemsTheSame(a: NoteDto, b: NoteDto) = a.id == b.id
        override fun areContentsTheSame(a: NoteDto, b: NoteDto) = a == b
    }

    inner class VH(private val b: ItemNoteBinding) :
        RecyclerView.ViewHolder(b.root) {

        fun bind(note: NoteDto) {
            b.noteTitle.text = note.title
            b.notePreview.text = note.type

            b.root.setOnClickListener {
                onOpen(note)
            }

            b.noteMenuButton.setOnClickListener { v: View ->
                val popup = PopupMenu(v.context, v)
                popup.menu.add("Переименовать")
                popup.menu.add("Информация")
                popup.menu.add("В корзину")

                popup.setOnMenuItemClickListener { menuItem ->
                    when (menuItem.title) {
                        "Переименовать" -> onRename(note)
                        "Информация" -> onInfo(note)
                        "В корзину" -> onTrash(note)
                    }
                    true
                }

                popup.show()
            }
        }
    }

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): VH {
        val binding = ItemNoteBinding.inflate(
            LayoutInflater.from(parent.context),
            parent,
            false
        )
        return VH(binding)
    }

    override fun onBindViewHolder(holder: VH, position: Int) {
        holder.bind(getItem(position))
    }
}