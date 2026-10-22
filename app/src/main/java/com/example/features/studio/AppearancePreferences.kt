package com.example.features.studio

import android.content.Context
import android.content.SharedPreferences
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import org.json.JSONObject
import kotlin.math.pow

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
    LIQUID_GLASS("Liquid Glass", "लिक्विड ग्लास"),
    CUSTOM_PALETTE("Custom User Palette", "कस्टम यूज़र पैलेट")
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

enum class FontFamilyMode {
    DEFAULT,
    MONOSPACE,
    SERIF,
    SANS_SERIF
}

enum class AnimationSpeed {
    OFF,
    SLOW,
    NORMAL,
    FAST
}

enum class HapticIntensity {
    NONE,
    LIGHT,
    MEDIUM,
    HEAVY
}

enum class DensityMode {
    COMPACT,
    COMFORTABLE,
    SPACIOUS
}

data class CustomPalette(
    val primaryHex: String = "#00F2FE",
    val secondaryHex: String = "#4FACFE",
    val tertiaryHex: String = "#7F00FF",
    val surfaceHex: String = "#121824",
    val backgroundHex: String = "#0A0D14",
    val cardHex: String = "#1E2638",
    val codeEditorBgHex: String = "#10141D",
    val codeEditorTextHex: String = "#00E676"
)

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
    val privacyPreviewEnabled: Boolean = false,
    val customPalette: CustomPalette = CustomPalette(),
    val fontFamilyMode: FontFamilyMode = FontFamilyMode.DEFAULT,
    val fontScale: Float = 1.0f,
    val animationSpeed: AnimationSpeed = AnimationSpeed.NORMAL,
    val reducedMotion: Boolean = false,
    val hapticIntensity: HapticIntensity = HapticIntensity.MEDIUM,
    val densityMode: DensityMode = DensityMode.COMFORTABLE
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

        val customPalette = CustomPalette(
            primaryHex = prefs.getString("custom_primary", "#00F2FE") ?: "#00F2FE",
            secondaryHex = prefs.getString("custom_secondary", "#4FACFE") ?: "#4FACFE",
            tertiaryHex = prefs.getString("custom_tertiary", "#7F00FF") ?: "#7F00FF",
            surfaceHex = prefs.getString("custom_surface", "#121824") ?: "#121824",
            backgroundHex = prefs.getString("custom_background", "#0A0D14") ?: "#0A0D14",
            cardHex = prefs.getString("custom_card", "#1E2638") ?: "#1E2638",
            codeEditorBgHex = prefs.getString("custom_code_bg", "#10141D") ?: "#10141D",
            codeEditorTextHex = prefs.getString("custom_code_text", "#00E676") ?: "#00E676"
        )

        val fontStr = prefs.getString("font_family", FontFamilyMode.DEFAULT.name) ?: FontFamilyMode.DEFAULT.name
        val font = runCatching { FontFamilyMode.valueOf(fontStr) }.getOrDefault(FontFamilyMode.DEFAULT)
        val fontScale = prefs.getFloat("font_scale", 1.0f)

        val animSpeedStr = prefs.getString("anim_speed", AnimationSpeed.NORMAL.name) ?: AnimationSpeed.NORMAL.name
        val animSpeed = runCatching { AnimationSpeed.valueOf(animSpeedStr) }.getOrDefault(AnimationSpeed.NORMAL)
        val redMotion = prefs.getBoolean("reduced_motion", false)

        val hapticStr = prefs.getString("haptic_intensity", HapticIntensity.MEDIUM.name) ?: HapticIntensity.MEDIUM.name
        val haptic = runCatching { HapticIntensity.valueOf(hapticStr) }.getOrDefault(HapticIntensity.MEDIUM)

        val densityStr = prefs.getString("density_mode", DensityMode.COMFORTABLE.name) ?: DensityMode.COMFORTABLE.name
        val density = runCatching { DensityMode.valueOf(densityStr) }.getOrDefault(DensityMode.COMFORTABLE)

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
            privacyPreviewEnabled = privacy,
            customPalette = customPalette,
            fontFamilyMode = font,
            fontScale = fontScale,
            animationSpeed = animSpeed,
            reducedMotion = redMotion,
            hapticIntensity = haptic,
            densityMode = density
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

    fun updateCustomPalette(palette: CustomPalette, setAsActivePreset: Boolean = true) {
        prefs.edit()
            .putString("custom_primary", palette.primaryHex)
            .putString("custom_secondary", palette.secondaryHex)
            .putString("custom_tertiary", palette.tertiaryHex)
            .putString("custom_surface", palette.surfaceHex)
            .putString("custom_background", palette.backgroundHex)
            .putString("custom_card", palette.cardHex)
            .putString("custom_code_bg", palette.codeEditorBgHex)
            .putString("custom_code_text", palette.codeEditorTextHex)
            .apply()

        if (setAsActivePreset) {
            prefs.edit().putString("theme", ThemePreset.CUSTOM_PALETTE.name).apply()
            _configFlow.value = _configFlow.value.copy(
                customPalette = palette,
                themePreset = ThemePreset.CUSTOM_PALETTE
            )
        } else {
            _configFlow.value = _configFlow.value.copy(customPalette = palette)
        }
    }

    fun updateFontFamily(fontMode: FontFamilyMode) {
        prefs.edit().putString("font_family", fontMode.name).apply()
        _configFlow.value = _configFlow.value.copy(fontFamilyMode = fontMode)
    }

    fun updateFontScale(scale: Float) {
        prefs.edit().putFloat("font_scale", scale).apply()
        _configFlow.value = _configFlow.value.copy(fontScale = scale)
    }

    fun updateAnimationSpeed(speed: AnimationSpeed) {
        prefs.edit().putString("anim_speed", speed.name).apply()
        _configFlow.value = _configFlow.value.copy(animationSpeed = speed)
    }

    fun toggleReducedMotion(enabled: Boolean) {
        prefs.edit().putBoolean("reduced_motion", enabled).apply()
        _configFlow.value = _configFlow.value.copy(reducedMotion = enabled)
    }

    fun updateHapticIntensity(intensity: HapticIntensity) {
        prefs.edit().putString("haptic_intensity", intensity.name).apply()
        _configFlow.value = _configFlow.value.copy(hapticIntensity = intensity)
    }

    fun updateDensityMode(mode: DensityMode) {
        prefs.edit().putString("density_mode", mode.name).apply()
        _configFlow.value = _configFlow.value.copy(densityMode = mode)
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

    fun exportProfileJson(): String {
        val cfg = _configFlow.value
        val json = JSONObject()
        json.put("version", 1)
        json.put("themeMode", cfg.themeMode.name)
        json.put("themePreset", cfg.themePreset.name)
        json.put("cornerRadiusDp", cfg.cornerRadiusDp)
        json.put("gridColumns", cfg.gridColumns)
        json.put("aspectRatioMode", cfg.aspectRatioMode.name)
        json.put("navBarMode", cfg.navBarMode.name)
        json.put("language", cfg.language.name)
        json.put("fontFamilyMode", cfg.fontFamilyMode.name)
        json.put("fontScale", cfg.fontScale)
        json.put("animationSpeed", cfg.animationSpeed.name)
        json.put("reducedMotion", cfg.reducedMotion)
        json.put("hapticIntensity", cfg.hapticIntensity.name)
        json.put("densityMode", cfg.densityMode.name)

        val paletteJson = JSONObject()
        paletteJson.put("primaryHex", cfg.customPalette.primaryHex)
        paletteJson.put("secondaryHex", cfg.customPalette.secondaryHex)
        paletteJson.put("tertiaryHex", cfg.customPalette.tertiaryHex)
        paletteJson.put("surfaceHex", cfg.customPalette.surfaceHex)
        paletteJson.put("backgroundHex", cfg.customPalette.backgroundHex)
        paletteJson.put("cardHex", cfg.customPalette.cardHex)
        paletteJson.put("codeEditorBgHex", cfg.customPalette.codeEditorBgHex)
        paletteJson.put("codeEditorTextHex", cfg.customPalette.codeEditorTextHex)
        json.put("customPalette", paletteJson)

        return json.toString(2)
    }

    fun importProfileJson(jsonStr: String): Boolean {
        return runCatching {
            val json = JSONObject(jsonStr)
            val themeMode = ThemeMode.valueOf(json.optString("themeMode", ThemeMode.SYSTEM_AUTO.name))
            val themePreset = ThemePreset.valueOf(json.optString("themePreset", ThemePreset.MATERIAL_YOU.name))
            val radiusDp = json.optInt("cornerRadiusDp", 16)
            val gridCols = json.optInt("gridColumns", 3)
            val aspectMode = AspectRatioMode.valueOf(json.optString("aspectRatioMode", AspectRatioMode.SQUARE.name))
            val navMode = NavBarMode.valueOf(json.optString("navBarMode", NavBarMode.FLOATING_PILL.name))
            val lang = AppLanguage.valueOf(json.optString("language", AppLanguage.EN.name))
            val fontMode = FontFamilyMode.valueOf(json.optString("fontFamilyMode", FontFamilyMode.DEFAULT.name))
            val fontScale = json.optDouble("fontScale", 1.0).toFloat()
            val animSpeed = AnimationSpeed.valueOf(json.optString("animationSpeed", AnimationSpeed.NORMAL.name))
            val reducedMotion = json.optBoolean("reducedMotion", false)
            val haptic = HapticIntensity.valueOf(json.optString("hapticIntensity", HapticIntensity.MEDIUM.name))
            val density = DensityMode.valueOf(json.optString("densityMode", DensityMode.COMFORTABLE.name))

            var customPalette = _configFlow.value.customPalette
            if (json.has("customPalette")) {
                val pJson = json.getJSONObject("customPalette")
                customPalette = CustomPalette(
                    primaryHex = pJson.optString("primaryHex", "#00F2FE"),
                    secondaryHex = pJson.optString("secondaryHex", "#4FACFE"),
                    tertiaryHex = pJson.optString("tertiaryHex", "#7F00FF"),
                    surfaceHex = pJson.optString("surfaceHex", "#121824"),
                    backgroundHex = pJson.optString("backgroundHex", "#0A0D14"),
                    cardHex = pJson.optString("cardHex", "#1E2638"),
                    codeEditorBgHex = pJson.optString("codeEditorBgHex", "#10141D"),
                    codeEditorTextHex = pJson.optString("codeEditorTextHex", "#00E676")
                )
            }

            updateThemeMode(themeMode)
            updateCustomPalette(customPalette, setAsActivePreset = (themePreset == ThemePreset.CUSTOM_PALETTE))
            updateTheme(themePreset)
            updateCornerRadius(radiusDp)
            updateGridColumns(gridCols)
            updateAspectRatio(aspectMode)
            updateNavBarMode(navMode)
            updateLanguage(lang)
            updateFontFamily(fontMode)
            updateFontScale(fontScale)
            updateAnimationSpeed(animSpeed)
            toggleReducedMotion(reducedMotion)
            updateHapticIntensity(haptic)
            updateDensityMode(density)

            true
        }.getOrDefault(false)
    }

    companion object {
        fun calculateContrastRatio(foregroundHex: String, backgroundHex: String): Double {
            return runCatching {
                val fgLuminance = calculateRelativeLuminance(foregroundHex)
                val bgLuminance = calculateRelativeLuminance(backgroundHex)
                val l1 = maxOf(fgLuminance, bgLuminance)
                val l2 = minOf(fgLuminance, bgLuminance)
                (l1 + 0.05) / (l2 + 0.05)
            }.getOrDefault(4.5)
        }

        private fun calculateRelativeLuminance(hex: String): Double {
            val cleanHex = hex.removePrefix("#")
            val colorInt = cleanHex.toLong(16).toInt()
            val r = ((colorInt shr 16) and 0xFF) / 255.0
            val g = ((colorInt shr 8) and 0xFF) / 255.0
            val b = (colorInt and 0xFF) / 255.0

            val rs = if (r <= 0.03928) r / 12.92 else ((r + 0.055) / 1.055).pow(2.4)
            val gs = if (g <= 0.03928) g / 12.92 else ((g + 0.055) / 1.055).pow(2.4)
            val bs = if (b <= 0.03928) b / 12.92 else ((b + 0.055) / 1.055).pow(2.4)

            return 0.2126 * rs + 0.7152 * gs + 0.0722 * bs
        }
    }
}
