package com.wiom.csp.domain.model

import kotlinx.serialization.Serializable

/** Complete schema contract — defines all display rules. App never hardcodes these. */
@Serializable
data class AppSchema(
    val version: String,
    val taskTypes: Map<String, TaskTypeSchema>,
    val assuranceChips: List<ChipSchema>,
    val walletLineTypes: Map<String, WalletLineTypeSchema>,
    val declineReasons: List<LabeledItem>,
    val quickNotes: List<LabeledItem>,
    val helpReasons: List<LabeledItem>,
    val slaDomains: List<SlaDomainSchema>,
    val exposureStates: Map<String, ExposureStateSchema>,
    val delegationStates: Map<String, DelegationStateSchema>,
    val escalationFlags: Map<String, EscalationFlagSchema>,
    val technicianBands: List<LabeledItem>,
    val supportStatuses: Map<String, StatusDisplaySchema>,
    val notificationTypes: Map<String, NotificationTypeSchema>,
    val onboardingFields: List<FormFieldSchema>
)

@Serializable
data class TaskTypeSchema(
    val label: String,
    val labelHi: String,
    val color: String,
    val dotColor: String,
    val states: Map<String, TaskStateSchema>
)

@Serializable
data class TaskStateSchema(
    val label: String,
    val labelHi: String,
    val actions: List<ActionSchema>,
    val color: String,
    val timerField: String? = null,
    val isTerminal: Boolean = false,
    val showCustomerDetails: Boolean = false,
    val showProofCapture: Boolean = false,
    val showDeclineReasons: Boolean = false,
    val showSlotPicker: Boolean = false,
    val reasonLabel: String? = null,
    val reasonLabelHi: String? = null
)

@Serializable
data class ActionSchema(
    val id: String,
    val label: String,
    val labelHi: String,
    val style: String = "primary", // primary, secondary, destructive
    val requiresProof: Boolean = false,
    val requiresReason: Boolean = false,
    val requiresSlot: Boolean = false,
    val requiresTechnician: Boolean = false,
    val confirmMessage: String? = null,
    val confirmMessageHi: String? = null
)

@Serializable
data class ChipSchema(
    val id: String,
    val label: String,
    val labelHi: String,
    val valueField: String,
    val stateColorField: String? = null,
    val trendField: String? = null,
    val drillDownType: String? = null,
    val format: String = "number" // number, currency, percentage
)

@Serializable
data class WalletLineTypeSchema(
    val label: String,
    val labelHi: String,
    val category: String, // earnings, deposits_fees, transactions
    val isCredit: Boolean,
    val color: String
)

@Serializable
data class SlaDomainSchema(
    val id: String,
    val label: String,
    val labelHi: String,
    val metrics: List<SlaMetricSchema>
)

@Serializable
data class SlaMetricSchema(
    val id: String,
    val label: String,
    val labelHi: String,
    val direction: String, // above, below
    val unit: String,
    val format: String = "percentage"
)

@Serializable
data class ExposureStateSchema(
    val label: String,
    val labelHi: String,
    val color: String,
    val description: String,
    val descriptionHi: String
)

@Serializable
data class DelegationStateSchema(
    val label: String,
    val labelHi: String,
    val isTerminal: Boolean = false
)

@Serializable
data class EscalationFlagSchema(
    val label: String,
    val labelHi: String,
    val severity: Int, // 1=critical, 2=high, 3=medium
    val color: String
)

@Serializable
data class StatusDisplaySchema(
    val label: String,
    val labelHi: String,
    val color: String,
    val bgColor: String
)

@Serializable
data class NotificationTypeSchema(
    val label: String,
    val labelHi: String,
    val isUrgent: Boolean = false,
    val sound: String = "standard" // standard, urgent, silent
)

@Serializable
data class FormFieldSchema(
    val id: String,
    val label: String,
    val labelHi: String,
    val type: String, // text, phone, number, select, checkbox
    val section: String,
    val required: Boolean = true,
    val validation: String? = null, // regex pattern
    val minLength: Int? = null,
    val maxLength: Int? = null,
    val options: List<LabeledItem>? = null
)

@Serializable
data class LabeledItem(
    val id: String,
    val label: String,
    val labelHi: String
)
