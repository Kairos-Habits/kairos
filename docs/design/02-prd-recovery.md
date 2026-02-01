# PRD: Recovery System

## Overview

Recovery is a **first-class feature** in Kairos, not an afterthought. This document defines the requirements for lapse detection, recovery sessions, and the relapse handling system that helps users return to habits without shame.

---

## Problem Statement

```mermaid
mindmap
  root((Why Recovery Matters))
    ADHD Reality
      72% failure rate with solo tracking
      Habits rarely become fully automatic
      Cycling between engagement is normal
    Traditional App Failure
      Broken streak = shame spiral
      No path back after missing days
      User abandons app entirely
    Kairos Approach
      Missing days are data
      Early intervention prevents abandonment
      Coming back IS the skill being developed
```

### The Shame Spiral

```mermaid
flowchart TB
    Miss["Miss 1 Day"] --> Feel["Feel Bad"]
    Feel --> Avoid["Avoid App"]
    Avoid --> Miss2["Miss More Days"]
    Miss2 --> Shame["Deep Shame"]
    Shame --> Quit["Abandon App"]
    Quit --> Worse["Back to Square One"]
    
    style Shame fill:#ff6b6b,color:#fff
    style Quit fill:#ff6b6b,color:#fff
```

### The Recovery Loop (Kairos)

```mermaid
flowchart TB
    Miss["Miss 1 Day"] --> Silent["Day 1: Silent"]
    Silent --> Miss2["Miss Day 2"]
    Miss2 --> Soft["Soft Check-in"]
    Soft --> Miss3["Miss Day 3"]
    Miss3 --> Prompt["Gentle Prompt"]
    Prompt --> Decision{"User Choice"}
    
    Decision --> Return["Quick Return"]
    Decision --> Pause["Pause Habit"]
    Decision --> Simplify["Simplify Habit"]
    
    Return --> Forming["Back to Tracking"]
    Pause --> Later["Resume When Ready"]
    Simplify --> Micro["Try Micro Version"]
    
    style Prompt fill:#4ecdc4,color:#fff
    style Return fill:#95e1d3,color:#000
```

---

## Goals

### Primary Goals (P0)

| Goal | Success Criteria |
|------|------------------|
| Detect lapses early (3 days) | 100% detection within 24h of threshold |
| Provide shame-free recovery path | Zero blame language in all prompts |
| Enable quick return without friction | 1 tap to resume tracking |
| Support intentional pausing | Pause option available at all recovery points |

### Secondary Goals (P1)

| Goal | Success Criteria |
|------|------------------|
| Identify blockers for pattern analysis | Blocker selection offered at recovery |
| Suggest contextual adjustments | AI-free rule-based suggestions |
| Detect relapse (7+ days) | Deeper intervention triggered |
| Track recovery success rate | Metric available for user insight |

### Tertiary Goals (P2)

| Goal | Success Criteria |
|------|------------------|
| Fresh start alignment | Offer restart on Mondays/month starts |
| Habit simplification suggestions | Micro-version prompts when struggling |
| Energy pattern correlation | Connect low energy to lapse patterns |

---

## Key Concepts

### Lapse vs. Relapse

```mermaid
flowchart LR
    subgraph Lapse["Lapse (1-6 days)"]
        L1["Day 1: Silent"]
        L2["Day 2: Soft nudge"]
        L3["Day 3: Recovery prompt"]
        L4["Days 4-6: Continued support"]
    end
    
    subgraph Relapse["Relapse (7+ days)"]
        R1["Day 7: Deeper reflection"]
        R2["Welcome back flow"]
        R3["Adjustment options"]
    end
    
    Lapse --> |"User returns"| Resolved["✓ Resolved"]
    Lapse --> |"7 days reached"| Relapse
    Relapse --> |"User returns"| FreshStart["Fresh Start"]
    Relapse --> |"User chooses"| Paused["Paused/Archived"]
```

| Term | Definition | Threshold | Response |
|------|------------|-----------|----------|
| **Lapse** | Short break from habit | 1-6 consecutive missed days | Gentle prompts, easy return |
| **Relapse** | Extended break from habit | 7+ consecutive missed days | Deeper reflection, reset option |
| **Recovery** | Process of returning | N/A | Structured support flow |
| **Fresh Start** | Clean slate restart | After relapse | Phase resets to FORMING |

### Blockers

Predefined reasons why habits get missed—used for pattern analysis, not judgment.

```mermaid
mindmap
  root((Blocker Categories))
    Energy
      Too tired
      Feeling unwell
      Low motivation day
    Time
      Schedule disrupted
      Ran out of time
      Unexpected obligation
    Context
      Routine changed
      Traveling
      Environment different
    Mental
      Overwhelmed
      Forgot despite reminder
      Couldn't start
    Intentional
      Needed a break
      Deprioritized deliberately
      Skip reason applies
```

---

## Use Cases

### UC-1: Lapse Detection (Day 3)

**Actor**: System (background)  
**Precondition**: Habit has 3 consecutive missed days  
**Trigger**: Daily lapse detection worker runs

```mermaid
sequenceDiagram
    participant Worker as Lapse Worker
    participant DB as Database
    participant Notif as Notification
    participant User
    
    Worker->>DB: Query habits with status ACTIVE
    DB-->>Worker: Return active habits
    
    loop Each Habit
        Worker->>DB: Count consecutive missed days
        DB-->>Worker: Return count
        
        alt count >= lapseThreshold (default 3)
            Worker->>DB: Update habit phase to LAPSED
            Worker->>DB: Create RecoverySession (pending)
            Worker->>Notif: Schedule gentle notification
            Notif->>User: "Take medication is waiting.<br/>Tap when ready to check in."
        end
    end
```

**Postcondition**: Habit phase updated, recovery session created  
**Messaging Example**:  
> "Take medication is waiting for you. No rush—tap when you're ready to check in."

### UC-2: Recovery Session (Quick Return)

**Actor**: User  
**Precondition**: Lapse detected, user taps notification or habit  
**Trigger**: User engages with lapsed habit

```mermaid
sequenceDiagram
    actor User
    participant App
    participant DB as Database
    
    User->>App: Tap lapsed habit / notification
    App->>User: Show recovery welcome
    Note over App: "Welcome back! Let's figure this out."
    
    App->>User: Show blocker selection (optional)
    User->>App: Select blocker or skip
    
    App->>User: Show options
    Note over App: Resume / Simplify / Pause / Archive
    
    alt Resume Now
        User->>App: Tap "Resume Tracking"
        App->>DB: Update phase to FORMING
        App->>DB: Complete RecoverySession
        App->>User: Show encouraging confirmation
        App->>User: Navigate to Today screen
    else Simplify
        User->>App: Tap "Try Smaller Version"
        App->>User: Show micro-version option
        User->>App: Confirm simplification
        App->>DB: Update habit with micro-version
        App->>DB: Update phase to FORMING
        App->>User: Navigate to Today screen
    end
```

**Postcondition**: User returned to active tracking or chose alternative  
**Messaging**: 
- Welcome: "Welcome back! Let's figure this out together."
- Resume: "Great! Your habit is ready for today."
- Simplify: "Smaller is smarter. You can always expand later."

### UC-3: Relapse Detection (Day 7)

**Actor**: System (background)  
**Precondition**: Habit has 7+ consecutive missed days  
**Trigger**: Daily lapse detection worker runs

```mermaid
sequenceDiagram
    participant Worker as Lapse Worker
    participant DB as Database
    participant Notif as Notification
    participant User
    
    Worker->>DB: Query LAPSED habits
    DB-->>Worker: Return lapsed habits
    
    loop Each Lapsed Habit
        Worker->>DB: Count consecutive missed days
        DB-->>Worker: Return count
        
        alt count >= relapseThreshold (default 7)
            Worker->>DB: Update phase to RELAPSED
            Worker->>DB: Update RecoverySession type
            Worker->>Notif: Schedule reflection notification
            Notif->>User: "It's been a week since Take medication.<br/>Ready for a fresh start?"
        end
    end
```

**Postcondition**: Habit phase updated to RELAPSED  
**Messaging**:
> "It's been a week since Take medication. That's okay—ready for a fresh start?"

### UC-4: Relapse Recovery Session

**Actor**: User  
**Precondition**: Habit in RELAPSED phase  
**Trigger**: User engages with relapsed habit

```mermaid
sequenceDiagram
    actor User
    participant App
    participant DB as Database
    
    User->>App: Engage with relapsed habit
    App->>User: Show warm welcome back
    Note over App: "Hey! It's good to see you."
    
    App->>User: Show reflection prompt (optional)
    Note over App: "What got in the way? (This helps us help you)"
    User->>App: Select blockers or skip
    
    App->>User: Show deep options
    Note over App: Fresh Start / Simplify / Pause / Archive
    
    alt Fresh Start
        User->>App: Tap "Fresh Start"
        App->>User: Confirm phase reset
        User->>App: Confirm
        App->>DB: Reset phase to FORMING
        App->>DB: Complete RecoverySession
        App->>User: Show celebration
        Note over App: "Day 1 of a new chapter. You've got this."
    else Pause
        User->>App: Tap "Take a Break"
        App->>DB: Update status to PAUSED
        App->>DB: Set pausedAt timestamp
        App->>User: Show confirmation
        Note over App: "No problem. It'll be here when you're ready."
    else Archive
        User->>App: Tap "Archive for Now"
        App->>DB: Update status to ARCHIVED
        App->>DB: Set archivedAt timestamp
        App->>User: Show confirmation
        Note over App: "Archived. Your progress is saved if you want to return."
    end
```

**Postcondition**: User chose recovery path  
**Key Principle**: Never delete data—user's history is preserved

### UC-5: Fresh Start Prompt (Monday/Month)

**Actor**: System  
**Precondition**: Monday morning OR first of month, user has paused/lapsed habits  
**Trigger**: Fresh start worker runs

```mermaid
sequenceDiagram
    participant Worker as Fresh Start Worker
    participant DB as Database
    participant Notif as Notification
    participant User
    
    Worker->>Worker: Check if Monday or 1st of month
    
    alt Is Fresh Start Day
        Worker->>DB: Query PAUSED or LAPSED habits
        DB-->>Worker: Return habits
        
        alt Has eligible habits
            Worker->>Notif: Schedule fresh start notification
            Notif->>User: "New week, clean slate.<br/>Any habits ready for a comeback?"
        end
    end
```

**Postcondition**: User notified of fresh start opportunity  
**Messaging**:
- Monday: "New week, clean slate. Any habits ready for a comeback?"
- Month: "New month energy! Perfect time for a fresh start."

---

## Functional Requirements

### FR-1: Lapse Detection

| ID | Requirement | Priority |
|----|-------------|----------|
| FR-1.1 | System detects consecutive missed days per habit | P0 |
| FR-1.2 | Lapse threshold configurable per habit (default 3) | P0 |
| FR-1.3 | Detection runs daily (background worker) | P0 |
| FR-1.4 | Day 1 missed: No notification (silent) | P0 |
| FR-1.5 | Day 2 missed: Soft notification (optional, no action required) | P1 |
| FR-1.6 | Day 3+ missed: Recovery prompt notification | P0 |
| FR-1.7 | Habit phase updates to LAPSED at threshold | P0 |
| FR-1.8 | RecoverySession record created at lapse | P0 |

### FR-2: Relapse Detection

| ID | Requirement | Priority |
|----|-------------|----------|
| FR-2.1 | Relapse threshold configurable per habit (default 7) | P0 |
| FR-2.2 | LAPSED habit crossing threshold becomes RELAPSED | P0 |
| FR-2.3 | Relapse notification distinct from lapse notification | P0 |
| FR-2.4 | RecoverySession type updated to RELAPSE | P0 |

### FR-3: Recovery Sessions

| ID | Requirement | Priority |
|----|-------------|----------|
| FR-3.1 | Recovery session records start time | P0 |
| FR-3.2 | User can select blocker(s) from predefined list | P1 |
| FR-3.3 | User can skip blocker selection | P0 |
| FR-3.4 | User presented with recovery options | P0 |
| FR-3.5 | Recovery options: Resume, Simplify, Pause, Archive | P0 |
| FR-3.6 | Recovery session records chosen action | P0 |
| FR-3.7 | Recovery session records completion time | P0 |
| FR-3.8 | Session can be abandoned (closed without action) | P0 |

### FR-4: Recovery Actions

| ID | Requirement | Priority |
|----|-------------|----------|
| FR-4.1 | Resume: Returns habit to FORMING phase | P0 |
| FR-4.2 | Simplify: Prompts user to try micro-version | P1 |
| FR-4.3 | Simplify: Updates habit's active version | P1 |
| FR-4.4 | Pause: Sets status to PAUSED, records timestamp | P0 |
| FR-4.5 | Archive: Sets status to ARCHIVED, records timestamp | P0 |
| FR-4.6 | Fresh Start: Resets phase to FORMING (after relapse) | P0 |
| FR-4.7 | All actions preserve historical completion data | P0 |

### FR-5: Fresh Start Prompts

| ID | Requirement | Priority |
|----|-------------|----------|
| FR-5.1 | Monday morning check for eligible habits | P1 |
| FR-5.2 | First of month check for eligible habits | P1 |
| FR-5.3 | Eligible: PAUSED or LAPSED status habits | P1 |
| FR-5.4 | Single notification for all eligible habits | P1 |
| FR-5.5 | User can dismiss without action | P1 |
| FR-5.6 | Notification links to habit list filtered for eligible | P2 |

### FR-6: Messaging Requirements

| ID | Requirement | Priority |
|----|-------------|----------|
| FR-6.1 | All recovery messaging is shame-free | P0 |
| FR-6.2 | No mention of "streak" or "broken streak" | P0 |
| FR-6.3 | No mention of "failed" or "failure" | P0 |
| FR-6.4 | Neutral or positive framing only | P0 |
| FR-6.5 | Messaging feels supportive, not judgmental | P0 |

---

## Non-Functional Requirements

### Performance

| ID | Requirement | Target |
|----|-------------|--------|
| NFR-P1 | Lapse detection worker runtime | < 5 seconds |
| NFR-P2 | Recovery session load time | < 500ms |
| NFR-P3 | Recovery action completion | < 300ms |

### Reliability

| ID | Requirement | Target |
|----|-------------|--------|
| NFR-R1 | Lapse detection runs daily | 100% (WorkManager guaranteed) |
| NFR-R2 | No false lapse detection | 100% (based on completion data) |
| NFR-R3 | Recovery data persists across app updates | 100% |

### Timing

| ID | Requirement | Target |
|----|-------------|--------|
| NFR-T1 | Lapse detection window | After midnight, before 6 AM |
| NFR-T2 | Fresh start check window | 6-8 AM on eligible days |
| NFR-T3 | Notification delivery | Within 1 hour of detection |

---

## UI Requirements

### Recovery Session Flow

```mermaid
flowchart TB
    subgraph Welcome["Welcome Screen"]
        W1["Warm greeting"]
        W2["No judgment messaging"]
        W3["Habit name + icon"]
    end
    
    subgraph Blocker["Blocker Selection (Optional)"]
        B1["What got in the way?"]
        B2["Predefined options"]
        B3["Skip option prominent"]
    end
    
    subgraph Actions["Action Selection"]
        A1["Resume Tracking"]
        A2["Try Smaller Version"]
        A3["Pause for Now"]
        A4["Archive Habit"]
    end
    
    subgraph Confirm["Confirmation"]
        C1["Encouraging message"]
        C2["Next step clarity"]
    end
    
    Welcome --> Blocker --> Actions --> Confirm
```

### Notification Content

| Trigger | Title | Body |
|---------|-------|------|
| Day 2 (soft) | Habit Name | "Just checking in. No pressure." |
| Day 3 (lapse) | Habit Name | "Ready when you are. Tap to check in." |
| Day 7 (relapse) | "Fresh Start?" | "It's been a week. Ready for a new chapter?" |
| Monday | "New Week" | "Clean slate. Any habits ready for a comeback?" |
| Month Start | "New Month" | "Perfect time for a fresh start." |

### Blocker Selection UI

```mermaid
flowchart TB
    subgraph BlockerUI["Blocker Selection"]
        Q["What got in the way?<br/>(Optional - helps us help you)"]
        
        subgraph Options["Tap all that apply"]
            E1["😴 Too tired"]
            E2["🤒 Felt unwell"]
            T1["⏰ No time"]
            T2["📅 Schedule changed"]
            C1["✈️ Traveling"]
            C2["🏠 Environment changed"]
            M1["😰 Overwhelmed"]
            M2["🧠 Just forgot"]
            I1["🎯 Needed a break"]
        end
        
        Skip["Skip →"]
    end
    
    Q --> Options
    Options --> Skip
```

---

## Data Requirements

### RecoverySession Entity

| Field | Type | Required | Notes |
|-------|------|----------|-------|
| id | UUID | Yes | System generated |
| habitId | UUID | Yes | Foreign key |
| type | Enum | Yes | LAPSE, RELAPSE |
| status | Enum | Yes | PENDING, COMPLETED, ABANDONED |
| triggeredAt | Timestamp | Yes | When detected |
| completedAt | Timestamp | No | When user completed session |
| blockers | List<Blocker> | No | Selected blockers |
| action | Enum | No | RESUME, SIMPLIFY, PAUSE, ARCHIVE, FRESH_START |
| notes | String | No | Optional user notes |

### Blocker Enum

| Value | Display Text | Category |
|-------|--------------|----------|
| TOO_TIRED | Too tired | Energy |
| FELT_UNWELL | Felt unwell | Energy |
| LOW_MOTIVATION | Low motivation day | Energy |
| NO_TIME | Ran out of time | Time |
| SCHEDULE_CHANGED | Schedule changed | Time |
| UNEXPECTED_OBLIGATION | Unexpected obligation | Time |
| TRAVELING | Traveling | Context |
| ENVIRONMENT_CHANGED | Environment different | Context |
| OVERWHELMED | Felt overwhelmed | Mental |
| FORGOT | Forgot despite reminder | Mental |
| COULDNT_START | Couldn't get started | Mental |
| NEEDED_BREAK | Needed a break | Intentional |
| DEPRIORITIZED | Chose to skip | Intentional |

### Recovery Action Enum

| Value | Effect |
|-------|--------|
| RESUME | Phase → FORMING, status unchanged |
| SIMPLIFY | Activates micro-version, phase → FORMING |
| PAUSE | Status → PAUSED, pausedAt set |
| ARCHIVE | Status → ARCHIVED, archivedAt set |
| FRESH_START | Phase → FORMING (resets progress tracking) |

---

## Invariants

1. **No Data Deletion**: Recovery actions never delete completion history
2. **Phase Consistency**: LAPSED/RELAPSED phases only exist during active recovery
3. **Threshold Order**: relapseThreshold > lapseThreshold always
4. **Session Completeness**: Completed sessions must have an action
5. **Messaging Compliance**: All user-facing text passes shame-free review

---

## State Machine

```mermaid
stateDiagram-v2
    [*] --> Active: Habit Created
    
    Active --> Lapsed: 3+ days missed
    Active --> Paused: User pauses
    
    Lapsed --> Active: Quick return (< 7 days)
    Lapsed --> Relapsed: 7+ days missed
    Lapsed --> Paused: User pauses
    
    Relapsed --> Active: Fresh start
    Relapsed --> Paused: User pauses
    Relapsed --> Archived: User archives
    
    Paused --> Active: User resumes
    Paused --> Archived: User archives
    
    Archived --> Active: User restores
    Archived --> [*]: User deletes
    
    note right of Lapsed
        RecoverySession created
        Gentle prompts begin
    end note
    
    note right of Relapsed
        Deeper reflection offered
        Fresh start available
    end note
```

---

## Success Metrics

| Metric | Definition | Target |
|--------|------------|--------|
| Recovery Rate | % of lapsed habits that return to active | > 60% |
| Time to Recovery | Days from lapse detection to return | < 3 days median |
| Relapse Prevention | % of lapses that don't become relapses | > 70% |
| Fresh Start Uptake | % of fresh start prompts that lead to action | > 30% |
| Blocker Completion | % of recovery sessions with blocker selected | > 50% |

---

## Messaging Reference

### Do Say

| Situation | Example |
|-----------|---------|
| Lapse notification | "Take medication is waiting. Tap when ready." |
| Recovery welcome | "Welcome back! Let's figure this out." |
| Resume confirmation | "Great! Your habit is ready for today." |
| Pause confirmation | "No problem. It'll be here when you're ready." |
| Fresh start | "Day 1 of a new chapter. You've got this." |

### Never Say

| Forbidden | Why |
|-----------|-----|
| "You broke your streak" | Shame trigger |
| "You failed to..." | Blame language |
| "You missed X days" | Emphasizes failure |
| "Try harder" | Dismissive of real challenges |
| "Don't give up" | Implies they're giving up |
| "You should have..." | Judgmental |

---

## Dependencies

| Dependency | Purpose |
|------------|---------|
| WorkManager | Background lapse/relapse detection |
| Notification System | Recovery prompts |
| Room Database | RecoverySession persistence |
| Habit System | Phase and status updates |

---

## Open Questions

1. Should Day 2 soft notification be opt-in or opt-out?
2. Should fresh start prompts respect Do Not Disturb?
3. What happens if user has 10+ lapsed habits—batch notification?
4. Should archived habits show in a separate view or just filtered out?
