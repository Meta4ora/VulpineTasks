package com.example.vulpinetasks

import android.animation.ObjectAnimator
import android.app.AlertDialog
import android.content.BroadcastReceiver
import android.content.Context
import android.content.Intent
import android.content.IntentFilter
import android.os.Bundle
import android.view.Menu
import android.view.MenuItem
import android.view.animation.LinearInterpolator
import android.widget.EditText
import android.widget.TextView
import android.widget.Toast
import androidx.appcompat.app.AppCompatActivity
import androidx.appcompat.widget.SearchView
import androidx.appcompat.widget.Toolbar
import androidx.core.view.GravityCompat
import androidx.lifecycle.lifecycleScope
import androidx.localbroadcastmanager.content.LocalBroadcastManager
import androidx.recyclerview.widget.LinearLayoutManager
import com.example.vulpinetasks.backend.NoteDto
import com.example.vulpinetasks.backend.TokenManager
import com.example.vulpinetasks.databinding.ActivityMainBinding
import com.example.vulpinetasks.room.AppGraph
import com.example.vulpinetasks.util.NetworkUtil
import com.example.vulpinetasks.utils.SettingsManager
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.launch
import java.text.SimpleDateFormat
import java.util.*

class MainActivity : AppCompatActivity() {

    private lateinit var binding: ActivityMainBinding
    private lateinit var adapter: NotesAdapter
    private lateinit var tokenManager: TokenManager
    private lateinit var settingsManager: SettingsManager
    private var syncAnimator: ObjectAnimator? = null
    private var isSyncing = false
    private var settingsReceiver: BroadcastReceiver? = null

    private val repo get() = AppGraph.notesRepository
    private lateinit var userId: String

    private val searchQuery = MutableStateFlow("")
    private val sortType = MutableStateFlow(SortType.DATE_DESC)
    private val allNotes = mutableListOf<NoteDto>()

    enum class SortType {
        DATE_DESC, DATE_ASC, ALPHABETICAL_ASC, ALPHABETICAL_DESC
    }

    override fun onCreate(savedInstanceState: Bundle?) {
        settingsManager = SettingsManager(this)
        if (settingsManager.isDarkMode()) {
            setTheme(R.style.Theme_VulpineTasks_Dark)
        } else {
            setTheme(R.style.Theme_VulpineTasks)
        }

        super.onCreate(savedInstanceState)

        binding = ActivityMainBinding.inflate(layoutInflater)
        setContentView(binding.root)

        tokenManager = TokenManager(this)
        settingsManager = SettingsManager(this)
        userId = tokenManager.getUserId() ?: "guest_${System.currentTimeMillis()}"
        tokenManager.saveUserId(userId)

        setupRecycler()
        observeNotes()
        setupDrawer()
        setupSearchAndFilter()
        setupSyncButton()
        updateNavHeader()

        registerSettingsReceiver()

        if (!tokenManager.isGuest()) {
            lifecycleScope.launch {
                repo.clearDeletedRelations()
                performInitialSync()
            }
        } else {
            updateSyncIndicator(true)
        }

        binding.addNoteButton.setOnClickListener {
            showTypeSelectionDialog()
        }

        applyKeepScreenOnSetting()
    }

    override fun onResume() {
        super.onResume()
        updateNavHeader()
        if (!tokenManager.isGuest()) {
            lifecycleScope.launch {
                checkUnsyncedNotes()
            }
        }
        applyFiltersAndSort()
        applyKeepScreenOnSetting()
        applyCompactViewSetting()
    }

    override fun onDestroy() {
        super.onDestroy()
        syncAnimator?.cancel()
        settingsReceiver?.let { LocalBroadcastManager.getInstance(this).unregisterReceiver(it) }
    }

    private fun registerSettingsReceiver() {
        settingsReceiver = object : BroadcastReceiver() {
            override fun onReceive(context: Context?, intent: Intent?) {
                if (intent?.action == SettingsManager.ACTION_SETTINGS_CHANGED) {
                    applyFiltersAndSort()
                    applyCompactViewSetting()
                    applyKeepScreenOnSetting()
                    // Обновляем адаптер
                    adapter.notifyDataSetChanged()
                }
            }
        }
        LocalBroadcastManager.getInstance(this).registerReceiver(
            settingsReceiver!!,
            IntentFilter(SettingsManager.ACTION_SETTINGS_CHANGED)
        )
    }

    private fun applyKeepScreenOnSetting() {
        if (settingsManager.isKeepScreenOn()) {
            window.addFlags(android.view.WindowManager.LayoutParams.FLAG_KEEP_SCREEN_ON)
        } else {
            window.clearFlags(android.view.WindowManager.LayoutParams.FLAG_KEEP_SCREEN_ON)
        }
    }

    private fun applyCompactViewSetting() {
        val padding = if (settingsManager.isCompactViewEnabled()) 4 else 16
        binding.notesRecyclerView.setPadding(padding, padding, padding, padding)
    }

    private fun setupSyncButton() {
        binding.syncButton.setOnClickListener {
            performSync()
        }
    }

    private suspend fun performInitialSync() {
        try {
            startSyncAnimation()
            isSyncing = true
            repo.fetchFromServer(userId)
            repo.syncUnsyncedNotes(userId)
            checkUnsyncedNotes()
        } catch (e: Exception) {
            e.printStackTrace()
        } finally {
            isSyncing = false
            stopSyncAnimation()
        }
    }

    private fun performSync() {
        if (tokenManager.isGuest()) {
            showGuestSyncDialog()
            return
        }

        if (!NetworkUtil.isOnline(this)) {
            showNoInternetDialog()
            return
        }

        if (isSyncing) {
            Toast.makeText(this, "Синхронизация уже выполняется...", Toast.LENGTH_SHORT).show()
            return
        }

        lifecycleScope.launch {
            try {
                startSyncAnimation()
                isSyncing = true
                Toast.makeText(this@MainActivity, "Синхронизация началась...", Toast.LENGTH_SHORT).show()
                repo.syncUnsyncedNotes(userId)
                repo.fetchFromServer(userId)
                repo.syncLocalChangesToServer(userId)
                checkUnsyncedNotes()
                Toast.makeText(this@MainActivity, "Синхронизация завершена успешно", Toast.LENGTH_SHORT).show()
            } catch (e: Exception) {
                Toast.makeText(this@MainActivity, "Ошибка синхронизации: ${e.message}", Toast.LENGTH_SHORT).show()
                updateSyncIndicator(false)
            } finally {
                isSyncing = false
                stopSyncAnimation()
            }
        }
    }

    private fun startSyncAnimation() {
        syncAnimator = ObjectAnimator.ofFloat(binding.syncButton, "rotation", 0f, 360f).apply {
            duration = 1000
            repeatCount = ObjectAnimator.INFINITE
            interpolator = LinearInterpolator()
            start()
        }
    }

    private fun stopSyncAnimation() {
        syncAnimator?.cancel()
        binding.syncButton.rotation = 0f
    }

    private suspend fun checkUnsyncedNotes() {
        if (tokenManager.isGuest()) {
            updateSyncIndicator(true)
            return
        }
        try {
            val hasUnsynced = repo.hasUnsyncedNotes(userId)
            updateSyncIndicator(!hasUnsynced)
        } catch (e: Exception) {
            updateSyncIndicator(true)
        }
    }

    private fun updateSyncIndicator(isSynced: Boolean) {
        val indicator = binding.syncIndicator
        if (isSynced) {
            indicator.visibility = android.view.View.GONE
        } else {
            indicator.visibility = android.view.View.VISIBLE
            indicator.setBackgroundResource(R.drawable.sync_indicator_red)
        }
    }

    private fun updateNavHeader() {
        try {
            val headerView = binding.navView.getHeaderView(0)
            val userEmailView = headerView.findViewById<TextView>(R.id.nav_user_email)
            if (tokenManager.getToken() != null) {
                userEmailView.text = tokenManager.getEmail() ?: "user@example.com"
                userEmailView.visibility = android.view.View.VISIBLE
            } else if (tokenManager.isGuest()) {
                userEmailView.text = "Гостевой режим"
                userEmailView.visibility = android.view.View.VISIBLE
            } else {
                userEmailView.text = "Не авторизован"
                userEmailView.visibility = android.view.View.VISIBLE
            }
        } catch (e: Exception) {
            e.printStackTrace()
        }
    }

    private fun showGuestSyncDialog() {
        AlertDialog.Builder(this)
            .setTitle("Синхронизация недоступна")
            .setMessage("Синхронизация доступна только для зарегистрированных пользователей. Хотите войти или зарегистрироваться?")
            .setPositiveButton("Войти") { _, _ ->
                startActivity(Intent(this, LoginActivity::class.java))
            }
            .setNegativeButton("Отмена", null)
            .show()
    }

    private fun showNoInternetDialog() {
        AlertDialog.Builder(this)
            .setTitle("Нет подключения к интернету")
            .setMessage("Для синхронизации требуется подключение к интернету. Проверьте соединение и попробуйте снова.")
            .setPositiveButton("Попробовать снова") { _, _ ->
                performSync()
            }
            .setNegativeButton("Отмена", null)
            .show()
    }

    private fun setupRecycler() {
        adapter = NotesAdapter(
            onOpen = { note ->
                val intent = Intent(this, NoteEditorActivity::class.java)
                intent.putExtra("note_id", note.id)
                intent.putExtra("note_title", note.title)
                startActivity(intent)
            },
            onTrash = { note ->
                lifecycleScope.launch {
                    repo.moveToTrash(note.id)
                    toast("Перемещено в корзину")
                    checkUnsyncedNotes()
                }
            },
            onInfo = { note ->
                showNoteInfoDialog(note)
            },
            onRename = { note ->
                showRenameDialog(note)
            },
            settingsManager = settingsManager
        )

        binding.notesRecyclerView.layoutManager = LinearLayoutManager(this)
        binding.notesRecyclerView.adapter = adapter
        applyCompactViewSetting()
    }

    private fun observeNotes() {
        lifecycleScope.launch {
            repo.observeNotes(userId).collect { notes ->
                allNotes.clear()
                allNotes.addAll(notes)
                applyFiltersAndSort()
            }
        }
    }

    private fun applyFiltersAndSort() {
        lifecycleScope.launch {
            combine(searchQuery, sortType) { query, sort ->
                filterAndSortNotes(query, sort)
            }.collect { filteredNotes ->
                adapter.submitList(filteredNotes)
                updateEmptyView(filteredNotes.isEmpty())
            }
        }
    }

    private fun filterAndSortNotes(query: String, sort: SortType): List<NoteDto> {
        var filtered = allNotes.toList()
        if (query.isNotEmpty()) {
            filtered = filtered.filter { note ->
                note.title.contains(query, ignoreCase = true) ||
                        note.content.contains(query, ignoreCase = true)
            }
        }
        filtered = when (sort) {
            SortType.DATE_DESC -> filtered.sortedByDescending { it.updatedAt }
            SortType.DATE_ASC -> filtered.sortedBy { it.updatedAt }
            SortType.ALPHABETICAL_ASC -> filtered.sortedBy { it.title.lowercase() }
            SortType.ALPHABETICAL_DESC -> filtered.sortedByDescending { it.title.lowercase() }
        }
        return filtered
    }

    private fun setupSearchAndFilter() {
        val toolbar = findViewById<Toolbar>(R.id.toolbar)
        setSupportActionBar(toolbar)
        supportActionBar?.setDisplayShowTitleEnabled(true)
    }

    override fun onCreateOptionsMenu(menu: Menu): Boolean {
        menuInflater.inflate(R.menu.main_menu, menu)
        val searchItem = menu.findItem(R.id.action_search)
        val searchView = searchItem.actionView as SearchView
        searchView.queryHint = "Поиск по заголовку или содержанию"
        searchView.setOnQueryTextListener(object : SearchView.OnQueryTextListener {
            override fun onQueryTextSubmit(query: String): Boolean {
                searchQuery.value = query
                return true
            }
            override fun onQueryTextChange(newText: String): Boolean {
                searchQuery.value = newText
                return true
            }
        })
        searchItem.setOnActionExpandListener(object : MenuItem.OnActionExpandListener {
            override fun onMenuItemActionExpand(item: MenuItem): Boolean = true
            override fun onMenuItemActionCollapse(item: MenuItem): Boolean {
                searchQuery.value = ""
                return true
            }
        })
        return true
    }

    override fun onOptionsItemSelected(item: MenuItem): Boolean {
        return when (item.itemId) {
            R.id.action_filter -> {
                showFilterDialog()
                true
            }
            else -> super.onOptionsItemSelected(item)
        }
    }

    private fun showFilterDialog() {
        val sortOptions = arrayOf("📅 Сначала новые", "📅 Сначала старые", "🔤 По алфавиту (А-Я)", "🔤 По алфавиту (Я-А)")
        val currentSelection = when (sortType.value) {
            SortType.DATE_DESC -> 0
            SortType.DATE_ASC -> 1
            SortType.ALPHABETICAL_ASC -> 2
            SortType.ALPHABETICAL_DESC -> 3
        }
        AlertDialog.Builder(this)
            .setTitle("Сортировка заметок")
            .setSingleChoiceItems(sortOptions, currentSelection) { _, which ->
                sortType.value = when (which) {
                    0 -> SortType.DATE_DESC
                    1 -> SortType.DATE_ASC
                    2 -> SortType.ALPHABETICAL_ASC
                    3 -> SortType.ALPHABETICAL_DESC
                    else -> SortType.DATE_DESC
                }
            }
            .setPositiveButton("Применить") { dialog, _ ->
                dialog.dismiss()
                toast("Сортировка применена")
            }
            .setNegativeButton("Отмена", null)
            .show()
    }

    private fun updateEmptyView(isEmpty: Boolean) {
        if (isEmpty) {
            binding.emptyView.visibility = android.view.View.VISIBLE
            binding.emptyText.text = if (searchQuery.value.isNotEmpty()) "Ничего не найдено" else "Нет заметок"
        } else {
            binding.emptyView.visibility = android.view.View.GONE
        }
    }

    private fun setupDrawer() {
        binding.menuButton.setOnClickListener {
            binding.drawerLayout.openDrawer(GravityCompat.START)
        }
        binding.navView.setNavigationItemSelectedListener { item ->
            when (item.itemId) {
                R.id.nav_login -> startActivity(Intent(this, LoginActivity::class.java))
                R.id.nav_trash -> startActivity(Intent(this, TrashActivity::class.java))
                R.id.nav_settings -> startActivity(Intent(this, SettingsActivity::class.java))
            }
            binding.drawerLayout.closeDrawer(GravityCompat.START)
            true
        }
    }

    private fun showTypeSelectionDialog() {
        val options = arrayOf("📝 Заметка", "✅ Задача")
        AlertDialog.Builder(this)
            .setTitle("Создать")
            .setItems(options) { _, which ->
                when (which) {
                    0 -> showCreateNoteDialog("note")
                    1 -> showCreateNoteDialog("task")
                }
            }
            .setNegativeButton("Отмена", null)
            .show()
    }

    private fun showCreateNoteDialog(type: String) {
        val input = EditText(this)
        input.hint = if (type == "note") "Введите заголовок заметки" else "Введите название задачи"
        val defaultTitle = settingsManager.getDefaultNoteTitle()
        input.setText(defaultTitle)
        input.selectAll()

        AlertDialog.Builder(this)
            .setTitle(if (type == "note") "Новая заметка" else "Новая задача")
            .setView(input)
            .setPositiveButton("Создать") { _, _ ->
                val text = input.text.toString().trim()
                if (text.isEmpty()) {
                    toast("Заголовок не может быть пустым")
                    return@setPositiveButton
                }
                lifecycleScope.launch {
                    try {
                        val defaultContent = if (type == "note") "# $text\n\n" else "[]"
                        repo.createNote(title = text, type = type, userId = userId, isOnline = NetworkUtil.isOnline(this@MainActivity))
                        kotlinx.coroutines.delay(100)
                        val createdNote = getNoteByTitle(text)
                        if (createdNote != null) {
                            repo.updateNoteContent(createdNote.id, userId, defaultContent)
                            toast(if (type == "note") "Заметка создана" else "Задача создана")
                        } else {
                            toast(if (type == "note") "Заметка создана" else "Задача создана")
                        }
                        if (!tokenManager.isGuest() && NetworkUtil.isOnline(this@MainActivity)) {
                            checkUnsyncedNotes()
                        }
                    } catch (e: Exception) {
                        toast("Ошибка при создании")
                        e.printStackTrace()
                    }
                }
            }
            .setNegativeButton("Отмена", null)
            .show()
    }

    private suspend fun getNoteByTitle(title: String): NoteDto? {
        val notes = repo.getAllNotes(userId)
        return notes.find { it.title == title }
    }

    private fun showRenameDialog(note: NoteDto) {
        val input = EditText(this)
        input.hint = "Введите новый заголовок"
        input.setText(note.title)
        input.selectAll()

        AlertDialog.Builder(this)
            .setTitle("Переименовать заметку")
            .setMessage("Введите новое название")
            .setView(input)
            .setPositiveButton("Сохранить") { _, _ ->
                val newTitle = input.text.toString().trim()
                if (newTitle.isEmpty()) {
                    toast("Название не может быть пустым")
                    return@setPositiveButton
                }
                if (newTitle == note.title) {
                    toast("Название не изменено")
                    return@setPositiveButton
                }
                lifecycleScope.launch {
                    try {
                        repo.updateNoteTitle(note.id, newTitle)
                        toast("Название изменено на: $newTitle")
                        if (!tokenManager.isGuest() && NetworkUtil.isOnline(this@MainActivity)) {
                            checkUnsyncedNotes()
                        }
                    } catch (e: Exception) {
                        toast("Ошибка при переименовании")
                        e.printStackTrace()
                    }
                }
            }
            .setNegativeButton("Отмена", null)
            .show()
    }

    private fun showNoteInfoDialog(note: NoteDto) {
        val dialogView = layoutInflater.inflate(R.layout.dialog_note_info, null)
        val titleText = dialogView.findViewById<TextView>(R.id.info_title)
        val createdAtText = dialogView.findViewById<TextView>(R.id.info_created_at)
        val updatedAtText = dialogView.findViewById<TextView>(R.id.info_updated_at)
        val childCountText = dialogView.findViewById<TextView>(R.id.info_child_count)
        val parentCountText = dialogView.findViewById<TextView>(R.id.info_parent_count)
        val dateFormat = SimpleDateFormat("dd.MM.yyyy HH:mm:ss", Locale.getDefault())

        titleText.text = note.title
        createdAtText.text = dateFormat.format(Date(note.createdAt))
        updatedAtText.text = dateFormat.format(Date(note.updatedAt))

        val dialog = AlertDialog.Builder(this)
            .setTitle("Информация о заметке")
            .setView(dialogView)
            .setPositiveButton("Закрыть", null)
            .create()
        dialog.show()

        lifecycleScope.launch {
            try {
                val childCount = repo.getChildNotesIds(note.id).size
                val parentCount = repo.getParentIdsForNoteInfo(note.id).size
                childCountText.text = childCount.toString()
                parentCountText.text = parentCount.toString()
            } catch (e: Exception) {
                childCountText.text = "0"
                parentCountText.text = "0"
            }
        }
    }

    private fun toast(msg: String) {
        Toast.makeText(this, msg, Toast.LENGTH_SHORT).show()
    }
}