package com.raamgroup.salesrecorder

import androidx.room.Entity
import androidx.room.PrimaryKey

@Entity(tableName = "call_logs")
data class CallLogEntity(
    @PrimaryKey val id: String,
    val userName: String,
    val phoneNumber: String,
    val type: String, // INCOMING, OUTGOING, MISSED
    val callDurationSeconds: Int,
    val recordingFilePath: String,
    val recordingFileName: String,
    val fileSize: Long,
    val timestamp: Long,
    val syncStatus: String, // PENDING, SYNCED, FAILED
    val supabaseFileUrl: String? = null,
    val createdAt: Long,
    val uploadedAt: Long? = null,
    val attempts: Int = 0
)
