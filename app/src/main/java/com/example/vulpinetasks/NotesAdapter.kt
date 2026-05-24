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
import com.example.vulpinetasks.utils.SettingsManager
import org.json.JSONArray
import java.text.SimpleDateFormat
import java.util.*

class NotesAdapter(
    private val onOpen: (NoteDto) -> Unit,
    private val onTrash: (NoteDto) -> Unit,
    private val onInfo: (NoteDto) -> Unit,
    private val onRename: (NoteDto) -> Unit,
    private val settingsManager: SettingsManager
) : ListAdapter<NoteDto, NotesAdapter.VH>(Diff) {

    private val dateFormat = SimpleDateFormat("dd.MM.yyyy", Locale.getDefault())

    object Diff : DiffUtil.ItemCallback<NoteDto>() {
        override fun areItemsTheSame(a: NoteDto, b: NoteDto) = a.id == b.id
        override fun areContentsTheSame(a: NoteDto, b: NoteDto) = a == b
    }

    inner class VH(private val b: ItemNoteBinding) :
        RecyclerView.ViewHolder(b.root) {

        fun bind(note: NoteDto) {
            b.noteTitle.text = note.title

            // Иконка в зависимости от типа заметки
            when (note.type.lowercase()) {
                "task" -> {
                    b.typeIcon.text = "✅"
                    b.typeIcon.contentDescription = "Задача"
                }
                else -> {
                    b.typeIcon.text = "📝"
                    b.typeIcon.contentDescription = "Заметка"
                }
            }

            // Предпросмотр (если включен в настройках)
            if (settingsManager.isPreviewEnabled()) {
                b.notePreview.visibility = View.VISIBLE
                b.notePreview.text = getPreviewText(note)
            } else {
                b.notePreview.visibility = View.GONE
            }

            // Дата
            b.dateText.text = dateFormat.format(Date(note.updatedAt))

            // Счётчик слов или подзадач (если включен в настройках)
            if (settingsManager.isWordCountEnabled()) {
                b.wordCount.visibility = View.VISIBLE
                if (note.type.lowercase() == "task") {
                    val (completed, total) = getSubtaskStats(note.content)
                    b.wordCount.text = if (total > 0) {
                        "✓ $completed/$total"
                    } else {
                        "Нет подзадач"
                    }
                } else {
                    b.wordCount.text = formatWordCount(countWords(note.content))
                }
            } else {
                b.wordCount.visibility = View.GONE
            }

            // Обработчик нажатия на карточку
            b.root.setOnClickListener {
                onOpen(note)
            }

            // Кнопка меню
            b.noteMenuButton.setOnClickListener { v: View ->
                val popup = PopupMenu(v.context, v)
                popup.menu.add("Переименовать")
                popup.menu.add("Информация")
                popup.menu.add("В корзину")

                popup.setOnMenuItemClickListener { menuItem ->
                    when (menuItem.title) {
                        "Переименовать" -> onRename(note)
                        "Информация" -> onInfo(note)
                        "В корзину" -> {
                            // Проверяем настройку подтверждения удаления
                            if (settingsManager.isConfirmDeleteEnabled()) {
                                showDeleteConfirmation(note)
                            } else {
                                onTrash(note)
                            }
                        }
                    }
                    true
                }
                popup.show()
            }
        }

        private fun showDeleteConfirmation(note: NoteDto) {
            val popupMenu = android.widget.PopupMenu(b.root.context, b.noteMenuButton)
            popupMenu.menu.add("Отмена")
            popupMenu.menu.add("Удалить")
            popupMenu.setOnMenuItemClickListener { item ->
                when (item.title) {
                    "Удалить" -> onTrash(note)
                }
                true
            }
            popupMenu.show()
        }

        private fun getPreviewText(note: NoteDto): String {
            return if (note.type.lowercase() == "task") {
                val (completed, total) = getSubtaskStats(note.content)
                if (total > 0) {
                    "📋 $completed из $total подзадач"
                } else {
                    "Нет подзадач"
                }
            } else {
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
                .replace(Regex("<[^>]*>"), "")
                .replace(Regex("[#*_>`~]"), "")
                .replace(Regex("\\s+"), " ")
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
                    if (jsonArray.getJSONObject(i).getBoolean("isCompleted")) {
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