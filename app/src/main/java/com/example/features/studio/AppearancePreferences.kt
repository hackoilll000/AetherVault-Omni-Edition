package com.example.features.studio

import android.content.Context
import android.content.SharedPreferences
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow

enum class ThemePreset {
    LIQUID_GLASS,
    OLED_STEALTH,
    FROSTED_GLASS,
    DOT_MATRIX,
    MATERIAL_YOU,
    ALABASTER_CLEAN
}

enum class AspectRatioMode {
    SQUARE,
    MASONRY
}

enum class NavBarMode {
    FLOATING_PILL,
    DOCKED,
    AUTO_HIDE
}

enum class AppLanguage {
    EN,
    HI
}

data class AppearanceConfig(
    val themePreset: ThemePreset = ThemePreset.LIQUID_GLASS,
    val cornerRadiusDp: Int = 16,
    val gridColumns: Int = 3,
    val aspectRatioMode: AspectRatioMode = AspectRatioMode.SQUARE,
    val navBarMode: NavBarMode = NavBarMode.FLOATING_PILL,
    val showTelemetryOverlay: Boolean = true,
    val language: AppLanguage = AppLanguage.EN,
    val lowMemoryOptimization: Boolean = false,
    val soundMuted: Boolean = false
)

class AppearancePreferences(context: Context) {
    private val prefs: SharedPreferences = context.getSharedPreferences("aethervault_appearance", Context.MODE_PRIVATE)

    private val _configFlow = MutableStateFlow(loadConfig())
    val configFlow: StateFlow<AppearanceConfig> = _configFlow.asStateFlow()

    private fun loadConfig(): AppearanceConfig {
        val themeStr = prefs.getString("theme", ThemePreset.LIQUID_GLASS.name) ?: ThemePreset.LIQUID_GLASS.name
        val theme = runCatching { ThemePreset.valueOf(themeStr) }.getOrDefault(ThemePreset.LIQUID_GLASS)
        val radius = prefs.getInt("cornerRadius", 16)
        val cols = prefs.getInt("gridColumns", 3)
        val aspectStr = prefs.getString("aspectRatio", AspectRatioMode.SQUARE.name) ?: AspectRatioMode.SQUARE.name
        val aspect = runCatching { AspectRatioMode.valueOf(aspectStr) }.getOrDefault(AspectRatioMode.SQUARE)
        val navStr = prefs.getString("navBarMode", NavBarMode.FLOATING_PILL.name) ?: NavBarMode.FLOATING_PILL.name
        val nav = runCatching { NavBarMode.valueOf(navStr) }.getOrDefault(NavBarMode.FLOATING_PILL)
        val telemetry = prefs.getBoolean("telemetry", true)
        val langStr = prefs.getString("lang", AppLanguage.EN.name) ?: AppLanguage.EN.name
        val lang = runCatching { AppLanguage.valueOf(langStr) }.getOrDefault(AppLanguage.EN)
        val lowMem = prefs.getBoolean("low_memory_opt", false)
        val muted = prefs.getBoolean("sound_muted", false)

        return AppearanceConfig(
            themePreset = theme,
            cornerRadiusDp = radius,
            gridColumns = cols,
            aspectRatioMode = aspect,
            navBarMode = nav,
            showTelemetryOverlay = telemetry,
            language = lang,
            lowMemoryOptimization = lowMem,
            soundMuted = muted
        )
    }

    fun updateTheme(preset: ThemePreset) {
        prefs.edit().putString("theme", preset.name).apply()
        _configFlow.value = _configFlow.value.copy(themePreset = preset)
    }

    fun updateCornerRadius(radiusDp: Int) {
        prefs.edit().putInt("cornerRadius", radiusDp).apply()
        _configFlow.value = _configFlow.value.copy(cornerRadiusDp = radiusDp)
    }

    fun updateGridColumns(cols: Int) {
        prefs.edit().putInt("gridColumns", cols).apply()
        _configFlow.value = _configFlow.value.copy(gridColumns = cols)
    }

    fun updateAspectRatio(mode: AspectRatioMode) {
        prefs.edit().putString("aspectRatio", mode.name).apply()
        _configFlow.value = _configFlow.value.copy(aspectRatioMode = mode)
    }

    fun updateNavBarMode(mode: NavBarMode) {
        prefs.edit().putString("navBarMode", mode.name).apply()
        _configFlow.value = _configFlow.value.copy(navBarMode = mode)
    }

    fun toggleTelemetry(enabled: Boolean) {
        prefs.edit().putBoolean("telemetry", enabled).apply()
        _configFlow.value = _configFlow.value.copy(showTelemetryOverlay = enabled)
    }

    fun updateLanguage(lang: AppLanguage) {
        prefs.edit().putString("lang", lang.name).apply()
        _configFlow.value = _configFlow.value.copy(language = lang)
    }

    fun toggleLowMemoryMode(enabled: Boolean) {
        prefs.edit().putBoolean("low_memory_opt", enabled).apply()
        _configFlow.value = _configFlow.value.copy(lowMemoryOptimization = enabled)
    }

    fun toggleSoundMute(muted: Boolean) {
        prefs.edit().putBoolean("sound_muted", muted).apply()
        _configFlow.value = _configFlow.value.copy(soundMuted = muted)
    }
}
