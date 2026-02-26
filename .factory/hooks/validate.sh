#!/bin/bash
# Code validation hook for Kairos multi-platform project
# Runs appropriate validators based on file path

input=$(cat)
file_path=$(echo "$input" | jq -r '.tool_input.file_path // ""')
PROJECT_ROOT="${FACTORY_PROJECT_DIR:-$(pwd)}"

# Skip if no file path or file doesn't exist
if [ -z "$file_path" ] || [ ! -f "$file_path" ]; then
    exit 0
fi

# Skip hidden and generated directories
case "$file_path" in
    */node_modules/*|*/build/*|*/dist/*|*/.gradle/*|*/.svelte-kit/*|*/.kotlin/*|*/.idea/*|*/.*)
        exit 0
        ;;
esac

# Web: eslint + tsc + semgrep
run_web_validation() {
    local file="$1"
    local web_root
    
    # Find web/ root
    web_root="${file%/web/*}/web"
    if [ ! -d "$web_root" ]; then
        return 0
    fi
    
    cd "$web_root"
    
    # ESLint for JS/TS/Svelte files
    case "$file" in
        *.ts|*.js|*.svelte)
            if [ -f "eslint.config.js" ]; then
                echo "🔍 Running ESLint on $(basename "$file")..."
                bun run lint 2>&1 | head -20 || true
            fi
            ;;
    esac
    
    # Semgrep for security (if available)
    if command -v semgrep &> /dev/null; then
        echo "🔍 Running Semgrep security scan..."
        semgrep --config=auto --quiet "$file" 2>&1 || true
    fi
}

# Kotlin: ktlint + detekt + semgrep
run_kotlin_validation() {
    local file="$1"
    local kotlin_root

    kotlin_root="${file%/kotlin/*}/kotlin"
    if [ ! -d "$kotlin_root" ]; then
        return 0
    fi

    cd "$kotlin_root"

    case "$file" in
        *.kt|*.kts)
            echo "🔍 Running Ktlint check..."
            ./gradlew ktlintCheck --quiet 2>&1 | tail -5 || echo "💡 Run './gradlew ktlintFormat' to fix"

            echo "🔍 Running Detekt..."
            ./gradlew detekt --quiet 2>&1 | tail -5 || true
            ;;
    esac

    # Semgrep for security (if available)
    if command -v semgrep &> /dev/null; then
        echo "🔍 Running Semgrep security scan..."
        semgrep --config=auto --quiet "$file" 2>&1 || true
    fi
}

# Firmware: clang-tidy + semgrep
run_firmware_validation() {
    local file="$1"
    
    case "$file" in
        *.c|*.cpp|*.h|*.hpp)
            if command -v clang-tidy &> /dev/null; then
                echo "🔍 Running clang-tidy on $(basename "$file")..."
                clang-tidy "$file" 2>&1 | head -10 || true
            fi
            
            if command -v semgrep &> /dev/null; then
                echo "🔍 Running Semgrep security scan..."
                semgrep --config=auto --quiet "$file" 2>&1 || true
            fi
            ;;
    esac
}

# Root: gitleaks for secrets
run_root_validation() {
    local file="$1"

    # Skip gradle version catalog (contains version strings that trigger false positives)
    case "$file" in
        */gradle/libs.versions.toml)
            return 0
            ;;
    esac

    # Check config files for secrets
    case "$file" in
        *.env*|*.yaml|*.yml|*.json|*.toml|*.properties|*.gradle.kts)
            if command -v gitleaks &> /dev/null; then
                echo "🔍 Running Gitleaks secret detection..."
                if ! gitleaks detect --source="$file" --no-git --quiet 2>&1; then
                    echo "❌ Potential secrets detected in $file" >&2
                    echo "Please use environment variables or secure secret management." >&2
                    return 1
                fi
                echo "✓ No secrets detected"
            fi
            ;;
    esac
}

# Main dispatch
case "$file_path" in
    */web/*)
        run_web_validation "$file_path"
        ;;
    */kotlin/*)
        run_kotlin_validation "$file_path"
        ;;
    */firmware/*)
        run_firmware_validation "$file_path"
        ;;
esac

# Always check for secrets in config files
run_root_validation "$file_path"

exit 0
