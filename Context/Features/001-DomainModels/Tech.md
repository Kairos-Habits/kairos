# Technical Planning: Domain Models

**Created**: 2026-02-01
**Status**: Planning Complete
**Prerequisites**: Completed business specification (Spec.md)

---

## Research & Analysis

### Research Scope

This research phase focused exclusively on **codebase integration analysis** for the domain models implementation. Since domain models are pure Kotlin data classes with no external service dependencies, research centered on:

- Existing `shared` module package structure and available dependencies
- kotlinx-datetime library capabilities for cross-platform date/time handling
- kotlinx-serialization integration for future persistence needs
- Kotlin 2.3.0 data class best practices and validation patterns

No external API research or architectural pattern research was required, as domain models are foundational data structures with well-established implementation patterns.

### Key Findings Summary

The `shared` module is **fully configured** and ready for domain model implementation with all required dependencies already present (kotlinx-datetime 0.7.1, kotlinx-serialization 1.10.0, Room 2.8.4). The module is currently empty with no conflicting implementations, providing a clean slate for implementing the 5 core domain models and 5 enum types specified in docs/design/05-domain-model.md.

### Codebase Integration Analysis

**Existing Architecture Patterns**:

- **Package base**: `com.kairoshabits.shared` (correctly configured in build.gradle.kts)
- **Code style**: Kotlin 2.3.0 with Java 17 compatibility, 4-space indentation, Spotless auto-formatting
- **Naming**: PascalCase for classes, lowercase packages, one public type per file
- **Pattern philosophy**: No premature abstraction, flat packages over nested hierarchies (from CLAUDE.md)

**Related Existing Components**:

- **Models**: None exist yet - this is Phase 1, creating foundational models from scratch
- **Views**: Not applicable - domain models have no UI dependencies
- **Services**: Not applicable - no repositories or business logic exist yet (Phase 3)
- **Navigation**: Not applicable - domain models are data structures, not UI components

**Integration Requirements**:

- **Files to Modify**: None - all files are new
- **New Files to Create**:
  - 5 domain model data classes: Habit, Completion, Routine, RoutineExecution, RecoverySession
  - 5 enum classes: HabitPhase, HabitStatus, HabitCategory, CompletionType, SkipReason
  - Directory structure: `shared/src/main/java/com/kairoshabits/shared/model/`
- **API Integration Points**: None - models are consumed by future phases, not integrated with existing code
- **Data Flow**: Models will serve as single source of truth for Phase 2 (Room entities) and Phase 3 (repositories)

**Implementation Considerations**:

- **Consistency Requirements**:
  - Follow flat package structure: all models and enums directly in `model/` package
  - Use kotlinx-datetime types (Instant, LocalDate, LocalTime) not java.time for cross-platform compatibility
  - Use java.util.UUID for id fields (standard library, no extra dependency)
  - Apply @Serializable annotations for future kotlinx-serialization support
- **Potential Conflicts**: None - module is empty
- **Refactoring Needs**: None - greenfield implementation

### Technology Research

#### kotlinx-datetime

**Version**: 0.7.1 (already configured in project)
**Documentation**: https://github.com/Kotlin/kotlinx-datetime
**Research Date**: 2026-02-01
**Status**: Existing dependency (libs.versions.toml)

**Key Capabilities**:

- Cross-platform date/time library for Kotlin Multiplatform (works on Android, JVM, iOS, etc.)
- Provides `Instant` for timestamps with nanosecond precision
- Provides `LocalDate` for date values without timezone (habit completion dates)
- Provides `LocalTime` for time-of-day values (habit time windows)
- TimeZone-aware conversions between Instant and LocalDateTime
- Serialization-friendly types (kotlinx-serialization support)

**Limitations**:

- Not a direct replacement for java.time - simpler API with fewer features
- Duration handling is basic compared to java.time.Duration
- No built-in formatting (use toString() or manual formatting)

**Best Practices**:

- Use `Instant` for all "when this happened" timestamps (createdAt, updatedAt, completedAt)
- Use `LocalDate` for "on this date" values (habit completion date)
- Use `LocalTime` for "at this time of day" values (time window bounds)
- Store `Instant` for sync and ordering, derive `LocalDate` for display in user's timezone

**Decision Rationale**: Required for cross-platform compatibility (Android + WearOS share the same models). Avoids java.time which has different behavior on older Android versions (pre-API 26 requires desugaring).

#### kotlinx-serialization

**Version**: 1.10.0 (already configured in project)
**Documentation**: https://github.com/Kotlin/kotlinx.serialization
**Research Date**: 2026-02-01
**Status**: Existing dependency with plugin enabled

**Key Capabilities**:

- Kotlin-native JSON/binary serialization without reflection
- Compile-time code generation for serializers (KSP-based)
- Supports complex types: UUIDs, Instants, LocalDates (with custom serializers)
- Built-in support for nullable fields, default values, lists, sets, maps
- Compatible with data classes, sealed classes, enums

**Limitations**:

- UUID requires custom serializer (serialize as string)
- kotlinx-datetime types require kotlinx-serialization-datetime artifact (not yet added)
- Generated code increases build time slightly

**Best Practices**:

- Annotate all domain models with `@Serializable` for future JSON export/import
- Use `@SerialName` for stable field names that won't break if Kotlin property names change
- Provide custom serializers for UUID (toString/fromString) and kotlinx-datetime types
- Keep models as data classes for automatic equals/hashCode/copy/toString

**Decision Rationale**: Enables future JSON export feature (Phase 10 settings) and potential network sync (Phase 11). Adds negligible runtime overhead and prepares models for serialization without code changes later.

#### Kotlin Data Classes

**Version**: Kotlin 2.3.0 (project standard)
**Documentation**: https://kotlinlang.org/docs/data-classes.html
**Research Date**: 2026-02-01
**Context**: Core Kotlin language feature

**Key Capabilities**:

- Automatic `equals()`, `hashCode()`, `toString()`, `copy()`, `componentN()` functions
- Destructuring support for pattern matching
- Immutable by default (val properties)
- Primary constructor for concise initialization
- `init` blocks for validation logic

**Limitations**:

- No inheritance between data classes (use composition instead)
- All properties in primary constructor are visible in `copy()` and `componentN()`
- No way to hide internal fields from copy/equals/hashCode

**Best Practices**:

- Put all fields in primary constructor for automatic data class methods
- Use `init` blocks for validation (throw IllegalArgumentException for invalid state)
- Keep data classes immutable (val not var)
- Use default parameter values for optional fields (`description: String? = null`)
- Provide KDoc comments for all public data classes and properties

**Decision Rationale**: Standard Kotlin pattern for domain models. Provides structural equality (value-based comparison) essential for testing and detecting changes in sync scenarios.

### API & Service Research

**Not Applicable**: Domain models are pure Kotlin data classes representing business entities. No external APIs or services are consumed by this feature. Integration with external services (if any) will occur in later phases:

- Phase 11: Sync integration with PowerSync/PocketBase
- Phase 13: MQTT integration for ESP32 devices

Models are designed to be serializable and sync-compatible but do not directly interact with external services.

### Architecture Pattern Research

#### Constructor Validation Pattern

**Research Sources**: Kotlin documentation (kotlinlang.org/docs/classes.html#constructors-and-initializer-blocks)
**Research Date**: 2026-02-01
**Context**: Standard Kotlin pattern for enforcing invariants

**Approach**:

- Use `init` blocks after primary constructor to validate parameters
- Throw `IllegalArgumentException` with descriptive message for invalid state
- Validation runs on every object creation (including `copy()`)
- Keep validation logic close to data class definition for maintainability

**Benefits**:

- Impossible to create invalid domain objects (invariants enforced at construction time)
- Validation logic is local and easy to find (in the model file itself)
- Clear error messages guide developers to fix issues immediately
- No need for separate validator classes or UseCase ceremony

**Drawbacks**:

- Cannot partially construct objects (all fields must be valid)
- Validation runs on every `copy()` call (small performance impact)
- Stack traces point to init block, not call site (mitigated by clear error messages)

**Implementation Considerations**:

- Error messages must follow ADHD-first language rules (no shame language)
- Use require() or check() with lazy message for clarity
- Document validation rules in KDoc comments on the data class

**Decision Rationale**: Aligns with project's "no premature abstraction" principle. Keeps validation colocated with models instead of scattered across validator classes. Ensures type safety at compile time (if it compiles and constructs, it's valid).

#### Immutable Domain Models Pattern

**Research Sources**: Domain-Driven Design (Evans), Kotlin best practices
**Research Date**: 2026-02-01
**Context**: Industry-standard pattern for domain entities

**Approach**:

- Use `val` (immutable) properties instead of `var` (mutable)
- Modifications create new instances via `copy()` method
- Enables structural equality (two objects with same values are equal)
- Prevents accidental mutation bugs

**Benefits**:

- Thread-safe by default (safe for coroutines and background workers)
- Easier to reason about (no hidden state changes)
- Works well with Jetpack Compose (recomposition detects new instances)
- Simplifies sync logic (can compare old vs new state easily)

**Drawbacks**:

- Slightly more memory allocations (mitigated by Kotlin's efficient copy())
- Must use `copy()` for updates instead of direct assignment

**Implementation Considerations**:

- Use default parameter values for optional fields (`description: String? = null`)
- Future Phase 2: Room entities can be mutable (vars), domain models stay immutable
- Mapping layer between mutable entities and immutable models preserves safety

**Decision Rationale**: Prevents entire classes of bugs (unintended mutations). Essential for multi-threaded environment (background workers for lapse detection). Aligns with functional programming best practices and Kotlin idioms.

### Research-Informed Recommendations

**Primary Technology Choices**:

- **kotlinx-datetime 0.7.1**: Use for all date/time fields (Instant, LocalDate, LocalTime) - already available
- **kotlinx-serialization 1.10.0**: Annotate all models with @Serializable for future JSON export - already available
- **Kotlin data classes**: Standard Kotlin pattern for value objects with automatic equals/hashCode/toString/copy
- **Constructor validation**: Use init blocks with require() for enforcing invariants (name length, partial percent range)

**Architecture Approach**:
Pure domain models as immutable Kotlin data classes with validation logic in init blocks. No framework dependencies (no Room annotations in Phase 1). Flat package structure (`model/` not `model/habit/`, `model/routine/`) following project's anti-nesting principle. Models serve as single source of truth for Phase 2 (Room entities) via entity-to-model mapping functions.

**Key Constraints Identified**:

- **UUID serialization**: Requires custom serializer for kotlinx-serialization (serialize as string, deserialize from string)
- **kotlinx-datetime serialization**: Requires adding `kotlinx-serialization-datetime` dependency (not currently in build.gradle.kts)
- **ADHD-first language enforcement**: All error messages must avoid shame language (documented in Context.md)
- **No premature abstraction**: Don't create interfaces or base classes unless 2+ implementations exist (project guideline)
- **Flat package**: All models and enums in `com.kairoshabits.shared.model/` directly, no sub-packages for habit/routine/recovery

---

## Technical Architecture

> **Note**: This section references the detailed research findings above to avoid duplication.

### System Overview

**High-Level Architecture**: Domain models are pure Kotlin data classes representing core business entities (Habit, Completion, Routine, RoutineExecution, RecoverySession) and their enums (HabitPhase, HabitStatus, HabitCategory, CompletionType, SkipReason). Models use kotlinx-datetime for dates/times and are annotated with @Serializable for future JSON serialization. Validation logic lives in init blocks per Constructor Validation Pattern researched above.

**Core Components**:

- **Domain Models**: Five immutable data classes representing business entities with built-in validation
- **Domain Enums**: Five enum classes defining allowed values for status fields and categorical data
- **Validation Logic**: Constructor init blocks enforcing business invariants (name length, partial %, etc.)

**Data Flow**: Models are created → validated → used by future phases:

1. Phase 2 (Database): Room entities will map to/from these models
2. Phase 3 (Repositories): Repositories return Flows of domain models, not entities
3. Phases 4-5 (UI): ViewModels expose domain models, Compose UI renders them
4. Phase 11 (Sync): Models serialize to JSON for network sync

### Android/Kotlin Implementation Details

#### Package Structure

**Directory Layout**:

```
shared/src/main/java/com/kairoshabits/shared/model/
├── Habit.kt                    # Primary domain model
├── Completion.kt              # Completion record model
├── Routine.kt                 # Routine grouping model
├── RoutineExecution.kt        # Routine run instance model
├── RecoverySession.kt         # Lapse/relapse recovery model
├── HabitPhase.kt              # Enum: ONBOARD, FORMING, MAINTAINING, LAPSED, RELAPSED
├── HabitStatus.kt             # Enum: ACTIVE, PAUSED, ARCHIVED
├── HabitCategory.kt           # Enum: MORNING, AFTERNOON, EVENING, ANYTIME
├── CompletionType.kt          # Enum: FULL, PARTIAL, SKIPPED, MISSED
└── SkipReason.kt              # Enum: TOO_TIRED, NO_TIME, TRAVELING, SICK, NEEDED_BREAK, OTHER
```

**Naming Conventions**: Following project style (from research above)

- Files: PascalCase matching primary type (Habit.kt for data class Habit)
- Packages: Lowercase, no underscores (model not model_package)
- One public type per file (Kotlin convention)

**Architectural Decision Rationale**:

- **Flat structure**: All models directly in `model/` package (no `model/habit/`, `model/routine/` sub-packages) per project's anti-nesting guideline
- **Separate files**: Each model and enum in its own file for clarity and git diffing
- **Co-located validation**: Init blocks in same file as data class (no separate validators)

#### Data Model Design

**Model Architecture** (Conceptual - detailed implementation in Steps phase):

```kotlin
// Primary domain model (Habit)
@Serializable
data class Habit(
    val id: UUID,
    val name: String,                    // 1-100 chars, validated in init
    val description: String? = null,
    val anchorBehavior: String,          // Required, non-blank
    val category: HabitCategory,
    val phase: HabitPhase,
    val status: HabitStatus,
    val createdAt: Instant,
    val updatedAt: Instant
    // ... additional fields from docs/design/05-domain-model.md
) {
    init {
        require(name.length in 1..100) {
            "Habit name must be between 1 and 100 characters, was ${name.length}"
        }
        require(anchorBehavior.isNotBlank()) {
            "Habit must have an anchor behavior"
        }
    }
}

// Completion record model
@Serializable
data class Completion(
    val id: UUID,
    val habitId: UUID,
    val date: LocalDate,
    val completedAt: Instant,
    val type: CompletionType,
    val partialPercent: Int? = null,     // 1-99 for PARTIAL, null otherwise
    val skipReason: SkipReason? = null
) {
    init {
        if (type == CompletionType.PARTIAL) {
            requireNotNull(partialPercent) {
                "Partial completion requires a percentage"
            }
            require(partialPercent in 1..99) {
                "Partial completion must be between 1-99%, was $partialPercent"
            }
        }
    }
}

// Example enum
enum class HabitPhase {
    ONBOARD,
    FORMING,
    MAINTAINING,
    LAPSED,
    RELAPSED
}
```

**Field Type Choices** (based on technology research above):

- **IDs**: `UUID` (java.util.UUID) for globally unique identifiers
- **Timestamps**: `Instant` (kotlinx.datetime) for "when this happened"
- **Dates**: `LocalDate` (kotlinx.datetime) for "on this calendar date"
- **Times**: `LocalTime` (kotlinx.datetime) for "at this time of day"
- **Optional fields**: Nullable with default `null` (`description: String? = null`)
- **Collections**: Immutable types (`List<String>` for subtasks, `Set<DayOfWeek>` for active days)

**Decision Rationale**:

- **Immutable data classes**: All properties are `val` per Immutable Domain Models pattern (prevents mutation bugs, thread-safe)
- **Constructor validation**: Init blocks enforce business invariants immediately (impossible to create invalid objects)
- **@Serializable annotation**: Prepares models for JSON export/import in future phases (Phase 10, Phase 11)
- **No Room annotations**: Models are pure business logic, Room entities (Phase 2) will map to/from these

#### Validation Strategy

**Validation Approach**: Constructor validation using init blocks per researched pattern

**Validation Rules** (from docs/design/06-invariants.md):

1. Habit name: 1-100 characters (empty and 101+ rejected)
2. Anchor behavior: Non-blank string (empty rejected)
3. Partial completion: 1-99% when type is PARTIAL (0, 100, null rejected for PARTIAL type)
4. Skip reason: Optional for SKIPPED type, null for other types
5. Timestamps: createdAt <= updatedAt (logical ordering)

**Error Message Pattern**:

```kotlin
// GOOD: Descriptive, no shame language
require(name.length in 1..100) {
    "Habit name must be between 1 and 100 characters, was ${name.length}"
}

// BAD: Shame language (violates ADHD-first principle)
require(name.isNotEmpty()) {
    "You failed to provide a habit name"
}
```

**Decision Rationale**: Validation at construction ensures type safety (if it compiles, it's structurally valid). Init blocks are standard Kotlin pattern, require() provides clear error messages. No separate validator classes needed (anti-ceremony principle).

#### Dependencies & Integration

**Required Dependencies** (already in shared/build.gradle.kts):

- `kotlinx-datetime:0.7.1` - Date/time types
- `kotlinx-serialization-json:1.10.0` - @Serializable support
- `java.util.UUID` - Standard library (no extra dependency)

**New Dependency Needed**:

- `kotlinx-serialization-datetime` - Serializers for Instant/LocalDate/LocalTime (not currently in build.gradle.kts)

**Integration Points**:

- **Phase 2 (Database)**: Room entities will reference these models

  ```kotlin
  // Future Phase 2 entity
  @Entity(tableName = "habits")
  data class HabitEntity(...) {
      fun toModel(): Habit = Habit(...)
  }

  // Extension function for mapping
  fun Habit.toEntity(): HabitEntity = HabitEntity(...)
  ```

- **Phase 3 (Repositories)**: Repositories return `Flow<List<Habit>>` (models, not entities)
- **Phase 4-5 (UI)**: ViewModels expose `StateFlow<List<Habit>>` for Compose UI

**Decision Rationale**: Keep models independent of persistence framework. Mapping layer (Phase 2) converts between entities (mutable, Room-annotated) and models (immutable, pure Kotlin).

#### Platform-Specific Considerations

**Android Implementation**:

- **Minimum SDK**: 28 (Android 9.0 Pie) - set in shared/build.gradle.kts
- **WearOS Compatibility**: Models are in `shared` module, automatically available to `wear` module
- **Device Support**: Phone (app module) and watch (wear module) share same models

**Build Configuration**:

- **Kotlin Version**: 2.3.0 (supports data classes, sealed classes, extensions)
- **Java Target**: Java 17 (supports records, text blocks if needed)
- **KSP**: 2.3.3 for kotlinx-serialization code generation

**Code Quality**:

- **Spotless Formatting**: Auto-formats Kotlin files on save (4-space indentation, import ordering)
- **Detekt Analysis**: Static analysis for code smells (configured in project)
- **ADHD-First Language**: Error messages validated to avoid shame language (skill: /adhd-first-review)

### Implementation Complexity Assessment

**Complexity Level**: **Simple**

This is a foundational phase creating pure Kotlin data classes with no external service dependencies, no UI components, and no database integration. Implementation is straightforward: define data classes, add validation, document with KDoc.

**Implementation Challenges**:

- **Setup and Infrastructure**: None - all dependencies already configured (kotlinx-datetime, kotlinx-serialization)
- **Core Implementation**: Low complexity - standard Kotlin data class patterns with init block validation
- **Integration Points**: None - no existing code to integrate with (greenfield implementation)
- **Testing Requirements**: Low complexity - unit tests for validation logic using JUnit, no mocking needed

**Risk Assessment**:

- **High Risk Areas**: None identified
- **Medium Risk Areas**:
  - Missing `kotlinx-serialization-datetime` dependency (need to add to build.gradle.kts)
  - Forgetting to add @Serializable annotations (caught by Phase 2 when trying to serialize)
  - Using shame language in error messages (mitigated by /adhd-first-review skill)
- **Mitigation Strategies**:
  - Add kotlinx-serialization-datetime dependency before implementing models
  - Create checklist: all models annotated with @Serializable
  - Run /adhd-first-review on all error messages before committing
- **Unknowns**: None - design is fully specified in docs/design/05-domain-model.md

**Dependency Analysis**:

- **External Dependencies**:
  - kotlinx-datetime:0.7.1 (already present)
  - kotlinx-serialization-json:1.10.0 (already present)
  - **NEW**: kotlinx-serialization-datetime (add to libs.versions.toml and build.gradle.kts)
- **Internal Dependencies**: None - no other Kairos code exists yet
- **Breaking Changes**: None - new code only

**Testing Strategy**:

- **Unit Tests**: Test each model's validation logic
  ```kotlin
  @Test
  fun `habit name must be 1-100 characters`() {
      assertThrows<IllegalArgumentException> {
          Habit(name = "", ...) // Empty name rejected
      }
      assertThrows<IllegalArgumentException> {
          Habit(name = "a".repeat(101), ...) // 101 chars rejected
      }
      Habit(name = "a".repeat(100), ...) // 100 chars accepted
  }
  ```
- **Integration Tests**: Not applicable - models have no external integrations
- **UI Tests**: Not applicable - models have no UI
- **Serialization Tests** (optional): Verify JSON round-trip for each model
  ```kotlin
  @Test
  fun `habit serializes and deserializes correctly`() {
      val habit = Habit(...)
      val json = Json.encodeToString(habit)
      val decoded = Json.decodeFromString<Habit>(json)
      assertEquals(habit, decoded)
  }
  ```

### Technical Clarifications

**Areas Requiring Resolution**: None

All technical decisions are clear based on:

- Design specification in docs/design/05-domain-model.md (complete field definitions)
- Business invariants in docs/design/06-invariants.md (validation rules)
- Project guidelines in CLAUDE.md and Context.md (architectural patterns)
- Existing dependencies in shared/build.gradle.kts (no missing critical libraries except kotlinx-serialization-datetime)

**Action Items Before Implementation**:

1. Add `kotlinx-serialization-datetime` dependency to libs.versions.toml and shared/build.gradle.kts
2. Create `shared/src/main/java/com/kairoshabits/shared/model/` directory
3. Review docs/design/05-domain-model.md for complete field definitions before writing each model

---

**Next Phase**: After this technical planning is approved, proceed to `/ctxk:plan:3-steps` for implementation task breakdown.
