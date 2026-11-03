package vn.mrsbi.handout.data

import kotlinx.serialization.Serializable

/** PERSONAL: paid per present student (each student can have their own fee). CENTER: a fixed amount per session. */
@Serializable
enum class ClassType { PERSONAL, CENTER }

@Serializable
data class Student(
    val id: String,
    val name: String,
    val age: Int? = null,
    /** Default fee per session (VND), copied into a class when the student joins it. */
    val fee: Long = 0,
    val note: String = "",
    val archived: Boolean = false,
)

/** Start and length of a weekly session on one weekday (minutes). */
@Serializable
data class DayTime(val start: Int, val duration: Int)

/** A student inside a personal class, with the fee agreed for that class (discounts / exemptions). */
@Serializable
data class Member(val studentId: String, val fee: Long)

@Serializable
data class ClassGroup(
    val id: String,
    val name: String,
    /** What is taught, e.g. "IELTS", "Lớp trung tâm". */
    val content: String = "",
    val type: ClassType = ClassType.PERSONAL,
    val color: Long = 0xFFE85D8E,
    /** CENTER classes: amount earned per taught session. */
    val centerFee: Long = 0,
    val members: List<Member> = emptyList(),
    /** true: weekly on [weekdays]; false: a single session on [startDay] (make-up / intensive class). */
    val repeat: Boolean = true,
    /** ISO weekdays, 1 = Monday … 7 = Sunday. */
    val weekdays: List<Int> = emptyList(),
    val startMinute: Int = 18 * 60,
    val durationMin: Int = 90,
    /** Epoch day of the first session (or of the only session when [repeat] is false). */
    val startDay: Long,
    val endDay: Long? = null,
    val archived: Boolean = false,
    /** Taught online, or offline at [address]. */
    val online: Boolean = false,
    val address: String = "",
    /** Weekday (ISO) → its own time, for classes whose sessions start at different hours. Missing days use [startMinute]/[durationMin]. */
    val dayTimes: Map<Int, DayTime> = emptyMap(),
) {
    fun timeOn(weekday: Int): DayTime = if (repeat) dayTimes[weekday] ?: DayTime(startMinute, durationMin) else DayTime(startMinute, durationMin)
}

/**
 * Stored only once a scheduled session is touched: taught (done), postponed (day/startMinute changed) or reopened.
 * Identified by class + the day it was originally scheduled on.
 */
@Serializable
data class SessionRecord(
    val classId: String,
    val originalDay: Long,
    val day: Long,
    val startMinute: Int,
    val done: Boolean = false,
    val absent: List<String> = emptyList(),
    /** Money earned when marked done: studentId → fee for personal classes, [CENTER_KEY] → fee for center classes. */
    val charges: Map<String, Long> = emptyMap(),
    val doneAt: Long? = null,
    /** Students joining only this session ("học ghép"): studentId → fee for this session. */
    val guests: Map<String, Long> = emptyMap(),
) {
    companion object { const val CENTER_KEY = "#class" }
}

/** A month's fee marked as collected. key = "s:<studentId>" (personal classes) or "c:<classId>" (center classes). */
@Serializable
data class Payment(val month: String, val key: String, val paidAt: Long)

@Serializable
data class AppData(
    val version: Int = 1,
    val students: List<Student> = emptyList(),
    val classes: List<ClassGroup> = emptyList(),
    val sessions: List<SessionRecord> = emptyList(),
    val payments: List<Payment> = emptyList(),
    val modifiedAt: Long = 0,
)
