package com.ibrah.abonelik.ui

import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.statusBarsPadding
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.CreditCard
import androidx.compose.material.icons.filled.Settings
import androidx.compose.material.icons.filled.Subscriptions
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.NavigationBar
import androidx.compose.material3.NavigationBarItem
import androidx.compose.material3.Scaffold
import androidx.compose.material3.SnackbarHost
import androidx.compose.material3.SnackbarHostState
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.unit.dp
import androidx.compose.runtime.LaunchedEffect
import com.ibrah.abonelik.core.Card
import com.ibrah.abonelik.core.ReminderSettings
import com.ibrah.abonelik.core.SettingsChange
import com.ibrah.abonelik.core.Sub
import kotlinx.coroutines.flow.map
import com.ibrah.abonelik.data.Repository
import com.ibrah.abonelik.data.SettingsStore
import com.ibrah.abonelik.notify.Notifications
import com.ibrah.abonelik.reminders.CatchUp
import java.time.LocalDate
import java.util.UUID
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext

private const val NEW = "new"

@Composable
fun AppNav() {
    val ctx = LocalContext.current
    val scope = rememberCoroutineScope()
    val repo = remember { Repository.get(ctx) }
    val store = remember { SettingsStore(ctx) }
    // null = henüz yüklenmedi. Döndürme/süreç ölümü sonrası ilk karede boş liste görülüp
    // açık düzenleyici kapatılmasın (yazılanlar kaybolmasın).
    val subsOrNull by remember { repo.subs.map<List<Sub>, List<Sub>?> { it } }.collectAsState(initial = null)
    val cardsOrNull by remember { repo.cards.map<List<Card>, List<Card>?> { it } }.collectAsState(initial = null)
    val loaded = subsOrNull != null && cardsOrNull != null
    val subs = subsOrNull.orEmpty()
    val cards = cardsOrNull.orEmpty()
    val settings by store.flow.collectAsState(initial = ReminderSettings())
    val theme by store.theme.collectAsState(initial = com.ibrah.abonelik.core.ThemeMode.SYSTEM)
    var tab by rememberSaveable { mutableStateOf(0) }
    var editingSub by rememberSaveable { mutableStateOf<String?>(null) }   // id ya da NEW
    var editingCard by rememberSaveable { mutableStateOf<String?>(null) }
    var showSettings by rememberSaveable { mutableStateOf(false) }
    var showSetup by rememberSaveable { mutableStateOf(false) }
    val snackbar = remember { SnackbarHostState() }
    val today = LocalDate.now()

    fun catchUp() = scope.launch { withContext(Dispatchers.IO) { runCatching { CatchUp.run(ctx) } } }

    editingSub?.let { id ->
        if (!loaded) return  // veriler gelene kadar bekle; düzenleyici durumu korunur
        val existing = subs.firstOrNull { it.id == id }
        if (id != NEW && existing == null) {
            LaunchedEffect(id) { editingSub = null } // silinmiş
            return
        }
        // Yeni öğenin kimliği oturum boyunca sabit: "Kaydet"e iki kez basmak çift kayıt yaratmaz (upsert).
        val saveId = rememberSaveable(id) { existing?.id ?: UUID.randomUUID().toString() }
        SubEditor(
            existing = existing, defaultColor = subs.size % 12, cards = cards,
            onSave = { f ->
                scope.launch {
                    repo.saveSub(f.toSub(saveId))
                    editingSub = null
                    catchUp()
                }
            },
            onDelete = { scope.launch { repo.deleteSub(id); editingSub = null; snackbar.showSnackbar("${existing?.name} silindi") } },
            onClose = { editingSub = null },
        )
        return
    }
    editingCard?.let { id ->
        if (!loaded) return
        val existing = cards.firstOrNull { it.id == id }
        if (id != NEW && existing == null) {
            LaunchedEffect(id) { editingCard = null }
            return
        }
        val saveId = rememberSaveable(id) { existing?.id ?: UUID.randomUUID().toString() }
        CardEditor(
            existing = existing, defaultColor = (cards.size + 3) % 12, linkedCount = subs.count { it.cardId == id },
            onSave = { f ->
                scope.launch {
                    repo.saveCard(f.toCard(saveId))
                    editingCard = null
                    catchUp()
                }
            },
            onDelete = { scope.launch { repo.deleteCard(id); editingCard = null; snackbar.showSnackbar("${existing?.name} silindi") } },
            onClose = { editingCard = null },
        )
        return
    }
    if (showSetup) {
        SetupScreen(settings, onGranted = { catchUp() }, onClose = { showSetup = false })
        return
    }
    if (showSettings) {
        SettingsScreen(
            settings,
            theme = theme,
            onTheme = { t -> scope.launch { store.setTheme(t) } },
            onChange = { f ->
                scope.launch {
                    val old = store.current()
                    store.update(f)
                    withContext(Dispatchers.IO) {
                        runCatching {
                            val new = store.current()
                            // Ses/titreşim değişince kanal hemen yenilensin (telefon ayarlarında da doğru görünsün).
                            Notifications.ensureChannel(ctx, new)
                            if (SettingsChange.needsCatchUp(old, new)) CatchUp.run(ctx) else CatchUp.reschedule(ctx)
                        }
                    }
                }
            },
            onClose = { showSettings = false },
        )
        return
    }

    Scaffold(
        containerColor = MaterialTheme.colorScheme.background,
        snackbarHost = { SnackbarHost(snackbar) },
        topBar = {
            Row(
                Modifier.fillMaxWidth().statusBarsPadding().padding(start = 20.dp, end = 8.dp, top = 8.dp, bottom = 4.dp),
                verticalAlignment = Alignment.CenterVertically,
            ) {
                Text(if (tab == 0) "Abonelikler" else "Kartlar", style = MaterialTheme.typography.headlineMedium,
                    modifier = Modifier.weight(1f))
                IconButton(onClick = { showSettings = true }) { Icon(Icons.Filled.Settings, "Ayarlar") }
            }
        },
        bottomBar = {
            NavigationBar(containerColor = MaterialTheme.colorScheme.surfaceContainerLowest) {
                NavigationBarItem(tab == 0, { tab = 0 }, { Icon(Icons.Filled.Subscriptions, null) }, label = { Text("Abonelikler") })
                NavigationBarItem(tab == 1, { tab = 1 }, { Icon(Icons.Filled.CreditCard, null) }, label = { Text("Kartlar") })
            }
        },
    ) { pad ->
        val m = Modifier.padding(pad)
        if (tab == 0) {
            HomeScreen(
                subs, cards, today,
                banner = {
                    val (setup, _) = rememberSetupState(settings)
                    SetupRow(setup, onOpen = { showSetup = true })
                },
                onAdd = { editingSub = NEW },
                onOpen = { editingSub = it.id },
                onOpenCard = { editingCard = it.id },
                modifier = m,
            )
        } else {
            CardsScreen(cards, subs, today, onAdd = { editingCard = NEW }, onOpen = { editingCard = it.id }, modifier = m)
        }
    }
}
