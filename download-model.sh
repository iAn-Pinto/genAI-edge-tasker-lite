#!/bin/bash

# Script to download EmbeddingGemma model for Android app
# This downloads the 179MB model file needed for semantic embeddings

set -e

echo "📥 Downloading EmbeddingGemma 300M model..."
echo ""

# Create assets directory if it doesn't exist
ASSETS_DIR="app/src/main/assets"
mkdir -p "$ASSETS_DIR"

# Navigate to assets directory
cd "$ASSETS_DIR"

# Remove old/corrupted model file if exists
if [ -f "embeddinggemma_512.tflite" ]; then
    echo "🗑️  Removing old model file..."
    rm embeddinggemma_512.tflite
fi

echo "⬇️  Downloading embeddinggemma_512.tflite (~179MB)..."
echo "This may take a few minutes depending on your connection..."
echo ""

# Method 1: Try direct download with curl (works better than wget for HuggingFace)
echo "Attempting direct download..."
if command -v curl &> /dev/null; then
    # Use the CDN link which doesn't require authentication
    curl -L --progress-bar \
         -H "User-Agent: Mozilla/5.0" \
         -o embeddinggemma_512.tflite \
         "https://huggingface.co/litert-community/embeddinggemma-300m/resolve/main/embeddinggemma_512.tflite"

    DOWNLOAD_SUCCESS=$?
else
    DOWNLOAD_SUCCESS=1
fi

# Method 2: If curl failed, try git-lfs clone
if [ $DOWNLOAD_SUCCESS -ne 0 ] || [ ! -f "embeddinggemma_512.tflite" ]; then
    echo ""
    echo "Direct download failed, trying git-lfs method..."

    if ! command -v git-lfs &> /dev/null; then
        echo ""
        echo "❌ Both direct download and git-lfs failed."
        echo ""
        echo "⚠️  git-lfs is not installed. Please install it first:"
        echo "  macOS:   brew install git-lfs"
        echo "  Linux:   sudo apt-get install git-lfs"
        echo ""
        echo "Or download manually (RECOMMENDED):"
        echo "1. Visit: https://huggingface.co/litert-community/embeddinggemma-300m"
        echo "2. Click 'Files and versions' tab"
        echo "3. Click 'embeddinggemma_512.tflite'"
        echo "4. Click the download button (↓)"
        echo "5. Move to: $PWD"
        exit 1
    fi

    echo "Using git-lfs to clone the model..."
    TEMP_DIR=$(mktemp -d)
    cd "$TEMP_DIR"

    git lfs install
    GIT_LFS_SKIP_SMUDGE=1 git clone https://huggingface.co/litert-community/embeddinggemma-300m
    cd embeddinggemma-300m
    git lfs pull --include="embeddinggemma_512.tflite"

    # Get back to assets directory
    cd "$OLDPWD"
    mv "$TEMP_DIR/embeddinggemma-300m/embeddinggemma_512.tflite" .
    rm -rf "$TEMP_DIR"
fi

# Verify download
if [ ! -f "embeddinggemma_512.tflite" ]; then
    echo "❌ Download failed!"
    echo ""
    echo "Please download manually:"
    echo "1. Visit: https://huggingface.co/litert-community/embeddinggemma-300m"
    echo "2. Click on 'Files and versions' tab"
    echo "3. Click on 'embeddinggemma_512.tflite'"
    echo "4. Click the download button (↓) on the right"
    echo "5. Move it to: $PWD"
    exit 1
fi

FILE_SIZE=$(stat -f%z embeddinggemma_512.tflite 2>/dev/null || stat -c%s embeddinggemma_512.tflite 2>/dev/null)
FILE_SIZE_MB=$((FILE_SIZE / 1024 / 1024))

echo ""
echo "✅ Download complete!"
echo "📊 File size: ${FILE_SIZE_MB}MB"

if [ "$FILE_SIZE_MB" -lt 100 ]; then
    echo "⚠️  WARNING: File seems too small. Expected ~179MB."
    echo ""
    echo "The download may have failed. Please download manually:"
    echo "1. Visit: https://huggingface.co/litert-community/embeddinggemma-300m"
    echo "2. Click on 'embeddinggemma_512.tflite' file"
    echo "3. Click the download button (↓)"
    echo "4. Move the downloaded file to: $PWD"
    exit 1
fi

echo ""
echo "🎉 Model ready! You can now build and run the app."
echo ""
echo "Location: $(pwd)/embeddinggemma_512.tflite"
