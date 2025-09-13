package com.example.privytaskai

import org.junit.Assert.assertEquals
import org.junit.Test
import kotlin.math.sqrt

class CosineTest {
    private fun cosine(a: FloatArray, b: FloatArray): Float {
        val n = minOf(a.size, b.size)
        if (n == 0) return 0f
        var dot = 0f
        var na = 0f
        var nb = 0f
        for (i in 0 until n) {
            dot += a[i] * b[i]
            na += a[i] * a[i]
            nb += b[i] * b[i]
        }
        val denom = (sqrt(na.toDouble()) * sqrt(nb.toDouble())).toFloat()
        return if (denom > 0f) dot / denom else 0f
    }

    @Test
    fun test_cosine_identity() {
        val v = floatArrayOf(1f, 2f, 3f)
        assertEquals(1f, cosine(v, v), 1e-6f)
    }

    @Test
    fun test_cosine_orthogonal() {
        val a = floatArrayOf(1f, 0f)
        val b = floatArrayOf(0f, 1f)
        assertEquals(0f, cosine(a, b), 1e-6f)
    }
}
