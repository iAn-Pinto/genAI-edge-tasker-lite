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

@Module
@InstallIn(SingletonComponent::class)
abstract class RepositoryModule {
    
    @Binds
    abstract fun bindTaskRepository(
        taskRepositoryImpl: TaskRepositoryImpl
    ): TaskRepository
    
    @Binds
    abstract fun bindDocumentRepository(
        documentRepositoryImpl: DocumentRepositoryImpl
    ): DocumentRepository

    @Binds
    abstract fun bindPrivacyAuditor(
        privacyAuditorImpl: PrivacyAuditorImpl
    ): PrivacyAuditor
}