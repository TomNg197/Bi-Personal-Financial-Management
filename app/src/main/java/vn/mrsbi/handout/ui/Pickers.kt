package vn.mrsbi.handout.ui

import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Add
import androidx.compose.material.icons.filled.Search
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedTextField
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import vn.mrsbi.handout.data.AppData
import vn.mrsbi.handout.data.Student
import vn.mrsbi.handout.util.Format
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.DatePicker
import androidx.compose.material3.DatePickerDialog
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.material3.TimePicker
import androidx.compose.material3.rememberDatePickerState
import androidx.compose.material3.rememberTimePickerState
import androidx.compose.runtime.Composable
import java.time.Instant
import java.time.LocalDate
import java.time.ZoneOffset

/** Material date picker working in epoch days (the picker itself uses UTC millis). */
@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun DayPickerDialog(initial: Long, onDismiss: () -> Unit, onPick: (Long) -> Unit) {
    val state = rememberDatePickerState(initialSelectedDateMillis = LocalDate.ofEpochDay(initial).atStartOfDay().toInstant(ZoneOffset.UTC).toEpochMilli())
    DatePickerDialog(
        onDismissRequest = onDismiss,
        confirmButton = {
            TextButton(onClick = {
                state.selectedDateMillis?.let { onPick(Instant.ofEpochMilli(it).atZone(ZoneOffset.UTC).toLocalDate().toEpochDay()) }
            }) { Text("Chọn") }
        },
        dismissButton = { TextButton(onClick = onDismiss) { Text("Huỷ") } },
    ) { DatePicker(state) }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun TimePickerDialog(initialMinute: Int, onDismiss: () -> Unit, onPick: (Int) -> Unit) {
    val state = rememberTimePickerState(initialHour = initialMinute / 60, initialMinute = initialMinute % 60, is24Hour = true)
    AlertDialog(
        onDismissRequest = onDismiss,
        confirmButton = { TextButton(onClick = { onPick(state.hour * 60 + state.minute) }) { Text("Chọn") } },
        dismissButton = { TextButton(onClick = onDismiss) { Text("Huỷ") } },
        text = { TimePicker(state) },
    )
}

/** Choose from the students already saved in Quản lý (searchable); [exclude] hides students already added. */
@Composable
fun StudentPickerDialog(data: AppData, exclude: Set<String>, title: String, onDismiss: () -> Unit,
                        header: @Composable () -> Unit = {}, onPick: (Student) -> Unit) {
    var query by remember { mutableStateOf("") }
    val list = data.students.filter { !it.archived && it.id !in exclude && it.name.contains(query.trim(), ignoreCase = true) }.sortedWith(compareBy(Format.byGivenName) { it.name })
    AlertDialog(onDismissRequest = onDismiss, confirmButton = {}, dismissButton = { TextButton(onClick = onDismiss) { Text("Xong") } },
        title = { Text(title) },
        text = {
            Column {
                OutlinedTextField(query, { query = it }, Modifier.fillMaxWidth(), placeholder = { Text("Tìm theo tên") }, singleLine = true,
                    leadingIcon = { Icon(Icons.Default.Search, null) })
                header()
                if (list.isEmpty()) Text(if (data.students.none { !it.archived }) "Chưa có học sinh nào. Thêm ở tab Quản lý → Học sinh trước." else "Không còn học sinh nào khác.",
                    color = Muted, modifier = Modifier.padding(vertical = 12.dp))
                LazyColumn(Modifier.fillMaxWidth().height((56 * list.size.coerceIn(1, 6)).dp)) {
                    items(list, key = { it.id }) { s ->
                        Row(Modifier.fillMaxWidth().clickable { onPick(s) }.padding(vertical = 8.dp), verticalAlignment = Alignment.CenterVertically) {
                            Column(Modifier.weight(1f)) {
                                Text(s.name, style = MaterialTheme.typography.bodyLarge)
                                Text(Format.money(s.fee) + "/buổi" + (s.age?.let { " · $it tuổi" } ?: ""), style = MaterialTheme.typography.bodySmall, color = Muted)
                            }
                            Icon(Icons.Default.Add, "Thêm", tint = Violet)
                        }
                        HorizontalDivider()
                    }
                }
            }
        })
}
