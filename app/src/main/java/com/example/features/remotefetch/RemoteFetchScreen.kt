package com.example.features.remotefetch

import android.content.ClipData
import android.content.ClipboardManager
import android.content.Context
import androidx.compose.animation.*
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
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
import com.example.features.studio.AppLanguage
import com.example.ui.theme.*

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun RemoteFetchScreen(
    worker: RemoteFetchWorker,
    language: AppLanguage = AppLanguage.EN,
    onBack: () -> Unit
) {
    val context = LocalContext.current
    val jobs by worker.jobsFlow.collectAsState()
    val isHindi = language == AppLanguage.HI

    var urlInput by remember { mutableStateOf("") }
    var filenameOverride by remember { mutableStateOf("") }
    var channelIdInput by remember { mutableStateOf("") }
    var snackbarMessage by remember { mutableStateOf<String?>(null) }

    val validationResult = remember(urlInput) {
        if (urlInput.isBlank()) null else UrlSecurityValidator.validateUrl(urlInput)
    }

    Scaffold(
        topBar = {
            TopAppBar(
                title = {
                    Text(
                        text = if (isHindi) "रिमोट फेच व क्लाउड ट्रांसफर" else "Remote Fetch & Cloud Transfer",
                        fontWeight = FontWeight.Bold
                    )
                },
                navigationIcon = {
                    IconButton(onClick = onBack, modifier = Modifier.testTag("remote_fetch_back_button")) {
                        Icon(Icons.AutoMirrored.Filled.ArrowBack, contentDescription = "Back")
                    }
                },
                colors = TopAppBarDefaults.topAppBarColors(containerColor = MaterialTheme.colorScheme.surface)
            )
        }
    ) { padding ->
        Box(modifier = Modifier.fillMaxSize().padding(padding)) {
            Column(
                modifier = Modifier
                    .fillMaxSize()
                    .verticalScroll(rememberScrollState())
                    .padding(16.dp),
                verticalArrangement = Arrangement.spacedBy(16.dp)
            ) {
                Card(
                    modifier = Modifier.fillMaxWidth(),
                    colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surfaceContainerHigh)
                ) {
                    Column(modifier = Modifier.padding(16.dp), verticalArrangement = Arrangement.spacedBy(12.dp)) {
                        Row(verticalAlignment = Alignment.CenterVertically) {
                            Icon(Icons.Default.CloudDownload, contentDescription = null, tint = MaterialTheme.colorScheme.primary)
                            Spacer(modifier = Modifier.width(8.dp))
                            Text("Submit Remote Direct Download Link", fontWeight = FontWeight.Bold, fontSize = 15.sp)
                        }

                        Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                            OutlinedTextField(
                                value = urlInput,
                                onValueChange = { urlInput = it },
                                label = { Text("Direct File URL (HTTP/HTTPS)") },
                                placeholder = { Text("https://example.com/file.zip") },
                                modifier = Modifier.weight(1f).testTag("remote_fetch_url_input")
                            )

                            Button(
                                onClick = {
                                    val clipboard = context.getSystemService(Context.CLIPBOARD_SERVICE) as ClipboardManager
                                    val clip = clipboard.primaryClip
                                    if (clip != null && clip.itemCount > 0) {
                                        urlInput = clip.getItemAt(0).text.toString().trim()
                                    }
                                }
                            ) {
                                Icon(Icons.Default.ContentPaste, contentDescription = "Paste")
                            }
                        }

                        validationResult?.let { (isValid, msg) ->
                            Surface(
                                color = if (isValid) MaterialTheme.colorScheme.primaryContainer else MaterialTheme.colorScheme.errorContainer,
                                shape = RoundedCornerShape(8.dp),
                                modifier = Modifier.fillMaxWidth()
                            ) {
                                Text(
                                    text = msg,
                                    fontSize = 11.sp,
                                    color = if (isValid) MaterialTheme.colorScheme.onPrimaryContainer else MaterialTheme.colorScheme.onErrorContainer,
                                    modifier = Modifier.padding(8.dp)
                                )
                            }
                        }

                        Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                            OutlinedTextField(
                                value = filenameOverride,
                                onValueChange = { filenameOverride = it },
                                label = { Text("Filename Override (Optional)") },
                                modifier = Modifier.weight(1f)
                            )
                            OutlinedTextField(
                                value = channelIdInput,
                                onValueChange = { channelIdInput = it },
                                label = { Text("Destination Channel ID") },
                                modifier = Modifier.weight(1f)
                            )
                        }

                        Button(
                            onClick = {
                                val submitted = worker.submitFetchJob(urlInput, filenameOverride, channelIdInput)
                                if (submitted != null) {
                                    snackbarMessage = "Remote Cloud Transfer Submitted!"
                                    urlInput = ""
                                    filenameOverride = ""
                                }
                            },
                            enabled = validationResult?.first == true,
                            modifier = Modifier.fillMaxWidth().testTag("submit_fetch_button")
                        ) {
                            Icon(Icons.Default.Send, contentDescription = null)
                            Spacer(modifier = Modifier.width(8.dp))
                            Text("Start Remote Transfer to Telegram")
                        }
                    }
                }

                Text(
                    text = "Transfer Jobs Queue & History (${jobs.size})",
                    fontSize = 15.sp,
                    fontWeight = FontWeight.Bold,
                    color = M3TextPrimary
                )

                if (jobs.isEmpty()) {
                    Surface(
                        color = M3Surface,
                        shape = RoundedCornerShape(12.dp),
                        modifier = Modifier.fillMaxWidth()
                    ) {
                        Text(
                            text = "No active or completed remote transfer jobs.",
                            fontSize = 12.sp,
                            color = M3TextMuted,
                            modifier = Modifier.padding(16.dp)
                        )
                    }
                } else {
                    jobs.forEach { job ->
                        Card(
                            modifier = Modifier.fillMaxWidth(),
                            colors = CardDefaults.cardColors(containerColor = M3Surface),
                            shape = RoundedCornerShape(12.dp)
                        ) {
                            Column(modifier = Modifier.padding(14.dp), verticalArrangement = Arrangement.spacedBy(6.dp)) {
                                Row(
                                    modifier = Modifier.fillMaxWidth(),
                                    horizontalArrangement = Arrangement.SpaceBetween,
                                    verticalAlignment = Alignment.CenterVertically
                                ) {
                                    Text(
                                        text = job.customFilename.ifBlank { "Remote File" },
                                        fontWeight = FontWeight.Bold,
                                        fontSize = 13.sp,
                                        color = M3TextPrimary
                                    )
                                    Surface(
                                        color = when (job.status) {
                                            FetchJobStatus.COMPLETED -> MaterialTheme.colorScheme.primaryContainer
                                            FetchJobStatus.FAILED -> MaterialTheme.colorScheme.errorContainer
                                            else -> MaterialTheme.colorScheme.tertiaryContainer
                                        },
                                        shape = RoundedCornerShape(6.dp)
                                    ) {
                                        Text(
                                            text = job.status.name,
                                            fontSize = 10.sp,
                                            fontWeight = FontWeight.Bold,
                                            modifier = Modifier.padding(horizontal = 6.dp, vertical = 2.dp)
                                        )
                                    }
                                }

                                Text(
                                    text = "URL: ${job.sourceUrl}",
                                    fontSize = 11.sp,
                                    color = M3TextMuted,
                                    maxLines = 1
                                )

                                LinearProgressIndicator(
                                    progress = { job.uploadProgressPercent / 100f },
                                    modifier = Modifier.fillMaxWidth().height(6.dp).clip(RoundedCornerShape(3.dp))
                                )

                                Row(
                                    modifier = Modifier.fillMaxWidth(),
                                    horizontalArrangement = Arrangement.SpaceBetween,
                                    verticalAlignment = Alignment.CenterVertically
                                ) {
                                    Text(
                                        text = "ID: ${job.jobId}",
                                        fontSize = 10.sp,
                                        fontFamily = FontFamily.Monospace,
                                        color = M3TextMuted
                                    )

                                    Row {
                                        IconButton(
                                            onClick = {
                                                val clipboard = context.getSystemService(Context.CLIPBOARD_SERVICE) as ClipboardManager
                                                clipboard.setPrimaryClip(ClipData.newPlainText("Job ID", job.jobId))
                                                snackbarMessage = "Job ID copied!"
                                            },
                                            modifier = Modifier.size(28.dp)
                                        ) {
                                            Icon(Icons.Default.ContentCopy, contentDescription = "Copy ID", modifier = Modifier.size(16.dp))
                                        }

                                        IconButton(
                                            onClick = { worker.removeJob(job.jobId) },
                                            modifier = Modifier.size(28.dp)
                                        ) {
                                            Icon(Icons.Default.Delete, contentDescription = "Remove", tint = MaterialTheme.colorScheme.error, modifier = Modifier.size(16.dp))
                                        }
                                    }
                                }
                            }
                        }
                    }
                }
            }

            snackbarMessage?.let { msg ->
                Snackbar(
                    modifier = Modifier.align(Alignment.BottomCenter).padding(16.dp),
                    action = { TextButton(onClick = { snackbarMessage = null }) { Text("OK") } }
                ) { Text(msg) }
            }
        }
    }
}
