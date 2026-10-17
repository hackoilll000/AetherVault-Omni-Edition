package com.example.features.onboarding

import android.content.Context
import androidx.compose.animation.*
import androidx.compose.animation.core.*
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.ui.window.Dialog
import androidx.compose.ui.window.DialogProperties
import com.example.core.sensory.HapticStyle
import com.example.core.sensory.SensoryFeedbackEngine
import com.example.core.sensory.VoiceNarratorEngine
import com.example.features.studio.AppLanguage
import com.example.ui.components.AetherVaultLogo
import com.example.ui.theme.CyanNeon
import com.example.ui.theme.IndigoHyper

data class TutorialStepItem(
    val stepNumber: Int,
    val icon: androidx.compose.ui.graphics.vector.ImageVector,
    val titleEn: String,
    val titleHi: String,
    val descEn: String,
    val descHi: String,
    val voiceScriptEn: String,
    val voiceScriptHi: String,
    val tipBadgeEn: String,
    val tipBadgeHi: String
)

@Composable
fun VoiceInteractiveTutorialDialog(
    language: AppLanguage,
    onDismiss: () -> Unit
) {
    val context = LocalContext.current
    val isHindi = language == AppLanguage.HI

    val narrator = remember { VoiceNarratorEngine(context) }
    DisposableEffect(narrator) {
        onDispose {
            narrator.shutdown()
        }
    }

    val steps = remember {
        listOf(
            TutorialStepItem(
                stepNumber = 1,
                icon = Icons.Default.CloudSync,
                titleEn = "Introduction & Unlimited Cloud",
                titleHi = "AetherVault व असीमित टेलीग्राम क्लाउड",
                descEn = "AetherVault turns Telegram into your personal, private, and unlimited cloud server. Files you select in the app are encrypted with AES-256 on your device and safely transmitted to your private Telegram channel.",
                descHi = "AetherVault आपके टेलीग्राम को एक पर्सनल, सुरक्षित और असीमित क्लाउड सर्वर बना देता है। आप ऐप से कोई भी फाइल चुनते हैं, वह फोन पर ही मिलिट्री-ग्रेड AES-256 एन्क्रिप्शन से लॉक होकर आपके प्राइवेट टेलीग्राम चैनल में सुरक्षित पहुंच जाती है।",
                voiceScriptEn = "Welcome to AetherVault. This app connects directly to Telegram to provide you with unlimited, free, zero-knowledge cloud storage. Plaintext files never leave your phone unencrypted.",
                voiceScriptHi = "AetherVault में आपका स्वागत है। यह ऐप टेलीग्राम से सीधे जुड़कर आपको पूरी तरह मुफ्त और असीमित क्लाउड स्टोरेज देता है। आपकी फाइलें फोन पर ही एन्क्रिप्ट होकर जाती हैं।",
                tipBadgeEn = "100% Free & Serverless",
                tipBadgeHi = "100% मुफ्त व प्राइवेट"
            ),
            TutorialStepItem(
                stepNumber = 2,
                icon = Icons.Default.SmartToy,
                titleEn = "Create Bot with @BotFather",
                titleHi = "@BotFather से अपना प्राइवेट बॉट बनाएं",
                descEn = "Open Telegram, search for '@BotFather', send '/newbot', and follow the prompts to choose a bot name and username. BotFather will provide an HTTP API Token (e.g. 7123456789:AAH...). Copy this token.",
                descHi = "टेलीग्राम खोलें और सर्च करें @BotFather। फिर /newbot भेजें और अपने बॉट का नाम व यूजरनेम चुनें। बॉटफादर आपको एक API टोकन देगा। उस टोकन को कॉपी करके ऐप में पेस्ट करें।",
                voiceScriptEn = "Step 1. Open Telegram and search for BotFather. Type slash newbot, give your bot a name, and copy the API token given to you.",
                voiceScriptHi = "पहला कदम। टेलीग्राम पर बॉटफादर सर्च करें। /newbot टाइप करके बॉट बनाएं और उसका API टोकन कॉपी करें।",
                tipBadgeEn = "Takes 30 seconds",
                tipBadgeHi = "सिर्फ 30 सेकंड का काम"
            ),
            TutorialStepItem(
                stepNumber = 3,
                icon = Icons.Default.Forum,
                titleEn = "Create Channel & Add Bot as Admin",
                titleHi = "प्राइवेट चैनल बनाएं और बॉट को Admin जोड़ें",
                descEn = "In Telegram, create a 'New Channel' and set it to 'Private'. Open Channel Settings -> Administrators -> Add Administrator -> Search your bot's username and grant 'Post Messages' rights. Copy your Channel ID.",
                descHi = "टेलीग्राम में नया प्राइवेट चैनल बनाएं। चैनल सेटिंग्स में जाकर 'Administrators' पर जाएं, अपने बॉट को सर्च करके Admin जोड़ें और 'Post Messages' की अनुमति दें। फिर अपना चैनल ID कॉपी करें।",
                voiceScriptEn = "Step 2. Create a private channel in Telegram. Add your bot as an administrator with posting rights. Enter your channel ID into the app.",
                voiceScriptHi = "दूसरा कदम। टेलीग्राम में एक प्राइवेट चैनल बनाएं। अपने बॉट को एडमिन जोड़कर मैसेज पोस्ट करने की परमिशन दें और चैनल ID ऐप में दर्ज करें।",
                tipBadgeEn = "Private & Invite Only",
                tipBadgeHi = "प्राइवेट चैनल स्टोरेज"
            ),
            TutorialStepItem(
                stepNumber = 4,
                icon = Icons.Default.Security,
                titleEn = "Direct File Upload & Live Verification",
                titleHi = "डायरेक्ट फाइल अपलोड व लाइव वेरिफिकेशन",
                descEn = "Select any file, photo, video, or document. The app splits it into 19MB streaming buffers, encrypts each chunk locally, and uploads them to your channel. You can verify and download anytime with 1-tap.",
                descHi = "फोन से कोई भी फोटो, वीडियो या डॉक्यूमेंट चुनें। ऐप उसे 19MB के हिस्सों में बांटकर फोन पर ही लॉक करता है और चैनल में भेजता है। आप कभी भी 1 टैप से वेरीफाई और डाउनलोड कर सकते हैं।",
                voiceScriptEn = "Step 3. When you pick any file, AetherVault buffers, encrypts, and uploads it to Telegram. You can verify chunk integrity or download anytime.",
                voiceScriptHi = "तीसरा कदम। कोई भी फाइल चुनें, ऐप उसे एन्क्रिप्ट करके चैनल में स्टोर कर देगा। आप कभी भी चेक और डाउनलोड कर सकते हैं।",
                tipBadgeEn = "AES-256-GCM Military Grade",
                tipBadgeHi = "मिलिट्री-ग्रेड सुरक्षा"
            ),
            TutorialStepItem(
                stepNumber = 5,
                icon = Icons.Default.RestorePage,
                titleEn = "Bot Backup & Recovery Protection",
                titleHi = "बॉट बैकअप व नए बॉट में रिकवरी",
                descEn = "If you ever delete your bot in @BotFather, don't worry! Your files remain safe in your Telegram Channel. Use 'Settings -> Backup & Bot Migration' to export your catalog, or simply bind a new bot token to instantly restore full access.",
                descHi = "अगर गलती से आपका बॉट डिलीट हो जाए, तो चिंता न करें! आपकी सारी फाइलें टेलीग्राम चैनल में सुरक्षित रहेंगी। सेटिंग्स में जाकर बैकअप ले सकते हैं और नया बॉट जोड़ते ही सब कुछ तुरंत वापस चालू हो जाएगा।",
                voiceScriptEn = "Step 4. If your bot is accidentally deleted, your files remain intact in your channel. Simply re-bind a new bot token or import your vault backup.",
                voiceScriptHi = "चौथा कदम। अगर बॉट डिलीट भी हो जाए तो चैनल में फाइलें सुरक्षित रहती हैं। आप नया बॉट जोड़कर या बैकअप रिस्टोर करके तुरंत काम जारी रख सकते हैं।",
                tipBadgeEn = "Full Data Ownership",
                tipBadgeHi = "डेटा का पूरा कंट्रोल"
            )
        )
    }

    var currentStepIndex by remember { mutableIntStateOf(0) }
    val currentStep = steps[currentStepIndex]

    // Auto-trigger voice narration on step change if unmuted
    LaunchedEffect(currentStepIndex, narrator.isInitialized) {
        if (narrator.isInitialized && !narrator.isMuted) {
            val script = if (isHindi) currentStep.voiceScriptHi else currentStep.voiceScriptEn
            narrator.speak(script, isHindi)
        }
    }

    Dialog(
        onDismissRequest = {
            narrator.stop()
            onDismiss()
        },
        properties = DialogProperties(usePlatformDefaultWidth = false)
    ) {
        Surface(
            modifier = Modifier
                .fillMaxWidth(0.94f)
                .fillMaxHeight(0.88f)
                .clip(RoundedCornerShape(24.dp))
                .border(
                    width = 1.2.dp,
                    brush = Brush.linearGradient(listOf(CyanNeon.copy(alpha = 0.8f), IndigoHyper.copy(alpha = 0.5f))),
                    shape = RoundedCornerShape(24.dp)
                ),
            color = MaterialTheme.colorScheme.surface.copy(alpha = 0.98f),
            tonalElevation = 16.dp
        ) {
            Column(
                modifier = Modifier
                    .fillMaxSize()
                    .padding(20.dp)
            ) {
                // Top Header Row
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        AetherVaultLogo(size = 32.dp, isPulsing = narrator.isSpeaking)
                        Spacer(modifier = Modifier.width(10.dp))
                        Column {
                            Text(
                                text = if (isHindi) "AetherVault ऑडियो ट्यूटोरियल" else "Interactive Voice Tutorial",
                                fontWeight = FontWeight.Bold,
                                fontSize = 16.sp
                            )
                            Text(
                                text = "By TRB FireOps (@trbfireops)",
                                fontSize = 11.sp,
                                color = CyanNeon,
                                fontWeight = FontWeight.Medium
                            )
                        }
                    }

                    // Voice Mute / Speak Toggle
                    IconButton(
                        onClick = {
                            SensoryFeedbackEngine.trigger(context, HapticStyle.LIGHT_TICK)
                            narrator.isMuted = !narrator.isMuted
                            if (narrator.isMuted) {
                                narrator.stop()
                            } else {
                                val script = if (isHindi) currentStep.voiceScriptHi else currentStep.voiceScriptEn
                                narrator.speak(script, isHindi)
                            }
                        },
                        modifier = Modifier
                            .size(40.dp)
                            .clip(CircleShape)
                            .background(if (narrator.isSpeaking) CyanNeon.copy(alpha = 0.2f) else MaterialTheme.colorScheme.surfaceVariant)
                    ) {
                        Icon(
                            imageVector = if (narrator.isMuted) Icons.Default.VolumeOff
                            else if (narrator.isSpeaking) Icons.Default.VolumeUp
                            else Icons.Default.VolumeDown,
                            contentDescription = "Voice Guide Toggle",
                            tint = if (narrator.isSpeaking) CyanNeon else MaterialTheme.colorScheme.onSurface
                        )
                    }
                }

                Spacer(modifier = Modifier.height(14.dp))

                // Step Progression Dots / Tabs
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.spacedBy(6.dp),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    for (i in steps.indices) {
                        Box(
                            modifier = Modifier
                                .weight(1f)
                                .height(4.dp)
                                .clip(RoundedCornerShape(2.dp))
                                .background(
                                    if (i == currentStepIndex) CyanNeon
                                    else if (i < currentStepIndex) IndigoHyper
                                    else MaterialTheme.colorScheme.onSurface.copy(alpha = 0.15f)
                                )
                        )
                    }
                }

                Spacer(modifier = Modifier.height(16.dp))

                // Scrollable Content
                Column(
                    modifier = Modifier
                        .weight(1f)
                        .verticalScroll(rememberScrollState()),
                    verticalArrangement = Arrangement.spacedBy(14.dp)
                ) {
                    // Step Badge & Icon Card
                    Card(
                        modifier = Modifier.fillMaxWidth(),
                        colors = CardDefaults.cardColors(
                            containerColor = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.7f)
                        ),
                        shape = RoundedCornerShape(16.dp),
                        border = androidx.compose.foundation.BorderStroke(1.dp, CyanNeon.copy(alpha = 0.3f))
                    ) {
                        Column(modifier = Modifier.padding(16.dp)) {
                            Row(
                                modifier = Modifier.fillMaxWidth(),
                                horizontalArrangement = Arrangement.SpaceBetween,
                                verticalAlignment = Alignment.CenterVertically
                            ) {
                                Surface(
                                    color = CyanNeon.copy(alpha = 0.2f),
                                    shape = RoundedCornerShape(8.dp)
                                ) {
                                    Text(
                                        text = if (isHindi) currentStep.tipBadgeHi else currentStep.tipBadgeEn,
                                        color = CyanNeon,
                                        fontSize = 11.sp,
                                        fontWeight = FontWeight.Bold,
                                        modifier = Modifier.padding(horizontal = 8.dp, vertical = 4.dp)
                                    )
                                }

                                Text(
                                    text = "${currentStep.stepNumber} / ${steps.size}",
                                    fontFamily = FontFamily.Monospace,
                                    fontSize = 12.sp,
                                    color = MaterialTheme.colorScheme.onSurfaceVariant
                                )
                            }

                            Spacer(modifier = Modifier.height(14.dp))

                            Row(verticalAlignment = Alignment.CenterVertically) {
                                Box(
                                    modifier = Modifier
                                        .size(52.dp)
                                        .clip(CircleShape)
                                        .background(Brush.linearGradient(listOf(CyanNeon, IndigoHyper))),
                                    contentAlignment = Alignment.Center
                                ) {
                                    Icon(
                                        imageVector = currentStep.icon,
                                        contentDescription = null,
                                        tint = Color.Black,
                                        modifier = Modifier.size(28.dp)
                                    )
                                }

                                Spacer(modifier = Modifier.width(14.dp))

                                Column {
                                    Text(
                                        text = if (isHindi) currentStep.titleHi else currentStep.titleEn,
                                        fontSize = 17.sp,
                                        fontWeight = FontWeight.Bold,
                                        color = MaterialTheme.colorScheme.onSurface
                                    )
                                    Text(
                                        text = if (isHindi) "स्टेप ${currentStep.stepNumber}" else "Step ${currentStep.stepNumber}",
                                        fontSize = 12.sp,
                                        color = CyanNeon
                                    )
                                }
                            }

                            Spacer(modifier = Modifier.height(14.dp))

                            Text(
                                text = if (isHindi) currentStep.descHi else currentStep.descEn,
                                fontSize = 13.sp,
                                lineHeight = 20.sp,
                                color = MaterialTheme.colorScheme.onSurfaceVariant
                            )
                        }
                    }

                    // Voice Audio Guidance Card with Wave Indicator
                    Card(
                        modifier = Modifier.fillMaxWidth(),
                        colors = CardDefaults.cardColors(
                            containerColor = if (narrator.isSpeaking) CyanNeon.copy(alpha = 0.12f)
                            else MaterialTheme.colorScheme.surfaceVariant
                        ),
                        shape = RoundedCornerShape(14.dp),
                        border = androidx.compose.foundation.BorderStroke(
                            1.dp,
                            if (narrator.isSpeaking) CyanNeon else Color.Transparent
                        )
                    ) {
                        Row(
                            modifier = Modifier
                                .fillMaxWidth()
                                .padding(12.dp),
                            verticalAlignment = Alignment.CenterVertically,
                            horizontalArrangement = Arrangement.SpaceBetween
                        ) {
                            Row(verticalAlignment = Alignment.CenterVertically, modifier = Modifier.weight(1f)) {
                                Icon(
                                    imageVector = if (narrator.isSpeaking) Icons.Default.GraphicEq else Icons.Default.Headphones,
                                    contentDescription = null,
                                    tint = CyanNeon,
                                    modifier = Modifier.size(22.dp)
                                )
                                Spacer(modifier = Modifier.width(10.dp))
                                Column {
                                    Text(
                                        text = if (narrator.isSpeaking) {
                                            if (isHindi) "वॉइस गाइडेंस बोल रहा है..." else "Voice Guide Speaking..."
                                        } else {
                                            if (isHindi) "वॉइस गाइडेंस सुनें" else "Listen to Audio Guide"
                                        },
                                        fontSize = 12.sp,
                                        fontWeight = FontWeight.Bold,
                                        color = if (narrator.isSpeaking) CyanNeon else MaterialTheme.colorScheme.onSurface
                                    )
                                    Text(
                                        text = if (isHindi) "हिंदी व अंग्रेजी में वॉइस ऑडियो" else "Spoken voice in Hindi & English",
                                        fontSize = 10.sp,
                                        color = MaterialTheme.colorScheme.onSurfaceVariant
                                    )
                                }
                            }

                            Button(
                                onClick = {
                                    SensoryFeedbackEngine.trigger(context, HapticStyle.LIGHT_TICK)
                                    val script = if (isHindi) currentStep.voiceScriptHi else currentStep.voiceScriptEn
                                    narrator.speak(script, isHindi)
                                },
                                shape = RoundedCornerShape(8.dp),
                                colors = ButtonDefaults.buttonColors(
                                    containerColor = CyanNeon,
                                    contentColor = Color.Black
                                ),
                                contentPadding = PaddingValues(horizontal = 12.dp, vertical = 6.dp)
                            ) {
                                Icon(Icons.Default.PlayArrow, contentDescription = null, modifier = Modifier.size(16.dp))
                                Spacer(modifier = Modifier.width(4.dp))
                                Text(if (isHindi) "दोबारा सुनें" else "Replay", fontSize = 11.sp, fontWeight = FontWeight.Bold)
                            }
                        }
                    }
                }

                Spacer(modifier = Modifier.height(14.dp))

                // Navigation Buttons (Back & Next / Finish)
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.spacedBy(10.dp),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    if (currentStepIndex > 0) {
                        OutlinedButton(
                            onClick = {
                                SensoryFeedbackEngine.trigger(context, HapticStyle.LIGHT_TICK)
                                currentStepIndex--
                            },
                            modifier = Modifier.weight(1f),
                            shape = RoundedCornerShape(12.dp)
                        ) {
                            Icon(Icons.Default.ArrowBack, contentDescription = null, modifier = Modifier.size(16.dp))
                            Spacer(modifier = Modifier.width(6.dp))
                            Text(if (isHindi) "पिछला" else "Back")
                        }
                    }

                    Button(
                        onClick = {
                            SensoryFeedbackEngine.trigger(context, HapticStyle.SUCCESS_DOUBLE_PULSE)
                            if (currentStepIndex < steps.size - 1) {
                                currentStepIndex++
                            } else {
                                narrator.stop()
                                onDismiss()
                            }
                        },
                        modifier = Modifier.weight(1f),
                        colors = ButtonDefaults.buttonColors(containerColor = CyanNeon, contentColor = Color.Black),
                        shape = RoundedCornerShape(12.dp)
                    ) {
                        Text(
                            text = if (currentStepIndex < steps.size - 1) {
                                if (isHindi) "अगला कदम" else "Next Step"
                            } else {
                                if (isHindi) "ट्यूटोरियल पूरा हुआ" else "Finish Tutorial"
                            },
                            fontWeight = FontWeight.Bold
                        )
                        Spacer(modifier = Modifier.width(6.dp))
                        Icon(
                            imageVector = if (currentStepIndex < steps.size - 1) Icons.Default.ArrowForward else Icons.Default.Check,
                            contentDescription = null,
                            modifier = Modifier.size(16.dp)
                        )
                    }
                }
            }
        }
    }
}
