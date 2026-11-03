package vn.mrsbi.handout.data

import java.time.LocalDate
import java.time.YearMonth

/** One session as shown on a given day: generated from a class schedule, possibly moved or already taught. */
data class Occurrence(val group: ClassGroup, val originalDay: Long, val day: Long, val startMinute: Int, val record: SessionRecord?) {
    /** Length of the session as scheduled on its original weekday. */
    val durationMin get() = group.timeOn(LocalDate.ofEpochDay(originalDay).dayOfWeek.value).duration
    val done get() = record?.done == true
    val moved get() = record != null && record.day != originalDay
    val key get() = group.id + "@" + originalDay
}

/** Fee line of the month: one per student (personal classes) or one per center class. */
data class FeeLine(val key: String, val title: String, val subtitle: String, val present: Int, val absent: Int, val amount: Long, val paidAt: Long?)

data class MonthSummary(val lines: List<FeeLine>) {
    val total get() = lines.sumOf { it.amount }
    val paid get() = lines.filter { it.paidAt != null }.sumOf { it.amount }
    val unpaid get() = total - paid
}

/** Pure rules (no Android): which sessions fall on a day, what a taught session earns, the month's fee lines. */
object Rules {
    fun scheduledOn(group: ClassGroup, day: Long): Boolean {
        if (group.archived) return false
        if (!group.repeat) return day == group.startDay
        if (day < group.startDay || (group.endDay != null && day > group.endDay)) return false
        return LocalDate.ofEpochDay(day).dayOfWeek.value in group.weekdays
    }

    fun occurrencesOn(data: AppData, day: Long): List<Occurrence> {
        val byKey = data.sessions.associateBy { it.classId + "@" + it.originalDay }
        val classes = data.classes.associateBy { it.id }
        val result = mutableListOf<Occurrence>()
        for (group in data.classes) {
            if (!scheduledOn(group, day)) continue
            val record = byKey[group.id + "@" + day]
            if (record != null && record.day != day) continue   // postponed to another day
            result += Occurrence(group, day, day, record?.startMinute ?: group.timeOn(LocalDate.ofEpochDay(day).dayOfWeek.value).start, record)
        }
        for (record in data.sessions) {   // sessions moved onto this day
            if (record.day != day || record.originalDay == day) continue
            val group = classes[record.classId] ?: continue
            result += Occurrence(group, record.originalDay, day, record.startMinute, record)
        }
        return result.sortedBy { it.startMinute }
    }

    /** The first session after [day] (looking ahead up to [days] days), for the "next class" card on a free day. */
    fun nextOccurrence(data: AppData, day: Long, days: Int = 60): Occurrence? =
        (1..days).asSequence().map { day + it }.firstNotNullOfOrNull { d -> occurrencesOn(data, d).firstOrNull() }

    private fun overlaps(aStart: Int, aLen: Int, bStart: Int, bLen: Int) = aStart < bStart + bLen && bStart < aStart + aLen

    /** Keys of the sessions on one day that overlap another session that day. */
    fun clashing(list: List<Occurrence>): Set<String> = list.filter { a ->
        list.any { b -> b.key != a.key && overlaps(a.startMinute, a.durationMin, b.startMinute, b.durationMin) }
    }.map { it.key }.toSet()

    /**
     * First session of another class that [group] would overlap, looking [days] days ahead from [from]
     * (used before saving a class so double bookings are caught up front).
     */
    fun findClash(data: AppData, group: ClassGroup, from: Long, days: Int = 56): Occurrence? {
        val others = data.copy(classes = data.classes.filter { it.id != group.id })
        val start = maxOf(from, group.startDay)
        for (day in start until start + days) {
            if (!scheduledOn(group, day)) continue
            val t = group.timeOn(LocalDate.ofEpochDay(day).dayOfWeek.value)
            occurrencesOn(others, day).firstOrNull { overlaps(t.start, t.duration, it.startMinute, it.durationMin) }?.let { return it }
            if (!group.repeat) break
        }
        return null
    }

    /** What a session earns when marked done with these absent students. */
    fun charges(group: ClassGroup, absent: Set<String>, guests: Map<String, Long> = emptyMap()): Map<String, Long> =
        if (group.type == ClassType.CENTER) mapOf(SessionRecord.CENTER_KEY to group.centerFee)
        else (group.members.associate { it.studentId to it.fee } + guests).filterKeys { it !in absent }

    fun earned(record: SessionRecord) = if (record.done) record.charges.values.sum() else 0L

    fun monthKey(month: YearMonth) = month.toString()   // yyyy-MM

    fun inMonth(day: Long, month: YearMonth) = YearMonth.from(LocalDate.ofEpochDay(day)) == month

    fun monthSummary(data: AppData, month: YearMonth): MonthSummary {
        val done = data.sessions.filter { it.done && inMonth(it.day, month) }
        val classes = data.classes.associateBy { it.id }
        val paid = data.payments.filter { it.month == monthKey(month) }.associate { it.key to it.paidAt }
        val lines = mutableListOf<FeeLine>()
        for (student in data.students) {
            var present = 0; var absent = 0; var amount = 0L
            val classNames = linkedSetOf<String>()
            for (record in done) {
                val group = classes[record.classId] ?: continue
                val involved = student.id in record.charges || student.id in record.absent
                if (group.type != ClassType.PERSONAL || !involved) continue
                classNames += group.name
                if (student.id in record.absent) absent++ else if (student.id in record.charges) { present++; amount += record.charges.getValue(student.id) }
            }
            if (present + absent == 0) continue
            val key = "s:" + student.id
            lines += FeeLine(key, student.name, classNames.joinToString(", "), present, absent, amount, paid[key])
        }
        for (group in data.classes.filter { it.type == ClassType.CENTER }) {
            val taught = done.filter { it.classId == group.id }
            if (taught.isEmpty()) continue
            val key = "c:" + group.id
            lines += FeeLine(key, group.name, group.content.ifBlank { "Lớp trung tâm" }, taught.size, 0, taught.sumOf { earned(it) }, paid[key])
        }
        return MonthSummary(lines)
    }

    /** Text the teacher sends to a parent (or a centre) after the month is closed. */
    /** How the teacher addresses a child: the name without the family name ("Hoàng Đức Anh" → "Đức Anh"). */
    fun callName(full: String): String { val w = full.trim().split(Regex("\\s+")); return if (w.size >= 3) w.drop(1).joinToString(" ") else w.last() }

    fun feeMessage(data: AppData, line: FeeLine, month: YearMonth, signature: String = "Cô Chi"): String {
        val m = month.monthValue
        val records = data.sessions.filter { it.done && inMonth(it.day, month) }
        val prices = if (line.key.startsWith("s:")) records.mapNotNull { it.charges[line.key.removePrefix("s:")] }
            else records.filter { it.classId == line.key.removePrefix("c:") }.map { earned(it) }
        val money = { v: Long -> "%,d".format(v).replace(',', '.') + "đ" }
        val how = if (prices.isNotEmpty() && prices.distinct().size == 1) "${line.present} buổi × ${money(prices.first())} = ${money(line.amount)}"
            else "${line.present} buổi = ${money(line.amount)}"
        val sign = signature.trim().let { if (it.isEmpty()) "" else " $it." }
        return if (line.key.startsWith("s:")) "Chào phụ huynh bạn ${callName(line.title)}, học phí tháng $m của con là $how (vắng ${line.absent} buổi). Cảm ơn phụ huynh ạ.$sign"
            else "Chào trung tâm, học phí tháng $m lớp ${line.title} là $how. Cảm ơn ạ.$sign"
    }

    /** Every taught session of one student in a month: day, class, present?, amount. */
    fun studentSessions(data: AppData, studentId: String, month: YearMonth): List<Triple<SessionRecord, ClassGroup, Boolean>> {
        val classes = data.classes.associateBy { it.id }
        return data.sessions.filter { it.done && inMonth(it.day, month) }.mapNotNull { record ->
            val group = classes[record.classId] ?: return@mapNotNull null
            when {
                studentId in record.absent -> Triple(record, group, false)
                studentId in record.charges -> Triple(record, group, true)
                else -> null
            }
        }.sortedBy { it.first.day }
    }
}
