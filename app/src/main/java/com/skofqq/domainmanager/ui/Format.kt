package com.skofqq.domainmanager.ui

import java.util.Locale

/** "512 B" / "3.4 KB" / "1.2 MB" / "2.05 GB" — for traffic counters and memory sizes. */
internal fun formatBytes(bytes: Long): String {
    if (bytes < 1024) return "$bytes B"
    val kb = bytes / 1024.0
    if (kb < 1024) return String.format(Locale.US, "%.1f KB", kb)
    val mb = kb / 1024.0
    if (mb < 1024) return String.format(Locale.US, "%.1f MB", mb)
    return String.format(Locale.US, "%.2f GB", mb / 1024.0)
}
