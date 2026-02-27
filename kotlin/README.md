# Kairos Kotlin Multiplatform

Kotlin Multiplatform project targeting Android and Desktop (JVM) for the Kairos ADHD-optimized habit building system.

## Module Structure

```
kotlin/
├── shared/       # Domain entities, event schemas, sync logic (pure Kotlin)
├── androidApp/   # Android mobile client
└── kiosk/        # Raspberry Pi desktop kiosk app (JVM)
```

### Dependency Direction

```
:androidApp ──→ :shared
:kiosk ───────→ :shared
:shared ───────→ (no app module dependencies)
```

- `:androidApp` and `:kiosk` may depend on `:shared`, never the reverse
- Shared UI components belong in `:shared` only if both apps will use them
- App shell code (navigation, main activity, window setup) stays in each app module

## Build Commands

### Shared Module

```bash
./gradlew :shared:assemble
```

### Android Application

```bash
./gradlew :androidApp:assembleDebug
```

### Desktop Kiosk (JVM)

```bash
# Build
./gradlew :kiosk:assemble

# Run
./gradlew :kiosk:run
```

### CI Build (catches 95% of breakage)

```bash
./gradlew :shared:assemble :androidApp:assembleDebug :kiosk:assemble
```

## Module Details

### :shared

Pure platform-agnostic Kotlin code:
- Domain entities (Task, ChecklistSession, etc.)
- Event schemas and sourcing
- Sync semantics and merge logic
- Scheduling calculations

### :androidApp

Android mobile client:
- Compose UI for mobile
- Local SQLite database
- Background sync service
- Notifications (future: WearOS support)

### :kiosk

Raspberry Pi desktop app:
- Compose Desktop UI
- Serial port ingestion (ESP32 JSONL protocol)
- Local SQLite database
- Presence-triggered checklist display

---

Learn more about [Kotlin Multiplatform](https://www.jetbrains.com/help/kotlin-multiplatform-dev/get-started.html).
