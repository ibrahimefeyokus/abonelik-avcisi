package com.ibrah.abonelik.widget

import android.content.Context
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.unit.DpSize
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.glance.GlanceId
import androidx.glance.GlanceModifier
import androidx.glance.LocalSize
import androidx.glance.action.actionStartActivity
import androidx.glance.action.clickable
import androidx.glance.appwidget.GlanceAppWidget
import androidx.glance.appwidget.GlanceAppWidgetReceiver
import androidx.glance.appwidget.SizeMode
import androidx.glance.appwidget.cornerRadius
import androidx.glance.appwidget.provideContent
import androidx.glance.appwidget.updateAll
import androidx.glance.background
import androidx.glance.color.ColorProvider
import androidx.glance.layout.Alignment
import androidx.glance.layout.Column
import androidx.glance.layout.Row
import androidx.glance.layout.Spacer
import androidx.glance.layout.fillMaxSize
import androidx.glance.layout.fillMaxWidth
import androidx.glance.layout.height
import androidx.glance.layout.padding
import androidx.glance.text.FontWeight
import androidx.glance.text.Text
import androidx.glance.text.TextStyle
import com.ibrah.abonelik.MainActivity
import com.ibrah.abonelik.core.Card
import com.ibrah.abonelik.core.Money
import com.ibrah.abonelik.core.Sub
import com.ibrah.abonelik.core.Totals
import com.ibrah.abonelik.core.Upcoming
import com.ibrah.abonelik.core.UpcomingItem
import com.ibrah.abonelik.data.Repository
import com.ibrah.abonelik.reminders.ReminderText
import java.time.LocalDate

data class WidgetRow(val title: String, val label: String, val amount: String?)
data class WidgetModel(val total: String, val rows: List<WidgetRow>, val empty: Boolean) {
    companion object {
        fun build(subs: List<Sub>, cards: List<Card>, today: LocalDate): WidgetModel {
            val rows = Upcoming.list(subs, cards, today).take(2).map { u ->
                val label = Upcoming.relativeLabel(u.date, today)
                when (u) {
                    is UpcomingItem.Renewal -> WidgetRow(u.sub.name, label, ReminderText.amountText(u.sub).substringBefore(" ≈"))
                    is UpcomingItem.Statement -> WidgetRow("${u.card.name} kesim", label, null)
                    is UpcomingItem.Due -> WidgetRow("${u.card.name} son ödeme", label, null)
                }
            }
            return WidgetModel(Money.format(Totals.monthlyKurus(subs)), rows, subs.isEmpty())
        }
    }
}

private val SMALL = DpSize(110.dp, 110.dp)
private val WIDE = DpSize(250.dp, 110.dp)
private val BG = ColorProvider(Color(0xFF0F7A5C), Color(0xFF0F3B2F))
private val FG = ColorProvider(Color.White, Color(0xFFE5E9F0))
private val FG_SOFT = ColorProvider(Color(0xCCFFFFFF), Color(0xB3E5E9F0))

class SubsWidget : GlanceAppWidget() {
    override val sizeMode = SizeMode.Responsive(setOf(SMALL, WIDE))

    override suspend fun provideGlance(context: Context, id: GlanceId) {
        val repo = Repository.get(context)
        val model = WidgetModel.build(repo.subsNow(), repo.cardsNow(), LocalDate.now())
        provideContent { Content(model) }
    }

    @androidx.compose.runtime.Composable
    private fun Content(m: WidgetModel) {
        val wide = LocalSize.current.width >= WIDE.width
        Column(
            GlanceModifier.fillMaxSize().background(BG).cornerRadius(20.dp).padding(14.dp)
                .clickable(actionStartActivity<MainActivity>()),
        ) {
            Text("Aylık toplam", style = TextStyle(color = FG_SOFT, fontSize = 12.sp))
            Text(m.total, style = TextStyle(color = FG, fontSize = if (wide) 24.sp else 20.sp, fontWeight = FontWeight.Bold), maxLines = 1)
            Spacer(GlanceModifier.height(8.dp))
            if (m.empty) {
                Text("Abonelik ekle", style = TextStyle(color = FG, fontSize = 13.sp, fontWeight = FontWeight.Medium))
            } else {
                m.rows.forEach { r ->
                    Row(GlanceModifier.fillMaxWidth().padding(top = 2.dp), verticalAlignment = Alignment.CenterVertically) {
                        Text(r.title, style = TextStyle(color = FG, fontSize = 13.sp, fontWeight = FontWeight.Medium),
                            maxLines = 1, modifier = GlanceModifier.defaultWeight())
                        Text(if (wide && r.amount != null) "${r.label} · ${r.amount}" else r.label,
                            style = TextStyle(color = FG_SOFT, fontSize = 12.sp), maxLines = 1)
                    }
                }
            }
        }
    }
}

class SubsWidgetReceiver : GlanceAppWidgetReceiver() {
    override val glanceAppWidget: GlanceAppWidget = SubsWidget()
}

object WidgetUpdater {
    suspend fun update(context: Context) {
        runCatching { SubsWidget().updateAll(context.applicationContext) }
    }
}
