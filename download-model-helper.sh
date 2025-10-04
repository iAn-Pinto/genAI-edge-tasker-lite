#!/bin/bash

# Simple manual download helper for EmbeddingGemma model
# Since HuggingFace requires authentication for automated downloads,
# this script helps you complete the manual download process

echo "============================================"
echo "📥 EmbeddingGemma Model Download Helper"
echo "============================================"
echo ""
echo "⚠️  Automated download requires HuggingFace authentication."
echo "The easiest way is to download manually through your browser."
echo ""
echo "📋 STEP-BY-STEP INSTRUCTIONS:"
echo ""
echo "1️⃣  Open this URL in your browser:"
echo "    https://huggingface.co/litert-community/embeddinggemma-300m/tree/main"
echo ""
echo "2️⃣  Click on 'embeddinggemma_512.tflite' (179 MB)"
echo ""
echo "3️⃣  Click the download button (↓) on the right side"
echo "    (It looks like a down arrow)"
echo ""
echo "4️⃣  Wait for the 179MB download to complete"
echo "    (This may take 2-5 minutes depending on your connection)"
echo ""
echo "5️⃣  Once downloaded, move it to the correct location:"
echo ""

ASSETS_DIR="$(pwd)/app/src/main/assets"
mkdir -p "$ASSETS_DIR"

echo "    CURRENT LOCATION:"
echo "    ~/Downloads/embeddinggemma_512.tflite"
echo ""
echo "    TARGET LOCATION:"
echo "    $ASSETS_DIR/embeddinggemma_512.tflite"
echo ""
echo "    RUN THIS COMMAND:"
echo "    mv ~/Downloads/embeddinggemma_512.tflite \"$ASSETS_DIR/\""
echo ""
echo "6️⃣  Verify the file size:"
echo "    ls -lh \"$ASSETS_DIR/embeddinggemma_512.tflite\""
echo "    (Should show ~179M)"
echo ""
echo "============================================"
echo ""

# Check if file already exists and is valid
if [ -f "$ASSETS_DIR/embeddinggemma_512.tflite" ]; then
    FILE_SIZE=$(stat -f%z "$ASSETS_DIR/embeddinggemma_512.tflite" 2>/dev/null || stat -c%s "$ASSETS_DIR/embeddinggemma_512.tflite" 2>/dev/null)
    FILE_SIZE_MB=$((FILE_SIZE / 1024 / 1024))

    if [ "$FILE_SIZE_MB" -gt 100 ]; then
        echo "✅ GOOD NEWS: Model file already exists and looks valid!"
        echo "   File size: ${FILE_SIZE_MB}MB"
        echo "   Location: $ASSETS_DIR/embeddinggemma_512.tflite"
        echo ""
        echo "🎉 You're ready to build and run the app!"
        exit 0
    else
        echo "⚠️  Model file exists but is too small (${FILE_SIZE_MB}MB)"
        echo "   Expected: ~179MB"
        echo "   Please follow the steps above to download the correct file."
        echo ""
    fi
fi

echo "💡 TIP: You can also open this URL directly:"
echo "   open https://huggingface.co/litert-community/embeddinggemma-300m/tree/main"
echo ""
echo "After downloading, run this script again to verify!"

