#!/bin/bash
# ═══════════════════════════════════════════════════════════════
#  Group Debug & Deploy Expert - Universal Installer v1.0.1
#  通用调试部署专家团队 - 通用安装程序
#  
#  Features:
#  ✅ File integrity verification (SHA256)
#  ✅ Install success/failure detection with reasons
#  ✅ Auto-open user manual after installation
#  ✅ Beginner-friendly step-by-step guidance
#  ✅ Multi-platform support (Linux/macOS/Windows via Git Bash)
# ═══════════════════════════════════════════════════════════════

set -e

# ═══════════════════════════════════════════════════════════════
#  Color Definitions (for beginner-friendly output)
# ═══════════════════════════════════════════════════════════════
RED='\033[0;31m'
GREEN='\033[0;32m'
YELLOW='\033[1;33m'
BLUE='\033[0;34m'
CYAN='\033[0;36m'
NC='\033[0m' # No Color
BOLD='\033[1m'

# ═══════════════════════════════════════════════════════════════
#  Global Variables
# ═══════════════════════════════════════════════════════════════
SCRIPT_DIR="$(cd "$(dirname "${BASH_SOURCE[0]}")" && pwd)"
VERSION="1.0.1"
PACKAGE_NAME="group-debug-deploy-expert"
EXPECTED_FILES=11
INSTALL_SUCCESS=false
ERROR_LOG=""
WARNINGS=0

# Target directories (platform detection)
if [[ "$OSTYPE" == "msys" || "$OSTYPE" == "cygwin" ]]; then
    # Windows (Git Bash/Cygwin)
    TRAE_SKILLS_DIR="$HOME/.trae/skills"
    PLATFORM="windows"
elif [[ "$OSTYPE" == "darwin"* ]]; then
    # macOS
    TRAE_SKILLS_DIR="$HOME/.trae/skills"
    PLATFORM="macos"
else
    # Linux
    TRAE_SKILLS_DIR="$HOME/.trae/skills"
    PLATFORM="linux"
fi

TARGET_DIR="$TRAE_SKILLS_DIR/$PACKAGE_NAME"

# ═══════════════════════════════════════════════════════════════
#  Helper Functions
# ═══════════════════════════════════════════════════════════════

print_header() {
    echo ""
    echo -e "${CYAN}╔══════════════════════════════════════════════════════════╗${NC}"
    echo -e "${CYAN}║${NC} ${BOLD}🛡️  Group Debug & Deploy Expert Installer v${VERSION}${NC}          ${CYAN}║${NC}"
    echo -e "${CYAN}║${NC}    通用调试部署专家团队 - Enterprise-grade AI Team         ${CYAN}║${NC}"
    echo -e "${CYAN}╚══════════════════════════════════════════════════════════╝${NC}"
    echo ""
}

print_step() {
    echo -e "${BLUE}▶ $1${NC}"
}

print_success() {
    echo -e "${GREEN}✅ $1${NC}"
}

print_warning() {
    echo -e "${YELLOW}⚠️  $1${NC}"
    WARNINGS=$((WARNINGS + 1))
}

print_error() {
    echo -e "${RED}❌ $1${NC}"
    ERROR_LOG="$ERROR_LOG\n❌ $1"
}

print_info() {
    echo -e "   ℹ️  $1"
}

# ═══════════════════════════════════════════════════════════════
#  Step 1: Pre-Installation Checks
# ═══════════════════════════════════════════════════════════════

check_prerequisites() {
    print_step "Step 1/6: Checking system requirements..."
    
    # Check if running in supported shell
    if [[ -z "$BASH_VERSION" ]]; then
        print_error "This installer requires Bash. Current shell: $SHELL"
        return 1
    fi
    print_success "Shell: Bash $BASH_VERSION"
    
    # Check disk space (need at least 10MB)
    if command -v df &> /dev/null; then
        AVAILABLE_SPACE=$(df -k "$SCRIPT_DIR" | awk 'NR==2 {print $4}')
        if [[ $AVAILABLE_SPACE -lt 10240 ]]; then
            print_error "Insufficient disk space. Need at least 10MB, available: $((AVAILABLE_SPACE / 1024))MB"
            return 1
        fi
        print_success "Disk space: $((AVAILABLE_SPACE / 1024))MB available"
    fi
    
    # Check write permissions to target directory
    PARENT_DIR=$(dirname "$TARGET_DIR")
    if [[ ! -d "$PARENT_DIR" ]] && ! mkdir -p "$PARENT_DIR" 2>/dev/null; then
        print_error "Cannot create directory: $PARENT_DIR (permission denied)"
        return 1
    fi
    print_success "Write permissions: OK"
    
    echo ""
    return 0
}

# ═══════════════════════════════════════════════════════════════
#  Step 2: File Integrity Verification (SHA256 + Count)
# ═══════════════════════════════════════════════════════════════

verify_integrity() {
    print_step "Step 2/6: Verifying package integrity..."
    
    # Check if checksums file exists
    if [[ ! -f "$SCRIPT_DIR/checksums.sha256" ]]; then
        print_warning "checksums.sha256 not found, skipping SHA256 verification"
    else
        print_info "Running SHA256 integrity check..."
        
        # Run sha256sum verification
        if command -v sha256sum &> /dev/null; then
            VERIFY_RESULT=$(cd "$SCRIPT_DIR" && sha256sum -c checksums.sha256 2>&1) || true
            
            # Parse results
            OK_COUNT=$(echo "$VERIFY_RESULT" | grep -c ": OK$" || true)
            FAIL_COUNT=$(echo "$VERIFY_RESULT" | grep -c ": FAILED$" || true)
            
            if [[ $FAIL_COUNT -gt 0 ]]; then
                print_error "SHA256 verification FAILED for $FAIL_COUNT file(s)"
                print_error "The package may be corrupted or tampered with"
                echo "$VERIFY_RESULT" | grep "FAILED" | while read line; do
                    print_error "  $line"
                done
                return 1
            elif [[ $OK_COUNT -gt 0 ]]; then
                print_success "SHA256 verification passed ($OK_COUNT files)"
            else
                print_warning "Could not verify SHA256 checksums"
            fi
        elif command -v shasum &> /dev/null; then
            # macOS uses shasum instead of sha256sum
            VERIFY_RESULT=$(cd "$SCRIPT_DIR" && shasum -a 256 -c checksums.sha256 2>&1) || true
            
            OK_COUNT=$(echo "$VERIFY_RESULT" | grep -c ": OK$" || true)
            FAIL_COUNT=$(echo "$VERIFY_RESULT" | grep -c ": FAILED$" || true)
            
            if [[ $FAIL_COUNT -gt 0 ]]; then
                print_error "SHA256 verification FAILED for $FAIL_COUNT file(s)"
                return 1
            elif [[ $OK_COUNT -gt 0 ]]; then
                print_success "SHA256 verification passed ($OK_COUNT files)"
            fi
        else
            print_warning "sha256sum/shasum not found, skipping integrity check"
        fi
    fi
    
    # Verify expected file count
    ACTUAL_FILE_COUNT=$(find "$SCRIPT_DIR" -type f \( \
        -name "*.md" -o \
        -name "*.json" -o \
        -name "*.yml" -o \
        -name "LICENSE" -o \
        -name "VERSION" -o \
        -name "checksums.*" -o \
        -name ".gitignore" \
    \) ! -path "*/publish/*" ! -path "*/.git/*" | wc -l | tr -d ' ')
    
    if [[ $ACTUAL_FILE_COUNT -lt $EXPECTED_FILES ]]; then
        print_error "Incomplete package: Expected $EXPECTED_FILES files, found $ACTUAL_FILE_COUNT"
        print_error "Some essential files may be missing or corrupted"
        
        # List missing critical files
        for req_file in "README.md" "package.json" "skill.json" "LICENSE" "VERSION"; do
            if [[ ! -f "$SCRIPT_DIR/$req_file" ]]; then
                print_error "  Missing: $req_file"
            fi
        done
        return 1
    fi
    
    print_success "File count verified: $ACTUAL_FILE_COUNT files present"
    echo ""
    return 0
}

# ═══════════════════════════════════════════════════════════════
#  Step 3: Installation Process
# ═══════════════════════════════════════════════════════════════

install_package() {
    print_step "Step 3/6: Installing $PACKAGE_NAME v$VERSION..."
    
    # Create target directory
    mkdir -p "$TARGET_DIR" 2>/dev/null || {
        print_error "Failed to create installation directory: $TARGET_DIR"
        return 1
    }
    
    # Copy core skill files
    CORE_FILES=(
        ".trae/skills/group-debug-deploy-expert/SKILL.md"
        ".trae/skills/group-debug-deploy-expert/LICENSE"
        ".trae/skills/group-debug-deploy-expert/VERSION"
        ".trae/rules/core_file_protection.md"
        ".trae/rules/project_rules.md"
        ".trae/rules/tier0_manifest.yml"
    )
    
    COPIED_COUNT=0
    FAILED_COUNT=0
    
    for file in "${CORE_FILES[@]}"; do
        SRC="$SCRIPT_DIR/$file"
        DST="$TARGET_DIR/$file"
        
        if [[ -f "$SRC" ]]; then
            DST_DIR=$(dirname "$DST")
            mkdir -p "$DST_DIR" 2>/dev/null
            
            if cp "$SRC" "$DST" 2>/dev/null; then
                COPIED_COUNT=$((COPIED_COUNT + 1))
            else
                print_warning "Failed to copy: $file"
                FAILED_COUNT=$((FAILED_COUNT + 1))
            fi
        else
            print_warning "Source file not found: $file"
            FAILED_COUNT=$((FAILED_COUNT + 1))
        fi
    done
    
    # Copy metadata files
    META_FILES=("README.md" "README-DEPLOY.md" "package.json" "skill.json" "LICENSE" "VERSION" "checksums.sha256")
    
    for file in "${META_FILES[@]}"; do
        SRC="$SCRIPT_DIR/$file"
        DST="$TARGET_DIR/$file"
        
        if [[ -f "$SRC" ]]; then
            if cp "$SRC" "$DST" 2>/dev/null; then
                COPIED_COUNT=$((COPIED_COUNT + 1))
            else
                FAILED_COUNT=$((FAILED_COUNT + 1))
            fi
        fi
    done
    
    if [[ $FAILED_COUNT -gt 0 ]]; then
        print_error "Installation partially failed: $FAILED_COUNT file(s) could not be copied"
        print_info "Successfully copied: $COPIED_COUNT/$((COPIED_COUNT + FAILED_COUNT)) files"
        return 1
    fi
    
    print_success "Installed $COPIED_COUNT files to $TARGET_DIR"
    echo ""
    return 0
}

# ═══════════════════════════════════════════════════════════════
#  Step 4: Post-Installation Verification
# ═══════════════════════════════════════════════════════════════

verify_installation() {
    print_step "Step 4/6: Verifying installation..."
    
    ISSUES=0
    
    # Check SKILL.md exists (critical file)
    if [[ -f "$TARGET_DIR/.trae/skills/group-debug-deploy-expert/SKILL.md" ]]; then
        SKILL_SIZE=$(stat -f%z "$TARGET_DIR/.trae/skills/group-debug-deploy-expert/SKILL.md" 2>/dev/null || stat -c%s "$TARGET_DIR/.trae/skills/group-debug-deploy-expert/SKILL.md" 2>/dev/null || echo "0")
        if [[ $SKILL_SIZE -gt 1000 ]]; then
            print_success "Core skill file: OK ($(echo "scale=1; $SKILL_SIZE/1024" | bc)KB)"
        else
            print_error "Core skill file appears corrupted (size: ${SKILL_SIZE}B)"
            ISSUES=$((ISSUES + 1))
        fi
    else
        print_error "Core skill file (SKILL.md) NOT FOUND"
        ISSUES=$((ISSUES + 1))
    fi
    
    # Check LICENSE exists
    if [[ -f "$TARGET_DIR/LICENSE" ]]; then
        print_success "License file: OK"
    else
        print_warning "License file missing (non-critical)"
    fi
    
    # Check README exists
    if [[ -f "$TARGET_DIR/README.md" ]]; then
        print_success "Documentation (README): OK"
    else
        print_warning "README missing (documentation unavailable)"
    fi
    
    # Check package.json for npm users
    if [[ -f "$TARGET_DIR/package.json" ]]; then
        print_success "Package metadata: OK"
    else
        print_warning "package.json missing (npm features disabled)"
    fi
    
    if [[ $ISSUES -eq 0 ]]; then
        INSTALL_SUCCESS=true
        print_success "Installation verification: PASSED ✓"
        echo ""
        return 0
    else
        print_error "Installation verification: FAILED ($ISSUES issue(s))"
        echo ""
        return 1
    fi
}

# ═══════════════════════════════════════════════════════════════
#  Step 5: Generate Installation Report
# ═══════════════════════════════════════════════════════════════

generate_report() {
    print_step "Step 5/6: Generating installation report..."
    
    REPORT_FILE="$TARGET_DIR/install-report.txt"
    
    cat > "$REPORT_FILE" << EOF
╔══════════════════════════════════════════════════════════╗
║     Installation Report - Group Debug & Deploy Expert      ║
║                   Version $VERSION                           ║
╚══════════════════════════════════════════════════════════╝

Generated: $(date '+%Y-%m-%d %H:%M:%S')
Platform: $(uname -s) $(uname -m)
Installer Version: 1.0.1

─── Status ───────────────────────────────────────────────
Result: $(if [ "$INSTALL_SUCCESS" = true ]; then echo "✅ SUCCESS"; else echo "❌ FAILED"; fi)
Warnings: $WARNINGS
Errors: $(if [ -n "$ERROR_LOG" ]; then echo "Yes"; else echo "None"; fi)

─── Installation Details ──────────────────────────────────
Target Directory: $TARGET_DIR
Files Installed: $COPIED_COUNT
Package Source: $SCRIPT_DIR

─── Verification Results ─────────────────────────────────
Integrity Check: $(if [ $? -eq 0 ]; then echo "PASSED"; else echo "FAILED"; fi)
File Count: $ACTUAL_FILE_COUNT / $EXPECTED_FILES

─── Support Information ───────────────────────────────────
Documentation: $TARGET_DIR/README.md
User Guide: $TARGET_DIR/USER_GUIDE.md (if available)
Issues: https://github.com/xuanji-ai-2026/group-debug-deploy-expert/issues
Email: z18288090942@gmail.com

───────────────────────────────────────────────────────────
EOF

    print_success "Report saved to: $REPORT_FILE"
    echo ""
}

# ═══════════════════════════════════════════════════════════════
#  Step 6: Post-Install Guidance (Beginner-Friendly)
# ═══════════════════════════════════════════════════════════════

show_post_install_guide() {
    print_step "Step 6/6: Showing getting-started guide..."
    
    echo ""
    echo -e "${CYAN}╔══════════════════════════════════════════════════════════╗${NC}"
    echo -e "${CYAN}║${NC}  ${BOLD}🎉 Installation Complete! Next Steps:${NC}                     ${CYAN}║${NC}"
    echo -e "${CYAN}╚══════════════════════════════════════════════════════════╝${NC}"
    echo ""
    
    if [[ "$INSTALL_SUCCESS" = true ]]; then
        echo -e "${GREEN}┌─────────────────────────────────────────────┐${NC}"
        echo -e "${GREEN}│${NC}  ${BOLD}✅ STATUS: INSTALLED SUCCESSFULLY${NC}           ${GREEN}│${NC}"
        echo -e "${GREEN}└─────────────────────────────────────────────┘${NC}"
        echo ""
        
        echo -e "${YELLOW}📖 Quick Start Guide for Beginners:${NC}"
        echo ""
        echo -e "  ${BOLD}1.${NC} Read the User Manual:"
        echo -e "     📄 $TARGET_DIR/README.md"
        echo ""
        echo -e "  ${BOLD}2.${NC} First Time Usage Example:"
        echo -e '     Say: "Help me debug my Spring Boot project"'
        echo ""
        echo -e "  ${BOLD}3.${NC} Learn the 21 Iron Principles:"
        echo -e "     See Section 3 in README.md"
        echo ""
        echo -e "  ${BOLD}4.${NC} Need Help?"
        echo -e "     📧 Email: z18288090942@gmail.com"
        echo -e "     📱 Phone: +86 19537722739"
        echo -e "     🌐 Issues: https://github.com/xuanji-ai-2026/group-debug-deploy-expert/issues"
        echo ""
        
        # Try to open documentation automatically
        if command -v open &> /dev/null; then
            # macOS
            echo -e "${BLUE}ℹ️  Opening user manual...${NC}"
            open "$TARGET_DIR/README.md" 2>/dev/null &
        elif command -v xdg-open &> /dev/null; then
            # Linux
            echo -e "${BLUE}ℹ️  Opening user manual...${NC}"
            xdg-open "$TARGET_DIR/README.md" 2>/dev/null &
        elif [[ "$PLATFORM" == "windows" ]] && command -v start &> /dev/null; then
            # Windows
            echo -e "${BLUE}ℹ️  Opening user manual...${NC}"
            start "$TARGET_DIR\README.md" 2>/dev/null &
        else
            echo -e "${YELLOW}ℹ️  Manual open required:${NC}"
            echo -e "     Please open manually: $TARGET_DIR/README.md"
        fi
        
    else
        echo -e "${RED}┌─────────────────────────────────────────────┐${NC}"
        echo -e "${RED}│${NC}  ${BOLD}❌ STATUS: INSTALLATION FAILED${NC}               ${RED}│${NC}"
        echo -e "${RED}└─────────────────────────────────────────────┘${NC}"
        echo ""
        
        echo -e "${YELLOW}🔍 Troubleshooting Steps:${NC}"
        echo ""
        echo -e "  ${BOLD}1.${NC} Review error messages above"
        echo -e "  ${BOLD}2.${N} Check permissions on target directory:"
        echo -e "     ls -la $TRAE_SKILLS_DIR"
        echo ""
        echo -e "  ${BOLD}3.${NC} Ensure disk space is sufficient"
        echo -e "  ${BOLD}4.${NC} Re-download the package and try again"
        echo ""
        echo -e "  ${BOLD}5.${NC} Contact support with this info:"
        echo -e "     Platform: $(uname -s) $(uname -m)"
        echo -e "     Error Log:$ERROR_LOG"
        echo -e "     📧 z18288090942@gmail.com"
        echo ""
    fi
    
    echo ""
    echo -e "${CYAN}═══════════════════════════════════════════════════${NC}"
    echo -e "${CYAN} Thank you for choosing Group Debug & Deploy Expert!${NC}"
    echo -e "${CYAN} 感谢您选择通用调试部署专家团队！${NC}"
    echo -e "${CYAN}═══════════════════════════════════════════════════${NC}"
    echo ""
}

# ═══════════════════════════════════════════════════════════════
#  Main Installation Flow
# ═══════════════════════════════════════════════════════════════

main() {
    print_header
    
    # Execute installation steps
    if ! check_prerequisites; then
        show_post_install_guide
        exit 1
    fi
    
    if ! verify_integrity; then
        print_error "Package integrity verification failed. Aborting installation."
        show_post_install_guide
        exit 1
    fi
    
    if ! install_package; then
        print_error "Installation process failed."
        verify_installation || true
        generate_report || true
        show_post_install_guide
        exit 1
    fi
    
    if ! verify_installation; then
        generate_report || true
        show_post_install_guide
        exit 1
    fi
    
    generate_report
    show_post_install_guide
    
    # Exit with appropriate code
    if [[ "$INSTALL_SUCCESS" = true ]]; then
        exit 0
    else
        exit 1
    fi
}

# Run main function
main "$@"
