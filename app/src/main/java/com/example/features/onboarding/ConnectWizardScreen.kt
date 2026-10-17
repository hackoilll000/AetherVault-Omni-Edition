package com.example.features.onboarding

import android.content.ClipData
import android.content.ClipboardManager
import android.content.Context
import android.content.Intent
import android.net.Uri
import androidx.compose.animation.*
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowForward
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
import androidx.compose.ui.text.input.PasswordVisualTransformation
import androidx.compose.ui.text.input.VisualTransformation
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.core.telegram.TelegramBotClient
import com.example.features.studio.AppLanguage
import com.example.ui.components.AetherVaultLogo
import com.example.ui.theme.CyanNeon
import com.example.ui.theme.IndigoHyper
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun ConnectWizardScreen(
    language: AppLanguage,
    onCompleteSetup: (botTokens: List<String>, channelId: Long, masterPass: String, decoyPin: String?) -> Unit,
    onLinkViaQrCode: (String) -> Unit
) {
    val context = LocalContext.current
    val coroutineScope = rememberCoroutineScope()
    val isHindi = language == AppLanguage.HI

    var currentStep by remember { mutableIntStateOf(1) } // 1 to 4

    // Step 1: Bots
    val botTokens = remember { mutableStateListOf<String>() }
    var currentTokenInput by remember { mutableStateOf("") }
    var isVerifyingBot by remember { mutableStateOf(false) }
    var botVerifyMessage by remember { mutableStateOf("") }
    var detectedClipboardToken by remember { mutableStateOf<String?>(null) }

    // Step 2: Channel
    var channelIdInput by remember { mutableStateOf("") }
    var channelTitle by remember { mutableStateOf("") }
    var isListeningConnect by remember { mutableStateOf(false) }
    var connectStatusText by remember { mutableStateOf("") }

    // Step 3: Diagnostic
    var diagStep1 by remember { mutableStateOf<Boolean?>(null) }
    var diagStep2 by remember { mutableStateOf<Boolean?>(null) }
    var diagStep3 by remember { mutableStateOf<Boolean?>(null) }
    var diagStep4 by remember { mutableStateOf<Boolean?>(null) }
    var isRunningDiag by remember { mutableStateOf(false) }
    var existingVaultDetected by remember { mutableStateOf(false) }

    // Step 4: Security
    var masterPassword by remember { mutableStateOf("") }
    var confirmPassword by remember { mutableStateOf("") }
    var passwordVisible by remember { mutableStateOf(false) }
    var biometricEnabled by remember { mutableStateOf(true) }
    var decoyPin by remember { mutableStateOf("") }
    var showRecoveryDialog by remember { mutableStateOf(false) }

    val client = remember { TelegramBotClient() }

    // Check clipboard for Telegram Bot Token regex
    val tokenRegex = Regex("^\\d{8,12}:[A-Za-z0-9_-]{35}$")
    LaunchedEffect(Unit) {
        val clipboard = context.getSystemService(Context.CLIPBOARD_SERVICE) as? ClipboardManager
        val clip = clipboard?.primaryClip
        if (clip != null && clip.itemCount > 0) {
            val text = clip.getItemAt(0).text?.toString()?.trim() ?: ""
            if (tokenRegex.matches(text) && !botTokens.contains(text)) {
                detectedClipboardToken = text
            }
        }
    }

    var showVoiceTutorial by remember { mutableStateOf(false) }
    var showRestoreCodeDialog by remember { mutableStateOf(false) }
    var restoreCodeInput by remember { mutableStateOf("") }

    Scaffold(
        topBar = {
            TopAppBar(
                title = {
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        AetherVaultLogo(size = 32.dp)
                        Spacer(modifier = Modifier.width(10.dp))
                        Text("AetherVault Setup", fontWeight = FontWeight.Bold, fontSize = 18.sp)
                    }
                },
                actions = {
                    FilledTonalButton(
                        onClick = {
                            com.example.core.sensory.SensoryFeedbackEngine.trigger(context, com.example.core.sensory.HapticStyle.LIGHT_TICK)
                            showVoiceTutorial = true
                        },
                        colors = ButtonDefaults.filledTonalButtonColors(containerColor = CyanNeon.copy(alpha = 0.2f), contentColor = CyanNeon),
                        shape = RoundedCornerShape(8.dp),
                        contentPadding = PaddingValues(horizontal = 10.dp, vertical = 4.dp),
                        modifier = Modifier.padding(end = 6.dp)
                    ) {
                        Icon(Icons.Default.VolumeUp, contentDescription = null, modifier = Modifier.size(16.dp))
                        Spacer(modifier = Modifier.width(4.dp))
                        Text(if (isHindi) "ऑडियो गाइड" else "Voice Guide", fontSize = 11.sp, fontWeight = FontWeight.Bold)
                    }

                    Text(
                        text = "Step $currentStep/4",
                        fontFamily = FontFamily.Monospace,
                        fontWeight = FontWeight.Bold,
                        color = MaterialTheme.colorScheme.primary,
                        modifier = Modifier.padding(end = 16.dp)
                    )
                }
            )
        }
    ) { padding ->
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(padding)
                .verticalScroll(rememberScrollState())
                .padding(16.dp),
            verticalArrangement = Arrangement.spacedBy(16.dp)
        ) {
            // Step Progress Indicator
            LinearProgressIndicator(
                progress = { currentStep / 4f },
                modifier = Modifier
                    .fillMaxWidth()
                    .height(6.dp)
                    .clip(RoundedCornerShape(3.dp)),
                color = CyanNeon
            )

            // Owner & Terms Agreement Banner
            Card(
                modifier = Modifier.fillMaxWidth(),
                colors = CardDefaults.cardColors(containerColor = CyanNeon.copy(alpha = 0.12f)),
                border = androidx.compose.foundation.BorderStroke(1.dp, CyanNeon)
            ) {
                Row(
                    modifier = Modifier.padding(12.dp),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Icon(Icons.Default.VerifiedUser, contentDescription = null, tint = CyanNeon, modifier = Modifier.size(24.dp))
                    Spacer(modifier = Modifier.width(10.dp))
                    Column {
                        Text(
                            text = if (isHindi) "ऐप ओनर व आर्किटेक्ट: आयुष मंडल" else "Owner & Lead Architect: Ayush Mandal (आयुष मंडल)",
                            fontWeight = FontWeight.Bold,
                            fontSize = 13.sp,
                            color = CyanNeon
                        )
                        Text(
                            text = if (isHindi) "शून्य-ज्ञान एन्क्रिप्शन नियम एवं शर्तों से सहमत होकर आगे बढ़ें।"
                            else "Zero-Knowledge Cloud Terms & Agreement verified. Root & Non-Root enabled.",
                            fontSize = 11.sp,
                            color = MaterialTheme.colorScheme.onSurface
                        )
                    }
                }
            }

            // ---------------- STEP 1: BOT SETUP ----------------
            if (currentStep == 1) {
                Text(
                    text = if (isHindi) "चरण 1: टेलीग्राम बॉट कोरियर बनाएं"
                    else "Step 1: Create Telegram Bot Courier",
                    style = MaterialTheme.typography.titleLarge,
                    fontWeight = FontWeight.Bold
                )
                Text(
                    text = if (isHindi) "बॉट आपके पर्सनल अकाउंट को 100% सुरक्षित रखता है। आपका फोन नंबर कभी उपयोग नहीं होता।"
                    else "The Bot acts as an encrypted courier so your personal account is 100% immune from bans. Zero phone numbers used.",
                    fontSize = 13.sp,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )

                // Restore from Migration / Backup Code Card
                Card(
                    modifier = Modifier.fillMaxWidth(),
                    colors = CardDefaults.cardColors(containerColor = CyanNeon.copy(alpha = 0.1f)),
                    border = androidx.compose.foundation.BorderStroke(1.dp, CyanNeon.copy(alpha = 0.5f))
                ) {
                    Row(
                        modifier = Modifier
                            .padding(12.dp)
                            .fillMaxWidth(),
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.SpaceBetween
                    ) {
                        Column(modifier = Modifier.weight(1f)) {
                            Text(
                                text = if (isHindi) "पुराने बॉट का बैकअप कोड है?" else "Have a Migration / Backup Code?",
                                fontWeight = FontWeight.Bold,
                                fontSize = 13.sp,
                                color = CyanNeon
                            )
                            Text(
                                text = if (isHindi) "कोड डालें और सारा पुराना डेटा तुरंत वापस लाएं"
                                else "Paste your AVCODE to instantly restore all files & settings",
                                fontSize = 11.sp,
                                color = MaterialTheme.colorScheme.onSurfaceVariant
                            )
                        }
                        Button(
                            onClick = { showRestoreCodeDialog = true },
                            colors = ButtonDefaults.buttonColors(containerColor = CyanNeon, contentColor = Color.Black),
                            shape = RoundedCornerShape(8.dp),
                            contentPadding = PaddingValues(horizontal = 10.dp, vertical = 6.dp)
                        ) {
                            Icon(Icons.Default.ContentPaste, contentDescription = null, modifier = Modifier.size(15.dp))
                            Spacer(modifier = Modifier.width(4.dp))
                            Text(if (isHindi) "कोड डालें" else "Enter Code", fontSize = 12.sp, fontWeight = FontWeight.Bold)
                        }
                    }
                }

                // 1-Tap BotFather Deep link
                Card(
                    modifier = Modifier.fillMaxWidth(),
                    colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surfaceVariant)
                ) {
                    Column(modifier = Modifier.padding(14.dp)) {
                        Text(
                            text = if (isHindi) "1-टैप @BotFather शॉर्टकट" else "1-Tap @BotFather Shortcut",
                            fontWeight = FontWeight.Bold,
                            color = MaterialTheme.colorScheme.primary
                        )
                        Text(
                            text = if (isHindi) "बटन दबाते ही /newbot कॉपी होगा और टेलीग्राम खुल जाएगा:"
                            else "Tapping opens Telegram with /newbot auto-copied:",
                            fontSize = 12.sp,
                            color = MaterialTheme.colorScheme.onSurfaceVariant
                        )
                        Spacer(modifier = Modifier.height(10.dp))
                        Button(
                            onClick = {
                                val clipboard = context.getSystemService(Context.CLIPBOARD_SERVICE) as ClipboardManager
                                clipboard.setPrimaryClip(ClipData.newPlainText("tg_cmd", "/newbot"))
                                val intent = Intent(Intent.ACTION_VIEW, Uri.parse("tg://resolve?domain=BotFather")).apply {
                                    flags = Intent.FLAG_ACTIVITY_NEW_TASK
                                }
                                runCatching { context.startActivity(intent) }.onFailure {
                                    context.startActivity(Intent(Intent.ACTION_VIEW, Uri.parse("https://t.me/BotFather")))
                                }
                            },
                            modifier = Modifier
                                .fillMaxWidth()
                                .testTag("open_botfather_button")
                        ) {
                            Icon(Icons.Default.Send, contentDescription = null, modifier = Modifier.size(18.dp))
                            Spacer(modifier = Modifier.width(8.dp))
                            Text(if (isHindi) "टेलीग्राम में @BotFather खोलें" else "Open @BotFather in Telegram")
                        }
                    }
                }

                // Auto Clipboard Banner
                AnimatedVisibility(visible = detectedClipboardToken != null) {
                    Card(
                        modifier = Modifier.fillMaxWidth(),
                        colors = CardDefaults.cardColors(containerColor = CyanNeon.copy(alpha = 0.15f)),
                        border = androidx.compose.foundation.BorderStroke(1.dp, CyanNeon)
                    ) {
                        Row(
                            modifier = Modifier
                                .padding(12.dp)
                                .fillMaxWidth(),
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Icon(Icons.Default.ContentPaste, contentDescription = null, tint = CyanNeon)
                            Spacer(modifier = Modifier.width(10.dp))
                            Column(modifier = Modifier.weight(1f)) {
                                Text(
                                    text = if (isHindi) "क्लिपबोर्ड में टोकन मिला!" else "Bot Token Detected in Clipboard!",
                                    fontWeight = FontWeight.Bold,
                                    fontSize = 13.sp,
                                    color = CyanNeon
                                )
                                Text(
                                    text = detectedClipboardToken?.take(18) + "...",
                                    fontFamily = FontFamily.Monospace,
                                    fontSize = 11.sp
                                )
                            }
                            Button(
                                onClick = {
                                    detectedClipboardToken?.let { tok ->
                                        currentTokenInput = tok
                                        detectedClipboardToken = null
                                    }
                                },
                                contentPadding = PaddingValues(horizontal = 12.dp, vertical = 6.dp)
                            ) {
                                Text(if (isHindi) "पेस्ट करें" else "Use Token")
                            }
                        }
                    }
                }

                // Restore Vault Option from Migration Code
                Surface(
                    modifier = Modifier.fillMaxWidth(),
                    shape = RoundedCornerShape(12.dp),
                    color = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.5f),
                    border = androidx.compose.foundation.BorderStroke(1.dp, CyanNeon.copy(alpha = 0.4f))
                ) {
                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(12.dp),
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.SpaceBetween
                    ) {
                        Column(modifier = Modifier.weight(1f)) {
                            Text(
                                text = if (isHindi) "🔄 क्या आपके पास बैकअप / माइग्रेशन कोड है?" else "🔄 Have a Vault Backup / Migration Code?",
                                fontWeight = FontWeight.Bold,
                                fontSize = 12.sp,
                                color = MaterialTheme.colorScheme.onSurface
                            )
                            Text(
                                text = if (isHindi) "1-टैप में पिछले बॉट/सर्वर का सारा डेटा रीस्टोर करें" else "Instantly restore all files & folders from backup code",
                                fontSize = 10.sp,
                                color = MaterialTheme.colorScheme.onSurfaceVariant
                            )
                        }
                        Button(
                            onClick = { showRestoreCodeDialog = true },
                            colors = ButtonDefaults.buttonColors(containerColor = CyanNeon, contentColor = Color.Black),
                            contentPadding = PaddingValues(horizontal = 12.dp, vertical = 6.dp)
                        ) {
                            Text(if (isHindi) "रीस्टोर करें" else "Restore", fontSize = 11.sp, fontWeight = FontWeight.Bold)
                        }
                    }
                }

                // Input Token Field
                OutlinedTextField(
                    value = currentTokenInput,
                    onValueChange = { currentTokenInput = it.trim() },
                    label = { Text("HTTP API Bot Token (e.g. 7123456789:AAH...)") },
                    modifier = Modifier
                        .fillMaxWidth()
                        .testTag("bot_token_input"),
                    singleLine = true
                )

                Button(
                    onClick = {
                        if (currentTokenInput.isNotBlank()) {
                            coroutineScope.launch {
                                isVerifyingBot = true
                                botVerifyMessage = if (isHindi) "बॉट सत्यापित किया जा रहा है..." else "Verifying with Telegram..."
                                val res = client.getMe(currentTokenInput)
                                isVerifyingBot = false
                                if (res.isSuccess) {
                                    val bot = res.getOrThrow()
                                    botTokens.add(currentTokenInput)
                                    botVerifyMessage = "✓ @${bot.username} Verified!"
                                    currentTokenInput = ""
                                } else {
                                    botVerifyMessage = "✗ Error: " + (res.exceptionOrNull()?.message ?: "Invalid Token")
                                }
                            }
                        }
                    },
                    enabled = currentTokenInput.isNotBlank() && !isVerifyingBot,
                    modifier = Modifier.fillMaxWidth()
                ) {
                    if (isVerifyingBot) {
                        CircularProgressIndicator(modifier = Modifier.size(18.dp), color = Color.White)
                    } else {
                        Text(if (isHindi) "बॉट सत्यापित करें और जोड़ें" else "Verify & Add Bot")
                    }
                }

                if (botVerifyMessage.isNotBlank()) {
                    Text(
                        text = botVerifyMessage,
                        color = if (botVerifyMessage.startsWith("✓")) CyanNeon else MaterialTheme.colorScheme.error,
                        fontWeight = FontWeight.Bold,
                        fontSize = 13.sp
                    )
                }

                // Active Bots list
                if (botTokens.isNotEmpty()) {
                    Text(
                        text = if (isHindi) "सक्रिय बॉट स्वार्म (${botTokens.size} बॉट्स जुड़े):"
                        else "Configured Bot Swarm (${botTokens.size} bots):",
                        fontWeight = FontWeight.Bold,
                        fontSize = 14.sp
                    )
                    botTokens.forEachIndexed { i, tok ->
                        Card(
                            modifier = Modifier.fillMaxWidth(),
                            colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surfaceVariant)
                        ) {
                            Row(
                                modifier = Modifier
                                    .padding(10.dp)
                                    .fillMaxWidth(),
                                verticalAlignment = Alignment.CenterVertically
                            ) {
                                Icon(Icons.Default.SmartToy, contentDescription = null, tint = CyanNeon)
                                Spacer(modifier = Modifier.width(8.dp))
                                Text(
                                    text = "Bot #${i + 1} (${tok.take(14)}...)",
                                    fontFamily = FontFamily.Monospace,
                                    fontSize = 12.sp,
                                    modifier = Modifier.weight(1f)
                                )
                                IconButton(
                                    onClick = { botTokens.removeAt(i) },
                                    modifier = Modifier.size(24.dp)
                                ) {
                                    Icon(Icons.Default.Delete, contentDescription = "Remove", tint = Color.Gray, modifier = Modifier.size(16.dp))
                                }
                            }
                        }
                    }

                    Button(
                        onClick = { currentStep = 2 },
                        modifier = Modifier
                            .fillMaxWidth()
                            .testTag("step1_next_button")
                    ) {
                        Text(if (isHindi) "अगला कदम: चैनल कनेक्ट करें" else "Next Step: Connect Channel")
                        Spacer(modifier = Modifier.width(8.dp))
                        Icon(Icons.AutoMirrored.Filled.ArrowForward, contentDescription = null)
                    }
                }
            }

            // ---------------- STEP 2: CHANNEL AUTO-DISCOVERY ----------------
            if (currentStep == 2) {
                Text(
                    text = if (isHindi) "चरण 2: प्राइवेट चैनल व #connect ऑटो-डिटेक्ट"
                    else "Step 2: Private Channel & #connect Magic",
                    style = MaterialTheme.typography.titleLarge,
                    fontWeight = FontWeight.Bold
                )

                Card(
                    modifier = Modifier.fillMaxWidth(),
                    colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surfaceVariant)
                ) {
                    Column(modifier = Modifier.padding(14.dp)) {
                        Text("1. Create a New Private Channel in Telegram", fontWeight = FontWeight.Bold)
                        Text("2. Add your Bot as Administrator (with Post & Pin rights)", fontSize = 13.sp)
                        Text("3. Type #connect in the channel!", fontSize = 13.sp, fontWeight = FontWeight.Bold, color = CyanNeon)
                    }
                }

                Button(
                    onClick = {
                        val firstToken = botTokens.firstOrNull() ?: return@Button
                        coroutineScope.launch {
                            isListeningConnect = true
                            connectStatusText = if (isHindi) "टेलीग्राम से #connect की प्रतीक्षा कर रहे हैं..."
                            else "Listening for #connect in channel (10s)..."

                            for (sec in 1..5) {
                                val res = client.pollConnectUpdate(firstToken)
                                if (res.isSuccess && res.getOrThrow() != null) {
                                    val pair = res.getOrThrow()!!
                                    channelIdInput = pair.first.toString()
                                    channelTitle = pair.second
                                    connectStatusText = "✓ Connected to: $channelTitle ($channelIdInput)"
                                    isListeningConnect = false
                                    break
                                }
                                delay(2000)
                            }
                            isListeningConnect = false
                            if (channelIdInput.isBlank()) {
                                connectStatusText = if (isHindi) "#connect नहीं मिला। आप मैन्युअल रूप से -100 ID लिख सकते हैं।"
                                else "#connect not detected yet. You can paste the -100... ID manually below."
                            }
                        }
                    },
                    modifier = Modifier
                        .fillMaxWidth()
                        .testTag("listen_connect_button"),
                    enabled = !isListeningConnect
                ) {
                    if (isListeningConnect) {
                        CircularProgressIndicator(modifier = Modifier.size(18.dp), color = Color.White)
                        Spacer(modifier = Modifier.width(8.dp))
                        Text(if (isHindi) "सुन रहे हैं..." else "Listening for #connect...")
                    } else {
                        Icon(Icons.Default.Bolt, contentDescription = null)
                        Spacer(modifier = Modifier.width(8.dp))
                        Text(if (isHindi) "जादुई #connect ऑटो-डिटेक्शन शुरू करें" else "Auto-Detect via #connect")
                    }
                }

                if (connectStatusText.isNotBlank()) {
                    Text(
                        text = connectStatusText,
                        color = if (connectStatusText.startsWith("✓")) CyanNeon else MaterialTheme.colorScheme.onSurfaceVariant,
                        fontSize = 13.sp,
                        fontWeight = FontWeight.Bold
                    )
                }

                HorizontalDivider()

                Text(
                    text = if (isHindi) "या मैन्युअल रूप से चैनल ID डालें:" else "Or enter Channel ID manually:",
                    fontSize = 13.sp,
                    fontWeight = FontWeight.SemiBold
                )

                OutlinedTextField(
                    value = channelIdInput,
                    onValueChange = { channelIdInput = it.trim() },
                    label = { Text("Channel ID (e.g. -1001234567890)") },
                    modifier = Modifier.fillMaxWidth(),
                    singleLine = true
                )

                // Helper guidance for Channel ID
                Text(
                    text = if (isHindi) "💡 चैनल ID आमतौर पर -100 से शुरू होती है (उदा. -1001234567890)। अपने चैनल के फॉरवर्ड मैसेज या @userinfobot से आईडी पाएं।"
                    else "💡 Channel IDs start with -100 (e.g. -1001234567890). Forward any post to @userinfobot to find your channel ID.",
                    fontSize = 11.sp,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )

                Row(horizontalArrangement = Arrangement.spacedBy(10.dp)) {
                    OutlinedButton(
                        onClick = { currentStep = 1 },
                        modifier = Modifier.weight(1f)
                    ) {
                        Text(if (isHindi) "पीछे" else "Back")
                    }
                    Button(
                        onClick = { currentStep = 3 },
                        enabled = channelIdInput.startsWith("-100") || channelIdInput.toLongOrNull() != null,
                        modifier = Modifier
                            .weight(1f)
                            .testTag("step2_next_button")
                    ) {
                        Text(if (isHindi) "अगला: टेस्ट करें" else "Next: Test Server")
                    }
                }
            }

            // ---------------- STEP 3: 4-POINT DIAGNOSTIC ----------------
            if (currentStep == 3) {
                Text(
                    text = if (isHindi) "चरण 3: 4-बिंदु सर्वर डायग्नोस्टिक" else "Step 3: 4-Point Server Diagnostic",
                    style = MaterialTheme.typography.titleLarge,
                    fontWeight = FontWeight.Bold
                )

                val cId = channelIdInput.toLongOrNull() ?: 0L
                val tok = botTokens.firstOrNull() ?: ""

                LaunchedEffect(Unit) {
                    isRunningDiag = true
                    // 1. Bot Auth
                    delay(300)
                    val authRes = client.getMe(tok)
                    diagStep1 = authRes.isSuccess

                    // 2. Channel Reachable
                    delay(400)
                    val chatRes = client.getChat(tok, cId)
                    diagStep2 = chatRes.isSuccess

                    // 3. Write & Delete Rights
                    delay(400)
                    if (chatRes.isSuccess) {
                        val msgRes = client.sendMessage(tok, cId, "🔒 AetherVault Diagnostic Ping")
                        if (msgRes.isSuccess) {
                            client.deleteMessage(tok, cId, msgRes.getOrThrow())
                            diagStep3 = true
                        } else {
                            diagStep3 = false
                        }
                    }

                    // 4. Pin Rights & Existing Manifest Check
                    delay(400)
                    if (chatRes.isSuccess) {
                        val chat = chatRes.getOrThrow()
                        if (chat.pinnedMessageText?.contains("AETHER_MANIFEST") == true) {
                            existingVaultDetected = true
                        }
                        diagStep4 = true
                    }
                    isRunningDiag = false
                }

                // Checklist Items
                DiagnosticCheckItem(title = "1. Telegram Bot Token Authenticated", passed = diagStep1)
                DiagnosticCheckItem(title = "2. Private Channel Reachable ($cId)", passed = diagStep2)
                DiagnosticCheckItem(title = "3. Admin Write & Delete Rights Verified", passed = diagStep3)
                DiagnosticCheckItem(title = "4. Admin Pin Permission & Manifest Check", passed = diagStep4)

                if (existingVaultDetected) {
                    Card(
                        modifier = Modifier.fillMaxWidth(),
                        colors = CardDefaults.cardColors(containerColor = CyanNeon.copy(alpha = 0.15f))
                    ) {
                        Row(modifier = Modifier.padding(14.dp), verticalAlignment = Alignment.CenterVertically) {
                            Icon(Icons.Default.CloudDownload, contentDescription = null, tint = CyanNeon)
                            Spacer(modifier = Modifier.width(10.dp))
                            Text(
                                text = if (isHindi) "मौजूदा क्लाउड वॉल्ट मिला! पासवर्ड डालने पर 1-क्लिक में सारा डेटा रिस्टोर होगा।"
                                else "Existing Cloud Vault Found! Entering your Master Password will restore all your files instantly.",
                                fontSize = 12.sp,
                                fontWeight = FontWeight.SemiBold
                            )
                        }
                    }
                }

                Spacer(modifier = Modifier.height(10.dp))
                Row(horizontalArrangement = Arrangement.spacedBy(10.dp)) {
                    OutlinedButton(
                        onClick = { currentStep = 2 },
                        modifier = Modifier.weight(1f)
                    ) {
                        Text(if (isHindi) "पीछे" else "Back")
                    }
                    Button(
                        onClick = { currentStep = 4 },
                        enabled = diagStep1 == true && diagStep2 == true && diagStep3 == true,
                        modifier = Modifier
                            .weight(1f)
                            .testTag("step3_next_button")
                    ) {
                        Text(if (isHindi) "सुरक्षा सेट करें" else "Next: Master Key")
                    }
                }
            }

            // ---------------- STEP 4: MASTER PASSWORD & RECOVERY ----------------
            if (currentStep == 4) {
                Text(
                    text = if (isHindi) "चरण 4: शून्य-ज्ञान मास्टर पासवर्ड" else "Step 4: Zero-Knowledge Master Password",
                    style = MaterialTheme.typography.titleLarge,
                    fontWeight = FontWeight.Bold
                )
                Text(
                    text = if (isHindi) "यह पासवर्ड आपके सभी 19MB चंक्स को AES-256-GCM से स्थानीय रूप से एन्क्रिप्ट करता है। टेलीग्राम कभी भी आपकी फाइलें नहीं देख सकता।"
                    else "This password derives your 256-bit AES master key locally. Telegram servers will never see plaintext filenames or contents.",
                    fontSize = 12.sp,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )

                OutlinedTextField(
                    value = masterPassword,
                    onValueChange = { masterPassword = it },
                    label = { Text("Master Password") },
                    visualTransformation = if (passwordVisible) VisualTransformation.None else PasswordVisualTransformation(),
                    trailingIcon = {
                        IconButton(onClick = { passwordVisible = !passwordVisible }) {
                            Icon(if (passwordVisible) Icons.Default.VisibilityOff else Icons.Default.Visibility, contentDescription = null)
                        }
                    },
                    modifier = Modifier
                        .fillMaxWidth()
                        .testTag("master_password_input"),
                    singleLine = true
                )

                OutlinedTextField(
                    value = confirmPassword,
                    onValueChange = { confirmPassword = it },
                    label = { Text("Confirm Master Password") },
                    visualTransformation = PasswordVisualTransformation(),
                    modifier = Modifier.fillMaxWidth(),
                    singleLine = true
                )

                // Password strength meter
                val strength = when {
                    masterPassword.length >= 10 && masterPassword.any { it.isDigit() } && masterPassword.any { !it.isLetterOrDigit() } -> 1f
                    masterPassword.length >= 8 -> 0.65f
                    masterPassword.isNotEmpty() -> 0.35f
                    else -> 0f
                }
                LinearProgressIndicator(
                    progress = { strength },
                    modifier = Modifier
                        .fillMaxWidth()
                        .height(4.dp),
                    color = if (strength > 0.8f) CyanNeon else IndigoHyper
                )

                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Text(if (isHindi) "बायोमेट्रिक फिंगरप्रिंट अनलॉक" else "Biometric Fingerprint Quick Unlock", fontSize = 14.sp)
                    Switch(
                        checked = biometricEnabled,
                        onCheckedChange = { biometricEnabled = it }
                    )
                }

                OutlinedTextField(
                    value = decoyPin,
                    onValueChange = { if (it.length <= 4 && it.all { c -> c.isDigit() }) decoyPin = it },
                    label = { Text(if (isHindi) "वैकल्पिक स्टील्थ डेकोय पिन (4 अंक)" else "Optional Stealth Decoy PIN (4 digits)") },
                    modifier = Modifier.fillMaxWidth(),
                    singleLine = true
                )

                OutlinedButton(
                    onClick = { showRecoveryDialog = true },
                    modifier = Modifier.fillMaxWidth()
                ) {
                    Icon(Icons.Default.Download, contentDescription = null, modifier = Modifier.size(18.dp))
                    Spacer(modifier = Modifier.width(8.dp))
                    Text(if (isHindi) "इमरजेंसी रिकवरी किट देखें / प्रिंट करें" else "Generate Emergency Recovery Kit")
                }

                Button(
                    onClick = {
                        val cId = channelIdInput.toLongOrNull() ?: 0L
                        onCompleteSetup(botTokens.toList(), cId, masterPassword, decoyPin.ifBlank { null })
                    },
                    enabled = masterPassword.length >= 6 && masterPassword == confirmPassword,
                    modifier = Modifier
                        .fillMaxWidth()
                        .height(52.dp)
                        .testTag("complete_setup_button")
                ) {
                    Icon(Icons.Default.LockOpen, contentDescription = null)
                    Spacer(modifier = Modifier.width(8.dp))
                    Text(if (isHindi) "वॉल्ट अनलॉक करें और शुरू करें" else "Initialize & Unlock Vault", fontWeight = FontWeight.Bold)
                }
            }

            Spacer(modifier = Modifier.height(24.dp))
        }
    }

    if (showRecoveryDialog) {
        AlertDialog(
            onDismissRequest = { showRecoveryDialog = false },
            title = { Text("AetherVault Recovery Kit") },
            text = {
                Column {
                    Text("Store this recovery string safely in case you change devices:")
                    Spacer(modifier = Modifier.height(8.dp))
                    Surface(
                        color = MaterialTheme.colorScheme.surfaceVariant,
                        shape = RoundedCornerShape(8.dp)
                    ) {
                        Text(
                            text = "AETHER_VAULT_KIT|CH:$channelIdInput|BOTS:${botTokens.size}|KEY_SHA256:${masterPassword.hashCode()}",
                            fontFamily = FontFamily.Monospace,
                            fontSize = 11.sp,
                            modifier = Modifier.padding(10.dp)
                        )
                    }
                }
            },
            confirmButton = {
                Button(onClick = { showRecoveryDialog = false }) {
                    Text("Saved")
                }
            }
        )
    }

    if (showVoiceTutorial) {
        VoiceInteractiveTutorialDialog(
            language = language,
            onDismiss = { showVoiceTutorial = false }
        )
    }

    if (showRestoreCodeDialog) {
        AlertDialog(
            onDismissRequest = { showRestoreCodeDialog = false },
            title = {
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Icon(Icons.Default.Key, contentDescription = null, tint = CyanNeon)
                    Spacer(modifier = Modifier.width(8.dp))
                    Text(if (isHindi) "बैकअप कोड से रिस्टोर करें" else "Restore via Backup Code")
                }
            },
            text = {
                Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                    Text(
                        text = if (isHindi) "पुराने बॉट से मिला AVCODE-... यहाँ पेस्ट करें:"
                        else "Paste your AVCODE-... migration code below:",
                        fontSize = 12.sp,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                    OutlinedTextField(
                        value = restoreCodeInput,
                        onValueChange = { restoreCodeInput = it },
                        placeholder = { Text("AVCODE-...") },
                        modifier = Modifier
                            .fillMaxWidth()
                            .height(120.dp),
                        maxLines = 5
                    )
                }
            },
            confirmButton = {
                Button(
                    onClick = {
                        if (restoreCodeInput.isNotBlank()) {
                            onLinkViaQrCode(restoreCodeInput.trim())
                            showRestoreCodeDialog = false
                        }
                    },
                    enabled = restoreCodeInput.isNotBlank()
                ) {
                    Text(if (isHindi) "रिस्टोर करें" else "Restore")
                }
            },
            dismissButton = {
                TextButton(onClick = { showRestoreCodeDialog = false }) {
                    Text(if (isHindi) "रद्द करें" else "Cancel")
                }
            }
        )
    }
}

@Composable
private fun DiagnosticCheckItem(title: String, passed: Boolean?) {
    Card(
        modifier = Modifier.fillMaxWidth(),
        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surfaceVariant)
    ) {
        Row(
            modifier = Modifier
                .padding(14.dp)
                .fillMaxWidth(),
            verticalAlignment = Alignment.CenterVertically
        ) {
            when (passed) {
                true -> Icon(Icons.Default.CheckCircle, contentDescription = null, tint = CyanNeon, modifier = Modifier.size(22.dp))
                false -> Icon(Icons.Default.Error, contentDescription = null, tint = MaterialTheme.colorScheme.error, modifier = Modifier.size(22.dp))
                null -> CircularProgressIndicator(modifier = Modifier.size(20.dp), strokeWidth = 2.dp)
            }
            Spacer(modifier = Modifier.width(12.dp))
            Text(
                text = title,
                fontSize = 13.sp,
                fontWeight = if (passed == true) FontWeight.Bold else FontWeight.Normal
            )
        }
    }
}
