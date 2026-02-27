package com.rghsoftware.kairos.sync

import android.content.Context
import app.cash.sqldelight.db.SqlDriver
import app.cash.sqldelight.driver.android.AndroidSqliteDriver
import com.rghsoftware.kairos.db.KairosDatabase

class AndroidDatabaseDriverFactory(private val context: Context) : DatabaseDriverFactory {
    override fun createDriver(): SqlDriver {
        return AndroidSqliteDriver(KairosDatabase.Schema, context, "kairos.db")
    }
}

private var _context: Context? = null

fun setAndroidContext(context: Context) {
    _context = context
}

actual fun createDatabaseDriverFactory(): DatabaseDriverFactory {
    val ctx = _context ?: error("Android context not set. Call setAndroidContext() first.")
    return AndroidDatabaseDriverFactory(ctx)
}
