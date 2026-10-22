package com.example.features.studio

import android.content.ClipData
import android.content.ClipboardManager
import android.content.Context
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun AppearanceStudioScreen(
    currentConfig: AppearanceConfig,
    appearancePrefs: AppearancePreferences,
    onUpdateTheme: (ThemePreset) -> Unit,
    onUpdateRadius: (Int) -> Unit,
    onUpdateCols: (Int) -> Unit,
    onUpdateAspect: (AspectRatioMode) -> Unit,
    onUpdateNavBar: (NavBarMode) -> Unit,
    onToggleTelemetry: (Boolean) -> Unit,
    onUpdateLang: (AppLanguage) -> Unit,
    onBack: () -> Unit
) {
    val context = LocalContext.current
    val scrollState = rememberScrollState()
    val isHindi = currentConfig.language == AppLanguage.HI

    var showExportDialog by remember { mutableStateOf(false) }
    var showImportDialog by remember { mutableStateOf(false) }
    var importJsonInput by remember { mutableStateOf("") }
    var importError by remember { mutableStateOf<String?>(null) }
    var snackbarMessage by remember { mutableStateOf<String?>(null) }

    var primaryHexInput by remember(currentConfig) { mutableStateOf(currentConfig.customPalette.primaryHex) }
    var surfaceHexInput by remember(currentConfig) { mutableStateOf(currentConfig.customPalette.surfaceHex) }

    val contrastRatio = remember(primaryHexInput, surfaceHexInput) {
        AppearancePreferences.calculateContrastRatio(primaryHexInput, surfaceHexInput)
    }

    Scaffold(
        topBar = {
            TopAppBar(
                title = {
                    Text(
                        text = if (isHindi) "अपीयरेंस व A-Z कस्टम स्टूडियो" else "A-Z Customization Studio",
                        fontWeight = FontWeight.Bold
                    )
                },
                navigationIcon = {
                    IconButton(
                        onClick = onBack,
                        modifier = Modifier.testTag("appearance_back_button")
                    ) {
                        Icon(
                            imageVector = Icons.AutoMirrored.Filled.ArrowBack,
                            contentDescription = "Back"
                        )
                    }
                },
                actions = {
                    IconButton(onClick = { showExportDialog = true }) {
                        Icon(Icons.Default.IosShare, contentDescription = "Export Profile")
                    }
                    IconButton(onClick = { showImportDialog = true }) {
                        Icon(Icons.Default.FileDownload, contentDescription = "Import Profile")
                    }
                    FilterChip(
                        selected = currentConfig.language == AppLanguage.HI,
                        onClick = {
                            onUpdateLang(if (isHindi) AppLanguage.EN else AppLanguage.HI)
                        },
                        label = { Text(if (isHindi) "EN" else "हिंदी") },
                        modifier = Modifier.padding(end = 8.dp)
                    )
                },
                colors = TopAppBarDefaults.topAppBarColors(
                    containerColor = MaterialTheme.colorScheme.surface
                )
            )
        }
    ) { padding ->
        Box(modifier = Modifier.fillMaxSize()) {
            Column(
                modifier = Modifier
                    .fillMaxSize()
                    .padding(padding)
                    .verticalScroll(scrollState)
                    .padding(16.dp),
                verticalArrangement = Arrangement.spacedBy(20.dp)
            ) {
                // Theme Presets
                Text(
                    text = if (isHindi) "थीम प्रीसेट्स (लक्जरी व परफॉरमेंस)" else "Theme Presets & Palettes",
                    style = MaterialTheme.typography.titleMedium,
                    fontWeight = FontWeight.Bold
                )

                val presets = listOf(
                    Pair(ThemePreset.OLED_STEALTH, "OLED Pitch-Black (Pro Stealth)"),
                    Pair(ThemePreset.OBSIDIAN_GOLD, "Obsidian Gold (Linear Luxury)"),
                    Pair(ThemePreset.TITANIUM_ICE, "Titanium Ice (Clean Tech)"),
                    Pair(ThemePreset.FROSTED_GLASS, "Frosted Glassmorphic HUD"),
                    Pair(ThemePreset.DOT_MATRIX, "Nothing Dot-Matrix (Industrial)"),
                    Pair(ThemePreset.MATERIAL_YOU, "Material You Expressive (Monet)"),
                    Pair(ThemePreset.ALABASTER_CLEAN, "Daylight Alabaster Clean"),
                    Pair(ThemePreset.CUSTOM_PALETTE, "User Custom Color Palette")
                )

                presets.forEach { (preset, label) ->
                    val selected = currentConfig.themePreset == preset
                    Card(
                        modifier = Modifier
                            .fillMaxWidth()
                            .clip(RoundedCornerShape(currentConfig.cornerRadiusDp.dp))
                            .clickable { onUpdateTheme(preset) }
                            .border(
                                width = if (selected) 2.dp else 0.8.dp,
                                color = if (selected) MaterialTheme.colorScheme.primary else MaterialTheme.colorScheme.outline.copy(alpha = 0.3f),
                                shape = RoundedCornerShape(currentConfig.cornerRadiusDp.dp)
                            ),
                        colors = CardDefaults.cardColors(
                            containerColor = if (selected) MaterialTheme.colorScheme.primary.copy(alpha = 0.12f)
                            else MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.4f)
                        )
                    ) {
                        Row(
                            modifier = Modifier
                                .padding(14.dp)
                                .fillMaxWidth(),
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            RadioButton(
                                selected = selected,
                                onClick = { onUpdateTheme(preset) }
                            )
                            Spacer(modifier = Modifier.width(10.dp))
                            Text(
                                text = label,
                                fontWeight = if (selected) FontWeight.Bold else FontWeight.Normal,
                                fontSize = 14.sp
                            )
                        }
                    }
                }

                HorizontalDivider()

                // Custom Color Palette Editor
                Text(
                    text = "Custom Color Palette & Contrast Validator",
                    style = MaterialTheme.typography.titleMedium,
                    fontWeight = FontWeight.Bold
                )

                Card(
                    modifier = Modifier.fillMaxWidth(),
                    colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surfaceContainerHigh)
                ) {
                    Column(modifier = Modifier.padding(14.dp), verticalArrangement = Arrangement.spacedBy(10.dp)) {
                        Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                            OutlinedTextField(
                                value = primaryHexInput,
                                onValueChange = { primaryHexInput = it },
                                label = { Text("Primary HEX") },
                                modifier = Modifier.weight(1f)
                            )
                            OutlinedTextField(
                                value = surfaceHexInput,
                                onValueChange = { surfaceHexInput = it },
                                label = { Text("Surface HEX") },
                                modifier = Modifier.weight(1f)
                            )
                        }

                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.SpaceBetween,
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Text(
                                text = "Contrast Ratio: ${"%.2f".format(contrastRatio)}:1 ${if (contrastRatio >= 4.5) "(WCAG AA Pass)" else "(Low Contrast)"}",
                                fontSize = 12.sp,
                                fontWeight = FontWeight.Bold,
                                color = if (contrastRatio >= 4.5) MaterialTheme.colorScheme.primary else MaterialTheme.colorScheme.error
                            )

                            Button(
                                onClick = {
                                    val newPalette = currentConfig.customPalette.copy(
                                        primaryHex = primaryHexInput,
                                        surfaceHex = surfaceHexInput
                                    )
                                    appearancePrefs.updateCustomPalette(newPalette)
                                    snackbarMessage = "Custom Palette Applied!"
                                }
                            ) {
                                Text("Apply Palette")
                            }
                        }
                    }
                }

                HorizontalDivider()

                // Typography & Font Scale
                Text(
                    text = "Typography & Font Scaling",
                    style = MaterialTheme.typography.titleMedium,
                    fontWeight = FontWeight.Bold
                )

                Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                    FontFamilyMode.values().forEach { fontMode ->
                        FilterChip(
                            selected = currentConfig.fontFamilyMode == fontMode,
                            onClick = { appearancePrefs.updateFontFamily(fontMode) },
                            label = { Text(fontMode.name) },
                            modifier = Modifier.weight(1f)
                        )
                    }
                }

                Text(
                    text = "Font Scale: ${"%.2f".format(currentConfig.fontScale)}x",
                    fontSize = 12.sp,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
                Slider(
                    value = currentConfig.fontScale,
                    onValueChange = { appearancePrefs.updateFontScale(it) },
                    valueRange = 0.85f..1.25f,
                    steps = 8
                )

                HorizontalDivider()

                // Animation & Reduced Motion
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Column(modifier = Modifier.weight(1f)) {
                        Text("Reduced Motion Mode", fontWeight = FontWeight.Bold, fontSize = 15.sp)
                        Text("Disables smooth transition animations for accessibility & performance", fontSize = 12.sp, color = MaterialTheme.colorScheme.onSurfaceVariant)
                    }
                    Switch(
                        checked = currentConfig.reducedMotion,
                        onCheckedChange = { appearancePrefs.toggleReducedMotion(it) }
                    )
                }

                HorizontalDivider()

                // Corner Radius Slider
                Text(
                    text = "Corner Radius: ${currentConfig.cornerRadiusDp} dp",
                    style = MaterialTheme.typography.titleMedium,
                    fontWeight = FontWeight.Bold
                )
                Slider(
                    value = currentConfig.cornerRadiusDp.toFloat(),
                    onValueChange = { onUpdateRadius(it.toInt()) },
                    valueRange = 0f..28f,
                    steps = 27,
                    modifier = Modifier.testTag("corner_radius_slider")
                )

                HorizontalDivider()

                // Grid Density Slider
                Text(
                    text = "Photo/Drive Grid Density: ${currentConfig.gridColumns} Columns",
                    style = MaterialTheme.typography.titleMedium,
                    fontWeight = FontWeight.Bold
                )
                Slider(
                    value = currentConfig.gridColumns.toFloat(),
                    onValueChange = { onUpdateCols(it.toInt()) },
                    valueRange = 2f..6f,
                    steps = 3,
                    modifier = Modifier.testTag("grid_columns_slider")
                )

                HorizontalDivider()

                // Navigation Bar Architecture
                Text(
                    text = "Navigation Bar Architecture",
                    style = MaterialTheme.typography.titleMedium,
                    fontWeight = FontWeight.Bold
                )
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    FilterChip(
                        selected = currentConfig.navBarMode == NavBarMode.FLOATING_PILL,
                        onClick = { onUpdateNavBar(NavBarMode.FLOATING_PILL) },
                        label = { Text("Floating Glass Pill") },
                        modifier = Modifier.weight(1f)
                    )
                    FilterChip(
                        selected = currentConfig.navBarMode == NavBarMode.DOCKED,
                        onClick = { onUpdateNavBar(NavBarMode.DOCKED) },
                        label = { Text("Docked Bar") },
                        modifier = Modifier.weight(1f)
                    )
                }

                HorizontalDivider()

                // HUD Telemetry Toggle
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Column(modifier = Modifier.weight(1f)) {
                        Text(
                            text = "HUD Telemetry Overlay",
                            fontWeight = FontWeight.Bold,
                            fontSize = 15.sp
                        )
                        Text(
                            text = "Show live MB/s, bot workers, RAM & AES cipher badge",
                            fontSize = 12.sp,
                            color = MaterialTheme.colorScheme.onSurfaceVariant
                        )
                    }
                    Switch(
                        checked = currentConfig.showTelemetryOverlay,
                        onCheckedChange = onToggleTelemetry,
                        modifier = Modifier.testTag("telemetry_switch")
                    )
                }

                Spacer(modifier = Modifier.height(24.dp))
            }

            if (showExportDialog) {
                val jsonProfile = remember { appearancePrefs.exportProfileJson() }
                AlertDialog(
                    onDismissRequest = { showExportDialog = false },
                    title = { Text("Export Customization Profile JSON") },
                    text = {
                        Column {
                            Text("Copy this versioned JSON profile to backup or share your theme setup across devices:")
                            Spacer(modifier = Modifier.height(8.dp))
                            Surface(
                                color = MaterialTheme.colorScheme.surfaceContainerHighest,
                                shape = RoundedCornerShape(8.dp),
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .height(160.dp)
                            ) {
                                Text(
                                    text = jsonProfile,
                                    fontSize = 11.sp,
                                    modifier = Modifier.padding(8.dp)
                                )
                            }
                        }
                    },
                    confirmButton = {
                        Button(
                            onClick = {
                                val clipboard = context.getSystemService(Context.CLIPBOARD_SERVICE) as ClipboardManager
                                clipboard.setPrimaryClip(ClipData.newPlainText("AetherVault Theme Profile", jsonProfile))
                                showExportDialog = false
                                snackbarMessage = "Theme Profile copied to clipboard!"
                            }
                        ) {
                            Text("Copy JSON")
                        }
                    },
                    dismissButton = {
                        TextButton(onClick = { showExportDialog = false }) {
                            Text("Close")
                        }
                    }
                )
            }

            if (showImportDialog) {
                AlertDialog(
                    onDismissRequest = { showImportDialog = false },
                    title = { Text("Import Customization Profile JSON") },
                    text = {
                        Column {
                            Text("Paste a versioned AetherVault customization JSON profile:")
                            Spacer(modifier = Modifier.height(8.dp))
                            OutlinedTextField(
                                value = importJsonInput,
                                onValueChange = {
                                    importJsonInput = it
                                    importError = null
                                },
                                label = { Text("Profile JSON") },
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .height(140.dp)
                            )
                            importError?.let { err ->
                                Text(err, color = MaterialTheme.colorScheme.error, fontSize = 12.sp)
                            }
                        }
                    },
                    confirmButton = {
                        Button(
                            onClick = {
                                val success = appearancePrefs.importProfileJson(importJsonInput)
                                if (success) {
                                    showImportDialog = false
                                    snackbarMessage = "Customization Profile imported successfully!"
                                } else {
                                    importError = "Invalid Profile JSON format"
                                }
                            }
                        ) {
                            Text("Import")
                        }
                    },
                    dismissButton = {
                        TextButton(onClick = { showImportDialog = false }) {
                            Text("Cancel")
                        }
                    }
                )
            }

            snackbarMessage?.let { msg ->
                Snackbar(
                    modifier = Modifier
                        .align(Alignment.BottomCenter)
                        .padding(16.dp),
                    action = {
                        TextButton(onClick = { snackbarMessage = null }) {
                            Text("OK")
                        }
                    }
                ) {
                    Text(msg)
                }
            }
        }
    }
}
