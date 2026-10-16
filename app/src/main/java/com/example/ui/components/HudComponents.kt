package com.example.ui.components

import androidx.compose.animation.*
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.grid.GridCells
import androidx.compose.foundation.lazy.grid.LazyVerticalGrid
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
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.core.database.TransferTask
import com.example.ui.theme.CyanNeon
import com.example.ui.theme.IndigoHyper

/**
 * Live HUD Telemetry Overlay Bar
 * Displays live transfer rate, active bot swarm threads, RAM usage, and cipher status
 */
@Composable
fun HudTelemetryBar(
    modifier: Modifier = Modifier,
    transferSpeedBps: Long = 0L,
    activeBots: Int = 3,
    totalBots: Int = 5,
    ramUsageMb: Int = 54
) {
    val speedMbStr = String.format("%.1f MB/s", transferSpeedBps / (1024f * 1024f))

    Surface(
        modifier = modifier
            .fillMaxWidth()
            .padding(horizontal = 12.dp, vertical = 4.dp),
        color = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.85f),
        shape = RoundedCornerShape(8.dp),
        tonalElevation = 4.dp,
        border = androidx.compose.foundation.BorderStroke(0.8.dp, MaterialTheme.colorScheme.outline.copy(alpha = 0.3f))
    ) {
        Row(
            modifier = Modifier
                .padding(horizontal = 10.dp, vertical = 4.dp)
                .fillMaxWidth(),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically
        ) {
            // Speed indicator
            Row(verticalAlignment = Alignment.CenterVertically) {
                Box(
                    modifier = Modifier
                        .size(6.dp)
                        .clip(CircleShape)
                        .background(if (transferSpeedBps > 0) CyanNeon else Color.Gray)
                )
                Spacer(modifier = Modifier.width(6.dp))
                Text(
                    text = if (transferSpeedBps > 0) speedMbStr else "IDLE",
                    fontSize = 11.sp,
                    fontFamily = FontFamily.Monospace,
                    fontWeight = FontWeight.Bold,
                    color = MaterialTheme.colorScheme.primary
                )
            }

            // Swarm workers
            Row(verticalAlignment = Alignment.CenterVertically) {
                Icon(
                    imageVector = Icons.Default.Groups,
                    contentDescription = null,
                    tint = IndigoHyper,
                    modifier = Modifier.size(12.dp)
                )
                Spacer(modifier = Modifier.width(4.dp))
                Text(
                    text = "$activeBots/$totalBots BOTS",
                    fontSize = 10.sp,
                    fontFamily = FontFamily.Monospace,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
            }

            // RAM footprint
            Row(verticalAlignment = Alignment.CenterVertically) {
                Icon(
                    imageVector = Icons.Default.Memory,
                    contentDescription = null,
                    tint = MaterialTheme.colorScheme.tertiary,
                    modifier = Modifier.size(12.dp)
                )
                Spacer(modifier = Modifier.width(4.dp))
                Text(
                    text = "$ramUsageMb MB",
                    fontSize = 10.sp,
                    fontFamily = FontFamily.Monospace,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
            }

            // Cipher status
            Text(
                text = "AES-256-GCM HW",
                fontSize = 9.sp,
                fontFamily = FontFamily.Monospace,
                color = CyanNeon.copy(alpha = 0.9f)
            )
        }
    }
}

/**
 * Dynamic Floating Transfer Pill docked above bottom navigation
 */
@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun DynamicTransferPill(
    activeTasks: List<TransferTask>,
    onCancelTask: (String) -> Unit,
    modifier: Modifier = Modifier
) {
    if (activeTasks.isEmpty()) return

    var showDetailsSheet by remember { mutableStateOf(false) }
    val primaryTask = activeTasks.firstOrNull() ?: return
    val progress = if (primaryTask.totalBytes > 0) {
        (primaryTask.bytesTransferred.toFloat() / primaryTask.totalBytes.toFloat()).coerceIn(0f, 1f)
    } else 0.5f

    val speedMbStr = String.format("%.1f MB/s", primaryTask.speedBps / (1024f * 1024f))

    Surface(
        modifier = modifier
            .padding(horizontal = 16.dp, vertical = 6.dp)
            .clickable { showDetailsSheet = true },
        shape = RoundedCornerShape(24.dp),
        color = MaterialTheme.colorScheme.surfaceVariant,
        tonalElevation = 8.dp,
        border = androidx.compose.foundation.BorderStroke(1.dp, CyanNeon.copy(alpha = 0.6f))
    ) {
        Row(
            modifier = Modifier.padding(horizontal = 14.dp, vertical = 8.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            CircularProgressIndicator(
                progress = { progress },
                modifier = Modifier.size(20.dp),
                color = CyanNeon,
                trackColor = MaterialTheme.colorScheme.outline.copy(alpha = 0.3f),
                strokeWidth = 2.5.dp
            )
            Spacer(modifier = Modifier.width(10.dp))
            Column {
                Text(
                    text = primaryTask.fileName,
                    fontSize = 12.sp,
                    fontWeight = FontWeight.SemiBold,
                    color = MaterialTheme.colorScheme.onSurface,
                    maxLines = 1
                )
                Text(
                    text = "${primaryTask.direction}: $speedMbStr • Chunks ${primaryTask.completedChunks}/${primaryTask.totalChunks}",
                    fontSize = 10.sp,
                    fontFamily = FontFamily.Monospace,
                    color = MaterialTheme.colorScheme.primary
                )
            }
            Spacer(modifier = Modifier.weight(1f))
            Icon(
                imageVector = Icons.Default.KeyboardArrowUp,
                contentDescription = "Expand Swarm Details",
                tint = MaterialTheme.colorScheme.primary,
                modifier = Modifier.size(18.dp)
            )
        }
    }

    if (showDetailsSheet) {
        ModalBottomSheet(
            onDismissRequest = { showDetailsSheet = false },
            containerColor = MaterialTheme.colorScheme.surface
        ) {
            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(20.dp)
            ) {
                Text(
                    text = "Transfer Swarm Monitor",
                    fontSize = 18.sp,
                    fontWeight = FontWeight.Bold,
                    color = MaterialTheme.colorScheme.onSurface
                )
                Spacer(modifier = Modifier.height(12.dp))

                activeTasks.forEach { task ->
                    Card(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(vertical = 6.dp),
                        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surfaceVariant)
                    ) {
                        Column(modifier = Modifier.padding(14.dp)) {
                            Row(
                                modifier = Modifier.fillMaxWidth(),
                                horizontalArrangement = Arrangement.SpaceBetween,
                                verticalAlignment = Alignment.CenterVertically
                            ) {
                                Text(
                                    text = task.fileName,
                                    fontWeight = FontWeight.SemiBold,
                                    fontSize = 14.sp
                                )
                                IconButton(
                                    onClick = { onCancelTask(task.taskId) },
                                    modifier = Modifier.size(24.dp)
                                ) {
                                    Icon(
                                        imageVector = Icons.Default.Close,
                                        contentDescription = "Cancel",
                                        tint = MaterialTheme.colorScheme.error,
                                        modifier = Modifier.size(16.dp)
                                    )
                                }
                            }

                            Spacer(modifier = Modifier.height(8.dp))
                            LinearProgressIndicator(
                                progress = {
                                    if (task.totalChunks > 0) task.completedChunks.toFloat() / task.totalChunks.toFloat() else 0f
                                },
                                modifier = Modifier.fillMaxWidth(),
                                color = CyanNeon
                            )

                            Spacer(modifier = Modifier.height(10.dp))
                            Text(
                                text = "19MB Chunks Visual Bitmap (${task.completedChunks} / ${task.totalChunks} Done):",
                                fontSize = 11.sp,
                                color = MaterialTheme.colorScheme.onSurfaceVariant
                            )
                            Spacer(modifier = Modifier.height(6.dp))

                            // Chunk blocks grid
                            val totalBlocks = task.totalChunks.coerceAtLeast(1).coerceAtMost(36)
                            LazyVerticalGrid(
                                columns = GridCells.Fixed(12),
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .height(48.dp),
                                horizontalArrangement = Arrangement.spacedBy(3.dp),
                                verticalArrangement = Arrangement.spacedBy(3.dp)
                            ) {
                                items(totalBlocks) { idx ->
                                    val isDone = idx < task.completedChunks
                                    Box(
                                        modifier = Modifier
                                            .size(16.dp)
                                            .clip(RoundedCornerShape(3.dp))
                                            .background(if (isDone) CyanNeon else Color(0xFF22283A))
                                    )
                                }
                            }
                        }
                    }
                }
                Spacer(modifier = Modifier.height(24.dp))
            }
        }
    }
}
