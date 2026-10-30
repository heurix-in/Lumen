package com.lumen.messages.ui

import android.Manifest
import android.app.Application
import android.app.role.RoleManager
import android.content.Context
import android.os.Build
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.setValue
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.viewModelScope
import com.lumen.messages.data.ContactEntry
import com.lumen.messages.data.Conversation
import com.lumen.messages.data.Message
import com.lumen.messages.data.SearchHit
import com.lumen.messages.data.SimInfo
import com.lumen.messages.data.ThreadHeader
import com.lumen.messages.lumen
import com.lumen.messages.notify.Notifier
import com.lumen.messages.sms.SmsSender
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.Job
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.collectLatest
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext

sealed interface Screen {
    val depth: Int

    data object List : Screen {
        override val depth = 0
    }

    data class Thread(val threadId: Long) : Screen {
        override val depth = 2
    }

    data class NewMessage(val nonce: Long = System.nanoTime()) : Screen {
        override val depth = 1
    }
}

class AppViewModel(app: Application) : AndroidViewModel(app) {

    private val repo = app.lumen.repository
    private val prefs = app.getSharedPreferences("lumen", Context.MODE_PRIVATE)

    // ------------------------------------------------------------ app state
    var screen by mutableStateOf<Screen>(Screen.List)
        private set
    var isDefaultSms by mutableStateOf(false)
        private set
    var hasSmsAccess by mutableStateOf(false)
        private set
    var hasContacts by mutableStateOf(false)
        private set
    var onboarded by mutableStateOf(prefs.getBoolean("onboarded", false))
        private set

    // ------------------------------------------------------------ list
    var conversations by mutableStateOf<List<Conversation>>(emptyList())
        private set
    var conversationsLoaded by mutableStateOf(false)
        private set
    var query by mutableStateOf("")
        private set
    var unreadOnly by mutableStateOf(false)
    var searchHits by mutableStateOf<List<SearchHit>>(emptyList())
        private set
    var selection by mutableStateOf<Set<Long>>(emptySet())
        private set

    /** The staggered list entrance only plays on the first launch of the session. */
    var listIntroPlayed = false

    // ------------------------------------------------------------ thread
    var header by mutableStateOf<ThreadHeader?>(null)
        private set
    var messages by mutableStateOf<List<Message>>(emptyList())
        private set
    var messagesLoaded by mutableStateOf(false)
        private set
    var draft by mutableStateOf("")
    var sims by mutableStateOf<List<SimInfo>>(emptyList())
        private set
    var selectedSubId by mutableStateOf(-1)

    // ------------------------------------------------------------ new message
    var contacts by mutableStateOf<List<ContactEntry>>(emptyList())
        private set
    var pendingBody: String? = null

    private val drafts = HashMap<Long, String>()
    private var observeJob: Job? = null
    private var searchJob: Job? = null

    init {
        refreshState()
    }

    fun refreshState() {
        val ctx = getApplication<Application>()
        val rm = ctx.getSystemService(RoleManager::class.java)
        isDefaultSms = runCatching { rm.isRoleHeld(RoleManager.ROLE_SMS) }.getOrDefault(false)
        hasSmsAccess = repo.has(Manifest.permission.READ_SMS)
        val contactsNow = repo.has(Manifest.permission.READ_CONTACTS)
        if (contactsNow != hasContacts) {
            hasContacts = contactsNow
            repo.contacts.invalidate()
            if (observeJob != null) restartObserving()
        }
        if (hasSmsAccess && observeJob == null) startObserving()
        viewModelScope.launch(Dispatchers.IO) {
            val list = repo.sims()
            val def = repo.defaultSmsSubId()
            withContext(Dispatchers.Main) {
                sims = list
                if (selectedSubId < 0 || list.none { it.subId == selectedSubId }) {
                    selectedSubId = list.firstOrNull { it.subId == def }?.subId ?: list.firstOrNull()?.subId ?: -1
                }
            }
        }
    }

    fun requiredPermissions(): Array<String> = buildList {
        add(Manifest.permission.READ_SMS)
        add(Manifest.permission.SEND_SMS)
        add(Manifest.permission.RECEIVE_SMS)
        add(Manifest.permission.READ_CONTACTS)
        add(Manifest.permission.READ_PHONE_STATE)
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) add(Manifest.permission.POST_NOTIFICATIONS)
    }.toTypedArray()

    fun finishOnboarding() {
        onboarded = true
        prefs.edit().putBoolean("onboarded", true).apply()
    }

    private fun startObserving() {
        observeJob = viewModelScope.launch {
            repo.observe().collectLatest {
                delay(120) // coalesce bursts (e.g. multipart sends) into one refresh
                reload()
            }
        }
    }

    private fun restartObserving() {
        observeJob?.cancel()
        observeJob = null
        startObserving()
    }

    private suspend fun reload() {
        val openThread = (screen as? Screen.Thread)?.threadId
        val (list, msgs) = withContext(Dispatchers.IO) {
            repo.loadConversations() to openThread?.let { repo.loadMessages(it) }
        }
        conversations = list
        conversationsLoaded = true
        if (openThread != null && msgs != null && (screen as? Screen.Thread)?.threadId == openThread) {
            messages = msgs
            messagesLoaded = true
            if (msgs.any { !it.isMine && !it.read }) {
                withContext(Dispatchers.IO) { repo.markRead(openThread) }
            }
        }
        if (query.isNotBlank()) runSearch(query, immediate = true)
    }

    // ------------------------------------------------------------ navigation

    fun back(): Boolean {
        return when {
            selection.isNotEmpty() -> {
                selection = emptySet(); true
            }
            screen is Screen.Thread || screen is Screen.NewMessage -> {
                saveDraft()
                screen = Screen.List
                true
            }
            query.isNotEmpty() -> {
                onQuery(""); true
            }
            else -> false
        }
    }

    private fun saveDraft() {
        val h = header ?: return
        if (draft.isNotBlank()) drafts[h.threadId] = draft else drafts.remove(h.threadId)
    }

    fun openConversation(c: Conversation) {
        openThread(ThreadHeader(c.threadId, c.address, c.name, c.photoUri))
    }

    fun openThreadById(threadId: Long) {
        val known = conversations.firstOrNull { it.threadId == threadId }
        if (known != null) {
            openConversation(known)
            return
        }
        viewModelScope.launch {
            val h = withContext(Dispatchers.IO) { repo.headerFor(threadId) } ?: return@launch
            openThread(h)
        }
    }

    private fun openThread(h: ThreadHeader) {
        if ((screen as? Screen.Thread)?.threadId != h.threadId) {
            saveDraft()
            messages = emptyList()
            messagesLoaded = false
        }
        header = h
        draft = pendingBody ?: drafts[h.threadId] ?: ""
        pendingBody = null
        selection = emptySet()
        screen = Screen.Thread(h.threadId)
        viewModelScope.launch {
            val msgs = withContext(Dispatchers.IO) {
                repo.markRead(h.threadId)
                Notifier.cancel(getApplication(), h.threadId)
                repo.loadMessages(h.threadId)
            }
            if ((screen as? Screen.Thread)?.threadId == h.threadId) {
                messages = msgs
                messagesLoaded = true
                // Prefer the SIM this conversation last used.
                msgs.lastOrNull { it.subId >= 0 }?.subId?.let { sub ->
                    if (sims.any { it.subId == sub }) selectedSubId = sub
                }
            }
        }
    }

    fun openNewMessage(prefillBody: String? = null) {
        pendingBody = prefillBody
        selection = emptySet()
        screen = Screen.NewMessage()
        loadContacts()
    }

    fun startConversationWith(address: String, body: String? = null) {
        if (body != null) pendingBody = body
        val clean = address.trim()
        if (clean.isEmpty()) return
        viewModelScope.launch {
            val h = withContext(Dispatchers.IO) {
                val tid = repo.threadIdFor(clean)
                val r = repo.contacts.lookup(clean)
                if (tid > 0) ThreadHeader(tid, clean, r.name, r.photoUri) else null
            } ?: return@launch
            openThread(h)
        }
    }

    fun loadContacts() {
        viewModelScope.launch {
            contacts = withContext(Dispatchers.IO) { repo.contacts.allContacts() }
        }
    }

    // ------------------------------------------------------------ list actions

    fun onQuery(q: String) {
        query = q
        runSearch(q, immediate = false)
    }

    private fun runSearch(q: String, immediate: Boolean) {
        searchJob?.cancel()
        if (q.isBlank()) {
            searchHits = emptyList()
            return
        }
        searchJob = viewModelScope.launch {
            if (!immediate) delay(220)
            searchHits = withContext(Dispatchers.IO) { repo.search(q.trim()) }
        }
    }

    fun toggleSelect(threadId: Long) {
        selection = if (threadId in selection) selection - threadId else selection + threadId
    }

    fun clearSelection() {
        selection = emptySet()
    }

    fun markSelectedRead() {
        val ids = selection
        selection = emptySet()
        viewModelScope.launch(Dispatchers.IO) {
            ids.forEach {
                repo.markRead(it)
                Notifier.cancel(getApplication(), it)
            }
        }
    }

    fun deleteSelected() {
        val ids = selection
        selection = emptySet()
        conversations = conversations.filterNot { it.threadId in ids } // optimistic, animates out
        viewModelScope.launch(Dispatchers.IO) { repo.deleteThreads(ids) }
    }

    // ------------------------------------------------------------ thread actions

    fun send() {
        val h = header ?: return
        val text = draft.trim()
        if (text.isEmpty()) return
        draft = ""
        drafts.remove(h.threadId)
        val sub = selectedSubId
        viewModelScope.launch(Dispatchers.IO) {
            SmsSender.send(getApplication(), h.address, text, sub)
        }
    }

    fun retry(m: Message) {
        val h = header ?: return
        viewModelScope.launch(Dispatchers.IO) {
            repo.deleteMessage(m.id)
            SmsSender.send(getApplication(), h.address, m.body, if (m.subId >= 0) m.subId else selectedSubId)
        }
    }

    fun deleteMessage(m: Message) {
        messages = messages.filterNot { it.id == m.id }
        viewModelScope.launch(Dispatchers.IO) { repo.deleteMessage(m.id) }
    }

    fun cycleSim() {
        if (sims.size < 2) return
        val i = sims.indexOfFirst { it.subId == selectedSubId }
        selectedSubId = sims[(i + 1).mod(sims.size)].subId
    }
}
