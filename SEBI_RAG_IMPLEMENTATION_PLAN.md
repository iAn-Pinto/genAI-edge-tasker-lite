# SEBI RAG Implementation Plan

## Project Goal
Build an on-device RAG system to analyze SEBI circulars with keyword queries, running entirely offline on budget Android devices (4GB RAM).

## Architecture Decision: Stick with Hilt ✅
- Already configured and working
- Compile-time safety for ML dependencies
- Better than Koin for production apps
- Industry standard

---

## Phase 1: Upgrade Embedding System (CURRENT PHASE)

### Current State
- Basic character-based embeddings (64 dimensions)
- Not semantically meaningful
- Won't work well for SEBI circular queries

### Target State
- EmbeddingGemma 308M (768 dimensions)
- Semantic understanding of financial/legal text
- Sub-70ms inference on CPU
- ~200MB RAM usage

### Implementation Steps

#### Step 1.1: Update Dependencies ✓ (Complete this first)
Add to `app/build.gradle.kts`:
```kotlin
// AI Edge / LiteRT for on-device ML
implementation("com.google.ai.edge.litert:litert:1.0.1")
implementation("com.google.ai.edge.litert:litert-gpu:1.0.1") 
implementation("com.google.ai.edge.litert:litert-support:1.0.1")

// HuggingFace tokenizers
implementation("ai.djl:api:0.25.0")
implementation("ai.djl.huggingface:tokenizers:0.25.0")

// PDF parsing (iTextPDF)
implementation("com.itextpdf:itextpdf:5.5.13.3")

// Coroutines for ML ops
implementation("org.jetbrains.kotlinx:kotlinx-coroutines-android:1.9.0") // Already have
```

#### Step 1.2: Download Model Files
Models to download (place in `app/src/main/assets/`):
1. `embeddinggemma_512.tflite` (179MB) - From huggingface.co/litert-community/embeddinggemma-300m
2. `tokenizer.model` (2MB) - Included in same repo

Command:
```bash
cd /Users/ianpinto/StudioProjects/genAI-edge-tasker-lite/app/src/main/assets
wget https://huggingface.co/litert-community/embeddinggemma-300m/resolve/main/embeddinggemma_512.tflite
wget https://huggingface.co/litert-community/embeddinggemma-300m/resolve/main/tokenizer.model
```

#### Step 1.3: Update AndroidManifest.xml
Add to prevent model compression:
```xml
<application>
    ...
    android:extractNativeLibs="true"
</application>
```

Update `app/build.gradle.kts`:
```kotlin
android {
    aaptOptions {
        noCompress("tflite")
    }
}
```

#### Step 1.4: Create EmbeddingGemmaModel
Replace `LocalEmbeddingService` with real ML implementation.

File: `app/src/main/java/com/example/privytaskai/data/ml/EmbeddingGemmaModel.kt`

#### Step 1.5: Update Hilt Module
Create new ML module for dependency injection.

File: `app/src/main/java/com/example/privytaskai/di/MLModule.kt`

#### Step 1.6: Update Repository
Modify `TaskRepositoryImpl` to use new embeddings.

---

## Phase 2: Add PDF Parsing (Week 3)

### Goal
Parse SEBI PDF circulars and extract text.

### Steps
1. Create `PDFReader` class using iTextPDF
2. Implement chunking strategy (100-600 tokens with 20% overlap)
3. Add document indexing use case
4. Test with sample SEBI circular

---

## Phase 3: Implement SQLite-vec (Week 4)

### Goal
Replace Room embeddings with proper vector search.

### Steps
1. Build/download SQLite-vec native library
2. Create VectorDatabase wrapper
3. Migrate from CSV storage to binary vectors
4. Implement k-NN search

---

## Phase 4: Add Gemma 3 Generation (Week 5-6)

### Goal
Generate answers from retrieved context.

### Steps
1. Download Gemma 3 1B INT4 model (~529MB)
2. Integrate MediaPipe LLM Inference API
3. Build RAG pipeline (retrieve → generate)
4. Add streaming response UI

---

## Testing Strategy

### Budget Device Testing
- Redmi Note 10 (4GB RAM, Snapdragon 678)
- Samsung Galaxy A32 (4GB RAM, MediaTek Helio G80)

### Performance Targets (Budget Devices)
- Embedding: < 100ms per chunk
- Vector search: < 200ms (10K vectors)
- End-to-end query: < 5 seconds
- Memory: < 500MB peak usage

---

## Risk Mitigation

### APK Size (Big Risk!)
- Current: ~20MB
- After Phase 1: ~200MB (EmbeddingGemma)
- After Phase 4: ~750MB (+ Gemma 3 1B)

**Solution**: Use Android App Bundle + Dynamic Delivery
- Download models on first run
- Allow users to choose model size

### Memory Pressure
**Solution**: 
- Lazy loading (load LLM only when needed)
- Implement onTrimMemory() callbacks
- Use int4 quantization for all models

### Thermal Throttling
**Solution**:
- Monitor ThermalManager
- Switch to lighter models when hot
- Pause indexing during generation

---

## Current Priority: Step 1.1-1.4

Let's start by:
1. ✅ Update build.gradle.kts dependencies
2. ✅ Create EmbeddingGemmaModel class
3. ✅ Create MLModule for Hilt
4. ⏳ Download model files
5. ⏳ Test embedding generation

Ready to proceed?
