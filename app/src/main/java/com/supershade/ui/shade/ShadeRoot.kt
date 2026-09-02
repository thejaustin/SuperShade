package com.supershade.ui.shade

import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.core.tween
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.animation.slideInVertically
import androidx.compose.animation.slideOutVertically
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxHeight
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.MaterialTheme
import androidx.compose.runtime.Composable
import androidx.compose.runtime.derivedStateOf
import androidx.compose.runtime.getValue
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.unit.dp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.supershade.domain.notification.model.ShadeCategory
import com.supershade.ui.theme.OneUiShadeTheme
import com.supershade.ui.theme.PixelShadeTheme
import com.supershade.ui.theme.ShadeTheme
import com.supershade.viewmodel.ShadeViewModel

@Composable
fun ShadeRoot(
    viewModel: ShadeViewModel,
    onDismiss: () -> Unit,
) {
    val state by viewModel.state.collectAsStateWithLifecycle()

    val categoryCounts by remember {
        derivedStateOf {
            val all = state.allNotifications
            val counts = mutableMapOf<ShadeCategory, Int>()
            all.forEach { n -> counts[n.category] = (counts[n.category] ?: 0) + 1 }
            counts[ShadeCategory.All] = all.size
            counts as Map<ShadeCategory, Int>
        }
    }

    val themeWrapper: @Composable (@Composable () -> Unit) -> Unit = when (state.theme) {
        ShadeTheme.Pixel -> { content -> PixelShadeTheme(content) }
        else             -> { content -> OneUiShadeTheme(content) }
    }

    themeWrapper {
        Box(modifier = Modifier.fillMaxSize()) {
            // Dimmer scrim — tapping it dismisses the shade
            Box(
                modifier = Modifier
                    .fillMaxSize()
                    .background(Color.Black.copy(alpha = 0.55f))
                    .clickable(onClick = onDismiss),
            )

            // Shade panel: slides down from the top, rounded bottom corners
            AnimatedVisibility(
                visible = state.isOpen,
                enter = slideInVertically(tween(300)) { -it } + fadeIn(tween(200)),
                exit  = slideOutVertically(tween(250)) { -it } + fadeOut(tween(200)),
            ) {
                Column(
                    modifier = Modifier
                        .fillMaxWidth()
                        .fillMaxHeight(0.72f)
                        .clip(RoundedCornerShape(bottomStart = 28.dp, bottomEnd = 28.dp))
                        .background(MaterialTheme.colorScheme.surface),
                ) {
                    StatusBarRow(statusBar = state.statusBar)
                    QuickSettingsGrid(
                        tiles = state.tiles,
                        theme = state.theme,
                        onTileClick = { viewModel.toggleTile(it) },
                    )
                    BrightnessSlider(
                        brightness = state.brightness,
                        onBrightnessChange = { viewModel.setBrightness(it) },
                    )
                    state.media?.let { media ->
                        MediaCard(
                            media = media,
                            onPlayPause = { viewModel.mediaPlayPause() },
                            onSkipNext = { viewModel.mediaSkipNext() },
                            onSkipPrevious = { viewModel.mediaSkipPrevious() },
                        )
                    }
                    CategoryBar(
                        categories = ShadeCategory.entries,
                        selected = state.selectedCategory,
                        onSelect = { viewModel.selectCategory(it) },
                        counts = categoryCounts,
                    )
                    NotificationFeed(
                        notifications = state.visibleNotifications,
                        onDismiss = { viewModel.dismissNotification(it) },
                        onClearAll = { viewModel.clearAllNotifications() },
                        onNotificationClick = { notification ->
                            viewModel.launchNotification(notification)
                            onDismiss()
                        },
                        modifier = Modifier.weight(1f),
                    )

                    // Drag-handle pill at the bottom edge of the panel
                    Box(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(vertical = 12.dp),
                        contentAlignment = Alignment.Center,
                    ) {
                        Box(
                            modifier = Modifier
                                .width(36.dp)
                                .height(4.dp)
                                .clip(CircleShape)
                                .background(Color.White.copy(alpha = 0.25f)),
                        )
                    }
                }
            }
        }
    }
}
