package com.ibrah.abonelik.ui

import android.Manifest
import android.annotation.SuppressLint
import android.content.Context
import android.content.Intent
import android.content.pm.PackageManager
import android.net.Uri
import android.os.PowerManager
import android.provider.Settings
import androidx.activity.compose.BackHandler
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.navigationBarsPadding
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.statusBarsPadding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.automirrored.filled.KeyboardArrowRight
import androidx.compose.material.icons.filled.BatteryAlert
import androidx.compose.material.icons.filled.CheckCircle
import androidx.compose.material.icons.filled.NotificationsActive
import androidx.compose.material.icons.filled.RestartAlt
import androidx.compose.material.icons.filled.Warning
import androidx.compose.material3.FilledTonalButton
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.DisposableEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.unit.dp
import androidx.lifecycle.Lifecycle
import androidx.lifecycle.LifecycleEventObserver
import androidx.lifecycle.compose.LocalLifecycleOwner
import com.ibrah.abonelik.core.ReminderSettings
import com.ibrah.abonelik.core.SetupState
import com.ibrah.abonelik.notify.Notifications
import com.ibrah.abonelik.ui.theme.AppColors

object Setup {
    fun notificationsGranted(ctx: Context) =
        ctx.checkSelfPermission(Manifest.permission.POST_NOTIFICATIONS) == PackageManager.PERMISSION_GRANTED

    fun batteryUnrestricted(ctx: Context) =
        ctx.getSystemService(PowerManager::class.java).isIgnoringBatteryOptimizations(ctx.packageName)

    private fun prefs(ctx: Context) = ctx.getSharedPreferences("setup", Context.MODE_PRIVATE)

    /** ColorOS'un otomatik başlatma ayarı uygulamadan okunamaz; kullanıcı "Yaptım" deyince tamam sayılır. */
    fun autoStartAcknowledged(ctx: Context) = prefs(ctx).getBoolean("autostart_ack", false)
    fun acknowledgeAutoStart(ctx: Context) = prefs(ctx).edit().putBoolean("autostart_ack", true).apply()

    /** Bildirimleri uygulamadan bilerek kapattıysa sorun sayılmaz; açıkken telefon engelliyorsa sayılır. */
    fun state(ctx: Context, s: ReminderSettings) = SetupState(
        notifications = !s.notificationsOn || (notificationsGranted(ctx) && Notifications.systemAllows(ctx, s)),
        battery = batteryUnrestricted(ctx),
        autoStart = autoStartAcknowledged(ctx),
    )

    fun openAppDetails(ctx: Context) =
        start(ctx, Intent(Settings.ACTION_APPLICATION_DETAILS_SETTINGS, Uri.parse("package:${ctx.packageName}")))

    fun openNotificationSettings(ctx: Context) =
        start(ctx, Intent(Settings.ACTION_APP_NOTIFICATION_SETTINGS).putExtra(Settings.EXTRA_APP_PACKAGE, ctx.packageName))

    /** Realme/ColorOS'ta arka plan kapatmasın diye pil kısıtlamasını kaldırma ekranı; olmazsa uygulama ayrıntıları. */
    @SuppressLint("BatteryLife")
    fun openBattery(ctx: Context) {
        val pkg = Uri.parse("package:${ctx.packageName}")
        val i = listOf(
            Intent(Settings.ACTION_REQUEST_IGNORE_BATTERY_OPTIMIZATIONS, pkg),
            Intent(Settings.ACTION_APPLICATION_DETAILS_SETTINGS, pkg),
        ).firstOrNull { it.resolveActivity(ctx.packageManager) != null } ?: return
        start(ctx, i)
    }

    private fun start(ctx: Context, i: Intent) {
        runCatching { ctx.startActivity(i.addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)) }
    }
}

/** Kurulum durumu; uygulama her öne geldiğinde (ayarlardan dönünce) yeniden okunur. */
@Composable
fun rememberSetupState(settings: ReminderSettings): Pair<SetupState, () -> Unit> {
    val ctx = LocalContext.current
    var tick by remember { mutableIntStateOf(0) }
    val owner = LocalLifecycleOwner.current
    DisposableEffect(owner) {
        val obs = LifecycleEventObserver { _, e -> if (e == Lifecycle.Event.ON_RESUME) tick++ }
        owner.lifecycle.addObserver(obs)
        onDispose { owner.lifecycle.removeObserver(obs) }
    }
    @Suppress("UNUSED_EXPRESSION") tick
    return Setup.state(ctx, settings) to { tick++ }
}

/** Ana ekranda tek satır: "Kurulum eksik (2) ›". Her şey tamamsa hiç görünmez. */
@Composable
fun SetupRow(state: SetupState, onOpen: () -> Unit) {
    if (state.missing == 0) return
    Surface(
        onClick = onOpen, modifier = Modifier.fillMaxWidth().padding(top = 8.dp), shape = MaterialTheme.shapes.small,
        color = MaterialTheme.colorScheme.errorContainer,
    ) {
        Row(Modifier.padding(horizontal = 14.dp, vertical = 12.dp), verticalAlignment = Alignment.CenterVertically) {
            Icon(Icons.Filled.Warning, null, tint = AppColors.Amber, modifier = Modifier.size(20.dp))
            Text("Hatırlatmalar için kurulum eksik (${state.missing})", style = MaterialTheme.typography.labelLarge,
                color = MaterialTheme.colorScheme.onErrorContainer, modifier = Modifier.padding(start = 10.dp).weight(1f))
            Icon(Icons.AutoMirrored.Filled.KeyboardArrowRight, null, tint = MaterialTheme.colorScheme.onErrorContainer)
        }
    }
}

/** Kurulum ekranı: her madde için durum ve yapılacak iş. */
@Composable
fun SetupScreen(settings: ReminderSettings, onGranted: () -> Unit, onClose: () -> Unit) {
    val ctx = LocalContext.current
    val (state, refresh) = rememberSetupState(settings)
    val launcher = rememberLauncherForActivityResult(ActivityResultContracts.RequestPermission()) { ok ->
        refresh(); if (ok) onGranted()
    }
    BackHandler(onBack = onClose)
    Column(Modifier.fillMaxSize().statusBarsPadding().navigationBarsPadding()) {
        Row(Modifier.fillMaxWidth().padding(4.dp), verticalAlignment = Alignment.CenterVertically) {
            IconButton(onClick = onClose) { Icon(Icons.AutoMirrored.Filled.ArrowBack, "Geri") }
            Text("Kurulum", style = MaterialTheme.typography.titleLarge)
        }
        Column(Modifier.verticalScroll(rememberScrollState()).padding(horizontal = 16.dp)) {
            Text(
                if (state.missing == 0) "Her şey hazır; hatırlatmalar zamanında gelecek."
                else "Hatırlatmaların zamanında gelmesi için şunları ayarla:",
                style = MaterialTheme.typography.bodyMedium, color = MaterialTheme.colorScheme.onSurfaceVariant,
                modifier = Modifier.padding(vertical = 8.dp),
            )
            CardBox {
                SetupItem(
                    Icons.Filled.NotificationsActive, "Bildirimler", state.notifications,
                    if (settings.notificationsOn) "Hatırlatmalar bildirim olarak gelir."
                    else "Uygulamanın Ayarlar ekranından kapattın; açınca hatırlatmalar gelir.",
                    "Kapalı: hatırlatmalar gelmez.", "İzin ver",
                ) {
                    if (!Setup.notificationsGranted(ctx)) launcher.launch(Manifest.permission.POST_NOTIFICATIONS)
                    else Setup.openNotificationSettings(ctx)
                }
                HorizontalDivider(Modifier.padding(start = 56.dp), color = MaterialTheme.colorScheme.outlineVariant)
                SetupItem(
                    Icons.Filled.BatteryAlert, "Pil kısıtlaması", state.battery,
                    "Kaldırıldı.", "Açık: telefon uygulamayı kapatıp hatırlatmayı geciktirebilir.", "Ayarla",
                ) { Setup.openBattery(ctx) }
                HorizontalDivider(Modifier.padding(start = 56.dp), color = MaterialTheme.colorScheme.outlineVariant)
                SetupItem(
                    Icons.Filled.RestartAlt, "Otomatik başlatma (Realme)", state.autoStart,
                    "Tamam.",
                    "Ayarlar > Uygulamalar > Abonelik Avcısı'nda \"Otomatik başlatma\" ve \"Arka planda çalışmaya izin ver\" " +
                        "açık olsun; yoksa telefon yeniden başlayınca hatırlatmalar uygulamayı açana kadar gelmez.",
                    "Ayarla",
                    extra = if (!state.autoStart) ({
                        TextButton({ Setup.acknowledgeAutoStart(ctx); refresh() }) { Text("Yaptım") }
                    }) else null,
                ) { Setup.openAppDetails(ctx) }
            }
        }
    }
}

@Composable
private fun SetupItem(
    icon: ImageVector,
    title: String,
    ok: Boolean,
    okText: String,
    todo: String,
    action: String,
    extra: (@Composable () -> Unit)? = null,
    onAction: () -> Unit,
) {
    Row(Modifier.fillMaxWidth().padding(14.dp), verticalAlignment = Alignment.Top) {
        Icon(icon, null, tint = if (ok) MaterialTheme.colorScheme.primary else AppColors.Amber, modifier = Modifier.size(26.dp))
        Spacer(Modifier.width(14.dp))
        Column(Modifier.weight(1f), verticalArrangement = Arrangement.spacedBy(4.dp)) {
            Row(verticalAlignment = Alignment.CenterVertically) {
                Text(title, style = MaterialTheme.typography.titleMedium, modifier = Modifier.weight(1f))
                if (ok) Icon(Icons.Filled.CheckCircle, "Tamam", tint = MaterialTheme.colorScheme.primary, modifier = Modifier.size(20.dp))
            }
            Text(if (ok) okText else todo, style = MaterialTheme.typography.bodySmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
            if (!ok) Row(horizontalArrangement = Arrangement.spacedBy(8.dp), verticalAlignment = Alignment.CenterVertically) {
                FilledTonalButton(onClick = onAction) { Text(action) }
                extra?.invoke()
            }
        }
    }
}
