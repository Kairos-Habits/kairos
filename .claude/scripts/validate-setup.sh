#!/bin/bash
# Validates Claude Code automation setup for Kairos project

set -e

echo "🔍 Validating Claude Code Automation Setup..."
echo ""

# Color codes
GREEN='\033[0;32m'
RED='\033[0;31m'
YELLOW='\033[1;33m'
NC='\033[0m' # No Color

ERRORS=0
WARNINGS=0

# Check 1: Directory structure
echo "📁 Checking directory structure..."
if [ -d ".claude/skills" ] && [ -d ".claude/agents" ]; then
    echo -e "${GREEN}✓${NC} Directory structure present"
else
    echo -e "${RED}✗${NC} Missing .claude directories"
    ERRORS=$((ERRORS + 1))
fi

# Check 2: Skills exist
echo ""
echo "🎯 Checking skills..."
SKILLS=("migration-helper" "adhd-first-review")
for skill in "${SKILLS[@]}"; do
    if [ -f ".claude/skills/$skill/SKILL.md" ]; then
        # Validate frontmatter
        if grep -q "^name: $skill" ".claude/skills/$skill/SKILL.md" && \
           grep -q "^description:" ".claude/skills/$skill/SKILL.md"; then
            echo -e "${GREEN}✓${NC} Skill '$skill' valid"
        else
            echo -e "${YELLOW}⚠${NC} Skill '$skill' has invalid frontmatter"
            WARNINGS=$((WARNINGS + 1))
        fi
    else
        echo -e "${RED}✗${NC} Skill '$skill' missing"
        ERRORS=$((ERRORS + 1))
    fi
done

# Check 3: Subagents exist
echo ""
echo "🤖 Checking subagents..."
AGENTS=("compose-reviewer" "clean-arch-enforcer")
for agent in "${AGENTS[@]}"; do
    if [ -f ".claude/agents/$agent.md" ]; then
        # Check for required sections
        if grep -q "## Role" ".claude/agents/$agent.md" && \
           grep -q "## Invocation" ".claude/agents/$agent.md"; then
            echo -e "${GREEN}✓${NC} Subagent '$agent' valid"
        else
            echo -e "${YELLOW}⚠${NC} Subagent '$agent' missing required sections"
            WARNINGS=$((WARNINGS + 1))
        fi
    else
        echo -e "${RED}✗${NC} Subagent '$agent' missing"
        ERRORS=$((ERRORS + 1))
    fi
done

# Check 4: Hooks configuration
echo ""
echo "⚡ Checking hooks configuration..."
if [ -f ".claude/settings.json" ]; then
    if grep -q "preToolUse" .claude/settings.json && \
       grep -q "postToolUse" .claude/settings.json; then
        echo -e "${GREEN}✓${NC} Hooks configured (preToolUse, postToolUse)"
    else
        echo -e "${YELLOW}⚠${NC} Incomplete hooks configuration"
        WARNINGS=$((WARNINGS + 1))
    fi
else
    echo -e "${RED}✗${NC} settings.json missing"
    ERRORS=$((ERRORS + 1))
fi

# Check 5: ktlint/Spotless setup
echo ""
echo "🔧 Checking ktlint/Spotless setup..."
if grep -q "com.diffplug.spotless" build.gradle.kts 2>/dev/null; then
    echo -e "${GREEN}✓${NC} Spotless plugin configured"
else
    echo -e "${YELLOW}⚠${NC} Spotless plugin not configured (optional - see .claude/README.md)"
    echo "   Run: Add spotless plugin to build.gradle.kts"
    WARNINGS=$((WARNINGS + 1))
fi

# Check 6: Room schema export
echo ""
echo "🗄️ Checking Room schema export setup..."
if [ -f "data/build.gradle.kts" ]; then
    if grep -q "androidx.room" data/build.gradle.kts && \
       grep -q "schemaDirectory" data/build.gradle.kts; then
        echo -e "${GREEN}✓${NC} Room schema export configured"
    else
        echo -e "${YELLOW}⚠${NC} Room schema export not configured (needed for migrations)"
        echo "   See migration-helper skill for setup instructions"
        WARNINGS=$((WARNINGS + 1))
    fi
else
    echo -e "${YELLOW}⚠${NC} data module not found yet (will be needed later)"
fi

# Check 7: GitHub CLI
echo ""
echo "🐙 Checking GitHub CLI..."
if command -v gh &> /dev/null; then
    GH_VERSION=$(gh --version | head -1)
    echo -e "${GREEN}✓${NC} GitHub CLI installed ($GH_VERSION)"
else
    echo -e "${YELLOW}⚠${NC} GitHub CLI not installed (optional - enhances github MCP)"
    echo "   Install: https://cli.github.com/"
    WARNINGS=$((WARNINGS + 1))
fi

# Summary
echo ""
echo "━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━"
echo "Summary:"
echo "━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━"

if [ $ERRORS -eq 0 ] && [ $WARNINGS -eq 0 ]; then
    echo -e "${GREEN}✅ All checks passed! Claude Code automations ready.${NC}"
    exit 0
elif [ $ERRORS -eq 0 ]; then
    echo -e "${YELLOW}⚠️  Setup complete with $WARNINGS warning(s).${NC}"
    echo ""
    echo "Warnings are optional improvements. Core automations are functional."
    exit 0
else
    echo -e "${RED}❌ Setup incomplete: $ERRORS error(s), $WARNINGS warning(s).${NC}"
    echo ""
    echo "Fix errors above before using Claude Code automations."
    exit 1
fi
