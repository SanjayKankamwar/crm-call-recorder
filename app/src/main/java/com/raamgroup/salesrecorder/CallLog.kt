package com.raamgroup.salesrecorder

data class CallLog(
    val id: String = System.currentTimeMillis().toString(),
    val userName: String, // <-- THIS LINE IS ADDED
    val phoneNumber: String?,
    val type: String,
    val callDurationSeconds: Long,
    val timestamp: Long = System.currentTimeMillis(),
    var localFilePath: String? = null,
    var uploadUrl: String? = null,
    var uploadStatus: String = "Not Recorded"
)