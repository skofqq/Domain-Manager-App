package com.skofqq.domainmanager.data

import android.content.Context
import android.net.ConnectivityManager
import android.net.NetworkCapabilities
import android.util.Log
import okhttp3.Call
import okhttp3.Dns
import okhttp3.EventListener
import okhttp3.HttpUrl
import okhttp3.OkHttpClient
import okhttp3.Protocol
import okhttp3.Request
import okhttp3.Response
import org.json.JSONObject
import java.io.IOException
import java.net.ConnectException
import java.net.InetAddress
import java.net.InetSocketAddress
import java.net.NoRouteToHostException
import java.net.Proxy
import java.net.SocketTimeoutException
import java.net.UnknownHostException
import java.util.concurrent.TimeUnit

data class DomainStatus(
    val domain: String,
    val magitrickle: Boolean,
    val mihomo: Boolean,
)

/** Typed network failure so the UI layer can localize the message. */
enum class NetFailure { TIMEOUT, UNREACHABLE, UNKNOWN_HOST, INVALID_ADDRESS, EMPTY_RESPONSE, OTHER }

sealed class ApiResult {
    data class Success(val status: DomainStatus) : ApiResult()
    data class ApiError(val code: Int, val error: String) : ApiResult()
    data class NetworkError(val kind: NetFailure, val detail: String? = null) : ApiResult()
}

sealed class ListResult {
    data class Success(val domains: List<DomainStatus>) : ListResult()
    data class ApiError(val code: Int, val error: String) : ListResult()
    data class NetworkError(val kind: NetFailure, val detail: String? = null) : ListResult()
}

/**
 * One router service. [running] is the live procd state (authoritative);
 * [enabled] is whether it will start on the next boot — two independent flags.
 */
data class ServiceStatus(
    val service: String,
    val running: Boolean,
    val enabled: Boolean,
)

sealed class ServiceListResult {
    data class Success(val services: List<ServiceStatus>) : ServiceListResult()
    data class ApiError(val code: Int, val error: String) : ServiceListResult()
    data class NetworkError(val kind: NetFailure, val detail: String? = null) : ServiceListResult()
}

sealed class ServiceResult {
    data class Success(val status: ServiceStatus) : ServiceResult()
    data class ApiError(val code: Int, val error: String) : ServiceResult()
    data class NetworkError(val kind: NetFailure, val detail: String? = null) : ServiceResult()
}

sealed class TestResult {
    data object Connected : TestResult()
    data object ConnectedBadToken : TestResult()
    data object ConnectedNoToken : TestResult()
    data class HttpError(val code: Int) : TestResult()
    data class Failed(val kind: NetFailure, val detail: String? = null) : TestResult()
}

class RouterApi(val prefs: PrefsStore, private val context: Context) {

    // Proxy.NO_PROXY: the router lives on the LAN — never send these
    // requests through a Wi-Fi proxy configured on the phone.
    private val baseClient = OkHttpClient.Builder()
        .connectTimeout(10, TimeUnit.SECONDS)
        .readTimeout(10, TimeUnit.SECONDS)
        .proxy(Proxy.NO_PROXY)
        .protocols(listOf(Protocol.HTTP_1_1))
        .eventListener(NetLogListener())
        .build()

    /** Socket-level timeline in logcat (tag RouterApiNet) for debugging router issues. */
    private class NetLogListener : EventListener() {
        private fun log(msg: String) {
            Log.d("RouterApiNet", msg)
        }
        override fun callStart(call: Call) { log("callStart ${call.request().url.redact()}") }
        override fun connectStart(call: Call, inetSocketAddress: InetSocketAddress, proxy: Proxy) {
            log("connectStart $inetSocketAddress")
        }
        override fun connectEnd(call: Call, inetSocketAddress: InetSocketAddress, proxy: Proxy, protocol: Protocol?) {
            log("connectEnd $inetSocketAddress proto=$protocol")
        }
        override fun connectFailed(call: Call, inetSocketAddress: InetSocketAddress, proxy: Proxy, protocol: Protocol?, ioe: IOException) {
            log("connectFailed $inetSocketAddress: $ioe")
        }
        override fun requestHeadersEnd(call: Call, request: Request) {
            log("requestHeadersEnd — request fully sent, output NOT closed")
        }
        override fun responseHeadersStart(call: Call) { log("responseHeadersStart — first response byte") }
        override fun responseHeadersEnd(call: Call, response: Response) {
            log("responseHeadersEnd code=${response.code}")
        }
        override fun callEnd(call: Call) { log("callEnd") }
        override fun callFailed(call: Call, ioe: IOException) { log("callFailed: $ioe") }
    }

    /**
     * Binds sockets to the Wi-Fi/Ethernet network directly, so requests reach the
     * router even when a VPN app (mihomo/Clash/etc.) owns the default route and
     * would swallow LAN traffic. Falls back to the default network when no
     * non-VPN Wi-Fi/Ethernet network is up.
     */
    private fun lanClient(): OkHttpClient {
        val cm = context.getSystemService(Context.CONNECTIVITY_SERVICE) as ConnectivityManager

        fun NetworkCapabilities?.isLan(): Boolean = this != null &&
            hasCapability(NetworkCapabilities.NET_CAPABILITY_NOT_VPN) &&
            (hasTransport(NetworkCapabilities.TRANSPORT_WIFI) ||
                hasTransport(NetworkCapabilities.TRANSPORT_ETHERNET))

        // Default network already is plain Wi-Fi/Ethernet: use normal routing.
        // Explicitly binding a socket to a network can break after a Wi-Fi reconnect
        // (stale policy-routing rules), so bind only when actually needed.
        val active = cm.activeNetwork
        if (cm.getNetworkCapabilities(active).isLan()) {
            Log.d("RouterApiNet", "default network $active is LAN — no binding")
            return baseClient
        }

        // Default is VPN/cellular: find the underlying Wi-Fi/Ethernet network and bind to it.
        @Suppress("DEPRECATION")
        val lan = cm.allNetworks.firstOrNull { cm.getNetworkCapabilities(it).isLan() }
        Log.d("RouterApiNet", "default $active is not LAN, binding to: $lan (null=default routing)")
        if (lan == null) return baseClient
        return baseClient.newBuilder()
            .socketFactory(lan.socketFactory)
            .dns(object : Dns {
                override fun lookup(hostname: String): List<InetAddress> =
                    lan.getAllByName(hostname).toList()
            })
            .build()
    }

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
            return ApiResult.NetworkError(NetFailure.INVALID_ADDRESS, e.message)
        }

        return try {
            val response = lanClient().newCall(Request.Builder().url(url).build()).execute()
            val body = response.body?.string()
                ?: return ApiResult.NetworkError(NetFailure.EMPTY_RESPONSE)
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
            ApiResult.NetworkError(classify(e), detailOf(e))
        }
    }

    /**
     * Fetches every domain the API manages (action=list). The result is a union of two
     * independent lists — the MagiTrickle Custom group and the mihomo inline rule-provider —
     * so a domain's two flags can disagree. Runs on IO thread.
     */
    fun listDomains(): ListResult {
        val url = try {
            buildUrl(prefs.routerHost, prefs.routerPort) {
                addQueryParameter("token", prefs.token)
                addQueryParameter("action", "list")
            }
        } catch (e: Exception) {
            return ListResult.NetworkError(NetFailure.INVALID_ADDRESS, e.message)
        }

        return try {
            val response = lanClient().newCall(Request.Builder().url(url).build()).execute()
            val body = response.body?.string()
                ?: return ListResult.NetworkError(NetFailure.EMPTY_RESPONSE)
            val json = JSONObject(body)
            if (response.isSuccessful) {
                val array = json.getJSONArray("domains")
                val domains = buildList {
                    for (i in 0 until array.length()) {
                        val entry = array.getJSONObject(i)
                        add(
                            DomainStatus(
                                domain = entry.getString("domain"),
                                magitrickle = entry.getBoolean("magitrickle"),
                                mihomo = entry.getBoolean("mihomo"),
                            )
                        )
                    }
                }
                ListResult.Success(domains)
            } else {
                ListResult.ApiError(response.code, json.optString("error", "unknown"))
            }
        } catch (e: Exception) {
            ListResult.NetworkError(classify(e), detailOf(e))
        }
    }

    /** Fetches state of all managed router services (action=svc_list). Runs on IO thread. */
    fun listServices(): ServiceListResult {
        val url = try {
            buildUrl(prefs.routerHost, prefs.routerPort) {
                addQueryParameter("token", prefs.token)
                addQueryParameter("action", "svc_list")
            }
        } catch (e: Exception) {
            return ServiceListResult.NetworkError(NetFailure.INVALID_ADDRESS, e.message)
        }

        return try {
            val response = lanClient().newCall(Request.Builder().url(url).build()).execute()
            val body = response.body?.string()
                ?: return ServiceListResult.NetworkError(NetFailure.EMPTY_RESPONSE)
            val json = JSONObject(body)
            if (response.isSuccessful) {
                val array = json.getJSONArray("services")
                val services = buildList {
                    for (i in 0 until array.length()) add(serviceOf(array.getJSONObject(i)))
                }
                ServiceListResult.Success(services)
            } else {
                ServiceListResult.ApiError(response.code, json.optString("error", "unknown"))
            }
        } catch (e: Exception) {
            ServiceListResult.NetworkError(classify(e), detailOf(e))
        }
    }

    /**
     * Runs one svc_* action (svc_status|svc_start|svc_stop|svc_restart) and returns the
     * post-action state. Start/stop/restart may block up to ~5 s on the router while it
     * waits for procd to settle. Runs on IO thread.
     */
    fun serviceAction(service: String, action: String): ServiceResult {
        val url = try {
            buildUrl(prefs.routerHost, prefs.routerPort) {
                addQueryParameter("token", prefs.token)
                addQueryParameter("action", action)
                addQueryParameter("service", service)
            }
        } catch (e: Exception) {
            return ServiceResult.NetworkError(NetFailure.INVALID_ADDRESS, e.message)
        }

        return try {
            val response = lanClient().newCall(Request.Builder().url(url).build()).execute()
            val body = response.body?.string()
                ?: return ServiceResult.NetworkError(NetFailure.EMPTY_RESPONSE)
            val json = JSONObject(body)
            if (response.isSuccessful) {
                ServiceResult.Success(serviceOf(json))
            } else {
                ServiceResult.ApiError(response.code, json.optString("error", "unknown"))
            }
        } catch (e: Exception) {
            ServiceResult.NetworkError(classify(e), detailOf(e))
        }
    }

    private fun serviceOf(json: JSONObject) = ServiceStatus(
        service = json.getString("service"),
        running = json.getBoolean("running"),
        enabled = json.getBoolean("enabled"),
    )

    /** One-shot connection test using caller-supplied credentials (not yet saved to prefs). */
    fun testConnection(host: String, port: Int, token: String): TestResult {
        val url = try {
            buildUrl(host, port) {
                addQueryParameter("token", token)
                addQueryParameter("domain", "test.com")
                addQueryParameter("action", "status")
                addQueryParameter("target", "both")
            }
        } catch (e: Exception) {
            return TestResult.Failed(NetFailure.INVALID_ADDRESS, e.message)
        }
        return try {
            val resp = lanClient().newCall(Request.Builder().url(url).build()).execute()
            when {
                resp.isSuccessful || resp.code == 400 -> TestResult.Connected
                resp.code == 403 -> TestResult.ConnectedBadToken
                resp.code == 500 -> TestResult.ConnectedNoToken
                else -> TestResult.HttpError(resp.code)
            }
        } catch (e: Exception) {
            TestResult.Failed(classify(e), detailOf(e))
        }
    }

    private fun classify(e: Exception): NetFailure = when (e) {
        is SocketTimeoutException -> NetFailure.TIMEOUT
        is ConnectException, is NoRouteToHostException -> NetFailure.UNREACHABLE
        is UnknownHostException -> NetFailure.UNKNOWN_HOST
        else -> NetFailure.OTHER
    }

    private fun detailOf(e: Exception): String =
        e.message?.let { "${e.javaClass.simpleName}: $it" } ?: e.javaClass.simpleName

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
