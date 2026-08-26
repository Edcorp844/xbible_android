package com.example.xbible.di

import android.app.Application
import com.example.xbible.data.BibleRepository
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
}
