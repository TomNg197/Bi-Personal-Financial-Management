package vn.mrsbi.handout.data

import java.time.YearMonth

/** All edits go through here so every change is saved and backed up the same way. */
fun Store.saveStudent(student: Student) = update { d ->
    val exists = d.students.any { it.id == student.id }
    d.copy(students = if (exists) d.students.map { if (it.id == student.id) student else it } else d.students + student)
}

/** Students who already have taught sessions are archived (history and fees stay correct); others are removed. */
fun Store.deleteStudent(id: String) = update { d ->
    val used = d.sessions.any { id in it.charges || id in it.absent }
    d.copy(
        students = if (used) d.students.map { if (it.id == id) it.copy(archived = true) else it } else d.students.filter { it.id != id },
        classes = d.classes.map { c -> c.copy(members = c.members.filter { it.studentId != id }) },
    )
}

fun Store.saveClass(group: ClassGroup) = update { d ->
    val exists = d.classes.any { it.id == group.id }
    d.copy(classes = if (exists) d.classes.map { if (it.id == group.id) group else it } else d.classes + group)
}

fun Store.deleteClass(id: String) = update { d ->
    val used = d.sessions.any { it.classId == id && it.done }
    d.copy(
        classes = if (used) d.classes.map { if (it.id == id) it.copy(archived = true) else it } else d.classes.filter { it.id != id },
        sessions = if (used) d.sessions else d.sessions.filter { it.classId != id },
    )
}

private fun AppData.withRecord(record: SessionRecord) = copy(
    sessions = sessions.filterNot { it.classId == record.classId && it.originalDay == record.originalDay } + record,
)

fun Store.markDone(o: Occurrence, absent: Set<String>) = update { d ->
    val base = d.sessions.firstOrNull { it.classId == o.group.id && it.originalDay == o.originalDay } ?: SessionRecord(o.group.id, o.originalDay, o.day, o.startMinute)
    d.withRecord(base.copy(done = true, absent = absent.toList(), charges = Rules.charges(o.group, absent, base.guests), doneAt = System.currentTimeMillis()))
}

/** "Học ghép": a student joins only this session (kept on the session, not on the class). */
fun Store.setGuests(o: Occurrence, guests: Map<String, Long>) = update { d ->
    val base = d.sessions.firstOrNull { it.classId == o.group.id && it.originalDay == o.originalDay } ?: SessionRecord(o.group.id, o.originalDay, o.day, o.startMinute)
    d.withRecord(base.copy(guests = guests))
}

/** Undo for the "Xong buổi" snackbar: put back the record exactly as it was before (or none). */
fun Store.restoreRecord(classId: String, originalDay: Long, previous: SessionRecord?) = update { d ->
    d.copy(sessions = d.sessions.filterNot { it.classId == classId && it.originalDay == originalDay } + listOfNotNull(previous))
}

fun Store.reopen(o: Occurrence) = update { d ->
    val record = o.record ?: return@update d
    d.withRecord(record.copy(done = false, absent = emptyList(), charges = emptyMap(), doneAt = null))
}

/** Move only this session; the weekly schedule is unchanged. */
fun Store.postpone(o: Occurrence, newDay: Long, newMinute: Int) = update { d ->
    val base = o.record ?: SessionRecord(o.group.id, o.originalDay, o.day, o.startMinute)
    d.withRecord(base.copy(day = newDay, startMinute = newMinute))
}

fun Store.closeMonth(month: YearMonth) = update { d -> d.copy(closedMonths = d.closedMonths + (Rules.monthKey(month) to System.currentTimeMillis())) }

fun Store.reopenMonth(month: YearMonth) = update { d -> d.copy(closedMonths = d.closedMonths - Rules.monthKey(month)) }

fun Store.markNoticeSent(month: YearMonth, key: String) = update { d -> d.copy(sentNotices = d.sentNotices + (Rules.monthKey(month) + "|" + key to System.currentTimeMillis())) }

fun Store.setPaid(month: YearMonth, key: String, paid: Boolean) = update { d ->
    val m = Rules.monthKey(month)
    val others = d.payments.filterNot { it.month == m && it.key == key }
    d.copy(payments = if (paid) others + Payment(m, key, System.currentTimeMillis()) else others)
}
