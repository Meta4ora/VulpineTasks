package com.example.vulpinetasks

import android.view.*
import android.widget.ImageView
import android.widget.TextView
import androidx.recyclerview.widget.RecyclerView
import com.example.vulpinetasks.backend.NoteDto

class TrashAdapter(
    private var notes: List<NoteDto>,
    private val onRestore: (NoteDto) -> Unit,
    private val onDelete: (NoteDto) -> Unit
) : RecyclerView.Adapter<TrashAdapter.VH>() {

    fun update(newList: List<NoteDto>) {
        notes = newList
        notifyDataSetChanged()
    }

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): VH {
        val view = LayoutInflater.from(parent.context)
            .inflate(R.layout.item_trash_note, parent, false)
        return VH(view)
    }

    override fun getItemCount() = notes.size

    override fun onBindViewHolder(holder: VH, position: Int) {
        holder.bind(notes[position])
    }

    inner class VH(view: View) : RecyclerView.ViewHolder(view) {

        private val title = view.findViewById<TextView>(R.id.title)
        private val restore = view.findViewById<ImageView>(R.id.restore)
        private val delete = view.findViewById<ImageView>(R.id.delete)

        fun bind(note: NoteDto) {
            title.text = note.title

            restore.setOnClickListener {
                onRestore(note)
            }

            delete.setOnClickListener {
                onDelete(note)
            }
        }
    }
}