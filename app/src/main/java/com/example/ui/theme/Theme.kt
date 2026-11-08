package com.example.ui.theme

import androidx.compose.foundation.isSystemInDarkTheme
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.darkColorScheme
import androidx.compose.material3.lightColorScheme
import androidx.compose.runtime.Composable
import androidx.compose.runtime.CompositionLocalProvider
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalLayoutDirection
import androidx.compose.ui.unit.LayoutDirection

private val DarkColorScheme = darkColorScheme(
    primary = AquaCyan,
    onPrimary = DeepNavy,
    primaryContainer = OceanBlue,
    onPrimaryContainer = PureWhite,
    secondary = Turquoise,
    onSecondary = DeepNavy,
    secondaryContainer = DeepNavyCard,
    onSecondaryContainer = IceBlue,
    tertiary = OceanBlue,
    background = DeepNavy,
    onBackground = PureWhite,
    surface = DeepNavySurface,
    onSurface = PureWhite,
    surfaceVariant = DeepNavyCard,
    onSurfaceVariant = IceBlue,
    error = HeatDangerRed,
    onError = PureWhite
)

private val LightColorScheme = lightColorScheme(
    primary = OceanBlue,
    onPrimary = PureWhite,
    primaryContainer = IceBlue,
    onPrimaryContainer = DeepNavy,
    secondary = Turquoise,
    onSecondary = PureWhite,
    secondaryContainer = Color(0xFFD0F4F0),
    onSecondaryContainer = DeepNavy,
    tertiary = AquaCyan,
    background = Color(0xFFF3FAFF),
    onBackground = DeepNavy,
    surface = PureWhite,
    onSurface = DeepNavy,
    surfaceVariant = IceBlue,
    onSurfaceVariant = Color(0xFF1E3A5F),
    error = Color(0xFFD32F2F),
    onError = PureWhite
)

@Composable
fun HydraMindTheme(
    darkTheme: Boolean = isSystemInDarkTheme(),
    isArabic: Boolean = true,
    content: @Composable () -> Unit
) {
    val colorScheme = if (darkTheme) DarkColorScheme else LightColorScheme
    val direction = if (isArabic) LayoutDirection.Rtl else LayoutDirection.Ltr

    CompositionLocalProvider(LocalLayoutDirection provides direction) {
        MaterialTheme(
            colorScheme = colorScheme,
            typography = Typography,
            content = content
        )
    }
}
