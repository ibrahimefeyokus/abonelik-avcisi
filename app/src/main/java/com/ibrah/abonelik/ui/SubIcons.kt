package com.ibrah.abonelik.ui

import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.ExperimentalLayoutApi
import androidx.compose.foundation.layout.FlowRow
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.MenuBook
import androidx.compose.material.icons.filled.Cloud
import androidx.compose.material.icons.filled.DirectionsCar
import androidx.compose.material.icons.filled.FitnessCenter
import androidx.compose.material.icons.filled.Home
import androidx.compose.material.icons.filled.Movie
import androidx.compose.material.icons.filled.MusicNote
import androidx.compose.material.icons.filled.Newspaper
import androidx.compose.material.icons.filled.PhoneAndroid
import androidx.compose.material.icons.filled.Receipt
import androidx.compose.material.icons.filled.Restaurant
import androidx.compose.material.icons.filled.ShoppingBag
import androidx.compose.material.icons.filled.SmartToy
import androidx.compose.material.icons.filled.SportsEsports
import androidx.compose.material.icons.filled.Tv
import androidx.compose.material.icons.filled.Wifi
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp

/** Abonelik simgeleri (marka logosu değil; genel kategori simgeleri). */
object SubIcons {
    val all: Map<String, Pair<ImageVector, String>> = linkedMapOf(
        "music" to (Icons.Filled.MusicNote to "Müzik"),
        "movie" to (Icons.Filled.Movie to "Film / dizi"),
        "tv" to (Icons.Filled.Tv to "TV"),
        "cloud" to (Icons.Filled.Cloud to "Bulut"),
        "ai" to (Icons.Filled.SmartToy to "Yapay zekâ"),
        "game" to (Icons.Filled.SportsEsports to "Oyun"),
        "fitness" to (Icons.Filled.FitnessCenter to "Spor"),
        "phone" to (Icons.Filled.PhoneAndroid to "Telefon"),
        "wifi" to (Icons.Filled.Wifi to "İnternet"),
        "bill" to (Icons.Filled.Receipt to "Fatura"),
        "news" to (Icons.Filled.Newspaper to "Haber"),
        "book" to (Icons.AutoMirrored.Filled.MenuBook to "Kitap"),
        "shopping" to (Icons.Filled.ShoppingBag to "Alışveriş"),
        "food" to (Icons.Filled.Restaurant to "Yemek"),
        "car" to (Icons.Filled.DirectionsCar to "Araç"),
        "home" to (Icons.Filled.Home to "Ev"),
    )

    fun get(key: String): ImageVector? = all[key]?.first
}

/** Simge seçici: ilk seçenek "harf" (boş anahtar). */
@OptIn(ExperimentalLayoutApi::class)
@Composable
fun IconPicker(name: String, color: Int, selected: String, onSelect: (String) -> Unit) {
    FlowRow(horizontalArrangement = Arrangement.spacedBy(10.dp), verticalArrangement = Arrangement.spacedBy(10.dp)) {
        PickerCell(selected.isEmpty(), { onSelect("") }) {
            Text(name.trim().take(1).uppercase(java.util.Locale.forLanguageTag("tr-TR")).ifEmpty { "A" },
                fontWeight = FontWeight.Bold, color = MaterialTheme.colorScheme.onSurface)
        }
        SubIcons.all.forEach { (key, v) ->
            PickerCell(selected == key, { onSelect(key) }) {
                Icon(v.first, v.second, tint = if (selected == key) paletteColor(color) else MaterialTheme.colorScheme.onSurfaceVariant)
            }
        }
    }
}

@Composable
private fun PickerCell(selected: Boolean, onClick: () -> Unit, content: @Composable () -> Unit) {
    Box(
        Modifier.size(44.dp).clip(CircleShape)
            .background(if (selected) MaterialTheme.colorScheme.primaryContainer else MaterialTheme.colorScheme.surfaceContainerHigh)
            .then(if (selected) Modifier.border(2.dp, MaterialTheme.colorScheme.primary, CircleShape) else Modifier)
            .clickable(onClick = onClick),
        contentAlignment = Alignment.Center,
    ) { content() }
}

/** Liste satırı simgesi: seçili simge ya da baş harf, aboneliğin renginde daire içinde. */
@Composable
fun SubAvatar(name: String, color: Int, icon: String, size: androidx.compose.ui.unit.Dp = 40.dp, dim: Boolean = false) {
    val vec = SubIcons.get(icon)
    if (vec == null) { Initial(name, color, size, dim); return }
    Box(
        Modifier.size(size).clip(CircleShape).background(paletteColor(color).copy(alpha = if (dim) 0.35f else 1f)),
        contentAlignment = Alignment.Center,
    ) { Icon(vec, null, tint = Color.White, modifier = Modifier.size(size * 0.55f)) }
}
