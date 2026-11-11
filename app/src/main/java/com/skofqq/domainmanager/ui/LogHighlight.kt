package com.skofqq.domainmanager.ui

import androidx.compose.runtime.Composable
import androidx.compose.runtime.Immutable
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.AnnotatedString
import androidx.compose.ui.text.SpanStyle
import androidx.compose.ui.text.buildAnnotatedString
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextDecoration
import com.skofqq.domainmanager.ui.theme.Helm
import com.skofqq.domainmanager.ui.theme.Tints

/** Severity of one log line, for its colour bar and background. */
enum class LogLevel { ERROR, WARN, INFO, DEBUG, NONE }

/** Colours of the log syntax highlighting, resolved from the current theme. */
@Immutable
data class LogPalette(
    val muted: Color,
    val text: Color,
    val error: Color,
    val warn: Color,
    val info: Color,
    val debug: Color,
    val process: Color,
    val key: Color,
    val string: Color,
    val address: Color,
    val url: Color,
    val arrow: Color,
)

@Composable
fun rememberLogPalette(): LogPalette = LogPalette(
    muted = Helm.colors.text2,
    text = Helm.colors.text,
    error = Tints.red.fg,
    warn = Tints.amber.fg,
    info = Helm.colors.accent,
    debug = Tints.gray.fg,
    process = Tints.purple.fg,
    key = Tints.teal.fg,
    string = Tints.green.fg,
    address = Tints.orange.fg,
    url = Tints.blue.fg,
    arrow = Helm.colors.accent,
)

fun LogPalette.colorOf(level: LogLevel): Color = when (level) {
    LogLevel.ERROR -> error
    LogLevel.WARN -> warn
    LogLevel.INFO -> info
    LogLevel.DEBUG -> debug
    LogLevel.NONE -> muted.copy(alpha = 0.35f)
}

// logread: "Tue Nov 11 01:23:45 2026 daemon.notice mihomo[1234]: …"
private val TIMESTAMP = Regex("""^\w{3} \w{3} +\d{1,2} \d{2}:\d{2}:\d{2} \d{4}""")
private val FACILITY = Regex("""\b(?:kern|user|mail|daemon|auth|authpriv|syslog|lpr|news|cron|local\d)\.(emerg|alert|crit|err|error|warn|warning|notice|info|debug)\b""")
private val PROCESS = Regex("""[\w.@/-]+\[\d+]:""")
// mihomo/zapret style "level=info", "[ERROR]", "WARN" words.
private val LEVEL_KV = Regex("""\blevel=("?)(\w+)\1""", RegexOption.IGNORE_CASE)
private val LEVEL_WORD = Regex("""\[?\b(FATAL|PANIC|ERROR|ERR|FAILED|FAIL|WARNING|WARN|INFO|DEBUG|TRACE|FTL|PNC|WRN|INF|DBG|TRC)\b]?""", RegexOption.IGNORE_CASE)

// ANSI SGR colour codes (MagiTrickle and other zerolog/logrus daemons colour their output). The ESC
// byte itself is often lost on the way through logread, so it is optional here.
private val ANSI = Regex("""\u001B?\[(\d{1,3}(?:;\d{1,3})*)?m""")

/** One run of ANSI styling over the cleaned text. */
private data class AnsiRun(val start: Int, val end: Int, val fg: Int?, val bold: Boolean, val dim: Boolean)

/** Removes ANSI codes and returns the clean text plus the styled runs they described. */
private fun parseAnsi(line: String): Pair<String, List<AnsiRun>> {
    if (!ANSI.containsMatchIn(line)) return line to emptyList()
    val out = StringBuilder()
    val runs = mutableListOf<AnsiRun>()
    var fg: Int? = null
    var bold = false
    var dim = false
    var runStart = 0
    var last = 0
    fun close() {
        if (out.length > runStart && (fg != null || bold || dim)) runs += AnsiRun(runStart, out.length, fg, bold, dim)
        runStart = out.length
    }
    for (m in ANSI.findAll(line)) {
        out.append(line, last, m.range.first)
        last = m.range.last + 1
        close()
        val codes = m.groupValues[1].ifEmpty { "0" }.split(';').mapNotNull { it.toIntOrNull() }
        for (c in codes) when (c) {
            0 -> { fg = null; bold = false; dim = false }
            1 -> bold = true
            2 -> dim = true
            22 -> { bold = false; dim = false }
            39 -> fg = null
            in 30..37, in 90..97 -> fg = c
        }
    }
    out.append(line, last, line.length)
    close()
    return out.toString() to runs
}

private fun LogPalette.ansiColor(code: Int): Color = when (code % 60) {
    30 -> if (code >= 90) muted else text
    31 -> error
    32 -> string
    33 -> warn
    34 -> url
    35 -> process
    36 -> key
    else -> text
}

/** The line as it reads without colour codes. */
fun cleanLogLine(line: String): String = parseAnsi(line).first
private val KEY = Regex("""\b[A-Za-z_][\w.-]*(?==)""")
private val QUOTED = Regex(""""(?:[^"\\]|\\.)*"""")
private val URL = Regex("""\b(?:https?|wss?)://[^\s"']+""")
private val IPV4 = Regex("""\b(?:\d{1,3}\.){3}\d{1,3}(?::\d{1,5})?\b""")
private val ARROW = Regex("""-->|->|=>|<--|<-""")

private fun levelOf(word: String): LogLevel = when (word.lowercase()) {
    "emerg", "alert", "crit", "err", "error", "fatal", "panic", "failed", "fail", "ftl", "pnc" -> LogLevel.ERROR
    "warn", "warning", "wrn" -> LogLevel.WARN
    "notice", "info", "inf" -> LogLevel.INFO
    "debug", "trace", "dbg", "trc" -> LogLevel.DEBUG
    else -> LogLevel.NONE
}

/**
 * The most severe level a line mentions: the syslog facility.level first, then a
 * `level=` field, then bare words like ERROR/WARN.
 */
fun logLevelOf(raw: String): LogLevel {
    val line = cleanLogLine(raw)
    FACILITY.find(line)?.let { m ->
        val fromFacility = levelOf(m.groupValues[1])
        // daemon.notice with level=error inside: the inner one is the real verdict.
        val inner = LEVEL_KV.find(line)?.let { levelOf(it.groupValues[2]) }
        return listOfNotNull(fromFacility, inner).minByOrNull { it.ordinal } ?: fromFacility
    }
    LEVEL_KV.find(line)?.let { return levelOf(it.groupValues[2]) }
    return LEVEL_WORD.findAll(line).map { levelOf(it.groupValues[1]) }.minByOrNull { it.ordinal } ?: LogLevel.NONE
}

/**
 * Syntax-highlighted log line. Spans are added from the broadest to the most specific,
 * so a later, narrower rule (an address inside a quoted message) wins over an earlier one.
 */
fun highlightLogLine(raw: String, p: LogPalette): AnnotatedString = buildAnnotatedString {
    val (line, ansi) = parseAnsi(raw)
    append(line)
    addStyle(SpanStyle(color = p.text), 0, line.length)
    fun paint(regex: Regex, group: Int = 0, style: (MatchResult) -> SpanStyle) {
        regex.findAll(line).forEach { m ->
            val range = m.groups[group]?.range ?: return@forEach
            addStyle(style(m), range.first, range.last + 1)
        }
    }
    paint(QUOTED) { SpanStyle(color = p.string) }
    paint(KEY) { SpanStyle(color = p.key) }
    paint(LEVEL_WORD) { m -> SpanStyle(color = p.colorOf(levelOf(m.groupValues[1])), fontWeight = FontWeight.Bold) }
    paint(LEVEL_KV, group = 2) { m -> SpanStyle(color = p.colorOf(levelOf(m.groupValues[2])), fontWeight = FontWeight.Bold) }
    paint(IPV4) { SpanStyle(color = p.address) }
    paint(URL) { SpanStyle(color = p.url, textDecoration = TextDecoration.Underline) }
    paint(ARROW) { SpanStyle(color = p.arrow, fontWeight = FontWeight.Bold) }
    paint(PROCESS) { SpanStyle(color = p.process, fontWeight = FontWeight.SemiBold) }
    paint(FACILITY) { m -> SpanStyle(color = p.colorOf(levelOf(m.groupValues[1])), fontWeight = FontWeight.Bold) }
    paint(TIMESTAMP) { SpanStyle(color = p.muted) }
    // The daemon's own colours (ANSI) are the most specific knowledge there is: they go last.
    ansi.forEach { r ->
        addStyle(
            SpanStyle(
                color = r.fg?.let { p.ansiColor(it) }?.let { if (r.dim) it.copy(alpha = 0.7f) else it } ?: Color.Unspecified,
                fontWeight = if (r.bold) FontWeight.Bold else null,
            ),
            r.start,
            r.end,
        )
    }
}
