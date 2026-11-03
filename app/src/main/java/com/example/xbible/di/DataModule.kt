package com.example.xbible.di

import android.app.Application
import com.example.xbible.data.BibleRepository
import com.example.xbible.data.AudioBibleRepository
import dagger.Module
import dagger.Provides
import dagger.hilt.InstallIn
import dagger.hilt.components.SingletonComponent
import javax.inject.Singleton

@Module
@InstallIn(SingletonComponent::class)
object DataModule {

    @Provides
    @Singleton
    fun provideBibleRepository(application: Application): BibleRepository {
        return BibleRepository(application)
    }

    @Provides
    @Singleton
    fun provideAudioBibleRepository(): AudioBibleRepository {
        return AudioBibleRepository()
    }
}