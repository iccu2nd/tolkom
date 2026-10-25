package com.tinta.reader.ui.theme

import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Shapes
import androidx.compose.material3.Typography
import androidx.compose.material3.darkColorScheme
import androidx.compose.runtime.Composable
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp

/** Design tokens. Semua warna/ukuran komponen merujuk ke sini. */
object Ink {
    val Background = Color(0xFF080808)
    val Surface = Color(0xFF141414)
    val Elevated = Color(0xFF202024)
    val TextPrimary = Color(0xFFF5F5F7)
    val TextSecondary = Color(0xFFA1A1AA)
    val TextMuted = Color(0xFF71717A)
    val Border = Color(0xFF29292F)
    val Accent = Color(0xFFB3261E)        // merah tua ala tinta komik
    val AccentOn = Color(0xFFFFFFFF)
    val Scrim = Color(0xFF000000)
}

object Dimens {
    val ScreenPadding = 16.dp
    val SectionGap = 24.dp
    val TopBarHeight = 60.dp
    val CoverRadius = 10.dp
    val ButtonRadius = 14.dp
    val CoverWidth = 124.dp
    const val CoverRatio = 2f / 3f
    val HeroHeight = 360.dp
    val MinTouch = 48.dp
}

private val Sans = FontFamily.SansSerif

private val InkTypography = Typography(
    headlineMedium = TextStyle(fontFamily = Sans, fontWeight = FontWeight.ExtraBold, fontSize = 28.sp, lineHeight = 32.sp, letterSpacing = (-0.4).sp),
    titleLarge = TextStyle(fontFamily = Sans, fontWeight = FontWeight.Bold, fontSize = 20.sp, lineHeight = 26.sp),
    titleMedium = TextStyle(fontFamily = Sans, fontWeight = FontWeight.SemiBold, fontSize = 16.sp, lineHeight = 22.sp),
    titleSmall = TextStyle(fontFamily = Sans, fontWeight = FontWeight.SemiBold, fontSize = 14.sp, lineHeight = 18.sp),
    bodyLarge = TextStyle(fontFamily = Sans, fontWeight = FontWeight.Normal, fontSize = 16.sp, lineHeight = 24.sp),
    bodyMedium = TextStyle(fontFamily = Sans, fontWeight = FontWeight.Normal, fontSize = 14.sp, lineHeight = 21.sp),
    bodySmall = TextStyle(fontFamily = Sans, fontWeight = FontWeight.Normal, fontSize = 12.sp, lineHeight = 16.sp),
    labelLarge = TextStyle(fontFamily = Sans, fontWeight = FontWeight.SemiBold, fontSize = 14.sp, lineHeight = 18.sp),
    labelMedium = TextStyle(fontFamily = Sans, fontWeight = FontWeight.Medium, fontSize = 12.sp, lineHeight = 16.sp),
    labelSmall = TextStyle(fontFamily = Sans, fontWeight = FontWeight.Medium, fontSize = 11.sp, lineHeight = 14.sp),
)

private val InkColors = darkColorScheme(
    primary = Ink.Accent,
    onPrimary = Ink.AccentOn,
    background = Ink.Background,
    onBackground = Ink.TextPrimary,
    surface = Ink.Surface,
    onSurface = Ink.TextPrimary,
    surfaceVariant = Ink.Elevated,
    onSurfaceVariant = Ink.TextSecondary,
    outline = Ink.Border,
    outlineVariant = Ink.Border,
    error = Color(0xFFFF6B63),
)

private val InkShapes = Shapes(
    extraSmall = RoundedCornerShape(6.dp),
    small = RoundedCornerShape(8.dp),
    medium = RoundedCornerShape(Dimens.CoverRadius),
    large = RoundedCornerShape(Dimens.ButtonRadius),
)

@Composable
fun TintaTheme(content: @Composable () -> Unit) {
    MaterialTheme(colorScheme = InkColors, typography = InkTypography, shapes = InkShapes, content = content)
}
