package com.ibrah.abonelik.ui

import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Add
import androidx.compose.material.icons.filled.CreditCard
import androidx.compose.material.icons.filled.EventAvailable
import androidx.compose.material3.FilledTonalIconButton
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import com.ibrah.abonelik.core.Card
import com.ibrah.abonelik.core.Money
import com.ibrah.abonelik.core.Period
import com.ibrah.abonelik.core.Sub
import com.ibrah.abonelik.core.Totals
import com.ibrah.abonelik.core.Upcoming
import com.ibrah.abonelik.core.UpcomingItem
import com.ibrah.abonelik.reminders.ReminderText
import com.ibrah.abonelik.ui.theme.AppColors
import java.time.LocalDate

private const val UPCOMING_SHOWN = 6

@Composable
fun HomeScreen(
    subs: List<Sub>,
    cards: List<Card>,
    today: LocalDate,
    banner: @Composable () -> Unit,
    onAdd: () -> Unit,
    onOpen: (Sub) -> Unit,
    onOpenCard: (Card) -> Unit,
    modifier: Modifier = Modifier,
) {
    val active = subs.count { !it.paused }
    val upcoming = Upcoming.list(subs, cards, today)
    LazyColumn(modifier.fillMaxSize(), contentPadding = PaddingValues(start = 16.dp, end = 16.dp, bottom = 24.dp)) {
        item { banner() }
        item { TotalCard(Money.format(Totals.monthlyKurus(subs)), Money.format(Totals.yearlyKurus(subs)), active) }

        if (upcoming.isNotEmpty()) {
            item { SectionTitle("Yaklaşanlar") }
            item {
                CardBox {
                    upcoming.take(UPCOMING_SHOWN).forEachIndexed { i, u ->
                        if (i > 0) HorizontalDivider(Modifier.padding(start = 68.dp), color = MaterialTheme.colorScheme.outlineVariant)
                        UpcomingRow(u, today, onOpen, onOpenCard)
                    }
                }
            }
        }

        item {
            SectionTitle("Tüm abonelikler") {
                FilledTonalIconButton(onClick = onAdd) { Icon(Icons.Filled.Add, "Abonelik ekle") }
            }
        }
        if (subs.isEmpty()) {
            item { EmptyState(onAdd) }
        } else {
            item {
                CardBox {
                    subs.forEachIndexed { i, s ->
                        if (i > 0) HorizontalDivider(Modifier.padding(start = 68.dp), color = MaterialTheme.colorScheme.outlineVariant)
                        SubRow(s, cards.firstOrNull { it.id == s.cardId }, onOpen)
                    }
                }
            }
        }
    }
}

@Composable
private fun TotalCard(monthly: String, yearly: String, count: Int) {
    Surface(
        Modifier.fillMaxWidth().padding(top = 8.dp), shape = MaterialTheme.shapes.large,
        color = MaterialTheme.colorScheme.primary,
    ) {
        Column(Modifier.padding(22.dp)) {
            Text("Aylık toplam", color = Color.White.copy(alpha = 0.8f), style = MaterialTheme.typography.labelLarge)
            Text(monthly, color = Color.White, style = MaterialTheme.typography.displaySmall, modifier = Modifier.padding(top = 4.dp))
            Text(
                "yılda $yearly · $count abonelik", color = Color.White.copy(alpha = 0.85f),
                style = MaterialTheme.typography.bodyMedium, modifier = Modifier.padding(top = 6.dp),
            )
        }
    }
}

@Composable
private fun UpcomingRow(u: UpcomingItem, today: LocalDate, onOpen: (Sub) -> Unit, onOpenCard: (Card) -> Unit) {
    val label = Upcoming.relativeLabel(u.date, today)
    val soon = u.date <= today.plusDays(1)
    when (u) {
        is UpcomingItem.Renewal -> ListRow(
            leading = { Initial(u.sub.name, u.sub.color) },
            title = u.sub.name, subtitle = "yenileniyor · $label", soon = soon,
            trailing = ReminderText.amountText(u.sub).substringBefore(" ≈"),
            onClick = { onOpen(u.sub) },
        )
        is UpcomingItem.Statement -> ListRow(
            leading = { IconCircle(Icons.Filled.CreditCard, paletteColor(u.card.color)) },
            title = "${u.card.name} kesim",
            subtitle = if (u.subsKurus.signum() > 0) "$label · bu ekstrede ${Money.format(u.subsKurus)} abonelik" else label,
            soon = soon, trailing = null, onClick = { onOpenCard(u.card) },
        )
        is UpcomingItem.Due -> ListRow(
            leading = { IconCircle(Icons.Filled.EventAvailable, paletteColor(u.card.color)) },
            title = "${u.card.name} son ödeme", subtitle = label, soon = soon, trailing = null,
            onClick = { onOpenCard(u.card) },
        )
    }
}

@Composable
private fun SubRow(s: Sub, card: Card?, onOpen: (Sub) -> Unit) {
    val period = if (s.period == Period.MONTHLY) "her ayın ${Turkish.dayWithSuffix(s.day)}" else "${s.day} ${MONTHS_TR[(s.month ?: 1) - 1]}"
    val sub = listOfNotNull(s.period.label, period, card?.name, if (s.paused) "duraklatıldı" else null).joinToString(" · ")
    ListRow(
        leading = { Initial(s.name, s.color, dim = s.paused) },
        title = s.name, subtitle = sub, soon = false,
        trailing = ReminderText.amountText(s).substringBefore(" ≈"), dim = s.paused,
        onClick = { onOpen(s) },
    )
}

@Composable
fun IconCircle(icon: androidx.compose.ui.graphics.vector.ImageVector, color: Color) {
    Box(Modifier.size(40.dp).clip(CircleShape).background(color.copy(alpha = 0.15f)), contentAlignment = Alignment.Center) {
        Icon(icon, null, tint = color, modifier = Modifier.size(22.dp))
    }
}

@Composable
fun ListRow(
    leading: @Composable () -> Unit,
    title: String,
    subtitle: String,
    soon: Boolean,
    trailing: String?,
    onClick: () -> Unit,
    dim: Boolean = false,
) {
    val alpha = if (dim) 0.5f else 1f
    Row(
        Modifier.fillMaxWidth().clickable(onClick = onClick).padding(horizontal = 14.dp, vertical = 11.dp),
        verticalAlignment = Alignment.CenterVertically,
    ) {
        leading()
        Spacer(Modifier.width(14.dp))
        Column(Modifier.weight(1f)) {
            Text(title, style = MaterialTheme.typography.titleMedium, maxLines = 1, overflow = TextOverflow.Ellipsis,
                color = MaterialTheme.colorScheme.onSurface.copy(alpha = alpha))
            Text(subtitle, style = MaterialTheme.typography.bodySmall, maxLines = 2,
                color = if (soon) AppColors.Amber else MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = alpha),
                fontWeight = if (soon) FontWeight.SemiBold else FontWeight.Normal)
        }
        if (trailing != null) {
            Text(trailing, style = MaterialTheme.typography.titleSmall, fontWeight = FontWeight.SemiBold,
                color = MaterialTheme.colorScheme.onSurface.copy(alpha = alpha), modifier = Modifier.padding(start = 8.dp))
        }
    }
}

@Composable
private fun EmptyState(onAdd: () -> Unit) {
    CardBox {
        Column(Modifier.fillMaxWidth().clickable(onClick = onAdd).padding(24.dp), horizontalAlignment = Alignment.CenterHorizontally,
            verticalArrangement = Arrangement.spacedBy(6.dp)) {
            Text("Henüz abonelik yok", style = MaterialTheme.typography.titleMedium)
            Text("Netflix, Spotify, telefon faturası… Eklemek için dokun.", style = MaterialTheme.typography.bodyMedium,
                color = MaterialTheme.colorScheme.onSurfaceVariant)
        }
    }
}
