package com.wiom.csp.domain.model

import kotlinx.serialization.Serializable
import kotlinx.serialization.json.JsonObject

/** All data models — these change frequently, synced via push/pull. */

@Serializable
data class TaskData(
    val taskId: String,
    val taskType: String,
    val currentState: String,
    val priority: String,
    val connectionId: String? = null,
    val netboxId: String? = null,
    val customerArea: String? = null,
    val customerName: String? = null,
    val customerPhone: String? = null,
    val assignedTo: String? = null,
    val assignedToName: String? = null,
    val delegationState: String = "UNASSIGNED",
    val escalationFlags: List<String> = emptyList(),
    val slaDeadlineAt: Long? = null,
    val offerExpiresAt: Long? = null,
    val acceptExpiresAt: Long? = null,
    val returnDueAt: Long? = null,
    val pickupDueAt: Long? = null,
    val dueAt: Long? = null,
    val blockedDueAt: Long? = null,
    val retryCount: Int = 0,
    val chainId: String? = null,
    val blockedReason: String? = null,
    val proofBundle: String? = null,
    val notes: List<NoteData> = emptyList(),
    val timeline: List<TimelineEntry> = emptyList(),
    val createdAt: Long = System.currentTimeMillis(),
    val updatedAt: Long = System.currentTimeMillis()
) {
    fun getTimerDeadline(timerField: String?): Long? = when (timerField) {
        "sla_deadline_at" -> slaDeadlineAt
        "offer_expires_at" -> offerExpiresAt
        "accept_expires_at" -> acceptExpiresAt
        "return_due_at" -> returnDueAt
        "pickup_due_at" -> pickupDueAt
        "due_at" -> dueAt
        "blocked_due_at" -> blockedDueAt
        else -> null
    }

    fun nearestDeadline(): Long? = listOfNotNull(
        slaDeadlineAt, offerExpiresAt, acceptExpiresAt,
        returnDueAt, pickupDueAt, dueAt
    ).minOrNull()

    fun isOverdue(): Boolean = listOfNotNull(
        slaDeadlineAt, offerExpiresAt, acceptExpiresAt,
        returnDueAt, pickupDueAt, dueAt
    ).any { it < System.currentTimeMillis() }
}

@Serializable
data class NoteData(
    val id: String,
    val text: String,
    val author: String,
    val authorType: String,
    val createdAt: Long
)

@Serializable
data class TimelineEntry(
    val timestamp: Long,
    val eventType: String,
    val actor: String,
    val actorType: String,
    val detail: String,
    val proof: String? = null
)

@Serializable
data class AssuranceData(
    val activeBase: Int = 0,
    val cycleEarned: Double = 0.0,
    val nextSettlementAt: Long? = null,
    val lifetimeEarnings: Double = 0.0,
    val slaStanding: String = "COMPLIANT",
    val exposureState: String = "ELIGIBLE",
    val exposureReason: String? = null,
    val exposureSince: Long? = null,
    val exposureTrend: String = "STABLE",
    val capabilityResetActive: Boolean = false,
    val activeRestores: Int = 0,
    val unresolvedCount: Int = 0,
    val activeBaseEvents: List<BaseEvent> = emptyList(),
    val chipValues: Map<String, ChipValue> = emptyMap()
)

@Serializable
data class BaseEvent(
    val id: String,
    val connectionId: String,
    val eventType: String,
    val timestamp: Long
)

@Serializable
data class ChipValue(
    val value: String,
    val stateColor: String? = null,
    val trend: String? = null
)

@Serializable
data class WalletData(
    val balance: Double = 0.0,
    val pendingSettlement: Double = 0.0,
    val frozen: Boolean = false,
    val frozenReason: String? = null,
    val lastWithdrawalAt: Long? = null,
    val transactions: List<WalletTransaction> = emptyList()
)

@Serializable
data class WalletTransaction(
    val id: String,
    val date: Long,
    val type: String,
    val amount: Double,
    val description: String,
    val status: String = "COMPLETED"
)

@Serializable
data class SlaData(
    val overallStanding: String = "COMPLIANT",
    val nextEvalDays: Int = 7,
    val routing: String = "FULL",
    val bonusStatus: String = "ACTIVE",
    val domains: List<SlaDomainData> = emptyList()
)

@Serializable
data class SlaDomainData(
    val id: String,
    val standing: String,
    val metrics: List<SlaMetricData> = emptyList(),
    val consequences: SlaConsequences? = null,
    val hysteresis: SlaHysteresis? = null
)

@Serializable
data class SlaMetricData(
    val id: String,
    val value: Double,
    val threshold: Double,
    val severeThreshold: Double? = null,
    val sampleCount: Int = 10,
    val minSample: Int = 5,
    val trend: String = "STABLE"
)

@Serializable
data class SlaConsequences(
    val routing: String = "FULL",
    val bonus: String = "ACTIVE"
)

@Serializable
data class SlaHysteresis(
    val requiredCleanWindows: Int = 3,
    val currentCleanWindows: Int = 0
)

@Serializable
data class TechnicianData(
    val id: String,
    val name: String,
    val phone: String,
    val band: String = "B",
    val available: Boolean = true,
    val joinDate: Long = System.currentTimeMillis()
)

@Serializable
data class SupportCaseData(
    val id: String,
    val subject: String,
    val status: String,
    val linkedTaskId: String? = null,
    val messages: List<CaseMessage> = emptyList(),
    val createdAt: Long = System.currentTimeMillis()
)

@Serializable
data class CaseMessage(
    val id: String,
    val text: String,
    val sender: String,
    val senderType: String,
    val timestamp: Long
)

@Serializable
data class DepositData(
    val balance: Double = 0.0,
    val ratePerUnit: Double = 1500.0,
    val carryFeePerDay: Double = 2.0,
    val gracePeriodDays: Int = 15,
    val units: List<NetBoxUnit> = emptyList(),
    val transactions: List<DepositTransaction> = emptyList()
)

@Serializable
data class NetBoxUnit(
    val id: String,
    val status: String,
    val assignedAt: Long,
    val expiresAt: Long? = null,
    val carryFeeAccrued: Double = 0.0
)

@Serializable
data class DepositTransaction(
    val id: String,
    val type: String,
    val amount: Double,
    val netboxId: String? = null,
    val date: Long,
    val description: String
)

@Serializable
data class AppNotification(
    val id: String,
    val type: String,
    val title: String,
    val titleHi: String? = null,
    val body: String,
    val bodyHi: String? = null,
    val taskId: String? = null,
    val timestamp: Long,
    val dismissed: Boolean = false
)

@Serializable
data class QueuedAction(
    val id: String,
    val endpoint: String,
    val payload: String,
    val createdAt: Long = System.currentTimeMillis(),
    val retryCount: Int = 0
)
