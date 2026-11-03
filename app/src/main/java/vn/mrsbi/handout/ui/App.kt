package vn.mrsbi.handout.ui

import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.CalendarViewWeek
import androidx.compose.material.icons.filled.Groups
import androidx.compose.material.icons.filled.Payments
import androidx.compose.material.icons.filled.Settings
import androidx.compose.material.icons.filled.Today
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Icon
import androidx.compose.material3.LinearProgressIndicator
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.NavigationBar
import androidx.compose.material3.NavigationBarItem
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableFloatStateOf
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.unit.dp
import kotlinx.coroutines.launch
import vn.mrsbi.handout.data.Rules
import vn.mrsbi.handout.data.Store
import vn.mrsbi.handout.data.closeMonth
import vn.mrsbi.handout.util.Updater

private data class Tab(val title: String, val icon: ImageVector)
private val tabs = listOf(
    Tab("Hôm nay", Icons.Default.Today), Tab("Tuần", Icons.Default.CalendarViewWeek), Tab("Học phí", Icons.Default.Payments),
    Tab("Quản lý", Icons.Default.Groups), Tab("Cài đặt", Icons.Default.Settings),
)

@Composable
fun App(store: Store) {
    val data by store.data.collectAsState()
    var tab by rememberSaveable { mutableIntStateOf(0) }
    LaunchedEffect(Unit) { store.syncSheet() }   // catch up on changes made while offline
    // On the last day of a month (or later, while the previous month is still open) ask once a day to close it.
    val today = java.time.LocalDate.now()
    val askMonth = listOf(java.time.YearMonth.from(today).minusMonths(1), java.time.YearMonth.from(today)).firstOrNull { m ->
        Rules.monthKey(m) !in data.closedMonths && !today.isBefore(m.atEndOfMonth()) && data.sessions.any { it.done && Rules.inMonth(it.day, m) }
    }
    var askClose by remember { mutableStateOf(askMonth != null && store.closeAskedDay != today.toEpochDay()) }
    if (askClose && askMonth != null) CloseMonthDialog(askMonth,
        onConfirm = { store.closeMonth(askMonth); store.closeAskedDay = today.toEpochDay(); askClose = false; tab = 2 },
        onDismiss = { store.closeAskedDay = today.toEpochDay(); askClose = false })
    Scaffold(
        bottomBar = {
            NavigationBar(containerColor = CardBg, tonalElevation = 0.dp) {
                tabs.forEachIndexed { i, t ->
                    NavigationBarItem(selected = tab == i, onClick = { tab = i }, icon = { Icon(t.icon, t.title, Modifier.size(24.dp)) }, label = { Text(t.title, maxLines = 1, softWrap = false) },
                        colors = androidx.compose.material3.NavigationBarItemDefaults.colors(selectedIconColor = Accent, selectedTextColor = Accent,
                            unselectedIconColor = MaterialTheme.colorScheme.onSurfaceVariant, unselectedTextColor = MaterialTheme.colorScheme.onSurfaceVariant, indicatorColor = MaterialTheme.colorScheme.primaryContainer))
                }
            }
        },
    ) { padding ->
        Box(Modifier.fillMaxSize().padding(padding)) {
            when (tab) {
                0 -> TodayScreen(store, data, openManage = { tab = 3 })
                1 -> WeekScreen(store, data)
                2 -> FeesScreen(store, data)
                3 -> ManageScreen(store, data)
                else -> SettingsScreen(store, data)
            }
        }
    }
    UpdatePrompt()
}

/** Checks the update branch once per launch and offers to install a newer signed APK. */
@Composable
fun UpdatePrompt(manual: Boolean = false, onClose: () -> Unit = {}) {
    val context = LocalContext.current
    val scope = rememberCoroutineScope()
    var release by remember { mutableStateOf<Updater.Release?>(null) }
    var checked by remember { mutableStateOf(false) }
    var progress by remember { mutableFloatStateOf(-1f) }
    var error by remember { mutableStateOf<String?>(null) }
    LaunchedEffect(Unit) { release = Updater.check(); checked = true }
    val r = release
    if (r != null) {
        AlertDialog(
            onDismissRequest = { if (progress < 0) { release = null; onClose() } },
            title = { Text("Có bản mới " + r.versionName) },
            text = {
                androidx.compose.foundation.layout.Column {
                    if (r.notes.isNotBlank()) Text(r.notes)
                    if (progress >= 0) LinearProgressIndicator(progress = { progress }, modifier = Modifier.padding(top = 12.dp))
                    error?.let { Text(it) }
                }
            },
            confirmButton = {
                TextButton(enabled = progress < 0, onClick = {
                    if (!Updater.canInstall(context)) { Updater.askInstallPermission(context); return@TextButton }
                    progress = 0f
                    scope.launch {
                        runCatching { Updater.download(context, r) { p -> progress = p } }
                            .onSuccess { Updater.install(context, it); progress = -1f }
                            .onFailure { error = "Tải lỗi: " + it.message; progress = -1f }
                    }
                }) { Text(if (Updater.canInstall(context)) "Cập nhật" else "Cho phép cài") }
            },
            dismissButton = { TextButton(enabled = progress < 0, onClick = { release = null; onClose() }) { Text("Để sau") } },
        )
    } else if (manual && checked) {
        AlertDialog(onDismissRequest = onClose, confirmButton = { TextButton(onClick = onClose) { Text("OK") } },
            text = { Text("Đang dùng bản mới nhất (hoặc không có mạng).") })
    }
}

