package com.example.ui.components

import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
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
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.ui.window.Dialog
import androidx.compose.ui.window.DialogProperties
import com.example.features.studio.AppLanguage

data class CommandActionItem(
    val id: String,
    val titleEn: String,
    val titleHi: String,
    val subtitleEn: String,
    val subtitleHi: String,
    val icon: ImageVector,
    val category: String
)

@Composable
fun CommandPaletteDialog(
    language: AppLanguage,
    onDismiss: () -> Unit,
    onExecuteAction: (actionId: String) -> Unit
) {
    val isHindi = language == AppLanguage.HI
    var query by remember { mutableStateOf("") }

    val allActions = remember {
        listOf(
            CommandActionItem(
                id = "upload_file",
                titleEn = "Upload File",
                titleHi = "फ़ाइल अपलोड करें",
                subtitleEn = "Select any document, archive, or binary from storage",
                subtitleHi = "डिवाइस से कोई भी डॉक्यूमेंट या फ़ाइल चुनें",
                icon = Icons.Default.UploadFile,
                category = "Files"
            ),
            CommandActionItem(
                id = "upload_media",
                titleEn = "Upload Media & Photos",
                titleHi = "फ़ोटो व वीडियो अपलोड करें",
                subtitleEn = "Add pictures or videos directly to Telegram",
                subtitleHi = "तस्वीरें या वीडियो सीधे टेलीग्राम में जोड़ें",
                icon = Icons.Default.AddPhotoAlternate,
                category = "Media"
            ),
            CommandActionItem(
                id = "new_folder",
                titleEn = "Create New Folder",
                titleHi = "नया फ़ोल्डर बनाएँ",
                subtitleEn = "Organize files into a virtual directory",
                subtitleHi = "फ़ाइलों को व्यवस्थित करने के लिए फ़ोल्डर बनाएँ",
                icon = Icons.Default.CreateNewFolder,
                category = "Files"
            ),
            CommandActionItem(
                id = "new_note",
                titleEn = "Create New Text Document",
                titleHi = "नया टेक्स्ट दस्तावेज़ बनाएँ",
                subtitleEn = "Quick encrypted note directly in cloud",
                subtitleHi = "क्लाउड में तुरंत सुरक्षित नोट लिखें",
                icon = Icons.Default.NoteAdd,
                category = "Create"
            ),
            CommandActionItem(
                id = "storage_metrics",
                titleEn = "Storage Metrics & Analytics",
                titleHi = "स्टोरेज विश्लेषण और मैट्रिक्स",
                subtitleEn = "View vault breakdowns, largest files & duplicates",
                subtitleHi = "फ़ाइल विभाजन, सबसे बड़ी फ़ाइलें और डुप्लिकेट देखें",
                icon = Icons.Default.PieChart,
                category = "Diagnostics"
            ),
            CommandActionItem(
                id = "search_vault",
                titleEn = "Search Vault Offline",
                titleHi = "वॉल्ट में खोजें",
                subtitleEn = "Instant local metadata and filename search",
                subtitleHi = "फ़ाइल नाम और एक्सटेंशन से तुरंत खोजें",
                icon = Icons.Default.Search,
                category = "Navigation"
            ),
            CommandActionItem(
                id = "toggle_theme",
                titleEn = "Switch Theme Mode",
                titleHi = "थीम मोड बदलें",
                subtitleEn = "Cycle between System Auto, Light, and Dark",
                subtitleHi = "सिस्टम ऑटो, लाइट और डार्क मोड में बदलें",
                icon = Icons.Default.DarkMode,
                category = "Appearance"
            ),
            CommandActionItem(
                id = "sync_channel",
                titleEn = "Synchronize Telegram Server",
                titleHi = "टेलीग्राम सर्वर सिंक करें",
                subtitleEn = "Scan channel for historical files and updates",
                subtitleHi = "पुराने संदेशों व फ़ाइलों को रिस्टोर करें",
                icon = Icons.Default.Sync,
                category = "Cloud"
            ),
            CommandActionItem(
                id = "lock_now",
                titleEn = "Lock Vault Now",
                titleHi = "वॉल्ट अभी लॉक करें",
                subtitleEn = "Secure session immediately and require password",
                subtitleHi = "सत्र को सुरक्षित करें और पासवर्ड मांगें",
                icon = Icons.Default.Lock,
                category = "Security"
            )
        )
    }

    val filteredActions = remember(query) {
        if (query.isBlank()) allActions
        else {
            val q = query.trim().lowercase()
            allActions.filter {
                it.titleEn.lowercase().contains(q) ||
                it.titleHi.lowercase().contains(q) ||
                it.subtitleEn.lowercase().contains(q) ||
                it.category.lowercase().contains(q)
            }
        }
    }

    Dialog(
        onDismissRequest = onDismiss,
        properties = DialogProperties(usePlatformDefaultWidth = false)
    ) {
        Surface(
            modifier = Modifier
                .fillMaxWidth(0.92f)
                .fillMaxHeight(0.75f)
                .testTag("command_palette_dialog"),
            shape = RoundedCornerShape(24.dp),
            color = MaterialTheme.colorScheme.surface,
            tonalElevation = 8.dp
        ) {
            Column(
                modifier = Modifier
                    .fillMaxSize()
                    .padding(16.dp)
            ) {
                // Search Input Header
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Icon(
                        imageVector = Icons.Default.Terminal,
                        contentDescription = null,
                        tint = MaterialTheme.colorScheme.primary,
                        modifier = Modifier.size(24.dp)
                    )
                    Spacer(modifier = Modifier.width(10.dp))
                    TextField(
                        value = query,
                        onValueChange = { query = it },
                        placeholder = { Text(if (isHindi) "त्वरित कमांड खोजें..." else "Type a command or action...") },
                        modifier = Modifier
                            .weight(1f)
                            .testTag("command_palette_input"),
                        singleLine = true,
                        colors = TextFieldDefaults.colors(
                            focusedContainerColor = androidx.compose.ui.graphics.Color.Transparent,
                            unfocusedContainerColor = androidx.compose.ui.graphics.Color.Transparent,
                            focusedIndicatorColor = androidx.compose.ui.graphics.Color.Transparent,
                            unfocusedIndicatorColor = androidx.compose.ui.graphics.Color.Transparent
                        )
                    )
                    IconButton(onClick = onDismiss) {
                        Icon(Icons.Default.Close, contentDescription = "Close")
                    }
                }

                HorizontalDivider(modifier = Modifier.padding(vertical = 8.dp))

                LazyColumn(
                    modifier = Modifier
                        .fillMaxWidth()
                        .weight(1f),
                    verticalArrangement = Arrangement.spacedBy(6.dp)
                ) {
                    items(filteredActions, key = { it.id }) { action ->
                        Surface(
                            modifier = Modifier
                                .fillMaxWidth()
                                .clip(RoundedCornerShape(12.dp))
                                .clickable {
                                    onDismiss()
                                    onExecuteAction(action.id)
                                },
                            color = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.5f),
                            shape = RoundedCornerShape(12.dp)
                        ) {
                            Row(
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .padding(12.dp),
                                verticalAlignment = Alignment.CenterVertically
                            ) {
                                Box(
                                    modifier = Modifier
                                        .size(36.dp)
                                        .clip(CircleShape)
                                        .background(MaterialTheme.colorScheme.primaryContainer),
                                    contentAlignment = Alignment.Center
                                ) {
                                    Icon(
                                        imageVector = action.icon,
                                        contentDescription = null,
                                        tint = MaterialTheme.colorScheme.onPrimaryContainer,
                                        modifier = Modifier.size(18.dp)
                                    )
                                }
                                Spacer(modifier = Modifier.width(12.dp))
                                Column(modifier = Modifier.weight(1f)) {
                                    Text(
                                        text = if (isHindi) action.titleHi else action.titleEn,
                                        fontSize = 13.sp,
                                        fontWeight = FontWeight.SemiBold,
                                        color = MaterialTheme.colorScheme.onSurface
                                    )
                                    Text(
                                        text = if (isHindi) action.subtitleHi else action.subtitleEn,
                                        fontSize = 11.sp,
                                        color = MaterialTheme.colorScheme.onSurfaceVariant
                                    )
                                }
                                Surface(
                                    color = MaterialTheme.colorScheme.surface,
                                    shape = RoundedCornerShape(6.dp)
                                ) {
                                    Text(
                                        text = action.category,
                                        fontSize = 10.sp,
                                        color = MaterialTheme.colorScheme.primary,
                                        fontWeight = FontWeight.Medium,
                                        modifier = Modifier.padding(horizontal = 6.dp, vertical = 2.dp)
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
