package com.example.privytaskai.data.database.migrations

import androidx.room.migration.Migration
import androidx.sqlite.db.SupportSQLiteDatabase

/**
 * Database migration from old 64-dim embeddings to new 768-dim EmbeddingGemma
 * 
 * Strategy: Add new columns with defaults, keep old data
 * Then regenerate all embeddings in background using WorkManager
 */
object Migrations {
    
    /**
     * Migration 1 -> 2: Add dimension, modelVersion, createdAt columns to embeddings
     * AND create new documents and document_chunks tables for RAG functionality
     */
    val MIGRATION_1_2 = object : Migration(1, 2) {
        override fun migrate(db: SupportSQLiteDatabase) {
            // 1. Add new columns to embeddings table with default values
            db.execSQL(
                """
                ALTER TABLE embeddings 
                ADD COLUMN dimension INTEGER NOT NULL DEFAULT 64
                """.trimIndent()
            )
            
            db.execSQL(
                """
                ALTER TABLE embeddings 
                ADD COLUMN modelVersion TEXT NOT NULL DEFAULT 'legacy'
                """.trimIndent()
            )
            
            db.execSQL(
                """
                ALTER TABLE embeddings 
                ADD COLUMN createdAt INTEGER NOT NULL DEFAULT 0
                """.trimIndent()
            )
            
            // Mark all existing embeddings as legacy
            db.execSQL(
                """
                UPDATE embeddings 
                SET modelVersion = 'legacy', 
                    dimension = 64,
                    createdAt = ${System.currentTimeMillis()}
                WHERE modelVersion = 'legacy'
                """.trimIndent()
            )

            // 2. Create documents table for RAG functionality
            db.execSQL(
                """
                CREATE TABLE IF NOT EXISTS documents (
                    id INTEGER PRIMARY KEY AUTOINCREMENT NOT NULL,
                    title TEXT NOT NULL,
                    content TEXT NOT NULL,
                    documentType TEXT NOT NULL,
                    author TEXT,
                    subject TEXT,
                    creationDate TEXT,
                    numberOfPages INTEGER NOT NULL,
                    fileSize INTEGER NOT NULL,
                    sourceUri TEXT,
                    createdAt INTEGER NOT NULL
                )
                """.trimIndent()
            )

            // 3. Create document_chunks table with embeddings
            db.execSQL(
                """
                CREATE TABLE IF NOT EXISTS document_chunks (
                    id INTEGER PRIMARY KEY AUTOINCREMENT NOT NULL,
                    documentId INTEGER NOT NULL,
                    chunkIndex INTEGER NOT NULL,
                    text TEXT NOT NULL,
                    startIndex INTEGER NOT NULL,
                    endIndex INTEGER NOT NULL,
                    embeddingCsv TEXT NOT NULL,
                    metadataJson TEXT NOT NULL,
                    FOREIGN KEY(documentId) REFERENCES documents(id) ON DELETE CASCADE
                )
                """.trimIndent()
            )

            // 4. Create index on documentId for faster chunk lookups
            db.execSQL(
                """
                CREATE INDEX IF NOT EXISTS index_document_chunks_documentId 
                ON document_chunks(documentId)
                """.trimIndent()
            )
        }
    }
    
    /**
     * Helper: Clear all embeddings (forces regeneration)
     * Use this if you want clean slate with new model
     */
    @Suppress("unused") // Utility function for maintenance
    fun clearAllEmbeddings(database: SupportSQLiteDatabase) {
        database.execSQL("DELETE FROM embeddings")
    }
}
