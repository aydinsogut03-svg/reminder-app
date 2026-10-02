package com.aydinsogut.reminder.ui.theme

import androidx.compose.material3.darkColorScheme
import androidx.compose.material3.lightColorScheme
import androidx.compose.ui.graphics.Color

/** Hatırlatıcılara verilebilen vurgu renkleri. Sıra veritabanında saklanır, değiştirme, sadece sona ekle. */
object ReminderPalette {
    val colors = listOf(
        Color(0xFF6366F1), // İndigo
        Color(0xFF14B8A6), // Turkuaz
        Color(0xFFF59E0B), // Amber
        Color(0xFFF43F5E), // Mercan
        Color(0xFFA855F7), // Mor
        Color(0xFF0EA5E9), // Mavi
        Color(0xFF22C55E), // Yeşil
    )

    fun color(index: Int): Color = colors[index.mod(colors.size)]
}

val Success = Color(0xFF16A34A)

/** Bir vurgu renginin zemine karışan yumuşak tonu (açık temada pastel, koyuda hafif parıltı). */
fun Color.soft(amount: Float = 0.12f): Color = copy(alpha = amount)

/** Beyaz zemin üzerinde soft, pastel tonlar: gölge yerine hafif renkli yüzeyler. */
val LightColors = lightColorScheme(
    primary = Color(0xFF5B5FE3),
    onPrimary = Color.White,
    primaryContainer = Color(0xFFEEEEFF),
    onPrimaryContainer = Color(0xFF26236F),
    secondary = Color(0xFF0F9F8F),
    onSecondary = Color.White,
    secondaryContainer = Color(0xFFE4F6F2),
    onSecondaryContainer = Color(0xFF0B3B35),
    tertiary = Color(0xFFD9861A),
    tertiaryContainer = Color(0xFFFFF3E0),
    onTertiaryContainer = Color(0xFF5A3606),
    background = Color.White,
    onBackground = Color(0xFF1C1F2A),
    surface = Color.White,
    onSurface = Color(0xFF1C1F2A),
    surfaceVariant = Color(0xFFF1F2F7),
    onSurfaceVariant = Color(0xFF6B7083),
    surfaceContainerLowest = Color.White,
    surfaceContainerLow = Color(0xFFF7F7FB),
    surfaceContainer = Color(0xFFF2F3F8),
    surfaceContainerHigh = Color(0xFFECEEF4),
    surfaceContainerHighest = Color(0xFFE6E8F0),
    outline = Color(0xFFCDD0DB),
    outlineVariant = Color(0xFFEDEEF3),
    error = Color(0xFFE5484D),
    errorContainer = Color(0xFFFDECEC),
    onErrorContainer = Color(0xFF7A1A1D),
)

val DarkColors = darkColorScheme(
    primary = Color(0xFFA5B4FC),
    onPrimary = Color(0xFF1E1B4B),
    primaryContainer = Color(0xFF3730A3),
    onPrimaryContainer = Color(0xFFE0E7FF),
    secondary = Color(0xFF5EEAD4),
    onSecondary = Color(0xFF042F2E),
    secondaryContainer = Color(0xFF115E59),
    onSecondaryContainer = Color(0xFFCCFBF1),
    tertiary = Color(0xFFFCD34D),
    tertiaryContainer = Color(0xFF78350F),
    onTertiaryContainer = Color(0xFFFEF3C7),
    background = Color(0xFF0E1016),
    onBackground = Color(0xFFE7E9F1),
    surface = Color(0xFF0E1016),
    onSurface = Color(0xFFE7E9F1),
    surfaceVariant = Color(0xFF222634),
    onSurfaceVariant = Color(0xFFA3A9BC),
    surfaceContainerLowest = Color(0xFF0A0C11),
    surfaceContainerLow = Color(0xFF171A23),
    surfaceContainer = Color(0xFF1B1F2A),
    surfaceContainerHigh = Color(0xFF232836),
    surfaceContainerHighest = Color(0xFF2B3040),
    outline = Color(0xFF4A5064),
    outlineVariant = Color(0xFF2E3343),
    error = Color(0xFFF87171),
    errorContainer = Color(0xFF450A0A),
    onErrorContainer = Color(0xFFFECACA),
)
