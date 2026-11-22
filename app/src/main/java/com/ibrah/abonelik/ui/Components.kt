package com.ibrah.abonelik.ui

import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.foundation.lazy.itemsIndexed
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Check
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.SegmentedButton
import androidx.compose.material3.SegmentedButtonDefaults
import androidx.compose.material3.SingleChoiceSegmentedButtonRow
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import com.ibrah.abonelik.ui.theme.AppColors

val MONTHS_TR = listOf("Ocak", "Şubat", "Mart", "Nisan", "Mayıs", "Haziran", "Temmuz", "Ağustos", "Eylül", "Ekim", "Kasım", "Aralık")

fun paletteColor(i: Int): Color = AppColors.Palette[((i % 12) + 12) % 12]

/** Renkli daire içinde adın baş harfi (marka logosu kullanılmaz). */
@Composable
fun Initial(name: String, color: Int, size: Dp = 40.dp, dim: Boolean = false) {
    Box(
        Modifier.size(size).clip(CircleShape).background(paletteColor(color).copy(alpha = if (dim) 0.35f else 1f)),
        contentAlignment = Alignment.Center,
    ) {
        Text(
            name.trim().take(1).uppercase(java.util.Locale.forLanguageTag("tr-TR")).ifEmpty { "?" },
            color = Color.White, fontWeight = FontWeight.Bold,
            style = if (size >= 40.dp) MaterialTheme.typography.titleMedium else MaterialTheme.typography.labelLarge,
        )
    }
}

@Composable
fun SectionTitle(text: String, modifier: Modifier = Modifier, trailing: @Composable () -> Unit = {}) {
    Row(modifier.fillMaxWidth().padding(top = 20.dp, bottom = 8.dp), verticalAlignment = Alignment.CenterVertically) {
        Text(text, style = MaterialTheme.typography.titleMedium, modifier = Modifier.weight(1f))
        trailing()
    }
}

/** Beyaz, köşeli kart; içindeki satırlar dikey dizilir. */
@Composable
fun CardBox(modifier: Modifier = Modifier, content: @Composable () -> Unit) {
    Surface(modifier.fillMaxWidth(), shape = MaterialTheme.shapes.medium, color = MaterialTheme.colorScheme.surfaceContainerLowest) {
        Column(Modifier.padding(vertical = 4.dp)) { content() }
    }
}

@Composable
fun <T> Segmented(options: List<Pair<T, String>>, selected: T, onSelect: (T) -> Unit, modifier: Modifier = Modifier) {
    SingleChoiceSegmentedButtonRow(modifier.fillMaxWidth()) {
        options.forEachIndexed { i, (value, label) ->
            SegmentedButton(
                selected = value == selected, onClick = { onSelect(value) },
                shape = SegmentedButtonDefaults.itemShape(i, options.size),
            ) { Text(label) }
        }
    }
}

@Composable
fun ColorPicker(selected: Int, onSelect: (Int) -> Unit) {
    LazyRow(horizontalArrangement = Arrangement.spacedBy(10.dp)) {
        itemsIndexed(AppColors.Palette) { i, c ->
            Box(
                Modifier.size(36.dp).clip(CircleShape).background(c)
                    .then(if (i == selected) Modifier.border(3.dp, MaterialTheme.colorScheme.onBackground, CircleShape) else Modifier)
                    .clickable { onSelect(i) },
                contentAlignment = Alignment.Center,
            ) { if (i == selected) Icon(Icons.Filled.Check, null, tint = Color.White, modifier = Modifier.size(18.dp)) }
        }
    }
}

@Composable
fun Pill(text: String, color: Color, container: Color) {
    Text(
        text, color = color, style = MaterialTheme.typography.labelMedium,
        modifier = Modifier.clip(RoundedCornerShape(8.dp)).background(container).padding(horizontal = 8.dp, vertical = 3.dp),
    )
}
