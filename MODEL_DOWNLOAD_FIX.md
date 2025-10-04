# Fix: Model File Download Required

## Problem
The app crashes with: `ByteBuffer is not a valid TensorFlow Lite model flatbuffer`

**Root Cause:** The `embeddinggemma_512.tflite` file in `app/src/main/assets/` is only 144 bytes instead of the required ~179MB. This is a placeholder file - the actual model needs to be downloaded.

## Solution: Download the Correct Model

### Option 1: Manual Download (RECOMMENDED - Most Reliable)

1. **Visit HuggingFace:**
   - Go to: https://huggingface.co/litert-community/embeddinggemma-300m
   
2. **Navigate to Files:**
   - Click on the **"Files and versions"** tab
   
3. **Download the Model:**
   - Click on **`embeddinggemma_512.tflite`** (179 MB)
   - Click the **download button (↓)** on the right
   - Wait for the 179MB download to complete
   
4. **Replace the File:**
   ```bash
   # From your project root
   cd app/src/main/assets
   rm embeddinggemma_512.tflite  # Delete the 144-byte placeholder
   # Move your downloaded file here
   mv ~/Downloads/embeddinggemma_512.tflite .
   ```

5. **Verify the File Size:**
   ```bash
   ls -lh embeddinggemma_512.tflite
   # Should show ~179M
   ```

### Option 2: Using Git LFS (Advanced)

If you have `git-lfs` installed:

```bash
# Install git-lfs first (if not already installed)
brew install git-lfs  # macOS
# or: sudo apt-get install git-lfs  # Linux

# Then run the download script
./download-model.sh
```

### Option 3: Using wget with Authentication

```bash
cd app/src/main/assets
rm embeddinggemma_512.tflite

# This may require HuggingFace authentication token
wget --no-check-certificate \
     -O embeddinggemma_512.tflite \
     "https://huggingface.co/litert-community/embeddinggemma-300m/resolve/main/embeddinggemma_512.tflite?download=true"
```

## Verification Steps

After downloading, verify the file:

```bash
cd app/src/main/assets
ls -lh embeddinggemma_512.tflite
```

You should see approximately **179MB** (not 144 bytes).

## After Download

1. **Clean the project** in Android Studio:
   - Build → Clean Project
   
2. **Rebuild the project:**
   - Build → Rebuild Project
   
3. **Run the app:**
   - The model should now initialize successfully
   
4. **Check Logcat:**
   - Look for: `"Model file size: 179MB - looks good!"`
   - If successful: `"Model initialized successfully"`

## Enhanced Error Handling

The app now includes smart error detection:

✅ **Before Loading:** Checks if model file is too small (< 1MB)  
✅ **During Loading:** Catches invalid flatbuffer errors  
✅ **Clear Messages:** Shows download instructions in Logcat  
✅ **File Size Check:** Validates the model is the correct size  

### What You'll See in Logcat

If the model is **missing or corrupted:**
```
❌ Model file is too small (0KB).

The EmbeddingGemma model should be ~179MB.

📥 Download the model:
1. Visit: https://huggingface.co/litert-community/embeddinggemma-300m
2. Download 'embeddinggemma_512.tflite' (179MB)
3. Place in: app/src/main/assets/embeddinggemma_512.tflite
```

If the model is **valid:**
```
Model file size: 179MB - looks good!
Model initialized successfully (Accelerator: true)
```

## Model Details

- **Name:** EmbeddingGemma 300M
- **File:** `embeddinggemma_512.tflite`
- **Size:** ~179MB (187,761,840 bytes)
- **Output:** 768-dimensional semantic embeddings
- **Performance:** Sub-70ms inference on CPU, ~64ms with NNAPI
- **Memory:** ~200MB RAM usage during inference

## Troubleshooting

### File is still 144 bytes after download
- Make sure you clicked the **download button (↓)** on HuggingFace, not just opened the file
- Check your Downloads folder for the actual large file
- Don't right-click → Save As (this may save the HTML page instead)

### Download is very slow
- The file is 179MB, so it will take time depending on your connection
- Use a stable network connection
- Consider downloading via browser (Option 1) instead of command line

### Still getting flatbuffer error
- Delete the file completely and re-download
- Verify the file size is exactly ~179MB (not 144 bytes, not 0 bytes)
- Rebuild the project after replacing the file

## Why This Happened

The initial `embeddinggemma_512.tflite` was a 144-byte placeholder (likely a Git LFS pointer file). HuggingFace uses Git LFS for large files, so direct `curl` downloads don't work without proper LFS handling or authentication.

The fix ensures you download the actual 179MB model file, not the pointer.
