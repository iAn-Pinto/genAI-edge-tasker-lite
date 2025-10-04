package com.example.privytaskai.data.database.entities

import androidx.room.Entity
import androidx.room.PrimaryKey

/**
 * Updated embedding entity for EmbeddingGemma (768 dimensions)
 * 
 * Storage formats:
 * - CSV: Easy to debug, but 3KB per embedding (768 floats * 4 bytes)
 * - Binary: Production-ready, ~3KB per embedding
 * - Truncated (256-dim): 1KB per embedding (acceptable quality loss < 5%)
 * 
 * For budget devices with 10K SEBI documents:
 * - Full 768-dim: ~30MB RAM for vectors
 * - Truncated 256-dim: ~10MB RAM for vectors
 * 
 * Recommendation: Start with CSV for Phase 1, migrate to binary in Phase 3
 */
@Entity(tableName = "embeddings")
data class EmbeddingEntity(
    @PrimaryKey 
    val taskId: Long,
    
    /**
     * Embedding vector stored as CSV string
     * Format: "0.123,-0.456,0.789,..."
     * 
     * Example migration from 64-dim to 768-dim:
     * - Delete old embeddings and regenerate, OR
     * - Pad old vectors with zeros (not recommended - semantic mismatch)
     */
    val vectorCsv: String,
    
    /**
     * Embedding dimension (64 for old, 768 for EmbeddingGemma)
     * Helps identify which model generated this embedding
     */
    val dimension: Int = 768,
    
    /**
     * Model version for future-proofing
     * "legacy" = old character-based
     * "embeddinggemma-v1" = EmbeddingGemma 308M
     */
    val modelVersion: String = "embeddinggemma-v1",
    
    /**
     * Timestamp when embedding was generated
     * Useful for cache invalidation and debugging
     */
    val createdAt: Long = System.currentTimeMillis()
)
