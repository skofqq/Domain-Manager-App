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

/** One domain with its personal DPI-bypass strategy number on a zapret engine. */
data class StrategyEntry(
    val domain: String,
    /** zapret2: 0..max where 0 = shared pool (valid state); zapret v1: 1..17. */
    val strategy: Int,
)

sealed class StrategyListResult {
    data class Success(val engine: String, val max: Int, val domains: List<StrategyEntry>) : StrategyListResult()
    data class ApiError(val code: Int, val error: String) : StrategyListResult()
    data class NetworkError(val kind: NetFailure, val detail: String? = null) : StrategyListResult()
}

sealed class StrategyResult {
    /** [strategy] is null after strat_remove, or when the router couldn't confirm the change. */
    data class Success(val domain: String, val strategy: Int?) : StrategyResult()
    data class ApiError(val code: Int, val error: String) : StrategyResult()
    data class NetworkError(val kind: NetFailure, val detail: String? = null) : StrategyResult()
}

/**
 * One zapret2 profile slot (action=z2profile_list) — always exactly 9 of these,
 * with fixed keys "1".."9" that never change between calls or routers.
 */
data class Z2Profile(
    val key: String,
    /** "tls" | "udp" | "http" — purely informational protocol tag for the UI badge. */
    val proto: String,
    /** Upper bound of THIS profile's own range (1..max) — each profile has its own max, not a shared tlsmax. */
    val max: Int,
    val strategy: Int,
    val name: String,
)

sealed class Z2ProfileListResult {
    data class Success(val profiles: List<Z2Profile>) : Z2ProfileListResult()
    data class ApiError(val code: Int, val error: String) : Z2ProfileListResult()
    data class NetworkError(val kind: NetFailure, val detail: String? = null) : Z2ProfileListResult()
}

/** action=z2profile_rollback: [rolledBack]=false means "nothing to roll back", not an error. zapret v1 has no equivalent — z1profile_apply is irreversible. */
data class Z2RollbackInfo(val rolledBack: Boolean, val message: String)

sealed class Z2RollbackResult {
    data class Success(val info: Z2RollbackInfo) : Z2RollbackResult()
    data class ApiError(val code: Int, val error: String) : Z2RollbackResult()
    data class NetworkError(val kind: NetFailure, val detail: String? = null) : Z2RollbackResult()
}

/**
 * One zapret v1 category slot (action=z1profile_list) — always exactly these 4,
 * with fixed STRING keys "udp_yt"|"tcp_yt"|"gv"|"rkn" (unlike zapret2's numeric
 * keys). No protocol tag (zapret v1 categories carry no such field — don't show
 * a proto badge for this list). [strategy]=0 is a legitimate "default strategy /
 * not personally set" state, not an error and not "still loading".
 */
data class Z1Profile(
    val key: String,
    val max: Int,
    val strategy: Int,
    val name: String,
)

sealed class Z1ProfileListResult {
    data class Success(val profiles: List<Z1Profile>) : Z1ProfileListResult()
    data class ApiError(val code: Int, val error: String) : Z1ProfileListResult()
    data class NetworkError(val kind: NetFailure, val detail: String? = null) : Z1ProfileListResult()
}

/** One reachability probe from action=checkconn — shared by both zapret engines, not engine-specific. [ok] means "got any HTTP response", not specifically 200. */
data class CheckEntry(val name: String, val ok: Boolean, val code: String)

sealed class CheckResult {
    data class Success(val checks: List<CheckEntry>) : CheckResult()
    data class ApiError(val code: Int, val error: String) : CheckResult()
    data class NetworkError(val kind: NetFailure, val detail: String? = null) : CheckResult()
}

/** action=voice_status / voice_set: Discord/WhatsApp/Telegram voice-traffic handling mode, for either zapret engine. */
data class VoiceInfo(val engine: String, val mode: String)

sealed class VoiceResult {
    data class Success(val info: VoiceInfo) : VoiceResult()
    data class ApiError(val code: Int, val error: String) : VoiceResult()
    data class NetworkError(val kind: NetFailure, val detail: String? = null) : VoiceResult()
}

/** Router health snapshot (action=sys_info). [wanIp] is empty when WAN is down — not an error. */
data class SysInfo(
    val uptimeSeconds: Long,
    val load1: Double,
    val load5: Double,
    val load15: Double,
    val memTotalKb: Long,
    val memFreeKb: Long,
    val memAvailableKb: Long,
    val wanIp: String,
    /** Public IPv6 (action=sys_info "wan_ipv6"); empty when the WAN has no IPv6 — hide, not an error. */
    val wanIpv6: String,
    /** e.g. "FriendlyElec NanoPi R3S" */
    val model: String,
    /** e.g. "ImmortalWrt 25.12.0 r37854-4b24da3b4c5c" */
    val firmware: String,
    /** null when the board has no cpu-thermal zone — not an error, just no data on this board. */
    val cpuTempC: Double?,
)

sealed class SysInfoResult {
    data class Success(val info: SysInfo) : SysInfoResult()
    data class ApiError(val code: Int, val error: String) : SysInfoResult()
    data class NetworkError(val kind: NetFailure, val detail: String? = null) : SysInfoResult()
}

/**
 * One LAN client (action=devices). [name] is empty when the device sent no hostname.
 * [type] is FREE TEXT from a user-maintained DHCP tag on the router ("Smart Speaker",
 * "ZigBee Gateway", …) — never treat it as an enum, match by keywords only.
 */
data class LanDevice(val name: String, val ip: String, val mac: String, val type: String)

sealed class DevicesResult {
    data class Success(val devices: List<LanDevice>) : DevicesResult()
    data class ApiError(val code: Int, val error: String) : DevicesResult()
    data class NetworkError(val kind: NetFailure, val detail: String? = null) : DevicesResult()
}

/** One rule inside a MagiTrickle group; [rule] is the domain/namespace text it matches. */
data class MagitrickleRule(
    val id: String,
    val type: String,
    val rule: String,
    val enable: Boolean,
)

/**
 * One MagiTrickle rule group (action=magitrickle_groups returns ALL of them —
 * Custom plus whatever else is configured on the router: Anime, Block, …).
 */
data class MagitrickleGroup(
    val id: String,
    val name: String,
    val rules: List<MagitrickleRule>,
)

sealed class MagitrickleGroupsResult {
    data class Success(val groups: List<MagitrickleGroup>) : MagitrickleGroupsResult()
    data class ApiError(val code: Int, val error: String) : MagitrickleGroupsResult()
    data class NetworkError(val kind: NetFailure, val detail: String? = null) : MagitrickleGroupsResult()
}

/** A switchable mihomo proxy group (type Selector): [now] is the active node out of [all]. */
data class MihomoGroup(val name: String, val now: String, val all: List<String>)

/**
 * Per-node capabilities straight from the raw mihomo /proxies payload: protocol
 * [type] plus UDP / TCP Fast Open support flags. Present for every proxy entry,
 * groups included (their type is Selector/URLTest/…).
 */
data class MihomoNodeInfo(val type: String, val udp: Boolean, val tfo: Boolean, val xudp: Boolean)

sealed class MihomoProxiesResult {
    data class Success(
        val groups: List<MihomoGroup>,
        /** name → info for ALL proxies (nodes and groups alike). */
        val nodes: Map<String, MihomoNodeInfo>,
    ) : MihomoProxiesResult()
    data class ApiError(val code: Int, val error: String) : MihomoProxiesResult()
    data class NetworkError(val kind: NetFailure, val detail: String? = null) : MihomoProxiesResult()
}

data class MihomoConnection(
    val id: String,
    /** metadata.host, falling back to the destination IP when there is no SNI/host. */
    val host: String,
    /** LAN IP the connection came from (metadata.sourceIP); empty when mihomo omits it. */
    val sourceIP: String,
    /** Proxy chain, group first (mihomo sends it exit-node first — reversed at parse time). */
    val chains: List<String>,
    val download: Long,
    val upload: Long,
)

/**
 * One poll of action=mihomo_connections. The API has no live traffic stream —
 * clients derive speed from the delta of the totals between two polls.
 */
data class MihomoConnectionsSnapshot(
    val downloadTotal: Long,
    val uploadTotal: Long,
    val connections: List<MihomoConnection>,
)

sealed class MihomoConnectionsResult {
    data class Success(val snapshot: MihomoConnectionsSnapshot) : MihomoConnectionsResult()
    data class ApiError(val code: Int, val error: String) : MihomoConnectionsResult()
    data class NetworkError(val kind: NetFailure, val detail: String? = null) : MihomoConnectionsResult()
}

/**
 * One action=ping run (3 packets, router-side). [avgMs] is null at 100% loss.
 * [host] may be an IP or a domain.
 */
data class PingInfo(
    val host: String,
    val reachable: Boolean,
    val lossPercent: Int,
    val avgMs: Double?,
)

sealed class PingResult {
    data class Success(val info: PingInfo) : PingResult()
    data class ApiError(val code: Int, val error: String) : PingResult()
    data class NetworkError(val kind: NetFailure, val detail: String? = null) : PingResult()
}

/** One traceroute hop; [ip] and [ms] are both null for a non-answering hop (the "*"). */
data class TraceHop(val hop: Int, val ip: String?, val ms: Double?)

sealed class TracerouteResult {
    data class Success(val host: String, val hops: List<TraceHop>) : TracerouteResult()
    data class ApiError(val code: Int, val error: String) : TracerouteResult()
    data class NetworkError(val kind: NetFailure, val detail: String? = null) : TracerouteResult()
}

/** action=speedtest: the ROUTER's WAN download speed, not the phone's Wi-Fi. */
data class SpeedtestInfo(val downloadMbps: Double, val bytesPerSec: Long)

sealed class SpeedtestResult {
    data class Success(val info: SpeedtestInfo) : SpeedtestResult()
    data class ApiError(val code: Int, val error: String) : SpeedtestResult()
    data class NetworkError(val kind: NetFailure, val detail: String? = null) : SpeedtestResult()
}

sealed class GroupDelayResult {
    /**
     * node name → latency ms. A node that did not answer within mihomo's 5 s test
     * window is simply ABSENT from the map (not 0/null) — that absence is the
     * "not responding" signal.
     */
    data class Success(val delays: Map<String, Int>) : GroupDelayResult()
    data class ApiError(val code: Int, val error: String) : GroupDelayResult()
    data class NetworkError(val kind: NetFailure, val detail: String? = null) : GroupDelayResult()
}

/** action=disk_info: /overlay usage. All fields are always present (never null/empty on a healthy router). */
data class DiskInfo(
    val mount: String,
    val totalKb: Long,
    val usedKb: Long,
    val availableKb: Long,
    val usedPercent: Int,
)

sealed class DiskInfoResult {
    data class Success(val info: DiskInfo) : DiskInfoResult()
    data class ApiError(val code: Int, val error: String) : DiskInfoResult()
    data class NetworkError(val kind: NetFailure, val detail: String? = null) : DiskInfoResult()
}

/**
 * action=service_log: up to 100 logread lines grep'd by service name. An empty
 * [lines] list is a legitimate answer (nothing logged under that substring), not
 * an error — magitrickle/zapret/zapret2 commonly have none.
 */
sealed class ServiceLogResult {
    data class Success(val lines: List<String>) : ServiceLogResult()
    data class ApiError(val code: Int, val error: String) : ServiceLogResult()
    data class NetworkError(val kind: NetFailure, val detail: String? = null) : ServiceLogResult()
}

/** action=versions: one field per managed service. Empty string = version could not be determined. */
data class ServiceVersions(
    val mihomo: String,
    val magitrickle: String,
    val zapret: String,
    val zapret2: String,
)

sealed class VersionsResult {
    data class Success(val versions: ServiceVersions) : VersionsResult()
    data class ApiError(val code: Int, val error: String) : VersionsResult()
    data class NetworkError(val kind: NetFailure, val detail: String? = null) : VersionsResult()
}

/** action=mihomo_check_update. */
data class MihomoUpdateInfo(val current: String, val latest: String, val updateAvailable: Boolean)

sealed class MihomoCheckUpdateResult {
    data class Success(val info: MihomoUpdateInfo) : MihomoCheckUpdateResult()
    data class ApiError(val code: Int, val error: String) : MihomoCheckUpdateResult()
    data class NetworkError(val kind: NetFailure, val detail: String? = null) : MihomoCheckUpdateResult()
}

/**
 * action=mihomo_node_ipv6: a LIVE per-node AAAA check (real DNS lookups on the
 * router) — deliberately never cached client-side, results can differ between
 * calls on subscriptions with dynamic DNS.
 */
sealed class NodeIpv6Result {
    /** node name → has-IPv6; a node absent from the router's answer is simply unknown, not false. */
    data class Success(val ipv6ByNode: Map<String, Boolean>) : NodeIpv6Result()
    data class ApiError(val code: Int, val error: String) : NodeIpv6Result()
    data class NetworkError(val kind: NetFailure, val detail: String? = null) : NodeIpv6Result()
}

/**
 * One mihomo rule-provider group added through this app (action=provider_list).
 * [provider] is ALREADY stripped of the router's internal "helm-" prefix — show
 * and send it exactly as it comes back. [group] is the proxy-group matched
 * traffic is routed to, NOT the group the rule-set itself is downloaded through.
 */
data class RuleProvider(
    val provider: String,
    val url: String,
    /** Refresh period in seconds (60..604800). */
    val interval: Int,
    /** "domain" | "ipcidr" | "classical" */
    val behavior: String,
    /** "mrs" | "yaml" | "text" */
    val format: String,
    val group: String,
)

sealed class ProxyGroupsResult {
    /** Every proxy-group configured on the router, in router order. */
    data class Success(val groups: List<String>) : ProxyGroupsResult()
    data class ApiError(val code: Int, val error: String) : ProxyGroupsResult()
    data class NetworkError(val kind: NetFailure, val detail: String? = null) : ProxyGroupsResult()
}

sealed class RuleProviderListResult {
    data class Success(val providers: List<RuleProvider>) : RuleProviderListResult()
    data class ApiError(val code: Int, val error: String) : RuleProviderListResult()
    data class NetworkError(val kind: NetFailure, val detail: String? = null) : RuleProviderListResult()
}

sealed class RuleProviderAddResult {
    data class Success(val provider: RuleProvider) : RuleProviderAddResult()
    data class ApiError(val code: Int, val error: String) : RuleProviderAddResult()
    data class NetworkError(val kind: NetFailure, val detail: String? = null) : RuleProviderAddResult()
}

sealed class RuleProviderRemoveResult {
    /** [removed]=false means the router had no such provider — a no-op, not a failure. */
    data class Success(val provider: String, val removed: Boolean) : RuleProviderRemoveResult()
    data class ApiError(val code: Int, val error: String) : RuleProviderRemoveResult()
    data class NetworkError(val kind: NetFailure, val detail: String? = null) : RuleProviderRemoveResult()
}

/** For actions whose success payload carries no data (reboot, mihomo_select, connection close). */
sealed class OkResult {
    data object Success : OkResult()
    data class ApiError(val code: Int, val error: String) : OkResult()
    data class NetworkError(val kind: NetFailure, val detail: String? = null) : OkResult()
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
    // Rebuilt lazily when the user changes the timeout in Diagnostics.
    @Volatile private var cachedClient: OkHttpClient? = null
    @Volatile private var cachedTimeout: Int = -1

    private fun baseClient(): OkHttpClient {
        val timeout = prefs.httpTimeoutSeconds.coerceIn(MIN_TIMEOUT_SECONDS, MAX_TIMEOUT_SECONDS)
        cachedClient?.let { if (cachedTimeout == timeout) return it }
        val built = OkHttpClient.Builder()
            .connectTimeout(timeout.toLong(), TimeUnit.SECONDS)
            .readTimeout(timeout.toLong(), TimeUnit.SECONDS)
            .proxy(Proxy.NO_PROXY)
            .protocols(listOf(Protocol.HTTP_1_1))
            .eventListener(NetLogListener())
            .build()
        cachedTimeout = timeout
        cachedClient = built
        return built
    }

    private data class HttpReply(val code: Int, val ok: Boolean, val body: String?)

    /**
     * Executes one GET and records it (token-masked) in [ApiLog] for the
     * Diagnostics screen — both successes and transport failures.
     * [shortConnect] caps the connect timeout at [FAILOVER_CONNECT_SECONDS] so a
     * dead primary address fails fast before the fallback attempt.
     * [minReadSeconds] RAISES the read timeout when the user's configured timeout is
     * shorter — for router-side actions that legitimately take long (speedtest,
     * traceroute, ping, group delay test).
     */
    private fun fetch(
        url: HttpUrl,
        shortConnect: Boolean = false,
        minReadSeconds: Int? = null,
    ): HttpReply {
        val started = System.currentTimeMillis()
        try {
            val client = lanClient().let { base ->
                var built = base
                if (shortConnect && built.connectTimeoutMillis > FAILOVER_CONNECT_SECONDS * 1000) {
                    built = built.newBuilder()
                        .connectTimeout(FAILOVER_CONNECT_SECONDS.toLong(), TimeUnit.SECONDS)
                        .build()
                }
                if (minReadSeconds != null && built.readTimeoutMillis < minReadSeconds * 1000) {
                    built = built.newBuilder()
                        .readTimeout(minReadSeconds.toLong(), TimeUnit.SECONDS)
                        .build()
                }
                built
            }
            val response = client.newCall(Request.Builder().url(url).build()).execute()
            val body = response.body?.string()
            ApiLog.record(url, response.code, body, System.currentTimeMillis() - started, null)
            return HttpReply(response.code, response.isSuccessful, body)
        } catch (e: Exception) {
            ApiLog.record(url, null, null, System.currentTimeMillis() - started, detailOf(e))
            throw e
        }
    }

    /**
     * Builds the query against the saved primary address and executes it; when a
     * fallback address is configured (e.g. Tailscale) and the primary attempt fails
     * at the transport level, the same query is retried against the fallback —
     * automatic failover, no manual switch. HTTP error responses are NOT failover
     * triggers: the router answered.
     */
    private fun fetchApi(minReadSeconds: Int? = null, query: HttpUrl.Builder.() -> Unit): HttpReply {
        val fallbackHost = prefs.fallbackHost.trim()
        val primary = buildUrl(prefs.routerHost, prefs.routerPort, query)
        if (fallbackHost.isEmpty()) return fetch(primary, minReadSeconds = minReadSeconds)
        return try {
            fetch(primary, shortConnect = true, minReadSeconds = minReadSeconds)
        } catch (primaryError: Exception) {
            val fallback = try {
                buildUrl(fallbackHost, prefs.fallbackPort, query)
            } catch (_: Exception) {
                throw primaryError
            }
            fetch(fallback, minReadSeconds = minReadSeconds)
        }
    }

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
            return baseClient()
        }

        // Default is VPN/cellular: find the underlying Wi-Fi/Ethernet network and bind to it.
        @Suppress("DEPRECATION")
        val lan = cm.allNetworks.firstOrNull { cm.getNetworkCapabilities(it).isLan() }
        Log.d("RouterApiNet", "default $active is not LAN, binding to: $lan (null=default routing)")
        if (lan == null) return baseClient()
        return baseClient().newBuilder()
            .socketFactory(lan.socketFactory)
            .dns(object : Dns {
                override fun lookup(hostname: String): List<InetAddress> =
                    lan.getAllByName(hostname).toList()
            })
            .build()
    }

    /** Runs on IO thread — caller must dispatch off main thread. */
    fun callApi(domain: String, action: String = "status", target: String? = null): ApiResult {
        return try {
            val reply = fetchApi {
                addQueryParameter("token", prefs.token)
                addQueryParameter("domain", domain)
                addQueryParameter("action", action)
                addQueryParameter("target", target ?: prefs.defaultTarget)
            }
            val body = reply.body
                ?: return ApiResult.NetworkError(NetFailure.EMPTY_RESPONSE)
            val json = JSONObject(body)
            if (reply.ok) {
                ApiResult.Success(
                    DomainStatus(
                        domain = json.getString("domain"),
                        magitrickle = json.getBoolean("magitrickle"),
                        mihomo = json.getBoolean("mihomo"),
                    )
                )
            } else {
                ApiResult.ApiError(reply.code, json.optString("error", "unknown"))
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
        return try {
            val reply = fetchApi {
                addQueryParameter("token", prefs.token)
                addQueryParameter("action", "list")
            }
            val body = reply.body
                ?: return ListResult.NetworkError(NetFailure.EMPTY_RESPONSE)
            val json = JSONObject(body)
            if (reply.ok) {
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
                ListResult.ApiError(reply.code, json.optString("error", "unknown"))
            }
        } catch (e: Exception) {
            ListResult.NetworkError(classify(e), detailOf(e))
        }
    }

    /** Fetches state of all managed router services (action=svc_list). Runs on IO thread. */
    fun listServices(): ServiceListResult {
        return try {
            val reply = fetchApi {
                addQueryParameter("token", prefs.token)
                addQueryParameter("action", "svc_list")
            }
            val body = reply.body
                ?: return ServiceListResult.NetworkError(NetFailure.EMPTY_RESPONSE)
            val json = JSONObject(body)
            if (reply.ok) {
                val array = json.getJSONArray("services")
                val services = buildList {
                    for (i in 0 until array.length()) add(serviceOf(array.getJSONObject(i)))
                }
                ServiceListResult.Success(services)
            } else {
                ServiceListResult.ApiError(reply.code, json.optString("error", "unknown"))
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
        return try {
            val reply = fetchApi {
                addQueryParameter("token", prefs.token)
                addQueryParameter("action", action)
                addQueryParameter("service", service)
            }
            val body = reply.body
                ?: return ServiceResult.NetworkError(NetFailure.EMPTY_RESPONSE)
            val json = JSONObject(body)
            if (reply.ok) {
                ServiceResult.Success(serviceOf(json))
            } else {
                ServiceResult.ApiError(reply.code, json.optString("error", "unknown"))
            }
        } catch (e: Exception) {
            ServiceResult.NetworkError(classify(e), detailOf(e))
        }
    }

    /** Fetches per-domain strategies for one engine (action=strat_list). Read-only, never restarts anything. */
    fun listStrategies(engine: String): StrategyListResult {
        return try {
            val reply = fetchApi {
                addQueryParameter("token", prefs.token)
                addQueryParameter("action", "strat_list")
                addQueryParameter("engine", engine)
            }
            val body = reply.body
                ?: return StrategyListResult.NetworkError(NetFailure.EMPTY_RESPONSE)
            val json = JSONObject(body)
            if (reply.ok) {
                val array = json.getJSONArray("domains")
                val domains = buildList {
                    for (i in 0 until array.length()) {
                        val entry = array.getJSONObject(i)
                        add(StrategyEntry(entry.getString("domain"), entry.getInt("strategy")))
                    }
                }
                StrategyListResult.Success(
                    engine = json.getString("engine"),
                    max = json.getInt("max"),
                    domains = domains,
                )
            } else {
                StrategyListResult.ApiError(reply.code, json.optString("error", "unknown"))
            }
        } catch (e: Exception) {
            StrategyListResult.NetworkError(classify(e), detailOf(e))
        }
    }

    /**
     * One strat_add / strat_set / strat_remove call ([strategy] must be null only for
     * strat_remove). WARNING: if [engine] is currently running, the router restarts its
     * daemon as part of the change — callers must invoke this once per explicit user
     * confirmation, never per keystroke/slider tick.
     */
    fun strategyAction(engine: String, domain: String, action: String, strategy: Int? = null): StrategyResult {
        return try {
            val reply = fetchApi {
                addQueryParameter("token", prefs.token)
                addQueryParameter("action", action)
                addQueryParameter("engine", engine)
                addQueryParameter("domain", domain)
                if (strategy != null) addQueryParameter("strategy", strategy.toString())
            }
            val body = reply.body
                ?: return StrategyResult.NetworkError(NetFailure.EMPTY_RESPONSE)
            val json = JSONObject(body)
            if (reply.ok) {
                StrategyResult.Success(
                    domain = json.getString("domain"),
                    strategy = if (json.isNull("strategy")) null else json.getInt("strategy"),
                )
            } else {
                StrategyResult.ApiError(reply.code, json.optString("error", "unknown"))
            }
        } catch (e: Exception) {
            StrategyResult.NetworkError(classify(e), detailOf(e))
        }
    }

    /** zapret2's fixed 9-slot profile list (action=z2profile_list). Read-only. Runs on IO thread. */
    fun z2ProfileList(): Z2ProfileListResult {
        return try {
            val reply = fetchApi {
                addQueryParameter("token", prefs.token)
                addQueryParameter("action", "z2profile_list")
            }
            val body = reply.body
                ?: return Z2ProfileListResult.NetworkError(NetFailure.EMPTY_RESPONSE)
            val json = JSONObject(body)
            if (reply.ok) {
                Z2ProfileListResult.Success(parseZ2Profiles(json))
            } else {
                Z2ProfileListResult.ApiError(reply.code, json.optString("error", "unknown"))
            }
        } catch (e: Exception) {
            Z2ProfileListResult.NetworkError(classify(e), detailOf(e))
        }
    }

    /**
     * Applies changed zapret2 profile strategies (action=z2profile_apply&values=key:strategy,…
     * — only the changed pairs need to be sent). WARNING: if zapret2 is currently running, this
     * restarts its daemon — call only from an explicit "Apply" confirmation, never per slider
     * tick. The router answers with a FRESH z2profile_list; callers should update the UI
     * straight from this response, no follow-up call needed. Runs on IO thread.
     */
    fun z2ProfileApply(values: Map<String, Int>): Z2ProfileListResult {
        return try {
            val reply = fetchApi {
                addQueryParameter("token", prefs.token)
                addQueryParameter("action", "z2profile_apply")
                addQueryParameter("values", values.entries.joinToString(",") { "${it.key}:${it.value}" })
            }
            val body = reply.body
                ?: return Z2ProfileListResult.NetworkError(NetFailure.EMPTY_RESPONSE)
            val json = JSONObject(body)
            if (reply.ok) {
                Z2ProfileListResult.Success(parseZ2Profiles(json))
            } else {
                Z2ProfileListResult.ApiError(reply.code, json.optString("error", "unknown"))
            }
        } catch (e: Exception) {
            Z2ProfileListResult.NetworkError(classify(e), detailOf(e))
        }
    }

    private fun parseZ2Profiles(json: JSONObject): List<Z2Profile> {
        val array = json.getJSONArray("profiles")
        return buildList {
            for (i in 0 until array.length()) {
                val entry = array.getJSONObject(i)
                add(
                    Z2Profile(
                        key = entry.getString("key"),
                        proto = entry.getString("proto"),
                        max = entry.getInt("max"),
                        strategy = entry.getInt("strategy"),
                        name = entry.getString("name"),
                    )
                )
            }
        }
    }

    /**
     * Reverts to the previous applied zapret2 profile set (action=z2profile_rollback).
     * [Z2RollbackInfo.rolledBack]=false is a normal answer ("nothing to undo"), not an error —
     * show it as a neutral message, never as a failure. Runs on IO thread.
     */
    fun z2ProfileRollback(): Z2RollbackResult {
        return try {
            val reply = fetchApi {
                addQueryParameter("token", prefs.token)
                addQueryParameter("action", "z2profile_rollback")
            }
            val body = reply.body
                ?: return Z2RollbackResult.NetworkError(NetFailure.EMPTY_RESPONSE)
            val json = JSONObject(body)
            if (reply.ok) {
                Z2RollbackResult.Success(
                    Z2RollbackInfo(
                        rolledBack = json.getBoolean("rolled_back"),
                        message = json.optString("message", ""),
                    )
                )
            } else {
                Z2RollbackResult.ApiError(reply.code, json.optString("error", "unknown"))
            }
        } catch (e: Exception) {
            Z2RollbackResult.NetworkError(classify(e), detailOf(e))
        }
    }

    /**
     * zapret v1's fixed 4-category list (action=z1profile_list). No proto tag, unlike
     * z2profile_list — strings keys "udp_yt"|"tcp_yt"|"gv"|"rkn". Read-only. Runs on IO thread.
     */
    fun z1ProfileList(): Z1ProfileListResult {
        return try {
            val reply = fetchApi {
                addQueryParameter("token", prefs.token)
                addQueryParameter("action", "z1profile_list")
            }
            val body = reply.body
                ?: return Z1ProfileListResult.NetworkError(NetFailure.EMPTY_RESPONSE)
            val json = JSONObject(body)
            if (reply.ok) {
                Z1ProfileListResult.Success(parseZ1Profiles(json))
            } else {
                Z1ProfileListResult.ApiError(reply.code, json.optString("error", "unknown"))
            }
        } catch (e: Exception) {
            Z1ProfileListResult.NetworkError(classify(e), detailOf(e))
        }
    }

    /**
     * Applies changed zapret v1 category strategies (action=z1profile_apply&values=key:strategy,…
     * — only the changed pairs need to be sent). Unlike zapret2's z2profile_apply, this does
     * NOT restart the whole daemon: each new strategy takes effect immediately for new
     * connections when zapret is the running engine. There is no rollback for this endpoint —
     * z4r/IndeecFOX gives no undo. The router answers with a FRESH z1profile_list; callers
     * should update the UI straight from this response. Runs on IO thread.
     */
    fun z1ProfileApply(values: Map<String, Int>): Z1ProfileListResult {
        return try {
            val reply = fetchApi {
                addQueryParameter("token", prefs.token)
                addQueryParameter("action", "z1profile_apply")
                addQueryParameter("values", values.entries.joinToString(",") { "${it.key}:${it.value}" })
            }
            val body = reply.body
                ?: return Z1ProfileListResult.NetworkError(NetFailure.EMPTY_RESPONSE)
            val json = JSONObject(body)
            if (reply.ok) {
                Z1ProfileListResult.Success(parseZ1Profiles(json))
            } else {
                Z1ProfileListResult.ApiError(reply.code, json.optString("error", "unknown"))
            }
        } catch (e: Exception) {
            Z1ProfileListResult.NetworkError(classify(e), detailOf(e))
        }
    }

    private fun parseZ1Profiles(json: JSONObject): List<Z1Profile> {
        val array = json.getJSONArray("profiles")
        return buildList {
            for (i in 0 until array.length()) {
                val entry = array.getJSONObject(i)
                add(
                    Z1Profile(
                        key = entry.getString("key"),
                        max = entry.getInt("max"),
                        strategy = entry.getInt("strategy"),
                        name = entry.getString("name"),
                    )
                )
            }
        }
    }

    /**
     * Reachability probe (action=checkconn — shared by both zapret engines, not
     * engine-specific) — a synchronous call that takes 2-4 s on the router; callers must
     * show a spinner. [CheckEntry.ok] means "got any HTTP response" (403/301 included), not
     * specifically 200. Runs on IO thread.
     */
    fun checkConnectivity(): CheckResult {
        return try {
            val reply = fetchApi(minReadSeconds = CHECK_CONN_READ_SECONDS) {
                addQueryParameter("token", prefs.token)
                addQueryParameter("action", "checkconn")
            }
            val body = reply.body
                ?: return CheckResult.NetworkError(NetFailure.EMPTY_RESPONSE)
            val json = JSONObject(body)
            if (reply.ok) {
                val array = json.getJSONArray("checks")
                val checks = buildList {
                    for (i in 0 until array.length()) {
                        val entry = array.getJSONObject(i)
                        add(
                            CheckEntry(
                                name = entry.getString("name"),
                                ok = entry.getBoolean("ok"),
                                code = entry.optString("code", ""),
                            )
                        )
                    }
                }
                CheckResult.Success(checks)
            } else {
                CheckResult.ApiError(reply.code, json.optString("error", "unknown"))
            }
        } catch (e: Exception) {
            CheckResult.NetworkError(classify(e), detailOf(e))
        }
    }

    /** Reads the voice-traffic handling mode for [engine] (action=voice_status). Runs on IO thread. */
    fun voiceStatus(engine: String): VoiceResult = voiceResultOf {
        addQueryParameter("action", "voice_status")
        addQueryParameter("engine", engine)
    }

    /**
     * Switches the voice-traffic handling mode (action=voice_set). Callers must take the
     * new mode FROM the response, not the value they sent — the router is authoritative.
     */
    fun voiceSet(engine: String, mode: String): VoiceResult = voiceResultOf {
        addQueryParameter("action", "voice_set")
        addQueryParameter("engine", engine)
        addQueryParameter("mode", mode)
    }

    private fun voiceResultOf(query: HttpUrl.Builder.() -> Unit): VoiceResult {
        return try {
            val reply = fetchApi {
                addQueryParameter("token", prefs.token)
                query()
            }
            val body = reply.body
                ?: return VoiceResult.NetworkError(NetFailure.EMPTY_RESPONSE)
            val json = JSONObject(body)
            if (reply.ok) {
                VoiceResult.Success(VoiceInfo(engine = json.getString("engine"), mode = json.getString("mode")))
            } else {
                VoiceResult.ApiError(reply.code, json.optString("error", "unknown"))
            }
        } catch (e: Exception) {
            VoiceResult.NetworkError(classify(e), detailOf(e))
        }
    }

    /** Router health snapshot (action=sys_info). Runs on IO thread. */
    fun sysInfo(): SysInfoResult {
        return try {
            val reply = fetchApi {
                addQueryParameter("token", prefs.token)
                addQueryParameter("action", "sys_info")
            }
            val body = reply.body
                ?: return SysInfoResult.NetworkError(NetFailure.EMPTY_RESPONSE)
            val json = JSONObject(body)
            if (reply.ok) {
                SysInfoResult.Success(
                    SysInfo(
                        uptimeSeconds = json.getLong("uptime_seconds"),
                        load1 = json.getDouble("load1"),
                        load5 = json.getDouble("load5"),
                        load15 = json.getDouble("load15"),
                        memTotalKb = json.getLong("mem_total_kb"),
                        memFreeKb = json.getLong("mem_free_kb"),
                        memAvailableKb = json.getLong("mem_available_kb"),
                        wanIp = json.optString("wan_ip", ""),
                        wanIpv6 = json.optString("wan_ipv6", ""),
                        model = json.optString("model", ""),
                        firmware = json.optString("firmware", ""),
                        // optDouble returns NaN for both a missing key and a JSON null —
                        // either way there's no reading for this board.
                        cpuTempC = json.optDouble("cpu_temp_c", Double.NaN).takeUnless { it.isNaN() },
                    )
                )
            } else {
                SysInfoResult.ApiError(reply.code, json.optString("error", "unknown"))
            }
        } catch (e: Exception) {
            SysInfoResult.NetworkError(classify(e), detailOf(e))
        }
    }

    /** Connected LAN devices (action=devices). Runs on IO thread. */
    fun listDevices(): DevicesResult {
        return try {
            val reply = fetchApi {
                addQueryParameter("token", prefs.token)
                addQueryParameter("action", "devices")
            }
            val body = reply.body
                ?: return DevicesResult.NetworkError(NetFailure.EMPTY_RESPONSE)
            val json = JSONObject(body)
            if (reply.ok) {
                val array = json.getJSONArray("devices")
                val devices = buildList {
                    for (i in 0 until array.length()) {
                        val entry = array.getJSONObject(i)
                        add(
                            LanDevice(
                                name = entry.optString("name", ""),
                                ip = entry.getString("ip"),
                                mac = entry.getString("mac"),
                                type = entry.optString("type", ""),
                            )
                        )
                    }
                }
                DevicesResult.Success(devices)
            } else {
                DevicesResult.ApiError(reply.code, json.optString("error", "unknown"))
            }
        } catch (e: Exception) {
            DevicesResult.NetworkError(classify(e), detailOf(e))
        }
    }

    /**
     * Reboots the router (action=reboot). The API answers immediately and the real
     * reboot follows ~2 s later; the router is then unreachable for 1–2 minutes.
     * Callers MUST have shown an explicit confirmation first. Runs on IO thread.
     */
    fun reboot(): OkResult = okAction { addQueryParameter("action", "reboot") }

    /**
     * mihomo Selector groups (action=mihomo_proxies — a transparent pass-through of
     * the Clash-compatible controller API). Non-Selector groups have no switch and
     * are skipped, as is the synthetic GLOBAL group. Runs on IO thread.
     */
    fun mihomoProxies(): MihomoProxiesResult {
        return try {
            val reply = fetchApi {
                addQueryParameter("token", prefs.token)
                addQueryParameter("action", "mihomo_proxies")
            }
            val body = reply.body
                ?: return MihomoProxiesResult.NetworkError(NetFailure.EMPTY_RESPONSE)
            val json = JSONObject(body)
            if (reply.ok) {
                val proxies = json.getJSONObject("proxies")
                val nodes = mutableMapOf<String, MihomoNodeInfo>()
                val groups = buildList {
                    for (name in proxies.keys()) {
                        if (name == "GLOBAL") continue
                        val entry = proxies.getJSONObject(name)
                        // udp/tfo/type ride along in mihomo's raw payload for every
                        // proxy — recorded for all entries so the node sheet can
                        // badge protocol/UDP/TFO without a second call.
                        nodes[name] = MihomoNodeInfo(
                            type = entry.optString("type", ""),
                            udp = entry.optBoolean("udp", false),
                            tfo = entry.optBoolean("tfo", false),
                            xudp = entry.optBoolean("xudp", false),
                        )
                        if (entry.optString("type") != "Selector") continue
                        val allArray = entry.optJSONArray("all") ?: continue
                        val all = buildList {
                            for (i in 0 until allArray.length()) add(allArray.getString(i))
                        }
                        add(MihomoGroup(name, entry.optString("now", ""), all))
                    }
                }
                MihomoProxiesResult.Success(groups, nodes)
            } else {
                MihomoProxiesResult.ApiError(reply.code, json.optString("error", "unknown"))
            }
        } catch (e: Exception) {
            MihomoProxiesResult.NetworkError(classify(e), detailOf(e))
        }
    }

    /**
     * Every proxy-group name configured on the router (action=pg_list) — unlike
     * [mihomoProxies] this is the router's own config view, so it also lists
     * groups that are not Selectors. Used to pick the destination group (and the
     * optional download group) when adding a rule-provider. Runs on IO thread.
     */
    fun proxyGroups(): ProxyGroupsResult {
        return try {
            val reply = fetchApi {
                addQueryParameter("token", prefs.token)
                addQueryParameter("action", "pg_list")
            }
            val body = reply.body
                ?: return ProxyGroupsResult.NetworkError(NetFailure.EMPTY_RESPONSE)
            val json = JSONObject(body)
            if (reply.ok) {
                val array = json.getJSONArray("groups")
                val groups = buildList {
                    for (i in 0 until array.length()) add(array.getString(i))
                }
                ProxyGroupsResult.Success(groups)
            } else {
                ProxyGroupsResult.ApiError(reply.code, json.optString("error", "unknown"))
            }
        } catch (e: Exception) {
            ProxyGroupsResult.NetworkError(classify(e), detailOf(e))
        }
    }

    /**
     * Rule-provider groups added through this app (action=provider_list). Only
     * ours — rule-providers configured on the router by other means are not
     * listed and cannot be removed from here. Runs on IO thread.
     */
    fun ruleProviders(): RuleProviderListResult {
        return try {
            val reply = fetchApi {
                addQueryParameter("token", prefs.token)
                addQueryParameter("action", "provider_list")
            }
            val body = reply.body
                ?: return RuleProviderListResult.NetworkError(NetFailure.EMPTY_RESPONSE)
            val json = JSONObject(body)
            if (reply.ok) {
                val array = json.getJSONArray("providers")
                val providers = buildList {
                    for (i in 0 until array.length()) add(parseRuleProvider(array.getJSONObject(i)))
                }
                RuleProviderListResult.Success(providers)
            } else {
                RuleProviderListResult.ApiError(reply.code, json.optString("error", "unknown"))
            }
        } catch (e: Exception) {
            RuleProviderListResult.NetworkError(classify(e), detailOf(e))
        }
    }

    /**
     * Adds a rule-provider and its routing rule (action=provider_add). Exactly one
     * of [group] (existing) / [newGroup] (created on the fly) must be non-null —
     * the router answers 400 group_and_new_group_both_set / missing_group otherwise.
     * [fetchProxy] is a completely separate thing: the proxy-group the ROUTER
     * downloads the rule-set through, for sources it can't reach directly.
     *
     * Slow: the router fetches the URL and runs a full mihomo config test before
     * answering, hence the raised read timeout. Runs on IO thread.
     */
    fun ruleProviderAdd(
        provider: String,
        url: String,
        behavior: String,
        format: String,
        interval: Int,
        group: String? = null,
        newGroup: String? = null,
        fetchProxy: String? = null,
    ): RuleProviderAddResult {
        return try {
            val reply = fetchApi(minReadSeconds = PROVIDER_ADD_READ_SECONDS) {
                addQueryParameter("token", prefs.token)
                addQueryParameter("action", "provider_add")
                addQueryParameter("provider", provider)
                addQueryParameter("url", url)
                addQueryParameter("behavior", behavior)
                addQueryParameter("format", format)
                addQueryParameter("interval", interval.toString())
                group?.let { addQueryParameter("group", it) }
                newGroup?.let { addQueryParameter("new_group", it) }
                fetchProxy?.let { addQueryParameter("fetch_proxy", it) }
            }
            val body = reply.body
                ?: return RuleProviderAddResult.NetworkError(NetFailure.EMPTY_RESPONSE)
            val json = JSONObject(body)
            if (reply.ok) {
                RuleProviderAddResult.Success(parseRuleProvider(json))
            } else {
                RuleProviderAddResult.ApiError(reply.code, json.optString("error", "unknown"))
            }
        } catch (e: Exception) {
            RuleProviderAddResult.NetworkError(classify(e), detailOf(e))
        }
    }

    /**
     * Removes a rule-provider and its routing rule (action=provider_remove). A
     * proxy-group that was created for it stays — group management is deliberately
     * out of this app's scope, by design.
     */
    fun ruleProviderRemove(provider: String): RuleProviderRemoveResult {
        return try {
            val reply = fetchApi {
                addQueryParameter("token", prefs.token)
                addQueryParameter("action", "provider_remove")
                addQueryParameter("provider", provider)
            }
            val body = reply.body
                ?: return RuleProviderRemoveResult.NetworkError(NetFailure.EMPTY_RESPONSE)
            val json = JSONObject(body)
            if (reply.ok) {
                RuleProviderRemoveResult.Success(
                    provider = json.optString("provider", provider),
                    removed = json.optBoolean("removed", false),
                )
            } else {
                RuleProviderRemoveResult.ApiError(reply.code, json.optString("error", "unknown"))
            }
        } catch (e: Exception) {
            RuleProviderRemoveResult.NetworkError(classify(e), detailOf(e))
        }
    }

    /** Shared by provider_list rows and the provider_add reply — identical shape. */
    private fun parseRuleProvider(entry: JSONObject) = RuleProvider(
        provider = entry.getString("provider"),
        url = entry.optString("url", ""),
        interval = entry.optInt("interval", DEFAULT_PROVIDER_INTERVAL),
        behavior = entry.optString("behavior", ""),
        format = entry.optString("format", ""),
        group = entry.optString("group", ""),
    )

    /**
     * ALL MagiTrickle rule groups (action=magitrickle_groups — a transparent
     * pass-through of MagiTrickle's own API), not just the Custom group this app
     * otherwise manages. Read-only. Runs on IO thread.
     */
    fun magitrickleGroups(): MagitrickleGroupsResult {
        return try {
            val reply = fetchApi {
                addQueryParameter("token", prefs.token)
                addQueryParameter("action", "magitrickle_groups")
            }
            val body = reply.body
                ?: return MagitrickleGroupsResult.NetworkError(NetFailure.EMPTY_RESPONSE)
            val json = JSONObject(body)
            if (reply.ok) {
                val array = json.getJSONArray("groups")
                val groups = buildList {
                    for (i in 0 until array.length()) {
                        val g = array.getJSONObject(i)
                        val rulesArray = g.optJSONArray("rules")
                        val rules = buildList {
                            for (r in 0 until (rulesArray?.length() ?: 0)) {
                                val ruleObj = rulesArray!!.getJSONObject(r)
                                add(
                                    MagitrickleRule(
                                        id = ruleObj.optString("id", ""),
                                        type = ruleObj.optString("type", ""),
                                        rule = ruleObj.optString("rule", ""),
                                        enable = ruleObj.optBoolean("enable", true),
                                    )
                                )
                            }
                        }
                        add(
                            MagitrickleGroup(
                                id = g.getString("id"),
                                name = g.getString("name"),
                                rules = rules,
                            )
                        )
                    }
                }
                MagitrickleGroupsResult.Success(groups)
            } else {
                MagitrickleGroupsResult.ApiError(reply.code, json.optString("error", "unknown"))
            }
        } catch (e: Exception) {
            MagitrickleGroupsResult.NetworkError(classify(e), detailOf(e))
        }
    }

    /** Active mihomo connections + traffic totals (action=mihomo_connections). Runs on IO thread. */
    fun mihomoConnections(): MihomoConnectionsResult {
        return try {
            val reply = fetchApi {
                addQueryParameter("token", prefs.token)
                addQueryParameter("action", "mihomo_connections")
            }
            val body = reply.body
                ?: return MihomoConnectionsResult.NetworkError(NetFailure.EMPTY_RESPONSE)
            val json = JSONObject(body)
            if (reply.ok) {
                // mihomo sends "connections": null (not []) when nothing is active.
                val array = json.optJSONArray("connections")
                val connections = buildList {
                    for (i in 0 until (array?.length() ?: 0)) {
                        val entry = array!!.getJSONObject(i)
                        val meta = entry.optJSONObject("metadata")
                        val host = meta?.optString("host").orEmpty()
                            .ifEmpty { meta?.optString("destinationIP").orEmpty() }
                        val chainsArray = entry.optJSONArray("chains")
                        val chains = buildList {
                            for (c in 0 until (chainsArray?.length() ?: 0)) {
                                add(chainsArray!!.getString(c))
                            }
                        }.reversed()
                        add(
                            MihomoConnection(
                                id = entry.getString("id"),
                                host = host,
                                sourceIP = meta?.optString("sourceIP").orEmpty(),
                                chains = chains,
                                download = entry.optLong("download", 0L),
                                upload = entry.optLong("upload", 0L),
                            )
                        )
                    }
                }
                MihomoConnectionsResult.Success(
                    MihomoConnectionsSnapshot(
                        downloadTotal = json.optLong("downloadTotal", 0L),
                        uploadTotal = json.optLong("uploadTotal", 0L),
                        connections = connections,
                    )
                )
            } else {
                MihomoConnectionsResult.ApiError(reply.code, json.optString("error", "unknown"))
            }
        } catch (e: Exception) {
            MihomoConnectionsResult.NetworkError(classify(e), detailOf(e))
        }
    }

    /**
     * Switches the active node of a Selector group (action=mihomo_select). Group and
     * proxy names may contain unicode/emoji — plain UTF-8 strings, the URL builder
     * percent-encodes them and the server re-encodes when proxying to mihomo.
     */
    fun mihomoSelect(group: String, proxy: String): OkResult = okAction {
        addQueryParameter("action", "mihomo_select")
        addQueryParameter("group", group)
        addQueryParameter("proxy", proxy)
    }

    /**
     * Closes one mihomo connection (action=mihomo_connection_close). The router
     * answers ok:true even for an already-gone id — success does not prove the
     * connection was still alive.
     */
    fun mihomoConnectionClose(id: String): OkResult = okAction {
        addQueryParameter("action", "mihomo_connection_close")
        addQueryParameter("id", id)
    }

    /**
     * Router-side ping, 3 packets (action=ping). [host] is an IP or a domain.
     * Blocks for several seconds on an unreachable host. Runs on IO thread.
     */
    fun ping(host: String): PingResult {
        return try {
            val reply = fetchApi(minReadSeconds = PING_READ_SECONDS) {
                addQueryParameter("token", prefs.token)
                addQueryParameter("action", "ping")
                addQueryParameter("host", host)
            }
            val body = reply.body
                ?: return PingResult.NetworkError(NetFailure.EMPTY_RESPONSE)
            val json = JSONObject(body)
            if (reply.ok) {
                PingResult.Success(
                    PingInfo(
                        host = json.optString("host", host),
                        reachable = json.getBoolean("reachable"),
                        lossPercent = json.optInt("loss_percent", if (json.getBoolean("reachable")) 0 else 100),
                        avgMs = if (json.isNull("avg_ms")) null else json.getDouble("avg_ms"),
                    )
                )
            } else {
                PingResult.ApiError(reply.code, json.optString("error", "unknown"))
            }
        } catch (e: Exception) {
            PingResult.NetworkError(classify(e), detailOf(e))
        }
    }

    /**
     * Router-side traceroute (action=traceroute). The router answers with the FULL
     * hop list in one response — this can take tens of seconds when intermediate
     * hops time out. Runs on IO thread.
     */
    fun traceroute(host: String): TracerouteResult {
        return try {
            val reply = fetchApi(minReadSeconds = TRACEROUTE_READ_SECONDS) {
                addQueryParameter("token", prefs.token)
                addQueryParameter("action", "traceroute")
                addQueryParameter("host", host)
            }
            val body = reply.body
                ?: return TracerouteResult.NetworkError(NetFailure.EMPTY_RESPONSE)
            val json = JSONObject(body)
            if (reply.ok) {
                val array = json.getJSONArray("hops")
                val hops = buildList {
                    for (i in 0 until array.length()) {
                        val entry = array.getJSONObject(i)
                        add(
                            TraceHop(
                                hop = entry.getInt("hop"),
                                ip = if (entry.isNull("ip")) null else entry.getString("ip"),
                                ms = if (entry.isNull("ms")) null else entry.getDouble("ms"),
                            )
                        )
                    }
                }
                TracerouteResult.Success(json.optString("host", host), hops)
            } else {
                TracerouteResult.ApiError(reply.code, json.optString("error", "unknown"))
            }
        } catch (e: Exception) {
            TracerouteResult.NetworkError(classify(e), detailOf(e))
        }
    }

    /**
     * WAN download speed measured BY THE ROUTER (action=speedtest). A long
     * synchronous call — several seconds, up to ~25 s server-side; callers must
     * show a progress indicator. Runs on IO thread.
     */
    fun speedtest(): SpeedtestResult {
        return try {
            val reply = fetchApi(minReadSeconds = SPEEDTEST_READ_SECONDS) {
                addQueryParameter("token", prefs.token)
                addQueryParameter("action", "speedtest")
            }
            val body = reply.body
                ?: return SpeedtestResult.NetworkError(NetFailure.EMPTY_RESPONSE)
            val json = JSONObject(body)
            if (reply.ok) {
                SpeedtestResult.Success(
                    SpeedtestInfo(
                        downloadMbps = json.getDouble("download_mbps"),
                        bytesPerSec = json.optLong("bytes_per_sec", 0L),
                    )
                )
            } else {
                SpeedtestResult.ApiError(reply.code, json.optString("error", "unknown"))
            }
        } catch (e: Exception) {
            SpeedtestResult.NetworkError(classify(e), detailOf(e))
        }
    }

    /**
     * Wake-on-LAN magic packet (action=wake). ok:true only means the packet was
     * SENT — WoL gives no confirmation the device actually woke up, so the UI must
     * not promise more than "packet sent". Strict AA:BB:CC:DD:EE:FF MAC format.
     */
    fun wake(mac: String): OkResult = okAction {
        addQueryParameter("action", "wake")
        addQueryParameter("mac", mac)
    }

    /**
     * mihomo's built-in latency test for ALL nodes of one group at once
     * (action=mihomo_group_delay — same mechanism as zashboard's Test button).
     * Nodes that don't answer within 5 s are absent from the result map.
     */
    fun mihomoGroupDelay(group: String): GroupDelayResult {
        return try {
            val reply = fetchApi(minReadSeconds = GROUP_DELAY_READ_SECONDS) {
                addQueryParameter("token", prefs.token)
                addQueryParameter("action", "mihomo_group_delay")
                addQueryParameter("group", group)
            }
            val body = reply.body
                ?: return GroupDelayResult.NetworkError(NetFailure.EMPTY_RESPONSE)
            val json = JSONObject(body)
            if (reply.ok) {
                val delays = buildMap {
                    for (name in json.keys()) put(name, json.getInt(name))
                }
                GroupDelayResult.Success(delays)
            } else {
                GroupDelayResult.ApiError(reply.code, json.optString("error", "unknown"))
            }
        } catch (e: Exception) {
            GroupDelayResult.NetworkError(classify(e), detailOf(e))
        }
    }

    /** Overlay filesystem usage (action=disk_info). Runs on IO thread. */
    fun diskInfo(): DiskInfoResult {
        return try {
            val reply = fetchApi {
                addQueryParameter("token", prefs.token)
                addQueryParameter("action", "disk_info")
            }
            val body = reply.body
                ?: return DiskInfoResult.NetworkError(NetFailure.EMPTY_RESPONSE)
            val json = JSONObject(body)
            if (reply.ok) {
                DiskInfoResult.Success(
                    DiskInfo(
                        mount = json.optString("mount", ""),
                        totalKb = json.getLong("total_kb"),
                        usedKb = json.getLong("used_kb"),
                        availableKb = json.getLong("available_kb"),
                        usedPercent = json.getInt("used_percent"),
                    )
                )
            } else {
                DiskInfoResult.ApiError(reply.code, json.optString("error", "unknown"))
            }
        } catch (e: Exception) {
            DiskInfoResult.NetworkError(classify(e), detailOf(e))
        }
    }

    /**
     * Up to 100 logread lines grep'd for [service] (action=service_log). An empty
     * list is a legitimate answer, not an error. Runs on IO thread.
     */
    fun serviceLog(service: String): ServiceLogResult {
        return try {
            val reply = fetchApi {
                addQueryParameter("token", prefs.token)
                addQueryParameter("action", "service_log")
                addQueryParameter("service", service)
            }
            val body = reply.body
                ?: return ServiceLogResult.NetworkError(NetFailure.EMPTY_RESPONSE)
            val json = JSONObject(body)
            if (reply.ok) {
                val array = json.optJSONArray("lines")
                val lines = buildList {
                    for (i in 0 until (array?.length() ?: 0)) add(array!!.getString(i))
                }
                ServiceLogResult.Success(lines)
            } else {
                ServiceLogResult.ApiError(reply.code, json.optString("error", "unknown"))
            }
        } catch (e: Exception) {
            ServiceLogResult.NetworkError(classify(e), detailOf(e))
        }
    }

    /** Installed version of all four managed services (action=versions). Runs on IO thread. */
    fun versions(): VersionsResult {
        return try {
            val reply = fetchApi {
                addQueryParameter("token", prefs.token)
                addQueryParameter("action", "versions")
            }
            val body = reply.body
                ?: return VersionsResult.NetworkError(NetFailure.EMPTY_RESPONSE)
            val json = JSONObject(body)
            if (reply.ok) {
                VersionsResult.Success(
                    ServiceVersions(
                        mihomo = json.optString("mihomo", ""),
                        magitrickle = json.optString("magitrickle", ""),
                        zapret = json.optString("zapret", ""),
                        zapret2 = json.optString("zapret2", ""),
                    )
                )
            } else {
                VersionsResult.ApiError(reply.code, json.optString("error", "unknown"))
            }
        } catch (e: Exception) {
            VersionsResult.NetworkError(classify(e), detailOf(e))
        }
    }

    /** Checks GitHub for a newer mihomo release (action=mihomo_check_update). Runs on IO thread. */
    fun mihomoCheckUpdate(): MihomoCheckUpdateResult {
        return try {
            val reply = fetchApi(minReadSeconds = VERSION_CHECK_READ_SECONDS) {
                addQueryParameter("token", prefs.token)
                addQueryParameter("action", "mihomo_check_update")
            }
            val body = reply.body
                ?: return MihomoCheckUpdateResult.NetworkError(NetFailure.EMPTY_RESPONSE)
            val json = JSONObject(body)
            if (reply.ok) {
                MihomoCheckUpdateResult.Success(
                    MihomoUpdateInfo(
                        current = json.optString("current", ""),
                        latest = json.optString("latest", ""),
                        updateAvailable = json.getBoolean("update_available"),
                    )
                )
            } else {
                MihomoCheckUpdateResult.ApiError(reply.code, json.optString("error", "unknown"))
            }
        } catch (e: Exception) {
            MihomoCheckUpdateResult.NetworkError(classify(e), detailOf(e))
        }
    }

    /**
     * Replaces the mihomo binary and restarts the service (action=mihomo_update).
     * Same class of action as reboot — callers MUST collect an explicit
     * confirmation first. A synchronous call that can take a noticeable while.
     */
    fun mihomoUpdate(): OkResult = okAction(minReadSeconds = MIHOMO_UPDATE_READ_SECONDS) {
        addQueryParameter("action", "mihomo_update")
    }

    /**
     * LIVE per-node IPv6 (AAAA) reachability (action=mihomo_node_ipv6) — a real DNS
     * round-trip per node on the router, deliberately uncached client-side (see
     * [NodeIpv6Result]). Slower than mihomo_proxies; call it separately so it never
     * blocks the first paint of the node list. Runs on IO thread.
     */
    fun mihomoNodeIpv6(): NodeIpv6Result {
        return try {
            val reply = fetchApi(minReadSeconds = NODE_IPV6_READ_SECONDS) {
                addQueryParameter("token", prefs.token)
                addQueryParameter("action", "mihomo_node_ipv6")
            }
            val body = reply.body
                ?: return NodeIpv6Result.NetworkError(NetFailure.EMPTY_RESPONSE)
            val json = JSONObject(body)
            if (reply.ok) {
                val array = json.getJSONArray("nodes")
                val map = buildMap {
                    for (i in 0 until array.length()) {
                        val entry = array.getJSONObject(i)
                        put(entry.getString("name"), entry.optBoolean("ipv6", false))
                    }
                }
                NodeIpv6Result.Success(map)
            } else {
                NodeIpv6Result.ApiError(reply.code, json.optString("error", "unknown"))
            }
        } catch (e: Exception) {
            NodeIpv6Result.NetworkError(classify(e), detailOf(e))
        }
    }

    /** Shared runner for actions whose success payload is just {"ok":true}. */
    private fun okAction(minReadSeconds: Int? = null, query: HttpUrl.Builder.() -> Unit): OkResult {
        return try {
            val reply = fetchApi(minReadSeconds = minReadSeconds) {
                addQueryParameter("token", prefs.token)
                query()
            }
            val body = reply.body
                ?: return OkResult.NetworkError(NetFailure.EMPTY_RESPONSE)
            val json = JSONObject(body)
            if (reply.ok) OkResult.Success
            else OkResult.ApiError(reply.code, json.optString("error", "unknown"))
        } catch (e: Exception) {
            OkResult.NetworkError(classify(e), detailOf(e))
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
            val reply = fetch(url)
            when {
                reply.ok || reply.code == 400 -> TestResult.Connected
                reply.code == 403 -> TestResult.ConnectedBadToken
                reply.code == 500 -> TestResult.ConnectedNoToken
                else -> TestResult.HttpError(reply.code)
            }
        } catch (e: Exception) {
            TestResult.Failed(classify(e), detailOf(e))
        }
    }

    private fun classify(e: Exception): NetFailure = when (e) {
        is SocketTimeoutException -> NetFailure.TIMEOUT
        is ConnectException, is NoRouteToHostException -> NetFailure.UNREACHABLE
        is UnknownHostException -> NetFailure.UNKNOWN_HOST
        // HttpUrl.Builder throws this for a malformed host/port (fetchApi builds
        // the URL inside the shared try now).
        is IllegalArgumentException -> NetFailure.INVALID_ADDRESS
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

    companion object {
        const val MIN_TIMEOUT_SECONDS = 3
        const val MAX_TIMEOUT_SECONDS = 60
        /** Connect timeout for the primary attempt when a fallback address exists. */
        const val FAILOVER_CONNECT_SECONDS = 3
        // Floor read timeouts for the long-running diagnostic actions — the
        // user-configured timeout (3..60 s, default 10) is meant for instant CGI
        // calls and would cut these off mid-run.
        private const val PING_READ_SECONDS = 20
        private const val TRACEROUTE_READ_SECONDS = 90
        private const val SPEEDTEST_READ_SECONDS = 40
        private const val GROUP_DELAY_READ_SECONDS = 20
        // mihomo_check_update calls out to GitHub from the router; mihomo_update
        // downloads+swaps the binary and restarts the daemon; mihomo_node_ipv6 does
        // one live DNS round-trip per subscription node.
        private const val VERSION_CHECK_READ_SECONDS = 20
        private const val MIHOMO_UPDATE_READ_SECONDS = 60
        private const val NODE_IPV6_READ_SECONDS = 30
        // checkconn probes several hosts from the router, 2-4 s typical.
        private const val CHECK_CONN_READ_SECONDS = 15
        // provider_add makes the router download the rule-set from the internet
        // and run a full mihomo config test before it answers.
        private const val PROVIDER_ADD_READ_SECONDS = 60

        /** Refresh period used when the router omits it / the UI has no explicit choice: once a day. */
        const val DEFAULT_PROVIDER_INTERVAL = 86400
        const val MIN_PROVIDER_INTERVAL = 60
        const val MAX_PROVIDER_INTERVAL = 604800

        /** Router-side rule is `[a-z][a-z0-9-]*` — validated here so a bad name never reaches the API. */
        private val PROVIDER_NAME_REGEX = Regex("^[a-z][a-z0-9-]*$")

        fun isValidProviderName(name: String): Boolean = PROVIDER_NAME_REGEX.matches(name)
    }
}

