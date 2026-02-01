---
name: adhd-first-review
description: Use when writing UI strings, notification text, error messages, or any user-facing content in Kairos habit tracking app - validates against shame-inducing language
---

# ADHD-First Language Review

## Overview

Validates user-facing text against ADHD-first design principles. Kairos uses shame-free language focused on recovery and compassion, never punishment or failure.

## When to Use

**Automatically applies when:**
- Writing Composable UI with text content
- Creating string resources (`strings.xml`)
- Writing notification messages
- Adding error messages or feedback
- Documenting user-facing features

**When NOT to use:**
- Internal code comments (not user-facing)
- API documentation
- System logs (unless shown to user)

## Core Principle

**People with ADHD experience shame and anxiety around "failure."** Language choices directly impact motivation and continued use. Recovery-focused language maintains engagement.

## Forbidden Terms → Alternatives

| ❌ Never Use | ✅ Use Instead | Context |
|-------------|---------------|---------|
| streak | rhythm, pattern, flow | Habit consistency |
| failure / failed | lapse, pause, break | Missed completions |
| miss / missed | skip, pause | Incomplete days |
| perfect | complete, full | 100% completion |
| on track | in rhythm, flowing | Progress status |
| off track | paused, taking a break | Non-completion |
| maintain streak | keep the rhythm | Ongoing habits |
| broke streak | rhythm paused | After lapse |
| consistency | rhythm, flow | General progress |
| slipping | adjusting, recalibrating | Lapse phase |

## Validation Pattern

```bash
# Scan for forbidden terms in UI modules
cd /home/rghamilton3/workspace/kairos-habits/kairos
grep -r "streak\|failure\|failed\|miss\|missed\|perfect" \
  app/src/main/res/values/ \
  app/src/main/kotlin/ \
  wear/src/main/res/values/ \
  wear/src/main/kotlin/ \
  ui-shared/src/main/kotlin/ \
  --include="*.xml" \
  --include="*.kt"
```

## Real Examples from Kairos

### ✅ Good Language

```kotlin
// Composable UI
Text("Your rhythm paused 3 days ago. Ready to start again?")
Button(onClick = { /* ... */ }) { Text("Resume rhythm") }

// Notification
"Time to continue your morning routine rhythm"

// Recovery messaging
"Lapses happen. You've got this - let's ease back in."
"You completed 4 out of 7 days. That's progress!"

// Phase transitions (from state machine)
HabitPhase.LAPSED -> "Habit paused - recovery available"
HabitPhase.RELAPSED -> "Fresh start opportunity"
```

### ❌ Bad Language (Never Use)

```kotlin
// WRONG - Shame-inducing
Text("You failed to maintain your streak")
Text("Missed 3 days - streak broken")
Text("Perfect completion required")
Button(onClick = { /* ... */ }) { Text("Fix your habit") }

// WRONG - Punishment framing
"You're off track. Get back on schedule."
"Failure to complete will reset progress."
```

## String Resource Validation

When creating `strings.xml`, apply same rules:

```xml
<!-- ✅ GOOD -->
<string name="habit_paused">Rhythm paused</string>
<string name="recovery_message">Ready to ease back in?</string>
<string name="partial_completion">You did %1$d%% - that counts!</string>

<!-- ❌ BAD -->
<string name="streak_broken">Streak broken</string>
<string name="missed_days">You missed %1$d days</string>
<string name="failure_message">Failed to complete</string>
```

## Context-Specific Guidance

### Habit Status Display

```kotlin
when (habit.phase) {
    HabitPhase.FORMING -> "Building your rhythm"
    HabitPhase.MAINTAINING -> "Rhythm established"
    HabitPhase.LAPSED -> "Paused - recovery available"
    HabitPhase.RELAPSED -> "Fresh start ready"
}
```

### Partial Completion

```kotlin
// Always frame partial as progress
val percentage = (completed / total) * 100
when {
    percentage >= 80 -> "Strong rhythm: $percentage%"
    percentage >= 50 -> "Good progress: $percentage%"
    percentage > 0 -> "Every step counts: $percentage%"
    else -> "Ready to start?"
}
```

### Recovery Messaging

```kotlin
// After lapse threshold
Text("Life happens. You've paused for ${daysLapsed} days.")
Button(onClick = { /* ... */ }) {
    Text("Continue where you left off")
}

// After relapse threshold
Text("Ready for a fresh start?")
Text("Previous rhythm: ${completionRate}% - you've done this before!")
```

## Automated Check Command

Create this as a git pre-commit hook or CI check:

```bash
#!/bin/bash
# .git/hooks/pre-commit or CI script

FORBIDDEN_TERMS="streak|failure|failed|miss(?!ion)|missed|perfect(?!ly valid)"

FILES=$(git diff --cached --name-only --diff-filter=ACM | \
  grep -E '\.(kt|xml)$' | \
  grep -E '(app|wear|ui-shared)/src/main/')

if [ -n "$FILES" ]; then
  VIOLATIONS=$(echo "$FILES" | xargs grep -nE "$FORBIDDEN_TERMS" || true)

  if [ -n "$VIOLATIONS" ]; then
    echo "❌ ADHD-First Language Violation Detected:"
    echo "$VIOLATIONS"
    echo ""
    echo "Use shame-free alternatives - see .claude/skills/adhd-first-review/"
    exit 1
  fi
fi

echo "✅ ADHD-first language check passed"
```

## Design Document Reference

Full language guidelines in:
- `docs/design/01-prd-core.md` - Core product principles
- `docs/design/06-invariants.md` - Language invariants

Key quote from PRD:
> "Kairos uses recovery-oriented language. We never say 'streak broken' or 'you failed.' Every interaction assumes return, not abandonment."

## Common Mistakes

| Mistake | Why It Happens | Fix |
|---------|---------------|-----|
| "Maintain streak" | Common habit app language | Use "Keep the rhythm" |
| "Perfect week" | Celebrating completion | Use "Complete week" or "Full rhythm" |
| "Don't miss your goal" | Motivational intent | Use "Continue your rhythm" |
| "Failed to complete" | Error message default | Use "Not completed yet" or "Paused" |
| Technical logs with "fail" | System terminology | OK for internal logs, forbidden in UI |

## Red Flags - STOP and Revise

If you catch yourself writing:
- Any form of "streak" in user-facing text
- "Miss" or "fail" describing user behavior
- Comparison to "perfect" completion
- Language implying disappointment or shame

**→ Reframe with compassion and recovery focus**

## Validation Checklist

Before committing UI changes:
- [ ] Grep for forbidden terms in changed files
- [ ] Review notifications for shame language
- [ ] Check error messages for blame framing
- [ ] Verify string resources use approved alternatives
- [ ] Test partial completion messaging (never implies failure)
- [ ] Confirm recovery flows are encouraging, not punitive
