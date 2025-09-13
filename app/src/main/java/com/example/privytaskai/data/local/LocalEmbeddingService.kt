package com.example.privytaskai.data.local

import javax.inject.Inject
import javax.inject.Singleton
import kotlin.math.sqrt

@Singleton
class LocalEmbeddingService @Inject constructor() {
    
    fun generateEmbedding(text: String, dimensions: Int = 64): List<Float> {
        val vector = FloatArray(dimensions)
        if (text.isEmpty()) return vector.toList()
        
        for (ch in text.lowercase()) {
            val idx = (ch.code * 131) % dimensions
            vector[idx] += 1f
        }
        
        val norm = sqrt(vector.sumOf { (it * it).toDouble() }.toFloat())
        if (norm > 0f) {
            for (i in vector.indices) {
                vector[i] /= norm
            }
        }
        
        return vector.toList()
    }
    
    fun vectorToCsv(vector: List<Float>): String = vector.joinToString(",")
    
    fun csvToVector(csv: String): List<Float> = 
        if (csv.isBlank()) emptyList() 
        else csv.split(',').map { it.toFloat() }
    
    fun cosineSimilarity(a: List<Float>, b: List<Float>): Float {
        val minSize = minOf(a.size, b.size)
        if (minSize == 0) return 0f
        
        var dot = 0f
        var normA = 0f
        var normB = 0f
        
        for (i in 0 until minSize) {
            dot += a[i] * b[i]
            normA += a[i] * a[i]
            normB += b[i] * b[i]
        }
        
        val denominator = (sqrt(normA.toDouble()) * sqrt(normB.toDouble())).toFloat()
        return if (denominator > 0f) dot / denominator else 0f
    }
}