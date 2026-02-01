---
name: migration-helper
description: Use when evolving Room database schema, adding/modifying tables or columns, before changing @Database version number
---

# Room Migration Helper

## Overview

Generates validated Room database migrations following Android best practices. Ensures schema changes are safe, tested, and properly integrated.

## When to Use

- Adding new tables or columns to Room database
- Modifying existing schema (rename, type change, constraints)
- Before incrementing version in `@Database` annotation
- When you see "Migration path not found" errors

**When NOT to use:**
- First database creation (no migration needed)
- In-memory databases for testing
- Non-Room database changes

## Quick Reference

| Task | Command Pattern |
|------|-----------------|
| Add table | Generate migration → Add table SQL → Test |
| Add column | Generate migration → ALTER TABLE → Test |
| Rename | Generate AutoMigration with spec |
| Complex change | Manual migration + validation test |

## Implementation Steps

### 1. Enable Schema Export

First time only - configure Room to export schemas:

```kotlin
// data/build.gradle.kts
plugins {
    id("androidx.room")
}

room {
    schemaDirectory("$projectDir/schemas")
}

android {
    defaultConfig {
        javaCompileOptions {
            annotationProcessorOptions {
                arguments["room.schemaLocation"] = "$projectDir/schemas"
            }
        }
    }
}
```

### 2. Generate Migration Code

**For simple changes (add table/column):**

```kotlin
// data/src/main/kotlin/com/kairos/data/local/KairosDatabase.kt

val MIGRATION_1_2 = object : Migration(1, 2) {
    override fun migrate(database: SupportSQLiteDatabase) {
        // Add new table example
        database.execSQL("""
            CREATE TABLE IF NOT EXISTS habit_notes (
                id TEXT PRIMARY KEY NOT NULL,
                habit_id TEXT NOT NULL,
                content TEXT NOT NULL,
                created_at INTEGER NOT NULL,
                FOREIGN KEY(habit_id) REFERENCES habits(id) ON DELETE CASCADE
            )
        """.trimIndent())

        // Add index
        database.execSQL("CREATE INDEX index_habit_notes_habit_id ON habit_notes(habit_id)")
    }
}

// Register in database
@Database(
    entities = [/* ... */],
    version = 2, // Increment version
    exportSchema = true
)
abstract class KairosDatabase : RoomDatabase() {
    companion object {
        fun buildDatabase(context: Context): KairosDatabase {
            return Room.databaseBuilder(context, KairosDatabase::class.java, "kairos.db")
                .addMigrations(MIGRATION_1_2) // Add here
                .build()
        }
    }
}
```

**For complex changes (rename, type change):**

Use AutoMigration with spec:

```kotlin
@Database(
    version = 3,
    autoMigrations = [
        AutoMigration(from = 2, to = 3, spec = AutoMigration2to3::class)
    ]
)
abstract class KairosDatabase : RoomDatabase()

@RenameColumn(tableName = "habits", fromColumnName = "name", toColumnName = "title")
class AutoMigration2to3 : AutoMigrationSpec
```

### 3. Create Migration Test

**Always test migrations:**

```kotlin
// data/src/test/kotlin/com/kairos/data/local/MigrationTest.kt
@RunWith(AndroidJUnit4::class)
class MigrationTest {
    private val TEST_DB = "migration-test"

    @get:Rule
    val helper: MigrationTestHelper = MigrationTestHelper(
        InstrumentationRegistry.getInstrumentation(),
        KairosDatabase::class.java
    )

    @Test
    fun migrate1To2() {
        // Create database at version 1
        helper.createDatabase(TEST_DB, 1).apply {
            // Insert test data for version 1
            execSQL("""
                INSERT INTO habits (id, userId, name, description)
                VALUES ('test-id', 'user-1', 'Test Habit', 'Description')
            """)
            close()
        }

        // Migrate to version 2
        val db = helper.runMigrationsAndValidate(TEST_DB, 2, true, MIGRATION_1_2)

        // Verify migration
        db.query("SELECT * FROM habit_notes").use { cursor ->
            assertThat(cursor.count).isEqualTo(0) // New table exists and is empty
        }

        // Verify old data preserved
        db.query("SELECT * FROM habits WHERE id = 'test-id'").use { cursor ->
            assertThat(cursor.moveToFirst()).isTrue()
            assertThat(cursor.getString(cursor.getColumnIndex("name"))).isEqualTo("Test Habit")
        }
    }
}
```

### 4. Validation Checklist

Run before committing migration:

```bash
# Build to verify compilation
./gradlew :data:build

# Run migration tests
./gradlew :data:testDebugUnitTest --tests "*MigrationTest*"

# Verify schema exported
ls -la data/schemas/
# Should see: 1.json, 2.json (new version)
```

## Common Mistakes

| Mistake | Fix |
|---------|-----|
| Forgot to increment version | Update `@Database(version = X)` |
| Schema not exported | Add `room.schemaLocation` in build.gradle.kts |
| Migration not registered | Add to `.addMigrations()` in database builder |
| No test | Create MigrationTest with before/after validation |
| Wrong SQL syntax | Test with SQLite command line first |
| Missing NOT NULL | Migrations to non-null columns need DEFAULT value |

## Migration Patterns

### Add Column (Nullable)

```kotlin
database.execSQL("ALTER TABLE habits ADD COLUMN tags TEXT")
```

### Add Column (Non-Nullable)

```kotlin
// Must provide default for existing rows
database.execSQL("ALTER TABLE habits ADD COLUMN sort_order INTEGER NOT NULL DEFAULT 0")
```

### Add Foreign Key

```kotlin
// SQLite requires recreate for FK constraints
database.execSQL("""
    CREATE TABLE habits_new (
        id TEXT PRIMARY KEY NOT NULL,
        user_id TEXT NOT NULL,
        FOREIGN KEY(user_id) REFERENCES users(id) ON DELETE CASCADE
    )
""")
database.execSQL("INSERT INTO habits_new SELECT * FROM habits")
database.execSQL("DROP TABLE habits")
database.execSQL("ALTER TABLE habits_new RENAME TO habits")
```

### Add Index

```kotlin
database.execSQL("CREATE INDEX index_habits_user_id ON habits(user_id)")
```

## Testing Edge Cases

Always test:
1. **Empty database** - Fresh install migration path
2. **Populated database** - Data preservation
3. **Multi-step migration** - 1→2→3 path (not just 1→3)
4. **Downgrade** (if supported) - Fallback migrations

## Reference

- [Room Migration Guide](https://developer.android.com/training/data-storage/room/migrating-db-versions)
- [AutoMigration API](https://developer.android.com/reference/androidx/room/AutoMigration)
- SQLite ALTER TABLE: [SQLite Docs](https://www.sqlite.org/lang_altertable.html)
