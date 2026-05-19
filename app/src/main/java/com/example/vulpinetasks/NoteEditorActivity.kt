package com.example.vulpinetasks

import android.content.Intent
import android.graphics.Rect
import android.os.Bundle
import android.os.Handler
import android.os.Looper
import android.util.Log
import android.view.View
import android.view.ViewTreeObserver
import android.webkit.JavascriptInterface
import android.webkit.WebChromeClient
import android.webkit.WebView
import android.webkit.WebViewClient
import android.widget.EditText
import android.widget.Toast
import androidx.appcompat.app.AlertDialog
import androidx.appcompat.app.AppCompatActivity
import androidx.coordinatorlayout.widget.CoordinatorLayout
import androidx.lifecycle.lifecycleScope
import androidx.recyclerview.widget.LinearLayoutManager
import com.example.vulpinetasks.backend.NoteDto
import com.example.vulpinetasks.backend.SubTaskDto
import com.example.vulpinetasks.backend.TokenManager
import com.example.vulpinetasks.databinding.ActivityNoteEditorBinding
import com.example.vulpinetasks.room.AppGraph
import kotlinx.coroutines.Job
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch
import org.json.JSONArray
import org.json.JSONObject

class NoteEditorActivity : AppCompatActivity() {

    private lateinit var binding: ActivityNoteEditorBinding
    private lateinit var tokenManager: TokenManager
    private var noteId: String? = null
    private var userId: String? = null
    private var originalContent: String = ""
    private var noteTitle: String = ""
    private var noteType: String = "note"
    private var childNotesAdapter: ChildNotesAdapter? = null
    private var subtasksAdapter: SubtasksAdapter? = null
    private var subtasks = mutableListOf<SubTaskDto>()
    private var isSaving = false
    private var saveJob: Job? = null
    private var currentHtml = ""
    private var isContentLoaded = false
    private val mainHandler = Handler(Looper.getMainLooper())

    companion object {
        private const val TAG = "NOTE_EDITOR"
        private const val SAVE_DELAY_MS = 1000L
    }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        binding = ActivityNoteEditorBinding.inflate(layoutInflater)
        setContentView(binding.root)

        tokenManager = TokenManager(this)
        noteId = intent.getStringExtra("note_id")
        noteTitle = intent.getStringExtra("note_title") ?: "Заметка"
        userId = tokenManager.getUserId()

        setupWebView()
        loadNoteType()
        setupToolbar()
        loadContent()
        setupChildNotesSection()
        loadChildNotes()
        setupSaveButton()
        setupAddChildNoteButton()
        setupFormatButtons()
        setupKeyboardListener()
    }

    private fun setupWebView() {
        binding.noteWebview.apply {
            settings.javaScriptEnabled = true
            settings.useWideViewPort = true
            settings.loadWithOverviewMode = true
            settings.setSupportZoom(true)
            settings.builtInZoomControls = true
            settings.displayZoomControls = false
            settings.domStorageEnabled = true

            addJavascriptInterface(WebAppInterface(), "Android")

            webViewClient = object : WebViewClient() {
                override fun onPageFinished(view: WebView?, url: String?) {
                    super.onPageFinished(view, url)
                    view?.loadUrl("javascript:document.body.contentEditable = true;")
                    view?.loadUrl("javascript:document.body.focus();")
                    isContentLoaded = true
                }
            }

            webChromeClient = WebChromeClient()

            isFocusable = true
            isFocusableInTouchMode = true
            requestFocus()
        }
    }

    private fun setupKeyboardListener() {
        binding.root.viewTreeObserver.addOnGlobalLayoutListener(object : ViewTreeObserver.OnGlobalLayoutListener {
            private var lastVisibleHeight = 0

            override fun onGlobalLayout() {
                val rect = Rect()
                binding.root.getWindowVisibleDisplayFrame(rect)
                val screenHeight = binding.root.height
                val keypadHeight = screenHeight - rect.bottom

                if (keypadHeight > screenHeight * 0.15) {
                    // Клавиатура открыта
                    val keyboardHeight = keypadHeight

                    // Добавляем отступ снизу для клавиатуры (24dp)
                    val adjustedKeyboardHeight = keyboardHeight + 24

                    // Поднимаем FAB над клавиатурой
                    val fabParams = binding.fabSave.layoutParams as CoordinatorLayout.LayoutParams
                    fabParams.bottomMargin = adjustedKeyboardHeight + 180
                    binding.fabSave.layoutParams = fabParams

                    // Поднимаем панель форматирования над клавиатурой
                    val panelParams = binding.formattingPanelContainer.layoutParams as CoordinatorLayout.LayoutParams
                    panelParams.bottomMargin = adjustedKeyboardHeight + 8
                    binding.formattingPanelContainer.layoutParams = panelParams

                    // Добавляем отступ снизу для ScrollView
                    binding.scrollView.setPadding(0, 0, 0, adjustedKeyboardHeight + 120)

                    lastVisibleHeight = adjustedKeyboardHeight
                } else if (lastVisibleHeight > 0) {
                    // Клавиатура закрыта - возвращаем на место
                    val fabParams = binding.fabSave.layoutParams as CoordinatorLayout.LayoutParams
                    fabParams.bottomMargin = 180  // Высоко поднята по умолчанию
                    binding.fabSave.layoutParams = fabParams

                    val panelParams = binding.formattingPanelContainer.layoutParams as CoordinatorLayout.LayoutParams
                    panelParams.bottomMargin = 24  // Отступ снизу для панели
                    binding.formattingPanelContainer.layoutParams = panelParams

                    binding.scrollView.setPadding(0, 0, 0, 0)

                    lastVisibleHeight = 0
                }
            }
        })
    }

    inner class WebAppInterface {
        @JavascriptInterface
        fun setContent(html: String) {
            mainHandler.post {
                currentHtml = html
                Log.d(TAG, "Content updated from WebView, length: ${html.length}")
            }
        }
    }

    private fun loadNoteType() {
        lifecycleScope.launch {
            val note = AppGraph.notesRepository.getNoteByIdRaw(noteId ?: return@launch)
            noteType = note?.type ?: "note"

            if (noteType == "task") {
                showTaskEditor()
            } else {
                showNoteEditor()
            }
        }
    }

    private fun showNoteEditor() {
        binding.noteEditorContainer.visibility = View.VISIBLE
        binding.taskEditorContainer.visibility = View.GONE
        binding.formattingPanelContainer.visibility = View.VISIBLE
    }

    private fun showTaskEditor() {
        binding.noteEditorContainer.visibility = View.GONE
        binding.taskEditorContainer.visibility = View.VISIBLE
        binding.formattingPanelContainer.visibility = View.GONE
        setupSubtasksRecyclerView()
        setupAddSubtaskButton()
    }

    private fun setupFormatButtons() {
        binding.btnUndo.setOnClickListener { undo() }
        binding.btnRedo.setOnClickListener { redo() }

        binding.btnBold.setOnClickListener {
            execJs("document.execCommand('bold', false, null);")
            scheduleAutoSaveForNote()
        }
        binding.btnItalic.setOnClickListener {
            execJs("document.execCommand('italic', false, null);")
            scheduleAutoSaveForNote()
        }
        binding.btnUnderline.setOnClickListener {
            execJs("document.execCommand('underline', false, null);")
            scheduleAutoSaveForNote()
        }
        binding.btnBulletList.setOnClickListener {
            execJs("document.execCommand('insertUnorderedList', false, null);")
            scheduleAutoSaveForNote()
        }
        binding.btnNumberList.setOnClickListener {
            execJs("document.execCommand('insertOrderedList', false, null);")
            scheduleAutoSaveForNote()
        }
        binding.btnHeading.setOnClickListener {
            execJs("document.execCommand('formatBlock', false, '<h3>');")
            scheduleAutoSaveForNote()
        }
        binding.btnTable.setOnClickListener { showTableDialog() }
    }

    private fun undo() {
        execJs("document.execCommand('undo', false, null);")
        scheduleAutoSaveForNote()
    }

    private fun redo() {
        execJs("document.execCommand('redo', false, null);")
        scheduleAutoSaveForNote()
    }

    private fun execJs(js: String) {
        binding.noteWebview.loadUrl("javascript:$js")
    }

    private fun scheduleAutoSaveForNote() {
        if (noteType != "task" && isContentLoaded) {
            saveJob?.cancel()
            saveJob = lifecycleScope.launch {
                delay(SAVE_DELAY_MS)
                getCurrentHtmlFromWebView()
            }
        }
    }

    private fun getCurrentHtmlFromWebView() {
        binding.noteWebview.loadUrl("javascript:Android.setContent(document.body.innerHTML);")
        mainHandler.postDelayed({
            if (currentHtml != originalContent && currentHtml.isNotBlank()) {
                lifecycleScope.launch {
                    saveContentToDatabase(currentHtml)
                }
            }
        }, 200)
    }

    private fun insertTableHtml(rows: Int, cols: Int) {
        val tableHtml = buildStyledTable(rows, cols)
        val js = "javascript:(function() {" +
                "var sel = window.getSelection();" +
                "if (sel.rangeCount > 0) {" +
                "var range = sel.getRangeAt(0);" +
                "var div = document.createElement('div');" +
                "div.innerHTML = `$tableHtml`;" +
                "range.deleteContents();" +
                "range.insertNode(div);" +
                "}" +
                "})()"
        binding.noteWebview.loadUrl(js)
        scheduleAutoSaveForNote()
    }

    private fun buildStyledTable(rows: Int, cols: Int): String {
        val sb = StringBuilder()
        sb.append("<div style='overflow-x: auto; margin: 8px 0;'>")
        sb.append("<table style='border-collapse: collapse; width: 100%; font-family: sans-serif; border: 1px solid #ddd;'>")

        sb.append("<thead>")
        sb.append("<tr style='background-color: #2196F3;'>")
        for (c in 0 until cols) {
            sb.append("<th style='border: 1px solid #ddd; padding: 8px; text-align: left; color: white;'>Заголовок ${c + 1}</th>")
        }
        sb.append("<tr>")
        sb.append("</thead>")

        sb.append("<tbody>")
        for (r in 0 until rows - 1) {
            sb.append("<tr>")
            for (c in 0 until cols) {
                sb.append("<td style='border: 1px solid #ddd; padding: 8px;'>Ячейка ${c + 1}</td>")
            }
            sb.append("</tr>")
        }
        sb.append("</tbody>")

        sb.append("</table>")
        sb.append("</div>")
        return sb.toString()
    }

    private fun showTableDialog() {
        val builder = AlertDialog.Builder(this)
        val view = layoutInflater.inflate(R.layout.dialog_table_config, null)

        val seekBarRows = view.findViewById<android.widget.SeekBar>(R.id.seekBarRows)
        val seekBarCols = view.findViewById<android.widget.SeekBar>(R.id.seekBarCols)
        val tvRows = view.findViewById<android.widget.TextView>(R.id.tvRows)
        val tvCols = view.findViewById<android.widget.TextView>(R.id.tvCols)

        seekBarRows.max = 6
        seekBarCols.max = 5

        var rows = 2
        var cols = 2

        seekBarRows.setOnSeekBarChangeListener(object : android.widget.SeekBar.OnSeekBarChangeListener {
            override fun onProgressChanged(seekBar: android.widget.SeekBar?, progress: Int, fromUser: Boolean) {
                rows = if (progress < 2) 2 else progress
                tvRows.text = "Строк: $rows"
                seekBar?.progress = rows
            }
            override fun onStartTrackingTouch(seekBar: android.widget.SeekBar?) {}
            override fun onStopTrackingTouch(seekBar: android.widget.SeekBar?) {}
        })

        seekBarCols.setOnSeekBarChangeListener(object : android.widget.SeekBar.OnSeekBarChangeListener {
            override fun onProgressChanged(seekBar: android.widget.SeekBar?, progress: Int, fromUser: Boolean) {
                cols = if (progress < 2) 2 else progress
                tvCols.text = "Столбцов: $cols"
                seekBar?.progress = cols
            }
            override fun onStartTrackingTouch(seekBar: android.widget.SeekBar?) {}
            override fun onStopTrackingTouch(seekBar: android.widget.SeekBar?) {}
        })

        builder.setTitle("Вставить таблицу")
            .setView(view)
            .setPositiveButton("Вставить") { _, _ ->
                insertTableHtml(rows, cols)
            }
            .setNegativeButton("Отмена", null)
            .show()
    }

    private fun setupSubtasksRecyclerView() {
        subtasksAdapter = SubtasksAdapter(
            subtasks = subtasks,
            onToggleComplete = { position, isChecked ->
                subtasks[position] = subtasks[position].copy(isCompleted = isChecked)
                subtasksAdapter?.updateList(subtasks)
                scheduleAutoSave()
            },
            onDeleteSubtask = { position ->
                subtasks.removeAt(position)
                subtasksAdapter?.updateList(subtasks)
                scheduleAutoSave()
            }
        )

        binding.subtasksRecyclerView.layoutManager = LinearLayoutManager(this)
        binding.subtasksRecyclerView.adapter = subtasksAdapter
    }

    private fun setupAddSubtaskButton() {
        binding.btnAddSubtask.setOnClickListener {
            showAddSubtaskDialog()
        }
    }

    private fun showAddSubtaskDialog() {
        val input = EditText(this)
        input.hint = "Название подзадачи"

        AlertDialog.Builder(this)
            .setTitle("Добавить подзадачу")
            .setView(input)
            .setPositiveButton("Добавить") { _, _ ->
                val title = input.text.toString().trim()
                if (title.isNotEmpty()) {
                    val newSubtask = SubTaskDto(
                        id = System.currentTimeMillis().toString(),
                        title = title,
                        isCompleted = false
                    )
                    subtasks.add(newSubtask)
                    subtasksAdapter?.updateList(subtasks)
                    scheduleAutoSave()
                }
            }
            .setNegativeButton("Отмена", null)
            .show()
    }

    private fun setupToolbar() {
        setSupportActionBar(binding.toolbar)
        supportActionBar?.setDisplayHomeAsUpEnabled(true)
        supportActionBar?.title = noteTitle
        binding.toolbar.setNavigationOnClickListener {
            saveAndClose()
        }
    }

    private fun setupChildNotesSection() {
        childNotesAdapter = ChildNotesAdapter(
            onNoteClick = { childNote ->
                val intent = Intent(this, NoteEditorActivity::class.java)
                intent.putExtra("note_id", childNote.id)
                intent.putExtra("note_title", childNote.title)
                startActivity(intent)
            },
            onUnlinkClick = { childNote ->
                lifecycleScope.launch {
                    if (noteId != null) {
                        AppGraph.notesRepository.removeParentRelation(childNote.id, noteId!!)
                        loadChildNotes()
                        Toast.makeText(
                            this@NoteEditorActivity,
                            "Заметка отвязана",
                            Toast.LENGTH_SHORT
                        ).show()
                    }
                }
            }
        )

        binding.childNotesRecyclerView.apply {
            layoutManager = LinearLayoutManager(
                this@NoteEditorActivity,
                LinearLayoutManager.HORIZONTAL,
                false
            )
            adapter = childNotesAdapter
            childNotesAdapter?.submitList(emptyList())
        }
    }

    private fun loadChildNotes() {
        lifecycleScope.launch {
            if (noteId != null && userId != null) {
                val childNotes = AppGraph.notesRepository.getChildNotes(noteId!!, userId!!)
                childNotesAdapter?.submitList(childNotes)
                binding.tagsSection.visibility = View.VISIBLE
                binding.tagsTitle.text = if (childNotes.isEmpty()) {
                    "Вложенные заметки (0)"
                } else {
                    "Вложенные заметки (${childNotes.size})"
                }
            }
        }
    }

    private fun setupAddChildNoteButton() {
        binding.addTagButton.setOnClickListener {
            showAddChildNoteDialog()
        }
    }

    private fun showAddChildNoteDialog() {
        lifecycleScope.launch {
            val currentNoteId = noteId ?: return@launch
            val currentUserId = userId ?: return@launch

            val allNotes = AppGraph.notesRepository.getAllNotes(currentUserId)
            val existingChildIds = AppGraph.notesRepository.getChildNotesIds(currentNoteId)

            val availableNotes = allNotes.filter { note ->
                note.id != currentNoteId && note.id !in existingChildIds
            }

            if (availableNotes.isEmpty()) {
                Toast.makeText(
                    this@NoteEditorActivity,
                    "Нет доступных заметок для добавления",
                    Toast.LENGTH_LONG
                ).show()
                return@launch
            }

            val titles = availableNotes.map { it.title }.toTypedArray()

            AlertDialog.Builder(this@NoteEditorActivity)
                .setTitle("Выберите заметку")
                .setItems(titles) { _, which ->
                    val selectedNote = availableNotes[which]
                    lifecycleScope.launch {
                        AppGraph.notesRepository.addParentRelation(selectedNote.id, currentNoteId)
                        loadChildNotes()
                        Toast.makeText(
                            this@NoteEditorActivity,
                            "Заметка добавлена: ${selectedNote.title}",
                            Toast.LENGTH_SHORT
                        ).show()
                    }
                }
                .setNegativeButton("Отмена", null)
                .show()
        }
    }

    private fun loadContent() {
        lifecycleScope.launch {
            if (noteId != null && userId != null) {
                showLoading(true)
                try {
                    val content = AppGraph.notesRepository.getNoteContent(noteId!!, userId!!)
                    originalContent = content
                    currentHtml = content

                    if (noteType == "task") {
                        parseSubtasksFromJson(content)
                        subtasksAdapter?.updateList(subtasks)
                    } else {
                        val fullHtml = """
                            <!DOCTYPE html>
                            <html>
                            <head>
                                <meta name="viewport" content="width=device-width, initial-scale=1.0, user-scalable=yes">
                                <style>
                                    * { box-sizing: border-box; }
                                    body {
                                        font-family: sans-serif;
                                        font-size: 16px;
                                        line-height: 1.5;
                                        padding: 16px;
                                        margin: 0;
                                        background: white;
                                        color: #212121;
                                        min-height: 100%;
                                    }
                                    h1 { font-size: 28px; margin: 16px 0 8px; }
                                    h2 { font-size: 24px; margin: 14px 0 8px; }
                                    h3 { font-size: 20px; margin: 12px 0 8px; }
                                    p { margin: 8px 0; }
                                    ul, ol { margin: 8px 0; padding-left: 24px; }
                                    li { margin: 4px 0; }
                                    table {
                                        border-collapse: collapse;
                                        width: 100%;
                                        margin: 12px 0;
                                    }
                                    th, td {
                                        border: 1px solid #ddd;
                                        padding: 8px;
                                        text-align: left;
                                    }
                                    th {
                                        background-color: #2196F3;
                                        color: white;
                                    }
                                    [contenteditable="true"]:focus {
                                        outline: none;
                                    }
                                </style>
                            </head>
                            <body contenteditable="true">
                                $content
                            </body>
                            </html>
                        """
                        binding.noteWebview.loadDataWithBaseURL(null, fullHtml, "text/html", "UTF-8", null)
                    }
                } catch (e: Exception) {
                    Log.e(TAG, "Error loading content", e)
                    if (noteType == "task") {
                        subtasks.clear()
                        subtasksAdapter?.updateList(subtasks)
                    } else {
                        val defaultHtml = """
                            <!DOCTYPE html>
                            <html>
                            <head>
                                <meta name="viewport" content="width=device-width, initial-scale=1.0">
                                <style>
                                    body { font-family: sans-serif; padding: 16px; margin: 0; background: white; }
                                    h1 { color: #2196F3; }
                                </style>
                            </head>
                            <body contenteditable="true">
                                <h1>$noteTitle</h1>
                                <p></p>
                            </body>
                            </html>
                        """
                        binding.noteWebview.loadDataWithBaseURL(null, defaultHtml, "text/html", "UTF-8", null)
                    }
                } finally {
                    showLoading(false)
                    isContentLoaded = true
                }
            }
        }
    }

    private fun parseSubtasksFromJson(json: String) {
        try {
            val jsonArray = JSONArray(json)
            subtasks.clear()
            for (i in 0 until jsonArray.length()) {
                val obj = jsonArray.getJSONObject(i)
                subtasks.add(
                    SubTaskDto(
                        id = obj.getString("id"),
                        title = obj.getString("title"),
                        isCompleted = obj.getBoolean("isCompleted")
                    )
                )
            }
        } catch (e: Exception) {
            Log.e(TAG, "Error parsing subtasks JSON", e)
            subtasks.clear()
        }
    }

    private fun saveSubtasksToJson(): String {
        val jsonArray = JSONArray()
        subtasks.forEach {
            val obj = JSONObject()
            obj.put("id", it.id)
            obj.put("title", it.title)
            obj.put("isCompleted", it.isCompleted)
            jsonArray.put(obj)
        }
        return jsonArray.toString()
    }

    private suspend fun saveContentToDatabase(newContent: String) {
        if (isSaving || newContent == originalContent) return

        isSaving = true
        try {
            if (noteId != null && userId != null) {
                AppGraph.notesRepository.updateNoteContent(noteId!!, userId!!, newContent)
                originalContent = newContent
                Log.d(TAG, "Content saved successfully")
            }
        } catch (e: Exception) {
            Log.e(TAG, "Error saving content", e)
        } finally {
            isSaving = false
        }
    }

    private fun scheduleAutoSave() {
        if (noteType != "task") return

        saveJob?.cancel()
        saveJob = lifecycleScope.launch {
            delay(SAVE_DELAY_MS)
            autoSaveContent()
        }
    }

    private suspend fun autoSaveContent() {
        if (isSaving) return

        val newContent = if (noteType == "task") {
            saveSubtasksToJson()
        } else {
            currentHtml
        }

        if (newContent == originalContent) return

        isSaving = true
        try {
            if (noteId != null && userId != null) {
                AppGraph.notesRepository.updateNoteContent(noteId!!, userId!!, newContent)
                originalContent = newContent
            }
        } catch (e: Exception) {
            Log.e(TAG, "Auto-save failed", e)
        } finally {
            isSaving = false
        }
    }

    private fun setupSaveButton() {
        binding.fabSave.setOnClickListener {
            saveAndClose()
        }
    }

    private fun saveAndClose() {
        saveJob?.cancel()

        if (noteType == "task") {
            val newContent = saveSubtasksToJson()
            if (newContent != originalContent) {
                lifecycleScope.launch {
                    saveContentToDatabase(newContent)
                    finish()
                }
            } else {
                finish()
            }
        } else {
            binding.noteWebview.loadUrl("javascript:Android.setContent(document.body.innerHTML);")
            mainHandler.postDelayed({
                if (currentHtml != originalContent && currentHtml.isNotBlank()) {
                    lifecycleScope.launch {
                        saveContentToDatabase(currentHtml)
                        finish()
                    }
                } else {
                    finish()
                }
            }, 300)
        }
    }

    private fun showLoading(show: Boolean) {
        binding.progressBar.visibility = if (show) View.VISIBLE else View.GONE
        binding.fabSave.isEnabled = !show
    }

    override fun onPause() {
        super.onPause()
        if (noteType == "task") {
            saveJob?.cancel()
            lifecycleScope.launch { autoSaveContent() }
        } else {
            saveJob?.cancel()
            if (isContentLoaded) {
                binding.noteWebview.loadUrl("javascript:Android.setContent(document.body.innerHTML);")
                mainHandler.postDelayed({
                    if (currentHtml != originalContent && currentHtml.isNotBlank()) {
                        lifecycleScope.launch {
                            saveContentToDatabase(currentHtml)
                        }
                    }
                }, 200)
            }
        }
    }

    override fun onBackPressed() {
        saveAndClose()
    }
}