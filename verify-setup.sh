#!/bin/bash

# Setup Verification Script for Slay the Spire Mods
# Checks JDK version, Maven, and bytecode compatibility

set -e

echo "========================================="
echo "  STS Mods Setup Verification"
echo "========================================="
echo ""

# Color codes
RED='\033[0;31m'
GREEN='\033[0;32m'
YELLOW='\033[1;33m'
NC='\033[0m' # No Color

# Check functions
check_pass() {
    echo -e "${GREEN}✓${NC} $1"
}

check_fail() {
    echo -e "${RED}✗${NC} $1"
}

check_warn() {
    echo -e "${YELLOW}⚠${NC} $1"
}

# 1. Check Java version
echo "1. Checking Java installation..."
if command -v java &> /dev/null; then
    JAVA_VERSION=$(java -version 2>&1 | head -n 1)
    echo "   Found: $JAVA_VERSION"

    # Extract major version
    if [[ $JAVA_VERSION =~ \"([0-9]+) ]]; then
        MAJOR_VERSION="${BASH_REMATCH[1]}"
        if [ "$MAJOR_VERSION" -ge 8 ]; then
            check_pass "Java $MAJOR_VERSION is installed"
        else
            check_fail "Java version too old. Need Java 8 or higher"
            exit 1
        fi
    fi
else
    check_fail "Java not found. Please install JDK."
    exit 1
fi
echo ""

# 2. Check JAVA_HOME
echo "2. Checking JAVA_HOME..."
if [ -n "$JAVA_HOME" ]; then
    check_pass "JAVA_HOME is set: $JAVA_HOME"
else
    check_warn "JAVA_HOME not set (optional but recommended)"
fi
echo ""

# 3. Check Maven
echo "3. Checking Maven installation..."
if command -v mvn &> /dev/null; then
    MVN_VERSION=$(mvn -version | head -n 1)
    echo "   Found: $MVN_VERSION"
    check_pass "Maven is installed"
else
    check_fail "Maven not found. Please install Maven 3.6+"
    exit 1
fi
echo ""

# 4. Check STS installation
echo "4. Checking Slay the Spire installation..."
if [ -n "$STS_INSTALL_DIR" ]; then
    if [ -f "$STS_INSTALL_DIR/desktop-1.0.jar" ]; then
        check_pass "STS installation found: $STS_INSTALL_DIR"

        # Check for required mod files
        if [ -f "$STS_INSTALL_DIR/mods/ModTheSpire.jar" ]; then
            check_pass "ModTheSpire.jar found"
        else
            check_warn "ModTheSpire.jar not found in mods folder"
        fi

        if [ -f "$STS_INSTALL_DIR/mods/BaseMod.jar" ]; then
            check_pass "BaseMod.jar found"
        else
            check_warn "BaseMod.jar not found in mods folder"
        fi
    else
        check_fail "desktop-1.0.jar not found in STS_INSTALL_DIR"
    fi
else
    check_warn "STS_INSTALL_DIR not set. You'll need to configure this."
    echo "   Set with: export STS_INSTALL_DIR=/path/to/SlayTheSpire"
fi
echo ""

# 5. Test Maven compilation
echo "5. Testing Maven compilation..."
if mvn clean compile -q 2>&1 | grep -q "BUILD SUCCESS"; then
    check_pass "Maven can compile the project"

    # Check bytecode version
    if [ -f "ascension-100/target/classes/com/stsmod/ascension100/Ascension100Mod.class" ]; then
        BYTECODE_VERSION=$(javap -verbose ascension-100/target/classes/com/stsmod/ascension100/Ascension100Mod.class 2>/dev/null | grep "major version" | awk '{print $NF}')

        if [ "$BYTECODE_VERSION" = "52" ]; then
            check_pass "Bytecode version is 52 (Java 8 compatible)"
        else
            check_warn "Bytecode version is $BYTECODE_VERSION (expected 52 for Java 8)"
        fi
    fi
else
    check_fail "Maven compilation failed. Check pom.xml configuration."
fi
echo ""

# 6. Summary
echo "========================================="
echo "  Verification Summary"
echo "========================================="
echo ""

if [ -n "$JAVA_HOME" ] && [ -n "$STS_INSTALL_DIR" ]; then
    echo -e "${GREEN}✓ Setup looks good!${NC}"
    echo ""
    echo "Next steps:"
    echo "  1. Run: mvn clean package"
    echo "  2. Copy JARs to ModTheSpire mods folder"
    echo "  3. Launch game and enable mods"
else
    echo -e "${YELLOW}⚠ Setup incomplete${NC}"
    echo ""
    echo "Required actions:"
    [ -z "$JAVA_HOME" ] && echo "  - Set JAVA_HOME environment variable"
    [ -z "$STS_INSTALL_DIR" ] && echo "  - Set STS_INSTALL_DIR environment variable"
    echo ""
    echo "See SETUP.md for detailed instructions"
fi
echo ""
