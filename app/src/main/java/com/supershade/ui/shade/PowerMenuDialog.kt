package com.supershade.ui.shade

import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Lock
import androidx.compose.material.icons.filled.MoreHoriz
import androidx.compose.material.icons.filled.PowerSettingsNew
import androidx.compose.material.icons.filled.RestartAlt
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.ui.window.Dialog

/**
 * Modern One UI style floating Quick Power Menu dialog.
 */
@Composable
fun PowerMenuDialog(
    onDismiss: () -> Unit,
    onLockScreen: () -> Unit,
    onRestart: () -> Unit,
    onPowerOff: () -> Unit,
    onSystemPowerDialog: () -> Unit,
) {
    Dialog(onDismissRequest = onDismiss) {
        Surface(
            shape = RoundedCornerShape(28.dp),
            color = MaterialTheme.colorScheme.surfaceContainerHigh,
            tonalElevation = 6.dp,
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = 16.dp),
        ) {
            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(24.dp),
                horizontalAlignment = Alignment.CenterHorizontally,
            ) {
                Text(
                    text = "Power options",
                    style = MaterialTheme.typography.titleMedium.copy(
                        fontWeight = FontWeight.Bold,
                        fontSize = 18.sp,
                    ),
                    color = MaterialTheme.colorScheme.onSurface,
                )
                Spacer(modifier = Modifier.height(6.dp))
                Text(
                    text = "Select a power or security action",
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                )

                Spacer(modifier = Modifier.height(24.dp))

                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceEvenly,
                ) {
                    PowerActionButton(
                        icon = Icons.Default.PowerSettingsNew,
                        label = "Power off",
                        iconColor = Color(0xFFE53935),
                        backgroundColor = Color(0xFFE53935).copy(alpha = 0.16f),
                        onClick = onPowerOff,
                    )
                    PowerActionButton(
                        icon = Icons.Default.RestartAlt,
                        label = "Restart",
                        iconColor = Color(0xFF43A047),
                        backgroundColor = Color(0xFF43A047).copy(alpha = 0.16f),
                        onClick = onRestart,
                    )
                    PowerActionButton(
                        icon = Icons.Default.Lock,
                        label = "Lock",
                        iconColor = Color(0xFF1E88E5),
                        backgroundColor = Color(0xFF1E88E5).copy(alpha = 0.16f),
                        onClick = onLockScreen,
                    )
                    PowerActionButton(
                        icon = Icons.Default.MoreHoriz,
                        label = "System",
                        iconColor = MaterialTheme.colorScheme.onSurface,
                        backgroundColor = MaterialTheme.colorScheme.surfaceVariant,
                        onClick = onSystemPowerDialog,
                    )
                }

                Spacer(modifier = Modifier.height(20.dp))

                TextButton(
                    onClick = onDismiss,
                    modifier = Modifier.align(Alignment.End),
                ) {
                    Text(
                        text = "Cancel",
                        color = MaterialTheme.colorScheme.primary,
                        fontWeight = FontWeight.SemiBold,
                    )
                }
            }
        }
    }
}

@Composable
private fun PowerActionButton(
    icon: ImageVector,
    label: String,
    iconColor: Color,
    backgroundColor: Color,
    onClick: () -> Unit,
) {
    Column(
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.spacedBy(8.dp),
        modifier = Modifier.clickable(onClick = onClick),
    ) {
        Box(
            modifier = Modifier
                .size(56.dp)
                .clip(CircleShape)
                .background(backgroundColor),
            contentAlignment = Alignment.Center,
        ) {
            Icon(
                imageVector = icon,
                contentDescription = label,
                tint = iconColor,
                modifier = Modifier.size(28.dp),
            )
        }
        Text(
            text = label,
            style = MaterialTheme.typography.labelMedium.copy(fontWeight = FontWeight.Medium),
            color = MaterialTheme.colorScheme.onSurface,
        )
    }
}
