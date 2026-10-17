package com.example.features.settings

import androidx.compose.animation.*
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.selection.SelectionContainer
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.PasswordVisualTransformation
import androidx.compose.ui.text.input.VisualTransformation
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.core.database.BotSwarmEntity
import com.example.features.studio.AppLanguage
import com.example.ui.theme.CyanNeon
import com.example.ui.theme.IndigoHyper
import com.example.ui.theme.VioletNeon

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun SettingsScreen(
    totalStorageBytes: Long,
    activeFileCount: Int,
    botSwarm: List<BotSwarmEntity>,
    language: AppLanguage,
    channelId: Long,
    masterBotToken: String,
    onOpenAppearanceStudio: () -> Unit,
    onAddBotToken: (String) -> Unit,
    onRemoveBot: (Long) -> Unit,
    onToggleWebDav: (Boolean) -> Unit,
    onLanguageChange: (AppLanguage) -> Unit,
    onSaveCredentials: (String, Long) -> Unit,
    onTestPing: (String, Long, (Boolean, String) -> Unit) -> Unit,
    onExportBackup: ((Boolean, String, java.io.File?) -> Unit) -> Unit = {},
    onImportBackup: (String, (Boolean, String) -> Unit) -> Unit = { _, _ -> },
    onRebindBot: (String, (Boolean, String) -> Unit) -> Unit = { _, _ -> },
    onGenerateMigrationCode: ((String) -> Unit) -> Unit = {},
    onRestoreFromCode: (String, (Boolean, String) -> Unit) -> Unit = { _, _ -> },
    onSyncExistingServer: ((Boolean, String) -> Unit) -> Unit = {},
    onClearAllData: () -> Unit,
    onLockVault: () -> Unit
) {
    val context = androidx.compose.ui.platform.LocalContext.current
    val isHindi = language == AppLanguage.HI
    var webDavEnabled by remember { mutableStateOf(true) }
    var showTermsDialog by remember { mutableStateOf(false) }
    var showCreditsDialog by remember { mutableStateOf(false) }
    var showVoiceTutorialDialog by remember { mutableStateOf(false) }

    // Backup & Migration state
    var showImportDialog by remember { mutableStateOf(false) }
    var importJsonText by remember { mutableStateOf("") }
    var backupStatusText by remember { mutableStateOf("") }
    var backupStatusSuccess by remember { mutableStateOf<Boolean?>(null) }
    var newBotTokenInput by remember { mutableStateOf("") }
    var isRebindingBot by remember { mutableStateOf(false) }

    // Instant Migration Code state
    var generatedMigrationCode by remember { mutableStateOf<String?>(null) }
    var showMigrationCodeDialog by remember { mutableStateOf(false) }
    var isGeneratingCode by remember { mutableStateOf(false) }
    var showEnterMigrationCodeDialog by remember { mutableStateOf(false) }
    var enterMigrationCodeInput by remember { mutableStateOf("") }
    var isRestoringCode by remember { mutableStateOf(false) }

    // Server Content Sync state
    var isSyncingServer by remember { mutableStateOf(false) }
    var serverSyncResult by remember { mutableStateOf("") }

    // Live Telegram Configuration State
    var configBotToken by remember(masterBotToken) { mutableStateOf(masterBotToken) }
    var configChannelId by remember(channelId) { mutableStateOf(if (channelId != 0L) channelId.toString() else "") }
    var isBotTokenMasked by remember { mutableStateOf(true) }
    var isChannelIdMasked by remember { mutableStateOf(true) }
    var showDebugConsoleDialog by remember { mutableStateOf(false) }
    var isTestingPing by remember { mutableStateOf(false) }
    var pingResultText by remember { mutableStateOf("") }
    var pingSuccess by remember { mutableStateOf<Boolean?>(null) }
    var showClearDataConfirm by remember { mutableStateOf(false) }

    val storageGb = totalStorageBytes / (1024f * 1024f * 1024f)
    val storageMb = totalStorageBytes / (1024f * 1024f)

    Scaffold(
        topBar = {
            TopAppBar(
                title = {
                    Text(
                        text = if (isHindi) "कंट्रोल सेंटर व सेटिंग्स" else "Control Center & Settings",
                        fontWeight = FontWeight.Bold
                    )
                },
                actions = {
                    IconButton(onClick = onLockVault, modifier = Modifier.testTag("lock_vault_button")) {
                        Icon(Icons.Default.Lock, contentDescription = "Lock Vault", tint = CyanNeon)
                    }
                }
            )
        }
    ) { padding ->
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(padding)
                .verticalScroll(rememberScrollState())
                .padding(16.dp),
            verticalArrangement = Arrangement.spacedBy(16.dp)
        ) {
            // Real Telegram Bot & Channel Connection Configuration
            Card(
                modifier = Modifier.fillMaxWidth(),
                colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surfaceVariant),
                shape = RoundedCornerShape(14.dp)
            ) {
                Column(modifier = Modifier.padding(16.dp)) {
                    Text(
                        text = if (isHindi) "टेलीग्राम सर्वर कनेक्शन व सेटिंग्स" else "Telegram Server & Channel Settings",
                        fontWeight = FontWeight.Bold,
                        fontSize = 15.sp
                    )
                    Text(
                        text = if (isHindi) "अपना बॉट टोकन और प्राइवेट चैनल ID यहां सेट करें और टेस्ट करें:"
                        else "Configure and live-test your Bot Token & Private Channel ID:",
                        fontSize = 12.sp,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )

                    Spacer(modifier = Modifier.height(12.dp))
                    OutlinedTextField(
                        value = configBotToken,
                        onValueChange = { configBotToken = it.trim() },
                        label = { Text("Telegram Bot API Token") },
                        placeholder = { Text("••••••••••••••••") },
                        visualTransformation = if (isBotTokenMasked) PasswordVisualTransformation() else VisualTransformation.None,
                        trailingIcon = {
                            IconButton(onClick = { isBotTokenMasked = !isBotTokenMasked }) {
                                Icon(
                                    imageVector = if (isBotTokenMasked) Icons.Default.VisibilityOff else Icons.Default.Visibility,
                                    contentDescription = "Toggle Bot Token Masking",
                                    tint = CyanNeon
                                )
                            }
                        },
                        modifier = Modifier.fillMaxWidth().testTag("config_bot_token_input"),
                        singleLine = true
                    )

                    Spacer(modifier = Modifier.height(8.dp))
                    OutlinedTextField(
                        value = configChannelId,
                        onValueChange = { configChannelId = it.trim() },
                        label = { Text("Private Channel ID") },
                        placeholder = { Text("••••••••••••••••") },
                        visualTransformation = if (isChannelIdMasked) PasswordVisualTransformation() else VisualTransformation.None,
                        trailingIcon = {
                            IconButton(onClick = { isChannelIdMasked = !isChannelIdMasked }) {
                                Icon(
                                    imageVector = if (isChannelIdMasked) Icons.Default.VisibilityOff else Icons.Default.Visibility,
                                    contentDescription = "Toggle Channel ID Masking",
                                    tint = CyanNeon
                                )
                            }
                        },
                        modifier = Modifier.fillMaxWidth().testTag("config_channel_id_input"),
                        singleLine = true
                    )

                    Spacer(modifier = Modifier.height(12.dp))
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.spacedBy(8.dp)
                    ) {
                        Button(
                            onClick = {
                                val normId = com.example.core.telegram.TelegramBotClient.normalizeChannelId(configChannelId)
                                val cId = if (normId != 0L) normId else channelId
                                if (normId != 0L) configChannelId = normId.toString()
                                onSaveCredentials(configBotToken, cId)
                                pingResultText = if (isHindi) "सेटिंग्स सफलतापूर्वक सेव हो गईं!" else "Saved credentials locally!"
                                pingSuccess = true
                            },
                            modifier = Modifier.weight(1f)
                        ) {
                            Text(if (isHindi) "सेव करें" else "Save Settings")
                        }

                        OutlinedButton(
                            onClick = {
                                if (configBotToken.isNotBlank()) {
                                    val normId = com.example.core.telegram.TelegramBotClient.normalizeChannelId(configChannelId)
                                    val cId = if (normId != 0L) normId else channelId
                                    if (normId != 0L) configChannelId = normId.toString()
                                    isTestingPing = true
                                    pingResultText = if (isHindi) "टेलीग्राम सर्वर से कनेक्शन टेस्ट कर रहे हैं..." else "Testing ping to Telegram..."
                                    onTestPing(configBotToken, cId) { ok, msg ->
                                        isTestingPing = false
                                        pingSuccess = ok
                                        pingResultText = msg
                                    }
                                }
                            },
                            modifier = Modifier.weight(1f),
                            enabled = !isTestingPing && configBotToken.isNotBlank()
                        ) {
                            if (isTestingPing) {
                                CircularProgressIndicator(modifier = Modifier.size(16.dp), strokeWidth = 2.dp)
                            } else {
                                Icon(Icons.Default.NetworkCheck, contentDescription = null, modifier = Modifier.size(16.dp))
                                Spacer(modifier = Modifier.width(4.dp))
                                Text(if (isHindi) "लाइव टेस्ट पिंग" else "Test Live Ping")
                            }
                        }
                    }

                    if (pingResultText.isNotBlank()) {
                        Spacer(modifier = Modifier.height(8.dp))
                        Surface(
                            color = if (pingSuccess == true) CyanNeon.copy(alpha = 0.15f) else MaterialTheme.colorScheme.error.copy(alpha = 0.15f),
                            shape = RoundedCornerShape(8.dp),
                            modifier = Modifier.fillMaxWidth()
                        ) {
                            Text(
                                text = pingResultText,
                                fontSize = 11.sp,
                                fontFamily = FontFamily.Monospace,
                                color = if (pingSuccess == true) CyanNeon else MaterialTheme.colorScheme.error,
                                modifier = Modifier.padding(10.dp)
                            )
                        }
                    }

                    Spacer(modifier = Modifier.height(10.dp))

                    // Sync Existing Channel Content Button
                    OutlinedButton(
                        onClick = {
                            com.example.core.sensory.SensoryFeedbackEngine.trigger(context, com.example.core.sensory.HapticStyle.LIGHT_TICK)
                            isSyncingServer = true
                            serverSyncResult = if (isHindi) "सर्वर से फाइलें व टेक्स्ट खोजे जा रहे हैं..." else "Scanning existing server messages & files..."
                            onSyncExistingServer { ok, msg ->
                                isSyncingServer = false
                                serverSyncResult = msg
                            }
                        },
                        modifier = Modifier.fillMaxWidth(),
                        enabled = !isSyncingServer && (configBotToken.isNotBlank() || masterBotToken.isNotBlank()),
                        shape = RoundedCornerShape(10.dp)
                    ) {
                        if (isSyncingServer) {
                            CircularProgressIndicator(modifier = Modifier.size(16.dp), strokeWidth = 2.dp, color = CyanNeon)
                            Spacer(modifier = Modifier.width(8.dp))
                        } else {
                            Icon(Icons.Default.CloudSync, contentDescription = null, modifier = Modifier.size(16.dp), tint = CyanNeon)
                            Spacer(modifier = Modifier.width(8.dp))
                        }
                        Text(
                            text = if (isHindi) "सर्वर की पुरानी फाइलें सिंक करें" else "Sync Existing Server Files (Offline View)",
                            fontSize = 12.sp
                        )
                    }

                    if (serverSyncResult.isNotBlank()) {
                        Spacer(modifier = Modifier.height(6.dp))
                        Surface(
                            color = CyanNeon.copy(alpha = 0.12f),
                            shape = RoundedCornerShape(8.dp),
                            modifier = Modifier.fillMaxWidth()
                        ) {
                            Text(
                                text = serverSyncResult,
                                fontSize = 11.sp,
                                color = CyanNeon,
                                modifier = Modifier.padding(10.dp)
                            )
                        }
                    }
                }
            }

            // Storage Analytics Card
            Card(
                modifier = Modifier.fillMaxWidth(),
                colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surfaceVariant),
                shape = RoundedCornerShape(14.dp)
            ) {
                Column(modifier = Modifier.padding(16.dp)) {
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Text(
                            text = if (isHindi) "क्लाउड स्टोरेज उपयोग" else "Cloud Storage Usage",
                            fontWeight = FontWeight.Bold,
                            fontSize = 15.sp
                        )
                        Surface(
                            color = CyanNeon.copy(alpha = 0.2f),
                            shape = RoundedCornerShape(6.dp)
                        ) {
                            Text(
                                text = "FREE & UNLIMITED",
                                fontSize = 10.sp,
                                fontWeight = FontWeight.Bold,
                                color = CyanNeon,
                                modifier = Modifier.padding(horizontal = 6.dp, vertical = 2.dp)
                            )
                        }
                    }

                    Spacer(modifier = Modifier.height(8.dp))
                    Text(
                        text = if (storageGb >= 1.0f) String.format("%.2f GB Used", storageGb)
                        else String.format("%.1f MB Used", storageMb),
                        fontSize = 24.sp,
                        fontWeight = FontWeight.ExtraBold,
                        color = MaterialTheme.colorScheme.primary
                    )
                    Text(
                        text = "$activeFileCount ${if (isHindi) "असली फाइलें एन्क्रिप्टेड" else "real user files securely stored"}",
                        fontSize = 12.sp,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                }
            }

            // Bot Migration & Cloud Vault Backup Card
            Card(
                modifier = Modifier.fillMaxWidth(),
                colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surfaceVariant),
                shape = RoundedCornerShape(14.dp),
                border = androidx.compose.foundation.BorderStroke(1.dp, IndigoHyper.copy(alpha = 0.4f))
            ) {
                Column(modifier = Modifier.padding(16.dp), verticalArrangement = Arrangement.spacedBy(10.dp)) {
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        Icon(Icons.Default.RestorePage, contentDescription = null, tint = IndigoHyper, modifier = Modifier.size(22.dp))
                        Spacer(modifier = Modifier.width(10.dp))
                        Column {
                            Text(
                                text = if (isHindi) "बॉट बैकअप व नए बॉट में रिकवरी" else "Bot Migration & Vault Backup",
                                fontWeight = FontWeight.Bold,
                                fontSize = 15.sp
                            )
                            Text(
                                text = if (isHindi) "अगर बॉट डिलीट हो जाए तो नया बॉट जोड़ें या बैकअप लें"
                                else "Backup files catalog or transfer to a new bot if deleted",
                                fontSize = 11.sp,
                                color = MaterialTheme.colorScheme.onSurfaceVariant
                            )
                        }
                    }

                    // Export & Import JSON Buttons
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.spacedBy(8.dp)
                    ) {
                        Button(
                            onClick = {
                                com.example.core.sensory.SensoryFeedbackEngine.trigger(context, com.example.core.sensory.HapticStyle.LIGHT_TICK)
                                backupStatusText = if (isHindi) "बैकअप तैयार हो रहा है..." else "Creating backup catalog..."
                                onExportBackup { ok, msg, _ ->
                                    backupStatusSuccess = ok
                                    backupStatusText = msg
                                }
                            },
                            modifier = Modifier.weight(1f),
                            colors = ButtonDefaults.buttonColors(containerColor = IndigoHyper),
                            shape = RoundedCornerShape(10.dp)
                        ) {
                            Icon(Icons.Default.Download, contentDescription = null, modifier = Modifier.size(16.dp))
                            Spacer(modifier = Modifier.width(4.dp))
                            Text(if (isHindi) "JSON बैकअप" else "JSON Export", fontSize = 12.sp)
                        }

                        OutlinedButton(
                            onClick = {
                                com.example.core.sensory.SensoryFeedbackEngine.trigger(context, com.example.core.sensory.HapticStyle.LIGHT_TICK)
                                showImportDialog = true
                            },
                            modifier = Modifier.weight(1f),
                            shape = RoundedCornerShape(10.dp)
                        ) {
                            Icon(Icons.Default.Upload, contentDescription = null, modifier = Modifier.size(16.dp))
                            Spacer(modifier = Modifier.width(4.dp))
                            Text(if (isHindi) "JSON रिस्टोर" else "JSON Restore", fontSize = 12.sp)
                        }
                    }

                    // 1-Tap Instant Migration Code
                    Text(
                        text = if (isHindi) "1-टैप इंस्टेंट बैकअप व माइग्रेशन कोड:" else "Instant Migration & Backup Code:",
                        fontSize = 12.sp,
                        fontWeight = FontWeight.SemiBold,
                        color = CyanNeon
                    )

                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.spacedBy(8.dp)
                    ) {
                        Button(
                            onClick = {
                                com.example.core.sensory.SensoryFeedbackEngine.trigger(context, com.example.core.sensory.HapticStyle.LIGHT_TICK)
                                isGeneratingCode = true
                                onGenerateMigrationCode { code ->
                                    isGeneratingCode = false
                                    generatedMigrationCode = code
                                    showMigrationCodeDialog = true
                                }
                            },
                            modifier = Modifier.weight(1f),
                            colors = ButtonDefaults.buttonColors(containerColor = CyanNeon, contentColor = Color.Black),
                            shape = RoundedCornerShape(10.dp),
                            enabled = !isGeneratingCode
                        ) {
                            if (isGeneratingCode) {
                                CircularProgressIndicator(modifier = Modifier.size(16.dp), strokeWidth = 2.dp)
                            } else {
                                Icon(Icons.Default.Key, contentDescription = null, modifier = Modifier.size(16.dp))
                                Spacer(modifier = Modifier.width(4.dp))
                                Text(if (isHindi) "कोड बनाएं" else "Generate Code", fontWeight = FontWeight.Bold, fontSize = 12.sp)
                            }
                        }

                        OutlinedButton(
                            onClick = {
                                com.example.core.sensory.SensoryFeedbackEngine.trigger(context, com.example.core.sensory.HapticStyle.LIGHT_TICK)
                                showEnterMigrationCodeDialog = true
                            },
                            modifier = Modifier.weight(1f),
                            shape = RoundedCornerShape(10.dp)
                        ) {
                            Icon(Icons.Default.ContentPaste, contentDescription = null, modifier = Modifier.size(16.dp), tint = CyanNeon)
                            Spacer(modifier = Modifier.width(4.dp))
                            Text(if (isHindi) "कोड पेस्ट करें" else "Enter Code", fontSize = 12.sp)
                        }
                    }

                    // Re-bind to New Bot Token
                    Text(
                        text = if (isHindi) "नया बॉट जोड़ें / Re-bind Bot Token:" else "Transfer to New Bot Token:",
                        fontSize = 12.sp,
                        fontWeight = FontWeight.SemiBold,
                        color = MaterialTheme.colorScheme.onSurface
                    )

                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.spacedBy(8.dp),
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        OutlinedTextField(
                            value = newBotTokenInput,
                            onValueChange = { newBotTokenInput = it.trim() },
                            label = { Text("New Bot Token") },
                            placeholder = { Text("123456:ABC-DEF...") },
                            modifier = Modifier.weight(1f),
                            singleLine = true
                        )

                        Button(
                            onClick = {
                                if (newBotTokenInput.isNotBlank()) {
                                    com.example.core.sensory.SensoryFeedbackEngine.trigger(context, com.example.core.sensory.HapticStyle.LIGHT_TICK)
                                    isRebindingBot = true
                                    backupStatusText = "Re-binding to new bot..."
                                    onRebindBot(newBotTokenInput) { ok, msg ->
                                        isRebindingBot = false
                                        backupStatusSuccess = ok
                                        backupStatusText = msg
                                        if (ok) newBotTokenInput = ""
                                    }
                                }
                            },
                            enabled = !isRebindingBot && newBotTokenInput.isNotBlank(),
                            colors = ButtonDefaults.buttonColors(containerColor = CyanNeon, contentColor = Color.Black),
                            shape = RoundedCornerShape(10.dp)
                        ) {
                            if (isRebindingBot) {
                                CircularProgressIndicator(modifier = Modifier.size(16.dp), strokeWidth = 2.dp)
                            } else {
                                Text(if (isHindi) "जोड़ें" else "Re-bind", fontWeight = FontWeight.Bold, fontSize = 12.sp)
                            }
                        }
                    }

                    if (backupStatusText.isNotBlank()) {
                        Surface(
                            color = if (backupStatusSuccess == true) CyanNeon.copy(alpha = 0.15f) else MaterialTheme.colorScheme.error.copy(alpha = 0.15f),
                            shape = RoundedCornerShape(8.dp),
                            modifier = Modifier.fillMaxWidth()
                        ) {
                            Text(
                                text = backupStatusText,
                                fontSize = 11.sp,
                                fontFamily = FontFamily.Monospace,
                                color = if (backupStatusSuccess == true) CyanNeon else MaterialTheme.colorScheme.error,
                                modifier = Modifier.padding(10.dp)
                            )
                        }
                    }
                }
            }

            // Appearance Studio Entry
            Card(
                modifier = Modifier
                    .fillMaxWidth()
                    .clickable { onOpenAppearanceStudio() }
                    .testTag("open_appearance_studio_button"),
                colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surfaceVariant),
                shape = RoundedCornerShape(12.dp)
            ) {
                Row(
                    modifier = Modifier.padding(16.dp),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Icon(Icons.Default.Palette, contentDescription = null, tint = CyanNeon)
                    Spacer(modifier = Modifier.width(14.dp))
                    Column(modifier = Modifier.weight(1f)) {
                        Text(
                            text = if (isHindi) "अपीयरेंस व HUD स्टूडियो" else "Appearance & HUD Studio",
                            fontWeight = FontWeight.Bold,
                            fontSize = 14.sp
                        )
                        Text(
                            text = "5 Themes, Corner Radius, Grid Density & Telemetry",
                            fontSize = 11.sp,
                            color = MaterialTheme.colorScheme.onSurfaceVariant
                        )
                    }
                    Icon(Icons.Default.ChevronRight, contentDescription = null, tint = Color.Gray)
                }
            }

            // Language Switcher
            Card(
                modifier = Modifier.fillMaxWidth(),
                colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surfaceVariant),
                shape = RoundedCornerShape(12.dp)
            ) {
                Row(
                    modifier = Modifier.padding(16.dp),
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.SpaceBetween
                ) {
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        Icon(Icons.Default.Language, contentDescription = null, tint = CyanNeon)
                        Spacer(modifier = Modifier.width(12.dp))
                        Text(text = "Language / भाषा", fontWeight = FontWeight.SemiBold, fontSize = 14.sp)
                    }
                    Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                        FilterChip(
                            selected = language == AppLanguage.EN,
                            onClick = { onLanguageChange(AppLanguage.EN) },
                            label = { Text("English") }
                        )
                        FilterChip(
                            selected = language == AppLanguage.HI,
                            onClick = { onLanguageChange(AppLanguage.HI) },
                            label = { Text("हिंदी") }
                        )
                    }
                }
            }

            // Clear All Residual / Cache Data Button
            OutlinedButton(
                onClick = { showClearDataConfirm = true },
                colors = ButtonDefaults.outlinedButtonColors(contentColor = MaterialTheme.colorScheme.error),
                modifier = Modifier.fillMaxWidth()
            ) {
                Icon(Icons.Default.DeleteSweep, contentDescription = null, modifier = Modifier.size(18.dp))
                Spacer(modifier = Modifier.width(8.dp))
                Text(if (isHindi) "लोकल कैशे साफ़ करें" else "Clear Local Cache")
            }

            Spacer(modifier = Modifier.height(16.dp))

            // Clean Single-Line Footer
            Text(
                text = "AetherVault Omni v3.0 • Encrypted Telegram Cloud Vault • By AJPM",
                fontSize = 11.sp,
                fontWeight = FontWeight.Medium,
                color = MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.7f),
                modifier = Modifier.fillMaxWidth(),
                textAlign = androidx.compose.ui.text.style.TextAlign.Center
            )

            Spacer(modifier = Modifier.height(24.dp))
        }
    }

    // Terms & Conditions Dialog
    if (showTermsDialog) {
        AlertDialog(
            onDismissRequest = { showTermsDialog = false },
            title = {
                Text("Terms & Conditions — AetherVault")
            },
            text = {
                Column(modifier = Modifier.verticalScroll(rememberScrollState())) {
                    Text(
                        text = "Creator & Architect: TRB FireOps (@trbfireops)\nYouTube: https://youtube.com/@trbfireops?si=LFRe4g7RiicR2EN0",
                        fontWeight = FontWeight.Bold,
                        color = CyanNeon,
                        fontSize = 13.sp
                    )
                    Spacer(modifier = Modifier.height(8.dp))
                    Text(
                        text = """
1. ARCHITECTURE & ZERO SUBSCRIPTION:
AetherVault is an autonomous, serverless personal cloud system authored by TRB FireOps (@trbfireops). It leverages official Telegram Bot API swarms and your own private Telegram Channel as encrypted storage.

2. ZERO-KNOWLEDGE CRYPTOGRAPHY:
Every file chunk is split deterministically into 19MB frames and encrypted on-device via AES-256-GCM. Plaintext filenames, contents, or metadata are never visible to Telegram servers.

3. DEVICE COMPATIBILITY:
Designed for universal compatibility on both Rooted and Non-Rooted Android devices (Android 7.0 through Android 16+) utilizing standard Storage Access Framework (SAF).

4. USER RESPONSIBILITY:
You are the sole custodian of your Master Encryption Password and Telegram Channel access keys.
                        """.trimIndent(),
                        fontSize = 12.sp,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                }
            },
            confirmButton = {
                Button(onClick = { showTermsDialog = false }) {
                    Text("I Agree & Accept")
                }
            }
        )
    }

    // Confirm Clear Data Dialog
    if (showClearDataConfirm) {
        AlertDialog(
            onDismissRequest = { showClearDataConfirm = false },
            title = { Text(if (isHindi) "डेटा साफ़ करें?" else "Clear Local Cache?") },
            text = {
                Text(
                    if (isHindi) "क्या आप सभी पुराने कैशे रिकॉर्ड्स को साफ़ करना चाहते हैं? आपका टेलीग्राम चैनल अप्रभावित रहेगा।"
                    else "This will clear all local SQLite cache records. Your files in the private Telegram channel remain safe."
                )
            },
            confirmButton = {
                Button(
                    onClick = {
                        onClearAllData()
                        showClearDataConfirm = false
                    },
                    colors = ButtonDefaults.buttonColors(containerColor = MaterialTheme.colorScheme.error)
                ) {
                    Text(if (isHindi) "साफ़ करें" else "Clear Cache")
                }
            },
            dismissButton = {
                TextButton(onClick = { showClearDataConfirm = false }) {
                    Text(if (isHindi) "रद्द करें" else "Cancel")
                }
            }
        )
    }

    // Import Backup Dialog
    if (showImportDialog) {
        AlertDialog(
            onDismissRequest = { showImportDialog = false },
            title = {
                Text(if (isHindi) "क्लाउड बैकअप रिस्टोर करें" else "Restore Cloud Backup (.json)")
            },
            text = {
                Column(verticalArrangement = Arrangement.spacedBy(10.dp)) {
                    Text(
                        text = if (isHindi) "अपने बैकअप JSON का कंटेंट यहाँ पेस्ट करें। आपकी सारी फाइलें व एन्क्रिप्शन कीज़ वापस रिस्टोर हो जाएँगी:"
                        else "Paste your backup JSON manifest content below to restore all cloud files and encryption keys:",
                        fontSize = 12.sp,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                    OutlinedTextField(
                        value = importJsonText,
                        onValueChange = { importJsonText = it },
                        placeholder = { Text("{\"format\": \"AETHER_VAULT_BACKUP_V1\", ...}") },
                        modifier = Modifier
                            .fillMaxWidth()
                            .height(140.dp),
                        maxLines = 6
                    )
                }
            },
            confirmButton = {
                Button(
                    onClick = {
                        if (importJsonText.isNotBlank()) {
                            onImportBackup(importJsonText) { ok, msg ->
                                backupStatusSuccess = ok
                                backupStatusText = msg
                                if (ok) {
                                    showImportDialog = false
                                    importJsonText = ""
                                }
                            }
                        }
                    },
                    enabled = importJsonText.isNotBlank()
                ) {
                    Text(if (isHindi) "रिस्टोर करें" else "Restore")
                }
            },
            dismissButton = {
                TextButton(onClick = { showImportDialog = false }) {
                    Text(if (isHindi) "रद्द करें" else "Cancel")
                }
            }
        )
    }

    // Display Generated Migration Code Dialog
    if (showMigrationCodeDialog && generatedMigrationCode != null) {
        val codeToShare = generatedMigrationCode ?: ""
        AlertDialog(
            onDismissRequest = { showMigrationCodeDialog = false },
            title = {
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Icon(Icons.Default.Key, contentDescription = null, tint = CyanNeon)
                    Spacer(modifier = Modifier.width(8.dp))
                    Text(if (isHindi) "इंस्टेंट माइग्रेशन कोड तैयार है" else "Migration Code Generated")
                }
            },
            text = {
                Column(verticalArrangement = Arrangement.spacedBy(10.dp)) {
                    Text(
                        text = if (isHindi) "इस कोड को कॉपी करें। जब आप किसी नए बॉट या नए फ़ोन में AetherVault सेटअप करें, तो यह कोड पेस्ट कर दें। आपका सारा क्लाउड डेटा तुरंत आ जाएगा:"
                        else "Copy this code. When setting up a new bot or device, paste this code to instantly restore all files, folders, and channel state:",
                        fontSize = 12.sp,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )

                    Surface(
                        color = MaterialTheme.colorScheme.surfaceVariant,
                        shape = RoundedCornerShape(8.dp),
                        modifier = Modifier.fillMaxWidth()
                    ) {
                        SelectionContainer {
                            Text(
                                text = codeToShare,
                                fontSize = 11.sp,
                                fontFamily = FontFamily.Monospace,
                                color = CyanNeon,
                                modifier = Modifier.padding(10.dp)
                            )
                        }
                    }
                }
            },
            confirmButton = {
                Button(
                    onClick = {
                        val clipboard = context.getSystemService(android.content.Context.CLIPBOARD_SERVICE) as? android.content.ClipboardManager
                        clipboard?.setPrimaryClip(android.content.ClipData.newPlainText("Vault Migration Code", codeToShare))
                        backupStatusSuccess = true
                        backupStatusText = if (isHindi) "माइग्रेशन कोड क्लिपबोर्ड पर कॉपी हो गया!" else "Migration code copied to clipboard!"
                        showMigrationCodeDialog = false
                    }
                ) {
                    Icon(Icons.Default.ContentCopy, contentDescription = null, modifier = Modifier.size(16.dp))
                    Spacer(modifier = Modifier.width(4.dp))
                    Text(if (isHindi) "कोड कॉपी करें" else "Copy Code")
                }
            },
            dismissButton = {
                TextButton(onClick = { showMigrationCodeDialog = false }) {
                    Text(if (isHindi) "बंद करें" else "Close")
                }
            }
        )
    }

    // Enter Migration Code Dialog
    if (showEnterMigrationCodeDialog) {
        AlertDialog(
            onDismissRequest = { showEnterMigrationCodeDialog = false },
            title = {
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Icon(Icons.Default.ContentPaste, contentDescription = null, tint = CyanNeon)
                    Spacer(modifier = Modifier.width(8.dp))
                    Text(if (isHindi) "माइग्रेशन कोड से डेटा रिस्टोर करें" else "Restore via Migration Code")
                }
            },
            text = {
                Column(verticalArrangement = Arrangement.spacedBy(10.dp)) {
                    Text(
                        text = if (isHindi) "पुराने बॉट या डिवाइस से जनरेट किया गया 'AVCODE-...' कोड यहाँ पेस्ट करें:"
                        else "Paste the 'AVCODE-...' migration code from your previous bot or device:",
                        fontSize = 12.sp,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )

                    OutlinedTextField(
                        value = enterMigrationCodeInput,
                        onValueChange = { enterMigrationCodeInput = it },
                        placeholder = { Text("AVCODE-...") },
                        modifier = Modifier
                            .fillMaxWidth()
                            .height(130.dp),
                        maxLines = 5
                    )
                }
            },
            confirmButton = {
                Button(
                    onClick = {
                        if (enterMigrationCodeInput.isNotBlank()) {
                            isRestoringCode = true
                            onRestoreFromCode(enterMigrationCodeInput.trim()) { ok, msg ->
                                isRestoringCode = false
                                backupStatusSuccess = ok
                                backupStatusText = msg
                                if (ok) {
                                    showEnterMigrationCodeDialog = false
                                    enterMigrationCodeInput = ""
                                }
                            }
                        }
                    },
                    enabled = !isRestoringCode && enterMigrationCodeInput.isNotBlank()
                ) {
                    if (isRestoringCode) {
                        CircularProgressIndicator(modifier = Modifier.size(16.dp), strokeWidth = 2.dp)
                    } else {
                        Text(if (isHindi) "डेटा लोड करें" else "Load Data")
                    }
                }
            },
            dismissButton = {
                TextButton(onClick = { showEnterMigrationCodeDialog = false }) {
                    Text(if (isHindi) "रद्द करें" else "Cancel")
                }
            }
        )
    }
}
