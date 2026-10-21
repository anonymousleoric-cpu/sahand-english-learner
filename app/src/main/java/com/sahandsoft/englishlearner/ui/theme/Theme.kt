package com.sahandsoft.englishlearner.ui.theme

import androidx.compose.foundation.isSystemInDarkTheme
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Typography
import androidx.compose.material3.darkColorScheme
import androidx.compose.material3.lightColorScheme
import androidx.compose.runtime.Composable
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.font.Font
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.sp
import com.sahandsoft.englishlearner.R

val AppFontFamily = FontFamily(
    Font(R.font.vazirmatn_regular, FontWeight.Normal),
    Font(R.font.vazirmatn_medium, FontWeight.Medium),
    Font(R.font.vazirmatn_semibold, FontWeight.SemiBold),
    Font(R.font.vazirmatn_bold, FontWeight.Bold)
)

private val LightColors = lightColorScheme(
    primary = LightPrimary,
    onPrimary = LightOnPrimary,
    primaryContainer = LightPrimaryContainer,
    onPrimaryContainer = LightOnPrimaryContainer,
    secondary = LightSecondary,
    onSecondary = LightOnSecondary,
    secondaryContainer = LightSecondaryContainer,
    onSecondaryContainer = LightOnSecondaryContainer,
    tertiary = LightTertiary,
    onTertiary = LightOnTertiary,
    tertiaryContainer = LightTertiaryContainer,
    onTertiaryContainer = LightOnTertiaryContainer,
    error = LightError,
    onError = LightOnError,
    background = LightBackground,
    onBackground = LightOnBackground,
    surface = LightSurface,
    onSurface = LightOnSurface,
    surfaceVariant = LightSurfaceVariant,
    onSurfaceVariant = LightOnSurfaceVariant,
    outline = LightOutline
)

private val DarkColors = darkColorScheme(
    primary = DarkPrimary,
    onPrimary = DarkOnPrimary,
    primaryContainer = DarkPrimaryContainer,
    onPrimaryContainer = DarkOnPrimaryContainer,
    secondary = DarkSecondary,
    onSecondary = DarkOnSecondary,
    secondaryContainer = DarkSecondaryContainer,
    onSecondaryContainer = DarkOnSecondaryContainer,
    tertiary = DarkTertiary,
    onTertiary = DarkOnTertiary,
    tertiaryContainer = DarkTertiaryContainer,
    onTertiaryContainer = DarkOnTertiaryContainer,
    error = DarkError,
    onError = DarkOnError,
    background = DarkBackground,
    onBackground = DarkOnBackground,
    surface = DarkSurface,
    onSurface = DarkOnSurface,
    surfaceVariant = DarkSurfaceVariant,
    onSurfaceVariant = DarkOnSurfaceVariant,
    outline = DarkOutline
)

private val ChildLightColors = lightColorScheme(
    primary = Color(0xFF0D9488),
    onPrimary = Color(0xFFFFFFFF),
    primaryContainer = Color(0xFFA7F3EB),
    onPrimaryContainer = Color(0xFF00332F),
    secondary = Color(0xFFEA7317),
    onSecondary = Color(0xFFFFFFFF),
    secondaryContainer = Color(0xFFFFE0B8),
    onSecondaryContainer = Color(0xFF3A2200),
    background = Color(0xFFFFF7EC),
    onBackground = Color(0xFF201A14),
    surface = Color(0xFFFFFFFF),
    onSurface = Color(0xFF201A14),
    surfaceVariant = Color(0xFFFDF0DC),
    onSurfaceVariant = Color(0xFF5B4A38)
)

private val ChildDarkColors = darkColorScheme(
    primary = Color(0xFF5EEAD4),
    onPrimary = Color(0xFF00382F),
    primaryContainer = Color(0xFF11655C),
    onPrimaryContainer = Color(0xFFA7F3EB),
    secondary = Color(0xFFFFB877),
    onSecondary = Color(0xFF3A2200),
    secondaryContainer = Color(0xFF8A4A0B),
    onSecondaryContainer = Color(0xFFFFDBB8),
    background = Color(0xFF141210),
    onBackground = Color(0xFFEFE6DA),
    surface = Color(0xFF1D1A16),
    onSurface = Color(0xFFEFE6DA),
    surfaceVariant = Color(0xFF2A251E),
    onSurfaceVariant = Color(0xFFD6C6B4)
)

fun sahandTypography(childMode: Boolean): Typography {
    val scale = if (childMode) 1.12f else 1f
    fun ts(base: Int, weight: FontWeight, lineHeight: Int = base + 8): TextStyle = TextStyle(
        fontFamily = AppFontFamily,
        fontWeight = weight,
        fontSize = (base * scale).sp,
        lineHeight = (lineHeight * scale).sp
    )
    return Typography(
        displayLarge = ts(34, FontWeight.Bold, 44),
        displayMedium = ts(30, FontWeight.Bold, 40),
        headlineLarge = ts(28, FontWeight.Bold, 38),
        headlineMedium = ts(24, FontWeight.Bold, 34),
        headlineSmall = ts(21, FontWeight.SemiBold, 30),
        titleLarge = ts(19, FontWeight.Bold, 28),
        titleMedium = ts(16, FontWeight.SemiBold, 24),
        titleSmall = ts(14, FontWeight.SemiBold, 22),
        bodyLarge = ts(15, FontWeight.Normal, 26),
        bodyMedium = ts(13, FontWeight.Normal, 22),
        bodySmall = ts(11, FontWeight.Normal, 18),
        labelLarge = ts(13, FontWeight.Medium, 20),
        labelMedium = ts(11, FontWeight.Medium, 18),
        labelSmall = ts(10, FontWeight.Medium, 16)
    )
}

@Composable
fun SahandTheme(
    themeMode: String = "system",
    childMode: Boolean = false,
    content: @Composable () -> Unit
) {
    val dark = when (themeMode) {
        "dark" -> true
        "light" -> false
        else -> isSystemInDarkTheme()
    }
    val colors = if (childMode) {
        if (dark) ChildDarkColors else ChildLightColors
    } else {
        if (dark) DarkColors else LightColors
    }
    MaterialTheme(
        colorScheme = colors,
        typography = sahandTypography(childMode),
        content = content
    )
}
