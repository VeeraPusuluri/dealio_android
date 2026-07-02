package com.dealio.app.data

import android.content.Context
import androidx.biometric.BiometricManager
import androidx.biometric.BiometricPrompt
import androidx.core.content.ContextCompat
import androidx.core.content.edit
import androidx.fragment.app.FragmentActivity

private const val ALLOWED_AUTHENTICATORS =
    BiometricManager.Authenticators.BIOMETRIC_STRONG or
        BiometricManager.Authenticators.DEVICE_CREDENTIAL

/**
 * Persists the biometric app-lock preference (whether the app must be unlocked
 * with fingerprint / face / device PIN on launch and after backgrounding).
 */
class AppLockStore(context: Context) {
    private val prefs = context.applicationContext
        .getSharedPreferences("dealio_app_lock", Context.MODE_PRIVATE)

    var enabled: Boolean
        get() = prefs.getBoolean(KEY_ENABLED, false)
        set(value) = prefs.edit { putBoolean(KEY_ENABLED, value) }

    private companion object {
        const val KEY_ENABLED = "enabled"
    }
}

/** True when the device has a biometric or device-credential the app can use. */
fun canUseAppLock(context: Context): Boolean =
    BiometricManager.from(context).canAuthenticate(ALLOWED_AUTHENTICATORS) ==
        BiometricManager.BIOMETRIC_SUCCESS

/**
 * Shows the system biometric / device-credential prompt. [onSuccess] fires on a
 * successful unlock; [onFail] fires on an unrecoverable error (not on a simple
 * retry-able failure) so the caller can decide what to do.
 */
fun promptAppLock(
    activity: FragmentActivity,
    title: String,
    subtitle: String,
    onSuccess: () -> Unit,
    onFail: () -> Unit = {},
) {
    val prompt = BiometricPrompt(
        activity,
        ContextCompat.getMainExecutor(activity),
        object : BiometricPrompt.AuthenticationCallback() {
            override fun onAuthenticationSucceeded(result: BiometricPrompt.AuthenticationResult) {
                onSuccess()
            }

            override fun onAuthenticationError(errorCode: Int, errString: CharSequence) {
                // User cancelled / lockout / no hardware — leave locked, let caller react.
                onFail()
            }
        },
    )
    val info = BiometricPrompt.PromptInfo.Builder()
        .setTitle(title)
        .setSubtitle(subtitle)
        .setAllowedAuthenticators(ALLOWED_AUTHENTICATORS)
        .build()
    prompt.authenticate(info)
}
