# Kotlin Rules

Applies to: `kotlin/` directory (Kotlin Multiplatform project)

## Module Structure

### Shared module is pure
**Applies to**: `:composeApp/src/commonMain`
**Rule**: The shared module must not depend on platform APIs. No Android, no JVM-specific, no iOS-specific code.

```
// ✅ Correct - pure Kotlin
expect class LocalDateTimeProvider {
    fun now(): Instant
}

// ❌ Avoid - platform dependency in commonMain
import java.time.Instant // NO - JVM only
import android.os.Bundle // NO - Android only
```

### Platform implementations in platform source sets
**Applies to**: `:composeApp/src/{androidMain,jvmMain}`
**Rule**: Use `actual` implementations in platform source sets, not in commonMain.

```
// commonMain
expect class LocalDateTimeProvider {
    fun now(): Instant
}

// androidMain
actual class LocalDateTimeProvider {
    actual fun now(): Instant = Clock.System.now()
}

// jvmMain
actual class LocalDateTimeProvider {
    actual fun now(): Instant = Clock.System.now()
}
```

## Domain Entities

### Use data classes for entities
**Applies to**: Domain entities (Task, ChecklistSession, etc.)
**Rule**: Define domain entities as data classes with immutable properties.

```kotlin
// ✅ Correct
data class Task(
    val id: String,
    val name: String,
    val mode: Mode,
    val createdAt: Instant
)

// ❌ Avoid
class Task(
    var id: String, // mutable
    var name: String
)
```

### Sealed classes for state machines
**Applies to**: Presence state, sync state, etc.
**Rule**: Use sealed classes/interfaces for finite state representations.

```kotlin
// ✅ Correct
sealed interface PresenceState {
    data object Idle : PresenceState
    data object PresenceDetected : PresenceState
    data object ChecklistActive : PresenceState
    data class Cooldown(val until: Instant) : PresenceState
}
```

### Value classes for IDs
**Applies to**: Entity identifiers
**Rule**: Use value classes to prevent ID mix-ups.

```kotlin
// ✅ Correct
@JvmInline
value class TaskId(val value: String)

@JvmInline
value class SessionId(val value: String)

data class Task(val id: TaskId, val name: String)
```

## Event Sourcing

### Events are immutable facts
**Applies to**: All event types
**Rule**: Events represent things that happened. Never mutate events.

```kotlin
// ✅ Correct
data class ChecklistCompletionEvent(
    override val id: String,
    override val timestamp: Instant,
    val taskId: TaskId,
    val sessionId: SessionId
) : Event
```

### Event IDs must be unique per device
**Applies to**: All event types
**Rule**: Generate event IDs using a combination of device ID and sequence number or UUID.

## Error Handling

### Use Result for recoverable errors
**Applies to**: Operations that can fail
**Rule**: Return `Result<T>` for expected failures. Throw exceptions only for unexpected/unrecoverable errors.

```kotlin
// ✅ Correct
suspend fun syncEvents(): Result<SyncResult> {
    return try {
        val events = fetchRemoteEvents()
        Result.success(SyncResult(events))
    } catch (e: IOException) {
        Result.failure(e)
    }
}
```

## Testing

### Test shared logic in commonTest
**Applies to**: Pure business logic
**Rule**: Domain logic tests go in `commonTest` so they run on all platforms.

### Use kotest assertions
**Applies to**: All Kotlin tests
**Rule**: Prefer kotest assertions style for readability.

```kotlin
// ✅ Correct
task.name shouldBe "Check door"
task.mode shouldBe Mode.LEAVING
```
