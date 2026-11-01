package vn.mrsbi.handout.util

import vn.mrsbi.handout.data.AppData
import vn.mrsbi.handout.data.Rules
import java.time.YearMonth

/** Month fee table as CSV (UTF-8 with BOM so Excel shows Vietnamese correctly). */
object Csv {
    private fun cell(s: String) = "\"" + s.replace("\"", "\"\"") + "\""

    fun month(data: AppData, month: YearMonth): String {
        val summary = Rules.monthSummary(data, month)
        val sb = StringBuilder("﻿")
        sb.append("Tháng,Học sinh / Lớp,Lớp,Buổi có mặt,Buổi vắng,Học phí (đ),Đã thu,Ngày thu\n")
        for (line in summary.lines) {
            sb.append(listOf(
                Format.month(month), line.title, line.subtitle, line.present.toString(), line.absent.toString(), line.amount.toString(),
                if (line.paidAt != null) "Đã thu" else "Chưa thu",
                line.paidAt?.let { java.text.SimpleDateFormat("dd/MM/yyyy").format(java.util.Date(it)) } ?: "",
            ).joinToString(",") { cell(it) }).append('\n')
        }
        sb.append(cell("Tổng")).append(",,,,,").append(summary.total).append(",").append(cell("Đã thu " + summary.paid)).append(",\n")
        return sb.toString()
    }
}
