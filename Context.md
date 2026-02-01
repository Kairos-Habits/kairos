# Project Context: Kairos

## Project Overview

- **Version**: ContextKit 0.2.0
- **Setup Date**: 2026-02-01
- **Components**: 3 modules discovered and analyzed (app, wear, shared)
- **Workspace**: None (standalone project)
- **Primary Tech Stack**: Kotlin, Jetpack Compose, Android, Room, Koin
- **Development Guidelines**: None (no Kotlin/Android guidelines available in ContextKit templates)

## Component Architecture

**Project Structure**:

```
📁 Kairos
├── 📱 app (Android Application) - Phone UI with Compose, ViewModels, navigation - Kotlin/Compose - ./app
├── ⌚ wear (Android Application) - WearOS UI with Wear Compose, Tiles - Kotlin/Wear Compose - ./wear
└── 📦 shared (Android Library) - Business logic, models, Room database, repositories - Kotlin - ./shared
```

**Component Summary**:
- **3 Kotlin/Android components** - Kotlin 2.3.0, Gradle 9.3.1, AGP 9.0.0
- **Dependency flow**: `app` → `shared` ← `wear`
- **Key frameworks**: Jetpack Compose, Room 2.8.4, Koin 4.1.1, kotlinx-coroutines 1.10.2

---

## Component Details

### app - Phone Application

**Location**: `./app`
**Purpose**: Android phone UI with Compose, ViewModels, and navigation
**Tech Stack**: Kotlin, Jetpack Compose, Material3, Koin

**File Structure**:
```
app/
├── src/
│   ├── main/java/com/kairoshabits/kairos/
│   │   ├── ui/theme/           # Compose theme (Color, Type, Theme)
│   │   ├── KairosApp.kt        # Application class
│   │   └── MainActivity.kt     # Main entry point
│   ├── test/                   # Unit tests
│   └── androidTest/            # Instrumented tests
└── build.gradle.kts
```

**Dependencies** (from build.gradle.kts):
- `androidx.compose.*` - Jetpack Compose UI framework
- `koin-android`, `koin-compose` - Dependency injection
- `timber` - Logging
- `project(":shared")` - Shared business logic

**Development Commands**:
```bash
# Build (validated during setup)
./gradlew :app:assembleDebug

# Test (validated during setup)
./gradlew :app:testDebugUnitTest

# Instrumented tests (requires device/emulator)
./gradlew :app:connectedAndroidTest
```

**Code Style** (detected):
- Spotless auto-formatting for Kotlin files (via PostToolUse hook)
- 4-space indentation
- Kotlin 2.3.0 with Java 17 compatibility

---

### wear - WearOS Application

**Location**: `./wear`
**Purpose**: WearOS companion app with Wear Compose and Tiles support
**Tech Stack**: Kotlin, Wear Compose, Tiles API

**File Structure**:
```
wear/
├── src/main/java/com/kairoshabits/wear/
│   └── presentation/
│       ├── MainActivity.kt     # Main WearOS entry point
│       └── theme/Theme.kt      # Wear Compose theme
└── build.gradle.kts
```

**Dependencies** (from build.gradle.kts):
- `androidx.wear.compose.*` - Wear Compose components
- `play-services-wearable` - Phone-watch communication
- `core-splashscreen` - Splash screen API
- `project(":shared")` - Shared business logic

**Development Commands**:
```bash
# Build (validated during setup)
./gradlew :wear:assembleDebug

# Test (if tests exist)
./gradlew :wear:testDebugUnitTest
```

**Code Style** (detected):
- Same as app module - Spotless formatting
- Kotlin 2.3.0 with Java 17 compatibility
- MinSDK 30 (WearOS 3.0+)

---

### shared - Shared Library

**Location**: `./shared`
**Purpose**: Business logic shared between phone and watch apps (models, database, repositories)
**Tech Stack**: Kotlin, Room (KSP), Koin, kotlinx-serialization, kotlinx-datetime

**File Structure**:
```
shared/
├── src/
│   ├── main/java/com/kairoshabits/shared/
│   │   ├── model/       # Domain objects
│   │   ├── db/          # Room entities, DAOs, database
│   │   ├── repository/  # Data access layer
│   │   └── di/          # Koin modules
│   ├── test/            # Unit tests
│   └── androidTest/     # Instrumented tests
├── schemas/             # Room schema exports for migrations
└── build.gradle.kts
```

**Dependencies** (from build.gradle.kts):
- `room-runtime`, `room-ktx`, `room-compiler` (KSP) - Local database
- `koin-core`, `koin-android` - Dependency injection
- `kotlinx-coroutines-*` - Async operations
- `kotlinx-serialization-json` - JSON serialization
- `kotlinx-datetime` - Date/time handling
- `datastore-preferences` - Preferences storage
- `timber` - Logging

**Development Commands**:
```bash
# Build (validated during setup)
./gradlew :shared:build

# Test (validated during setup)
./gradlew :shared:testDebugUnitTest

# Instrumented tests (requires device/emulator)
./gradlew :shared:connectedAndroidTest
```

**Room Configuration**:
```kotlin
ksp {
    arg("room.schemaLocation", "$projectDir/schemas")
    arg("room.generateKotlin", "true")
}
```

**Code Style** (detected):
- Same as app module - Spotless formatting
- Kotlin 2.3.0 with Java 17 compatibility
- MinSDK 28

---

## Development Environment

**Requirements** (from analysis):
- JDK 17
- Android SDK 36 (API 36 / compileSdk)
- Gradle 9.3.1

**Build Tools** (detected):
- Gradle 9.3.1 with Kotlin DSL
- Android Gradle Plugin 9.0.0
- KSP 2.3.3 (for Room code generation)

**Formatters** (configured):
- Spotless with automatic formatting on file edit
- Detekt for static analysis

**All-Module Commands**:
```bash
# Build all
./gradlew assembleDebug

# Test all
./gradlew test

# Code quality
./gradlew spotlessCheck       # Verify formatting
./gradlew spotlessApply       # Auto-fix formatting
./gradlew detekt              # Static analysis
```

## Development Guidelines

**Applied Guidelines**: None (no Kotlin/Android guidelines available in ContextKit templates)

**Project-Specific Guidelines** (from CLAUDE.md):
- No premature abstraction—add interfaces when 2+ implementations exist
- No UseCase ceremony—logic lives in repositories or ViewModels
- Flat packages—avoid deep nesting
- Entity ↔ Model mapping via extension functions, not mapper classes

**ADHD-First Language Rules** (critical):
| Never Use | Use Instead |
|-----------|-------------|
| streak | rhythm, pattern |
| failure/failed | lapse, pause |
| miss/missed | skip, pause |
| perfect | complete, full |
| off track | paused, taking a break |

## Constitutional Principles

**Core Principles**:
- ✅ Accessibility-first design (UI supports all assistive technologies)
- ✅ Privacy by design (minimal data collection, explicit consent)
- ✅ Localizability from day one (externalized strings, cultural adaptation)
- ✅ Code maintainability (readable, testable, documented code)
- ✅ Platform-appropriate UX (Material Design 3 for Android, Wear guidelines for WearOS)

**Project-Specific Principles** (ADHD-First):
- ✅ Shame-free language (no streaks, no failure framing)
- ✅ Recovery as first-class feature (missed days are data, not failures)
- ✅ Partial completion always counts
- ✅ Context over time (event-based triggers preferred)
- ✅ No gamification (no points, badges, leaderboards)

**Workspace Inheritance**: None - using project defaults

## ContextKit Workflow

**Systematic Feature Development**:
- `/ctxk:plan:1-spec` - Create business requirements specification (prompts interactively)
- `/ctxk:plan:2-research-tech` - Define technical research, architecture and implementation approach
- `/ctxk:plan:3-steps` - Break down into executable implementation tasks

**Development Execution**:
- `/ctxk:impl:start-working` - Continue development within feature branch (requires completed planning phases)
- `/ctxk:impl:commit-changes` - Auto-format code and commit with intelligent messages

**Quality Assurance**: Automated agents validate code quality during development
**Project Management**: All validated build/test commands documented above for immediate use

## Development Automation

**Quality Agents Available**:
- `build-project` - Execute builds with constitutional compliance validation
- `check-accessibility` - TalkBack, contrast, keyboard navigation validation
- `check-localization` - String externalization and cultural adaptation validation
- `check-error-handling` - Kotlin Result patterns and coroutine error handling
- `check-modern-code` - API modernization (kotlinx-datetime, coroutines)
- `check-code-debt` - Technical debt cleanup and AI artifact removal

**Project-Specific Skills**:
- `/migration-helper` - Use when evolving Room database schema
- `/adhd-first-review` - Use when writing UI strings, notifications, or error messages

## Configuration Hierarchy

**Inheritance**: **This Project** (standalone)

**This Project Inherits From**:
- **Workspace**: None (standalone project)
- **Project**: Component-specific configurations documented above

**Override Precedence**: Project component settings override defaults

---
*Generated by ContextKit with comprehensive component analysis. Manual edits preserved during updates.*
