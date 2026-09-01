package com.supershade.ui.theme

import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Typography
import androidx.compose.material3.darkColorScheme
import androidx.compose.runtime.Composable
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.sp

// Samsung OneUI 8.5 dark shade palette
private val OneUiColors = darkColorScheme(
    background        = Color(0xCC0D0D0D),   // ~80 % opaque near-black
    surface           = Color(0xFF1A1A1A),
    surfaceVariant    = Color(0xFF262626),
    surfaceContainer  = Color(0xFF202020),
    primary           = Color(0xFF1C6FEE),   // Samsung Galaxy Blue
    primaryContainer  = Color(0xFF0F4BA8),
    onPrimary         = Color.White,
    onPrimaryContainer = Color.White,
    secondary         = Color(0xFF9E9E9E),
    onSecondary       = Color.White,
    onBackground      = Color.White,
    onSurface         = Color.White,
    onSurfaceVariant  = Color(0xFFAAAAAA),
    outline           = Color(0xFF3A3A3A),
    error             = Color(0xFFCF6679),
    onError           = Color.White,
)

// Typography tuned to Samsung's visual weight hierarchy
private val OneUiTypography = Typography(
    // Clock — large, light weight
    displayMedium = TextStyle(
        fontWeight = FontWeight.Light,
        fontSize   = 52.sp,
        lineHeight = 56.sp,
        letterSpacing = (-1).sp,
    ),
    // Date sub-line
    bodyMedium = TextStyle(
        fontWeight = FontWeight.Normal,
        fontSize   = 13.sp,
        lineHeight = 18.sp,
        letterSpacing = 0.sp,
    ),
    // Notification title / card heading
    titleSmall = TextStyle(
        fontWeight = FontWeight.SemiBold,
        fontSize   = 13.sp,
        lineHeight = 18.sp,
        letterSpacing = 0.sp,
    ),
    // Notification body text
    bodySmall = TextStyle(
        fontWeight = FontWeight.Normal,
        fontSize   = 12.sp,
        lineHeight = 16.sp,
        letterSpacing = 0.sp,
    ),
    // Tile label / chip label
    labelSmall = TextStyle(
        fontWeight = FontWeight.Normal,
        fontSize   = 10.sp,
        lineHeight = 13.sp,
        letterSpacing = 0.sp,
    ),
    labelMedium = TextStyle(
        fontWeight = FontWeight.Medium,
        fontSize   = 12.sp,
        lineHeight = 16.sp,
        letterSpacing = 0.sp,
    ),
)

@Composable
fun OneUiShadeTheme(content: @Composable () -> Unit) {
    MaterialTheme(
        colorScheme = OneUiColors,
        typography  = OneUiTypography,
        content     = content,
    )
}
