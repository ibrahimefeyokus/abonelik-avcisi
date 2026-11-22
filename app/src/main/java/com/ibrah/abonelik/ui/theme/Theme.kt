package com.ibrah.abonelik.ui.theme

import androidx.compose.foundation.isSystemInDarkTheme
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Shapes
import androidx.compose.material3.Typography
import androidx.compose.material3.darkColorScheme
import androidx.compose.material3.lightColorScheme
import androidx.compose.runtime.Composable
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp

/** Tek vurgu koyu zümrüt; uyarılar kehribar, gerisi sakin nötrler. */
object AppColors {
    val Brand = Color(0xFF0F7A5C)
    val BrandDeep = Color(0xFF0A5A44)
    val BrandSoft = Color(0xFFE2F2EC)
    val Amber = Color(0xFFD97706)
    val AmberSoft = Color(0xFFFEF3E2)
    val Red = Color(0xFFDC2626)

    /** Abonelik/kart renk seçenekleri (12). */
    val Palette = listOf(
        0xFFE50914, 0xFF1DB954, 0xFFFF0033, 0xFF3B82F6, 0xFF8B5CF6, 0xFFF59E0B,
        0xFF0EA5E9, 0xFFEC4899, 0xFF14B8A6, 0xFF64748B, 0xFFF97316, 0xFF0F7A5C,
    ).map { Color(it) }
}

private val Light = lightColorScheme(
    primary = AppColors.Brand,
    onPrimary = Color.White,
    primaryContainer = AppColors.BrandSoft,
    onPrimaryContainer = AppColors.BrandDeep,
    secondary = Color(0xFF475569),
    secondaryContainer = Color(0xFFEEF2F0),
    onSecondaryContainer = Color(0xFF1E293B),
    background = Color(0xFFF4F7F5),
    onBackground = Color(0xFF0F172A),
    surface = Color(0xFFF4F7F5),
    onSurface = Color(0xFF0F172A),
    surfaceContainerLowest = Color.White,
    surfaceContainerLow = Color.White,
    surfaceContainer = Color.White,
    surfaceContainerHigh = Color(0xFFEFF3F1),
    surfaceVariant = Color(0xFFEEF2F0),
    onSurfaceVariant = Color(0xFF64748B),
    outline = Color(0xFFCBD5E1),
    outlineVariant = Color(0xFFE2E8F0),
    error = AppColors.Red,
    errorContainer = AppColors.AmberSoft,
    onErrorContainer = Color(0xFF7C2D12),
)

private val Dark = darkColorScheme(
    primary = Color(0xFF5FD3AE),
    onPrimary = Color(0xFF00382A),
    primaryContainer = Color(0xFF0F3B2F),
    onPrimaryContainer = Color(0xFFC9F2E3),
    secondary = Color(0xFF94A3B8),
    secondaryContainer = Color(0xFF1E2633),
    onSecondaryContainer = Color(0xFFE2E8F0),
    background = Color(0xFF0B0F14),
    onBackground = Color(0xFFE5E9F0),
    surface = Color(0xFF0B0F14),
    onSurface = Color(0xFFE5E9F0),
    surfaceContainerLowest = Color(0xFF131A22),
    surfaceContainerLow = Color(0xFF131A22),
    surfaceContainer = Color(0xFF131A22),
    surfaceContainerHigh = Color(0xFF1A222D),
    surfaceVariant = Color(0xFF1A222D),
    onSurfaceVariant = Color(0xFF94A3B8),
    outline = Color(0xFF334155),
    outlineVariant = Color(0xFF1E293B),
    errorContainer = Color(0xFF3B2410),
    onErrorContainer = Color(0xFFFED7AA),
)

private val AppShapes = Shapes(
    small = RoundedCornerShape(12.dp),
    medium = RoundedCornerShape(20.dp),
    large = RoundedCornerShape(28.dp),
    extraLarge = RoundedCornerShape(32.dp),
)

private val base = Typography()
private val AppType = base.copy(
    displaySmall = base.displaySmall.copy(fontWeight = FontWeight.Bold, letterSpacing = (-0.5).sp),
    headlineMedium = base.headlineMedium.copy(fontWeight = FontWeight.Bold, letterSpacing = (-0.3).sp),
    titleLarge = base.titleLarge.copy(fontWeight = FontWeight.SemiBold),
    titleMedium = base.titleMedium.copy(fontWeight = FontWeight.SemiBold),
    labelLarge = base.labelLarge.copy(fontWeight = FontWeight.SemiBold, letterSpacing = 0.2.sp),
    labelMedium = TextStyle(fontSize = 12.sp, fontWeight = FontWeight.Medium, letterSpacing = 0.4.sp),
)

@Composable
fun AppTheme(dark: Boolean = isSystemInDarkTheme(), content: @Composable () -> Unit) {
    MaterialTheme(
        colorScheme = if (dark) Dark else Light,
        shapes = AppShapes,
        typography = AppType,
        content = content,
    )
}
