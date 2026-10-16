package com.example.ui.components

import androidx.compose.animation.core.*
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.layout.size
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.*
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.graphics.drawscope.rotate
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import com.example.ui.theme.CyanNeon
import com.example.ui.theme.IndigoHyper
import com.example.ui.theme.VioletNeon
import kotlin.math.cos
import kotlin.math.sin

/**
 * Animated Quantum Paper-Plane Vault Logo
 * Interlocking hexagonal shield morphing into an ascending origami paper plane
 * with pulsing cyber nexus core.
 */
@Composable
fun AetherVaultLogo(
    modifier: Modifier = Modifier,
    size: Dp = 64.dp,
    isPulsing: Boolean = true
) {
    val infiniteTransition = rememberInfiniteTransition(label = "vault_pulse")

    val pulseScale by infiniteTransition.animateFloat(
        initialValue = 0.95f,
        targetValue = 1.05f,
        animationSpec = infiniteRepeatable(
            animation = tween(1200, easing = FastOutSlowInEasing),
            repeatMode = RepeatMode.Reverse
        ),
        label = "pulse_scale"
    )

    val coreRotation by infiniteTransition.animateFloat(
        initialValue = 0f,
        targetValue = 360f,
        animationSpec = infiniteRepeatable(
            animation = tween(12000, easing = LinearEasing),
            repeatMode = RepeatMode.Restart
        ),
        label = "core_rotation"
    )

    Canvas(modifier = modifier.size(size)) {
        val w = this.size.width
        val h = this.size.height
        val center = Offset(w / 2f, h / 2f)
        val radius = (w / 2f) * 0.82f * (if (isPulsing) pulseScale else 1f)

        // 1. Draw Hexagonal Cryptographic Shield
        val hexPath = Path()
        for (i in 0 until 6) {
            val angleRad = Math.toRadians((60.0 * i) - 30.0)
            val x = center.x + (radius * cos(angleRad)).toFloat()
            val y = center.y + (radius * sin(angleRad)).toFloat()
            if (i == 0) hexPath.moveTo(x, y) else hexPath.lineTo(x, y)
        }
        hexPath.close()

        val shieldGradient = Brush.linearGradient(
            colors = listOf(CyanNeon, IndigoHyper, VioletNeon),
            start = Offset(0f, 0f),
            end = Offset(w, h)
        )

        drawPath(
            path = hexPath,
            brush = shieldGradient,
            style = Stroke(width = w * 0.045f, cap = StrokeCap.Round, join = StrokeJoin.Round)
        )

        // 2. Draw Origami Paper Plane Symbol (Telegram speed + Vault security)
        val planePath = Path().apply {
            moveTo(center.x - radius * 0.45f, center.y + radius * 0.25f)
            lineTo(center.x + radius * 0.65f, center.y - radius * 0.45f)
            lineTo(center.x + radius * 0.05f, center.y + radius * 0.6f)
            lineTo(center.x - radius * 0.1f, center.y + radius * 0.2f)
            close()
        }
        drawPath(path = planePath, color = CyanNeon.copy(alpha = 0.9f))

        val foldPath = Path().apply {
            moveTo(center.x - radius * 0.1f, center.y + radius * 0.2f)
            lineTo(center.x + radius * 0.65f, center.y - radius * 0.45f)
            lineTo(center.x + radius * 0.2f, center.y + radius * 0.15f)
            close()
        }
        drawPath(path = foldPath, color = IndigoHyper)

        // 3. Central Glowing Nexus Core (Pulsing Keyhole)
        rotate(degrees = if (isPulsing) coreRotation else 0f, pivot = center) {
            drawCircle(
                brush = Brush.radialGradient(
                    colors = listOf(Color.White, CyanNeon, Color.Transparent),
                    center = center,
                    radius = radius * 0.22f
                ),
                radius = radius * 0.18f,
                center = center
            )
        }
    }
}
