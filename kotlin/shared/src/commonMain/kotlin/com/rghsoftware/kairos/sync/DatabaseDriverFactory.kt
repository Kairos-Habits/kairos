package com.rghsoftware.kairos.sync

import app.cash.sqldelight.db.SqlDriver

/**
 * Platform-specific database driver factory.
 * Each platform provides its own implementation via createDriver().
 */
interface DatabaseDriverFactory {
    fun createDriver(): SqlDriver
}

/**
 * Creates the platform-specific database driver factory.
 * Call this from platform code with appropriate configuration.
 */
expect fun createDatabaseDriverFactory(): DatabaseDriverFactory
