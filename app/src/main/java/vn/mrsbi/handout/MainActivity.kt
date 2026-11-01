package vn.mrsbi.handout

import android.os.Bundle
import android.os.SystemClock
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.biometric.BiometricManager
import androidx.biometric.BiometricManager.Authenticators.BIOMETRIC_WEAK
import androidx.biometric.BiometricManager.Authenticators.DEVICE_CREDENTIAL
import androidx.biometric.BiometricPrompt
import androidx.compose.runtime.mutableStateOf
import androidx.core.content.ContextCompat
import androidx.fragment.app.FragmentActivity
import vn.mrsbi.handout.data.Store
import vn.mrsbi.handout.ui.App
import vn.mrsbi.handout.ui.LockScreen
import vn.mrsbi.handout.ui.MrsBiTheme

/** FragmentActivity because BiometricPrompt needs it. The app locks again after 60 s in the background. */
class MainActivity : FragmentActivity() {
    private lateinit var store: Store
    private val unlocked = mutableStateOf(false)
    private var leftAt = 0L

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        enableEdgeToEdge()
        store = Store(applicationContext)
        unlocked.value = !store.lockEnabled || !canAuthenticate()
        setContent {
            MrsBiTheme {
                if (unlocked.value) App(store) else LockScreen(onUnlock = ::authenticate)
            }
        }
        if (!unlocked.value) authenticate()
    }

    override fun onStop() { super.onStop(); leftAt = SystemClock.elapsedRealtime() }

    override fun onStart() {
        super.onStart()
        if (leftAt > 0 && store.lockEnabled && canAuthenticate() && SystemClock.elapsedRealtime() - leftAt > 60_000) {
            unlocked.value = false; authenticate()
        }
    }

    private fun canAuthenticate() =
        BiometricManager.from(this).canAuthenticate(BIOMETRIC_WEAK or DEVICE_CREDENTIAL) == BiometricManager.BIOMETRIC_SUCCESS

    fun authenticate() {
        val prompt = BiometricPrompt(this, ContextCompat.getMainExecutor(this), object : BiometricPrompt.AuthenticationCallback() {
            override fun onAuthenticationSucceeded(result: BiometricPrompt.AuthenticationResult) { unlocked.value = true }
        })
        prompt.authenticate(BiometricPrompt.PromptInfo.Builder()
            .setTitle("Mrs Bi Handout")
            .setSubtitle("Mở khoá bằng vân tay hoặc khuôn mặt")
            .setAllowedAuthenticators(BIOMETRIC_WEAK or DEVICE_CREDENTIAL)
            .build())
    }
}
