package com.example.arcoresnippet.data

import com.example.arcoresnippet.data.repository.DirectionsRepositoryImpl
import com.example.arcoresnippet.domain.repository.DirectionsRepository
import dagger.Binds
import dagger.Module
import dagger.hilt.InstallIn
import dagger.hilt.components.SingletonComponent
import javax.inject.Singleton

@Module
@InstallIn(SingletonComponent::class)
abstract class RepositoryModule {

    @Binds
    @Singleton
    abstract fun bindDirectionsRepository(
        directionsRepositoryImpl: DirectionsRepositoryImpl
    ): DirectionsRepository
}