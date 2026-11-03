package vn.mrsbi.handout.ui

import android.content.Intent
import android.widget.Toast
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.RowScope
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.navigationBarsPadding
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.KeyboardArrowLeft
import androidx.compose.material.icons.automirrored.filled.KeyboardArrowRight
import androidx.compose.material.icons.automirrored.filled.Send
import androidx.compose.material.icons.filled.Check
import androidx.compose.material.icons.filled.Event
import androidx.compose.material.icons.filled.FileDownload
import androidx.compose.material.icons.filled.MoreVert
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.DropdownMenu
import androidx.compose.material3.DropdownMenuItem
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.FilledTonalButton
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.ModalBottomSheet
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.SnackbarDuration
import androidx.compose.material3.SnackbarHost
import androidx.compose.material3.SnackbarHostState
import androidx.compose.material3.SnackbarResult
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.SpanStyle
import androidx.compose.ui.text.buildAnnotatedString
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.text.withStyle
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch
import vn.mrsbi.handout.data.AppData
import vn.mrsbi.handout.data.FeeLine
import vn.mrsbi.handout.data.Rules
import vn.mrsbi.handout.data.Store
import vn.mrsbi.handout.data.closeMonth
import vn.mrsbi.handout.data.reopenMonth
import vn.mrsbi.handout.data.setPaid
import vn.mrsbi.handout.util.Csv
import vn.mrsbi.handout.util.Format
import java.time.LocalDate
import java.time.YearMonth

private val Pink = Color(0xFFE85D8E)

/**
 * Fees are collected at the end of the month. Before the month is closed the screen shows a running estimate;
 * after closing it becomes a collection list (unpaid first) with messages for parents.
 */
@Composable
fun FeesScreen(store: Store, data: AppData) {
    var monthText by rememberSaveable { mutableStateOf(YearMonth.now().toString()) }
    val month = YearMonth.parse(monthText)
    val summary = Rules.monthSummary(data, month)
    val closedAt = data.closedMonths[Rules.monthKey(month)]
    var detail by remember { mutableStateOf<FeeLine?>(null) }
    var share by remember { mutableStateOf(false) }
    var confirmClose by remember { mutableStateOf(false) }
    val snackbar = remember { SnackbarHostState() }
    val scope = rememberCoroutineScope()
    val context = LocalContext.current
    val export = rememberLauncherForActivityResult(ActivityResultContracts.CreateDocument("text/csv")) { uri ->
        if (uri != null) runCatching { context.contentResolver.openOutputStream(uri)!!.use { it.write(Csv.month(data, month).toByteArray()) } }
            .onSuccess { Toast.makeText(context, "Đã xuất bảng học phí", Toast.LENGTH_SHORT).show() }
    }
    fun markPaid(line: FeeLine, paid: Boolean) {
        store.setPaid(month, line.key, paid)
        scope.launch {
            val job = launch {
                val r = snackbar.showSnackbar((if (paid) "Đã thu " else "Bỏ đánh dấu ") + line.title, actionLabel = "Hoàn tác", duration = SnackbarDuration.Indefinite)
                if (r == SnackbarResult.ActionPerformed) store.setPaid(month, line.key, !paid)
            }
            delay(5000); snackbar.currentSnackbarData?.dismiss(); job.join()
        }
    }
    val students = summary.lines.filter { it.key.startsWith("s:") }.sortedWith(compareBy(Format.byGivenName) { it.title })
    val centers = summary.lines.filter { it.key.startsWith("c:") }
    val sessionsTaught = data.sessions.count { it.done && Rules.inMonth(it.day, month) }
    Box(Modifier.fillMaxSize()) {
        Column(Modifier.fillMaxSize()) {
            Row(Modifier.fillMaxWidth().padding(start = 4.dp, end = 12.dp, top = 8.dp, bottom = 4.dp), verticalAlignment = Alignment.CenterVertically) {
                IconButton(onClick = { monthText = month.minusMonths(1).toString() }, Modifier.size(48.dp)) { Icon(Icons.AutoMirrored.Filled.KeyboardArrowLeft, "Tháng trước") }
                Text(Format.month(month), style = MaterialTheme.typography.titleLarge, fontWeight = FontWeight.ExtraBold, maxLines = 1)
                IconButton(onClick = { monthText = month.plusMonths(1).toString() }, Modifier.size(48.dp)) { Icon(Icons.AutoMirrored.Filled.KeyboardArrowRight, "Tháng sau") }
                Spacer(Modifier.weight(1f))
                OutlinedButton(onClick = { export.launch("hoc-phi-" + month + ".csv") }, modifier = Modifier.heightIn(min = 48.dp)) {
                    Icon(Icons.Default.FileDownload, null, Modifier.size(20.dp)); Spacer(Modifier.width(6.dp)); Text("CSV", fontWeight = FontWeight.Bold)
                }
            }
            LazyColumn(Modifier.fillMaxSize().padding(horizontal = 14.dp), verticalArrangement = Arrangement.spacedBy(10.dp)) {
                if (closedAt == null) {
                    item("est") { EstimateCard(summary.total, month, sessionsTaught) }
                    item("note") { CloseNote(month, onClose = { confirmClose = true }) }
                    if (summary.lines.isEmpty()) item("none") { Empty() }
                    if (students.isNotEmpty()) item("hs") { Section("THEO HỌC SINH · ${students.size}") }
                    items(students, key = { it.key }) { line -> RunningRow(line, onOpen = { detail = line }, onPrepaid = { markPaid(line, it) }) }
                    if (centers.isNotEmpty()) item("ttc") { Section("LỚP TRUNG TÂM · ${centers.size}") }
                    items(centers, key = { it.key }) { line -> RunningRow(line, onOpen = null, onPrepaid = { markPaid(line, it) }) }
                } else {
                    val unpaid = summary.lines.filter { it.paidAt == null }
                    val paid = summary.lines.filter { it.paidAt != null }
                    item("due") { DueCard(summary.unpaid, summary.paid, summary.total, closedAt, onReopen = { store.reopenMonth(month) }) }
                    if (unpaid.isNotEmpty()) item("send") {
                        OutlinedButton(onClick = { share = true }, modifier = Modifier.fillMaxWidth().heightIn(min = 52.dp),
                            border = androidx.compose.foundation.BorderStroke(1.5.dp, Pink.copy(alpha = .5f))) {
                            Icon(Icons.AutoMirrored.Filled.Send, null, tint = Pink); Spacer(Modifier.width(10.dp))
                            Text("Gửi bảng học phí cho ${unpaid.count { it.key.startsWith("s:") }.takeIf { it > 0 } ?: unpaid.size} " +
                                (if (unpaid.any { it.key.startsWith("s:") }) "phụ huynh" else "lớp"), color = Pink, fontSize = 16.sp, fontWeight = FontWeight.Bold)
                        }
                    }
                    if (unpaid.isNotEmpty()) item("u") { Section("CHƯA THU · ${unpaid.size}") }
                    items(unpaid.sortedWith(compareBy(Format.byGivenName) { it.title }), key = { it.key }) { line ->
                        CollectRow(line, onOpen = { if (line.key.startsWith("s:")) detail = line }, onPaid = { markPaid(line, true) })
                    }
                    if (paid.isNotEmpty()) item("p") { Section("ĐÃ THU · ${paid.size}") }
                    items(paid.sortedWith(compareBy(Format.byGivenName) { it.title }), key = { it.key }) { line ->
                        PaidRow(line, onOpen = { if (line.key.startsWith("s:")) detail = line }, onUnpay = { markPaid(line, false) })
                    }
                }
                item("end") { Spacer(Modifier.height(72.dp)) }
            }
        }
        SnackbarHost(snackbar, Modifier.align(Alignment.BottomCenter))
    }
    if (confirmClose) CloseMonthDialog(month, onConfirm = { store.closeMonth(month); confirmClose = false }, onDismiss = { confirmClose = false })
    if (share) ShareSheet(data, summary.lines.filter { it.paidAt == null }, month, onClose = { share = false })
    detail?.let { line ->
        val sessions = Rules.studentSessions(data, line.key.removePrefix("s:"), month)
        AlertDialog(
            onDismissRequest = { detail = null },
            confirmButton = { TextButton(onClick = { detail = null }) { Text("Đóng") } },
            title = { Text(line.title) },
            text = {
                LazyColumn {
                    items(sessions) { (record, group, present) ->
                        Row(Modifier.fillMaxWidth().padding(vertical = 6.dp)) {
                            Text(Format.shortDay(record.day) + " · " + group.name, Modifier.weight(1f))
                            Text(if (present) Format.money(record.charges[line.key.removePrefix("s:")] ?: 0) else "Vắng",
                                color = if (present) Color.Unspecified else Absent, fontWeight = FontWeight.SemiBold)
                        }
                    }
                }
            },
        )
    }
}

/** Ask once to close a month (from the note card, or on the last day of the month from [App]). */
@Composable
fun CloseMonthDialog(month: YearMonth, onConfirm: () -> Unit, onDismiss: () -> Unit) {
    AlertDialog(onDismissRequest = onDismiss, title = { Text("Chốt học phí " + Format.month(month).lowercase() + "?") },
        text = { Text("Sau khi chốt, tab Học phí chuyển sang danh sách thu tiền và soạn sẵn tin gửi phụ huynh. Chốt nhầm thì mở lại được.") },
        confirmButton = { TextButton(onClick = onConfirm) { Text("Chốt tháng", fontWeight = FontWeight.Bold) } },
        dismissButton = { TextButton(onClick = onDismiss) { Text("Để sau") } })
}

@Composable
private fun Section(text: String) {
    Text(text, Modifier.padding(start = 4.dp, top = 10.dp), fontSize = 13.sp, fontWeight = FontWeight.Bold, letterSpacing = 1.sp, color = MaterialTheme.colorScheme.onSurfaceVariant)
}

@Composable
private fun Empty() {
    Text("Chưa có buổi nào được bấm Xong trong tháng.", Modifier.padding(vertical = 24.dp, horizontal = 8.dp), style = MaterialTheme.typography.bodyLarge,
        color = MaterialTheme.colorScheme.onSurfaceVariant)
}

@Composable
private fun EstimateCard(total: Long, month: YearMonth, taught: Int) {
    val today = LocalDate.now()
    val current = YearMonth.from(today) == month
    Column(Modifier.fillMaxWidth().clip(RoundedCornerShape(24.dp)).background(Brush.linearGradient(listOf(Color(0xFF6A48F5), Color(0xFFA08BFF)))).padding(20.dp)) {
        Row(verticalAlignment = Alignment.CenterVertically) {
            Text(if (current) "Tạm tính đến hôm nay" else "Tạm tính · chưa chốt", Modifier.weight(1f), color = Color.White.copy(alpha = .92f), fontSize = 16.sp, fontWeight = FontWeight.SemiBold)
            Text("$taught buổi đã dạy", color = Color.White.copy(alpha = .92f), fontSize = 15.sp)
        }
        Text(Format.money(total), color = Color.White, fontSize = 36.sp, fontWeight = FontWeight.Black)
        Text(buildAnnotatedString {
            append("Thu vào "); withStyle(SpanStyle(fontWeight = FontWeight.Bold)) { append("cuối tháng") }
            if (current) append(" · còn " + (month.lengthOfMonth() - today.dayOfMonth) + " ngày")
        }, color = Color.White.copy(alpha = .92f), fontSize = 15.sp)
    }
}

@Composable
private fun CloseNote(month: YearMonth, onClose: () -> Unit) {
    val last = month.atEndOfMonth()
    val due = !LocalDate.now().isBefore(last)
    Row(Modifier.fillMaxWidth().clip(RoundedCornerShape(16.dp)).background(CardBg).padding(start = 16.dp, end = 8.dp, top = 8.dp, bottom = 8.dp),
        verticalAlignment = Alignment.CenterVertically) {
        Icon(Icons.Default.Event, null, tint = MaterialTheme.colorScheme.onSurfaceVariant); Spacer(Modifier.width(12.dp))
        Text(if (due) "Đã tới cuối tháng, cô chốt để gửi bảng học phí." else "Ngày " + Format.shortDay(last.toEpochDay()) + " app nhắc cô chốt tháng và gửi bảng học phí.",
            Modifier.weight(1f).padding(vertical = 6.dp), fontSize = 15.sp, color = MaterialTheme.colorScheme.onSurfaceVariant)
        TextButton(onClick = onClose) { Text(if (due) "Chốt ngay" else "Chốt sớm", fontWeight = FontWeight.Bold) }
    }
}

@Composable
private fun DueCard(unpaid: Long, paid: Long, total: Long, closedAt: Long, onReopen: () -> Unit) {
    var menu by remember { mutableStateOf(false) }
    Column(Modifier.fillMaxWidth().clip(RoundedCornerShape(24.dp)).background(Brush.linearGradient(listOf(Color(0xFFE2568A), Color(0xFFF59AB8)))).padding(20.dp)) {
        Row(verticalAlignment = Alignment.CenterVertically) {
            Text("Còn phải thu · đã chốt ngày " + Format.shortDay(java.time.Instant.ofEpochMilli(closedAt).atZone(java.time.ZoneId.systemDefault()).toLocalDate().toEpochDay()),
                Modifier.weight(1f), color = Color.White.copy(alpha = .95f), fontSize = 16.sp, fontWeight = FontWeight.SemiBold, maxLines = 2, overflow = TextOverflow.Ellipsis)
            Box {
                IconButton(onClick = { menu = true }, Modifier.size(40.dp)) { Icon(Icons.Default.MoreVert, "Tuỳ chọn tháng", tint = Color.White) }
                DropdownMenu(menu, { menu = false }) { DropdownMenuItem(text = { Text("Mở lại tháng") }, onClick = { menu = false; onReopen() }) }
            }
        }
        Text(Format.money(unpaid), color = Color.White, fontSize = 36.sp, fontWeight = FontWeight.Black)
        Spacer(Modifier.height(8.dp))
        val fraction = if (total > 0) (paid.toFloat() / total).coerceIn(0f, 1f) else 0f
        Box(Modifier.fillMaxWidth().height(10.dp).clip(RoundedCornerShape(50)).background(Color.White.copy(alpha = .35f))) {
            Box(Modifier.fillMaxWidth(fraction).height(10.dp).clip(RoundedCornerShape(50)).background(Color.White))
        }
        Spacer(Modifier.height(8.dp))
        Row {
            Text(buildAnnotatedString { append("Đã thu "); withStyle(SpanStyle(fontWeight = FontWeight.Bold)) { append(Format.money(paid)) } },
                Modifier.weight(1f), color = Color.White, fontSize = 15.sp)
            Text(buildAnnotatedString { append("Phải thu "); withStyle(SpanStyle(fontWeight = FontWeight.Bold)) { append(Format.money(total)) } }, color = Color.White, fontSize = 15.sp)
        }
    }
}

private fun subtitle(line: FeeLine, suffix: String) = line.subtitle + " · " + line.present + " buổi" + suffix + if (line.absent > 0) " · vắng " + line.absent else ""

private fun shortDate(millis: Long) = java.text.SimpleDateFormat("dd/MM", java.util.Locale("vi")).format(java.util.Date(millis))

@Composable
private fun LineCard(onOpen: (() -> Unit)?, content: @Composable RowScope.() -> Unit) {
    Row(Modifier.fillMaxWidth().clip(RoundedCornerShape(16.dp)).background(CardBg).clickable(enabled = onOpen != null) { onOpen?.invoke() }
        .padding(start = 16.dp, end = 6.dp, top = 12.dp, bottom = 12.dp), verticalAlignment = Alignment.CenterVertically) { content() }
}

/** Before closing: estimate so far; a parent who pays early can be marked from the ⋯ menu. */
@Composable
private fun RunningRow(line: FeeLine, onOpen: (() -> Unit)?, onPrepaid: (Boolean) -> Unit) {
    var menu by remember { mutableStateOf(false) }
    LineCard(onOpen) {
        Column(Modifier.weight(1f)) {
            Text(line.title, fontSize = 17.sp, fontWeight = FontWeight.Bold, maxLines = 1, overflow = TextOverflow.Ellipsis)
            Text(subtitle(line, if (line.paidAt == null) " đến nay" else ""), fontSize = 14.sp, color = MaterialTheme.colorScheme.onSurfaceVariant)
        }
        Column(horizontalAlignment = Alignment.End) {
            Text(Format.money(line.amount), fontSize = 16.sp, fontWeight = FontWeight.Bold)
            if (line.paidAt != null) Row(verticalAlignment = Alignment.CenterVertically) {
                Icon(Icons.Default.Check, null, tint = Earned, modifier = Modifier.size(16.dp))
                Text(" Đóng trước " + shortDate(line.paidAt), fontSize = 14.sp, color = Earned, fontWeight = FontWeight.SemiBold)
            }
        }
        Box {
            IconButton(onClick = { menu = true }) { Icon(Icons.Default.MoreVert, "Tuỳ chọn") }
            DropdownMenu(menu, { menu = false }) {
                if (line.paidAt == null) DropdownMenuItem(text = { Text("Đã đóng trước") }, onClick = { menu = false; onPrepaid(true) })
                else DropdownMenuItem(text = { Text("Bỏ đánh dấu đã đóng") }, onClick = { menu = false; onPrepaid(false) })
            }
        }
    }
}

@Composable
private fun CollectRow(line: FeeLine, onOpen: () -> Unit, onPaid: () -> Unit) {
    LineCard(onOpen) {
        Column(Modifier.weight(1f)) {
            Text(line.title, fontSize = 17.sp, fontWeight = FontWeight.Bold, maxLines = 1, overflow = TextOverflow.Ellipsis)
            Text(subtitle(line, ""), fontSize = 14.sp, color = MaterialTheme.colorScheme.onSurfaceVariant)
        }
        Text(Format.money(line.amount), Modifier.padding(horizontal = 8.dp), fontSize = 16.sp, fontWeight = FontWeight.Bold)
        FilledTonalButton(onClick = onPaid, modifier = Modifier.heightIn(min = 48.dp), colors = ButtonDefaults.filledTonalButtonColors(
            containerColor = MaterialTheme.colorScheme.primaryContainer, contentColor = Accent)) { Text("Đã thu", fontWeight = FontWeight.Bold, maxLines = 1, softWrap = false) }
    }
}

@Composable
private fun PaidRow(line: FeeLine, onOpen: () -> Unit, onUnpay: () -> Unit) {
    var menu by remember { mutableStateOf(false) }
    LineCard(onOpen) {
        Column(Modifier.weight(1f)) {
            Text(line.title, fontSize = 17.sp, fontWeight = FontWeight.Bold, maxLines = 1, overflow = TextOverflow.Ellipsis, color = MaterialTheme.colorScheme.onSurface.copy(alpha = .8f))
            Text(subtitle(line, ""), fontSize = 14.sp, color = MaterialTheme.colorScheme.onSurfaceVariant)
        }
        Column(horizontalAlignment = Alignment.End) {
            Text(Format.money(line.amount), fontSize = 16.sp, fontWeight = FontWeight.Bold)
            Row(verticalAlignment = Alignment.CenterVertically) {
                Icon(Icons.Default.Check, null, tint = Earned, modifier = Modifier.size(16.dp))
                Text(" Thu ngày " + shortDate(line.paidAt!!), fontSize = 14.sp, color = Earned, fontWeight = FontWeight.SemiBold)
            }
        }
        Box {
            IconButton(onClick = { menu = true }) { Icon(Icons.Default.MoreVert, "Tuỳ chọn") }
            DropdownMenu(menu, { menu = false }) { DropdownMenuItem(text = { Text("Bỏ đánh dấu đã thu") }, onClick = { menu = false; onUnpay() }) }
        }
    }
}

/** One prepared message per unpaid line; the teacher picks Zalo, SMS or copy. The app never sends anything itself. */
@OptIn(ExperimentalMaterial3Api::class)
@Composable
private fun ShareSheet(data: AppData, lines: List<FeeLine>, month: YearMonth, onClose: () -> Unit) {
    val context = LocalContext.current
    ModalBottomSheet(onDismissRequest = onClose, containerColor = CardBg) {
        Column(Modifier.padding(horizontal = 20.dp).navigationBarsPadding()) {
            Text("Gửi bảng học phí", style = MaterialTheme.typography.titleLarge, fontWeight = FontWeight.Bold)
            Text("Bấm Gửi ở từng bạn rồi chọn Zalo, tin nhắn hoặc sao chép.", style = MaterialTheme.typography.bodyMedium, color = MaterialTheme.colorScheme.onSurfaceVariant)
            Spacer(Modifier.height(8.dp))
            LazyColumn(Modifier.heightIn(max = 480.dp)) {
                items(lines.sortedWith(compareBy(Format.byGivenName) { it.title }), key = { it.key }) { line ->
                    val text = Rules.feeMessage(data, line, month)
                    Row(Modifier.fillMaxWidth().padding(vertical = 10.dp), verticalAlignment = Alignment.CenterVertically) {
                        Column(Modifier.weight(1f)) {
                            Text(line.title, fontWeight = FontWeight.Bold, fontSize = 16.sp)
                            Text(text, fontSize = 14.sp, color = MaterialTheme.colorScheme.onSurfaceVariant)
                        }
                        Spacer(Modifier.width(8.dp))
                        FilledTonalButton(onClick = {
                            context.startActivity(Intent.createChooser(Intent(Intent.ACTION_SEND).setType("text/plain").putExtra(Intent.EXTRA_TEXT, text), "Gửi học phí " + line.title))
                        }, modifier = Modifier.heightIn(min = 48.dp), colors = ButtonDefaults.filledTonalButtonColors(
                            containerColor = MaterialTheme.colorScheme.primaryContainer, contentColor = Accent)) { Text("Gửi", fontWeight = FontWeight.Bold) }
                    }
                    HorizontalDivider()
                }
            }
            Spacer(Modifier.height(16.dp))
        }
    }
}
