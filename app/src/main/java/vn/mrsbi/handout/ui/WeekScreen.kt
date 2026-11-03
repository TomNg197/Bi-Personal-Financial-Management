package vn.mrsbi.handout.ui

import androidx.compose.foundation.Canvas
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxHeight
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.offset
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.KeyboardArrowLeft
import androidx.compose.material.icons.automirrored.filled.KeyboardArrowRight
import androidx.compose.material.icons.filled.Check
import androidx.compose.material.icons.filled.UnfoldMore
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableLongStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.produceState
import androidx.compose.runtime.remember
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import kotlinx.coroutines.delay
import vn.mrsbi.handout.data.AppData
import vn.mrsbi.handout.data.Rules
import vn.mrsbi.handout.data.Store
import vn.mrsbi.handout.util.Format
import java.time.LocalDate
import java.time.LocalTime

/** One stretch of the time axis: an hour row, or a run of empty hours folded into a thin strip. */
private data class Band(val fromHour: Int, val toHour: Int, val folded: Boolean)

/** Weekly timetable (Mon–Sun): blocks placed by start time and length; empty stretches of 3 h or more are folded. */
@Composable
fun WeekScreen(store: Store, data: AppData) {
    var monday by rememberSaveable { mutableLongStateOf(LocalDate.now().let { it.minusDays(it.dayOfWeek.value - 1L) }.toEpochDay()) }
    var unfolded by remember(monday) { mutableStateOf(emptySet<Int>()) }
    val now by produceState(LocalTime.now().toSecondOfDay() / 60) { while (true) { delay(60_000); value = LocalTime.now().toSecondOfDay() / 60 } }
    val days = (0..6).map { monday + it }
    val week = days.associateWith { Rules.occurrencesOn(data, it) }
    val all = week.values.flatten()
    val firstHour = ((all.minOfOrNull { it.startMinute } ?: (8 * 60)) / 60).coerceAtMost(8)
    val lastHour = ((all.maxOfOrNull { it.startMinute + it.durationMin } ?: (20 * 60)) + 59).div(60).coerceAtLeast(20).coerceAtMost(24)
    val busy = (firstHour until lastHour).filter { h -> all.any { it.startMinute < (h + 1) * 60 && it.startMinute + it.durationMin > h * 60 } }.toSet()
    val bands = buildList {
        var h = firstHour
        while (h < lastHour) {
            if (h in busy) { add(Band(h, h + 1, false)); h++; continue }
            var end = h; while (end < lastHour && end !in busy) end++
            if (end - h >= 3 && h !in unfolded) add(Band(h, end, true)) else for (x in h until end) add(Band(x, x + 1, false))
            h = end
        }
    }
    val hourHeight = 56.dp; val foldHeight = 30.dp
    // Minute of the day → offset from the top of the grid.
    fun y(minute: Int): Dp {
        var top = 0.dp
        for (b in bands) {
            val h = if (b.folded) foldHeight else hourHeight
            if (minute < b.toHour * 60) return top + if (b.folded) 0.dp else h * ((minute - b.fromHour * 60) / 60f)
            top += h
        }
        return top
    }
    val total = bands.fold(0.dp) { acc, b -> acc + if (b.folded) foldHeight else hourHeight }
    val today = Format.today()
    Column(Modifier.fillMaxSize()) {
        Row(Modifier.fillMaxWidth().padding(8.dp), verticalAlignment = Alignment.CenterVertically) {
            IconButton(onClick = { monday -= 7 }, Modifier.size(48.dp)) { Icon(Icons.AutoMirrored.Filled.KeyboardArrowLeft, "Tuần trước") }
            Column(Modifier.weight(1f), horizontalAlignment = Alignment.CenterHorizontally) {
                Text("Thời khoá biểu", style = MaterialTheme.typography.titleLarge, fontWeight = FontWeight.ExtraBold, color = Accent)
                Text(Format.shortDay(days.first()) + " – " + Format.shortDay(days.last()) + " · " + all.size + " buổi · đã dạy " + all.count { it.done },
                    style = MaterialTheme.typography.bodyMedium, color = MaterialTheme.colorScheme.onSurfaceVariant)
            }
            IconButton(onClick = { monday += 7 }, Modifier.size(48.dp)) { Icon(Icons.AutoMirrored.Filled.KeyboardArrowRight, "Tuần sau") }
        }
        Row(Modifier.fillMaxWidth().padding(start = 36.dp, end = 4.dp)) {
            days.forEach { d ->
                val isToday = d == today
                Column(Modifier.weight(1f).padding(horizontal = 2.dp).clip(RoundedCornerShape(12.dp))
                    .background(if (isToday) MaterialTheme.colorScheme.primaryContainer else Color.Transparent).padding(vertical = 4.dp),
                    horizontalAlignment = Alignment.CenterHorizontally) {
                    Text(Format.weekdayShort(LocalDate.ofEpochDay(d).dayOfWeek.value), fontWeight = FontWeight.Bold, fontSize = 15.sp, color = if (isToday) Accent else MaterialTheme.colorScheme.onSurface)
                    Text(Format.shortDay(d), fontSize = 12.sp, color = if (isToday) Accent else MaterialTheme.colorScheme.onSurfaceVariant)
                }
            }
        }
        Box(Modifier.fillMaxSize().verticalScroll(rememberScrollState()).padding(bottom = 16.dp, end = 4.dp)) {
            val line = MaterialTheme.colorScheme.outlineVariant
            bands.filter { !it.folded }.forEach { b -> Box(Modifier.padding(start = 36.dp).offset(y = y(b.fromHour * 60)).fillMaxWidth().height(1.dp).background(line)) }
            Row(Modifier.fillMaxWidth().height(total)) {
                // Hour labels
                Box(Modifier.width(36.dp).fillMaxHeight()) {
                    bands.filter { !it.folded }.forEach { b ->
                        Text("%02d".format(b.fromHour), Modifier.offset(y = y(b.fromHour * 60)).fillMaxWidth().padding(top = 2.dp), fontSize = 12.sp,
                            textAlign = TextAlign.Center, color = MaterialTheme.colorScheme.onSurfaceVariant)
                    }
                }
                days.forEach { d ->
                    Box(Modifier.weight(1f).fillMaxHeight().padding(horizontal = 2.dp)
                        .background(if (d == today) MaterialTheme.colorScheme.primary.copy(alpha = .08f) else Color.Transparent, RoundedCornerShape(10.dp))) {
                        week.getValue(d).forEach { o ->
                            val color = Color(o.group.color)
                            val top = y(o.startMinute); val height = y(o.startMinute + o.durationMin) - top
                            Box(Modifier.offset(y = top).padding(vertical = 1.dp).fillMaxWidth().height(height).clip(RoundedCornerShape(10.dp))
                                .background(Brush.verticalGradient(listOf(color, lerpColor(color, Color.White, .25f)))).padding(5.dp)) {
                                Column {
                                    Text(Format.time(o.startMinute), fontSize = 11.sp, color = Color.White, fontWeight = FontWeight.Bold)
                                    Text(o.group.shortName.ifBlank { o.group.name }, fontSize = 11.sp, color = Color.White, fontWeight = FontWeight.Bold, lineHeight = 13.sp,
                                        maxLines = 2, overflow = TextOverflow.Ellipsis)
                                }
                                if (o.done) Box(Modifier.align(Alignment.BottomEnd).size(18.dp).clip(CircleShape).background(Color.White), contentAlignment = Alignment.Center) {
                                    Icon(Icons.Default.Check, "Đã dạy", tint = Earned, modifier = Modifier.size(13.dp))
                                }
                            }
                        }
                        if (d == today && now in firstHour * 60 until lastHour * 60) {
                            val red = Color(0xFFE5484D)
                            Box(Modifier.offset(y = y(now) - 5.dp).fillMaxWidth().height(10.dp)) {
                                Canvas(Modifier.fillMaxSize()) {
                                    drawLine(red, Offset(0f, size.height / 2), Offset(size.width, size.height / 2), strokeWidth = 2.dp.toPx())
                                    drawCircle(red, radius = 5.dp.toPx(), center = Offset(0f, size.height / 2))
                                }
                            }
                        }
                    }
                }
            }
            // Folded empty stretches across the whole week: tap to open them.
            val stripe = MaterialTheme.colorScheme.primary.copy(alpha = .07f)
            bands.filter { it.folded }.forEach { b ->
                Row(Modifier.offset(y = y(b.fromHour * 60)).fillMaxWidth().height(foldHeight).clip(RoundedCornerShape(8.dp))
                    .background(Brush.linearGradient(0f to stripe, .5f to stripe, .5f to Color.Transparent, 1f to Color.Transparent, start = Offset.Zero, end = Offset(24f, 24f),
                        tileMode = androidx.compose.ui.graphics.TileMode.Repeated))
                    .clickable { unfolded = unfolded + (b.fromHour until b.toHour) }, verticalAlignment = Alignment.CenterVertically) {
                    Spacer(Modifier.weight(1f))
                    Icon(Icons.Default.UnfoldMore, null, Modifier.size(18.dp), tint = MaterialTheme.colorScheme.onSurfaceVariant)
                    Text("%02d:00 – %02d:00 trống".format(b.fromHour, b.toHour), fontSize = 14.sp, fontWeight = FontWeight.SemiBold, color = MaterialTheme.colorScheme.onSurfaceVariant)
                    Spacer(Modifier.weight(1f))
                }
            }
        }
    }
}
