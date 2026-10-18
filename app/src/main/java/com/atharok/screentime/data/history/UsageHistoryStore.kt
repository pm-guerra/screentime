package com.atharok.screentime.data.history

import android.content.ContentValues
import android.content.Context
import android.database.sqlite.SQLiteDatabase
import android.database.sqlite.SQLiteOpenHelper

/**
 * Local copy of the usage intervals of complete days.
 *
 * Android deletes its detailed usage events after about 10 days, so the intervals computed
 * from them are saved here to keep the history beyond that limit.
 */
class UsageHistoryStore(context: Context) : SQLiteOpenHelper(context, DATABASE_NAME, null, DATABASE_VERSION) {

    data class StoredPackage(
        val label: String?,
        val launchable: Boolean
    )

    companion object {
        private const val DATABASE_NAME = "usage_history.db"
        private const val DATABASE_VERSION = 1

        private const val TABLE_SESSIONS = "sessions"
        private const val TABLE_SAVED_RANGES = "saved_ranges"
        private const val TABLE_PACKAGES = "packages"

        private const val COLUMN_PACKAGE_NAME = "package_name"
        private const val COLUMN_START = "start"
        private const val COLUMN_END = "end"
        private const val COLUMN_LABEL = "label"
        private const val COLUMN_LAUNCHABLE = "launchable"
    }

    override fun onCreate(db: SQLiteDatabase) {
        db.execSQL(
            "CREATE TABLE $TABLE_SESSIONS (" +
                "$COLUMN_PACKAGE_NAME TEXT NOT NULL, " +
                "$COLUMN_START INTEGER NOT NULL, " +
                "$COLUMN_END INTEGER NOT NULL)"
        )
        db.execSQL("CREATE INDEX index_sessions_start ON $TABLE_SESSIONS ($COLUMN_START)")

        // Time ranges whose sessions are saved, so that gaps (no data saved) can be told apart
        // from periods without usage.
        db.execSQL(
            "CREATE TABLE $TABLE_SAVED_RANGES (" +
                "$COLUMN_START INTEGER NOT NULL, " +
                "$COLUMN_END INTEGER NOT NULL)"
        )

        // Keeps the name of the apps and whether they could be launched, so that the history of
        // uninstalled apps can still be displayed and filtered.
        db.execSQL(
            "CREATE TABLE $TABLE_PACKAGES (" +
                "$COLUMN_PACKAGE_NAME TEXT PRIMARY KEY NOT NULL, " +
                "$COLUMN_LABEL TEXT, " +
                "$COLUMN_LAUNCHABLE INTEGER NOT NULL)"
        )
    }

    override fun onUpgrade(db: SQLiteDatabase, oldVersion: Int, newVersion: Int) {}

    /**
     * Saves the intervals between the end of the already saved history and [saveUntil].
     * The already saved history is never rewritten.
     *
     * @param windowStart Start of the period covered by [intervalsByPackage].
     * @param saveUntil Start of the current day: only complete days are saved.
     */
    fun save(
        intervalsByPackage: Map<String, List<Pair<Long, Long>>>,
        windowStart: Long,
        saveUntil: Long,
        packageDetails: (packageName: String) -> StoredPackage
    ) {
        val db = writableDatabase
        db.beginTransaction()
        try {
            val saveFrom = maxOf(windowStart, queryCoveredUntil(db) ?: windowStart)
            if (saveFrom < saveUntil) {
                intervalsByPackage.forEach { (packageName: String, intervals: List<Pair<Long, Long>>) ->
                    val clippedIntervals = clipIntervals(intervals, saveFrom, saveUntil)
                    if (clippedIntervals.isNotEmpty()) {
                        clippedIntervals.forEach { (start: Long, end: Long) ->
                            db.insert(TABLE_SESSIONS, null, ContentValues().apply {
                                put(COLUMN_PACKAGE_NAME, packageName)
                                put(COLUMN_START, start)
                                put(COLUMN_END, end)
                            })
                        }
                        val details = packageDetails(packageName)
                        db.insertWithOnConflict(TABLE_PACKAGES, null, ContentValues().apply {
                            put(COLUMN_PACKAGE_NAME, packageName)
                            put(COLUMN_LABEL, details.label)
                            put(COLUMN_LAUNCHABLE, if (details.launchable) 1 else 0)
                        }, if (details.label != null) {
                            SQLiteDatabase.CONFLICT_REPLACE
                        } else {
                            // The app is no longer installed: keeps the details already saved.
                            SQLiteDatabase.CONFLICT_IGNORE
                        })
                    }
                }

                db.insert(TABLE_SAVED_RANGES, null, ContentValues().apply {
                    put(COLUMN_START, saveFrom)
                    put(COLUMN_END, saveUntil)
                })
            }
            db.setTransactionSuccessful()
        } finally {
            db.endTransaction()
        }
    }

    /**
     * @return The saved intervals clipped to [start, endExclusive), grouped by package.
     */
    fun getIntervals(start: Long, endExclusive: Long): Map<String, List<Pair<Long, Long>>> {
        val intervalsByPackage = mutableMapOf<String, MutableList<Pair<Long, Long>>>()

        readableDatabase.query(
            TABLE_SESSIONS,
            arrayOf(COLUMN_PACKAGE_NAME, COLUMN_START, COLUMN_END),
            "$COLUMN_START < ? AND $COLUMN_END > ?",
            arrayOf(endExclusive.toString(), start.toString()),
            null, null, null
        ).use { cursor ->
            while (cursor.moveToNext()) {
                intervalsByPackage
                    .getOrPut(cursor.getString(0)) { mutableListOf() }
                    .add(maxOf(cursor.getLong(1), start) to minOf(cursor.getLong(2), endExclusive))
            }
        }

        return intervalsByPackage
    }

    /**
     * @return The end of the most recent saved range, or null if nothing has been saved yet.
     */
    fun getCoveredUntil(): Long? = queryCoveredUntil(readableDatabase)

    /**
     * @return The start of the oldest saved range, or null if nothing has been saved yet.
     */
    fun getCoveredFrom(): Long? = readableDatabase
        .rawQuery("SELECT MIN($COLUMN_START) FROM $TABLE_SAVED_RANGES", null)
        .use { cursor ->
            if (cursor.moveToFirst() && !cursor.isNull(0)) cursor.getLong(0) else null
        }

    fun getPackages(): Map<String, StoredPackage> {
        val packages = mutableMapOf<String, StoredPackage>()

        readableDatabase.query(
            TABLE_PACKAGES,
            arrayOf(COLUMN_PACKAGE_NAME, COLUMN_LABEL, COLUMN_LAUNCHABLE),
            null, null, null, null, null
        ).use { cursor ->
            while (cursor.moveToNext()) {
                packages[cursor.getString(0)] = StoredPackage(
                    label = if (cursor.isNull(1)) null else cursor.getString(1),
                    launchable = cursor.getInt(2) == 1
                )
            }
        }

        return packages
    }

    private fun queryCoveredUntil(db: SQLiteDatabase): Long? = db
        .rawQuery("SELECT MAX($COLUMN_END) FROM $TABLE_SAVED_RANGES", null)
        .use { cursor ->
            if (cursor.moveToFirst() && !cursor.isNull(0)) cursor.getLong(0) else null
        }
}
