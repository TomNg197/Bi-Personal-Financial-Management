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
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.unit.dp

val Pink = Color(0xFFE85D8E)
val ClassColors = listOf(0xFFE85D8E, 0xFF7C5CFC, 0xFF2BB3A3, 0xFFF59E0B, 0xFF3B82F6, 0xFF10B981, 0xFFEF4444, 0xFF8B5E3C)

@Composable
fun MrsBiTheme(content: @Composable () -> Unit) {
    MaterialTheme(
        colorScheme = lightColorScheme(
            primary = Pink, onPrimary = Color.White, primaryContainer = Color(0xFFFFD9E4), onPrimaryContainer = Color(0xFF3E001D),
            secondary = Color(0xFF7C5CFC), background = Color(0xFFFFF7FA), surface = Color(0xFFFFF7FA),
            surfaceVariant = Color(0xFFF6E8EE), surfaceContainer = Color(0xFFFBEFF4),
        ),
        content = content,
    )
}

@Composable
fun LockScreen(onUnlock: () -> Unit) {
    Surface(Modifier.fillMaxSize(), color = MaterialTheme.colorScheme.background) {
        Column(Modifier.padding(32.dp), verticalArrangement = Arrangement.Center, horizontalAlignment = Alignment.CenterHorizontally) {
            Icon(Icons.Default.Fingerprint, null, tint = Pink, modifier = Modifier.height(72.dp))
            Spacer(Modifier.height(16.dp))
            Text("Mrs Bi Handout", style = MaterialTheme.typography.headlineSmall)
            Spacer(Modifier.height(8.dp))
            Text("Dữ liệu học phí được khoá bằng vân tay / khuôn mặt của máy.", style = MaterialTheme.typography.bodyMedium)
            Spacer(Modifier.height(24.dp))
            Button(onClick = onUnlock) { Text("Mở khoá") }
        }
    }
}
