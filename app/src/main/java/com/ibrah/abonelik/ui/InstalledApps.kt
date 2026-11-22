package com.ibrah.abonelik.ui

import android.content.Context
import android.content.Intent
import android.util.LruCache
import androidx.compose.foundation.Image
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Search
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.ModalBottomSheet
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Text
import androidx.compose.material3.rememberModalBottomSheetState
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.produceState
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.ImageBitmap
import androidx.compose.ui.graphics.asImageBitmap
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import androidx.core.graphics.drawable.toBitmap
import com.ibrah.abonelik.core.AppEntry
import com.ibrah.abonelik.core.AppMatch
import java.util.Locale
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext

/** Telefonda yüklü (başlatılabilir) uygulamalar ve simgeleri; internet gerekmez. */
object InstalledApps {
    private val icons = LruCache<String, ImageBitmap>(64)
    @Volatile private var cached: List<AppEntry>? = null

    suspend fun list(ctx: Context): List<AppEntry> = cached ?: withContext(Dispatchers.IO) {
        val pm = ctx.packageManager
        val main = Intent(Intent.ACTION_MAIN).addCategory(Intent.CATEGORY_LAUNCHER)
        pm.queryIntentActivities(main, 0)
            .map { AppEntry(it.activityInfo.packageName, it.loadLabel(pm).toString()) }
            .filter { it.pkg != ctx.packageName }
            .distinctBy { it.pkg }
            .sortedBy { it.label.lowercase(Locale.forLanguageTag("tr-TR")) }
            .also { cached = it }
    }

    /** Uygulama silinmişse null (abonelik baş harfe döner). */
    suspend fun icon(ctx: Context, pkg: String): ImageBitmap? {
        icons.get(pkg)?.let { return it }
        return withContext(Dispatchers.IO) {
            runCatching { ctx.packageManager.getApplicationIcon(pkg).toBitmap(144, 144).asImageBitmap() }
                .getOrNull()?.also { icons.put(pkg, it) }
        }
    }
}

@Composable
fun rememberAppIcon(pkg: String?): ImageBitmap? {
    val ctx = LocalContext.current
    val bmp by produceState<ImageBitmap?>(null, pkg) { value = pkg?.let { InstalledApps.icon(ctx, it) } }
    return bmp
}

@Composable
fun rememberInstalledApps(): List<AppEntry> {
    val ctx = LocalContext.current
    val list by produceState(emptyList<AppEntry>()) { value = InstalledApps.list(ctx) }
    return list
}

@Composable
fun AppIconImage(pkg: String, size: Dp, fallback: @Composable () -> Unit) {
    val bmp = rememberAppIcon(pkg)
    if (bmp == null) fallback() else Image(bmp, null, Modifier.size(size).clip(CircleShape))
}

/** Telefondaki uygulamalardan seçim: arama kutusu + simgeli liste. */
@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun AppChooserSheet(onPick: (AppEntry) -> Unit, onDismiss: () -> Unit) {
    val apps = rememberInstalledApps()
    var q by remember { mutableStateOf("") }
    val shown = remember(apps, q) {
        val f = AppMatch.fold(q)
        if (f.isEmpty()) apps else apps.filter { AppMatch.fold(it.label).contains(f) }
    }
    ModalBottomSheet(onDismissRequest = onDismiss, sheetState = rememberModalBottomSheetState(skipPartiallyExpanded = true),
        containerColor = MaterialTheme.colorScheme.surfaceContainerLowest) {
        Column(Modifier.padding(horizontal = 16.dp)) {
            Text("Telefondaki uygulamadan seç", style = MaterialTheme.typography.titleMedium, modifier = Modifier.padding(bottom = 10.dp))
            OutlinedTextField(q, { q = it }, Modifier.fillMaxWidth(), placeholder = { Text("Ara: Netflix, Spotify…") },
                leadingIcon = { Icon(Icons.Filled.Search, null) }, singleLine = true, shape = MaterialTheme.shapes.small)
            LazyColumn(Modifier.heightIn(max = 520.dp).padding(top = 8.dp)) {
                items(shown, key = { it.pkg }) { a ->
                    Row(Modifier.fillMaxWidth().clickable { onPick(a) }.padding(vertical = 10.dp),
                        verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(14.dp)) {
                        AppIconImage(a.pkg, 36.dp) { Box(Modifier.size(36.dp)) }
                        Text(a.label, style = MaterialTheme.typography.bodyLarge)
                    }
                }
            }
        }
    }
}
