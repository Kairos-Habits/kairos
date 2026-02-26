# Architecture Decision Record

# Web + Android + Pi Compose Structure

## 1. Context

Kairos requires:

- A primary mobile client (Android)
- A front-door kiosk (Raspberry Pi)
- A planning and configuration surface (Web)
- Hardware presence integration
- Deterministic sync and eventual consistency

Earlier iterations considered:

- Desktop main application
- Full web kiosk
- WebView-wrapped Android
- Realtime Supabase

These options were evaluated against long-term maintainability.

---

## 2. Decision

The system will use:

- Kotlin Multiplatform as semantic core
- Separate Android application subproject (AGP 9 compliant)
- Compose Desktop kiosk on Raspberry Pi (JVM target)
- SvelteKit web application for planning
- ESP32 firmware node for presence

No desktop “main app.”

---

## 3. Rationale

### 3.1 Kotlin-First Core

Reasons:

- Android + potential WearOS
- Shared contracts
- Shared sync logic
- Deterministic invariants

Avoids duplicating scheduling or sync logic in JavaScript.

---

### 3.2 Separate Android Application Module

Reason:

- AGP 9 compatibility
- Future-proof against deprecation of KMP + com.android.application in same module
- Clean separation of app shell and shared core

---

### 3.3 Compose Desktop for Kiosk

Reasons:

- Reduced UI paradigm switching
- Tight integration with serial + SQLite
- Appliance-style reliability
- No browser dependency

Kiosk is a first-class system node.

---

### 3.4 SvelteKit for Web

Reasons:

- Modern, stable framework
- Strong TypeScript support
- Hosted independently
- Online-first design

Web does not own sync semantics.

---

### 3.5 No Desktop Main Client

Reason:

- Not required for personal use
- Avoids additional packaging surface
- Keeps architecture focused

---

## 4. Alternatives Rejected

### 4.1 All-Web (PWA + Android Wrapper)

Rejected due to:

- Background reliability constraints
- WearOS limitations
- Complex notification handling

---

### 4.2 Vaadin for Main Web

Rejected because:

- Server-driven model unnecessary
- Increased backend complexity
- Less alignment with Kotlin-first mobile path

---

### 4.3 Realtime-Heavy Architecture

Rejected because:

- Eventual consistency sufficient
- Realtime introduces complexity
- Poll-based sync adequate for use case

---

## 5. Consequences

Positive:

- Strong semantic consistency
- Clear module boundaries
- Hardware isolation
- Future WearOS compatibility

Negative:

- Two UI paradigms (Compose + SvelteKit)
- Contract export pipeline required

Tradeoff accepted.

---

## 6. Long-Term Stability

This structure supports:

- Scheduling engine growth
- FCM addition
- BLE integration
- Additional sensor nodes
- Jetson Nano upgrade

No structural rewrites required.

---

## 7. Invariants

- Contracts originate in Kotlin.
- Android is separate subproject.
- Kiosk is JVM target.
- Web consumes generated types.
- Firmware is isolated.
