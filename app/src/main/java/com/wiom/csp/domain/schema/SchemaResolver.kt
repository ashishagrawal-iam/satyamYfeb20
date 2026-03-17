package com.wiom.csp.domain.schema

import com.wiom.csp.domain.model.*

/**
 * SchemaResolver: the heart of the state-driven renderer.
 * All display decisions go through here — the app NEVER hardcodes
 * what a state looks like or what actions are available.
 *
 * Render = schema[task_type][current_state] → label, color, actions
 */
class SchemaResolver(private var schema: AppSchema) {

    fun updateSchema(newSchema: AppSchema) { schema = newSchema }
    fun getSchema(): AppSchema = schema

    // ── Task rendering ─────────────────────────────────────────────
    fun resolveTaskType(taskType: String): TaskTypeSchema? =
        schema.taskTypes[taskType]

    fun resolveTaskState(taskType: String, state: String): TaskStateSchema? =
        schema.taskTypes[taskType]?.states?.get(state)

    fun resolveActions(taskType: String, state: String): List<ActionSchema> =
        resolveTaskState(taskType, state)?.actions ?: emptyList()

    fun resolveTaskLabel(taskType: String, state: String, hindi: Boolean): String {
        val stateSchema = resolveTaskState(taskType, state)
        return if (hindi) stateSchema?.labelHi ?: stateSchema?.label ?: state
        else stateSchema?.label ?: state
    }

    fun resolveTaskTypeLabel(taskType: String, hindi: Boolean): String {
        val typeSchema = resolveTaskType(taskType)
        return if (hindi) typeSchema?.labelHi ?: typeSchema?.label ?: taskType
        else typeSchema?.label ?: taskType
    }

    fun resolveTaskColor(taskType: String): String =
        resolveTaskType(taskType)?.dotColor ?: "#888888"

    fun resolveStateColor(taskType: String, state: String): String =
        resolveTaskState(taskType, state)?.color ?: "#888888"

    fun isTerminalState(taskType: String, state: String): Boolean =
        resolveTaskState(taskType, state)?.isTerminal ?: false

    fun resolveTimerField(taskType: String, state: String): String? =
        resolveTaskState(taskType, state)?.timerField

    fun resolveReasonLabel(taskType: String, state: String, task: TaskData, hindi: Boolean): String {
        // Priority: escalation flag > state reason label > state label
        val highestFlag = task.escalationFlags
            .mapNotNull { schema.escalationFlags[it] }
            .minByOrNull { it.severity }
        if (highestFlag != null) return if (hindi) highestFlag.labelHi else highestFlag.label

        val stateSchema = resolveTaskState(taskType, state)
        val reason = if (hindi) stateSchema?.reasonLabelHi ?: stateSchema?.reasonLabel
                     else stateSchema?.reasonLabel
        if (!reason.isNullOrBlank()) return reason

        return resolveTaskLabel(taskType, state, hindi)
    }

    // ── Urgency ────────────────────────────────────────────────────
    fun resolveUrgencyColor(task: TaskData, bucketIndex: Int): String {
        if (task.isOverdue()) return "#FF3B30"
        if (task.priority == "HIGH") return "#FF3B30"
        if (bucketIndex <= 1) return "#FF3B30"
        if (bucketIndex <= 3) return "#FF8000"
        return "#34C759"
    }

    // ── Escalation flags ───────────────────────────────────────────
    fun resolveEscalationFlag(flagId: String): EscalationFlagSchema? =
        schema.escalationFlags[flagId]

    // ── Delegation ─────────────────────────────────────────────────
    fun resolveDelegationLabel(state: String, hindi: Boolean): String {
        val ds = schema.delegationStates[state]
        return if (hindi) ds?.labelHi ?: ds?.label ?: state else ds?.label ?: state
    }

    // ── Assurance chips ────────────────────────────────────────────
    fun resolveAssuranceChips(): List<ChipSchema> = schema.assuranceChips

    fun resolveChipLabel(chipId: String, hindi: Boolean): String {
        val chip = schema.assuranceChips.find { it.id == chipId }
        return if (hindi) chip?.labelHi ?: chip?.label ?: chipId else chip?.label ?: chipId
    }

    // ── Wallet ─────────────────────────────────────────────────────
    fun resolveWalletLineType(type: String): WalletLineTypeSchema? =
        schema.walletLineTypes[type]

    fun resolveWalletCategory(type: String): String =
        schema.walletLineTypes[type]?.category ?: "transactions"

    fun resolveWalletLabel(type: String, hindi: Boolean): String {
        val lt = schema.walletLineTypes[type]
        return if (hindi) lt?.labelHi ?: lt?.label ?: type else lt?.label ?: type
    }

    // ── SLA ────────────────────────────────────────────────────────
    fun resolveSlaDomainsSchema(): List<SlaDomainSchema> = schema.slaDomains

    fun resolveSlaMetricSchema(domainId: String, metricId: String): SlaMetricSchema? =
        schema.slaDomains.find { it.id == domainId }?.metrics?.find { it.id == metricId }

    // ── Exposure ───────────────────────────────────────────────────
    fun resolveExposureState(state: String): ExposureStateSchema? =
        schema.exposureStates[state]

    // ── Support ────────────────────────────────────────────────────
    fun resolveSupportStatus(status: String): StatusDisplaySchema? =
        schema.supportStatuses[status]

    // ── Notifications ──────────────────────────────────────────────
    fun resolveNotificationType(type: String): NotificationTypeSchema? =
        schema.notificationTypes[type]

    // ── Decline reasons, quick notes, help reasons ─────────────────
    fun declineReasons(): List<LabeledItem> = schema.declineReasons
    fun quickNotes(): List<LabeledItem> = schema.quickNotes
    fun helpReasons(): List<LabeledItem> = schema.helpReasons
    fun technicianBands(): List<LabeledItem> = schema.technicianBands

    // ── Onboarding ─────────────────────────────────────────────────
    fun onboardingFields(): List<FormFieldSchema> = schema.onboardingFields
}
