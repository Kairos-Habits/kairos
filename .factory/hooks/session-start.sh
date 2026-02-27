#!/bin/bash
# Kairos Session Start Hook
# Loads project context and environment for this multi-platform ADHD habit system

set -e

input=$(cat)
source_type=$(echo "$input" | jq -r '.source')

# Only run on startup and resume
if [ "$source_type" != "startup" ] && [ "$source_type" != "resume" ]; then
  exit 0
fi

cwd=$(echo "$input" | jq -r '.cwd')
cd "$cwd"

echo "## Kairos Session Context"
echo ""

# Detect active workspace from recent file changes
echo "### Active Workspace Detection"
echo ""

active_dir=""
if [ -d ".git" ]; then
  # Find most recently modified directory
  recent_files=$(git diff --name-only HEAD~10..HEAD 2>/dev/null | head -n 20)
  if [ -n "$recent_files" ]; then
    kotlin_changes=$(echo "$recent_files" | grep -c "^kotlin/" || echo 0)
    web_changes=$(echo "$recent_files" | grep -c "^web/" || echo 0)
    firmware_changes=$(echo "$recent_files" | grep -c "^firmware/" || echo 0)
    
    if [ "$kotlin_changes" -gt "$web_changes" ] && [ "$kotlin_changes" -gt "$firmware_changes" ]; then
      active_dir="kotlin"
    elif [ "$web_changes" -gt "$firmware_changes" ]; then
      active_dir="web"
    elif [ "$firmware_changes" -gt 0 ]; then
      active_dir="firmware"
    fi
  fi
fi

if [ -n "$active_dir" ]; then
  echo "Most recent work in: **$active_dir/**"
else
  echo "No recent git history - fresh project or new session"
fi
echo ""

# Sub-project status summary
echo "### Sub-Project Status"
echo ""

# Kotlin (Android + Desktop)
if [ -d "kotlin" ]; then
  echo "**kotlin/** (Kotlin Multiplatform)"
  if [ -f "kotlin/build.gradle.kts" ]; then
    kotlin_version=$(grep -m1 "kotlin" kotlin/build.gradle.kts | grep -oP '\d+\.\d+\.\d+' | head -1 || echo "detected")
    echo "  - Kotlin version: $kotlin_version"
  fi
  if [ -f "kotlin/gradlew" ]; then
    echo "  - Gradle wrapper present"
  fi
  # Check for local builds
  if ls kotlin/androidApp/build/*.apk 2>/dev/null | head -1 >/dev/null; then
    echo "  - APK built recently"
  fi
  echo ""
fi

# Web (SvelteKit)
if [ -d "web" ]; then
  echo "**web/** (SvelteKit)"
  if [ -f "web/package.json" ]; then
    web_name=$(jq -r '.name' web/package.json)
    echo "  - Package: $web_name"
    if [ -d "web/node_modules" ]; then
      echo "  - Dependencies installed"
    else
      echo "  - Missing: run \`bun install\` in web/"
    fi
  fi
  echo ""
fi

# Firmware (ESP32)
if [ -d "firmware" ]; then
  echo "**firmware/** (ESP-IDF)"
  if [ -f "firmware/presence-esp32/CMakeLists.txt" ]; then
    echo "  - ESP-IDF project configured"
    if command -v idf.py &>/dev/null; then
      echo "  - ESP-IDF available in PATH"
    else
      echo "  - Note: ESP-IDF not in PATH (run \`. ~/esp/esp-idf/export.sh\`)"
    fi
  fi
  echo ""
fi

# Git status
if [ -d ".git" ]; then
  echo "### Git Status"
  echo ""
  
  branch=$(git branch --show-current 2>/dev/null || echo "detached")
  echo "Branch: \`$branch\`"
  
  uncommitted=$(git status --short 2>/dev/null | wc -l | tr -d ' ')
  if [ "$uncommitted" -gt 0 ]; then
    echo "Uncommitted changes: $uncommitted files"
    git status --short | head -n 5 | sed 's/^/  /'
    if [ "$uncommitted" -gt 5 ]; then
      echo "  ... and $((uncommitted - 5)) more"
    fi
  else
    echo "Working tree clean"
  fi
  echo ""
  
  # Recent commits
  echo "Recent commits:"
  git log --oneline -5 2>/dev/null | sed 's/^/  /'
  echo ""
fi

# Environment check
echo "### Environment Check"
echo ""

# Check for required tools
tools_ok=true

if command -v bun &>/dev/null; then
  bun_version=$(bun --version)
  echo "- bun: $bun_version"
else
  echo "- bun: NOT FOUND (required for web/)"
  tools_ok=false
fi

if command -v java &>/dev/null; then
  java_version=$(java -version 2>&1 | head -1 | cut -d'"' -f2)
  echo "- java: $java_version"
else
  echo "- java: NOT FOUND (required for kotlin/)"
  tools_ok=false
fi

if command -v python3 &>/dev/null; then
  py_version=$(python3 --version | cut -d' ' -f2)
  echo "- python3: $py_version"
fi

if [ "$tools_ok" = false ]; then
  echo ""
  echo "Missing tools detected. Install before working on affected sub-projects."
fi
echo ""

# Quick suggestions based on context
echo "### Quick Start Commands"
echo ""
echo "\`\`\`"
echo "# Kotlin (Android/Desktop)"
echo "cd kotlin && ./gradlew :kiosk:run             # Desktop kiosk"
echo "cd kotlin && ./gradlew :androidApp:assembleDebug  # Android APK"
echo ""
echo "# Web (SvelteKit)"
echo "cd web && bun install && bun run dev          # Dev server"
echo ""
echo "# Firmware (ESP32)"
echo "cd firmware/presence-esp32 && idf.py build    # Build"
echo "cd firmware/presence-esp32 && idf.py -p /dev/ttyUSB0 flash monitor"
echo "\`\`\`"
echo ""

# Load architecture reminders from memories
if [ -f "$cwd/.factory/memories.md" ]; then
  # Extract key invariants
  echo "### Architecture Invariants"
  echo ""
  grep -A 6 "^## Key Architectural Invariants" "$cwd/.factory/memories.md" 2>/dev/null | tail -n +2 | head -n 6 || true
  echo ""
fi

exit 0
