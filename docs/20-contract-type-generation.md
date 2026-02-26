# Kairos – Contract and Type Generation Strategy

## 1. Purpose

This document defines how domain contracts defined in Kotlin
are exported for use in the SvelteKit web application.

Goal:

- Kotlin is the source of truth.
- Web types are generated, not hand-written.
- Drift between Android, kiosk, and web is prevented.

---

## 2. Source of Truth

All domain models originate in:

    :shared (Kotlin Multiplatform module)

Specifically:

- Task
- ChecklistSession
- ChecklistCompletion
- PresenceEvent
- Sync metadata structures
- Invariants

These are defined in commonMain.

---

## 3. Export Strategy

Export flow:

    Kotlin Contracts
          ↓
    Schema Export Tool (Gradle task)
          ↓
    JSON Schema / OpenAPI / DTO snapshot
          ↓
    TypeScript type generation
          ↓
    web/generated/

The web app imports from web/generated only.

Manual duplication is not allowed.

---

## 4. Generation Options

Possible approaches:

1. Kotlin serialization → JSON Schema → TS types
2. KSP-based schema extractor
3. OpenAPI generation via server contract definitions
4. Manual JSON schema maintained alongside Kotlin

Recommended:

- Use Kotlin serialization as canonical definition.
- Generate JSON schema.
- Generate TS types from schema.

---

## 5. CI Enforcement

CI must:

- Run contract export task
- Regenerate TypeScript types
- Fail build if generated files differ from committed files

This guarantees no silent drift.

---

## 6. Web Usage Pattern

In SvelteKit:

- Import types from web/generated/
- Use them for:
      API responses
      Form validation
      Task editing
      Analytics display

Web must not define its own parallel types.

---

## 7. Versioning Strategy

Contracts may evolve.

Rules:

- Additive changes allowed.
- Removal requires migration strategy.
- Breaking changes must increment schema version.
- Sync logic must remain backward-compatible during migration.

---

## 8. Invariants Enforcement

Important invariants must live in Kotlin.

Examples:

- ChecklistCompletion must reference valid session.
- Mode must be LEAVING or ARRIVING.
- Event timestamps must be monotonic per device.

Web should validate UI input,
but authoritative validation remains server-side.

---

## 9. Long-Term Extensions

Future expansions may include:

- Scheduling DTOs
- Analytics aggregates
- Notification payload types
- Device metadata types

All follow same export flow.

---

## 10. Architectural Principle

Types flow outward from Kotlin.

Never inward from Web.

Android and Pi use contracts directly.
Web consumes generated derivatives.
