package vn.mrsbi.handout.util

import android.content.Context
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import kotlinx.serialization.json.Json
import kotlinx.serialization.json.JsonArray
import kotlinx.serialization.json.JsonPrimitive
import kotlinx.serialization.json.buildJsonArray
import kotlinx.serialization.json.buildJsonObject
import kotlinx.serialization.json.jsonObject
import kotlinx.serialization.json.jsonPrimitive
import kotlinx.serialization.json.put
import vn.mrsbi.handout.data.AppData
import vn.mrsbi.handout.data.Rules
import java.net.HttpURLConnection
import java.net.URL
import java.time.LocalDate
import java.time.YearMonth

/**
 * Mirrors the monthly fee table to the user's Google Sheet through their Apps Script web app (one tab per month).
 * Only months whose table changed since the last successful sync are sent. URL + key stay on the phone (never in git).
 */
class SheetSync(context: Context) {
    private val prefs = context.getSharedPreferences("sheet", Context.MODE_PRIVATE)
    var url: String get() = prefs.getString("url", "") ?: ""; set(v) { prefs.edit().putString("url", v.trim()).apply() }
    var key: String get() = prefs.getString("key", "") ?: ""; set(v) { prefs.edit().putString("key", v.trim()).apply() }
    var status: String get() = prefs.getString("status", "") ?: ""; private set(v) { prefs.edit().putString("status", v).apply() }
    val configured get() = url.startsWith("https://") && key.isNotBlank()

    /** Accepts the one-line config "URL|KEY" (sent in chat) so nobody types the long link on the phone. */
    fun importConfig(text: String): Boolean {
        val parts = text.trim().split('|')
        if (parts.size != 2 || !parts[0].trim().startsWith("https://script.google.com/")) return false
        url = parts[0]; key = parts[1]; prefs.edit().remove("hashes").apply(); return true
    }

    private fun rows(data: AppData, month: YearMonth) = buildJsonArray {
        for (line in Rules.monthSummary(data, month).lines) {
            add(buildJsonArray {
                add(JsonPrimitive(if (line.key.startsWith("s:")) "Cá nhân" else "Trung tâm"))
                add(JsonPrimitive(line.title))
                add(JsonPrimitive(line.subtitle))
                add(JsonPrimitive(line.present)); add(JsonPrimitive(line.absent)); add(JsonPrimitive(line.amount))
                add(JsonPrimitive(if (line.paidAt != null) "Đã thu" else "Chưa thu"))
                add(JsonPrimitive(line.paidAt?.let { java.text.SimpleDateFormat("dd/MM/yyyy").format(java.util.Date(it)) } ?: ""))
            })
        }
    }

    /** Months that have taught sessions or payments. */
    private fun months(data: AppData): List<YearMonth> =
        (data.sessions.filter { it.done }.map { YearMonth.from(LocalDate.ofEpochDay(it.day)) } + data.payments.map { YearMonth.parse(it.month) }).distinct().sorted()

    /** Sends changed months; force = resend everything. Returns a status line. */
    suspend fun sync(data: AppData, force: Boolean = false): String = withContext(Dispatchers.IO) {
        if (!configured) return@withContext "Chưa cài Google Sheet"
        val known = if (force) emptyMap() else (prefs.getString("hashes", "") ?: "").split(';').filter { '=' in it }.associate { it.substringBefore('=') to it.substringAfter('=') }
        val changed = months(data).map { it to rows(data, it) }.filter { (m, r) -> known[m.toString()] != r.toString().hashCode().toString() }
        if (changed.isEmpty()) return@withContext status.ifBlank { "Google Sheet đã khớp" }
        val body = buildJsonObject {
            put("key", key)
            put("months", JsonArray(changed.map { (m, r) -> buildJsonObject { put("month", m.toString()); put("title", "%02d/%d".format(m.monthValue, m.year)); put("rows", r) } }))
        }.toString()
        val result = runCatching {
            val c = (URL(url).openConnection() as HttpURLConnection).apply {
                requestMethod = "POST"; doOutput = true; connectTimeout = 10000; readTimeout = 60000; instanceFollowRedirects = true
                setRequestProperty("Content-Type", "application/json; charset=utf-8")
            }
            c.outputStream.use { it.write(body.toByteArray()) }
            val text = (if (c.responseCode in 200..399) c.inputStream else c.errorStream).use { it.readBytes().decodeToString() }
            val reply = Json.parseToJsonElement(text).jsonObject
            if (reply["ok"]?.jsonPrimitive?.content != "true") error(reply["error"]?.jsonPrimitive?.content ?: "lỗi không rõ")
        }
        val now = java.text.SimpleDateFormat("HH:mm dd/MM").format(java.util.Date())
        status = result.fold(
            onSuccess = {
                val all = known + changed.associate { (m, r) -> m.toString() to r.toString().hashCode().toString() }
                prefs.edit().putString("hashes", all.entries.joinToString(";") { it.key + "=" + it.value }).apply()
                "Đã cập nhật Google Sheet lúc $now (${changed.size} tháng)"
            },
            onFailure = { "Google Sheet lỗi lúc $now: " + (it.message ?: it.javaClass.simpleName) },
        )
        status
    }
}
