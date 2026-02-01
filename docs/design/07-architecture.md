# System Architecture

## Overview

Kairos follows a **Clean Architecture** pattern with clear separation between layers. The system is designed for offline-first operation with optional cloud synchronization.

---

## High-Level Architecture

```mermaid
flowchart TB
    subgraph Clients["Client Applications"]
        Android["📱 Android App<br/>Jetpack Compose"]
        WearOS["⌚ WearOS App<br/>Tiles + Compose"]
        Widget["🔲 Home Widget<br/>Glance"]
    end
    
    subgraph AppLayers["Application Layers"]
        subgraph Presentation["Presentation Layer"]
            Screens["Screens"]
            ViewModels["ViewModels"]
            Components["UI Components"]
        end
        
        subgraph Domain["Domain Layer"]
            UseCases["Use Cases"]
            Models["Domain Models"]
            Interfaces["Repository Interfaces"]
        end
        
        subgraph Data["Data Layer"]
            Repos["Repository Implementations"]
            LocalDS["Local Data Sources"]
            RemoteDS["Remote Data Sources"]
        end
    end
    
    subgraph Infrastructure["Infrastructure"]
        Room["Room Database"]
        DataStore["DataStore Preferences"]
        WorkMgr["WorkManager"]
        Notifications["Notification Service"]
    end
    
    subgraph External["Self-Hosted Server (Docker)"]
        PocketBase["PocketBase<br/>Auth + Admin"]
        Postgres["Postgres<br/>Data Storage"]
        PowerSync["PowerSync<br/>Offline-First Sync"]
        Mosquitto["Mosquitto<br/>MQTT for ESP32"]
    end
    
    Android --> Presentation
    WearOS --> Presentation
    Widget --> ViewModels
    
    Screens --> ViewModels
    ViewModels --> UseCases
    UseCases --> Models
    UseCases --> Interfaces
    
    Interfaces --> Repos
    Repos --> LocalDS
    Repos --> RemoteDS
    
    LocalDS --> Room
    LocalDS --> DataStore
    RemoteDS --> PowerSync
    RemoteDS --> PocketBase
    
    PowerSync --> Postgres
    
    WorkMgr --> Repos
    Notifications --> Android
    Notifications --> WearOS
```

---

## Layer Responsibilities

### Presentation Layer

```mermaid
flowchart LR
    subgraph Presentation["Presentation Layer"]
        subgraph Screens["Screens"]
            Today["TodayScreen"]
            Create["CreateHabitScreen"]
            Detail["HabitDetailScreen"]
            Routine["RoutineRunnerScreen"]
            Recovery["RecoveryScreen"]
            Settings["SettingsScreen"]
        end
        
        subgraph ViewModels["ViewModels"]
            TodayVM["TodayViewModel"]
            CreateVM["CreateHabitViewModel"]
            RoutineVM["RoutineViewModel"]
            SettingsVM["SettingsViewModel"]
        end
        
        subgraph UIState["UI State"]
            State["StateFlow<UiState>"]
            Events["Channel<UiEvent>"]
        end
    end
    
    Screens --> ViewModels
    ViewModels --> UIState
```

| Component | Responsibility |
|-----------|----------------|
| Screens | Compose UI, observes ViewModel state |
| ViewModels | Manages UI state, invokes use cases |
| UI State | Immutable state objects, one-way data flow |
| UI Events | One-time events (navigation, toasts) |

### Domain Layer

```mermaid
flowchart TB
    subgraph Domain["Domain Layer"]
        subgraph UseCases["Use Cases"]
            CreateHabit["CreateHabitUseCase"]
            CompleteHabit["CompleteHabitUseCase"]
            GetTodayHabits["GetTodayHabitsUseCase"]
            DetectLapses["DetectLapsesUseCase"]
            StartRoutine["StartRoutineUseCase"]
            SyncData["SyncDataUseCase"]
        end
        
        subgraph Models["Domain Models"]
            Habit["Habit"]
            Completion["Completion"]
            Routine["Routine"]
            HabitWithStatus["HabitWithStatus"]
            WeeklyReport["WeeklyReport"]
        end
        
        subgraph RepoInterfaces["Repository Interfaces"]
            HabitRepo["HabitRepository"]
            CompletionRepo["CompletionRepository"]
            RoutineRepo["RoutineRepository"]
            SyncRepo["SyncRepository"]
        end
    end
    
    UseCases --> Models
    UseCases --> RepoInterfaces
```

| Component | Responsibility |
|-----------|----------------|
| Use Cases | Single business operation, orchestrates repositories |
| Domain Models | Pure business entities, no framework dependencies |
| Repository Interfaces | Contracts for data access |

### Data Layer

```mermaid
flowchart TB
    subgraph Data["Data Layer"]
        subgraph RepoImpl["Repository Implementations"]
            HabitRepoImpl["HabitRepositoryImpl"]
            CompletionRepoImpl["CompletionRepositoryImpl"]
            RoutineRepoImpl["RoutineRepositoryImpl"]
            SyncRepoImpl["SyncRepositoryImpl"]
        end
        
        subgraph LocalSources["Local Data Sources"]
            HabitDao["HabitDao"]
            CompletionDao["CompletionDao"]
            RoutineDao["RoutineDao"]
            PendingChangeDao["PendingChangeDao"]
            PrefsStore["PreferencesDataStore"]
        end
        
        subgraph RemoteSources["Remote Data Sources"]
            SyncProvider["SyncProvider Interface"]
            FirebaseImpl["FirebaseProvider"]
            PocketBaseImpl["PocketBaseProvider"]
        end
        
        subgraph Mappers["Mappers"]
            EntityMapper["Entity ↔ Domain"]
            DtoMapper["DTO ↔ Entity"]
        end
    end
    
    RepoImpl --> LocalSources
    RepoImpl --> RemoteSources
    RepoImpl --> Mappers
```

| Component | Responsibility |
|-----------|----------------|
| Repositories | Coordinate local and remote data sources |
| DAOs | Room database access objects |
| Providers | Backend-specific sync implementations |
| Mappers | Transform between layer-specific models |

---

## Module Structure

```mermaid
flowchart TB
    subgraph Modules["Project Modules"]
        App["app<br/>Android application"]
        Wear["wear<br/>WearOS application"]
        Core["core<br/>Shared business logic"]
        Data["data<br/>Data layer implementation"]
        Domain["domain<br/>Domain models + interfaces"]
        Sync["sync<br/>Sync infrastructure"]
        UI["ui<br/>Shared UI components"]
    end
    
    App --> Core
    App --> UI
    Wear --> Core
    Wear --> UI
    Core --> Data
    Core --> Domain
    Data --> Domain
    Data --> Sync
    Sync --> Domain
```

### Module Descriptions

| Module | Contents | Dependencies |
|--------|----------|--------------|
| `app` | Android app, DI setup, navigation | core, ui |
| `wear` | WearOS app, tiles, complications | core, ui |
| `core` | Use cases, ViewModels | data, domain |
| `data` | Repositories, DAOs, entities | domain, sync |
| `domain` | Domain models, interfaces | None (pure) |
| `sync` | SyncProvider, queue, conflict resolution | domain |
| `ui` | Shared Compose components, theme | None |

---

## Data Flow

### Habit Completion Flow

```mermaid
sequenceDiagram
    participant UI as TodayScreen
    participant VM as TodayViewModel
    participant UC as CompleteHabitUseCase
    participant Repo as HabitRepository
    participant DAO as CompletionDao
    participant Sync as SyncManager
    
    UI->>VM: onCompleteHabit(habitId, type)
    VM->>UC: invoke(habitId, type)
    
    UC->>UC: Validate completion
    UC->>Repo: createCompletion(completion)
    
    Repo->>DAO: insert(completionEntity)
    DAO-->>Repo: Success
    
    Repo->>Sync: enqueueChange(CREATE, completion)
    Sync-->>Repo: Queued
    
    Repo-->>UC: Completion created
    UC-->>VM: Result.Success
    
    VM->>VM: Update UI state
    VM-->>UI: New state emitted
    
    Note over Sync: Async sync attempt
    Sync->>Sync: Push to remote if online
```

### Routine Execution Flow

```mermaid
sequenceDiagram
    participant UI as RoutineRunner
    participant VM as RoutineViewModel
    participant UC as RunRoutineUseCase
    participant Timer as TimerService
    participant Repo as RoutineRepository
    
    UI->>VM: startRoutine(routineId)
    VM->>UC: start(routineId)
    UC->>Repo: createExecution(routineId)
    Repo-->>UC: Execution created
    UC->>Timer: startTimer(firstHabit.duration)
    UC-->>VM: Execution started
    
    loop Each Habit
        Timer-->>VM: onTick(remaining)
        VM-->>UI: Update timer display
        
        alt Timer expires
            Timer-->>VM: onComplete()
            VM->>UI: Show "Done?" prompt
        else User taps Done
            UI->>VM: onHabitDone()
        end
        
        VM->>UC: completeStep(stepIndex)
        UC->>Repo: recordStepCompletion()
        UC->>Timer: startTimer(nextHabit.duration)
    end
    
    UC->>Repo: completeExecution()
    UC->>Repo: createHabitCompletions()
    UC-->>VM: Routine complete
    VM-->>UI: Show summary
```

### Sync Data Flow

```mermaid
sequenceDiagram
    participant App as Application
    participant SM as SyncManager
    participant Queue as PendingChangeQueue
    participant Provider as SyncProvider
    participant Remote as Remote Backend
    participant Local as Local Database
    
    Note over App,Local: Push Flow (Local → Remote)
    
    App->>Local: Modify entity
    Local->>Queue: Enqueue change
    
    SM->>Queue: Poll pending changes
    Queue-->>SM: Return changes
    
    loop Each change
        SM->>Provider: pushChange(change)
        Provider->>Remote: Write data
        Remote-->>Provider: Success/Conflict
        
        alt Success
            Provider-->>SM: PushResult.Success
            SM->>Local: Mark SYNCED
            SM->>Queue: Remove from queue
        else Conflict
            Provider-->>SM: PushResult.Conflict
            SM->>SM: Resolve conflict
            SM->>Provider: pushChange(resolved)
        end
    end
    
    Note over App,Local: Pull Flow (Remote → Local)
    
    SM->>Provider: pullChanges(since)
    Provider->>Remote: Query changes
    Remote-->>Provider: Return changes
    Provider-->>SM: PullResult
    
    loop Each remote change
        SM->>Local: Check for pending local changes
        
        alt No pending changes
            SM->>Local: Apply remote change
        else Has pending changes
            SM->>SM: Skip (local wins during push)
        end
    end
```

---

## Background Processing

```mermaid
flowchart TB
    subgraph Workers["WorkManager Workers"]
        Lapse["LapseDetectionWorker<br/>Daily at midnight"]
        Fresh["FreshStartWorker<br/>Monday + 1st of month"]
        Sync["SyncWorker<br/>Periodic + on change"]
        Reminder["ReminderWorker<br/>Per habit schedule"]
    end
    
    subgraph Triggers["Triggers"]
        Time["Time-based"]
        Change["Data change"]
        Network["Network available"]
    end
    
    subgraph Actions["Actions"]
        DetectLapse["Detect lapsed habits"]
        CreateRecovery["Create recovery sessions"]
        Notify["Send notifications"]
        PushChanges["Push pending changes"]
        PullChanges["Pull remote changes"]
    end
    
    Time --> Lapse
    Time --> Fresh
    Time --> Reminder
    Change --> Sync
    Network --> Sync
    
    Lapse --> DetectLapse --> CreateRecovery --> Notify
    Fresh --> Notify
    Sync --> PushChanges
    Sync --> PullChanges
```

### Worker Schedule

| Worker | Schedule | Constraints |
|--------|----------|-------------|
| LapseDetectionWorker | Daily, 00:00-06:00 | Battery not low |
| FreshStartWorker | Monday 06:00, 1st of month 06:00 | None |
| SyncWorker | Every 15 minutes | Network connected |
| ReminderWorker | Per-habit schedule | None |

---

## Notification Architecture

```mermaid
flowchart TB
    subgraph Sources["Notification Sources"]
        Reminder["Habit Reminders"]
        Recovery["Recovery Prompts"]
        Fresh["Fresh Start"]
        Routine["Routine Timer"]
        Sync["Sync Status"]
    end
    
    subgraph Channels["Notification Channels"]
        ReminderCh["habit_reminders<br/>Normal priority"]
        RecoveryCh["recovery<br/>Low priority"]
        RoutineCh["routine_timer<br/>High priority"]
        SystemCh["system<br/>Default priority"]
    end
    
    subgraph Manager["NotificationManager"]
        Builder["NotificationBuilder"]
        Scheduler["AlarmManager"]
        Handler["Action Handler"]
    end
    
    Reminder --> ReminderCh
    Recovery --> RecoveryCh
    Fresh --> RecoveryCh
    Routine --> RoutineCh
    Sync --> SystemCh
    
    ReminderCh --> Manager
    RecoveryCh --> Manager
    RoutineCh --> Manager
    SystemCh --> Manager
```

### Notification Actions

| Notification Type | Actions |
|-------------------|---------|
| Habit Reminder | Complete, Snooze, Skip |
| Recovery Prompt | Open Recovery, Dismiss |
| Fresh Start | View Habits, Dismiss |
| Routine Timer | Done, Skip, Pause |

---

## WearOS Architecture

```mermaid
flowchart TB
    subgraph Phone["Phone App"]
        PhoneDB["Room Database"]
        PhoneSync["Sync Manager"]
    end
    
    subgraph Watch["WearOS App"]
        subgraph WearUI["UI"]
            TileService["HabitTileService"]
            Complication["HabitComplication"]
            WearScreens["Wear Compose Screens"]
        end
        
        subgraph WearData["Data"]
            WearDB["Room Database<br/>(Mirror)"]
            DataLayer["Data Layer API"]
        end
    end
    
    subgraph Cloud["Cloud"]
        Backend["Firebase/Custom"]
    end
    
    PhoneDB <--> DataLayer
    DataLayer <--> WearDB
    
    PhoneSync <--> Backend
    
    WearDB --> TileService
    WearDB --> Complication
    WearDB --> WearScreens
    
    Note over DataLayer: Wear Data Layer API<br/>syncs subset of data<br/>between phone and watch
```

### Phone-Watch Sync Strategy

| Data Type | Sync Strategy |
|-----------|---------------|
| Today's Habits | Full sync via Data Layer |
| Completions (today) | Full sync via Data Layer |
| Historical Data | Not synced to watch |
| Routines | Active routine only |
| Settings | Subset (notification prefs) |

---

## Dependency Injection

```mermaid
flowchart TB
    subgraph Modules["Hilt Modules"]
        AppModule["AppModule<br/>Application scope"]
        DatabaseModule["DatabaseModule<br/>Singleton scope"]
        NetworkModule["NetworkModule<br/>Singleton scope"]
        WorkerModule["WorkerModule<br/>Singleton scope"]
    end
    
    subgraph Provides["Provided Dependencies"]
        DB["KairosDatabase"]
        DAOs["All DAOs"]
        Repos["All Repositories"]
        UseCases["All Use Cases"]
        SyncProvider["SyncProvider"]
        WorkMgr["WorkManager"]
    end
    
    DatabaseModule --> DB --> DAOs
    NetworkModule --> SyncProvider
    AppModule --> Repos
    AppModule --> UseCases
    WorkerModule --> WorkMgr
```

---

## Error Handling Strategy

```mermaid
flowchart TB
    subgraph Errors["Error Types"]
        Validation["ValidationException<br/>Invalid input"]
        NotFound["NotFoundException<br/>Entity missing"]
        Conflict["ConflictException<br/>Sync conflict"]
        Network["NetworkException<br/>Connectivity"]
        Auth["AuthException<br/>Authentication"]
    end
    
    subgraph Handling["Handling Strategy"]
        UseCaseH["Use Case Layer<br/>Catch, wrap in Result"]
        ViewModelH["ViewModel Layer<br/>Map to UI state"]
        UIH["UI Layer<br/>Display message"]
    end
    
    subgraph Recovery["Recovery Actions"]
        Retry["Retry operation"]
        Redirect["Navigate to auth"]
        Show["Show error message"]
        Log["Log for debugging"]
    end
    
    Errors --> UseCaseH --> ViewModelH --> UIH
    
    Validation --> Show
    NotFound --> Show
    Conflict --> Retry
    Network --> Retry
    Auth --> Redirect
```

### Result Type Pattern

```mermaid
classDiagram
    class Result~T~ {
        <<sealed>>
    }
    
    class Success~T~ {
        +data: T
    }
    
    class Error {
        +exception: Exception
        +message: String
    }
    
    class Loading {
    }
    
    Result <|-- Success
    Result <|-- Error
    Result <|-- Loading
```

---

## Security Architecture

```mermaid
flowchart TB
    subgraph Client["Client Security"]
        Keystore["Android Keystore<br/>Token storage"]
        SSL["SSL Pinning<br/>Network security"]
        Proguard["ProGuard/R8<br/>Code obfuscation"]
    end
    
    subgraph Transport["Transport Security"]
        TLS["TLS 1.3<br/>All connections"]
        Cert["Certificate validation"]
    end
    
    subgraph Backend["Backend Security"]
        Auth["Firebase Auth<br/>Identity verification"]
        Rules["Firestore Rules<br/>Access control"]
        Isolation["User data isolation"]
    end
    
    Client --> Transport --> Backend
```

### Security Rules

| Rule | Implementation |
|------|----------------|
| Token storage | Android Keystore (hardware-backed) |
| Network | TLS required, no cleartext |
| Data isolation | Firestore rules enforce user-only access |
| PII in logs | Prohibited, enforced by lint |
| Session management | Refresh tokens, automatic expiry |

---

## Performance Considerations

### Database Optimization

| Optimization | Purpose |
|--------------|---------|
| Indices on query columns | Fast habit/completion lookup |
| Paging for history | Memory efficiency |
| Precomputed views | Today screen performance |
| Batch operations | Sync efficiency |

### UI Optimization

| Optimization | Purpose |
|--------------|---------|
| Lazy composition | Only render visible items |
| Stable keys | Minimize recomposition |
| Async image loading | Smooth scrolling |
| Remember/derivedStateOf | Avoid redundant computation |

### Sync Optimization

| Optimization | Purpose |
|--------------|---------|
| Incremental sync | Minimize data transfer |
| Change coalescing | Reduce operations |
| Batch writes | Fewer round trips |
| Offline queue | Never block UI |

---

## Deployment Architecture

```mermaid
flowchart LR
    subgraph Development["Development"]
        Local["Local builds"]
        Debug["Debug variants"]
    end
    
    subgraph CI["CI/CD"]
        GitHub["GitHub Actions"]
        Build["Build + Test"]
        Sign["Sign APK/AAB"]
    end
    
    subgraph Distribution["Distribution"]
        PlayStore["Google Play Store"]
        Internal["Internal testing"]
        GitHub2["GitHub Releases"]
    end
    
    Development --> CI --> Distribution
```

### Build Variants

| Variant | Backend | Logging | Debuggable |
|---------|---------|---------|------------|
| debug | Local server (configurable) | Verbose | Yes |
| staging | Staging server | Info | Yes |
| release | User-configured server | Error only | No |
