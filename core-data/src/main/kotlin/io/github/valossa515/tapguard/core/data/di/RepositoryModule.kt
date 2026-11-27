package io.github.valossa515.tapguard.core.data.di

import dagger.Binds
import dagger.Module
import dagger.hilt.InstallIn
import dagger.hilt.components.SingletonComponent
import io.github.valossa515.tapguard.core.data.repository.RoomScanRepository
import io.github.valossa515.tapguard.core.data.repository.RoomTrustedAppRepository
import io.github.valossa515.tapguard.core.domain.risk.TrustedAppRepository
import io.github.valossa515.tapguard.core.domain.scan.ScanRepository

/** Liga as portas do `core-domain` às implementações sobre o Room. */
@Module
@InstallIn(SingletonComponent::class)
abstract class RepositoryModule {
    @Binds
    abstract fun bindScanRepository(impl: RoomScanRepository): ScanRepository

    @Binds
    abstract fun bindTrustedAppRepository(impl: RoomTrustedAppRepository): TrustedAppRepository
}
