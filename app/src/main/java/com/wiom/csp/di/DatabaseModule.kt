package com.wiom.csp.di

import android.content.Context
import androidx.room.Room
import com.wiom.csp.data.db.AppDatabase
import com.wiom.csp.data.db.ActionQueueDao
import com.wiom.csp.data.db.CacheMetaDao
import com.wiom.csp.data.db.NotificationDao
import com.wiom.csp.data.db.SchemaDao
import com.wiom.csp.data.db.SingleCacheDao
import com.wiom.csp.data.db.SupportCaseDao
import com.wiom.csp.data.db.TaskDao
import com.wiom.csp.data.db.TechnicianDao
import dagger.Module
import dagger.Provides
import dagger.hilt.InstallIn
import dagger.hilt.android.qualifiers.ApplicationContext
import dagger.hilt.components.SingletonComponent
import javax.inject.Singleton

@Module
@InstallIn(SingletonComponent::class)
object DatabaseModule {

    @Provides
    @Singleton
    fun provideDatabase(@ApplicationContext ctx: Context): AppDatabase =
        Room.databaseBuilder(ctx, AppDatabase::class.java, "wiom_csp.db")
            .fallbackToDestructiveMigration()
            .build()

    @Provides
    fun provideSchemaDao(db: AppDatabase): SchemaDao = db.schemaDao()

    @Provides
    fun provideTaskDao(db: AppDatabase): TaskDao = db.taskDao()

    @Provides
    fun provideSingleCacheDao(db: AppDatabase): SingleCacheDao = db.singleCacheDao()

    @Provides
    fun provideTechnicianDao(db: AppDatabase): TechnicianDao = db.technicianDao()

    @Provides
    fun provideSupportCaseDao(db: AppDatabase): SupportCaseDao = db.supportCaseDao()

    @Provides
    fun provideNotificationDao(db: AppDatabase): NotificationDao = db.notificationDao()

    @Provides
    fun provideActionQueueDao(db: AppDatabase): ActionQueueDao = db.actionQueueDao()

    @Provides
    fun provideCacheMetaDao(db: AppDatabase): CacheMetaDao = db.cacheMetaDao()
}
