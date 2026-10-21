package com.example.ui.components

import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.outlined.*
import androidx.compose.material3.*
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.ui.theme.*

data class M3DrawerMenuItem(
    val title: String,
    val icon: ImageVector,
    val onClick: () -> Unit
)

@Composable
fun M3NavigationDrawerContent(
    mbUsedText: String = "1.3 MB of 15 GB used",
    onSelectMenu: (String) -> Unit,
    onConfigureBot: () -> Unit,
    modifier: Modifier = Modifier
) {
    Surface(
        modifier = modifier
            .fillMaxHeight()
            .widthIn(max = 320.dp)
            .clip(RoundedCornerShape(topEnd = 24.dp, bottomEnd = 24.dp))
            .testTag("m3_navigation_drawer"),
        color = Color(0xFFF8FAFD)
    ) {
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(vertical = 24.dp)
        ) {
            // Header
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(horizontal = 24.dp, vertical = 8.dp),
                verticalAlignment = Alignment.CenterVertically
            ) {
                Text(
                    text = "Tele Drive",
                    fontSize = 22.sp,
                    fontWeight = FontWeight.Bold,
                    color = M3TextPrimary
                )
            }

            HorizontalDivider(
                color = M3BorderSubtle,
                thickness = 1.dp,
                modifier = Modifier.padding(vertical = 12.dp)
            )

            // Menu Items
            val menuItems = listOf(
                M3DrawerMenuItem("Recent", Icons.Outlined.Schedule) { onSelectMenu("Recent") },
                M3DrawerMenuItem("Telegram Sync & Chat", Icons.Outlined.Forum) { onSelectMenu("TelegramChat") },
                M3DrawerMenuItem("Uploads", Icons.Outlined.CloudUpload) { onSelectMenu("Uploads") },
                M3DrawerMenuItem("Offline", Icons.Outlined.OfflinePin) { onSelectMenu("Offline") },
                M3DrawerMenuItem("Bin", Icons.Outlined.Delete) { onSelectMenu("Bin") },
                M3DrawerMenuItem("Legal Disclaimer", Icons.Outlined.Gavel) { onSelectMenu("LegalDisclaimer") },
                M3DrawerMenuItem("Settings", Icons.Outlined.Settings) { onSelectMenu("Settings") },
                M3DrawerMenuItem("Help and feedback", Icons.Outlined.HelpOutline) { onSelectMenu("Help") }
            )

            Column(modifier = Modifier.weight(1f)) {
                menuItems.forEach { item ->
                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(horizontal = 12.dp, vertical = 4.dp)
                            .clip(RoundedCornerShape(9999.dp))
                            .clickable { item.onClick() }
                            .padding(horizontal = 16.dp, vertical = 12.dp),
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Icon(
                            imageVector = item.icon,
                            contentDescription = item.title,
                            tint = M3TextMuted,
                            modifier = Modifier.size(22.dp)
                        )
                        Spacer(modifier = Modifier.width(16.dp))
                        Text(
                            text = item.title,
                            fontSize = 15.sp,
                            fontWeight = FontWeight.Medium,
                            color = M3TextPrimary
                        )
                    }
                }
            }

            HorizontalDivider(
                color = M3BorderSubtle,
                thickness = 1.dp,
                modifier = Modifier.padding(vertical = 12.dp)
            )

            // Storage Meter Footer
            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(horizontal = 24.dp)
            ) {
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Icon(
                        imageVector = Icons.Outlined.Cloud,
                        contentDescription = "Storage",
                        tint = M3TextMuted,
                        modifier = Modifier.size(20.dp)
                    )
                    Spacer(modifier = Modifier.width(12.dp))
                    Text(
                        text = "Storage",
                        fontSize = 15.sp,
                        fontWeight = FontWeight.Medium,
                        color = M3TextPrimary
                    )
                }

                Spacer(modifier = Modifier.height(10.dp))

                LinearProgressIndicator(
                    progress = { 0.08f },
                    modifier = Modifier
                        .fillMaxWidth()
                        .height(4.dp)
                        .clip(RoundedCornerShape(2.dp)),
                    color = M3PrimaryBlue,
                    trackColor = M3TonalSurface
                )

                Spacer(modifier = Modifier.height(8.dp))

                Text(
                    text = mbUsedText,
                    fontSize = 13.sp,
                    fontWeight = FontWeight.Normal,
                    color = M3TextMuted
                )

                Spacer(modifier = Modifier.height(12.dp))

                OutlinedButton(
                    onClick = onConfigureBot,
                    modifier = Modifier.clip(CircleShape)
                ) {
                    Text(
                        text = "Configure Bot Token",
                        fontSize = 13.sp,
                        fontWeight = FontWeight.Medium,
                        color = M3PrimaryBlue
                    )
                }

                Spacer(modifier = Modifier.height(16.dp))

                Surface(
                    color = M3TonalSurface,
                    shape = RoundedCornerShape(12.dp),
                    modifier = Modifier.fillMaxWidth()
                ) {
                    Column(modifier = Modifier.padding(10.dp)) {
                        Text(
                            text = "Created by Ayush Mondal",
                            fontSize = 11.sp,
                            fontWeight = FontWeight.Bold,
                            color = M3TextPrimary
                        )
                        Text(
                            text = "Class 9th, 14 years old in 2026 • TRB FireOps",
                            fontSize = 10.sp,
                            fontWeight = FontWeight.Normal,
                            color = M3TextMuted
                        )
                    }
                }
            }
        }
    }
}
