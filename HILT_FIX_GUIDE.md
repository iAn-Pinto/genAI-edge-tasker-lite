# Quick Fix Guide: Hilt Injection Error

## Problem
`PDFReader could not be resolved` during Hilt annotation processing

## Root Cause
Android Studio is showing cached compilation errors. The actual code is correct, but Hilt's annotation processor needs to regenerate the dependency injection code.

## Solution: Clean Build & Rebuild

### Option 1: Using Android Studio (Recommended)
1. **Build > Clean Project**
2. **File > Invalidate Caches / Restart... > Invalidate and Restart**
3. Wait for Android Studio to restart
4. **Build > Rebuild Project**
5. Wait for Hilt annotation processing to complete (~2-3 minutes)

### Option 2: Using Terminal
```bash
cd /Users/ianpinto/StudioProjects/genAI-edge-tasker-lite

# Make script executable
chmod +x rebuild-hilt.sh

# Run the rebuild script
./rebuild-hilt.sh
```

Then in Android Studio:
- **File > Sync Project with Gradle Files**

### Option 3: Manual Commands
```bash
cd /Users/ianpinto/StudioProjects/genAI-edge-tasker-lite

# Clean everything
./gradlew clean
rm -rf .gradle/
rm -rf app/build/
rm -rf build/

# Rebuild with Hilt processing
./gradlew :app:kaptDebugKotlin --rerun-tasks
```

## What's Happening
Hilt uses Kotlin Annotation Processing Tool (KAPT) to generate dependency injection code at compile time. The error occurs because:

1. `MLModule` provides `PDFReader` via `@Provides` annotation
2. `IndexDocumentUseCase` injects `PDFReader` via constructor injection
3. KAPT needs to scan all modules and generate binding code
4. Old cached bytecode is preventing KAPT from seeing the new `PDFReader` provider

## Verification
After rebuild, check these files were generated:
```
app/build/generated/source/kapt/debug/
├── com/example/privytaskai/di/MLModule_ProvidePDFReaderFactory.java
├── com/example/privytaskai/di/MLModule_ProvideDocumentChunkerFactory.java
└── ... (other Hilt generated files)
```

## If Error Persists
1. Check that `PDFReader.kt` compiles without errors
2. Verify `MLModule.kt` has no compilation errors (warnings are OK)
3. Ensure all imports in `IndexDocumentUseCase.kt` are resolved
4. Check that Hilt plugin is properly configured in `build.gradle.kts`:
   ```kotlin
   plugins {
       id("com.android.application")
       id("org.jetbrains.kotlin.android")
       id("org.jetbrains.kotlin.kapt")
       id("dagger.hilt.android.plugin")  // ← This must be present
   }
   ```

## Expected Result
After successful rebuild, the error should disappear and you'll see:
- ✅ No compilation errors in any `.kt` files
- ✅ Hilt-generated factory classes in `build/generated/`
- ✅ App builds successfully with `./gradlew assembleDebug`

---

**The code is already correct - this is purely a build cache issue!** 🎯

