package com.supershade.ui.shade

import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.animateContentSize
import androidx.compose.animation.core.Spring
import androidx.compose.animation.core.animateDpAsState
import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.animation.core.spring
import androidx.compose.animation.expandVertically
import androidx.compose.animation.shrinkVertically
import androidx.compose.material.icons.filled.KeyboardArrowDown
import androidx.compose.foundation.Image
import androidx.compose.foundation.LocalIndication
import androidx.compose.foundation.background
import androidx.compose.foundation.combinedClickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxHeight
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.offset
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.border
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.ui.text.font.FontFamily
import com.supershade.ui.theme.LocalShadeTheme
import com.supershade.ui.theme.ShadeTheme
import com.supershade.ui.theme.ChamferedCornerShape
import androidx.compose.ui.text.font.FontWeight
import com.supershade.ui.theme.LocalShadeShapeScheme
import com.supershade.ui.theme.getCardBorder
import androidx.compose.ui.graphics.graphicsLayer
import android.content.Intent
import android.provider.Settings
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.ClearAll
import androidx.compose.material.icons.filled.Delete
import androidx.compose.material.icons.filled.ExpandLess
import androidx.compose.material.icons.filled.ExpandMore
import androidx.compose.material.icons.filled.NotificationsOff
import androidx.compose.material.icons.filled.Settings
import androidx.compose.material.icons.filled.Snooze
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.DropdownMenu
import androidx.compose.material3.DropdownMenuItem
import kotlin.math.abs as absF
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.produceState
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import com.supershade.haptics.LocalSuperHaptics
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.ImageBitmap
import androidx.compose.ui.graphics.asImageBitmap
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.core.graphics.drawable.toBitmap
import com.supershade.domain.notification.model.ShadeNotification
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext

data class NotificationGroup(
    val packageName: String,
    val groupKey: String?,
    val notifications: List<ShadeNotification>,
) {
    val isStacked: Boolean get() = notifications.size > 1
    val preview: ShadeNotification get() = notifications.first()
}

fun List<ShadeNotification>.toGroups(): List<NotificationGroup> {
    val map = linkedMapOf<String, MutableList<ShadeNotification>>()
    forEach { n ->
        val key = "${n.packageName}::${n.groupKey.orEmpty()}"
        map.getOrPut(key) { mutableListOf() }.add(n)
    }
    return map.values.map { items ->
        NotificationGroup(
            packageName = items.first().packageName,
            groupKey = items.first().groupKey,
            notifications = items,
        )
    }
}

@Composable
fun GroupedNotificationCard(
    group: NotificationGroup,
    onDismissGroup: () -> Unit,
    onDismiss: (String) -> Unit,
    onNotificationClick: (ShadeNotification) -> Unit,
    onSnooze: (String, Long) -> Unit = { _, _ -> },
    onHideChannel: (pkg: String, channelId: String) -> Unit = { _, _ -> },
    pinnedKeys: Set<String> = emptySet(),
    onTogglePin: (String) -> Unit = {},
    compact: Boolean = false,
    onReply: (ShadeNotification, String) -> Unit = { _, _ -> },
    modifier: Modifier = Modifier,
) {
    val shapes = LocalShadeShapeScheme.current
    val shadeTheme = LocalShadeTheme.current
    var expanded by remember { mutableStateOf(false) }
    var showSettingsMenu by remember { mutableStateOf(false) }
    val context = LocalContext.current
    val haptics = LocalSuperHaptics.current ?: remember(context) { com.supershade.haptics.SuperHaptics(context) }

    val appName = remember(group.packageName) {
        try {
            val info = context.packageManager.getApplicationInfo(group.packageName, 0)
            context.packageManager.getApplicationLabel(info).toString()
        } catch (_: Exception) {
            group.packageName.substringAfterLast('.').replaceFirstChar { it.uppercase() }
        }
    }

    val appIconBitmap by produceState<ImageBitmap?>(null, group.packageName) {
        value = withContext(Dispatchers.IO) {
            try {
                context.packageManager.getApplicationIcon(group.packageName)
                    .toBitmap(48, 48, android.graphics.Bitmap.Config.ARGB_8888)
                    .asImageBitmap()
            } catch (_: Exception) { null }
        }
    }

    val swipeState = rememberFluidSwipeState(key = group.groupKey ?: group.packageName)

    FluidSwipeToDismiss(
        state = swipeState,
        onDismissed = { onDismissGroup() },
        modifier = modifier.fillMaxWidth(),
        backgroundContent = {
            val isSwiping = !swipeState.isAtRest
            if (!isSwiping) return@FluidSwipeToDismiss
            val currentOffset = swipeState.offsetPx
            val alignment = if (currentOffset > 0f) Alignment.CenterStart else Alignment.CenterEnd
            val progress = swipeState.progress
            val isPastDismissThreshold = absF(currentOffset) > swipeState.thresholdPx

            val badgeScale by animateFloatAsState(
                targetValue = if (isPastDismissThreshold) 1.08f else (0.80f + progress * 0.40f).coerceIn(0.80f, 1.0f),
                animationSpec = spring(Spring.DampingRatioMediumBouncy, Spring.StiffnessMediumLow),
                label = "groupSwipeBadgeScale",
            )
            val iconRotation by animateFloatAsState(
                targetValue = if (isPastDismissThreshold) 0f else 12f,
                animationSpec = spring(Spring.DampingRatioMediumBouncy, Spring.StiffnessMediumLow),
                label = "groupSwipeIconRotation",
            )
            val trackBgColor = when (shadeTheme) {
                is ShadeTheme.Cyberpunk -> Color(0xFFFF0055).copy(alpha = (progress * 0.40f).coerceIn(0f, 0.35f))
                is ShadeTheme.Nothing -> Color(0xFFD71920).copy(alpha = (progress * 0.45f).coerceIn(0f, 0.40f))
                else -> Color(0xFFE53935).copy(alpha = (progress * 0.45f).coerceIn(0f, 0.40f))
            }
            val badgeColor = when (shadeTheme) {
                is ShadeTheme.Cyberpunk -> if (isPastDismissThreshold) Color(0xFFFF0055) else Color(0xFFFF0055).copy(alpha = 0.85f)
                is ShadeTheme.Nothing -> if (isPastDismissThreshold) Color(0xFFD71920) else Color(0xFFD71920).copy(alpha = 0.85f)
                else -> if (isPastDismissThreshold) Color(0xFFE53935) else Color(0xFFE53935).copy(alpha = 0.85f)
            }
            val badgeShape = when (shadeTheme) {
                is ShadeTheme.Cyberpunk -> ChamferedCornerShape(6.dp)
                is ShadeTheme.Nothing -> RoundedCornerShape(8.dp)
                is ShadeTheme.Pixel -> RoundedCornerShape(50)
                else -> RoundedCornerShape(20.dp)
            }
            val badgeBorder = when (shadeTheme) {
                is ShadeTheme.Cyberpunk -> BorderStroke(1.dp, Color(0xFFFF007F))
                is ShadeTheme.Nothing -> BorderStroke(1.dp, Color.White.copy(alpha = 0.30f))
                else -> null
            }
            val label = when (shadeTheme) {
                is ShadeTheme.Cyberpunk -> "[PURGE_GROUP]"
                is ShadeTheme.Nothing -> "CLEAR GROUP"
                else -> "Clear group"
            }

            val iconSlideOffset by animateDpAsState(
                targetValue = if (currentOffset > 0f) {
                    ((progress.coerceIn(0f, 0.35f) / 0.35f - 1f) * 16f).dp
                } else {
                    ((1f - progress.coerceIn(0f, 0.35f) / 0.35f) * 16f).dp
                },
                animationSpec = spring(Spring.DampingRatioMediumBouncy, Spring.StiffnessMediumLow),
                label = "groupSwipeIconSlide",
            )

            Box(
                modifier = Modifier
                    .fillMaxSize()
                    .clip(shapes.card)
                    .background(trackBgColor),
                contentAlignment = alignment,
            ) {
                Surface(
                    shape = badgeShape,
                    color = badgeColor,
                    border = badgeBorder,
                    shadowElevation = if (isPastDismissThreshold) 6.dp else 1.dp,
                    modifier = Modifier
                        .padding(horizontal = 16.dp)
                        .offset(x = iconSlideOffset)
                        .graphicsLayer {
                            scaleX = badgeScale
                            scaleY = badgeScale
                        },
                ) {
                    Row(
                        modifier = Modifier.padding(horizontal = 14.dp, vertical = 8.dp),
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.spacedBy(6.dp),
                    ) {
                        Icon(
                            imageVector = Icons.Default.Delete,
                            contentDescription = "Clear group",
                            tint = Color.White,
                            modifier = Modifier
                                .size(18.dp)
                                .graphicsLayer { rotationZ = iconRotation },
                        )
                        if (progress > 0.25f) {
                            Text(
                                text = label,
                                style = MaterialTheme.typography.labelMedium.copy(
                                    fontWeight = FontWeight.Bold,
                                    fontFamily = if (shadeTheme is ShadeTheme.Cyberpunk) FontFamily.Monospace else FontFamily.Default,
                                    fontSize = 12.5.sp,
                                ),
                                color = Color.White,
                            )
                        }
                    }
                }
            }
        },
    ) {
        val dragProgress = swipeState.progress
        val isSwiping = !swipeState.isAtRest
        val cardScale = (1.0f - dragProgress * 0.04f).coerceIn(0.95f, 1.0f)
        val cardAlpha = if (dragProgress > 0.75f) (1f - (dragProgress - 0.75f) * 2.5f).coerceIn(0.4f, 1.0f) else 1.0f
        val cardElevation = (dragProgress * 8f).dp

        val baseThemeShape = when (shadeTheme) {
            is ShadeTheme.Cyberpunk -> ChamferedCornerShape(10.dp)
            is ShadeTheme.Nothing -> RoundedCornerShape(16.dp)
            is ShadeTheme.Pixel -> RoundedCornerShape(26.dp)
            is ShadeTheme.PureMaterial -> RoundedCornerShape(24.dp)
            is ShadeTheme.OneUI -> RoundedCornerShape(24.dp)
            else -> shapes.card
        }

        val dynamicCardShape = if (isSwiping && dragProgress > 0.05f && baseThemeShape is RoundedCornerShape) {
            RoundedCornerShape(24.dp + (dragProgress * 6f).dp)
        } else {
            baseThemeShape
        }

        val cardBorder = when (shadeTheme) {
            is ShadeTheme.Cyberpunk -> BorderStroke(1.dp, Color(0xFF00F0FF).copy(alpha = 0.45f))
            is ShadeTheme.Nothing -> BorderStroke(1.dp, Color.White.copy(alpha = 0.16f))
            is ShadeTheme.OneUI -> BorderStroke(0.5.dp, MaterialTheme.colorScheme.outlineVariant.copy(alpha = 0.25f))
            else -> getCardBorder(alpha = 0.25f)
        }

        val cardBg = when (shadeTheme) {
            is ShadeTheme.Cyberpunk -> Color(0xFF0A0E1A)
            is ShadeTheme.Nothing -> Color(0xFF0D0F12)
            is ShadeTheme.Pixel -> MaterialTheme.colorScheme.surfaceContainerHigh
            else -> MaterialTheme.colorScheme.surfaceContainer
        }

        Box(
            modifier = Modifier
                .fillMaxWidth()
                .graphicsLayer {
                    scaleX = cardScale
                    scaleY = cardScale
                    alpha = cardAlpha
                }
        ) {

            val ghostBorder = when (shadeTheme) {
                is ShadeTheme.Cyberpunk -> BorderStroke(1.dp, Color(0xFF00F0FF).copy(alpha = 0.20f))
                is ShadeTheme.Nothing -> BorderStroke(1.dp, Color.White.copy(alpha = 0.10f))
                is ShadeTheme.OneUI -> BorderStroke(0.5.dp, MaterialTheme.colorScheme.outlineVariant.copy(alpha = 0.20f))
                else -> null
            }
            val ghostLowestBg = when (shadeTheme) {
                is ShadeTheme.Cyberpunk -> Color(0xFF060914)
                is ShadeTheme.Nothing -> Color(0xFF090A0D)
                is ShadeTheme.OneUI -> MaterialTheme.colorScheme.surfaceContainerLowest.copy(alpha = 0.70f)
                else -> MaterialTheme.colorScheme.surfaceContainerLowest.copy(alpha = 0.8f)
            }
            val ghostLowBg = when (shadeTheme) {
                is ShadeTheme.Cyberpunk -> Color(0xFF080D1C)
                is ShadeTheme.Nothing -> Color(0xFF0C0E12)
                is ShadeTheme.OneUI -> MaterialTheme.colorScheme.surfaceContainerLow.copy(alpha = 0.85f)
                else -> MaterialTheme.colorScheme.surfaceContainerLow.copy(alpha = 0.9f)
            }

            val showGhostCards = !expanded && (shadeTheme !is ShadeTheme.Pixel && shadeTheme !is ShadeTheme.PureMaterial)

            // Farthest ghost — narrowest, offsets most below the main card (One UI 8dp offset)
            if (showGhostCards && group.notifications.size >= 3) {
                Box(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(horizontal = 8.dp)
                        .align(Alignment.BottomCenter)
                        .offset(y = 8.dp)
                        .height(16.dp)
                        .clip(dynamicCardShape)
                        .background(ghostLowestBg)
                        .then(if (ghostBorder != null) Modifier.border(ghostBorder, dynamicCardShape) else Modifier),
                )
            }
            // Closer ghost — slightly less narrow, offset less (One UI 4dp offset)
            if (showGhostCards && group.notifications.size >= 2) {
                Box(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(horizontal = 4.dp)
                        .align(Alignment.BottomCenter)
                        .offset(y = 4.dp)
                        .height(12.dp)
                        .clip(dynamicCardShape)
                        .background(ghostLowBg)
                        .then(if (ghostBorder != null) Modifier.border(ghostBorder, dynamicCardShape) else Modifier),
                )
            }

            // Main card — on top, determines Box height
            Card(
                shape = dynamicCardShape,
                colors = CardDefaults.cardColors(
                    containerColor = cardBg,
                ),
                elevation = CardDefaults.cardElevation(defaultElevation = cardElevation),
                border = cardBorder,
                modifier = Modifier
                    .fillMaxWidth()
                    .animateContentSize(spring(Spring.DampingRatioMediumBouncy, Spring.StiffnessMedium))
                    .combinedClickable(
                        interactionSource = remember { androidx.compose.foundation.interaction.MutableInteractionSource() },
                        indication = LocalIndication.current,
                        onClick = { expanded = !expanded },
                        onLongClick = { showSettingsMenu = true },
                    ),
            ) {
                val hPadding = if (compact) 10.dp else 14.dp
                val vPadding = if (compact) 6.dp else 12.dp
                val iconBoxSize = if (compact) 30.dp else 38.dp
                val iconRadius = if (compact) 8.dp else 12.dp
                val iconImgSize = if (compact) 26.dp else 34.dp
                val columnSpacing = if (compact) 9.dp else 12.dp
                val iconShape = when (shadeTheme) {
                    is ShadeTheme.Cyberpunk -> ChamferedCornerShape(if (compact) 4.dp else 6.dp)
                    is ShadeTheme.Nothing -> RoundedCornerShape(if (compact) 6.dp else 8.dp)
                    is ShadeTheme.Pixel -> CircleShape
                    else -> RoundedCornerShape(iconRadius)
                }

                Column(modifier = Modifier.padding(horizontal = hPadding, vertical = vPadding)) {
                    // One UI / Multi-Theme Group header & collapsed preview
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.spacedBy(columnSpacing),
                        verticalAlignment = Alignment.Top,
                    ) {
                        // App icon badge
                        Box(
                            modifier = Modifier
                                .size(iconBoxSize)
                                .clip(iconShape)
                                .then(
                                    if (shadeTheme is ShadeTheme.Cyberpunk) {
                                        Modifier
                                            .background(Color(0xFF00F0FF).copy(alpha = 0.10f))
                                            .border(BorderStroke(1.dp, Color(0xFF00F0FF).copy(alpha = 0.40f)), iconShape)
                                    } else Modifier
                                ),
                            contentAlignment = Alignment.Center,
                        ) {
                            if (appIconBitmap != null) {
                                Image(
                                    bitmap = appIconBitmap!!,
                                    contentDescription = null,
                                    modifier = Modifier
                                        .size(iconImgSize)
                                        .clip(RoundedCornerShape(if (compact) 6.dp else 8.dp)),
                                )
                            } else {
                                Box(
                                    modifier = Modifier
                                        .size(iconBoxSize)
                                        .clip(iconShape)
                                        .background(MaterialTheme.colorScheme.primaryContainer),
                                    contentAlignment = Alignment.Center,
                                ) {
                                    Text(
                                        text = appName.take(1).uppercase(),
                                        style = if (compact)
                                            MaterialTheme.typography.labelMedium.copy(fontWeight = FontWeight.Bold)
                                        else
                                            MaterialTheme.typography.titleSmall.copy(fontWeight = FontWeight.Bold),
                                        color = MaterialTheme.colorScheme.onPrimaryContainer,
                                    )
                                }
                            }
                        }

                        // App Name + Collapsed preview column
                        Column(modifier = Modifier.weight(1f)) {
                            Row(
                                modifier = Modifier.fillMaxWidth(),
                                verticalAlignment = Alignment.CenterVertically,
                                horizontalArrangement = Arrangement.SpaceBetween,
                            ) {
                                Text(
                                    text = if (shadeTheme is ShadeTheme.Cyberpunk) "[GROUP // ${appName.uppercase()}]"
                                           else if (shadeTheme is ShadeTheme.Nothing) appName.uppercase()
                                           else appName,
                                    style = if (compact)
                                        MaterialTheme.typography.labelSmall.copy(
                                            fontWeight = FontWeight.SemiBold,
                                            fontFamily = if (shadeTheme is ShadeTheme.Cyberpunk) FontFamily.Monospace else FontFamily.Default,
                                            fontSize = 11.5.sp,
                                        )
                                    else
                                        MaterialTheme.typography.labelMedium.copy(
                                            fontWeight = FontWeight.SemiBold,
                                            fontFamily = if (shadeTheme is ShadeTheme.Cyberpunk) FontFamily.Monospace else FontFamily.Default,
                                        ),
                                    color = if (shadeTheme is ShadeTheme.Cyberpunk) Color(0xFF00F0FF) else MaterialTheme.colorScheme.onSurfaceVariant,
                                    maxLines = 1,
                                    overflow = TextOverflow.Ellipsis,
                                    modifier = Modifier.weight(1f, fill = false),
                                )
                                Row(
                                    verticalAlignment = Alignment.CenterVertically,
                                    horizontalArrangement = Arrangement.spacedBy(if (compact) 3.dp else 4.dp),
                                ) {
                                    val countBadgeShape = when (shadeTheme) {
                                        is ShadeTheme.Cyberpunk -> ChamferedCornerShape(4.dp)
                                        is ShadeTheme.Nothing -> RoundedCornerShape(4.dp)
                                        is ShadeTheme.Pixel -> RoundedCornerShape(50)
                                        is ShadeTheme.OneUI -> RoundedCornerShape(50)
                                        else -> RoundedCornerShape(6.dp)
                                    }
                                    val countBadgeBg = when (shadeTheme) {
                                        is ShadeTheme.Cyberpunk -> Color(0xFF00F0FF).copy(alpha = 0.20f)
                                        is ShadeTheme.Nothing -> Color(0xFFD71920)
                                        is ShadeTheme.Pixel -> MaterialTheme.colorScheme.secondaryContainer
                                        is ShadeTheme.OneUI -> MaterialTheme.colorScheme.primaryContainer.copy(alpha = 0.75f)
                                        else -> MaterialTheme.colorScheme.primaryContainer.copy(alpha = 0.6f)
                                    }
                                    val countBadgeTextColor = when (shadeTheme) {
                                        is ShadeTheme.Cyberpunk -> Color(0xFF00F0FF)
                                        is ShadeTheme.Nothing -> Color.White
                                        is ShadeTheme.Pixel -> MaterialTheme.colorScheme.onSecondaryContainer
                                        is ShadeTheme.OneUI -> MaterialTheme.colorScheme.onPrimaryContainer
                                        else -> MaterialTheme.colorScheme.primary
                                    }
                                    val badgeText = when (shadeTheme) {
                                        is ShadeTheme.OneUI -> if (compact) "${group.notifications.size}" else "${group.notifications.size} new"
                                        is ShadeTheme.Cyberpunk -> "[${group.notifications.size}]"
                                        else -> "${group.notifications.size}"
                                    }

                                    val hasVip = group.notifications.any { it.isVipAlert }
                                    val hasOtp = group.notifications.any { it.otpCode != null }
                                    if (hasVip) {
                                        Text(
                                            text = if (shadeTheme is ShadeTheme.Cyberpunk) "[VIP]" else "VIP",
                                            style = MaterialTheme.typography.labelSmall.copy(
                                                fontWeight = FontWeight.Bold,
                                                fontFamily = if (shadeTheme is ShadeTheme.Cyberpunk) FontFamily.Monospace else FontFamily.Default,
                                                fontSize = if (compact) 9.sp else 10.sp,
                                            ),
                                            color = if (shadeTheme is ShadeTheme.Cyberpunk) Color(0xFFFF0055) else MaterialTheme.colorScheme.error,
                                            modifier = Modifier
                                                .clip(countBadgeShape)
                                                .background(
                                                    if (shadeTheme is ShadeTheme.Cyberpunk) Color(0xFFFF0055).copy(alpha = 0.20f)
                                                    else MaterialTheme.colorScheme.errorContainer.copy(alpha = 0.70f)
                                                )
                                                .padding(horizontal = if (compact) 4.dp else 6.dp, vertical = 2.dp),
                                        )
                                    } else if (hasOtp) {
                                        Text(
                                            text = if (shadeTheme is ShadeTheme.Cyberpunk) "[OTP]" else "OTP",
                                            style = MaterialTheme.typography.labelSmall.copy(
                                                fontWeight = FontWeight.Bold,
                                                fontFamily = if (shadeTheme is ShadeTheme.Cyberpunk) FontFamily.Monospace else FontFamily.Default,
                                                fontSize = if (compact) 9.sp else 10.sp,
                                            ),
                                            color = if (shadeTheme is ShadeTheme.Cyberpunk) Color(0xFF00F0FF) else MaterialTheme.colorScheme.primary,
                                            modifier = Modifier
                                                .clip(countBadgeShape)
                                                .background(
                                                    if (shadeTheme is ShadeTheme.Cyberpunk) Color(0xFF00F0FF).copy(alpha = 0.20f)
                                                    else MaterialTheme.colorScheme.primaryContainer.copy(alpha = 0.70f)
                                                )
                                                .padding(horizontal = if (compact) 4.dp else 6.dp, vertical = 2.dp),
                                        )
                                    }

                                    Text(
                                        text = badgeText,
                                        style = MaterialTheme.typography.labelSmall.copy(
                                            fontWeight = FontWeight.Bold,
                                            fontFamily = if (shadeTheme is ShadeTheme.Cyberpunk) FontFamily.Monospace else FontFamily.Default,
                                            fontSize = if (compact) 10.sp else 11.sp,
                                        ),
                                        color = countBadgeTextColor,
                                        modifier = Modifier
                                            .clip(countBadgeShape)
                                            .background(countBadgeBg)
                                            .padding(horizontal = if (compact) 6.dp else 8.dp, vertical = 2.dp),
                                    )
                                    if (expanded) {
                                        IconButton(
                                            onClick = {
                                                onDismissGroup()
                                            },
                                            modifier = Modifier.size(if (compact) 28.dp else 36.dp),
                                        ) {
                                            Icon(
                                                imageVector = Icons.Default.ClearAll,
                                                contentDescription = "Clear group",
                                                tint = MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.75f),
                                                modifier = Modifier.size(if (compact) 15.dp else 18.dp),
                                            )
                                        }
                                    }
                                    val groupChevronRotation by animateFloatAsState(
                                        targetValue = if (expanded) 180f else 0f,
                                        animationSpec = spring(Spring.DampingRatioMediumBouncy, Spring.StiffnessMediumLow),
                                        label = "groupChevronRotation",
                                    )
                                    IconButton(
                                        onClick = {
                                            haptics.lightTap()
                                            expanded = !expanded
                                        },
                                        modifier = Modifier.size(if (compact) 28.dp else 36.dp),
                                    ) {
                                        Icon(
                                            imageVector = Icons.Default.KeyboardArrowDown,
                                            contentDescription = if (expanded) "Collapse group" else "Expand group",
                                            tint = MaterialTheme.colorScheme.onSurfaceVariant,
                                            modifier = Modifier
                                                .size(if (compact) 18.dp else 22.dp)
                                                .graphicsLayer { rotationZ = groupChevronRotation },
                                        )
                                    }
                                }
                            }

                            // Collapsed preview: first notification title + text + "+N more"
                            if (!expanded) {
                                val preview = group.preview
                                val displayTitle = preview.title.ifBlank { appName }
                                if (compact) {
                                    Row(
                                        modifier = Modifier.fillMaxWidth().padding(top = 1.dp),
                                        verticalAlignment = Alignment.CenterVertically,
                                        horizontalArrangement = Arrangement.spacedBy(4.dp),
                                    ) {
                                        Text(
                                            text = displayTitle,
                                            style = MaterialTheme.typography.bodySmall.copy(
                                                fontWeight = FontWeight.SemiBold,
                                                fontSize = 12.sp,
                                            ),
                                            color = MaterialTheme.colorScheme.onSurface,
                                            maxLines = 1,
                                            overflow = TextOverflow.Ellipsis,
                                            modifier = Modifier.weight(1f, fill = false),
                                        )
                                        if (preview.text.isNotBlank()) {
                                            Text(
                                                text = "— ${preview.text}",
                                                style = MaterialTheme.typography.bodySmall.copy(fontSize = 11.5.sp),
                                                color = MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.85f),
                                                maxLines = 1,
                                                overflow = TextOverflow.Ellipsis,
                                                modifier = Modifier.weight(1f),
                                            )
                                        }
                                    }
                                } else {
                                    Spacer(Modifier.height(2.dp))
                                    Text(
                                        text = displayTitle,
                                        style = MaterialTheme.typography.titleSmall.copy(fontWeight = FontWeight.SemiBold),
                                        color = MaterialTheme.colorScheme.onSurface,
                                        maxLines = 1,
                                        overflow = TextOverflow.Ellipsis,
                                    )
                                    if (preview.text.isNotBlank()) {
                                        Spacer(Modifier.height(2.dp))
                                        Text(
                                            text = preview.text,
                                            style = MaterialTheme.typography.bodySmall,
                                            color = MaterialTheme.colorScheme.onSurfaceVariant,
                                            maxLines = 2,
                                            overflow = TextOverflow.Ellipsis,
                                        )
                                    }
                                    if (group.notifications.size > 1) {
                                        Spacer(Modifier.height(4.dp))
                                        val moreText = when (shadeTheme) {
                                            is ShadeTheme.Cyberpunk -> "[+${group.notifications.size - 1} PACKETS PENDING]"
                                            is ShadeTheme.Nothing -> "+${group.notifications.size - 1} MORE"
                                            else -> "+${group.notifications.size - 1} more"
                                        }
                                        Text(
                                            text = moreText,
                                            style = MaterialTheme.typography.labelSmall.copy(
                                                fontFamily = if (shadeTheme is ShadeTheme.Cyberpunk) FontFamily.Monospace else FontFamily.Default,
                                                fontWeight = FontWeight.SemiBold,
                                            ),
                                            color = when (shadeTheme) {
                                                is ShadeTheme.Cyberpunk -> Color(0xFF00F0FF)
                                                is ShadeTheme.Nothing -> Color.White.copy(alpha = 0.85f)
                                                else -> MaterialTheme.colorScheme.primary.copy(alpha = 0.85f)
                                            },
                                        )
                                    }
                                }
                            }
                        }
                    }

                    // Settings & Snooze dropdown for the whole group — shown on long-press (OS-style)
                    DropdownMenu(
                        expanded = showSettingsMenu,
                        onDismissRequest = { showSettingsMenu = false },
                    ) {
                        DropdownMenuItem(
                            text = { Text("Notification settings", style = MaterialTheme.typography.bodyMedium) },
                            leadingIcon = {
                                Icon(Icons.Default.Settings, contentDescription = null, modifier = Modifier.size(20.dp))
                            },
                            onClick = {
                                showSettingsMenu = false
                                try {
                                    val intent = Intent(Settings.ACTION_APP_NOTIFICATION_SETTINGS).apply {
                                        putExtra(Settings.EXTRA_APP_PACKAGE, group.packageName)
                                        addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
                                    }
                                    context.startActivity(intent)
                                } catch (_: Exception) {}
                            },
                        )
                        val channelId = group.preview.channelId
                        if (channelId != null) {
                            DropdownMenuItem(
                                text = { Text("Turn off notifications", style = MaterialTheme.typography.bodyMedium) },
                                leadingIcon = {
                                    Icon(Icons.Default.NotificationsOff, contentDescription = null, modifier = Modifier.size(20.dp))
                                },
                                onClick = {
                                    showSettingsMenu = false
                                    onHideChannel(group.packageName, channelId)
                                },
                            )
                            DropdownMenuItem(
                                text = { Text("Open notification settings →", style = MaterialTheme.typography.bodyMedium) },
                                leadingIcon = {
                                    Icon(Icons.Default.Settings, contentDescription = null, modifier = Modifier.size(20.dp))
                                },
                                onClick = {
                                    showSettingsMenu = false
                                    try {
                                        val intent = Intent(Settings.ACTION_CHANNEL_NOTIFICATION_SETTINGS).apply {
                                            putExtra(Settings.EXTRA_APP_PACKAGE, group.packageName)
                                            putExtra(Settings.EXTRA_CHANNEL_ID, channelId)
                                            addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
                                        }
                                        context.startActivity(intent)
                                    } catch (_: Exception) {}
                                },
                            )
                        } else {
                            DropdownMenuItem(
                                text = { Text("Turn off notifications from app", style = MaterialTheme.typography.bodyMedium) },
                                leadingIcon = {
                                    Icon(Icons.Default.NotificationsOff, contentDescription = null, modifier = Modifier.size(20.dp))
                                },
                                onClick = {
                                    showSettingsMenu = false
                                    onHideChannel(group.packageName, "default")
                                },
                            )
                        }
                        HorizontalDivider(modifier = Modifier.padding(vertical = 4.dp))
                        listOf(
                            "Snooze 15 minutes" to 15 * 60 * 1_000L,
                            "Snooze 1 hour"     to 60 * 60 * 1_000L,
                            "Snooze 4 hours"    to 4 * 60 * 60 * 1_000L,
                        ).forEach { (label, delayMs) ->
                            DropdownMenuItem(
                                text = { Text(label, style = MaterialTheme.typography.bodyMedium) },
                                leadingIcon = {
                                    Icon(Icons.Default.Snooze, contentDescription = null, modifier = Modifier.size(20.dp))
                                },
                                onClick = {
                                    group.notifications.forEach { onSnooze(it.key, delayMs) }
                                    showSettingsMenu = false
                                },
                            )
                        }
                    }
                }

                // Expanded: individual cards grouped by notification channel
                AnimatedVisibility(
                    visible = expanded,
                    enter = expandVertically(spring(Spring.DampingRatioMediumBouncy, Spring.StiffnessMedium)),
                    exit = shrinkVertically(spring(Spring.DampingRatioMediumBouncy, Spring.StiffnessMedium)),
                ) {
                    Column {
                        HorizontalDivider(modifier = Modifier.padding(horizontal = 14.dp))

                        // Group notifications by channelId so we can show channel sub-headers
                        val byChannel = remember(group.notifications) {
                            group.notifications
                                .groupBy { it.channelId ?: "" }
                                .entries.toList()
                        }
                        val multiChannel = byChannel.size > 1

                        byChannel.forEachIndexed { channelIdx, (channelId, channelNotifs) ->
                            // ── Channel sub-header (only shown when > 1 channel present) ──────
                            if (multiChannel) {
                                Row(
                                    modifier = Modifier
                                        .fillMaxWidth()
                                        .padding(horizontal = 16.dp, vertical = 4.dp),
                                    verticalAlignment = Alignment.CenterVertically,
                                    horizontalArrangement = Arrangement.spacedBy(8.dp),
                                ) {
                                    val channelLabel = when (shadeTheme) {
                                        is ShadeTheme.Cyberpunk -> "[CHANNEL // ${channelId.ifBlank { "DEFAULT" }.uppercase()}]"
                                        is ShadeTheme.Nothing -> channelId.ifBlank { "DEFAULT" }.uppercase()
                                        else -> channelId.ifBlank { "Default" }.let { id ->
                                            id.replace(Regex("[_.]"), " ")
                                              .split(" ")
                                              .joinToString(" ") { word -> word.replaceFirstChar { it.uppercase() } }
                                        }
                                    }
                                    Text(
                                        text = channelLabel,
                                        style = MaterialTheme.typography.labelSmall.copy(
                                            fontWeight = FontWeight.SemiBold,
                                            fontFamily = if (shadeTheme is ShadeTheme.Cyberpunk) FontFamily.Monospace else FontFamily.Default,
                                        ),
                                        color = if (shadeTheme is ShadeTheme.Cyberpunk) Color(0xFF00F0FF) else MaterialTheme.colorScheme.primary.copy(alpha = 0.80f),
                                    )
                                    HorizontalDivider(
                                        modifier = Modifier.weight(1f),
                                        color = if (shadeTheme is ShadeTheme.Cyberpunk) Color(0xFF00F0FF).copy(alpha = 0.25f) else MaterialTheme.colorScheme.outlineVariant.copy(alpha = 0.35f),
                                        thickness = 0.5.dp,
                                    )
                                    // "Manage" chip taps to channel settings
                                    val manageShape = when (shadeTheme) {
                                        is ShadeTheme.Cyberpunk -> ChamferedCornerShape(4.dp)
                                        is ShadeTheme.Nothing -> RoundedCornerShape(6.dp)
                                        is ShadeTheme.Pixel -> RoundedCornerShape(50)
                                        else -> RoundedCornerShape(12.dp)
                                    }
                                    val manageBorder = when (shadeTheme) {
                                        is ShadeTheme.Cyberpunk -> BorderStroke(1.dp, Color(0xFF00F0FF).copy(alpha = 0.40f))
                                        is ShadeTheme.Nothing -> BorderStroke(1.dp, Color.White.copy(alpha = 0.20f))
                                        else -> null
                                    }
                                    val manageBg = when (shadeTheme) {
                                        is ShadeTheme.Cyberpunk -> Color(0xFF070B16)
                                        is ShadeTheme.Nothing -> Color(0xFF14171C)
                                        else -> MaterialTheme.colorScheme.surfaceContainerHighest.copy(alpha = 0.60f)
                                    }
                                    Surface(
                                        onClick = {
                                            if (channelId.isNotBlank()) {
                                                try {
                                                    val intent = Intent(Settings.ACTION_CHANNEL_NOTIFICATION_SETTINGS).apply {
                                                        putExtra(Settings.EXTRA_APP_PACKAGE, group.packageName)
                                                        putExtra(Settings.EXTRA_CHANNEL_ID, channelId)
                                                        addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
                                                    }
                                                    context.startActivity(intent)
                                                } catch (_: Exception) {}
                                            }
                                        },
                                        shape = manageShape,
                                        color = manageBg,
                                        border = manageBorder,
                                        modifier = Modifier,
                                    ) {
                                        Text(
                                            text = when (shadeTheme) {
                                                is ShadeTheme.Cyberpunk -> "// CFG"
                                                is ShadeTheme.Nothing -> "MANAGE"
                                                else -> "Manage"
                                            },
                                            style = MaterialTheme.typography.labelSmall.copy(
                                                fontSize = 9.sp,
                                                fontFamily = if (shadeTheme is ShadeTheme.Cyberpunk) FontFamily.Monospace else FontFamily.Default,
                                                fontWeight = FontWeight.Bold,
                                            ),
                                            color = if (shadeTheme is ShadeTheme.Cyberpunk) Color(0xFF00F0FF) else MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.7f),
                                            modifier = Modifier.padding(horizontal = 8.dp, vertical = 3.dp),
                                        )
                                    }
                                }
                            }

                            channelNotifs.forEachIndexed { index, notification ->
                                NotificationCard(
                                    notification = notification,
                                    onDismiss = { onDismiss(notification.key) },
                                    onClick = { onNotificationClick(notification) },
                                    onSnooze = { delayMs -> onSnooze(notification.key, delayMs) },
                                    onHideChannel = onHideChannel,
                                    isPinned = notification.key in pinnedKeys,
                                    onTogglePin = { onTogglePin(notification.key) },
                                    compact = compact,
                                    onReply = onReply,
                                    modifier = Modifier.padding(horizontal = 4.dp, vertical = if (compact) 2.dp else 4.dp),
                                )
                                val isLast = channelIdx == byChannel.lastIndex && index == channelNotifs.lastIndex
                                if (!isLast) {
                                    HorizontalDivider(
                                        modifier = Modifier.padding(horizontal = 14.dp),
                                        thickness = if (shadeTheme is ShadeTheme.Pixel) 0.8.dp else 0.5.dp,
                                        color = if (shadeTheme is ShadeTheme.Pixel) {
                                            MaterialTheme.colorScheme.outlineVariant.copy(alpha = 0.5f)
                                        } else {
                                            MaterialTheme.colorScheme.outlineVariant.copy(alpha = 0.35f)
                                        },
                                    )
                                }
                            }
                        }
                        Spacer(Modifier.height(4.dp))
                    }
                }
            }
        }
    }
}
