package com.supershade.ui.shade

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.lazy.grid.GridCells
import androidx.compose.foundation.lazy.grid.LazyVerticalGrid
import androidx.compose.foundation.lazy.grid.items
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import com.supershade.domain.tile.TileDefinition
import com.supershade.ui.theme.ShadeTheme

// 4-column grid, 2 rows fixed — mirrors the OneUI compact QS panel
// Height: 2 × 72dp (tiles) + 8dp gap + 8dp top + 8dp bottom padding = 168dp
@Composable
fun QuickSettingsGrid(
    tiles: List<TileDefinition>,
    theme: ShadeTheme,
    onTileClick: (TileDefinition) -> Unit,
) {
    LazyVerticalGrid(
        columns = GridCells.Fixed(4),
        modifier = Modifier
            .fillMaxWidth()
            .height(168.dp)
            .padding(horizontal = 16.dp, vertical = 8.dp),
        horizontalArrangement = Arrangement.spacedBy(8.dp),
        verticalArrangement = Arrangement.spacedBy(8.dp),
        userScrollEnabled = false,
    ) {
        items(tiles.take(8)) { tile ->
            TileCard(
                tile = tile,
                theme = theme,
                onClick = { onTileClick(tile) },
            )
        }
    }
}
