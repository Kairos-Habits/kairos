# Entity-Relationship Diagrams

## Overview

This document provides comprehensive entity-relationship diagrams for both the local Room database and remote cloud schema.

---

## Complete ERD

```mermaid
erDiagram
    USER ||--|| USER_PREFERENCES : "has"
    USER ||--o{ HABIT : "owns"
    USER ||--o{ ROUTINE : "owns"
    
    HABIT ||--o{ COMPLETION : "tracks"
    HABIT ||--o{ RECOVERY_SESSION : "generates"
    HABIT ||--o{ HABIT_NOTIFICATION : "scheduled for"
    
    ROUTINE ||--o{ ROUTINE_HABIT : "contains"
    ROUTINE ||--o{ ROUTINE_VARIANT : "has variants"
    ROUTINE ||--o{ ROUTINE_EXECUTION : "executed as"
    
    ROUTINE_HABIT }o--|| HABIT : "references"
    ROUTINE_EXECUTION }o--o| ROUTINE_VARIANT : "uses"
    
    PENDING_CHANGE }o--|| HABIT : "references"
    PENDING_CHANGE }o--|| COMPLETION : "references"
    PENDING_CHANGE }o--|| ROUTINE : "references"

    USER {
        uuid id PK
        string email
        string display_name
        timestamp created_at
        timestamp last_login_at
    }
    
    USER_PREFERENCES {
        uuid id PK
        uuid user_id FK,UK
        boolean notification_enabled
        time default_reminder_time
        enum theme
        enum conflict_strategy
        boolean energy_tracking_enabled
        json notification_channels
        timestamp created_at
        timestamp updated_at
        enum sync_status
        bigint version
    }
    
    HABIT {
        uuid id PK
        uuid user_id FK
        string name
        string description
        string icon
        string color
        string anchor_behavior
        enum anchor_type
        time time_window_start
        time time_window_end
        enum category
        enum frequency
        json active_days
        int estimated_seconds
        string micro_version
        boolean allow_partial
        json subtasks
        json notification_config
        int lapse_threshold_days
        int relapse_threshold_days
        int allowed_skips_per_week
        enum phase
        enum status
        timestamp created_at
        timestamp updated_at
        timestamp paused_at
        timestamp archived_at
        enum sync_status
        bigint version
    }
    
    COMPLETION {
        uuid id PK
        uuid habit_id FK
        date date
        timestamp completed_at
        enum type
        int partial_percent
        enum skip_reason
        int energy_level
        string note
        timestamp created_at
        enum sync_status
        bigint version
    }
    
    ROUTINE {
        uuid id PK
        uuid user_id FK
        string name
        string description
        string icon
        string color
        enum category
        enum status
        timestamp created_at
        timestamp updated_at
        enum sync_status
        bigint version
    }
    
    ROUTINE_HABIT {
        uuid id PK
        uuid routine_id FK
        uuid habit_id FK
        int order_index
        int override_duration_seconds
        json variant_ids
        enum sync_status
        bigint version
    }
    
    ROUTINE_VARIANT {
        uuid id PK
        uuid routine_id FK
        string name
        int estimated_minutes
        boolean is_default
        enum sync_status
        bigint version
    }
    
    ROUTINE_EXECUTION {
        uuid id PK
        uuid routine_id FK
        uuid variant_id FK
        timestamp started_at
        timestamp completed_at
        enum status
        int current_step_index
        int current_step_remaining_seconds
        int total_paused_seconds
        json step_results
        enum sync_status
        bigint version
    }
    
    RECOVERY_SESSION {
        uuid id PK
        uuid habit_id FK
        enum type
        enum status
        timestamp triggered_at
        timestamp completed_at
        json blockers
        enum action
        string notes
        enum sync_status
        bigint version
    }
    
    HABIT_NOTIFICATION {
        uuid id PK
        uuid habit_id FK
        enum type
        time scheduled_time
        json days_of_week
        boolean enabled
        timestamp last_triggered_at
    }
    
    PENDING_CHANGE {
        uuid id PK
        enum entity_type
        uuid entity_id
        enum operation
        json payload
        timestamp created_at
        int attempts
        timestamp last_attempt_at
        string last_error
    }
```

---

## Local Database Schema (Room)

### Core Tables

```mermaid
erDiagram
    habits {
        TEXT id PK "UUID"
        TEXT user_id FK "UUID, nullable for local-only"
        TEXT name "NOT NULL"
        TEXT description
        TEXT icon
        TEXT color
        TEXT anchor_behavior "NOT NULL"
        TEXT anchor_type "NOT NULL, enum"
        TEXT time_window_start "HH:mm format"
        TEXT time_window_end "HH:mm format"
        TEXT category "NOT NULL, enum"
        TEXT frequency "NOT NULL, enum"
        TEXT active_days "JSON array"
        INTEGER estimated_seconds "DEFAULT 300"
        TEXT micro_version
        INTEGER allow_partial "BOOLEAN, DEFAULT 1"
        TEXT subtasks "JSON array"
        TEXT notification_config "JSON object"
        INTEGER lapse_threshold_days "DEFAULT 3"
        INTEGER relapse_threshold_days "DEFAULT 7"
        INTEGER allowed_skips_per_week "DEFAULT 2"
        TEXT phase "NOT NULL, enum"
        TEXT status "NOT NULL, enum"
        INTEGER created_at "Unix timestamp"
        INTEGER updated_at "Unix timestamp"
        INTEGER paused_at "Unix timestamp, nullable"
        INTEGER archived_at "Unix timestamp, nullable"
        TEXT sync_status "enum"
        INTEGER version "DEFAULT 1"
    }
    
    completions {
        TEXT id PK "UUID"
        TEXT habit_id FK "NOT NULL"
        TEXT date "DATE format YYYY-MM-DD"
        INTEGER completed_at "Unix timestamp"
        TEXT type "NOT NULL, enum"
        INTEGER partial_percent "1-99, nullable"
        TEXT skip_reason "enum, nullable"
        INTEGER energy_level "1-5, nullable"
        TEXT note
        INTEGER created_at "Unix timestamp"
        TEXT sync_status "enum"
        INTEGER version "DEFAULT 1"
    }
    
    habits ||--o{ completions : "habit_id"
```

### Routine Tables

```mermaid
erDiagram
    routines {
        TEXT id PK "UUID"
        TEXT user_id FK "UUID, nullable"
        TEXT name "NOT NULL"
        TEXT description
        TEXT icon
        TEXT color
        TEXT category "NOT NULL, enum"
        TEXT status "NOT NULL, enum"
        INTEGER created_at "Unix timestamp"
        INTEGER updated_at "Unix timestamp"
        TEXT sync_status "enum"
        INTEGER version "DEFAULT 1"
    }
    
    routine_habits {
        TEXT id PK "UUID"
        TEXT routine_id FK "NOT NULL"
        TEXT habit_id FK "NOT NULL"
        INTEGER order_index "NOT NULL"
        INTEGER override_duration_seconds
        TEXT variant_ids "JSON array"
        TEXT sync_status "enum"
        INTEGER version "DEFAULT 1"
    }
    
    routine_variants {
        TEXT id PK "UUID"
        TEXT routine_id FK "NOT NULL"
        TEXT name "NOT NULL"
        INTEGER estimated_minutes "NOT NULL"
        INTEGER is_default "BOOLEAN, DEFAULT 0"
        TEXT sync_status "enum"
        INTEGER version "DEFAULT 1"
    }
    
    routine_executions {
        TEXT id PK "UUID"
        TEXT routine_id FK "NOT NULL"
        TEXT variant_id FK
        INTEGER started_at "Unix timestamp, NOT NULL"
        INTEGER completed_at "Unix timestamp"
        TEXT status "NOT NULL, enum"
        INTEGER current_step_index "DEFAULT 0"
        INTEGER current_step_remaining_seconds
        INTEGER total_paused_seconds "DEFAULT 0"
        TEXT step_results "JSON array"
        TEXT sync_status "enum"
        INTEGER version "DEFAULT 1"
    }
    
    routines ||--o{ routine_habits : "routine_id"
    routines ||--o{ routine_variants : "routine_id"
    routines ||--o{ routine_executions : "routine_id"
    routine_habits }o--|| habits : "habit_id"
    routine_executions }o--o| routine_variants : "variant_id"
```

### Recovery Tables

```mermaid
erDiagram
    recovery_sessions {
        TEXT id PK "UUID"
        TEXT habit_id FK "NOT NULL"
        TEXT type "NOT NULL, enum"
        TEXT status "NOT NULL, enum"
        INTEGER triggered_at "Unix timestamp, NOT NULL"
        INTEGER completed_at "Unix timestamp"
        TEXT blockers "JSON array"
        TEXT action "enum"
        TEXT notes
        TEXT sync_status "enum"
        INTEGER version "DEFAULT 1"
    }
    
    habits ||--o{ recovery_sessions : "habit_id"
```

### Notification Tables

```mermaid
erDiagram
    habit_notifications {
        TEXT id PK "UUID"
        TEXT habit_id FK "NOT NULL"
        TEXT type "NOT NULL, enum"
        TEXT scheduled_time "HH:mm format"
        TEXT days_of_week "JSON array"
        INTEGER enabled "BOOLEAN, DEFAULT 1"
        INTEGER last_triggered_at "Unix timestamp"
    }
    
    habits ||--o{ habit_notifications : "habit_id"
```

### Sync Tables

```mermaid
erDiagram
    pending_changes {
        TEXT id PK "UUID"
        TEXT entity_type "NOT NULL, enum"
        TEXT entity_id "NOT NULL"
        TEXT operation "NOT NULL, enum"
        TEXT payload "JSON, NOT NULL"
        INTEGER created_at "Unix timestamp, NOT NULL"
        INTEGER attempts "DEFAULT 0"
        INTEGER last_attempt_at "Unix timestamp"
        TEXT last_error
    }
    
    sync_metadata {
        TEXT id PK "singleton key"
        INTEGER last_sync_at "Unix timestamp"
        TEXT last_sync_token
        INTEGER pending_count
    }
```

### Preferences Table

```mermaid
erDiagram
    user_preferences {
        TEXT id PK "UUID"
        TEXT user_id "nullable"
        INTEGER notification_enabled "BOOLEAN, DEFAULT 1"
        TEXT default_reminder_time "HH:mm format"
        TEXT theme "enum, DEFAULT SYSTEM"
        TEXT conflict_strategy "enum, DEFAULT LAST_WRITE_WINS"
        INTEGER energy_tracking_enabled "BOOLEAN, DEFAULT 0"
        TEXT notification_channels "JSON object"
        INTEGER created_at "Unix timestamp"
        INTEGER updated_at "Unix timestamp"
        TEXT sync_status "enum"
        INTEGER version "DEFAULT 1"
    }
```

### Household Tables (v1.1)

> **Note**: These tables are created in v1.0 but unused until v1.1 when shared habits are implemented.

```mermaid
erDiagram
    households {
        TEXT id PK "UUID"
        TEXT name "NOT NULL"
        TEXT created_by FK "User who created"
        INTEGER created_at "Unix timestamp"
        INTEGER updated_at "Unix timestamp"
        TEXT sync_status "enum"
        INTEGER version "DEFAULT 1"
    }
    
    household_members {
        TEXT id PK "UUID"
        TEXT household_id FK "NOT NULL"
        TEXT user_id FK "NOT NULL"
        TEXT role "NOT NULL, enum: admin, member"
        INTEGER joined_at "Unix timestamp"
        TEXT sync_status "enum"
        INTEGER version "DEFAULT 1"
    }
    
    households ||--o{ household_members : "household_id"
```

#### Household Completion Mode (v1.1)

When shared habits are implemented, habits will support these completion modes:

| Mode | Description | Example |
|------|-------------|---------|
| `ANYONE_ONCE` | First person to complete, done for all | Feed the dog |
| `EVERYONE` | Each member must complete individually | Take vitamins |
| `ASSIGNED` | Specific member(s) responsible | Kid's medication |
| `ROTATING` | Auto-assigns, rotates through members | Take out trash |

#### Extended Habit Schema (v1.1)

```sql
-- These columns exist but are NULL in v1.0
ALTER TABLE habits ADD COLUMN household_id TEXT REFERENCES households(id);
ALTER TABLE habits ADD COLUMN completion_mode TEXT;

-- Constraint: habit belongs to user OR household, not both
-- CHECK (user_id IS NOT NULL OR household_id IS NOT NULL)
-- CHECK (NOT (user_id IS NOT NULL AND household_id IS NOT NULL))
```

#### Shared Completion Tracking (v1.1)

For shared habits, completions need to track who completed:

```sql
-- completions table already has this, just used differently for shared habits
-- For personal habits: user who owns the habit (implicit)
-- For shared habits: user who performed the completion (explicit)
-- The completed_by field will be populated for shared habit completions
```

---

## Indices

### Habit Indices

```sql
-- Primary queries: Today's habits
CREATE INDEX idx_habits_status_category 
    ON habits(status, category);

-- Lapse detection query
CREATE INDEX idx_habits_phase 
    ON habits(phase);

-- User lookup (for sync)
CREATE INDEX idx_habits_user_id 
    ON habits(user_id);

-- Sync status query
CREATE INDEX idx_habits_sync_status 
    ON habits(sync_status);
```

### Completion Indices

```sql
-- Primary query: Completions for a habit on a date range
CREATE INDEX idx_completions_habit_date 
    ON completions(habit_id, date);

-- Today's completions
CREATE INDEX idx_completions_date 
    ON completions(date);

-- Sync status query
CREATE INDEX idx_completions_sync_status 
    ON completions(sync_status);

-- Unique constraint: One completion per habit per day
CREATE UNIQUE INDEX idx_completions_habit_date_unique 
    ON completions(habit_id, date);
```

### Routine Indices

```sql
-- Routine habit ordering
CREATE UNIQUE INDEX idx_routine_habits_order 
    ON routine_habits(routine_id, order_index);

-- Active executions
CREATE INDEX idx_routine_executions_status 
    ON routine_executions(status);
```

### Pending Change Indices

```sql
-- Queue processing order
CREATE INDEX idx_pending_changes_created 
    ON pending_changes(created_at ASC);

-- Retry lookup
CREATE INDEX idx_pending_changes_attempts 
    ON pending_changes(attempts, last_attempt_at);
```

---

## Remote Schema (Firestore)

### Collection Structure

```mermaid
flowchart TB
    subgraph Firestore["Firestore Collections"]
        Users["users/{userId}"]
        Habits["users/{userId}/habits/{habitId}"]
        Completions["users/{userId}/completions/{completionId}"]
        Routines["users/{userId}/routines/{routineId}"]
        RoutineHabits["users/{userId}/routines/{routineId}/habits/{id}"]
        Variants["users/{userId}/routines/{routineId}/variants/{id}"]
        Executions["users/{userId}/routine_executions/{id}"]
        RecoverySessions["users/{userId}/recovery_sessions/{id}"]
        Preferences["users/{userId}/preferences/{id}"]
        Deletions["users/{userId}/deletions/{id}"]
    end
    
    Users --> Habits
    Users --> Completions
    Users --> Routines
    Routines --> RoutineHabits
    Routines --> Variants
    Users --> Executions
    Users --> RecoverySessions
    Users --> Preferences
    Users --> Deletions
```

### Document Schemas

#### User Document

```typescript
// users/{userId}
interface UserDocument {
  email: string;
  displayName: string | null;
  createdAt: Timestamp;
  lastLoginAt: Timestamp;
  deviceIds: string[];  // For multi-device tracking
}
```

#### Habit Document

```typescript
// users/{userId}/habits/{habitId}
interface HabitDocument {
  id: string;
  name: string;
  description: string | null;
  icon: string | null;
  color: string | null;
  anchorBehavior: string;
  anchorType: 'AFTER_BEHAVIOR' | 'BEFORE_BEHAVIOR' | 'AT_LOCATION' | 'AT_TIME';
  timeWindow: {
    start: string;  // HH:mm
    end: string;
  } | null;
  category: 'MORNING' | 'AFTERNOON' | 'EVENING' | 'ANYTIME';
  frequency: 'DAILY' | 'WEEKDAYS' | 'WEEKENDS' | 'CUSTOM';
  activeDays: string[] | null;  // ['MONDAY', 'WEDNESDAY', ...]
  estimatedSeconds: number;
  microVersion: string | null;
  allowPartial: boolean;
  subtasks: string[];
  notificationConfig: object | null;
  lapseThresholdDays: number;
  relapseThresholdDays: number;
  allowedSkipsPerWeek: number;
  phase: 'ONBOARD' | 'FORMING' | 'MAINTAINING' | 'LAPSED' | 'RELAPSED';
  status: 'ACTIVE' | 'PAUSED' | 'ARCHIVED';
  createdAt: Timestamp;
  updatedAt: Timestamp;
  pausedAt: Timestamp | null;
  archivedAt: Timestamp | null;
  version: number;
}
```

#### Completion Document

```typescript
// users/{userId}/completions/{completionId}
interface CompletionDocument {
  id: string;
  habitId: string;
  date: string;  // YYYY-MM-DD
  completedAt: Timestamp;
  type: 'FULL' | 'PARTIAL' | 'SKIPPED' | 'MISSED';
  partialPercent: number | null;
  skipReason: string | null;
  energyLevel: number | null;
  note: string | null;
  createdAt: Timestamp;
  version: number;
}
```

#### Deletion Log Document

```typescript
// users/{userId}/deletions/{deletionId}
interface DeletionDocument {
  id: string;
  entityType: 'HABIT' | 'COMPLETION' | 'ROUTINE' | 'ROUTINE_HABIT' | 'RECOVERY_SESSION';
  entityId: string;
  deletedAt: Timestamp;
}
```

### Firestore Security Rules

```javascript
rules_version = '2';
service cloud.firestore {
  match /databases/{database}/documents {
    // User can only access their own data
    match /users/{userId} {
      allow read, write: if request.auth != null && request.auth.uid == userId;
      
      // All subcollections inherit user access
      match /{document=**} {
        allow read, write: if request.auth != null && request.auth.uid == userId;
      }
    }
  }
}
```

---

## Data Type Mappings

### Local to Remote Type Mapping

| Local (Room/SQLite) | Remote (Firestore) | Notes |
|---------------------|-------------------|-------|
| TEXT (UUID) | string | Same format |
| INTEGER (timestamp) | Timestamp | Convert Unix → Firestore |
| TEXT (date) | string | Keep YYYY-MM-DD format |
| TEXT (time) | string | Keep HH:mm format |
| TEXT (enum) | string | Uppercase enum name |
| TEXT (JSON array) | array | Parse/stringify |
| TEXT (JSON object) | map | Parse/stringify |
| INTEGER (boolean) | boolean | 0/1 → false/true |

### Sync Status Mapping

| Local Status | Cloud Behavior |
|--------------|----------------|
| LOCAL_ONLY | Not synced |
| SYNCED | Matches remote |
| PENDING_SYNC | Queued for push |
| PENDING_DELETE | Queued for deletion |
| CONFLICT | Requires resolution |

---

## Migration Strategy

### Version Tracking

```kotlin
@Database(
    entities = [
        HabitEntity::class,
        CompletionEntity::class,
        RoutineEntity::class,
        RoutineHabitEntity::class,
        RoutineVariantEntity::class,
        RoutineExecutionEntity::class,
        RecoverySessionEntity::class,
        HabitNotificationEntity::class,
        PendingChangeEntity::class,
        SyncMetadataEntity::class,
        UserPreferencesEntity::class
    ],
    version = 1,
    exportSchema = true
)
abstract class KairosDatabase : RoomDatabase() {
    // DAOs...
}
```

### Migration Example

```kotlin
val MIGRATION_1_2 = object : Migration(1, 2) {
    override fun migrate(database: SupportSQLiteDatabase) {
        // Add new column
        database.execSQL(
            "ALTER TABLE habits ADD COLUMN new_field TEXT DEFAULT NULL"
        )
        
        // Create new index
        database.execSQL(
            "CREATE INDEX idx_habits_new_field ON habits(new_field)"
        )
    }
}
```

---

## Query Examples

### Get Today's Habits with Status

```sql
SELECT 
    h.*,
    c.id as completion_id,
    c.type as completion_type,
    c.partial_percent
FROM habits h
LEFT JOIN completions c ON h.id = c.habit_id AND c.date = :today
WHERE h.status = 'ACTIVE'
    AND (
        h.frequency = 'DAILY'
        OR (h.frequency = 'WEEKDAYS' AND :dayOfWeek NOT IN ('SATURDAY', 'SUNDAY'))
        OR (h.frequency = 'WEEKENDS' AND :dayOfWeek IN ('SATURDAY', 'SUNDAY'))
        OR (h.frequency = 'CUSTOM' AND h.active_days LIKE '%' || :dayOfWeek || '%')
    )
ORDER BY 
    CASE h.category 
        WHEN 'MORNING' THEN 1 
        WHEN 'AFTERNOON' THEN 2 
        WHEN 'EVENING' THEN 3 
        ELSE 4 
    END,
    h.created_at ASC
```

### Get Completion Rate for Week

```sql
SELECT 
    h.id,
    h.name,
    COUNT(CASE WHEN c.type IN ('FULL', 'PARTIAL') THEN 1 END) as completed_count,
    COUNT(CASE WHEN c.type = 'SKIPPED' THEN 1 END) as skipped_count,
    7 as total_days
FROM habits h
LEFT JOIN completions c ON h.id = c.habit_id 
    AND c.date BETWEEN :weekStart AND :weekEnd
WHERE h.status = 'ACTIVE'
GROUP BY h.id
```

### Detect Lapsed Habits

```sql
SELECT h.*
FROM habits h
WHERE h.status = 'ACTIVE'
    AND h.phase NOT IN ('LAPSED', 'RELAPSED')
    AND NOT EXISTS (
        SELECT 1 FROM completions c 
        WHERE c.habit_id = h.id 
            AND c.type IN ('FULL', 'PARTIAL', 'SKIPPED')
            AND c.date >= date(:today, '-' || h.lapse_threshold_days || ' days')
    )
```

### Get Pending Sync Changes

```sql
SELECT * FROM pending_changes
WHERE attempts < 10
ORDER BY created_at ASC
LIMIT 50
```
