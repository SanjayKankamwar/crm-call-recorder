package com.raamgroup.salesrecorder

import android.content.Context
import android.util.Log
import androidx.room.Room
import io.github.jan.supabase.createSupabaseClient
import io.github.jan.supabase.postgrest.Postgrest
import io.github.jan.supabase.storage.Storage
import io.github.jan.supabase.storage.storage
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import java.io.File
import java.util.UUID

class CallLogManager(private val context: Context) {

    companion object {
        private const val TAG = "CallLogManager"
        private const val RECORDINGS_DIR = "call_recordings"
        private const val SUPABASE_BUCKET = "recordings"  // Corrected bucket name
        private const val DATABASE_NAME = "call_logs_db"
    }

    private val database by lazy {
        Room.databaseBuilder(context, CallLogDatabase::class.java, DATABASE_NAME)
            .build()
    }

    private val supabase = createSupabaseClient(
        supabaseUrl = "https://upiwtpqqdghtzyqhrpaj.supabase.co",
        supabaseKey = "eyJhbGciOiJIUzI1NiIsInR5cCI6IkpXVCJ9.eyJpc3MiOiJzdXBhYmFzZSIsInJlZiI6InVwaXd0cHFxZGdodHp5cWhycGFqIiwicm9sZSI6ImFub24iLCJpYXQiOjE3NjE5MDY4NTQsImV4cCI6MjA3NzQ4Mjg1NH0.JpMh_NTANeoHf4N3ULydndrZfBzyfIO_z_ujcrRUSxw"
    ) {
        install(Postgrest)
        install(Storage)
    }

    // 1. Save dummy/recorded call log locally
    suspend fun saveCallLogLocally(
        userName: String,
        phoneNumber: String,
        type: String,
        callDurationSeconds: Int,
        recordingFile: File
    ): CallLogEntity {
        return withContext(Dispatchers.IO) {
            val callLogId = UUID.randomUUID().toString()
            val fileName = "recording_${callLogId}.wav"
            val localDir = File(context.cacheDir, RECORDINGS_DIR)

            if (!localDir.exists()) {
                localDir.mkdirs()
            }

            // Copy recording to local storage
            val localFile = File(localDir, fileName)
            recordingFile.copyTo(localFile, overwrite = true)

            val currentTime = System.currentTimeMillis()
            val callLogEntity = CallLogEntity(
                id = callLogId,
                userName = userName,
                phoneNumber = phoneNumber,
                type = type,
                callDurationSeconds = callDurationSeconds,
                recordingFilePath = localFile.absolutePath,
                recordingFileName = fileName,
                fileSize = localFile.length(),
                timestamp = currentTime,
                syncStatus = "PENDING",
                createdAt = currentTime
            )

            // Save to local Room database
            database.callLogDao().insertCallLog(callLogEntity)
            Log.d(TAG, "Call log saved locally: $callLogId")

            callLogEntity
        }
    }

    // 2. Upload to Supabase and update DB
    suspend fun syncCallLogToSupabase(callLogEntity: CallLogEntity): Boolean {
        return withContext(Dispatchers.IO) {
            try {
                val localFile = File(callLogEntity.recordingFilePath)

                if (!localFile.exists()) {
                    Log.e(TAG, "Local file not found: ${callLogEntity.recordingFilePath}")
                    updateSyncStatus(callLogEntity.id, "FAILED", callLogEntity.attempts + 1)
                    return@withContext false
                }

                // Upload to Supabase Storage
                val bucket = supabase.storage.from(SUPABASE_BUCKET)
                val fileBytes = localFile.readBytes()
                val uploadPath = "${callLogEntity.phoneNumber}/${callLogEntity.recordingFileName}"

                bucket.upload(uploadPath, fileBytes, upsert = false)

                // Get public URL
                val publicUrl = bucket.publicUrl(uploadPath)

                Log.d(TAG, "File uploaded successfully to: $publicUrl")

                // Update database with sync status and URL
                val updatedEntity = callLogEntity.copy(
                    syncStatus = "SYNCED",
                    supabaseFileUrl = publicUrl,
                    uploadedAt = System.currentTimeMillis()
                )
                database.callLogDao().updateCallLog(updatedEntity)

                // Delete local file after successful upload
                deleteLocalFile(callLogEntity.recordingFilePath)

                return@withContext true

            } catch (e: Exception) {
                Log.e(TAG, "Error syncing call log: ${e.message}")
                updateSyncStatus(callLogEntity.id, "FAILED", callLogEntity.attempts + 1)
                return@withContext false
            }
        }
    }

    // 3. Delete local file after sync
    private suspend fun deleteLocalFile(filePath: String) {
        withContext(Dispatchers.IO) {
            try {
                val file = File(filePath)
                if (file.exists()) {
                    file.delete()
                    Log.d(TAG, "Local file deleted: $filePath")
                }
            } catch (e: Exception) {
                Log.e(TAG, "Error deleting local file: ${e.message}")
            }
        }
    }

    // 4. Get all call logs for UI
    suspend fun getAllCallLogs(): List<CallLogEntity> {
        return withContext(Dispatchers.IO) {
            database.callLogDao().getAllCallLogs()
        }
    }

    // 5. Get synced call logs with URLs
    suspend fun getSyncedCallLogs(): List<CallLogEntity> {
        return withContext(Dispatchers.IO) {
            database.callLogDao().getCallLogsByStatus("SYNCED")
        }
    }

    // 6. Retry failed uploads
    suspend fun retrySyncFailed() {
        withContext(Dispatchers.IO) {
            val failedLogs = database.callLogDao().getCallLogsByStatus("FAILED")
            failedLogs.forEach { log ->
                if (log.attempts < 3) {
                    syncCallLogToSupabase(log)
                }
            }
        }
    }

    private suspend fun updateSyncStatus(id: String, status: String, attempts: Int) {
        withContext(Dispatchers.IO) {
            database.callLogDao().updateSyncStatus(id, status, attempts)
        }
    }
}