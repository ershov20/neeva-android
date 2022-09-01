// Copyright 2022 Neeva Inc. All rights reserved.
// Use of this source code is governed by a BSD-style license that can be
// found in the LICENSE file.

package com.neeva.app.ui.theme

import androidx.annotation.ColorRes
import androidx.compose.ui.graphics.Color
import com.neeva.app.R

// If any colors are changed here, make sure to update colors.xml, as well.

/** Official colors defined from https://www.figma.com/file/axgC8GRRAo588urHgfIba7/Colors */
object ColorPalette {
    object Brand {
        val Blue = Color(0xff415aff)
        val BlueVariant = Color(0xff1c3bc7)
        val Seafoam = Color(0xffd6eee6)
        val Polar = Color(0xffd4f0f5)
        val PolarVariant = Color(0xffade1ea)
        val PistachioCandidate = Color(0xfff6fde3)
        val Pistachio = Color(0xffe4f3db)
        val PistachioVariant = Color(0xffd0ecbf)
        val Offwhite = Color(0xfffffdf5)
        val OffwhiteVariant = Color(0xfff4efd9)
        val Red = Color(0xfff4604d)
        val RedVariant = Color(0xffd12e19)
        val Orange = Color(0xffff8952)
        val OrangeVariant = Color(0xffe66d35)
        val Peach = Color(0xffffc682)
        val PeachVariant = Color(0xfff4a649)
        val Yellow = Color(0xfff5ff93)
        val YellowVariant = Color(0xfff5fe50)
        val Pink = Color(0xfffaabd8)
        val Green = Color(0xff00a773)
        val GreenVariant = Color(0xff028961)
        val Mint = Color(0xffabedcf)
        val MintVariant = Color(0xff78d8ae)
        val Maya = Color(0xff73a8ff)
        val MayaVariant = Color(0xff528ff3)
        val Gold = Color(0xff84805e)
        val GoldVariant = Color(0xff77734f)
        val Charcoal = Color(0xff191919)
        val White = Color(0xffffffff)
        val BlueUICandidate = Color(0xff4069fd)
        val Purple = Color(0xff9d63db)
        val PurpleVariant = Color(0xff622cac)
        val PinkVariant = Color(0xfff985c7)
        val SeafoamVariant = Color(0xffbbe3d5)
    }
}

val md_theme_light_primary = Color(0xFF2c47ef)
val md_theme_light_onPrimary = Color(0xFFffffff)
val md_theme_light_primaryContainer = Color(0xFFdde0ff)
val md_theme_light_onPrimaryContainer = Color(0xFF000965)
val md_theme_light_secondary = Color(0xFF1b5daf)
val md_theme_light_onSecondary = Color(0xFFffffff)
val md_theme_light_secondaryContainer = Color(0xFFd5e3ff)
val md_theme_light_onSecondaryContainer = Color(0xFF001b3f)
val md_theme_light_tertiary = Color(0xFF006874)
val md_theme_light_onTertiary = Color(0xFFffffff)
val md_theme_light_tertiaryContainer = Color(0xFF8ef1ff)
val md_theme_light_onTertiaryContainer = Color(0xFF001f24)
val md_theme_light_error = Color(0xFFba1b1b)
val md_theme_light_errorContainer = Color(0xFFffdad4)
val md_theme_light_onError = Color(0xFFffffff)
val md_theme_light_onErrorContainer = Color(0xFF410001)
val md_theme_light_background = Color(0xFFfbfdfd)
val md_theme_light_onBackground = Color(0xFF191c1d)
val md_theme_light_surface = Color(0xFFfbfdfd)
val md_theme_light_onSurface = Color(0xFF191c1d)
val md_theme_light_surfaceVariant = Color(0xFFe3e1ec)
val md_theme_light_onSurfaceVariant = Color(0xFF46464e)
val md_theme_light_outline = Color(0xFF767680)
val md_theme_light_inverseOnSurface = Color(0xFFeff1f1)
val md_theme_light_inverseSurface = Color(0xFF2d3132)
val md_theme_light_inversePrimary = Color(0xFFbbc3ff)
val md_theme_light_shadow = Color(0xFF000000)

val md_theme_dark_primary = Color(0xFFbbc3ff)
val md_theme_dark_onPrimary = Color(0xFF00159e)
val md_theme_dark_primaryContainer = Color(0xFF0025d8)
val md_theme_dark_onPrimaryContainer = Color(0xFFdde0ff)
val md_theme_dark_secondary = Color(0xFFa8c7ff)
val md_theme_dark_onSecondary = Color(0xFF002f66)
val md_theme_dark_secondaryContainer = Color(0xFF00458f)
val md_theme_dark_onSecondaryContainer = Color(0xFFd5e3ff)
val md_theme_dark_tertiary = Color(0xFF4fd8eb)
val md_theme_dark_onTertiary = Color(0xFF00363d)
val md_theme_dark_tertiaryContainer = Color(0xFF004f59)
val md_theme_dark_onTertiaryContainer = Color(0xFF8ef1ff)
val md_theme_dark_error = Color(0xFFffb4a9)
val md_theme_dark_errorContainer = Color(0xFF930006)
val md_theme_dark_onError = Color(0xFF680003)
val md_theme_dark_onErrorContainer = Color(0xFFffdad4)
val md_theme_dark_background = Color(0xFF191c1d)
val md_theme_dark_onBackground = Color(0xFFe0e3e3)
val md_theme_dark_surface = Color(0xFF191c1d)
val md_theme_dark_onSurface = Color(0xFFe0e3e3)
val md_theme_dark_surfaceVariant = Color(0xFF46464e)
val md_theme_dark_onSurfaceVariant = Color(0xFFc7c5d0)
val md_theme_dark_outline = Color(0xFF91909a)
val md_theme_dark_inverseOnSurface = Color(0xFF191c1d)
val md_theme_dark_inverseSurface = Color(0xFFe0e3e3)
val md_theme_dark_inversePrimary = Color(0xFF2c47ef)
val md_theme_dark_shadow = Color(0xFF000000)

val seed = Color(0xFF415aff)
val error = Color(0xFFba1b1b)

object Ui {
    val DefaultAqua = Color(0xff62b9f9)
    val DarkModeAqua = Color(0xff64c7ff)
    val DefaultBlue = Color(0xff4078fb)
    val DarkModeBlue = Color(0xff4788ff)
    val DefaultBackground = Color(0xfff9f9f7)
    val Gray20 = Color(0xff2c2e33)
    val Gray30 = Color(0xff44464d)
    val Gray40 = Color(0xff5c5f67)
    val Gray50 = Color(0xff737980)
    val Gray60 = Color(0xff8d9499)
    val Gray70 = Color(0xffa6b0b2)
    val Gray80 = Color(0xffc4cccc)
    val Gray91 = Color(0xffe3e7e2)
    val Gray94 = Color(0xffeef0ec)
    val Gray96 = Color(0xfff2f4f0)
    val Gray97 = Color(0xfff2f4f0)
    val Gray98 = Color(0xfff9faf6)
    val Gray99 = Color(0xfffbfbf8)
}

/**
 * Maps colors from Compose colors to resources defined in our XML files.
 * This function is necessary mainly for interacting with Android Views provided
 * by WebLayer, which require resource IDs to be passed along.
 *
 * @throws IllegalArgumentException if the color passed in has no mapping.
 */
@ColorRes
fun mapComposeColorToResource(color: Color): Int = when (color) {
    md_theme_light_primary -> R.color.md_theme_light_primary
    md_theme_light_onPrimary -> R.color.md_theme_light_onPrimary
    md_theme_light_primaryContainer -> R.color.md_theme_light_primaryContainer
    md_theme_light_onPrimaryContainer -> R.color.md_theme_light_onPrimaryContainer
    md_theme_light_secondary -> R.color.md_theme_light_secondary
    md_theme_light_onSecondary -> R.color.md_theme_light_onSecondary
    md_theme_light_secondaryContainer -> R.color.md_theme_light_secondaryContainer
    md_theme_light_onSecondaryContainer -> R.color.md_theme_light_onSecondaryContainer
    md_theme_light_tertiary -> R.color.md_theme_light_tertiary
    md_theme_light_onTertiary -> R.color.md_theme_light_onTertiary
    md_theme_light_tertiaryContainer -> R.color.md_theme_light_tertiaryContainer
    md_theme_light_onTertiaryContainer -> R.color.md_theme_light_onTertiaryContainer
    md_theme_light_error -> R.color.md_theme_light_error
    md_theme_light_errorContainer -> R.color.md_theme_light_errorContainer
    md_theme_light_onError -> R.color.md_theme_light_onError
    md_theme_light_onErrorContainer -> R.color.md_theme_light_onErrorContainer
    md_theme_light_background -> R.color.md_theme_light_background
    md_theme_light_onBackground -> R.color.md_theme_light_onBackground
    md_theme_light_surface -> R.color.md_theme_light_surface
    md_theme_light_onSurface -> R.color.md_theme_light_onSurface
    md_theme_light_surfaceVariant -> R.color.md_theme_light_surfaceVariant
    md_theme_light_onSurfaceVariant -> R.color.md_theme_light_onSurfaceVariant
    md_theme_light_outline -> R.color.md_theme_light_outline
    md_theme_light_inverseOnSurface -> R.color.md_theme_light_inverseOnSurface
    md_theme_light_inverseSurface -> R.color.md_theme_light_inverseSurface
    md_theme_light_inversePrimary -> R.color.md_theme_light_inversePrimary
    md_theme_light_shadow -> R.color.md_theme_light_shadow

    md_theme_dark_primary -> R.color.md_theme_dark_primary
    md_theme_dark_onPrimary -> R.color.md_theme_dark_onPrimary
    md_theme_dark_primaryContainer -> R.color.md_theme_dark_primaryContainer
    md_theme_dark_onPrimaryContainer -> R.color.md_theme_dark_onPrimaryContainer
    md_theme_dark_secondary -> R.color.md_theme_dark_secondary
    md_theme_dark_onSecondary -> R.color.md_theme_dark_onSecondary
    md_theme_dark_secondaryContainer -> R.color.md_theme_dark_secondaryContainer
    md_theme_dark_onSecondaryContainer -> R.color.md_theme_dark_onSecondaryContainer
    md_theme_dark_tertiary -> R.color.md_theme_dark_tertiary
    md_theme_dark_onTertiary -> R.color.md_theme_dark_onTertiary
    md_theme_dark_tertiaryContainer -> R.color.md_theme_dark_tertiaryContainer
    md_theme_dark_onTertiaryContainer -> R.color.md_theme_dark_onTertiaryContainer
    md_theme_dark_error -> R.color.md_theme_dark_error
    md_theme_dark_errorContainer -> R.color.md_theme_dark_errorContainer
    md_theme_dark_onError -> R.color.md_theme_dark_onError
    md_theme_dark_onErrorContainer -> R.color.md_theme_dark_onErrorContainer
    md_theme_dark_background -> R.color.md_theme_dark_background
    md_theme_dark_onBackground -> R.color.md_theme_dark_onBackground
    md_theme_dark_surface -> R.color.md_theme_dark_surface
    md_theme_dark_onSurface -> R.color.md_theme_dark_onSurface
    md_theme_dark_surfaceVariant -> R.color.md_theme_dark_surfaceVariant
    md_theme_dark_onSurfaceVariant -> R.color.md_theme_dark_onSurfaceVariant
    md_theme_dark_outline -> R.color.md_theme_dark_outline
    md_theme_dark_inverseOnSurface -> R.color.md_theme_dark_inverseOnSurface
    md_theme_dark_inverseSurface -> R.color.md_theme_dark_inverseSurface
    md_theme_dark_inversePrimary -> R.color.md_theme_dark_inversePrimary
    md_theme_dark_shadow -> R.color.md_theme_dark_shadow

    else -> throw IllegalArgumentException("Color mapping is undefined")
}

/** Determines the alpha value to use when rendering controls that can be disabled. */
fun getClickableAlpha(isEnabled: Boolean): Float = if (isEnabled) 1.0f else 0.38f
