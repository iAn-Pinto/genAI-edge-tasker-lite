#!/bin/bash

# Script to clean build cache and rebuild Hilt dependencies
# Run this from Android Studio Terminal or command line

echo "🧹 Cleaning build cache..."
./gradlew clean

echo "📦 Deleting .gradle cache..."
rm -rf .gradle/

echo "🗑️  Clearing build directories..."
rm -rf app/build/
rm -rf build/

echo "🔄 Rebuilding project with Hilt annotation processing..."
./gradlew :app:kaptDebugKotlin --rerun-tasks

echo "✅ Done! Now sync your project in Android Studio (File -> Sync Project with Gradle Files)"

