package vn.mrsbi.handout.data

import android.content.Context
import android.net.Uri
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.launch
import kotlinx.serialization.json.Json
import java.io.File
import java.util.UUID

/**
 * Single JSON document kept in app storage. Every change is written atomically and then mirrored to the backup
 * file the user picked on Google Drive (Storage Access Framework), so a new phone restores from that file.
 */
class Store(private val context: Context) {
    private val json = Json { ignoreUnknownKeys = true; encodeDefaults = true; prettyPrint = false }
    private val file = File(context.filesDir, "data.json")
    private val prefs = context.getSharedPreferences("settings", Context.MODE_PRIVATE)
    private val scope = CoroutineScope(SupervisorJob() + Dispatchers.IO)
    private val _data = MutableStateFlow(load())
    val data: StateFlow<AppData> = _data

    private val _backupStatus = MutableStateFlow(prefs.getString("backupStatus", "") ?: "")
    val backupStatus: StateFlow<String> = _backupStatus

    var backupUri: Uri?
        get() = prefs.getString("backupUri", null)?.let(Uri::parse)
        set(value) { prefs.edit().putString("backupUri", value?.toString()).apply() }
    var lockEnabled: Boolean
        get() = prefs.getBoolean("lock", true)
        set(value) { prefs.edit().putBoolean("lock", value).apply() }

    private fun load(): AppData = runCatching { json.decodeFromString<AppData>(file.readText()) }.getOrElse { AppData() }

    fun update(change: (AppData) -> AppData) {
        val next = change(_data.value).copy(modifiedAt = System.currentTimeMillis())
        _data.value = next
        val text = json.encodeToString(AppData.serializer(), next)
        val tmp = File(file.parentFile, "data.json.tmp")
        tmp.writeText(text); tmp.renameTo(file)
        backupUri?.let { uri -> scope.launch { writeBackup(uri, text) } }
    }

    private fun writeBackup(uri: Uri, text: String) {
        val status = runCatching {
            context.contentResolver.openOutputStream(uri, "wt")!!.use { it.write(text.toByteArray()) }
            "Đã sao lưu lúc " + java.text.SimpleDateFormat("HH:mm dd/MM", java.util.Locale("vi")).format(java.util.Date())
        }.getOrElse { "Sao lưu lỗi: " + (it.message ?: it.javaClass.simpleName) }
        prefs.edit().putString("backupStatus", status).apply()
        _backupStatus.value = status
    }

    fun backupNow() { backupUri?.let { uri -> val text = json.encodeToString(AppData.serializer(), _data.value); scope.launch { writeBackup(uri, text) } } }

    /** Replace everything with the content of a backup file. Returns an error message or null. */
    fun restore(uri: Uri): String? = runCatching {
        val text = context.contentResolver.openInputStream(uri)!!.use { it.readBytes().decodeToString() }
        val restored = json.decodeFromString<AppData>(text)
        update { restored }
        null
    }.getOrElse { "Không đọc được file: " + (it.message ?: "") }

    companion object { fun newId(): String = UUID.randomUUID().toString().substring(0, 8) }
}
