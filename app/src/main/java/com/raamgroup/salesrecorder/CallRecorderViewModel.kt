package com.raamgroup.salesrecorder

import android.content.Context
import android.util.Log
import androidx.lifecycle.MutableLiveData
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import kotlinx.coroutines.launch
import java.io.File

class CallRecorderViewModel(private val context: Context) : ViewModel() {

    private val callLogManager = CallLogManager(context)
    val callLogs = MutableLiveData<List<CallLogEntity>>()
    val syncedLogs = MutableLiveData<List<CallLogEntity>>()

    // When virtual dialer creates a call
    fun onCallRecorded(
        userName: String,
        phoneNumber: String,
        type: String,
        duration: Int,
        recordingFile: File
    ) {
        viewModelScope.launch {
            // Step 1: Save locally
            val callLog = callLogManager.saveCallLogLocally(
                userName, phoneNumber, type, duration, recordingFile
            )

            // Step 2: Sync to Supabase
            val synced = callLogManager.syncCallLogToSupabase(callLog)

            if (synced) {
                Log.d("CallRecorder", "Call log synced successfully")
            }

            // Step 3: Refresh UI
            refreshCallLogs()
        }
    }

    fun refreshCallLogs() {
        viewModelScope.launch {
            callLogs.value = callLogManager.getAllCallLogs()
            syncedLogs.value = callLogManager.getSyncedCallLogs()
        }
    }
}