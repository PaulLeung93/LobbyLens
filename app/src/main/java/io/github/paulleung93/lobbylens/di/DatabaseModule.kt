package io.github.paulleung93.lobbylens.di

import android.content.Context
import androidx.room.Room
import dagger.Module
import dagger.Provides
import dagger.hilt.InstallIn
import dagger.hilt.android.qualifiers.ApplicationContext
import dagger.hilt.components.SingletonComponent
import io.github.paulleung93.lobbylens.data.local.CandidateCacheDao
import io.github.paulleung93.lobbylens.data.local.ContributionCacheDao
import io.github.paulleung93.lobbylens.data.local.LobbyLensDatabase
import io.github.paulleung93.lobbylens.data.local.SenateCacheDao
import javax.inject.Singleton

/**
 * Hilt module that provides database-related dependencies.
 */
@Module
@InstallIn(SingletonComponent::class)
object DatabaseModule {

    @Provides
    @Singleton
    fun provideDatabase(@ApplicationContext context: Context): LobbyLensDatabase {
        return Room.databaseBuilder(
            context,
            LobbyLensDatabase::class.java,
            LobbyLensDatabase.DATABASE_NAME
        )
        .fallbackToDestructiveMigration()
        .build()
    }

    @Provides
    fun provideCandidateCacheDao(database: LobbyLensDatabase): CandidateCacheDao {
        return database.candidateCacheDao()
    }

    @Provides
    fun provideContributionCacheDao(database: LobbyLensDatabase): ContributionCacheDao {
        return database.contributionCacheDao()
    }

    @Provides
    fun provideSenateCacheDao(database: LobbyLensDatabase): SenateCacheDao {
        return database.senateCacheDao()
    }
}
