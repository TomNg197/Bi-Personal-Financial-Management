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
import androidx.compose.material.icons.filled.Close
import androidx.compose.material.icons.filled.PersonAdd
import androidx.compose.material3.Checkbox
import vn.mrsbi.handout.data.Member
import vn.mrsbi.handout.data.Student
import vn.mrsbi.handout.data.saveClass
import vn.mrsbi.handout.data.setGuests
import androidx.compose.material.icons.filled.Place
import androidx.compose.material.icons.filled.Wifi
import vn.mrsbi.handout.data.ClassGroup
import vn.mrsbi.handout.util.Format

/** Main screen: the classes of the chosen day (default today) with their student counts. */
@Composable
fun TodayScreen(store: Store, data: AppData, openManage: () -> Unit) {
    var day by rememberSaveable { mutableLongStateOf(Format.today()) }
    var opened by remember { mutableStateOf<String?>(null) }
    val list = Rules.occurrencesOn(data, day)
    val earned = list.mapNotNull { it.record }.sumOf { Rules.earned(it) }
    Column(Modifier.fillMaxSize()) {
        Row(Modifier.fillMaxWidth().padding(start = 16.dp, end = 4.dp, top = 12.dp), verticalAlignment = Alignment.CenterVertically) {
            Column(Modifier.weight(1f)) {
                Text(if (day == Format.today()) "Hôm nay" else Format.weekday(day), style = MaterialTheme.typography.headlineMedium, fontWeight = FontWeight.ExtraBold, color = Violet)
                Text(Format.day(day), style = MaterialTheme.typography.bodyMedium, color = Muted)
            }
            IconButton(onClick = { day-- }) { Icon(Icons.AutoMirrored.Filled.KeyboardArrowLeft, "Hôm trước") }
            IconButton(onClick = { day++ }) { Icon(Icons.AutoMirrored.Filled.KeyboardArrowRight, "Hôm sau") }
        }
        if (day != Format.today()) TextButton(onClick = { day = Format.today() }, modifier = Modifier.padding(start = 4.dp)) { Text("← Về hôm nay") }
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
        } else LazyColumn(Modifier.fillMaxSize().padding(horizontal = 14.dp), verticalArrangement = Arrangement.spacedBy(12.dp)) {
            item { Spacer(Modifier.height(4.dp)) }
            items(list, key = { it.key }) { o -> SessionCard(o, onClick = { opened = o.key }) }
            item { Spacer(Modifier.height(12.dp)) }
        }
    }
    val current = list.firstOrNull { it.key == opened }
    if (current != null) AttendanceSheet(store, data, current, onClose = { opened = null })
}

fun studentsOf(o: Occurrence) = o.group.members.size + (o.record?.guests?.keys?.count { id -> o.group.members.none { it.studentId == id } } ?: 0)

/** Each class is a colour board (its own colour, soft gradient) so classes are easy to tell apart at a glance. */
@Composable
private fun SessionCard(o: Occurrence, onClick: () -> Unit) {
    val color = Color(o.group.color)
    val brush = androidx.compose.ui.graphics.Brush.linearGradient(listOf(color, lerpColor(color, Color.White, .28f)))
    Card(
        Modifier.fillMaxWidth().clickable(onClick = onClick), shape = RoundedCornerShape(22.dp),
        colors = CardDefaults.cardColors(containerColor = Color.Transparent), elevation = CardDefaults.cardElevation(if (o.done) 0.dp else 3.dp),
    ) {
        Box(Modifier.fillMaxWidth().background(brush).padding(16.dp)) {
            Column(Modifier.fillMaxWidth().padding(end = 64.dp)) {
                Text(Format.range(o.startMinute, o.durationMin), color = Color.White.copy(alpha = .9f), style = MaterialTheme.typography.labelLarge, fontWeight = FontWeight.Bold)
                Text(o.group.name, color = Color.White, style = MaterialTheme.typography.titleLarge, fontWeight = FontWeight.ExtraBold, maxLines = 1, overflow = TextOverflow.Ellipsis)
                val type = if (o.group.type == ClassType.CENTER) "Trung tâm" else "Cá nhân"
                Text(listOf(o.group.content, type).filter { it.isNotBlank() }.joinToString(" · ") + if (o.moved) " · dời từ " + Format.shortDay(o.originalDay) else "",
                    color = Color.White.copy(alpha = .85f), style = MaterialTheme.typography.bodyMedium, maxLines = 1, overflow = TextOverflow.Ellipsis)
                Spacer(Modifier.height(8.dp))
                PlaceTag(o.group, color)
                Spacer(Modifier.height(8.dp))
                Box(Modifier.clip(RoundedCornerShape(50)).background(Color.White.copy(alpha = .25f)).padding(horizontal = 12.dp, vertical = 5.dp)) {
                    Text(if (o.done) "✓ Đã dạy · " + Format.money(Rules.earned(o.record!!)) else "Chạm để điểm danh", color = Color.White,
                        style = MaterialTheme.typography.labelLarge, fontWeight = FontWeight.SemiBold)
                }
            }
            Column(Modifier.align(Alignment.CenterEnd), horizontalAlignment = Alignment.CenterHorizontally) {
                Box(Modifier.size(54.dp).clip(CircleShape).background(Color.White), contentAlignment = Alignment.Center) {
                    Text(studentsOf(o).toString(), color = color, fontWeight = FontWeight.ExtraBold, style = MaterialTheme.typography.titleLarge)
                }
                Text("học sinh", color = Color.White, style = MaterialTheme.typography.labelSmall)
            }
            if (o.done) Box(Modifier.matchParentSize().background(Color.White.copy(alpha = .25f)))
        }
    }
}

fun lerpColor(a: Color, b: Color, t: Float) = Color(a.red + (b.red - a.red) * t, a.green + (b.green - a.green) * t, a.blue + (b.blue - a.blue) * t, 1f)

fun initials(name: String): String {
    val words = name.trim().split(Regex("\\s+")).filter { it.isNotEmpty() }
    return when { words.isEmpty() -> "?"; words.size == 1 -> words[0].take(2).uppercase(); else -> (words[0].take(1) + words[1].take(1)).uppercase() }
}

/** Attendance: everyone present by default; "Xong buổi" books the money, "Hoãn" moves only this session. */
@OptIn(ExperimentalMaterial3Api::class)
@Composable
private fun AttendanceSheet(store: Store, data: AppData, o: Occurrence, onClose: () -> Unit) {
    val names = data.students.associateBy { it.id }
    var absent by remember(o.key) { mutableStateOf(o.record?.absent?.toSet() ?: emptySet()) }
    var guests by remember(o.key) { mutableStateOf(o.record?.guests ?: emptyMap()) }
    var pickDay by remember { mutableStateOf(false) }
    var newDay by remember { mutableLongStateOf(o.day) }
    var pickTime by remember { mutableStateOf(false) }
    var pickGuest by remember { mutableStateOf(false) }
    // Class members first, then this session's "học ghép" students.
    val rows = o.group.members.map { Triple(it.studentId, it.fee, false) } +
        guests.filterKeys { id -> o.group.members.none { it.studentId == id } }.map { Triple(it.key, it.value, true) }
    val amount = Rules.charges(o.group, absent, guests).values.sum()
    ModalBottomSheet(onDismissRequest = onClose) {
        Column(Modifier.padding(horizontal = 20.dp).navigationBarsPadding()) {
            Text(o.group.name, style = MaterialTheme.typography.titleLarge, fontWeight = FontWeight.Bold)
            Text(Format.fullDay(o.day) + " · " + Format.range(o.startMinute, o.durationMin), color = MaterialTheme.colorScheme.onSurfaceVariant)
            Spacer(Modifier.height(6.dp))
            PlaceTag(o.group, Color(o.group.color), onBoard = false)
            Spacer(Modifier.height(4.dp))
            Text("Có mặt ${rows.count { it.first !in absent }}/${rows.size}", style = MaterialTheme.typography.labelLarge, color = Violet)
            Spacer(Modifier.height(8.dp))
            if (rows.isEmpty()) Text("Lớp chưa có học sinh. Thêm ở tab Quản lý hoặc bấm “Thêm học sinh học ghép”.", style = MaterialTheme.typography.bodyMedium)
            LazyColumn(Modifier.fillMaxWidth().height((60 * rows.size.coerceIn(1, 7)).dp)) {
                items(rows, key = { it.first }) { (id, fee, guest) ->
                    val present = id !in absent
                    Row(Modifier.fillMaxWidth().clickable(enabled = !o.done) { absent = if (present) absent + id else absent - id }.padding(vertical = 6.dp),
                        verticalAlignment = Alignment.CenterVertically) {
                        Column(Modifier.weight(1f)) {
                            Text(names[id]?.name ?: "(đã xoá)", style = MaterialTheme.typography.bodyLarge)
                            val info = listOfNotNull(if (guest) "học ghép" else null, if (o.group.type == ClassType.PERSONAL) Format.money(fee) + "/buổi" else null)
                            if (info.isNotEmpty()) Text(info.joinToString(" · "), style = MaterialTheme.typography.bodySmall, color = if (guest) Rose else MaterialTheme.colorScheme.onSurfaceVariant)
                        }
                        if (guest && !o.done) IconButton(onClick = { guests = guests - id; absent = absent - id; store.setGuests(o, guests) }) {
                            Icon(Icons.Default.Close, "Bỏ học ghép", tint = Muted)
                        }
                        Text(if (present) "Có mặt" else "Vắng", color = if (present) Color(0xFF10B981) else Color(0xFFEF4444), style = MaterialTheme.typography.labelLarge)
                        Spacer(Modifier.width(8.dp))
                        Switch(checked = present, enabled = !o.done, onCheckedChange = { absent = if (it) absent - id else absent + id })
                    }
                    HorizontalDivider()
                }
            }
            if (!o.done) TextButton(onClick = { pickGuest = true }) {
                Icon(Icons.Default.PersonAdd, null); Spacer(Modifier.width(8.dp)); Text("Thêm học sinh học ghép")
            }
            Spacer(Modifier.height(8.dp))
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
    if (pickGuest) GuestPicker(data, exclude = rows.map { it.first }.toSet(), onDismiss = { pickGuest = false }) { student, keep ->
        pickGuest = false
        if (keep) store.saveClass(o.group.copy(members = o.group.members + Member(student.id, student.fee)))
        else { guests = guests + (student.id to student.fee); store.setGuests(o, guests) }
    }
    if (pickDay) DayPickerDialog(o.day, onDismiss = { pickDay = false }) { newDay = it; pickDay = false; pickTime = true }
    if (pickTime) TimePickerDialog(o.startMinute, onDismiss = { pickTime = false }) { minute -> pickTime = false; store.postpone(o, newDay, minute); onClose() }
}

/** Pick an existing student to join this session ("học ghép"), or to join the class for good. */
@Composable
private fun GuestPicker(data: AppData, exclude: Set<String>, onDismiss: () -> Unit, onPick: (Student, Boolean) -> Unit) {
    var keep by remember { mutableStateOf(false) }
    StudentPickerDialog(data, exclude, "Thêm học sinh học ghép", onDismiss, header = {
        Row(Modifier.fillMaxWidth().clickable { keep = !keep }.padding(vertical = 4.dp), verticalAlignment = Alignment.CenterVertically) {
            Checkbox(keep, { keep = it })
            Text("Thêm hẳn vào lớp (các buổi sau)", style = MaterialTheme.typography.bodyMedium)
        }
    }) { onPick(it, keep) }
}

/** Highlight tag: Online, or Offline with the address next to it. */
@Composable
fun PlaceTag(group: ClassGroup, color: Color, onBoard: Boolean = true) {
    Row(verticalAlignment = Alignment.CenterVertically) {
        val bg = if (onBoard) Color.White else color
        val fg = if (onBoard) color else Color.White
        Row(Modifier.clip(RoundedCornerShape(8.dp)).background(bg).padding(horizontal = 8.dp, vertical = 3.dp), verticalAlignment = Alignment.CenterVertically) {
            Icon(if (group.online) Icons.Default.Wifi else Icons.Default.Place, null, tint = fg, modifier = Modifier.size(14.dp))
            Spacer(Modifier.width(4.dp))
            Text(if (group.online) "ONLINE" else "OFFLINE", color = fg, style = MaterialTheme.typography.labelMedium, fontWeight = FontWeight.ExtraBold)
        }
        if (!group.online && group.address.isNotBlank()) {
            Spacer(Modifier.width(6.dp))
            Text(group.address, color = if (onBoard) Color.White else MaterialTheme.colorScheme.onSurfaceVariant, style = MaterialTheme.typography.bodySmall,
                fontWeight = FontWeight.SemiBold, maxLines = 1, overflow = TextOverflow.Ellipsis)
        }
    }
}
