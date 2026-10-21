package com.example.ui.components

import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Add
import androidx.compose.material.icons.outlined.PhotoCamera
import androidx.compose.material3.Icon
import androidx.compose.material3.Surface
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.shadow
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.unit.dp
import com.example.ui.theme.*

@Composable
fun M3StackedFabs(
    onPrimaryFabClick: () -> Unit,
    onSecondaryFabClick: () -> Unit,
    modifier: Modifier = Modifier
) {
    Column(
        modifier = modifier.testTag("m3_stacked_fabs"),
        horizontalAlignment = Alignment.End,
        verticalArrangement = Arrangement.spacedBy(16.dp)
    ) {
        // Secondary Mini FAB (Camera / Media Upload)
        Surface(
            modifier = Modifier
                .size(48.dp)
                .shadow(elevation = 4.dp, shape = RoundedCornerShape(14.dp))
                .clip(RoundedCornerShape(14.dp))
                .clickable { onSecondaryFabClick() }
                .testTag("secondary_mini_fab"),
            color = M3SecondaryFab
        ) {
            Box(
                modifier = Modifier.fillMaxSize(),
                contentAlignment = Alignment.Center
            ) {
                Icon(
                    imageVector = Icons.Outlined.PhotoCamera,
                    contentDescription = "Quick Camera Upload",
                    tint = M3SecondaryFabIcon,
                    modifier = Modifier.size(22.dp)
                )
            }
        }

        // Primary Large FAB (+ File Upload)
        Surface(
            modifier = Modifier
                .size(64.dp)
                .shadow(elevation = 8.dp, shape = RoundedCornerShape(20.dp))
                .clip(RoundedCornerShape(20.dp))
                .clickable { onPrimaryFabClick() }
                .testTag("primary_large_fab"),
            color = M3ActivePill
        ) {
            Box(
                modifier = Modifier.fillMaxSize(),
                contentAlignment = Alignment.Center
            ) {
                Icon(
                    imageVector = Icons.Default.Add,
                    contentDescription = "Upload File to Tele Drive",
                    tint = M3ActivePillText,
                    modifier = Modifier.size(28.dp)
                )
            }
        }
    }
}
