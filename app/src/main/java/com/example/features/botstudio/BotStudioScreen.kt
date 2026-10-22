package com.example.features.botstudio

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
import androidx.compose.material.icons.filled.*
import androidx.compose.material.icons.outlined.*
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
fun BotStudioScreen(
    manager: BotProjectManager,
    language: AppLanguage = AppLanguage.EN
) {
    val context = LocalContext.current
    val projects by manager.projectsFlow.collectAsState()
    val logs by manager.logsFlow.collectAsState()

    var activeTab by remember { mutableIntStateOf(0) }
    var selectedProject by remember { mutableStateOf<BotProject?>(projects.firstOrNull()) }
    var syntaxMessage by remember { mutableStateOf<String?>(null) }
    var copiedNotice by remember { mutableStateOf<String?>(null) }

    LaunchedEffect(projects) {
        if (selectedProject == null && projects.isNotEmpty()) {
            selectedProject = projects.first()
        }
    }

    val currentProj = selectedProject ?: projects.firstOrNull()

    Scaffold(
        containerColor = M3Background,
        topBar = {
            Surface(
                color = M3Surface,
                tonalElevation = 2.dp,
                modifier = Modifier.fillMaxWidth()
            ) {
                Column(modifier = Modifier.padding(16.dp)) {
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Row(verticalAlignment = Alignment.CenterVertically) {
                            Box(
                                modifier = Modifier
                                    .size(40.dp)
                                    .clip(CircleShape)
                                    .background(M3ActivePill),
                                contentAlignment = Alignment.Center
                            ) {
                                Icon(
                                    imageVector = Icons.Default.SmartToy,
                                    contentDescription = null,
                                    tint = M3ActivePillText,
                                    modifier = Modifier.size(24.dp)
                                )
                            }
                            Spacer(modifier = Modifier.width(12.dp))
                            Column {
                                Text(
                                    text = if (language == AppLanguage.HI) "पायथन बॉट स्टूडियो" else "Python Bot Studio",
                                    fontSize = 18.sp,
                                    fontWeight = FontWeight.Bold,
                                    color = M3TextPrimary
                                )
                                Text(
                                    text = if (language == AppLanguage.HI) "सेल्फ-होस्टेड बॉट ऑटोमेशन और टर्मक्स डिप्लॉय" else "Self-Hosted Bot Automation & Termux Deployer",
                                    fontSize = 12.sp,
                                    color = M3TextMuted
                                )
                            }
                        }

                        IconButton(
                            onClick = {
                                val newProj = BotProject(
                                    name = "New Bot ${projects.size + 1}",
                                    description = "Custom Python Telegram Bot"
                                )
                                manager.saveProject(newProj)
                                selectedProject = newProj
                                activeTab = 1
                            },
                            modifier = Modifier.testTag("create_bot_project_button")
                        ) {
                            Icon(
                                imageVector = Icons.Default.Add,
                                contentDescription = "New Bot Project",
                                tint = M3TextPrimary
                            )
                        }
                    }

                    Spacer(modifier = Modifier.height(12.dp))

                    ScrollableTabRow(
                        selectedTabIndex = activeTab,
                        containerColor = Color.Transparent,
                        contentColor = MaterialTheme.colorScheme.primary,
                        edgePadding = 0.dp
                    ) {
                        Tab(
                            selected = activeTab == 0,
                            onClick = { activeTab = 0 },
                            text = { Text("Projects", fontSize = 13.sp) },
                            icon = { Icon(Icons.Default.Folder, contentDescription = null, modifier = Modifier.size(18.dp)) }
                        )
                        Tab(
                            selected = activeTab == 1,
                            onClick = { activeTab = 1 },
                            text = { Text("Python Editor", fontSize = 13.sp) },
                            icon = { Icon(Icons.Default.Code, contentDescription = null, modifier = Modifier.size(18.dp)) }
                        )
                        Tab(
                            selected = activeTab == 2,
                            onClick = { activeTab = 2 },
                            text = { Text("Termux Deployer", fontSize = 13.sp) },
                            icon = { Icon(Icons.Default.Terminal, contentDescription = null, modifier = Modifier.size(18.dp)) }
                        )
                        Tab(
                            selected = activeTab == 3,
                            onClick = { activeTab = 3 },
                            text = { Text("AI & Database", fontSize = 13.sp) },
                            icon = { Icon(Icons.Default.Psychology, contentDescription = null, modifier = Modifier.size(18.dp)) }
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
            when (activeTab) {
                0 -> BotProjectsTab(
                    projects = projects,
                    selectedProject = currentProj,
                    onSelectProject = { selectedProject = it },
                    onEditProject = {
                        selectedProject = it
                        activeTab = 1
                    },
                    onDeleteProject = { manager.deleteProject(it.id) },
                    onSelectTemplate = { templateType ->
                        val templateCode = manager.loadTemplate(templateType)
                        val newProj = BotProject(
                            name = "${templateType.displayName} Project",
                            description = templateType.description,
                            pythonScript = templateCode
                        )
                        manager.saveProject(newProj)
                        selectedProject = newProj
                        activeTab = 1
                    }
                )

                1 -> currentProj?.let { proj ->
                    PythonEditorTab(
                        project = proj,
                        onSaveProject = { updated ->
                            manager.saveProject(updated)
                            selectedProject = updated
                        },
                        onValidateSyntax = { code ->
                            val result = manager.validatePythonScript(code)
                            syntaxMessage = result.second
                            manager.appendLog("Syntax Check for '${proj.name}': ${result.second}")
                        },
                        syntaxMessage = syntaxMessage
                    )
                }

                2 -> currentProj?.let { proj ->
                    TermuxDeployerTab(
                        project = proj,
                        manager = manager,
                        logs = logs,
                        onCopy = { text, label ->
                            val clipboard = context.getSystemService(Context.CLIPBOARD_SERVICE) as ClipboardManager
                            clipboard.setPrimaryClip(ClipData.newPlainText(label, text))
                            copiedNotice = "$label copied to clipboard!"
                        }
                    )
                }

                3 -> currentProj?.let { proj ->
                    AiAndDatabaseTab(
                        project = proj,
                        onSaveProject = { updated ->
                            manager.saveProject(updated)
                            selectedProject = updated
                        }
                    )
                }
            }

            copiedNotice?.let { notice ->
                Snackbar(
                    modifier = Modifier
                        .align(Alignment.BottomCenter)
                        .padding(16.dp),
                    action = {
                        TextButton(onClick = { copiedNotice = null }) {
                            Text("OK", color = MaterialTheme.colorScheme.primary)
                        }
                    }
                ) {
                    Text(notice)
                }
            }
        }
    }
}

@Composable
fun BotProjectsTab(
    projects: List<BotProject>,
    selectedProject: BotProject?,
    onSelectProject: (BotProject) -> Unit,
    onEditProject: (BotProject) -> Unit,
    onDeleteProject: (BotProject) -> Unit,
    onSelectTemplate: (BotTemplateType) -> Unit
) {
    LazyColumn(
        modifier = Modifier
            .fillMaxSize()
            .padding(16.dp),
        verticalArrangement = Arrangement.spacedBy(16.dp)
    ) {
        item {
            Text(
                text = "Bot Starter Templates",
                fontSize = 15.sp,
                fontWeight = FontWeight.Bold,
                color = M3TextPrimary
            )
            Spacer(modifier = Modifier.height(8.dp))
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(vertical = 4.dp),
                horizontalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                BotTemplateType.values().take(3).forEach { template ->
                    Card(
                        modifier = Modifier
                            .weight(1f)
                            .clickable { onSelectTemplate(template) },
                        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surfaceContainerHigh)
                    ) {
                        Column(modifier = Modifier.padding(10.dp)) {
                            Icon(
                                Icons.Default.Extension,
                                contentDescription = null,
                                tint = MaterialTheme.colorScheme.primary,
                                modifier = Modifier.size(20.dp)
                            )
                            Spacer(modifier = Modifier.height(4.dp))
                            Text(
                                text = template.displayName,
                                fontSize = 12.sp,
                                fontWeight = FontWeight.Bold,
                                color = M3TextPrimary
                            )
                        }
                    }
                }
            }
        }

        item {
            Text(
                text = "Your Bot Projects (${projects.size})",
                fontSize = 15.sp,
                fontWeight = FontWeight.Bold,
                color = M3TextPrimary
            )
        }

        items(projects, key = { it.id }) { proj ->
            val isSelected = selectedProject?.id == proj.id
            Card(
                modifier = Modifier
                    .fillMaxWidth()
                    .clickable { onSelectProject(proj) },
                colors = CardDefaults.cardColors(
                    containerColor = if (isSelected) MaterialTheme.colorScheme.secondaryContainer else M3Surface
                ),
                shape = RoundedCornerShape(12.dp)
            ) {
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(14.dp),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Column(modifier = Modifier.weight(1f)) {
                        Row(verticalAlignment = Alignment.CenterVertically) {
                            Text(
                                text = proj.name,
                                fontSize = 14.sp,
                                fontWeight = FontWeight.Bold,
                                color = M3TextPrimary
                            )
                            Spacer(modifier = Modifier.width(8.dp))
                            Surface(
                                color = MaterialTheme.colorScheme.tertiaryContainer,
                                shape = RoundedCornerShape(6.dp)
                            ) {
                                Text(
                                    text = proj.status.name,
                                    fontSize = 10.sp,
                                    fontWeight = FontWeight.Medium,
                                    color = MaterialTheme.colorScheme.onTertiaryContainer,
                                    modifier = Modifier.padding(horizontal = 6.dp, vertical = 2.dp)
                                )
                            }
                        }
                        Spacer(modifier = Modifier.height(4.dp))
                        Text(
                            text = proj.description,
                            fontSize = 12.sp,
                            color = M3TextMuted,
                            maxLines = 2
                        )
                    }

                    Row {
                        IconButton(onClick = { onEditProject(proj) }) {
                            Icon(Icons.Default.Edit, contentDescription = "Edit", tint = MaterialTheme.colorScheme.primary)
                        }
                        IconButton(onClick = { onDeleteProject(proj) }) {
                            Icon(Icons.Default.Delete, contentDescription = "Delete", tint = MaterialTheme.colorScheme.error)
                        }
                    }
                }
            }
        }
    }
}

@Composable
fun PythonEditorTab(
    project: BotProject,
    onSaveProject: (BotProject) -> Unit,
    onValidateSyntax: (String) -> Unit,
    syntaxMessage: String?
) {
    var botName by remember(project) { mutableStateOf(project.name) }
    var botToken by remember(project) { mutableStateOf(project.botTokenSecret) }
    var channelId by remember(project) { mutableStateOf(project.channelId) }
    var scriptCode by remember(project) { mutableStateOf(project.pythonScript) }

    Column(
        modifier = Modifier
            .fillMaxSize()
            .padding(16.dp)
            .verticalScroll(rememberScrollState()),
        verticalArrangement = Arrangement.spacedBy(12.dp)
    ) {
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically
        ) {
            OutlinedTextField(
                value = botName,
                onValueChange = { botName = it },
                label = { Text("Bot Name") },
                modifier = Modifier.weight(1f)
            )
            Spacer(modifier = Modifier.width(8.dp))
            Button(
                onClick = {
                    val updated = project.copy(
                        name = botName,
                        botTokenSecret = botToken,
                        channelId = channelId,
                        pythonScript = scriptCode
                    )
                    onSaveProject(updated)
                },
                modifier = Modifier.testTag("save_bot_project_button")
            ) {
                Icon(Icons.Default.Save, contentDescription = null, modifier = Modifier.size(16.dp))
                Spacer(modifier = Modifier.width(4.dp))
                Text("Save")
            }
        }

        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.spacedBy(8.dp)
        ) {
            OutlinedTextField(
                value = botToken,
                onValueChange = { botToken = it },
                label = { Text("Bot API Token") },
                modifier = Modifier.weight(1.2f)
            )
            OutlinedTextField(
                value = channelId,
                onValueChange = { channelId = it },
                label = { Text("Channel ID") },
                modifier = Modifier.weight(0.8f)
            )
        }

        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically
        ) {
            Text(
                text = "Python Bot Script (main.py)",
                fontSize = 14.sp,
                fontWeight = FontWeight.Bold,
                color = M3TextPrimary
            )

            OutlinedButton(
                onClick = { onValidateSyntax(scriptCode) }
            ) {
                Icon(Icons.Default.CheckCircle, contentDescription = null, modifier = Modifier.size(16.dp))
                Spacer(modifier = Modifier.width(4.dp))
                Text("Check Syntax")
            }
        }

        syntaxMessage?.let { msg ->
            Surface(
                color = if (msg.contains("Passed")) MaterialTheme.colorScheme.primaryContainer else MaterialTheme.colorScheme.errorContainer,
                shape = RoundedCornerShape(8.dp),
                modifier = Modifier.fillMaxWidth()
            ) {
                Text(
                    text = msg,
                    fontSize = 12.sp,
                    color = if (msg.contains("Passed")) MaterialTheme.colorScheme.onPrimaryContainer else MaterialTheme.colorScheme.onErrorContainer,
                    modifier = Modifier.padding(10.dp)
                )
            }
        }

        Surface(
            color = MaterialTheme.colorScheme.surfaceContainerHighest,
            shape = RoundedCornerShape(8.dp),
            modifier = Modifier
                .fillMaxWidth()
                .height(320.dp)
        ) {
            OutlinedTextField(
                value = scriptCode,
                onValueChange = { scriptCode = it },
                textStyle = LocalTextStyle.current.copy(
                    fontFamily = FontFamily.Monospace,
                    fontSize = 12.5.sp,
                    lineHeight = 17.sp,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                ),
                modifier = Modifier
                    .fillMaxSize()
                    .testTag("python_code_editor_input"),
                placeholder = { Text("Write your Python Telegram Bot code here...") }
            )
        }
    }
}

@Composable
fun TermuxDeployerTab(
    project: BotProject,
    manager: BotProjectManager,
    logs: List<String>,
    onCopy: (String, String) -> Unit
) {
    val setupScript = remember(project) { manager.generateTermuxSetupScript(project) }
    val envTemplate = remember(project) { manager.generateEnvTemplate(project) }

    Column(
        modifier = Modifier
            .fillMaxSize()
            .padding(16.dp)
            .verticalScroll(rememberScrollState()),
        verticalArrangement = Arrangement.spacedBy(14.dp)
    ) {
        Card(
            modifier = Modifier.fillMaxWidth(),
            colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.tertiaryContainer)
        ) {
            Column(modifier = Modifier.padding(14.dp)) {
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Icon(Icons.Default.Terminal, contentDescription = null, tint = MaterialTheme.colorScheme.onTertiaryContainer)
                    Spacer(modifier = Modifier.width(8.dp))
                    Text(
                        text = "Termux Self-Hosting Commands",
                        fontWeight = FontWeight.Bold,
                        color = MaterialTheme.colorScheme.onTertiaryContainer
                    )
                }
                Spacer(modifier = Modifier.height(6.dp))
                Text(
                    text = "Run these commands inside Termux on your Android device to start your Python Telegram bot locally without developer servers.",
                    fontSize = 12.sp,
                    color = MaterialTheme.colorScheme.onTertiaryContainer.copy(alpha = 0.9f)
                )
            }
        }

        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically
        ) {
            Text(
                text = "Termux Deployment Script (termux_start_bot.sh)",
                fontSize = 13.sp,
                fontWeight = FontWeight.Bold,
                color = M3TextPrimary
            )
            Button(onClick = { onCopy(setupScript, "Termux Setup Script") }) {
                Icon(Icons.Default.ContentCopy, contentDescription = null, modifier = Modifier.size(16.dp))
                Spacer(modifier = Modifier.width(4.dp))
                Text("Copy Script")
            }
        }

        Surface(
            color = MaterialTheme.colorScheme.surfaceContainerHighest,
            shape = RoundedCornerShape(8.dp),
            modifier = Modifier
                .fillMaxWidth()
                .height(180.dp)
        ) {
            Text(
                text = setupScript,
                fontFamily = FontFamily.Monospace,
                fontSize = 11.sp,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
                modifier = Modifier.padding(10.dp)
            )
        }

        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically
        ) {
            Text(
                text = "Environment Variables (.env)",
                fontSize = 13.sp,
                fontWeight = FontWeight.Bold,
                color = M3TextPrimary
            )
            OutlinedButton(onClick = { onCopy(envTemplate, ".env file") }) {
                Icon(Icons.Default.ContentCopy, contentDescription = null, modifier = Modifier.size(16.dp))
                Spacer(modifier = Modifier.width(4.dp))
                Text("Copy .env")
            }
        }

        Surface(
            color = MaterialTheme.colorScheme.surfaceContainerHighest,
            shape = RoundedCornerShape(8.dp),
            modifier = Modifier
                .fillMaxWidth()
                .height(120.dp)
        ) {
            Text(
                text = envTemplate,
                fontFamily = FontFamily.Monospace,
                fontSize = 11.sp,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
                modifier = Modifier.padding(10.dp)
            )
        }

        Text(
            text = "Redacted Bot Logs & Console Output",
            fontSize = 13.sp,
            fontWeight = FontWeight.Bold,
            color = M3TextPrimary
        )

        Surface(
            color = MaterialTheme.colorScheme.surfaceContainerLowest,
            shape = RoundedCornerShape(8.dp),
            modifier = Modifier
                .fillMaxWidth()
                .height(160.dp)
        ) {
            LazyColumn(
                modifier = Modifier
                    .fillMaxSize()
                    .padding(8.dp)
            ) {
                if (logs.isEmpty()) {
                    item {
                        Text(
                            text = "[Console Ready] No active logs recorded yet.",
                            fontSize = 11.sp,
                            fontFamily = FontFamily.Monospace,
                            color = M3TextMuted
                        )
                    }
                } else {
                    items(logs) { logLine ->
                        Text(
                            text = logLine,
                            fontSize = 11.sp,
                            fontFamily = FontFamily.Monospace,
                            color = MaterialTheme.colorScheme.onSurface
                        )
                    }
                }
            }
        }
    }
}

@Composable
fun AiAndDatabaseTab(
    project: BotProject,
    onSaveProject: (BotProject) -> Unit
) {
    var aiEnabled by remember(project) { mutableStateOf(project.aiConfig.enabled) }
    var aiProvider by remember(project) { mutableStateOf(project.aiConfig.provider) }
    var aiApiKey by remember(project) { mutableStateOf(project.aiConfig.apiKey) }
    var aiModel by remember(project) { mutableStateOf(project.aiConfig.modelName) }
    var systemPrompt by remember(project) { mutableStateOf(project.aiConfig.systemPrompt) }

    var dbEnabled by remember(project) { mutableStateOf(project.databaseConfig.enabled) }
    var dbType by remember(project) { mutableStateOf(project.databaseConfig.dbType) }
    var dbHost by remember(project) { mutableStateOf(project.databaseConfig.host) }
    var dbName by remember(project) { mutableStateOf(project.databaseConfig.dbName) }
    var dbUser by remember(project) { mutableStateOf(project.databaseConfig.username) }

    Column(
        modifier = Modifier
            .fillMaxSize()
            .padding(16.dp)
            .verticalScroll(rememberScrollState()),
        verticalArrangement = Arrangement.spacedBy(16.dp)
    ) {
        Card(
            modifier = Modifier.fillMaxWidth(),
            colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surfaceContainerHigh)
        ) {
            Column(modifier = Modifier.padding(14.dp)) {
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        Icon(Icons.Default.Psychology, contentDescription = null, tint = MaterialTheme.colorScheme.primary)
                        Spacer(modifier = Modifier.width(8.dp))
                        Text("AI Provider Integration", fontWeight = FontWeight.Bold, color = M3TextPrimary)
                    }
                    Switch(checked = aiEnabled, onCheckedChange = { aiEnabled = it })
                }

                if (aiEnabled) {
                    Spacer(modifier = Modifier.height(10.dp))
                    OutlinedTextField(
                        value = aiApiKey,
                        onValueChange = { aiApiKey = it },
                        label = { Text("AI Provider API Key (User Provided)") },
                        modifier = Modifier.fillMaxWidth()
                    )
                    Spacer(modifier = Modifier.height(8.dp))
                    Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                        OutlinedTextField(
                            value = aiProvider,
                            onValueChange = { aiProvider = it },
                            label = { Text("Provider (Gemini/OpenAI)") },
                            modifier = Modifier.weight(1f)
                        )
                        OutlinedTextField(
                            value = aiModel,
                            onValueChange = { aiModel = it },
                            label = { Text("Model Name") },
                            modifier = Modifier.weight(1f)
                        )
                    }
                    Spacer(modifier = Modifier.height(8.dp))
                    OutlinedTextField(
                        value = systemPrompt,
                        onValueChange = { systemPrompt = it },
                        label = { Text("AI System Instructions") },
                        modifier = Modifier.fillMaxWidth()
                    )
                }
            }
        }

        Card(
            modifier = Modifier.fillMaxWidth(),
            colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surfaceContainerHigh)
        ) {
            Column(modifier = Modifier.padding(14.dp)) {
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        Icon(Icons.Default.Storage, contentDescription = null, tint = MaterialTheme.colorScheme.primary)
                        Spacer(modifier = Modifier.width(8.dp))
                        Text("Database Connector", fontWeight = FontWeight.Bold, color = M3TextPrimary)
                    }
                    Switch(checked = dbEnabled, onCheckedChange = { dbEnabled = it })
                }

                if (dbEnabled) {
                    Spacer(modifier = Modifier.height(10.dp))
                    Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                        OutlinedTextField(
                            value = dbType,
                            onValueChange = { dbType = it },
                            label = { Text("Type (SQLite/Postgres)") },
                            modifier = Modifier.weight(1f)
                        )
                        OutlinedTextField(
                            value = dbHost,
                            onValueChange = { dbHost = it },
                            label = { Text("Host Address") },
                            modifier = Modifier.weight(1f)
                        )
                    }
                    Spacer(modifier = Modifier.height(8.dp))
                    Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                        OutlinedTextField(
                            value = dbName,
                            onValueChange = { dbName = it },
                            label = { Text("Database Name") },
                            modifier = Modifier.weight(1f)
                        )
                        OutlinedTextField(
                            value = dbUser,
                            onValueChange = { dbUser = it },
                            label = { Text("Database User") },
                            modifier = Modifier.weight(1f)
                        )
                    }
                }
            }
        }

        Button(
            onClick = {
                val updated = project.copy(
                    aiConfig = project.aiConfig.copy(
                        enabled = aiEnabled,
                        provider = aiProvider,
                        apiKey = aiApiKey,
                        modelName = aiModel,
                        systemPrompt = systemPrompt
                    ),
                    databaseConfig = project.databaseConfig.copy(
                        enabled = dbEnabled,
                        dbType = dbType,
                        host = dbHost,
                        dbName = dbName,
                        username = dbUser
                    )
                )
                onSaveProject(updated)
            },
            modifier = Modifier.fillMaxWidth()
        ) {
            Icon(Icons.Default.Save, contentDescription = null)
            Spacer(modifier = Modifier.width(8.dp))
            Text("Save Integrations Configuration")
        }
    }
}
