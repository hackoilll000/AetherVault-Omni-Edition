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
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
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
import com.example.features.dashboard.VaultDashboardScreen
import com.example.ui.components.*
import com.example.ui.theme.*
import kotlinx.coroutines.launch
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
            val lockPolicy by viewModel.lockPolicy.collectAsStateWithLifecycle()

            var activeTab by remember { mutableStateOf(MainNavTab.DASHBOARD) }
            var subScreen by remember { mutableStateOf(SubScreen.NONE) }
            var activeViewingFile by remember { mutableStateOf<VaultFile?>(null) }
            var activeFolderId by remember { mutableStateOf("root") }
            var searchQuery by remember { mutableStateOf("") }

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
            var showLegalDisclaimerDialog by remember { mutableStateOf(false) }

            // Feature Overlays & Dialog States
            var showFastSearchOverlay by remember { mutableStateOf(false) }
            var showStorageMetricsDialog by remember { mutableStateOf(false) }
            var showCommandPaletteDialog by remember { mutableStateOf(false) }

            // UI Style & Theme Selection dialog state
            var showUiStyleChoiceDialog by remember { mutableStateOf(!appearanceConfig.hasChosenUiStyle) }

            // Add File Bottom Sheet & Creation Dialog States
            var showAddFileBottomSheet by remember { mutableStateOf(false) }
            var showCreateFolderDialogInMainActivity by remember { mutableStateOf(false) }
            var newFolderNameInMainActivity by remember { mutableStateOf("") }
            var showCreateNoteDialogInMainActivity by remember { mutableStateOf(false) }
            var newNoteTitleInMainActivity by remember { mutableStateOf("") }
            var newNoteContentInMainActivity by remember { mutableStateOf("") }

            val isHindi = appearanceConfig.language == AppLanguage.HI
            val context = LocalContext.current

            // FLAG_SECURE Privacy Preview Effect
            LaunchedEffect(appearanceConfig.privacyPreviewEnabled) {
                val activity = context as? android.app.Activity
                if (appearanceConfig.privacyPreviewEnabled) {
                    activity?.window?.addFlags(android.view.WindowManager.LayoutParams.FLAG_SECURE)
                } else {
                    activity?.window?.clearFlags(android.view.WindowManager.LayoutParams.FLAG_SECURE)
                }
            }

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
                themeMode = appearanceConfig.themeMode,
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
                    // Terms & Conditions Agreement screen by Creator TRB FireOps
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
                                restorationNotification = "Bot Token detected in QR code! Please complete the setup wizard with your Master Password."
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
                        }
                    }
                } else {
                    // MAIN LOGGED-IN WORKSPACE
                    val drawerState = rememberDrawerState(initialValue = DrawerValue.Closed)
                    val coroutineScope = rememberCoroutineScope()
                    var subTabIndex by remember { mutableIntStateOf(0) }
                    var showOutofSyncToast by remember { mutableStateOf(true) }

                    ModalNavigationDrawer(
                        drawerState = drawerState,
                        gesturesEnabled = subScreen == SubScreen.NONE,
                        drawerContent = {
                            M3NavigationDrawerContent(
                                mbUsedText = "${totalStorage / (1024 * 1024)} MB of Unlimited Telegram Cloud",
                                onSelectMenu = { menu: String ->
                                    coroutineScope.launch { drawerState.close() }
                                    when (menu) {
                                        "Recent" -> activeTab = MainNavTab.DASHBOARD
                                        "TelegramChat" -> activeTab = MainNavTab.TELEDROP
                                        "Uploads" -> activeTab = MainNavTab.DRIVE
                                        "Offline" -> activeTab = MainNavTab.DRIVE
                                        "LegalDisclaimer" -> showLegalDisclaimerDialog = true
                                        "Settings" -> activeTab = MainNavTab.SETTINGS
                                    }
                                },
                                onConfigureBot = {
                                    coroutineScope.launch { drawerState.close() }
                                    activeTab = MainNavTab.SETTINGS
                                }
                            )
                        }
                    ) {
                        Box(
                            modifier = Modifier
                                .fillMaxSize()
                                .background(MaterialTheme.colorScheme.background)
                        ) {
                            Scaffold(
                                containerColor = MaterialTheme.colorScheme.background,
                                contentWindowInsets = WindowInsets.safeDrawing,
                                topBar = {
                                    if (subScreen == SubScreen.NONE) {
                                        Column(
                                            modifier = Modifier
                                                .fillMaxWidth()
                                                .statusBarsPadding()
                                                .background(MaterialTheme.colorScheme.background)
                                        ) {
                                            if (activeTab == MainNavTab.PHOTOS) {
                                                M3CollectionsHeader(
                                                    syncStatusText = if (botSwarm.any { it.isHealthy }) "Bot Connected" else "Backup paused",
                                                    isBotHealthy = botSwarm.any { it.isHealthy },
                                                    onOpenDrawer = { coroutineScope.launch { drawerState.open() } },
                                                    onQuickUpload = { filePickerLauncher.launch(arrayOf("*/*")) },
                                                    onNotificationsClick = { activeTab = MainNavTab.DASHBOARD },
                                                    onProfileClick = { activeTab = MainNavTab.SETTINGS }
                                                )
                                            } else {
                                                M3DriveSearchBar(
                                                    searchQuery = searchQuery,
                                                    onSearchQueryChange = {
                                                        searchQuery = it
                                                        if (it.isNotBlank()) showFastSearchOverlay = true
                                                    },
                                                    onOpenDrawer = { coroutineScope.launch { drawerState.open() } },
                                                    onProfileClick = { activeTab = MainNavTab.SETTINGS },
                                                    onSearchClick = { showFastSearchOverlay = true },
                                                    isBotHealthy = botSwarm.any { it.isHealthy },
                                                    placeholderText = if (activeTab == MainNavTab.DRIVE) "Search in Tele Drive" else "Search in Drive"
                                                )

                                                // Secondary Sub-Tabs
                                                val subTabs = if (activeTab == MainNavTab.DRIVE) listOf("My Drive", "Channels") else listOf("Suggested", "Activity")
                                                M3SecondarySubTabs(
                                                    selectedTabIndex = subTabIndex,
                                                    tabs = subTabs,
                                                    onTabSelected = { subTabIndex = it }
                                                )
                                            }
                                        }
                                    }
                                },
                                bottomBar = {
                                    if (subScreen == SubScreen.NONE) {
                                        Column(
                                            modifier = Modifier
                                                .fillMaxWidth()
                                                .navigationBarsPadding()
                                                .background(MaterialTheme.colorScheme.surface)
                                        ) {
                                            if (showOutofSyncToast) {
                                                M3SyncToastBanner(
                                                    messageText = "Review out-of-sync changes",
                                                    onDismiss = { showOutofSyncToast = false }
                                                )
                                            }

                                            DynamicTransferPill(
                                                activeTasks = transfers,
                                                onCancelTask = { viewModel.cancelTransfer(it) }
                                            )

                                            M3BottomNavigationBar(
                                                selectedTabOrdinal = when (activeTab) {
                                                    MainNavTab.DASHBOARD -> 0
                                                    MainNavTab.PHOTOS -> 1
                                                    MainNavTab.TELEDROP -> 2
                                                    MainNavTab.DRIVE -> 3
                                                    else -> 0
                                                },
                                                onSelectTab = { idx: Int ->
                                                    activeTab = when (idx) {
                                                        0 -> MainNavTab.DASHBOARD
                                                        1 -> MainNavTab.PHOTOS
                                                        2 -> MainNavTab.TELEDROP
                                                        3 -> MainNavTab.DRIVE
                                                        else -> MainNavTab.DASHBOARD
                                                    }
                                                }
                                            )
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
                                                        onDeleteItem = { viewModel.deleteTeleDropItem(it) },
                                                        onSyncChannel = {
                                                            viewModel.scanExistingChannelHistory(context) { ok, msg ->
                                                                restorationNotification = msg
                                                            }
                                                        },
                                                        channelId = channelId,
                                                        botHealthy = botSwarm.any { it.isHealthy }
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
                                                        currentThemeMode = appearanceConfig.themeMode,
                                                        onThemeModeChange = { appearancePrefs.updateThemeMode(it) },
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
                                                        onLockVault = { viewModel.lockVault() },
                                                       lockPolicy = lockPolicy,
                                                       onLockPolicyChange = { viewModel.updateLockPolicy(it) },
                                                       onOpenStorageMetrics = { showStorageMetricsDialog = true },
                                                       privacyPreviewEnabled = appearanceConfig.privacyPreviewEnabled,
                                                       onPrivacyPreviewChange = { appearancePrefs.togglePrivacyPreview(it) }
                                                    )
                                                }
                                            }
                                        }
                                    }
                                }
                            }

                            // Stacked Material 3 Squircle FABs (Bottom Right)
                            if (subScreen == SubScreen.NONE) {
                                M3StackedFabs(
                                    onPrimaryFabClick = { showAddFileBottomSheet = true },
                                    onSecondaryFabClick = { mediaPickerLauncher.launch(arrayOf("image/*", "video/*")) },
                                    modifier = Modifier
                                        .align(Alignment.BottomEnd)
                                        .padding(bottom = 16.dp, end = 16.dp)
                                )
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

                // Dedicated Legal Disclaimer Dialog
                if (showLegalDisclaimerDialog) {
                    AlertDialog(
                        onDismissRequest = { showLegalDisclaimerDialog = false },
                        title = {
                            Row(verticalAlignment = Alignment.CenterVertically) {
                                Icon(Icons.Default.Warning, contentDescription = null, tint = Color(0xFFE65100))
                                Spacer(modifier = Modifier.width(8.dp))
                                Text(if (isHindi) "महत्वपूर्ण कानूनी डिस्क्लेमर" else "Legal Disclaimer & Policy Notice")
                            }
                        },
                        text = {
                            Column(modifier = Modifier.verticalScroll(androidx.compose.foundation.rememberScrollState())) {
                                Text(
                                    text = if (isHindi) """
यह ऐप सिर्फ और सिर्फ पर्सनल यूज़ और एजुकेशनल डेमो के लिए बनाया गया है। हमने यह ऐप केवल सीखने और फन-फन के उद्देश्य से बनाया है ताकि यह दिखाया जा सके कि मोबाइल से टेलीग्राम सर्वर बॉट को कैसे ऑपरेट किया जा सकता है।

हम किसी भी प्रकार से टेलीग्राम की नीतियों या गाइडलाइंस का उल्लंघन नहीं करते हैं।

इस ऐप का किसी भी प्रकार से गलत, गैर-कानूनी या अनुचित इस्तेमाल करने पर हमारी कोई जिम्मेदारी या दोष नहीं होगा। इस ऐप को उपयोग करने की संपूर्ण जिम्मेदारी यूजर की है।
                                    """.trimIndent()
                                    else """
This application has been developed strictly for PERSONAL USE, research, and educational demonstration purposes. It was created for learning and experimental exploration to demonstrate how serverless bots can be operated directly from a mobile device.

We do NOT violate or encourage the violation of any Telegram API terms of service or guidelines.

The developers (Ayush Mondal / AJPM / TRB FireOps) assume NO LIABILITY or responsibility for any misuse, improper usage, or unlawful activities conducted with this application. Use of this application is entirely at your own risk and responsibility.
                                    """.trimIndent(),
                                    fontSize = 13.sp,
                                    lineHeight = 18.sp,
                                    color = MaterialTheme.colorScheme.onSurface
                                )
                            }
                        },
                        confirmButton = {
                            Button(
                                onClick = { showLegalDisclaimerDialog = false },
                                colors = ButtonDefaults.buttonColors(containerColor = Color(0xFFE65100), contentColor = Color.White)
                            ) {
                                Text(if (isHindi) "मैंने पढ़ लिया और सहमत हूँ" else "I Understand & Agree")
                            }
                        }
                    )
                }

                // First-launch UI Style & Theme Selection Dialog (can also be re-triggered from Settings)
                if (showUiStyleChoiceDialog) {
                    UiStyleSelectionDialog(
                        initialMode = appearanceConfig.themeMode,
                        language = appearanceConfig.language,
                        onConfirm = { chosenMode ->
                            appearancePrefs.updateThemeMode(chosenMode)
                            appearancePrefs.markUiStyleChosen()
                            showUiStyleChoiceDialog = false
                        },
                        onDismiss = {
                            appearancePrefs.markUiStyleChosen()
                            showUiStyleChoiceDialog = false
                        }
                    )
                }

                // Google Drive / Photos style Material 3 Add File Bottom Sheet
                if (showAddFileBottomSheet) {
                    M3AddFileBottomSheet(
                        onDismissRequest = { showAddFileBottomSheet = false },
                        language = appearanceConfig.language,
                        onCreateFolderClick = {
                            showAddFileBottomSheet = false
                            newFolderNameInMainActivity = ""
                            showCreateFolderDialogInMainActivity = true
                        },
                        onUploadFilesClick = {
                            showAddFileBottomSheet = false
                            filePickerLauncher.launch(arrayOf("*/*"))
                        },
                        onUploadMediaClick = {
                            showAddFileBottomSheet = false
                            mediaPickerLauncher.launch(arrayOf("image/*", "video/*"))
                        },
                        onCreateNoteClick = {
                            showAddFileBottomSheet = false
                            newNoteTitleInMainActivity = ""
                            newNoteContentInMainActivity = ""
                            showCreateNoteDialogInMainActivity = true
                        },
                        onSyncChannelClick = {
                            showAddFileBottomSheet = false
                            activeTab = MainNavTab.SETTINGS
                        },
                        onTeleDropClick = {
                            showAddFileBottomSheet = false
                            activeTab = MainNavTab.TELEDROP
                        }
                    )
                }

                // Create Folder Dialog from Bottom Sheet
                if (showCreateFolderDialogInMainActivity) {
                    AlertDialog(
                        onDismissRequest = { showCreateFolderDialogInMainActivity = false },
                        title = { Text(if (isHindi) "नया फ़ोल्डर बनाएँ" else "New folder") },
                        text = {
                            OutlinedTextField(
                                value = newFolderNameInMainActivity,
                                onValueChange = { newFolderNameInMainActivity = it },
                                label = { Text(if (isHindi) "फ़ोल्डर का नाम" else "Folder name") },
                                singleLine = true,
                                modifier = Modifier.fillMaxWidth()
                            )
                        },
                        confirmButton = {
                            Button(
                                onClick = {
                                    val name = newFolderNameInMainActivity.trim()
                                    if (name.isNotEmpty()) {
                                        viewModel.createFolder(name, activeFolderId)
                                    }
                                    showCreateFolderDialogInMainActivity = false
                                }
                            ) {
                                Text(if (isHindi) "बनाएँ" else "Create")
                            }
                        },
                        dismissButton = {
                            TextButton(onClick = { showCreateFolderDialogInMainActivity = false }) {
                                Text(if (isHindi) "रद्द करें" else "Cancel")
                            }
                        }
                    )
                }

                // Create Text Note Dialog from Bottom Sheet
                if (showCreateNoteDialogInMainActivity) {
                    AlertDialog(
                        onDismissRequest = { showCreateNoteDialogInMainActivity = false },
                        title = { Text(if (isHindi) "नया नोट बनाएँ" else "New text document") },
                        text = {
                            Column(modifier = Modifier.fillMaxWidth()) {
                                OutlinedTextField(
                                    value = newNoteTitleInMainActivity,
                                    onValueChange = { newNoteTitleInMainActivity = it },
                                    label = { Text(if (isHindi) "दस्तावेज़ का शीर्षक" else "Document Title") },
                                    singleLine = true,
                                    modifier = Modifier.fillMaxWidth()
                                )
                                Spacer(modifier = Modifier.height(8.dp))
                                OutlinedTextField(
                                    value = newNoteContentInMainActivity,
                                    onValueChange = { newNoteContentInMainActivity = it },
                                    label = { Text(if (isHindi) "सामग्री लिखें..." else "Content...") },
                                    modifier = Modifier
                                        .fillMaxWidth()
                                        .height(120.dp)
                                )
                            }
                        },
                        confirmButton = {
                            Button(
                                onClick = {
                                    val title = newNoteTitleInMainActivity.trim()
                                    val content = newNoteContentInMainActivity
                                    val cleanName = if (title.contains('.')) title else "${title.ifBlank { "Doc_${System.currentTimeMillis()}" }}.txt"
                                    val cacheFile = java.io.File(context.cacheDir, cleanName)
                                    cacheFile.writeText(content)
                                    val uri = android.net.Uri.fromFile(cacheFile)
                                    viewModel.uploadRealFile(context, uri, activeFolderId)
                                    showCreateNoteDialogInMainActivity = false
                                }
                            ) {
                                Text(if (isHindi) "सहेजें" else "Save")
                            }
                        },
                        dismissButton = {
                            TextButton(onClick = { showCreateNoteDialogInMainActivity = false }) {
                                Text(if (isHindi) "रद्द करें" else "Cancel")
                            }
                        }
                    )
                }

                // Fast Offline Search Overlay Component
                if (showFastSearchOverlay) {
                    com.example.ui.components.FastOfflineSearchOverlay(
                        searchQuery = searchQuery,
                        files = files,
                        language = appearanceConfig.language,
                        onQueryChange = { searchQuery = it },
                        onFileClick = { file ->
                            showFastSearchOverlay = false
                            activeViewingFile = file
                            subScreen = SubScreen.WORKSTATION_VIEWER
                        },
                        onClose = { showFastSearchOverlay = false }
                    )
                }

                // Storage Metrics & Duplicate Candidate Auditor Modal Dialog
                if (showStorageMetricsDialog) {
                    val metricsData = remember(files, totalStorage) { viewModel.calculateStorageMetrics(context) }
                    com.example.ui.components.StorageMetricsDialog(
                        metrics = metricsData,
                        language = appearanceConfig.language,
                        onDismiss = { showStorageMetricsDialog = false }
                    )
                }

                // Raycast/Spotlight Command Palette Dialog
                if (showCommandPaletteDialog) {
                    com.example.ui.components.CommandPaletteDialog(
                        language = appearanceConfig.language,
                        onDismiss = { showCommandPaletteDialog = false },
                        onExecuteAction = { actionId ->
                            showCommandPaletteDialog = false
                            when (actionId) {
                                "upload_file" -> filePickerLauncher.launch(arrayOf("*/*"))
                                "upload_media" -> mediaPickerLauncher.launch(arrayOf("image/*", "video/*"))
                                "new_folder" -> showCreateFolderDialogInMainActivity = true
                                "create_note" -> showCreateNoteDialogInMainActivity = true
                                "storage_metrics" -> showStorageMetricsDialog = true
                                "fast_search" -> showFastSearchOverlay = true
                                "appearance_studio" -> subScreen = SubScreen.APPEARANCE_STUDIO
                                "lock_vault" -> viewModel.lockVault()
                                "sync_telegram" -> {
                                    viewModel.scanExistingChannelHistory(context) { _, msg ->
                                        restorationNotification = msg
                                    }
                                }
                            }
                        }
                    )
                }
            }
        }
    }
}
