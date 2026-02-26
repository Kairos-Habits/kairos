#!/bin/bash
# Auto-formatting hook for Kairos multi-language project
# Skips hidden directories and generated folders

set -e

# Read the hook input
input=$(cat)
file_path=$(echo "$input" | jq -r '.tool_input.file_path')
PROJECT_ROOT="${FACTORY_PROJECT_DIR:-$(pwd)}"

# Skip if file doesn't exist
if [ ! -f "$file_path" ]; then
  exit 0
fi

# Skip hidden directories and generated folders
should_skip() {
    local path="$1"
    # Check for hidden directories (starting with .) except .factory
    if echo "$path" | grep -qE '/\.([a-zA-Z0-9_-]+)/' && ! echo "$path" | grep -qE '/\.factory/'; then
        return 0
    fi
    # Check for generated/build directories
    if echo "$path" | grep -qE '/(node_modules|build|dist|\.gradle|\.kotlin|\.svelte-kit|test-results|captures|\.externalNativeBuild|\.cxx)/'; then
        return 0
    fi
    return 1
}

if should_skip "$file_path"; then
  exit 0
fi

# Determine formatter based on file path and extension
case "$file_path" in
  # Web files (SvelteKit) - use prettier
  "$PROJECT_ROOT"/web/*)
    case "$file_path" in
      *.ts|*.tsx|*.js|*.jsx|*.json|*.css|*.scss|*.md|*.svelte)
        if [ -f "$PROJECT_ROOT/web/node_modules/.bin/prettier" ]; then
          cd "$PROJECT_ROOT/web"
          ./node_modules/.bin/prettier --write "$file_path" 2>&1 || true
          echo "Formatted with Prettier: $file_path"
        elif command -v prettier &> /dev/null; then
          prettier --write "$file_path" 2>&1 || true
          echo "Formatted with Prettier: $file_path"
        fi
        ;;
    esac
    ;;

  # Kotlin files - use ktlint via gradle plugin
  "$PROJECT_ROOT"/kotlin/*)
    case "$file_path" in
      *.kt|*.kts)
        cd "$PROJECT_ROOT/kotlin"
        ./gradlew ktlintFormat --quiet 2>&1 || true
        echo "Formatted with Ktlint: $file_path"
        ;;
    esac
    ;;

  # Firmware files (ESP32) - use clang-format
  "$PROJECT_ROOT"/firmware/*)
    case "$file_path" in
      *.c|*.h|*.cpp|*.hpp)
        if command -v clang-format &> /dev/null; then
          clang-format -i "$file_path" 2>&1 || true
          echo "Formatted with clang-format: $file_path"
        fi
        ;;
    esac
    ;;

  *)
    # No formatter for this file type or location
    exit 0
    ;;
esac

exit 0
