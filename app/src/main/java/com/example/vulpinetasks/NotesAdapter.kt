package com.example.vulpinetasks

import android.view.*
import android.widget.TextView
import androidx.recyclerview.widget.RecyclerView
import com.example.vulpinetasks.backend.NoteDto

class NotesAdapter(
    private var notes: List<NoteDto>,
    private val onClick: (NoteDto) -> Unit
) : RecyclerView.Adapter<NotesAdapter.VH>() {

    fun updateNotes(newNotes: List<NoteDto>) {
        notes = newNotes
        notifyDataSetChanged()
    }

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): VH {
        val view = LayoutInflater.from(parent.context)
            .inflate(R.layout.item_note, parent, false)
        return VH(view)
    }

    override fun onBindViewHolder(holder: VH, position: Int) {
        holder.bind(notes[position])
    }

    override fun getItemCount() = notes.size

    inner class VH(itemView: View) : RecyclerView.ViewHolder(itemView) {

        private val icon = itemView.findViewById<TextView>(R.id.type_icon)
        private val title = itemView.findViewById<TextView>(R.id.note_title)
        private val preview = itemView.findViewById<TextView>(R.id.note_preview)
        private val date = itemView.findViewById<TextView>(R.id.date_text)

        fun bind(note: NoteDto) {

            icon.text = when (note.type) {
                "task" -> "✅"
                else -> "📝"
            }

            title.text = note.title
            preview.text = note.type
            date.text = note.createdAt.toString()

            itemView.setOnClickListener {
                onClick(note)
            }
        }
    }
}