package vn.mrsbi.handout.ui

import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.navigationBarsPadding
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.KeyboardArrowLeft
import androidx.compose.material.icons.automirrored.filled.KeyboardArrowRight
import androidx.compose.material.icons.filled.CheckCircle
import androidx.compose.material.icons.filled.EventRepeat
import androidx.compose.material3.AssistChip
import androidx.compose.material3.Button
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.ModalBottomSheet
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.Switch
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableLongStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import vn.mrsbi.handout.data.AppData
import vn.mrsbi.handout.data.ClassType
import vn.mrsbi.handout.data.Occurrence
import vn.mrsbi.handout.data.Rules
import vn.mrsbi.handout.data.Store
import vn.mrsbi.handout.data.markDone
import vn.mrsbi.handout.data.postpone
import vn.mrsbi.handout.data.reopen
import vn.mrsbi.handout.util.Format

/** Main screen: the classes of the chosen day (default today) with their student counts. */
@Composable
fun TodayScreen(store: Store, data: AppData, openManage: () -> Unit) {
    var day by rememberSaveable { mutableLongStateOf(Format.today()) }
    var opened by remember { mutableStateOf<String?>(null) }
    val list = Rules.occurrencesOn(data, day)
    val earned = list.mapNotNull { it.record }.sumOf { Rules.earned(it) }
    Column(Modifier.fillMaxSize()) {
        Row(Modifier.fillMaxWidth().padding(horizontal = 8.dp, vertical = 8.dp), verticalAlignment = Alignment.CenterVertically) {
            IconButton(onClick = { day-- }) { Icon(Icons.AutoMirrored.Filled.KeyboardArrowLeft, "Hôm trước") }
            Column(Modifier.weight(1f), horizontalAlignment = Alignment.CenterHorizontally) {
                Text(if (day == Format.today()) "Hôm nay" else Format.weekday(day), style = MaterialTheme.typography.titleLarge, fontWeight = FontWeight.Bold)
                Text(Format.day(day), style = MaterialTheme.typography.bodyMedium, color = MaterialTheme.colorScheme.onSurfaceVariant)
            }
            IconButton(onClick = { day++ }) { Icon(Icons.AutoMirrored.Filled.KeyboardArrowRight, "Hôm sau") }
        }
        if (day != Format.today()) TextButton(onClick = { day = Format.today() }, modifier = Modifier.align(Alignment.CenterHorizontally)) { Text("Về hôm nay") }
        Text(
            "${list.size} lớp · ${list.sumOf { studentsOf(it) }} học sinh · đã dạy ${list.count { it.done }}/${list.size}" + if (earned > 0) " · " + Format.money(earned) else "",
            Modifier.padding(horizontal = 16.dp), style = MaterialTheme.typography.labelLarge, color = MaterialTheme.colorScheme.onSurfaceVariant,
        )
        if (list.isEmpty()) {
            Box(Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
                Column(horizontalAlignment = Alignment.CenterHorizontally) {
                    Text("Không có lớp nào trong ngày", style = MaterialTheme.typography.bodyLarge)
                    if (data.classes.isEmpty()) { Spacer(Modifier.height(8.dp)); Button(onClick = openManage) { Text("Thêm lớp đầu tiên") } }
                }
            }
        } else LazyColumn(Modifier.fillMaxSize().padding(horizontal = 12.dp), verticalArrangement = Arrangement.spacedBy(10.dp)) {
            item { Spacer(Modifier.height(4.dp)) }
            items(list, key = { it.key }) { o -> SessionCard(o, onClick = { opened = o.key }) }
            item { Spacer(Modifier.height(12.dp)) }
        }
    }
    val current = list.firstOrNull { it.key == opened }
    if (current != null) AttendanceSheet(store, data, current, onClose = { opened = null })
}

fun studentsOf(o: Occurrence) = if (o.group.type == ClassType.CENTER && o.group.members.isEmpty()) 0 else o.group.members.size

@Composable
private fun SessionCard(o: Occurrence, onClick: () -> Unit) {
    val color = Color(o.group.color)
    Card(
        Modifier.fillMaxWidth().clickable(onClick = onClick),
        colors = CardDefaults.cardColors(containerColor = if (o.done) MaterialTheme.colorScheme.surfaceVariant else Color.White),
        shape = RoundedCornerShape(16.dp), elevation = CardDefaults.cardElevation(2.dp),
    ) {
        Row(Modifier.padding(14.dp), verticalAlignment = Alignment.CenterVertically) {
            Box(Modifier.width(6.dp).height(52.dp).clip(RoundedCornerShape(3.dp)).background(color))
            Spacer(Modifier.width(12.dp))
            Column(Modifier.weight(1f)) {
                Text(Format.range(o.startMinute, o.group.durationMin), style = MaterialTheme.typography.labelLarge, color = color, fontWeight = FontWeight.Bold)
                Text(o.group.name, style = MaterialTheme.typography.titleMedium, fontWeight = FontWeight.SemiBold, maxLines = 1, overflow = TextOverflow.Ellipsis)
                val type = if (o.group.type == ClassType.CENTER) "Trung tâm" else "Cá nhân"
                Text(listOf(o.group.content, type).filter { it.isNotBlank() }.joinToString(" · ") + if (o.moved) " · dời từ " + Format.shortDay(o.originalDay) else "",
                    style = MaterialTheme.typography.bodySmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
            }
            Column(horizontalAlignment = Alignment.CenterHorizontally) {
                Box(Modifier.size(44.dp).clip(CircleShape).background(color.copy(alpha = .15f)), contentAlignment = Alignment.Center) {
                    Text(studentsOf(o).toString(), color = color, fontWeight = FontWeight.Bold, style = MaterialTheme.typography.titleMedium)
                }
                Text("HS", style = MaterialTheme.typography.labelSmall)
            }
            if (o.done) { Spacer(Modifier.width(8.dp)); Icon(Icons.Default.CheckCircle, "Đã dạy", tint = Color(0xFF10B981)) }
        }
    }
}

/** Attendance: everyone present by default; "Xong buổi" books the money, "Hoãn" moves only this session. */
@OptIn(ExperimentalMaterial3Api::class)
@Composable
private fun AttendanceSheet(store: Store, data: AppData, o: Occurrence, onClose: () -> Unit) {
    val names = data.students.associateBy { it.id }
    var absent by remember(o.key) { mutableStateOf(o.record?.absent?.toSet() ?: emptySet()) }
    var pickDay by remember { mutableStateOf(false) }
    var newDay by remember { mutableLongStateOf(o.day) }
    var pickTime by remember { mutableStateOf(false) }
    val amount = Rules.charges(o.group, absent).values.sum()
    ModalBottomSheet(onDismissRequest = onClose) {
        Column(Modifier.padding(horizontal = 20.dp).navigationBarsPadding()) {
            Text(o.group.name, style = MaterialTheme.typography.titleLarge, fontWeight = FontWeight.Bold)
            Text(Format.fullDay(o.day) + " · " + Format.range(o.startMinute, o.group.durationMin), color = MaterialTheme.colorScheme.onSurfaceVariant)
            Spacer(Modifier.height(12.dp))
            if (o.group.members.isEmpty()) Text("Lớp chưa có học sinh. Thêm ở tab Quản lý.", style = MaterialTheme.typography.bodyMedium)
            LazyColumn(Modifier.fillMaxWidth().height((56 * o.group.members.size.coerceIn(1, 7)).dp)) {
                items(o.group.members, key = { it.studentId }) { m ->
                    val present = m.studentId !in absent
                    Row(Modifier.fillMaxWidth().clickable(enabled = !o.done) { absent = if (present) absent + m.studentId else absent - m.studentId }.padding(vertical = 6.dp),
                        verticalAlignment = Alignment.CenterVertically) {
                        Column(Modifier.weight(1f)) {
                            Text(names[m.studentId]?.name ?: "(đã xoá)", style = MaterialTheme.typography.bodyLarge)
                            if (o.group.type == ClassType.PERSONAL) Text(Format.money(m.fee) + "/buổi", style = MaterialTheme.typography.bodySmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
                        }
                        Text(if (present) "Có mặt" else "Vắng", color = if (present) Color(0xFF10B981) else Color(0xFFEF4444), style = MaterialTheme.typography.labelLarge)
                        Spacer(Modifier.width(8.dp))
                        Switch(checked = present, enabled = !o.done, onCheckedChange = { absent = if (it) absent - m.studentId else absent + m.studentId })
                    }
                    HorizontalDivider()
                }
            }
            Spacer(Modifier.height(12.dp))
            if (o.done) {
                AssistChip(onClick = {}, label = { Text("Đã dạy · " + Format.money(Rules.earned(o.record!!))) }, leadingIcon = { Icon(Icons.Default.CheckCircle, null) })
                OutlinedButton(onClick = { store.reopen(o); onClose() }, modifier = Modifier.fillMaxWidth()) { Text("Mở lại buổi (bấm nhầm)") }
            } else {
                Button(onClick = { store.markDone(o, absent); onClose() }, modifier = Modifier.fillMaxWidth().height(52.dp)) {
                    Text("Xong buổi · " + Format.money(amount), style = MaterialTheme.typography.titleMedium)
                }
                Spacer(Modifier.height(8.dp))
                OutlinedButton(onClick = { pickDay = true }, modifier = Modifier.fillMaxWidth()) {
                    Icon(Icons.Default.EventRepeat, null); Spacer(Modifier.width(8.dp)); Text("Hoãn sang ngày khác")
                }
            }
            Spacer(Modifier.height(20.dp))
        }
    }
    if (pickDay) DayPickerDialog(o.day, onDismiss = { pickDay = false }) { newDay = it; pickDay = false; pickTime = true }
    if (pickTime) TimePickerDialog(o.startMinute, onDismiss = { pickTime = false }) { minute -> pickTime = false; store.postpone(o, newDay, minute); onClose() }
}
