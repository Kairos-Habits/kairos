# PRD: Cloud Synchronization

## Overview

Cloud synchronization enables multi-device usage (phone, watch, smart mirror, ESP32 devices) while maintaining Kairos's offline-first architecture. The system uses a **self-hosted stack**:

- **PocketBase** — Authentication, OAuth, user management
- **Postgres + PowerSync** — True offline-first data synchronization
- **Mosquitto** — MQTT broker for ESP32 embedded devices

---

## Problem Statement

```mermaid
mindmap
  root((Multi-Device Need))
    Phone Dependency
      Phone causes distraction
      Phone not always accessible
      Watch reduces phone pulls
    Data Continuity
      Device replacement
      Multiple devices
      Backup and restore
    Future Devices
      Smart mirror
      ESP32 buttons
      Home Assistant
```

### Design Constraints

```mermaid
flowchart TB
    subgraph MustHave["Non-Negotiable Requirements"]
        O["Offline-First<br/>All features work without network"]
        L["Local Source of Truth<br/>Local SQLite is authoritative"]
        Z["Zero Data Loss<br/>Sync never overwrites unsynced local"]
    end
    
    subgraph NiceToHave["Desirable Properties"]
        R["Real-Time Updates<br/>Changes appear quickly"]
        S["Self-Hosted<br/>Full data ownership"]
        E["Embedded Support<br/>ESP32 integration"]
    end
    
    MustHave --> NiceToHave
```

---

## Goals

### Primary Goals (P0)

| Goal | Success Criteria |
|------|------------------|
| Offline-first architecture uncompromised | 100% feature parity offline |
| Self-hosted deployment works | Single `docker-compose up` |
| No data loss during sync | Zero local changes lost |
| Multi-device sync (phone + watch) | Changes propagate within 5 seconds |

### Secondary Goals (P1)

| Goal | Success Criteria |
|------|------------------|
| Multi-user support | Household can share server |
| Transparent conflict handling | Last-write-wins with visibility |
| Sync status always visible | Clear indicator in UI |
| ESP32 integration | MQTT bridge to database |

### Tertiary Goals (P2)

| Goal | Success Criteria |
|------|------------------|
| Shared habits (v1.1) | Household members share habits |
| Smart mirror support | Web UI syncs via PowerSync |
| End-to-end encryption | Data encrypted before upload |

---

## Architecture Overview

```mermaid
flowchart TB
    subgraph Clients["Client Devices"]
        Phone["📱 Android Phone"]
        Watch["⌚ WearOS"]
        Mirror["🪞 Smart Mirror<br/>(Web UI)"]
        ESP["🔘 ESP32 Devices"]
    end
    
    subgraph Server["Self-Hosted Server (Docker)"]
        PocketBase["PocketBase<br/>Auth + User Mgmt"]
        PowerSync["PowerSync Service"]
        Postgres[(Postgres)]
        Mosquitto["Mosquitto<br/>MQTT Broker"]
        Bridge["MQTT Bridge"]
        
        PowerSync <--> Postgres
        Bridge <--> Postgres
        Bridge <--> Mosquitto
    end
    
    Phone <-->|"PowerSync SDK"| PowerSync
    Phone <-->|"Auth"| PocketBase
    Watch <--> Phone
    Mirror <-->|"PowerSync JS"| PowerSync
    ESP <-->|"MQTT"| Mosquitto
```

### Component Responsibilities

| Component | Purpose | Technology |
|-----------|---------|------------|
| **PocketBase** | Auth, OAuth, user management, admin UI | Single Go binary |
| **Postgres** | Persistent data storage | PostgreSQL 16 |
| **PowerSync** | Offline-first sync protocol | PowerSync Service |
| **Mosquitto** | ESP32 communication | MQTT broker |
| **MQTT Bridge** | Translates MQTT ↔ Postgres | Small Python/Go service |

---

## User Personas

### Self-Hoster (Primary)

```mermaid
mindmap
  root((Self-Hoster))
    Characteristics
      Runs Proxmox/Docker
      Values data ownership
      Comfortable with CLI
    Needs
      Docker compose deployment
      Full control over data
      Multi-user for household
    Environment
      Home server or VPS
      Tailscale for remote access
```

### Household Member

```mermaid
mindmap
  root((Household Member))
    Characteristics
      Uses shared server
      Not technical
      Just wants app to work
    Needs
      Simple login
      Data syncs automatically
      Works offline
    Concerns
      Privacy from other members
      Easy recovery if forgot password
```

---

## Authentication Flow

PocketBase handles all authentication. PowerSync validates JWTs issued by PocketBase.

```mermaid
sequenceDiagram
    participant App
    participant PocketBase
    participant PowerSync
    participant Postgres
    
    App->>PocketBase: Login (email/OAuth)
    PocketBase-->>App: JWT + Refresh Token
    App->>App: Store tokens securely
    
    App->>PowerSync: Connect with JWT
    PowerSync->>PocketBase: Validate JWT (JWKS)
    PocketBase-->>PowerSync: Valid, user_id = "abc123"
    
    PowerSync->>Postgres: Sync data WHERE user_id = "abc123"
    Postgres-->>PowerSync: User's data only
    PowerSync-->>App: Sync to local SQLite
    
    Note over App: Works offline now
    Note over App: Re-auth only when tokens expire
```

### Token Strategy

| Token | Lifetime | Purpose |
|-------|----------|---------|
| Access Token | 15 minutes | Short-lived, used for API calls |
| Refresh Token | 90 days | Long-lived, used to get new access tokens |

**Offline behavior**: App works fully offline. Tokens only matter when syncing. If refresh token expires while offline, user re-authenticates when back online—no data loss.

---

## Sync Architecture

### PowerSync Data Flow

```mermaid
flowchart LR
    subgraph Client["Android App"]
        LocalDB[(SQLite)]
        SDK["PowerSync SDK"]
        LocalDB <--> SDK
    end
    
    subgraph Server["Server"]
        PS["PowerSync Service"]
        PG[(Postgres)]
        PS <-->|"Logical Replication"| PG
    end
    
    SDK <-->|"Sync Protocol"| PS
```

### How PowerSync Works

1. **Local-first**: App reads/writes to local SQLite
2. **Change tracking**: PowerSync SDK tracks local changes
3. **Sync when online**: Changes pushed to server, server changes pulled
4. **Conflict resolution**: Server applies sync rules
5. **Offline capable**: Full functionality without network

### Sync Rules

```yaml
# powersync.yaml
bucket_definitions:
  user_data:
    parameters:
      - name: user_id
        type: string
    data:
      - table: habits
        filter: "user_id = :user_id"
      - table: completions
        filter: "habit_id IN (SELECT id FROM habits WHERE user_id = :user_id)"
      - table: routines
        filter: "user_id = :user_id"
      - table: routine_habits
        filter: "routine_id IN (SELECT id FROM routines WHERE user_id = :user_id)"
      - table: recovery_sessions
        filter: "habit_id IN (SELECT id FROM habits WHERE user_id = :user_id)"
```

Each user only syncs their own data. The server never sends other users' data to your device.

---

## Use Cases

### UC-1: First-Time Setup

**Actor**: New user with self-hosted server  
**Precondition**: Server running, app installed  
**Trigger**: User opens app for first time

```mermaid
sequenceDiagram
    actor User
    participant App
    participant PocketBase
    participant PowerSync
    
    User->>App: Open app
    App->>User: Welcome screen
    User->>App: Tap "Sign In"
    App->>User: Enter server URL
    User->>App: Enter https://kairos.home.local
    
    App->>PocketBase: Check server
    PocketBase-->>App: Server OK
    
    App->>User: Login options
    User->>App: Create account / OAuth
    App->>PocketBase: Authenticate
    PocketBase-->>App: JWT tokens
    
    App->>PowerSync: Initial sync
    PowerSync-->>App: Empty (new user)
    
    App->>User: Ready! Create your first habit
```

### UC-2: Offline Usage

**Actor**: User without network  
**Precondition**: Previously synced  
**Trigger**: User completes habit while offline

```mermaid
sequenceDiagram
    actor User
    participant App
    participant LocalDB as Local SQLite
    participant PowerSync
    
    Note over App: Network unavailable
    
    User->>App: Complete habit
    App->>LocalDB: INSERT completion
    LocalDB-->>App: Success
    App->>User: ✓ Habit completed
    
    Note over App: Full functionality continues
    
    User->>App: Create new habit
    App->>LocalDB: INSERT habit
    App->>User: ✓ Habit created
    
    Note over App: Later... network returns
    
    App->>PowerSync: Sync pending changes
    PowerSync-->>App: Changes uploaded
    PowerSync-->>App: Pull remote changes
    App->>User: Sync indicator: ✓
```

### UC-3: Multi-Device Sync

**Actor**: User with phone and watch  
**Precondition**: Both devices authenticated  
**Trigger**: Complete habit on watch

```mermaid
sequenceDiagram
    participant Watch
    participant Phone
    participant PowerSync
    participant Postgres
    
    Watch->>Watch: Complete habit (local)
    Watch->>Phone: Relay via Data Layer
    Phone->>PowerSync: Sync completion
    PowerSync->>Postgres: Write completion
    
    Note over Postgres: Change captured
    
    PowerSync-->>Phone: Ack
    Phone->>Phone: Update local state
    
    Note over Phone,Watch: Both devices consistent
```

### UC-4: Household Member Joins

**Actor**: Existing user invites partner  
**Precondition**: Server running, admin account exists  
**Trigger**: Admin creates invite

```mermaid
sequenceDiagram
    actor Admin
    participant PocketBase
    actor Partner
    participant App
    
    Admin->>PocketBase: Create user invite
    PocketBase->>Partner: Email with invite link
    
    Partner->>PocketBase: Click link
    PocketBase->>Partner: Set password form
    Partner->>PocketBase: Create account
    
    Partner->>App: Install app
    Partner->>App: Login with new account
    App->>PocketBase: Authenticate
    PocketBase-->>App: JWT (user_id = "partner123")
    
    App->>App: Sync only partner's data
    Note over App: Cannot see admin's habits
```

---

## Functional Requirements

### FR-1: Authentication

| ID | Requirement | Priority |
|----|-------------|----------|
| FR-1.1 | Email/password authentication | P0 |
| FR-1.2 | OAuth support (Google, Apple) | P1 |
| FR-1.3 | Secure token storage (Keystore) | P0 |
| FR-1.4 | Automatic token refresh | P0 |
| FR-1.5 | Sign out (clears tokens, keeps local data option) | P0 |
| FR-1.6 | Account deletion | P1 |
| FR-1.7 | Password reset flow | P0 |

### FR-2: Sync Operations

| ID | Requirement | Priority |
|----|-------------|----------|
| FR-2.1 | Local SQLite as source of truth | P0 |
| FR-2.2 | Automatic sync when online | P0 |
| FR-2.3 | Manual sync trigger available | P1 |
| FR-2.4 | Incremental sync (not full download) | P0 |
| FR-2.5 | Sync works after extended offline | P0 |
| FR-2.6 | Conflict resolution (last-write-wins) | P0 |

### FR-3: Multi-User

| ID | Requirement | Priority |
|----|-------------|----------|
| FR-3.1 | User data isolation (row-level security) | P0 |
| FR-3.2 | Multiple users per server instance | P0 |
| FR-3.3 | Admin UI for user management | P0 |
| FR-3.4 | User invite flow | P1 |
| FR-3.5 | Household sharing (v1.1) | P2 |

### FR-4: Server Configuration

| ID | Requirement | Priority |
|----|-------------|----------|
| FR-4.1 | User enters server URL | P0 |
| FR-4.2 | Server URL persisted | P0 |
| FR-4.3 | Server health check | P0 |
| FR-4.4 | Clear error on server unreachable | P0 |

---

## Non-Functional Requirements

### Performance

| ID | Requirement | Target |
|----|-------------|--------|
| NFR-P1 | Initial sync (empty → 100 habits) | < 10 seconds |
| NFR-P2 | Incremental sync | < 2 seconds |
| NFR-P3 | Local write latency | < 50ms |
| NFR-P4 | Sync after offline period | < 30 seconds |

### Reliability

| ID | Requirement | Target |
|----|-------------|--------|
| NFR-R1 | Data loss during sync | Zero |
| NFR-R2 | Offline feature parity | 100% |
| NFR-R3 | Sync recovery after crash | Automatic |

### Security

| ID | Requirement | Target |
|----|-------------|--------|
| NFR-S1 | Transport encryption | TLS required |
| NFR-S2 | Token storage | Android Keystore |
| NFR-S3 | Data isolation | Row-level by user_id |
| NFR-S4 | No PII in logs | Enforced |

---

## Deployment

### Docker Compose

```yaml
version: "3.8"

services:
  pocketbase:
    image: ghcr.io/muchobien/pocketbase:latest
    container_name: kairos-auth
    restart: unless-stopped
    ports:
      - "8090:8090"
    volumes:
      - pb_data:/pb_data
    environment:
      - POCKETBASE_ADMIN_EMAIL=${ADMIN_EMAIL}
      - POCKETBASE_ADMIN_PASSWORD=${ADMIN_PASSWORD}

  postgres:
    image: postgres:16-alpine
    container_name: kairos-db
    restart: unless-stopped
    environment:
      POSTGRES_DB: kairos
      POSTGRES_USER: kairos
      POSTGRES_PASSWORD: ${DB_PASSWORD}
    volumes:
      - postgres_data:/var/lib/postgresql/data
      - ./migrations:/docker-entrypoint-initdb.d

  powersync:
    image: journeyapps/powersync-service:latest
    container_name: kairos-sync
    restart: unless-stopped
    ports:
      - "8080:8080"
    environment:
      POWERSYNC_DATABASE_URL: postgresql://kairos:${DB_PASSWORD}@postgres:5432/kairos
      POWERSYNC_JWKS_URI: http://pocketbase:8090/api/.well-known/jwks.json
    depends_on:
      - postgres
      - pocketbase

  mosquitto:
    image: eclipse-mosquitto:2
    container_name: kairos-mqtt
    restart: unless-stopped
    ports:
      - "1883:1883"
    volumes:
      - ./mosquitto.conf:/mosquitto/config/mosquitto.conf
      - mosquitto_data:/mosquitto/data

  mqtt-bridge:
    build: ./mqtt-bridge
    container_name: kairos-mqtt-bridge
    restart: unless-stopped
    environment:
      DATABASE_URL: postgresql://kairos:${DB_PASSWORD}@postgres:5432/kairos
      MQTT_BROKER: mosquitto:1883
    depends_on:
      - postgres
      - mosquitto

volumes:
  pb_data:
  postgres_data:
  mosquitto_data:
```

### Resource Requirements

| Service | RAM | CPU | Storage |
|---------|-----|-----|---------|
| PocketBase | ~50MB | Minimal | ~100MB |
| Postgres | ~100MB | Minimal | Varies |
| PowerSync | ~100MB | Minimal | Minimal |
| Mosquitto | ~10MB | Minimal | Minimal |
| MQTT Bridge | ~50MB | Minimal | Minimal |
| **Total** | ~300MB | < 1 core | < 1GB base |

Runs comfortably on a Raspberry Pi 4, Proxmox LXC, or small VPS.

---

## UI Requirements

### Server Configuration

```
┌─────────────────────────────────────┐
│  Connect to Server                  │
├─────────────────────────────────────┤
│                                     │
│  Server URL                         │
│  ┌─────────────────────────────┐   │
│  │ https://kairos.home.local   │   │
│  └─────────────────────────────┘   │
│                                     │
│  [Test Connection]                  │
│                                     │
│  ✓ Server reachable                │
│                                     │
│  [Continue to Login]                │
│                                     │
└─────────────────────────────────────┘
```

### Sync Status Indicator

| State | Icon | Text | Notes |
|-------|------|------|-------|
| Synced | 🟢 | "Synced" | All changes uploaded |
| Syncing | 🔄 | "Syncing..." | Transfer in progress |
| Pending | 🟡 | "3 pending" | Offline changes queued |
| Offline | 🟡 | "Offline" | No network, working locally |
| Error | 🔴 | "Sync error" | Tap for details |
| Not configured | ⚫ | "Not synced" | No server configured |

### Settings Screen

```
┌─────────────────────────────────────┐
│  Sync & Account                     │
├─────────────────────────────────────┤
│                                     │
│  Status: ✓ Synced                   │
│  Last sync: 2 minutes ago           │
│  Server: kairos.home.local          │
│                                     │
│  Account: you@email.com             │
│                                     │
│  ─────────────────────────────────  │
│                                     │
│  [Sync Now]                         │
│  [Sign Out]                         │
│  [Delete Account]                   │
│                                     │
└─────────────────────────────────────┘
```

---

## Error Handling

| Error | User Message | Recovery |
|-------|--------------|----------|
| Server unreachable | "Can't reach server. Working offline." | Auto-retry, manual sync |
| Auth expired | "Please sign in again" | Login screen |
| Sync conflict | (Silent, last-write-wins) | Automatic |
| Server error | "Sync temporarily unavailable" | Auto-retry |
| Invalid server URL | "Server not found at this address" | Re-enter URL |

---

## Migration Path

### From Local-Only to Synced

```mermaid
sequenceDiagram
    actor User
    participant App
    participant LocalDB
    participant PowerSync
    participant Server
    
    Note over App: User has local data, no sync
    
    User->>App: Configure server + login
    App->>PowerSync: Connect
    
    PowerSync->>Server: Check for existing data
    Server-->>PowerSync: Empty (new user on server)
    
    App->>LocalDB: Read all local data
    App->>PowerSync: Upload local data
    PowerSync->>Server: Write data
    
    Note over App: Local data is now synced
    Note over App: Future changes sync automatically
```

---

## Security Considerations

### Data Isolation

```sql
-- All tables have user_id
-- PowerSync sync rules enforce filtering
-- Users cannot query other users' data

-- Even if someone bypassed the app:
CREATE POLICY user_isolation ON habits
    USING (user_id = current_user_id());
```

### Network Security

| Layer | Protection |
|-------|------------|
| Transport | TLS 1.3 required |
| Authentication | JWT with short expiry |
| Authorization | Row-level security |
| Tokens | Android Keystore storage |

### Self-Hosted Benefits

- Data never leaves your network (unless you choose VPS)
- No third-party access
- Full audit capability
- GDPR compliance simplified (you control data)

---

## Future Considerations

### v1.1: Shared Habits

- Household table and membership
- Shared habit completion modes
- See document: [14-shared-habits.md](./14-shared-habits.md)

### v1.x: Additional Features

- End-to-end encryption (encrypt before sync)
- Selective sync (recent data only on watch)
- Webhook notifications (Home Assistant integration)
- Backup/restore to file
