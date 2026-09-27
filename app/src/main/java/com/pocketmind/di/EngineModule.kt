package com.pocketmind.di

import android.content.Context
import com.pocketmind.engine.LlamaCppEngine
import com.pocketmind.engine.ModelDownloader
import com.pocketmind.engine.SLMEngine
import dagger.Module
import dagger.Provides
import dagger.hilt.InstallIn
import dagger.hilt.android.qualifiers.ApplicationContext
import dagger.hilt.components.SingletonComponent
import javax.inject.Singleton

@Module
@InstallIn(SingletonComponent::class)
object EngineModule {

    @Provides
    @Singleton
    fun provideSLMEngine(): SLMEngine {
        return LlamaCppEngine()
    }

    @Provides
    @Singleton
    fun provideModelDownloader(@ApplicationContext context: Context): ModelDownloader {
        return ModelDownloader(context)
    }
}
