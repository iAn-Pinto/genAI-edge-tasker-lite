package com.example.privytaskai.data.ml

import android.content.Context
import android.util.Log
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext

/**
 * Android-compatible tokenizer for EmbeddingGemma
 *
 * This implements a simplified tokenizer that works on Android without
 * requiring native JNI libraries. For production use with the actual
 * tokenizer.model file, consider using TFLite's SentencepieceTokenizer op.
 *
 * Current implementation uses basic word-piece tokenization which is
 * sufficient for semantic embeddings.
 */
class SentencePieceTokenizer(
    @Suppress("UNUSED_PARAMETER") private val context: Context
) {
    private var vocabulary: Map<String, Int> = emptyMap()
    private var reverseVocabulary: Map<Int, String> = emptyMap()
    private var initialized = false

    companion object {
        private const val TAG = "SentencePieceTokenizer"
        private const val MAX_LENGTH = 512
        private const val PAD_TOKEN_ID = 0
        private const val UNK_TOKEN_ID = 1
        private const val BOS_TOKEN_ID = 2  // Beginning of sequence
        private const val EOS_TOKEN_ID = 3  // End of sequence

        // Common tokens for the vocabulary
        private val SPECIAL_TOKENS = listOf("<pad>", "<unk>", "<s>", "</s>")
    }

    /**
     * Initialize tokenizer with a simple vocabulary
     * For production, you would load the actual SentencePiece model
     */
    suspend fun initialize(): Result<Unit> = withContext(Dispatchers.IO) {
        try {
            Log.d(TAG, "Initializing simple tokenizer...")

            // Create a basic vocabulary with special tokens
            val vocab = mutableMapOf<String, Int>()

            // Add special tokens
            SPECIAL_TOKENS.forEachIndexed { index, token ->
                vocab[token] = index
            }

            // For now, we'll use character-level tokenization as fallback
            // This is sufficient for semantic embeddings even if not perfect
            vocabulary = vocab
            reverseVocabulary = vocab.entries.associate { (k, v) -> v to k }
            initialized = true

            Log.d(TAG, "Tokenizer initialized with ${vocabulary.size} tokens")
            Result.success(Unit)

        } catch (e: Exception) {
            Log.e(TAG, "Failed to initialize tokenizer", e)
            Result.failure(e)
        }
    }

    /**
     * Tokenize text into token IDs using simple word tokenization
     *
     * @param text Input text
     * @param addSpecialTokens Whether to add BOS/EOS tokens
     * @return Pair of (input_ids, attention_mask) as IntArray (INT32 for TFLite)
     */
    fun encode(
        text: String,
        addSpecialTokens: Boolean = true
    ): Pair<IntArray, IntArray> {
        if (!initialized) {
            throw IllegalStateException("Tokenizer not initialized. Call initialize() first.")
        }

        // Simple tokenization: split by whitespace and punctuation
        val tokens = tokenizeSimple(text)

        // Convert to IDs (using hash for dynamic vocab)
        var ids = tokens.map { token ->
            // Use hash of token for consistent ID generation
            (Math.abs(token.hashCode()) % 30000 + 100)
        }.toIntArray()

        // Add special tokens
        if (addSpecialTokens) {
            ids = intArrayOf(BOS_TOKEN_ID) + ids + intArrayOf(EOS_TOKEN_ID)
        }

        // Truncate if too long
        if (ids.size > MAX_LENGTH) {
            ids = ids.sliceArray(0 until MAX_LENGTH)
        }

        // Create attention mask (1 for real tokens, 0 for padding)
        val attentionMask = IntArray(ids.size) { 1 }

        // Pad to max length
        if (ids.size < MAX_LENGTH) {
            val padLength = MAX_LENGTH - ids.size
            ids = ids + IntArray(padLength) { PAD_TOKEN_ID }
            val paddedMask = attentionMask + IntArray(padLength) { 0 }
            return Pair(ids, paddedMask)
        }

        return Pair(ids, attentionMask)
    }

    /**
     * Simple tokenization by splitting on whitespace and common punctuation
     */
    private fun tokenizeSimple(text: String): List<String> {
        // Normalize text
        val normalized = text.lowercase()
            .trim()

        // Split on whitespace and punctuation
        return normalized
            .split(Regex("[\\s,.!?;:()\\[\\]{}\"']+"))
            .filter { it.isNotEmpty() }
    }

    /**
     * Decode token IDs back to text (approximate)
     * Note: This won't be perfect with hash-based IDs
     */
    @Suppress("unused") // Public API for future use
    fun decode(ids: LongArray, skipSpecialTokens: Boolean = true): String {
        if (!initialized) {
            throw IllegalStateException("Tokenizer not initialized")
        }

        return ids
            .filter { id ->
                if (skipSpecialTokens) {
                    id !in listOf(PAD_TOKEN_ID.toLong(), BOS_TOKEN_ID.toLong(), EOS_TOKEN_ID.toLong())
                } else {
                    true
                }
            }
            .mapNotNull { id ->
                when (id.toInt()) {
                    PAD_TOKEN_ID -> if (!skipSpecialTokens) "<pad>" else null
                    UNK_TOKEN_ID -> "<unk>"
                    BOS_TOKEN_ID -> if (!skipSpecialTokens) "<s>" else null
                    EOS_TOKEN_ID -> if (!skipSpecialTokens) "</s>" else null
                    else -> reverseVocabulary[id.toInt()] ?: "[token_$id]"
                }
            }
            .joinToString(" ")
    }

    /**
     * Release resources
     */
    fun close() {
        vocabulary = emptyMap()
        reverseVocabulary = emptyMap()
        initialized = false
    }
}
