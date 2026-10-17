package com.example.ui.theme

import android.os.Build
import androidx.compose.foundation.isSystemInDarkTheme
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.*
import androidx.compose.runtime.Composable
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.unit.dp
import com.example.features.studio.ThemePreset

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

@Composable
fun AetherVaultTheme(
    preset: ThemePreset = ThemePreset.LIQUID_GLASS,
    cornerRadiusDp: Int = 16,
    content: @Composable () -> Unit
) {
    val context = LocalContext.current
    val darkTheme = isSystemInDarkTheme()

    val colorScheme = when (preset) {
        ThemePreset.LIQUID_GLASS -> LiquidGlassColorScheme
        ThemePreset.OLED_STEALTH -> OledColorScheme
        ThemePreset.FROSTED_GLASS -> FrostedGlassColorScheme
        ThemePreset.DOT_MATRIX -> DotMatrixColorScheme
        ThemePreset.MATERIAL_YOU -> {
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.S) {
                if (darkTheme) dynamicDarkColorScheme(context) else dynamicLightColorScheme(context)
            } else {
                LiquidGlassColorScheme
            }
        }
        ThemePreset.ALABASTER_CLEAN -> AlabasterColorScheme
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
