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
import org.json.JSONArray
import java.text.SimpleDateFormat
import java.util.*

class NotesAdapter(
    private val onOpen: (NoteDto) -> Unit,
    private val onTrash: (NoteDto) -> Unit,
    private val onInfo: (NoteDto) -> Unit,
    private val onRename: (NoteDto) -> Unit
) : ListAdapter<NoteDto, NotesAdapter.VH>(Diff) {

    private val dateFormat = SimpleDateFormat("dd.MM.yyyy", Locale.getDefault())

    object Diff : DiffUtil.ItemCallback<NoteDto>() {
        override fun areItemsTheSame(a: NoteDto, b: NoteDto) = a.id == b.id
        override fun areContentsTheSame(a: NoteDto, b: NoteDto) = a == b
    }

    inner class VH(private val b: ItemNoteBinding) :
        RecyclerView.ViewHolder(b.root) {

        fun bind(note: NoteDto) {
            // Устанавливаем заголовок
            b.noteTitle.text = note.title

            // Устанавливаем иконку в зависимости от типа заметки
            when (note.type.lowercase()) {
                "task" -> {
                    b.typeIcon.text = "✅"
                    b.typeIcon.contentDescription = "Задача"
                }
                "note" -> {
                    b.typeIcon.text = "📝"
                    b.typeIcon.contentDescription = "Заметка"
                }
                else -> {
                    b.typeIcon.text = "📄"
                    b.typeIcon.contentDescription = "Заметка"
                }
            }

            // Устанавливаем предпросмотр содержания
            b.notePreview.text = getPreviewText(note)

            // Устанавливаем дату
            b.dateText.text = dateFormat.format(Date(note.updatedAt))

            // Устанавливаем счётчик слов или статус подзадач
            if (note.type.lowercase() == "task") {
                val (completed, total) = getSubtaskStats(note.content)
                b.wordCount.text = if (total > 0) {
                    "✓ $completed/$total"
                } else {
                    "Нет подзадач"
                }
            } else {
                val wordCount = countWords(note.content)
                b.wordCount.text = formatWordCount(wordCount)
            }

            // Обработчики нажатий
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

        private fun getPreviewText(note: NoteDto): String {
            return if (note.type.lowercase() == "task") {
                // Для задач показываем количество подзадач
                val (completed, total) = getSubtaskStats(note.content)
                if (total > 0) {
                    "📋 $completed из $total подзадач выполнено"
                } else {
                    "Нет подзадач"
                }
            } else {
                // Для заметок показываем первые 100 символов
                val cleanText = cleanHtml(note.content)
                if (cleanText.length > 100) {
                    cleanText.substring(0, 100) + "..."
                } else if (cleanText.isNotEmpty()) {
                    cleanText
                } else {
                    "Нет содержания"
                }
            }
        }

        private fun cleanHtml(html: String): String {
            return html
                .replace(Regex("<[^>]*>"), "") // Удаляем HTML теги
                .replace(Regex("[#*_>`~]"), "") // Удаляем маркдаун символы
                .replace(Regex("\\s+"), " ") // Заменяем множественные пробелы на один
                .trim()
        }

        private fun countWords(text: String): Int {
            val cleanText = cleanHtml(text)
            if (cleanText.isEmpty()) return 0
            return cleanText.split(Regex("\\s+")).size
        }

        private fun getSubtaskStats(content: String): Pair<Int, Int> {
            return try {
                val jsonArray = JSONArray(content)
                var completed = 0
                for (i in 0 until jsonArray.length()) {
                    val obj = jsonArray.getJSONObject(i)
                    if (obj.getBoolean("isCompleted")) {
                        completed++
                    }
                }
                Pair(completed, jsonArray.length())
            } catch (e: Exception) {
                Pair(0, 0)
            }
        }

        private fun formatWordCount(count: Int): String {
            return when {
                count == 0 -> "нет слов"
                count % 10 == 1 && count % 100 != 11 -> "$count слово"
                count % 10 in 2..4 && count % 100 !in 12..14 -> "$count слова"
                else -> "$count слов"
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