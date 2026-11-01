package vn.mrsbi.handout.data

import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Test
import java.time.LocalDate
import java.time.YearMonth

/** Spec acceptance: personal class of 4 (one with a discount) + a center class; one absence; postponing moves one session only. */
class RulesTest {
    private val monday = LocalDate.of(2026, 11, 2).toEpochDay()
    private val students = listOf(Student("a", "An", fee = 150_000), Student("b", "Bình", fee = 150_000), Student("c", "Chi", fee = 150_000), Student("d", "Dũng", fee = 150_000))
    private val personal = ClassGroup("p", "IELTS nhóm", type = ClassType.PERSONAL, weekdays = listOf(1, 3), startDay = monday,
        members = listOf(Member("a", 150_000), Member("b", 150_000), Member("c", 150_000), Member("d", 50_000)))
    private val center = ClassGroup("c", "Lớp trung tâm", type = ClassType.CENTER, centerFee = 300_000, weekdays = listOf(1), startDay = monday, startMinute = 8 * 60)
    private fun base() = AppData(students = students, classes = listOf(personal, center))

    private fun done(d: AppData, o: Occurrence, absent: Set<String>) = d.copy(sessions = d.sessions.filterNot { it.classId == o.group.id && it.originalDay == o.originalDay } +
        SessionRecord(o.group.id, o.originalDay, o.day, o.startMinute, done = true, absent = absent.toList(), charges = Rules.charges(o.group, absent)))

    @Test fun mondayHasBothClassesSortedByTime() {
        val list = Rules.occurrencesOn(base(), monday)
        assertEquals(listOf("c", "p"), list.map { it.group.id })
        assertTrue(Rules.occurrencesOn(base(), monday + 1).isEmpty())
    }

    @Test fun absentStudentIsNotChargedAndDiscountApplies() {
        var d = base()
        val (centerSession, personalSession) = Rules.occurrencesOn(d, monday)
        d = done(d, personalSession, setOf("b"))
        d = done(d, centerSession, emptySet())
        val summary = Rules.monthSummary(d, YearMonth.of(2026, 11))
        assertEquals(150_000L, summary.lines.first { it.key == "s:a" }.amount)
        assertEquals(0L, summary.lines.first { it.key == "s:b" }.amount)
        assertEquals(1, summary.lines.first { it.key == "s:b" }.absent)
        assertEquals(50_000L, summary.lines.first { it.key == "s:d" }.amount)
        assertEquals(300_000L, summary.lines.first { it.key == "c:c" }.amount)
        assertEquals(150_000L * 2 + 50_000 + 300_000, summary.total)
    }

    @Test fun postponeMovesOnlyThatSession() {
        val o = Rules.occurrencesOn(base(), monday).first { it.group.id == "p" }
        val d = base().copy(sessions = listOf(SessionRecord("p", o.originalDay, monday + 1, 19 * 60)))
        assertTrue(Rules.occurrencesOn(d, monday).none { it.group.id == "p" })
        assertEquals(19 * 60, Rules.occurrencesOn(d, monday + 1).single().startMinute)
        assertEquals(1, Rules.occurrencesOn(d, monday + 2).count { it.group.id == "p" })   // Wednesday unchanged
        assertEquals(1, Rules.occurrencesOn(d, monday + 7).count { it.group.id == "p" })   // next week unchanged
    }

    @Test fun oneOffClassOnlyOnItsDay() {
        val extra = ClassGroup("x", "Lớp bù", repeat = false, startDay = monday + 5)
        val d = base().copy(classes = base().classes + extra)
        assertEquals(1, Rules.occurrencesOn(d, monday + 5).count { it.group.id == "x" })
        assertEquals(0, Rules.occurrencesOn(d, monday + 12).count { it.group.id == "x" })
    }

    @Test fun paidMarksSplitTotals() {
        var d = base()
        d = done(d, Rules.occurrencesOn(d, monday).first { it.group.id == "c" }, emptySet())
        d = d.copy(payments = listOf(Payment("2026-11", "c:c", 1L)))
        val s = Rules.monthSummary(d, YearMonth.of(2026, 11))
        assertEquals(300_000L, s.paid); assertEquals(0L, s.unpaid)
    }
}
