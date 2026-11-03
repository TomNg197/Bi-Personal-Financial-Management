package vn.mrsbi.handout.ui

import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.ExperimentalLayoutApi
import androidx.compose.foundation.layout.FlowRow
import androidx.compose.foundation.layout.IntrinsicSize
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxHeight
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
import androidx.compose.foundation.pager.HorizontalPager
import androidx.compose.foundation.pager.rememberPagerState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.KeyboardArrowLeft
import androidx.compose.material.icons.automirrored.filled.KeyboardArrowRight
import androidx.compose.material.icons.filled.Add
import androidx.compose.material.icons.filled.Check
import androidx.compose.material.icons.filled.CheckCircle
import androidx.compose.material.icons.filled.Close
import androidx.compose.material.icons.filled.EventRepeat
import androidx.compose.material.icons.filled.Groups
import androidx.compose.material.icons.filled.PersonAdd
import androidx.compose.material.icons.filled.Place
import androidx.compose.material.icons.filled.Schedule
import androidx.compose.material.icons.filled.Wifi
import androidx.compose.material3.AssistChip
import androidx.compose.material3.BottomSheetDefaults
import androidx.compose.material3.Button
import androidx.compose.material3.Checkbox
import androidx.compose.material3.ExperimentalMaterial3Api
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
import androidx.compose.material3.Switch
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableLongStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.produceState
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.alpha
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.shadow
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextDecoration
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch
import vn.mrsbi.handout.data.AppData
import vn.mrsbi.handout.data.ClassGroup
import vn.mrsbi.handout.data.ClassType
import vn.mrsbi.handout.data.Member
import vn.mrsbi.handout.data.Occurrence
import vn.mrsbi.handout.data.Rules
import vn.mrsbi.handout.data.SessionRecord
import vn.mrsbi.handout.data.Store
import vn.mrsbi.handout.data.Student
import vn.mrsbi.handout.data.markDone
import vn.mrsbi.handout.data.postpone
import vn.mrsbi.handout.data.reopen
import vn.mrsbi.handout.data.restoreRecord
import vn.mrsbi.handout.data.saveClass
import vn.mrsbi.handout.data.setGuests
import vn.mrsbi.handout.util.Format
import java.time.LocalDate
import java.time.LocalTime

/** Main screen: the classes of the chosen day (default today), upcoming first, taught ones collapsed at the bottom. */
@Composable
fun TodayScreen(store: Store, data: AppData, openManage: () -> Unit, initialDay: Long = Format.today()) {
    // Swipe left/right to change day; page Origin is today.
    val pager = rememberPagerState(initialPage = (Origin + initialDay - Format.today()).toInt()) { Origin * 2 }
    val scope = rememberCoroutineScope()
    val day = Format.today() + (pager.currentPage - Origin)
    fun goTo(d: Long) { scope.launch { pager.animateScrollToPage((Origin + d - Format.today()).toInt()) } }
    var opened by remember { mutableStateOf<String?>(null) }
    var makeup by remember { mutableStateOf<ClassGroup?>(null) }
    val snackbar = remember { SnackbarHostState() }
    val now by produceState(LocalTime.now().toSecondOfDay() / 60) { while (true) { delay(30_000); value = LocalTime.now().toSecondOfDay() / 60 } }
    val list = Rules.occurrencesOn(data, day)
    val earned = list.mapNotNull { it.record }.sumOf { Rules.earned(it) }
    Box(Modifier.fillMaxSize()) {
        Column(Modifier.fillMaxSize()) {
            Row(Modifier.fillMaxWidth().padding(start = 16.dp, end = 4.dp, top = 12.dp), verticalAlignment = Alignment.CenterVertically) {
                Column(Modifier.weight(1f)) {
                    Text(if (day == Format.today()) "Hôm nay" else Format.weekday(day), style = MaterialTheme.typography.headlineMedium, fontWeight = FontWeight.ExtraBold, color = Accent)
                    Text(if (day == Format.today()) Format.fullDay(day) else Format.day(day), style = MaterialTheme.typography.bodyMedium, color = MaterialTheme.colorScheme.onSurfaceVariant)
                }
                IconButton(onClick = { goTo(day - 1) }, Modifier.size(48.dp)) { Icon(Icons.AutoMirrored.Filled.KeyboardArrowLeft, "Hôm trước") }
                IconButton(onClick = { goTo(day + 1) }, Modifier.size(48.dp)) { Icon(Icons.AutoMirrored.Filled.KeyboardArrowRight, "Hôm sau") }
            }
            if (day != Format.today()) TextButton(onClick = { goTo(Format.today()) }, modifier = Modifier.padding(start = 4.dp)) { Text("← Về hôm nay", fontWeight = FontWeight.SemiBold) }
            if (list.isNotEmpty()) Text(
                "${list.size} lớp · ${list.sumOf { studentsOf(it) }} học sinh" + if (earned > 0) " · dạy được " + Format.money(earned) else "",
                Modifier.padding(horizontal = 16.dp), style = MaterialTheme.typography.titleSmall, fontWeight = FontWeight.SemiBold,
                color = MaterialTheme.colorScheme.onSurface.copy(alpha = .8f),
            )
            HorizontalPager(pager, Modifier.fillMaxSize(), beyondViewportPageCount = 1) { page ->
                val pageDay = Format.today() + (page - Origin)
                val all = Rules.occurrencesOn(data, pageDay)
                if (all.isEmpty()) FreeDay(data, pageDay, onNext = { goTo(it) }, onMakeup = {
                    makeup = ClassGroup(Store.newId(), "", repeat = false, startDay = pageDay, color = ClassColors[data.classes.size % ClassColors.size])
                }, showAdd = data.classes.isNotEmpty(), openManage = openManage)
                else {
                    val upcoming = all.filter { !it.done }.sortedBy { it.startMinute }
                    val taught = all.filter { it.done }.sortedBy { it.startMinute }
                    // The next class: in progress, or starting within 3 hours (today only).
                    val next = if (pageDay == Format.today()) upcoming.firstOrNull { now < it.startMinute + it.durationMin && it.startMinute - now <= 180 } else null
                    val clashes = Rules.clashing(all)
                    LazyColumn(Modifier.fillMaxSize().padding(horizontal = 14.dp), verticalArrangement = Arrangement.spacedBy(12.dp)) {
                        if (upcoming.isNotEmpty()) item("h1") { SectionTitle("SẮP TỚI") }
                        items(upcoming, key = { it.key }) { o ->
                            SessionCard(o, highlight = o == next, clash = o.key in clashes, now = now, onClick = { opened = o.key }, modifier = Modifier.animateItem())
                        }
                        if (taught.isNotEmpty()) item("h2") { SectionTitle("ĐÃ DẠY") }
                        items(taught, key = { it.key }) { o -> TaughtRow(o, onClick = { opened = o.key }, modifier = Modifier.animateItem()) }
                        item("end") { Spacer(Modifier.height(72.dp)) }
                    }
                }
            }
        }
        SnackbarHost(snackbar, Modifier.align(Alignment.BottomCenter))
    }
    val current = list.firstOrNull { it.key == opened }
    if (current != null) AttendanceSheet(store, data, current, onClose = { opened = null }, onDone = { o, previous, amount ->
        scope.launch {
            // Visible for 5 s with "Hoàn tác" that restores the session as it was.
            val job = launch {
                val result = snackbar.showSnackbar("Đã lưu buổi · " + Format.money(amount), actionLabel = "Hoàn tác", duration = SnackbarDuration.Indefinite)
                if (result == SnackbarResult.ActionPerformed) store.restoreRecord(o.group.id, o.originalDay, previous)
            }
            delay(5000); snackbar.currentSnackbarData?.dismiss(); job.join()
        }
    })
    makeup?.let { c -> ClassEditor(c, data, isNew = true, onSave = { store.saveClass(it); makeup = null }, onDelete = { makeup = null }, onClose = { makeup = null }) }
}

private const val Origin = 5000

@Composable
private fun SectionTitle(text: String) {
    Text(text, Modifier.padding(start = 4.dp, top = 12.dp), fontSize = 13.sp, fontWeight = FontWeight.Bold, letterSpacing = 1.sp, color = MaterialTheme.colorScheme.onSurfaceVariant)
}

fun studentsOf(o: Occurrence) = o.group.members.size + (o.record?.guests?.keys?.count { id -> o.group.members.none { it.studentId == id } } ?: 0)

private fun untilText(minutes: Int) = if (minutes < 60) "Còn $minutes phút" else "Còn " + Format.duration(minutes)

/** Each class is a colour board; a dark veil behind the text column keeps white text readable on light colours. */
@OptIn(ExperimentalLayoutApi::class)
@Composable
private fun SessionCard(o: Occurrence, highlight: Boolean, clash: Boolean, now: Int, onClick: () -> Unit, modifier: Modifier = Modifier) {
    val color = Color(o.group.color)
    val shape = RoundedCornerShape(22.dp)
    val ring = if (highlight) Modifier.shadow(14.dp, RoundedCornerShape(26.dp), ambientColor = color, spotColor = color)
        .border(4.dp, color.copy(alpha = .55f), RoundedCornerShape(26.dp)).padding(4.dp) else Modifier
    Box(modifier.fillMaxWidth().then(ring)) {
        Box(Modifier.fillMaxWidth().shadow(if (highlight) 0.dp else 3.dp, shape).clip(shape)
            .background(Brush.linearGradient(listOf(lerpColor(color, Color.Black, .12f), lerpColor(color, Color.White, .22f))))
            .background(Brush.horizontalGradient(0f to Color.Black.copy(alpha = .18f), .75f to Color.Transparent))
            .then(if (highlight) Modifier.border(3.dp, Color.White, shape) else Modifier)
            .clickable(onClick = onClick).padding(18.dp)) {
            Column(Modifier.fillMaxWidth()) {
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Text(Format.range(o.startMinute, o.durationMin), Modifier.weight(1f), color = Color.White, fontSize = 15.sp, fontWeight = FontWeight.Bold)
                    if (clash) Row(Modifier.padding(end = 6.dp).clip(RoundedCornerShape(50)).background(Clash).padding(horizontal = 10.dp, vertical = 5.dp), verticalAlignment = Alignment.CenterVertically) {
                        Text("! Trùng giờ", color = Color.White, fontSize = 14.sp, fontWeight = FontWeight.Bold, maxLines = 1)
                    }
                    if (highlight) Row(Modifier.clip(RoundedCornerShape(50)).background(Color.White).padding(horizontal = 10.dp, vertical = 5.dp), verticalAlignment = Alignment.CenterVertically) {
                        Icon(Icons.Default.Schedule, null, tint = color, modifier = Modifier.size(16.dp)); Spacer(Modifier.width(4.dp))
                        Text(if (now >= o.startMinute) "Đang dạy" else untilText(o.startMinute - now), color = color, fontSize = 14.sp, fontWeight = FontWeight.Bold)
                    }
                }
                Text(o.group.name, color = Color.White, fontSize = 24.sp, fontWeight = FontWeight.Black, maxLines = 2, overflow = TextOverflow.Ellipsis, lineHeight = 28.sp)
                val type = if (o.group.type == ClassType.CENTER) "Trung tâm" else "Cá nhân"
                Text(listOf(o.group.content, type).filter { it.isNotBlank() }.joinToString(" · ") + if (o.moved) " · dời từ " + Format.shortDay(o.originalDay) else "",
                    color = Color.White.copy(alpha = .92f), fontSize = 15.sp, maxLines = 1, overflow = TextOverflow.Ellipsis)
                Spacer(Modifier.height(10.dp))
                PlaceTag(o.group, color)
                Spacer(Modifier.height(14.dp))
                // Button left, chip right; with large text the chip wraps under the button (left-aligned, 8dp gap).
                FlowRow(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween, verticalArrangement = Arrangement.spacedBy(8.dp)) {
                    Box(Modifier.height(48.dp), contentAlignment = Alignment.Center) {
                        Box(Modifier.height(44.dp).shadow(2.dp, RoundedCornerShape(50)).clip(RoundedCornerShape(50)).background(Color.White).clickable(onClick = onClick)
                            .padding(horizontal = 22.dp), contentAlignment = Alignment.Center) {
                            Text("Điểm danh →", color = color, fontSize = 16.sp, fontWeight = FontWeight.Bold, maxLines = 1, softWrap = false)
                        }
                    }
                    Box(Modifier.height(48.dp), contentAlignment = Alignment.Center) {
                    Row(Modifier.clip(RoundedCornerShape(50)).background(Color.White.copy(alpha = .2f)).border(1.dp, Color.White.copy(alpha = .7f), RoundedCornerShape(50))
                        .padding(horizontal = 12.dp, vertical = 6.dp), verticalAlignment = Alignment.CenterVertically) {
                        if (o.group.type == ClassType.CENTER) Text(Format.money(o.group.centerFee) + "/buổi", color = Color.White, fontSize = 15.sp, fontWeight = FontWeight.Bold,
                            maxLines = 1)
                        else { Icon(Icons.Default.Groups, "Số học sinh", tint = Color.White, modifier = Modifier.size(18.dp)); Spacer(Modifier.width(6.dp))
                            Text(studentsOf(o).toString(), color = Color.White, fontSize = 15.sp, fontWeight = FontWeight.Bold) }
                    }
                    }
                }
            }
        }
    }
}

/** A taught session collapses into one row: colour edge, tick, attendance and what it earned. */
@Composable
private fun TaughtRow(o: Occurrence, onClick: () -> Unit, modifier: Modifier = Modifier) {
    val color = Color(o.group.color)
    val record = o.record!!
    val total = record.charges.size + record.absent.size
    val present = if (o.group.type == ClassType.CENTER) null else "có mặt " + (total - record.absent.size) + "/" + total
    Row(modifier.fillMaxWidth().height(IntrinsicSize.Min).clip(RoundedCornerShape(16.dp)).background(CardBg).clickable(onClick = onClick), verticalAlignment = Alignment.CenterVertically) {
        Box(Modifier.width(5.dp).fillMaxHeight().background(color))
        Box(Modifier.padding(start = 14.dp).size(28.dp).clip(CircleShape).background(color.copy(alpha = .16f)), contentAlignment = Alignment.Center) {
            Icon(Icons.Default.Check, "Đã dạy", tint = color, modifier = Modifier.size(18.dp))
        }
        Column(Modifier.weight(1f).padding(horizontal = 12.dp, vertical = 14.dp)) {
            Text(o.group.name, fontSize = 15.sp, fontWeight = FontWeight.Bold, maxLines = 1, overflow = TextOverflow.Ellipsis)
            Text(listOfNotNull(Format.range(o.startMinute, o.durationMin), present).joinToString(" · "), fontSize = 14.sp, color = MaterialTheme.colorScheme.onSurfaceVariant)
        }
        Text(Format.money(Rules.earned(record)), Modifier.padding(end = 16.dp), fontSize = 15.sp, fontWeight = FontWeight.Bold, color = Earned)
    }
}

/** A day without classes: a calm message, the next class (tap to jump there) and a way to add a make-up session. */
@Composable
private fun FreeDay(data: AppData, day: Long, onNext: (Long) -> Unit, onMakeup: () -> Unit, showAdd: Boolean, openManage: () -> Unit) {
    val next = Rules.nextOccurrence(data, day)
    Column(Modifier.fillMaxSize().padding(horizontal = 16.dp), horizontalAlignment = Alignment.CenterHorizontally) {
        Spacer(Modifier.height(56.dp))
        Box(Modifier.size(96.dp).clip(CircleShape).background(Brush.linearGradient(listOf(Color(0xFFE9E2FF), Color(0xFFFFDDE8)))), contentAlignment = Alignment.Center) {
            Text("☕", fontSize = 44.sp)
        }
        Spacer(Modifier.height(20.dp))
        Text(if (day == Format.today()) "Hôm nay cô được nghỉ" else "Ngày này cô được nghỉ", fontSize = 22.sp, fontWeight = FontWeight.ExtraBold, textAlign = TextAlign.Center)
        Text(if (data.classes.isEmpty()) "Chưa có lớp nào. Thêm lớp đầu tiên để bắt đầu." else "Không có lớp nào trong ngày.",
            style = MaterialTheme.typography.bodyLarge, color = MaterialTheme.colorScheme.onSurfaceVariant, textAlign = TextAlign.Center)
        Spacer(Modifier.height(28.dp))
        if (next != null) {
            val color = Color(next.group.color)
            Row(Modifier.fillMaxWidth().height(IntrinsicSize.Min).clip(RoundedCornerShape(16.dp)).background(CardBg).clickable { onNext(next.day) },
                verticalAlignment = Alignment.CenterVertically) {
                Box(Modifier.width(5.dp).fillMaxHeight().background(color))
                Column(Modifier.weight(1f).padding(horizontal = 16.dp, vertical = 14.dp)) {
                    Text("LỚP TIẾP THEO", fontSize = 13.sp, fontWeight = FontWeight.Bold, letterSpacing = 1.sp, color = MaterialTheme.colorScheme.onSurfaceVariant)
                    Text(Format.weekday(next.day) + ", " + Format.shortDay(next.day) + " · " + Format.time(next.startMinute), fontSize = 17.sp, fontWeight = FontWeight.Bold, color = lerpColor(color, Color.Black, .25f))
                    Text(listOf(next.group.name, next.group.content).filter { it.isNotBlank() }.joinToString(" · "), fontSize = 15.sp, maxLines = 1, overflow = TextOverflow.Ellipsis)
                }
                Icon(Icons.AutoMirrored.Filled.KeyboardArrowRight, "Tới ngày này", Modifier.padding(end = 12.dp), tint = MaterialTheme.colorScheme.onSurfaceVariant)
            }
            Spacer(Modifier.height(20.dp))
        }
        if (showAdd) OutlinedButton(onClick = onMakeup, modifier = Modifier.heightIn(min = 48.dp)) {
            Icon(Icons.Default.Add, null); Spacer(Modifier.width(8.dp)); Text("Thêm buổi học bù", fontSize = 16.sp, fontWeight = FontWeight.SemiBold)
        } else Button(onClick = openManage, modifier = Modifier.heightIn(min = 48.dp)) { Text("Thêm lớp đầu tiên") }
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
private fun AttendanceSheet(store: Store, data: AppData, o: Occurrence, onClose: () -> Unit, onDone: (Occurrence, SessionRecord?, Long) -> Unit) {
    val names = data.students.associateBy { it.id }
    val color = Color(o.group.color)
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
    val absentCount = rows.count { it.first in absent }
    ModalBottomSheet(onDismissRequest = onClose, containerColor = CardBg, dragHandle = {
        Column(Modifier.fillMaxWidth(), horizontalAlignment = Alignment.CenterHorizontally) {
            Box(Modifier.fillMaxWidth().height(8.dp).background(Brush.horizontalGradient(listOf(color, lerpColor(color, Color.White, .3f)))))
            BottomSheetDefaults.DragHandle()
        }
    }) {
        Column(Modifier.padding(horizontal = 20.dp).navigationBarsPadding()) {
            Text(o.group.name, style = MaterialTheme.typography.titleLarge, fontWeight = FontWeight.Bold)
            Text(Format.fullDay(o.day) + " · " + Format.range(o.startMinute, o.durationMin), color = MaterialTheme.colorScheme.onSurfaceVariant)
            Spacer(Modifier.height(8.dp))
            PlaceTag(o.group, color, onBoard = false)
            if (rows.isNotEmpty()) Row(Modifier.fillMaxWidth().padding(top = 6.dp), verticalAlignment = Alignment.CenterVertically) {
                Text("Có mặt ${rows.size - absentCount}/${rows.size}", style = MaterialTheme.typography.titleSmall, color = Accent, fontWeight = FontWeight.Bold)
                if (absentCount > 0) Text(" · Vắng $absentCount", style = MaterialTheme.typography.titleSmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
                Spacer(Modifier.weight(1f))
                if (!o.done && absentCount > 0) TextButton(onClick = { absent = emptySet() }) { Text("Tất cả có mặt", fontWeight = FontWeight.SemiBold) }
            }
            Spacer(Modifier.height(4.dp))
            if (rows.isEmpty()) Text(if (o.group.type == ClassType.CENTER) "Lớp trung tâm tính tiền theo buổi, không cần điểm danh từng bạn."
                else "Lớp chưa có học sinh. Thêm ở tab Quản lý hoặc bấm “Thêm học sinh học ghép”.", style = MaterialTheme.typography.bodyMedium)
            LazyColumn(Modifier.fillMaxWidth().heightIn(max = (61 * 6).dp)) {
                items(rows, key = { it.first }) { (id, fee, guest) ->
                    val present = id !in absent
                    Row(Modifier.fillMaxWidth().heightIn(min = 60.dp).clickable(enabled = !o.done) { absent = if (present) absent + id else absent - id }
                        .alpha(if (present) 1f else .55f), verticalAlignment = Alignment.CenterVertically) {
                        Column(Modifier.weight(1f)) {
                            Text(names[id]?.name ?: "(đã xoá)", style = MaterialTheme.typography.bodyLarge, fontWeight = FontWeight.Medium)
                            val info = listOfNotNull(if (guest) "học ghép" else null, if (o.group.type == ClassType.PERSONAL) Format.money(fee) + "/buổi" else null)
                            if (info.isNotEmpty()) Text(info.joinToString(" · "), style = MaterialTheme.typography.bodyMedium, color = if (guest) Rose else MaterialTheme.colorScheme.onSurfaceVariant,
                                textDecoration = if (present) null else TextDecoration.LineThrough)
                        }
                        if (guest && !o.done) IconButton(onClick = { guests = guests - id; absent = absent - id; store.setGuests(o, guests) }) {
                            Icon(Icons.Default.Close, "Bỏ học ghép", tint = MaterialTheme.colorScheme.onSurfaceVariant)
                        }
                        Text(if (present) "Có mặt" else "Vắng", color = if (present) Accent else Absent, style = MaterialTheme.typography.labelLarge, fontWeight = FontWeight.Bold)
                        Spacer(Modifier.width(10.dp))
                        Switch(checked = present, enabled = !o.done, onCheckedChange = { absent = if (it) absent - id else absent + id })
                    }
                    HorizontalDivider()
                }
            }
            if (!o.done) TextButton(onClick = { pickGuest = true }, modifier = Modifier.heightIn(min = 48.dp)) {
                Icon(Icons.Default.PersonAdd, null); Spacer(Modifier.width(8.dp)); Text("Thêm học sinh học ghép", fontWeight = FontWeight.SemiBold)
            }
            Spacer(Modifier.height(8.dp))
            if (o.done) {
                AssistChip(onClick = {}, label = { Text("Đã dạy · " + Format.money(Rules.earned(o.record!!))) }, leadingIcon = { Icon(Icons.Default.CheckCircle, null) })
                OutlinedButton(onClick = { store.reopen(o); onClose() }, modifier = Modifier.fillMaxWidth().heightIn(min = 48.dp)) { Text("Mở lại buổi (bấm nhầm)") }
            } else {
                Button(onClick = { val previous = o.record; store.markDone(o, absent); onClose(); onDone(o, previous, amount) }, modifier = Modifier.fillMaxWidth().height(54.dp)) {
                    Text("Xong buổi · " + Format.money(amount), fontSize = 17.sp, fontWeight = FontWeight.Bold)
                }
                Spacer(Modifier.height(10.dp))
                OutlinedButton(onClick = { pickDay = true }, modifier = Modifier.fillMaxWidth().height(52.dp)) {
                    Icon(Icons.Default.EventRepeat, null); Spacer(Modifier.width(8.dp)); Text("Hoãn sang ngày khác", fontSize = 16.sp, fontWeight = FontWeight.SemiBold)
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
