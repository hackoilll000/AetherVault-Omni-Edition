package com.example.features.studio

import android.content.Context
import android.content.SharedPreferences
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow

enum class ThemeMode {
    SYSTEM_AUTO,
    LIGHT,
    DARK
}

enum class ThemePreset(val displayNameEn: String, val displayNameHi: String) {
    MATERIAL_YOU("Material You Tonal", "मटीरियल यू टोनल"),
    OBSIDIAN_GOLD("Obsidian Gold (Linear Luxury)", "ओब्सीडियन गोल्ड (लक्जरी)"),
    MIDNIGHT_PLATINUM("Midnight Platinum (Tech)", "मिडनाइट प्लैटिनम"),
    TITANIUM_ICE("Titanium Ice (Clean)", "टाइटेनियम आइस"),
    PURE_AMOLED("Pure AMOLED (Deep Black)", "प्योर एमोलेड (डीप ब्लैक)"),
    MINIMAL_PERFORMANCE("Minimal Performance (Fast)", "मिनिमल परफॉरमेंस"),
    ALABASTER_CLEAN("Alabaster Clean", "एलेबास्टर क्लीन"),
    OLED_STEALTH("OLED Stealth", "ओलेड स्टेल्थ"),
    FROSTED_GLASS("Frosted Glass", "फ्रॉस्टेड ग्लास"),
    DOT_MATRIX("Dot Matrix (Nothing OS)", "डॉट मैट्रिक्स"),
    LIQUID_GLASS("Liquid Glass", "लिक्विड ग्लास")
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
    val themeMode: ThemeMode = ThemeMode.SYSTEM_AUTO,
    val themePreset: ThemePreset = ThemePreset.MATERIAL_YOU,
    val cornerRadiusDp: Int = 16,
    val gridColumns: Int = 3,
    val aspectRatioMode: AspectRatioMode = AspectRatioMode.SQUARE,
    val navBarMode: NavBarMode = NavBarMode.FLOATING_PILL,
    val showTelemetryOverlay: Boolean = false,
    val language: AppLanguage = AppLanguage.EN,
    val lowMemoryOptimization: Boolean = false,
    val soundMuted: Boolean = false,
    val hasChosenUiStyle: Boolean = false,
    val privacyPreviewEnabled: Boolean = false
)

class AppearancePreferences(context: Context) {
    private val prefs: SharedPreferences = context.getSharedPreferences("aethervault_appearance", Context.MODE_PRIVATE)

    private val _configFlow = MutableStateFlow(loadConfig())
    val configFlow: StateFlow<AppearanceConfig> = _configFlow.asStateFlow()

    private fun loadConfig(): AppearanceConfig {
        val modeStr = prefs.getString("theme_mode", ThemeMode.SYSTEM_AUTO.name) ?: ThemeMode.SYSTEM_AUTO.name
        val themeMode = runCatching { ThemeMode.valueOf(modeStr) }.getOrDefault(ThemeMode.SYSTEM_AUTO)

        val themeStr = prefs.getString("theme", ThemePreset.MATERIAL_YOU.name) ?: ThemePreset.MATERIAL_YOU.name
        val theme = runCatching { ThemePreset.valueOf(themeStr) }.getOrDefault(ThemePreset.MATERIAL_YOU)

        val radius = prefs.getInt("cornerRadius", 16)
        val cols = prefs.getInt("gridColumns", 3)
        val aspectStr = prefs.getString("aspectRatio", AspectRatioMode.SQUARE.name) ?: AspectRatioMode.SQUARE.name
        val aspect = runCatching { AspectRatioMode.valueOf(aspectStr) }.getOrDefault(AspectRatioMode.SQUARE)
        val navStr = prefs.getString("navBarMode", NavBarMode.FLOATING_PILL.name) ?: NavBarMode.FLOATING_PILL.name
        val nav = runCatching { NavBarMode.valueOf(navStr) }.getOrDefault(NavBarMode.FLOATING_PILL)
        val telemetry = prefs.getBoolean("telemetry", false)
        val langStr = prefs.getString("lang", AppLanguage.EN.name) ?: AppLanguage.EN.name
        val lang = runCatching { AppLanguage.valueOf(langStr) }.getOrDefault(AppLanguage.EN)
        val lowMem = prefs.getBoolean("low_memory_opt", false)
        val muted = prefs.getBoolean("sound_muted", false)
        val hasChosen = prefs.getBoolean("has_chosen_ui_style", false)
        val privacy = prefs.getBoolean("privacy_preview_enabled", false)

        return AppearanceConfig(
            themeMode = themeMode,
            themePreset = theme,
            cornerRadiusDp = radius,
            gridColumns = cols,
            aspectRatioMode = aspect,
            navBarMode = nav,
            showTelemetryOverlay = telemetry,
            language = lang,
            lowMemoryOptimization = lowMem,
            soundMuted = muted,
            hasChosenUiStyle = hasChosen,
            privacyPreviewEnabled = privacy
        )
    }

    fun togglePrivacyPreview(enabled: Boolean) {
        prefs.edit().putBoolean("privacy_preview_enabled", enabled).apply()
        _configFlow.value = _configFlow.value.copy(privacyPreviewEnabled = enabled)
    }

    fun updateThemeMode(mode: ThemeMode) {
        prefs.edit().putString("theme_mode", mode.name).putBoolean("has_chosen_ui_style", true).apply()
        _configFlow.value = _configFlow.value.copy(themeMode = mode, hasChosenUiStyle = true)
    }

    fun markUiStyleChosen() {
        prefs.edit().putBoolean("has_chosen_ui_style", true).apply()
        _configFlow.value = _configFlow.value.copy(hasChosenUiStyle = true)
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
