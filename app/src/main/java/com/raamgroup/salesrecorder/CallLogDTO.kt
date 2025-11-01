package com.raamgroup.salesrecorder

import kotlinx.serialization.Serializable

@Serializable
data class CallLogDTO(
    val id: Long,
    val created_at: String,
    val user_name: String?,
    val phone_number: String?,
    val type: String?,
    val call_duration_seconds: Long?,
    val upload_url: String?
)
