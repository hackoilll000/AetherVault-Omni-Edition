package com.example.ui.theme

import android.os.Build
import androidx.compose.foundation.isSystemInDarkTheme
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.*
import androidx.compose.runtime.Composable
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.unit.dp
import com.example.features.studio.ThemeMode
import com.example.features.studio.ThemePreset

private val M3LightColorScheme = lightColorScheme(
    primary = Color(0xFF0B57D0),
    onPrimary = Color(0xFFFFFFFF),
    primaryContainer = Color(0xFFD3E3FD),
    onPrimaryContainer = Color(0xFF041E49),
    secondary = Color(0xFF00639B),
    onSecondary = Color(0xFFFFFFFF),
    secondaryContainer = Color(0xFFC2E7FF),
    onSecondaryContainer = Color(0xFF001D35),
    tertiary = Color(0xFF7D5260),
    onTertiary = Color(0xFFFFFFFF),
    tertiaryContainer = Color(0xFFFFD8E4),
    onTertiaryContainer = Color(0xFF31111D),
    background = Color(0xFFF8FAFD),
    onBackground = Color(0xFF1F1F1F),
    surface = Color(0xFFFFFFFF),
    onSurface = Color(0xFF1F1F1F),
    surfaceVariant = Color(0xFFF0F4F9),
    onSurfaceVariant = Color(0xFF444746),
    surfaceContainer = Color(0xFFF0F4F9),
    surfaceContainerHigh = Color(0xFFEEF2F8),
    surfaceContainerHighest = Color(0xFFE2E7F0),
    outline = Color(0xFFE1E3E1),
    outlineVariant = Color(0xFFC4C7C5)
)

private val M3DarkColorScheme = darkColorScheme(
    primary = Color(0xFFA8C7FA),
    onPrimary = Color(0xFF003062),
    primaryContainer = Color(0xFF0842A0),
    onPrimaryContainer = Color(0xFFD3E3FD),
    secondary = Color(0xFF7FCFFF),
    onSecondary = Color(0xFF00344F),
    secondaryContainer = Color(0xFF004B72),
    onSecondaryContainer = Color(0xFFC2E7FF),
    tertiary = Color(0xFFEFB8C8),
    onTertiary = Color(0xFF492532),
    tertiaryContainer = Color(0xFF633B48),
    onTertiaryContainer = Color(0xFFFFD8E4),
    background = Color(0xFF111318),
    onBackground = Color(0xFFE2E2E6),
    surface = Color(0xFF191C20),
    onSurface = Color(0xFFE2E2E6),
    surfaceVariant = Color(0xFF2B3038),
    onSurfaceVariant = Color(0xFFC4C7C5),
    surfaceContainer = Color(0xFF1F2329),
    surfaceContainerHigh = Color(0xFF282C33),
    surfaceContainerHighest = Color(0xFF333840),
    outline = Color(0xFF444746),
    outlineVariant = Color(0xFF35393E)
)

private val LiquidGlassColorScheme = darkColorScheme(
    primary = LiquidAccentCyan,
    secondary = LiquidAccentIndigo,
    tertiary = LiquidAccentViolet,
    background = LiquidBackground,
    surface = LiquidSurface,
    surfaceVariant = LiquidCard,
    surfaceContainer = LiquidCard,
    outline = LiquidBorder,
    onPrimary = Color.Black,
    onSecondary = Color.White,
    onBackground = Color(0xFFF8FAFC),
    onSurface = Color(0xFFF8FAFC),
    onSurfaceVariant = Color(0xFF94A3B8)
)

private val OledColorScheme = darkColorScheme(
    primary = OledAccentPrimary,
    secondary = OledAccentSecondary,
    tertiary = CyanNeon,
    background = OledBackground,
    surface = OledSurface,
    surfaceVariant = OledCard,
    surfaceContainer = OledElevated,
    outline = OledBorder,
    onPrimary = Color.Black,
    onSecondary = Color.White,
    onBackground = OledTextPrimary,
    onSurface = OledTextPrimary,
    onSurfaceVariant = OledTextSecondary
)

private val FrostedGlassColorScheme = darkColorScheme(
    primary = GlassAccent,
    secondary = VioletNeon,
    tertiary = CyanNeon,
    background = GlassBackground,
    surface = GlassSurface,
    surfaceVariant = GlassCard,
    surfaceContainer = GlassCard,
    outline = GlassBorder,
    onPrimary = Color.Black,
    onSecondary = Color.White,
    onBackground = Color(0xFFF1F5F9),
    onSurface = Color(0xFFF1F5F9),
    onSurfaceVariant = Color(0xFFCBD5E1)
)

private val DotMatrixColorScheme = darkColorScheme(
    primary = DotMatrixAccent,
    secondary = Color.White,
    tertiary = DotMatrixAccent,
    background = DotMatrixBackground,
    surface = DotMatrixSurface,
    surfaceVariant = DotMatrixCard,
    surfaceContainer = DotMatrixCard,
    outline = Color(0xFF333333),
    onPrimary = Color.White,
    onSecondary = Color.Black,
    onBackground = DotMatrixText,
    onSurface = DotMatrixText,
    onSurfaceVariant = DotMatrixTextMuted
)

private val AlabasterColorScheme = lightColorScheme(
    primary = AlabasterAccent,
    secondary = IndigoHyper,
    tertiary = CyanNeon,
    background = AlabasterBackground,
    surface = AlabasterSurface,
    surfaceVariant = AlabasterCard,
    surfaceContainer = AlabasterCard,
    outline = Color(0xFFE2E8F0),
    onPrimary = Color.White,
    onSecondary = Color.White,
    onBackground = AlabasterText,
    onSurface = AlabasterText,
    onSurfaceVariant = AlabasterTextSecondary
)

private val ObsidianGoldColorScheme = darkColorScheme(
    primary = Color(0xFFD4AF37),
    onPrimary = Color(0xFF1A1400),
    primaryContainer = Color(0xFF382D07),
    onPrimaryContainer = Color(0xFFF9E79F),
    secondary = Color(0xFFE6CA65),
    onSecondary = Color(0xFF1E1700),
    secondaryContainer = Color(0xFF30270A),
    onSecondaryContainer = Color(0xFFFFF2C2),
    background = Color(0xFF0C0C0D),
    onBackground = Color(0xFFE8E8E8),
    surface = Color(0xFF151517),
    onSurface = Color(0xFFE8E8E8),
    surfaceVariant = Color(0xFF222226),
    onSurfaceVariant = Color(0xFFAAAAAA),
    outline = Color(0xFF444444)
)

private val MidnightPlatinumColorScheme = darkColorScheme(
    primary = Color(0xFF82AAFF),
    onPrimary = Color(0xFF081A38),
    primaryContainer = Color(0xFF152A4E),
    onPrimaryContainer = Color(0xFFD4E2FF),
    secondary = Color(0xFFB0BEC5),
    onSecondary = Color(0xFF102027),
    secondaryContainer = Color(0xFF263238),
    onSecondaryContainer = Color(0xFFECEFF1),
    background = Color(0xFF090D16),
    onBackground = Color(0xFFE2E8F0),
    surface = Color(0xFF111827),
    onSurface = Color(0xFFE2E8F0),
    surfaceVariant = Color(0xFF1F2937),
    onSurfaceVariant = Color(0xFF94A3B8),
    outline = Color(0xFF374151)
)

private val TitaniumIceColorScheme = darkColorScheme(
    primary = Color(0xFF38BDF8),
    onPrimary = Color(0xFF082F49),
    primaryContainer = Color(0xFF0C4A6E),
    onPrimaryContainer = Color(0xFFBAE6FD),
    secondary = Color(0xFF94A3B8),
    onSecondary = Color(0xFF0F172A),
    secondaryContainer = Color(0xFF1E293B),
    onSecondaryContainer = Color(0xFFE2E8F0),
    background = Color(0xFF0F172A),
    onBackground = Color(0xFFF8FAFC),
    surface = Color(0xFF1E293B),
    onSurface = Color(0xFFF8FAFC),
    surfaceVariant = Color(0xFF334155),
    onSurfaceVariant = Color(0xFFCBD5E1),
    outline = Color(0xFF475569)
)

private val PureAmoledColorScheme = darkColorScheme(
    primary = Color(0xFF3B82F6),
    onPrimary = Color(0xFFFFFFFF),
    primaryContainer = Color(0xFF1D4ED8),
    onPrimaryContainer = Color(0xFFDBEAFE),
    secondary = Color(0xFF60A5FA),
    onSecondary = Color(0xFF000000),
    secondaryContainer = Color(0xFF1E3A8A),
    onSecondaryContainer = Color(0xFFBFDBFE),
    background = Color(0xFF000000),
    onBackground = Color(0xFFFFFFFF),
    surface = Color(0xFF000000),
    onSurface = Color(0xFFFFFFFF),
    surfaceVariant = Color(0xFF141414),
    onSurfaceVariant = Color(0xFFAAAAAA),
    outline = Color(0xFF2A2A2A)
)

private val MinimalPerformanceColorScheme = lightColorScheme(
    primary = Color(0xFF1E293B),
    onPrimary = Color(0xFFFFFFFF),
    primaryContainer = Color(0xFFE2E8F0),
    onPrimaryContainer = Color(0xFF0F172A),
    background = Color(0xFFF8FAFC),
    onBackground = Color(0xFF0F172A),
    surface = Color(0xFFFFFFFF),
    onSurface = Color(0xFF0F172A),
    surfaceVariant = Color(0xFFF1F5F9),
    onSurfaceVariant = Color(0xFF475569),
    outline = Color(0xFFCBD5E1)
)

@Composable
fun AetherVaultTheme(
    themeMode: ThemeMode = ThemeMode.SYSTEM_AUTO,
    preset: ThemePreset = ThemePreset.MATERIAL_YOU,
    cornerRadiusDp: Int = 16,
    content: @Composable () -> Unit
) {
    val context = LocalContext.current
    val systemInDark = isSystemInDarkTheme()

    val isDark = when (themeMode) {
        ThemeMode.SYSTEM_AUTO -> systemInDark
        ThemeMode.LIGHT -> false
        ThemeMode.DARK -> true
    }

    val colorScheme = when (preset) {
        ThemePreset.MATERIAL_YOU -> {
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.S) {
                if (isDark) dynamicDarkColorScheme(context) else dynamicLightColorScheme(context)
            } else {
                if (isDark) M3DarkColorScheme else M3LightColorScheme
            }
        }
        ThemePreset.OBSIDIAN_GOLD -> if (isDark) ObsidianGoldColorScheme else M3LightColorScheme
        ThemePreset.MIDNIGHT_PLATINUM -> if (isDark) MidnightPlatinumColorScheme else M3LightColorScheme
        ThemePreset.TITANIUM_ICE -> if (isDark) TitaniumIceColorScheme else M3LightColorScheme
        ThemePreset.PURE_AMOLED -> if (isDark) PureAmoledColorScheme else M3LightColorScheme
        ThemePreset.MINIMAL_PERFORMANCE -> if (isDark) M3DarkColorScheme else MinimalPerformanceColorScheme
        ThemePreset.ALABASTER_CLEAN -> if (isDark) M3DarkColorScheme else AlabasterColorScheme
        ThemePreset.OLED_STEALTH -> if (isDark) OledColorScheme else M3LightColorScheme
        ThemePreset.FROSTED_GLASS -> if (isDark) FrostedGlassColorScheme else M3LightColorScheme
        ThemePreset.DOT_MATRIX -> if (isDark) DotMatrixColorScheme else M3LightColorScheme
        ThemePreset.LIQUID_GLASS -> if (isDark) LiquidGlassColorScheme else M3LightColorScheme
    }

    val dynamicShapes = Shapes(
        extraSmall = RoundedCornerShape((cornerRadiusDp / 4).coerceAtLeast(2).dp),
        small = RoundedCornerShape((cornerRadiusDp / 2).coerceAtLeast(4).dp),
        medium = RoundedCornerShape(cornerRadiusDp.dp),
        large = RoundedCornerShape((cornerRadiusDp * 1.5).toInt().dp),
        extraLarge = RoundedCornerShape((cornerRadiusDp * 2).dp)
    )

    MaterialTheme(
        colorScheme = colorScheme,
        shapes = dynamicShapes,
        typography = Typography,
        content = content
    )
}
