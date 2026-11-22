package com.ibrah.abonelik.ui

import androidx.activity.compose.BackHandler
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.imePadding
import androidx.compose.foundation.layout.navigationBarsPadding
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.statusBarsPadding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Add
import androidx.compose.material.icons.filled.CreditCard
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.FilledTonalIconButton
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.saveable.listSaver
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.unit.dp
import com.ibrah.abonelik.core.Card
import com.ibrah.abonelik.core.Money
import com.ibrah.abonelik.core.Schedule
import com.ibrah.abonelik.core.Sub
import com.ibrah.abonelik.core.Totals
import com.ibrah.abonelik.core.Upcoming
import java.time.LocalDate
import java.time.format.DateTimeFormatter
import java.util.Locale

private val DAY_MONTH = DateTimeFormatter.ofPattern("d MMMM", Locale.forLanguageTag("tr-TR"))

@Composable
fun CardsScreen(
    cards: List<Card>,
    subs: List<Sub>,
    today: LocalDate,
    onAdd: () -> Unit,
    onOpen: (Card) -> Unit,
    modifier: Modifier = Modifier,
) {
    LazyColumn(modifier.fillMaxSize(), contentPadding = PaddingValues(start = 16.dp, end = 16.dp, bottom = 24.dp)) {
        item {
            SectionTitle("Kartlarım") { FilledTonalIconButton(onClick = onAdd) { Icon(Icons.Filled.Add, "Kart ekle") } }
        }
        if (cards.isEmpty()) {
            item {
                CardBox {
                    Column(Modifier.fillMaxWidth().clickable(onClick = onAdd).padding(24.dp),
                        horizontalAlignment = Alignment.CenterHorizontally, verticalArrangement = Arrangement.spacedBy(6.dp)) {
                        Text("Henüz kart yok", style = MaterialTheme.typography.titleMedium)
                        Text("Kartın adını ve kesim / son ödeme günlerini ekle; kart numarası istenmez.",
                            style = MaterialTheme.typography.bodyMedium, color = MaterialTheme.colorScheme.onSurfaceVariant)
                    }
                }
            }
        }
        items(cards, key = { it.id }) { c -> CardItem(c, subs, today) { onOpen(c) } }
    }
}

@Composable
private fun CardItem(c: Card, subs: List<Sub>, today: LocalDate, onClick: () -> Unit) {
    val statement = Schedule.nextStatement(c, today)
    // Bugünden önce kesilmiş ekstrenin son ödemesi henüz gelmediyse önce o gösterilir.
    val lastStatement = Schedule.nextStatement(c, today.minusMonths(1)).let { if (it >= today) null else it }
    val pendingDue = lastStatement?.let { Schedule.dueAfter(c, it) }?.takeIf { it >= today }
    val due = pendingDue ?: Schedule.dueAfter(c, statement)
    val onThis = Totals.onStatement(c, subs, statement)
    val linked = subs.filter { it.cardId == c.id }
    Column(Modifier.padding(bottom = 12.dp)) {
        CardBox {
            Column(Modifier.fillMaxWidth().clickable(onClick = onClick).padding(16.dp)) {
                Row(verticalAlignment = Alignment.CenterVertically) {
                    IconCircle(Icons.Filled.CreditCard, paletteColor(c.color))
                    Spacer(Modifier.width(12.dp))
                    Column(Modifier.weight(1f)) {
                        Text(c.name, style = MaterialTheme.typography.titleLarge)
                        Text("Kesim her ayın ${Turkish.dayWithSuffix(c.statementDay)} · son ödeme ${Turkish.dayWithSuffix(c.dueDay)}",
                            style = MaterialTheme.typography.bodySmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
                    }
                }
                HorizontalDivider(Modifier.padding(vertical = 12.dp), color = MaterialTheme.colorScheme.outlineVariant)
                InfoLine("Sonraki kesim", "${DAY_MONTH.format(statement)} (${Upcoming.relativeLabel(statement, today)})")
                InfoLine("Son ödeme", "${DAY_MONTH.format(due)} (${Upcoming.relativeLabel(due, today)})")
                InfoLine("Bu ekstrede abonelik", Money.format(onThis), bold = true)
                InfoLine("Bağlı abonelik", if (linked.isEmpty()) "yok" else "${linked.size} · aylık ${Money.format(Totals.monthlyKurus(linked))}")
            }
        }
    }
}

@Composable
private fun InfoLine(label: String, value: String, bold: Boolean = false) {
    Row(Modifier.fillMaxWidth().padding(vertical = 3.dp)) {
        Text(label, style = MaterialTheme.typography.bodyMedium, color = MaterialTheme.colorScheme.onSurfaceVariant, modifier = Modifier.weight(1f))
        Text(value, style = MaterialTheme.typography.bodyMedium, fontWeight = if (bold) FontWeight.SemiBold else FontWeight.Medium)
    }
}

@Composable
fun CardEditor(
    existing: Card?,
    defaultColor: Int,
    linkedCount: Int,
    onSave: (CardForm) -> Unit,
    onDelete: () -> Unit,
    onClose: () -> Unit,
) {
    val initial = remember { existing?.let { CardForm.from(it) } ?: CardForm(color = defaultColor) }
    var form by rememberSaveable(stateSaver = CardFormSaver) { mutableStateOf(initial) }
    var errors by remember { mutableStateOf(emptyMap<String, String>()) }
    var confirmDelete by remember { mutableStateOf(false) }
    var confirmDiscard by remember { mutableStateOf(false) }
    val close = { if (form != initial) confirmDiscard = true else onClose() }
    BackHandler(onBack = close)
    if (confirmDiscard) DiscardDialog({ confirmDiscard = false }, onClose)
    fun set(f: CardForm) { form = f; if (errors.isNotEmpty()) errors = f.validate() }

    Column(Modifier.fillMaxSize().statusBarsPadding().navigationBarsPadding().imePadding()) {
        EditorTopBar(if (existing == null) "Yeni kart" else "Kartı düzenle", close) {
            val e = form.validate(); errors = e
            if (e.isEmpty()) onSave(form)
        }
        Column(
            Modifier.fillMaxSize().verticalScroll(rememberScrollState()).padding(horizontal = 16.dp, vertical = 8.dp),
            verticalArrangement = Arrangement.spacedBy(14.dp),
        ) {
            Field("Kart adı", form.name, errors["name"], placeholder = "Bonus, Maximum, World…") { set(form.copy(name = it)) }
            Text("Kart numarası istenmez; yalnızca kendin tanıyacağın bir ad yeter.",
                style = MaterialTheme.typography.bodySmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
            Row(horizontalArrangement = Arrangement.spacedBy(10.dp)) {
                Field("Hesap kesim günü", form.statementDay, errors["statementDay"], KeyboardType.Number, "25", Modifier.weight(1f)) {
                    set(form.copy(statementDay = it.filter(Char::isDigit).take(2)))
                }
                Field("Son ödeme günü", form.dueDay, errors["dueDay"], KeyboardType.Number, "5", Modifier.weight(1f)) {
                    set(form.copy(dueDay = it.filter(Char::isDigit).take(2)))
                }
            }
            Text("Son ödeme günü kesim gününden önceyse, son ödeme bir sonraki aya sayılır.",
                style = MaterialTheme.typography.bodySmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
            Text("Renk", style = MaterialTheme.typography.labelLarge)
            ColorPicker(form.color) { set(form.copy(color = it)) }
            if (existing != null) {
                OutlinedButton(onClick = { confirmDelete = true }, Modifier.fillMaxWidth()) {
                    Text("Kartı sil", color = MaterialTheme.colorScheme.error)
                }
            }
        }
    }
    if (confirmDelete) AlertDialog(
        onDismissRequest = { confirmDelete = false },
        containerColor = MaterialTheme.colorScheme.surfaceContainerLowest,
        title = { Text("\"${existing?.name}\" silinsin mi?") },
        text = {
            Text(if (linkedCount > 0) "Bu karta bağlı $linkedCount abonelik silinmez, kartsız kalır." else "Kart kalıcı olarak silinecek.")
        },
        confirmButton = { TextButton({ confirmDelete = false; onDelete() }) { Text("Sil", color = MaterialTheme.colorScheme.error) } },
        dismissButton = { TextButton({ confirmDelete = false }) { Text("Vazgeç") } },
    )
}

val CardFormSaver = listSaver<CardForm, Any>(
    save = { listOf(it.name, it.statementDay, it.dueDay, it.color) },
    restore = { CardForm(it[0] as String, it[1] as String, it[2] as String, it[3] as Int) },
)
