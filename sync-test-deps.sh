#!/bin/bash

# Force Gradle Sync and Download Dependencies
# Run this to resolve the BaseUnitTest.kt testing dependency error

echo "🔄 Forcing Gradle sync to download test dependencies..."

cd /Users/ianpinto/StudioProjects/genAI-edge-tasker-lite

# Step 1: Clean build cache
echo "1️⃣ Cleaning build cache..."
./gradlew clean

# Step 2: Delete Gradle cache to force re-download
echo "2️⃣ Clearing Gradle dependency cache..."
rm -rf ~/.gradle/caches/modules-2/files-2.1/androidx.arch.core/

# Step 3: Sync and download all dependencies
echo "3️⃣ Downloading dependencies..."
./gradlew :app:dependencies --refresh-dependencies

# Step 4: Compile test sources to verify
echo "4️⃣ Compiling test sources..."
./gradlew :app:compileDebugUnitTestKotlin

echo ""
echo "✅ Done! Now in Android Studio:"
echo "   1. File > Sync Project with Gradle Files"
echo "   2. File > Invalidate Caches / Restart (if needed)"
echo ""

