package vn.mrsbi.handout.ui

import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.ExperimentalLayoutApi
import androidx.compose.foundation.layout.FlowRow
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.imePadding
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Add
import androidx.compose.material.icons.filled.Close
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Button
import androidx.compose.material3.Checkbox
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.ExtendedFloatingActionButton
import androidx.compose.material3.FilterChip
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.SegmentedButton
import androidx.compose.material3.SegmentedButtonDefaults
import androidx.compose.material3.SingleChoiceSegmentedButtonRow
import androidx.compose.material3.Surface
import androidx.compose.material3.Switch
import androidx.compose.material3.Tab
import androidx.compose.material3.TabRow
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.unit.dp
import androidx.compose.ui.window.Dialog
import androidx.compose.ui.window.DialogProperties
import vn.mrsbi.handout.data.AppData
import vn.mrsbi.handout.data.ClassGroup
import vn.mrsbi.handout.data.ClassType
import androidx.compose.material.icons.filled.Check
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.ui.semantics.contentDescription
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.unit.sp
import vn.mrsbi.handout.data.Member
import vn.mrsbi.handout.data.DayTime
import vn.mrsbi.handout.data.Store
import vn.mrsbi.handout.data.Student
import vn.mrsbi.handout.data.deleteClass
import vn.mrsbi.handout.data.deleteStudent
import vn.mrsbi.handout.data.saveClass
import vn.mrsbi.handout.data.saveStudent
import vn.mrsbi.handout.util.Format

@Composable
fun ManageScreen(store: Store, data: AppData) {
    var tab by rememberSaveable { mutableIntStateOf(0) }
    var editStudent by remember { mutableStateOf<Student?>(null) }
    var editClass by remember { mutableStateOf<ClassGroup?>(null) }
    Box(Modifier.fillMaxSize()) {
        Column(Modifier.fillMaxSize()) {
            TabRow(selectedTabIndex = tab) {
                Tab(tab == 0, onClick = { tab = 0 }, text = { Text("Lớp (${data.classes.count { !it.archived }})") })
                Tab(tab == 1, onClick = { tab = 1 }, text = { Text("Học sinh (${data.students.count { !it.archived }})") })
            }
            if (tab == 0) LazyColumn(Modifier.fillMaxSize()) {
                items(data.classes.filter { !it.archived }, key = { it.id }) { c ->
                    Row(Modifier.fillMaxWidth().clickable { editClass = c }.padding(16.dp), verticalAlignment = Alignment.CenterVertically) {
                        Box(Modifier.size(42.dp).clip(CircleShape).background(Color(c.color)), contentAlignment = Alignment.Center) {
                            Text(initials(c.name), color = Color.White, fontWeight = FontWeight.Bold)
                        }
                        Spacer(Modifier.width(12.dp))
                        Column(Modifier.weight(1f)) {
                            Text(c.name, fontWeight = FontWeight.SemiBold)
                            Text(scheduleText(c) + " · " + (if (c.type == ClassType.CENTER) "Trung tâm " + Format.money(c.centerFee) + "/buổi" else "Cá nhân · " + c.members.size + " HS"),
                                style = MaterialTheme.typography.bodySmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
                        }
                    }
                    HorizontalDivider(color = Hairline)
                }
                item { Spacer(Modifier.height(96.dp)) }
            } else LazyColumn(Modifier.fillMaxSize()) {
                items(data.students.filter { !it.archived }.sortedWith(compareBy(Format.byGivenName) { it.name }), key = { it.id }) { s ->
                    // Avatar in the colour of the student's first class.
                    val tint = data.classes.firstOrNull { c -> !c.archived && c.members.any { it.studentId == s.id } }?.let { Color(it.color) } ?: Accent
                    Row(Modifier.fillMaxWidth().clickable { editStudent = s }.padding(16.dp), verticalAlignment = Alignment.CenterVertically) {
                        Box(Modifier.size(42.dp).clip(CircleShape).background(tint.copy(alpha = .16f)), contentAlignment = Alignment.Center) {
                            Text(initials(s.name), color = tint, fontWeight = FontWeight.Bold)
                        }
                        Spacer(Modifier.width(12.dp))
                        Column(Modifier.weight(1f)) {
                            Text(s.name, fontWeight = FontWeight.SemiBold)
                            val classes = data.classes.filter { c -> !c.archived && c.members.any { it.studentId == s.id } }.joinToString(", ") { it.name }
                            Text(listOfNotNull(s.age?.let { "$it tuổi" }, Format.money(s.fee) + "/buổi", classes.ifBlank { null }).joinToString(" · "),
                                style = MaterialTheme.typography.bodySmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
                        }
                    }
                    HorizontalDivider(color = Hairline)
                }
                item { Spacer(Modifier.height(96.dp)) }
            }
        }
        ExtendedFloatingActionButton(
            onClick = {
                if (tab == 0) editClass = ClassGroup(Store.newId(), "", startDay = Format.today(), color = ClassColors[data.classes.size % ClassColors.size])
                else editStudent = Student(Store.newId(), "")
            },
            modifier = Modifier.align(Alignment.BottomEnd).padding(16.dp),
            icon = { Icon(Icons.Default.Add, null) }, text = { Text(if (tab == 0) "Thêm lớp" else "Thêm học sinh") },
        )
    }
    editStudent?.let { s -> StudentEditor(s, isNew = data.students.none { it.id == s.id }, onSave = { store.saveStudent(it); editStudent = null },
        onDelete = { store.deleteStudent(s.id); editStudent = null }, onClose = { editStudent = null }) }
    editClass?.let { c -> ClassEditor(c, data, isNew = data.classes.none { it.id == c.id }, onSave = { store.saveClass(it); editClass = null },
        onDelete = { store.deleteClass(c.id); editClass = null }, onClose = { editClass = null }) }
}

/** Turning a weekday on copies the time of the last chosen day; turning it off drops its own time. */
private fun ClassGroup.toggleDay(d: Int): ClassGroup =
    if (d in weekdays) copy(weekdays = weekdays - d, dayTimes = dayTimes - d)
    else copy(weekdays = (weekdays + d).sorted(), dayTimes = dayTimes + (d to (weekdays.lastOrNull()?.let { timeOn(it) } ?: DayTime(startMinute, durationMin))))

/** [day] 0 edits the single time of a one-off class; otherwise that weekday's own time. The first weekday also sets the class default. */
private fun ClassGroup.withTime(day: Int, t: DayTime): ClassGroup {
    if (day == 0) return copy(startMinute = t.start, durationMin = t.duration)
    val times = weekdays.associateWith { timeOn(it) } + (day to t)
    val first = times.getValue(weekdays.sorted().first())
    return copy(dayTimes = times, startMinute = first.start, durationMin = first.duration)
}

fun scheduleText(c: ClassGroup) =
    if (c.repeat) c.weekdays.sorted().groupBy { c.timeOn(it).start }.entries.joinToString(" · ") { (start, days) -> days.joinToString(", ") { Format.weekdayShort(it) } + " " + Format.time(start) }
    else "Buổi lẻ " + Format.shortDay(c.startDay) + " " + Format.time(c.startMinute)

@Composable
private fun MoneyField(label: String, value: Long, modifier: Modifier = Modifier, onChange: (Long) -> Unit) {
    var text by remember { mutableStateOf(Format.moneyInput(value)) }
    OutlinedTextField(
        value = text, onValueChange = { t -> text = t; Format.parseMoney(t)?.let(onChange) }, label = { Text(label) }, suffix = { Text("đ") },
        singleLine = true, keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number), modifier = modifier,
        supportingText = { Text("Gõ 150k hoặc 150000") },
    )
}

@Composable
private fun StudentEditor(student: Student, isNew: Boolean, onSave: (Student) -> Unit, onDelete: () -> Unit, onClose: () -> Unit) {
    var name by remember { mutableStateOf(student.name) }
    var age by remember { mutableStateOf(student.age?.toString() ?: "") }
    var fee by remember { mutableStateOf(student.fee) }
    var note by remember { mutableStateOf(student.note) }
    var confirmDelete by remember { mutableStateOf(false) }
    AlertDialog(
        onDismissRequest = onClose,
        title = { Text(if (isNew) "Thêm học sinh" else "Sửa học sinh") },
        text = {
            Column(Modifier.verticalScroll(rememberScrollState()), verticalArrangement = Arrangement.spacedBy(8.dp)) {
                OutlinedTextField(name, { name = it }, label = { Text("Họ tên") }, singleLine = true)
                OutlinedTextField(age, { age = it.filter(Char::isDigit).take(3) }, label = { Text("Tuổi") }, singleLine = true,
                    keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number))
                MoneyField("Học phí mặc định / buổi", fee) { fee = it }
                OutlinedTextField(note, { note = it }, label = { Text("Ghi chú (miễn giảm, phụ huynh…)") })
                if (!isNew) TextButton(onClick = { confirmDelete = true }) { Text("Xoá học sinh", color = Color(0xFFEF4444)) }
            }
        },
        confirmButton = { TextButton(enabled = name.isNotBlank(), onClick = { onSave(student.copy(name = name.trim(), age = age.toIntOrNull(), fee = fee, note = note.trim())) }) { Text("Lưu") } },
        dismissButton = { TextButton(onClick = onClose) { Text("Huỷ") } },
    )
    if (confirmDelete) AlertDialog(onDismissRequest = { confirmDelete = false },
        title = { Text("Xoá " + name + "?") }, text = { Text("Lịch sử học phí đã có vẫn được giữ trong báo cáo.") },
        confirmButton = { TextButton(onClick = onDelete) { Text("Xoá", color = Color(0xFFEF4444)) } },
        dismissButton = { TextButton(onClick = { confirmDelete = false }) { Text("Huỷ") } })
}

@OptIn(ExperimentalMaterial3Api::class, ExperimentalLayoutApi::class)
@Composable
internal fun ClassEditor(
    group: ClassGroup, data: AppData, isNew: Boolean, onSave: (ClassGroup) -> Unit, onDelete: () -> Unit, onClose: () -> Unit,
) {
    var c by remember { mutableStateOf(group) }
    var pickStart by remember { mutableStateOf(false) }
    var pickEnd by remember { mutableStateOf(false) }
    var pickTime by remember { mutableStateOf(false) }
    var pickEndTime by remember { mutableStateOf(false) }
    // Weekday whose own time is being edited (0 = the single time of a one-off class).
    var timeDay by remember { mutableIntStateOf(0) }
    var pickStudent by remember { mutableStateOf(false) }
    var confirmDelete by remember { mutableStateOf(false) }
    val names = data.students.associateBy { it.id }
    Dialog(onDismissRequest = onClose, properties = DialogProperties(usePlatformDefaultWidth = false)) {
        Surface(Modifier.fillMaxSize(), color = MaterialTheme.colorScheme.background) {
            Column(Modifier.fillMaxSize().imePadding()) {
                Row(Modifier.fillMaxWidth().padding(8.dp), verticalAlignment = Alignment.CenterVertically) {
                    IconButton(onClick = onClose) { Icon(Icons.Default.Close, "Đóng") }
                    Text(if (isNew) "Thêm lớp" else "Sửa lớp", Modifier.weight(1f), style = MaterialTheme.typography.titleLarge)
                    Button(enabled = c.name.isNotBlank() && (!c.repeat || c.weekdays.isNotEmpty()), onClick = { onSave(c.copy(name = c.name.trim())) }) { Text("Lưu") }
                }
                Column(Modifier.fillMaxSize().verticalScroll(rememberScrollState()).padding(16.dp), verticalArrangement = Arrangement.spacedBy(12.dp)) {
                    OutlinedTextField(c.name, { c = c.copy(name = it) }, label = { Text("Tên lớp") }, singleLine = true, modifier = Modifier.fillMaxWidth())
                    OutlinedTextField(c.content, { c = c.copy(content = it) }, label = { Text("Nội dung dạy (VD: IELTS, lớp trung tâm)") }, singleLine = true, modifier = Modifier.fillMaxWidth())
                    OutlinedTextField(c.shortName, { c = c.copy(shortName = it.take(12)) }, label = { Text("Tên ngắn trên thời khoá biểu (VD: Toán 5A)") }, singleLine = true,
                        modifier = Modifier.fillMaxWidth())
                    SingleChoiceSegmentedButtonRow(Modifier.fillMaxWidth()) {
                        ClassType.entries.forEachIndexed { i, t ->
                            SegmentedButton(selected = c.type == t, onClick = { c = c.copy(type = t) }, shape = SegmentedButtonDefaults.itemShape(i, 2)) {
                                Text(if (t == ClassType.PERSONAL) "Cá nhân (theo HS)" else "Trung tâm (theo buổi)")
                            }
                        }
                    }
                    if (c.type == ClassType.CENTER) MoneyField("Giá mỗi buổi", c.centerFee, Modifier.fillMaxWidth()) { c = c.copy(centerFee = it) }
                    SingleChoiceSegmentedButtonRow(Modifier.fillMaxWidth()) {
                        listOf(false, true).forEachIndexed { i, online ->
                            SegmentedButton(selected = c.online == online, onClick = { c = c.copy(online = online) }, shape = SegmentedButtonDefaults.itemShape(i, 2)) {
                                Text(if (online) "Online" else "Offline")
                            }
                        }
                    }
                    if (!c.online) OutlinedTextField(c.address, { c = c.copy(address = it) }, label = { Text("Địa chỉ học") }, singleLine = true, modifier = Modifier.fillMaxWidth())
                    // 8 colours as an even 4 × 2 grid.
                    Column {
                        ClassColors.chunked(4).forEachIndexed { r, row ->
                            Row(Modifier.fillMaxWidth()) {
                                row.forEachIndexed { k, col ->
                                    val i = r * 4 + k
                                    Box(Modifier.weight(1f).height(48.dp).clickable { c = c.copy(color = col) }.semantics { contentDescription = "Màu " + ClassColorNames[i] },
                                        contentAlignment = Alignment.Center) {
                                        Box(Modifier.size(40.dp).clip(CircleShape).background(Color(col)), contentAlignment = Alignment.Center) {
                                            if (c.color == col) Icon(Icons.Default.Check, null, tint = Color.White, modifier = Modifier.size(20.dp))
                                        }
                                    }
                                }
                            }
                        }
                    }
                    HorizontalDivider()
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        Column(Modifier.weight(1f)) {
                            Text("Lặp lại hàng tuần", fontWeight = FontWeight.SemiBold)
                            Text(if (c.repeat) "Lớp cố định theo thứ" else "Buổi lẻ: lớp bù / cấp tốc", style = MaterialTheme.typography.bodySmall)
                        }
                        Switch(c.repeat, { c = c.copy(repeat = it) })
                    }
                    if (c.repeat) Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(4.dp)) {
                        (1..7).forEach { d ->
                            val on = d in c.weekdays
                            Box(Modifier.weight(1f).height(48.dp).clickable { c = c.toggleDay(d) }, contentAlignment = Alignment.Center) {
                                Box(Modifier.fillMaxWidth().height(40.dp).clip(RoundedCornerShape(10.dp))
                                    .background(if (on) Color(c.color) else Color.Transparent).border(1.dp, if (on) Color(c.color) else Hairline, RoundedCornerShape(10.dp)),
                                    contentAlignment = Alignment.Center) {
                                    Text(Format.weekdayShort(d), fontSize = 14.sp, fontWeight = FontWeight.SemiBold, color = if (on) Color.White else MaterialTheme.colorScheme.onSurface)
                                }
                            }
                        }
                    }
                    if (c.repeat) {
                        if (c.weekdays.isNotEmpty()) Text("Giờ học từng buổi (mỗi thứ chỉnh riêng được)", style = MaterialTheme.typography.bodySmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
                        c.weekdays.sorted().forEach { d ->
                            val t = c.timeOn(d)
                            Row(horizontalArrangement = Arrangement.spacedBy(8.dp), verticalAlignment = Alignment.CenterVertically) {
                                Text(Format.weekdayShort(d), Modifier.width(36.dp), fontWeight = FontWeight.Bold, color = Accent)
                                OutlinedButton(onClick = { timeDay = d; pickTime = true }) { Text(Format.time(t.start)) }
                                Text("–")
                                OutlinedButton(onClick = { timeDay = d; pickEndTime = true }) { Text(Format.time((t.start + t.duration) % (24 * 60))) }
                                Text(Format.duration(t.duration), style = MaterialTheme.typography.bodySmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
                            }
                        }
                    } else {
                        Row(horizontalArrangement = Arrangement.spacedBy(8.dp), verticalAlignment = Alignment.CenterVertically) {
                            OutlinedButton(onClick = { timeDay = 0; pickTime = true }) { Text("Bắt đầu " + Format.time(c.startMinute)) }
                            OutlinedButton(onClick = { timeDay = 0; pickEndTime = true }) { Text("Kết thúc " + Format.time((c.startMinute + c.durationMin) % (24 * 60))) }
                        }
                        Text("Thời lượng " + Format.duration(c.durationMin), style = MaterialTheme.typography.bodySmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
                    }
                    Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                        OutlinedButton(onClick = { pickStart = true }) { Text((if (c.repeat) "Từ " else "Ngày ") + Format.shortDay(c.startDay)) }
                        if (c.repeat) OutlinedButton(onClick = { pickEnd = true }) { Text(c.endDay?.let { "Đến " + Format.shortDay(it) } ?: "Không hạn") }
                    }
                    HorizontalDivider()
                    Text("Học sinh trong lớp (" + c.members.size + ")", fontWeight = FontWeight.SemiBold)
                    if (c.type == ClassType.PERSONAL) Text("Học phí riêng từng bạn: sửa để miễn giảm.", style = MaterialTheme.typography.bodySmall)
                    if (c.members.isEmpty()) Text("Lớp chưa có học sinh.", style = MaterialTheme.typography.bodyMedium, color = MaterialTheme.colorScheme.onSurfaceVariant)
                    c.members.forEach { m ->
                        Row(verticalAlignment = Alignment.CenterVertically) {
                            Text(names[m.studentId]?.name ?: "(đã xoá)", Modifier.weight(1f))
                            if (c.type == ClassType.PERSONAL)
                                MoneyField("Học phí", m.fee, Modifier.width(150.dp)) { fee -> c = c.copy(members = c.members.map { if (it.studentId == m.studentId) it.copy(fee = fee) else it }) }
                            IconButton(onClick = { c = c.copy(members = c.members.filter { it.studentId != m.studentId }) }) {
                                Icon(Icons.Default.Close, "Xoá khỏi lớp", tint = Color(0xFFEF4444))
                            }
                        }
                        HorizontalDivider()
                    }
                    OutlinedButton(onClick = { pickStudent = true }, modifier = Modifier.fillMaxWidth()) {
                        Icon(Icons.Default.Add, null); Spacer(Modifier.width(8.dp)); Text("Thêm học sinh vào lớp")
                    }
                    if (!isNew) TextButton(onClick = { confirmDelete = true }) { Text("Xoá lớp", color = Color(0xFFEF4444)) }
                    Spacer(Modifier.height(40.dp))
                }
            }
        }
    }
    val editing = if (timeDay == 0) DayTime(c.startMinute, c.durationMin) else c.timeOn(timeDay)
    if (pickTime) TimePickerDialog(editing.start, { pickTime = false }) { c = c.withTime(timeDay, editing.copy(start = it)); pickTime = false }
    // The end time sets the length; an end at or before the start is read as running past midnight.
    if (pickEndTime) TimePickerDialog((editing.start + editing.duration) % (24 * 60), { pickEndTime = false }) { end ->
        val length = Math.floorMod(end - editing.start, 24 * 60)
        if (length > 0) c = c.withTime(timeDay, editing.copy(duration = length)); pickEndTime = false
    }
    if (pickStudent) StudentPickerDialog(data, c.members.map { it.studentId }.toSet(), "Thêm học sinh vào lớp", { pickStudent = false }) { s ->
        c = c.copy(members = c.members + Member(s.id, s.fee))
    }
    if (pickStart) DayPickerDialog(c.startDay, { pickStart = false }) { c = c.copy(startDay = it); pickStart = false }
    if (pickEnd) DayPickerDialog(c.endDay ?: c.startDay, { pickEnd = false }) { c = c.copy(endDay = it); pickEnd = false }
    if (confirmDelete) AlertDialog(onDismissRequest = { confirmDelete = false }, title = { Text("Xoá lớp " + c.name + "?") },
        text = { Text("Các buổi đã dạy và học phí đã ghi vẫn được giữ.") },
        confirmButton = { TextButton(onClick = onDelete) { Text("Xoá", color = Color(0xFFEF4444)) } },
        dismissButton = { TextButton(onClick = { confirmDelete = false }) { Text("Huỷ") } })
}
