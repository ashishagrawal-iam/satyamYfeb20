package com.wiom.csp.data.db

import androidx.room.*
import com.wiom.csp.data.db.entity.*

@Dao
interface SchemaDao {
    @Query("SELECT * FROM schema_cache WHERE `key` = 'app_schema'")
    suspend fun getSchema(): SchemaEntity?

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertSchema(schema: SchemaEntity)
}

@Dao
interface TaskDao {
    @Query("SELECT * FROM tasks ORDER BY bucketIndex ASC, updatedAt ASC")
    suspend fun getAllTasks(): List<TaskEntity>

    @Query("SELECT * FROM tasks WHERE taskId = :id")
    suspend fun getTask(id: String): TaskEntity?

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertTask(task: TaskEntity)

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertTasks(tasks: List<TaskEntity>)

    @Query("DELETE FROM tasks WHERE taskId = :id")
    suspend fun deleteTask(id: String)

    @Query("DELETE FROM tasks")
    suspend fun deleteAll()
}

@Dao
interface SingleCacheDao {
    @Query("SELECT * FROM assurance_cache WHERE `key` = 'assurance'")
    suspend fun getAssurance(): AssuranceEntity?
    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertAssurance(entity: AssuranceEntity)

    @Query("SELECT * FROM wallet_cache WHERE `key` = 'wallet'")
    suspend fun getWallet(): WalletEntity?
    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertWallet(entity: WalletEntity)

    @Query("SELECT * FROM sla_cache WHERE `key` = 'sla'")
    suspend fun getSla(): SlaEntity?
    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertSla(entity: SlaEntity)

    @Query("SELECT * FROM deposit_cache WHERE `key` = 'deposit'")
    suspend fun getDeposit(): DepositEntity?
    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertDeposit(entity: DepositEntity)
}

@Dao
interface TechnicianDao {
    @Query("SELECT * FROM technicians")
    suspend fun getAll(): List<TechnicianEntity>
    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insert(entity: TechnicianEntity)
    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertAll(entities: List<TechnicianEntity>)
    @Query("DELETE FROM technicians WHERE id = :id")
    suspend fun delete(id: String)
}

@Dao
interface SupportCaseDao {
    @Query("SELECT * FROM support_cases ORDER BY updatedAt DESC")
    suspend fun getAll(): List<SupportCaseEntity>
    @Query("SELECT * FROM support_cases WHERE id = :id")
    suspend fun getById(id: String): SupportCaseEntity?
    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insert(entity: SupportCaseEntity)
    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertAll(entities: List<SupportCaseEntity>)
}

@Dao
interface NotificationDao {
    @Query("SELECT * FROM notifications WHERE dismissed = 0 ORDER BY timestamp DESC")
    suspend fun getActive(): List<NotificationEntity>
    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insert(entity: NotificationEntity)
    @Query("UPDATE notifications SET dismissed = 1 WHERE id = :id")
    suspend fun dismiss(id: String)
    @Query("DELETE FROM notifications WHERE timestamp < :before")
    suspend fun pruneOld(before: Long)
}

@Dao
interface ActionQueueDao {
    @Query("SELECT * FROM action_queue ORDER BY createdAt ASC")
    suspend fun getPending(): List<ActionQueueEntity>
    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insert(entity: ActionQueueEntity)
    @Query("DELETE FROM action_queue WHERE id = :id")
    suspend fun remove(id: String)
    @Query("UPDATE action_queue SET retryCount = retryCount + 1 WHERE id = :id")
    suspend fun incrementRetry(id: String)
}

@Dao
interface CacheMetaDao {
    @Query("SELECT value FROM cache_meta WHERE `key` = :key")
    suspend fun get(key: String): String?
    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun set(entity: CacheMetaEntity)
}

@Database(
    entities = [
        SchemaEntity::class, TaskEntity::class, AssuranceEntity::class,
        WalletEntity::class, SlaEntity::class, TechnicianEntity::class,
        SupportCaseEntity::class, DepositEntity::class, NotificationEntity::class,
        ActionQueueEntity::class, CacheMetaEntity::class
    ],
    version = 1,
    exportSchema = false
)
abstract class AppDatabase : RoomDatabase() {
    abstract fun schemaDao(): SchemaDao
    abstract fun taskDao(): TaskDao
    abstract fun singleCacheDao(): SingleCacheDao
    abstract fun technicianDao(): TechnicianDao
    abstract fun supportCaseDao(): SupportCaseDao
    abstract fun notificationDao(): NotificationDao
    abstract fun actionQueueDao(): ActionQueueDao
    abstract fun cacheMetaDao(): CacheMetaDao
}
