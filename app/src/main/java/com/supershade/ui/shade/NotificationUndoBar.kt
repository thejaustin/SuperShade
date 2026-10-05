package com.supershade.ui.shade

import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.animation.slideInVertically
import androidx.compose.animation.slideOutVertically
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.Undo
import androidx.compose.material.icons.filled.Close
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.supershade.domain.notification.model.ShadeNotification
import com.supershade.haptics.LocalSuperHaptics
import com.supershade.ui.theme.ChamferedCornerShape
import com.supershade.ui.theme.LocalShadeTheme
import com.supershade.ui.theme.ShadeTheme
import kotlinx.coroutines.delay

@Composable
fun NotificationUndoBar(
    lastDismissed: ShadeNotification?,
    onUndo: () -> Unit,
    onClearUndo: () -> Unit,
    modifier: Modifier = Modifier,
) {
    val context = LocalContext.current
    val haptics = LocalSuperHaptics.current ?: remember(context) { com.supershade.haptics.SuperHaptics(context) }
    val theme = LocalShadeTheme.current

    LaunchedEffect(lastDismissed) {
        if (lastDismissed != null) {
            delay(3500L)
            onClearUndo()
        }
    }

    AnimatedVisibility(
        visible = lastDismissed != null,
        enter = slideInVertically(initialOffsetY = { it }) + fadeIn(),
        exit = slideOutVertically(targetOffsetY = { it }) + fadeOut(),
        modifier = modifier,
    ) {
        if (lastDismissed != null) {
            val appName = remember(lastDismissed.packageName) {
                try {
                    val info = context.packageManager.getApplicationInfo(lastDismissed.packageName, 0)
                    context.packageManager.getApplicationLabel(info).toString()
                } catch (_: Exception) {
                    lastDismissed.packageName.substringAfterLast('.').replaceFirstChar { it.uppercase() }
                }
            }

            val pillShape = when (theme) {
                is ShadeTheme.Cyberpunk -> ChamferedCornerShape(8.dp)
                is ShadeTheme.Nothing -> RoundedCornerShape(8.dp)
                else -> RoundedCornerShape(50)
            }

            val pillBorder = when (theme) {
                is ShadeTheme.Cyberpunk -> BorderStroke(1.dp, Color(0xFF00F0FF).copy(alpha = 0.60f))
                is ShadeTheme.Nothing -> BorderStroke(1.dp, Color.White.copy(alpha = 0.25f))
                else -> BorderStroke(0.5.dp, MaterialTheme.colorScheme.outlineVariant.copy(alpha = 0.40f))
            }

            val pillBg = when (theme) {
                is ShadeTheme.Cyberpunk -> Color(0xFF090D1A)
                is ShadeTheme.Nothing -> Color(0xFF14171E)
                else -> MaterialTheme.colorScheme.surfaceContainerHighest
            }

            Surface(
                shape = pillShape,
                color = pillBg,
                border = pillBorder,
                shadowElevation = 8.dp,
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(horizontal = 24.dp, vertical = 8.dp),
            ) {
                Row(
                    modifier = Modifier.padding(start = 16.dp, end = 8.dp, top = 8.dp, bottom = 8.dp),
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.SpaceBetween,
                ) {
                    Row(
                        modifier = Modifier.weight(1f, fill = false),
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.spacedBy(8.dp),
                    ) {
                        Text(
                            text = if (theme is ShadeTheme.Cyberpunk) "[DISMISSED // ${appName.uppercase()}]"
                                   else if (theme is ShadeTheme.Nothing) "${appName.uppercase()} DISMISSED"
                                   else "$appName dismissed",
                            style = MaterialTheme.typography.bodyMedium.copy(
                                fontWeight = FontWeight.SemiBold,
                                fontFamily = if (theme is ShadeTheme.Cyberpunk) FontFamily.Monospace else FontFamily.Default,
                                fontSize = 13.sp,
                            ),
                            color = if (theme is ShadeTheme.Cyberpunk) Color(0xFF00F0FF) else MaterialTheme.colorScheme.onSurface,
                            maxLines = 1,
                            overflow = TextOverflow.Ellipsis,
                        )
                    }

                    Row(
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.spacedBy(4.dp),
                    ) {
                        TextButton(
                            onClick = {
                                haptics.lightTap()
                                onUndo()
                            },
                        ) {
                            Icon(
                                imageVector = Icons.AutoMirrored.Filled.Undo,
                                contentDescription = null,
                                tint = when (theme) {
                                    is ShadeTheme.Cyberpunk -> Color(0xFFFF007F)
                                    is ShadeTheme.Nothing -> Color(0xFFD71920)
                                    else -> MaterialTheme.colorScheme.primary
                                },
                                modifier = Modifier.size(16.dp),
                            )
                            Spacer(Modifier.width(4.dp))
                            Text(
                                text = when (theme) {
                                    is ShadeTheme.Cyberpunk -> "RESTORE"
                                    is ShadeTheme.Nothing -> "UNDO"
                                    else -> "Undo"
                                },
                                style = MaterialTheme.typography.labelLarge.copy(
                                    fontWeight = FontWeight.Bold,
                                    fontFamily = if (theme is ShadeTheme.Cyberpunk) FontFamily.Monospace else FontFamily.Default,
                                ),
                                color = when (theme) {
                                    is ShadeTheme.Cyberpunk -> Color(0xFFFF007F)
                                    is ShadeTheme.Nothing -> Color(0xFFD71920)
                                    else -> MaterialTheme.colorScheme.primary
                                },
                            )
                        }

                        IconButton(
                            onClick = {
                                onClearUndo()
                            },
                            modifier = Modifier.size(28.dp),
                        ) {
                            Icon(
                                imageVector = Icons.Default.Close,
                                contentDescription = "Dismiss undo",
                                tint = MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.60f),
                                modifier = Modifier.size(16.dp),
                            )
                        }
                    }
                }
            }
        }
    }
}
