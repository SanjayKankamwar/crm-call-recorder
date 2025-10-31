package com.raamgroup.salesrecorder

import android.Manifest
import android.content.BroadcastReceiver
import android.content.ComponentName
import android.content.Context
import android.content.Intent
import android.content.IntentFilter
import android.net.Uri
import android.os.Build
import android.os.Bundle
import android.provider.Settings
import android.util.Log
import android.widget.Button
import android.widget.Toast
import androidx.activity.result.contract.ActivityResultContracts
import androidx.activity.viewModels
import androidx.appcompat.app.AppCompatActivity
import androidx.lifecycle.Observer
import androidx.recyclerview.widget.LinearLayoutManager
import androidx.recyclerview.widget.RecyclerView
import java.io.File

class MainActivity : AppCompatActivity() {

    private lateinit var callLogRecyclerView: RecyclerView
    private lateinit var callLogAdapter: CallLogAdapter
    private lateinit var createDummyLogButton: Button

    private val callRecorderViewModel: CallRecorderViewModel by viewModels { 
        CallRecorderViewModelFactory(applicationContext) 
    }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)

        val sharedPrefs = getSharedPreferences("AppPrefs", Context.MODE_PRIVATE)
        val userName = sharedPrefs.getString("USER_NAME", null)
        if (userName == null) {
            startActivity(Intent(this, LoginActivity::class.java))
            finish()
            return
        }

        setContentView(R.layout.activity_main)

        setupUI()
        requestAllPermissions()
        registerCallEndedReceiver()

        callRecorderViewModel.callLogs.observe(this, Observer { logs ->
            callLogAdapter.updateLogs(logs)
        })

        callRecorderViewModel.refreshCallLogs()
    }

    private fun setupUI() {
        callLogRecyclerView = findViewById(R.id.callLogRecyclerView)
        callLogRecyclerView.layoutManager = LinearLayoutManager(this)
        callLogAdapter = CallLogAdapter(mutableListOf()) { callLog -> playRecording(callLog) }
        callLogRecyclerView.adapter = callLogAdapter

        createDummyLogButton = findViewById(R.id.createDummyLogButton)
        createDummyLogButton.setOnClickListener {
            createDummyCallLog()
        }
    }

    private fun createDummyCallLog() {
        val dummyFile = File.createTempFile("dummy_recording", ".wav", cacheDir)
        dummyFile.writeBytes("dummy audio data".toByteArray())

        callRecorderViewModel.onCallRecorded(
            userName = "Dummy User",
            phoneNumber = "1234567890",
            type = "OUTGOING",
            duration = 60,
            recordingFile = dummyFile
        )
    }

    private fun playRecording(callLog: CallLogEntity) {
        if (callLog.syncStatus == "SYNCED" && callLog.supabaseFileUrl != null) {
            try {
                val intent = Intent(Intent.ACTION_VIEW, Uri.parse(callLog.supabaseFileUrl))
                startActivity(intent)
            } catch (e: Exception) {
                Toast.makeText(this, "Could not open recording link.", Toast.LENGTH_SHORT).show()
                Log.e("MainActivity", "Error opening URL: ${callLog.supabaseFileUrl}", e)
            }
        } else {
            Toast.makeText(this, "Recording is not yet synced or has failed to upload.", Toast.LENGTH_SHORT).show()
        }
    }

    private val permissionsToRequest = arrayOf(
        Manifest.permission.RECORD_AUDIO,
        Manifest.permission.READ_PHONE_STATE,
        Manifest.permission.READ_CALL_LOG,
        Manifest.permission.PROCESS_OUTGOING_CALLS,
        Manifest.permission.INTERNET
    )

    private val requestPermissionLauncher =
        registerForActivityResult(ActivityResultContracts.RequestMultiplePermissions()) { permissions ->
            val allGranted = permissions.entries.all { it.value }
            if (allGranted) {
                Log.d("MainActivity", "All permissions granted.")
                checkAndPromptForAccessibility()
            } else {
                Toast.makeText(this, "Some permissions were denied. App may not function.", Toast.LENGTH_LONG).show()
            }
        }

    private fun requestAllPermissions() {
        requestPermissionLauncher.launch(permissionsToRequest)
    }

    private fun checkAndPromptForAccessibility() {
        val accessibilityService = ComponentName(this, CallDetectionService::class.java)
        val enabledServices = Settings.Secure.getString(contentResolver, Settings.Secure.ENABLED_ACCESSIBILITY_SERVICES)
        if (enabledServices == null || !enabledServices.contains(accessibilityService.flattenToString())) {
            Toast.makeText(this, "Please enable the \'SalesCallRecorder\' service in Accessibility.", Toast.LENGTH_LONG).show()
            startActivity(Intent(Settings.ACTION_ACCESSIBILITY_SETTINGS))
        }
    }

    private val callEndedReceiver = object : BroadcastReceiver() {
        override fun onReceive(context: Context?, intent: Intent?) {
            Log.d("MainActivity", "Received call ended broadcast!")
            intent?.let {
                val userName = it.getStringExtra("userName") ?: "Unknown User"
                val phoneNumber = it.getStringExtra("phoneNumber") ?: "Unknown"
                val type = it.getStringExtra("type") ?: "UNKNOWN"
                val duration = it.getLongExtra("duration", 0)
                val recordingPath = RecordingService.lastRecordingPath

                if (recordingPath != null) {
                    callRecorderViewModel.onCallRecorded(
                        userName,
                        phoneNumber,
                        type,
                        duration.toInt(),
                        File(recordingPath)
                    )
                } else {
                    // Handle case where there is no recording
                }
            }
        }
    }

    private fun registerCallEndedReceiver() {
        val intentFilter = IntentFilter(CallReceiver.ACTION_CALL_ENDED)
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
            registerReceiver(callEndedReceiver, intentFilter, Context.RECEIVER_EXPORTED)
        } else {
            registerReceiver(callEndedReceiver, intentFilter)
        }
    }

    override fun onDestroy() {
        super.onDestroy()
        unregisterReceiver(callEndedReceiver)
    }
}