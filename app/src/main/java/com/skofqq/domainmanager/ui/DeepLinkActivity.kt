package com.skofqq.domainmanager.ui

import android.os.Bundle
import android.widget.Toast
import androidx.activity.ComponentActivity
import androidx.lifecycle.lifecycleScope
import com.skofqq.domainmanager.R
import com.skofqq.domainmanager.data.ApiResult
import com.skofqq.domainmanager.data.HistoryStore
import com.skofqq.domainmanager.data.PrefsStore
import com.skofqq.domainmanager.data.RouterApi
import com.skofqq.domainmanager.util.extractDomain
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext

/**
 * Headless automation entry point: `routerdomains://add?domain=X` (Tasker, App
 * Actions, any automator) fires the existing action=add with the default target
 * and reports the outcome as a toast — no UI opens. The translucent theme keeps
 * the caller's screen visible while the request runs; the activity finishes as
 * soon as the toast is shown.
 *
 * Only the `add` host is registered in the manifest — a scanned setup QR
 * (routerdomains://setup) is deliberately handled ONLY by the in-app scanner,
 * so a random QR can never overwrite saved credentials.
 */
class DeepLinkActivity : ComponentActivity() {

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        // "domain" arrives as a URI query from deep links, or as a plain extra
        // from the shortcuts.xml capability template.
        val raw = intent?.data?.getQueryParameter("domain")
            ?: intent?.getStringExtra("domain")
        val domain = raw?.let { extractDomain(it) }
        if (domain == null) {
            Toast.makeText(this, getString(R.string.err_bad_domain), Toast.LENGTH_LONG).show()
            finish()
            return
        }
        val api = RouterApi(PrefsStore(this), applicationContext)
        val history = HistoryStore.get(this)
        lifecycleScope.launch {
            val target = api.prefs.defaultTarget
            val result = withContext(Dispatchers.IO) { api.callApi(domain, "add", target) }
            val message = when (result) {
                is ApiResult.Success -> {
                    withContext(Dispatchers.IO) {
                        history.logRouting(api.prefs.activeProfileId, domain, target)
                    }
                    getString(R.string.deeplink_added, domain)
                }
                is ApiResult.ApiError ->
                    apiErrorMessage(result.code, result.error).resolve(this@DeepLinkActivity)
                is ApiResult.NetworkError ->
                    networkErrorMessage(result.kind, result.detail).resolve(this@DeepLinkActivity)
            }
            Toast.makeText(this@DeepLinkActivity, message, Toast.LENGTH_LONG).show()
            finish()
        }
    }
}
