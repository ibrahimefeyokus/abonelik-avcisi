package com.ibrah.abonelik.ui

import androidx.activity.compose.BackHandler
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.navigationBarsPadding
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.statusBarsPadding
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.filled.Add
import androidx.compose.material.icons.filled.Remove
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Switch
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.material3.TimePicker
import androidx.compose.material3.rememberTimePickerState
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import com.ibrah.abonelik.core.ReminderSettings
import java.time.LocalTime

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun SettingsScreen(settings: ReminderSettings, onChange: ((ReminderSettings) -> ReminderSettings) -> Unit, onClose: () -> Unit) {
    var picking by remember { mutableStateOf(false) }
    BackHandler(onBack = onClose)
    Column(Modifier.fillMaxSize().statusBarsPadding().navigationBarsPadding()) {
        Row(Modifier.fillMaxWidth().padding(4.dp), verticalAlignment = Alignment.CenterVertically) {
            IconButton(onClick = onClose) { Icon(Icons.AutoMirrored.Filled.ArrowBack, "Geri") }
            Text("Hatırlatma ayarları", style = MaterialTheme.typography.titleLarge)
        }
        Column(Modifier.verticalScroll(rememberScrollState()).padding(horizontal = 16.dp)) {
            CardBox {
                SettingRow("Bildirim saati", "Hatırlatmalar her gün bu saatte gelir",
                    Modifier.clickable { picking = true }) {
                    Text("%02d:%02d".format(settings.time.hour, settings.time.minute), style = MaterialTheme.typography.titleMedium,
                        color = MaterialTheme.colorScheme.primary)
                }
            }
            SectionTitle("Abonelikler")
            CardBox {
                SettingRow("Yenilenme hatırlatması", null) { Switch(settings.subOn, { v -> onChange { it.copy(subOn = v) } }) }
                Divider()
                SettingRow("Kaç gün önce?", if (settings.subDaysBefore == 0) "yenilenme günü" else "${settings.subDaysBefore} gün önce") {
                    Stepper(settings.subDaysBefore) { n -> onChange { it.copy(subDaysBefore = n) } }
                }
            }
            SectionTitle("Kredi kartları")
            CardBox {
                SettingRow("Hesap kesim günü hatırlatması", "Kesim günü sabahı") {
                    Switch(settings.statementOn, { v -> onChange { it.copy(statementOn = v) } })
                }
                Divider()
                SettingRow("Son ödeme hatırlatması", null) { Switch(settings.dueOn, { v -> onChange { it.copy(dueOn = v) } }) }
                Divider()
                SettingRow("Kaç gün önce?", if (settings.dueDaysBefore == 0) "son ödeme günü" else "${settings.dueDaysBefore} gün önce") {
                    Stepper(settings.dueDaysBefore) { n -> onChange { it.copy(dueDaysBefore = n) } }
                }
            }
            Text(
                "Telefon kapalıyken kaçan hatırlatmalar, tarihi geçmediyse telefon açılınca ya da uygulamaya girince gelir.",
                style = MaterialTheme.typography.bodySmall, color = MaterialTheme.colorScheme.onSurfaceVariant,
                modifier = Modifier.padding(vertical = 16.dp),
            )
        }
    }
    if (picking) {
        val state = rememberTimePickerState(settings.time.hour, settings.time.minute, is24Hour = true)
        AlertDialog(
            onDismissRequest = { picking = false },
            containerColor = MaterialTheme.colorScheme.surfaceContainerLowest,
            title = { Text("Bildirim saati") },
            text = { TimePicker(state) },
            confirmButton = {
                TextButton({ picking = false; onChange { it.copy(time = LocalTime.of(state.hour, state.minute)) } }) { Text("Tamam") }
            },
            dismissButton = { TextButton({ picking = false }) { Text("Vazgeç") } },
        )
    }
}

@Composable
private fun Divider() = HorizontalDivider(Modifier.padding(start = 16.dp), color = MaterialTheme.colorScheme.outlineVariant)

@Composable
private fun SettingRow(title: String, subtitle: String?, modifier: Modifier = Modifier, trailing: @Composable () -> Unit) {
    Row(modifier.fillMaxWidth().padding(horizontal = 16.dp, vertical = 12.dp), verticalAlignment = Alignment.CenterVertically) {
        Column(Modifier.weight(1f), verticalArrangement = Arrangement.spacedBy(2.dp)) {
            Text(title, style = MaterialTheme.typography.titleMedium)
            if (subtitle != null) Text(subtitle, style = MaterialTheme.typography.bodySmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
        }
        trailing()
    }
}

@Composable
private fun Stepper(value: Int, onChange: (Int) -> Unit) {
    Row(verticalAlignment = Alignment.CenterVertically) {
        IconButton({ onChange((value - 1).coerceAtLeast(0)) }, enabled = value > 0) { Icon(Icons.Filled.Remove, "Azalt") }
        Text("$value", style = MaterialTheme.typography.titleMedium)
        IconButton({ onChange((value + 1).coerceAtMost(7)) }, enabled = value < 7) { Icon(Icons.Filled.Add, "Artır") }
    }
}
