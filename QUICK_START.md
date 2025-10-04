# EmbeddingGemma Quick Reference

## Download Models (Do This First!)

```bash
cd app/src/main/assets
wget https://huggingface.co/litert-community/embeddinggemma-300m/resolve/main/embeddinggemma_512.tflite
wget https://huggingface.co/litert-community/embeddinggemma-300m/resolve/main/tokenizer.model
```

## Add Test to MainActivity

```kotlin
import com.example.privytaskai.util.EmbeddingModelTester
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch

// In MainActivity.onCreate() - after setContent { }
CoroutineScope(Dispatchers.Main).launch {
    val tester = EmbeddingModelTester(this@MainActivity)
    tester.runAllTests()  // Check LogCat for results
}
```

## Watch LogCat

```bash
adb logcat | grep -E "EmbeddingGemma|EmbeddingModelTester"
```

## Expected Output

```
✅ Model initialized successfully
✅ Generated 768-dim embedding with 745 non-zero values
Similarity (similar texts): 0.87
Similarity (different texts): 0.23
✅ Cosine similarity working correctly
📊 Inference Performance:
   Average: 64ms
   Max: 89ms
   Min: 52ms
✅ Performance acceptable for budget devices
📊 Test Results: 4/4 passed
✅ All tests PASSED
```

## Performance Targets

| Metric | Target | Acceptable |
|--------|--------|------------|
| Inference (CPU) | < 70ms | < 100ms |
| Inference (GPU) | < 50ms | < 80ms |
| Memory Usage | 200MB | 250MB |
| Embedding Dim | 768 | 768 |

## Common Issues

### Model Not Found
```bash
# Verify files exist
ls -lh app/src/main/assets/
# Should show embeddinggemma_512.tflite (179MB) and tokenizer.model (2MB)
```

### Slow Performance
- Test on physical device (emulator is 5-10x slower)
- Ensure GPU delegate loads (check LogCat)
- Close other apps

### Build Failure
```bash
./gradlew clean
./gradlew assembleDebug
```

## Quick Test Flow

1. Download models → 2. Build app → 3. Run app → 4. Check LogCat → 5. Verify tests pass

**Success = All 4 tests pass + <100ms inference time**
