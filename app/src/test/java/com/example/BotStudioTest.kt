package com.example

import com.example.features.botstudio.BotProject
import com.example.features.botstudio.BotProjectManager
import com.example.features.botstudio.BotStatus
import com.example.features.botstudio.BotTemplateType
import com.example.features.studio.AppearancePreferences
import com.example.features.studio.ThemeMode
import com.example.features.studio.ThemePreset
import org.junit.Assert.*
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner
import org.robolectric.RuntimeEnvironment

@RunWith(RobolectricTestRunner::class)
class BotStudioTest {

    @Test
    fun testPythonSyntaxValidator_ValidScript() {
        val manager = BotProjectManager(RuntimeEnvironment.getApplication())
        val script = """
            def hello():
                print("Hello World")
        """.trimIndent()

        val (isValid, message) = manager.validatePythonScript(script)
        assertTrue(isValid)
        assertTrue(message.contains("Passed"))
    }

    @Test
    fun testPythonSyntaxValidator_UnmatchedParentheses() {
        val manager = BotProjectManager(RuntimeEnvironment.getApplication())
        val script = """
            print("Unclosed paren"
        """.trimIndent()

        val (isValid, message) = manager.validatePythonScript(script)
        assertFalse(isValid)
        assertTrue(message.contains("Unmatched parentheses"))
    }

    @Test
    fun testTermuxScriptGeneration() {
        val manager = BotProjectManager(RuntimeEnvironment.getApplication())
        val project = BotProject(
            name = "Test Echo Bot",
            botTokenSecret = "123456:ABC-DEF"
        )

        val script = manager.generateTermuxSetupScript(project)
        assertTrue(script.contains("123456:ABC-DEF"))
        assertTrue(script.contains("python-telegram-bot"))
    }

    @Test
    fun testAppearancePreferencesExportImportJson() {
        val context = RuntimeEnvironment.getApplication()
        val prefs = AppearancePreferences(context)

        prefs.updateTheme(ThemePreset.OBSIDIAN_GOLD)
        prefs.updateThemeMode(ThemeMode.DARK)

        val exportedJson = prefs.exportProfileJson()
        assertTrue(exportedJson.contains("OBSIDIAN_GOLD"))

        val success = prefs.importProfileJson(exportedJson)
        assertTrue(success)
        assertEquals(ThemePreset.OBSIDIAN_GOLD, prefs.configFlow.value.themePreset)
    }

    @Test
    fun testContrastRatioCalculation() {
        val ratio = AppearancePreferences.calculateContrastRatio("#000000", "#FFFFFF")
        assertTrue(ratio >= 20.0) // Black on White is ~21:1
    }
}
