## 🚨 CRITICAL: This File is Your Progress Tracker

**This Steps.md file serves as the authoritative source of truth for implementation progress across all development sessions.**

### Key Principles

- **Token limits are irrelevant** - Progress is tracked here, sessions are resumable
- **Never rush or take shortcuts** - Each step deserves proper attention and time
- **Session boundaries don't matter** - User can resume where this file shows progress
- **Steps.md is the real todo list** - Even if AI uses TodoWrite during a session, THIS file is what persists
- **Quality over speed** - Thoroughness is mandatory, optimization for token limits is forbidden
- **Check off progress here** - Mark tasks as complete in this file as they're finished

### How This Works

1. Each task has a checkbox: `- [ ] **S001** Task description`
2. As tasks complete, they're marked: `- [x] **S001** Task description`
3. AI ignores token limit concerns and works methodically through steps
4. If context usage gets high (>80%), AI suggests user runs `/compact` before continuing
5. If session ends: User starts new session and resumes (this file has all progress)
6. Take the time needed for each step - there's no rush to finish in one session

---

# Implementation Steps: Domain Models

**Created**: 2026-02-01
**Status**: Implementation Plan
**Prerequisites**: Completed business specification (Spec.md) and technical planning (Tech.md with research and architecture)

## Implementation Phases _(mandatory)_

### Phase 1: Setup & Configuration

_Foundation tasks that must complete before development_

- [ ] **S001** Create model package directory
  - **Path**: `shared/src/main/java/com/kairoshabits/shared/model/`
  - **Dependencies**: None
  - **Notes**: Create directory structure for domain models (directory already exists per codebase analysis)

- [ ] **S002** Add kotlinx-serialization-datetime dependency
  - **Path**: `gradle/libs.versions.toml` and `shared/build.gradle.kts`
  - **Dependencies**: None
  - **Notes**: Add kotlinx-serialization-datetime library for Instant/LocalDate/LocalTime serialization support (identified in tech plan as missing dependency)

**🏁 MILESTONE: Foundation Setup**
_Use Task tool with commit-changes agent to commit: "Setup domain models foundation - package structure and dependencies"_

### Phase 2: Domain Enums

_Define enum types before data classes that use them_

- [ ] **S003** [P] Create HabitPhase enum
  - **Path**: `shared/src/main/java/com/kairoshabits/shared/model/HabitPhase.kt`
  - **Dependencies**: S001
  - **Notes**: Enum with values: ONBOARD, FORMING, MAINTAINING, LAPSED, RELAPSED

- [ ] **S004** [P] Create HabitStatus enum
  - **Path**: `shared/src/main/java/com/kairoshabits/shared/model/HabitStatus.kt`
  - **Dependencies**: S001
  - **Notes**: Enum with values: ACTIVE, PAUSED, ARCHIVED

- [ ] **S005** [P] Create HabitCategory enum
  - **Path**: `shared/src/main/java/com/kairoshabits/shared/model/HabitCategory.kt`
  - **Dependencies**: S001
  - **Notes**: Enum with values: MORNING, AFTERNOON, EVENING, ANYTIME

- [ ] **S006** [P] Create CompletionType enum
  - **Path**: `shared/src/main/java/com/kairoshabits/shared/model/CompletionType.kt`
  - **Dependencies**: S001
  - **Notes**: Enum with values: FULL, PARTIAL, SKIPPED, MISSED

- [ ] **S007** [P] Create SkipReason enum
  - **Path**: `shared/src/main/java/com/kairoshabits/shared/model/SkipReason.kt`
  - **Dependencies**: S001
  - **Notes**: Enum with values: TOO_TIRED, NO_TIME, TRAVELING, SICK, NEEDED_BREAK, OTHER

**🏁 MILESTONE: Enums Complete**
_Use Task tool with commit-changes agent to commit: "Implement domain model enums"_

### Phase 3: Core Domain Models

_Implement data classes with validation logic_

- [ ] **S008** Create Habit data class
  - **Path**: `shared/src/main/java/com/kairoshabits/shared/model/Habit.kt`
  - **Dependencies**: S003, S004, S005
  - **Notes**: Include all fields from docs/design/05-domain-model.md with init block validation (name 1-100 chars, anchorBehavior non-blank). Add @Serializable annotation and KDoc documentation.

- [ ] **S009** Create Completion data class
  - **Path**: `shared/src/main/java/com/kairoshabits/shared/model/Completion.kt`
  - **Dependencies**: S006, S007
  - **Notes**: Include habitId, date, type, partialPercent, skipReason fields with init block validation (partialPercent 1-99 when type is PARTIAL). Add @Serializable annotation and KDoc documentation.

- [ ] **S010** [P] Create Routine data class
  - **Path**: `shared/src/main/java/com/kairoshabits/shared/model/Routine.kt`
  - **Dependencies**: S005 (HabitCategory)
  - **Notes**: Include name, category, status fields with validation. Add @Serializable annotation and KDoc documentation.

- [ ] **S011** [P] Create RoutineExecution data class
  - **Path**: `shared/src/main/java/com/kairoshabits/shared/model/RoutineExecution.kt`
  - **Dependencies**: S001
  - **Notes**: Include routineId, status, currentStep, stepResults fields. Add @Serializable annotation and KDoc documentation.

- [ ] **S012** [P] Create RecoverySession data class
  - **Path**: `shared/src/main/java/com/kairoshabits/shared/model/RecoverySession.kt`
  - **Dependencies**: S001
  - **Notes**: Include habitId, type, status, offeredAt fields. Add @Serializable annotation and KDoc documentation.

**🏁 MILESTONE: Models Complete**
_Use Task tool with commit-changes agent to commit: "Implement domain models with validation"_

### Phase 4: Build Validation & Quality Checks

_Automated testing and build verification_

- [ ] **S013** Run unit tests
  - **Path**: Execute test suite using run-test-suite agent
  - **Dependencies**: S008-S012
  - **Notes**: Use Task tool with run-test-suite agent to execute unit tests and validate all model validation logic works correctly

- [ ] **S014** Validate build success
  - **Path**: Build shared module using build-project agent
  - **Dependencies**: S013
  - **Notes**: Use Task tool with build-project agent to ensure clean compilation with no warnings

- [ ] **S015** [P] Run ADHD-first language review
  - **Path**: Review all error messages and KDoc comments
  - **Dependencies**: S008-S012
  - **Notes**: Run /adhd-first-review skill on all validation error messages to ensure no shame language

- [ ] **S016** [P] Run code modernization check
  - **Path**: Verify kotlinx-datetime usage
  - **Dependencies**: S008-S012
  - **Notes**: Use check-modern-code agent to ensure no java.time or java.util.Date usage

**🏁 MILESTONE: Quality Validated**
_Use Task tool with commit-changes agent to commit: "Validate domain models - build and quality checks passed"_

## AI-Assisted Development Time Estimation _(Claude Code + Human Review)_

> **⚠️ ESTIMATION BASIS**: These estimates assume development with Claude Code (AI) executing implementation tasks with human review and guidance. Times reflect AI execution + human review cycles, not manual coding.

### Phase-by-Phase Review Time

**Setup & Configuration (S001-S002)**: 5-10 minutes

- _AI executes quickly, human reviews dependency addition and directory structure_

**Domain Enums (S003-S007)**: 10-15 minutes

- _AI creates enum files, human reviews naming and values match design spec_

**Core Models (S008-S012)**: 20-30 minutes

- _AI implements data classes with validation, human validates business logic and field completeness against docs/design/05-domain-model.md_

**Build Validation & Quality (S013-S016)**: 10-15 minutes

- _AI runs automated checks, human reviews test output and quality reports_

### Knowledge Gap Risk Factors

**🟢 Low Risk** (Kotlin data classes, kotlinx-datetime): Minimal correction cycles expected

- Well-documented Kotlin standard patterns
- Official kotlinx-datetime documentation available
- No external API dependencies

### Total Estimated Review Time

**Core Development**: 45-70 minutes
**Risk-Adjusted Time**: 50-75 minutes (low risk adjustment ~+10%)
**Manual Testing Allocation**: N/A (no UI or user interaction for domain models)

> **💡 TIME COMPOSITION**:
>
> - AI Implementation: ~15% (Claude Code writes Kotlin data classes quickly)
> - Human Review: ~50% (reading generated code, comparing against design spec)
> - Correction Cycles: ~20% (minor refinements to field types, validation messages)
> - Build Validation: ~15% (reviewing test output, build logs)

## Implementation Structure _(AI guidance)_

### Task Numbering Convention

- **Format**: `S###` with sequential numbering (S001, S002, S003...)
- **Parallel Markers**: `[P]` for tasks that can run concurrently
- **Dependencies**: Clear prerequisite task references
- **File Paths**: Specific target files for each implementation task

### Progress Tracking & Session Continuity

- **This file is the progress tracker** - Check off tasks as `[x]` when complete
- **Sessions are resumable** - New sessions read this file to see what's done
- **Token limits don't matter** - Work can span multiple sessions seamlessly
- **Never rush to completion** - Take the time each step needs for quality
- **TodoWrite is temporary** - Only this file persists across sessions
- **Quality is paramount** - Shortcuts and speed optimizations are forbidden

### Parallel Execution Rules

- **Different files** = `[P]` parallel safe (S003-S007 all create different enum files)
- **Same file modifications** = Sequential only
- **Independent components** = `[P]` parallel safe (S010-S012 are independent models)
- **Shared resources** = Sequential only
- **Dependencies must complete first** = S008 requires S003-S005 (enums), S009 requires S006-S007

### Quality Integration

_Built into implementation phases, not separate agent tasks_

- **Code Standards**: Follow Kotlin conventions and Spotless formatting (auto-applied)
- **Validation Logic**: Use init blocks with require() for business invariants
- **ADHD-First Language**: Error messages must avoid shame language (validated in S015)
- **Documentation**: KDoc comments required for all public data classes and properties
- **Serialization**: @Serializable annotations on all models for future JSON support

## Dependency Analysis _(AI generated)_

### Critical Path

Longest dependency chain: **S001 → S002 → S003-S007 (enums) → S008-S009 (models requiring enums) → S013-S016 (validation)**

**Estimated time on critical path**: 50-75 minutes

- S001: 2 minutes (directory already exists, just verify)
- S002: 5-8 minutes (add dependency to version catalog and build file)
- S003-S007: 10-15 minutes (create 5 enum files in parallel)
- S008-S009: 15-20 minutes (create Habit and Completion with validation)
- S010-S012: Can run parallel with S008-S009 once enums complete
- S013-S016: 10-15 minutes (build and quality validation)

### Parallel Opportunities

**Phase 2 (Enums)**: All 5 enum tasks (S003-S007) can execute concurrently - different files, no shared dependencies

**Phase 3 (Models)**: S010-S012 (Routine, RoutineExecution, RecoverySession) can run in parallel after S001 completes - these don't depend on other models or enum types beyond HabitCategory (S005)

**Phase 4 (Validation)**: S015-S016 can run in parallel - independent validation checks

### Platform Dependencies

**Android SDK**: MinSDK 28 (already configured in shared/build.gradle.kts)
**Kotlin**: 2.3.0 (supports data classes, sealed classes, init blocks)
**Dependencies**: kotlinx-datetime 0.7.1, kotlinx-serialization 1.10.0 (already present), kotlinx-serialization-datetime (needs to be added in S002)

## Completion Verification _(mandatory)_

### Implementation Completeness

- [ ] All user scenarios from Spec.md have corresponding implementation tasks?
- [ ] All architectural components from Tech.md have creation/modification tasks?
- [ ] Error handling and edge cases covered in task breakdown?
- [ ] Performance requirements addressed in implementation plan?
- [ ] Platform-specific requirements integrated throughout phases?

### Quality Standards

- [ ] Each task specifies exact file paths and dependencies?
- [ ] Parallel markers `[P]` applied correctly for independent tasks?
- [ ] Test tasks included for all major implementation components?
- [ ] Code standards and guidelines referenced throughout plan?
- [ ] No implementation details that should be in tech plan?

### Release Readiness

- [ ] Privacy and compliance considerations addressed?
- [ ] Documentation and release preparation tasks included?
- [ ] Feature branch ready for systematic development execution?
- [ ] All milestones defined with appropriate commit guidance?

---

**Next Phase**: After implementation steps are completed, proceed to `/ctxk:impl:start-working` to begin systematic development execution.

---
