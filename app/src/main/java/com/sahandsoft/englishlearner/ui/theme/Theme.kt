package com.sahandsoft.englishlearner.ui.theme

import androidx.compose.foundation.isSystemInDarkTheme
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Typography
import androidx.compose.material3.darkColorScheme
import androidx.compose.material3.lightColorScheme
import androidx.compose.runtime.Composable
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
    primary = ChildLightPrimary,
    onPrimary = Color_White,
    primaryContainer = ChildLightPrimaryContainer,
    onPrimaryContainer = Color_00332F,
    secondary = ChildLightSecondary,
    onSecondary = Color_White,
    secondaryContainer = ChildLightSecondaryContainer,
    onSecondaryContainer = Color_3A2200,
    background = ChildLightBackground,
    onBackground = Color_201A14,
    surface = ChildLightSurface,
    onSurface = Color_201A14,
    surfaceVariant = ChildLightSurfaceVariant,
    onSurfaceVariant = ChildLightOnSurfaceVariant
)

private val ChildDarkColors = darkColorScheme(
    primary = ChildDarkPrimary,
    onPrimary = Color_00382F,
    primaryContainer = ChildDarkPrimaryContainer,
    onPrimaryContainer = Color_A7F3EB,
    secondary = ChildDarkSecondary,
    onSecondary = Color_3A2200,
    secondaryContainer = ChildDarkSecondaryContainer,
    onSecondaryContainer = Color_FFDBB8,
    background = ChildDarkBackground,
    onBackground = Color_EFE6DA,
    surface = ChildDarkSurface,
    onSurface = Color_EFE6DA,
    surfaceVariant = ChildDarkSurfaceVariant,
    onSurfaceVariant = ChildDarkOnSurfaceVariant
)

private val Color_White = androidx.compose.ui.graphics.Color(0xFFFFFFFF)
private val Color_00332F = androidx.compose.ui.graphics.Color(0xFF00332F)
private val Color_3A2200 = androidx.compose.ui.graphics.Color(0xFF3A2200)
private val Color_201A14 = androidx.compose.ui.graphics.Color(0xFF201A14)
private val Color_00382F = androidx.compose.ui.graphics.Color(0xFF00382F)
private val Color_A7F3EB = androidx.compose.ui.graphics.Color(0xFFA7F3EB)
private val Color_FFDBB8 = androidx.compose.ui.graphics.Color(0xFFFFDBB8)
private val Color_EFE6DA = androidx.compose.ui.graphics.Color(0xFFEFE6DA)

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
