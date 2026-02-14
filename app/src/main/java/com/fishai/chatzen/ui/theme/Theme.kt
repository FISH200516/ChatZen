package com.fishai.chatzen.ui.theme

import android.app.Activity
import android.os.Build
import androidx.compose.foundation.isSystemInDarkTheme
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.ColorScheme
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Shapes
import androidx.compose.material3.darkColorScheme
import androidx.compose.material3.dynamicDarkColorScheme
import androidx.compose.material3.dynamicLightColorScheme
import androidx.compose.material3.lightColorScheme
import androidx.compose.runtime.Composable
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.luminance
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.unit.dp

private val DarkColorScheme = darkColorScheme(
    primary = Purple80,
    secondary = PurpleGrey80,
    tertiary = Pink80
)

private val LightColorScheme = lightColorScheme(
    primary = Purple40,
    secondary = PurpleGrey40,
    tertiary = Pink40,
    background = AppBackground,
    surface = AppBackground,
    surfaceVariant = RoundedBackground,
    onSurfaceVariant = Color.Black,
    secondaryContainer = LightGrayOpaque,
    onSecondaryContainer = Color.Black,
    surfaceContainer = RoundedBackground,
    surfaceContainerHigh = RoundedBackground,
    surfaceContainerHighest = RoundedBackground,
    surfaceContainerLow = RoundedBackground
)

@Composable
fun ChatZenTheme(
    darkTheme: Boolean = isSystemInDarkTheme(),
    themeMode: String = "DYNAMIC", // DYNAMIC, PRESET, CUSTOM
    customThemeColor: Int = -16777216, // Default
    userBubbleColor: Int? = null,
    aiBubbleColor: Int? = null,
    globalCornerRadius: Float = 12f,
    content: @Composable () -> Unit
) {
    val context = LocalContext.current
    val view = androidx.compose.ui.platform.LocalView.current

    if (!view.isInEditMode) {
        androidx.compose.runtime.SideEffect {
            val window = (view.context as Activity).window
            androidx.core.view.WindowCompat.getInsetsController(window, view).apply {
                isAppearanceLightStatusBars = !darkTheme
                isAppearanceLightNavigationBars = !darkTheme
            }
        }
    }
    
    // 1. Determine Base Color Scheme
    var colorScheme = when (themeMode) {
        "DYNAMIC" -> {
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.S) {
                val context = LocalContext.current
                if (darkTheme) dynamicDarkColorScheme(context) else dynamicLightColorScheme(context).copy(
                    background = AppBackground,
                    surface = AppBackground,
                    surfaceVariant = RoundedBackground,
                    secondaryContainer = LightGrayOpaque,
                    onSecondaryContainer = Color.Black,
                    surfaceContainer = RoundedBackground,
                    surfaceContainerHigh = RoundedBackground,
                    surfaceContainerHighest = RoundedBackground,
                    surfaceContainerLow = RoundedBackground
                )
            } else {
                if (darkTheme) DarkColorScheme else LightColorScheme
            }
        }
        "CUSTOM" -> {
            // In Dark Mode, override custom primary color to White/Monochrome for better readability
            val primaryColor = if (darkTheme) Color.White else Color(customThemeColor)
            
            // Ensure high contrast for text on colored backgrounds
            val onPrimary = if (primaryColor.luminance() > 0.5f) Color.Black else Color.White
            val secondaryColor = primaryColor.copy(alpha = 0.8f)
            val onSecondary = if (secondaryColor.luminance() > 0.5f) Color.Black else Color.White
            val tertiaryColor = primaryColor.copy(alpha = 0.6f)
            val onTertiary = if (tertiaryColor.luminance() > 0.5f) Color.Black else Color.White
            
            // Fix app-wide text to Black or White (no tinting)
            val surface = if (darkTheme) Color(0xFF121212) else AppBackground // Darker surface for better OLED look
            val onSurface = if (darkTheme) Color(0xFFE0E0E0) else Color.Black // Slightly off-white text for less eye strain
            val background = if (darkTheme) Color(0xFF121212) else AppBackground
            val onBackground = if (darkTheme) Color(0xFFE0E0E0) else Color.Black
            
            if (darkTheme) {
                darkColorScheme(
                    primary = primaryColor,
                    onPrimary = onPrimary,
                    secondary = secondaryColor,
                    onSecondary = onSecondary,
                    tertiary = tertiaryColor,
                    onTertiary = onTertiary,
                    background = background,
                    onBackground = onBackground,
                    surface = surface,
                    onSurface = onSurface,
                    // Use a slightly lighter dark grey for surfaceVariant (Navigation Bar) to distinguish from background
                    surfaceVariant = Color(0xFF1E1E1E), 
                    onSurfaceVariant = Color(0xFFCCCCCC) // Lighter grey for icons
                )
            } else {
                lightColorScheme(
                    primary = primaryColor,
                    onPrimary = onPrimary,
                    secondary = secondaryColor,
                    onSecondary = onSecondary,
                    tertiary = tertiaryColor,
                    onTertiary = onTertiary,
                    background = background,
                    onBackground = onBackground,
                    surface = surface,
                    onSurface = onSurface,
                    surfaceVariant = RoundedBackground,
                    onSurfaceVariant = Color.Black,
                    secondaryContainer = LightGrayOpaque,
                    onSecondaryContainer = Color.Black,
                    surfaceContainer = RoundedBackground,
                    surfaceContainerHigh = RoundedBackground,
                    surfaceContainerHighest = RoundedBackground,
                    surfaceContainerLow = RoundedBackground
                )
            }
        }
        else -> { // PRESET (Default to Purple for now if PRESET logic isn't complex)
             if (darkTheme) DarkColorScheme else LightColorScheme
        }
    }

    // 2. Apply Independent Bubble Colors Overrides (Always override primaryContainer/secondaryContainer)
    // User Bubble Logic
    val finalUserBubbleColor = if (userBubbleColor != null) {
        Color(userBubbleColor)
    } else {
        // Default: Blue (User Request: 默认为蓝色)
        Color(0xFF2196F3)
    }
    val onUserBubbleColor = if (finalUserBubbleColor.luminance() < 0.5f) Color.White else Color.Black

    // AI Bubble Logic
    val finalAiBubbleColor = if (aiBubbleColor != null) {
        Color(aiBubbleColor)
    } else {
        // Default: surfaceVariant with 0.3 alpha (User Request: 默认为MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.3f))
        // Since we are inside the Theme definition, we can't access MaterialTheme.colorScheme directly as it's being built.
        // We use the 'colorScheme' variable we just defined/modified above.
        colorScheme.surfaceVariant.copy(alpha = 0.3f)
    }
    // For text color on AI bubble, if it's transparent/light, use onSurface or onSurfaceVariant for better readability
    // If user sets a custom color, calculate contrast. If default (transparent), use onSurface.
    val onAiBubbleColor = if (aiBubbleColor != null) {
        if (finalAiBubbleColor.luminance() < 0.5f) Color.White else Color.Black
    } else {
        // For the default transparent style, use onSurface for text
        colorScheme.onSurface
    }

    colorScheme = colorScheme.copy(
        primaryContainer = finalUserBubbleColor,
        onPrimaryContainer = onUserBubbleColor,
        secondaryContainer = finalAiBubbleColor,
        onSecondaryContainer = onAiBubbleColor
    )

    // 3. Generate Dynamic Shapes
    val dynamicShapes = Shapes(
        small = RoundedCornerShape(globalCornerRadius.dp),
        medium = RoundedCornerShape(globalCornerRadius.dp),
        large = RoundedCornerShape(globalCornerRadius.dp),
        extraLarge = RoundedCornerShape(globalCornerRadius.dp)
    )

    MaterialTheme(
        colorScheme = colorScheme,
        typography = Typography,
        shapes = dynamicShapes,
        content = content
    )
}