#!/bin/bash

# Slay the Spire Mods Build Script (Gradle)
# Usage: ./build.sh [all|ascension|relics]

set -e

echo "======================================"
echo "  Slay the Spire Mods Build Script"
echo "  (Using Gradle)"
echo "======================================"
echo ""

# Check if Gradle wrapper exists, otherwise use system gradle
if [ -f "./gradlew" ]; then
    GRADLE_CMD="./gradlew"
else
    GRADLE_CMD="gradle"
fi

# Function to build a specific module
build_module() {
    local module=$1
    local name=$2

    echo "Building $name..."
    $GRADLE_CMD :$module:clean :$module:build -q

    if [ $? -eq 0 ]; then
        echo "✓ $name build successful"
        echo "  Output: $module/build/libs/*.jar"
    else
        echo "✗ $name build failed"
        exit 1
    fi
    echo ""
}

# Parse command line arguments
case "${1:-all}" in
    all)
        echo "Building all modules..."
        echo ""
        $GRADLE_CMD clean build -q
        echo "✓ All modules built successfully"
        echo "  Ascension 100: ascension-100/build/libs/Ascension100.jar"
        echo "  Custom Relics: custom-relics/build/libs/CustomRelics.jar"
        ;;
    ascension)
        build_module "ascension-100" "Ascension 100"
        ;;
    relics)
        build_module "custom-relics" "Custom Relics"
        ;;
    *)
        echo "Usage: $0 [all|ascension|relics]"
        echo ""
        echo "Options:"
        echo "  all       - Build all modules (default)"
        echo "  ascension - Build Ascension 100 only"
        echo "  relics    - Build Custom Relics only"
        exit 1
        ;;
esac

echo ""
echo "======================================"
echo "  Build Complete!"
echo "======================================"
