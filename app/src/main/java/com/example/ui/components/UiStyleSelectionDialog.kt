package com.example.ui.components

import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.BrightnessAuto
import androidx.compose.material.icons.filled.CheckCircle
import androidx.compose.material.icons.filled.DarkMode
import androidx.compose.material.icons.filled.LightMode
import androidx.compose.material.icons.outlined.Palette
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.features.studio.AppLanguage
import com.example.features.studio.ThemeMode

@Composable
fun UiStyleSelectionDialog(
    initialMode: ThemeMode,
    language: AppLanguage,
    onConfirm: (ThemeMode) -> Unit,
    onDismiss: () -> Unit
) {
    val isHindi = language == AppLanguage.HI
    var selectedMode by remember { mutableStateOf(initialMode) }

    AlertDialog(
        onDismissRequest = onDismiss,
        title = {
            Row(verticalAlignment = Alignment.CenterVertically) {
                Box(
                    modifier = Modifier
                        .size(36.dp)
                        .clip(CircleShape)
                        .background(MaterialTheme.colorScheme.primaryContainer),
                    contentAlignment = Alignment.Center
                ) {
                    Icon(
                        imageVector = Icons.Outlined.Palette,
                        contentDescription = null,
                        tint = MaterialTheme.colorScheme.onPrimaryContainer,
                        modifier = Modifier.size(20.dp)
                    )
                }
                Spacer(modifier = Modifier.width(12.dp))
                Column {
                    Text(
                        text = if (isHindi) "UI स्टाइल और थीम चुनें" else "Choose UI Style & Theme",
                        style = MaterialTheme.typography.titleMedium,
                        fontWeight = FontWeight.Bold,
                        color = MaterialTheme.colorScheme.onSurface
                    )
                    Text(
                        text = if (isHindi) "बाद में सेटिंग्स से कभी भी बदल सकते हैं" else "You can always change this in Settings",
                        fontSize = 11.sp,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                }
            }
        },
        text = {
            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(vertical = 4.dp),
                verticalArrangement = Arrangement.spacedBy(10.dp)
            ) {
                UiStyleOptionCard(
                    title = if (isHindi) "सिस्टम ऑटो (ऑटो-ऑप्टिमाइज्ड)" else "System Auto (Recommended)",
                    subtitle = if (isHindi) "फोन के डार्क/लाइट मोड के अनुसार अपने आप बदलेगा" else "Automatically matches device dark or light theme",
                    icon = Icons.Default.BrightnessAuto,
                    isSelected = selectedMode == ThemeMode.SYSTEM_AUTO,
                    onClick = { selectedMode = ThemeMode.SYSTEM_AUTO },
                    testTag = "theme_option_auto"
                )

                UiStyleOptionCard(
                    title = if (isHindi) "मटीरियल 3 लाइट मोड" else "Material 3 Light",
                    subtitle = if (isHindi) "साफ़, क्रिस्प गूगल ड्राइव व फ़ोटोज़ स्टाइल" else "Crisp white & alabaster daylight aesthetic",
                    icon = Icons.Default.LightMode,
                    isSelected = selectedMode == ThemeMode.LIGHT,
                    onClick = { selectedMode = ThemeMode.LIGHT },
                    testTag = "theme_option_light"
                )

                UiStyleOptionCard(
                    title = if (isHindi) "मटीरियल 3 डार्क मोड" else "Material 3 Dark (OLED)",
                    subtitle = if (isHindi) "आँखों के लिए आरामदायक, बैटरी बचत" else "Deep dark palette for night use & battery saving",
                    icon = Icons.Default.DarkMode,
                    isSelected = selectedMode == ThemeMode.DARK,
                    onClick = { selectedMode = ThemeMode.DARK },
                    testTag = "theme_option_dark"
                )
            }
        },
        confirmButton = {
            Button(
                onClick = { onConfirm(selectedMode) },
                modifier = Modifier.testTag("apply_ui_style_button")
            ) {
                Text(if (isHindi) "लागू करें" else "Apply Style")
            }
        },
        dismissButton = {
            TextButton(onClick = onDismiss) {
                Text(if (isHindi) "बाद में" else "Dismiss")
            }
        }
    )
}

@Composable
private fun UiStyleOptionCard(
    title: String,
    subtitle: String,
    icon: ImageVector,
    isSelected: Boolean,
    onClick: () -> Unit,
    testTag: String
) {
    val borderColor = if (isSelected) MaterialTheme.colorScheme.primary else MaterialTheme.colorScheme.outlineVariant
    val containerColor = if (isSelected) MaterialTheme.colorScheme.primaryContainer.copy(alpha = 0.25f)
    else MaterialTheme.colorScheme.surfaceContainer

    Surface(
        shape = RoundedCornerShape(16.dp),
        color = containerColor,
        modifier = Modifier
            .fillMaxWidth()
            .clip(RoundedCornerShape(16.dp))
            .border(width = if (isSelected) 2.dp else 1.dp, color = borderColor, shape = RoundedCornerShape(16.dp))
            .clickable { onClick() }
            .testTag(testTag)
    ) {
        Row(
            modifier = Modifier.padding(14.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            Box(
                modifier = Modifier
                    .size(40.dp)
                    .clip(CircleShape)
                    .background(if (isSelected) MaterialTheme.colorScheme.primary else MaterialTheme.colorScheme.surfaceVariant),
                contentAlignment = Alignment.Center
            ) {
                Icon(
                    imageVector = icon,
                    contentDescription = null,
                    tint = if (isSelected) MaterialTheme.colorScheme.onPrimary else MaterialTheme.colorScheme.onSurfaceVariant,
                    modifier = Modifier.size(20.dp)
                )
            }

            Spacer(modifier = Modifier.width(12.dp))

            Column(modifier = Modifier.weight(1f)) {
                Text(
                    text = title,
                    fontWeight = FontWeight.SemiBold,
                    fontSize = 14.sp,
                    color = MaterialTheme.colorScheme.onSurface
                )
                Text(
                    text = subtitle,
                    fontSize = 11.sp,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                    lineHeight = 15.sp
                )
            }

            if (isSelected) {
                Spacer(modifier = Modifier.width(8.dp))
                Icon(
                    imageVector = Icons.Default.CheckCircle,
                    contentDescription = "Selected",
                    tint = MaterialTheme.colorScheme.primary,
                    modifier = Modifier.size(20.dp)
                )
            }
        }
    }
}
