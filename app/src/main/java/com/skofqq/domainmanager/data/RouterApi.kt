package com.skofqq.domainmanager.data

import okhttp3.HttpUrl
import okhttp3.OkHttpClient
import okhttp3.Request
import org.json.JSONObject
import java.util.concurrent.TimeUnit

data class DomainStatus(
    val domain: String,
    val magitrickle: Boolean,
    val mihomo: Boolean,
)

sealed class ApiResult {
    data class Success(val status: DomainStatus) : ApiResult()
    data class ApiError(val code: Int, val error: String) : ApiResult()
    data class NetworkError(val message: String) : ApiResult()
}

class RouterApi(val prefs: PrefsStore) {

    private val client = OkHttpClient.Builder()
        .connectTimeout(10, TimeUnit.SECONDS)
        .readTimeout(10, TimeUnit.SECONDS)
        .build()

    /** Runs on IO thread — caller must dispatch off main thread. */
    fun callApi(domain: String, action: String = "status", target: String? = null): ApiResult {
        val url = try {
            buildUrl(prefs.routerHost, prefs.routerPort) {
                addQueryParameter("token", prefs.token)
                addQueryParameter("domain", domain)
                addQueryParameter("action", action)
                addQueryParameter("target", target ?: prefs.defaultTarget)
            }
        } catch (e: Exception) {
            return ApiResult.NetworkError("Invalid router address: ${e.message}")
        }

        return try {
            val response = client.newCall(Request.Builder().url(url).build()).execute()
            val body = response.body?.string() ?: return ApiResult.NetworkError("Empty response")
            val json = JSONObject(body)
            if (response.isSuccessful) {
                ApiResult.Success(
                    DomainStatus(
                        domain = json.getString("domain"),
                        magitrickle = json.getBoolean("magitrickle"),
                        mihomo = json.getBoolean("mihomo"),
                    )
                )
            } else {
                ApiResult.ApiError(response.code, json.optString("error", "unknown"))
            }
        } catch (e: Exception) {
            ApiResult.NetworkError(e.message ?: "Network error")
        }
    }

    /** One-shot connection test using caller-supplied credentials (not yet saved to prefs). */
    fun testConnection(host: String, port: Int, token: String): String {
        val url = try {
            buildUrl(host, port) {
                addQueryParameter("token", token)
                addQueryParameter("domain", "test.com")
                addQueryParameter("action", "status")
                addQueryParameter("target", "both")
            }
        } catch (e: Exception) {
            return "Invalid address: ${e.message}"
        }
        return try {
            val resp = client.newCall(Request.Builder().url(url).build()).execute()
            when {
                resp.isSuccessful || resp.code == 400 -> "Connected"
                resp.code == 403 -> "Connected (bad token)"
                resp.code == 500 -> "Connected (token not configured on router)"
                else -> "Error: HTTP ${resp.code}"
            }
        } catch (e: Exception) {
            "Failed: ${e.message}"
        }
    }

    private fun buildUrl(
        host: String,
        port: Int,
        block: HttpUrl.Builder.() -> Unit,
    ): HttpUrl = HttpUrl.Builder()
        .scheme("http")
        .host(host)
        .port(port)
        .addPathSegments("cgi-bin/domain-api")
        .apply(block)
        .build()
}
