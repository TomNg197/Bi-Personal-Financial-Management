package vn.mrsbi.handout.ui

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Fingerprint
import androidx.compose.material3.Button
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.lightColorScheme
import androidx.compose.material3.darkColorScheme
import androidx.compose.foundation.isSystemInDarkTheme
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.material3.Typography
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.unit.sp
import androidx.compose.ui.unit.dp

/** Colourful but calm: violet → pink accents on a soft lilac background; each class keeps its own board colour. */
val Ink = Color(0xFF1D1B2F)
val Violet = Color(0xFF7C5CFC)
val Rose = Color(0xFFE85D8E)
val Muted = Color(0xFF6B6782)
val Absent = Color(0xFFD92D20)
/** Two sessions at the same time. */
val Clash = Color(0xFFF97316)
val ClassColorNames = listOf("Hồng", "Tím", "Xanh ngọc", "Cam", "Xanh dương", "Xanh lá", "Đỏ", "Nâu")
val Hairline = Color(0xFFECE8F7)
val Lilac = Color(0xFFF6F3FF)
val ClassColors = listOf(0xFFE85D8E, 0xFF7C5CFC, 0xFF2BB3A3, 0xFFF59E0B, 0xFF3B82F6, 0xFF10B981, 0xFFEF4444, 0xFF8B5E3C)

/** Cards and bars sit on this (white in light mode, raised violet-grey in dark mode). */
val CardBg: Color @Composable get() = MaterialTheme.colorScheme.surfaceContainerLowest
/** Brand violet for titles; lighter in dark mode so it stays readable. */
val Accent: Color @Composable get() = MaterialTheme.colorScheme.primary
/** Money already earned / collected. */
val Earned = Color(0xFF12996B)

@Composable
fun MrsBiTheme(dark: Boolean = isSystemInDarkTheme(), content: @Composable () -> Unit) {
    val light = lightColorScheme(
        primary = Violet, onPrimary = Color.White, primaryContainer = Color(0xFFE9E2FF), onPrimaryContainer = Color(0xFF2A1670),
        secondary = Rose, onSecondary = Color.White, secondaryContainer = Color(0xFFFFDDE8), onSecondaryContainer = Color(0xFF5A0F2E),
        background = Lilac, onBackground = Ink, surface = Lilac, onSurface = Ink,
        surfaceVariant = Color(0xFFEDE8FA), onSurfaceVariant = Muted, outline = Color(0xFFCFC8E6), outlineVariant = Hairline,
        surfaceContainer = Color.White, surfaceContainerLow = Color.White, surfaceContainerHigh = Color(0xFFF3EFFF),
        surfaceContainerHighest = Color(0xFFEDE8FA), surfaceContainerLowest = Color.White,
    )
    val night = darkColorScheme(
        primary = Color(0xFF9F8BFF), onPrimary = Color(0xFF1A0F4D), primaryContainer = Color(0xFF3A2C7A), onPrimaryContainer = Color(0xFFE9E2FF),
        secondary = Color(0xFFFF8DB3), onSecondary = Color(0xFF4A0B24), secondaryContainer = Color(0xFF5E1A38), onSecondaryContainer = Color(0xFFFFDDE8),
        background = Color(0xFF15131F), onBackground = Color(0xFFECE9F5), surface = Color(0xFF15131F), onSurface = Color(0xFFECE9F5),
        surfaceVariant = Color(0xFF2A2638), onSurfaceVariant = Color(0xFFB3AEC6), outline = Color(0xFF4A4560), outlineVariant = Color(0xFF2E2A3E),
        surfaceContainer = Color(0xFF1F1C2C), surfaceContainerLow = Color(0xFF1F1C2C), surfaceContainerHigh = Color(0xFF2A2638),
        surfaceContainerHighest = Color(0xFF332F43), surfaceContainerLowest = Color(0xFF221F30),
    )
    MaterialTheme(
        colorScheme = if (dark) night else light,
        // Body text never below 14sp (bodySmall is 12sp in Material by default).
        typography = Typography().let { t -> t.copy(bodySmall = t.bodySmall.copy(fontSize = 14.sp, lineHeight = 20.sp)) },
        content = content,
    )
}

@Composable
fun LockScreen(onUnlock: () -> Unit) {
    Surface(Modifier.fillMaxSize(), color = MaterialTheme.colorScheme.background) {
        Column(Modifier.padding(32.dp), verticalArrangement = Arrangement.Center, horizontalAlignment = Alignment.CenterHorizontally) {
            Icon(Icons.Default.Fingerprint, null, tint = Accent, modifier = Modifier.height(72.dp))
            Spacer(Modifier.height(16.dp))
            Text("Just Bi", style = MaterialTheme.typography.headlineSmall)
            Spacer(Modifier.height(8.dp))
            Text("Dữ liệu học phí được khoá bằng vân tay / khuôn mặt của máy.", style = MaterialTheme.typography.bodyMedium)
            Spacer(Modifier.height(24.dp))
            Button(onClick = onUnlock) { Text("Mở khoá") }
        }
    }
}
