package com.example.features.photos

import android.graphics.BitmapFactory
import androidx.compose.animation.*
import androidx.compose.foundation.*
import androidx.compose.foundation.gestures.detectDragGesturesAfterLongPress
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.foundation.lazy.grid.*
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
import androidx.compose.ui.graphics.asImageBitmap
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.LocalHapticFeedback
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.hapticfeedback.HapticFeedbackType
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.core.database.VaultFile
import com.example.features.studio.AppLanguage
import com.example.features.studio.AspectRatioMode
import com.example.ui.theme.CyanNeon
import com.example.ui.theme.IndigoHyper
import java.text.SimpleDateFormat
import java.util.*

data class PhotoMemory(
    val title: String,
    val subtitle: String,
    val iconColor: Color
)

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun PhotosTimelineScreen(
    photos: List<VaultFile>,
    gridColumns: Int,
    aspectRatioMode: AspectRatioMode,
    language: AppLanguage,
    onPhotoClick: (VaultFile) -> Unit,
    onFreeUpSpaceClick: () -> Unit,
    onUploadMediaClick: () -> Unit,
    onVerifyTelegram: (VaultFile) -> Unit = {},
    onDownloadToDevice: (VaultFile) -> Unit = {},
    onSaveToGallery: (VaultFile) -> Unit = {}
) {
    val haptic = LocalHapticFeedback.current
    val isHindi = language == AppLanguage.HI

    var selectedFilter by remember { mutableStateOf("ALL") }
    var searchQuery by remember { mutableStateOf("") }
    val selectedItemIds = remember { mutableStateListOf<String>() }
    var isSelectionMode by remember { mutableStateOf(false) }

    val filteredPhotos = remember(photos, selectedFilter, searchQuery) {
        photos.filter { file ->
            val matchFilter = when (selectedFilter) {
                "FAVORITES" -> file.isStarred
                "VIDEOS" -> file.mimeType.startsWith("video/")
                "DOCS" -> file.mimeType.contains("pdf") || file.extension in listOf("doc", "docx", "pdf", "txt")
                else -> true
            }
            val matchSearch = searchQuery.isBlank() || file.name.contains(searchQuery, ignoreCase = true)
            matchFilter && matchSearch
        }
    }

    val gridState = rememberLazyGridState()

    // Determine fast-scroll month bubble
    val firstVisibleItem = filteredPhotos.getOrNull(gridState.firstVisibleItemIndex)
    val currentMonthYear = remember(firstVisibleItem) {
        firstVisibleItem?.let {
            SimpleDateFormat("MMMM yyyy", Locale.getDefault()).format(Date(it.modifiedAt))
        } ?: "Timeline"
    }

    // Dynamic photo statistics based on real user files
    val dynamicStats = remember(photos) {
        if (photos.isEmpty()) emptyList()
        else {
            val totalMb = (photos.sumOf { it.totalSizeBytes } / (1024.0 * 1024.0))
            val starredCount = photos.count { it.isStarred }
            val videoCount = photos.count { it.mimeType.startsWith("video/") }
            listOf(
                PhotoMemory(if (isHindi) "कुल फ़ाइलें" else "Total Vault Items", "${photos.size} real files", CyanNeon),
                PhotoMemory(if (isHindi) "क्लाउड स्टोरेज" else "Storage Used", String.format("%.1f MB Encrypted", totalMb), IndigoHyper),
                PhotoMemory(if (isHindi) "पसंदीदा" else "Favorites", "$starredCount items", Color(0xFFF59E0B)),
                PhotoMemory("Creator: TRB FireOps", "@trbfireops • AES-256", Color(0xFF10B981))
            )
        }
    }

    Scaffold(
        topBar = {
            Column(modifier = Modifier.background(MaterialTheme.colorScheme.surface)) {
                // Top Search Bar
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(horizontal = 16.dp, vertical = 8.dp),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    OutlinedTextField(
                        value = searchQuery,
                        onValueChange = { searchQuery = it },
                        placeholder = {
                            Text(
                                if (isHindi) "खोजें 'बिल', 'गोवा 2026', 'दस्तावेज'..."
                                else "Search 'Receipts', 'Goa', 'Documents'...",
                                fontSize = 13.sp
                            )
                        },
                        leadingIcon = {
                            Icon(Icons.Default.Search, contentDescription = "Search", tint = CyanNeon)
                        },
                        trailingIcon = {
                            if (searchQuery.isNotBlank()) {
                                IconButton(onClick = { searchQuery = "" }) {
                                    Icon(Icons.Default.Close, contentDescription = "Clear")
                                }
                            }
                        },
                        modifier = Modifier
                            .weight(1f)
                            .height(50.dp)
                            .testTag("photos_search_input"),
                        shape = RoundedCornerShape(24.dp),
                        singleLine = true
                    )
                    Spacer(modifier = Modifier.width(8.dp))
                    IconButton(
                        onClick = onUploadMediaClick,
                        modifier = Modifier
                            .size(42.dp)
                            .clip(CircleShape)
                            .background(MaterialTheme.colorScheme.surfaceVariant)
                            .testTag("photos_upload_button")
                    ) {
                        Icon(
                            Icons.Default.AddPhotoAlternate,
                            contentDescription = "Upload Photos",
                            tint = CyanNeon,
                            modifier = Modifier.size(20.dp)
                        )
                    }
                    Spacer(modifier = Modifier.width(6.dp))
                    IconButton(
                        onClick = onFreeUpSpaceClick,
                        modifier = Modifier
                            .size(42.dp)
                            .clip(CircleShape)
                            .background(MaterialTheme.colorScheme.surfaceVariant)
                    ) {
                        Icon(
                            Icons.Default.CleaningServices,
                            contentDescription = "Free Up Space",
                            tint = CyanNeon,
                            modifier = Modifier.size(20.dp)
                        )
                    }
                }

                // Dynamic Stats Carousel
                if (dynamicStats.isNotEmpty()) {
                    LazyRow(
                        contentPadding = PaddingValues(horizontal = 16.dp, vertical = 4.dp),
                        horizontalArrangement = Arrangement.spacedBy(10.dp)
                    ) {
                        items(dynamicStats) { mem ->
                            Card(
                                modifier = Modifier
                                    .width(135.dp)
                                    .height(68.dp),
                                shape = RoundedCornerShape(12.dp),
                                colors = CardDefaults.cardColors(containerColor = mem.iconColor.copy(alpha = 0.16f)),
                                border = androidx.compose.foundation.BorderStroke(0.8.dp, mem.iconColor.copy(alpha = 0.4f))
                            ) {
                                Column(
                                    modifier = Modifier
                                        .padding(8.dp)
                                        .fillMaxSize(),
                                    verticalArrangement = Arrangement.Center
                                ) {
                                    Text(
                                        text = mem.title,
                                        fontWeight = FontWeight.Bold,
                                        fontSize = 11.sp,
                                        color = mem.iconColor,
                                        maxLines = 1
                                    )
                                    Text(
                                        text = mem.subtitle,
                                        fontSize = 9.sp,
                                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                                        maxLines = 1
                                    )
                                }
                            }
                        }
                    }
                }

                // Filter Chips
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .horizontalScroll(rememberScrollState())
                        .padding(horizontal = 16.dp, vertical = 6.dp),
                    horizontalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    listOf("ALL", "FAVORITES", "VIDEOS", "DOCS").forEach { filterKey ->
                        val selected = selectedFilter == filterKey
                        FilterChip(
                            selected = selected,
                            onClick = { selectedFilter = filterKey },
                            label = {
                                Text(
                                    when (filterKey) {
                                        "ALL" -> if (isHindi) "सभी फोटो" else "All Media"
                                        "FAVORITES" -> if (isHindi) "पसंदीदा" else "Favorites"
                                        "VIDEOS" -> if (isHindi) "वीडियो" else "Videos"
                                        else -> if (isHindi) "दस्तावेज़" else "Documents"
                                    },
                                    fontSize = 12.sp
                                )
                            }
                        )
                    }
                }
            }
        }
    ) { padding ->
        Box(
            modifier = Modifier
                .fillMaxSize()
                .padding(padding)
        ) {
            if (filteredPhotos.isEmpty()) {
                Column(
                    modifier = Modifier
                        .fillMaxSize()
                        .padding(32.dp),
                    horizontalAlignment = Alignment.CenterHorizontally,
                    verticalArrangement = Arrangement.Center
                ) {
                    Icon(
                        imageVector = Icons.Default.PhotoLibrary,
                        contentDescription = null,
                        modifier = Modifier.size(64.dp),
                        tint = MaterialTheme.colorScheme.outline
                    )
                    Spacer(modifier = Modifier.height(16.dp))
                    Text(
                        text = if (isHindi) "क्लाउड में कोई फोटो नहीं मिली" else "No photos found in Vault",
                        fontWeight = FontWeight.Bold,
                        fontSize = 16.sp
                    )
                    Text(
                        text = if (isHindi) "कैमरा बैकअप चालू करें या ड्राइव टैब से फाइल अपलोड करें"
                        else "Upload media from the Drive tab or beam via TeleDrop",
                        fontSize = 13.sp,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                }
            } else {
                LazyVerticalGrid(
                    columns = GridCells.Fixed(gridColumns),
                    state = gridState,
                    modifier = Modifier
                        .fillMaxSize()
                        .padding(horizontal = 4.dp),
                    horizontalArrangement = Arrangement.spacedBy(4.dp),
                    verticalArrangement = Arrangement.spacedBy(4.dp)
                ) {
                    items(
                        items = filteredPhotos,
                        key = { it.id },
                        contentType = { it.mimeType }
                    ) { file ->
                        val isSelected = selectedItemIds.contains(file.id)

                        Box(
                            modifier = Modifier
                                .fillMaxWidth()
                                .aspectRatio(if (aspectRatioMode == AspectRatioMode.SQUARE) 1f else 0.8f)
                                .clip(RoundedCornerShape(8.dp))
                                .background(MaterialTheme.colorScheme.surfaceVariant)
                                .border(
                                    width = if (isSelected) 2.5.dp else 0.dp,
                                    color = if (isSelected) CyanNeon else Color.Transparent,
                                    shape = RoundedCornerShape(8.dp)
                                )
                                .pointerInput(Unit) {
                                    detectDragGesturesAfterLongPress(
                                        onDragStart = {
                                            haptic.performHapticFeedback(HapticFeedbackType.LongPress)
                                            isSelectionMode = true
                                            if (!selectedItemIds.contains(file.id)) {
                                                selectedItemIds.add(file.id)
                                            }
                                        },
                                        onDrag = { _, _ -> }
                                    )
                                }
                                .clickable {
                                    if (isSelectionMode) {
                                        haptic.performHapticFeedback(HapticFeedbackType.TextHandleMove)
                                        if (isSelected) {
                                            selectedItemIds.remove(file.id)
                                            if (selectedItemIds.isEmpty()) isSelectionMode = false
                                        } else {
                                            selectedItemIds.add(file.id)
                                        }
                                    } else {
                                        onPhotoClick(file)
                                    }
                                }
                        ) {
                            // Two-tier thumbnail rendering
                            if (file.microThumbWebp != null && file.microThumbWebp.isNotEmpty()) {
                                val bitmap = remember(file.id) {
                                    BitmapFactory.decodeByteArray(file.microThumbWebp, 0, file.microThumbWebp.size)?.asImageBitmap()
                                }
                                if (bitmap != null) {
                                    Image(
                                        bitmap = bitmap,
                                        contentDescription = file.name,
                                        modifier = Modifier.fillMaxSize(),
                                        contentScale = ContentScale.Crop
                                    )
                                }
                            } else {
                                // Default gradient card with icon
                                Box(
                                    modifier = Modifier
                                        .fillMaxSize()
                                        .background(
                                            Brush.linearGradient(
                                                listOf(
                                                    Color(0xFF1E293B),
                                                    Color(0xFF0F172A)
                                                )
                                            )
                                        ),
                                    contentAlignment = Alignment.Center
                                ) {
                                    Icon(
                                        imageVector = if (file.mimeType.startsWith("video/")) Icons.Default.PlayCircle
                                        else Icons.Default.Image,
                                        contentDescription = null,
                                        tint = CyanNeon.copy(alpha = 0.7f),
                                        modifier = Modifier.size(28.dp)
                                    )
                                }
                            }

                            // Starred badge
                            if (file.isStarred) {
                                Icon(
                                    imageVector = Icons.Default.Star,
                                    contentDescription = "Starred",
                                    tint = Color(0xFFFBBF24),
                                    modifier = Modifier
                                        .align(Alignment.TopEnd)
                                        .padding(4.dp)
                                        .size(16.dp)
                                )
                            }

                            // Video duration overlay
                            if (file.mimeType.startsWith("video/")) {
                                Surface(
                                    color = Color.Black.copy(alpha = 0.6f),
                                    shape = RoundedCornerShape(4.dp),
                                    modifier = Modifier
                                        .align(Alignment.BottomEnd)
                                        .padding(4.dp)
                                ) {
                                    Text(
                                        text = "0:30",
                                        color = Color.White,
                                        fontSize = 10.sp,
                                        fontFamily = androidx.compose.ui.text.font.FontFamily.Monospace,
                                        modifier = Modifier.padding(horizontal = 4.dp, vertical = 2.dp)
                                    )
                                }
                            }

                            // Selection checkmark
                            if (isSelected) {
                                Box(
                                    modifier = Modifier
                                        .align(Alignment.TopStart)
                                        .padding(6.dp)
                                        .size(22.dp)
                                        .clip(CircleShape)
                                        .background(CyanNeon),
                                    contentAlignment = Alignment.Center
                                ) {
                                    Icon(
                                        Icons.Default.Check,
                                        contentDescription = "Selected",
                                        tint = Color.Black,
                                        modifier = Modifier.size(14.dp)
                                    )
                                }
                            }
                            // Quick Animated Download to Device / Gallery Button
                            Box(
                                modifier = Modifier
                                    .align(Alignment.BottomStart)
                                    .padding(4.dp)
                            ) {
                                com.example.ui.components.AnimatedCloudDownloadButton(
                                    onClick = { onSaveToGallery(file) },
                                    isCompact = true,
                                    testTag = "photo_download_${file.id}"
                                )
                            }
                        }
                    }
                }

                // Fast-Scroll Date Capsule Scrubber
                Surface(
                    color = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.9f),
                    shape = RoundedCornerShape(16.dp),
                    tonalElevation = 6.dp,
                    border = androidx.compose.foundation.BorderStroke(1.dp, CyanNeon.copy(alpha = 0.5f)),
                    modifier = Modifier
                        .align(Alignment.CenterEnd)
                        .padding(end = 12.dp)
                ) {
                    Text(
                        text = currentMonthYear,
                        fontSize = 11.sp,
                        fontWeight = FontWeight.Bold,
                        color = CyanNeon,
                        modifier = Modifier.padding(horizontal = 10.dp, vertical = 6.dp)
                    )
                }

                // Floating Multi-Download Action Bar
                if (isSelectionMode && selectedItemIds.isNotEmpty()) {
                    Surface(
                        modifier = Modifier
                            .align(Alignment.BottomCenter)
                            .padding(bottom = 24.dp)
                            .padding(horizontal = 20.dp),
                        shape = RoundedCornerShape(20.dp),
                        color = MaterialTheme.colorScheme.surface,
                        tonalElevation = 8.dp,
                        border = androidx.compose.foundation.BorderStroke(1.2.dp, CyanNeon)
                    ) {
                        Row(
                            modifier = Modifier.padding(horizontal = 16.dp, vertical = 10.dp),
                            verticalAlignment = Alignment.CenterVertically,
                            horizontalArrangement = Arrangement.spacedBy(12.dp)
                        ) {
                            Text(
                                text = "${selectedItemIds.size} ${if (isHindi) "चुने गए" else "selected"}",
                                fontWeight = FontWeight.Bold,
                                fontSize = 13.sp
                            )

                            com.example.ui.components.AnimatedCloudDownloadButton(
                                onClick = {
                                    val itemsToDownload = photos.filter { selectedItemIds.contains(it.id) }
                                    itemsToDownload.forEach { onSaveToGallery(it) }
                                    isSelectionMode = false
                                    selectedItemIds.clear()
                                },
                                label = if (isHindi) "गैलरी में सहेजें" else "Download to Gallery",
                                testTag = "batch_download_photos"
                            )

                            IconButton(
                                onClick = {
                                    isSelectionMode = false
                                    selectedItemIds.clear()
                                },
                                modifier = Modifier.size(32.dp)
                            ) {
                                Icon(Icons.Default.Close, contentDescription = "Cancel", modifier = Modifier.size(18.dp))
                            }
                        }
                    }
                }
            }
        }
    }
}
