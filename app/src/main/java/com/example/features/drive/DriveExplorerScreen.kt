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
    onShareDecrypted: (VaultFile) -> Unit = {}
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
                    Row {
                        IconButton(onClick = { isGridView = !isGridView }) {
                            Icon(
                                imageVector = if (isGridView) Icons.Default.ViewList else Icons.Default.GridView,
                                contentDescription = "Toggle Grid/List",
                                tint = CyanNeon
                            )
                        }
                    }
                }

                // Interactive Breadcrumb Bar
                LazyRow(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(horizontal = 16.dp, vertical = 4.dp),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    items(breadcrumbs) { crumb ->
                        Surface(
                            shape = RoundedCornerShape(12.dp),
                            color = if (crumb.first == currentFolderId) CyanNeon.copy(alpha = 0.2f)
                            else MaterialTheme.colorScheme.surfaceVariant,
                            modifier = Modifier.clickable { onNavigateFolder(crumb.first) }
                        ) {
                            Text(
                                text = crumb.second,
                                fontSize = 12.sp,
                                fontWeight = FontWeight.SemiBold,
                                color = if (crumb.first == currentFolderId) CyanNeon else MaterialTheme.colorScheme.onSurface,
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
                HorizontalDivider(modifier = Modifier.padding(top = 4.dp))
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
            if (folders.isNotEmpty()) {
                item {
                    Text(
                        text = if (isHindi) "फ़ोल्डर्स" else "Folders",
                        fontSize = 13.sp,
                        fontWeight = FontWeight.Bold,
                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                        modifier = Modifier.padding(vertical = 4.dp)
                    )
                }

                items(folders) { folder ->
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
                                    text = if (folder.syncLocalPath != null) "2-Way Live Sync: Active" else "Cloud Encrypted",
                                    fontSize = 11.sp,
                                    color = MaterialTheme.colorScheme.onSurfaceVariant
                                )
                            }
                            Icon(Icons.Default.ChevronRight, contentDescription = null, tint = Color.Gray)
                        }
                    }
                }
            }

            // Files Section
            item {
                Spacer(modifier = Modifier.height(8.dp))
                Text(
                    text = if (isHindi) "फाइलें (${files.size})" else "Files (${files.size})",
                    fontSize = 13.sp,
                    fontWeight = FontWeight.Bold,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                    modifier = Modifier.padding(vertical = 4.dp)
                )
            }

            if (files.isEmpty()) {
                item {
                    Box(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(24.dp),
                        contentAlignment = Alignment.Center
                    ) {
                        Text(
                            text = if (isHindi) "इस फ़ोल्डर में कोई फ़ाइल नहीं है। FAB बटन से अपलोड करें।"
                            else "No files in this folder. Use '+' FAB to upload.",
                            fontSize = 13.sp,
                            color = MaterialTheme.colorScheme.onSurfaceVariant
                        )
                    }
                }
            } else {
                items(files) { file ->
                    var showVersionSheet by remember { mutableStateOf(false) }

                    Card(
                        modifier = Modifier
                            .fillMaxWidth()
                            .clickable { onFileClick(file) },
                        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surfaceVariant),
                        shape = RoundedCornerShape(12.dp)
                    ) {
                        Row(
                            modifier = Modifier
                                .padding(12.dp)
                                .fillMaxWidth(),
                            verticalAlignment = Alignment.CenterVertically
                        ) {
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

                            // Star action
                            IconButton(onClick = { onToggleStar(file) }) {
                                Icon(
                                    imageVector = if (file.isStarred) Icons.Default.Star else Icons.Default.StarBorder,
                                    contentDescription = "Star",
                                    tint = if (file.isStarred) Color(0xFFFBBF24) else Color.Gray,
                                    modifier = Modifier.size(20.dp)
                                )
                            }

                            // Download & Decrypt to Device with smooth animation
                            com.example.ui.components.AnimatedCloudDownloadButton(
                                onClick = { onDownloadToDevice(file) },
                                isCompact = true,
                                testTag = "drive_download_${file.id}"
                            )

                            // Share Decrypted File action
                            IconButton(onClick = { onShareDecrypted(file) }) {
                                Icon(
                                    imageVector = Icons.Default.Share,
                                    contentDescription = "Share Decrypted File",
                                    tint = MaterialTheme.colorScheme.primary,
                                    modifier = Modifier.size(18.dp)
                                )
                            }

                            // Delete / Move to Trash action
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
}
