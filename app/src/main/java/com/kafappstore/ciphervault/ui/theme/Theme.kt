package com.kafappstore.ciphervault.ui.theme

import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.darkColorScheme
import androidx.compose.runtime.Composable
import androidx.compose.ui.graphics.Color
import com.kafappstore.ciphervault.data.CyberThemeMode

private val MatrixColorScheme = darkColorScheme(
    primary = MatrixGreenPrimary,
    onPrimary = Color.Black,
    primaryContainer = MatrixGreenDark,
    onPrimaryContainer = MatrixGreenSecondary,
    secondary = CyberCyan,
    onSecondary = Color.Black,
    tertiary = CyberAmber,
    onTertiary = Color.Black,
    background = MatrixDarkBackground,
    onBackground = MatrixTextPrimary,
    surface = MatrixSurface,
    onSurface = MatrixTextPrimary,
    surfaceVariant = MatrixSurfaceVariant,
    onSurfaceVariant = MatrixTextSecondary,
    error = CyberCrimson,
    onError = Color.White
)

private val CyberNeonColorScheme = darkColorScheme(
    primary = CyberCyan,
    onPrimary = Color.Black,
    primaryContainer = Color(0xFF003844),
    onPrimaryContainer = Color(0xFF80F2FF),
    secondary = CyberPurple,
    onSecondary = Color.White,
    tertiary = MatrixGreenPrimary,
    onTertiary = Color.Black,
    background = Color(0xFF080C14),
    onBackground = Color(0xFFD4F0FF),
    surface = Color(0xFF0E1624),
    onSurface = Color(0xFFD4F0FF),
    surfaceVariant = Color(0xFF162338),
    onSurfaceVariant = Color(0xFF82A4C2),
    error = CyberCrimson,
    onError = Color.White
)

private val DeepVoidColorScheme = darkColorScheme(
    primary = MatrixGreenPrimary,
    onPrimary = Color.Black,
    primaryContainer = Color(0xFF111814),
    onPrimaryContainer = MatrixGreenPrimary,
    secondary = MatrixGreenDim,
    onSecondary = Color.Black,
    tertiary = CyberAmber,
    onTertiary = Color.Black,
    background = Color(0xFF000000),
    onBackground = Color(0xFFCCCCCC),
    surface = Color(0xFF080808),
    onSurface = Color(0xFFDCDCDC),
    surfaceVariant = Color(0xFF141414),
    onSurfaceVariant = Color(0xFF888888),
    error = CyberCrimson,
    onError = Color.White
)

@Composable
fun CipherVaultTheme(
    themeMode: CyberThemeMode = CyberThemeMode.MATRIX_GREEN,
    content: @Composable () -> Unit
) {
    val colorScheme = when (themeMode) {
        CyberThemeMode.MATRIX_GREEN -> MatrixColorScheme
        CyberThemeMode.CYBER_NEON -> CyberNeonColorScheme
        CyberThemeMode.DEEP_VOID -> DeepVoidColorScheme
    }

    MaterialTheme(
        colorScheme = colorScheme,
        typography = Typography,
        content = content
    )
}

// Backward compatibility alias for preview/tests
@Composable
fun MyApplicationTheme(
    content: @Composable () -> Unit
) {
    CipherVaultTheme(content = content)
}
