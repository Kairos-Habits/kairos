# User Flows & Experience Design

## Overview

This document maps the key user journeys through Kairos, detailing screens, interactions, and decision points.

---

## Flow Index

```mermaid
mindmap
  root((User Flows))
    Onboarding
      First launch
      Create first habit
      Enable notifications
    Daily Usage
      View today's habits
      Complete habit
      Skip habit
      Run routine
    Recovery
      Lapse prompt
      Recovery session
      Fresh start
    Management
      Edit habit
      Pause habit
      Archive habit
    Settings
      Notification preferences
      Sync setup
      Theme selection
```

---

## Flow 1: First Launch & Onboarding

```mermaid
flowchart TB
    subgraph FirstLaunch["First Launch Flow"]
        Start["App Opens"] --> Welcome["Welcome Screen<br/>'Kairos helps you build<br/>habits that stick'"]
        Welcome --> Philosophy["Philosophy Screen<br/>'No streaks. No judgment.<br/>Just progress.'"]
        Philosophy --> FirstHabit["Create First Habit<br/>(Guided flow)"]
        FirstHabit --> NotifPermission{"Enable<br/>Notifications?"}
        NotifPermission -->|Yes| NotifSetup["Configure reminders"]
        NotifPermission -->|No| Later["Maybe Later"]
        NotifSetup --> Today["Today Screen"]
        Later --> Today
    end
```

### Welcome Screen

| Element | Content |
|---------|---------|
| Illustration | Calm, non-gamified visual |
| Headline | "Kairos" |
| Subhead | "Habit building designed for how your brain actually works" |
| CTA | "Get Started" |
| Skip | Not available (must see philosophy) |

### Philosophy Screen

| Element | Content |
|---------|---------|
| Headline | "Three things we do differently" |
| Point 1 | "No streaks to break" with icon |
| Point 2 | "Partial completion counts" with icon |
| Point 3 | "Recovery is built in" with icon |
| CTA | "Create Your First Habit" |

---

## Flow 2: Create Habit

```mermaid
flowchart TB
    subgraph CreateHabit["Create Habit Flow"]
        Start["Tap + or 'Add Habit'"]
        
        subgraph Required["Required Steps"]
            Name["1. Enter Habit Name<br/>'What do you want to do?'"]
            Anchor["2. Select Anchor<br/>'When will you do this?'"]
            Category["3. Choose Category<br/>Morning / Afternoon / Evening / Anytime"]
        end
        
        subgraph Optional["Optional Configuration"]
            Duration["Estimated time"]
            MicroVersion["Micro version<br/>'Smallest possible version'"]
            Reminders["Notification settings"]
            Visual["Icon & color"]
        end
        
        Start --> Name --> Anchor --> Category
        Category --> Optional
        Optional --> Review["Review & Create"]
        Review --> Success["Success!<br/>Habit added to Today"]
    end
```

### Create Habit: Name Input

```mermaid
flowchart LR
    subgraph NameScreen["Name Input Screen"]
        Input["Text input<br/>'Take morning medication'"]
        Examples["Suggestions<br/>Based on common habits"]
        Next["Continue →"]
    end
```

| UI Element | Behavior |
|------------|----------|
| Text input | Auto-focus, 1-100 characters |
| Character count | Subtle indicator |
| Suggestions | Tap to fill, categorized |
| Continue | Enabled when name valid |

### Create Habit: Anchor Selection

```mermaid
flowchart TB
    subgraph AnchorScreen["Anchor Selection"]
        Question["When will you do this?"]
        
        subgraph AnchorTypes["Anchor Type Tabs"]
            After["After I..."]
            Before["Before I..."]
            AtLocation["When I arrive at..."]
            AtTime["At a specific time"]
        end
        
        subgraph AfterOptions["After I... Options"]
            A1["Wake up"]
            A2["Brush my teeth"]
            A3["Have breakfast"]
            A4["Get to work"]
            A5["Custom..."]
        end
    end
    
    Question --> AnchorTypes
    After --> AfterOptions
```

| Anchor Type | Input Method | Examples |
|-------------|--------------|----------|
| After behavior | Preset list + custom | "After I brush my teeth" |
| Before behavior | Preset list + custom | "Before I start work" |
| At location | Location picker | "When I arrive at gym" |
| At time | Time picker | "At 7:00 AM" |

### Create Habit: Category

```mermaid
flowchart LR
    subgraph CategoryScreen["Category Selection"]
        Morning["🌅 Morning<br/>Before noon"]
        Afternoon["☀️ Afternoon<br/>12 PM - 6 PM"]
        Evening["🌙 Evening<br/>After 6 PM"]
        Anytime["⏰ Anytime<br/>No specific time"]
    end
```

---

## Flow 3: Today Screen

```mermaid
flowchart TB
    subgraph TodayScreen["Today Screen"]
        Header["Header<br/>Date + Progress Ring"]
        
        subgraph HabitGroups["Habit Groups"]
            Morning["🌅 Morning Habits"]
            Afternoon["☀️ Afternoon Habits"]
            Evening["🌙 Evening Habits"]
            Anytime["⏰ Anytime"]
        end
        
        subgraph HabitCard["Habit Card"]
            CardName["Habit Name"]
            CardAnchor["Anchor text (subtle)"]
            CardAction["Completion Button"]
        end
        
        FAB["+ Add Habit"]
    end
    
    Header --> HabitGroups
    Morning --> HabitCard
```

### Today Screen States

```mermaid
stateDiagram-v2
    [*] --> Empty: No habits
    [*] --> HasHabits: Has habits
    
    state Empty {
        [*] --> EmptyState
        EmptyState: Illustration
        EmptyState: "Add your first habit"
        EmptyState: + Button
    }
    
    state HasHabits {
        [*] --> Pending
        Pending --> AllDone: All completed
        Pending --> PartiallyDone: Some completed
    }
    
    state AllDone {
        [*] --> Celebration
        Celebration: "All done for today!"
        Celebration: Subtle celebration
    }
```

### Habit Card Interaction

```mermaid
sequenceDiagram
    actor User
    participant Card as Habit Card
    participant Sheet as Bottom Sheet
    participant DB as Database
    
    User->>Card: Tap completion button
    Card->>Sheet: Show completion options
    
    alt Quick complete (full)
        Sheet->>User: "Done" option
        User->>Sheet: Tap "Done"
        Sheet->>DB: Record FULL completion
        DB-->>Card: Update state
        Card->>User: Checkmark animation
    else Partial
        Sheet->>User: "Partial" option
        User->>Sheet: Tap "Partial"
        Sheet->>User: Show percentage slider
        User->>Sheet: Select percentage
        Sheet->>DB: Record PARTIAL completion
        DB-->>Card: Update state
        Card->>User: Partial indicator
    else Skip
        Sheet->>User: "Skip Today" option
        User->>Sheet: Tap "Skip"
        Sheet->>User: Optional reason picker
        Sheet->>DB: Record SKIPPED
        DB-->>Card: Update state
        Card->>User: Skip indicator
    end
```

---

## Flow 4: Complete Habit

### Quick Complete (Single Tap)

```mermaid
flowchart LR
    subgraph QuickComplete["Quick Complete"]
        Tap["Tap ✓ button"]
        Haptic["Haptic feedback"]
        Animation["Checkmark animation"]
        Update["Card updates"]
    end
    
    Tap --> Haptic --> Animation --> Update
```

### Full Completion Sheet

```mermaid
flowchart TB
    subgraph CompletionSheet["Completion Bottom Sheet"]
        HabitInfo["Habit name + anchor"]
        
        subgraph Options["Completion Options"]
            Done["✓ Done<br/>Fully completed"]
            Partial["◐ Partial<br/>Did some of it"]
            Skip["⊘ Skip Today<br/>Not doing it"]
        end
        
        subgraph OptionalTracking["Optional (if enabled)"]
            Energy["Energy Level<br/>1-5 selector"]
            Note["Add Note"]
        end
    end
```

---

## Flow 5: Run Routine

```mermaid
flowchart TB
    subgraph RoutineFlow["Routine Runner Flow"]
        Start["Tap routine 'Play' button"]
        
        subgraph VariantSelect["Variant Selection (if multiple)"]
            Quick["Quick (10 min)"]
            Standard["Standard (20 min)"]
            Extended["Extended (35 min)"]
        end
        
        subgraph Runner["Routine Runner"]
            CurrentHabit["Current Habit<br/>Large timer display"]
            Progress["Progress indicator<br/>2 of 5"]
            Controls["Done | Skip | Pause"]
            UpNext["Up next preview"]
        end
        
        subgraph Complete["Completion"]
            Summary["Routine Summary<br/>Time: 18:32<br/>Completed: 4/5"]
            Celebrate["Celebration animation"]
            BackToToday["Return to Today"]
        end
    end
    
    Start --> VariantSelect --> Runner
    Runner --> Complete
```

### Routine Runner Screen

```mermaid
flowchart TB
    subgraph RunnerScreen["Routine Runner Layout"]
        Header["Morning Routine<br/>Step 2 of 4"]
        
        subgraph Timer["Timer Display"]
            HabitName["Take medication"]
            TimerRing["Large countdown ring<br/>2:45"]
        end
        
        subgraph Actions["Action Buttons"]
            Done["✓ Done"]
            Skip["Skip →"]
            More["•••"]
        end
        
        Preview["Up next: Drink water (1 min)"]
    end
```

---

## Flow 6: Recovery (Lapse)

```mermaid
flowchart TB
    subgraph LapseFlow["Lapse Recovery Flow"]
        Detect["System detects 3+ missed days"]
        Notify["Gentle notification<br/>'Habit is waiting when you're ready'"]
        
        subgraph UserEngages["User Taps Notification"]
            Welcome["Welcome back!<br/>Let's figure this out."]
            
            subgraph Blocker["What got in the way? (optional)"]
                B1["Too tired"]
                B2["No time"]
                B3["Forgot"]
                B4["Needed break"]
                B5["Skip this"]
            end
            
            subgraph Actions["What would you like to do?"]
                Resume["Resume Tracking<br/>Pick up where you left off"]
                Simplify["Try Smaller<br/>Use micro version"]
                Pause["Take a Break<br/>Pause this habit"]
                Archive["Archive<br/>Remove for now"]
            end
        end
        
        Detect --> Notify --> Welcome --> Blocker --> Actions
    end
```

### Recovery Session UI

| Screen | Purpose | Messaging |
|--------|---------|-----------|
| Welcome | Warm return | "Welcome back! Let's figure this out together." |
| Blocker | Optional data | "What got in the way? (This helps us help you)" |
| Actions | Choose path | Clear options, no judgment |
| Confirm | Reinforce choice | "Great choice. Your habit is ready." |

---

## Flow 7: Fresh Start (Relapse)

```mermaid
flowchart TB
    subgraph RelapseFlow["Relapse Fresh Start"]
        Detect["7+ days missed"]
        Notify["Fresh start notification"]
        
        subgraph Session["Fresh Start Session"]
            Welcome["It's good to see you!<br/>Ready for a fresh start?"]
            Reflection["Quick reflection (optional)<br/>What happened?"]
            
            subgraph Options["Options"]
                Fresh["Fresh Start<br/>Day 1 of a new chapter"]
                Pause["Take a Break<br/>Come back when ready"]
                Archive["Archive<br/>Not right now"]
            end
        end
        
        Fresh --> NewPhase["Phase resets to FORMING<br/>Progress preserved"]
    end
    
    Detect --> Notify --> Session
```

---

## Flow 8: Settings & Sync

```mermaid
flowchart TB
    subgraph SettingsFlow["Settings Flow"]
        Main["Settings Main"]
        
        subgraph Sections["Setting Sections"]
            Notifications["Notifications<br/>Channels, quiet hours"]
            Sync["Sync & Backup<br/>Enable, account, status"]
            Appearance["Appearance<br/>Theme, colors"]
            Data["Data<br/>Export, import, delete"]
            About["About<br/>Version, feedback"]
        end
    end
    
    Main --> Sections
```

### Sync Setup Flow

```mermaid
sequenceDiagram
    actor User
    participant Settings
    participant Auth as Auth Flow
    participant Provider as Firebase
    participant DB as Local DB
    
    User->>Settings: Tap "Enable Sync"
    Settings->>Auth: Launch sign-in
    Auth->>User: Google OAuth
    User->>Auth: Authorize
    Auth-->>Settings: Token received
    
    Settings->>Provider: Authenticate
    Provider-->>Settings: Success
    
    Settings->>DB: Query local data
    Settings->>Provider: Upload initial data
    Provider-->>Settings: Sync complete
    
    Settings->>User: "Sync enabled!"
```

---

## Flow 9: WearOS Interactions

```mermaid
flowchart TB
    subgraph WearFlows["WearOS Flows"]
        subgraph Tile["Habit Tile"]
            TileView["Today's habits list"]
            TileTap["Tap habit → Complete"]
        end
        
        subgraph Complication["Complication"]
            CompView["Remaining count: 3"]
            CompTap["Tap → Open app"]
        end
        
        subgraph App["Watch App"]
            AppList["Habit list view"]
            AppDetail["Habit detail + complete"]
            AppRoutine["Routine runner"]
        end
    end
```

### Watch Complete Flow

```mermaid
sequenceDiagram
    actor User
    participant Watch
    participant Phone
    participant Cloud
    
    User->>Watch: Tap habit on tile
    Watch->>Watch: Show confirm dialog
    User->>Watch: Confirm "Done"
    Watch->>Watch: Record completion locally
    Watch->>User: Haptic + visual confirm
    
    Watch->>Phone: Sync via Data Layer
    Phone->>Phone: Update local DB
    Phone->>Cloud: Sync to cloud
```

---

## Error States & Empty States

### Error Handling Flows

```mermaid
flowchart TB
    subgraph Errors["Error States"]
        SyncError["Sync failed<br/>'Changes saved locally'<br/>Retry button"]
        NetworkError["Offline<br/>'You're offline'<br/>Local mode active"]
        AuthError["Session expired<br/>'Please sign in again'<br/>Sign in button"]
    end
```

### Empty States

| Screen | Empty State | CTA |
|--------|-------------|-----|
| Today (no habits) | Illustration + "Add your first habit" | Add Habit button |
| Today (all done) | "All done for today! 🎉" | None needed |
| Routines (none) | "Group habits into routines" | Create Routine |
| History (no data) | "Your history will appear here" | None |

---

## Interaction Specifications

### Tap Targets

| Element | Minimum Size | Recommended Size |
|---------|--------------|------------------|
| Primary buttons | 48dp | 56dp |
| Completion button | 48dp | 64dp |
| List items | 48dp height | 72dp height |
| Icon buttons | 48dp | 48dp |

### Animations

| Action | Animation | Duration |
|--------|-----------|----------|
| Habit complete | Checkmark draw + scale | 300ms |
| Card expand | Height + fade | 200ms |
| Screen transition | Shared element | 300ms |
| Progress update | Ring fill | 400ms |

### Haptic Feedback

| Action | Haptic Type |
|--------|-------------|
| Habit complete | Success (tick) |
| Button tap | Light click |
| Error | Error pattern |
| Routine step done | Medium click |

---

## Accessibility

### Screen Reader Support

| Element | Content Description |
|---------|---------------------|
| Completion button | "Complete [habit name], button" |
| Progress ring | "[X] of [Y] habits completed" |
| Habit card | "[name], [status], [anchor]" |

### Motion Reduction

| Animation | Reduced Motion Alternative |
|-----------|---------------------------|
| Celebration | Static checkmark |
| Progress ring | Instant fill |
| Transitions | Fade only |
