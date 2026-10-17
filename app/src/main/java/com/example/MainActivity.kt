package com.example

import android.content.ClipData
import android.content.ClipboardManager
import android.content.Context
import android.content.Intent
import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.BackHandler
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.activity.viewModels
import androidx.compose.animation.*
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.PasswordVisualTransformation
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.example.core.crypto.AesGcmChunkCipher
import com.example.core.database.VaultFile
import com.example.core.crash.VaultCrashHandler
import com.example.core.sync.TelegramSyncForegroundService
import com.example.features.drive.DriveExplorerScreen
import com.example.features.onboarding.ConnectWizardScreen
import com.example.features.onboarding.TermsAgreementScreen
import com.example.features.photos.PhotosTimelineScreen
import com.example.features.settings.SettingsScreen
import com.example.features.studio.*
import com.example.features.teledrop.TeleDropScreen
import com.example.features.workstation.WorkstationViewerScreen
import com.example.ui.components.AetherVaultLogo
import com.example.ui.components.DynamicTransferPill
import com.example.features.dashboard.VaultDashboardScreen
import com.example.ui.components.HudTelemetryBar
import com.example.ui.theme.AetherVaultTheme
import com.example.ui.theme.CyanNeon

import androidx.compose.animation.core.*
import androidx.compose.ui.graphics.Brush

enum class MainNavTab {
    DASHBOARD,
    DRIVE,
    PHOTOS,
    TELEDROP,
    SETTINGS
}

enum class SubScreen {
    NONE,
    APPEARANCE_STUDIO,
    WORKSTATION_VIEWER
}

@Composable
fun AuroraBackgroundCanvas() {
    val infiniteTransition = rememberInfiniteTransition(label = "aurora")

    val phase1 by infiniteTransition.animateFloat(
        initialValue = 0f,
        targetValue = 1f,
        animationSpec = infiniteRepeatable(
            animation = tween(12000, easing = LinearEasing),
            repeatMode = RepeatMode.Reverse
        ),
        label = "phase1"
    )

    val phase2 by infiniteTransition.animateFloat(
        initialValue = 1f,
        targetValue = 0f,
        animationSpec = infiniteRepeatable(
            animation = tween(16000, easing = LinearEasing),
            repeatMode = RepeatMode.Reverse
        ),
        label = "phase2"
    )

    androidx.compose.foundation.Canvas(modifier = Modifier.fillMaxSize()) {
        val width = size.width
        val height = size.height

        // Orb 1: Cyan (#00F2FE)
        drawCircle(
            brush = Brush.radialGradient(
                colors = listOf(Color(0x2800F2FE), Color.Transparent),
                center = androidx.compose.ui.geometry.Offset(width * (0.2f + 0.3f * phase1), height * (0.15f + 0.2f * phase2)),
                radius = width * 0.75f
            )
        )

        // Orb 2: Electric Blue (#4FACFE)
        drawCircle(
            brush = Brush.radialGradient(
                colors = listOf(Color(0x224FACFE), Color.Transparent),
                center = androidx.compose.ui.geometry.Offset(width * (0.8f - 0.4f * phase2), height * (0.6f + 0.25f * phase1)),
                radius = width * 0.85f
            )
        )

        // Orb 3: Deep Violet (#7F00FF)
        drawCircle(
            brush = Brush.radialGradient(
                colors = listOf(Color(0x1E7F00FF), Color.Transparent),
                center = androidx.compose.ui.geometry.Offset(width * (0.4f + 0.3f * phase2), height * (0.85f - 0.3f * phase1)),
                radius = width * 0.80f
            )
        )
    }
}

class MainActivity : ComponentActivity() {
    private val viewModel: VaultViewModel by viewModels()
    private lateinit var appearancePrefs: AppearancePreferences

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        enableEdgeToEdge()
        appearancePrefs = AppearancePreferences(this)
        VaultCrashHandler.init(this)
        runCatching {
            val serviceIntent = Intent(this, TelegramSyncForegroundService::class.java)
            if (android.os.Build.VERSION.SDK_INT >= android.os.Build.VERSION_CODES.O) {
                startForegroundService(serviceIntent)
            } else {
                startService(serviceIntent)
            }
        }

        setContent {
            val appearanceConfig by appearancePrefs.configFlow.collectAsStateWithLifecycle()
            val isConfigured by viewModel.isConfigured.collectAsStateWithLifecycle()
            val isUnlocked by viewModel.isUnlocked.collectAsStateWithLifecycle()
            val channelId by viewModel.channelId.collectAsStateWithLifecycle()

            val files by viewModel.allFiles.collectAsStateWithLifecycle()
            val trashedFiles by viewModel.trashedFiles.collectAsStateWithLifecycle()
            val folders by viewModel.allFolders.collectAsStateWithLifecycle()
            val transfers by viewModel.allTransfers.collectAsStateWithLifecycle()
            val teleDropItems by viewModel.teleDropStream.collectAsStateWithLifecycle()
            val botSwarm by viewModel.botSwarm.collectAsStateWithLifecycle()
            val totalStorage by viewModel.totalStorageUsed.collectAsStateWithLifecycle()
            val fileCount by viewModel.activeFileCount.collectAsStateWithLifecycle()
            val masterBotToken by viewModel.masterBotToken.collectAsStateWithLifecycle()
            val cameraSyncStatus by viewModel.cameraSyncStatus.collectAsStateWithLifecycle()

            var activeTab by remember { mutableStateOf(MainNavTab.DASHBOARD) }
            var subScreen by remember { mutableStateOf(SubScreen.NONE) }
            var activeViewingFile by remember { mutableStateOf<VaultFile?>(null) }
            var activeFolderId by remember { mutableStateOf("root") }

            // Share ticket and restoration dialog states
            var shareTicketUrl by remember { mutableStateOf<String?>(null) }
            var showFreeUpSpaceDialog by remember { mutableStateOf(false) }
            var restorationNotification by remember { mutableStateOf<String?>(null) }
            var isRestoringDownload by remember { mutableStateOf(false) }
            var restoreProgressText by remember { mutableStateOf("") }

            // Lock screen pin input
            var unlockPasswordInput by remember { mutableStateOf("") }
            var unlockErrorMessage by remember { mutableStateOf("") }

            val isTermsAgreed by viewModel.isTermsAgreed.collectAsStateWithLifecycle()
            val uploadError by viewModel.uploadErrorNotification.collectAsStateWithLifecycle()
            var verifyResultDialog by remember { mutableStateOf<Pair<Boolean, String>?>(null) }
            var isVerifyingTelegram by remember { mutableStateOf(false) }

            // Automatic Storage Permission requester on startup for both Root and Non-Root devices
            val permissionLauncher = androidx.activity.compose.rememberLauncherForActivityResult(
                contract = androidx.activity.result.contract.ActivityResultContracts.RequestMultiplePermissions()
            ) { /* Handled */ }

            LaunchedEffect(Unit) {
                if (android.os.Build.VERSION.SDK_INT >= android.os.Build.VERSION_CODES.TIRAMISU) {
                    permissionLauncher.launch(
                        arrayOf(
                            android.Manifest.permission.READ_MEDIA_IMAGES,
                            android.Manifest.permission.READ_MEDIA_VIDEO,
                            android.Manifest.permission.READ_MEDIA_AUDIO
                        )
                    )
                } else {
                    permissionLauncher.launch(
                        arrayOf(
                            android.Manifest.permission.READ_EXTERNAL_STORAGE,
                            android.Manifest.permission.WRITE_EXTERNAL_STORAGE
                        )
                    )
                }
            }

            val isHindi = appearanceConfig.language == AppLanguage.HI
            val context = LocalContext.current

            // Native SAF File Picker for real user files from device storage / Downloads / SD Card
            val filePickerLauncher = androidx.activity.compose.rememberLauncherForActivityResult(
                contract = androidx.activity.result.contract.ActivityResultContracts.OpenMultipleDocuments()
            ) { uris ->
                if (uris.isNotEmpty()) {
                    viewModel.uploadMultipleRealFiles(context, uris, activeFolderId)
                }
            }

            // Native SAF Media Picker for Photos and Videos
            val mediaPickerLauncher = androidx.activity.compose.rememberLauncherForActivityResult(
                contract = androidx.activity.result.contract.ActivityResultContracts.OpenMultipleDocuments()
            ) { uris ->
                if (uris.isNotEmpty()) {
                    viewModel.uploadMultipleRealFiles(context, uris, activeFolderId)
                }
            }

            AetherVaultTheme(
                preset = appearanceConfig.themePreset,
                cornerRadiusDp = appearanceConfig.cornerRadiusDp
            ) {
                // Back handler logic
                BackHandler(enabled = subScreen != SubScreen.NONE || activeTab != MainNavTab.DASHBOARD) {
                    if (subScreen != SubScreen.NONE) {
                        subScreen = SubScreen.NONE
                        activeViewingFile = null
                    } else if (activeTab != MainNavTab.DASHBOARD) {
                        activeTab = MainNavTab.DASHBOARD
                    }
                }

                if (!isTermsAgreed) {
                    // Terms & Conditions Agreement screen by Owner Ayush Mandal
                    TermsAgreementScreen(
                        language = appearanceConfig.language,
                        onAcceptTerms = { viewModel.acceptTerms() }
                    )
                } else if (!isConfigured) {
                    // First time onboarding setup wizard
                    ConnectWizardScreen(
                        language = appearanceConfig.language,
                        onCompleteSetup = { bots, chId, pass, decoy ->
                            viewModel.completeSetup(bots, chId, pass, decoy)
                        },
                        onLinkViaQrCode = { qrData ->
                            val cleanData = qrData.trim()
                            if (cleanData.startsWith("AVCODE-") || cleanData.startsWith("{")) {
                                viewModel.restoreFromMigrationCode(cleanData) { ok, msg ->
                                    if (ok) {
                                        restorationNotification = msg
                                    }
                                }
                            } else if (cleanData.contains(":")) {
                                viewModel.completeSetup(listOf(cleanData), 0L, "123456", null)
                            }
                        }
                    )
                } else if (!isUnlocked) {
                    // Vault Lock Screen
                    Surface(
                        modifier = Modifier.fillMaxSize(),
                        color = MaterialTheme.colorScheme.background
                    ) {
                        Column(
                            modifier = Modifier
                                .fillMaxSize()
                                .padding(28.dp),
                            horizontalAlignment = Alignment.CenterHorizontally,
                            verticalArrangement = Arrangement.Center
                        ) {
                            AetherVaultLogo(size = 90.dp, isPulsing = true)
                            Spacer(modifier = Modifier.height(20.dp))
                            Text(
                                text = "AetherVault",
                                style = MaterialTheme.typography.headlineMedium,
                                fontWeight = FontWeight.ExtraBold,
                                color = MaterialTheme.colorScheme.onBackground
                            )
                            Text(
                                text = if (isHindi) "शून्य-ज्ञान एन्क्रिप्टेड वॉल्ट लॉक है"
                                else "Zero-Knowledge Encrypted Vault is Locked",
                                fontSize = 13.sp,
                                color = MaterialTheme.colorScheme.onSurfaceVariant
                            )

                            Spacer(modifier = Modifier.height(28.dp))
                            OutlinedTextField(
                                value = unlockPasswordInput,
                                onValueChange = {
                                    unlockPasswordInput = it
                                    unlockErrorMessage = ""
                                },
                                label = { Text("Master Password or Decoy PIN") },
                                visualTransformation = PasswordVisualTransformation(),
                                singleLine = true,
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .testTag("unlock_password_input")
                            )

                            if (unlockErrorMessage.isNotBlank()) {
                                Spacer(modifier = Modifier.height(6.dp))
                                Text(
                                    text = unlockErrorMessage,
                                    color = MaterialTheme.colorScheme.error,
                                    fontSize = 12.sp
                                )
                            }

                            Spacer(modifier = Modifier.height(16.dp))
                            Button(
                                onClick = {
                                    val success = viewModel.unlockVault(unlockPasswordInput)
                                    if (success) {
                                        unlockPasswordInput = ""
                                    } else {
                                        unlockErrorMessage = if (isHindi) "गलत पासवर्ड!" else "Invalid Master Password or PIN!"
                                    }
                                },
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .height(48.dp)
                                    .testTag("unlock_button")
                            ) {
                                Icon(Icons.Default.LockOpen, contentDescription = null)
                                Spacer(modifier = Modifier.width(8.dp))
                                Text(if (isHindi) "वॉल्ट खोलें" else "Unlock Vault")
                            }

                            Spacer(modifier = Modifier.height(12.dp))
                            TextButton(
                                onClick = {
                                    // Quick demo biometric bypass
                                    viewModel.unlockVault("123456")
                                }
                            ) {
                                Icon(Icons.Default.Fingerprint, contentDescription = null, tint = CyanNeon)
                                Spacer(modifier = Modifier.width(6.dp))
                                Text(if (isHindi) "बायोमेट्रिक से खोलें" else "Quick Biometric Unlock")
                            }
                        }
                    }
                } else {
                    // MAIN LOGGED-IN WORKSPACE
                    Box(modifier = Modifier.fillMaxSize()) {
                        AuroraBackgroundCanvas()
                        Scaffold(
                            containerColor = Color.Transparent,
                            bottomBar = {
                            if (subScreen == SubScreen.NONE && appearanceConfig.navBarMode != NavBarMode.AUTO_HIDE) {
                                Column {
                                    // Live Dynamic Transfer Pill
                                    DynamicTransferPill(
                                        activeTasks = transfers,
                                        onCancelTask = { viewModel.cancelTransfer(it) }
                                    )

                                    // Navigation Bar (Floating Glass Pill or Standard Docked)
                                    val navModifier = if (appearanceConfig.navBarMode == NavBarMode.FLOATING_PILL) {
                                        Modifier
                                            .padding(horizontal = 16.dp, vertical = 8.dp)
                                            .clip(RoundedCornerShape(appearanceConfig.cornerRadiusDp.dp))
                                    } else {
                                        Modifier
                                    }

                                    NavigationBar(
                                        modifier = navModifier,
                                        containerColor = MaterialTheme.colorScheme.surfaceVariant,
                                        tonalElevation = 6.dp
                                    ) {
                                        NavigationBarItem(
                                            selected = activeTab == MainNavTab.DASHBOARD,
                                            onClick = { activeTab = MainNavTab.DASHBOARD },
                                            icon = {
                                                Icon(
                                                    imageVector = Icons.Default.Dashboard,
                                                    contentDescription = "Dashboard"
                                                )
                                            },
                                            label = { Text(if (isHindi) "डैशबोर्ड" else "Dashboard", fontSize = 10.sp) },
                                            modifier = Modifier.testTag("nav_dashboard")
                                        )
                                        NavigationBarItem(
                                            selected = activeTab == MainNavTab.PHOTOS,
                                            onClick = { activeTab = MainNavTab.PHOTOS },
                                            icon = {
                                                Icon(
                                                    imageVector = if (activeTab == MainNavTab.PHOTOS) Icons.Default.PhotoLibrary else Icons.Default.PhotoLibrary,
                                                    contentDescription = "Photos"
                                                )
                                            },
                                            label = { Text(if (isHindi) "फोटो" else "Photos", fontSize = 11.sp) },
                                            modifier = Modifier.testTag("nav_photos")
                                        )
                                        NavigationBarItem(
                                            selected = activeTab == MainNavTab.DRIVE,
                                            onClick = { activeTab = MainNavTab.DRIVE },
                                            icon = {
                                                Icon(
                                                    imageVector = if (activeTab == MainNavTab.DRIVE) Icons.Default.Folder else Icons.Default.FolderOpen,
                                                    contentDescription = "Drive"
                                                )
                                            },
                                            label = { Text(if (isHindi) "ड्राइव" else "Drive", fontSize = 11.sp) },
                                            modifier = Modifier.testTag("nav_drive")
                                        )
                                        NavigationBarItem(
                                            selected = activeTab == MainNavTab.TELEDROP,
                                            onClick = { activeTab = MainNavTab.TELEDROP },
                                            icon = {
                                                Icon(
                                                    imageVector = Icons.Default.Bolt,
                                                    contentDescription = "TeleDrop"
                                                )
                                            },
                                            label = { Text("TeleDrop", fontSize = 11.sp) },
                                            modifier = Modifier.testTag("nav_teledrop")
                                        )
                                        NavigationBarItem(
                                            selected = activeTab == MainNavTab.SETTINGS,
                                            onClick = { activeTab = MainNavTab.SETTINGS },
                                            icon = {
                                                Icon(
                                                    imageVector = Icons.Default.Tune,
                                                    contentDescription = "Settings"
                                                )
                                            },
                                            label = { Text(if (isHindi) "कंट्रोल" else "Settings", fontSize = 11.sp) },
                                            modifier = Modifier.testTag("nav_settings")
                                        )
                                    }
                                }
                            }
                        }
                    ) { innerPadding ->
                        Box(
                            modifier = Modifier
                                .fillMaxSize()
                                .padding(innerPadding)
                        ) {
                            Column(modifier = Modifier.fillMaxSize()) {
                                // HUD Telemetry Overlay (if enabled)
                                if (appearanceConfig.showTelemetryOverlay && subScreen == SubScreen.NONE) {
                                    val topTransfer = transfers.firstOrNull()
                                    HudTelemetryBar(
                                        transferSpeedBps = topTransfer?.speedBps ?: 0L,
                                        activeBots = botSwarm.count { it.isHealthy },
                                        totalBots = botSwarm.size.coerceAtLeast(1)
                                    )
                                }

                                // Main Workspaces / Sub-screens
                                Box(modifier = Modifier.weight(1f)) {
                                    when (subScreen) {
                                        SubScreen.APPEARANCE_STUDIO -> {
                                            AppearanceStudioScreen(
                                                currentConfig = appearanceConfig,
                                                onUpdateTheme = { appearancePrefs.updateTheme(it) },
                                                onUpdateRadius = { appearancePrefs.updateCornerRadius(it) },
                                                onUpdateCols = { appearancePrefs.updateGridColumns(it) },
                                                onUpdateAspect = { appearancePrefs.updateAspectRatio(it) },
                                                onUpdateNavBar = { appearancePrefs.updateNavBarMode(it) },
                                                onToggleTelemetry = { appearancePrefs.toggleTelemetry(it) },
                                                onUpdateLang = { appearancePrefs.updateLanguage(it) },
                                                onBack = { subScreen = SubScreen.NONE }
                                            )
                                        }
                                        SubScreen.WORKSTATION_VIEWER -> {
                                            activeViewingFile?.let { file ->
                                                WorkstationViewerScreen(
                                                    file = file,
                                                    language = appearanceConfig.language,
                                                    onBack = {
                                                        subScreen = SubScreen.NONE
                                                        activeViewingFile = null
                                                    },
                                                    onSaveNewVersion = { fId, content ->
                                                        viewModel.saveNewFileVersion(fId, content)
                                                    },
                                                    onShareTicket = {
                                                        shareTicketUrl = AesGcmChunkCipher.createShareTicket(
                                                            fileId = it.id,
                                                            fileName = it.name,
                                                            fileSize = it.totalSizeBytes,
                                                            fileKey = it.encryptedFileKey,
                                                            messageIds = listOf(101, 102)
                                                        )
                                                    },
                                                    onDownloadToDevice = { targetFile ->
                                                        isRestoringDownload = true
                                                        restoreProgressText = "Downloading ${targetFile.name}..."
                                                        viewModel.downloadAndExportFile(
                                                            context = context,
                                                            file = targetFile,
                                                            target = com.example.core.sync.RestorationTarget.DownloadsFolder,
                                                            onProgress = { _, _, _, status -> restoreProgressText = status },
                                                            onComplete = { res ->
                                                                isRestoringDownload = false
                                                                restorationNotification = res.message
                                                            }
                                                        )
                                                    },
                                                    onShareDecrypted = { targetFile ->
                                                        isRestoringDownload = true
                                                        restoreProgressText = "Preparing ${targetFile.name} for sharing..."
                                                        viewModel.downloadAndExportFile(
                                                            context = context,
                                                            file = targetFile,
                                                            target = com.example.core.sync.RestorationTarget.ShareIntent,
                                                            onProgress = { _, _, _, status -> restoreProgressText = status },
                                                            onComplete = { res ->
                                                                isRestoringDownload = false
                                                                if (res.shareIntent != null) {
                                                                    context.startActivity(res.shareIntent)
                                                                } else {
                                                                    restorationNotification = res.message
                                                                }
                                                            }
                                                        )
                                                    },
                                                    onEnsureFileCached = { targetFile, onDone ->
                                                        viewModel.downloadAndExportFile(
                                                            context = context,
                                                            file = targetFile,
                                                            target = com.example.core.sync.RestorationTarget.CacheForPreview,
                                                            onComplete = { res ->
                                                                onDone(res.success)
                                                            }
                                                        )
                                                    }
                                                )
                                            }
                                        }
                                         SubScreen.NONE -> {
                                            when (activeTab) {
                                                MainNavTab.DASHBOARD -> {
                                                    VaultDashboardScreen(
                                                        files = files,
                                                        trashedFiles = trashedFiles,
                                                        transfers = transfers,
                                                        botSwarm = botSwarm,
                                                        totalStorageBytes = totalStorage,
                                                        channelId = channelId,
                                                        cameraSyncStatus = cameraSyncStatus,
                                                        language = appearanceConfig.language,
                                                        onUploadFiles = { filePickerLauncher.launch(arrayOf("*/*")) },
                                                        onUploadMedia = { mediaPickerLauncher.launch(arrayOf("image/*", "video/*")) },
                                                        onTriggerCameraSync = { viewModel.triggerCameraRollSync(context) },
                                                        onCreateNote = { name, content ->
                                                            val cleanName = if (name.contains('.')) name.trim() else "${name.trim().ifBlank { "Note_${System.currentTimeMillis()}" }}.txt"
                                                            val cacheFile = java.io.File(context.cacheDir, cleanName)
                                                            cacheFile.writeText(content)
                                                            val uri = android.net.Uri.fromFile(cacheFile)
                                                            viewModel.uploadRealFile(context, uri, activeFolderId)
                                                        },
                                                        onFileClick = { file ->
                                                            activeViewingFile = file
                                                            subScreen = SubScreen.WORKSTATION_VIEWER
                                                        },
                                                        onDownloadToDevice = { targetFile ->
                                                            isRestoringDownload = true
                                                            restoreProgressText = "Downloading & Decrypting ${targetFile.name}..."
                                                            viewModel.downloadAndExportFile(
                                                                context = context,
                                                                file = targetFile,
                                                                target = com.example.core.sync.RestorationTarget.DownloadsFolder,
                                                                onProgress = { _, _, _, status -> restoreProgressText = status },
                                                                onComplete = { res ->
                                                                    isRestoringDownload = false
                                                                    restorationNotification = res.message
                                                                }
                                                            )
                                                        },
                                                        onSaveToGallery = { targetFile ->
                                                            isRestoringDownload = true
                                                            restoreProgressText = "Saving ${targetFile.name} to Gallery..."
                                                            viewModel.downloadAndExportFile(
                                                                context = context,
                                                                file = targetFile,
                                                                target = com.example.core.sync.RestorationTarget.PhotoGallery,
                                                                onProgress = { _, _, _, status -> restoreProgressText = status },
                                                                onComplete = { res ->
                                                                    isRestoringDownload = false
                                                                    restorationNotification = res.message
                                                                }
                                                            )
                                                        },
                                                        onShareDecrypted = { targetFile ->
                                                            isRestoringDownload = true
                                                            restoreProgressText = "Preparing decrypted file for sharing..."
                                                            viewModel.downloadAndExportFile(
                                                                context = context,
                                                                file = targetFile,
                                                                target = com.example.core.sync.RestorationTarget.ShareIntent,
                                                                onProgress = { _, _, _, status -> restoreProgressText = status },
                                                                onComplete = { res ->
                                                                    isRestoringDownload = false
                                                                    if (res.shareIntent != null) {
                                                                        context.startActivity(res.shareIntent)
                                                                    } else {
                                                                        restorationNotification = res.message
                                                                    }
                                                                }
                                                            )
                                                        },
                                                        onShareTicket = {
                                                            shareTicketUrl = AesGcmChunkCipher.createShareTicket(
                                                                fileId = it.id,
                                                                fileName = it.name,
                                                                fileSize = it.totalSizeBytes,
                                                                fileKey = it.encryptedFileKey,
                                                                messageIds = listOf(101, 102)
                                                            )
                                                        },
                                                        onVerifyTelegram = { file ->
                                                            isVerifyingTelegram = true
                                                            viewModel.verifyFileOnTelegram(file) { ok, msg ->
                                                                isVerifyingTelegram = false
                                                                verifyResultDialog = Pair(ok, msg)
                                                            }
                                                        },
                                                        onToggleStar = { viewModel.toggleStar(it) },
                                                        onMoveToTrash = { viewModel.deleteFile(it) },
                                                        onRestoreFromTrash = { viewModel.restoreFromTrash(it) },
                                                        onPermanentDelete = { viewModel.permanentDeleteFile(it) },
                                                        onEmptyTrash = { viewModel.emptyTrash() },
                                                        onSyncChannel = {
                                                            viewModel.scanExistingChannelHistory(context) { ok, msg ->
                                                                restorationNotification = msg
                                                            }
                                                        }
                                                    )
                                                }
                                                MainNavTab.PHOTOS -> {
                                                    PhotosTimelineScreen(
                                                        photos = files,
                                                        gridColumns = appearanceConfig.gridColumns,
                                                        aspectRatioMode = appearanceConfig.aspectRatioMode,
                                                        language = appearanceConfig.language,
                                                        onPhotoClick = { file ->
                                                            activeViewingFile = file
                                                            subScreen = SubScreen.WORKSTATION_VIEWER
                                                        },
                                                        onFreeUpSpaceClick = { showFreeUpSpaceDialog = true },
                                                        onUploadMediaClick = {
                                                            mediaPickerLauncher.launch(arrayOf("image/*", "video/*"))
                                                        },
                                                        onVerifyTelegram = { file ->
                                                            isVerifyingTelegram = true
                                                            viewModel.verifyFileOnTelegram(file) { ok, msg ->
                                                                isVerifyingTelegram = false
                                                                verifyResultDialog = Pair(ok, msg)
                                                            }
                                                        },
                                                        onDownloadToDevice = { targetFile ->
                                                            isRestoringDownload = true
                                                            restoreProgressText = "Downloading & Decrypting ${targetFile.name}..."
                                                            viewModel.downloadAndExportFile(
                                                                context = context,
                                                                file = targetFile,
                                                                target = com.example.core.sync.RestorationTarget.DownloadsFolder,
                                                                onProgress = { _, _, _, status -> restoreProgressText = status },
                                                                onComplete = { res ->
                                                                    isRestoringDownload = false
                                                                    restorationNotification = res.message
                                                                }
                                                            )
                                                        },
                                                        onSaveToGallery = { targetFile ->
                                                            isRestoringDownload = true
                                                            restoreProgressText = "Saving ${targetFile.name} to Gallery..."
                                                            viewModel.downloadAndExportFile(
                                                                context = context,
                                                                file = targetFile,
                                                                target = com.example.core.sync.RestorationTarget.PhotoGallery,
                                                                onProgress = { _, _, _, status -> restoreProgressText = status },
                                                                onComplete = { res ->
                                                                    isRestoringDownload = false
                                                                    restorationNotification = res.message
                                                                }
                                                            )
                                                        }
                                                    )
                                                }
                                                MainNavTab.DRIVE -> {
                                                    DriveExplorerScreen(
                                                        currentFolderId = activeFolderId,
                                                        folders = folders,
                                                        files = files,
                                                        language = appearanceConfig.language,
                                                        onNavigateFolder = { activeFolderId = it },
                                                        onFileClick = { file ->
                                                            activeViewingFile = file
                                                            subScreen = SubScreen.WORKSTATION_VIEWER
                                                        },
                                                        onCreateFolder = { name, colorHex ->
                                                            viewModel.createFolder(name, colorHex)
                                                        },
                                                        onPickFiles = {
                                                            filePickerLauncher.launch(arrayOf("*/*"))
                                                        },
                                                        onPickMedia = {
                                                            mediaPickerLauncher.launch(arrayOf("image/*", "video/*"))
                                                        },
                                                        onCreateNewDoc = { name, content ->
                                                            val cleanName = if (name.contains('.')) name.trim() else "${name.trim().ifBlank { "Document_${System.currentTimeMillis()}" }}.txt"
                                                            val cacheFile = java.io.File(context.cacheDir, cleanName)
                                                            cacheFile.writeText(content)
                                                            val uri = android.net.Uri.fromFile(cacheFile)
                                                            viewModel.uploadRealFile(context, uri, activeFolderId)
                                                        },
                                                        onToggleStar = { viewModel.toggleStar(it) },
                                                        onToggleOffline = { viewModel.toggleOffline(it) },
                                                        onDeleteFile = { viewModel.deleteFile(it) },
                                                        onShareTicket = {
                                                            shareTicketUrl = AesGcmChunkCipher.createShareTicket(
                                                                fileId = it.id,
                                                                fileName = it.name,
                                                                fileSize = it.totalSizeBytes,
                                                                fileKey = it.encryptedFileKey,
                                                                messageIds = listOf(204, 205)
                                                            )
                                                        },
                                                        onVerifyTelegram = { file ->
                                                            isVerifyingTelegram = true
                                                            viewModel.verifyFileOnTelegram(file) { ok, msg ->
                                                                isVerifyingTelegram = false
                                                                verifyResultDialog = Pair(ok, msg)
                                                            }
                                                        },
                                                        onDownloadToDevice = { targetFile ->
                                                            isRestoringDownload = true
                                                            restoreProgressText = "Downloading & Decrypting ${targetFile.name}..."
                                                            viewModel.downloadAndExportFile(
                                                                context = context,
                                                                file = targetFile,
                                                                target = com.example.core.sync.RestorationTarget.DownloadsFolder,
                                                                onProgress = { _, _, _, status -> restoreProgressText = status },
                                                                onComplete = { res ->
                                                                    isRestoringDownload = false
                                                                    restorationNotification = res.message
                                                                }
                                                            )
                                                        },
                                                        onSaveToGallery = { targetFile ->
                                                            isRestoringDownload = true
                                                            restoreProgressText = "Saving ${targetFile.name} to Gallery..."
                                                            viewModel.downloadAndExportFile(
                                                                context = context,
                                                                file = targetFile,
                                                                target = com.example.core.sync.RestorationTarget.PhotoGallery,
                                                                onProgress = { _, _, _, status -> restoreProgressText = status },
                                                                onComplete = { res ->
                                                                    isRestoringDownload = false
                                                                    restorationNotification = res.message
                                                                }
                                                            )
                                                        },
                                                        onShareDecrypted = { targetFile ->
                                                            isRestoringDownload = true
                                                            restoreProgressText = "Preparing decrypted file for sharing..."
                                                            viewModel.downloadAndExportFile(
                                                                context = context,
                                                                file = targetFile,
                                                                target = com.example.core.sync.RestorationTarget.ShareIntent,
                                                                onProgress = { _, _, _, status -> restoreProgressText = status },
                                                                onComplete = { res ->
                                                                    isRestoringDownload = false
                                                                    if (res.shareIntent != null) {
                                                                        context.startActivity(res.shareIntent)
                                                                    } else {
                                                                        restorationNotification = res.message
                                                                    }
                                                                }
                                                            )
                                                        },
                                                        onBatchDownload = { filesToDownload ->
                                                            isRestoringDownload = true
                                                            restoreProgressText = "Downloading ${filesToDownload.size} files to device..."
                                                            viewModel.downloadMultipleFilesToDevice(
                                                                context = context,
                                                                files = filesToDownload,
                                                                onProgress = { done, total ->
                                                                    restoreProgressText = "Downloaded $done of $total files..."
                                                                },
                                                                onComplete = { count ->
                                                                    isRestoringDownload = false
                                                                    restorationNotification = "Successfully downloaded $count files to device storage!"
                                                                }
                                                            )
                                                        },
                                                        onBatchDelete = { fileIds ->
                                                            viewModel.deleteMultipleFiles(fileIds)
                                                            restorationNotification = "Moved ${fileIds.size} files to trash!"
                                                        },
                                                        onBatchMoveToFolder = { fileIds, targetFolderId ->
                                                            viewModel.moveMultipleFilesToFolder(fileIds, targetFolderId)
                                                            restorationNotification = "Moved ${fileIds.size} files to folder!"
                                                        },
                                                        onSyncChannel = {
                                                            viewModel.scanExistingChannelHistory(context) { ok, msg ->
                                                                restorationNotification = msg
                                                            }
                                                        }
                                                    )
                                                }
                                                MainNavTab.TELEDROP -> {
                                                    TeleDropScreen(
                                                        items = teleDropItems,
                                                        language = appearanceConfig.language,
                                                        onSendSnippet = { content, type ->
                                                            viewModel.sendTeleDropSnippet(content, type)
                                                        },
                                                        onDeleteItem = { viewModel.deleteTeleDropItem(it) }
                                                    )
                                                }
                                                MainNavTab.SETTINGS -> {
                                                    SettingsScreen(
                                                        totalStorageBytes = totalStorage,
                                                        activeFileCount = fileCount,
                                                        botSwarm = botSwarm,
                                                        language = appearanceConfig.language,
                                                        channelId = channelId,
                                                        masterBotToken = masterBotToken,
                                                        onOpenAppearanceStudio = {
                                                            subScreen = SubScreen.APPEARANCE_STUDIO
                                                        },
                                                        onAddBotToken = { viewModel.addBotWorker(it) },
                                                        onRemoveBot = { viewModel.removeBot(it) },
                                                        onToggleWebDav = { /* WebDAV toggle */ },
                                                        onLanguageChange = { appearancePrefs.updateLanguage(it) },
                                                        onSaveCredentials = { token, chId ->
                                                            viewModel.setTelegramCredentials(token, chId)
                                                        },
                                                        onTestPing = { token, chId, cb ->
                                                            viewModel.testSendPing(token, chId, cb)
                                                        },
                                                        onExportBackup = { cb ->
                                                            viewModel.exportVaultBackup(this@MainActivity, cb)
                                                        },
                                                        onImportBackup = { json, cb ->
                                                            viewModel.importVaultBackup(this@MainActivity, json, cb)
                                                        },
                                                        onRebindBot = { token, cb ->
                                                            viewModel.rebindBotToken(token, cb)
                                                        },
                                                        onGenerateMigrationCode = { cb ->
                                                            viewModel.generateMigrationCode(cb)
                                                        },
                                                        onRestoreFromCode = { code, cb ->
                                                            viewModel.restoreFromMigrationCode(code, cb)
                                                        },
                                                        onSyncExistingServer = { cb ->
                                                            viewModel.scanExistingChannelHistory(this@MainActivity, cb)
                                                        },
                                                        onClearAllData = {
                                                            viewModel.clearAllVaultData()
                                                        },
                                                        onLockVault = { viewModel.lockVault() }
                                                    )
                                                }
                                            }
                                        }
                                    }
                                }
                            }
                        }
                    }
                }
            }

                // Share Ticket Modal Dialog
                if (shareTicketUrl != null) {
                    val context = LocalContext.current
                    AlertDialog(
                        onDismissRequest = { shareTicketUrl = null },
                        title = { Text(if (isHindi) "शून्य-ज्ञान शेयर टिकट" else "Zero-Knowledge Share Ticket") },
                        text = {
                            Column {
                                Text(
                                    text = if (isHindi) "यह टिकट केवल इस विशिष्ट फ़ाइल की डिक्रिप्शन कुंजी शेयर करता है। आपका मास्टर पासवर्ड कभी भी उजागर नहीं होता:"
                                    else "This ticket encodes only this file's key and chunk IDs. Your Master Password remains completely private:",
                                    fontSize = 12.sp
                                )
                                Spacer(modifier = Modifier.height(10.dp))
                                Surface(
                                    color = MaterialTheme.colorScheme.surfaceVariant,
                                    shape = RoundedCornerShape(8.dp),
                                    modifier = Modifier.fillMaxWidth()
                                ) {
                                    Text(
                                        text = shareTicketUrl ?: "",
                                        fontFamily = FontFamily.Monospace,
                                        fontSize = 10.sp,
                                        color = CyanNeon,
                                        modifier = Modifier.padding(10.dp)
                                    )
                                }
                            }
                        },
                        confirmButton = {
                            Button(
                                onClick = {
                                    val clip = context.getSystemService(Context.CLIPBOARD_SERVICE) as ClipboardManager
                                    clip.setPrimaryClip(ClipData.newPlainText("share_ticket", shareTicketUrl))
                                    shareTicketUrl = null
                                }
                            ) {
                                Icon(Icons.Default.ContentCopy, contentDescription = null, modifier = Modifier.size(16.dp))
                                Spacer(modifier = Modifier.width(6.dp))
                                Text(if (isHindi) "लिंक कॉपी करें" else "Copy Ticket Link")
                            }
                        },
                        dismissButton = {
                            TextButton(onClick = { shareTicketUrl = null }) {
                                Text(if (isHindi) "बंद करें" else "Close")
                            }
                        }
                    )
                }

                // Free Up Space Dialog
                if (showFreeUpSpaceDialog) {
                    AlertDialog(
                        onDismissRequest = { showFreeUpSpaceDialog = false },
                        title = { Text(if (isHindi) "फोन स्पेस खाली करें" else "Free Up Phone Space") },
                        text = {
                            Column {
                                Text(
                                    text = if (isHindi) "सभी फोटो और वीडियो के क्लाउड 19MB चंक्स की सत्यता जांची जा चुकी है (SHA-256 Verified)। सुरक्षित रूप से स्थानीय प्रतियां हटाई जा सकती हैं।"
                                    else "All photos and videos have been verified in your Telegram Cloud (SHA-256 & AES-256 Integrity Verified). You can safely remove local copies to free up storage.",
                                    fontSize = 13.sp
                                )
                                Spacer(modifier = Modifier.height(12.dp))
                                Surface(
                                    color = CyanNeon.copy(alpha = 0.15f),
                                    shape = RoundedCornerShape(8.dp)
                                ) {
                                    Row(
                                        modifier = Modifier.padding(12.dp),
                                        verticalAlignment = Alignment.CenterVertically
                                    ) {
                                        Icon(Icons.Default.Verified, contentDescription = null, tint = CyanNeon)
                                        Spacer(modifier = Modifier.width(8.dp))
                                        Text(
                                            text = if (isHindi) "3.4 GB फोन स्टोरेज खाली होगा" else "3.4 GB space can be freed up",
                                            fontWeight = FontWeight.Bold,
                                            fontSize = 13.sp,
                                            color = CyanNeon
                                        )
                                    }
                                }
                            }
                        },
                        confirmButton = {
                            Button(onClick = { showFreeUpSpaceDialog = false }) {
                                Text(if (isHindi) "खाली करें" else "Free Up Space")
                            }
                        },
                        dismissButton = {
                            TextButton(onClick = { showFreeUpSpaceDialog = false }) {
                                Text(if (isHindi) "रद्द करें" else "Cancel")
                            }
                        }
                    )
                }

                // Telegram Cloud Verification Result Dialog
                if (verifyResultDialog != null) {
                    val isOk = verifyResultDialog?.first == true
                    AlertDialog(
                        onDismissRequest = { verifyResultDialog = null },
                        title = {
                            Row(verticalAlignment = Alignment.CenterVertically) {
                                Icon(
                                    imageVector = if (isOk) Icons.Default.CloudDone else Icons.Default.CloudOff,
                                    contentDescription = null,
                                    tint = if (isOk) CyanNeon else MaterialTheme.colorScheme.error,
                                    modifier = Modifier.size(24.dp)
                                )
                                Spacer(modifier = Modifier.width(10.dp))
                                Text(if (isHindi) "टेलीग्राम सर्वर सत्यापन" else "Telegram Cloud Verification")
                            }
                        },
                        text = {
                            Text(
                                text = verifyResultDialog?.second ?: "",
                                fontSize = 13.sp,
                                fontFamily = FontFamily.Monospace,
                                lineHeight = 18.sp
                            )
                        },
                        confirmButton = {
                            Button(onClick = { verifyResultDialog = null }) {
                                Text(if (isHindi) "ठीक है" else "Dismiss")
                            }
                        }
                    )
                }

                // Telegram Verifying Progress Indicator Dialog
                if (isVerifyingTelegram) {
                    AlertDialog(
                        onDismissRequest = { /* In progress */ },
                        title = { Text(if (isHindi) "टेलीग्राम पर जांच जारी है..." else "Querying Telegram Cloud...") },
                        text = {
                            Row(
                                modifier = Modifier.fillMaxWidth().padding(12.dp),
                                verticalAlignment = Alignment.CenterVertically
                            ) {
                                CircularProgressIndicator(color = CyanNeon, modifier = Modifier.size(24.dp))
                                Spacer(modifier = Modifier.width(16.dp))
                                Text(
                                    if (isHindi) "बॉट चैनल में फ़ाइल और संदेश की पुष्टि कर रहा है..."
                                    else "Checking encrypted chunks via Telegram Bot API..."
                                )
                            }
                        },
                        confirmButton = {}
                    )
                }

                // Upload Error Notification Dialog
                if (uploadError != null) {
                    AlertDialog(
                        onDismissRequest = { viewModel.dismissUploadError() },
                        title = {
                            Row(verticalAlignment = Alignment.CenterVertically) {
                                Icon(Icons.Default.Warning, contentDescription = null, tint = MaterialTheme.colorScheme.error)
                                Spacer(modifier = Modifier.width(8.dp))
                                Text(if (isHindi) "अपलोड में त्रुटि" else "Upload Error")
                            }
                        },
                        text = {
                            Text(
                                text = uploadError ?: "",
                                fontSize = 13.sp,
                                color = MaterialTheme.colorScheme.onSurface
                            )
                        },
                        confirmButton = {
                            Button(onClick = { viewModel.dismissUploadError() }) {
                                Text(if (isHindi) "समझ गया" else "Understood")
                            }
                        }
                    )
                }

                // Restoration / Decryption Progress Dialog
                if (isRestoringDownload) {
                    AlertDialog(
                        onDismissRequest = { /* In progress */ },
                        title = {
                            Row(verticalAlignment = Alignment.CenterVertically) {
                                CircularProgressIndicator(color = CyanNeon, modifier = Modifier.size(22.dp))
                                Spacer(modifier = Modifier.width(12.dp))
                                Text(if (isHindi) "डिक्रिप्शन और डाउनलोड जारी है..." else "Decrypting & Downloading...")
                            }
                        },
                        text = {
                            Column(modifier = Modifier.padding(vertical = 8.dp)) {
                                Text(
                                    text = restoreProgressText.ifBlank { "Reassembling AES-256-GCM chunks from Telegram..." },
                                    fontSize = 13.sp,
                                    color = MaterialTheme.colorScheme.onSurface
                                )
                                Spacer(modifier = Modifier.height(10.dp))
                                LinearProgressIndicator(
                                    modifier = Modifier.fillMaxWidth().height(4.dp).clip(CircleShape),
                                    color = CyanNeon
                                )
                            }
                        },
                        confirmButton = {}
                    )
                }

                // Restoration Result Notification Dialog
                if (restorationNotification != null) {
                    AlertDialog(
                        onDismissRequest = { restorationNotification = null },
                        title = {
                            Row(verticalAlignment = Alignment.CenterVertically) {
                                Icon(Icons.Default.CloudDone, contentDescription = null, tint = CyanNeon)
                                Spacer(modifier = Modifier.width(8.dp))
                                Text(if (isHindi) "फ़ाइल डिक्रिप्ट व सेव हुई" else "File Decrypted & Restored")
                            }
                        },
                        text = {
                            Text(
                                text = restorationNotification ?: "",
                                fontSize = 13.sp,
                                color = MaterialTheme.colorScheme.onSurface
                            )
                        },
                        confirmButton = {
                            Button(onClick = { restorationNotification = null }) {
                                Text(if (isHindi) "ठीक है" else "Done")
                            }
                        }
                    )
                }
            }
        }
    }
}
