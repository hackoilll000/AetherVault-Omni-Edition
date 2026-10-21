package com.example.ui.components

import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.outlined.CloudDone
import androidx.compose.material.icons.outlined.CloudOff
import androidx.compose.material.icons.outlined.Folder
import androidx.compose.material3.Icon
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.core.database.VirtualFolder
import com.example.ui.theme.*

@Composable
fun M3BentoGridSection(
    sectionTitle: String = "On Tele Drive",
    folders: List<VirtualFolder>,
    onFolderClick: (VirtualFolder) -> Unit,
    onViewAllClick: () -> Unit,
    modifier: Modifier = Modifier
) {
    Column(
        modifier = modifier
            .fillMaxWidth()
            .padding(horizontal = 16.dp, vertical = 8.dp)
    ) {
        // Section Header
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically
        ) {
            Row(verticalAlignment = Alignment.CenterVertically) {
                Text(
                    text = sectionTitle,
                    fontSize = 18.sp,
                    fontWeight = FontWeight.SemiBold,
                    color = M3TextPrimary
                )
                Spacer(modifier = Modifier.width(4.dp))
                Text(
                    text = "^",
                    fontSize = 14.sp,
                    fontWeight = FontWeight.Bold,
                    color = M3TextMuted
                )
            }

            TextButton(onClick = onViewAllClick) {
                Text(
                    text = "View all",
                    fontSize = 14.sp,
                    fontWeight = FontWeight.Medium,
                    color = M3PrimaryBlue
                )
            }
        }

        Spacer(modifier = Modifier.height(8.dp))

        // 2-Column Bento Grid
        val chunked = folders.chunked(2)
        Column(verticalArrangement = Arrangement.spacedBy(12.dp)) {
            for (rowFolders in chunked) {
                Row(
                    horizontalArrangement = Arrangement.spacedBy(12.dp),
                    modifier = Modifier.fillMaxWidth()
                ) {
                    for (folder in rowFolders) {
                        M3BentoFolderCard(
                            folder = folder,
                            onClick = { onFolderClick(folder) },
                            modifier = Modifier.weight(1f)
                        )
                    }
                    if (rowFolders.size == 1) {
                        Spacer(modifier = Modifier.weight(1f))
                    }
                }
            }
        }
    }
}

@Composable
fun M3BentoFolderCard(
    folder: VirtualFolder,
    onClick: () -> Unit,
    modifier: Modifier = Modifier
) {
    Surface(
        shape = RoundedCornerShape(24.dp),
        color = M3TonalSurface,
        modifier = modifier
            .clip(RoundedCornerShape(24.dp))
            .clickable { onClick() }
    ) {
        Column(modifier = Modifier.padding(12.dp)) {
            // Square Thumbnail Container
            Box(
                modifier = Modifier
                    .fillMaxWidth()
                    .aspectRatio(1f)
                    .clip(RoundedCornerShape(16.dp))
                    .background(Color.White),
                contentAlignment = Alignment.Center
            ) {
                // Placeholder Folder Icon / Preview
                Icon(
                    imageVector = Icons.Outlined.Folder,
                    contentDescription = null,
                    tint = M3PrimaryBlue,
                    modifier = Modifier.size(56.dp)
                )

                // Top-Right Cloud Sync Overlay
                Box(
                    modifier = Modifier
                        .align(Alignment.TopEnd)
                        .padding(8.dp)
                        .size(28.dp)
                        .clip(CircleShape)
                        .background(Color.Black.copy(alpha = 0.35f)),
                    contentAlignment = Alignment.Center
                ) {
                    Icon(
                        imageVector = if (folder.isStarred) Icons.Outlined.CloudDone else Icons.Outlined.CloudOff,
                        contentDescription = "Cloud Sync Status",
                        tint = Color.White,
                        modifier = Modifier.size(16.dp)
                    )
                }
            }

            Spacer(modifier = Modifier.height(10.dp))

            // Card Footer
            Text(
                text = folder.name,
                fontSize = 15.sp,
                fontWeight = FontWeight.Medium,
                color = M3TextPrimary,
                maxLines = 1,
                overflow = TextOverflow.Ellipsis
            )

            Text(
                text = if (folder.isStarred) "Synced via Bot" else "Backup off",
                fontSize = 12.sp,
                fontWeight = FontWeight.Normal,
                color = M3TextSubtle
            )
        }
    }
}
