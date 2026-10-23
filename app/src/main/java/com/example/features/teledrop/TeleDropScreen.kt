package com.example.features.teledrop

import android.content.ClipData
import android.content.ClipboardManager
import android.content.Context
import androidx.compose.animation.*
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.*
import androidx.compose.material.icons.outlined.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.LocalHapticFeedback
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.hapticfeedback.HapticFeedbackType
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.core.database.TeleDropItem
import com.example.features.studio.AppLanguage
import com.example.ui.theme.*
import java.text.SimpleDateFormat
import java.util.*

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun TeleDropScreen(
    items: List<TeleDropItem>,
    language: AppLanguage,
    onSendSnippet: (content: String, type: String) -> Unit,
    onDeleteItem: (String) -> Unit,
    onSyncChannel: (() -> Unit)? = null,
    onAttachFile: (() -> Unit)? = null,
    isSyncing: Boolean = false,
    channelId: Long = 0L,
    botHealthy: Boolean = true
) {
    val context = LocalContext.current
    val haptic = LocalHapticFeedback.current
    val isHindi = language == AppLanguage.HI

    var inputSnippetText by remember { mutableStateOf("") }
    var selectedFilter by remember { mutableStateOf("ALL") }
    var copiedNotice by remember { mutableStateOf<String?>(null) }
    var showDisclaimerDetails by remember { mutableStateOf(false) }

    val filteredItems = remember(items, selectedFilter) {
        when (selectedFilter) {
            "TEXT" -> items.filter { it.type == "TEXT" }
            "LINK_CODE" -> items.filter { it.type == "LINK" || it.type == "CODE" }
            "CHANNEL" -> items.filter { it.sourceDevice.contains("Telegram", ignoreCase = true) }
            else -> items
        }
    }

    Scaffold(
        containerColor = M3Background,
        topBar = {
            Surface(
                color = M3Surface,
                tonalElevation = 2.dp,
                modifier = Modifier.fillMaxWidth()
            ) {
                Column(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(horizontal = 16.dp, vertical = 12.dp)
                ) {
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Row(verticalAlignment = Alignment.CenterVertically) {
                            Box(
                                modifier = Modifier
                                    .size(36.dp)
                                    .clip(CircleShape)
                                    .background(M3ActivePill),
                                contentAlignment = Alignment.Center
                            ) {
                                Icon(
                                    imageVector = Icons.Default.SendToMobile,
                                    contentDescription = null,
                                    tint = M3ActivePillText,
                                    modifier = Modifier.size(20.dp)
                                )
                            }
                            Spacer(modifier = Modifier.width(12.dp))
                            Column {
                                Text(
                                    text = if (isHindi) "टेलीग्राम चैट और क्लाउड सिंक" else "Telegram Chat & Cloud Sync",
                                    fontWeight = FontWeight.Bold,
                                    fontSize = 17.sp,
                                    color = M3TextPrimary
                                )
                                Row(verticalAlignment = Alignment.CenterVertically) {
                                    Box(
                                        modifier = Modifier
                                            .size(7.dp)
                                            .clip(CircleShape)
                                            .background(if (botHealthy) Color(0xFF1B873F) else Color(0xFFD93025))
                                    )
                                    Spacer(modifier = Modifier.width(5.dp))
                                    Text(
                                        text = if (channelId != 0L) "Channel: #$channelId" else "Bot Swarm Active",
                                        fontSize = 12.sp,
                                        color = M3TextMuted
                                    )
                                }
                            }
                        }

                        // Sync Telegram History Button
                        if (onSyncChannel != null) {
                            FilledTonalButton(
                                onClick = {
                                    haptic.performHapticFeedback(HapticFeedbackType.LongPress)
                                    onSyncChannel()
                                },
                                enabled = !isSyncing,
                                colors = ButtonDefaults.filledTonalButtonColors(
                                    containerColor = M3ActivePill,
                                    contentColor = M3ActivePillText
                                ),
                                contentPadding = PaddingValues(horizontal = 12.dp, vertical = 6.dp),
                                shape = RoundedCornerShape(9999.dp),
                                modifier = Modifier.testTag("sync_telegram_history_button")
                            ) {
                                if (isSyncing) {
                                    CircularProgressIndicator(
                                        modifier = Modifier.size(16.dp),
                                        strokeWidth = 2.dp,
                                        color = M3ActivePillText
                                    )
                                } else {
                                    Icon(
                                        imageVector = Icons.Default.Sync,
                                        contentDescription = "Sync",
                                        modifier = Modifier.size(16.dp)
                                    )
                                }
                                Spacer(modifier = Modifier.width(6.dp))
                                Text(
                                    text = if (isHindi) "सिंक करें" else "Sync History",
                                    fontSize = 12.sp,
                                    fontWeight = FontWeight.SemiBold
                                )
                            }
                        }
                    }

                    Spacer(modifier = Modifier.height(10.dp))

                    // Filter Chips Row
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.spacedBy(8.dp)
                    ) {
                        FilterChip(
                            selected = selectedFilter == "ALL",
                            onClick = { selectedFilter = "ALL" },
                            label = { Text(if (isHindi) "सभी (${items.size})" else "All (${items.size})", fontSize = 12.sp) },
                            shape = RoundedCornerShape(9999.dp)
                        )
                        FilterChip(
                            selected = selectedFilter == "TEXT",
                            onClick = { selectedFilter = "TEXT" },
                            label = { Text(if (isHindi) "चैट / टेक्स्ट" else "Chat & Text", fontSize = 12.sp) },
                            shape = RoundedCornerShape(9999.dp)
                        )
                        FilterChip(
                            selected = selectedFilter == "LINK_CODE",
                            onClick = { selectedFilter = "LINK_CODE" },
                            label = { Text("Links & Code", fontSize = 12.sp) },
                            shape = RoundedCornerShape(9999.dp)
                        )
                        FilterChip(
                            selected = selectedFilter == "CHANNEL",
                            onClick = { selectedFilter = "CHANNEL" },
                            label = { Text("Channel Posts", fontSize = 12.sp) },
                            shape = RoundedCornerShape(9999.dp)
                        )
                    }
                }
            }
        },
        bottomBar = {
            Surface(
                color = M3Surface,
                tonalElevation = 6.dp,
                modifier = Modifier
                    .fillMaxWidth()
                    .navigationBarsPadding()
            ) {
                Row(
                    modifier = Modifier
                        .padding(horizontal = 12.dp, vertical = 8.dp)
                        .fillMaxWidth(),
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    // Dedicated Attachment '+' Action Button
                    if (onAttachFile != null) {
                        IconButton(
                            onClick = onAttachFile,
                            modifier = Modifier
                                .size(40.dp)
                                .clip(CircleShape)
                                .background(M3SearchBarBg)
                                .testTag("teledrop_attach_button")
                        ) {
                            Icon(
                                imageVector = Icons.Default.Add,
                                contentDescription = "Attach File",
                                tint = M3PrimaryBlue
                            )
                        }
                    }

                    // Quick paste from clipboard
                    IconButton(
                        onClick = {
                            val clipMgr = context.getSystemService(Context.CLIPBOARD_SERVICE) as ClipboardManager
                            val clip = clipMgr.primaryClip
                            if (clip != null && clip.itemCount > 0) {
                                val text = clip.getItemAt(0).text?.toString() ?: ""
                                if (text.isNotBlank()) {
                                    inputSnippetText = text
                                    haptic.performHapticFeedback(HapticFeedbackType.LongPress)
                                }
                            }
                        },
                        modifier = Modifier.size(40.dp)
                    ) {
                        Icon(
                            imageVector = Icons.Default.ContentPaste,
                            contentDescription = "Paste Clipboard",
                            tint = M3TextMuted
                        )
                    }

                    // Message / Link Input Field
                    OutlinedTextField(
                        value = inputSnippetText,
                        onValueChange = { inputSnippetText = it },
                        placeholder = {
                            Text(
                                if (isHindi) "संदेश या लिंक दर्ज करें..."
                                else "Send message or link...",
                                fontSize = 13.sp,
                                color = M3TextMuted
                            )
                        },
                        modifier = Modifier
                            .weight(1f)
                            .testTag("teledrop_input"),
                        shape = RoundedCornerShape(24.dp),
                        maxLines = 3,
                        colors = OutlinedTextFieldDefaults.colors(
                            focusedContainerColor = M3SearchBarBg,
                            unfocusedContainerColor = M3SearchBarBg,
                            focusedBorderColor = M3PrimaryBlue,
                            unfocusedBorderColor = Color.Transparent
                        )
                    )

                    // Send Button
                    IconButton(
                        onClick = {
                            if (inputSnippetText.isNotBlank()) {
                                val type = when {
                                    inputSnippetText.startsWith("http://") || inputSnippetText.startsWith("https://") -> "LINK"
                                    inputSnippetText.contains("\n") || inputSnippetText.contains("{") -> "CODE"
                                    else -> "TEXT"
                                }
                                onSendSnippet(inputSnippetText.trim(), type)
                                inputSnippetText = ""
                                haptic.performHapticFeedback(HapticFeedbackType.LongPress)
                            }
                        },
                        modifier = Modifier
                            .size(44.dp)
                            .clip(CircleShape)
                            .background(M3PrimaryBlue)
                            .testTag("teledrop_send_button")
                    ) {
                        Icon(
                            Icons.Default.Send,
                            contentDescription = "Send",
                            tint = Color.White,
                            modifier = Modifier.size(18.dp)
                        )
                    }
                }
            }
        }
    ) { padding ->
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(padding)
                .padding(horizontal = 16.dp)
        ) {
            Spacer(modifier = Modifier.height(8.dp))

            // Prominent Legal Disclaimer Banner
            Card(
                modifier = Modifier
                    .fillMaxWidth()
                    .clickable { showDisclaimerDetails = !showDisclaimerDetails },
                colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.tertiaryContainer),
                shape = RoundedCornerShape(12.dp),
                border = androidx.compose.foundation.BorderStroke(1.dp, MaterialTheme.colorScheme.outlineVariant)
            ) {
                Column(modifier = Modifier.padding(12.dp)) {
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Row(
                            verticalAlignment = Alignment.CenterVertically,
                            modifier = Modifier.weight(1f)
                        ) {
                            Icon(
                                imageVector = Icons.Default.Warning,
                                contentDescription = null,
                                tint = MaterialTheme.colorScheme.onTertiaryContainer,
                                modifier = Modifier.size(18.dp)
                            )
                            Spacer(modifier = Modifier.width(8.dp))
                            Text(
                                text = if (isHindi) "महत्वपूर्ण कानूनी डिस्क्लेमर (कानूनी नोटिस)" else "Legal Disclaimer & Compliance Notice",
                                fontSize = 12.sp,
                                fontWeight = FontWeight.Bold,
                                color = MaterialTheme.colorScheme.onTertiaryContainer
                            )
                        }
                        Icon(
                            imageVector = if (showDisclaimerDetails) Icons.Default.ExpandLess else Icons.Default.ExpandMore,
                            contentDescription = "Toggle",
                            tint = MaterialTheme.colorScheme.onTertiaryContainer,
                            modifier = Modifier.size(20.dp)
                        )
                    }

                    Text(
                        text = if (isHindi) "यह ऐप सिर्फ पर्सनल यूज़ और बॉट ऑपरेशन डेमो के लिए है। किसी भी दुरुपयोग की जिम्मेदारी यूजर की है।"
                        else "Strictly for educational demonstration and personal use. Developers assume no liability for misuse.",
                        fontSize = 11.sp,
                        color = MaterialTheme.colorScheme.onTertiaryContainer,
                        lineHeight = 15.sp,
                        modifier = Modifier.padding(top = 4.dp)
                    )

                    if (showDisclaimerDetails) {
                        Spacer(modifier = Modifier.height(6.dp))
                        HorizontalDivider(color = MaterialTheme.colorScheme.outlineVariant, thickness = 0.8.dp)
                        Spacer(modifier = Modifier.height(6.dp))
                        Text(
                            text = if (isHindi) """
• हमने यह ऐप सिर्फ और सिर्फ फन-फन में सीखने के लिए बनाया है कि मोबाइल से सर्वर बॉट कैसे ऑपरेट कर सकते हैं।
• हम किसी भी प्रकार से टेलीग्राम (Telegram) की नीतियों या गाइडलाइंस का उल्लंघन नहीं करते हैं।
• इस ऐप को यूज़ करना पूर्णतः आपकी जिम्मेदारी है; डेवलपर्स (Ayush Mondal / AJPM / TRB FireOps) किसी भी गलत इस्तेमाल के लिए उत्तरदायी नहीं हैं।
                            """.trimIndent()
                            else """
• Built strictly as an architectural experiment demonstrating how to operate serverless bots from a mobile device.
• Does not violate, bypass, or advocate breach of Telegram Terms of Service or API policies.
• End users bear sole responsibility for all activities; developers accept zero liability.
                            """.trimIndent(),
                            fontSize = 10.5.sp,
                            color = MaterialTheme.colorScheme.onTertiaryContainer.copy(alpha = 0.85f),
                            lineHeight = 15.sp
                        )
                    }
                }
            }

            if (copiedNotice != null) {
                Spacer(modifier = Modifier.height(6.dp))
                Surface(
                    color = M3ActivePill,
                    shape = RoundedCornerShape(8.dp),
                    modifier = Modifier.fillMaxWidth()
                ) {
                    Text(
                        text = copiedNotice ?: "",
                        fontSize = 12.sp,
                        fontWeight = FontWeight.SemiBold,
                        color = M3ActivePillText,
                        modifier = Modifier.padding(horizontal = 12.dp, vertical = 6.dp)
                    )
                }
            }

            Spacer(modifier = Modifier.height(8.dp))

            if (filteredItems.isEmpty()) {
                Box(
                    modifier = Modifier
                        .fillMaxSize()
                        .weight(1f),
                    contentAlignment = Alignment.Center
                ) {
                    Column(
                        horizontalAlignment = Alignment.CenterHorizontally,
                        modifier = Modifier.padding(24.dp)
                    ) {
                        Box(
                            modifier = Modifier
                                .size(64.dp)
                                .clip(CircleShape)
                                .background(M3TonalSurface),
                            contentAlignment = Alignment.Center
                        ) {
                            Icon(
                                Icons.Outlined.Forum,
                                contentDescription = null,
                                tint = M3PrimaryBlue,
                                modifier = Modifier.size(32.dp)
                            )
                        }
                        Spacer(modifier = Modifier.height(14.dp))
                        Text(
                            text = if (isHindi) "कोई टेलीग्राम संदेश या सिंक आइटम नहीं" else "No Telegram Messages or Synced Items",
                            fontWeight = FontWeight.SemiBold,
                            fontSize = 16.sp,
                            color = M3TextPrimary
                        )
                        Spacer(modifier = Modifier.height(4.dp))
                        Text(
                            text = if (isHindi) "अपने चैनल की पुरानी चैट और फाइलों को सिंक करने के लिए नीचे टैप करें"
                            else "Tap below to synchronize past channel messages, texts, and posts via Bot",
                            fontSize = 12.sp,
                            color = M3TextMuted,
                            lineHeight = 16.sp
                        )

                        if (onSyncChannel != null) {
                            Spacer(modifier = Modifier.height(16.dp))
                            Button(
                                onClick = onSyncChannel,
                                enabled = !isSyncing,
                                colors = ButtonDefaults.buttonColors(
                                    containerColor = M3PrimaryBlue,
                                    contentColor = Color.White
                                ),
                                shape = RoundedCornerShape(9999.dp)
                            ) {
                                Icon(Icons.Default.CloudSync, contentDescription = null, modifier = Modifier.size(18.dp))
                                Spacer(modifier = Modifier.width(8.dp))
                                Text(
                                    text = if (isHindi) "टेलीग्राम हिस्ट्री सिंक करें" else "Synchronize Telegram History",
                                    fontSize = 13.sp,
                                    fontWeight = FontWeight.SemiBold
                                )
                            }
                        }
                    }
                }
            } else {
                LazyColumn(
                    modifier = Modifier
                        .fillMaxSize()
                        .weight(1f),
                    verticalArrangement = Arrangement.spacedBy(8.dp),
                    contentPadding = PaddingValues(vertical = 8.dp)
                ) {
                    items(filteredItems, key = { it.itemId }) { item ->
                        val isChannelPost = item.sourceDevice.contains("Telegram", ignoreCase = true)
                        val isPcBeam = item.sourceDevice.contains("PC", ignoreCase = true)

                        Card(
                            modifier = Modifier.fillMaxWidth(),
                            colors = CardDefaults.cardColors(
                                containerColor = if (isChannelPost) M3TonalSurface else M3Surface
                            ),
                            shape = RoundedCornerShape(14.dp),
                            border = androidx.compose.foundation.BorderStroke(
                                1.dp,
                                if (isChannelPost) M3ActivePill.copy(alpha = 0.5f) else M3BorderSubtle
                            )
                        ) {
                            Column(modifier = Modifier.padding(14.dp)) {
                                Row(
                                    modifier = Modifier.fillMaxWidth(),
                                    horizontalArrangement = Arrangement.SpaceBetween,
                                    verticalAlignment = Alignment.CenterVertically
                                ) {
                                    Row(verticalAlignment = Alignment.CenterVertically) {
                                        Box(
                                            modifier = Modifier
                                                .size(24.dp)
                                                .clip(CircleShape)
                                                .background(
                                                    when {
                                                        isChannelPost -> Color(0xFF2AABEE)
                                                        isPcBeam -> Color(0xFF673AB7)
                                                        else -> M3PrimaryBlue
                                                    }
                                                ),
                                            contentAlignment = Alignment.Center
                                        ) {
                                            Icon(
                                                imageVector = when {
                                                    isChannelPost -> Icons.Default.Cloud
                                                    isPcBeam -> Icons.Default.Computer
                                                    else -> Icons.Default.Smartphone
                                                },
                                                contentDescription = null,
                                                tint = Color.White,
                                                modifier = Modifier.size(13.dp)
                                            )
                                        }
                                        Spacer(modifier = Modifier.width(8.dp))
                                        Text(
                                            text = item.sourceDevice,
                                            fontWeight = FontWeight.SemiBold,
                                            fontSize = 12.sp,
                                            color = M3TextPrimary
                                        )
                                        Spacer(modifier = Modifier.width(8.dp))
                                        Text(
                                            text = SimpleDateFormat("MMM d, HH:mm", Locale.getDefault()).format(Date(item.createdAt)),
                                            fontSize = 11.sp,
                                            color = M3TextMuted
                                        )
                                    }

                                    Row {
                                        // 1-Tap Copy Button
                                        IconButton(
                                            onClick = {
                                                val clipMgr = context.getSystemService(Context.CLIPBOARD_SERVICE) as ClipboardManager
                                                clipMgr.setPrimaryClip(ClipData.newPlainText("telegram_msg", item.content))
                                                haptic.performHapticFeedback(HapticFeedbackType.LongPress)
                                                copiedNotice = "Copied to clipboard!"
                                            },
                                            modifier = Modifier.size(30.dp)
                                        ) {
                                            Icon(
                                                Icons.Default.ContentCopy,
                                                contentDescription = "Copy",
                                                tint = M3TextMuted,
                                                modifier = Modifier.size(16.dp)
                                            )
                                        }

                                        // 1-Tap Delete Button
                                        IconButton(
                                            onClick = { onDeleteItem(item.itemId) },
                                            modifier = Modifier.size(30.dp)
                                        ) {
                                            Icon(
                                                Icons.Default.DeleteOutline,
                                                contentDescription = "Delete",
                                                tint = M3TextMuted,
                                                modifier = Modifier.size(16.dp)
                                            )
                                        }
                                    }
                                }

                                Spacer(modifier = Modifier.height(6.dp))

                                Surface(
                                    color = if (item.type == "CODE") MaterialTheme.colorScheme.surfaceContainerHighest else Color.Transparent,
                                    shape = RoundedCornerShape(8.dp),
                                    modifier = Modifier.fillMaxWidth()
                                ) {
                                    Text(
                                        text = item.content,
                                        fontSize = 13.sp,
                                        lineHeight = 18.sp,
                                        fontFamily = if (item.type == "CODE") FontFamily.Monospace else FontFamily.Default,
                                        color = if (item.type == "CODE") MaterialTheme.colorScheme.onSurfaceVariant else M3TextPrimary,
                                        modifier = if (item.type == "CODE") Modifier.padding(10.dp) else Modifier
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
