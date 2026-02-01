# Kairos Documentation

> **ADHD-optimized habit building & tracking system for Android + WearOS**

This directory contains the complete design documentation for Kairos, structured for Spec-Driven Development. All documents include comprehensive Mermaid diagrams for visualization.

---

## Quick Links

| I want to... | Start here |
|--------------|------------|
| Understand the project | [00 - Project Overview](./00-project-overview.md) |
| See what we're building | [01 - Core PRD](./01-prd-core.md) |
| Understand the data model | [05 - Domain Model](./05-domain-model.md) |
| See the database schema | [08 - ERD](./08-erd.md) |
| Understand system design | [07 - Architecture](./07-architecture.md) |
| Review business rules | [06 - Invariants](./06-invariants.md) |

---

## Document Index

### 📋 Product Requirements

| Document | Description | Key Diagrams |
|----------|-------------|--------------|
| [00 - Project Overview](./00-project-overview.md) | Philosophy, ADHD principles, differentiators, success metrics | Mind maps, principle flows |
| [01 - PRD: Core Habits](./01-prd-core.md) | Habit creation, completion tracking, Today screen | Sequence diagrams, UI flows |
| [02 - PRD: Recovery](./02-prd-recovery.md) | Lapse detection, recovery sessions, fresh starts | State machines, flow diagrams |
| [03 - PRD: Routines](./03-prd-routines.md) | Routine runner, variants, timer-led execution | Execution flows, UI layouts |
| [04 - PRD: Sync](./04-prd-sync.md) | Backend-agnostic sync, Firebase, offline queue | Architecture diagrams, sequences |

### 🏗️ Technical Design

| Document | Description | Key Diagrams |
|----------|-------------|--------------|
| [05 - Domain Model](./05-domain-model.md) | Bounded contexts, entities, aggregates, enums | Class diagrams, ERD |
| [06 - Invariants](./06-invariants.md) | Business rules, constraints, ADHD principles | Validation flows |
| [07 - Architecture](./07-architecture.md) | Clean Architecture, layers, modules, data flow | Component diagrams, sequences |
| [08 - ERD](./08-erd.md) | Room schema, Firestore schema, indices, queries | Full ERD, table definitions |
| [09 - State Machines](./09-state-machines.md) | Habit lifecycle, sync status, execution states | State diagrams |

### 🎨 Experience Design

| Document | Description | Key Diagrams |
|----------|-------------|--------------|
| [10 - User Flows](./10-user-flows.md) | Journey maps, screen flows, interactions | Flow diagrams, wireframes |
| [11 - Notification Design](./11-notification-design.md) | Channels, messaging, scheduling | Notification flows |
| [12 - WearOS Design](./12-wearos-design.md) | Tiles, complications, Data Layer sync | Component diagrams |
| [13 - Embedded Integration](./13-embedded-integration.md) | ESP32, MQTT, physical devices | Architecture, data flows |
| [14 - Shared Habits](./14-shared-habits.md) | Household sharing (v1.1) | Data model, UI flows |

---

## Core Concepts

### ADHD-First Design Principles

```
┌─────────────────────────────────────────────────────────────┐
│  1. Executive Function Externalization                      │
│     → System does cognitive work, not the user              │
│                                                             │
│  2. Sustainable Imperfection                                │
│     → Design for the return, not the streak                 │
│                                                             │
│  3. Immediate Dopamine                                      │
│     → Every interaction provides instant feedback           │
│                                                             │
│  4. Context Over Time                                       │
│     → Event-based triggers beat time-based                  │
│                                                             │
│  5. Flexible Structure                                      │
│     → Structured options with escape hatches                │
│                                                             │
│  6. Shame-Free Recovery                                     │
│     → Missed days are data, not failures                    │
│                                                             │
│  7. Built-in Novelty                                        │
│     → Combat boredom with variation                         │
└─────────────────────────────────────────────────────────────┘
```

### What Makes Kairos Different

| ❌ Traditional Apps | ✅ Kairos |
|--------------------|-----------|
| Streaks to maintain | No streaks |
| Points and badges | No gamification |
| "You failed" messaging | Shame-free language |
| Binary completion | Partial always counts |
| Time-based triggers | Context-based anchors |
| Recovery as afterthought | Recovery is first-class |

---

## Architecture Overview

```
┌─────────────────────────────────────────────────────────────┐
│                      Client Layer                           │
│  ┌─────────────┐  ┌─────────────┐  ┌─────────────┐         │
│  │   Android   │  │   WearOS    │  │   Widget    │         │
│  │   (Compose) │  │   (Tiles)   │  │  (Glance)   │         │
│  └─────────────┘  └─────────────┘  └─────────────┘         │
├─────────────────────────────────────────────────────────────┤
│                   Presentation Layer                        │
│         Screens → ViewModels → UI State (StateFlow)         │
├─────────────────────────────────────────────────────────────┤
│                      Domain Layer                           │
│            Use Cases → Domain Models → Interfaces           │
├─────────────────────────────────────────────────────────────┤
│                       Data Layer                            │
│  ┌─────────────┐  ┌─────────────┐  ┌─────────────┐         │
│  │  PowerSync  │  │  DataStore  │  │  PocketBase │         │
│  │  (SQLite)   │  │   (Prefs)   │  │   (Auth)    │         │
│  └─────────────┘  └─────────────┘  └─────────────┘         │
├─────────────────────────────────────────────────────────────┤
│                Self-Hosted Server (Docker)                  │
│  ┌──────────┐ ┌──────────┐ ┌──────────┐ ┌──────────┐       │
│  │PocketBase│ │ Postgres │ │PowerSync │ │Mosquitto │       │
│  │  (Auth)  │ │  (Data)  │ │  (Sync)  │ │  (MQTT)  │       │
│  └──────────┘ └──────────┘ └──────────┘ └──────────┘       │
└─────────────────────────────────────────────────────────────┘
```

---

## Entity Relationships

```
┌──────────┐       ┌────────────┐       ┌─────────────────┐
│  Habit   │──────<│ Completion │       │ RecoverySession │
└──────────┘       └────────────┘       └─────────────────┘
     │                                          │
     └──────────────────────────────────────────┘

┌──────────┐       ┌──────────────┐       ┌───────────────────┐
│ Routine  │──────<│ RoutineHabit │>──────│      Habit        │
└──────────┘       └──────────────┘       └───────────────────┘
     │
     └────────────<│ RoutineExecution │
```

---

## Habit Lifecycle

```
                    ┌─────────┐
         ┌─────────│ ONBOARD │─────────┐
         │         └─────────┘         │
         │              │              │
         │         Week 2+             │
         │              ▼              │
         │         ┌─────────┐         │
         │    ┌────│ FORMING │────┐    │
         │    │    └─────────┘    │    │
         │    │         │         │    │
         │    │    Week 16+       │    │
         │    │         ▼         │    │
         │    │  ┌─────────────┐  │    │
         │    │  │ MAINTAINING │  │    │
         │    │  └─────────────┘  │    │
         │    │         │         │    │
         │    │    3+ days missed │    │
         │    │         ▼         │    │
         │    │    ┌────────┐     │    │
         │    └───>│ LAPSED │<────┘    │
         │         └────────┘          │
         │              │              │
         │         7+ days             │
         │              ▼              │
         │        ┌──────────┐         │
         │        │ RELAPSED │         │
         │        └──────────┘         │
         │              │              │
         └──────────────┴──────────────┘
                  Fresh Start
```

---

## Reading Order

### For Product Understanding
1. [00 - Project Overview](./00-project-overview.md) — Start here
2. [01 - PRD: Core Habits](./01-prd-core.md)
3. [02 - PRD: Recovery](./02-prd-recovery.md)
4. [10 - User Flows](./10-user-flows.md)

### For Technical Implementation
1. [05 - Domain Model](./05-domain-model.md)
2. [06 - Invariants](./06-invariants.md)
3. [08 - ERD](./08-erd.md)
4. [07 - Architecture](./07-architecture.md)
5. [09 - State Machines](./09-state-machines.md)

### For Feature Development
| Feature | Documents |
|---------|-----------|
| Habit CRUD | 01, 05, 08 |
| Completion tracking | 01, 05, 09 |
| Recovery system | 02, 06, 09, 11 |
| Routines | 03, 05, 08, 09 |
| Cloud sync | 04, 07, 08 |
| Notifications | 11 |
| WearOS | 12 |
| ESP32 / Physical devices | 13 |
| Shared habits (v1.1) | 14, 08 |

---

## Diagram Types Used

All documents use [Mermaid](https://mermaid.js.org/) diagrams:

| Type | Used For |
|------|----------|
| `flowchart` | Processes, UI flows, architecture |
| `sequenceDiagram` | Use cases, API calls, interactions |
| `stateDiagram-v2` | Lifecycles, state machines |
| `erDiagram` | Database schema, relationships |
| `classDiagram` | Domain models, interfaces |
| `mindmap` | Concept exploration, hierarchies |

---

## Contributing

When updating documentation:

1. **Keep diagrams current** — Update Mermaid diagrams when logic changes
2. **Maintain invariants** — Document 06 is the source of truth for rules
3. **Shame-free language** — Review all user-facing text against messaging guidelines
4. **Version documents** — Note significant changes in document headers

---

## Tech Stack Reference

| Layer | Technology |
|-------|------------|
| UI | Jetpack Compose, Wear Compose |
| State | StateFlow, ViewModel |
| Database | Room (SQLite) via PowerSync SDK |
| Preferences | DataStore |
| Background | WorkManager |
| DI | Hilt |
| Auth | PocketBase |
| Sync | PowerSync + Postgres |
| Embedded | Mosquitto (MQTT) |
| Watch Sync | Wear Data Layer API |

---

*Last updated: January 2026*
