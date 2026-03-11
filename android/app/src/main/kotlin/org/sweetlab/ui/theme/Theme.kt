package org.sweetlab.ui.theme

import android.os.Build
import androidx.compose.foundation.isSystemInDarkTheme
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.darkColorScheme
import androidx.compose.material3.dynamicDarkColorScheme
import androidx.compose.material3.dynamicLightColorScheme
import androidx.compose.material3.lightColorScheme
import androidx.compose.runtime.Composable
import androidx.compose.runtime.CompositionLocalProvider
import androidx.compose.runtime.staticCompositionLocalOf
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.LocalLayoutDirection
import androidx.compose.ui.unit.LayoutDirection

// Sweet Lab brand colors — warm confectionery palette
private val SweetPrimary = Color(0xFF8B4513)       // Chocolate brown
private val SweetOnPrimary = Color(0xFFFFFFFF)
private val SweetPrimaryContainer = Color(0xFFFFDCC2)
private val SweetOnPrimaryContainer = Color(0xFF2E1500)

private val SweetSecondary = Color(0xFFD4A574)      // Caramel
private val SweetOnSecondary = Color(0xFF3E2700)
private val SweetSecondaryContainer = Color(0xFFFFDDB3)
private val SweetOnSecondaryContainer = Color(0xFF2A1800)

private val SweetTertiary = Color(0xFFC62828)       // Cherry red accent
private val SweetOnTertiary = Color(0xFFFFFFFF)
private val SweetTertiaryContainer = Color(0xFFFFDAD6)
private val SweetOnTertiaryContainer = Color(0xFF410002)

private val SweetError = Color(0xFFBA1A1A)
private val SweetOnError = Color(0xFFFFFFFF)

private val LightColorScheme = lightColorScheme(
    primary = SweetPrimary,
    onPrimary = SweetOnPrimary,
    primaryContainer = SweetPrimaryContainer,
    onPrimaryContainer = SweetOnPrimaryContainer,
    secondary = SweetSecondary,
    onSecondary = SweetOnSecondary,
    secondaryContainer = SweetSecondaryContainer,
    onSecondaryContainer = SweetOnSecondaryContainer,
    tertiary = SweetTertiary,
    onTertiary = SweetOnTertiary,
    tertiaryContainer = SweetTertiaryContainer,
    onTertiaryContainer = SweetOnTertiaryContainer,
    error = SweetError,
    onError = SweetOnError,
)

private val DarkColorScheme = darkColorScheme(
    primary = SweetPrimaryContainer,
    onPrimary = SweetOnPrimaryContainer,
    primaryContainer = SweetPrimary,
    onPrimaryContainer = SweetOnPrimary,
    secondary = SweetSecondaryContainer,
    onSecondary = SweetOnSecondaryContainer,
    secondaryContainer = SweetSecondary,
    onSecondaryContainer = SweetOnSecondary,
    tertiary = SweetTertiaryContainer,
    onTertiary = SweetOnTertiaryContainer,
    tertiaryContainer = SweetTertiary,
    onTertiaryContainer = SweetOnTertiary,
    error = Color(0xFFFFB4AB),
    onError = Color(0xFF690005),
)

/**
 * Composition local to indicate whether the app is in offline mode.
 * Observed by OfflineIndicator composable across all screens.
 */
val LocalIsOffline = staticCompositionLocalOf { false }

/**
 * Sweet Lab Material 3 theme with Arabic RTL support.
 *
 * Uses dynamic color on Android 12+ when available, otherwise falls back
 * to the Sweet Lab brand palette.
 */
@Composable
fun SweetLabTheme(
    darkTheme: Boolean = isSystemInDarkTheme(),
    dynamicColor: Boolean = false,
    isRtl: Boolean = true, // Arabic RTL by default
    content: @Composable () -> Unit
) {
    val colorScheme = when {
        dynamicColor && Build.VERSION.SDK_INT >= Build.VERSION_CODES.S -> {
            val context = LocalContext.current
            if (darkTheme) dynamicDarkColorScheme(context)
            else dynamicLightColorScheme(context)
        }
        darkTheme -> DarkColorScheme
        else -> LightColorScheme
    }

    val layoutDirection = if (isRtl) LayoutDirection.Rtl else LayoutDirection.Ltr

    CompositionLocalProvider(LocalLayoutDirection provides layoutDirection) {
        MaterialTheme(
            colorScheme = colorScheme,
            content = content
        )
    }
}
