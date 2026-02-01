# WearOS Integration Design

## Overview

WearOS integration is a key differentiator for Kairos—it reduces phone dependency, which is critical for ADHD users who find phones distracting. The watch serves as a low-friction completion device and gentle reminder system.

---

## Design Philosophy

```mermaid
mindmap
  root((WearOS Philosophy))
    Reduce Phone Pulls
      Quick glance info
      Complete without phone
      Minimal interaction
    Complement Not Replace
      Subset of features
      Phone for setup
      Watch for execution
    Glanceable
      Key info visible immediately
      No scrolling required
      Clear visual hierarchy
    Low Friction
      One-tap complete
      No typing
      Fast interactions
```

### WearOS-Specific Constraints

| Constraint | Implication |
|------------|-------------|
| Small screen | Prioritize essential info only |
| Limited battery | Minimize background processing |
| Short interactions | Design for <5 second sessions |
| No keyboard | No text input, selection only |
| Connectivity | May be disconnected from phone |

---

## Feature Scope

### Included on Watch

```mermaid
flowchart LR
    subgraph WatchFeatures["Watch Features"]
        View["View today's habits"]
        Complete["Complete habits"]
        Skip["Skip habits"]
        Routine["Run routines"]
        Progress["View progress"]
        Reminders["Receive reminders"]
    end
```

### Phone Only

```mermaid
flowchart LR
    subgraph PhoneOnly["Phone Only Features"]
        Create["Create habits"]
        Edit["Edit habits"]
        Recovery["Recovery sessions"]
        Analytics["Detailed analytics"]
        Settings["Full settings"]
        Sync["Sync configuration"]
    end
```

---

## Watch Components

### Component Overview

```mermaid
flowchart TB
    subgraph WearComponents["WearOS Components"]
        subgraph Surfaces["Glanceable Surfaces"]
            Tile["Habit Tile<br/>Primary interaction"]
            Complication["Complication<br/>Watch face widget"]
        end
        
        subgraph App["Watch App"]
            HabitList["Habit List Screen"]
            HabitDetail["Habit Detail"]
            RoutineRunner["Routine Runner"]
        end
        
        subgraph Background["Background"]
            DataSync["Data Layer Sync"]
            NotifBridge["Notification Bridge"]
        end
    end
```

---

## Tile Design

### Habit Tile Layout

```mermaid
flowchart TB
    subgraph TileLayout["Habit Tile (Primary)"]
        Header["Today • 3/5 done"]
        
        subgraph HabitList["Habit List"]
            H1["☐ Take medication"]
            H2["☐ Drink water"]
            H3["✓ Stretch (done)"]
        end
        
        Footer["Tap habit to complete"]
    end
```

### Tile States

```mermaid
stateDiagram-v2
    [*] --> Loading: Tile requested
    Loading --> Empty: No habits today
    Loading --> HasHabits: Habits loaded
    
    state Empty {
        [*] --> EmptyMsg
        EmptyMsg: "No habits today"
        EmptyMsg: "Tap to open app"
    }
    
    state HasHabits {
        [*] --> ShowList
        ShowList --> AllDone: All completed
    }
    
    state AllDone {
        [*] --> Celebration
        Celebration: "✓ All done!"
    }
```

### Tile Interaction

```mermaid
sequenceDiagram
    actor User
    participant Tile
    participant Service as TileService
    participant Phone as Phone (via DataLayer)
    
    User->>Tile: Tap habit row
    Tile->>Service: onTileRequest (with clickable)
    Service->>Service: Identify tapped habit
    Service->>User: Show confirmation dialog
    
    alt Confirm completion
        User->>Service: Confirm "Done"
        Service->>Service: Update local state
        Service->>Tile: Refresh tile
        Service->>Phone: Sync completion
    else Cancel
        User->>Service: Dismiss
        Service->>Tile: No change
    end
```

### Tile Technical Specs

| Spec | Value |
|------|-------|
| Max habits shown | 5 (scrollable) |
| Refresh rate | On data change + every 15 min |
| Clickable elements | Each habit row |
| Tile type | Single tile, full-width |

---

## Complication Design

### Complication Types

```mermaid
flowchart LR
    subgraph Complications["Supported Complications"]
        Short["Short Text<br/>'3 left'"]
        Long["Long Text<br/>'3 habits remaining'"]
        Ranged["Ranged Value<br/>Progress ring"]
        Small["Small Image<br/>App icon + badge"]
    end
```

### Complication Data

| Type | Display | Tap Action |
|------|---------|------------|
| SHORT_TEXT | "3 left" or "✓ Done" | Open tile |
| LONG_TEXT | "3 habits remaining" | Open tile |
| RANGED_VALUE | Progress percentage | Open app |
| SMALL_IMAGE | Icon with count badge | Open app |

### Complication States

| State | Short Text | Long Text | Icon |
|-------|------------|-----------|------|
| Habits pending | "3 left" | "3 habits remaining" | App icon |
| All done | "✓" | "All done for today" | Checkmark |
| No habits | "—" | "No habits today" | App icon (dim) |

---

## Watch App Screens

### Screen Flow

```mermaid
flowchart TB
    subgraph WatchApp["Watch App Navigation"]
        Launch["App Launch"]
        
        subgraph Main["Main Flow"]
            HabitList["Habit List<br/>(Scrollable)"]
            HabitDetail["Habit Detail<br/>+ Complete"]
        end
        
        subgraph Routine["Routine Flow"]
            RoutineList["Routine List"]
            RoutineRun["Routine Runner"]
        end
        
        Settings["Settings<br/>(Link to phone)"]
    end
    
    Launch --> HabitList
    HabitList --> HabitDetail
    HabitList --> RoutineList
    RoutineList --> RoutineRun
    HabitList --> Settings
```

### Habit List Screen

```mermaid
flowchart TB
    subgraph HabitListScreen["Habit List (ScalingLazyColumn)"]
        Header["Today<br/>Progress chip: 3/5"]
        
        subgraph Items["Habit Items"]
            Morning["🌅 Morning"]
            M1["○ Take medication"]
            M2["○ Drink water"]
            Afternoon["☀️ Afternoon"]
            A1["○ Review calendar"]
        end
        
        Routines["▶ Routines"]
    end
```

| Element | Component | Behavior |
|---------|-----------|----------|
| Header | Text + Chip | Static |
| Category | Text | Section header |
| Habit item | Chip (toggleable) | Tap → Detail or confirm |
| Routine link | Chip | Tap → Routine list |

### Habit Detail Screen

```mermaid
flowchart TB
    subgraph HabitDetailScreen["Habit Detail"]
        Name["Take medication"]
        Anchor["After brushing teeth"]
        Duration["~2 min"]
        
        subgraph Actions["Actions"]
            Done["✓ Done"]
            Partial["◐ Partial"]
            Skip["⊘ Skip"]
        end
    end
```

### Routine Runner (Watch)

```mermaid
flowchart TB
    subgraph WatchRoutineRunner["Routine Runner (Watch)"]
        StepIndicator["Step 2 of 4"]
        HabitName["Take medication"]
        Timer["2:45"]
        
        subgraph Controls["Controls"]
            Done["✓"]
            Skip["→"]
        end
        
        UpNext["Next: Drink water"]
    end
```

| Feature | Watch | Phone |
|---------|-------|-------|
| Start routine | ✓ | ✓ |
| View current step | ✓ | ✓ |
| Done/Skip controls | ✓ | ✓ |
| Pause routine | ✓ | ✓ |
| Variant selection | ❌ (uses default) | ✓ |
| View full summary | ❌ | ✓ |

---

## Data Synchronization

### Data Layer Architecture

```mermaid
flowchart TB
    subgraph Phone["Phone"]
        PhoneDB["Room Database"]
        PhoneRepo["Repository"]
        DataClient["DataClient"]
    end
    
    subgraph DataLayer["Wear Data Layer"]
        DataItems["Data Items<br/>/kairos/habits<br/>/kairos/completions"]
        Messages["Messages<br/>completion_created<br/>routine_started"]
    end
    
    subgraph Watch["Watch"]
        WatchClient["DataClient"]
        WatchCache["Local Cache"]
        WatchUI["UI Components"]
    end
    
    PhoneRepo --> DataClient
    DataClient --> DataItems
    DataClient --> Messages
    
    DataItems --> WatchClient
    Messages --> WatchClient
    WatchClient --> WatchCache
    WatchCache --> WatchUI
```

### Sync Strategy

| Data Type | Sync Method | Direction |
|-----------|-------------|-----------|
| Today's habits | DataItem | Phone → Watch |
| Today's completions | DataItem | Bidirectional |
| Active routine | DataItem | Bidirectional |
| Completion action | Message | Watch → Phone |
| Routine control | Message | Watch → Phone |

### Data Item Paths

| Path | Content | Update Frequency |
|------|---------|------------------|
| `/kairos/today/habits` | Today's habits JSON | On change |
| `/kairos/today/completions` | Today's completions JSON | On change |
| `/kairos/routine/active` | Active routine state | Real-time |
| `/kairos/config` | User preferences subset | On change |

### Message Types

| Message | Payload | Handler |
|---------|---------|---------|
| `habit_completed` | habitId, type, timestamp | Phone creates completion |
| `habit_skipped` | habitId, reason | Phone creates completion |
| `routine_started` | routineId | Phone starts routine |
| `routine_step_done` | executionId, stepIndex | Phone updates execution |
| `routine_paused` | executionId | Phone pauses routine |

---

## Offline Behavior

### Watch Standalone Mode

```mermaid
flowchart TB
    subgraph Offline["Watch Disconnected from Phone"]
        Check{"Phone<br/>connected?"}
        
        Check -->|Yes| Normal["Normal sync<br/>via Data Layer"]
        Check -->|No| Standalone["Standalone Mode"]
        
        subgraph Standalone["Standalone Mode"]
            Cache["Use cached data"]
            LocalAction["Queue actions locally"]
            Sync["Sync when reconnected"]
        end
    end
```

### Offline Capabilities

| Feature | Offline Behavior |
|---------|------------------|
| View habits | ✓ (cached) |
| Complete habit | ✓ (queued) |
| View completions | ✓ (cached) |
| Run routine | ✓ (local state) |
| Receive reminders | ✓ (bridged or local) |

### Sync Queue

When watch reconnects to phone:
1. Push queued completions
2. Push routine state
3. Pull latest habits/completions
4. Refresh tile and complications

---

## Notifications on Watch

### Bridged Notifications

| Phone Notification | Watch Behavior |
|-------------------|----------------|
| Habit reminder | Bridged with Done/Snooze/Skip |
| Lapse prompt | Bridged (opens on phone) |
| Fresh start | Bridged (opens on phone) |
| Routine timer | Bridged with controls |

### Watch Actions

```mermaid
flowchart LR
    subgraph WatchNotifActions["Watch Notification Actions"]
        Reminder["Habit Reminder"]
        Reminder --> Done["✓ Done"]
        Reminder --> Snooze["⏰ +10 min"]
        Reminder --> Phone["📱 On Phone"]
        
        Routine["Routine Step"]
        Routine --> RDone["✓ Done"]
        Routine --> RSkip["Skip →"]
    end
```

---

## UI Components (Wear Compose)

### Chip Styles

```mermaid
flowchart LR
    subgraph Chips["Habit Chips"]
        Pending["○ Take medication<br/>(Pending)"]
        Done["✓ Take medication<br/>(Completed)"]
        Skipped["⊘ Take medication<br/>(Skipped)"]
    end
```

### Progress Indicators

| Context | Indicator |
|---------|-----------|
| Daily progress | Circular progress (complication) |
| Routine progress | Linear step indicator |
| Routine timer | Countdown with progress ring |

### Color Scheme

| State | Color | Usage |
|-------|-------|-------|
| Pending | Surface | Uncompleted habits |
| Completed | Primary | Completed habits |
| Skipped | Surface variant | Skipped habits |
| Routine active | Primary | Current step |

---

## Performance Considerations

### Battery Optimization

| Strategy | Implementation |
|----------|----------------|
| Minimal wake locks | Only for routine timer |
| Efficient tile updates | Debounce rapid changes |
| Lazy data loading | Load on-demand |
| Compression | Compress data items |

### Memory Management

| Limit | Value |
|-------|-------|
| Cached habits | Today only |
| Cached completions | Today only |
| Max data item size | 100 KB |

### Tile Performance

| Metric | Target |
|--------|--------|
| Tile render time | < 100ms |
| Data fetch time | < 500ms |
| Total load time | < 1 second |

---

## Testing Considerations

### Test Scenarios

| Scenario | Test Case |
|----------|-----------|
| Phone connected | Normal sync, real-time updates |
| Phone disconnected | Standalone mode, queue actions |
| Reconnection | Queue flush, state reconciliation |
| Low battery | Tile still renders |
| Screen off/on | Data refreshes correctly |

### Emulator Testing

- Use Wear OS emulator paired with phone emulator
- Test Data Layer sync
- Test notification bridging
- Test standalone mode (disconnect phone)

---

## Implementation Notes

### Dependencies

```kotlin
// build.gradle.kts (wear module)
dependencies {
    // Compose for Wear
    implementation("androidx.wear.compose:compose-material:1.3.0")
    implementation("androidx.wear.compose:compose-foundation:1.3.0")
    implementation("androidx.wear.compose:compose-navigation:1.3.0")
    
    // Tiles
    implementation("androidx.wear.tiles:tiles:1.3.0")
    implementation("androidx.wear.tiles:tiles-material:1.3.0")
    
    // Complications
    implementation("androidx.wear.watchface:watchface-complications-data-source-ktx:1.2.1")
    
    // Data Layer
    implementation("com.google.android.gms:play-services-wearable:18.1.0")
    
    // Horologist (Wear utilities)
    implementation("com.google.android.horologist:horologist-compose-layout:0.5.17")
    implementation("com.google.android.horologist:horologist-tiles:0.5.17")
}
```

### Module Structure

```
wear/
├── src/main/kotlin/com/kairos/wear/
│   ├── WearApp.kt                    # Application class
│   ├── tile/
│   │   ├── HabitTileService.kt       # Tile implementation
│   │   └── TileState.kt              # Tile state model
│   ├── complication/
│   │   └── HabitComplicationService.kt
│   ├── presentation/
│   │   ├── MainActivity.kt
│   │   ├── HabitListScreen.kt
│   │   ├── HabitDetailScreen.kt
│   │   ├── RoutineRunnerScreen.kt
│   │   └── theme/
│   │       └── WearTheme.kt
│   ├── data/
│   │   ├── WearDataRepository.kt     # Data Layer client
│   │   ├── DataLayerListenerService.kt
│   │   └── LocalCache.kt
│   └── di/
│       └── WearModule.kt             # Hilt module
```

---

## Future Enhancements

| Enhancement | Priority | Description |
|-------------|----------|-------------|
| Standalone app | P2 | Full offline without phone |
| Voice input | P3 | Complete via voice |
| Haptic patterns | P2 | Custom vibration for reminders |
| Health Services | P3 | Step count correlation |
| Always-on display | P2 | Routine timer visible on AOD |
