package com.example.filemapper.di

import android.content.Context
import androidx.room.Room
import com.example.filemapper.data.local.AppDatabase
import com.example.filemapper.data.local.dao.FileNodeDao
import dagger.Module
import dagger.Provides
import dagger.hilt.InstallIn
import dagger.hilt.android.qualifiers.ApplicationContext
import dagger.hilt.components.SingletonComponent
import javax.inject.Singleton

/**
 * Hilt module providing database-related dependencies.
 */
@Module
@InstallIn(SingletonComponent::class)
object DatabaseModule {
    
    /**
     * Provides the Room Database instance as a Singleton.
     */
    @Provides
    @Singleton
    fun provideAppDatabase(
        @ApplicationContext context: Context
    ): AppDatabase {
        return Room.databaseBuilder(
            context,
            AppDatabase::class.java,
            AppDatabase.DATABASE_NAME
        ).build()
    }
    
    /**
     * Provides the FileNodeDao instance.
     */
    @Provides
    @Singleton
    fun provideFileNodeDao(database: AppDatabase): FileNodeDao {
        return database.fileNodeDao()
    }
}
