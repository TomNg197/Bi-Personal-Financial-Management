package vn.mrsbi.handout.ui

import androidx.compose.material3.OutlinedTextField
import vn.mrsbi.handout.util.Format
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.draw.clip
import androidx.compose.material3.Icon
import androidx.compose.material.icons.filled.Warning
import androidx.compose.material.icons.Icons
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.border
import androidx.compose.foundation.background
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
import androidx.compose.ui.platform.LocalClipboardManager
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
    val clipboard = LocalClipboardManager.current
    val sheetStatus by store.sheetStatus.collectAsState()
    var sheetReady by remember { mutableStateOf(store.sheet.configured) }
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
        if (backup == null) {
            Column(Modifier.fillMaxWidth().clip(RoundedCornerShape(16.dp)).background(Color(0xFFF59E0B).copy(alpha = .14f)).border(1.dp, Color(0xFFF59E0B), RoundedCornerShape(16.dp)).padding(16.dp),
                verticalArrangement = Arrangement.spacedBy(8.dp)) {
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Icon(Icons.Default.Warning, null, tint = Color(0xFFB45309)); Spacer(Modifier.width(8.dp))
                    Text("Chưa có bản sao lưu", fontWeight = FontWeight.Bold, color = MaterialTheme.colorScheme.onSurface)
                }
                Text("Mất máy là mất dữ liệu. Chọn nơi lưu trên Google Drive.", style = MaterialTheme.typography.bodyMedium, color = MaterialTheme.colorScheme.onSurface)
                Button(onClick = { create.launch("just-bi-backup.json") }, modifier = Modifier.fillMaxWidth()) { Text("Chọn nơi sao lưu") }
            }
        } else {
            val last = store.lastBackupAt
            Text(if (last > 0 && !status.startsWith("Sao lưu lỗi")) "Sao lưu lần cuối: " + Format.lastBackup(last) else status.ifBlank { "Đang sao lưu tự động vào file đã chọn." },
                style = MaterialTheme.typography.bodyMedium, color = if (status.startsWith("Sao lưu lỗi")) Absent else MaterialTheme.colorScheme.onSurface)
            Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                OutlinedButton(onClick = { store.backupNow() }, modifier = Modifier.weight(1f)) { Text("Sao lưu ngay") }
                OutlinedButton(onClick = { create.launch("just-bi-backup.json") }, modifier = Modifier.weight(1f)) { Text("Đổi nơi sao lưu") }
            }
        }
        OutlinedButton(onClick = { pick.launch(arrayOf("application/json", "application/octet-stream", "*/*")) }, modifier = Modifier.fillMaxWidth()) { Text("Khôi phục từ file sao lưu") }
        HorizontalDivider()
        Text("Google Sheet học phí (mỗi tháng 1 tab)", fontWeight = FontWeight.SemiBold)
        Text(if (sheetReady) "Tự cập nhật vài giây sau mỗi lần Xong buổi / tick đã thu.\n" + sheetStatus
            else "Copy dòng cấu hình (link|mã) được gửi riêng, rồi bấm nút dưới.", style = MaterialTheme.typography.bodyMedium)
        Button(onClick = {
            val ok = store.sheet.importConfig(clipboard.getText()?.text ?: "")
            sheetReady = store.sheet.configured
            Toast.makeText(context, if (ok) "Đã lưu cấu hình Google Sheet" else "Clipboard không phải dòng cấu hình hợp lệ", Toast.LENGTH_LONG).show()
            if (ok) store.syncSheet(force = true)
        }, modifier = Modifier.fillMaxWidth()) { Text(if (sheetReady) "Dán lại cấu hình Google Sheet" else "Dán cấu hình Google Sheet") }
        if (sheetReady) OutlinedButton(onClick = { store.syncSheet(force = true) }, modifier = Modifier.fillMaxWidth()) { Text("Cập nhật Google Sheet ngay") }
        HorizontalDivider()
        var sign by remember { mutableStateOf(store.signature) }
        OutlinedTextField(sign, { sign = it; store.signature = it }, label = { Text("Ký tên cuối tin nhắn học phí") }, singleLine = true, modifier = Modifier.fillMaxWidth())
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
