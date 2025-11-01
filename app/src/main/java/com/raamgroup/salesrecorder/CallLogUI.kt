package com.raamgroup.salesrecorder

import java.time.LocalDateTime

data class CallLogUI(
    val id: Long,
    val createdAt: LocalDateTime,
    val userName: String,
    val phoneNumber: String?,
    val type: String,
    val callDurationSeconds: Long,
    val uploadUrl: String,
    val isPlaying: Boolean = false
)
