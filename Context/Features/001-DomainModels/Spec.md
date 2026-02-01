# Feature Specification: Domain Models

**Feature Branch**: `feature/001-domain-models`
**Created**: 2026-02-01
**Status**: Draft
**Input**:
"""
phase 1 of @docs/implementation-plan.md
"""

## User Scenarios & Testing _(mandatory)_

### Primary User Story

As a **Kairos app developer**, I want to **define foundational domain models as Kotlin data classes** so that **both phone and WearOS apps have a consistent, type-safe representation of core business entities**.

**Platform Context**:

- **Multi-platform**: Models live in the `shared` module and are used by both the `app` (Android phone) and `wear` (WearOS) modules
- **User Experience**: These models are not directly visible to end users but enable consistent behavior across all app surfaces
- **Data Handling**: Models serve as the single source of truth for business logic before database persistence layer is added in Phase 2

### Acceptance Scenarios

1. **Given** a developer needs to create a Habit model, **When** they instantiate a Habit data class with valid parameters, **Then** the object is created successfully with all fields properly initialized
   - **Happy Path**: Create habit with name "Morning meditation", anchor "After brushing teeth", category MORNING, phase ONBOARD, status ACTIVE
   - **Error Path**: Attempt to create habit with empty name throws validation error in init block
   - **Edge Cases**: Habit name at exactly 100 characters is accepted, 101 characters is rejected

2. **Given** a developer needs to create a Completion record, **When** they instantiate a Completion for a PARTIAL type, **Then** the partialPercent field is validated to be between 1-99
   - **Happy Path**: Completion with type=PARTIAL and partialPercent=50 is valid
   - **Error Path**: Completion with type=PARTIAL and partialPercent=0 or 100 throws validation error

3. **Given** a developer needs domain enums, **When** they reference HabitPhase, HabitStatus, HabitCategory, CompletionType, or SkipReason, **Then** all expected enum values are available
   - **Happy Path**: HabitPhase.ONBOARD, HabitPhase.FORMING, HabitPhase.MAINTAINING, HabitPhase.LAPSED, HabitPhase.RELAPSED all exist
   - **Edge Cases**: Enum serialization to/from string works for all values

### Edge Cases

- **Validation boundary conditions**: Name length at exactly 1 char (minimum) and 100 chars (maximum), partial completion at 1% and 99%
- **Null vs empty handling**: Optional fields (description, icon, microVersion) can be null but not empty strings
- **Enum exhaustiveness**: Pattern matching on enums must be exhaustive in when expressions
- **Timestamp precision**: kotlinx-datetime Instant precision is consistent across all platforms
- **Collection constraints**: activeDays set for CUSTOM frequency, subtasks list ordering preserved

## Requirements _(mandatory)_

### Functional Requirements

- **FR-001**: All core domain models (Habit, Completion, Routine, RoutineExecution, RecoverySession) MUST be defined as Kotlin data classes in `shared/src/main/kotlin/com/kairoshabits/shared/model/`

- **FR-002**: All domain enums (HabitPhase, HabitStatus, HabitCategory, CompletionType, SkipReason) MUST be defined as Kotlin enum classes in `shared/src/main/kotlin/com/kairoshabits/shared/model/`

- **FR-003**: Habit model MUST enforce validation rules in its constructor: name length 1-100 characters, lapse threshold < relapse threshold (when those fields are added)

- **FR-004**: Completion model MUST enforce that partialPercent is between 1-99 when type is PARTIAL

- **FR-005**: All models MUST use kotlinx-datetime types (Instant, LocalDate, LocalTime) instead of java.time or java.util.Date for cross-platform compatibility

- **FR-006**: All models MUST use UUID (java.util.UUID or kotlin equivalent) for id fields to prepare for multi-device sync

- **FR-007**: Models MUST NOT contain any persistence logic, database annotations, or framework-specific code - they are pure Kotlin data classes representing business concepts

- **FR-008**: Validation errors MUST be thrown as IllegalArgumentException with descriptive messages following ADHD-first language rules (no shame language)

## Scope Boundaries _(mandatory)_

- **IN SCOPE**:
  - Habit data class with all fields from docs/design/05-domain-model.md (name, description, anchorBehavior, category, phase, status, etc.)
  - Completion data class with habitId, date, type, partialPercent, skipReason fields
  - Routine data class with name, category, status fields
  - RoutineExecution data class with routineId, status, currentStep, stepResults fields
  - RecoverySession data class with habitId, type, status, offeredAt fields
  - All required enums: HabitPhase (5 values), HabitStatus (3 values), HabitCategory (4 values), CompletionType (4 values), SkipReason (6 values)
  - Constructor validation for name length, partial completion percentage
  - KDoc documentation for all public models and fields

- **OUT OF SCOPE**:
  - Room database entities or @Entity annotations (Phase 2)
  - Repository classes or data access logic (Phase 3)
  - ViewModel classes or UI state (Phases 4-5)
  - Mapping functions between models and entities (Phase 2)
  - Koin dependency injection setup for models (not needed - models are data classes)
  - Network sync models or PowerSync integration (Phase 11)
  - User preferences or settings models (Phase 10)
  - Notification-related models (Phase 9)
  - Advanced validation like lapse/relapse threshold ordering (can be added when those fields are finalized)

---
