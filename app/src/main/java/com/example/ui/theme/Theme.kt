package com.example.ui.theme

import android.os.Build
import androidx.compose.foundation.isSystemInDarkTheme
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.darkColorScheme
import androidx.compose.material3.dynamicDarkColorScheme
import androidx.compose.material3.dynamicLightColorScheme
import androidx.compose.material3.lightColorScheme
import androidx.compose.runtime.Composable
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext

private val ClashDarkColorScheme = darkColorScheme(
    primary = CocGold,
    onPrimary = Color(0xFF281D00),
    primaryContainer = CocGoldContainer,
    onPrimaryContainer = CocGoldLight,

    secondary = CocElixir,
    onSecondary = Color.White,
    secondaryContainer = CocElixirContainer,
    onSecondaryContainer = CocElixirLight,

    tertiary = CocDarkElixir,
    onTertiary = Color(0xFF002A33),
    tertiaryContainer = CocDarkElixirContainer,
    onTertiaryContainer = CocDarkElixirLight,

    background = CocBackgroundDark,
    onBackground = CocOnSurfaceDark,
    surface = CocSurfaceDark,
    onSurface = CocOnSurfaceDark,
    surfaceVariant = CocSurfaceVariantDark,
    onSurfaceVariant = CocOnSurfaceVariantDark,
    outline = CocOutlineDark
)

private val ClashLightColorScheme = lightColorScheme(
    primary = CocGoldDark,
    onPrimary = Color.White,
    primaryContainer = Color(0xFFFFECB3),
    onPrimaryContainer = Color(0xFF3E2D00),

    secondary = CocElixirDark,
    onSecondary = Color.White,
    secondaryContainer = Color(0xFFF3E5F5),
    onSecondaryContainer = Color(0xFF4A148C),

    tertiary = Color(0xFF00838F),
    onTertiary = Color.White,
    tertiaryContainer = Color(0xFFE0F7FA),
    onTertiaryContainer = Color(0xFF004D40),

    background = Color(0xFFF6F7FA),
    onBackground = Color(0xFF191C20),
    surface = Color.White,
    onSurface = Color(0xFF191C20),
    surfaceVariant = Color(0xFFECEFF4),
    onSurfaceVariant = Color(0xFF44474E),
    outline = Color(0xFF74777F)
)

@Composable
fun MyApplicationTheme(
    darkTheme: Boolean = isSystemInDarkTheme(),
    dynamicColor: Boolean = false, // Default to rich Clash theme colors
    content: @Composable () -> Unit
) {
    val colorScheme = when {
        dynamicColor && Build.VERSION.SDK_INT >= Build.VERSION_CODES.S -> {
            val context = LocalContext.current
            if (darkTheme) dynamicDarkColorScheme(context) else dynamicLightColorScheme(context)
        }
        darkTheme -> ClashDarkColorScheme
        else -> ClashDarkColorScheme // Clash style looks best in dark mode
    }

    MaterialTheme(
        colorScheme = colorScheme,
        typography = Typography,
        content = content
    )
}
