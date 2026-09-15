package com.supershade.ui.shade

import androidx.compose.animation.core.Spring
import androidx.compose.animation.core.animateDpAsState
import androidx.compose.animation.core.spring
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.lazy.grid.GridCells
import androidx.compose.foundation.lazy.grid.LazyVerticalGrid
import androidx.compose.foundation.lazy.grid.items
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import com.supershade.domain.tile.TileDefinition
import com.supershade.ui.theme.ShadeTheme

/**
 * Quick settings grid supporting compact mode (1 row, 4 primary quick tiles)
 * and expanded mode (up to 3 rows, 12 quick tiles) enclosed in a modern One UI 8 island container.
 */
@Composable
fun QuickSettingsGrid(
    tiles: List<TileDefinition>,
    theme: ShadeTheme,
    isShizukuConnected: Boolean,
    isExpanded: Boolean = false,
    onTileClick: (TileDefinition) -> Unit,
) {
    val displayedTiles = if (isExpanded) tiles.take(12) else tiles.take(4)
    val rowCount = if (isExpanded) ((displayedTiles.size + 3) / 4).coerceAtLeast(1) else 1
    val targetHeight = (rowCount * 72 + (rowCount - 1) * 8 + 20).dp

    val gridHeight by animateDpAsState(
        targetValue = targetHeight,
        animationSpec = spring(
            dampingRatio = Spring.DampingRatioLowBouncy,
            stiffness = Spring.StiffnessMediumLow,
        ),
        label = "qsGridHeight",
    )

    Surface(
        shape = RoundedCornerShape(26.dp),
        color = MaterialTheme.colorScheme.surfaceContainer.copy(alpha = 0.55f),
        border = BorderStroke(
            width = 1.dp,
            color = MaterialTheme.colorScheme.outlineVariant.copy(alpha = 0.30f),
        ),
        modifier = Modifier
            .fillMaxWidth()
            .padding(horizontal = 14.dp, vertical = 4.dp),
    ) {
        LazyVerticalGrid(
            columns = GridCells.Fixed(4),
            modifier = Modifier
                .fillMaxWidth()
                .height(gridHeight)
                .padding(horizontal = 10.dp, vertical = 10.dp),
            horizontalArrangement = Arrangement.spacedBy(8.dp),
            verticalArrangement = Arrangement.spacedBy(8.dp),
            userScrollEnabled = false,
        ) {
            items(displayedTiles, key = { it.id }) { tile ->
                TileCard(
                    tile = tile,
                    theme = theme,
                    isShizukuConnected = isShizukuConnected,
                    onClick = { onTileClick(tile) },
                )
            }
        }
    }
}
