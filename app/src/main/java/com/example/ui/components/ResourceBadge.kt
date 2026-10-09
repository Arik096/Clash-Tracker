package com.example.ui.components

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Circle
import androidx.compose.material.icons.filled.Diamond
import androidx.compose.material.icons.filled.MonetizationOn
import androidx.compose.material.icons.filled.WaterDrop
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.model.BuildingCategory
import com.example.model.ResourceType
import com.example.ui.theme.CocDarkElixir
import com.example.ui.theme.CocElixir
import com.example.ui.theme.CocGem
import com.example.ui.theme.CocGold
import java.text.DecimalFormat
import java.text.NumberFormat
import java.util.Locale

fun formatResourceAmount(amount: Long): String {
    return when {
        amount >= 1_000_000 -> {
            val df = DecimalFormat("#.#")
            "${df.format(amount / 1_000_000.0)}M"
        }
        amount >= 1_000 -> {
            val df = DecimalFormat("#.#")
            "${df.format(amount / 1_000.0)}K"
        }
        else -> NumberFormat.getNumberInstance(Locale.US).format(amount)
    }
}

fun formatFullResourceAmount(amount: Long): String {
    return NumberFormat.getNumberInstance(Locale.US).format(amount)
}

fun formatDuration(seconds: Long): String {
    val days = seconds / 86400
    val hours = (seconds % 86400) / 3600
    val minutes = (seconds % 3600) / 60
    val secs = seconds % 60

    return when {
        days > 0 -> "${days}d ${hours}h"
        hours > 0 -> "${hours}h ${minutes}m"
        minutes > 0 -> "${minutes}m ${secs}s"
        else -> "${secs}s"
    }
}

fun formatRemainingMillis(millis: Long): String {
    if (millis <= 0) return "Finished!"
    val totalSecs = millis / 1000
    val days = totalSecs / 86400
    val hours = (totalSecs % 86400) / 3600
    val minutes = (totalSecs % 3600) / 60
    val seconds = totalSecs % 60

    return when {
        days > 0 -> "${days}d ${hours}h ${minutes}m"
        hours > 0 -> "${hours}h ${minutes}m ${seconds}s"
        else -> "${minutes}m ${seconds}s"
    }
}

fun getResourceColor(type: ResourceType): Color {
    return when (type) {
        ResourceType.GOLD -> CocGold
        ResourceType.ELIXIR -> CocElixir
        ResourceType.DARK_ELIXIR -> CocDarkElixir
        ResourceType.GEMS -> CocGem
    }
}

@Composable
fun ResourceBadge(
    type: ResourceType,
    amount: Long,
    modifier: Modifier = Modifier,
    useShortFormat: Boolean = true
) {
    val color = getResourceColor(type)
    Row(
        modifier = modifier
            .background(color.copy(alpha = 0.15f), RoundedCornerShape(8.dp))
            .padding(horizontal = 8.dp, vertical = 4.dp),
        verticalAlignment = Alignment.CenterVertically
    ) {
        when (type) {
            ResourceType.GOLD -> Icon(
                imageVector = Icons.Default.MonetizationOn,
                contentDescription = "Gold",
                tint = color,
                modifier = Modifier.size(16.dp)
            )
            ResourceType.ELIXIR -> Icon(
                imageVector = Icons.Default.WaterDrop,
                contentDescription = "Elixir",
                tint = color,
                modifier = Modifier.size(16.dp)
            )
            ResourceType.DARK_ELIXIR -> Icon(
                imageVector = Icons.Default.WaterDrop,
                contentDescription = "Dark Elixir",
                tint = color,
                modifier = Modifier.size(16.dp)
            )
            ResourceType.GEMS -> Icon(
                imageVector = Icons.Default.Diamond,
                contentDescription = "Gems",
                tint = color,
                modifier = Modifier.size(16.dp)
            )
        }
        Spacer(modifier = Modifier.width(4.dp))
        Text(
            text = if (useShortFormat) formatResourceAmount(amount) else formatFullResourceAmount(amount),
            style = MaterialTheme.typography.labelMedium.copy(
                fontWeight = FontWeight.Bold,
                color = color
            )
        )
    }
}

@Composable
fun CategoryChip(
    category: BuildingCategory,
    modifier: Modifier = Modifier
) {
    val chipColor = when (category) {
        BuildingCategory.TOWN_HALL -> Color(0xFFFF9800)
        BuildingCategory.DEFENSE -> Color(0xFFF44336)
        BuildingCategory.ARMY -> Color(0xFF9C27B0)
        BuildingCategory.HERO -> Color(0xFF00BCD4)
        BuildingCategory.RESOURCE -> Color(0xFF4CAF50)
        BuildingCategory.TRAP -> Color(0xFFFF5722)
        BuildingCategory.WALL -> Color(0xFF607D8B)
    }

    Box(
        modifier = modifier
            .background(chipColor.copy(alpha = 0.2f), RoundedCornerShape(6.dp))
            .padding(horizontal = 6.dp, vertical = 2.dp)
    ) {
        Text(
            text = category.displayName,
            style = MaterialTheme.typography.labelSmall.copy(
                fontWeight = FontWeight.SemiBold,
                color = chipColor,
                fontSize = 11.sp
            )
        )
    }
}
