package com.example.features.studio

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
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.ui.components.AetherVaultLogo
import com.example.ui.theme.CyanNeon
import com.example.ui.theme.IndigoHyper

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun AppearanceStudioScreen(
    currentConfig: AppearanceConfig,
    onUpdateTheme: (ThemePreset) -> Unit,
    onUpdateRadius: (Int) -> Unit,
    onUpdateCols: (Int) -> Unit,
    onUpdateAspect: (AspectRatioMode) -> Unit,
    onUpdateNavBar: (NavBarMode) -> Unit,
    onToggleTelemetry: (Boolean) -> Unit,
    onUpdateLang: (AppLanguage) -> Unit,
    onBack: () -> Unit
) {
    val scrollState = rememberScrollState()
    val isHindi = currentConfig.language == AppLanguage.HI

    Scaffold(
        topBar = {
            TopAppBar(
                title = {
                    Text(
                        text = if (isHindi) "अपीयरेंस व HUD स्टूडियो" else "Appearance & HUD Studio",
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
                    // Quick language toggle
                    FilterChip(
                        selected = currentConfig.language == AppLanguage.HI,
                        onClick = {
                            onUpdateLang(if (isHindi) AppLanguage.EN else AppLanguage.HI)
                        },
                        label = { Text(if (isHindi) "EN" else "हिंदी") },
                        modifier = Modifier.padding(end = 12.dp)
                    )
                },
                colors = TopAppBarDefaults.topAppBarColors(
                    containerColor = MaterialTheme.colorScheme.surface
                )
            )
        }
    ) { padding ->
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(padding)
                .verticalScroll(scrollState)
                .padding(16.dp),
            verticalArrangement = Arrangement.spacedBy(20.dp)
        ) {
            // Live Preview Card
            Card(
                modifier = Modifier
                    .fillMaxWidth()
                    .border(
                        1.dp,
                        CyanNeon.copy(alpha = 0.4f),
                        RoundedCornerShape(currentConfig.cornerRadiusDp.dp)
                    ),
                shape = RoundedCornerShape(currentConfig.cornerRadiusDp.dp),
                colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surfaceVariant)
            ) {
                Column(
                    modifier = Modifier.padding(16.dp),
                    horizontalAlignment = Alignment.CenterHorizontally
                ) {
                    Text(
                        text = if (isHindi) "लाइव थीम प्रिव्यू" else "Live Theme Preview",
                        style = MaterialTheme.typography.titleMedium,
                        fontWeight = FontWeight.Bold,
                        color = MaterialTheme.colorScheme.primary
                    )
                    Spacer(modifier = Modifier.height(12.dp))
                    AetherVaultLogo(size = 54.dp)
                    Spacer(modifier = Modifier.height(8.dp))
                    Text(
                        text = "AetherVault Omni",
                        fontWeight = FontWeight.Bold,
                        fontSize = 16.sp
                    )
                    Text(
                        text = if (isHindi) "अनंत क्लाउड • शून्य ज्ञान • आपका हमेशा के लिए"
                        else "Infinite Cloud • Zero Knowledge • Yours Forever",
                        fontSize = 12.sp,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                    Spacer(modifier = Modifier.height(12.dp))
                    Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                        Button(
                            onClick = {},
                            shape = RoundedCornerShape(currentConfig.cornerRadiusDp.dp),
                            modifier = Modifier.height(38.dp)
                        ) {
                            Text(if (isHindi) "एक्शन बटन" else "Action Button")
                        }
                        OutlinedButton(
                            onClick = {},
                            shape = RoundedCornerShape(currentConfig.cornerRadiusDp.dp),
                            modifier = Modifier.height(38.dp)
                        ) {
                            Text(if (isHindi) "आउटलाइन" else "Outlined")
                        }
                    }
                }
            }

            // Theme Presets
            Text(
                text = if (isHindi) "थीम प्रीसेट्स (5 डिज़ाइन मोड्स)" else "Theme Presets (5 Design Modes)",
                style = MaterialTheme.typography.titleMedium,
                fontWeight = FontWeight.Bold
            )

            val presets = listOf(
                Pair(ThemePreset.OLED_STEALTH, if (isHindi) "OLED पिच-ब्लैक (डिफ़ॉल्ट प्रो)" else "OLED Pitch-Black (Pro)"),
                Pair(ThemePreset.FROSTED_GLASS, if (isHindi) "फ्रॉस्टेड ग्लासमॉर्फिक HUD" else "Frosted Glassmorphic HUD"),
                Pair(ThemePreset.DOT_MATRIX, if (isHindi) "नथिंग डॉट-मैट्रिक्स (इंडस्ट्रियल)" else "Nothing Dot-Matrix (Industrial)"),
                Pair(ThemePreset.MATERIAL_YOU, if (isHindi) "मटीरियल यू एक्सप्रेसिव (मोनेट)" else "Material You Expressive (Monet)"),
                Pair(ThemePreset.ALABASTER_CLEAN, if (isHindi) "डे-लाइट अलबास्टर क्लीन" else "Daylight Alabaster Clean")
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

            // Corner Radius Slider
            Text(
                text = if (isHindi) "कॉर्नर रेडियस: ${currentConfig.cornerRadiusDp} dp"
                else "Corner Radius: ${currentConfig.cornerRadiusDp} dp",
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
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween
            ) {
                Text("0dp (Sharp Brutalist)", fontSize = 11.sp, color = MaterialTheme.colorScheme.onSurfaceVariant)
                Text("14dp (Balanced)", fontSize = 11.sp, color = MaterialTheme.colorScheme.onSurfaceVariant)
                Text("28dp (Super Pill)", fontSize = 11.sp, color = MaterialTheme.colorScheme.onSurfaceVariant)
            }

            HorizontalDivider()

            // Grid Density Slider
            Text(
                text = if (isHindi) "फोटो/ड्राइव ग्रिड कॉलम्स: ${currentConfig.gridColumns} कॉलम्स"
                else "Photo/Drive Grid Density: ${currentConfig.gridColumns} Columns",
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
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween
            ) {
                Text("2 (Large Cards)", fontSize = 11.sp, color = MaterialTheme.colorScheme.onSurfaceVariant)
                Text("4 (Compact)", fontSize = 11.sp, color = MaterialTheme.colorScheme.onSurfaceVariant)
                Text("6 (Dense Overview)", fontSize = 11.sp, color = MaterialTheme.colorScheme.onSurfaceVariant)
            }

            HorizontalDivider()

            // Aspect Ratio Mode
            Text(
                text = if (isHindi) "थंबनेल क्रॉप मोड" else "Thumbnail Aspect Ratio Mode",
                style = MaterialTheme.typography.titleMedium,
                fontWeight = FontWeight.Bold
            )
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.spacedBy(10.dp)
            ) {
                FilterChip(
                    selected = currentConfig.aspectRatioMode == AspectRatioMode.SQUARE,
                    onClick = { onUpdateAspect(AspectRatioMode.SQUARE) },
                    label = { Text("1:1 Square Grid") },
                    leadingIcon = {
                        if (currentConfig.aspectRatioMode == AspectRatioMode.SQUARE) {
                            Icon(Icons.Default.Check, contentDescription = null, modifier = Modifier.size(16.dp))
                        }
                    },
                    modifier = Modifier.weight(1f)
                )
                FilterChip(
                    selected = currentConfig.aspectRatioMode == AspectRatioMode.MASONRY,
                    onClick = { onUpdateAspect(AspectRatioMode.MASONRY) },
                    label = { Text("Original Aspect Masonry") },
                    leadingIcon = {
                        if (currentConfig.aspectRatioMode == AspectRatioMode.MASONRY) {
                            Icon(Icons.Default.Check, contentDescription = null, modifier = Modifier.size(16.dp))
                        }
                    },
                    modifier = Modifier.weight(1f)
                )
            }

            HorizontalDivider()

            // Navigation Bar Architecture
            Text(
                text = if (isHindi) "नेविगेशन बार आर्किटेक्चर" else "Navigation Bar Architecture",
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
                        text = if (isHindi) "HUD टेलीमेट्री ओवरले" else "HUD Telemetry Overlay",
                        fontWeight = FontWeight.Bold,
                        fontSize = 15.sp
                    )
                    Text(
                        text = if (isHindi) "लाइव MB/s, बॉट वर्कर्स, RAM व AES सिफर दिखाएं"
                        else "Show live MB/s, bot workers, RAM & AES cipher badge",
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
    }
}
