package com.supershade.ui.shade

import androidx.compose.animation.core.Spring
import androidx.compose.animation.core.animateDpAsState
import androidx.compose.animation.core.spring
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.lazy.grid.GridCells
import androidx.compose.foundation.lazy.grid.LazyVerticalGrid
import androidx.compose.foundation.lazy.grid.items
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import com.supershade.domain.tile.TileDefinition
import com.supershade.ui.theme.ShadeTheme

/**
 * Quick settings grid supporting compact mode (1 row, 4 primary quick tiles)
 * and expanded mode (2 rows, 8 quick tiles).
 */
@Composable
fun QuickSettingsGrid(
    tiles: List<TileDefinition>,
    theme: ShadeTheme,
    isShizukuConnected: Boolean,
    isExpanded: Boolean = false,
    onTileClick: (TileDefinition) -> Unit,
) {
    val displayedTiles = if (isExpanded) tiles.take(8) else tiles.take(4)
    val gridHeight by animateDpAsState(
        targetValue = if (isExpanded) 168.dp else 84.dp,
        animationSpec = spring(
            dampingRatio = Spring.DampingRatioLowBouncy,
            stiffness = Spring.StiffnessMediumLow,
        ),
        label = "qsGridHeight",
    )

    LazyVerticalGrid(
        columns = GridCells.Fixed(4),
        modifier = Modifier
            .fillMaxWidth()
            .height(gridHeight)
            .padding(horizontal = 16.dp, vertical = 6.dp),
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
