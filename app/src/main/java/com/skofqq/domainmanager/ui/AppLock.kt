package com.skofqq.domainmanager.ui

import android.content.Context
import androidx.activity.compose.LocalActivity
import androidx.biometric.BiometricManager
import androidx.biometric.BiometricManager.Authenticators.BIOMETRIC_WEAK
import androidx.biometric.BiometricManager.Authenticators.DEVICE_CREDENTIAL
import androidx.biometric.BiometricPrompt
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.outlined.Lock
import androidx.compose.material3.FilledTonalButton
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.core.content.ContextCompat
import androidx.fragment.app.FragmentActivity
import com.skofqq.domainmanager.R

/**
 * Biometric-or-device-credential gate. BIOMETRIC_WEAK | DEVICE_CREDENTIAL means:
 * fingerprint/face when enrolled, otherwise the device PIN/pattern/password — the
 * androidx.biometric compat layer handles the pre-API-30 combinations itself.
 */
private const val AUTHENTICATORS = BIOMETRIC_WEAK or DEVICE_CREDENTIAL

/** False when the device has neither biometrics nor any screen lock — nothing to require. */
fun deviceAuthAvailable(context: Context): Boolean =
    BiometricManager.from(context).canAuthenticate(AUTHENTICATORS) == BiometricManager.BIOMETRIC_SUCCESS

/**
 * Shows the system auth sheet. Failure/cancel simply doesn't call [onSuccess];
 * the caller's UI stays as-is and can retry.
 */
fun promptDeviceAuth(activity: FragmentActivity, title: String, onSuccess: () -> Unit) {
    val prompt = BiometricPrompt(
        activity,
        ContextCompat.getMainExecutor(activity),
        object : BiometricPrompt.AuthenticationCallback() {
            override fun onAuthenticationSucceeded(result: BiometricPrompt.AuthenticationResult) {
                onSuccess()
            }
        },
    )
    prompt.authenticate(
        BiometricPrompt.PromptInfo.Builder()
            .setTitle(title)
            // With DEVICE_CREDENTIAL allowed, a negative button must not be set.
            .setAllowedAuthenticators(AUTHENTICATORS)
            .build()
    )
}

/**
 * Full-screen gate shown instead of the app content while locked. Auto-triggers
 * the system prompt once on entry; the button retries after a cancel.
 */
@Composable
fun AppLockScreen(onUnlocked: () -> Unit) {
    val activity = LocalActivity.current as? FragmentActivity
    val title = stringResource(R.string.unlock_title)

    fun ask() {
        val host = activity ?: return
        // Devices that lost their screen lock since the setting was enabled would
        // otherwise soft-brick the app — let them straight through.
        if (!deviceAuthAvailable(host)) onUnlocked() else promptDeviceAuth(host, title, onUnlocked)
    }

    LaunchedEffect(Unit) { ask() }

    Surface(modifier = Modifier.fillMaxSize()) {
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(horizontal = 32.dp),
            horizontalAlignment = Alignment.CenterHorizontally,
            verticalArrangement = androidx.compose.foundation.layout.Arrangement.Center,
        ) {
            Icon(
                Icons.Outlined.Lock,
                contentDescription = null,
                tint = MaterialTheme.colorScheme.primary,
                modifier = Modifier.size(48.dp),
            )
            Spacer(Modifier.height(16.dp))
            Text(
                text = stringResource(R.string.app_name),
                style = MaterialTheme.typography.titleLarge,
                textAlign = TextAlign.Center,
            )
            Spacer(Modifier.height(24.dp))
            FilledTonalButton(onClick = { ask() }) {
                Text(stringResource(R.string.unlock))
            }
        }
    }
}
