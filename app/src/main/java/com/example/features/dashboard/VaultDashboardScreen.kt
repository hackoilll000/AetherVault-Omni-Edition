package com.example.features.dashboard

import android.content.Context
import androidx.compose.animation.*
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.foundation.lazy.grid.GridCells
import androidx.compose.foundation.lazy.grid.LazyVerticalGrid
import androidx.compose.foundation.lazy.grid.items
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.core.database.BotSwarmEntity
import com.example.core.database.TransferTask
import com.example.core.database.VaultFile
import com.example.core.sync.CameraSyncStatus
import com.example.features.studio.AppLanguage
import com.example.ui.theme.CyanNeon
import com.example.ui.theme.IndigoHyper
import java.text.SimpleDateFormat
import java.util.*

enum class DashboardCategoryFilter {
    ALL,
    IMAGES,
    VIDEOS,
    DOCUMENTS,
    AUDIO,
    ARCHIVES_CODE,
    STARRED
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun VaultDashboardScreen(
    files: List<VaultFile>,
    trashedFiles: List<VaultFile>,
    transfers: List<TransferTask>,
    botSwarm: List<BotSwarmEntity>,
    totalStorageBytes: Long,
    channelId: Long,
    cameraSyncStatus: CameraSyncStatus,
    language: AppLanguage,
    onUploadFiles: () -> Unit,
    onUploadMedia: () -> Unit,
    onTriggerCameraSync: () -> Unit,
    onCreateNote: (String, String) -> Unit,
    onFileClick: (VaultFile) -> Unit,
    onDownloadToDevice: (VaultFile) -> Unit,
    onSaveToGallery: (VaultFile) -> Unit,
    onShareDecrypted: (VaultFile) -> Unit,
    onShareTicket: (VaultFile) -> Unit,
    onVerifyTelegram: (VaultFile) -> Unit,
    onToggleStar: (VaultFile) -> Unit,
    onMoveToTrash: (VaultFile) -> Unit,
    onRestoreFromTrash: (VaultFile) -> Unit,
    onPermanentDelete: (VaultFile) -> Unit,
    onEmptyTrash: () -> Unit,
    onSyncChannel: () -> Unit = {}
) {
    val isHindi = language == AppLanguage.HI
    val context = LocalContext.current

    var searchQuery by remember { mutableStateOf("") }
    var selectedCategory by remember { mutableStateOf(DashboardCategoryFilter.ALL) }
    var showTrashDialog by remember { mutableStateOf(false) }
    var showCreateNoteDialog by remember { mutableStateOf(false) }
    var newNoteTitle by remember { mutableStateOf("") }
    var newNoteContent by remember { mutableStateOf("") }

    // Filter files according to search and category
    val filteredFiles = remember(files, searchQuery, selectedCategory) {
        files.filter { file ->
            val matchesSearch = searchQuery.isBlank() ||
                    file.name.contains(searchQuery, ignoreCase = true) ||
                    file.extension.contains(searchQuery, ignoreCase = true)

            val matchesCategory = when (selectedCategory) {
                DashboardCategoryFilter.ALL -> true
                DashboardCategoryFilter.IMAGES -> file.mimeType.startsWith("image/")
                DashboardCategoryFilter.VIDEOS -> file.mimeType.startsWith("video/")
                DashboardCategoryFilter.DOCUMENTS -> file.mimeType.startsWith("application/pdf") ||
                        file.mimeType.contains("document") || file.mimeType.contains("text") ||
                        file.extension in listOf("pdf", "doc", "docx", "txt", "md", "csv", "xlsx")
                DashboardCategoryFilter.AUDIO -> file.mimeType.startsWith("audio/") || file.extension in listOf("mp3", "m4a", "wav", "flac", "ogg")
                DashboardCategoryFilter.ARCHIVES_CODE -> file.extension in listOf("zip", "rar", "7z", "tar", "gz", "kt", "java", "py", "js", "html", "css", "apk")
                DashboardCategoryFilter.STARRED -> file.isStarred
            }

            matchesSearch && matchesCategory
        }
    }

    Scaffold(
        topBar = {
            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .background(MaterialTheme.colorScheme.surface)
            ) {
                // Header with App Title and Quick Badges
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(horizontal = 16.dp, vertical = 10.dp),
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.SpaceBetween
                ) {
                    Column {
                        Text(
                            text = if (isHindi) "क्लाउड वॉल्ट डैशबोर्ड" else "Cloud Vault Dashboard",
                            style = MaterialTheme.typography.titleLarge,
                            fontWeight = FontWeight.ExtraBold,
                            color = MaterialTheme.colorScheme.onSurface
                        )
                        Row(verticalAlignment = Alignment.CenterVertically) {
                            Box(
                                modifier = Modifier
                                    .size(7.dp)
                                    .clip(CircleShape)
                                    .background(Color(0xFF10B981))
                            )
                            Spacer(modifier = Modifier.width(6.dp))
                            Text(
                                text = if (isHindi) "टेलीग्राम प्राइवेट सर्वर लाइव" else "Telegram Cloud Live",
                                fontSize = 11.sp,
                                color = MaterialTheme.colorScheme.primary,
                                fontWeight = FontWeight.SemiBold
                            )
                        }
                    }

                    Row(verticalAlignment = Alignment.CenterVertically) {
                        // Sync Existing Server Files & Text Button
                        IconButton(
                            onClick = onSyncChannel,
                            modifier = Modifier.testTag("dashboard_sync_channel_button")
                        ) {
                            Icon(
                                imageVector = Icons.Default.Sync,
                                contentDescription = "Sync Channel Content",
                                tint = CyanNeon
                            )
                        }

                        // Trash Bin Action with Badge
                        IconButton(
                            onClick = { showTrashDialog = true },
                            modifier = Modifier.testTag("dashboard_trash_button")
                        ) {
                            BadgedBox(
                                badge = {
                                    if (trashedFiles.isNotEmpty()) {
                                        Badge(containerColor = MaterialTheme.colorScheme.error) {
                                            Text("${trashedFiles.size}")
                                        }
                                    }
                                }
                            ) {
                                Icon(
                                    imageVector = Icons.Default.DeleteOutline,
                                    contentDescription = "Trash Bin",
                                    tint = MaterialTheme.colorScheme.onSurfaceVariant
                                )
                            }
                        }
                    }
                }

                // Search Bar
                OutlinedTextField(
                    value = searchQuery,
                    onValueChange = { searchQuery = it },
                    placeholder = {
                        Text(
                            if (isHindi) "फ़ाइल का नाम या एक्सटेंशन खोजें..." else "Search files by name or extension...",
                            fontSize = 13.sp
                        )
                    },
                    leadingIcon = {
                        Icon(Icons.Default.Search, contentDescription = null, tint = CyanNeon)
                    },
                    trailingIcon = {
                        if (searchQuery.isNotEmpty()) {
                            IconButton(onClick = { searchQuery = "" }) {
                                Icon(Icons.Default.Close, contentDescription = "Clear", modifier = Modifier.size(18.dp))
                            }
                        }
                    },
                    singleLine = true,
                    shape = RoundedCornerShape(14.dp),
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(horizontal = 16.dp, vertical = 4.dp)
                        .testTag("dashboard_search_input")
                )

                // Category Filter Pills
                LazyRow(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(horizontal = 16.dp, vertical = 6.dp),
                    horizontalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    items(DashboardCategoryFilter.values()) { cat ->
                        val isSelected = selectedCategory == cat
                        val label = when (cat) {
                            DashboardCategoryFilter.ALL -> if (isHindi) "सभी फ़ाइलें" else "All Files"
                            DashboardCategoryFilter.IMAGES -> if (isHindi) "फ़ोटो" else "Images"
                            DashboardCategoryFilter.VIDEOS -> if (isHindi) "वीडियो" else "Videos"
                            DashboardCategoryFilter.DOCUMENTS -> if (isHindi) "दस्तावेज़" else "Docs"
                            DashboardCategoryFilter.AUDIO -> if (isHindi) "ऑडियो" else "Audio"
                            DashboardCategoryFilter.ARCHIVES_CODE -> if (isHindi) "ज़िप / कोड" else "Archives & Code"
                            DashboardCategoryFilter.STARRED -> if (isHindi) "तारांकित" else "Starred"
                        }

                        FilterChip(
                            selected = isSelected,
                            onClick = { selectedCategory = cat },
                            label = { Text(label, fontSize = 12.sp) },
                            leadingIcon = if (isSelected) {
                                { Icon(Icons.Default.Check, contentDescription = null, modifier = Modifier.size(14.dp)) }
                            } else null,
                            colors = FilterChipDefaults.filterChipColors(
                                selectedContainerColor = CyanNeon.copy(alpha = 0.2f),
                                selectedLabelColor = CyanNeon
                            )
                        )
                    }
                }
            }
        }
    ) { innerPadding ->
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(innerPadding)
        ) {
            // Hero Telemetry & Cloud Storage Overview Card
            Card(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(horizontal = 16.dp, vertical = 8.dp),
                shape = RoundedCornerShape(16.dp),
                colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.6f))
            ) {
                Column(modifier = Modifier.padding(14.dp)) {
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Row(verticalAlignment = Alignment.CenterVertically) {
                            Surface(
                                shape = CircleShape,
                                color = IndigoHyper.copy(alpha = 0.25f),
                                modifier = Modifier.size(36.dp)
                            ) {
                                Box(contentAlignment = Alignment.Center) {
                                    Icon(Icons.Default.CloudQueue, contentDescription = null, tint = CyanNeon, modifier = Modifier.size(20.dp))
                                }
                            }
                            Spacer(modifier = Modifier.width(10.dp))
                            Column {
                                Text(
                                    text = formatStorageSize(totalStorageBytes),
                                    fontSize = 18.sp,
                                    fontWeight = FontWeight.ExtraBold,
                                    color = MaterialTheme.colorScheme.onSurface
                                )
                                Text(
                                    text = if (isHindi) "कुल स्टोरेज (असीमित टेलीग्राम क्लाउड)" else "Total Used (Unlimited Cloud)",
                                    fontSize = 11.sp,
                                    color = MaterialTheme.colorScheme.onSurfaceVariant
                                )
                            }
                        }

                        // Files Count & Bots badge
                        Column(horizontalAlignment = Alignment.End) {
                            Surface(
                                shape = RoundedCornerShape(8.dp),
                                color = CyanNeon.copy(alpha = 0.15f)
                            ) {
                                Text(
                                    text = "${files.size} ${if (isHindi) "फ़ाइलें" else "Files"}",
                                    fontSize = 11.sp,
                                    fontWeight = FontWeight.Bold,
                                    color = CyanNeon,
                                    modifier = Modifier.padding(horizontal = 8.dp, vertical = 3.dp)
                                )
                            }
                            Spacer(modifier = Modifier.height(4.dp))
                            Text(
                                text = "${botSwarm.size.coerceAtLeast(1)} Bot Swarm",
                                fontSize = 10.sp,
                                color = MaterialTheme.colorScheme.onSurfaceVariant
                            )
                        }
                    }

                    Spacer(modifier = Modifier.height(12.dp))

                    // Quick Action Row inside Hero
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.spacedBy(8.dp)
                    ) {
                        Button(
                            onClick = onUploadFiles,
                            modifier = Modifier
                                .weight(1f)
                                .testTag("dashboard_upload_files_button"),
                            contentPadding = PaddingValues(horizontal = 8.dp, vertical = 8.dp),
                            shape = RoundedCornerShape(10.dp)
                        ) {
                            Icon(Icons.Default.UploadFile, contentDescription = null, modifier = Modifier.size(16.dp))
                            Spacer(modifier = Modifier.width(4.dp))
                            Text(if (isHindi) "फ़ाइल अपलोड" else "Upload File", fontSize = 11.sp)
                        }

                        FilledTonalButton(
                            onClick = onUploadMedia,
                            modifier = Modifier
                                .weight(1f)
                                .testTag("dashboard_upload_media_button"),
                            contentPadding = PaddingValues(horizontal = 8.dp, vertical = 8.dp),
                            shape = RoundedCornerShape(10.dp)
                        ) {
                            Icon(Icons.Default.AddPhotoAlternate, contentDescription = null, modifier = Modifier.size(16.dp))
                            Spacer(modifier = Modifier.width(4.dp))
                            Text(if (isHindi) "फ़ोटो/वीडियो" else "Media", fontSize = 11.sp)
                        }

                        OutlinedButton(
                            onClick = onTriggerCameraSync,
                            modifier = Modifier
                                .weight(1f)
                                .testTag("dashboard_sync_camera_button"),
                            contentPadding = PaddingValues(horizontal = 8.dp, vertical = 8.dp),
                            shape = RoundedCornerShape(10.dp)
                        ) {
                            Icon(Icons.Default.Sync, contentDescription = null, modifier = Modifier.size(16.dp))
                            Spacer(modifier = Modifier.width(4.dp))
                            Text(if (isHindi) "कैमरा बैकअप" else "Auto-Sync", fontSize = 11.sp)
                        }
                    }

                    // Camera Roll Sync Status Banner if active or recently triggered
                    if (cameraSyncStatus.isSyncing || cameraSyncStatus.message.startsWith("✓")) {
                        Spacer(modifier = Modifier.height(8.dp))
                        Surface(
                            shape = RoundedCornerShape(8.dp),
                            color = MaterialTheme.colorScheme.surface,
                            modifier = Modifier.fillMaxWidth()
                        ) {
                            Row(
                                modifier = Modifier.padding(horizontal = 10.dp, vertical = 6.dp),
                                verticalAlignment = Alignment.CenterVertically
                            ) {
                                if (cameraSyncStatus.isSyncing) {
                                    CircularProgressIndicator(modifier = Modifier.size(14.dp), strokeWidth = 2.dp, color = CyanNeon)
                                    Spacer(modifier = Modifier.width(8.dp))
                                } else {
                                    Icon(Icons.Default.CheckCircle, contentDescription = null, tint = Color(0xFF10B981), modifier = Modifier.size(14.dp))
                                    Spacer(modifier = Modifier.width(8.dp))
                                }
                                Text(
                                    text = cameraSyncStatus.message,
                                    fontSize = 11.sp,
                                    color = MaterialTheme.colorScheme.onSurface,
                                    maxLines = 1,
                                    overflow = TextOverflow.Ellipsis
                                )
                            }
                        }
                    }
                }
            }

            // Active Transfer / Download Progress HUD if any
            val activeTransfers = transfers.filter { it.status == "ACTIVE" }
            if (activeTransfers.isNotEmpty()) {
                val currentTask = activeTransfers.first()
                val isDownload = currentTask.direction == "DOWNLOAD"
                Card(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(horizontal = 16.dp, vertical = 4.dp),
                    shape = RoundedCornerShape(12.dp),
                    colors = CardDefaults.cardColors(containerColor = CyanNeon.copy(alpha = 0.12f)),
                    border = androidx.compose.foundation.BorderStroke(1.dp, CyanNeon.copy(alpha = 0.4f))
                ) {
                    Column(modifier = Modifier.padding(12.dp)) {
                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.SpaceBetween,
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Row(verticalAlignment = Alignment.CenterVertically) {
                                Icon(
                                    imageVector = if (isDownload) Icons.Default.Download else Icons.Default.Upload,
                                    contentDescription = null,
                                    tint = CyanNeon,
                                    modifier = Modifier.size(18.dp)
                                )
                                Spacer(modifier = Modifier.width(8.dp))
                                Text(
                                    text = if (isDownload) "Restoring & Decrypting: ${currentTask.fileName}" else "Encrypting & Uploading: ${currentTask.fileName}",
                                    fontWeight = FontWeight.Bold,
                                    fontSize = 12.sp,
                                    maxLines = 1,
                                    overflow = TextOverflow.Ellipsis
                                )
                            }
                            Text(
                                text = "${currentTask.speedBps / 1024} KB/s",
                                fontFamily = FontFamily.Monospace,
                                fontSize = 11.sp,
                                color = CyanNeon
                            )
                        }
                        Spacer(modifier = Modifier.height(6.dp))
                        val progress = if (currentTask.totalBytes > 0) {
                            currentTask.bytesTransferred.toFloat() / currentTask.totalBytes.toFloat()
                        } else {
                            0f
                        }
                        LinearProgressIndicator(
                            progress = { progress.coerceIn(0f, 1f) },
                            modifier = Modifier.fillMaxWidth().height(6.dp).clip(CircleShape),
                            color = CyanNeon,
                            trackColor = MaterialTheme.colorScheme.surfaceVariant
                        )
                    }
                }
            }

            // Files Grid View
            if (filteredFiles.isEmpty()) {
                Box(
                    modifier = Modifier
                        .fillMaxSize()
                        .padding(32.dp),
                    contentAlignment = Alignment.Center
                ) {
                    Column(horizontalAlignment = Alignment.CenterHorizontally) {
                        Surface(
                            shape = CircleShape,
                            color = MaterialTheme.colorScheme.surfaceVariant,
                            modifier = Modifier.size(72.dp)
                        ) {
                            Box(contentAlignment = Alignment.Center) {
                                Icon(
                                    imageVector = Icons.Default.CloudQueue,
                                    contentDescription = null,
                                    tint = CyanNeon,
                                    modifier = Modifier.size(36.dp)
                                )
                            }
                        }
                        Spacer(modifier = Modifier.height(16.dp))
                        Text(
                            text = if (isHindi) "कोई फ़ाइल नहीं मिली" else "No Files in Telegram Cloud",
                            fontWeight = FontWeight.Bold,
                            fontSize = 16.sp
                        )
                        Spacer(modifier = Modifier.height(6.dp))
                        Text(
                            text = if (isHindi) "ऊपर दिए गए 'फ़ाइल अपलोड' या 'फ़ोटो/वीडियो' बटन से अपनी फ़ाइलें प्राइवेट क्लाउड में जोड़ें।"
                            else "Tap 'Upload File' or 'Media' above to encrypt & store your first files in Telegram Cloud.",
                            fontSize = 12.sp,
                            color = MaterialTheme.colorScheme.onSurfaceVariant,
                            textAlign = androidx.compose.ui.text.style.TextAlign.Center
                        )
                    }
                }
            } else {
                LazyVerticalGrid(
                    columns = GridCells.Adaptive(minSize = 160.dp),
                    contentPadding = PaddingValues(16.dp),
                    horizontalArrangement = Arrangement.spacedBy(12.dp),
                    verticalArrangement = Arrangement.spacedBy(12.dp),
                    modifier = Modifier.fillMaxSize()
                ) {
                    items(filteredFiles, key = { it.id }) { file ->
                        DashboardFileGridCard(
                            file = file,
                            isHindi = isHindi,
                            onFileClick = { onFileClick(file) },
                            onDownloadToDevice = { onDownloadToDevice(file) },
                            onSaveToGallery = { onSaveToGallery(file) },
                            onShareDecrypted = { onShareDecrypted(file) },
                            onShareTicket = { onShareTicket(file) },
                            onVerifyTelegram = { onVerifyTelegram(file) },
                            onToggleStar = { onToggleStar(file) },
                            onMoveToTrash = { onMoveToTrash(file) }
                        )
                    }
                }
            }
        }
    }

    // Secure Trash Bin Modal
    if (showTrashDialog) {
        AlertDialog(
            onDismissRequest = { showTrashDialog = false },
            title = {
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Icon(Icons.Default.Delete, contentDescription = null, tint = MaterialTheme.colorScheme.error)
                    Spacer(modifier = Modifier.width(8.dp))
                    Text(if (isHindi) "सुरक्षित रीसायकल बिन (30-दिन ऑटो-हटाना)" else "Secure Trash Bin (30-Day Auto-Purge)")
                }
            },
            text = {
                Column(modifier = Modifier.heightIn(max = 350.dp)) {
                    Text(
                        text = if (isHindi) "हटाए गए फ़ाइलें 30 दिनों तक सुरक्षित रहती हैं। आप 1-टैप में रीस्टोर कर सकते हैं:"
                        else "Deleted files are securely held here for 30 days before permanent zero-knowledge purge:",
                        fontSize = 12.sp,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                    Spacer(modifier = Modifier.height(10.dp))

                    if (trashedFiles.isEmpty()) {
                        Text(
                            text = if (isHindi) "रीसायकल बिन खाली है।" else "Trash Bin is completely empty.",
                            fontSize = 13.sp,
                            color = MaterialTheme.colorScheme.onSurfaceVariant,
                            modifier = Modifier.padding(vertical = 20.dp)
                        )
                    } else {
                        androidx.compose.foundation.lazy.LazyColumn(
                            verticalArrangement = Arrangement.spacedBy(8.dp)
                        ) {
                            items(trashedFiles) { trashFile ->
                                Surface(
                                    shape = RoundedCornerShape(8.dp),
                                    color = MaterialTheme.colorScheme.surfaceVariant,
                                    modifier = Modifier.fillMaxWidth()
                                ) {
                                    Row(
                                        modifier = Modifier.padding(10.dp),
                                        verticalAlignment = Alignment.CenterVertically,
                                        horizontalArrangement = Arrangement.SpaceBetween
                                    ) {
                                        Column(modifier = Modifier.weight(1f)) {
                                            Text(trashFile.name, fontWeight = FontWeight.Bold, fontSize = 12.sp, maxLines = 1, overflow = TextOverflow.Ellipsis)
                                            Text(formatStorageSize(trashFile.totalSizeBytes), fontSize = 10.sp, color = MaterialTheme.colorScheme.onSurfaceVariant)
                                        }
                                        Row {
                                            IconButton(onClick = { onRestoreFromTrash(trashFile) }) {
                                                Icon(Icons.Default.Restore, contentDescription = "Restore", tint = CyanNeon, modifier = Modifier.size(20.dp))
                                            }
                                            IconButton(onClick = { onPermanentDelete(trashFile) }) {
                                                Icon(Icons.Default.DeleteForever, contentDescription = "Delete Forever", tint = MaterialTheme.colorScheme.error, modifier = Modifier.size(20.dp))
                                            }
                                        }
                                    }
                                }
                            }
                        }
                    }
                }
            },
            confirmButton = {
                if (trashedFiles.isNotEmpty()) {
                    Button(
                        onClick = {
                            onEmptyTrash()
                            showTrashDialog = false
                        },
                        colors = ButtonDefaults.buttonColors(containerColor = MaterialTheme.colorScheme.error)
                    ) {
                        Text(if (isHindi) "पूरा बिन खाली करें" else "Empty Trash Bin")
                    }
                }
            },
            dismissButton = {
                TextButton(onClick = { showTrashDialog = false }) {
                    Text(if (isHindi) "बंद करें" else "Close")
                }
            }
        )
    }
}

@Composable
fun DashboardFileGridCard(
    file: VaultFile,
    isHindi: Boolean,
    onFileClick: () -> Unit,
    onDownloadToDevice: () -> Unit,
    onSaveToGallery: () -> Unit,
    onShareDecrypted: () -> Unit,
    onShareTicket: () -> Unit,
    onVerifyTelegram: () -> Unit,
    onToggleStar: () -> Unit,
    onMoveToTrash: () -> Unit
) {
    var showMenu by remember { mutableStateOf(false) }
    val isMedia = file.mimeType.startsWith("image/") || file.mimeType.startsWith("video/")

    Card(
        modifier = Modifier
            .fillMaxWidth()
            .clickable { onFileClick() }
            .testTag("file_card_${file.id}"),
        shape = RoundedCornerShape(14.dp),
        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.5f)),
        border = androidx.compose.foundation.BorderStroke(0.5.dp, MaterialTheme.colorScheme.outline.copy(alpha = 0.2f))
    ) {
        Column(modifier = Modifier.fillMaxWidth()) {
            // Preview / Icon Container
            Box(
                modifier = Modifier
                    .fillMaxWidth()
                    .height(110.dp)
                    .background(
                        Brush.verticalGradient(
                            listOf(
                                MaterialTheme.colorScheme.surfaceVariant,
                                MaterialTheme.colorScheme.surface
                            )
                        )
                    ),
                contentAlignment = Alignment.Center
            ) {
                val icon = when {
                    file.mimeType.startsWith("image/") -> Icons.Default.Image
                    file.mimeType.startsWith("video/") -> Icons.Default.VideoLibrary
                    file.mimeType.startsWith("audio/") -> Icons.Default.AudioFile
                    file.mimeType.startsWith("application/pdf") -> Icons.Default.PictureAsPdf
                    file.extension in listOf("zip", "rar", "7z", "tar", "gz") -> Icons.Default.FolderZip
                    file.extension in listOf("kt", "java", "py", "js", "html", "css", "json") -> Icons.Default.Code
                    else -> Icons.Default.InsertDriveFile
                }

                Icon(
                    imageVector = icon,
                    contentDescription = null,
                    tint = CyanNeon,
                    modifier = Modifier.size(44.dp)
                )

                // Star badge top-left
                IconButton(
                    onClick = onToggleStar,
                    modifier = Modifier
                        .align(Alignment.TopStart)
                        .padding(4.dp)
                        .size(32.dp)
                ) {
                    Icon(
                        imageVector = if (file.isStarred) Icons.Default.Star else Icons.Default.StarBorder,
                        contentDescription = "Star",
                        tint = if (file.isStarred) Color(0xFFFBBF24) else Color.Gray,
                        modifier = Modifier.size(18.dp)
                    )
                }

                // Extension Chip top-right
                Surface(
                    shape = RoundedCornerShape(6.dp),
                    color = MaterialTheme.colorScheme.surface.copy(alpha = 0.85f),
                    modifier = Modifier
                        .align(Alignment.TopEnd)
                        .padding(6.dp)
                ) {
                    Text(
                        text = file.extension.uppercase().ifBlank { "BIN" },
                        fontFamily = FontFamily.Monospace,
                        fontSize = 9.sp,
                        fontWeight = FontWeight.Bold,
                        color = CyanNeon,
                        modifier = Modifier.padding(horizontal = 5.dp, vertical = 2.dp)
                    )
                }
            }

            // File Information & Actions
            Column(modifier = Modifier.padding(10.dp)) {
                Text(
                    text = file.name,
                    fontWeight = FontWeight.Bold,
                    fontSize = 12.sp,
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis
                )

                Spacer(modifier = Modifier.height(2.dp))

                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Text(
                        text = formatStorageSize(file.totalSizeBytes),
                        fontSize = 10.sp,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )

                    Surface(
                        shape = RoundedCornerShape(4.dp),
                        color = Color(0xFF10B981).copy(alpha = 0.15f)
                    ) {
                        Text(
                            text = "Telegram Cloud",
                            fontSize = 8.sp,
                            fontWeight = FontWeight.Bold,
                            color = Color(0xFF10B981),
                            modifier = Modifier.padding(horizontal = 4.dp, vertical = 1.dp)
                        )
                    }
                }

                Spacer(modifier = Modifier.height(8.dp))

                // Quick Action Buttons Row on Card
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.spacedBy(6.dp),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    // Direct Animated Download to Device Button
                    com.example.ui.components.AnimatedCloudDownloadButton(
                        onClick = onDownloadToDevice,
                        label = if (isHindi) "डाउनलोड" else "Download",
                        modifier = Modifier.weight(1f),
                        testTag = "card_download_${file.id}"
                    )

                    // Share Decrypted File Button
                    FilledTonalIconButton(
                        onClick = onShareDecrypted,
                        modifier = Modifier.size(32.dp).testTag("card_share_${file.id}")
                    ) {
                        Icon(Icons.Default.Share, contentDescription = "Share", tint = MaterialTheme.colorScheme.primary, modifier = Modifier.size(15.dp))
                    }

                    // More Options Dropdown
                    Box {
                        IconButton(
                            onClick = { showMenu = true },
                            modifier = Modifier.size(32.dp).testTag("card_more_${file.id}")
                        ) {
                            Icon(Icons.Default.MoreVert, contentDescription = "More", modifier = Modifier.size(16.dp))
                        }

                        DropdownMenu(
                            expanded = showMenu,
                            onDismissRequest = { showMenu = false }
                        ) {
                            if (isMedia) {
                                DropdownMenuItem(
                                    text = { Text(if (isHindi) "गैलरी में सहेजें" else "Save to Gallery") },
                                    leadingIcon = { Icon(Icons.Default.PhotoLibrary, contentDescription = null) },
                                    onClick = {
                                        showMenu = false
                                        onSaveToGallery()
                                    }
                                )
                            }

                            DropdownMenuItem(
                                text = { Text(if (isHindi) "शून्य-ज्ञान शेयर टिकट" else "Share Ticket Link") },
                                leadingIcon = { Icon(Icons.Default.Link, contentDescription = null) },
                                onClick = {
                                    showMenu = false
                                    onShareTicket()
                                }
                            )

                            DropdownMenuItem(
                                text = { Text(if (isHindi) "टेलीग्राम सर्वर पर जांचें" else "Verify on Telegram") },
                                leadingIcon = { Icon(Icons.Default.CloudDone, contentDescription = null) },
                                onClick = {
                                    showMenu = false
                                    onVerifyTelegram()
                                }
                            )

                            HorizontalDivider()

                            DropdownMenuItem(
                                text = { Text(if (isHindi) "रीसायकल बिन में भेजें" else "Move to Trash", color = MaterialTheme.colorScheme.error) },
                                leadingIcon = { Icon(Icons.Default.DeleteOutline, contentDescription = null, tint = MaterialTheme.colorScheme.error) },
                                onClick = {
                                    showMenu = false
                                    onMoveToTrash()
                                }
                            )
                        }
                    }
                }
            }
        }
    }
}

private fun formatStorageSize(bytes: Long): String {
    if (bytes <= 0) return "0 B"
    val kb = bytes / 1024.0
    val mb = kb / 1024.0
    val gb = mb / 1024.0
    return when {
        gb >= 1.0 -> String.format(Locale.US, "%.2f GB", gb)
        mb >= 1.0 -> String.format(Locale.US, "%.1f MB", mb)
        kb >= 1.0 -> String.format(Locale.US, "%.1f KB", kb)
        else -> "$bytes B"
    }
}
