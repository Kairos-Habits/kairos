# Kairos

> ADHD-optimized habit building & tracking for Android + WearOS

Kairos is designed for how ADHD brains actually work: no streaks, no shame, just sustainable progress.

## Philosophy

- **No streaks** — Broken streaks trigger shame spirals. We don't have them.
- **Partial completion counts** — Did 50%? That counts. Always.
- **Recovery is first-class** — Missed days are data, not failures.
- **Context over time** — "After I brush my teeth" beats "at 7:00 AM".
- **No gamification** — No points, badges, or leaderboards. Just habits.

## Project Structure

```
kairos/
├── app/                 # Android phone app
├── wear/                # WearOS app
├── core/                # Shared business logic (use cases, workers)
├── domain/              # Pure Kotlin domain models
├── data/                # Room database, repositories
├── ui-shared/           # Shared Compose components & theme
├── sync-api/            # Sync provider interface
├── sync-powersync/      # PowerSync implementation
├── build-logic/         # Gradle convention plugins
└── docs/                # Design documentation
```

## Tech Stack

| Layer | Technology |
|-------|------------|
| UI | Jetpack Compose |
| Architecture | Clean Architecture + MVI |
| DI | Hilt |
| Database | Room (via PowerSync SDK) |
| Sync | PowerSync + Postgres |
| Auth | PocketBase |
| Background | WorkManager |
| Watch | Wear Compose + Tiles |

## Getting Started

### Prerequisites

- Android Studio Ladybug (2024.2.1) or newer
- JDK 17
- Android SDK 35

### Build

```bash
# Clone the repo
git clone https://github.com/yourusername/kairos.git
cd kairos

# Build debug APK
./gradlew :app:assembleDebug

# Build WearOS APK
./gradlew :wear:assembleDebug

# Run all tests
./gradlew test
```

### Server Setup

Kairos requires a self-hosted backend. See [kairos-server](https://github.com/yourusername/kairos-server) for Docker Compose setup with:
- PocketBase (auth)
- Postgres (data)
- PowerSync (sync)
- Mosquitto (MQTT for ESP32)

## Documentation

See [`docs/`](./docs/) for comprehensive design documentation:

- [Project Overview](./docs/design/00-project-overview.md)
- [Core PRD](./docs/design/01-prd-core.md)
- [Recovery System](./docs/design/02-prd-recovery.md)
- [Architecture](./docs/design/07-architecture.md)
- [Full documentation index](./docs/design/README.md)

## Related Repositories

| Repo | Description |
|------|-------------|
| `kairos` | This repo — Android app |
| `kairos-server` | Docker Compose deployment |
| `kairos-embedded` | ESP32 firmware for buttons/lights |
| `kairos-schemas` | Shared Protobuf schemas |

## License

[MIT](LICENSE)
