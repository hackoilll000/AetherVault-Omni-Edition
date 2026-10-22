package com.example.ui.components

import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.*
import androidx.compose.material.icons.outlined.*
import androidx.compose.material3.*
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.features.studio.AppLanguage

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun M3AddFileBottomSheet(
    onDismissRequest: () -> Unit,
    language: AppLanguage,
    onCreateFolderClick: () -> Unit,
    onUploadFilesClick: () -> Unit,
    onUploadMediaClick: () -> Unit,
    onCreateNoteClick: () -> Unit,
    onSyncChannelClick: () -> Unit,
    onTeleDropClick: () -> Unit
) {
    val isHindi = language == AppLanguage.HI

    ModalBottomSheet(
        onDismissRequest = onDismissRequest,
        shape = RoundedCornerShape(topStart = 28.dp, topEnd = 28.dp),
        containerColor = MaterialTheme.colorScheme.surface,
        dragHandle = { BottomSheetDefaults.DragHandle() }
    ) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = 24.dp)
                .padding(bottom = 36.dp)
                .navigationBarsPadding()
        ) {
            Text(
                text = if (isHindi) "नया जोड़ें / बनाएँ" else "Create new",
                style = MaterialTheme.typography.titleMedium,
                fontWeight = FontWeight.Bold,
                color = MaterialTheme.colorScheme.onSurface,
                modifier = Modifier.padding(bottom = 20.dp, start = 4.dp)
            )

            // 3-Column Action Grid
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceAround
            ) {
                AddFileActionItem(
                    title = if (isHindi) "फ़ोल्डर" else "Folder",
                    icon = Icons.Outlined.CreateNewFolder,
                    testTag = "add_folder_action",
                    onClick = {
                        onDismissRequest()
                        onCreateFolderClick()
                    }
                )

                AddFileActionItem(
                    title = if (isHindi) "फ़ाइलें अपलोड" else "Upload",
                    icon = Icons.Outlined.UploadFile,
                    testTag = "add_upload_files_action",
                    onClick = {
                        onDismissRequest()
                        onUploadFilesClick()
                    }
                )

                AddFileActionItem(
                    title = if (isHindi) "मीडिया" else "Media / Scan",
                    icon = Icons.Outlined.PhotoCamera,
                    testTag = "add_media_action",
                    onClick = {
                        onDismissRequest()
                        onUploadMediaClick()
                    }
                )
            }

            Spacer(modifier = Modifier.height(20.dp))

            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceAround
            ) {
                AddFileActionItem(
                    title = if (isHindi) "नया नोट" else "Doc / Note",
                    icon = Icons.Outlined.Description,
                    testTag = "add_note_action",
                    onClick = {
                        onDismissRequest()
                        onCreateNoteClick()
                    }
                )

                AddFileActionItem(
                    title = if (isHindi) "टेलीग्राम सिंक" else "Sync Channel",
                    icon = Icons.Outlined.CloudSync,
                    testTag = "add_sync_channel_action",
                    onClick = {
                        onDismissRequest()
                        onSyncChannelClick()
                    }
                )

                AddFileActionItem(
                    title = if (isHindi) "टेलीड्रॉप" else "TeleDrop",
                    icon = Icons.Outlined.SendToMobile,
                    testTag = "add_teledrop_action",
                    onClick = {
                        onDismissRequest()
                        onTeleDropClick()
                    }
                )
            }
        }
    }
}

@Composable
private fun AddFileActionItem(
    title: String,
    icon: ImageVector,
    testTag: String,
    onClick: () -> Unit
) {
    Column(
        modifier = Modifier
            .width(90.dp)
            .clickable { onClick() }
            .padding(vertical = 4.dp)
            .testTag(testTag),
        horizontalAlignment = Alignment.CenterHorizontally
    ) {
        Box(
            modifier = Modifier
                .size(56.dp)
                .clip(CircleShape)
                .background(MaterialTheme.colorScheme.surfaceContainerHigh),
            contentAlignment = Alignment.Center
        ) {
            Icon(
                imageVector = icon,
                contentDescription = title,
                tint = MaterialTheme.colorScheme.primary,
                modifier = Modifier.size(26.dp)
            )
        }

        Spacer(modifier = Modifier.height(8.dp))

        Text(
            text = title,
            fontSize = 12.sp,
            fontWeight = FontWeight.Medium,
            color = MaterialTheme.colorScheme.onSurface,
            textAlign = TextAlign.Center,
            maxLines = 1
        )
    }
}
