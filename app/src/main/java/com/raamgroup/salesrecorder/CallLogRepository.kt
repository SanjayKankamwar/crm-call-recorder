package com.raamgroup.salesrecorder

import android.util.Log
import io.github.jan.supabase.SupabaseClient
import io.github.jan.supabase.postgrest.from
import io.github.jan.supabase.realtime.PostgresAction
import io.github.jan.supabase.realtime.channel
import io.github.jan.supabase.realtime.postgresChangeFlow
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.asStateFlow
import java.time.LocalDateTime

class CallLogRepository(private val supabase: SupabaseClient) {

    companion object {
        private const val TAG = "CallLogRepository"
        private const val TABLE = "call_logs"
        private const val SCHEMA = "public"
    }

    private val _callLogs = MutableStateFlow<List<CallLogUI>>(emptyList())
    val callLogs = _callLogs.asStateFlow()

    suspend fun subscribeToCallLogs() {
        try {
            val channel = supabase.channel("public:$TABLE")

            // Fixed: Include schema and table parameters
            channel.postgresChangeFlow<PostgresAction>(schema = SCHEMA) {
                table = TABLE
                filter = "event=*"
            }.collect { action ->
                when (action) {
                    is PostgresAction.Insert -> {
                        Log.d(TAG, "New call log inserted")
                        try {
                            val newLog = parseCallLogDTO(action.record)
                            if (newLog != null) {
                                addCallLog(newLog)
                            }
                        } catch (e: Exception) {
                            Log.e(TAG, "Error parsing insert: ${e.message}", e)
                        }
                    }
                    is PostgresAction.Update -> {
                        Log.d(TAG, "Call log updated")
                        try {
                            val updatedLog = parseCallLogDTO(action.record)
                            if (updatedLog != null) {
                                updateCallLog(updatedLog)
                            }
                        } catch (e: Exception) {
                            Log.e(TAG, "Error parsing update: ${e.message}", e)
                        }
                    }
                    is PostgresAction.Delete -> {
                        Log.d(TAG, "Call log deleted")
                        try {
                            val deletedLog = parseCallLogDTO(action.oldRecord)
                            if (deletedLog != null) {
                                removeCallLog(deletedLog.id)
                            }
                        } catch (e: Exception) {
                            Log.e(TAG, "Error parsing delete: ${e.message}", e)
                        }
                    }
                    is PostgresAction.Select -> {
                        Log.d(TAG, "Select action received")
                    }
                }
            }

            channel.subscribe(blockUntilSubscribed = true)
            Log.d(TAG, "Successfully subscribed to $TABLE")

        } catch (e: Exception) {
            Log.e(TAG, "Error subscribing to realtime: ${e.message}", e)
        }
    }

    private fun parseCallLogDTO(data: Any?): CallLogDTO? {
        return try {
            when (data) {
                is Map<*, *> -> {
                    CallLogDTO(
                        id = (data["id"] as? Number)?.toLong() ?: 0,
                        created_at = data["created_at"] as? String ?: "",
                        user_name = data["user_name"] as? String,
                        phone_number = data["phone_number"] as? String,
                        type = data["type"] as? String,
                        call_duration_seconds = (data["call_duration_seconds"] as? Number)?.toLong(),
                        upload_url = data["upload_url"] as? String
                    )
                }
                else -> null
            }
        } catch (e: Exception) {
            Log.e(TAG, "Failed to parse CallLogDTO: ${e.message}", e)
            null
        }
    }

    private fun addCallLog(dto: CallLogDTO) {
        val uiLog = dtoToUI(dto)
        val updatedList = (_callLogs.value + uiLog).sortedByDescending { it.createdAt }
        _callLogs.value = updatedList
        Log.d(TAG, "Call log added. Total: ${updatedList.size}")
    }

    private fun updateCallLog(dto: CallLogDTO) {
        val uiLog = dtoToUI(dto)
        val updatedList = _callLogs.value.map {
            if (it.id == uiLog.id) uiLog else it
        }
        _callLogs.value = updatedList
        Log.d(TAG, "Call log updated: ${uiLog.id}")
    }

    private fun removeCallLog(id: Long) {
        _callLogs.value = _callLogs.value.filter { it.id != id }
        Log.d(TAG, "Call log removed: $id")
    }

    private fun dtoToUI(dto: CallLogDTO): CallLogUI {
        return try {
            val createdAt = try {
                LocalDateTime.parse(
                    dto.created_at.replace("Z", "").substring(0, 19)
                )
            } catch (e: Exception) {
                Log.e(TAG, "Error parsing date: ${e.message}")
                LocalDateTime.now()
            }

            CallLogUI(
                id = dto.id,
                createdAt = createdAt,
                userName = dto.user_name ?: "Unknown",
                phoneNumber = dto.phone_number ?: "Unknown",
                type = dto.type ?: "UNKNOWN",
                callDurationSeconds = dto.call_duration_seconds ?: 0,
                uploadUrl = dto.upload_url ?: ""
            )
        } catch (e: Exception) {
            Log.e(TAG, "Error converting DTO to UI: ${e.message}", e)
            CallLogUI(
                id = 0,
                createdAt = LocalDateTime.now(),
                userName = "Error",
                phoneNumber = "N/A",
                type = "ERROR",
                callDurationSeconds = 0,
                uploadUrl = ""
            )
        }
    }

    suspend fun fetchInitialCallLogs() {
        try {
            val response = supabase.from(TABLE)
                .select()
                .decodeList<CallLogDTO>()

            _callLogs.value = response.map { dtoToUI(it) }
                .sortedByDescending { it.createdAt }

            Log.d(TAG, "Fetched ${response.size} initial call logs")
        } catch (e: Exception) {
            Log.e(TAG, "Error fetching initial logs: ${e.message}", e)
        }
    }
}