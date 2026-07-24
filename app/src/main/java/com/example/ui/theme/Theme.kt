package com.example.ui.theme

import androidx.compose.foundation.isSystemInDarkTheme
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.darkColorScheme
import androidx.compose.material3.lightColorScheme
import androidx.compose.runtime.Composable
import androidx.compose.ui.graphics.Color

private val NexusLightColorScheme = lightColorScheme(
  primary = NexusDark,
  onPrimary = NexusWhite,
  primaryContainer = Color(0xFFF1F5F9),
  onPrimaryContainer = NexusDark,
  secondary = NexusBlue,
  onSecondary = NexusWhite,
  tertiary = NexusGreen,
  onTertiary = NexusWhite,
  background = NexusBackground,
  onBackground = NexusTextPrimary,
  surface = NexusSurface,
  onSurface = NexusTextPrimary,
  surfaceVariant = Color(0xFFF1F5F9),
  onSurfaceVariant = NexusTextSecondary,
  outline = NexusBorder,
  error = NexusRed,
  onError = NexusWhite
)

private val NexusDarkColorScheme = darkColorScheme(
  primary = Color(0xFFF8FAFC),
  onPrimary = NexusDark,
  primaryContainer = Color(0xFF1E293B),
  onPrimaryContainer = Color(0xFFF8FAFC),
  secondary = NexusBlue,
  onSecondary = NexusWhite,
  tertiary = NexusGreen,
  onTertiary = NexusWhite,
  background = Color(0xFF0F172A),
  onBackground = Color(0xFFF8FAFC),
  surface = Color(0xFF1E293B),
  onSurface = Color(0xFFF8FAFC),
  surfaceVariant = Color(0xFF334155),
  onSurfaceVariant = Color(0xFFCBD5E1),
  outline = Color(0xFF475569),
  error = NexusRed,
  onError = NexusWhite
)

@Composable
fun NexusAdminTheme(
  darkTheme: Boolean = isSystemInDarkTheme(),
  content: @Composable () -> Unit,
) {
  val colorScheme = if (darkTheme) NexusDarkColorScheme else NexusLightColorScheme

  MaterialTheme(
    colorScheme = colorScheme,
    typography = Typography,
    content = content
  )
}
