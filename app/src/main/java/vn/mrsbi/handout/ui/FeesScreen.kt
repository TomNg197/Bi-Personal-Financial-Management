package vn.mrsbi.handout.ui

import android.widget.Toast
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.KeyboardArrowLeft
import androidx.compose.material.icons.automirrored.filled.KeyboardArrowRight
import androidx.compose.material.icons.filled.FileDownload
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.Checkbox
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import vn.mrsbi.handout.data.AppData
import vn.mrsbi.handout.data.FeeLine
import vn.mrsbi.handout.data.Rules
import vn.mrsbi.handout.data.Store
import vn.mrsbi.handout.data.setPaid
import vn.mrsbi.handout.util.Csv
import vn.mrsbi.handout.util.Format
import java.time.YearMonth

/** Month fees: per student (personal classes) and per class (center classes), each with a "đã thu" tick. */
@Composable
fun FeesScreen(store: Store, data: AppData) {
    var monthText by rememberSaveable { mutableStateOf(YearMonth.now().toString()) }
    val month = YearMonth.parse(monthText)
    val summary = Rules.monthSummary(data, month)
    var detail by remember { mutableStateOf<FeeLine?>(null) }
    val context = LocalContext.current
    val export = rememberLauncherForActivityResult(ActivityResultContracts.CreateDocument("text/csv")) { uri ->
        if (uri != null) runCatching { context.contentResolver.openOutputStream(uri)!!.use { it.write(Csv.month(data, month).toByteArray()) } }
            .onSuccess { Toast.makeText(context, "Đã xuất bảng học phí", Toast.LENGTH_SHORT).show() }
    }
    Column(Modifier.fillMaxSize()) {
        Row(Modifier.fillMaxWidth().padding(8.dp), verticalAlignment = Alignment.CenterVertically) {
            IconButton(onClick = { monthText = month.minusMonths(1).toString() }) { Icon(Icons.AutoMirrored.Filled.KeyboardArrowLeft, "Tháng trước") }
            Text(Format.month(month), Modifier.weight(1f), style = MaterialTheme.typography.titleLarge, fontWeight = FontWeight.Bold,
                textAlign = androidx.compose.ui.text.style.TextAlign.Center)
            IconButton(onClick = { monthText = month.plusMonths(1).toString() }) { Icon(Icons.AutoMirrored.Filled.KeyboardArrowRight, "Tháng sau") }
            IconButton(onClick = { export.launch("hoc-phi-" + month + ".csv") }) { Icon(Icons.Default.FileDownload, "Xuất CSV") }
        }
        Row(Modifier.fillMaxWidth().padding(horizontal = 12.dp), horizontalArrangement = Arrangement.spacedBy(8.dp)) {
            Stat("Phải thu", summary.total, Violet, Modifier.weight(1f))
            Stat("Đã thu", summary.paid, Color(0xFF10B981), Modifier.weight(1f))
            Stat("Còn lại", summary.unpaid, Color(0xFFF59E0B), Modifier.weight(1f))
        }
        val personal = summary.lines.filter { it.key.startsWith("s:") }
        val center = summary.lines.filter { it.key.startsWith("c:") }
        if (summary.lines.isEmpty()) Text("Chưa có buổi nào được bấm Xong trong tháng.", Modifier.padding(24.dp))
        LazyColumn(Modifier.fillMaxSize().padding(horizontal = 12.dp)) {
            if (personal.isNotEmpty()) item { Header("Lớp cá nhân · theo học sinh") }
            items(personal, key = { it.key }) { line -> FeeRow(line, onTick = { store.setPaid(month, line.key, it) }, onOpen = { detail = line }) }
            if (center.isNotEmpty()) item { Header("Lớp trung tâm · theo lớp") }
            items(center, key = { it.key }) { line -> FeeRow(line, onTick = { store.setPaid(month, line.key, it) }, onOpen = null) }
            item { Spacer(Modifier.height(16.dp)) }
        }
    }
    detail?.let { line ->
        val sessions = Rules.studentSessions(data, line.key.removePrefix("s:"), month)
        AlertDialog(
            onDismissRequest = { detail = null },
            confirmButton = { TextButton(onClick = { detail = null }) { Text("Đóng") } },
            title = { Text(line.title) },
            text = {
                LazyColumn {
                    items(sessions) { (record, group, present) ->
                        Row(Modifier.fillMaxWidth().padding(vertical = 4.dp)) {
                            Text(Format.shortDay(record.day) + " · " + group.name, Modifier.weight(1f))
                            Text(if (present) Format.money(record.charges[line.key.removePrefix("s:")] ?: 0) else "Vắng",
                                color = if (present) Color.Unspecified else Color(0xFFEF4444))
                        }
                    }
                }
            },
        )
    }
}

@Composable
private fun Stat(label: String, value: Long, color: Color, modifier: Modifier) {
    Card(modifier, shape = RoundedCornerShape(14.dp), colors = CardDefaults.cardColors(containerColor = color.copy(alpha = .12f))) {
        Column(Modifier.padding(10.dp)) {
            Text(label, style = MaterialTheme.typography.labelMedium, color = color)
            Text(Format.money(value), style = MaterialTheme.typography.titleSmall, fontWeight = FontWeight.Bold)
        }
    }
}

@Composable
private fun Header(text: String) {
    Text(text, Modifier.padding(top = 16.dp, bottom = 4.dp), style = MaterialTheme.typography.titleSmall, color = MaterialTheme.colorScheme.primary)
}

@Composable
private fun FeeRow(line: FeeLine, onTick: (Boolean) -> Unit, onOpen: (() -> Unit)?) {
    Row(Modifier.fillMaxWidth().clickable(enabled = onOpen != null) { onOpen?.invoke() }.padding(vertical = 8.dp), verticalAlignment = Alignment.CenterVertically) {
        Column(Modifier.weight(1f)) {
            Text(line.title, style = MaterialTheme.typography.bodyLarge, fontWeight = FontWeight.SemiBold)
            Text(line.subtitle + " · " + line.present + " buổi" + if (line.absent > 0) " · vắng " + line.absent else "",
                style = MaterialTheme.typography.bodySmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
        }
        Column(horizontalAlignment = Alignment.End) {
            Text(Format.money(line.amount), fontWeight = FontWeight.Bold)
            Text(if (line.paidAt != null) "Đã thu " + java.text.SimpleDateFormat("dd/MM").format(java.util.Date(line.paidAt)) else "Chưa thu",
                style = MaterialTheme.typography.labelSmall, color = if (line.paidAt != null) Color(0xFF10B981) else Color(0xFFF59E0B))
        }
        Checkbox(checked = line.paidAt != null, onCheckedChange = onTick)
    }
    HorizontalDivider()
}
