package com.ibrah.abonelik

import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.SystemBarStyle
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.compose.foundation.isSystemInDarkTheme
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.runtime.DisposableEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.remember
import androidx.compose.ui.Modifier
import androidx.lifecycle.lifecycleScope
import com.ibrah.abonelik.core.ThemeMode
import com.ibrah.abonelik.data.SettingsStore
import com.ibrah.abonelik.reminders.CatchUp
import com.ibrah.abonelik.ui.AppNav
import com.ibrah.abonelik.ui.theme.AppTheme
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch

class MainActivity : ComponentActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        enableEdgeToEdge()
        setContent {
            val store = remember { SettingsStore(applicationContext) }
            val mode by store.theme.collectAsState(initial = ThemeMode.SYSTEM)
            val dark = mode.isDark(isSystemInDarkTheme())
            // Durum çubuğu simgeleri seçilen temaya uysun (koyu temada açık renk simgeler).
            DisposableEffect(dark) {
                val style = if (dark) SystemBarStyle.dark(android.graphics.Color.TRANSPARENT)
                else SystemBarStyle.light(android.graphics.Color.TRANSPARENT, android.graphics.Color.TRANSPARENT)
                enableEdgeToEdge(statusBarStyle = style, navigationBarStyle = style)
                onDispose {}
            }
            AppTheme(dark = dark) {
                Surface(Modifier.fillMaxSize(), color = MaterialTheme.colorScheme.background) { AppNav() }
            }
        }
    }

    /** Her açılışta kaçırılan hatırlatmaları telafi et ve günlük alarmı yeniden kur. */
    override fun onResume() {
        super.onResume()
        lifecycleScope.launch(Dispatchers.IO) { runCatching { CatchUp.run(applicationContext) } }
    }
}
