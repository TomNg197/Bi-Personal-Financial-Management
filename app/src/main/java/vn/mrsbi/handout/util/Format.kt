package vn.mrsbi.handout.util

import java.time.LocalDate
import java.time.YearMonth
import java.time.format.DateTimeFormatter
import java.util.Locale

object Format {
    private val vi = Locale("vi", "VN")
    private val dayFormat = DateTimeFormatter.ofPattern("dd/MM/yyyy", vi)
    private val shortDay = DateTimeFormatter.ofPattern("dd/MM", vi)
    private val weekdays = listOf("Thứ 2", "Thứ 3", "Thứ 4", "Thứ 5", "Thứ 6", "Thứ 7", "Chủ nhật")
    private val weekdaysShort = listOf("T2", "T3", "T4", "T5", "T6", "T7", "CN")

    fun money(v: Long): String = "%,d".format(v).replace(',', '.') + "đ"
    fun time(minute: Int) = "%02d:%02d".format(minute / 60, minute % 60)
    fun range(start: Int, duration: Int) = time(start) + " – " + time((start + duration) % (24 * 60))
    fun day(epochDay: Long): String = LocalDate.ofEpochDay(epochDay).format(dayFormat)
    fun shortDay(epochDay: Long): String = LocalDate.ofEpochDay(epochDay).format(shortDay)
    fun weekday(epochDay: Long) = weekdays[LocalDate.ofEpochDay(epochDay).dayOfWeek.value - 1]
    fun weekdayShort(iso: Int) = weekdaysShort[iso - 1]
    fun fullDay(epochDay: Long) = weekday(epochDay) + ", " + day(epochDay)
    fun month(m: YearMonth) = "Tháng " + m.monthValue + "/" + m.year
    fun today(): Long = LocalDate.now().toEpochDay()

    /** "150.000" / "150000" / "150k" → 150000. */
    fun parseMoney(text: String): Long? {
        val t = text.trim().lowercase()
        if (t.isEmpty()) return 0
        val k = t.endsWith("k")
        val digits = t.removeSuffix("k").filter { it.isDigit() }
        val v = digits.toLongOrNull() ?: return null
        return if (k) v * 1000 else v
    }
    fun moneyInput(v: Long) = if (v == 0L) "" else "%,d".format(v).replace(',', '.')
}
