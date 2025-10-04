# ML Model Assets

This directory contains machine learning models for the app.

## Required Models

### EmbeddingGemma Model
- **File:** `embeddinggemma_512.tflite`
- **Size:** ~179MB
- **Purpose:** Generates 768-dimensional semantic embeddings for RAG functionality
- **Download:** Run `../../download-model-helper.sh` from project root

### Tokenizer Model
- **File:** `tokenizer.model`
- **Size:** ~2MB
- **Purpose:** Tokenizes text for EmbeddingGemma model
- **Download:** Automatically downloaded with the model script

## ⚠️ Important Notes

**DO NOT commit the `.tflite` model files to git!**
- They are too large (179MB) for git
- They are listed in `.gitignore`
- Use the download script instead

## Download Instructions

From the project root directory:

```bash
./download-model-helper.sh
```

Or manually download from:
https://huggingface.co/litert-community/embeddinggemma-300m

## Verification

After downloading, verify the files exist:

```bash
ls -lh app/src/main/assets/
```

You should see:
- `embeddinggemma_512.tflite` (~179MB)
- `tokenizer.model` (~2MB)

