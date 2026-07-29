package com.whitelistchecker.ui.theme

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

private val DarkColorScheme = darkColorScheme(
    primary = SignalGreen80,
    onPrimary = Color(0xFF00382A),
    primaryContainer = Color(0xFF00513B),
    onPrimaryContainer = Color(0xFF91F4CE),
    secondary = BlueGrey80,
    onSecondary = Color(0xFF22333B),
    secondaryContainer = Color(0xFF344A54),
    onSecondaryContainer = Color(0xFFD4E6F2),
    tertiary = Amber80,
    onTertiary = Color(0xFF402D00),
    tertiaryContainer = Color(0xFF5C4200),
    onTertiaryContainer = Color(0xFFFFDEA3),
    background = Color(0xFF101413),
    onBackground = Color(0xFFE0E4E1),
    surface = Color(0xFF101413),
    onSurface = Color(0xFFE0E4E1),
    surfaceVariant = Color(0xFF3F4945),
    onSurfaceVariant = Color(0xFFBEC9C4),
    outline = Color(0xFF89938F),
)

private val LightColorScheme = lightColorScheme(
    primary = SignalGreen40,
    onPrimary = Color.White,
    primaryContainer = Color(0xFF91F4CE),
    onPrimaryContainer = Color(0xFF002116),
    secondary = BlueGrey40,
    onSecondary = Color.White,
    secondaryContainer = Color(0xFFD4E6F2),
    onSecondaryContainer = Color(0xFF071F28),
    tertiary = Amber40,
    onTertiary = Color.White,
    tertiaryContainer = Color(0xFFFFDEA3),
    onTertiaryContainer = Color(0xFF261900),
    background = Color(0xFFF7FAF7),
    onBackground = Color(0xFF191C1B),
    surface = Color(0xFFF7FAF7),
    onSurface = Color(0xFF191C1B),
    surfaceVariant = Color(0xFFDCE5E0),
    onSurfaceVariant = Color(0xFF404943),
    outline = Color(0xFF707973),
)

@Composable
fun WhiteListCheckerTheme(
    darkTheme: Boolean = isSystemInDarkTheme(),
    dynamicColor: Boolean = false,
    content: @Composable () -> Unit,
) {
    val colorScheme = when {
        dynamicColor && Build.VERSION.SDK_INT >= Build.VERSION_CODES.S -> {
            val context = LocalContext.current
            if (darkTheme) dynamicDarkColorScheme(context) else dynamicLightColorScheme(context)
        }
        darkTheme -> DarkColorScheme
        else -> LightColorScheme
    }

    MaterialTheme(
        colorScheme = colorScheme,
        typography = Typography,
        content = content,
    )
}
