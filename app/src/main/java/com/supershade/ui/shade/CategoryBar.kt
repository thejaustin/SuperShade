package com.supershade.ui.shade

import androidx.compose.animation.animateColorAsState
import androidx.compose.animation.core.Spring
import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.animation.core.spring
import androidx.compose.foundation.horizontalScroll
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.interaction.collectIsPressedAsState
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Alarm
import androidx.compose.material.icons.filled.AllInclusive
import androidx.compose.material.icons.filled.Apps
import androidx.compose.material.icons.filled.Call
import androidx.compose.material.icons.filled.ChatBubble
import androidx.compose.material.icons.filled.Email
import androidx.compose.material.icons.filled.Info
import androidx.compose.material.icons.filled.MusicNote
import androidx.compose.material.icons.filled.People
import androidx.compose.material.icons.filled.TaskAlt
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.supershade.domain.notification.model.ShadeCategory
import com.supershade.haptics.LocalSuperHaptics
import com.supershade.haptics.SuperHaptics
import com.supershade.ui.theme.LocalShadeShapeScheme
import com.supershade.ui.theme.getCardBorder

fun ShadeCategory.icon(): ImageVector = when (this) {
    ShadeCategory.All -> Icons.Default.AllInclusive
    ShadeCategory.Messages -> Icons.Default.ChatBubble
    ShadeCategory.Social -> Icons.Default.People
    ShadeCategory.Email -> Icons.Default.Email
    ShadeCategory.Calls -> Icons.Default.Call
    ShadeCategory.Productivity -> Icons.Default.TaskAlt
    ShadeCategory.Media -> Icons.Default.MusicNote
    ShadeCategory.Alarms -> Icons.Default.Alarm
    ShadeCategory.System -> Icons.Default.Info
    ShadeCategory.Apps -> Icons.Default.Apps
}

@Composable
fun CategoryBar(
    categories: List<ShadeCategory>,
    selected: ShadeCategory,
    onSelect: (ShadeCategory) -> Unit,
    counts: Map<ShadeCategory, Int> = emptyMap(),
) {
    val scrollState = rememberScrollState()

    val visibleCategories = remember(categories, selected, counts) {
        categories.filter { category ->
            category == ShadeCategory.All || category == selected || (counts[category] ?: 0) > 0
        }
    }

    // Auto-scroll toward the selected chip when selection changes.
    val selectedIndex = visibleCategories.indexOf(selected)
    LaunchedEffect(selectedIndex) {
        if (selectedIndex > 0) {
            scrollState.animateScrollTo(
                (selectedIndex * 120).coerceAtMost(scrollState.maxValue)
            )
        } else {
            scrollState.animateScrollTo(0)
        }
    }

    Row(
        modifier = Modifier
            .fillMaxWidth()
            .horizontalScroll(scrollState)
            .padding(horizontal = 16.dp, vertical = 6.dp),
        horizontalArrangement = Arrangement.spacedBy(8.dp),
    ) {
        visibleCategories.forEach { category ->
            CategoryChip(
                category = category,
                isSelected = category == selected,
                count = counts[category] ?: 0,
                onClick = { onSelect(category) },
            )
        }
    }
}

@Composable
private fun CategoryChip(
    category: ShadeCategory,
    isSelected: Boolean,
    count: Int,
    onClick: () -> Unit,
) {
    val context = LocalContext.current
    val haptics = LocalSuperHaptics.current ?: remember(context) { SuperHaptics(context) }
    val interactionSource = remember { MutableInteractionSource() }
    val isPressed by interactionSource.collectIsPressedAsState()

    // Active chip: Samsung blue/accent pill. Inactive: frosted container with subtle border.
    val containerColor by animateColorAsState(
        targetValue = if (isSelected)
            MaterialTheme.colorScheme.primary
        else
            MaterialTheme.colorScheme.surfaceContainer.copy(alpha = 0.70f),
        animationSpec = spring(
            dampingRatio = Spring.DampingRatioMediumBouncy,
            stiffness = Spring.StiffnessMediumLow,
        ),
        label = "chipContainer",
    )
    val labelColor by animateColorAsState(
        targetValue = if (isSelected)
            MaterialTheme.colorScheme.onPrimary
        else
            MaterialTheme.colorScheme.onSurface,
        animationSpec = spring(
            dampingRatio = Spring.DampingRatioMediumBouncy,
            stiffness = Spring.StiffnessMediumLow,
        ),
        label = "chipLabel",
    )

    // Press-responsive squish
    val scale by animateFloatAsState(
        targetValue = if (isPressed) 0.92f else 1f,
        animationSpec = spring(
            dampingRatio = Spring.DampingRatioLowBouncy,
            stiffness = Spring.StiffnessHigh,
        ),
        label = "chipScale",
    )

    val border = if (isSelected) null else getCardBorder(alpha = 0.35f)

    Surface(
        onClick = {
            haptics.lightTap()
            onClick()
        },
        shape = LocalShadeShapeScheme.current.chip,
        color = containerColor,
        border = border,
        interactionSource = interactionSource,
        modifier = Modifier.graphicsLayer { scaleX = scale; scaleY = scale },
    ) {
        Row(
            modifier = Modifier.padding(horizontal = 14.dp, vertical = 7.dp),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.spacedBy(6.dp),
        ) {
            Icon(
                imageVector = category.icon(),
                contentDescription = null,
                tint = labelColor,
                modifier = Modifier.size(15.dp),
            )
            Text(
                text = category.label,
                style = MaterialTheme.typography.labelMedium.copy(
                    fontWeight = if (isSelected) FontWeight.Bold else FontWeight.Medium
                ),
                color = labelColor,
            )
            if (count > 0) {
                Surface(
                    shape = CircleShape,
                    color = if (isSelected) MaterialTheme.colorScheme.onPrimary.copy(alpha = 0.22f)
                    else MaterialTheme.colorScheme.surfaceVariant,
                ) {
                    Text(
                        text = count.toString(),
                        style = MaterialTheme.typography.labelSmall.copy(
                            fontSize = 10.sp,
                            fontWeight = FontWeight.Bold,
                        ),
                        color = if (isSelected) MaterialTheme.colorScheme.onPrimary else MaterialTheme.colorScheme.primary,
                        modifier = Modifier.padding(horizontal = 6.dp, vertical = 1.dp),
                    )
                }
            }
        }
    }
}
