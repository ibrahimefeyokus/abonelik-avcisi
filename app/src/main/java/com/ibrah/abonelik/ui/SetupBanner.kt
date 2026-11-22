package com.ibrah.abonelik.ui

import android.Manifest
import android.annotation.SuppressLint
import android.content.Context
import android.content.Intent
import android.content.pm.PackageManager
import android.net.Uri
import android.os.PowerManager
import android.provider.Settings
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.material3.FilledTonalButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.DisposableEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.unit.dp
import androidx.lifecycle.Lifecycle
import androidx.lifecycle.LifecycleEventObserver
import androidx.lifecycle.compose.LocalLifecycleOwner

object Setup {
    fun notificationsGranted(ctx: Context) =
        ctx.checkSelfPermission(Manifest.permission.POST_NOTIFICATIONS) == PackageManager.PERMISSION_GRANTED

    fun batteryUnrestricted(ctx: Context) =
        ctx.getSystemService(PowerManager::class.java).isIgnoringBatteryOptimizations(ctx.packageName)

    /** Realme/ColorOS'ta arka plan kapatmasın diye pil kısıtlamasını kaldırma ekranı; olmazsa uygulama ayrıntıları. */
    @SuppressLint("BatteryLife")
    fun openBattery(ctx: Context) {
        val pkg = Uri.parse("package:${ctx.packageName}")
        val intents = listOf(
            Intent(Settings.ACTION_REQUEST_IGNORE_BATTERY_OPTIMIZATIONS, pkg),
            Intent(Settings.ACTION_APPLICATION_DETAILS_SETTINGS, pkg),
        )
        val i = intents.firstOrNull { it.resolveActivity(ctx.packageManager) != null } ?: return
        runCatching { ctx.startActivity(i.addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)) }
    }
}

/** "Hatırlatmalar için kurulum eksik" bandı: bildirim izni ve pil kısıtlaması. Her açılışta yeniden denetlenir. */
@Composable
fun SetupBanner(onGranted: () -> Unit) {
    val ctx = LocalContext.current
    var tick by remember { mutableIntStateOf(0) }
    val owner = LocalLifecycleOwner.current
    DisposableEffect(owner) {
        val obs = LifecycleEventObserver { _, e -> if (e == Lifecycle.Event.ON_RESUME) tick++ }
        owner.lifecycle.addObserver(obs)
        onDispose { owner.lifecycle.removeObserver(obs) }
    }
    val launcher = rememberLauncherForActivityResult(ActivityResultContracts.RequestPermission()) { ok ->
        tick++
        if (ok) onGranted()
    }
    @Suppress("UNUSED_EXPRESSION") tick
    val notif = Setup.notificationsGranted(ctx)
    val battery = Setup.batteryUnrestricted(ctx)
    if (notif && battery) return

    Surface(Modifier.fillMaxWidth().padding(top = 8.dp), shape = MaterialTheme.shapes.medium,
        color = MaterialTheme.colorScheme.errorContainer) {
        Column(Modifier.padding(16.dp), verticalArrangement = Arrangement.spacedBy(10.dp)) {
            Text("Hatırlatmalar için kurulum eksik", style = MaterialTheme.typography.titleMedium,
                color = MaterialTheme.colorScheme.onErrorContainer)
            if (!notif) BannerRow("Bildirim izni yok: hatırlatmalar gelmez.", "İzin ver") {
                launcher.launch(Manifest.permission.POST_NOTIFICATIONS)
            }
            if (!battery) BannerRow("Pil kısıtlaması açık: telefon uygulamayı kapatıp hatırlatmayı geciktirebilir.", "Ayarla") {
                Setup.openBattery(ctx)
            }
        }
    }
}

@Composable
private fun BannerRow(text: String, action: String, onClick: () -> Unit) {
    Row(verticalAlignment = Alignment.CenterVertically) {
        Text(text, style = MaterialTheme.typography.bodyMedium, color = MaterialTheme.colorScheme.onErrorContainer,
            modifier = Modifier.weight(1f).padding(end = 8.dp))
        FilledTonalButton(onClick = onClick) { Text(action) }
    }
}
