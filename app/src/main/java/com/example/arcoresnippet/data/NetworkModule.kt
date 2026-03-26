package com.example.arcoresnippet.data

import com.example.arcoresnippet.BuildConfig
import com.example.arcoresnippet.data.datasource.RoutesDataSource
import com.squareup.moshi.Moshi
import dagger.Module
import dagger.Provides
import dagger.hilt.InstallIn
import dagger.hilt.components.SingletonComponent
import retrofit2.Retrofit
import retrofit2.converter.moshi.MoshiConverterFactory
import javax.inject.Named
import javax.inject.Singleton

@Module
@InstallIn(SingletonComponent::class)
object NetworkModule {

    @Provides
    @Named("arcore_api_key")
    fun provideApiKey(): String = BuildConfig.ARCORE_API_KEY

    @Provides
    @Singleton
    fun provideMoshi(): Moshi = Moshi.Builder()
        .build()

    @Provides
    @Singleton
    fun provideRoutesDataSource(moshi: Moshi): RoutesDataSource {
        return Retrofit.Builder()
            .baseUrl("https://routes.googleapis.com/")
            .addConverterFactory(MoshiConverterFactory.create(moshi))
            .build()
            .create(RoutesDataSource::class.java)
    }
}