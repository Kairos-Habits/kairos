# CLAUDE.md

@Context.md

This file provides guidance to Claude Code (claude.ai/code) when working with code in this repository.

## Project Overview

Kairos is an ADHD-optimized habit tracking app for Android and WearOS. The core philosophy: **no streaks, no shame, just sustainable progress**. Recovery is first-class—missed days are data, not failures.

## Build Commands

```bash
# Build
./gradlew :app:assembleDebug          # Phone app
./gradlew :wear:assembleDebug         # WearOS app
./gradlew :shared:build               # Shared library

# Test
./gradlew test                        # All unit tests
./gradlew :app:testDebugUnitTest      # App unit tests only
./gradlew :shared:testDebugUnitTest   # Shared module tests
./gradlew connectedAndroidTest        # Instrumented tests (device required)

# Code quality
./gradlew spotlessCheck               # Verify formatting
./gradlew spotlessApply               # Auto-fix formatting
./gradlew detekt                      # Static analysis
```

## Architecture

**Three-module structure:**
```
app/     → Phone UI (Compose, ViewModels, navigation)
wear/    → WearOS UI (Wear Compose, Tiles)
shared/  → Business logic shared by both (models, database, repositories)
```

Dependency flow: `app` → `shared` ← `wear`

**Tech stack:** Jetpack Compose, Room (KSP), Koin (not Hilt), Coroutines/Flows, kotlinx-serialization, kotlinx-datetime

**Architectural principles:**
- No premature abstraction—add interfaces when 2+ implementations exist
- No UseCase ceremony—logic lives in repositories or ViewModels
- Flat packages—avoid deep nesting
- Entity ↔ Model mapping via extension functions, not mapper classes

## Room Database

Room schemas export to `shared/schemas/` for migration tracking.

KSP configuration in `shared/build.gradle.kts`:
```kotlin
ksp {
    arg("room.schemaLocation", "$projectDir/schemas")
    arg("room.generateKotlin", "true")
}
```

Use the `/migration-helper` skill when evolving database schema.

## ADHD-First Language Rules

**Critical:** All user-facing text must use shame-free language. This is enforced by CI.

| Never Use | Use Instead |
|-----------|-------------|
| streak | rhythm, pattern |
| failure/failed | lapse, pause |
| miss/missed | skip, pause |
| perfect | complete, full |
| off track | paused, taking a break |

Use the `/adhd-first-review` skill when writing UI strings, notifications, or error messages.

## Package Structure

```
com.kairoshabits.{module}/
├── model/       # Domain objects
├── db/          # Room entities, DAOs, database
├── repository/  # Data access layer
├── di/          # Koin modules
├── ui/          # Compose screens, components, theme
├── worker/      # Background workers
└── sync/        # Sync infrastructure (future)
```

## Design Documentation

Comprehensive design docs in `docs/design/`:
- `00-project-overview.md` — Philosophy and principles
- `05-domain-model.md` — Entity definitions and relationships
- `06-invariants.md` — Business rules (including language constraints)
- `07-architecture.md` — System architecture diagrams
- `08-erd.md` — Database schema
- `09-state-machines.md` — Habit, routine, recovery state transitions

Implementation roadmap: `docs/implementation-plan.md` (15 phases)

## Current State

Phase 0-1 complete: Project scaffolding, Koin setup, basic theme. No domain models or database entities implemented yet.
