package com.example.features.onboarding

import androidx.compose.animation.*
import androidx.compose.foundation.background
import androidx.compose.foundation.border
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
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.features.studio.AppLanguage
import com.example.ui.components.AetherVaultLogo
import com.example.ui.theme.CyanNeon
import com.example.ui.theme.IndigoHyper

@Composable
fun TermsAgreementScreen(
    language: AppLanguage,
    onAcceptTerms: () -> Unit
) {
    val isHindi = language == AppLanguage.HI
    var hasCheckedAgreement by remember { mutableStateOf(false) }

    Scaffold(
        bottomBar = {
            Surface(
                modifier = Modifier.fillMaxWidth(),
                tonalElevation = 8.dp,
                color = MaterialTheme.colorScheme.surface
            ) {
                Column(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(16.dp)
                ) {
                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(bottom = 8.dp),
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Checkbox(
                            checked = hasCheckedAgreement,
                            onCheckedChange = { hasCheckedAgreement = it },
                            colors = CheckboxDefaults.colors(checkedColor = CyanNeon),
                            modifier = Modifier.testTag("terms_checkbox")
                        )
                        Spacer(modifier = Modifier.width(6.dp))
                        Text(
                            text = if (isHindi) "मैंने ओनर आयुष मंडल के नियम एवं शर्तें ध्यानपूर्वक पढ़ लिए हैं और सहमत हूँ।"
                            else "I have read, understood, and agree to the Terms of Service & Ownership by Ayush Mandal (आयुष मंडल).",
                            fontSize = 12.sp,
                            fontWeight = FontWeight.Medium,
                            color = MaterialTheme.colorScheme.onSurface
                        )
                    }

                    Button(
                        onClick = onAcceptTerms,
                        enabled = hasCheckedAgreement,
                        modifier = Modifier
                            .fillMaxWidth()
                            .height(52.dp)
                            .testTag("accept_terms_button"),
                        colors = ButtonDefaults.buttonColors(containerColor = CyanNeon, contentColor = Color.Black),
                        shape = RoundedCornerShape(12.dp)
                    ) {
                        Icon(Icons.Default.Verified, contentDescription = null, modifier = Modifier.size(20.dp))
                        Spacer(modifier = Modifier.width(8.dp))
                        Text(
                            text = if (isHindi) "नियम स्वीकार करें और आगे बढ़ें" else "Accept Terms & Continue",
                            fontWeight = FontWeight.Bold,
                            fontSize = 15.sp
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
                .verticalScroll(rememberScrollState())
                .padding(20.dp),
            horizontalAlignment = Alignment.CenterHorizontally
        ) {
            Spacer(modifier = Modifier.height(12.dp))
            AetherVaultLogo(size = 72.dp, isPulsing = true)
            Spacer(modifier = Modifier.height(16.dp))

            Text(
                text = "AetherVault",
                style = MaterialTheme.typography.headlineMedium,
                fontWeight = FontWeight.ExtraBold,
                color = MaterialTheme.colorScheme.onBackground
            )
            Text(
                text = "TeleVault Omni Edition",
                fontSize = 13.sp,
                fontFamily = FontFamily.Monospace,
                color = CyanNeon,
                fontWeight = FontWeight.Bold
            )

            Spacer(modifier = Modifier.height(20.dp))

            // Owner Card
            Card(
                modifier = Modifier.fillMaxWidth(),
                colors = CardDefaults.cardColors(containerColor = CyanNeon.copy(alpha = 0.12f)),
                shape = RoundedCornerShape(14.dp),
                border = androidx.compose.foundation.BorderStroke(1.2.dp, CyanNeon)
            ) {
                Row(
                    modifier = Modifier.padding(14.dp),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Box(
                        modifier = Modifier
                            .size(46.dp)
                            .clip(CircleShape)
                            .background(CyanNeon),
                        contentAlignment = Alignment.Center
                    ) {
                        Icon(Icons.Default.Person, contentDescription = null, tint = Color.Black, modifier = Modifier.size(26.dp))
                    }
                    Spacer(modifier = Modifier.width(14.dp))
                    Column {
                        Text(
                            text = if (isHindi) "ओनर व आर्किटेक्ट: आयुष मंडल" else "Owner & Lead Architect: Ayush Mandal (आयुष मंडल)",
                            fontWeight = FontWeight.Bold,
                            fontSize = 14.sp,
                            color = CyanNeon
                        )
                        Text(
                            text = "AetherVault Official Software License & Zero-Knowledge Architecture",
                            fontSize = 11.sp,
                            color = MaterialTheme.colorScheme.onSurfaceVariant
                        )
                    }
                }
            }

            Spacer(modifier = Modifier.height(18.dp))

            // Explanation Card: How the App Works
            Card(
                modifier = Modifier.fillMaxWidth(),
                colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surfaceVariant),
                shape = RoundedCornerShape(14.dp)
            ) {
                Column(modifier = Modifier.padding(16.dp)) {
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        Icon(Icons.Default.CloudSync, contentDescription = null, tint = CyanNeon, modifier = Modifier.size(22.dp))
                        Spacer(modifier = Modifier.width(10.dp))
                        Text(
                            text = if (isHindi) "यह ऐप कैसे काम करता है?" else "How Does AetherVault Work?",
                            fontWeight = FontWeight.Bold,
                            fontSize = 15.sp
                        )
                    }
                    Spacer(modifier = Modifier.height(10.dp))
                    Text(
                        text = if (isHindi) """
1. टेलीग्राम बॉट और प्राइवेट चैनल:
ऐप आपके प्राइवेट टेलीग्राम बॉट से कनेक्ट होता है। बॉट आपके टेलीग्राम चैनल के साथ जुड़कर एक पर्सनल असीमित क्लाउड सर्वर बना देता है।

2. डायरेक्ट और सुरक्षित फाइल अपलोड:
जब भी आप अपने फोन से कोई फाइल चुनते हैं, वह 19MB के चंक्स में विभाजित होकर मिलिट्री-ग्रेड AES-256-GCM एन्क्रिप्शन के साथ आपके टेलीग्राम चैनल में स्टोर हो जाती है।

3. कभी भी फाइल चेक और डाउनलोड करें:
आप ऐप के अंदर से कभी भी चेक कर सकते हैं कि फाइल चैनल में पहुंची या नहीं। जरूरत पड़ने पर 1-टैप में वापस डिक्रिप्ट करके डाउनलोड कर सकते हैं।
                        """.trimIndent()
                        else """
1. Private Telegram Bot & Channel Cloud:
The app connects to your private Telegram Bot courier and connects it directly to your private Telegram Channel to establish an autonomous, serverless cloud storage vault.

2. On-Device Encryption & Direct Transfer:
When you upload any file from your phone, it is buffered into 19MB chunks and encrypted on-device via AES-256-GCM before transmission. Plaintext data never touches Telegram.

3. Live Verification & Instant Decryption:
You can verify at any time whether each chunk is safely stored in your Telegram Channel. Files can be decrypted and restored to your device storage with one tap.
                        """.trimIndent(),
                        fontSize = 12.sp,
                        lineHeight = 18.sp,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                }
            }

            Spacer(modifier = Modifier.height(14.dp))

            // Root & Non-Root Device Support Card
            Card(
                modifier = Modifier.fillMaxWidth(),
                colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surfaceVariant),
                shape = RoundedCornerShape(14.dp)
            ) {
                Column(modifier = Modifier.padding(16.dp)) {
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        Icon(Icons.Default.PhoneAndroid, contentDescription = null, tint = IndigoHyper, modifier = Modifier.size(22.dp))
                        Spacer(modifier = Modifier.width(10.dp))
                        Text(
                            text = if (isHindi) "रूट और बिना रूट वाले सभी डिवाइस के लिए" else "Root & Non-Root Device Compatibility",
                            fontWeight = FontWeight.Bold,
                            fontSize = 14.sp
                        )
                    }
                    Spacer(modifier = Modifier.height(8.dp))
                    Text(
                        text = if (isHindi) """
• बिना रूट (Standard Non-Root): एंड्रॉइड के आधिकारिक Storage Access Framework (SAF) के जरिए सभी प्रकार की फाइलें, फोटो, वीडियो सुरक्षित रूप से चुनी जा सकती हैं।
• रूट वाले फोन (Rooted Devices): फुल फाइलसिस्टम और डायरेक्ट डिस्क स्टोरेज सपोर्ट पूरी तरह सक्रिय है।
• आवश्यक स्टोरेज और नेटवर्क परमिशन ऐप को आपकी चुनी हुई फाइलों को एन्क्रिप्ट और सिंक करने की अनुमति देती है।
                        """.trimIndent()
                        else """
• Non-Root Devices: Fully supported using Android's native Storage Access Framework (SAF) to pick photos, videos, APKs, documents, and code files.
• Rooted Devices: Fully supported with broad storage and direct filesystem integration.
• Device storage permissions are used solely to read user-selected files for encryption and to save decrypted downloads.
                        """.trimIndent(),
                        fontSize = 12.sp,
                        lineHeight = 18.sp,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                }
            }

            Spacer(modifier = Modifier.height(14.dp))

            // Terms Text
            Card(
                modifier = Modifier.fillMaxWidth(),
                colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surfaceVariant),
                shape = RoundedCornerShape(14.dp)
            ) {
                Column(modifier = Modifier.padding(16.dp)) {
                    Text(
                        text = if (isHindi) "नियम एवं शर्तें (Terms & Conditions)" else "Terms of Service & License",
                        fontWeight = FontWeight.Bold,
                        fontSize = 14.sp
                    )
                    Spacer(modifier = Modifier.height(8.dp))
                    Text(
                        text = """
1. OWNERSHIP:
AetherVault is created and owned by Ayush Mandal (आयुष मंडल). All rights reserved.

2. ZERO SUBSCRIPTION & ZERO TRACKING:
This software operates without recurring fees, advertisements, or telemetry collection. All cryptographic keys are derived locally on your device.

3. RESPONSIBILITY:
Users are responsible for maintaining the confidentiality of their Master Encryption Password and Telegram Channel administration credentials.
                        """.trimIndent(),
                        fontSize = 11.sp,
                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                        lineHeight = 16.sp
                    )
                }
            }

            Spacer(modifier = Modifier.height(24.dp))
        }
    }
}
