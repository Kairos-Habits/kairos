# Claude Code Automation Setup

This directory contains Claude Code automations for the Kairos habit tracking project.

## 📁 Structure

```
.claude/
├── skills/              # User-invocable workflows and reference guides
│   ├── migration-helper/
│   │   └── SKILL.md    # Room database migration workflow
│   └── adhd-first-review/
│       └── SKILL.md    # ADHD-first language validation
├── agents/              # Specialized reviewers (run in parallel)
│   ├── compose-reviewer.md      # Jetpack Compose quality checks
│   └── clean-arch-enforcer.md   # Architecture boundary validation
├── settings.json        # Hooks and permissions configuration
└── README.md           # This file
```

## 🎯 Skills

### `/migration-helper`
**Use when**: Evolving Room database schema, adding tables/columns

Generates validated Room migrations with:
- Schema export configuration
- Migration code templates
- Test generation
- Validation checklist

**Example**:
```bash
# Not directly invoked - provides reference when Claude detects schema changes
```

### `/adhd-first-review`
**Use when**: Writing user-facing text, notifications, or UI strings

Validates language against ADHD-first principles:
- No shame-inducing terms ("streak", "failure", "missed")
- Recovery-focused alternatives
- Automated grep checks for forbidden terms

**Auto-invoked**: Claude will use this when writing UI text

## 🤖 Subagents

### compose-reviewer
**Purpose**: Review Jetpack Compose code for performance, accessibility, and best practices

**Checks**:
- Recomposition performance (unstable parameters)
- Accessibility (contentDescription, semantic properties)
- Wear Compose patterns (ScalingLazyColumn, etc.)
- Proper side effect usage

**Usage**: Automatically invoked during PR reviews or when requested

### clean-arch-enforcer
**Purpose**: Validate Clean Architecture boundaries in multi-module setup

**Validates**:
- Domain module has no dependencies
- Data/Core only depend on domain
- No Android framework in domain/core
- ViewModels only in presentation modules

**Usage**: Automatically invoked for build.gradle.kts changes or architecture reviews

## ⚡ Hooks

### PreToolUse Hooks (Protection)
Blocks edits to:
- `build/generated/` - Hilt/Room generated code
- `/schemas/` - Room database schemas (managed by migrations)
- `gradle.properties`, `gradlew`, `gradle-wrapper.*` - Build configs

### PostToolUse Hooks (Auto-format)
After editing/writing `.kt` files:
- Runs `./gradlew spotlessApply` to auto-format with ktlint
- Falls back gracefully if ktlint not configured yet

## ✅ Integrated Code Quality Tools

The following code quality tools are now fully configured:

- **Spotless + ktlint**: Auto-formatting (configured in root `build.gradle.kts`)
- **Detekt**: Static analysis (configured in `config/detekt/detekt.yml`)
- **Pre-commit hooks**: Local validation (`scripts/pre-commit.sh`)
- **CI/CD**: GitHub Actions workflow (`.github/workflows/code-quality.yml`)

See `../CODE_QUALITY_SETUP.md` for complete documentation.

## 🔧 Setup Required

### 1. ~~ktlint/Spotless (for auto-formatting)~~ ✅ DONE

~~Add to root `build.gradle.kts`:~~
Already configured! To use:

```kotlin
plugins {
    id("com.diffplug.spotless") version "6.25.0" apply false
}
```

**To format code:**
```bash
./gradlew spotlessApply
```

**To check formatting:**
```bash
./gradlew spotlessCheck
```

**Install pre-commit hooks:**
```bash
./scripts/install-hooks.sh
```

### 2. Room Schema Export (for migration-helper)

If not already configured, add to `data/build.gradle.kts`:

```kotlin
plugins {
    id("androidx.room")
}

room {
    schemaDirectory("$projectDir/schemas")
}
```

### 3. MCP Servers (Already Installed)

The following MCP servers are already installed:
- ✅ **context7** - Live documentation for Android libraries
- ✅ **github** - GitHub integration for issues/PRs

## 🚀 Using the Automations

### Skills
Skills are automatically discovered by Claude when relevant. You can also invoke them:
- Just mention the task (e.g., "I need to add a column to the database")
- Claude will reference the migration-helper skill

### Subagents
Subagents run automatically during:
- PR reviews (`/review-pr` command)
- Architecture changes (build file edits)
- Compose UI changes

You can also request them explicitly:
```
"Review this Compose code for performance issues"
"Validate the architecture boundaries"
```

### Hooks
Hooks run automatically - no action needed:
- Edits are blocked for protected files
- Kotlin files are auto-formatted after saves

## 🔧 Serena Project

This project uses **Serena** for semantic code navigation and editing.

**Activate on session start**:
```
Activate the Serena project "kairos"
```

Or run:
```bash
./.claude/activate-serena.sh
```

See `SERENA_SETUP.md` for complete Serena documentation.

## 📚 References

- **Project Architecture**: `../docs/design/07-architecture.md`
- **ADHD-First Design**: `../docs/design/01-prd-core.md`
- **Domain Model**: `../docs/design/05-domain-model.md`
- **Main Instructions**: `../CLAUDE.md`
- **Serena Setup**: `SERENA_SETUP.md`

## 🔍 Validation

Test that automations are working:

```bash
# Verify hook configuration
cat .claude/settings.json

# Check skills are present
ls -la .claude/skills/*/SKILL.md

# Check subagents
ls -la .claude/agents/*.md

# Test protection hook (should fail)
echo "test" > build/generated/test.kt
# Claude should block this edit

# Test formatting hook
# Edit any .kt file - should auto-format
```

## 🤝 Contributing

To add new automations:

1. **Skills**: Create `.claude/skills/<name>/SKILL.md` with proper frontmatter
2. **Subagents**: Create `.claude/agents/<name>.md` with role and process
3. **Hooks**: Edit `.claude/settings.json` to add new pre/post hooks

Follow the patterns in existing files for consistency.

## 📝 Notes

- Hooks are defined in `.claude/settings.json` and apply to all Claude Code sessions
- Skills use YAML frontmatter for metadata (name, description)
- Subagents are Markdown files describing specialized review processes
- All paths are relative to the repository root
