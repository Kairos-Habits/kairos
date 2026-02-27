package com.rghsoftware.kairos.sync

import app.cash.sqldelight.db.SqlDriver
import app.cash.sqldelight.driver.jdbc.sqlite.JdbcSqliteDriver
import com.rghsoftware.kairos.db.KairosDatabase
import java.io.File

class JvmDatabaseDriverFactory(
    private val dbPath: String? = null,
) : DatabaseDriverFactory {
    override fun createDriver(): SqlDriver {
        val path = dbPath ?: File(System.getProperty("java.io.tmpdir"), "kairos.db").absolutePath
        val driver: SqlDriver = JdbcSqliteDriver("jdbc:sqlite:$path")
        KairosDatabase.Schema.create(driver)
        return driver
    }
}

actual fun createDatabaseDriverFactory(): DatabaseDriverFactory {
    return JvmDatabaseDriverFactory()
}
