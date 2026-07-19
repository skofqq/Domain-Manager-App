package com.skofqq.domainmanager.ui

import android.content.Context
import android.content.Intent
import androidx.core.content.pm.ShortcutInfoCompat
import androidx.core.content.pm.ShortcutManagerCompat
import androidx.core.graphics.drawable.IconCompat
import com.skofqq.domainmanager.R

/** One entry in the fixed catalog the user picks 2-4 of, in Settings → Ярлыки. */
data class ShortcutSpec(val id: String, val labelRes: Int)

val SHORTCUT_CATALOG = listOf(
    ShortcutSpec("status", R.string.shortcut_status),
    ShortcutSpec("devices", R.string.shortcut_devices),
    ShortcutSpec("reboot", R.string.shortcut_reboot),
    ShortcutSpec("mihomo_update", R.string.shortcut_mihomo_update),
    ShortcutSpec("speedtest", R.string.shortcut_speedtest),
    // Generic label — only used by the Settings picker list. The actually
    // published shortcut's label is built from the live-running engine name
    // (see [AppShortcuts.publish]'s activeZapretEngine parameter).
    ShortcutSpec("zapret_restart", R.string.shortcut_zapret_restart),
)

/** Carries which shortcut was tapped through MainActivity's launch intent. */
const val EXTRA_SHORTCUT_ACTION = "shortcut_action"

/** Which engine ("zapret"/"zapret2") the "zapret_restart" shortcut was published for. */
const val EXTRA_SHORTCUT_ZAPRET_ENGINE = "shortcut_zapret_engine"

/**
 * Publishes dynamic App Shortcuts (long-press the launcher icon) for the ids
 * enabled in Settings. Every shortcut only OPENS the app at the relevant
 * screen — actions that need confirmation (reboot, the mihomo update button)
 * land exactly on the screen that already has that confirm dialog; nothing
 * ever fires from the shortcut tap itself. The two non-destructive additions
 * (speed test, zapret restart) are the exception: they run immediately on
 * open, no extra tap, same as the LuCI web UI would let you do in one click.
 */
object AppShortcuts {
    /**
     * [activeZapretEngine] is whichever of "zapret"/"zapret2" is currently
     * running (from the last svc_list), or null if neither is (or it hasn't
     * been checked yet this session) — the "zapret_restart" entry is simply
     * omitted from the published set in that case, since there is nothing to
     * restart and no single engine name to show. Every other entry's label is
     * the static catalog string and ignores this parameter.
     */
    fun publish(context: Context, ids: List<String>, activeZapretEngine: String? = null) {
        val shortcuts = ids.mapNotNull { id -> SHORTCUT_CATALOG.find { it.id == id } }
            .take(4)
            .mapNotNull { spec -> buildShortcut(context, spec, activeZapretEngine) }
        ShortcutManagerCompat.removeAllDynamicShortcuts(context)
        ShortcutManagerCompat.setDynamicShortcuts(context, shortcuts)
    }

    private fun buildShortcut(
        context: Context,
        spec: ShortcutSpec,
        activeZapretEngine: String?,
    ): ShortcutInfoCompat? {
        val label: String
        val intent = Intent(context, MainActivity::class.java).apply {
            action = Intent.ACTION_VIEW
            flags = Intent.FLAG_ACTIVITY_CLEAR_TOP or Intent.FLAG_ACTIVITY_SINGLE_TOP
            putExtra(EXTRA_SHORTCUT_ACTION, spec.id)
        }
        if (spec.id == "zapret_restart") {
            val engine = activeZapretEngine ?: return null
            label = context.getString(R.string.shortcut_zapret_restart_named, engine)
            intent.putExtra(EXTRA_SHORTCUT_ZAPRET_ENGINE, engine)
        } else {
            label = context.getString(spec.labelRes)
        }
        return ShortcutInfoCompat.Builder(context, spec.id)
            .setShortLabel(label)
            // Full adaptive launcher icon (background+foreground composited) —
            // the monochrome-only layer would render as an invisible white
            // silhouette without the system's own themed-icon background.
            .setIcon(IconCompat.createWithResource(context, R.mipmap.ic_launcher))
            .setIntent(intent)
            .build()
    }
}
