package com.example.privytaskai.di

import com.example.privytaskai.data.repository.TaskRepositoryImpl
import com.example.privytaskai.data.repository.DocumentRepositoryImpl
import com.example.privytaskai.domain.repository.TaskRepository
import com.example.privytaskai.domain.repository.DocumentRepository
import com.example.privytaskai.privacy.PrivacyAuditor
import com.example.privytaskai.privacy.PrivacyAuditorImpl
import dagger.Binds
import dagger.Module
import dagger.hilt.InstallIn
import dagger.hilt.components.SingletonComponent
import javax.inject.Singleton

/**
 * Module providing repository implementations
 */
@Module
@InstallIn(SingletonComponent::class)
@Suppress("unused") // Hilt module used via annotation processing
abstract class RepositoryModule {
    
    @Binds
    @Singleton
    @Suppress("unused") // Hilt binding used via annotation processing
    abstract fun bindTaskRepository(
        impl: TaskRepositoryImpl
    ): TaskRepository
    
    @Binds
    @Singleton
    @Suppress("unused") // Hilt binding used via annotation processing
    abstract fun bindDocumentRepository(
        impl: DocumentRepositoryImpl
    ): DocumentRepository

    @Binds
    @Singleton
    @Suppress("unused") // Hilt binding used via annotation processing
    abstract fun bindPrivacyAuditor(
        impl: PrivacyAuditorImpl
    ): PrivacyAuditor
}