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
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.material3.MaterialTheme
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.ui.Modifier
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

    val themeWrapper: @Composable (@Composable () -> Unit) -> Unit = when (state.theme) {
        ShadeTheme.Pixel -> { content -> PixelShadeTheme(content) }
        else             -> { content -> OneUiShadeTheme(content) }
    }

    themeWrapper {
        Box(
            modifier = Modifier
                .fillMaxSize()
                .background(MaterialTheme.colorScheme.background),
        ) {
            // Tap outside the shade panel to dismiss
            Box(
                modifier = Modifier
                    .fillMaxSize()
                    .clickable(onClick = onDismiss),
            )

            AnimatedVisibility(
                visible = state.isOpen,
                enter = slideInVertically(tween(300)) { -it } + fadeIn(tween(300)),
                exit  = slideOutVertically(tween(250)) { -it } + fadeOut(tween(250)),
            ) {
                Column(modifier = Modifier.fillMaxWidth()) {
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
                    )
                    NotificationFeed(
                        notifications = state.visibleNotifications,
                        onDismiss = { viewModel.dismissNotification(it) },
                    )
                }
            }
        }
    }
}
