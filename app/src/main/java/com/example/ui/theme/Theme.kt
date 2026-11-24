package com.example.ui.theme

import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.darkColorScheme
import androidx.compose.runtime.Composable

private val StadiumDarkColorScheme =
  darkColorScheme(
    primary = PitchTeal,
    onPrimary = OnPitchTeal,
    primaryContainer = StadiumSurfaceVariant,
    onPrimaryContainer = StadiumText,
    secondary = TrophyGold,
    onSecondary = OnTrophyGold,
    tertiary = PitchMint,
    onTertiary = OnPitchTeal,
    background = StadiumNight,
    onBackground = StadiumText,
    surface = StadiumSurface,
    onSurface = StadiumText,
    surfaceVariant = StadiumSurfaceVariant,
    onSurfaceVariant = StadiumMutedText,
    outline = StadiumOutline,
    error = AlertRed,
    onError = StadiumNight,
  )

@Composable
fun BeACoachTheme(content: @Composable () -> Unit) {
  MaterialTheme(colorScheme = StadiumDarkColorScheme, typography = Typography, content = content)
}
