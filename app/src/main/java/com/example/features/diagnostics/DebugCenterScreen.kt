package com.example.features.diagnostics

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
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
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
fun DebugCenterScreen(
    manager: DiagnosticManager,
    language: AppLanguage = AppLanguage.EN,
    onBack: () -> Unit
) {
    val context = LocalContext.current
    val checkResults by manager.checkResultsFlow.collectAsState()
    val logs by manager.logsFlow.collectAsState()

    var searchQuery by remember { mutableStateOf("") }
    var snackbarMessage by remember { mutableStateOf<String?>(null) }
    var showExportDialog by remember { mutableStateOf(false) }

    LaunchedEffect(Unit) {
        if (checkResults.isEmpty()) {
            manager.runDiagnostics()
        }
    }

    val filteredLogs = remember(logs, searchQuery) {
        if (searchQuery.isBlank()) logs
        else logs.filter { it.summary.contains(searchQuery, ignoreCase = true) || it.category.contains(searchQuery, ignoreCase = true) }
    }

    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text("In-App Debug Center & Diagnostics", fontWeight = FontWeight.Bold) },
                navigationIcon = {
                    IconButton(onClick = onBack, modifier = Modifier.testTag("debug_center_back_button")) {
                        Icon(Icons.AutoMirrored.Filled.ArrowBack, contentDescription = "Back")
                    }
                },
                actions = {
                    IconButton(onClick = { manager.runDiagnostics() }) {
                        Icon(Icons.Default.Refresh, contentDescription = "Run Diagnostics")
                    }
                    IconButton(onClick = { showExportDialog = true }) {
                        Icon(Icons.Default.IosShare, contentDescription = "Export Report")
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
                    Column(modifier = Modifier.padding(16.dp), verticalArrangement = Arrangement.spacedBy(8.dp)) {
                        Row(verticalAlignment = Alignment.CenterVertically) {
                            Icon(Icons.Default.BugReport, contentDescription = null, tint = MaterialTheme.colorScheme.primary)
                            Spacer(modifier = Modifier.width(8.dp))
                            Text("System Diagnostic Dashboard", fontWeight = FontWeight.Bold, fontSize = 15.sp)
                        }
                        Text("App Version: 1.0.0-Omni • Android SDK: ${android.os.Build.VERSION.SDK_INT}", fontSize = 12.sp, color = M3TextMuted)
                        Text("Active Checks Passed: ${checkResults.count { it.status == DiagnosticStatus.PASSED }} / ${checkResults.size}", fontSize = 12.sp, fontWeight = FontWeight.Bold, color = MaterialTheme.colorScheme.primary)
                    }
                }

                Text("System Health Checks", fontSize = 15.sp, fontWeight = FontWeight.Bold, color = M3TextPrimary)

                checkResults.forEach { result ->
                    Card(
                        modifier = Modifier.fillMaxWidth(),
                        colors = CardDefaults.cardColors(containerColor = M3Surface),
                        shape = RoundedCornerShape(10.dp)
                    ) {
                        Row(
                            modifier = Modifier.padding(12.dp).fillMaxWidth(),
                            horizontalArrangement = Arrangement.SpaceBetween,
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Column(modifier = Modifier.weight(1f)) {
                                Text(result.checkName, fontWeight = FontWeight.Bold, fontSize = 13.sp, color = M3TextPrimary)
                                Text(result.message, fontSize = 11.sp, color = M3TextMuted)
                            }
                            Surface(
                                color = if (result.status == DiagnosticStatus.PASSED) MaterialTheme.colorScheme.primaryContainer else MaterialTheme.colorScheme.errorContainer,
                                shape = RoundedCornerShape(6.dp)
                            ) {
                                Text(
                                    text = result.status.name,
                                    fontSize = 10.sp,
                                    fontWeight = FontWeight.Bold,
                                    modifier = Modifier.padding(horizontal = 6.dp, vertical = 2.dp)
                                )
                            }
                        }
                    }
                }

                Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween, verticalAlignment = Alignment.CenterVertically) {
                    Text("Structured Log Console (${filteredLogs.size})", fontSize = 15.sp, fontWeight = FontWeight.Bold, color = M3TextPrimary)
                    OutlinedTextField(
                        value = searchQuery,
                        onValueChange = { searchQuery = it },
                        placeholder = { Text("Search logs...") },
                        modifier = Modifier.width(160.dp).height(48.dp)
                    )
                }

                Surface(
                    color = MaterialTheme.colorScheme.surfaceContainerLowest,
                    shape = RoundedCornerShape(8.dp),
                    modifier = Modifier.fillMaxWidth().height(220.dp)
                ) {
                    LazyColumn(modifier = Modifier.fillMaxSize().padding(8.dp)) {
                        items(filteredLogs) { log ->
                            Text(
                                text = "[${log.category}] ${log.summary}",
                                fontSize = 11.sp,
                                fontFamily = FontFamily.Monospace,
                                color = MaterialTheme.colorScheme.onSurface
                            )
                        }
                    }
                }
            }

            if (showExportDialog) {
                val reportJson = remember { manager.exportDiagnosticReportJson() }
                AlertDialog(
                    onDismissRequest = { showExportDialog = false },
                    title = { Text("Export Sanitized Diagnostic Report") },
                    text = {
                        Column {
                            Text("All credentials and bot tokens are automatically redacted:")
                            Spacer(modifier = Modifier.height(8.dp))
                            Surface(
                                color = MaterialTheme.colorScheme.surfaceContainerHighest,
                                shape = RoundedCornerShape(8.dp),
                                modifier = Modifier.fillMaxWidth().height(160.dp)
                            ) {
                                Text(reportJson, fontSize = 11.sp, fontFamily = FontFamily.Monospace, modifier = Modifier.padding(8.dp))
                            }
                        }
                    },
                    confirmButton = {
                        Button(onClick = {
                            val clipboard = context.getSystemService(Context.CLIPBOARD_SERVICE) as ClipboardManager
                            clipboard.setPrimaryClip(ClipData.newPlainText("AetherVault Diagnostic Report", reportJson))
                            showExportDialog = false
                            snackbarMessage = "Report copied to clipboard!"
                        }) { Text("Copy JSON") }
                    },
                    dismissButton = { TextButton(onClick = { showExportDialog = false }) { Text("Close") } }
                )
            }

            snackbarMessage?.let { msg ->
                Snackbar(modifier = Modifier.align(Alignment.BottomCenter).padding(16.dp), action = { TextButton(onClick = { snackbarMessage = null }) { Text("OK") } }) { Text(msg) }
            }
        }
    }
}
