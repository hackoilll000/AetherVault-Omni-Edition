package com.example.ui.components

import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.outlined.Archive
import androidx.compose.material.icons.outlined.Delete
import androidx.compose.material.icons.outlined.PhoneAndroid
import androidx.compose.material.icons.outlined.StarOutline
import androidx.compose.material3.Icon
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.ui.theme.M3TextPrimary
import com.example.ui.theme.M3TonalSurface

data class M3CategoryItem(
    val title: String,
    val icon: ImageVector,
    val onClick: () -> Unit
)

@Composable
fun M3QuickCategoryRow(
    onCategoryClick: (String) -> Unit,
    modifier: Modifier = Modifier
) {
    val items = listOf(
        M3CategoryItem("Favourites", Icons.Outlined.StarOutline) { onCategoryClick("Favourites") },
        M3CategoryItem("Screenshots", Icons.Outlined.PhoneAndroid) { onCategoryClick("Screenshots") },
        M3CategoryItem("Archive", Icons.Outlined.Archive) { onCategoryClick("Archive") },
        M3CategoryItem("Bin", Icons.Outlined.Delete) { onCategoryClick("Bin") }
    )

    Row(
        modifier = modifier
            .fillMaxWidth()
            .padding(horizontal = 16.dp, vertical = 8.dp),
        horizontalArrangement = Arrangement.spacedBy(10.dp)
    ) {
        items.forEach { item ->
            Column(
                modifier = Modifier
                    .weight(1f)
                    .clip(RoundedCornerShape(20.dp))
                    .background(M3TonalSurface)
                    .clickable { item.onClick() }
                    .padding(vertical = 16.dp, horizontal = 4.dp),
                horizontalAlignment = Alignment.CenterHorizontally,
                verticalArrangement = Arrangement.Center
            ) {
                Box(
                    modifier = Modifier
                        .size(48.dp)
                        .clip(CircleShape)
                        .background(androidx.compose.ui.graphics.Color.White),
                    contentAlignment = Alignment.Center
                ) {
                    Icon(
                        imageVector = item.icon,
                        contentDescription = item.title,
                        tint = M3TextPrimary,
                        modifier = Modifier.size(22.dp)
                    )
                }

                Spacer(modifier = Modifier.height(8.dp))

                Text(
                    text = item.title,
                    fontSize = 12.sp,
                    fontWeight = FontWeight.Medium,
                    color = M3TextPrimary,
                    textAlign = TextAlign.Center
                )
            }
        }
    }
}
