package com.example.ui.components

import androidx.compose.foundation.Canvas
import androidx.compose.foundation.layout.*
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.Path
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.ui.theme.*

enum class EmptyStateType {
    HOME_SUGGESTED,
    STARRED,
    SHARED,
    FILES_MY_DRIVE
}

@Composable
fun M3EmptyStateIllustration(
    type: EmptyStateType,
    modifier: Modifier = Modifier
) {
    Column(
        modifier = modifier
            .fillMaxWidth()
            .padding(horizontal = 32.dp, vertical = 24.dp),
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.Center
    ) {
        Canvas(
            modifier = Modifier
                .size(200.dp)
                .padding(8.dp)
        ) {
            val w = size.width
            val h = size.height

            when (type) {
                EmptyStateType.HOME_SUGGESTED -> {
                    // Person sitting with dog and floating files (Green #C8E6C9, Blue #D3E3FD, Orange #FFE0B2)
                    drawCircle(color = Color(0xFFE8F5E9), center = Offset(w * 0.5f, h * 0.5f), radius = w * 0.42f)
                    
                    // Floating files
                    drawRoundRect(
                        color = Color.White,
                        topLeft = Offset(w * 0.32f, h * 0.15f),
                        size = Size(w * 0.22f, h * 0.25f),
                        cornerRadius = androidx.compose.ui.geometry.CornerRadius(12f)
                    )
                    drawRoundRect(
                        color = Color(0xFF43A047),
                        topLeft = Offset(w * 0.55f, h * 0.22f),
                        size = Size(w * 0.24f, h * 0.14f),
                        cornerRadius = androidx.compose.ui.geometry.CornerRadius(16f)
                    )
                    // Person chair/body
                    val personPath = Path().apply {
                        moveTo(w * 0.38f, h * 0.48f)
                        lineTo(w * 0.58f, h * 0.52f)
                        lineTo(w * 0.45f, h * 0.85f)
                        lineTo(w * 0.32f, h * 0.82f)
                        close()
                    }
                    drawPath(personPath, color = Color(0xFFD3E3FD))
                    drawPath(personPath, color = Color(0xFF041E49), style = Stroke(width = 4f))

                    // Sleeping dog
                    drawOval(
                        color = Color(0xFFFFB74D),
                        topLeft = Offset(w * 0.22f, h * 0.72f),
                        size = Size(w * 0.28f, h * 0.16f)
                    )
                }

                EmptyStateType.STARRED -> {
                    // Girl holding yellow star
                    drawCircle(color = Color(0xFFFFF8E1), center = Offset(w * 0.5f, h * 0.5f), radius = w * 0.4f)
                    
                    // Star shape
                    val starPath = Path().apply {
                        moveTo(w * 0.62f, h * 0.18f)
                        lineTo(w * 0.68f, h * 0.32f)
                        lineTo(w * 0.85f, h * 0.33f)
                        lineTo(w * 0.72f, h * 0.44f)
                        lineTo(w * 0.76f, h * 0.60f)
                        lineTo(w * 0.62f, h * 0.50f)
                        lineTo(w * 0.48f, h * 0.60f)
                        lineTo(w * 0.52f, h * 0.44f)
                        lineTo(w * 0.39f, h * 0.33f)
                        lineTo(w * 0.56f, h * 0.32f)
                        close()
                    }
                    drawPath(starPath, color = Color(0xFFFFD54F))
                    drawPath(starPath, color = Color(0xFF1F1F1F), style = Stroke(width = 4f))

                    // Character body
                    drawRoundRect(
                        color = Color(0xFFD3E3FD),
                        topLeft = Offset(w * 0.28f, h * 0.45f),
                        size = Size(w * 0.32f, h * 0.42f),
                        cornerRadius = androidx.compose.ui.geometry.CornerRadius(24f)
                    )
                }

                EmptyStateType.SHARED -> {
                    // Two people holding folder
                    drawCircle(color = Color(0xFFE1F5FE), center = Offset(w * 0.5f, h * 0.5f), radius = w * 0.42f)
                    
                    // Main folder
                    val folderPath = Path().apply {
                        moveTo(w * 0.3f, h * 0.42f)
                        lineTo(w * 0.45f, h * 0.42f)
                        lineTo(w * 0.52f, h * 0.48f)
                        lineTo(w * 0.72f, h * 0.48f)
                        lineTo(w * 0.72f, h * 0.72f)
                        lineTo(w * 0.3f, h * 0.72f)
                        close()
                    }
                    drawPath(folderPath, color = Color(0xFF42A5F5))
                    drawPath(folderPath, color = Color(0xFF0D47A1), style = Stroke(width = 4f))

                    // Person 1 (Green shirt)
                    drawRoundRect(
                        color = Color(0xFFA5D6A7),
                        topLeft = Offset(w * 0.52f, h * 0.52f),
                        size = Size(w * 0.26f, h * 0.35f),
                        cornerRadius = androidx.compose.ui.geometry.CornerRadius(18f)
                    )
                }

                EmptyStateType.FILES_MY_DRIVE -> {
                    // Orange cat hugging document
                    drawCircle(color = Color(0xFFFFF3E0), center = Offset(w * 0.5f, h * 0.5f), radius = w * 0.42f)

                    // Blue Document
                    drawRoundRect(
                        color = Color(0xFFBBDEFB),
                        topLeft = Offset(w * 0.48f, h * 0.45f),
                        size = Size(w * 0.3f, h * 0.38f),
                        cornerRadius = androidx.compose.ui.geometry.CornerRadius(12f)
                    )
                    drawRoundRect(
                        color = Color(0xFF1976D2),
                        topLeft = Offset(w * 0.48f, h * 0.45f),
                        size = Size(w * 0.3f, h * 0.38f),
                        cornerRadius = androidx.compose.ui.geometry.CornerRadius(12f),
                        style = Stroke(width = 4f)
                    )

                    // Orange Cat
                    val catPath = Path().apply {
                        moveTo(w * 0.28f, h * 0.35f)
                        cubicTo(w * 0.38f, h * 0.28f, w * 0.65f, h * 0.28f, w * 0.68f, h * 0.42f)
                        cubicTo(w * 0.72f, h * 0.55f, w * 0.55f, h * 0.78f, w * 0.28f, h * 0.78f)
                        close()
                    }
                    drawPath(catPath, color = Color(0xFFFF8A65))
                    drawPath(catPath, color = Color(0xFFBF360C), style = Stroke(width = 4f))
                }
            }
        }

        Spacer(modifier = Modifier.height(16.dp))

        val titleText = when (type) {
            EmptyStateType.HOME_SUGGESTED -> "A place for all of your files"
            EmptyStateType.STARRED -> "No starred files"
            EmptyStateType.SHARED -> "Nothing has been shared with you yet"
            EmptyStateType.FILES_MY_DRIVE -> "A place for all of your files"
        }

        val subtitleText = when (type) {
            EmptyStateType.HOME_SUGGESTED -> "Tap the + button to upload a file or create something"
            EmptyStateType.STARRED -> "Add stars to things that you want to easily find later"
            EmptyStateType.SHARED -> "See all the items shared with you in one place."
            EmptyStateType.FILES_MY_DRIVE -> "Tap the + button to upload a file or create something"
        }

        Text(
            text = titleText,
            fontSize = 19.sp,
            fontWeight = FontWeight.Medium,
            color = M3TextPrimary,
            textAlign = TextAlign.Center
        )

        Spacer(modifier = Modifier.height(8.dp))

        Text(
            text = subtitleText,
            fontSize = 13.sp,
            fontWeight = FontWeight.Normal,
            color = M3TextMuted,
            textAlign = TextAlign.Center
        )
    }
}
