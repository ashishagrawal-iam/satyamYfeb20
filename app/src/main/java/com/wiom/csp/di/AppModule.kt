package com.wiom.csp.di

import com.wiom.csp.data.repository.SchemaRepository
import com.wiom.csp.domain.schema.SchemaResolver
import com.wiom.csp.mock.SeedDataProvider
import dagger.Module
import dagger.Provides
import dagger.hilt.InstallIn
import dagger.hilt.components.SingletonComponent
import kotlinx.serialization.json.Json
import javax.inject.Singleton

@Module
@InstallIn(SingletonComponent::class)
object AppModule {

    @Provides
    @Singleton
    fun provideSchemaResolver(schemaRepository: SchemaRepository): SchemaResolver {
        // Use seed data as initial schema; in production, load from cache via schemaRepository
        val initialSchema = SeedDataProvider.buildSchema()
        return SchemaResolver(initialSchema)
    }

    @Provides
    @Singleton
    fun provideJson(): Json = Json {
        ignoreUnknownKeys = true
        isLenient = true
    }
}
