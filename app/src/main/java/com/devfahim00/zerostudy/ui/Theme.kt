package com.devfahim00.zerostudy.ui

import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Typography
import androidx.compose.material3.darkColorScheme
import androidx.compose.material3.lightColorScheme
import androidx.compose.runtime.Composable
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.font.Font
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.sp
import com.devfahim00.zerostudy.R

/* Color palette mirroring the web app's CSS variables. */
data class Pal(
    val bg: Color,
    val card: Color,
    val line: Color,
    val ink: Color,
    val mut: Color,
    val a: Color,
    val b: Color,
    val ok: Color,
    val inp: Color,
    val trk: Color,
    val hov: Color,
    val nav: Color,
    val red: Color,
    val warn: Color
)

val DarkPal = Pal(
    bg = Color(0xFF05060A),
    card = Color(0xFF0E1016),
    line = Color(0xFF1B1E29),
    ink = Color(0xFFE8EBF2),
    mut = Color(0xFF7D8396),
    a = Color(0xFF7CC4FF),
    b = Color(0xFFA78BFA),
    ok = Color(0xFF5EEAD4),
    inp = Color(0xFF090B10),
    trk = Color(0xFF141824),
    hov = Color(0xFF343A4F),
    nav = Color(0xE605060A),
    red = Color(0xFFFB7185),
    warn = Color(0xFFFBBF24)
)

val LightPal = Pal(
    bg = Color(0xFFF3F5FA),
    card = Color(0xFFFFFFFF),
    line = Color(0xFFDFE3ED),
    ink = Color(0xFF13151D),
    mut = Color(0xFF667089),
    a = Color(0xFF2B86D9),
    b = Color(0xFF7C5CF0),
    ok = Color(0xFF0F9F8F),
    inp = Color(0xFFF3F5FA),
    trk = Color(0xFFE6E9F2),
    hov = Color(0xFFB4BCD0),
    nav = Color(0xE6F3F5FA),
    red = Color(0xFFFB7185),
    warn = Color(0xFFD97706)
)

/** Subject color palette from the web app. */
fun subjectColor(hex: String): Color = try {
    Color(android.graphics.Color.parseColor(hex))
} catch (e: Exception) {
    Color(0xFF7D8396)
}

val Sora = FontFamily(
    Font(R.font.sora_light, FontWeight.Light),
    Font(R.font.sora_regular, FontWeight.Normal),
    Font(R.font.sora_semibold, FontWeight.SemiBold),
    Font(R.font.sora_bold, FontWeight.Bold)
)

@Composable
fun pal(): Pal = if (com.devfahim00.zerostudy.Model.S.theme == "dark") DarkPal else LightPal

private fun scheme(p: Pal, dark: Boolean) = if (dark) darkColorScheme(
    primary = p.a,
    onPrimary = p.bg,
    secondary = p.b,
    onSecondary = p.bg,
    tertiary = p.ok,
    onTertiary = p.bg,
    background = p.bg,
    onBackground = p.ink,
    surface = p.card,
    onSurface = p.ink,
    surfaceVariant = p.card,
    onSurfaceVariant = p.mut,
    outline = p.line,
    outlineVariant = p.line,
    error = p.red,
    onError = p.bg
) else lightColorScheme(
    primary = p.a,
    onPrimary = Color.White,
    secondary = p.b,
    onSecondary = Color.White,
    tertiary = p.ok,
    onTertiary = Color.White,
    background = p.bg,
    onBackground = p.ink,
    surface = p.card,
    onSurface = p.ink,
    surfaceVariant = p.card,
    onSurfaceVariant = p.mut,
    outline = p.line,
    outlineVariant = p.line,
    error = p.red,
    onError = Color.White
)

private val AppTypography = Typography(
    bodyLarge = TextStyle(fontFamily = Sora, fontWeight = FontWeight.Normal, fontSize = 15.sp, lineHeight = 22.sp),
    bodyMedium = TextStyle(fontFamily = Sora, fontWeight = FontWeight.Normal, fontSize = 13.sp, lineHeight = 19.sp),
    bodySmall = TextStyle(fontFamily = Sora, fontWeight = FontWeight.Normal, fontSize = 11.sp, lineHeight = 16.sp),
    titleLarge = TextStyle(fontFamily = Sora, fontWeight = FontWeight.SemiBold, fontSize = 20.sp),
    titleMedium = TextStyle(fontFamily = Sora, fontWeight = FontWeight.SemiBold, fontSize = 15.sp),
    titleSmall = TextStyle(fontFamily = Sora, fontWeight = FontWeight.SemiBold, fontSize = 13.sp),
    labelLarge = TextStyle(fontFamily = Sora, fontWeight = FontWeight.SemiBold, fontSize = 14.sp),
    labelMedium = TextStyle(fontFamily = Sora, fontWeight = FontWeight.Medium, fontSize = 12.sp),
    labelSmall = TextStyle(fontFamily = Sora, fontWeight = FontWeight.Medium, fontSize = 10.sp)
)

@Composable
fun ZeroStudyTheme(content: @Composable () -> Unit) {
    val p = pal()
    val dark = com.devfahim00.zerostudy.Model.S.theme == "dark"
    MaterialTheme(
        colorScheme = scheme(p, dark),
        typography = AppTypography,
        content = content
    )
}
