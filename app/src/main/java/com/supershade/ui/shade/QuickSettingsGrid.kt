package com.supershade.ui.shade

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.lazy.grid.GridCells
import androidx.compose.foundation.lazy.grid.LazyHorizontalGrid
import androidx.compose.foundation.lazy.grid.items
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import com.supershade.domain.tile.TileDefinition
import com.supershade.ui.theme.ShadeTheme

@Composable
fun QuickSettingsGrid(
    tiles: List<TileDefinition>,
    theme: ShadeTheme,
    onTileClick: (TileDefinition) -> Unit
) {
    LazyHorizontalGrid(
        rows = GridCells.Fixed(2),
        modifier = Modifier
            .fillMaxWidth()
            .height(152.dp)
            .padding(horizontal = 16.dp, vertical = 8.dp),
        horizontalArrangement = Arrangement.spacedBy(8.dp),
        verticalArrangement = Arrangement.spacedBy(8.dp)
    ) {
        items(tiles) { tile ->
            TileCard(
                tile = tile,
                theme = theme,
                onClick = { onTileClick(tile) }
            )
        }
    }
}
