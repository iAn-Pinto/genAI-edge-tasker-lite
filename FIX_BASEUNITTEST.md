# Fix for BaseUnitTest.kt - Unresolved Reference: testing

## Problem
`androidx.arch.core.executor.testing.InstantTaskExecutorRule` cannot be resolved.

## Solution Applied
Added the missing test dependency to `app/build.gradle.kts`:
```kotlin
testImplementation("androidx.arch.core:core-testing:2.2.0")
```

## Required Action: Sync Gradle

The dependency has been added but you need to sync Gradle files for it to take effect.

### Option 1: Android Studio UI (Recommended)
1. Click the **"Sync Now"** banner that appears at the top of the editor
   - OR -
2. Go to **File > Sync Project with Gradle Files**
3. Wait for the sync to complete (~30 seconds)

### Option 2: Gradle Sync Button
- Click the elephant icon 🐘 in the toolbar and select **"Sync Project with Gradle Files"**

### Option 3: Clean & Rebuild (If sync doesn't work)
1. **Build > Clean Project**
2. **Build > Rebuild Project**

## Verification
After sync completes, the error should disappear and you'll see:
- ✅ Green checkmark next to `import androidx.arch.core.executor.testing.InstantTaskExecutorRule`
- ✅ No red underlines in `BaseUnitTest.kt`

## What This Library Provides
`androidx.arch.core:core-testing` contains:
- `InstantTaskExecutorRule` - Makes LiveData execute synchronously in tests
- Utilities for testing Architecture Components
- Required for unit testing ViewModels and coroutines

## If Error Persists
1. Check internet connection (Gradle needs to download the library)
2. Invalidate caches: **File > Invalidate Caches / Restart**
3. Check `~/.gradle/caches/` for corrupted cache
4. Verify the dependency was added to the test section (not implementation)

---

**The fix is applied - just sync Gradle files to complete!** ✅

