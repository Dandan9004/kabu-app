package com.kabuapp.app

import androidx.compose.runtime.Composable
import androidx.compose.runtime.ReadOnlyComposable
import androidx.compose.runtime.staticCompositionLocalOf
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.Font
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontWeight

// Same colors as the owner's launcher, plus the darker shades the UI review asked for where
// small text or marks sit on a tile (WCAG 4.5:1 for text, 3:1 for marks).
data class Palette(
    val background: Color,
    val ink: Color,
    val sub: Color,
    /** Secondary text on a tile. */
    val subOnTile: Color,
    val tileFill: Color,
    val surface: Color,
    /** Decoration only (tab underline): too light for text. */
    val accent: Color,
    /** Bad news for a holder and the 反対役 card, on the background. */
    val warn: Color,
    /** The same, on a tile. */
    val warnOnTile: Color,
    val divider: Color,
)

val LightPalette = Palette(
    background = Color(0xFFE8DCC4),
    ink = Color(0xFF1E2A44),
    sub = Color(0xFF4A5670),
    subOnTile = Color(0xFF404B65),
    tileFill = Color(0xFFD3C2A1),
    surface = Color(0xFFF1E8D6),
    accent = Color(0xFFD9822B),
    warn = Color(0xFFB5683F),
    warnOnTile = Color(0xFF9A5230),
    divider = Color(0x261E2A44),
)

val DarkPalette = Palette(
    background = Color(0xFF161513),
    ink = Color(0xFFEDE6D6),
    sub = Color(0xFFA39C8F),
    subOnTile = Color(0xFFBFB39B),
    tileFill = Color(0xFF4A4640),
    surface = Color(0xFF2A2723),
    accent = Color(0xFFF2A64A),
    warn = Color(0xFFC97B4F),
    warnOnTile = Color(0xFFE09A6A),
    divider = Color(0x26EDE6D6),
)

val LocalPalette = staticCompositionLocalOf { LightPalette }

val Bg: Color @Composable @ReadOnlyComposable get() = LocalPalette.current.background
val Ink: Color @Composable @ReadOnlyComposable get() = LocalPalette.current.ink
val Sub: Color @Composable @ReadOnlyComposable get() = LocalPalette.current.sub
val SubOnTile: Color @Composable @ReadOnlyComposable get() = LocalPalette.current.subOnTile
val TileFill: Color @Composable @ReadOnlyComposable get() = LocalPalette.current.tileFill
val Surface: Color @Composable @ReadOnlyComposable get() = LocalPalette.current.surface
val Accent: Color @Composable @ReadOnlyComposable get() = LocalPalette.current.accent
val Warn: Color @Composable @ReadOnlyComposable get() = LocalPalette.current.warn
val WarnOnTile: Color @Composable @ReadOnlyComposable get() = LocalPalette.current.warnOnTile
val Divider: Color @Composable @ReadOnlyComposable get() = LocalPalette.current.divider

/** DINish SemiBold, the launcher's numbers font. */
val Din = FontFamily(Font(R.font.dinish_semibold, FontWeight.SemiBold))
