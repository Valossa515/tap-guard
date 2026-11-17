package io.github.valossa515.tapguard.core.data.di

import android.content.Context
import androidx.datastore.core.DataStore
import androidx.datastore.preferences.core.PreferenceDataStoreFactory
import androidx.datastore.preferences.core.Preferences
import androidx.datastore.preferences.preferencesDataStoreFile
import androidx.room.Room
import dagger.Module
import dagger.Provides
import dagger.hilt.InstallIn
import dagger.hilt.android.qualifiers.ApplicationContext
import dagger.hilt.components.SingletonComponent
import io.github.valossa515.tapguard.core.data.database.TapGuardDatabase
import io.github.valossa515.tapguard.core.data.database.dao.AlertEventDao
import io.github.valossa515.tapguard.core.data.database.dao.AppSnapshotDao
import io.github.valossa515.tapguard.core.data.database.dao.ScanRunDao
import io.github.valossa515.tapguard.core.data.database.dao.UserDecisionDao
import io.github.valossa515.tapguard.core.data.settings.SettingsDataSource
import javax.inject.Singleton

/** Fornece o banco, os DAOs e o DataStore. Banco e DataStore são únicos no app. */
@Module
@InstallIn(SingletonComponent::class)
object DataModule {
    @Provides
    @Singleton
    fun provideDatabase(
        @ApplicationContext context: Context,
    ): TapGuardDatabase =
        Room
            .databaseBuilder(context, TapGuardDatabase::class.java, TapGuardDatabase.DATABASE_NAME)
            .build()

    @Provides
    fun provideScanRunDao(database: TapGuardDatabase): ScanRunDao = database.scanRunDao()

    @Provides
    fun provideAppSnapshotDao(database: TapGuardDatabase): AppSnapshotDao = database.appSnapshotDao()

    @Provides
    fun provideUserDecisionDao(database: TapGuardDatabase): UserDecisionDao = database.userDecisionDao()

    @Provides
    fun provideAlertEventDao(database: TapGuardDatabase): AlertEventDao = database.alertEventDao()

    // Um único DataStore por arquivo: dois no mesmo arquivo derrubam o app.
    @Provides
    @Singleton
    fun provideSettingsDataStore(
        @ApplicationContext context: Context,
    ): DataStore<Preferences> =
        PreferenceDataStoreFactory.create {
            context.preferencesDataStoreFile(SettingsDataSource.DATASTORE_NAME)
        }
}
