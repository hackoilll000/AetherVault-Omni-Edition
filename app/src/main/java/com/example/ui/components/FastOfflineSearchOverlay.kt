package com.example.ui.components

import android.content.Context
import androidx.compose.animation.*
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.LazyRow
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
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.core.database.VaultFile
import com.example.features.studio.AppLanguage
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale

enum class SearchCategoryFilter(val labelEn: String, val labelHi: String) {
    ALL("All", "सभी"),
    IMAGES("Photos", "फ़ोटो"),
    VIDEOS("Videos", "वीडियो"),
    AUDIO("Audio", "ऑडियो"),
    DOCUMENTS("Documents", "दस्तावेज़"),
    ARCHIVES("Archives", "ज़िप/संग्रह")
}

enum class SearchSortOrder(val labelEn: String, val labelHi: String) {
    DATE_DESC("Newest", "नवीनतम"),
    DATE_ASC("Oldest", "पुरातन"),
    NAME_ASC("Name A-Z", "नाम क-ज्ञ"),
    SIZE_DESC("Largest", "सबसे बड़ा"),
    SIZE_ASC("Smallest", "सबसे छोटा")
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun FastOfflineSearchOverlay(
    searchQuery: String,
    files: List<VaultFile>,
    language: AppLanguage,
    onQueryChange: (String) -> Unit,
    onFileClick: (VaultFile) -> Unit,
    onClose: () -> Unit
) {
    val isHindi = language == AppLanguage.HI
    val context = LocalContext.current
    val searchPrefs = remember { context.getSharedPreferences("aethervault_search", Context.MODE_PRIVATE) }

    var selectedCategory by remember { mutableStateOf(SearchCategoryFilter.ALL) }
    var selectedSort by remember { mutableStateOf(SearchSortOrder.DATE_DESC) }
    var recentSearches by remember {
        mutableStateOf<List<String>>(
            searchPrefs.getStringSet("recent_queries", emptySet())?.toList()?.sorted() ?: emptyList()
        )
    }

    fun addRecentQuery(q: String) {
        val clean = q.trim()
        if (clean.length >= 2) {
            val updated = (listOf(clean) + recentSearches.filter { it != clean }).take(8)
            searchPrefs.edit().putStringSet("recent_queries", updated.toSet()).apply()
            recentSearches = updated
        }
    }

    fun clearRecentQueries() {
        searchPrefs.edit().remove("recent_queries").apply()
        recentSearches = emptyList<String>()
    }

    // Fast local filtering and sorting
    val filteredFiles = remember(searchQuery, files, selectedCategory, selectedSort) {
        val q = searchQuery.trim().lowercase()
        var result = files

        if (q.isNotEmpty()) {
            result = result.filter { f ->
                f.name.lowercase().contains(q) ||
                f.extension.lowercase().contains(q) ||
                f.mimeType.lowercase().contains(q)
            }
        }

        result = when (selectedCategory) {
            SearchCategoryFilter.ALL -> result
            SearchCategoryFilter.IMAGES -> result.filter { it.mimeType.startsWith("image/") || it.extension.lowercase() in listOf("jpg", "jpeg", "png", "webp", "gif") }
            SearchCategoryFilter.VIDEOS -> result.filter { it.mimeType.startsWith("video/") || it.extension.lowercase() in listOf("mp4", "mkv", "mov", "avi") }
            SearchCategoryFilter.AUDIO -> result.filter { it.mimeType.startsWith("audio/") || it.extension.lowercase() in listOf("mp3", "m4a", "wav", "flac") }
            SearchCategoryFilter.DOCUMENTS -> result.filter { it.mimeType.startsWith("text/") || it.extension.lowercase() in listOf("pdf", "doc", "docx", "txt", "md", "xls", "ppt") }
            SearchCategoryFilter.ARCHIVES -> result.filter { it.extension.lowercase() in listOf("zip", "rar", "7z", "tar", "gz") }
        }

        when (selectedSort) {
            SearchSortOrder.DATE_DESC -> result.sortedByDescending { it.modifiedAt }
            SearchSortOrder.DATE_ASC -> result.sortedBy { it.modifiedAt }
            SearchSortOrder.NAME_ASC -> result.sortedBy { it.name.lowercase() }
            SearchSortOrder.SIZE_DESC -> result.sortedByDescending { it.totalSizeBytes }
            SearchSortOrder.SIZE_ASC -> result.sortedBy { it.totalSizeBytes }
        }
    }

    Surface(
        modifier = Modifier
            .fillMaxSize()
            .testTag("fast_offline_search_overlay"),
        color = MaterialTheme.colorScheme.background
    ) {
        Column(modifier = Modifier.fillMaxSize()) {
            // Search Input Header
            Surface(
                color = MaterialTheme.colorScheme.surface,
                tonalElevation = 3.dp,
                modifier = Modifier.fillMaxWidth()
            ) {
                Column(modifier = Modifier.padding(horizontal = 16.dp, vertical = 8.dp)) {
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        IconButton(onClick = onClose) {
                            Icon(Icons.Default.ArrowBack, contentDescription = "Back")
                        }
                        TextField(
                            value = searchQuery,
                            onValueChange = onQueryChange,
                            placeholder = { Text(if (isHindi) "वॉल्ट में तुरंत खोजें..." else "Fast offline search...") },
                            modifier = Modifier
                                .weight(1f)
                                .testTag("search_text_input"),
                            singleLine = true,
                            colors = TextFieldDefaults.colors(
                                focusedContainerColor = Color.Transparent,
                                unfocusedContainerColor = Color.Transparent,
                                focusedIndicatorColor = Color.Transparent,
                                unfocusedIndicatorColor = Color.Transparent
                            )
                        )
                        if (searchQuery.isNotEmpty()) {
                            IconButton(onClick = { onQueryChange("") }) {
                                Icon(Icons.Default.Close, contentDescription = "Clear")
                            }
                        }
                    }

                    // Category Filter Chips
                    LazyRow(
                        horizontalArrangement = Arrangement.spacedBy(8.dp),
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(vertical = 4.dp)
                    ) {
                        items(SearchCategoryFilter.entries.toTypedArray()) { cat ->
                            FilterChip(
                                selected = selectedCategory == cat,
                                onClick = { selectedCategory = cat },
                                label = { Text(if (isHindi) cat.labelHi else cat.labelEn, fontSize = 12.sp) }
                            )
                        }
                    }
                }
            }

            // Recent Searches Chips (if query is empty)
            if (searchQuery.isBlank() && recentSearches.isNotEmpty()) {
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(horizontal = 16.dp, vertical = 8.dp),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Text(
                        text = if (isHindi) "हाल की खोजें" else "Recent searches",
                        fontSize = 12.sp,
                        fontWeight = FontWeight.SemiBold,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                    TextButton(onClick = { clearRecentQueries() }) {
                        Text(if (isHindi) "मिटाएं" else "Clear", fontSize = 11.sp)
                    }
                }

                LazyRow(
                    contentPadding = PaddingValues(horizontal = 16.dp),
                    horizontalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    items(recentSearches) { pastQuery: String ->
                        SuggestionChip(
                            onClick = {
                                onQueryChange(pastQuery)
                                addRecentQuery(pastQuery)
                            },
                            label = { Text(pastQuery, fontSize = 12.sp) },
                            icon = { Icon(Icons.Default.History, contentDescription = null, modifier = Modifier.size(14.dp)) }
                        )
                    }
                }
            }

            // Results count and sort row
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(horizontal = 16.dp, vertical = 8.dp),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Text(
                    text = if (isHindi) "${filteredFiles.size} परिणाम मिले" else "${filteredFiles.size} files found",
                    fontSize = 12.sp,
                    fontWeight = FontWeight.Bold,
                    color = MaterialTheme.colorScheme.primary
                )

                // Sort Order Selector
                LazyRow(horizontalArrangement = Arrangement.spacedBy(6.dp)) {
                    items(SearchSortOrder.entries.toTypedArray()) { sortOpt ->
                        FilterChip(
                            selected = selectedSort == sortOpt,
                            onClick = { selectedSort = sortOpt },
                            label = { Text(if (isHindi) sortOpt.labelHi else sortOpt.labelEn, fontSize = 11.sp) }
                        )
                    }
                }
            }

            HorizontalDivider(color = MaterialTheme.colorScheme.outlineVariant.copy(alpha = 0.5f))

            // File Results List
            if (filteredFiles.isEmpty()) {
                Box(
                    modifier = Modifier
                        .fillMaxSize()
                        .padding(32.dp),
                    contentAlignment = Alignment.Center
                ) {
                    Column(horizontalAlignment = Alignment.CenterHorizontally) {
                        Icon(
                            Icons.Default.SearchOff,
                            contentDescription = null,
                            modifier = Modifier.size(48.dp),
                            tint = MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.5f)
                        )
                        Spacer(modifier = Modifier.height(12.dp))
                        Text(
                            text = if (isHindi) "कोई फ़ाइल नहीं मिली" else "No matching files found",
                            fontSize = 14.sp,
                            fontWeight = FontWeight.Medium,
                            color = MaterialTheme.colorScheme.onSurfaceVariant
                        )
                    }
                }
            } else {
                LazyColumn(
                    modifier = Modifier.fillMaxSize(),
                    contentPadding = PaddingValues(16.dp),
                    verticalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    items(filteredFiles, key = { it.id }) { file ->
                        Surface(
                            modifier = Modifier
                                .fillMaxWidth()
                                .clip(RoundedCornerShape(12.dp))
                                .clickable {
                                    addRecentQuery(searchQuery)
                                    onFileClick(file)
                                },
                            color = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.6f)
                        ) {
                            Row(
                                modifier = Modifier.padding(12.dp),
                                verticalAlignment = Alignment.CenterVertically
                            ) {
                                Box(
                                    modifier = Modifier
                                        .size(40.dp)
                                        .clip(CircleShape)
                                        .background(MaterialTheme.colorScheme.primaryContainer),
                                    contentAlignment = Alignment.Center
                                ) {
                                    Icon(
                                        imageVector = when {
                                            file.mimeType.startsWith("image/") -> Icons.Default.Image
                                            file.mimeType.startsWith("video/") -> Icons.Default.Movie
                                            file.mimeType.startsWith("audio/") -> Icons.Default.MusicNote
                                            file.extension.lowercase() in listOf("pdf", "doc", "docx", "txt") -> Icons.Default.Description
                                            file.extension.lowercase() in listOf("zip", "rar", "7z") -> Icons.Default.FolderZip
                                            else -> Icons.Default.InsertDriveFile
                                        },
                                        contentDescription = null,
                                        tint = MaterialTheme.colorScheme.onPrimaryContainer,
                                        modifier = Modifier.size(20.dp)
                                    )
                                }

                                Spacer(modifier = Modifier.width(12.dp))

                                Column(modifier = Modifier.weight(1f)) {
                                    Text(
                                        text = file.name,
                                        fontWeight = FontWeight.SemiBold,
                                        fontSize = 14.sp,
                                        color = MaterialTheme.colorScheme.onSurface
                                    )
                                    Spacer(modifier = Modifier.height(2.dp))
                                    Row(verticalAlignment = Alignment.CenterVertically) {
                                        Text(
                                            text = "${file.totalSizeBytes / (1024 * 1024)} MB",
                                            fontSize = 11.sp,
                                            fontWeight = FontWeight.Medium,
                                            color = MaterialTheme.colorScheme.primary
                                        )
                                        Text(
                                            text = " • ",
                                            fontSize = 11.sp,
                                            color = MaterialTheme.colorScheme.onSurfaceVariant
                                        )
                                        val df = remember { SimpleDateFormat("dd MMM yyyy", Locale.getDefault()) }
                                        Text(
                                            text = df.format(Date(file.modifiedAt)),
                                            fontSize = 11.sp,
                                            color = MaterialTheme.colorScheme.onSurfaceVariant
                                        )
                                    }
                                }

                                Icon(
                                    Icons.Default.ChevronRight,
                                    contentDescription = null,
                                    tint = MaterialTheme.colorScheme.onSurfaceVariant
                                )
                            }
                        }
                    }
                }
            }
        }
    }
}
