# Kairos Implementation Plan

**Last updated:** 2026-01-31

Phased roadmap for implementing Kairos. Follow phases sequentially—each phase builds on the previous.

---

## Module Structure

```
kairos/
├── app/                    # Phone app
│   └── src/main/kotlin/com/kairos/
│       ├── MainActivity.kt
│       ├── KairosApp.kt
│       ├── di/             # Koin modules
│       ├── ui/
│       │   ├── theme/
│       │   ├── components/
│       │   └── feature/
│       │       ├── today/
│       │       ├── habits/
│       │       ├── routines/
│       │       └── settings/
│       └── navigation/
│
├── wear/                   # Watch app
│   └── src/main/kotlin/com/kairos/wear/
│       ├── WearActivity.kt
│       ├── tile/
│       └── ui/
│
└── shared/                 # Shared code (Android library)
    └── src/main/kotlin/com/kairos/shared/
        ├── model/          # Domain objects
        ├── db/             # Room database, DAOs, entities
        ├── repository/     # Data access
        ├── sync/           # PowerSync (when ready)
        └── worker/         # Background jobs
```

| Module | Purpose                                                           |
| ------ | ----------------------------------------------------------------- |
| app    | Phone UI, ViewModels, navigation, theme, components               |
| wear   | Watch UI, tiles, complications                                    |
| shared | Models, database, repositories, workers—everything both apps need |

**Dependency flow:** `app` → `shared` ← `wear`

> **Ref:** [07-architecture.md](./docs/design/07-architecture.md) for high-level system design

---

## Guiding Principles

1. **No premature abstraction** — Add interfaces when you have 2+ implementations, not before
2. **No ceremony** — No UseCase classes that just delegate. Logic lives in repositories or ViewModels
3. **Flat over nested** — Avoid deep folder hierarchies
4. **Add complexity to solve problems** — Not to prevent hypothetical ones

---

## Phase 0: Project Setup

**Goal:** Working multi-module project with current tooling.

### 0.1 Create Project

Use Android Studio's wizard:

- New Project → No Activity
- Set package to `com.kairos`
- Let it generate with current Gradle/AGP/Kotlin versions

### 0.2 Add Modules

- Create `shared` module (Android Library)
- Create `wear` module (Wear OS Application)
- Set up dependencies in `settings.gradle.kts`

### 0.3 Add Core Dependencies

Add to version catalog:

- Compose BOM (current)
- Room
- Koin (not Hilt—simpler, faster builds)
- Kotlin Coroutines
- Kotlin Serialization
- Timber
- Wear Compose + Tiles + Horologist

### 0.4 Basic Koin Setup

Create `di/AppModule.kt` in `app` with empty module. Initialize in Application class.

### Outputs

- Project builds successfully
- All three modules compile
- Can run empty app on device

---

## Phase 1: Domain Models

**Goal:** Define all data structures.  
**Ref:** [05-domain-model.md](./docs/design/05-domain-model.md), [08-erd.md](./docs/design/08-erd.md)

### 1.1 Core Models

Location: `shared/.../model/`

| Model            | Key Fields                                          | Reference                                                            |
| ---------------- | --------------------------------------------------- | -------------------------------------------------------------------- |
| Habit            | id, name, anchorBehavior, category, phase, status   | [05-domain-model.md §Habit](./docs/design/05-domain-model.md)        |
| Completion       | id, habitId, date, type, partialPercent, skipReason | [05-domain-model.md §Completion](./docs/design/05-domain-model.md)   |
| Routine          | id, name, category, habits (ordered)                | [03-prd-routines.md §Domain Model](./docs/design/03-prd-routines.md) |
| RoutineExecution | id, routineId, status, currentStep, stepResults     | [03-prd-routines.md §Execution](./docs/design/03-prd-routines.md)    |
| RecoverySession  | id, habitId, type, status, offeredAt                | [02-prd-recovery.md §Data Model](./docs/design/02-prd-recovery.md)   |

### 1.2 Enums

Define in `shared/.../model/`:

- HabitPhase: ONBOARD, FORMING, MAINTAINING, LAPSED, RELAPSED
- HabitStatus: ACTIVE, PAUSED, ARCHIVED
- HabitCategory: MORNING, AFTERNOON, EVENING, ANYTIME
- CompletionType: FULL, PARTIAL, SKIPPED, MISSED
- SkipReason: TOO_TIRED, NO_TIME, TRAVELING, SICK, NEEDED_BREAK, OTHER

### 1.3 Validation

Put validation in model constructors:

- Habit name 1-100 chars
- Lapse threshold < relapse threshold
- Partial completion 1-99%

**Ref:** [06-invariants.md](./docs/design/06-invariants.md)

### Outputs

- All models in `shared/.../model/`
- Models are data classes with validation in `init` blocks

---

## Phase 2: Database

**Goal:** Room database with all tables.  
**Ref:** [08-erd.md](./docs/design/08-erd.md)

### 2.1 Entities

Location: `shared/.../db/entity/`

Create Room entities matching schema from [08-erd.md](./docs/design/08-erd.md):

- HabitEntity (include nullable v1.1 fields: householdId, completionMode)
- CompletionEntity
- RoutineEntity, RoutineHabitEntity
- RoutineExecutionEntity
- RecoverySessionEntity

### 2.2 Type Converters

Location: `shared/.../db/Converters.kt`

Single class with converters for:

- Instant ↔ Long
- LocalDate ↔ String
- LocalTime ↔ String
- Set<Int> ↔ String (JSON)
- List<String> ↔ String (JSON)
- Enums ↔ String

### 2.3 DAOs

Location: `shared/.../db/dao/`

One DAO per entity with queries from [08-erd.md §Common Queries](./docs/design/08-erd.md):

- HabitDao: getActive(), getByCategory(), getById()
- CompletionDao: getForDate(), getInRange()
- RoutineDao: getActive(), getWithHabits()
- RecoveryDao: getActiveSession()

### 2.4 Database

Location: `shared/.../db/KairosDatabase.kt`

- Define all entities
- Include type converters
- Use `.fallbackToDestructiveMigration()` during development

### 2.5 Entity ↔ Model Mapping

Add extension functions in each entity file:

```kotlin
fun HabitEntity.toModel(): Habit = ...
fun Habit.toEntity(): HabitEntity = ...
```

No separate mapper classes.

### Outputs

- `KairosDatabase.kt`
- Entity classes with mapping extensions
- DAO interfaces
- Converters

---

## Phase 3: Repositories

**Goal:** Data access layer.

### 3.1 Repositories

Location: `shared/.../repository/`

Create repository classes (not interfaces):

- HabitRepository
- CompletionRepository
- RoutineRepository
- RecoveryRepository

Each repository:

- Takes DAO in constructor
- Returns Flow for queries
- Has suspend functions for mutations
- Handles entity ↔ model mapping internally

### 3.2 Koin Module

Location: `shared/.../di/SharedModule.kt`

Provide:

- KairosDatabase (singleton)
- All DAOs (from database)
- All repositories

### Outputs

- Repository classes in `shared/.../repository/`
- `SharedModule.kt` with all providers

---

## Phase 4: Theme & Components

**Goal:** Design system foundation.  
**Ref:** [00-project-overview.md](./docs/design/00-project-overview.md) for ADHD-friendly principles

### 4.1 Theme

Location: `app/.../ui/theme/`

Create KairosTheme with:

- Colors: Sage green primary (`#5B7B6F`), calming palette
- Typography: System fonts, clear hierarchy
- Spacing: 4dp scale (4, 8, 12, 16, 24, 32)
- Shapes: Rounded corners (8dp, 16dp)

### 4.2 Core Components

Location: `app/.../ui/components/`

Build only what you need:

- HabitCard (completion button, name, anchor, menu)
- ProgressHeader (X of Y completed)
- EmptyState (illustration, message, action)
- KairosButton (primary, secondary variants)

Keep components minimal. Add more as needed.

### 4.3 App Shell

Location: `app/.../navigation/`

- Bottom navigation: Today, Habits, Routines, Settings
- NavHost with routes
- Scaffold with bottom bar

### Outputs

- Theme in `app/.../ui/theme/`
- Components in `app/.../ui/components/`
- Navigation shell

---

## Phase 5: Today Screen

**Goal:** First working feature end-to-end.  
**Ref:** [10-user-flows.md §Today Screen](./docs/design/10-user-flows.md)

### 5.1 TodayViewModel

Location: `app/.../ui/feature/today/`

- Inject HabitRepository, CompletionRepository
- Expose Flow of habits with today's completion status
- Group by category
- Calculate progress count
- Functions: complete(), skip(), undo()

### 5.2 TodayScreen

- Progress header
- Habit list grouped by category
- HabitCard with tap-to-complete
- Swipe actions (complete right, skip left)
- Pull-to-refresh
- Empty state when no habits

### 5.3 Koin Wiring

Register TodayViewModel in AppModule.

### Verification

- Habits display grouped by category
- Tap completes habit
- Swipe works
- Progress updates
- Persists across app restart

---

## Phase 6: Habit CRUD

**Goal:** Create, edit, archive habits.  
**Ref:** [01-prd-core.md §Habit Management](./docs/design/01-prd-core.md), [10-user-flows.md §Habit Creation](./docs/design/10-user-flows.md)

### 6.1 Create Habit

Multi-step flow:

1. Name + anchor behavior
2. Category + frequency + time window
3. Options (duration, notifications, micro version)
4. Review + save

### 6.2 Habit Detail

- All properties displayed
- Completion history (simple list, calendar later)
- Phase indicator
- Edit / Archive / Pause actions

### 6.3 Habits List

- All habits grouped by status
- Search
- Quick pause/resume

### 6.4 Edit Habit

- Same form as create, pre-populated
- Discard confirmation if unsaved changes

### Verification

- Full create flow works
- Edit persists changes
- Archive removes from Today
- Pause/resume works

---

## Phase 7: Recovery System

**Goal:** Compassionate handling of missed habits.  
**Ref:** [02-prd-recovery.md](./docs/design/02-prd-recovery.md), [09-state-machines.md §Recovery](./docs/design/09-state-machines.md)

### 7.1 Lapse Detection Worker

Location: `shared/.../worker/LapseCheckWorker.kt`

- Schedule daily via WorkManager
- Query habits exceeding lapse threshold
- Transition to LAPSED phase
- Create RecoverySession

### 7.2 Recovery Prompt

When app opens with pending recovery:

- Show bottom sheet (not blocking)
- Gentle language per [02-prd-recovery.md §Messaging](./docs/design/02-prd-recovery.md)
- Options: Try again, Micro version, Pause, Adjust schedule

### 7.3 Recovery Actions

Implement each:

- Try again → Reset to ONBOARD
- Micro version → Set microVersion field
- Pause → Set PAUSED with return date
- Adjust → Open schedule editor

### 7.4 Relapse Handling

After extended absence:

- Offer Fresh Start
- Reset to new habit, preserve history

### Verification

- Lapse detected correctly
- Prompt appears on app open
- Each action works
- Phase transitions correct

---

## Phase 8: Routines

**Goal:** Grouped habit execution.  
**Ref:** [03-prd-routines.md](./docs/design/03-prd-routines.md), [09-state-machines.md §Routine Execution](./docs/design/09-state-machines.md)

### 8.1 Routine CRUD

- Create with name, category, habits
- Drag-and-drop reorder
- Duration overrides per habit

### 8.2 Routine Runner

Full-screen UI:

- Current step with timer
- Progress (step X of Y)
- Controls: Complete, Skip, Pause, Exit
- Audio cue on auto-advance (optional)

### 8.3 Execution State

Track in RoutineExecution:

- Current step index
- Step results array
- Pause time accumulator
- Final status (COMPLETED, ABANDONED)

### 8.4 Completions from Routine

When routine finishes:

- Create Completion for each completed step
- Handle partial (some skipped)

### Verification

- Create and edit routines
- Runner advances correctly
- Timer works with pause
- Completions recorded

---

## Phase 9: Notifications

**Goal:** Context-aware reminders.  
**Ref:** [11-notification-design.md](./docs/design/11-notification-design.md)

### 9.1 Channels

Create per [11-notification-design.md §Channels](./docs/design/11-notification-design.md):

- habit_reminders (default)
- recovery (low)
- routine_timer (high, no sound)

### 9.2 Scheduling

- Use AlarmManager for exact timing
- Schedule based on time windows
- Cancel when completed
- Reschedule on habit update

### 9.3 Content

- Anchor-based: "After [anchor], time for [habit]"
- No guilt language
- Actions: Complete, Skip, Snooze

### 9.4 Action Handling

- Complete → record, dismiss
- Skip → quick skip or reason picker
- Snooze → reschedule 15/30/60 min

### 9.5 Boot Receiver

Reschedule all on device boot.

### Verification

- Notifications at correct times
- Actions work
- Quiet hours respected

---

## Phase 10: Settings

**Goal:** User preferences.

### 10.1 Settings Screen

Sections:

- Notifications (toggle, quiet hours)
- Appearance (theme)
- Data (export, clear)
- About (version, licenses)

### 10.2 Preferences

Use DataStore for:

- Theme preference
- Quiet hours
- First day of week
- Notification defaults

### 10.3 Export/Import

- Export all data as JSON
- Import with basic validation

### Verification

- Settings persist
- Theme changes immediately
- Export produces valid file

---

## Phase 11: Sync

**Goal:** Multi-device sync.  
**Ref:** [04-prd-sync.md](./docs/design/04-prd-sync.md)

### 11.1 Server Setup UI

- Server URL input
- Health check validation
- Store in encrypted prefs

### 11.2 Auth

Location: `shared/.../sync/`

- PocketBase login/register
- Token storage (Android Keystore)
- Auto refresh

### 11.3 PowerSync Integration

- Add SDK dependency
- Configure with PocketBase JWT
- Define sync rules per [04-prd-sync.md §Sync Rules](./docs/design/04-prd-sync.md)

### 11.4 Offline Handling

- Queue changes locally
- Sync when online
- Last-write-wins conflicts

### 11.5 UI

- Sync status indicator
- Manual sync button
- Settings screen details

### Verification

- Server configuration works
- Login/logout works
- Data syncs between devices
- Offline changes sync

---

## Phase 12: WearOS

**Goal:** Quick tracking from watch.  
**Ref:** [12-wearos-design.md](./docs/design/12-wearos-design.md)

### 12.1 Tile

Per [12-wearos-design.md §Tile Design](./docs/design/12-wearos-design.md):

- Progress (X/Y)
- Next pending habits
- Tap to complete

### 12.2 App

Simple screens:

- Pending habit list
- Tap to complete
- Swipe to skip

### 12.3 Data Sync

- DataClient for phone ↔ watch
- Send completions from watch
- Receive updates from phone

### 12.4 Complications

- SHORT_TEXT: "3/5"
- RANGED_VALUE: Progress arc

### Verification

- Tile shows correct data
- Can complete from watch
- Syncs with phone

---

## Phase 13: ESP32 Integration (v1.x)

**Goal:** Physical device tracking.  
**Ref:** [13-embedded-integration.md](./docs/design/13-embedded-integration.md)

> Requires kairos-server deployment

### 13.1 MQTT Bridge

Server component:

- Subscribe to device topics
- Validate tokens
- Write to database

### 13.2 Device Registration

- PocketBase admin UI
- Token generation
- Habit assignment

### 13.3 App Integration

- Device status in settings
- Troubleshooting info

### Verification

- Button creates completion
- Light reflects status

---

## Phase 14: Shared Habits (v1.1)

**Goal:** Household sharing.  
**Ref:** [14-shared-habits.md](./docs/design/14-shared-habits.md)

### 14.1 Household Management

- Create household
- Invite members
- Roles (admin/member)

### 14.2 Shared Habits

- "Share with household" toggle
- Completion mode picker
- Attribution display

### 14.3 Sync Rules Update

- Filter by household_id
- Sync member data

### Verification

- Create/join household
- Shared habits visible
- Attribution correct

---

## Phase 15: Polish & Testing

### 15.1 Testing

- Unit tests for repositories
- ViewModel tests
- UI tests for critical flows

### 15.2 Performance

- Startup optimization
- Database query profiling

### 15.3 Accessibility

- Content descriptions
- 48dp touch targets
- Screen reader testing

### 15.4 Documentation

- README setup instructions
- Onboarding flow

---

## Version Planning

| Version | Phases | Features                      |
| ------- | ------ | ----------------------------- |
| 0.1.0   | 0-5    | Today screen, complete habits |
| 0.2.0   | 6-7    | Habit CRUD, recovery          |
| 0.3.0   | 8-9    | Routines, notifications       |
| 0.4.0   | 10-11  | Settings, sync                |
| 0.5.0   | 12     | WearOS                        |
| 1.0.0   | 13, 15 | ESP32, polish                 |
| 1.1.0   | 14     | Shared habits                 |

---

## When to Add Complexity

| Situation                        | Action                                |
| -------------------------------- | ------------------------------------- |
| Need to mock repository in tests | Extract interface for that repository |
| ViewModel exceeds ~200 lines     | Extract helper functions              |
| Same logic in multiple places    | Move to repository                    |
| Adding iOS                       | Extract models to KMP module          |
| Second sync backend              | Create sync interface                 |

---

## References

### PRDs

- [01-prd-core.md](./docs/design/01-prd-core.md) — Core habit tracking
- [02-prd-recovery.md](./docs/design/02-prd-recovery.md) — Lapse/relapse handling
- [03-prd-routines.md](./docs/design/03-prd-routines.md) — Grouped execution
- [04-prd-sync.md](./docs/design/04-prd-sync.md) — Cloud sync

### Technical

- [05-domain-model.md](./docs/design/05-domain-model.md) — Entity definitions
- [06-invariants.md](./docs/design/06-invariants.md) — Business rules
- [07-architecture.md](./docs/design/07-architecture.md) — System architecture
- [08-erd.md](./docs/design/08-erd.md) — Database schema
- [09-state-machines.md](./docs/design/09-state-machines.md) — State transitions

### UX

- [10-user-flows.md](./docs/design/10-user-flows.md) — Screens and interactions
- [11-notification-design.md](./docs/design/11-notification-design.md) — Notifications
- [12-wearos-design.md](./docs/design/12-wearos-design.md) — Watch design

### Future

- [13-embedded-integration.md](./docs/design/13-embedded-integration.md) — ESP32/MQTT
- [14-shared-habits.md](./docs/design/14-shared-habits.md) — Household sharing
