package com.example.ui.components

import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.animation.core.spring
import androidx.compose.foundation.clickable
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.interaction.collectIsPressedAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.remember
import androidx.compose.ui.Modifier
import androidx.compose.ui.composed
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.platform.LocalContext
import com.example.core.sensory.HapticStyle
import com.example.core.sensory.SensoryFeedbackEngine
import com.example.core.sensory.SoundEffectType
import com.example.core.sensory.SpatialSoundEngine

/**
 * Modern Liquid Clickable Modifier
 * Scales smoothly to 0.96f with spring physics on touch-down and fires tactile haptics/audio.
 */
fun Modifier.liquidClickable(
    enabled: Boolean = true,
    hapticStyle: HapticStyle = HapticStyle.LIGHT_TICK,
    soundType: SoundEffectType = SoundEffectType.GLASS_TAP,
    onClick: () -> Unit
): Modifier = composed {
    val context = LocalContext.current
    val interactionSource = remember { MutableInteractionSource() }
    val isPressed by interactionSource.collectIsPressedAsState()

    val scale by animateFloatAsState(
        targetValue = if (isPressed) 0.96f else 1f,
        animationSpec = spring(dampingRatio = 0.68f, stiffness = 320f),
        label = "liquid_scale"
    )

    val alpha by animateFloatAsState(
        targetValue = if (isPressed) 0.92f else 1f,
        label = "liquid_alpha"
    )

    this
        .graphicsLayer {
            scaleX = scale
            scaleY = scale
            this.alpha = alpha
        }
        .clickable(
            interactionSource = interactionSource,
            indication = null,
            enabled = enabled
        ) {
            SensoryFeedbackEngine.trigger(context, hapticStyle, false)
            SpatialSoundEngine.play(context, soundType)
            onClick()
        }
}
