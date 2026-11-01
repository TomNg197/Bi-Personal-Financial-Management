package vn.mrsbi.handout.util

import android.content.Context
import android.content.Intent
import android.net.Uri
import android.os.Build
import android.provider.Settings
import androidx.core.content.FileProvider
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import kotlinx.serialization.Serializable
import kotlinx.serialization.json.Json
import vn.mrsbi.handout.BuildConfig
import java.io.File
import java.net.HttpURLConnection
import java.net.URL

/**
 * In-app update: the `update` branch of the GitHub repo holds version.json + the signed APK.
 * Same signing key ⇒ Android installs over the old app and keeps its data.
 */
object Updater {
    private const val BASE = "https://raw.githubusercontent.com/TomNg197/Bi-Personal-Financial-Management/update/"

    @Serializable
    data class Release(val versionCode: Int, val versionName: String, val apk: String, val notes: String = "")

    private val json = Json { ignoreUnknownKeys = true }

    private fun open(url: String): HttpURLConnection = (URL(url).openConnection() as HttpURLConnection).apply {
        connectTimeout = 8000; readTimeout = 20000; useCaches = false
        setRequestProperty("Cache-Control", "no-cache")
    }

    /** The newer release, or null when up to date / offline. */
    suspend fun check(): Release? = withContext(Dispatchers.IO) {
        runCatching {
            val text = open(BASE + "version.json?t=" + System.currentTimeMillis()).inputStream.use { it.readBytes().decodeToString() }
            json.decodeFromString<Release>(text).takeIf { it.versionCode > BuildConfig.VERSION_CODE }
        }.getOrNull()
    }

    suspend fun download(context: Context, release: Release, progress: (Float) -> Unit): File = withContext(Dispatchers.IO) {
        val dir = File(context.cacheDir, "updates").apply { mkdirs() }
        val target = File(dir, "update.apk")
        val connection = open(BASE + release.apk)
        val total = connection.contentLengthLong.coerceAtLeast(1)
        connection.inputStream.use { input ->
            target.outputStream().use { output ->
                val buffer = ByteArray(64 * 1024); var read: Int; var done = 0L
                while (input.read(buffer).also { read = it } >= 0) { output.write(buffer, 0, read); done += read; progress(done.toFloat() / total) }
            }
        }
        target
    }

    fun canInstall(context: Context) = Build.VERSION.SDK_INT < 26 || context.packageManager.canRequestPackageInstalls()

    fun askInstallPermission(context: Context) {
        context.startActivity(Intent(Settings.ACTION_MANAGE_UNKNOWN_APP_SOURCES, Uri.parse("package:" + context.packageName)).addFlags(Intent.FLAG_ACTIVITY_NEW_TASK))
    }

    fun install(context: Context, apk: File) {
        val uri = FileProvider.getUriForFile(context, context.packageName + ".files", apk)
        context.startActivity(Intent(Intent.ACTION_VIEW).setDataAndType(uri, "application/vnd.android.package-archive")
            .addFlags(Intent.FLAG_GRANT_READ_URI_PERMISSION or Intent.FLAG_ACTIVITY_NEW_TASK))
    }
}
