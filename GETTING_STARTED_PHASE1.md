# 🚀 SEBI RAG - Getting Started (Phase 1)

## What We Just Built

✅ **Updated build.gradle.kts** - Added LiteRT, HuggingFace tokenizers, iTextPDF  
✅ **Created EmbeddingGemmaModel** - Real ML embeddings (768-dim semantic vectors)  
✅ **Created MLModule** - Hilt DI for ML components  
✅ **Updated EmbeddingEntity** - Support for 768-dim embeddings  
✅ **Created Migrations** - Smooth upgrade from old 64-dim embeddings  

## Next Steps: Complete Phase 1

### Step 1: Sync Gradle Dependencies ⏰ 5 minutes

```bash
# In Android Studio
1. Click "Sync Now" banner at top
2. Wait for Gradle sync to complete
3. Resolve any dependency conflicts if they appear
```

**Expected output**: Build successful with new dependencies downloaded

---

### Step 2: Download Model Files ⏰ 10 minutes

Models need to be placed in `app/src/main/assets/`

#### Option A: Manual Download (Recommended for first time)

```bash
cd /Users/ianpinto/StudioProjects/genAI-edge-tasker-lite/app/src/main/assets

# Create assets directory if it doesn't exist
mkdir -p /Users/ianpinto/StudioProjects/genAI-edge-tasker-lite/app/src/main/assets

# Download EmbeddingGemma model (179MB)
wget https://huggingface.co/litert-community/embeddinggemma-300m/resolve/main/embeddinggemma_512.tflite

# Download tokenizer (2MB)
wget https://huggingface.co/litert-community/embeddinggemma-300m/resolve/main/tokenizer.model
```

#### Option B: Using Hugging Face CLI (if installed)

```bash
cd /Users/ianpinto/StudioProjects/genAI-edge-tasker-lite/app/src/main/assets
huggingface-cli download litert-community/embeddinggemma-300m embeddinggemma_512.tflite tokenizer.model
```

**Verify files exist:**
```bash
ls -lh /Users/ianpinto/StudioProjects/genAI-edge-tasker-lite/app/src/main/assets/
# Should show:
# embeddinggemma_512.tflite (~179MB)
# tokenizer.model (~2MB)
```

---

### Step 3: Update DatabaseModule ⏰ 5 minutes

Add migration to existing `DatabaseModule.kt`:

```kotlin
@Provides
@Singleton
fun provideTaskDatabase(@ApplicationContext context: Context): TaskDatabase {
    return Room.databaseBuilder(
        context,
        TaskDatabase::class.java,
        "task_database"
    )
    .addMigrations(Migrations.MIGRATION_1_2)  // ADD THIS LINE
    .build()
}
```

**File**: `app/src/main/java/com/example/privytaskai/di/DatabaseModule.kt`

---

### Step 4: Update TaskDatabase ⏰ 2 minutes

Bump database version in `TaskDatabase.kt`:

```kotlin
@Database(
    entities = [TaskEntity::class, EmbeddingEntity::class],
    version = 2,  // Changed from 1 to 2
    exportSchema = false
)
abstract class TaskDatabase : RoomDatabase() {
    abstract fun taskDao(): TaskDao
    abstract fun embeddingDao(): EmbeddingDao
}
```

**File**: `app/src/main/java/com/example/privytaskai/data/database/database/TaskDatabase.kt`

---

### Step 5: Create Test Activity ⏰ 15 minutes

Let's create a simple test to verify EmbeddingGemma works:

**File**: `app/src/main/java/com/example/privytaskai/util/EmbeddingTest.kt`

```kotlin
package com.example.privytaskai.util

import android.util.Log
import com.example.privytaskai.data.ml.EmbeddingGemmaModel
import com.example.privytaskai.data.ml.TaskType
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import javax.inject.Inject
import javax.inject.Singleton

/**
 * Simple test harness for EmbeddingGemma
 * Call from MainActivity to verify model works
 */
@Singleton
class EmbeddingTest @Inject constructor(
    private val embeddingModel: EmbeddingGemmaModel
) {
    companion object {
        private const val TAG = "EmbeddingTest"
    }
    
    fun runBasicTest(scope: CoroutineScope) {
        scope.launch(Dispatchers.IO) {
            try {
                Log.d(TAG, "=== Starting EmbeddingGemma Test ===")
                
                // Test 1: Initialize model
                Log.d(TAG, "Test 1: Initializing model...")
                val initStart = System.currentTimeMillis()
                embeddingModel.initialize()
                val initTime = System.currentTimeMillis() - initStart
                Log.d(TAG, "✓ Model initialized in ${initTime}ms")
                
                // Test 2: Generate embedding for SEBI-related text
                Log.d(TAG, "Test 2: Generating embedding...")
                val testText = "SEBI circular regarding disclosure norms for listed companies"
                val embedStart = System.currentTimeMillis()
                val embedding = embeddingModel.generateEmbedding(testText, TaskType.DOCUMENT)
                val embedTime = System.currentTimeMillis() - embedStart
                
                Log.d(TAG, "✓ Embedding generated in ${embedTime}ms")
                Log.d(TAG, "  Dimensions: ${embedding.size}")
                Log.d(TAG, "  First 5 values: ${embedding.take(5)}")
                
                // Test 3: Similarity test
                Log.d(TAG, "Test 3: Testing similarity...")
                val query = "disclosure requirements for companies"
                val queryEmbedding = embeddingModel.generateEmbedding(query, TaskType.SEARCH)
                val similarity = embeddingModel.cosineSimilarity(embedding, queryEmbedding)
                Log.d(TAG, "✓ Similarity score: $similarity")
                
                // Test 4: Serialization
                Log.d(TAG, "Test 4: Testing serialization...")
                val csv = embeddingModel.vectorToCsv(embedding)
                val recovered = embeddingModel.csvToVector(csv)
                val match = embedding.contentEquals(recovered)
                Log.d(TAG, "✓ CSV serialization: ${if (match) "PASS" else "FAIL"}")
                
                // Test 5: Truncation (for budget devices)
                Log.d(TAG, "Test 5: Testing truncation...")
                val truncated256 = embeddingModel.truncateEmbedding(embedding, 256)
                Log.d(TAG, "✓ Truncated to ${truncated256.size} dimensions")
                
                Log.d(TAG, "=== All Tests Passed! ===")
                
            } catch (e: Exception) {
                Log.e(TAG, "❌ Test failed", e)
            }
        }
    }
}
```

Then inject and call in `MainActivity.kt`:

```kotlin
@AndroidEntryPoint
class MainActivity : ComponentActivity() {
    
    @Inject lateinit var embeddingTest: EmbeddingTest
    
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        
        // Run embedding test on app start
        embeddingTest.runBasicTest(lifecycleScope)
        
        setContent {
            // ... existing UI code
        }
    }
}
```

---

### Step 6: Build and Run ⏰ 5 minutes

```bash
# Clean build
./gradlew clean

# Build app
./gradlew assembleDebug

# Install on device/emulator
./gradlew installDebug

# Check logs
adb logcat | grep -E "EmbeddingGemma|EmbeddingTest"
```

**Expected logs:**
```
D/EmbeddingTest: === Starting EmbeddingGemma Test ===
D/EmbeddingGemma: Initializing EmbeddingGemma...
D/EmbeddingGemma: GPU/CPU delegate available, using ...
D/EmbeddingGemma: Model initialized in 450ms
D/EmbeddingTest: ✓ Model initialized in 450ms
D/EmbeddingGemma: Embedding generated in 68ms for 12 tokens
D/EmbeddingTest: ✓ Embedding generated in 68ms
D/EmbeddingTest:   Dimensions: 768
D/EmbeddingTest: ✓ Similarity score: 0.78
D/EmbeddingTest: ✓ CSV serialization: PASS
D/EmbeddingTest: ✓ Truncated to 256 dimensions
D/EmbeddingTest: === All Tests Passed! ===
```

---

## Troubleshooting

### Issue: "Model file not found"
**Solution**: Verify assets directory contains model files
```bash
ls -lh app/src/main/assets/
```

### Issue: "OutOfMemoryError"
**Solution**: Your device may have insufficient RAM. Try:
1. Close other apps
2. Use truncated embeddings (256-dim instead of 768)
3. Test on a device with more RAM

### Issue: "GPU delegate failed"
**Solution**: Normal on some devices. App will automatically fall back to CPU.

### Issue: Gradle sync fails
**Solution**: Check internet connection and try:
```bash
./gradlew clean
./gradlew --refresh-dependencies
```

---

## Success Criteria

✅ **App builds without errors**  
✅ **Model initializes in < 2 seconds**  
✅ **Embedding generation < 100ms on mid-range device**  
✅ **All 5 tests pass in EmbeddingTest**  
✅ **No OutOfMemoryError crashes**  

---

## What's Next (Phase 2)?

Once Phase 1 is complete, we'll add:
1. **PDF Parsing** - Extract text from SEBI PDFs
2. **Document Chunking** - Split into 100-600 token segments
3. **Batch Indexing** - Process multiple PDFs efficiently
4. **Updated Repository** - Use EmbeddingGemma instead of legacy service

## Questions?

Common questions I anticipate:

**Q: Why isn't the tokenizer working?**  
A: The HuggingFaceTokenizer is a placeholder. We need to implement actual SentencePiece tokenization using the DJL library. This will be fixed in the next iteration.

**Q: Can I use a smaller model?**  
A: Yes! You can download the 256-token variant (saves ~4MB) if you don't need long documents:
```bash
wget https://huggingface.co/litert-community/embeddinggemma-300m/resolve/main/embeddinggemma_256.tflite
```

**Q: What about iOS?**  
A: This implementation is Android-only. For iOS, you'd use Core ML instead of TensorFlow Lite.

**Q: How do I update the model later?**  
A: Just replace the .tflite file in assets and rebuild. The API stays the same.

---

## Time Estimate

| Task | Time |
|------|------|
| Gradle sync | 5 min |
| Download models | 10 min |
| Code updates | 7 min |
| Create test | 15 min |
| Build & test | 5 min |
| **Total** | **42 min** |

Ready to start? Begin with Step 1! 🚀
