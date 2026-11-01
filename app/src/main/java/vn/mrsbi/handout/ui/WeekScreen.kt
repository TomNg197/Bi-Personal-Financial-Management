package vn.mrsbi.handout.ui

import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxHeight
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.offset
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.KeyboardArrowLeft
import androidx.compose.material.icons.automirrored.filled.KeyboardArrowRight
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableLongStateOf
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import vn.mrsbi.handout.data.AppData
import vn.mrsbi.handout.data.Rules
import vn.mrsbi.handout.data.Store
import vn.mrsbi.handout.util.Format
import java.time.LocalDate

/** Weekly timetable (Mon–Sun): one column per day, blocks placed by start time and length. */
@Composable
fun WeekScreen(store: Store, data: AppData) {
    var monday by rememberSaveable { mutableLongStateOf(LocalDate.now().let { it.minusDays(it.dayOfWeek.value - 1L) }.toEpochDay()) }
    val days = (0..6).map { monday + it }
    val week = days.associateWith { Rules.occurrencesOn(data, it) }
    val all = week.values.flatten()
    val firstHour = ((all.minOfOrNull { it.startMinute } ?: (8 * 60)) / 60).coerceAtMost(8)
    val lastHour = ((all.maxOfOrNull { it.startMinute + it.group.durationMin } ?: (20 * 60)) / 60 + 1).coerceAtLeast(20).coerceAtMost(24)
    val hourHeight = 56.dp
    Column(Modifier.fillMaxSize()) {
        Row(Modifier.fillMaxWidth().padding(8.dp), verticalAlignment = Alignment.CenterVertically) {
            IconButton(onClick = { monday -= 7 }) { Icon(Icons.AutoMirrored.Filled.KeyboardArrowLeft, "Tuần trước") }
            Column(Modifier.weight(1f), horizontalAlignment = Alignment.CenterHorizontally) {
                Text("Thời khoá biểu", style = MaterialTheme.typography.titleLarge, fontWeight = FontWeight.ExtraBold, color = Violet)
                Text(Format.day(days.first()) + " – " + Format.day(days.last()) + " · " + all.size + " buổi", style = MaterialTheme.typography.bodyMedium)
            }
            IconButton(onClick = { monday += 7 }) { Icon(Icons.AutoMirrored.Filled.KeyboardArrowRight, "Tuần sau") }
        }
        Row(Modifier.fillMaxWidth().padding(start = 36.dp)) {
            days.forEach { d ->
                val today = d == Format.today()
                Column(Modifier.weight(1f), horizontalAlignment = Alignment.CenterHorizontally) {
                    Text(Format.weekdayShort(LocalDate.ofEpochDay(d).dayOfWeek.value), fontWeight = FontWeight.Bold, color = if (today) Violet else Color.Unspecified)
                    Text(Format.shortDay(d), style = MaterialTheme.typography.labelSmall, color = if (today) Violet else MaterialTheme.colorScheme.onSurfaceVariant)
                }
            }
        }
        Row(Modifier.fillMaxSize().verticalScroll(rememberScrollState()).padding(bottom = 16.dp)) {
            Column(Modifier.width(36.dp)) {
                for (h in firstHour until lastHour) Text("%02d".format(h), Modifier.height(hourHeight).fillMaxWidth(), fontSize = 11.sp, textAlign = TextAlign.Center,
                    color = MaterialTheme.colorScheme.onSurfaceVariant)
            }
            days.forEach { d ->
                Box(Modifier.weight(1f).height(hourHeight * (lastHour - firstHour)).border(0.5.dp, MaterialTheme.colorScheme.surfaceVariant)
                    .background(if (d == Format.today()) Violet.copy(alpha = .08f) else Color.White)) {
                    for (h in 1 until lastHour - firstHour) Box(Modifier.fillMaxWidth().height(0.5.dp).offset(y = hourHeight * h).background(MaterialTheme.colorScheme.surfaceVariant))
                    week.getValue(d).forEach { o ->
                        val top = hourHeight * ((o.startMinute - firstHour * 60) / 60f)
                        val height = hourHeight * (o.group.durationMin / 60f)
                        val color = Color(o.group.color)
                        // Board per class: its own colour as a soft vertical gradient; taught sessions fade.
                        val board = androidx.compose.ui.graphics.Brush.verticalGradient(listOf(color, lerpColor(color, Color.White, .25f)))
                        Box(Modifier.offset(y = top).padding(horizontal = 2.dp, vertical = 1.dp).fillMaxWidth().height(height).clip(RoundedCornerShape(8.dp))
                            .background(board).background(if (o.done) Color.White.copy(alpha = .45f) else Color.Transparent).padding(4.dp)) {
                            Column(Modifier.fillMaxHeight()) {
                                Text(Format.time(o.startMinute), fontSize = 9.sp, color = Color.White, fontWeight = FontWeight.Bold)
                                Text(o.group.name, fontSize = 10.sp, color = Color.White, lineHeight = 11.sp, maxLines = 3, overflow = TextOverflow.Ellipsis)
                            }
                        }
                    }
                }
            }
        }
    }
}
