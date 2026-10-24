package com.example.features.linkmanager

import android.content.ClipData
import android.content.ClipboardManager
import android.content.Context
import android.content.Intent
import android.net.Uri
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.animation.*
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
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
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import coil.compose.AsyncImage
import coil.request.ImageRequest
import com.example.features.studio.AppLanguage
import com.example.ui.theme.CyanNeon
import com.example.ui.theme.IndigoHyper

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun UniversalLinkManagerScreen(
    coordinator: UniversalLinkCoordinator,
    initialSharedUrl: String? = null,
    language: AppLanguage = AppLanguage.EN,
    onBack: () -> Unit,
    onStreamMedia: (ResolvedLinkResource) -> Unit = {}
) {
    val context = LocalContext.current
    val isHindi = language == AppLanguage.HI

    var urlInput by remember { mutableStateOf(initialSharedUrl ?: "") }
    var filenameOverride by remember { mutableStateOf("") }
    var channelIdInput by remember { mutableStateOf("") }

    val resolvedResource by coordinator.resolvedResourceFlow.collectAsState()
    val isResolving by coordinator.isResolvingFlow.collectAsState()
    val resolveError by coordinator.resolveErrorFlow.collectAsState()
    val tasks by coordinator.tasksFlow.collectAsState()

    var showTelegramConfirmDialog by remember { mutableStateOf(false) }
    var showTechSpecsDialog by remember { mutableStateOf(false) }
    var snackbarMessage by remember { mutableStateOf<String?>(null) }

    // Storage Access Framework launcher for user-selected local destination
    val createDocLauncher = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.CreateDocument("*/*")
    ) { userUri: Uri? ->
        val resource = resolvedResource
        if (resource != null && userUri != null) {
            coordinator.startLocalDownload(
                resource = resource,
                customFilename = filenameOverride,
                userSelectedSafUri = userUri
            )
            snackbarMessage = if (isHindi) "डाउनलोड प्रारंभ हुआ!" else "Download started to chosen folder!"
        }
    }

    // Auto-resolve initial shared URL if provided
    LaunchedEffect(initialSharedUrl) {
        if (!initialSharedUrl.isNullOrBlank()) {
            urlInput = initialSharedUrl
            coordinator.resolveUrl(initialSharedUrl)
        }
    }

    val validationResult = remember(urlInput) {
        if (urlInput.isBlank()) null else UniversalLinkRegistry.validateLinkSecurity(urlInput)
    }

    Scaffold(
        topBar = {
            TopAppBar(
                title = {
                    Column {
                        Text(
                            text = if (isHindi) "यूनिवर्सल लिंक मैनेजर" else "Universal Link Manager",
                            fontWeight = FontWeight.Bold,
                            fontSize = 16.sp
                        )
                        Text(
                            text = if (isHindi) "मल्टी-प्लेटफ़ॉर्म मीडिया, डाउनलोड व क्लाउड ट्रांसफर" else "Multi-Platform Media, Download & Cloud Ingest",
                            fontSize = 11.sp,
                            color = CyanNeon
                        )
                    }
                },
                navigationIcon = {
                    IconButton(onClick = onBack, modifier = Modifier.testTag("link_manager_back_button")) {
                        Icon(Icons.AutoMirrored.Filled.ArrowBack, contentDescription = "Back")
                    }
                },
                colors = TopAppBarDefaults.topAppBarColors(containerColor = MaterialTheme.colorScheme.surface)
            )
        }
    ) { padding ->
        Box(modifier = Modifier.fillMaxSize().padding(padding)) {
            Column(
                modifier = Modifier
                    .fillMaxSize()
                    .verticalScroll(rememberScrollState())
                    .padding(16.dp),
                verticalArrangement = Arrangement.spacedBy(16.dp)
            ) {
                // Section 1: URL Input & Security Validation Card
                Card(
                    modifier = Modifier.fillMaxWidth(),
                    colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surfaceContainerHigh)
                ) {
                    Column(modifier = Modifier.padding(16.dp), verticalArrangement = Arrangement.spacedBy(12.dp)) {
                        Row(verticalAlignment = Alignment.CenterVertically) {
                            Icon(Icons.Default.Link, contentDescription = null, tint = CyanNeon)
                            Spacer(modifier = Modifier.width(8.dp))
                            Text(
                                text = if (isHindi) "लिंक दर्ज करें या शेयर करें" else "Submit or Paste Remote URL",
                                fontWeight = FontWeight.Bold,
                                fontSize = 15.sp
                            )
                        }

                        Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                            OutlinedTextField(
                                value = urlInput,
                                onValueChange = { urlInput = it },
                                label = { Text("URL (Drive, YouTube, Direct File, Web)") },
                                placeholder = { Text("https://...") },
                                modifier = Modifier.weight(1f).testTag("link_url_input"),
                                singleLine = true,
                                trailingIcon = {
                                    if (urlInput.isNotBlank()) {
                                        IconButton(onClick = { urlInput = "" }) {
                                            Icon(Icons.Default.Clear, contentDescription = "Clear")
                                        }
                                    }
                                }
                            )

                            // Quick Paste Button
                            Button(
                                onClick = {
                                    val clipboard = context.getSystemService(Context.CLIPBOARD_SERVICE) as ClipboardManager
                                    val clip = clipboard.primaryClip
                                    if (clip != null && clip.itemCount > 0) {
                                        val text = clip.getItemAt(0).text?.toString()?.trim() ?: ""
                                        if (text.isNotBlank()) {
                                            urlInput = text
                                            coordinator.resolveUrl(text)
                                        }
                                    }
                                },
                                contentPadding = PaddingValues(horizontal = 12.dp)
                            ) {
                                Icon(Icons.Default.ContentPaste, contentDescription = "Paste")
                            }
                        }

                        // Live Security & Scheme Indicator
                        validationResult?.let { (isValid, msg) ->
                            Surface(
                                color = if (isValid) MaterialTheme.colorScheme.primaryContainer else MaterialTheme.colorScheme.errorContainer,
                                shape = RoundedCornerShape(8.dp),
                                modifier = Modifier.fillMaxWidth()
                            ) {
                                Row(
                                    modifier = Modifier.padding(horizontal = 10.dp, vertical = 6.dp),
                                    verticalAlignment = Alignment.CenterVertically
                                ) {
                                    Icon(
                                        imageVector = if (isValid) Icons.Default.CheckCircle else Icons.Default.Warning,
                                        contentDescription = null,
                                        tint = if (isValid) MaterialTheme.colorScheme.primary else MaterialTheme.colorScheme.error,
                                        modifier = Modifier.size(16.dp)
                                    )
                                    Spacer(modifier = Modifier.width(6.dp))
                                    Text(
                                        text = msg,
                                        fontSize = 11.sp,
                                        color = if (isValid) MaterialTheme.colorScheme.onPrimaryContainer else MaterialTheme.colorScheme.onErrorContainer
                                    )
                                }
                            }
                        }

                        // Resolve Button
                        Button(
                            onClick = { coordinator.resolveUrl(urlInput) },
                            enabled = validationResult?.first == true && !isResolving,
                            modifier = Modifier.fillMaxWidth().testTag("resolve_link_button")
                        ) {
                            if (isResolving) {
                                CircularProgressIndicator(color = Color.White, modifier = Modifier.size(18.dp), strokeWidth = 2.dp)
                                Spacer(modifier = Modifier.width(8.dp))
                                Text(if (isHindi) "विश्लेषण हो रहा है..." else "Resolving Platform & Capabilities...")
                            } else {
                                Icon(Icons.Default.TravelExplore, contentDescription = null)
                                Spacer(modifier = Modifier.width(8.dp))
                                Text(if (isHindi) "लिंक विश्लेषण करें" else "Analyze & Resolve Capabilities")
                            }
                        }

                        // Error Banner if Resolution Failed
                        resolveError?.let { err ->
                            Surface(
                                color = MaterialTheme.colorScheme.errorContainer,
                                shape = RoundedCornerShape(8.dp),
                                modifier = Modifier.fillMaxWidth()
                            ) {
                                Row(modifier = Modifier.padding(10.dp), verticalAlignment = Alignment.CenterVertically) {
                                    Icon(Icons.Default.ErrorOutline, contentDescription = null, tint = MaterialTheme.colorScheme.error)
                                    Spacer(modifier = Modifier.width(8.dp))
                                    Text(err, color = MaterialTheme.colorScheme.onErrorContainer, fontSize = 12.sp)
                                }
                            }
                        }
                    }
                }

                // Section 2: Resolved Resource Card & Capability Engine
                resolvedResource?.let { res ->
                    Card(
                        modifier = Modifier.fillMaxWidth().testTag("resolved_resource_card"),
                        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surfaceVariant),
                        shape = RoundedCornerShape(16.dp)
                    ) {
                        Column(modifier = Modifier.padding(16.dp), verticalArrangement = Arrangement.spacedBy(12.dp)) {
                            // Header with Platform Badge & Content Type
                            Row(
                                modifier = Modifier.fillMaxWidth(),
                                horizontalArrangement = Arrangement.SpaceBetween,
                                verticalAlignment = Alignment.CenterVertically
                            ) {
                                Surface(
                                    color = CyanNeon.copy(alpha = 0.2f),
                                    shape = RoundedCornerShape(6.dp)
                                ) {
                                    Text(
                                        text = res.platform.displayName,
                                        fontWeight = FontWeight.Bold,
                                        fontSize = 11.sp,
                                        color = CyanNeon,
                                        modifier = Modifier.padding(horizontal = 8.dp, vertical = 4.dp)
                                    )
                                }

                                Surface(
                                    color = MaterialTheme.colorScheme.surfaceContainerHighest,
                                    shape = RoundedCornerShape(6.dp)
                                ) {
                                    Text(
                                        text = res.contentType.label,
                                        fontSize = 10.sp,
                                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                                        modifier = Modifier.padding(horizontal = 6.dp, vertical = 3.dp)
                                    )
                                }
                            }

                            // Title & Description
                            Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(12.dp)) {
                                if (!res.thumbnailUrl.isNullOrBlank()) {
                                    AsyncImage(
                                        model = ImageRequest.Builder(context)
                                            .data(res.thumbnailUrl)
                                            .crossfade(true)
                                            .build(),
                                        contentDescription = "Thumbnail",
                                        modifier = Modifier
                                            .size(80.dp)
                                            .clip(RoundedCornerShape(8.dp))
                                            .background(Color.Black),
                                        contentScale = ContentScale.Crop
                                    )
                                }

                                Column(modifier = Modifier.weight(1f)) {
                                    Text(res.title, fontWeight = FontWeight.Bold, fontSize = 15.sp, maxLines = 2)
                                    Spacer(modifier = Modifier.height(4.dp))
                                    res.description?.let { desc ->
                                        Text(desc, fontSize = 12.sp, color = MaterialTheme.colorScheme.onSurfaceVariant, maxLines = 2)
                                    }
                                    Spacer(modifier = Modifier.height(4.dp))
                                    res.fileSizeBytes?.let { size ->
                                        Text(
                                            "Size: ${formatBytes(size)} • MIME: ${res.mimeType}",
                                            fontSize = 11.sp,
                                            color = MaterialTheme.colorScheme.outline
                                        )
                                    }
                                }
                            }

                            // Capability & Restriction Notice Banner
                            res.capabilities.restrictionNotice?.let { notice ->
                                Surface(
                                    color = MaterialTheme.colorScheme.tertiaryContainer,
                                    shape = RoundedCornerShape(8.dp),
                                    modifier = Modifier.fillMaxWidth()
                                ) {
                                    Row(modifier = Modifier.padding(10.dp), verticalAlignment = Alignment.Top) {
                                        Icon(Icons.Default.Info, contentDescription = null, tint = MaterialTheme.colorScheme.onTertiaryContainer, modifier = Modifier.size(16.dp))
                                        Spacer(modifier = Modifier.width(8.dp))
                                        Text(notice, fontSize = 11.sp, color = MaterialTheme.colorScheme.onTertiaryContainer)
                                    }
                                }
                            }

                            HorizontalDivider(color = MaterialTheme.colorScheme.outlineVariant.copy(alpha = 0.5f))

                            // Action Buttons Grid
                            Text("Available Operations:", fontWeight = FontWeight.SemiBold, fontSize = 12.sp)

                            Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                                // Action A: Stream / Preview
                                Button(
                                    onClick = { onStreamMedia(res) },
                                    enabled = res.capabilities.canStream,
                                    modifier = Modifier.weight(1f).testTag("action_stream_button"),
                                    colors = ButtonDefaults.buttonColors(containerColor = CyanNeon, contentColor = Color.Black)
                                ) {
                                    Icon(Icons.Default.PlayArrow, contentDescription = null, modifier = Modifier.size(18.dp))
                                    Spacer(modifier = Modifier.width(4.dp))
                                    Text("Stream", fontSize = 12.sp)
                                }

                                // Action B: Local Storage Download (SAF)
                                Button(
                                    onClick = {
                                        createDocLauncher.launch(res.suggestedFilename)
                                    },
                                    enabled = res.capabilities.canDownloadLocally,
                                    modifier = Modifier.weight(1f).testTag("action_download_button")
                                ) {
                                    Icon(Icons.Default.Download, contentDescription = null, modifier = Modifier.size(18.dp))
                                    Spacer(modifier = Modifier.width(4.dp))
                                    Text("Save File", fontSize = 12.sp)
                                }
                            }

                            Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                                // Action C: Telegram Cloud Upload
                                OutlinedButton(
                                    onClick = { showTelegramConfirmDialog = true },
                                    enabled = res.capabilities.canUploadToTelegram,
                                    modifier = Modifier.weight(1f).testTag("action_telegram_button")
                                ) {
                                    Icon(Icons.Default.CloudUpload, contentDescription = null, modifier = Modifier.size(18.dp))
                                    Spacer(modifier = Modifier.width(4.dp))
                                    Text("To Telegram", fontSize = 12.sp)
                                }

                                // Action D: Open in Official App / Browser
                                OutlinedButton(
                                    onClick = {
                                        try {
                                            val viewIntent = Intent(Intent.ACTION_VIEW, Uri.parse(res.rawUrl))
                                            context.startActivity(viewIntent)
                                        } catch (e: Exception) {
                                            snackbarMessage = "No app available to handle this link"
                                        }
                                    },
                                    enabled = res.capabilities.canOpenInApp,
                                    modifier = Modifier.weight(1f).testTag("action_open_app_button")
                                ) {
                                    Icon(Icons.Default.OpenInNew, contentDescription = null, modifier = Modifier.size(18.dp))
                                    Spacer(modifier = Modifier.width(4.dp))
                                    Text("Open App", fontSize = 12.sp)
                                }
                            }

                            // Technical Specs Action
                            TextButton(
                                onClick = { showTechSpecsDialog = true },
                                modifier = Modifier.align(Alignment.End)
                            ) {
                                Icon(Icons.Default.Code, contentDescription = null, modifier = Modifier.size(14.dp))
                                Spacer(modifier = Modifier.width(4.dp))
                                Text("Technical Capability Specs", fontSize = 11.sp)
                            }
                        }
                    }
                }

                // Section 3: Active & Recent Transfer Pipeline Dashboard
                Text(
                    text = if (isHindi) "सक्रिय व हालिया ट्रांसफ़र" else "Transfer Execution Dashboard",
                    fontWeight = FontWeight.Bold,
                    fontSize = 16.sp
                )

                if (tasks.isEmpty()) {
                    Surface(
                        color = MaterialTheme.colorScheme.surfaceContainerLow,
                        shape = RoundedCornerShape(12.dp),
                        modifier = Modifier.fillMaxWidth()
                    ) {
                        Column(
                            modifier = Modifier.padding(24.dp),
                            horizontalAlignment = Alignment.CenterHorizontally
                        ) {
                            Icon(Icons.Default.CloudQueue, contentDescription = null, tint = MaterialTheme.colorScheme.outline, modifier = Modifier.size(40.dp))
                            Spacer(modifier = Modifier.height(8.dp))
                            Text(
                                text = if (isHindi) "कोई सक्रिय ट्रांसफ़र नहीं है" else "No active or recent link transfers",
                                fontSize = 13.sp,
                                color = MaterialTheme.colorScheme.outline
                            )
                        }
                    }
                } else {
                    Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                        tasks.forEach { task ->
                            TransferTaskCard(
                                task = task,
                                onCancel = { coordinator.cancelTask(task.taskId) },
                                onRemove = { coordinator.removeTask(task.taskId) }
                            )
                        }
                    }
                }
            }

            // Snackbar
            snackbarMessage?.let { msg ->
                Snackbar(
                    modifier = Modifier.align(Alignment.BottomCenter).padding(16.dp),
                    action = {
                        TextButton(onClick = { snackbarMessage = null }) {
                            Text("OK", color = CyanNeon)
                        }
                    }
                ) {
                    Text(msg)
                }
            }
        }
    }

    // Modal Dialog: Telegram Cloud Transfer Confirmation
    if (showTelegramConfirmDialog && resolvedResource != null) {
        val res = resolvedResource!!
        AlertDialog(
            onDismissRequest = { showTelegramConfirmDialog = false },
            title = { Text(if (isHindi) "टेलीग्राम क्लाउड ट्रांसफ़र" else "Telegram Cloud Transfer") },
            text = {
                Column(verticalArrangement = Arrangement.spacedBy(10.dp)) {
                    Text("Transfer '${res.suggestedFilename}' directly into your Telegram supergroup.", fontSize = 13.sp)
                    OutlinedTextField(
                        value = channelIdInput,
                        onValueChange = { channelIdInput = it },
                        label = { Text("Destination Channel ID (Optional override)") },
                        placeholder = { Text("Default: Saved channel") },
                        modifier = Modifier.fillMaxWidth()
                    )
                    OutlinedTextField(
                        value = filenameOverride,
                        onValueChange = { filenameOverride = it },
                        label = { Text("Custom Filename Override") },
                        placeholder = { Text(res.suggestedFilename) },
                        modifier = Modifier.fillMaxWidth()
                    )
                }
            },
            confirmButton = {
                Button(
                    onClick = {
                        showTelegramConfirmDialog = false
                        coordinator.startTelegramUpload(
                            resource = res,
                            customFilename = filenameOverride,
                            channelIdInput = channelIdInput
                        )
                        snackbarMessage = "Queued cloud transfer to Telegram!"
                    }
                ) {
                    Text("Confirm & Upload")
                }
            },
            dismissButton = {
                TextButton(onClick = { showTelegramConfirmDialog = false }) {
                    Text("Cancel")
                }
            }
        )
    }

    // Modal Dialog: Technical Capability Specs
    if (showTechSpecsDialog && resolvedResource != null) {
        val res = resolvedResource!!
        AlertDialog(
            onDismissRequest = { showTechSpecsDialog = false },
            title = { Text("Technical Capability Specs") },
            text = {
                Column(
                    modifier = Modifier.verticalScroll(rememberScrollState()),
                    verticalArrangement = Arrangement.spacedBy(6.dp)
                ) {
                    SpecRow("Normalized URL", res.normalizedUrl)
                    SpecRow("Platform Adapter", res.platform.displayName)
                    SpecRow("Content Classification", res.contentType.label)
                    SpecRow("Can Stream Directly", res.capabilities.canStream.toString())
                    SpecRow("Can Download Locally", res.capabilities.canDownloadLocally.toString())
                    SpecRow("Can Upload to Telegram", res.capabilities.canUploadToTelegram.toString())
                    SpecRow("DRM / Protected", res.capabilities.isRestrictedOrDrm.toString())
                    res.metadataMap.forEach { (k, v) ->
                        SpecRow(k, v)
                    }
                }
            },
            confirmButton = {
                Button(onClick = { showTechSpecsDialog = false }) {
                    Text("Close")
                }
            }
        )
    }
}

@Composable
private fun SpecRow(label: String, value: String) {
    Column(modifier = Modifier.padding(vertical = 2.dp)) {
        Text(label, fontWeight = FontWeight.Bold, fontSize = 11.sp, color = CyanNeon)
        Text(value, fontSize = 11.sp, fontFamily = FontFamily.Monospace, color = MaterialTheme.colorScheme.onSurfaceVariant)
    }
}

@Composable
private fun TransferTaskCard(
    task: LinkTransferTask,
    onCancel: () -> Unit,
    onRemove: () -> Unit
) {
    Card(
        modifier = Modifier.fillMaxWidth(),
        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surfaceContainer)
    ) {
        Column(modifier = Modifier.padding(12.dp), verticalArrangement = Arrangement.spacedBy(6.dp)) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Text(
                    text = task.resource.title,
                    fontWeight = FontWeight.Bold,
                    fontSize = 13.sp,
                    maxLines = 1,
                    modifier = Modifier.weight(1f)
                )

                Surface(
                    color = when (task.stage) {
                        TransferStage.COMPLETED -> Color(0xFF10B981).copy(alpha = 0.2f)
                        TransferStage.FAILED, TransferStage.CANCELLED -> MaterialTheme.colorScheme.errorContainer
                        else -> CyanNeon.copy(alpha = 0.2f)
                    },
                    shape = RoundedCornerShape(4.dp)
                ) {
                    Text(
                        text = task.stage.userLabel,
                        fontSize = 10.sp,
                        fontWeight = FontWeight.Bold,
                        color = when (task.stage) {
                            TransferStage.COMPLETED -> Color(0xFF10B981)
                            TransferStage.FAILED, TransferStage.CANCELLED -> MaterialTheme.colorScheme.error
                            else -> CyanNeon
                        },
                        modifier = Modifier.padding(horizontal = 6.dp, vertical = 2.dp)
                    )
                }
            }

            Text(
                text = "${task.destination.label} • ${task.destinationDetails}",
                fontSize = 11.sp,
                color = MaterialTheme.colorScheme.outline
            )

            // Progress Bar
            val totalProgress = if (task.destination == TransferDestination.TELEGRAM_CLOUD) {
                (task.downloadProgressPercent + task.uploadProgressPercent) / 100f
            } else {
                task.downloadProgressPercent / 100f
            }

            LinearProgressIndicator(
                progress = { totalProgress.coerceIn(0f, 1f) },
                modifier = Modifier.fillMaxWidth().height(6.dp).clip(RoundedCornerShape(3.dp)),
                color = CyanNeon,
                trackColor = MaterialTheme.colorScheme.surfaceContainerHighest
            )

            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Text(
                    text = "${formatBytes(task.transferredBytes)} / ${if (task.totalBytes > 0) formatBytes(task.totalBytes) else "..."}",
                    fontSize = 10.sp,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )

                Row {
                    if (task.stage in listOf(TransferStage.QUEUED, TransferStage.DOWNLOADING, TransferStage.UPLOADING)) {
                        TextButton(onClick = onCancel, contentPadding = PaddingValues(horizontal = 8.dp)) {
                            Text("Cancel", fontSize = 11.sp, color = MaterialTheme.colorScheme.error)
                        }
                    } else {
                        IconButton(onClick = onRemove, modifier = Modifier.size(24.dp)) {
                            Icon(Icons.Default.Close, contentDescription = "Remove", modifier = Modifier.size(16.dp))
                        }
                    }
                }
            }

            task.errorMessage?.let { err ->
                Text(err, color = MaterialTheme.colorScheme.error, fontSize = 11.sp)
            }
        }
    }
}

private fun formatBytes(bytes: Long): String {
    return when {
        bytes >= 1024 * 1024 * 1024 -> String.format("%.2f GB", bytes.toDouble() / (1024 * 1024 * 1024))
        bytes >= 1024 * 1024 -> String.format("%.2f MB", bytes.toDouble() / (1024 * 1024))
        bytes >= 1024 -> String.format("%.1f KB", bytes.toDouble() / 1024)
        else -> "$bytes B"
    }
}
