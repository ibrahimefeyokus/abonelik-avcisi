package com.ibrah.abonelik.ui

import androidx.activity.compose.BackHandler
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.imePadding
import androidx.compose.foundation.layout.navigationBarsPadding
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.statusBarsPadding
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.DropdownMenuItem
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.ExposedDropdownMenuBox
import androidx.compose.material3.ExposedDropdownMenuDefaults
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.MenuAnchorType
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Switch
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.unit.dp
import com.ibrah.abonelik.core.AppMatch
import com.ibrah.abonelik.core.Card
import com.ibrah.abonelik.core.IconKey
import com.ibrah.abonelik.core.Currency
import com.ibrah.abonelik.core.Money
import com.ibrah.abonelik.core.Period
import com.ibrah.abonelik.core.Sub
import com.ibrah.abonelik.core.Totals

/** Tam ekran abonelik formu. [existing] null ise yeni abonelik. */
@Composable
fun SubEditor(
    existing: Sub?,
    defaultColor: Int,
    cards: List<Card>,
    onSave: (SubForm) -> Unit,
    onDelete: () -> Unit,
    onClose: () -> Unit,
) {
    val initial = remember { existing?.let { SubForm.from(it) } ?: SubForm(color = defaultColor) }
    var form by rememberSaveable(stateSaver = SubFormSaver) { mutableStateOf(initial) }
    var errors by remember { mutableStateOf(emptyMap<String, String>()) }
    var confirmDelete by remember { mutableStateOf(false) }
    var confirmDiscard by remember { mutableStateOf(false) }
    val close = { if (form != initial) confirmDiscard = true else onClose() }
    BackHandler(onBack = close)
    if (confirmDiscard) DiscardDialog({ confirmDiscard = false }, onClose)

    fun set(f: SubForm) { form = f; if (errors.isNotEmpty()) errors = f.validate() }

    Column(Modifier.fillMaxSize().statusBarsPadding().navigationBarsPadding().imePadding()) {
        EditorTopBar(if (existing == null) "Yeni abonelik" else "Aboneliği düzenle", close) {
            val e = form.validate()
            errors = e
            if (e.isEmpty()) onSave(form)
        }
        Column(
            Modifier.fillMaxSize().verticalScroll(rememberScrollState()).padding(horizontal = 16.dp, vertical = 8.dp),
            verticalArrangement = Arrangement.spacedBy(14.dp),
        ) {
            Field("Ad", form.name, errors["name"], placeholder = "Netflix, Spotify, fatura…") { set(form.copy(name = it)) }
            // Ad telefondaki bir uygulamayla eşleşiyorsa onun simgesini öner (seçili değilse).
            val apps = rememberInstalledApps()
            val match = remember(form.name, apps) { AppMatch.best(form.name, apps) }
            if (match != null && IconKey.appPackage(form.icon) != match.pkg) {
                androidx.compose.material3.AssistChip(
                    onClick = { set(form.copy(icon = IconKey.forApp(match.pkg))) },
                    label = { Text("${match.label} simgesini kullan") },
                    leadingIcon = { AppIconImage(match.pkg, 20.dp) {} },
                )
            }
            Row(horizontalArrangement = Arrangement.spacedBy(10.dp), verticalAlignment = Alignment.Top) {
                Field("Tutar", form.amount, errors["amount"], KeyboardType.Decimal, "229,99", Modifier.weight(1f)) {
                    set(form.copy(amount = it))
                }
                Segmented(
                    Currency.entries.map { it to it.symbol }, form.currency, { set(form.copy(currency = it)) },
                    Modifier.weight(1f).padding(top = 8.dp),
                )
            }
            if (form.currency != Currency.TRY) {
                Field("1 ${form.currency.symbol} kaç TL?", form.rate, errors["rate"], KeyboardType.Decimal, "34,25") {
                    set(form.copy(rate = it))
                }
                val preview = runCatching { form.toSub("p") }.getOrNull()?.takeIf { form.validate().isEmpty() }
                if (preview != null) Text("≈ ${Money.format(Totals.tryKurus(preview))} her ödeme",
                    style = MaterialTheme.typography.bodySmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
            }
            Segmented(Period.entries.map { it to it.label.replaceFirstChar { c -> c.titlecase() } }, form.period,
                { set(form.copy(period = it)) })
            Row(horizontalArrangement = Arrangement.spacedBy(10.dp), verticalAlignment = Alignment.Top) {
                Field(if (form.period == Period.MONTHLY) "Her ayın kaçı?" else "Gün", form.day, errors["day"],
                    KeyboardType.Number, "23", Modifier.weight(1f)) { set(form.copy(day = it.filter(Char::isDigit).take(2))) }
                if (form.period == Period.YEARLY) {
                    Dropdown("Ay", form.month?.let { MONTHS_TR[it - 1] }, MONTHS_TR, errors["month"], Modifier.weight(1f)) {
                        set(form.copy(month = it + 1))
                    }
                }
            }
            Dropdown(
                "Hangi karttan? (isteğe bağlı)", cards.firstOrNull { it.id == form.cardId }?.name ?: "Kart yok",
                listOf("Kart yok") + cards.map { it.name }, null,
            ) { i -> set(form.copy(cardId = if (i == 0) null else cards[i - 1].id)) }
            Text("Simge", style = MaterialTheme.typography.labelLarge)
            IconPicker(form.name, form.color, form.icon) { set(form.copy(icon = it)) }
            Text("Renk", style = MaterialTheme.typography.labelLarge)
            ColorPicker(form.color) { set(form.copy(color = it)) }
            if (existing != null) {
                Row(Modifier.fillMaxWidth(), verticalAlignment = Alignment.CenterVertically) {
                    Column(Modifier.weight(1f)) {
                        Text("Duraklat", style = MaterialTheme.typography.titleMedium)
                        Text("Toplama girmez, hatırlatma gelmez", style = MaterialTheme.typography.bodySmall,
                            color = MaterialTheme.colorScheme.onSurfaceVariant)
                    }
                    Switch(form.paused, { set(form.copy(paused = it)) })
                }
                OutlinedButton(onClick = { confirmDelete = true }, Modifier.fillMaxWidth()) {
                    Text("Aboneliği sil", color = MaterialTheme.colorScheme.error)
                }
            }
        }
    }
    if (confirmDelete) AlertDialog(
        onDismissRequest = { confirmDelete = false },
        containerColor = MaterialTheme.colorScheme.surfaceContainerLowest,
        title = { Text("\"${existing?.name}\" silinsin mi?") },
        text = { Text("Bu abonelik ve hatırlatmaları kalıcı olarak silinecek.") },
        confirmButton = { TextButton({ confirmDelete = false; onDelete() }) { Text("Sil", color = MaterialTheme.colorScheme.error) } },
        dismissButton = { TextButton({ confirmDelete = false }) { Text("Vazgeç") } },
    )
}

@Composable
fun DiscardDialog(onKeep: () -> Unit, onDiscard: () -> Unit) = AlertDialog(
    onDismissRequest = onKeep,
    containerColor = MaterialTheme.colorScheme.surfaceContainerLowest,
    title = { Text("Değişiklikler kaydedilmedi") },
    text = { Text("Çıkarsan yazdıkların silinecek.") },
    confirmButton = { TextButton(onDiscard) { Text("Çık", color = MaterialTheme.colorScheme.error) } },
    dismissButton = { TextButton(onKeep) { Text("Düzenlemeye dön") } },
)

@Composable
fun EditorTopBar(title: String, onClose: () -> Unit, onSave: () -> Unit) {
    Row(Modifier.fillMaxWidth().padding(horizontal = 4.dp, vertical = 4.dp), verticalAlignment = Alignment.CenterVertically) {
        IconButton(onClick = onClose) { Icon(Icons.AutoMirrored.Filled.ArrowBack, "Geri") }
        Text(title, style = MaterialTheme.typography.titleLarge, modifier = Modifier.weight(1f))
        TextButton(onClick = onSave) { Text("Kaydet", style = MaterialTheme.typography.labelLarge) }
    }
}

@Composable
fun Field(
    label: String,
    value: String,
    error: String?,
    keyboard: KeyboardType = KeyboardType.Text,
    placeholder: String = "",
    modifier: Modifier = Modifier.fillMaxWidth(),
    onChange: (String) -> Unit,
) {
    OutlinedTextField(
        value = value, onValueChange = onChange, label = { Text(label) }, singleLine = true,
        placeholder = { Text(placeholder) }, isError = error != null,
        supportingText = error?.let { { Text(it) } },
        keyboardOptions = KeyboardOptions(keyboardType = keyboard), modifier = modifier,
        shape = MaterialTheme.shapes.small,
    )
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun Dropdown(
    label: String,
    selected: String?,
    options: List<String>,
    error: String?,
    modifier: Modifier = Modifier.fillMaxWidth(),
    onSelect: (Int) -> Unit,
) {
    var open by remember { mutableStateOf(false) }
    ExposedDropdownMenuBox(open, { open = it }, modifier) {
        OutlinedTextField(
            value = selected.orEmpty(), onValueChange = {}, readOnly = true, label = { Text(label) },
            trailingIcon = { ExposedDropdownMenuDefaults.TrailingIcon(open) }, isError = error != null,
            supportingText = error?.let { { Text(it) } }, shape = MaterialTheme.shapes.small,
            modifier = Modifier.fillMaxWidth().menuAnchor(MenuAnchorType.PrimaryNotEditable),
        )
        ExposedDropdownMenu(open, { open = false }) {
            options.forEachIndexed { i, o -> DropdownMenuItem({ Text(o) }, { onSelect(i); open = false }) }
        }
    }
}

/** Ekran döndürmede form kaybolmasın. */
val SubFormSaver = androidx.compose.runtime.saveable.listSaver<SubForm, Any?>(
    save = { listOf(it.name, it.amount, it.currency.name, it.rate, it.period.name, it.day, it.month, it.cardId, it.color, it.paused, it.icon) },
    restore = {
        SubForm(it[0] as String, it[1] as String, Currency.valueOf(it[2] as String), it[3] as String,
            Period.valueOf(it[4] as String), it[5] as String, it[6] as Int?, it[7] as String?, it[8] as Int, it[9] as Boolean,
            it.getOrNull(10) as? String ?: "")
    },
)
