package com.wiom.csp.data.remote.dto

import kotlinx.serialization.Serializable

@Serializable
data class SendOtpRequest(
    val appName: String,
    val mobile: String
)

@Serializable
data class TaskActionRequest(
    val taskId: String,
    val action: String,
    val payload: Map<String, String> = emptyMap()
)

@Serializable
data class WalletActionRequest(
    val type: String,
    val amount: Double,
    val method: String? = null
)

@Serializable
data class CreateCaseRequest(
    val subject: String,
    val description: String,
    val linkedTaskId: String? = null
)

@Serializable
data class CaseReplyRequest(
    val text: String
)

@Serializable
data class AddTechRequest(
    val name: String,
    val phone: String
)

@Serializable
data class DismissRequest(
    val id: String
)

@Serializable
data class ThemeRequest(
    val theme: String
)
