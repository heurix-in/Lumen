package com.lumen.messages.ui

import android.Manifest
import android.app.Application
import android.app.role.RoleManager
import android.content.Context
import android.os.Build
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateListOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.setValue
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.viewModelScope
import com.lumen.messages.data.Category
import com.lumen.messages.data.ContactEntry
import com.lumen.messages.data.Conversation
import com.lumen.messages.data.Message
import com.lumen.messages.data.SearchHit
import com.lumen.messages.data.SimInfo
import com.lumen.messages.data.SmsRepository
import com.lumen.messages.data.ThreadHeader
import com.lumen.messages.data.Transaction
import com.lumen.messages.lumen
import com.lumen.messages.notify.Notifier
import com.lumen.messages.sms.SmsSender
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.Job
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.collectLatest
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import java.time.YearMonth

sealed interface Screen {
    val depth: Int

    data object List : Screen {
        override val depth = 0
    }

    data class NewMessage(val nonce: Long = System.nanoTime()) : Screen {
        override val depth = 1
    }

    data object Money : Screen {
        override val depth = 1
    }

    data object Settings : Screen {
        override val depth = 1
    }

    data class Thread(val threadId: Long) : Screen {
        override val depth = 2
    }
}

/** Tabs above the conversation list. */
enum class ListFilter(val label: String) {
    All("All"),
    Unread("Unread"),
    Personal("Personal"),
    Transactions("Transactions"),
    Otp("OTPs"),
    Updates("Updates"),
}

enum class MoneyGrouping(val label: String) { Date("By date"), Party("By payee"), Account("By account") }

class AppViewModel(app: Application) : AndroidViewModel(app) {

    private val repo = app.lumen.repository
    val prefs = app.lumen.prefs
    private val sp = app.getSharedPreferences("lumen", Context.MODE_PRIVATE)

    // ------------------------------------------------------------ navigation (simple back stack)
    private val stack = mutableStateListOf<Screen>(Screen.List)
    val screen: Screen get() = stack.last()

    private fun push(s: Screen) {
        val top = stack.last()
        when {
            top == s -> Unit
            top is Screen.Thread && s is Screen.Thread -> stack[stack.lastIndex] = s
            top is Screen.NewMessage && s is Screen.Thread -> stack[stack.lastIndex] = s
            else -> stack.add(s)
        }
    }

    // ------------------------------------------------------------ app state
    var isDefaultSms by mutableStateOf(false)
        private set
    var hasSmsAccess by mutableStateOf(false)
        private set
    var hasContacts by mutableStateOf(false)
        private set
    var onboarded by mutableStateOf(sp.getBoolean("onboarded", false))
        private set

    // ------------------------------------------------------------ list
    var conversations by mutableStateOf<List<Conversation>>(emptyList())
        private set
    var conversationsLoaded by mutableStateOf(false)
        private set
    var query by mutableStateOf("")
        private set
    var filter by mutableStateOf(ListFilter.All)
    var searchHits by mutableStateOf<List<SearchHit>>(emptyList())
        private set
    var selection by mutableStateOf<Set<Long>>(emptySet())
        private set

    // ------------------------------------------------------------ thread
    var header by mutableStateOf<ThreadHeader?>(null)
        private set
    var messages by mutableStateOf<List<Message>>(emptyList())
        private set
    var rows by mutableStateOf<List<ChatRow>>(emptyList())
        private set
    var messagesLoaded by mutableStateOf(false)
        private set
    var hasOlder by mutableStateOf(false)
        private set
    private var messageLimit = SmsRepository.PAGE
    private var loadingOlder = false
    var draft by mutableStateOf("")
    var sims by mutableStateOf<List<SimInfo>>(emptyList())
        private set
    var selectedSubId by mutableStateOf(-1)

    // ------------------------------------------------------------ new message
    var contacts by mutableStateOf<List<ContactEntry>>(emptyList())
        private set
    var pendingBody: String? = null

    // ------------------------------------------------------------ money
    var transactions by mutableStateOf<List<Transaction>>(emptyList())
        private set
    var transactionsLoaded by mutableStateOf(false)
        private set
    var moneyMonth by mutableStateOf(YearMonth.now())
    var moneyAccount by mutableStateOf<String?>(null)
    var moneyGrouping by mutableStateOf(MoneyGrouping.Date)

    private val drafts = HashMap<Long, String>()
    private var observeJob: Job? = null
    private var searchJob: Job? = null
    private var txJob: Job? = null

    init {
        // Show the last known list instantly, then refresh.
        viewModelScope.launch {
            val cached = withContext(Dispatchers.IO) { repo.loadCache() }
            if (!conversationsLoaded && cached.isNotEmpty()) conversations = cached
        }
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
        if (hasSmsAccess && observeJob == null) {
            startObserving()
            loadTransactions()
        }
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
        sp.edit().putBoolean("onboarded", true).apply()
    }

    private fun startObserving() {
        observeJob = viewModelScope.launch {
            repo.observe().collectLatest {
                delay(200) // coalesce bursts (multipart sends, mark-read) into one refresh
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
        val list = withContext(Dispatchers.IO) { repo.loadConversations() }
        conversations = list
        conversationsLoaded = true
        viewModelScope.launch(Dispatchers.IO) { repo.saveCache(list) }
        val open = (screen as? Screen.Thread)?.threadId
        if (open != null) loadThread(open, markRead = true)
        if (query.isNotBlank()) runSearch(query, immediate = true)
    }

    private suspend fun loadThread(threadId: Long, markRead: Boolean) {
        val (msgs, more) = withContext(Dispatchers.IO) { repo.loadMessages(threadId, messageLimit) }
        val built = withContext(Dispatchers.Default) { buildRows(msgs) }
        if ((screen as? Screen.Thread)?.threadId != threadId) return
        messages = msgs
        rows = built
        hasOlder = more
        messagesLoaded = true
        if (markRead && msgs.any { !it.isMine && !it.read }) {
            withContext(Dispatchers.IO) {
                repo.markRead(threadId)
                Notifier.cancel(getApplication(), threadId)
            }
        }
    }

    fun loadOlder() {
        val tid = (screen as? Screen.Thread)?.threadId ?: return
        if (!hasOlder || loadingOlder) return
        loadingOlder = true
        messageLimit += SmsRepository.PAGE
        viewModelScope.launch {
            loadThread(tid, markRead = false)
            loadingOlder = false
        }
    }

    // ------------------------------------------------------------ navigation

    fun back(): Boolean {
        if (selection.isNotEmpty()) {
            selection = emptySet(); return true
        }
        if (stack.size > 1) {
            if (screen is Screen.Thread) saveDraft()
            stack.removeAt(stack.lastIndex)
            return true
        }
        if (query.isNotEmpty()) {
            onQuery(""); return true
        }
        return false
    }

    private fun saveDraft() {
        val h = header ?: return
        if (draft.isNotBlank()) drafts[h.threadId] = draft else drafts.remove(h.threadId)
    }

    fun openMoney() {
        selection = emptySet()
        push(Screen.Money)
        if (!transactionsLoaded) loadTransactions()
    }

    fun openSettings() {
        selection = emptySet()
        push(Screen.Settings)
    }

    fun openConversation(c: Conversation) {
        openThread(ThreadHeader(c.threadId, c.address, c.name, c.photoUri))
    }

    fun openThreadById(threadId: Long, fromNotification: Boolean = false) {
        if (fromNotification) {
            while (stack.size > 1) stack.removeAt(stack.lastIndex)
        }
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
        val current = (screen as? Screen.Thread)?.threadId
        if (current != h.threadId) {
            if (current != null) saveDraft()
            messages = emptyList()
            rows = emptyList()
            messagesLoaded = false
            hasOlder = false
            messageLimit = SmsRepository.PAGE
        }
        header = h
        draft = pendingBody ?: drafts[h.threadId] ?: ""
        pendingBody = null
        selection = emptySet()
        push(Screen.Thread(h.threadId))
        viewModelScope.launch {
            loadThread(h.threadId, markRead = true)
            withContext(Dispatchers.IO) {
                repo.markRead(h.threadId)
                Notifier.cancel(getApplication(), h.threadId)
            }
            // Prefer the SIM this conversation last used.
            messages.lastOrNull { it.subId >= 0 }?.subId?.let { sub ->
                if (sims.any { it.subId == sub }) selectedSubId = sub
            }
        }
    }

    fun openNewMessage(prefillBody: String? = null) {
        pendingBody = prefillBody
        selection = emptySet()
        push(Screen.NewMessage())
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

    fun matchesFilter(c: Conversation): Boolean = when (filter) {
        ListFilter.All -> true
        ListFilter.Unread -> c.unread > 0
        ListFilter.Personal -> c.category == Category.Personal
        ListFilter.Transactions -> c.category == Category.Transactions
        ListFilter.Otp -> c.category == Category.Otp
        ListFilter.Updates -> c.category == Category.Updates
    }

    fun countFor(f: ListFilter): Int = when (f) {
        ListFilter.All -> 0
        ListFilter.Unread -> conversations.count { it.unread > 0 }
        ListFilter.Personal -> conversations.count { it.category == Category.Personal && it.unread > 0 }
        ListFilter.Transactions -> conversations.count { it.category == Category.Transactions && it.unread > 0 }
        ListFilter.Otp -> conversations.count { it.category == Category.Otp && it.unread > 0 }
        ListFilter.Updates -> conversations.count { it.category == Category.Updates && it.unread > 0 }
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
        rows = rows.filterNot { it is MsgRow && it.msg.id == m.id }
        viewModelScope.launch(Dispatchers.IO) { repo.deleteMessage(m.id) }
    }

    fun cycleSim() {
        if (sims.size < 2) return
        val i = sims.indexOfFirst { it.subId == selectedSubId }
        selectedSubId = sims[(i + 1).mod(sims.size)].subId
    }

    // ------------------------------------------------------------ money

    fun loadTransactions() {
        if (txJob?.isActive == true) return
        txJob = viewModelScope.launch {
            val list = withContext(Dispatchers.IO) { repo.scanTransactions() }
            transactions = list
            transactionsLoaded = true
            if (list.isNotEmpty() && list.none { YearMonth.from(Fmt.localDate(it.date)) == moneyMonth }) {
                moneyMonth = YearMonth.from(Fmt.localDate(list.first().date))
            }
            // Re-categorise now that transaction threads are known.
            if (conversationsLoaded) {
                conversations = withContext(Dispatchers.IO) { repo.loadConversations() }
            }
        }
    }
}
