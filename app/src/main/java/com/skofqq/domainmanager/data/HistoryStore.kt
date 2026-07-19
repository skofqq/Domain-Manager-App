package com.skofqq.domainmanager.data

import android.content.ContentValues
import android.content.Context
import android.database.sqlite.SQLiteDatabase
import android.database.sqlite.SQLiteOpenHelper
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow

/**
 * One successfully sent add — either a routing add (action=add) or a strategy add
 * (action=strat_add). Phone-local only: the router keeps no timestamps, so this
 * log is never synced with (or reconciled against) the router's lists.
 */
data class HistoryEntry(
    val id: Long,
    val domain: String,
    val timeMillis: Long,
    /** "routing" (target set) or "strategy" (engine/strategy set). */
    val kind: String,
    val target: String?,
    val engine: String?,
    val strategy: Int?,
)



/**
 * Local append-only history of added domains, backed by a small SQLite table.
 * Deliberately NOT Room: this build uses AGP's built-in Kotlin without a KSP
 * pipeline, and one table doesn't justify adding an annotation processor.
 *
 * All methods that touch the database must be called off the main thread.
 */
class HistoryStore private constructor(context: Context) {

    private val helper = object : SQLiteOpenHelper(context, DB_NAME, null, DB_VERSION) {
        override fun onCreate(db: SQLiteDatabase) {
            db.execSQL(
                """
                CREATE TABLE history (
                    id INTEGER PRIMARY KEY AUTOINCREMENT,
                    domain TEXT NOT NULL,
                    time_millis INTEGER NOT NULL,
                    kind TEXT NOT NULL,
                    target TEXT,
                    engine TEXT,
                    strategy INTEGER,
                    router_id TEXT
                )
                """.trimIndent()
            )
            db.execSQL("CREATE INDEX idx_history_time ON history(time_millis DESC)")
        }

        override fun onUpgrade(db: SQLiteDatabase, oldVersion: Int, newVersion: Int) {
            if (oldVersion < 2) {
                // Multi-router update: history becomes per-profile. Old rows get
                // NULL here and are adopted by the first profile that touches the
                // store (right after migration that IS the pre-update router).
                db.execSQL("ALTER TABLE history ADD COLUMN router_id TEXT")
            }
        }
    }

    /** null until the first [reload]; newest first, scoped to one router profile. */
    private val _entries = MutableStateFlow<List<HistoryEntry>?>(null)
    val entries: StateFlow<List<HistoryEntry>?> = _entries

    /** Router the current [_entries] snapshot belongs to (for live re-reads on insert). */
    @Volatile private var loadedRouterId: String? = null

    fun logRouting(routerId: String, domain: String, target: String) =
        insert(routerId, domain, "routing", target = target, engine = null, strategy = null)

    fun logStrategy(routerId: String, domain: String, engine: String, strategy: Int?) =
        insert(routerId, domain, "strategy", target = null, engine = engine, strategy = strategy)

    private fun insert(routerId: String, domain: String, kind: String, target: String?, engine: String?, strategy: Int?) {
        adoptLegacyRows(routerId)
        helper.writableDatabase.insert(
            "history",
            null,
            ContentValues().apply {
                put("domain", domain)
                put("time_millis", System.currentTimeMillis())
                put("kind", kind)
                put("router_id", routerId)
                if (target != null) put("target", target)
                if (engine != null) put("engine", engine)
                if (strategy != null) put("strategy", strategy)
            },
        )
        // Keep the History screen live if it's already been opened this session.
        loadedRouterId?.let { reload(it) }
    }

    /**
     * Rows written before the multi-router update have no router_id; the first
     * profile to use the store after the update is the pre-update router, so it
     * adopts them. Idempotent — after the first pass there are no NULL rows.
     */
    private fun adoptLegacyRows(routerId: String) {
        helper.writableDatabase.execSQL(
            "UPDATE history SET router_id = ? WHERE router_id IS NULL",
            arrayOf(routerId),
        )
    }

    fun reload(routerId: String) {
        adoptLegacyRows(routerId)
        loadedRouterId = routerId
        val list = buildList {
            helper.readableDatabase.query(
                "history", null, "router_id = ?", arrayOf(routerId), null, null,
                "time_millis DESC", MAX_ENTRIES.toString(),
            ).use { c ->
                val id = c.getColumnIndexOrThrow("id")
                val domain = c.getColumnIndexOrThrow("domain")
                val time = c.getColumnIndexOrThrow("time_millis")
                val kind = c.getColumnIndexOrThrow("kind")
                val target = c.getColumnIndexOrThrow("target")
                val engine = c.getColumnIndexOrThrow("engine")
                val strategy = c.getColumnIndexOrThrow("strategy")
                while (c.moveToNext()) {
                    add(
                        HistoryEntry(
                            id = c.getLong(id),
                            domain = c.getString(domain),
                            timeMillis = c.getLong(time),
                            kind = c.getString(kind),
                            target = if (c.isNull(target)) null else c.getString(target),
                            engine = if (c.isNull(engine)) null else c.getString(engine),
                            strategy = if (c.isNull(strategy)) null else c.getInt(strategy),
                        )
                    )
                }
            }
        }
        _entries.value = list
    }

    companion object {
        private const val DB_NAME = "domain_history.db"
        private const val DB_VERSION = 2
        /** The screen shows at most this many; older rows stay in the table. */
        private const val MAX_ENTRIES = 500

        @Volatile private var instance: HistoryStore? = null

        fun get(context: Context): HistoryStore =
            instance ?: synchronized(this) {
                instance ?: HistoryStore(context.applicationContext).also { instance = it }
            }
    }
}
