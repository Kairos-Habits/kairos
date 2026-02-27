## Implementation Plan: Display Mode State Machine

### Overview
Add a deterministic `DisplayModeStateMachine` to `:shared` with platform-agnostic policy logic. The kiosk module will feed events and execute the returned display decisions.

### File Structure

```
kotlin/shared/src/
├── commonMain/kotlin/com/rghsoftware/kairos/display/
│   ├── DisplayMode.kt              # enum KIOSK | HA
│   ├── PresenceState.kt            # sealed interface PRESENT | ABSENT | UNKNOWN
│   ├── DisplayModeState.kt         # data class with all state fields
│   ├── DisplayModeEvent.kt         # sealed interface for events
│   ├── DisplayModeConfig.kt        # configurable timing parameters
│   └── DisplayModeStateMachine.kt  # reducer function
└── commonTest/kotlin/com/rghsoftware/kairos/display/
    └── DisplayModeStateMachineTest.kt
```

### Components

**1. `DisplayMode.kt`** - Enum for display targets
```kotlin
enum class DisplayMode { KIOSK, HA }
```

**2. `PresenceState.kt`** - Sealed interface (per kotlin.md conventions)
```kotlin
sealed interface PresenceState {
    data object Present : PresenceState
    data object Absent : PresenceState  
    data object Unknown : PresenceState
}
```

**3. `DisplayModeState.kt`** - Immutable state container
```kotlin
data class DisplayModeState(
    val displayMode: DisplayMode = DisplayMode.HA,
    val presenceState: PresenceState = PresenceState.Unknown,
    val manualOverrideUntil: Instant? = null,
    val cooldownUntil: Instant? = null
)
```

**4. `DisplayModeEvent.kt`** - Sealed event interface
```kotlin
sealed interface DisplayModeEvent {
    data class PresenceChanged(val state: PresenceState, val timestamp: Instant) : DisplayModeEvent
    data class SwipeToggle(val timestamp: Instant) : DisplayModeEvent
    data class TimerTick(val timestamp: Instant) : DisplayModeEvent
}
```

**5. `DisplayModeConfig.kt`** - Configurable parameters
```kotlin
data class DisplayModeConfig(
    val manualOverrideDuration: Duration = 5.minutes,
    val autoSwitchCooldown: Duration = 90.seconds
)
```

**6. `DisplayModeStateMachine.kt`** - Pure reducer function
```kotlin
data class DisplayModeTransition(
    val newState: DisplayModeState,
    val targetDisplayMode: DisplayMode,
    val displayChanged: Boolean
)

fun DisplayModeState.reduce(event: DisplayModeEvent, config: DisplayModeConfig): DisplayModeTransition
```

### Transition Logic (Core Rules)

| Current | Event | Condition | New Display | Side Effects |
|---------|-------|-----------|-------------|--------------|
| HA | PresenceChanged(PRESENT) | no override, no cooldown | KIOSK | start cooldown |
| HA | PresenceChanged(PRESENT) | override active | HA | none |
| * | SwipeToggle | any | toggle | set override +5min |
| * | TimerTick | override expired | - | clear override |
| * | TimerTick | cooldown expired | - | clear cooldown |

- **Manual override blocks all auto-switching**
- **Cooldown blocks presence-triggered auto-switch only**
- **No auto-return to HA on ABSENT**

### Testing Strategy

Unit tests in `commonTest` covering:
1. FR-1: HA→KIOSK on presence (no override/cooldown)
2. FR-2: Swipe always toggles, sets override
3. FR-3: Cooldown blocks auto-switch
4. FR-4: No auto-return to HA on absence
5. Override expiration via TimerTick
6. Cooldown expiration via TimerTick
7. Edge cases (rapid events, expired states)

### Kiosk Integration (Future - not in this PR)

The kiosk module will:
1. Instantiate `DisplayModeStateMachine` with config
2. Feed `PresenceChanged` from serial JSONL
3. Feed `SwipeToggle` from gesture detector
4. Feed `TimerTick` on interval
5. Execute `targetDisplayMode` on transitions where `displayChanged=true`

### Dependencies
- `kotlinx-datetime` for `Instant` and `Duration` (cross-platform)
- Add to `shared/build.gradle.kts`:
  ```kotlin
  commonMain {
      dependencies {
          implementation("org.jetbrains.kotlinx:kotlinx-datetime:0.6.1")
      }
  }
  ```

### Estimated Files Changed
- **New**: 7 source files, 1 test file
- **Modified**: `shared/build.gradle.kts` (add kotlinx-datetime)

### Verification
```bash
cd kotlin && ./gradlew :shared:test :shared:assemble
```