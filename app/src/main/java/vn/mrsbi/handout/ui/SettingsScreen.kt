package vn.mrsbi.handout.ui

import android.content.Intent
import android.widget.Toast
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Button
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.Switch
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import vn.mrsbi.handout.BuildConfig
import vn.mrsbi.handout.data.AppData
import vn.mrsbi.handout.data.Store

@Composable
fun SettingsScreen(store: Store, data: AppData) {
    val context = LocalContext.current
    var lock by remember { mutableStateOf(store.lockEnabled) }
    var backup by remember { mutableStateOf(store.backupUri) }
    val status by store.backupStatus.collectAsState()
    var confirmRestore by remember { mutableStateOf<android.net.Uri?>(null) }
    var checkUpdate by remember { mutableStateOf(false) }
    val flags = Intent.FLAG_GRANT_READ_URI_PERMISSION or Intent.FLAG_GRANT_WRITE_URI_PERMISSION
    // Create (or pick) the backup file on Google Drive once; the permission is kept so every later change is mirrored there.
    val create = rememberLauncherForActivityResult(ActivityResultContracts.CreateDocument("application/json")) { uri ->
        if (uri != null) { context.contentResolver.takePersistableUriPermission(uri, flags); store.backupUri = uri; backup = uri; store.backupNow() }
    }
    val pick = rememberLauncherForActivityResult(ActivityResultContracts.OpenDocument()) { uri ->
        if (uri != null) { runCatching { context.contentResolver.takePersistableUriPermission(uri, Intent.FLAG_GRANT_READ_URI_PERMISSION) }; confirmRestore = uri }
    }
    Column(Modifier.fillMaxSize().verticalScroll(rememberScrollState()).padding(16.dp), verticalArrangement = Arrangement.spacedBy(12.dp)) {
        Text("Cài đặt", style = MaterialTheme.typography.titleLarge, fontWeight = FontWeight.Bold)
        Row(verticalAlignment = Alignment.CenterVertically) {
            Column(Modifier.weight(1f)) {
                Text("Khoá bằng vân tay / khuôn mặt", fontWeight = FontWeight.SemiBold)
                Text("Hỏi khi mở app và khi quay lại sau 1 phút", style = MaterialTheme.typography.bodySmall)
            }
            Switch(lock, { lock = it; store.lockEnabled = it })
        }
        HorizontalDivider()
        Text("Sao lưu Google Drive", fontWeight = FontWeight.SemiBold)
        Text(
            if (backup == null) "Chưa chọn file sao lưu. Bấm nút dưới, chọn Google Drive và lưu file mrs-bi-backup.json. Từ đó mọi thay đổi tự ghi vào file này."
            else "Đang sao lưu tự động vào file đã chọn.\n" + status,
            style = MaterialTheme.typography.bodyMedium,
        )
        Button(onClick = { create.launch("mrs-bi-backup.json") }, modifier = Modifier.fillMaxWidth()) { Text(if (backup == null) "Chọn nơi sao lưu (Drive)" else "Đổi file sao lưu") }
        if (backup != null) OutlinedButton(onClick = { store.backupNow() }, modifier = Modifier.fillMaxWidth()) { Text("Sao lưu ngay") }
        OutlinedButton(onClick = { pick.launch(arrayOf("application/json", "application/octet-stream", "*/*")) }, modifier = Modifier.fillMaxWidth()) { Text("Khôi phục từ file sao lưu") }
        HorizontalDivider()
        Text("Dữ liệu: ${data.students.count { !it.archived }} học sinh · ${data.classes.count { !it.archived }} lớp · ${data.sessions.count { it.done }} buổi đã dạy",
            style = MaterialTheme.typography.bodyMedium)
        HorizontalDivider()
        Text("Phiên bản " + BuildConfig.VERSION_NAME + " (" + BuildConfig.VERSION_CODE + ")", style = MaterialTheme.typography.bodyMedium)
        OutlinedButton(onClick = { checkUpdate = true }, modifier = Modifier.fillMaxWidth()) { Text("Kiểm tra cập nhật") }
    }
    if (checkUpdate) UpdatePrompt(manual = true, onClose = { checkUpdate = false })
    confirmRestore?.let { uri ->
        AlertDialog(onDismissRequest = { confirmRestore = null }, title = { Text("Khôi phục dữ liệu?") },
            text = { Text("Toàn bộ dữ liệu trên máy sẽ được thay bằng nội dung file sao lưu.") },
            confirmButton = { TextButton(onClick = {
                val error = store.restore(uri); confirmRestore = null
                Toast.makeText(context, error ?: "Đã khôi phục", Toast.LENGTH_LONG).show()
            }) { Text("Khôi phục") } },
            dismissButton = { TextButton(onClick = { confirmRestore = null }) { Text("Huỷ") } })
    }
}
