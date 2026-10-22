package com.example.features.drive

import androidx.compose.animation.*
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowForward
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
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.core.database.VaultFile
import com.example.core.database.VirtualFolder
import com.example.features.studio.AppLanguage
import com.example.ui.theme.CyanNeon
import com.example.ui.theme.IndigoHyper
import java.text.SimpleDateFormat
import java.util.*

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun DriveExplorerScreen(
    currentFolderId: String,
    folders: List<VirtualFolder>,
    files: List<VaultFile>,
    language: AppLanguage,
    onNavigateFolder: (String) -> Unit,
    onFileClick: (VaultFile) -> Unit,
    onCreateFolder: (name: String, colorHex: String) -> Unit,
    onRenameFolder: (folderId: String, newName: String) -> Unit = { _, _ -> },
    onDeleteFolder: (folderId: String) -> Unit = {},
    onPickFiles: () -> Unit,
    onPickMedia: () -> Unit,
    onCreateNewDoc: (name: String, content: String) -> Unit,
    onToggleStar: (VaultFile) -> Unit,
    onToggleOffline: (VaultFile) -> Unit,
    onDeleteFile: (VaultFile) -> Unit,
    onShareTicket: (VaultFile) -> Unit,
    onVerifyTelegram: (VaultFile) -> Unit = {},
    onDownloadToDevice: (VaultFile) -> Unit = {},
    onSaveToGallery: (VaultFile) -> Unit = {},
    onShareDecrypted: (VaultFile) -> Unit = {},
    onBatchDownload: (List<VaultFile>) -> Unit = {},
    onBatchDelete: (List<String>) -> Unit = {},
    onBatchMoveToFolder: (fileIds: List<String>, targetFolderId: String) -> Unit = { _, _ -> },
    onSyncChannel: () -> Unit = {}
) {
    val isHindi = language == AppLanguage.HI

    var isGridView by remember { mutableStateOf(false) }
    var showCreateFolderDialog by remember { mutableStateOf(false) }
    var newFolderName by remember { mutableStateOf("") }
    var selectedColorHex by remember { mutableStateOf("#00F0FF") }
    var showCreateDocDialog by remember { mutableStateOf(false) }
    var newDocTitle by remember { mutableStateOf("") }
    var newDocContent by remember { mutableStateOf("") }
    var showFabSpeedDial by remember { mutableStateOf(false) }

    // Folder Rename/Delete State
    var folderToRename by remember { mutableStateOf<VirtualFolder?>(null) }
    var renameInput by remember { mutableStateOf("") }
    var singleFileToMove by remember { mutableStateOf<VaultFile?>(null) }

    // Multi-Selection State
    var isSelectionMode by remember { mutableStateOf(false) }
    val selectedFileIds = remember { mutableStateListOf<String>() }
    var showMoveToFolderDialog by remember { mutableStateOf(false) }

    // Filter subfolders and files for current folder view
    val currentSubFolders = remember(folders, currentFolderId) {
        folders.filter {
            if (currentFolderId == "root") (it.parentId == null || it.parentId == "root")
            else it.parentId == currentFolderId
        }
    }

    val currentFolderFiles = remember(files, currentFolderId) {
        files.filter {
            if (currentFolderId == "root") (it.folderId == "root" || it.folderId.isBlank())
            else it.folderId == currentFolderId
        }
    }

    // Dynamic breadcrumb path stack
    val breadcrumbs = remember(currentFolderId, folders) {
        val list = mutableListOf(Pair("root", if (isHindi) "वॉल्ट" else "Vault"))
        if (currentFolderId != "root") {
            val curr = folders.find { it.id == currentFolderId }
            if (curr != null) {
                list.add(Pair(curr.id, curr.name))
            }
        }
        list
    }

    Scaffold(
        topBar = {
            Column(modifier = Modifier.background(MaterialTheme.colorScheme.surface)) {
                // Top App Bar
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(horizontal = 16.dp, vertical = 8.dp),
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.SpaceBetween
                ) {
                    Text(
                        text = if (isHindi) "क्लाउड ड्राइव VFS" else "Vault Cloud Drive",
                        style = MaterialTheme.typography.titleLarge,
                        fontWeight = FontWeight.Bold
                    )
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        // Direct Create Folder Button
                        IconButton(
                            onClick = { showCreateFolderDialog = true },
                            modifier = Modifier.testTag("create_folder_top_button")
                        ) {
                            Icon(
                                imageVector = Icons.Default.CreateNewFolder,
                                contentDescription = "Create Folder",
                                tint = MaterialTheme.colorScheme.primary
                            )
                        }

                        // Multi-Select Toggle Button
                        IconButton(
                            onClick = {
                                isSelectionMode = !isSelectionMode
                                if (!isSelectionMode) selectedFileIds.clear()
                            },
                            modifier = Modifier.testTag("multi_select_toggle_button")
                        ) {
                            Icon(
                                imageVector = if (isSelectionMode) Icons.Default.CheckCircle else Icons.Default.CheckCircleOutline,
                                contentDescription = "Select Files",
                                tint = if (isSelectionMode) MaterialTheme.colorScheme.primary else MaterialTheme.colorScheme.onSurfaceVariant
                            )
                        }

                        // Sync Channel Content Button
                        IconButton(
                            onClick = onSyncChannel,
                            modifier = Modifier.testTag("sync_channel_button")
                        ) {
                            Icon(
                                imageVector = Icons.Default.Sync,
                                contentDescription = "Sync Server",
                                tint = MaterialTheme.colorScheme.primary
                            )
                        }

                        IconButton(onClick = { isGridView = !isGridView }) {
                            Icon(
                                imageVector = if (isGridView) Icons.Default.ViewList else Icons.Default.GridView,
                                contentDescription = "Toggle Grid/List",
                                tint = MaterialTheme.colorScheme.onSurfaceVariant
                            )
                        }
                    }
                }

                // Interactive Breadcrumb Bar with Back Button
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(horizontal = 16.dp, vertical = 4.dp),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    if (breadcrumbs.size > 1) {
                        IconButton(
                            onClick = { onNavigateFolder(breadcrumbs[breadcrumbs.size - 2].first) },
                            modifier = Modifier.size(28.dp).padding(end = 4.dp)
                        ) {
                            Icon(
                                imageVector = Icons.Default.ArrowBack,
                                contentDescription = "Back",
                                tint = MaterialTheme.colorScheme.primary,
                                modifier = Modifier.size(18.dp)
                            )
                        }
                    }

                    LazyRow(
                        modifier = Modifier.weight(1f),
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        items(breadcrumbs) { crumb ->
                            Surface(
                                shape = RoundedCornerShape(12.dp),
                                color = if (crumb.first == currentFolderId) MaterialTheme.colorScheme.primaryContainer
                                else MaterialTheme.colorScheme.surfaceVariant,
                                modifier = Modifier.clickable { onNavigateFolder(crumb.first) }
                            ) {
                                Text(
                                    text = crumb.second,
                                    fontSize = 12.sp,
                                    fontWeight = FontWeight.SemiBold,
                                    color = if (crumb.first == currentFolderId) MaterialTheme.colorScheme.onPrimaryContainer else MaterialTheme.colorScheme.onSurface,
                                    modifier = Modifier.padding(horizontal = 10.dp, vertical = 4.dp)
                                )
                            }
                            if (crumb != breadcrumbs.last()) {
                                Icon(
                                    imageVector = Icons.Default.ChevronRight,
                                    contentDescription = null,
                                    tint = Color.Gray,
                                    modifier = Modifier.size(16.dp)
                                )
                            }
                        }
                    }
                }

                // Multi-selection helper bar
                AnimatedVisibility(visible = isSelectionMode) {
                    Surface(
                        color = CyanNeon.copy(alpha = 0.12f),
                        modifier = Modifier.fillMaxWidth()
                    ) {
                        Row(
                            modifier = Modifier
                                .fillMaxWidth()
                                .padding(horizontal = 16.dp, vertical = 6.dp),
                            horizontalArrangement = Arrangement.SpaceBetween,
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Text(
                                text = if (isHindi) "${selectedFileIds.size} फ़ाइलें चुनी गईं" else "${selectedFileIds.size} files selected",
                                fontSize = 12.sp,
                                fontWeight = FontWeight.Bold,
                                color = CyanNeon
                            )
                            Row(horizontalArrangement = Arrangement.spacedBy(4.dp)) {
                                TextButton(
                                    onClick = {
                                        selectedFileIds.clear()
                                        selectedFileIds.addAll(currentFolderFiles.map { it.id })
                                    },
                                    contentPadding = PaddingValues(horizontal = 6.dp, vertical = 2.dp)
                                ) {
                                    Text(if (isHindi) "सभी चुनें" else "Select All", fontSize = 11.sp, color = CyanNeon)
                                }
                                TextButton(
                                    onClick = {
                                        isSelectionMode = false
                                        selectedFileIds.clear()
                                    },
                                    contentPadding = PaddingValues(horizontal = 6.dp, vertical = 2.dp)
                                ) {
                                    Text(if (isHindi) "रद्द करें" else "Cancel", fontSize = 11.sp, color = MaterialTheme.colorScheme.onSurfaceVariant)
                                }
                            }
                        }
                    }
                }
                HorizontalDivider(modifier = Modifier.padding(top = 4.dp))
            }
        },
        bottomBar = {
            AnimatedVisibility(
                visible = isSelectionMode && selectedFileIds.isNotEmpty(),
                enter = slideInVertically(initialOffsetY = { it }) + fadeIn(),
                exit = slideOutVertically(targetOffsetY = { it }) + fadeOut()
            ) {
                Surface(
                    color = MaterialTheme.colorScheme.surfaceVariant,
                    tonalElevation = 8.dp,
                    shadowElevation = 8.dp,
                    shape = RoundedCornerShape(topStart = 16.dp, topEnd = 16.dp),
                    modifier = Modifier.fillMaxWidth()
                ) {
                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(horizontal = 16.dp, vertical = 10.dp),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Button(
                            onClick = {
                                val selectedFiles = files.filter { it.id in selectedFileIds }
                                onBatchDownload(selectedFiles)
                                isSelectionMode = false
                                selectedFileIds.clear()
                            },
                            colors = ButtonDefaults.buttonColors(containerColor = CyanNeon, contentColor = Color.Black),
                            shape = RoundedCornerShape(10.dp),
                            modifier = Modifier.testTag("batch_download_button")
                        ) {
                            Icon(Icons.Default.Download, contentDescription = null, modifier = Modifier.size(16.dp))
                            Spacer(modifier = Modifier.width(4.dp))
                            Text(
                                text = if (isHindi) "डाउनलोड (${selectedFileIds.size})" else "Download (${selectedFileIds.size})",
                                fontWeight = FontWeight.Bold,
                                fontSize = 12.sp
                            )
                        }

                        OutlinedButton(
                            onClick = { showMoveToFolderDialog = true },
                            shape = RoundedCornerShape(10.dp),
                            modifier = Modifier.testTag("batch_move_folder_button")
                        ) {
                            Icon(Icons.Default.DriveFileMove, contentDescription = null, modifier = Modifier.size(16.dp))
                            Spacer(modifier = Modifier.width(4.dp))
                            Text(if (isHindi) "फ़ोल्डर में" else "Move", fontSize = 12.sp)
                        }

                        Button(
                            onClick = {
                                onBatchDelete(selectedFileIds.toList())
                                isSelectionMode = false
                                selectedFileIds.clear()
                            },
                            colors = ButtonDefaults.buttonColors(containerColor = MaterialTheme.colorScheme.error),
                            shape = RoundedCornerShape(10.dp),
                            modifier = Modifier.testTag("batch_delete_button")
                        ) {
                            Icon(Icons.Default.Delete, contentDescription = null, modifier = Modifier.size(16.dp))
                            Spacer(modifier = Modifier.width(4.dp))
                            Text(
                                text = if (isHindi) "डिलीट (${selectedFileIds.size})" else "Delete (${selectedFileIds.size})",
                                fontSize = 12.sp
                            )
                        }
                    }
                }
            }
        },
        floatingActionButton = {
            Column(horizontalAlignment = Alignment.End) {
                AnimatedVisibility(visible = showFabSpeedDial) {
                    Column(
                        horizontalAlignment = Alignment.End,
                        verticalArrangement = Arrangement.spacedBy(8.dp),
                        modifier = Modifier.padding(bottom = 12.dp)
                    ) {
                        ExtendedFloatingActionButton(
                            onClick = {
                                showFabSpeedDial = false
                                showCreateFolderDialog = true
                            },
                            icon = { Icon(Icons.Default.CreateNewFolder, contentDescription = null) },
                            text = { Text(if (isHindi) "नया फ़ोल्डर" else "New Folder") },
                            containerColor = IndigoHyper,
                            contentColor = Color.White
                        )
                        ExtendedFloatingActionButton(
                            onClick = {
                                showFabSpeedDial = false
                                showCreateDocDialog = true
                            },
                            icon = { Icon(Icons.Default.NoteAdd, contentDescription = null) },
                            text = { Text(if (isHindi) "कोड / मार्कडाउन फ़ाइल" else "New Code / Doc") },
                            containerColor = MaterialTheme.colorScheme.secondary,
                            contentColor = Color.White
                        )
                        ExtendedFloatingActionButton(
                            onClick = {
                                showFabSpeedDial = false
                                onPickMedia()
                            },
                            icon = { Icon(Icons.Default.AddPhotoAlternate, contentDescription = null) },
                            text = { Text(if (isHindi) "फोटो व वीडियो चुनें" else "Upload Photos / Media") },
                            containerColor = Color(0xFF10B981),
                            contentColor = Color.White
                        )
                        ExtendedFloatingActionButton(
                            onClick = {
                                showFabSpeedDial = false
                                onPickFiles()
                            },
                            icon = { Icon(Icons.Default.UploadFile, contentDescription = null) },
                            text = { Text(if (isHindi) "फ़ोन से फ़ाइल चुनें (SAF)" else "Pick Any File (SAF)") },
                            containerColor = CyanNeon,
                            contentColor = Color.Black
                        )
                    }
                }

                FloatingActionButton(
                    onClick = { showFabSpeedDial = !showFabSpeedDial },
                    containerColor = CyanNeon,
                    contentColor = Color.Black,
                    modifier = Modifier.testTag("drive_fab")
                ) {
                    Icon(
                        imageVector = if (showFabSpeedDial) Icons.Default.Close else Icons.Default.Add,
                        contentDescription = "Actions"
                    )
                }
            }
        }
    ) { padding ->
        LazyColumn(
            modifier = Modifier
                .fillMaxSize()
                .padding(padding)
                .padding(horizontal = 16.dp),
            verticalArrangement = Arrangement.spacedBy(8.dp)
        ) {
            // Folders Section
            item {
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(vertical = 4.dp),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Text(
                        text = if (isHindi) "फ़ोल्डर्स (${currentSubFolders.size})" else "Folders (${currentSubFolders.size})",
                        fontSize = 13.sp,
                        fontWeight = FontWeight.Bold,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                    TextButton(
                        onClick = { showCreateFolderDialog = true },
                        contentPadding = PaddingValues(horizontal = 8.dp, vertical = 2.dp),
                        modifier = Modifier.testTag("inline_create_folder_button")
                    ) {
                        Icon(Icons.Default.CreateNewFolder, contentDescription = null, modifier = Modifier.size(15.dp), tint = CyanNeon)
                        Spacer(modifier = Modifier.width(4.dp))
                        Text(if (isHindi) "+ नया फ़ोल्डर" else "+ New Folder", fontSize = 12.sp, color = CyanNeon, fontWeight = FontWeight.Bold)
                    }
                }
            }

            if (currentSubFolders.isEmpty()) {
                item {
                    Surface(
                        color = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.5f),
                        shape = RoundedCornerShape(10.dp),
                        modifier = Modifier
                            .fillMaxWidth()
                            .clickable { showCreateFolderDialog = true }
                    ) {
                        Row(
                            modifier = Modifier.padding(12.dp),
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Icon(Icons.Default.CreateNewFolder, contentDescription = null, tint = CyanNeon, modifier = Modifier.size(20.dp))
                            Spacer(modifier = Modifier.width(10.dp))
                            Text(
                                text = if (isHindi) "यहाँ कोई फ़ोल्डर नहीं है — नया फ़ोल्डर बनाने के लिए टैप करें" else "No subfolders here — Tap to create folder",
                                fontSize = 12.sp,
                                color = MaterialTheme.colorScheme.onSurfaceVariant
                            )
                        }
                    }
                }
            } else {
                items(currentSubFolders) { folder ->
                    Card(
                        modifier = Modifier
                            .fillMaxWidth()
                            .clickable { onNavigateFolder(folder.id) },
                        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surfaceVariant),
                        shape = RoundedCornerShape(12.dp)
                    ) {
                        Row(
                            modifier = Modifier
                                .padding(12.dp)
                                .fillMaxWidth(),
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Box(
                                modifier = Modifier
                                    .size(36.dp)
                                    .clip(RoundedCornerShape(8.dp))
                                    .background(runCatching { Color(android.graphics.Color.parseColor(folder.colorHex)) }.getOrDefault(CyanNeon)),
                                contentAlignment = Alignment.Center
                            ) {
                                Icon(Icons.Default.Folder, contentDescription = null, tint = Color.Black, modifier = Modifier.size(20.dp))
                            }
                            Spacer(modifier = Modifier.width(12.dp))
                            Column(modifier = Modifier.weight(1f)) {
                                Text(text = folder.name, fontWeight = FontWeight.SemiBold, fontSize = 14.sp)
                                Text(
                                    text = "Cloud Encrypted Folder",
                                    fontSize = 11.sp,
                                    color = MaterialTheme.colorScheme.onSurfaceVariant
                                )
                            }

                            // Rename Folder action
                            IconButton(
                                onClick = {
                                    folderToRename = folder
                                    renameInput = folder.name
                                },
                                modifier = Modifier.size(28.dp)
                            ) {
                                Icon(Icons.Default.Edit, contentDescription = "Rename Folder", tint = CyanNeon, modifier = Modifier.size(16.dp))
                            }

                            // Delete Folder action
                            IconButton(
                                onClick = { onDeleteFolder(folder.id) },
                                modifier = Modifier.size(28.dp)
                            ) {
                                Icon(Icons.Default.DeleteOutline, contentDescription = "Delete Folder", tint = MaterialTheme.colorScheme.error, modifier = Modifier.size(16.dp))
                            }

                            Icon(Icons.Default.ChevronRight, contentDescription = null, tint = Color.Gray, modifier = Modifier.size(20.dp))
                        }
                    }
                }
            }

            // Files Section
            item {
                Spacer(modifier = Modifier.height(8.dp))
                Text(
                    text = if (isHindi) "फाइलें (${currentFolderFiles.size})" else "Files (${currentFolderFiles.size})",
                    fontSize = 13.sp,
                    fontWeight = FontWeight.Bold,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                    modifier = Modifier.padding(vertical = 4.dp)
                )
            }

            if (currentFolderFiles.isEmpty()) {
                item {
                    Box(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(24.dp),
                        contentAlignment = Alignment.Center
                    ) {
                        Text(
                            text = if (isHindi) "इस फ़ोल्डर में कोई फ़ाइल नहीं है। FAB '+' बटन से अपलोड करें।"
                            else "No files in this folder. Use '+' FAB to upload.",
                            fontSize = 13.sp,
                            color = MaterialTheme.colorScheme.onSurfaceVariant
                        )
                    }
                }
            } else {
                items(currentFolderFiles) { file ->
                    val isSelected = file.id in selectedFileIds

                    Card(
                        modifier = Modifier
                            .fillMaxWidth()
                            .clickable {
                                if (isSelectionMode) {
                                    if (isSelected) selectedFileIds.remove(file.id)
                                    else selectedFileIds.add(file.id)
                                } else {
                                    onFileClick(file)
                                }
                            },
                        colors = CardDefaults.cardColors(
                            containerColor = if (isSelected) CyanNeon.copy(alpha = 0.15f) else MaterialTheme.colorScheme.surfaceVariant
                        ),
                        border = if (isSelected) androidx.compose.foundation.BorderStroke(1.5.dp, CyanNeon) else null,
                        shape = RoundedCornerShape(12.dp)
                    ) {
                        Row(
                            modifier = Modifier
                                .padding(12.dp)
                                .fillMaxWidth(),
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            if (isSelectionMode) {
                                Checkbox(
                                    checked = isSelected,
                                    onCheckedChange = { checked ->
                                        if (checked) selectedFileIds.add(file.id)
                                        else selectedFileIds.remove(file.id)
                                    },
                                    colors = CheckboxDefaults.colors(checkedColor = CyanNeon)
                                )
                                Spacer(modifier = Modifier.width(6.dp))
                            }
                            // File Type Icon
                            val iconVector = when {
                                file.mimeType.startsWith("video/") -> Icons.Default.VideoFile
                                file.mimeType.startsWith("image/") -> Icons.Default.Image
                                file.mimeType.contains("pdf") -> Icons.Default.PictureAsPdf
                                file.extension in listOf("kt", "py", "js", "html", "css", "sql") -> Icons.Default.Code
                                file.extension in listOf("zip", "rar", "7z", "apk") -> Icons.Default.FolderZip
                                else -> Icons.Default.InsertDriveFile
                            }

                            Box(
                                modifier = Modifier
                                    .size(38.dp)
                                    .clip(RoundedCornerShape(8.dp))
                                    .background(IndigoHyper.copy(alpha = 0.2f)),
                                contentAlignment = Alignment.Center
                            ) {
                                Icon(iconVector, contentDescription = null, tint = CyanNeon, modifier = Modifier.size(20.dp))
                            }

                            Spacer(modifier = Modifier.width(12.dp))

                            Column(modifier = Modifier.weight(1f)) {
                                Text(
                                    text = file.name,
                                    fontWeight = FontWeight.SemiBold,
                                    fontSize = 14.sp,
                                    maxLines = 1
                                )
                                Row(verticalAlignment = Alignment.CenterVertically) {
                                    val sizeStr = if (file.totalSizeBytes > 1024 * 1024) {
                                        String.format("%.1f MB", file.totalSizeBytes / (1024f * 1024f))
                                    } else {
                                        "${file.totalSizeBytes / 1024} KB"
                                    }
                                    Text(
                                        text = "$sizeStr • ${SimpleDateFormat("dd MMM", Locale.getDefault()).format(Date(file.modifiedAt))}",
                                        fontSize = 11.sp,
                                        color = MaterialTheme.colorScheme.onSurfaceVariant
                                    )
                                    Spacer(modifier = Modifier.width(6.dp))
                                    Surface(
                                        color = CyanNeon.copy(alpha = 0.15f),
                                        shape = RoundedCornerShape(4.dp)
                                    ) {
                                        Text(
                                            text = "v${file.currentVersion}",
                                            fontSize = 9.sp,
                                            fontWeight = FontWeight.Bold,
                                            color = CyanNeon,
                                            modifier = Modifier.padding(horizontal = 4.dp, vertical = 1.dp)
                                        )
                                    }
                                }
                            }

                            // Move to folder action for single file
                            IconButton(onClick = {
                                singleFileToMove = file
                                showMoveToFolderDialog = true
                            }) {
                                Icon(
                                    imageVector = Icons.Default.DriveFileMove,
                                    contentDescription = "Move File",
                                    tint = CyanNeon,
                                    modifier = Modifier.size(18.dp)
                                )
                            }

                            // Download & Decrypt to Device button
                            com.example.ui.components.AnimatedCloudDownloadButton(
                                onClick = { onDownloadToDevice(file) },
                                isCompact = true,
                                testTag = "drive_download_${file.id}"
                            )

                            // Delete action
                            IconButton(onClick = { onDeleteFile(file) }) {
                                Icon(
                                    imageVector = Icons.Default.DeleteOutline,
                                    contentDescription = "Delete",
                                    tint = MaterialTheme.colorScheme.error.copy(alpha = 0.8f),
                                    modifier = Modifier.size(18.dp)
                                )
                            }
                        }
                    }
                }
            }

            item {
                Spacer(modifier = Modifier.height(64.dp))
            }
        }
    }

    if (showCreateFolderDialog) {
        AlertDialog(
            onDismissRequest = { showCreateFolderDialog = false },
            title = { Text(if (isHindi) "नया फ़ोल्डर बनाएं" else "New Virtual Folder") },
            text = {
                Column {
                    OutlinedTextField(
                        value = newFolderName,
                        onValueChange = { newFolderName = it },
                        label = { Text("Folder Name") },
                        modifier = Modifier.fillMaxWidth(),
                        singleLine = true
                    )
                    Spacer(modifier = Modifier.height(12.dp))
                    Text("Select Folder Color Tag:", fontSize = 12.sp)
                    Spacer(modifier = Modifier.height(6.dp))
                    Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                        listOf("#00F0FF", "#6366F1", "#A855F7", "#10B981", "#EF4444", "#F59E0B").forEach { hex ->
                            Box(
                                modifier = Modifier
                                    .size(28.dp)
                                    .clip(CircleShape)
                                    .background(Color(android.graphics.Color.parseColor(hex)))
                                    .clickable { selectedColorHex = hex }
                                    .border(
                                        width = if (selectedColorHex == hex) 2.dp else 0.dp,
                                        color = Color.White,
                                        shape = CircleShape
                                    )
                            )
                        }
                    }
                }
            },
            confirmButton = {
                Button(
                    onClick = {
                        if (newFolderName.isNotBlank()) {
                            onCreateFolder(newFolderName.trim(), selectedColorHex)
                            newFolderName = ""
                            showCreateFolderDialog = false
                        }
                    }
                ) {
                    Text(if (isHindi) "बनाएं" else "Create")
                }
            },
            dismissButton = {
                TextButton(onClick = { showCreateFolderDialog = false }) {
                    Text(if (isHindi) "रद्द करें" else "Cancel")
                }
            }
        )
    }

    // Folder Rename Dialog
    folderToRename?.let { folder ->
        AlertDialog(
            onDismissRequest = { folderToRename = null },
            title = { Text(if (isHindi) "फ़ोल्डर का नाम बदलें" else "Rename Folder") },
            text = {
                OutlinedTextField(
                    value = renameInput,
                    onValueChange = { renameInput = it },
                    label = { Text("New Folder Name") },
                    singleLine = true,
                    modifier = Modifier.fillMaxWidth()
                )
            },
            confirmButton = {
                Button(
                    onClick = {
                        if (renameInput.isNotBlank()) {
                            onRenameFolder(folder.id, renameInput.trim())
                            folderToRename = null
                        }
                    }
                ) {
                    Text(if (isHindi) "सेव करें" else "Save")
                }
            },
            dismissButton = {
                TextButton(onClick = { folderToRename = null }) {
                    Text(if (isHindi) "रद्द करें" else "Cancel")
                }
            }
        )
    }

    if (showCreateDocDialog) {
        AlertDialog(
            onDismissRequest = { showCreateDocDialog = false },
            title = { Text(if (isHindi) "नया कोड / मार्कडाउन दस्तावेज़" else "New Code / Markdown Document") },
            text = {
                Column {
                    OutlinedTextField(
                        value = newDocTitle,
                        onValueChange = { newDocTitle = it },
                        label = { Text("Filename (e.g. notes.md or script.kt)") },
                        modifier = Modifier.fillMaxWidth(),
                        singleLine = true
                    )
                    Spacer(modifier = Modifier.height(10.dp))
                    OutlinedTextField(
                        value = newDocContent,
                        onValueChange = { newDocContent = it },
                        label = { Text("Document Content") },
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
                        if (newDocTitle.isNotBlank()) {
                            onCreateNewDoc(newDocTitle.trim(), newDocContent)
                            newDocTitle = ""
                            newDocContent = ""
                            showCreateDocDialog = false
                        }
                    }
                ) {
                    Text(if (isHindi) "सहेजें" else "Save to Cloud")
                }
            },
            dismissButton = {
                TextButton(onClick = { showCreateDocDialog = false }) {
                    Text(if (isHindi) "रद्द करें" else "Cancel")
                }
            }
        )
    }

    // Move Selected Files / Single File to Folder Dialog
    if (showMoveToFolderDialog) {
        val targets = if (singleFileToMove != null) listOf(singleFileToMove!!.id) else selectedFileIds.toList()
        AlertDialog(
            onDismissRequest = {
                showMoveToFolderDialog = false
                singleFileToMove = null
            },
            title = {
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Icon(Icons.Default.DriveFileMove, contentDescription = null, tint = CyanNeon)
                    Spacer(modifier = Modifier.width(8.dp))
                    Text(if (isHindi) "फ़ाइलें फ़ोल्डर में डालें" else "Move Files to Folder")
                }
            },
            text = {
                Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                    Text(
                        text = if (isHindi) "${targets.size} चुनी गई फ़ाइलों को किस फ़ोल्डर में डालना चाहते हैं?"
                        else "Select destination folder for ${targets.size} file(s):",
                        fontSize = 12.sp,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )

                    // Root option
                    Card(
                        modifier = Modifier
                            .fillMaxWidth()
                            .clickable {
                                onBatchMoveToFolder(targets, "root")
                                showMoveToFolderDialog = false
                                singleFileToMove = null
                                isSelectionMode = false
                                selectedFileIds.clear()
                            },
                        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surfaceVariant)
                    ) {
                        Row(modifier = Modifier.padding(12.dp), verticalAlignment = Alignment.CenterVertically) {
                            Icon(Icons.Default.FolderSpecial, contentDescription = null, tint = CyanNeon)
                            Spacer(modifier = Modifier.width(8.dp))
                            Text(if (isHindi) "मुख्य वॉल्ट (Root Vault)" else "Root Vault", fontWeight = FontWeight.Bold)
                        }
                    }

                    // Available folders
                    folders.forEach { f ->
                        Card(
                            modifier = Modifier
                                .fillMaxWidth()
                                .clickable {
                                    onBatchMoveToFolder(targets, f.id)
                                    showMoveToFolderDialog = false
                                    singleFileToMove = null
                                    isSelectionMode = false
                                    selectedFileIds.clear()
                                },
                            colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surfaceVariant)
                        ) {
                            Row(modifier = Modifier.padding(12.dp), verticalAlignment = Alignment.CenterVertically) {
                                Box(
                                    modifier = Modifier
                                        .size(28.dp)
                                        .clip(RoundedCornerShape(6.dp))
                                        .background(runCatching { Color(android.graphics.Color.parseColor(f.colorHex)) }.getOrDefault(CyanNeon)),
                                    contentAlignment = Alignment.Center
                                ) {
                                    Icon(Icons.Default.Folder, contentDescription = null, tint = Color.Black, modifier = Modifier.size(16.dp))
                                }
                                Spacer(modifier = Modifier.width(8.dp))
                                Text(f.name, fontWeight = FontWeight.SemiBold)
                            }
                        }
                    }
                }
            },
            confirmButton = {},
            dismissButton = {
                TextButton(onClick = {
                    showMoveToFolderDialog = false
                    singleFileToMove = null
                }) {
                    Text(if (isHindi) "रद्द करें" else "Cancel")
                }
            }
        )
    }
}

