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
import vn.mrsbi.handout.data.Member
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
                        Box(Modifier.size(14.dp).clip(CircleShape).background(Color(c.color)))
                        Spacer(Modifier.width(12.dp))
                        Column(Modifier.weight(1f)) {
                            Text(c.name, fontWeight = FontWeight.SemiBold)
                            Text(scheduleText(c) + " · " + (if (c.type == ClassType.CENTER) "Trung tâm " + Format.money(c.centerFee) + "/buổi" else "Cá nhân") + " · " + c.members.size + " HS",
                                style = MaterialTheme.typography.bodySmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
                        }
                    }
                    HorizontalDivider()
                }
                item { Spacer(Modifier.height(96.dp)) }
            } else LazyColumn(Modifier.fillMaxSize()) {
                items(data.students.filter { !it.archived }.sortedBy { it.name }, key = { it.id }) { s ->
                    Row(Modifier.fillMaxWidth().clickable { editStudent = s }.padding(16.dp)) {
                        Column(Modifier.weight(1f)) {
                            Text(s.name, fontWeight = FontWeight.SemiBold)
                            val classes = data.classes.filter { c -> !c.archived && c.members.any { it.studentId == s.id } }.joinToString(", ") { it.name }
                            Text(listOfNotNull(s.age?.let { "$it tuổi" }, Format.money(s.fee) + "/buổi", classes.ifBlank { null }).joinToString(" · "),
                                style = MaterialTheme.typography.bodySmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
                        }
                    }
                    HorizontalDivider()
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
        onDelete = { store.deleteClass(c.id); editClass = null }, onClose = { editClass = null },
        onNewStudent = { name, fee -> Student(Store.newId(), name, fee = fee).also { store.saveStudent(it) } }) }
}

fun scheduleText(c: ClassGroup) =
    if (c.repeat) c.weekdays.sorted().joinToString(", ") { Format.weekdayShort(it) } + " " + Format.time(c.startMinute)
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
private fun ClassEditor(
    group: ClassGroup, data: AppData, isNew: Boolean, onSave: (ClassGroup) -> Unit, onDelete: () -> Unit, onClose: () -> Unit,
    onNewStudent: (String, Long) -> Student,
) {
    var c by remember { mutableStateOf(group) }
    var pickStart by remember { mutableStateOf(false) }
    var pickEnd by remember { mutableStateOf(false) }
    var pickTime by remember { mutableStateOf(false) }
    var confirmDelete by remember { mutableStateOf(false) }
    var quickName by remember { mutableStateOf("") }
    val students = data.students.filter { !it.archived }.sortedBy { it.name }
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
                    SingleChoiceSegmentedButtonRow(Modifier.fillMaxWidth()) {
                        ClassType.entries.forEachIndexed { i, t ->
                            SegmentedButton(selected = c.type == t, onClick = { c = c.copy(type = t) }, shape = SegmentedButtonDefaults.itemShape(i, 2)) {
                                Text(if (t == ClassType.PERSONAL) "Cá nhân (theo HS)" else "Trung tâm (theo buổi)")
                            }
                        }
                    }
                    if (c.type == ClassType.CENTER) MoneyField("Tiền mỗi buổi dạy", c.centerFee, Modifier.fillMaxWidth()) { c = c.copy(centerFee = it) }
                    Row(horizontalArrangement = Arrangement.spacedBy(10.dp)) {
                        ClassColors.forEach { col ->
                            Box(Modifier.size(30.dp).clip(CircleShape).background(Color(col)).border(3.dp, if (c.color == col) Color.Black else Color.Transparent, CircleShape)
                                .clickable { c = c.copy(color = col) })
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
                    if (c.repeat) FlowRow(horizontalArrangement = Arrangement.spacedBy(6.dp)) {
                        (1..7).forEach { d ->
                            FilterChip(selected = d in c.weekdays, onClick = { c = c.copy(weekdays = if (d in c.weekdays) c.weekdays - d else (c.weekdays + d).sorted()) },
                                label = { Text(Format.weekdayShort(d)) })
                        }
                    }
                    Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                        OutlinedButton(onClick = { pickTime = true }) { Text("Giờ " + Format.time(c.startMinute)) }
                        OutlinedButton(onClick = { pickStart = true }) { Text((if (c.repeat) "Từ " else "Ngày ") + Format.shortDay(c.startDay)) }
                        if (c.repeat) OutlinedButton(onClick = { pickEnd = true }) { Text(c.endDay?.let { "Đến " + Format.shortDay(it) } ?: "Không hạn") }
                    }
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        Text("Thời lượng", Modifier.weight(1f))
                        listOf(60, 90, 120).forEach { m -> FilterChip(selected = c.durationMin == m, onClick = { c = c.copy(durationMin = m) }, label = { Text("$m'") }, modifier = Modifier.padding(start = 6.dp)) }
                    }
                    HorizontalDivider()
                    Text("Học sinh trong lớp (" + c.members.size + ")", fontWeight = FontWeight.SemiBold)
                    if (c.type == ClassType.PERSONAL) Text("Học phí riêng từng bạn: sửa để miễn giảm.", style = MaterialTheme.typography.bodySmall)
                    students.forEach { s ->
                        val member = c.members.firstOrNull { it.studentId == s.id }
                        Row(verticalAlignment = Alignment.CenterVertically) {
                            Checkbox(member != null, { on ->
                                c = c.copy(members = if (on) c.members + Member(s.id, s.fee) else c.members.filter { it.studentId != s.id })
                            })
                            Text(s.name, Modifier.weight(1f))
                            if (member != null && c.type == ClassType.PERSONAL)
                                MoneyField("Học phí", member.fee, Modifier.width(170.dp)) { fee -> c = c.copy(members = c.members.map { if (it.studentId == s.id) it.copy(fee = fee) else it }) }
                        }
                    }
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        OutlinedTextField(quickName, { quickName = it }, label = { Text("Thêm nhanh học sinh mới") }, singleLine = true, modifier = Modifier.weight(1f))
                        TextButton(enabled = quickName.isNotBlank(), onClick = {
                            val s = onNewStudent(quickName.trim(), 0); c = c.copy(members = c.members + Member(s.id, 0)); quickName = ""
                        }) { Text("Thêm") }
                    }
                    if (!isNew) TextButton(onClick = { confirmDelete = true }) { Text("Xoá lớp", color = Color(0xFFEF4444)) }
                    Spacer(Modifier.height(40.dp))
                }
            }
        }
    }
    if (pickTime) TimePickerDialog(c.startMinute, { pickTime = false }) { c = c.copy(startMinute = it); pickTime = false }
    if (pickStart) DayPickerDialog(c.startDay, { pickStart = false }) { c = c.copy(startDay = it); pickStart = false }
    if (pickEnd) DayPickerDialog(c.endDay ?: c.startDay, { pickEnd = false }) { c = c.copy(endDay = it); pickEnd = false }
    if (confirmDelete) AlertDialog(onDismissRequest = { confirmDelete = false }, title = { Text("Xoá lớp " + c.name + "?") },
        text = { Text("Các buổi đã dạy và học phí đã ghi vẫn được giữ.") },
        confirmButton = { TextButton(onClick = onDelete) { Text("Xoá", color = Color(0xFFEF4444)) } },
        dismissButton = { TextButton(onClick = { confirmDelete = false }) { Text("Huỷ") } })
}
