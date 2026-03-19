package com.wiom.csp.mock

import com.wiom.csp.domain.model.*

/**
 * Complete seed-data provider. The app is state-driven: every label, color,
 * action, and rule originates from the [AppSchema] returned here.
 * Nothing is hard-coded in UI or domain logic.
 */
object SeedDataProvider {

    // ---------------------------------------------------------------
    // Time helpers
    // ---------------------------------------------------------------
    private fun now() = System.currentTimeMillis()
    private fun minutesFromNow(m: Int) = now() + m * 60_000L
    private fun hoursFromNow(h: Int) = now() + h * 3_600_000L
    private fun daysFromNow(d: Int) = now() + d * 86_400_000L
    private fun daysAgo(d: Int) = now() - d * 86_400_000L
    private fun hoursAgo(h: Int) = now() - h * 3_600_000L
    private fun minutesAgo(m: Int) = now() - m * 60_000L

    // ---------------------------------------------------------------
    // Reusable ActionSchema builders
    // ---------------------------------------------------------------
    private val ACTION_CLAIM = ActionSchema(
        id = "CLAIM", label = "Claim", labelHi = "\u0938\u094D\u0935\u0940\u0915\u093E\u0930",
        style = "primary"
    )
    private val ACTION_CLAIM_AND_ASSIGN = ActionSchema(
        id = "CLAIM_AND_ASSIGN", label = "Claim & Assign",
        labelHi = "\u0938\u094D\u0935\u0940\u0915\u093E\u0930 \u0914\u0930 \u0938\u094C\u0902\u092A\u0947\u0902",
        style = "secondary", requiresTechnician = true
    )
    private val ACTION_DECLINE = ActionSchema(
        id = "DECLINE", label = "Decline", labelHi = "\u0905\u0938\u094D\u0935\u0940\u0915\u093E\u0930",
        style = "destructive", requiresReason = true
    )
    private val ACTION_SCHEDULE = ActionSchema(
        id = "SCHEDULE", label = "Schedule", labelHi = "\u0936\u0947\u0921\u094D\u092F\u0942\u0932",
        style = "primary", requiresSlot = true
    )
    private val ACTION_ASSIGN = ActionSchema(
        id = "ASSIGN", label = "Assign", labelHi = "\u0938\u094C\u0902\u092A\u0947\u0902",
        style = "secondary", requiresTechnician = true
    )
    private val ACTION_START_WORK = ActionSchema(
        id = "START_WORK", label = "Start Work", labelHi = "\u0915\u093E\u092E \u0936\u0941\u0930\u0942",
        style = "primary"
    )
    private val ACTION_START_INSTALLATION = ActionSchema(
        id = "START_INSTALLATION", label = "Start Installation",
        labelHi = "\u0907\u0902\u0938\u094D\u091F\u0949\u0932\u0947\u0936\u0928 \u0936\u0941\u0930\u0942",
        style = "primary",
        confirmMessage = "Installation flow started",
        confirmMessageHi = "\u0907\u0902\u0938\u094D\u091F\u0949\u0932\u0947\u0936\u0928 \u092B\u094D\u0932\u094B \u0936\u0941\u0930\u0942"
    )
    private val ACTION_INSTALL = ActionSchema(
        id = "INSTALL", label = "Complete Install",
        labelHi = "\u0907\u0902\u0938\u094D\u091F\u0949\u0932 \u092A\u0942\u0930\u093E",
        style = "primary", requiresProof = true
    )
    private val ACTION_VERIFY = ActionSchema(
        id = "VERIFY", label = "Verify Activation",
        labelHi = "\u0938\u0915\u094D\u0930\u093F\u092F\u0923 \u0938\u0924\u094D\u092F\u093E\u092A\u093F\u0924",
        style = "primary"
    )
    private val ACTION_REASSIGN = ActionSchema(
        id = "REASSIGN", label = "Reassign",
        labelHi = "\u092A\u0941\u0928\u0903 \u0938\u094C\u0902\u092A\u0947\u0902",
        style = "secondary", requiresTechnician = true
    )
    private val ACTION_RESOLVE = ActionSchema(
        id = "RESOLVE", label = "Resolve", labelHi = "\u0939\u0932",
        style = "primary"
    )
    private val ACTION_RESOLVE_BLOCKED = ActionSchema(
        id = "RESOLVE_BLOCKED", label = "Report Blocked",
        labelHi = "\u092C\u094D\u0932\u0949\u0915 \u0930\u093F\u092A\u094B\u0930\u094D\u091F",
        style = "destructive", requiresReason = true
    )
    private val ACTION_ESCALATE = ActionSchema(
        id = "ESCALATE", label = "Escalate",
        labelHi = "\u090F\u0938\u094D\u0915\u0932\u0947\u091F",
        style = "destructive"
    )
    private val ACTION_COLLECT = ActionSchema(
        id = "COLLECT", label = "Collect NetBox",
        labelHi = "\u0928\u0947\u091F\u092C\u0949\u0915\u094D\u0938 \u0907\u0915\u0920\u094D\u0920\u093E",
        style = "primary", requiresProof = true
    )
    private val ACTION_CONFIRM_RETURN = ActionSchema(
        id = "CONFIRM_RETURN", label = "Confirm Return",
        labelHi = "\u0935\u093E\u092A\u0938\u0940 \u0915\u0940 \u092A\u0941\u0937\u094D\u091F\u093F",
        style = "primary", requiresProof = true
    )
    private val ACTION_START_RECHARGE = ActionSchema(
        id = "START_RECHARGE", label = "Start Recharge",
        labelHi = "\u0930\u093F\u091A\u093E\u0930\u094D\u091C \u0936\u0941\u0930\u0942",
        style = "primary",
        confirmMessage = "ISP Recharge flow started",
        confirmMessageHi = "ISP \u0930\u093F\u091A\u093E\u0930\u094D\u091C \u092B\u094D\u0932\u094B \u0936\u0941\u0930\u0942"
    )
    private val ACTION_ACKNOWLEDGE_RECHARGE = ActionSchema(
        id = "ACKNOWLEDGE_RECHARGE", label = "Mark as Recharged",
        labelHi = "\u0930\u093F\u091A\u093E\u0930\u094D\u091C \u0915\u093F\u092F\u093E",
        style = "primary"
    )

    // ---------------------------------------------------------------
    // 1. buildSchema()
    // ---------------------------------------------------------------
    fun buildSchema(): AppSchema = AppSchema(
        version = "1.0.0",
        taskTypes = buildTaskTypes(),
        assuranceChips = buildAssuranceChips(),
        walletLineTypes = buildWalletLineTypes(),
        declineReasons = buildDeclineReasons(),
        quickNotes = buildQuickNotes(),
        helpReasons = buildHelpReasons(),
        slaDomains = buildSlaDomains(),
        exposureStates = buildExposureStates(),
        delegationStates = buildDelegationStates(),
        escalationFlags = buildEscalationFlags(),
        technicianBands = buildTechnicianBands(),
        supportStatuses = buildSupportStatuses(),
        notificationTypes = buildNotificationTypes(),
        onboardingFields = buildOnboardingFields()
    )

    // ---- Task types ----

    private fun buildTaskTypes(): Map<String, TaskTypeSchema> = mapOf(
        "INSTALL" to TaskTypeSchema(
            label = "Install",
            labelHi = "\u0907\u0902\u0938\u094D\u091F\u0949\u0932",
            color = "#6C5CE7",
            dotColor = "#6C5CE7",
            states = mapOf(
                "OFFERED" to TaskStateSchema(
                    label = "Offered", labelHi = "\u0911\u092B\u0930",
                    actions = listOf(ACTION_CLAIM, ACTION_CLAIM_AND_ASSIGN, ACTION_DECLINE),
                    color = "#6C5CE7",
                    timerField = "offer_expires_at",
                    showCustomerDetails = true,
                    showDeclineReasons = true,
                    reasonLabel = "New Offer",
                    reasonLabelHi = "\u0928\u092F\u093E \u0911\u092B\u0930"
                ),
                "ACCEPTED" to TaskStateSchema(
                    label = "Accepted", labelHi = "\u0938\u094D\u0935\u0940\u0915\u0943\u0924",
                    actions = listOf(ACTION_SCHEDULE, ACTION_ASSIGN, ACTION_START_WORK),
                    color = "#0984E3",
                    timerField = "accept_expires_at",
                    showSlotPicker = true,
                    reasonLabel = "Accepted - Schedule",
                    reasonLabelHi = "\u0938\u094D\u0935\u0940\u0915\u0943\u0924 - \u0936\u0947\u0921\u094D\u092F\u0942\u0932"
                ),
                "SCHEDULED" to TaskStateSchema(
                    label = "Scheduled", labelHi = "\u0936\u0947\u0921\u094D\u092F\u0942\u0932\u094D\u0921",
                    actions = listOf(ACTION_START_WORK, ACTION_REASSIGN),
                    color = "#00B894",
                    timerField = "sla_deadline_at",
                    reasonLabel = "Scheduled",
                    reasonLabelHi = "\u0936\u0947\u0921\u094D\u092F\u0942\u0932\u094D\u0921"
                ),
                "IN_PROGRESS" to TaskStateSchema(
                    label = "In Progress", labelHi = "\u092A\u094D\u0930\u0917\u0924\u093F \u092E\u0947\u0902",
                    actions = listOf(ACTION_START_INSTALLATION, ACTION_REASSIGN),
                    color = "#FDCB6E",
                    timerField = "sla_deadline_at",
                    showProofCapture = true,
                    reasonLabel = "Work In Progress",
                    reasonLabelHi = "\u0915\u093E\u092E \u091C\u093E\u0930\u0940"
                ),
                "INSTALLED" to TaskStateSchema(
                    label = "Installed", labelHi = "\u0907\u0902\u0938\u094D\u091F\u0949\u0932\u094D\u0921",
                    actions = listOf(ACTION_VERIFY),
                    color = "#00CEC9",
                    timerField = "sla_deadline_at",
                    reasonLabel = "Awaiting Verification",
                    reasonLabelHi = "\u0938\u0924\u094D\u092F\u093E\u092A\u0928 \u092A\u094D\u0930\u0924\u0940\u0915\u094D\u0937\u093E"
                ),
                "ACTIVATION_VERIFIED" to TaskStateSchema(
                    label = "Activation Verified",
                    labelHi = "\u0938\u0915\u094D\u0930\u093F\u092F\u0923 \u0938\u0924\u094D\u092F\u093E\u092A\u093F\u0924",
                    actions = emptyList(),
                    color = "#00B894",
                    isTerminal = true,
                    reasonLabel = "Activation Verified",
                    reasonLabelHi = "\u0938\u0915\u094D\u0930\u093F\u092F\u0923 \u0938\u0924\u094D\u092F\u093E\u092A\u093F\u0924"
                ),
                "FAILED" to TaskStateSchema(
                    label = "Failed", labelHi = "\u0935\u093F\u092B\u0932",
                    actions = emptyList(),
                    color = "#D63031",
                    isTerminal = true,
                    reasonLabel = "Failed",
                    reasonLabelHi = "\u0935\u093F\u092B\u0932"
                ),
                "DECLINED" to TaskStateSchema(
                    label = "Declined", labelHi = "\u0905\u0938\u094D\u0935\u0940\u0915\u0943\u0924",
                    actions = emptyList(),
                    color = "#636E72",
                    isTerminal = true,
                    reasonLabel = "Declined",
                    reasonLabelHi = "\u0905\u0938\u094D\u0935\u0940\u0915\u0943\u0924"
                )
            )
        ),
        "RESTORE" to TaskTypeSchema(
            label = "Restore",
            labelHi = "\u0930\u093F\u0938\u094D\u091F\u094B\u0930",
            color = "#D9008D",
            dotColor = "#D9008D",
            states = mapOf(
                "ALERTED" to TaskStateSchema(
                    label = "Alerted", labelHi = "\u0905\u0932\u0930\u094D\u091F",
                    actions = listOf(ACTION_CLAIM, ACTION_CLAIM_AND_ASSIGN, ACTION_DECLINE),
                    color = "#D9008D",
                    timerField = "sla_deadline_at",
                    showDeclineReasons = true,
                    reasonLabel = "Connectivity Loss",
                    reasonLabelHi = "\u0915\u0928\u0947\u0915\u094D\u091F\u093F\u0935\u093F\u091F\u0940 \u0939\u093E\u0928\u093F"
                ),
                "OFFERED" to TaskStateSchema(
                    label = "Offered", labelHi = "\u0911\u092B\u0930",
                    actions = listOf(ACTION_CLAIM, ACTION_CLAIM_AND_ASSIGN, ACTION_DECLINE),
                    color = "#D9008D",
                    timerField = "offer_expires_at",
                    showDeclineReasons = true,
                    reasonLabel = "Restore Needed",
                    reasonLabelHi = "\u0930\u093F\u0938\u094D\u091F\u094B\u0930 \u0906\u0935\u0936\u094D\u092F\u0915"
                ),
                "ACCEPTED" to TaskStateSchema(
                    label = "Accepted", labelHi = "\u0938\u094D\u0935\u0940\u0915\u0943\u0924",
                    actions = listOf(ACTION_START_WORK, ACTION_ASSIGN),
                    color = "#0984E3",
                    timerField = "sla_deadline_at",
                    reasonLabel = "Accepted",
                    reasonLabelHi = "\u0938\u094D\u0935\u0940\u0915\u0943\u0924"
                ),
                "IN_PROGRESS" to TaskStateSchema(
                    label = "In Progress", labelHi = "\u092A\u094D\u0930\u0917\u0924\u093F \u092E\u0947\u0902",
                    actions = listOf(ACTION_RESOLVE, ACTION_RESOLVE_BLOCKED, ACTION_REASSIGN),
                    color = "#FDCB6E",
                    timerField = "sla_deadline_at",
                    showProofCapture = true,
                    reasonLabel = "Restoring",
                    reasonLabelHi = "\u092A\u0941\u0928\u0930\u094D\u0938\u094D\u0925\u093E\u092A\u0928\u093E"
                ),
                "BLOCKED" to TaskStateSchema(
                    label = "Blocked", labelHi = "\u092C\u094D\u0932\u0949\u0915\u094D\u0921",
                    actions = listOf(ACTION_RESOLVE, ACTION_ESCALATE),
                    color = "#E17055",
                    timerField = "blocked_due_at",
                    reasonLabel = "Blocked",
                    reasonLabelHi = "\u092C\u094D\u0932\u0949\u0915\u094D\u0921"
                ),
                "RESOLVED" to TaskStateSchema(
                    label = "Resolved", labelHi = "\u0939\u0932",
                    actions = emptyList(),
                    color = "#00B894",
                    isTerminal = true,
                    reasonLabel = "Resolved",
                    reasonLabelHi = "\u0939\u0932"
                ),
                "UNRESOLVED" to TaskStateSchema(
                    label = "Unresolved", labelHi = "\u0905\u0928\u0938\u0941\u0932\u091D\u093E",
                    actions = emptyList(),
                    color = "#D63031",
                    isTerminal = true,
                    reasonLabel = "Unresolved",
                    reasonLabelHi = "\u0905\u0928\u0938\u0941\u0932\u091D\u093E"
                ),
                "DECLINED" to TaskStateSchema(
                    label = "Declined", labelHi = "\u0905\u0938\u094D\u0935\u0940\u0915\u0943\u0924",
                    actions = emptyList(),
                    color = "#636E72",
                    isTerminal = true,
                    reasonLabel = "Declined",
                    reasonLabelHi = "\u0905\u0938\u094D\u0935\u0940\u0915\u0943\u0924"
                )
            )
        ),
        "NETBOX" to TaskTypeSchema(
            label = "NetBox",
            labelHi = "\u0928\u0947\u091F\u092C\u0949\u0915\u094D\u0938",
            color = "#E67E22",
            dotColor = "#E67E22",
            states = mapOf(
                "OFFERED" to TaskStateSchema(
                    label = "Offered", labelHi = "\u0911\u092B\u0930",
                    actions = listOf(ACTION_CLAIM, ACTION_DECLINE),
                    color = "#E67E22",
                    timerField = "offer_expires_at",
                    showDeclineReasons = true,
                    reasonLabel = "NetBox Recovery",
                    reasonLabelHi = "\u0928\u0947\u091F\u092C\u0949\u0915\u094D\u0938 \u0930\u093F\u0915\u0935\u0930\u0940"
                ),
                "ACCEPTED" to TaskStateSchema(
                    label = "Accepted", labelHi = "\u0938\u094D\u0935\u0940\u0915\u0943\u0924",
                    actions = listOf(ACTION_COLLECT, ACTION_ASSIGN),
                    color = "#0984E3",
                    timerField = "pickup_due_at",
                    reasonLabel = "Accepted - Collect",
                    reasonLabelHi = "\u0938\u094D\u0935\u0940\u0915\u0943\u0924 - \u0907\u0915\u0920\u094D\u0920\u093E"
                ),
                "COLLECTED" to TaskStateSchema(
                    label = "Collected", labelHi = "\u0907\u0915\u0920\u094D\u0920\u093E",
                    actions = listOf(ACTION_CONFIRM_RETURN),
                    color = "#00CEC9",
                    timerField = "return_due_at",
                    showProofCapture = true,
                    reasonLabel = "Collected - Return Pending",
                    reasonLabelHi = "\u0907\u0915\u0920\u094D\u0920\u093E - \u0935\u093E\u092A\u0938\u0940 \u092C\u093E\u0915\u0940"
                ),
                "RETURN_CONFIRMED" to TaskStateSchema(
                    label = "Return Confirmed",
                    labelHi = "\u0935\u093E\u092A\u0938\u0940 \u092A\u0941\u0937\u094D\u091F\u093F",
                    actions = emptyList(),
                    color = "#00B894",
                    isTerminal = true,
                    reasonLabel = "Returned",
                    reasonLabelHi = "\u0935\u093E\u092A\u0938"
                ),
                "LOST_DECLARED" to TaskStateSchema(
                    label = "Lost Declared",
                    labelHi = "\u0916\u094B\u092F\u093E \u0918\u094B\u0937\u093F\u0924",
                    actions = emptyList(),
                    color = "#D63031",
                    isTerminal = true,
                    reasonLabel = "Declared Lost",
                    reasonLabelHi = "\u0916\u094B\u092F\u093E \u0918\u094B\u0937\u093F\u0924"
                ),
                "DECLINED" to TaskStateSchema(
                    label = "Declined", labelHi = "\u0905\u0938\u094D\u0935\u0940\u0915\u0943\u0924",
                    actions = emptyList(),
                    color = "#636E72",
                    isTerminal = true,
                    reasonLabel = "Declined",
                    reasonLabelHi = "\u0905\u0938\u094D\u0935\u0940\u0915\u0943\u0924"
                )
            )
        ),
        "RECHARGE" to TaskTypeSchema(
            label = "Recharge",
            labelHi = "\u0930\u093F\u091A\u093E\u0930\u094D\u091C",
            color = "#00B894",
            dotColor = "#00B894",
            states = mapOf(
                "PENDING_RECHARGE" to TaskStateSchema(
                    label = "Pending Recharge", labelHi = "\u0930\u093F\u091A\u093E\u0930\u094D\u091C \u092C\u093E\u0915\u0940",
                    actions = listOf(ACTION_ACKNOWLEDGE_RECHARGE, ACTION_START_RECHARGE),
                    color = "#FDCB6E",
                    timerField = "due_at",
                    reasonLabel = "Recharge Due",
                    reasonLabelHi = "\u0930\u093F\u091A\u093E\u0930\u094D\u091C \u092C\u093E\u0915\u0940"
                ),
                "IN_PROGRESS" to TaskStateSchema(
                    label = "Recharging", labelHi = "\u0930\u093F\u091A\u093E\u0930\u094D\u091C \u091C\u093E\u0930\u0940",
                    actions = emptyList(),
                    color = "#0984E3",
                    reasonLabel = "Recharge In Progress",
                    reasonLabelHi = "\u0930\u093F\u091A\u093E\u0930\u094D\u091C \u091C\u093E\u0930\u0940"
                ),
                "COMPLETED" to TaskStateSchema(
                    label = "Recharged", labelHi = "\u0930\u093F\u091A\u093E\u0930\u094D\u091C \u0939\u0941\u0906",
                    actions = emptyList(),
                    color = "#00B894",
                    isTerminal = true,
                    reasonLabel = "Recharge Complete",
                    reasonLabelHi = "\u0930\u093F\u091A\u093E\u0930\u094D\u091C \u092A\u0942\u0930\u093E"
                ),
                "FAILED" to TaskStateSchema(
                    label = "Failed", labelHi = "\u0935\u093F\u092B\u0932",
                    actions = listOf(ACTION_START_RECHARGE),
                    color = "#D63031",
                    reasonLabel = "Recharge Failed",
                    reasonLabelHi = "\u0930\u093F\u091A\u093E\u0930\u094D\u091C \u0935\u093F\u092B\u0932"
                )
            )
        )
    )

    // ---- Assurance chips ----

    private fun buildAssuranceChips(): List<ChipSchema> = listOf(
        ChipSchema(
            id = "active_base", label = "Active Base",
            labelHi = "\u0938\u0915\u094D\u0930\u093F\u092F \u092C\u0947\u0938",
            valueField = "activeBase", format = "number"
        ),
        ChipSchema(
            id = "cycle_earnings", label = "Cycle Earnings",
            labelHi = "\u091A\u0915\u094D\u0930 \u0906\u092F",
            valueField = "cycleEarned", format = "currency"
        ),
        ChipSchema(
            id = "sla_standing", label = "SLA",
            labelHi = "SLA",
            valueField = "slaStanding", stateColorField = "slaStanding",
            drillDownType = "sla"
        ),
        ChipSchema(
            id = "exposure", label = "Exposure",
            labelHi = "\u090F\u0915\u094D\u0938\u092A\u094B\u091C\u093C\u0930",
            valueField = "exposureState", stateColorField = "exposureState",
            trendField = "exposureTrend", drillDownType = "exposure"
        )
    )

    // ---- Wallet line types ----

    private fun buildWalletLineTypes(): Map<String, WalletLineTypeSchema> = mapOf(
        "SETTLEMENT" to WalletLineTypeSchema(
            label = "Settlement", labelHi = "\u0928\u093F\u092A\u091F\u093E\u0928",
            category = "earnings", isCredit = true, color = "#34C759"
        ),
        "BONUS" to WalletLineTypeSchema(
            label = "Quality Bonus", labelHi = "\u0917\u0941\u0923\u0935\u0924\u094D\u0924\u093E \u092C\u094B\u0928\u0938",
            category = "earnings", isCredit = true, color = "#34C759"
        ),
        "INSTALL_HANDLING" to WalletLineTypeSchema(
            label = "Install Handling", labelHi = "\u0907\u0902\u0938\u094D\u091F\u0949\u0932 \u0939\u0948\u0902\u0921\u0932\u093F\u0902\u0917",
            category = "earnings", isCredit = true, color = "#34C759"
        ),
        "COLLECTION_HANDLING" to WalletLineTypeSchema(
            label = "Collection Handling", labelHi = "\u0915\u0932\u0947\u0915\u094D\u0936\u0928 \u0939\u0948\u0902\u0921\u0932\u093F\u0902\u0917",
            category = "earnings", isCredit = true, color = "#34C759"
        ),
        "WITHDRAWAL" to WalletLineTypeSchema(
            label = "Withdrawal", labelHi = "\u0928\u093F\u0915\u093E\u0938\u0940",
            category = "transactions", isCredit = false, color = "#FF3B30"
        ),
        "TOP_UP" to WalletLineTypeSchema(
            label = "Top Up", labelHi = "\u091F\u0949\u092A \u0905\u092A",
            category = "transactions", isCredit = true, color = "#34C759"
        ),
        "CARRY_FEE" to WalletLineTypeSchema(
            label = "Carry Fee", labelHi = "\u0915\u0948\u0930\u0940 \u0936\u0941\u0932\u094D\u0915",
            category = "deposits_fees", isCredit = false, color = "#FF8000"
        ),
        "LOSS_RECOVERY" to WalletLineTypeSchema(
            label = "Loss Recovery", labelHi = "\u0939\u093E\u0928\u093F \u0935\u0938\u0942\u0932\u0940",
            category = "deposits_fees", isCredit = false, color = "#FF3B30"
        ),
        "DEDUCTION" to WalletLineTypeSchema(
            label = "Deduction", labelHi = "\u0915\u091F\u094C\u0924\u0940",
            category = "deposits_fees", isCredit = false, color = "#FF3B30"
        ),
        "RECHARGE_SHARE" to WalletLineTypeSchema(
            label = "Recharge Share", labelHi = "\u0930\u093F\u091A\u093E\u0930\u094D\u091C \u0939\u093F\u0938\u094D\u0938\u093E",
            category = "earnings", isCredit = true, color = "#00B894"
        )
    )

    // ---- Decline reasons ----

    private fun buildDeclineReasons(): List<LabeledItem> = listOf(
        LabeledItem(id = "customer_unavailable", label = "Customer unavailable", labelHi = "\u0917\u094D\u0930\u093E\u0939\u0915 \u0905\u0928\u0941\u092A\u0932\u092C\u094D\u0927"),
        LabeledItem(id = "wrong_address", label = "Wrong address", labelHi = "\u0917\u0932\u0924 \u092A\u0924\u093E"),
        LabeledItem(id = "area_inaccessible", label = "Area inaccessible", labelHi = "\u0915\u094D\u0937\u0947\u0924\u094D\u0930 \u092A\u0939\u0941\u0901\u091A \u0938\u0947 \u092C\u093E\u0939\u0930"),
        LabeledItem(id = "equipment_missing", label = "Equipment missing", labelHi = "\u0909\u092A\u0915\u0930\u0923 \u0917\u093E\u092F\u092C"),
        LabeledItem(id = "other", label = "Other", labelHi = "\u0905\u0928\u094D\u092F")
    )

    // ---- Quick notes ----

    private fun buildQuickNotes(): List<LabeledItem> = listOf(
        LabeledItem(id = "customer_not_available", label = "Customer not available", labelHi = "\u0917\u094D\u0930\u093E\u0939\u0915 \u0909\u092A\u0932\u092C\u094D\u0927 \u0928\u0939\u0940\u0902"),
        LabeledItem(id = "wrong_address", label = "Wrong address", labelHi = "\u0917\u0932\u0924 \u092A\u0924\u093E"),
        LabeledItem(id = "need_material", label = "Need material", labelHi = "\u0938\u093E\u092E\u0917\u094D\u0930\u0940 \u091A\u093E\u0939\u093F\u090F"),
        LabeledItem(id = "rescheduled", label = "Rescheduled", labelHi = "\u092A\u0941\u0928\u0930\u094D\u0928\u093F\u0930\u094D\u0927\u093E\u0930\u093F\u0924"),
        LabeledItem(id = "waiting_for_access", label = "Waiting for access", labelHi = "\u092A\u0939\u0941\u0901\u091A \u0915\u0940 \u092A\u094D\u0930\u0924\u0940\u0915\u094D\u0937\u093E")
    )

    // ---- Help reasons ----

    private fun buildHelpReasons(): List<LabeledItem> = listOf(
        LabeledItem(id = "technical_issue", label = "Technical issue", labelHi = "\u0924\u0915\u0928\u0940\u0915\u0940 \u0938\u092E\u0938\u094D\u092F\u093E"),
        LabeledItem(id = "customer_dispute", label = "Customer dispute", labelHi = "\u0917\u094D\u0930\u093E\u0939\u0915 \u0935\u093F\u0935\u093E\u0926"),
        LabeledItem(id = "equipment_problem", label = "Equipment problem", labelHi = "\u0909\u092A\u0915\u0930\u0923 \u0938\u092E\u0938\u094D\u092F\u093E"),
        LabeledItem(id = "safety_concern", label = "Safety concern", labelHi = "\u0938\u0941\u0930\u0915\u094D\u0937\u093E \u091A\u093F\u0902\u0924\u093E"),
        LabeledItem(id = "other", label = "Other", labelHi = "\u0905\u0928\u094D\u092F")
    )

    // ---- SLA domains ----

    private fun buildSlaDomains(): List<SlaDomainSchema> = listOf(
        SlaDomainSchema(
            id = "installation",
            label = "Installation", labelHi = "\u0907\u0902\u0938\u094D\u091F\u0949\u0932\u0947\u0936\u0928",
            metrics = listOf(
                SlaMetricSchema(
                    id = "install_completion_rate",
                    label = "Install Completion Rate",
                    labelHi = "\u0907\u0902\u0938\u094D\u091F\u0949\u0932 \u092A\u0942\u0930\u094D\u0923\u0924\u093E \u0926\u0930",
                    direction = "above", unit = "percentage", format = "percentage"
                ),
                SlaMetricSchema(
                    id = "install_avg_days",
                    label = "Avg Install Days",
                    labelHi = "\u0914\u0938\u0924 \u0907\u0902\u0938\u094D\u091F\u0949\u0932 \u0926\u093F\u0928",
                    direction = "below", unit = "days", format = "number"
                )
            )
        ),
        SlaDomainSchema(
            id = "resolution",
            label = "Resolution", labelHi = "\u0938\u092E\u093E\u0927\u093E\u0928",
            metrics = listOf(
                SlaMetricSchema(
                    id = "restore_sla_hit",
                    label = "Restore SLA Hit Rate",
                    labelHi = "\u0930\u093F\u0938\u094D\u091F\u094B\u0930 SLA \u0939\u093F\u091F \u0926\u0930",
                    direction = "above", unit = "percentage", format = "percentage"
                ),
                SlaMetricSchema(
                    id = "restore_avg_hours",
                    label = "Avg Restore Hours",
                    labelHi = "\u0914\u0938\u0924 \u0930\u093F\u0938\u094D\u091F\u094B\u0930 \u0918\u0902\u091F\u0947",
                    direction = "below", unit = "hours", format = "number"
                )
            )
        ),
        SlaDomainSchema(
            id = "stability",
            label = "Stability", labelHi = "\u0938\u094D\u0925\u093F\u0930\u0924\u093E",
            metrics = listOf(
                SlaMetricSchema(
                    id = "churn_rate",
                    label = "Churn Rate",
                    labelHi = "\u091A\u0930\u094D\u0928 \u0926\u0930",
                    direction = "below", unit = "percentage", format = "percentage"
                )
            )
        ),
        SlaDomainSchema(
            id = "experience",
            label = "Experience", labelHi = "\u0905\u0928\u0941\u092D\u0935",
            metrics = listOf(
                SlaMetricSchema(
                    id = "csat_score",
                    label = "CSAT Score",
                    labelHi = "CSAT \u0938\u094D\u0915\u094B\u0930",
                    direction = "above", unit = "score", format = "number"
                )
            )
        )
    )

    // ---- Exposure states ----

    private fun buildExposureStates(): Map<String, ExposureStateSchema> = mapOf(
        "ELIGIBLE" to ExposureStateSchema(
            label = "Eligible", labelHi = "\u092A\u093E\u0924\u094D\u0930",
            color = "#00B894",
            description = "Full routing",
            descriptionHi = "\u092A\u0942\u0930\u094D\u0923 \u0930\u093E\u0909\u091F\u093F\u0902\u0917"
        ),
        "LIMITED" to ExposureStateSchema(
            label = "Limited", labelHi = "\u0938\u0940\u092E\u093F\u0924",
            color = "#FDCB6E",
            description = "Tapered routing",
            descriptionHi = "\u0915\u092E \u0930\u093E\u0909\u091F\u093F\u0902\u0917"
        ),
        "INELIGIBLE" to ExposureStateSchema(
            label = "Ineligible", labelHi = "\u0905\u092A\u093E\u0924\u094D\u0930",
            color = "#D63031",
            description = "No new routing",
            descriptionHi = "\u0915\u094B\u0908 \u0928\u0908 \u0930\u093E\u0909\u091F\u093F\u0902\u0917 \u0928\u0939\u0940\u0902"
        )
    )

    // ---- Delegation states ----

    private fun buildDelegationStates(): Map<String, DelegationStateSchema> = mapOf(
        "UNASSIGNED" to DelegationStateSchema(
            label = "Unassigned", labelHi = "\u0905\u0928\u093F\u0930\u094D\u0927\u093E\u0930\u093F\u0924"
        ),
        "ASSIGNED" to DelegationStateSchema(
            label = "Assigned", labelHi = "\u0928\u093F\u0930\u094D\u0927\u093E\u0930\u093F\u0924"
        ),
        "ACCEPTED" to DelegationStateSchema(
            label = "Accepted", labelHi = "\u0938\u094D\u0935\u0940\u0915\u0943\u0924"
        ),
        "IN_PROGRESS" to DelegationStateSchema(
            label = "In Progress", labelHi = "\u092A\u094D\u0930\u0917\u0924\u093F \u092E\u0947\u0902"
        ),
        "BLOCKED" to DelegationStateSchema(
            label = "Blocked", labelHi = "\u092C\u094D\u0932\u0949\u0915\u094D\u0921"
        ),
        "DONE" to DelegationStateSchema(
            label = "Done", labelHi = "\u092A\u0942\u0930\u093E",
            isTerminal = true
        )
    )

    // ---- Escalation flags ----

    private fun buildEscalationFlags(): Map<String, EscalationFlagSchema> = mapOf(
        "BLOCKED_STALE" to EscalationFlagSchema(
            label = "Blocked Stale", labelHi = "\u092C\u094D\u0932\u0949\u0915 \u092A\u0941\u0930\u093E\u0928\u093E",
            severity = 1, color = "#D63031"
        ),
        "RETURN_OVERDUE" to EscalationFlagSchema(
            label = "Return Overdue", labelHi = "\u0935\u093E\u092A\u0938\u0940 \u0905\u0924\u093F\u0926\u0947\u092F",
            severity = 1, color = "#D63031"
        ),
        "VERIFICATION_PENDING" to EscalationFlagSchema(
            label = "Verification Pending", labelHi = "\u0938\u0924\u094D\u092F\u093E\u092A\u0928 \u0932\u0902\u092C\u093F\u0924",
            severity = 2, color = "#E17055"
        ),
        "SLA_BREACH_IMMINENT" to EscalationFlagSchema(
            label = "SLA Breach Imminent", labelHi = "SLA \u0909\u0932\u094D\u0932\u0902\u0918\u0928 \u0906\u0938\u0928\u094D\u0928",
            severity = 1, color = "#D63031"
        ),
        "OFFER_EXPIRING" to EscalationFlagSchema(
            label = "Offer Expiring", labelHi = "\u0911\u092B\u0930 \u0938\u092E\u093E\u092A\u094D\u0924\u093F",
            severity = 2, color = "#E17055"
        ),
        "HIGH_RESTORE_ALERT" to EscalationFlagSchema(
            label = "High Restore Alert", labelHi = "\u0909\u091A\u094D\u091A \u0930\u093F\u0938\u094D\u091F\u094B\u0930 \u0905\u0932\u0930\u094D\u091F",
            severity = 1, color = "#D63031"
        ),
        "REPEAT_VISIT" to EscalationFlagSchema(
            label = "Repeat Visit", labelHi = "\u0926\u094B\u092C\u093E\u0930\u093E \u0926\u094C\u0930\u093E",
            severity = 3, color = "#FDCB6E"
        ),
        "CUSTOMER_ESCALATION" to EscalationFlagSchema(
            label = "Customer Escalation", labelHi = "\u0917\u094D\u0930\u093E\u0939\u0915 \u090F\u0938\u094D\u0915\u0932\u0947\u0936\u0928",
            severity = 2, color = "#E17055"
        ),
        "QUALITY_FLAG" to EscalationFlagSchema(
            label = "Quality Flag", labelHi = "\u0917\u0941\u0923\u0935\u0924\u094D\u0924\u093E \u092B\u094D\u0932\u0948\u0917",
            severity = 3, color = "#FDCB6E"
        ),
        "PICKUP_OVERDUE" to EscalationFlagSchema(
            label = "Pickup Overdue", labelHi = "\u092A\u093F\u0915\u0905\u092A \u0905\u0924\u093F\u0926\u0947\u092F",
            severity = 2, color = "#E17055"
        ),
        "ACCEPT_EXPIRING" to EscalationFlagSchema(
            label = "Accept Expiring", labelHi = "\u0938\u094D\u0935\u0940\u0915\u0943\u0924\u093F \u0938\u092E\u093E\u092A\u094D\u0924\u093F",
            severity = 2, color = "#E17055"
        ),
        "CHAIN_GROWING" to EscalationFlagSchema(
            label = "Chain Growing", labelHi = "\u0936\u094D\u0930\u0943\u0902\u0916\u0932\u093E \u092C\u0922\u093C \u0930\u0939\u0940",
            severity = 3, color = "#FDCB6E"
        ),
        "MANUAL_EXCEPTION" to EscalationFlagSchema(
            label = "Manual Exception", labelHi = "\u092E\u0948\u0928\u0941\u0905\u0932 \u0905\u092A\u0935\u093E\u0926",
            severity = 2, color = "#E17055"
        )
    )

    // ---- Technician bands ----

    private fun buildTechnicianBands(): List<LabeledItem> = listOf(
        LabeledItem(id = "A", label = "Band A", labelHi = "\u092C\u0948\u0902\u0921 A"),
        LabeledItem(id = "B", label = "Band B", labelHi = "\u092C\u0948\u0902\u0921 B"),
        LabeledItem(id = "C", label = "Band C", labelHi = "\u092C\u0948\u0902\u0921 C")
    )

    // ---- Support statuses ----

    private fun buildSupportStatuses(): Map<String, StatusDisplaySchema> = mapOf(
        "OPEN" to StatusDisplaySchema(
            label = "Open", labelHi = "\u0916\u0941\u0932\u093E",
            color = "#E67E22", bgColor = "#FFF3E0"
        ),
        "IN_PROGRESS" to StatusDisplaySchema(
            label = "In Progress", labelHi = "\u092A\u094D\u0930\u0917\u0924\u093F \u092E\u0947\u0902",
            color = "#D9008D", bgColor = "#FCE4EC"
        ),
        "RESOLVED" to StatusDisplaySchema(
            label = "Resolved", labelHi = "\u0939\u0932",
            color = "#00B894", bgColor = "#E8F5E9"
        ),
        "CLOSED" to StatusDisplaySchema(
            label = "Closed", labelHi = "\u092C\u0902\u0926",
            color = "#636E72", bgColor = "#ECEFF1"
        )
    )

    // ---- Notification types ----

    private fun buildNotificationTypes(): Map<String, NotificationTypeSchema> = mapOf(
        "TASK_OFFERED" to NotificationTypeSchema(
            label = "Task Offered", labelHi = "\u0915\u093E\u0930\u094D\u092F \u0911\u092B\u0930",
            sound = "standard"
        ),
        "TASK_CLAIMED" to NotificationTypeSchema(
            label = "Task Claimed", labelHi = "\u0915\u093E\u0930\u094D\u092F \u0938\u094D\u0935\u0940\u0915\u0943\u0924",
            sound = "standard"
        ),
        "SLA_CHANGE" to NotificationTypeSchema(
            label = "SLA Change", labelHi = "SLA \u092A\u0930\u093F\u0935\u0930\u094D\u0924\u0928",
            sound = "standard"
        ),
        "WALLET_UPDATE" to NotificationTypeSchema(
            label = "Wallet Update", labelHi = "\u0935\u0949\u0932\u0947\u091F \u0905\u092A\u0921\u0947\u091F",
            sound = "standard"
        ),
        "ESCALATION" to NotificationTypeSchema(
            label = "Escalation", labelHi = "\u090F\u0938\u094D\u0915\u0932\u0947\u0936\u0928",
            sound = "urgent"
        ),
        "SUPPORT_REPLY" to NotificationTypeSchema(
            label = "Support Reply", labelHi = "\u0938\u092A\u094B\u0930\u094D\u091F \u0909\u0924\u094D\u0924\u0930",
            sound = "standard"
        ),
        "TECH_ACTION" to NotificationTypeSchema(
            label = "Technician Action", labelHi = "\u0924\u0915\u0928\u0940\u0936\u093F\u092F\u0928 \u0915\u093E\u0930\u094D\u0930\u0935\u093E\u0908",
            sound = "standard"
        ),
        "SYSTEM_ALERT" to NotificationTypeSchema(
            label = "System Alert", labelHi = "\u0938\u093F\u0938\u094D\u091F\u092E \u0905\u0932\u0930\u094D\u091F",
            sound = "standard"
        ),
        "QUALITY_EVENT" to NotificationTypeSchema(
            label = "Quality Event", labelHi = "\u0917\u0941\u0923\u0935\u0924\u094D\u0924\u093E \u0918\u091F\u0928\u093E",
            sound = "standard"
        ),
        "HIGH_RESTORE_ALERT" to NotificationTypeSchema(
            label = "High Restore Alert",
            labelHi = "\u0909\u091A\u094D\u091A \u0930\u093F\u0938\u094D\u091F\u094B\u0930 \u0905\u0932\u0930\u094D\u091F",
            isUrgent = true, sound = "urgent"
        )
    )

    // ---- Onboarding fields ----

    private fun buildOnboardingFields(): List<FormFieldSchema> = listOf(
        FormFieldSchema(
            id = "business_name", label = "Business Name", labelHi = "\u0935\u094D\u092F\u0935\u0938\u093E\u092F \u0915\u093E \u0928\u093E\u092E",
            type = "text", section = "business", required = true,
            minLength = 2, maxLength = 100
        ),
        FormFieldSchema(
            id = "entity_type", label = "Entity Type", labelHi = "\u0907\u0915\u093E\u0908 \u092A\u094D\u0930\u0915\u093E\u0930",
            type = "select", section = "business", required = true,
            options = listOf(
                LabeledItem(id = "individual", label = "Individual", labelHi = "\u0935\u094D\u092F\u0915\u094D\u0924\u093F"),
                LabeledItem(id = "firm", label = "Firm", labelHi = "\u092B\u0930\u094D\u092E"),
                LabeledItem(id = "company", label = "Company", labelHi = "\u0915\u0902\u092A\u0928\u0940")
            )
        ),
        FormFieldSchema(
            id = "state", label = "State", labelHi = "\u0930\u093E\u091C\u094D\u092F",
            type = "text", section = "address", required = true
        ),
        FormFieldSchema(
            id = "city", label = "City", labelHi = "\u0936\u0939\u0930",
            type = "text", section = "address", required = true
        ),
        FormFieldSchema(
            id = "area", label = "Area", labelHi = "\u0915\u094D\u0937\u0947\u0924\u094D\u0930",
            type = "text", section = "address", required = true
        ),
        FormFieldSchema(
            id = "pincode", label = "Pincode", labelHi = "\u092A\u093F\u0928\u0915\u094B\u0921",
            type = "number", section = "address", required = true,
            validation = "^[0-9]{6}$", minLength = 6, maxLength = 6
        ),
        FormFieldSchema(
            id = "aadhaar", label = "Aadhaar Number", labelHi = "\u0906\u0927\u093E\u0930 \u0928\u0902\u092C\u0930",
            type = "number", section = "kyc", required = true,
            validation = "^[0-9]{12}$", minLength = 12, maxLength = 12
        ),
        FormFieldSchema(
            id = "pan", label = "PAN", labelHi = "\u092A\u0948\u0928",
            type = "text", section = "kyc", required = true,
            validation = "^[A-Z]{5}[0-9]{4}[A-Z]$", minLength = 10, maxLength = 10
        ),
        FormFieldSchema(
            id = "account_number", label = "Account Number", labelHi = "\u0916\u093E\u0924\u093E \u0928\u0902\u092C\u0930",
            type = "number", section = "bank", required = true,
            validation = "^[0-9]{9,18}$", minLength = 9, maxLength = 18
        ),
        FormFieldSchema(
            id = "ifsc", label = "IFSC Code", labelHi = "IFSC \u0915\u094B\u0921",
            type = "text", section = "bank", required = true,
            validation = "^[A-Z]{4}0[A-Z0-9]{6}$", minLength = 11, maxLength = 11
        ),
        FormFieldSchema(
            id = "bank_name", label = "Bank Name", labelHi = "\u092C\u0948\u0902\u0915 \u0915\u093E \u0928\u093E\u092E",
            type = "text", section = "bank", required = true
        ),
        FormFieldSchema(
            id = "agreement", label = "I agree to terms and conditions",
            labelHi = "\u092E\u0948\u0902 \u0928\u093F\u092F\u092E \u0914\u0930 \u0936\u0930\u094D\u0924\u094B\u0902 \u0938\u0947 \u0938\u0939\u092E\u0924 \u0939\u0942\u0901",
            type = "checkbox", section = "agreement", required = true
        )
    )

    // ---------------------------------------------------------------
    // 2. buildSeedTasks()
    // ---------------------------------------------------------------
    fun buildSeedTasks(): List<TaskData> = listOf(
        // 1. INSTALL OFFERED HIGH priority
        TaskData(
            taskId = "TSK-INS-001",
            taskType = "INSTALL",
            currentState = "OFFERED",
            priority = "HIGH",
            connectionId = "CN-2847",
            customerArea = "Sector 12, Pune",
            customerName = "Priya Sharma",
            customerPhone = "+919876543210",
            offerExpiresAt = minutesFromNow(30),
            createdAt = minutesAgo(5),
            updatedAt = minutesAgo(5),
            escalationFlags = listOf("OFFER_EXPIRING"),
            timeline = listOf(
                TimelineEntry(
                    timestamp = minutesAgo(5),
                    eventType = "CREATED",
                    actor = "system",
                    actorType = "SYSTEM",
                    detail = "Install task created and offered"
                )
            )
        ),

        // 2. INSTALL ACCEPTED NORMAL
        TaskData(
            taskId = "TSK-INS-002",
            taskType = "INSTALL",
            currentState = "ACCEPTED",
            priority = "NORMAL",
            connectionId = "CN-3102",
            customerArea = "Baner, Pune",
            customerName = "Rajesh Kulkarni",
            customerPhone = "+919812345678",
            acceptExpiresAt = hoursFromNow(2),
            createdAt = minutesAgo(45),
            updatedAt = minutesAgo(10),
            timeline = listOf(
                TimelineEntry(
                    timestamp = minutesAgo(45),
                    eventType = "CREATED",
                    actor = "system",
                    actorType = "SYSTEM",
                    detail = "Install task created and offered"
                ),
                TimelineEntry(
                    timestamp = minutesAgo(10),
                    eventType = "CLAIMED",
                    actor = "csp",
                    actorType = "CSP",
                    detail = "Task claimed by CSP"
                )
            ),
            notes = listOf(
                NoteData(
                    id = "N-001",
                    text = "Customer prefers morning slot",
                    author = "system",
                    authorType = "SYSTEM",
                    createdAt = minutesAgo(45)
                )
            )
        ),

        // 3. INSTALL IN_PROGRESS NORMAL, self-assigned by CSP
        TaskData(
            taskId = "TSK-INS-003",
            taskType = "INSTALL",
            currentState = "IN_PROGRESS",
            priority = "NORMAL",
            connectionId = "CN-1589",
            customerArea = "Wakad, Pune",
            customerName = "Meena Deshmukh",
            customerPhone = "+919988776655",
            slaDeadlineAt = hoursFromNow(4),
            delegationState = "IN_PROGRESS",
            assignedTo = "Self (CSP-MH-1001)",
            assignedToName = "Self (CSP)",
            createdAt = hoursAgo(3),
            updatedAt = minutesAgo(30),
            timeline = listOf(
                TimelineEntry(
                    timestamp = hoursAgo(3),
                    eventType = "CREATED",
                    actor = "system",
                    actorType = "SYSTEM",
                    detail = "Install task created"
                ),
                TimelineEntry(
                    timestamp = hoursAgo(2),
                    eventType = "CLAIMED",
                    actor = "csp",
                    actorType = "CSP",
                    detail = "Task claimed by CSP"
                ),
                TimelineEntry(
                    timestamp = hoursAgo(1),
                    eventType = "SCHEDULED",
                    actor = "csp",
                    actorType = "CSP",
                    detail = "Scheduled for today"
                ),
                TimelineEntry(
                    timestamp = minutesAgo(30),
                    eventType = "STARTED",
                    actor = "TECH-001",
                    actorType = "TECHNICIAN",
                    detail = "Work started by Ajay Patil"
                )
            )
        ),

        // 4. RESTORE ALERTED HIGH, bucket-0 candidate
        TaskData(
            taskId = "TSK-RST-001",
            taskType = "RESTORE",
            currentState = "ALERTED",
            priority = "HIGH",
            connectionId = "CN-4201",
            customerArea = "Hinjewadi, Pune",
            customerName = "Amit Joshi",
            customerPhone = "+919900112233",
            slaDeadlineAt = hoursFromNow(1),
            createdAt = minutesAgo(15),
            updatedAt = minutesAgo(15),
            escalationFlags = listOf("HIGH_RESTORE_ALERT", "SLA_BREACH_IMMINENT"),
            timeline = listOf(
                TimelineEntry(
                    timestamp = minutesAgo(15),
                    eventType = "ALERT_RAISED",
                    actor = "system",
                    actorType = "SYSTEM",
                    detail = "Connectivity loss detected for CN-4201"
                )
            )
        ),

        // 5. RESTORE IN_PROGRESS NORMAL, assigned to tech
        TaskData(
            taskId = "TSK-RST-002",
            taskType = "RESTORE",
            currentState = "IN_PROGRESS",
            priority = "NORMAL",
            connectionId = "CN-2903",
            customerArea = "Kothrud, Pune",
            customerName = "Sneha Pawar",
            customerPhone = "+919811223344",
            slaDeadlineAt = hoursFromNow(3),
            delegationState = "IN_PROGRESS",
            assignedTo = "TECH-002",
            assignedToName = "Suresh Kamble",
            createdAt = hoursAgo(2),
            updatedAt = minutesAgo(45),
            timeline = listOf(
                TimelineEntry(
                    timestamp = hoursAgo(2),
                    eventType = "ALERT_RAISED",
                    actor = "system",
                    actorType = "SYSTEM",
                    detail = "Connectivity loss detected for CN-2903"
                ),
                TimelineEntry(
                    timestamp = hoursAgo(1),
                    eventType = "CLAIMED",
                    actor = "csp",
                    actorType = "CSP",
                    detail = "Task claimed and assigned to Suresh Kamble"
                ),
                TimelineEntry(
                    timestamp = minutesAgo(45),
                    eventType = "STARTED",
                    actor = "TECH-002",
                    actorType = "TECHNICIAN",
                    detail = "Work started by Suresh Kamble"
                )
            ),
            notes = listOf(
                NoteData(
                    id = "N-002",
                    text = "Router power issue suspected",
                    author = "TECH-002",
                    authorType = "TECHNICIAN",
                    createdAt = minutesAgo(30)
                )
            )
        ),

        // 6. NETBOX OFFERED NORMAL
        TaskData(
            taskId = "TSK-NBX-001",
            taskType = "NETBOX",
            currentState = "OFFERED",
            priority = "NORMAL",
            netboxId = "NB-1004",
            connectionId = "CN-3300",
            customerArea = "Aundh, Pune",
            customerName = "Vikram Rao",
            customerPhone = "+919822334455",
            offerExpiresAt = daysFromNow(2),
            pickupDueAt = daysFromNow(2),
            createdAt = hoursAgo(1),
            updatedAt = hoursAgo(1),
            timeline = listOf(
                TimelineEntry(
                    timestamp = hoursAgo(1),
                    eventType = "CREATED",
                    actor = "system",
                    actorType = "SYSTEM",
                    detail = "NetBox recovery task created for NB-1004"
                )
            )
        ),

        // 7. NETBOX COLLECTED NORMAL, proof captured
        TaskData(
            taskId = "TSK-NBX-002",
            taskType = "NETBOX",
            currentState = "COLLECTED",
            priority = "NORMAL",
            netboxId = "NB-1002",
            connectionId = "CN-2100",
            customerArea = "Hadapsar, Pune",
            customerName = "Anil Gaikwad",
            customerPhone = "+919833445566",
            returnDueAt = daysFromNow(5),
            proofBundle = "proof_nbx_1002_collected.jpg",
            delegationState = "IN_PROGRESS",
            assignedTo = "TECH-001",
            assignedToName = "Ajay Patil",
            createdAt = daysAgo(3),
            updatedAt = daysAgo(1),
            timeline = listOf(
                TimelineEntry(
                    timestamp = daysAgo(3),
                    eventType = "CREATED",
                    actor = "system",
                    actorType = "SYSTEM",
                    detail = "NetBox recovery task created for NB-1002"
                ),
                TimelineEntry(
                    timestamp = daysAgo(2),
                    eventType = "CLAIMED",
                    actor = "csp",
                    actorType = "CSP",
                    detail = "Task claimed"
                ),
                TimelineEntry(
                    timestamp = daysAgo(1),
                    eventType = "COLLECTED",
                    actor = "TECH-001",
                    actorType = "TECHNICIAN",
                    detail = "NetBox collected by Ajay Patil",
                    proof = "proof_nbx_1002_collected.jpg"
                )
            )
        ),

        // 8. RECHARGE PENDING_RECHARGE — batch of customers to recharge
        TaskData(
            taskId = "TSK-RCH-001",
            taskType = "RECHARGE",
            currentState = "PENDING_RECHARGE",
            priority = "NORMAL",
            connectionId = "BATCH-001",
            customerArea = "Hinjewadi, Pune",
            customerName = "5 Customers",
            dueAt = hoursFromNow(6),
            createdAt = hoursAgo(1),
            updatedAt = hoursAgo(1),
            timeline = listOf(
                TimelineEntry(
                    timestamp = hoursAgo(1),
                    eventType = "CREATED",
                    actor = "system",
                    actorType = "SYSTEM",
                    detail = "Recharge batch created — 5 customers due for renewal"
                )
            )
        ),

        // 9. INSTALL ACTIVATION_VERIFIED NORMAL (terminal, completed)
        TaskData(
            taskId = "TSK-INS-004",
            taskType = "INSTALL",
            currentState = "ACTIVATION_VERIFIED",
            priority = "NORMAL",
            connectionId = "CN-1001",
            customerArea = "Viman Nagar, Pune",
            customerName = "Sanjay Bhosle",
            customerPhone = "+919844556677",
            proofBundle = "proof_ins_1001_complete.jpg",
            delegationState = "DONE",
            assignedTo = "TECH-001",
            assignedToName = "Ajay Patil",
            createdAt = daysAgo(5),
            updatedAt = daysAgo(2),
            timeline = listOf(
                TimelineEntry(
                    timestamp = daysAgo(5),
                    eventType = "CREATED",
                    actor = "system",
                    actorType = "SYSTEM",
                    detail = "Install task created"
                ),
                TimelineEntry(
                    timestamp = daysAgo(4),
                    eventType = "CLAIMED",
                    actor = "csp",
                    actorType = "CSP",
                    detail = "Task claimed"
                ),
                TimelineEntry(
                    timestamp = daysAgo(3),
                    eventType = "STARTED",
                    actor = "TECH-001",
                    actorType = "TECHNICIAN",
                    detail = "Work started"
                ),
                TimelineEntry(
                    timestamp = daysAgo(3),
                    eventType = "INSTALLED",
                    actor = "TECH-001",
                    actorType = "TECHNICIAN",
                    detail = "Installation completed",
                    proof = "proof_ins_1001_complete.jpg"
                ),
                TimelineEntry(
                    timestamp = daysAgo(2),
                    eventType = "VERIFIED",
                    actor = "system",
                    actorType = "SYSTEM",
                    detail = "Activation verified by system"
                )
            )
        )
    )

    // ---------------------------------------------------------------
    // 3. buildSeedAssurance()
    // ---------------------------------------------------------------
    fun buildSeedAssurance(): AssuranceData = AssuranceData(
        activeBase = 47,
        cycleEarned = 12500.0,
        nextSettlementAt = daysFromNow(3),
        lifetimeEarnings = 87500.0,
        slaStanding = "COMPLIANT",
        exposureState = "ELIGIBLE",
        exposureReason = null,
        exposureSince = null,
        exposureTrend = "STABLE",
        capabilityResetActive = false,
        activeRestores = 2,
        unresolvedCount = 0,
        activeBaseEvents = listOf(
            BaseEvent(
                id = "BE-001", connectionId = "CN-1001",
                eventType = "ACTIVATED", timestamp = daysAgo(5)
            ),
            BaseEvent(
                id = "BE-002", connectionId = "CN-2903",
                eventType = "RESTORE_STARTED", timestamp = hoursAgo(2)
            ),
            BaseEvent(
                id = "BE-003", connectionId = "CN-4201",
                eventType = "CONNECTIVITY_LOST", timestamp = minutesAgo(15)
            ),
            BaseEvent(
                id = "BE-004", connectionId = "CN-3300",
                eventType = "CHURNED", timestamp = hoursAgo(1)
            )
        ),
        chipValues = mapOf(
            "activeBase" to ChipValue(value = "47"),
            "cycleEarned" to ChipValue(value = "12500.0"),
            "slaStanding" to ChipValue(value = "COMPLIANT", stateColor = "#00B894"),
            "exposureState" to ChipValue(
                value = "ELIGIBLE", stateColor = "#00B894", trend = "STABLE"
            )
        )
    )

    // ---------------------------------------------------------------
    // 4. buildSeedWallet()
    // ---------------------------------------------------------------
    fun buildSeedWallet(): WalletData = WalletData(
        balance = 8750.0,
        pendingSettlement = 2500.0,
        frozen = false,
        frozenReason = null,
        lastWithdrawalAt = daysAgo(7),
        transactions = listOf(
            WalletTransaction(
                id = "WTX-001", date = daysAgo(1), type = "SETTLEMENT",
                amount = 3200.0, description = "Cycle settlement - March W2",
                status = "COMPLETED"
            ),
            WalletTransaction(
                id = "WTX-002", date = daysAgo(2), type = "BONUS",
                amount = 500.0, description = "Quality bonus - 95% SLA compliance",
                status = "COMPLETED"
            ),
            WalletTransaction(
                id = "WTX-003", date = daysAgo(3), type = "INSTALL_HANDLING",
                amount = 350.0, description = "Install handling fee - CN-1001",
                status = "COMPLETED"
            ),
            WalletTransaction(
                id = "WTX-004", date = daysAgo(4), type = "COLLECTION_HANDLING",
                amount = 200.0, description = "Collection handling - NB-1003",
                status = "COMPLETED"
            ),
            WalletTransaction(
                id = "WTX-005", date = daysAgo(5), type = "WITHDRAWAL",
                amount = 5000.0, description = "Bank withdrawal to HDFC ***4321",
                status = "COMPLETED"
            ),
            WalletTransaction(
                id = "WTX-006", date = daysAgo(7), type = "TOP_UP",
                amount = 2000.0, description = "UPI top-up",
                status = "COMPLETED"
            ),
            WalletTransaction(
                id = "WTX-007", date = daysAgo(8), type = "CARRY_FEE",
                amount = 40.0, description = "Carry fee - NB-1005 (20 days overdue)",
                status = "COMPLETED"
            ),
            WalletTransaction(
                id = "WTX-008", date = daysAgo(10), type = "LOSS_RECOVERY",
                amount = 1500.0, description = "Loss recovery - NB-1006 declared lost",
                status = "COMPLETED"
            ),
            WalletTransaction(
                id = "WTX-009", date = daysAgo(12), type = "DEDUCTION",
                amount = 250.0, description = "SLA penalty deduction - missed restore window",
                status = "COMPLETED"
            ),
            WalletTransaction(
                id = "WTX-010", date = now(), type = "SETTLEMENT",
                amount = 2500.0, description = "Pending settlement - March W3",
                status = "PENDING"
            )
        )
    )

    // ---------------------------------------------------------------
    // 5. buildSeedSla()
    // ---------------------------------------------------------------
    fun buildSeedSla(): SlaData = SlaData(
        overallStanding = "COMPLIANT",
        nextEvalDays = 5,
        routing = "FULL",
        bonusStatus = "ACTIVE",
        domains = listOf(
            SlaDomainData(
                id = "installation",
                standing = "COMPLIANT",
                metrics = listOf(
                    SlaMetricData(
                        id = "install_completion_rate",
                        value = 94.5,
                        threshold = 90.0,
                        severeThreshold = 80.0,
                        sampleCount = 20,
                        minSample = 5,
                        trend = "STABLE"
                    ),
                    SlaMetricData(
                        id = "install_avg_days",
                        value = 2.1,
                        threshold = 3.0,
                        severeThreshold = 5.0,
                        sampleCount = 20,
                        minSample = 5,
                        trend = "IMPROVING"
                    )
                ),
                consequences = SlaConsequences(routing = "FULL", bonus = "ACTIVE"),
                hysteresis = SlaHysteresis(requiredCleanWindows = 3, currentCleanWindows = 3)
            ),
            SlaDomainData(
                id = "resolution",
                standing = "COMPLIANT",
                metrics = listOf(
                    SlaMetricData(
                        id = "restore_sla_hit",
                        value = 91.2,
                        threshold = 85.0,
                        severeThreshold = 70.0,
                        sampleCount = 15,
                        minSample = 5,
                        trend = "STABLE"
                    ),
                    SlaMetricData(
                        id = "restore_avg_hours",
                        value = 6.3,
                        threshold = 8.0,
                        severeThreshold = 12.0,
                        sampleCount = 15,
                        minSample = 5,
                        trend = "STABLE"
                    )
                ),
                consequences = SlaConsequences(routing = "FULL", bonus = "ACTIVE"),
                hysteresis = SlaHysteresis(requiredCleanWindows = 3, currentCleanWindows = 2)
            ),
            SlaDomainData(
                id = "stability",
                standing = "COMPLIANT",
                metrics = listOf(
                    SlaMetricData(
                        id = "churn_rate",
                        value = 3.2,
                        threshold = 5.0,
                        severeThreshold = 10.0,
                        sampleCount = 47,
                        minSample = 10,
                        trend = "STABLE"
                    )
                ),
                consequences = SlaConsequences(routing = "FULL", bonus = "ACTIVE"),
                hysteresis = SlaHysteresis(requiredCleanWindows = 3, currentCleanWindows = 3)
            ),
            SlaDomainData(
                id = "experience",
                standing = "COMPLIANT",
                metrics = listOf(
                    SlaMetricData(
                        id = "csat_score",
                        value = 4.2,
                        threshold = 3.5,
                        severeThreshold = 2.5,
                        sampleCount = 30,
                        minSample = 10,
                        trend = "IMPROVING"
                    )
                ),
                consequences = SlaConsequences(routing = "FULL", bonus = "ACTIVE"),
                hysteresis = SlaHysteresis(requiredCleanWindows = 3, currentCleanWindows = 3)
            )
        )
    )

    // ---------------------------------------------------------------
    // 6. buildSeedTechnicians()
    // ---------------------------------------------------------------
    fun buildSeedTechnicians(): List<TechnicianData> = listOf(
        TechnicianData(
            id = "TECH-001", name = "Ajay Patil", phone = "+919876001001",
            band = "A", available = true, joinDate = daysAgo(180)
        ),
        TechnicianData(
            id = "TECH-002", name = "Suresh Kamble", phone = "+919876002002",
            band = "B", available = true, joinDate = daysAgo(120)
        ),
        TechnicianData(
            id = "TECH-003", name = "Ramesh Jadhav", phone = "+919876003003",
            band = "B", available = false, joinDate = daysAgo(90)
        ),
        TechnicianData(
            id = "TECH-004", name = "Vikram Shinde", phone = "+919876004004",
            band = "C", available = true, joinDate = daysAgo(30)
        )
    )

    // ---------------------------------------------------------------
    // 7. buildSeedSupportCases()
    // ---------------------------------------------------------------
    fun buildSeedSupportCases(): List<SupportCaseData> = listOf(
        SupportCaseData(
            id = "SUP-001",
            subject = "NetBox not powering on at customer site",
            status = "OPEN",
            linkedTaskId = "TSK-RST-001",
            createdAt = hoursAgo(3),
            messages = listOf(
                CaseMessage(
                    id = "MSG-001", text = "NetBox at CN-4201 is not powering on. Customer reports no lights on the device.",
                    sender = "csp", senderType = "CSP", timestamp = hoursAgo(3)
                ),
                CaseMessage(
                    id = "MSG-002", text = "Please check the power adapter and try a hard reset. If issue persists, we will arrange a replacement.",
                    sender = "support", senderType = "SUPPORT", timestamp = hoursAgo(2)
                )
            )
        ),
        SupportCaseData(
            id = "SUP-002",
            subject = "Wallet balance discrepancy after settlement",
            status = "IN_PROGRESS",
            linkedTaskId = null,
            createdAt = daysAgo(2),
            messages = listOf(
                CaseMessage(
                    id = "MSG-003", text = "My March W1 settlement shows Rs 2800 but I expected Rs 3200 based on completed installs.",
                    sender = "csp", senderType = "CSP", timestamp = daysAgo(2)
                ),
                CaseMessage(
                    id = "MSG-004", text = "We are reviewing your settlement calculation. Two installs had verification delays that pushed them to the next cycle.",
                    sender = "support", senderType = "SUPPORT", timestamp = daysAgo(1)
                ),
                CaseMessage(
                    id = "MSG-005", text = "Thank you. Can you confirm which two installs were affected?",
                    sender = "csp", senderType = "CSP", timestamp = hoursAgo(6)
                )
            )
        ),
        SupportCaseData(
            id = "SUP-003",
            subject = "Customer dispute regarding installation quality",
            status = "RESOLVED",
            linkedTaskId = "TSK-INS-004",
            createdAt = daysAgo(7),
            messages = listOf(
                CaseMessage(
                    id = "MSG-006", text = "Customer at CN-1001 complains about cable routing. Requesting revisit approval.",
                    sender = "csp", senderType = "CSP", timestamp = daysAgo(7)
                ),
                CaseMessage(
                    id = "MSG-007", text = "Revisit approved. Please schedule within 2 days and capture updated proof photos.",
                    sender = "support", senderType = "SUPPORT", timestamp = daysAgo(6)
                ),
                CaseMessage(
                    id = "MSG-008", text = "Revisit completed. Customer satisfied with corrected routing. Proof uploaded.",
                    sender = "csp", senderType = "CSP", timestamp = daysAgo(4)
                ),
                CaseMessage(
                    id = "MSG-009", text = "Confirmed. Case resolved. Quality review passed.",
                    sender = "support", senderType = "SUPPORT", timestamp = daysAgo(3)
                )
            )
        )
    )

    // ---------------------------------------------------------------
    // 8. buildSeedDeposit()
    // ---------------------------------------------------------------
    fun buildSeedDeposit(): DepositData = DepositData(
        balance = 7500.0,
        ratePerUnit = 1500.0,
        carryFeePerDay = 2.0,
        gracePeriodDays = 15,
        units = listOf(
            NetBoxUnit(
                id = "NB-1001", status = "ACTIVE",
                assignedAt = daysAgo(60), expiresAt = null, carryFeeAccrued = 0.0
            ),
            NetBoxUnit(
                id = "NB-1002", status = "ACTIVE",
                assignedAt = daysAgo(45), expiresAt = null, carryFeeAccrued = 0.0
            ),
            NetBoxUnit(
                id = "NB-1003", status = "ACTIVE",
                assignedAt = daysAgo(30), expiresAt = null, carryFeeAccrued = 0.0
            ),
            NetBoxUnit(
                id = "NB-1004", status = "ACTIVE",
                assignedAt = daysAgo(20), expiresAt = null, carryFeeAccrued = 0.0
            ),
            NetBoxUnit(
                id = "NB-1005", status = "PAST_EXPIRY",
                assignedAt = daysAgo(90), expiresAt = daysAgo(20),
                carryFeeAccrued = 40.0 // 20 days * 2.0/day
            ),
            NetBoxUnit(
                id = "NB-1006", status = "LOST",
                assignedAt = daysAgo(50), expiresAt = daysAgo(12),
                carryFeeAccrued = 0.0 // within 15-day grace
            )
        ),
        transactions = listOf(
            DepositTransaction(
                id = "DTX-001", type = "DEPOSIT", amount = 1500.0,
                netboxId = "NB-1001", date = daysAgo(60),
                description = "Deposit for NB-1001"
            ),
            DepositTransaction(
                id = "DTX-002", type = "DEPOSIT", amount = 1500.0,
                netboxId = "NB-1004", date = daysAgo(20),
                description = "Deposit for NB-1004"
            ),
            DepositTransaction(
                id = "DTX-003", type = "CARRY_FEE", amount = 40.0,
                netboxId = "NB-1005", date = daysAgo(1),
                description = "Carry fee accrued - NB-1005 (20 days past expiry)"
            ),
            DepositTransaction(
                id = "DTX-004", type = "LOSS_RECOVERY", amount = 1500.0,
                netboxId = "NB-1006", date = daysAgo(10),
                description = "Loss recovery deducted - NB-1006 declared lost"
            )
        )
    )

    // ---------------------------------------------------------------
    // 9. buildSeedNotifications()
    // ---------------------------------------------------------------
    fun buildSeedNotifications(): List<AppNotification> = listOf(
        AppNotification(
            id = "NOTIF-001",
            type = "TASK_OFFERED",
            title = "Urgent: Connectivity Loss",
            titleHi = "\u0905\u0930\u094D\u091C\u0947\u0902\u091F: \u0915\u0928\u0947\u0915\u094D\u091F\u093F\u0935\u093F\u091F\u0940 \u0939\u093E\u0928\u093F",
            body = "CN-4201 in Hinjewadi has lost connectivity. SLA deadline in 1 hour. Immediate action required.",
            bodyHi = "CN-4201 \u0939\u093F\u0902\u091C\u0947\u0935\u093E\u0921\u0940 \u092E\u0947\u0902 \u0915\u0928\u0947\u0915\u094D\u091F\u093F\u0935\u093F\u091F\u0940 \u0916\u094B \u0917\u0908 \u0939\u0948\u0964 SLA \u0938\u092E\u092F\u0938\u0940\u092E\u093E 1 \u0918\u0902\u091F\u0947 \u092E\u0947\u0902\u0964 \u0924\u0941\u0930\u0902\u0924 \u0915\u093E\u0930\u094D\u0930\u0935\u093E\u0908 \u0906\u0935\u0936\u094D\u092F\u0915\u0964",
            taskId = "TSK-RST-001",
            timestamp = minutesAgo(15),
            dismissed = false
        ),
        AppNotification(
            id = "NOTIF-002",
            type = "TASK_OFFERED",
            title = "New Install Task",
            titleHi = "\u0928\u092F\u093E \u0907\u0902\u0938\u094D\u091F\u0949\u0932 \u0915\u093E\u0930\u094D\u092F",
            body = "New install offer for CN-2847 in Sector 12. Offer expires in 30 minutes.",
            bodyHi = "CN-2847 \u0915\u0947 \u0932\u093F\u090F \u0928\u092F\u093E \u0907\u0902\u0938\u094D\u091F\u0949\u0932 \u0911\u092B\u0930, \u0938\u0947\u0915\u094D\u091F\u0930 12\u0964 \u0911\u092B\u0930 30 \u092E\u093F\u0928\u091F \u092E\u0947\u0902 \u0938\u092E\u093E\u092A\u094D\u0924 \u0939\u094B\u0917\u093E\u0964",
            taskId = "TSK-INS-001",
            timestamp = minutesAgo(5),
            dismissed = false
        ),
        AppNotification(
            id = "NOTIF-003",
            type = "WALLET_UPDATE",
            title = "Settlement Credited",
            titleHi = "\u0928\u093F\u092A\u091F\u093E\u0928 \u091C\u092E\u093E",
            body = "Rs 3,200 has been credited to your wallet as March W2 settlement.",
            bodyHi = "\u0906\u092A\u0915\u0947 \u0935\u0949\u0932\u0947\u091F \u092E\u0947\u0902 \u092E\u093E\u0930\u094D\u091A W2 \u0928\u093F\u092A\u091F\u093E\u0928 \u0915\u0947 \u0930\u0942\u092A \u092E\u0947\u0902 \u20B93,200 \u091C\u092E\u093E \u0915\u093F\u092F\u093E \u0917\u092F\u093E \u0939\u0948\u0964",
            taskId = null,
            timestamp = daysAgo(1),
            dismissed = true
        )
    )

    // ---------------------------------------------------------------
    // 10. buildSeedRechargeCustomers()
    // ---------------------------------------------------------------
    fun buildSeedRechargeCustomers(): List<RechargeCustomer> = listOf(
        RechargeCustomer(
            id = "RCH-C001", name = "Priya Sharma",
            connectionId = "CN-1001", deviceId = "DEV-4501",
            speed = "100 Mbps", username = "priya.sharma@isp",
            phoneLast5 = "56789", shareAmount = 300.0
        ),
        RechargeCustomer(
            id = "RCH-C002", name = "Rajesh Kumar",
            connectionId = "CN-1045", deviceId = "DEV-4502",
            speed = "100 Mbps", username = "rajesh.kumar@isp",
            phoneLast5 = "12345", shareAmount = 300.0
        ),
        RechargeCustomer(
            id = "RCH-C003", name = "Sunita Patil",
            connectionId = "CN-1102", deviceId = "DEV-4503",
            speed = "100 Mbps", username = "sunita.patil@isp",
            phoneLast5 = "67890", shareAmount = 300.0
        ),
        RechargeCustomer(
            id = "RCH-C004", name = "Amit Deshmukh",
            connectionId = "CN-1200", deviceId = "DEV-4504",
            speed = "100 Mbps", username = "amit.d@isp",
            phoneLast5 = "34567", shareAmount = 300.0
        ),
        RechargeCustomer(
            id = "RCH-C005", name = "Kavita Joshi",
            connectionId = "CN-1305", deviceId = "DEV-4505",
            speed = "100 Mbps", username = "kavita.j@isp",
            phoneLast5 = "89012", shareAmount = 300.0
        )
    )

    // ---------------------------------------------------------------
    // 11. buildSeedISPPortals()
    // ---------------------------------------------------------------
    fun buildSeedISPPortals(): List<ISPPortal> = listOf(
        ISPPortal(
            id = "PORTAL-001",
            name = "Hathway ISP Portal",
            url = "https://portal.hathway.com",
            username = "csp_mh_1001",
            password = "portal@123",
            verified = true,
            createdAt = daysAgo(30)
        )
    )
}
