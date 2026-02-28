# Shared Module — Domain Logic

**Generated:** 2026-02-28 | **Commit:** 7dd282c

Pure Kotlin domain logic for Kairos habit tracking. No platform dependencies.

## Structure

```
shared/src/
├── commonMain/           # Pure Kotlin (NO platform imports)
│   └── .../kairos/
│       ├── sync/         # Event-driven sync logic
│       ├── display/      # DisplayModeStateMachine
│       └── App.kt        # Shared Compose entry
├── commonTest/           # Kotest assertions
├── androidMain/          # Android expect/actual
└── jvmMain/              # Desktop expect/actual
```

## Where to Look

| Task | Location |
|------|----------|
| Sync events | `sync/` — Event schemas, merge logic |
| State machine | `display/` — DisplayModeStateMachine, reduce() |
| Platform impls | `androidMain/`, `jvmMain/` — expect/actual |

## Key Patterns

### DisplayModeStateMachine

```kotlin
// Pure function reducer
fun reduce(state: State, event: Event, config: Config): Transition

// Events are sealed interface
sealed interface Event {
    data class PresenceChanged(val state: PresenceState) : Event
    data object SwipeToggle : Event
    data class TimerTick(val now: Instant) : Event
}

// State is immutable data class
data class State(
    val displayMode: DisplayMode,
    val presenceState: PresenceState,
    val manualOverrideUntil: Instant?,
    val cooldownUntil: Instant?
)
```

### Expect/Actual Pattern

```kotlin
// commonMain
expect class LocalDateTimeProvider {
    fun now(): Instant
}

// androidMain / jvmMain
actual class LocalDateTimeProvider {
    actual fun now(): Instant = Clock.System.now()
}
```

## Anti-Patterns (THIS MODULE)

- **NEVER** `import java.time.*` in commonMain
- **NEVER** `import android.*` in commonMain
- **NEVER** mutable state in domain entities
- **NEVER** platform-specific code outside androidMain/jvmMain

## Testing

- Domain logic tests go in `commonTest/`
- Use kotest assertions: `task.name shouldBe "Check door"`
- Tests run on all platforms via `./gradlew :shared:allTests`
