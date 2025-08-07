package com.chromachaos.game.di

import android.content.Context
import com.chromachaos.game.data.local.GameDatabase
import com.chromachaos.game.data.repository.GameRepository
import com.chromachaos.game.data.repository.GameRepositoryImpl
import dagger.Module
import dagger.Provides
import dagger.hilt.InstallIn
import dagger.hilt.android.qualifiers.ApplicationContext
import dagger.hilt.components.SingletonComponent
import javax.inject.Singleton

@Module
@InstallIn(SingletonComponent::class)
object AppModule {
    
    @Provides
    @Singleton
    fun provideGameDatabase(@ApplicationContext context: Context): GameDatabase {
        return GameDatabase.getDatabase(context)
    }
    
    @Provides
    @Singleton
    fun provideGameDao(database: GameDatabase) = database.gameDao()
    
    @Provides
    @Singleton
    fun provideGameSettingsDao(database: GameDatabase) = database.settingsDao()
    
    @Provides
    @Singleton
    fun provideGameRepository(
        gameDao: com.chromachaos.game.data.local.GameDao,
        settingsDao: com.chromachaos.game.data.local.GameSettingsDao
    ): GameRepository {
        return GameRepositoryImpl(gameDao, settingsDao)
    }
} 