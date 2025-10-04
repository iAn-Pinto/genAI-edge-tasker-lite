package com.example.privytaskai.di

import android.content.Context
import com.example.privytaskai.data.ml.EmbeddingGemmaModel
import com.example.privytaskai.data.ml.SentencePieceTokenizer
import com.example.privytaskai.data.pdf.PDFReader
import com.example.privytaskai.data.pdf.DocumentChunker
import dagger.Module
import dagger.Provides
import dagger.hilt.InstallIn
import dagger.hilt.android.qualifiers.ApplicationContext
import dagger.hilt.components.SingletonComponent
import kotlinx.coroutines.runBlocking
import javax.inject.Singleton

/**
 * Dependency injection module for Machine Learning components
 */
@Module
@InstallIn(SingletonComponent::class)
object MLModule {
    
    /**
     * Provide SentencePiece tokenizer as singleton
     */
    @Provides
    @Singleton
    fun provideTokenizer(
        @ApplicationContext context: Context
    ): SentencePieceTokenizer {
        val tokenizer = SentencePieceTokenizer(context)
        
        // Initialize tokenizer synchronously during DI
        runBlocking {
            tokenizer.initialize().getOrThrow()
        }
        
        return tokenizer
    }
    
    /**
     * Provide EmbeddingGemma model as singleton
     * Initialized eagerly during app startup
     */
    @Provides
    @Singleton
    fun provideEmbeddingModel(
        @ApplicationContext context: Context,
        tokenizer: SentencePieceTokenizer
    ): EmbeddingGemmaModel {
        val model = EmbeddingGemmaModel(context, tokenizer)
        
        // Initialize model synchronously during DI
        runBlocking {
            model.initialize().getOrThrow()
        }
        
        return model
    }
    
    /**
     * Provide PDF reader
     */
    @Provides
    @Singleton
    fun providePDFReader(): PDFReader {
        return PDFReader()
    }
    
    /**
     * Provide document chunker with optimal settings for RAG
     */
    @Provides
    @Singleton
    fun provideDocumentChunker(): DocumentChunker {
        return DocumentChunker(
            minChunkSize = 100,
            maxChunkSize = 600,
            overlapTokens = 100
        )
    }
}
