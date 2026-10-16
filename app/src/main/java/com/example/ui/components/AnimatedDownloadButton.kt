package com.example.ui.components

import androidx.compose.animation.core.*
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.interaction.collectIsPressedAsState
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Check
import androidx.compose.material.icons.filled.Download
import androidx.compose.material.icons.filled.FileDownload
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.scale
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.hapticfeedback.HapticFeedbackType
import androidx.compose.ui.platform.LocalHapticFeedback
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.ui.theme.CyanNeon
import com.example.ui.theme.IndigoHyper
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch

/**
 * High-performance, animated download button with spring scaling,
 * bouncing arrow kinematics, haptic feedback, and visual state confirmation.
 */
@Composable
fun AnimatedCloudDownloadButton(
    onClick: () -> Unit,
    modifier: Modifier = Modifier,
    label: String? = null,
    isCompact: Boolean = false,
    testTag: String = "animated_download_button"
) {
    val haptic = LocalHapticFeedback.current
    val coroutineScope = rememberCoroutineScope()
    val interactionSource = remember { MutableInteractionSource() }
    val isPressed by interactionSource.collectIsPressedAsState()

    var isTriggered by remember { mutableStateOf(false) }

    // Spring scale effect when pressed or triggered
    val targetScale = when {
        isPressed -> 0.88f
        isTriggered -> 1.08f
        else -> 1.0f
    }
    val animatedScale by animateFloatAsState(
        targetValue = targetScale,
        animationSpec = spring(
            dampingRatio = Spring.DampingRatioMediumBouncy,
            stiffness = Spring.StiffnessLow
        ),
        label = "download_button_scale"
    )

    // Bouncing arrow translation
    val infiniteTransition = rememberInfiniteTransition(label = "download_arrow_anim")
    val arrowOffsetY by infiniteTransition.animateFloat(
        initialValue = -1.5f,
        targetValue = 2.5f,
        animationSpec = infiniteRepeatable(
            animation = tween(durationMillis = 800, easing = FastOutSlowInEasing),
            repeatMode = RepeatMode.Reverse
        ),
        label = "arrow_bounce"
    )

    val buttonShape = RoundedCornerShape(10.dp)

    Surface(
        modifier = modifier
            .scale(animatedScale)
            .clip(buttonShape)
            .clickable(
                interactionSource = interactionSource,
                indication = ripple(color = CyanNeon)
            ) {
                haptic.performHapticFeedback(HapticFeedbackType.LongPress)
                isTriggered = true
                coroutineScope.launch {
                    delay(500)
                    isTriggered = false
                }
                onClick()
            }
            .border(
                width = 1.dp,
                brush = Brush.horizontalGradient(
                    listOf(
                        CyanNeon.copy(alpha = if (isTriggered) 0.9f else 0.5f),
                        IndigoHyper.copy(alpha = if (isTriggered) 0.9f else 0.4f)
                    )
                ),
                shape = buttonShape
            )
            .testTag(testTag),
        shape = buttonShape,
        color = if (isTriggered) MaterialTheme.colorScheme.primaryContainer else MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.85f),
        tonalElevation = if (isTriggered) 6.dp else 2.dp
    ) {
        Row(
            modifier = Modifier
                .padding(
                    horizontal = if (label != null) 10.dp else 6.dp,
                    vertical = if (label != null) 6.dp else 6.dp
                ),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.Center
        ) {
            // Icon with bounce animation
            Box(
                modifier = Modifier
                    .graphicsLayer {
                        translationY = if (!isTriggered) arrowOffsetY else 0f
                    },
                contentAlignment = Alignment.Center
            ) {
                Icon(
                    imageVector = if (isTriggered) Icons.Default.Check else Icons.Default.Download,
                    contentDescription = "Download to Device",
                    tint = if (isTriggered) Color(0xFF10B981) else CyanNeon,
                    modifier = Modifier.size(if (isCompact) 15.dp else 18.dp)
                )
            }

            if (label != null) {
                Spacer(modifier = Modifier.width(6.dp))
                Text(
                    text = if (isTriggered) "..." else label,
                    fontSize = if (isCompact) 11.sp else 12.sp,
                    fontWeight = FontWeight.Bold,
                    color = if (isTriggered) Color(0xFF10B981) else MaterialTheme.colorScheme.onSurface
                )
            }
        }
    }
}

/**
 * Prominent Floating / Banner Download CTA for screens and viewers
 */
@Composable
fun ProminentDownloadPill(
    onClick: () -> Unit,
    title: String,
    subtitle: String? = null,
    modifier: Modifier = Modifier
) {
    val haptic = LocalHapticFeedback.current
    var isClicked by remember { mutableStateOf(false) }
    val coroutineScope = rememberCoroutineScope()

    val scale by animateFloatAsState(
        targetValue = if (isClicked) 0.92f else 1f,
        animationSpec = spring(dampingRatio = Spring.DampingRatioMediumBouncy),
        label = "pill_scale"
    )

    Surface(
        modifier = modifier
            .scale(scale)
            .clip(RoundedCornerShape(14.dp))
            .clickable {
                haptic.performHapticFeedback(HapticFeedbackType.LongPress)
                isClicked = true
                coroutineScope.launch {
                    delay(300)
                    isClicked = false
                }
                onClick()
            }
            .border(
                width = 1.2.dp,
                brush = Brush.horizontalGradient(listOf(CyanNeon, IndigoHyper)),
                shape = RoundedCornerShape(14.dp)
            ),
        shape = RoundedCornerShape(14.dp),
        color = MaterialTheme.colorScheme.surface
    ) {
        Row(
            modifier = Modifier.padding(horizontal = 14.dp, vertical = 10.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            Box(
                modifier = Modifier
                    .size(36.dp)
                    .clip(CircleShape)
                    .background(CyanNeon.copy(alpha = 0.2f)),
                contentAlignment = Alignment.Center
            ) {
                Icon(
                    imageVector = Icons.Default.FileDownload,
                    contentDescription = null,
                    tint = CyanNeon,
                    modifier = Modifier.size(20.dp)
                )
            }
            Spacer(modifier = Modifier.width(10.dp))
            Column {
                Text(
                    text = title,
                    fontWeight = FontWeight.Bold,
                    fontSize = 13.sp,
                    color = MaterialTheme.colorScheme.onSurface
                )
                if (subtitle != null) {
                    Text(
                        text = subtitle,
                        fontSize = 11.sp,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                }
            }
        }
    }
}
