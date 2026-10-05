package com.supershade.ui.shade

import android.content.Intent
import android.content.pm.PackageManager
import android.provider.Settings
import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.animateColorAsState
import androidx.compose.animation.core.Spring
import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.animation.core.spring
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.animation.slideInVertically
import androidx.compose.animation.slideOutVertically
import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.horizontalScroll
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxHeight
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.navigationBarsPadding
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.OpenInNew
import androidx.compose.material.icons.filled.Close
import androidx.compose.material.icons.filled.DeleteSweep
import androidx.compose.material.icons.filled.History
import androidx.compose.material.icons.filled.HistoryToggleOff
import androidx.compose.material.icons.filled.Search
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.OutlinedTextFieldDefaults
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.produceState
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.ImageBitmap
import androidx.compose.ui.graphics.asImageBitmap
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.core.graphics.drawable.toBitmap
import androidx.compose.foundation.BorderStroke
import androidx.compose.ui.text.font.FontFamily
import com.supershade.domain.notification.DismissedNotificationRecord
import com.supershade.domain.notification.model.ShadeCategory
import com.supershade.haptics.LocalSuperHaptics
import com.supershade.haptics.SuperHaptics
import com.supershade.ui.theme.ChamferedCornerShape
import com.supershade.ui.theme.LocalShadeShapeScheme
import com.supershade.ui.theme.LocalShadeTheme
import com.supershade.ui.theme.ShadeTheme
import com.supershade.ui.theme.getCardBorder
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext

@Composable
fun NotificationHistorySheet(
    isOpen: Boolean,
    history: List<DismissedNotificationRecord>,
    onDismiss: () -> Unit,
    onClearHistory: () -> Unit,
    modifier: Modifier = Modifier,
) {
    val context = LocalContext.current
    val haptics = LocalSuperHaptics.current ?: remember(context) { SuperHaptics(context) }
    val shapes = LocalShadeShapeScheme.current
    val theme = LocalShadeTheme.current

    val sheetShape = when (theme) {
        is ShadeTheme.Cyberpunk -> ChamferedCornerShape(16.dp)
        is ShadeTheme.Nothing -> RoundedCornerShape(topStart = 20.dp, topEnd = 20.dp)
        else -> RoundedCornerShape(topStart = 28.dp, topEnd = 28.dp)
    }
    val sheetBorder = when (theme) {
        is ShadeTheme.Cyberpunk -> BorderStroke(1.dp, Color(0xFF00F0FF).copy(alpha = 0.40f))
        is ShadeTheme.Nothing -> BorderStroke(1.dp, Color.White.copy(alpha = 0.18f))
        else -> getCardBorder(alpha = 0.35f)
    }

    AnimatedVisibility(
        visible = isOpen,
        enter = fadeIn(spring(stiffness = Spring.StiffnessMediumLow)) +
            slideInVertically(
                spring(dampingRatio = Spring.DampingRatioLowBouncy, stiffness = Spring.StiffnessMediumLow),
                initialOffsetY = { it / 2 },
            ),
        exit = fadeOut(spring(stiffness = Spring.StiffnessMedium)) +
            slideOutVertically(
                spring(stiffness = Spring.StiffnessMedium),
                targetOffsetY = { it },
            ),
        modifier = modifier.fillMaxSize(),
    ) {
        Box(
            modifier = Modifier
                .fillMaxSize()
                .background(Color.Black.copy(alpha = 0.52f))
                .clickable(
                    interactionSource = remember { MutableInteractionSource() },
                    indication = null,
                    onClick = onDismiss,
                ),
            contentAlignment = Alignment.BottomCenter,
        ) {
            Surface(
                shape = sheetShape,
                color = when (theme) {
                    is ShadeTheme.Cyberpunk -> Color(0xFF040711)
                    is ShadeTheme.Nothing -> Color(0xFF08090C)
                    else -> MaterialTheme.colorScheme.surfaceContainer
                },
                border = sheetBorder,
                modifier = Modifier
                    .fillMaxWidth()
                    .heightIn(max = 680.dp)
                    .clickable(
                        interactionSource = remember { MutableInteractionSource() },
                        indication = null,
                        onClick = {}, // Prevent tap through
                    ),
            ) {
                Column(
                    modifier = Modifier
                        .fillMaxWidth()
                        .navigationBarsPadding(),
                ) {
                    // Drag pill indicator
                    Box(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(top = 10.dp, bottom = 4.dp),
                        contentAlignment = Alignment.Center,
                    ) {
                        Box(
                            modifier = Modifier
                                .size(width = 38.dp, height = 4.5.dp)
                                .clip(
                                    when (theme) {
                                        is ShadeTheme.Cyberpunk -> ChamferedCornerShape(2.dp)
                                        else -> RoundedCornerShape(50)
                                    }
                                )
                                .background(
                                    when (theme) {
                                        is ShadeTheme.Cyberpunk -> Color(0xFF00F0FF).copy(alpha = 0.65f)
                                        is ShadeTheme.Nothing -> Color.White.copy(alpha = 0.40f)
                                        else -> MaterialTheme.colorScheme.outlineVariant.copy(alpha = 0.6f)
                                    }
                                ),
                        )
                    }

                    // Header row
                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(horizontal = 20.dp, vertical = 8.dp),
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.SpaceBetween,
                    ) {
                        Row(
                            verticalAlignment = Alignment.CenterVertically,
                            horizontalArrangement = Arrangement.spacedBy(10.dp),
                        ) {
                            val headerIconShape = when (theme) {
                                is ShadeTheme.Cyberpunk -> ChamferedCornerShape(6.dp)
                                is ShadeTheme.Nothing -> RoundedCornerShape(8.dp)
                                is ShadeTheme.OneUI -> RoundedCornerShape(12.dp)
                                else -> CircleShape
                            }
                            val headerIconBorder = when (theme) {
                                is ShadeTheme.Cyberpunk -> BorderStroke(1.dp, Color(0xFF00F0FF).copy(alpha = 0.5f))
                                is ShadeTheme.Nothing -> BorderStroke(1.dp, Color.White.copy(alpha = 0.2f))
                                else -> null
                            }
                            val headerIconBg = when (theme) {
                                is ShadeTheme.Cyberpunk -> Color(0xFF050B14)
                                is ShadeTheme.Nothing -> Color(0xFF14161B)
                                else -> MaterialTheme.colorScheme.primaryContainer.copy(alpha = 0.65f)
                            }
                            val headerIconTint = when (theme) {
                                is ShadeTheme.Cyberpunk -> Color(0xFF00F0FF)
                                is ShadeTheme.Nothing -> Color.White
                                else -> MaterialTheme.colorScheme.primary
                            }

                            Box(
                                modifier = Modifier
                                    .size(36.dp)
                                    .clip(headerIconShape)
                                    .then(if (headerIconBorder != null) Modifier.border(headerIconBorder, headerIconShape) else Modifier)
                                    .background(headerIconBg),
                                contentAlignment = Alignment.Center,
                            ) {
                                Icon(
                                    imageVector = Icons.Default.History,
                                    contentDescription = null,
                                    tint = headerIconTint,
                                    modifier = Modifier.size(20.dp),
                                )
                            }
                            Column {
                                val titleText = when (theme) {
                                    is ShadeTheme.Cyberpunk -> "[ARCHIVE // LOGS]"
                                    is ShadeTheme.Nothing -> "HISTORY"
                                    is ShadeTheme.OneUI -> "Notification history"
                                    else -> "Notification History"
                                }
                                val subtitleText = when (theme) {
                                    is ShadeTheme.Cyberpunk -> "${history.size} ARCHIVED PACKETS"
                                    is ShadeTheme.Nothing -> "${history.size} DISMISSED"
                                    else -> "${history.size} dismissed notification${if (history.size != 1) "s" else ""}"
                                }
                                val titleFont = when (theme) {
                                    is ShadeTheme.Cyberpunk -> FontFamily.Monospace
                                    else -> FontFamily.Default
                                }
                                val titleColor = when (theme) {
                                    is ShadeTheme.Cyberpunk -> Color(0xFF00F0FF)
                                    else -> MaterialTheme.colorScheme.onSurface
                                }
                                val subtitleColor = when (theme) {
                                    is ShadeTheme.Cyberpunk -> Color(0xFFFF007F).copy(alpha = 0.85f)
                                    is ShadeTheme.Nothing -> Color(0xFFD71920).copy(alpha = 0.9f)
                                    else -> MaterialTheme.colorScheme.onSurfaceVariant
                                }

                                Text(
                                    text = titleText,
                                    style = MaterialTheme.typography.titleMedium.copy(
                                        fontWeight = FontWeight.Bold,
                                        fontFamily = titleFont,
                                        letterSpacing = if (theme is ShadeTheme.Nothing) 1.sp else 0.sp,
                                    ),
                                    color = titleColor,
                                )
                                Text(
                                    text = subtitleText,
                                    style = MaterialTheme.typography.bodySmall.copy(
                                        fontFamily = if (theme is ShadeTheme.Cyberpunk) FontFamily.Monospace else FontFamily.Default,
                                    ),
                                    color = subtitleColor,
                                )
                            }
                        }

                        Row(
                            verticalAlignment = Alignment.CenterVertically,
                            horizontalArrangement = Arrangement.spacedBy(4.dp),
                        ) {
                            if (history.isNotEmpty()) {
                                IconButton(
                                    onClick = {
                                        haptics.notificationDismissCommit()
                                        onClearHistory()
                                    },
                                    modifier = Modifier.size(36.dp),
                                ) {
                                    Icon(
                                        imageVector = Icons.Default.DeleteSweep,
                                        contentDescription = "Clear history",
                                        tint = MaterialTheme.colorScheme.error.copy(alpha = 0.85f),
                                        modifier = Modifier.size(20.dp),
                                    )
                                }
                            }
                            IconButton(
                                onClick = {
                                    haptics.lightTap()
                                    onDismiss()
                                },
                                modifier = Modifier.size(36.dp),
                            ) {
                                Icon(
                                    imageVector = Icons.Default.Close,
                                    contentDescription = "Close",
                                    tint = MaterialTheme.colorScheme.onSurfaceVariant,
                                    modifier = Modifier.size(20.dp),
                                )
                            }
                        }
                    }

                    var searchQuery by remember { mutableStateOf("") }
                    var selectedCategoryFilter by remember { mutableStateOf<ShadeCategory?>(null) }

                    // Search input
                    if (history.size > 3) {
                        val searchShape = when (theme) {
                            is ShadeTheme.Cyberpunk -> ChamferedCornerShape(6.dp)
                            is ShadeTheme.Nothing -> RoundedCornerShape(8.dp)
                            is ShadeTheme.OneUI -> RoundedCornerShape(18.dp)
                            else -> RoundedCornerShape(16.dp)
                        }
                        val searchColors = when (theme) {
                            is ShadeTheme.Cyberpunk -> OutlinedTextFieldDefaults.colors(
                                focusedContainerColor = Color(0xFF060914),
                                unfocusedContainerColor = Color(0xFF04060E),
                                focusedBorderColor = Color(0xFF00F0FF),
                                unfocusedBorderColor = Color(0xFF00F0FF).copy(alpha = 0.35f),
                                focusedTextColor = Color(0xFF00F0FF),
                                unfocusedTextColor = Color.White,
                            )
                            is ShadeTheme.Nothing -> OutlinedTextFieldDefaults.colors(
                                focusedContainerColor = Color(0xFF14161B),
                                unfocusedContainerColor = Color(0xFF0E1014),
                                focusedBorderColor = Color.White,
                                unfocusedBorderColor = Color.White.copy(alpha = 0.25f),
                                focusedTextColor = Color.White,
                                unfocusedTextColor = Color.White.copy(alpha = 0.85f),
                            )
                            else -> OutlinedTextFieldDefaults.colors(
                                focusedContainerColor = MaterialTheme.colorScheme.surfaceContainerHighest.copy(alpha = 0.45f),
                                unfocusedContainerColor = MaterialTheme.colorScheme.surfaceContainerHighest.copy(alpha = 0.35f),
                                focusedBorderColor = MaterialTheme.colorScheme.primary.copy(alpha = 0.50f),
                                unfocusedBorderColor = MaterialTheme.colorScheme.outlineVariant.copy(alpha = 0.30f),
                            )
                        }
                        val searchPlaceholder = when (theme) {
                            is ShadeTheme.Cyberpunk -> "[SEARCH_LOGS...]"
                            is ShadeTheme.Nothing -> "SEARCH..."
                            else -> "Search dismissed alerts..."
                        }

                        OutlinedTextField(
                            value = searchQuery,
                            onValueChange = { searchQuery = it },
                            placeholder = {
                                Text(
                                    text = searchPlaceholder,
                                    style = MaterialTheme.typography.bodyMedium.copy(
                                        fontFamily = if (theme is ShadeTheme.Cyberpunk) FontFamily.Monospace else FontFamily.Default,
                                        letterSpacing = if (theme is ShadeTheme.Nothing) 0.6.sp else 0.sp,
                                    ),
                                )
                            },
                            leadingIcon = {
                                Icon(
                                    imageVector = Icons.Default.Search,
                                    contentDescription = null,
                                    tint = if (theme is ShadeTheme.Cyberpunk) Color(0xFF00F0FF).copy(alpha = 0.8f) else MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.7f),
                                    modifier = Modifier.size(18.dp),
                                )
                            },
                            singleLine = true,
                            shape = searchShape,
                            colors = searchColors,
                            modifier = Modifier
                                .fillMaxWidth()
                                .padding(horizontal = 18.dp, vertical = 6.dp),
                        )
                    }

                    // Category filter chips
                    val categoriesInHistory = remember(history) {
                        history.map { it.category }.distinct().filter { it != ShadeCategory.All }
                    }
                    if (categoriesInHistory.size > 1) {
                        Row(
                            modifier = Modifier
                                .fillMaxWidth()
                                .horizontalScroll(rememberScrollState())
                                .padding(horizontal = 18.dp, vertical = 4.dp),
                            horizontalArrangement = Arrangement.spacedBy(6.dp),
                        ) {
                            HistoryFilterChip(
                                label = "All",
                                isSelected = selectedCategoryFilter == null,
                                theme = theme,
                                onClick = {
                                    haptics.lightTap()
                                    selectedCategoryFilter = null
                                },
                            )
                            categoriesInHistory.forEach { cat ->
                                HistoryFilterChip(
                                    label = cat.label,
                                    isSelected = selectedCategoryFilter == cat,
                                    theme = theme,
                                    onClick = {
                                        haptics.lightTap()
                                        selectedCategoryFilter = if (selectedCategoryFilter == cat) null else cat
                                    },
                                )
                            }
                        }
                    }

                    HorizontalDivider(
                        modifier = Modifier.padding(top = 6.dp),
                        color = when (theme) {
                            is ShadeTheme.Cyberpunk -> Color(0xFF00F0FF).copy(alpha = 0.18f)
                            is ShadeTheme.Nothing -> Color.White.copy(alpha = 0.10f)
                            else -> MaterialTheme.colorScheme.outlineVariant.copy(alpha = 0.25f)
                        },
                    )

                    val filteredHistory = remember(history, searchQuery, selectedCategoryFilter) {
                        history.filter { record ->
                            val matchesCategory = selectedCategoryFilter == null || record.category == selectedCategoryFilter
                            val matchesQuery = searchQuery.isBlank() ||
                                record.title.contains(searchQuery, ignoreCase = true) ||
                                record.text.contains(searchQuery, ignoreCase = true) ||
                                record.packageName.contains(searchQuery, ignoreCase = true)
                            matchesCategory && matchesQuery
                        }
                    }

                    if (filteredHistory.isEmpty()) {
                        Column(
                            modifier = Modifier
                                .fillMaxWidth()
                                .padding(vertical = 48.dp, horizontal = 24.dp),
                            horizontalAlignment = Alignment.CenterHorizontally,
                            verticalArrangement = Arrangement.spacedBy(10.dp),
                        ) {
                            val emptyIconShape = when (theme) {
                                is ShadeTheme.Cyberpunk -> ChamferedCornerShape(8.dp)
                                is ShadeTheme.Nothing -> RoundedCornerShape(10.dp)
                                else -> CircleShape
                            }
                            val emptyIconBg = when (theme) {
                                is ShadeTheme.Cyberpunk -> Color(0xFF070B14)
                                is ShadeTheme.Nothing -> Color(0xFF14161B)
                                else -> MaterialTheme.colorScheme.surfaceContainerHighest.copy(alpha = 0.5f)
                            }
                            val emptyIconBorder = when (theme) {
                                is ShadeTheme.Cyberpunk -> BorderStroke(1.dp, Color(0xFF00F0FF).copy(alpha = 0.35f))
                                is ShadeTheme.Nothing -> BorderStroke(1.dp, Color.White.copy(alpha = 0.15f))
                                else -> null
                            }
                            Box(
                                modifier = Modifier
                                    .size(56.dp)
                                    .clip(emptyIconShape)
                                    .then(if (emptyIconBorder != null) Modifier.border(emptyIconBorder, emptyIconShape) else Modifier)
                                    .background(emptyIconBg),
                                contentAlignment = Alignment.Center,
                            ) {
                                Icon(
                                    imageVector = Icons.Default.HistoryToggleOff,
                                    contentDescription = null,
                                    tint = if (theme is ShadeTheme.Cyberpunk) Color(0xFF00F0FF).copy(alpha = 0.7f) else MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.6f),
                                    modifier = Modifier.size(28.dp),
                                )
                            }
                            val emptyTitle = when {
                                searchQuery.isNotBlank() -> if (theme is ShadeTheme.Cyberpunk) "[NO_MATCHING_TELEMETRY]" else "No matching notifications"
                                theme is ShadeTheme.Cyberpunk -> "[NO_ARCHIVED_PACKETS]"
                                theme is ShadeTheme.Nothing -> "NO DISMISSED NOTIFICATIONS"
                                else -> "No dismissed notifications"
                            }
                            Text(
                                text = emptyTitle,
                                style = MaterialTheme.typography.titleSmall.copy(
                                    fontWeight = FontWeight.SemiBold,
                                    fontFamily = if (theme is ShadeTheme.Cyberpunk) FontFamily.Monospace else FontFamily.Default,
                                    letterSpacing = if (theme is ShadeTheme.Nothing) 0.6.sp else 0.sp,
                                ),
                                color = if (theme is ShadeTheme.Cyberpunk) Color(0xFF00F0FF) else MaterialTheme.colorScheme.onSurface,
                            )
                            Text(
                                text = if (searchQuery.isNotBlank())
                                    "Try searching for another keyword or clear the filter."
                                else
                                    "Notifications dismissed while SuperShade is active will appear here.",
                                style = MaterialTheme.typography.bodySmall.copy(
                                    fontFamily = if (theme is ShadeTheme.Cyberpunk) FontFamily.Monospace else FontFamily.Default,
                                ),
                                color = if (theme is ShadeTheme.Cyberpunk) Color(0xFF00F0FF).copy(alpha = 0.6f) else MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.7f),
                                textAlign = androidx.compose.ui.text.style.TextAlign.Center,
                            )
                        }
                    } else {
                        LazyColumn(
                            modifier = Modifier
                                .fillMaxWidth()
                                .weight(1f, fill = false),
                            contentPadding = PaddingValues(horizontal = 16.dp, vertical = 8.dp),
                            verticalArrangement = Arrangement.spacedBy(8.dp),
                        ) {
                            items(
                                items = filteredHistory,
                                key = { "${it.key}_${it.dismissedTime}" },
                            ) { item ->
                                DismissedNotificationCard(
                                    record = item,
                                    theme = theme,
                                    onClick = {
                                        haptics.lightTap()
                                        try {
                                            val intent = Intent(Settings.ACTION_APP_NOTIFICATION_SETTINGS).apply {
                                                putExtra(Settings.EXTRA_APP_PACKAGE, item.packageName)
                                                addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
                                            }
                                            context.startActivity(intent)
                                        } catch (_: Exception) {}
                                    },
                                )
                            }
                        }
                    }

                    // Footer button to open Android's OS notification history
                    val footerShape = when (theme) {
                        is ShadeTheme.Cyberpunk -> ChamferedCornerShape(6.dp)
                        is ShadeTheme.Nothing -> RoundedCornerShape(8.dp)
                        is ShadeTheme.OneUI -> RoundedCornerShape(16.dp)
                        else -> RoundedCornerShape(16.dp)
                    }
                    val footerBorder = when (theme) {
                        is ShadeTheme.Cyberpunk -> BorderStroke(1.dp, Color(0xFF00F0FF).copy(alpha = 0.35f))
                        is ShadeTheme.Nothing -> BorderStroke(1.dp, Color.White.copy(alpha = 0.20f))
                        else -> getCardBorder(alpha = 0.25f)
                    }
                    val footerBg = when (theme) {
                        is ShadeTheme.Cyberpunk -> Color(0xFF060914)
                        is ShadeTheme.Nothing -> Color(0xFF14161B)
                        else -> MaterialTheme.colorScheme.surfaceContainerHighest.copy(alpha = 0.55f)
                    }

                    Surface(
                        onClick = {
                            haptics.sheetDetent()
                            try {
                                val intent = Intent("android.settings.NOTIFICATION_HISTORY").apply {
                                    addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
                                }
                                context.startActivity(intent)
                            } catch (_: Exception) {
                                try {
                                    val intent = Intent(Settings.ACTION_NOTIFICATION_LISTENER_SETTINGS).apply {
                                        addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
                                    }
                                    context.startActivity(intent)
                                } catch (_: Exception) {}
                            }
                        },
                        shape = footerShape,
                        color = footerBg,
                        border = footerBorder,
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(horizontal = 18.dp, vertical = 10.dp),
                    ) {
                        Row(
                            modifier = Modifier.padding(horizontal = 16.dp, vertical = 10.dp),
                            verticalAlignment = Alignment.CenterVertically,
                            horizontalArrangement = Arrangement.SpaceBetween,
                        ) {
                            Row(
                                verticalAlignment = Alignment.CenterVertically,
                                horizontalArrangement = Arrangement.spacedBy(8.dp),
                            ) {
                                Icon(
                                    imageVector = Icons.Default.History,
                                    contentDescription = null,
                                    tint = if (theme is ShadeTheme.Cyberpunk) Color(0xFF00F0FF) else MaterialTheme.colorScheme.primary,
                                    modifier = Modifier.size(16.dp),
                                )
                                val footerLabel = when (theme) {
                                    is ShadeTheme.Cyberpunk -> "[OPEN_OS_HISTORY]"
                                    is ShadeTheme.Nothing -> "ANDROID NOTIFICATION HISTORY"
                                    else -> "Android System Notification History"
                                }
                                Text(
                                    text = footerLabel,
                                    style = MaterialTheme.typography.labelMedium.copy(
                                        fontWeight = FontWeight.SemiBold,
                                        fontFamily = if (theme is ShadeTheme.Cyberpunk) FontFamily.Monospace else FontFamily.Default,
                                        letterSpacing = if (theme is ShadeTheme.Nothing) 0.6.sp else 0.sp,
                                    ),
                                    color = if (theme is ShadeTheme.Cyberpunk) Color(0xFF00F0FF) else MaterialTheme.colorScheme.onSurface,
                                )
                            }
                            Icon(
                                imageVector = Icons.AutoMirrored.Filled.OpenInNew,
                                contentDescription = null,
                                tint = if (theme is ShadeTheme.Cyberpunk) Color(0xFF00F0FF).copy(alpha = 0.7f) else MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.7f),
                                modifier = Modifier.size(16.dp),
                            )
                        }
                    }
                }
            }
        }
    }
}

@Composable
private fun HistoryFilterChip(
    label: String,
    isSelected: Boolean,
    theme: ShadeTheme,
    onClick: () -> Unit,
    modifier: Modifier = Modifier,
) {
    val chipShape = when (theme) {
        is ShadeTheme.Cyberpunk -> ChamferedCornerShape(4.dp)
        is ShadeTheme.Nothing -> RoundedCornerShape(6.dp)
        else -> RoundedCornerShape(50)
    }
    val chipBorder = when {
        theme is ShadeTheme.Cyberpunk -> BorderStroke(1.dp, if (isSelected) Color(0xFF00F0FF) else Color(0xFF00F0FF).copy(alpha = 0.25f))
        theme is ShadeTheme.Nothing -> if (isSelected) null else BorderStroke(1.dp, Color.White.copy(alpha = 0.15f))
        else -> null
    }
    val chipBg = when {
        theme is ShadeTheme.Cyberpunk -> if (isSelected) Color(0xFF00F0FF).copy(alpha = 0.20f) else Color(0xFF050B14)
        theme is ShadeTheme.Nothing -> if (isSelected) Color(0xFFD71920) else Color(0xFF14161B)
        isSelected -> MaterialTheme.colorScheme.primary
        else -> MaterialTheme.colorScheme.surfaceContainerHighest.copy(alpha = 0.55f)
    }
    val chipTextColor = when {
        theme is ShadeTheme.Cyberpunk -> if (isSelected) Color(0xFF00F0FF) else Color(0xFF00F0FF).copy(alpha = 0.65f)
        theme is ShadeTheme.Nothing -> if (isSelected) Color.White else Color.White.copy(alpha = 0.70f)
        isSelected -> MaterialTheme.colorScheme.onPrimary
        else -> MaterialTheme.colorScheme.onSurfaceVariant
    }

    Surface(
        onClick = onClick,
        shape = chipShape,
        color = chipBg,
        border = chipBorder,
        modifier = modifier,
    ) {
        Text(
            text = if (theme is ShadeTheme.Nothing) label.uppercase() else label,
            style = MaterialTheme.typography.labelSmall.copy(
                fontWeight = FontWeight.SemiBold,
                fontFamily = if (theme is ShadeTheme.Cyberpunk) FontFamily.Monospace else FontFamily.Default,
                letterSpacing = if (theme is ShadeTheme.Nothing) 0.8.sp else 0.sp,
            ),
            color = chipTextColor,
            modifier = Modifier.padding(horizontal = 12.dp, vertical = 6.dp),
        )
    }
}

@Composable
private fun DismissedNotificationCard(
    record: DismissedNotificationRecord,
    theme: ShadeTheme,
    onClick: () -> Unit,
    modifier: Modifier = Modifier,
) {
    val context = LocalContext.current
    val shapes = LocalShadeShapeScheme.current

    val appName = remember(record.packageName) {
        try {
            val pm = context.packageManager
            val info = pm.getApplicationInfo(record.packageName, 0)
            pm.getApplicationLabel(info).toString()
        } catch (_: Exception) {
            record.packageName.substringAfterLast('.').replaceFirstChar { it.uppercase() }
        }
    }

    val appIconBitmap by produceState<ImageBitmap?>(null, record.packageName) {
        value = withContext(Dispatchers.IO) {
            try {
                context.packageManager.getApplicationIcon(record.packageName)
                    .toBitmap(40, 40, android.graphics.Bitmap.Config.ARGB_8888)
                    .asImageBitmap()
            } catch (_: Exception) { null }
        }
    }

    val displayTitle = when {
        record.title.isNotBlank() -> record.title
        record.text.isNotBlank() -> appName
        else -> appName
    }

    val dismissedTimeLabel = remember(record.dismissedTime) {
        val diffMs = System.currentTimeMillis() - record.dismissedTime
        when {
            diffMs < 60_000L -> "Just now"
            diffMs < 3_600_000L -> "${diffMs / 60_000L}m ago"
            diffMs < 86_400_000L -> "${diffMs / 3_600_000L}h ago"
            else -> "${diffMs / 86_400_000L}d ago"
        }
    }

    val cardShape = when (theme) {
        is ShadeTheme.Cyberpunk -> ChamferedCornerShape(8.dp)
        is ShadeTheme.Nothing -> RoundedCornerShape(10.dp)
        is ShadeTheme.OneUI -> RoundedCornerShape(18.dp)
        else -> shapes.card
    }
    val cardBorder = when (theme) {
        is ShadeTheme.Cyberpunk -> BorderStroke(1.dp, Color(0xFF00F0FF).copy(alpha = 0.30f))
        is ShadeTheme.Nothing -> BorderStroke(1.dp, Color.White.copy(alpha = 0.15f))
        is ShadeTheme.OneUI -> BorderStroke(1.dp, Color.White.copy(alpha = 0.08f))
        else -> getCardBorder(alpha = 0.22f)
    }
    val cardBg = when (theme) {
        is ShadeTheme.Cyberpunk -> Color(0xFF060914)
        is ShadeTheme.Nothing -> Color(0xFF090A0D)
        else -> MaterialTheme.colorScheme.surfaceContainerHighest.copy(alpha = 0.45f)
    }

    Card(
        shape = cardShape,
        colors = CardDefaults.cardColors(containerColor = cardBg),
        border = cardBorder,
        modifier = modifier
            .fillMaxWidth()
            .clickable(onClick = onClick),
    ) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(12.dp),
            horizontalArrangement = Arrangement.spacedBy(10.dp),
            verticalAlignment = Alignment.Top,
        ) {
            val iconBoxShape = when (theme) {
                is ShadeTheme.Cyberpunk -> ChamferedCornerShape(4.dp)
                is ShadeTheme.Nothing -> RoundedCornerShape(6.dp)
                else -> RoundedCornerShape(10.dp)
            }
            val iconBoxBorder = when (theme) {
                is ShadeTheme.Cyberpunk -> BorderStroke(1.dp, Color(0xFF00F0FF).copy(alpha = 0.35f))
                is ShadeTheme.Nothing -> BorderStroke(1.dp, Color.White.copy(alpha = 0.12f))
                else -> null
            }
            val iconBoxBg = when (theme) {
                is ShadeTheme.Cyberpunk -> Color(0xFF04060E)
                is ShadeTheme.Nothing -> Color(0xFF14161B)
                else -> Color.Transparent
            }

            Box(
                modifier = Modifier
                    .size(34.dp)
                    .clip(iconBoxShape)
                    .then(if (iconBoxBorder != null) Modifier.border(iconBoxBorder, iconBoxShape) else Modifier)
                    .background(iconBoxBg),
                contentAlignment = Alignment.Center,
            ) {
                if (appIconBitmap != null) {
                    Image(
                        bitmap = appIconBitmap!!,
                        contentDescription = null,
                        modifier = Modifier.size(30.dp),
                    )
                } else {
                    Box(
                        modifier = Modifier
                            .fillMaxSize()
                            .background(MaterialTheme.colorScheme.primaryContainer),
                        contentAlignment = Alignment.Center,
                    ) {
                        Text(
                            text = appName.take(1).uppercase(),
                            style = MaterialTheme.typography.titleSmall.copy(fontWeight = FontWeight.Bold),
                            color = MaterialTheme.colorScheme.onPrimaryContainer,
                        )
                    }
                }
            }

            Column(modifier = Modifier.weight(1f)) {
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.SpaceBetween,
                ) {
                    Text(
                        text = if (theme is ShadeTheme.Nothing) appName.uppercase() else appName,
                        style = MaterialTheme.typography.labelSmall.copy(
                            fontWeight = FontWeight.SemiBold,
                            fontFamily = if (theme is ShadeTheme.Cyberpunk) FontFamily.Monospace else FontFamily.Default,
                            letterSpacing = if (theme is ShadeTheme.Nothing) 0.5.sp else 0.sp,
                        ),
                        color = if (theme is ShadeTheme.Cyberpunk) Color(0xFF00F0FF) else MaterialTheme.colorScheme.onSurfaceVariant,
                        maxLines = 1,
                        overflow = TextOverflow.Ellipsis,
                        modifier = Modifier.weight(1f, fill = false),
                    )
                    Text(
                        text = dismissedTimeLabel,
                        style = MaterialTheme.typography.labelSmall.copy(
                            fontSize = 10.5.sp,
                            fontFamily = if (theme is ShadeTheme.Cyberpunk) FontFamily.Monospace else FontFamily.Default,
                        ),
                        color = if (theme is ShadeTheme.Cyberpunk) Color(0xFFFF007F).copy(alpha = 0.7f) else MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.65f),
                    )
                }

                Spacer(modifier = Modifier.height(2.dp))

                Text(
                    text = displayTitle,
                    style = MaterialTheme.typography.bodyMedium.copy(
                        fontWeight = FontWeight.SemiBold,
                        fontFamily = if (theme is ShadeTheme.Cyberpunk) FontFamily.Monospace else FontFamily.Default,
                    ),
                    color = MaterialTheme.colorScheme.onSurface,
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis,
                )

                if (record.text.isNotBlank()) {
                    Spacer(modifier = Modifier.height(2.dp))
                    Text(
                        text = record.text,
                        style = MaterialTheme.typography.bodySmall.copy(
                            fontFamily = if (theme is ShadeTheme.Cyberpunk) FontFamily.Monospace else FontFamily.Default,
                        ),
                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                        maxLines = 2,
                        overflow = TextOverflow.Ellipsis,
                    )
                }
            }
        }
    }
}
