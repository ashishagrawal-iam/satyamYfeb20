package com.wiom.csp.data.db.entity

import androidx.room.Entity
import androidx.room.PrimaryKey

@Entity(tableName = "schema_cache")
data class SchemaEntity(
    @PrimaryKey val key: String = "app_schema",
    val json: String,
    val version: String,
    val updatedAt: Long = System.currentTimeMillis()
)

@Entity(tableName = "tasks")
data class TaskEntity(
    @PrimaryKey val taskId: String,
    val json: String,
    val taskType: String,
    val currentState: String,
    val priority: String,
    val bucketIndex: Int = 99,
    val updatedAt: Long = System.currentTimeMillis()
)

@Entity(tableName = "assurance_cache")
data class AssuranceEntity(
    @PrimaryKey val key: String = "assurance",
    val json: String,
    val updatedAt: Long = System.currentTimeMillis()
)

@Entity(tableName = "wallet_cache")
data class WalletEntity(
    @PrimaryKey val key: String = "wallet",
    val json: String,
    val updatedAt: Long = System.currentTimeMillis()
)

@Entity(tableName = "sla_cache")
data class SlaEntity(
    @PrimaryKey val key: String = "sla",
    val json: String,
    val updatedAt: Long = System.currentTimeMillis()
)

@Entity(tableName = "technicians")
data class TechnicianEntity(
    @PrimaryKey val id: String,
    val json: String,
    val updatedAt: Long = System.currentTimeMillis()
)

@Entity(tableName = "support_cases")
data class SupportCaseEntity(
    @PrimaryKey val id: String,
    val json: String,
    val updatedAt: Long = System.currentTimeMillis()
)

@Entity(tableName = "deposit_cache")
data class DepositEntity(
    @PrimaryKey val key: String = "deposit",
    val json: String,
    val updatedAt: Long = System.currentTimeMillis()
)

@Entity(tableName = "notifications")
data class NotificationEntity(
    @PrimaryKey val id: String,
    val json: String,
    val dismissed: Boolean = false,
    val timestamp: Long
)

@Entity(tableName = "action_queue")
data class ActionQueueEntity(
    @PrimaryKey val id: String,
    val endpoint: String,
    val payload: String,
    val createdAt: Long = System.currentTimeMillis(),
    val retryCount: Int = 0
)

@Entity(tableName = "cache_meta")
data class CacheMetaEntity(
    @PrimaryKey val key: String,
    val value: String,
    val updatedAt: Long = System.currentTimeMillis()
)
