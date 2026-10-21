package com.example.ui.components

import androidx.compose.animation.animateColorAsState
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Folder
import androidx.compose.material.icons.filled.Home
import androidx.compose.material.icons.filled.People
import androidx.compose.material.icons.filled.Star
import androidx.compose.material.icons.outlined.Folder
import androidx.compose.material.icons.outlined.Home
import androidx.compose.material.icons.outlined.People
import androidx.compose.material.icons.outlined.StarOutline
import androidx.compose.material3.Icon
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.ui.theme.*

data class M3NavItemSpec(
    val title: String,
    val filledIcon: ImageVector,
    val outlinedIcon: ImageVector,
    val tag: String
)

@Composable
fun M3BottomNavigationBar(
    selectedTabOrdinal: Int,
    onSelectTab: (Int) -> Unit,
    modifier: Modifier = Modifier
) {
    val items = listOf(
        M3NavItemSpec("Home", Icons.Filled.Home, Icons.Outlined.Home, "nav_home"),
        M3NavItemSpec("Starred", Icons.Filled.Star, Icons.Outlined.StarOutline, "nav_starred"),
        M3NavItemSpec("Shared", Icons.Filled.People, Icons.Outlined.People, "nav_shared"),
        M3NavItemSpec("Files", Icons.Filled.Folder, Icons.Outlined.Folder, "nav_files")
    )

    Surface(
        modifier = modifier
            .fillMaxWidth()
            .height(72.dp)
            .testTag("m3_bottom_nav_bar"),
        color = M3BottomBarBg,
        tonalElevation = 3.dp
    ) {
        Row(
            modifier = Modifier.fillMaxSize(),
            horizontalArrangement = Arrangement.SpaceAround,
            verticalAlignment = Alignment.CenterVertically
        ) {
            items.forEachIndexed { index, item ->
                val isSelected = index == selectedTabOrdinal

                Column(
                    modifier = Modifier
                        .weight(1f)
                        .clickable { onSelectTab(index) }
                        .padding(vertical = 8.dp),
                    horizontalAlignment = Alignment.CenterHorizontally,
                    verticalArrangement = Arrangement.Center
                ) {
                    Box(
                        modifier = Modifier
                            .width(64.dp)
                            .height(32.dp)
                            .clip(CircleShape)
                            .background(if (isSelected) M3ActivePill else Color.Transparent),
                        contentAlignment = Alignment.Center
                    ) {
                        Icon(
                            imageVector = if (isSelected) item.filledIcon else item.outlinedIcon,
                            contentDescription = item.title,
                            tint = if (isSelected) M3ActivePillText else M3TextMuted,
                            modifier = Modifier.size(22.dp)
                        )
                    }

                    Spacer(modifier = Modifier.height(4.dp))

                    Text(
                        text = item.title,
                        fontSize = 12.sp,
                        fontWeight = if (isSelected) FontWeight.SemiBold else FontWeight.Normal,
                        color = if (isSelected) M3TextPrimary else M3TextMuted
                    )
                }
            }
        }
    }
}
