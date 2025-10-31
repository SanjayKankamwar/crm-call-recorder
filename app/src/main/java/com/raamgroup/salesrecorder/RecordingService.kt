package com.raamgroup.salesrecorder

import android.app.Notification
import android.app.NotificationChannel
import android.app.NotificationManager
import android.app.Service
import android.content.Intent
import android.media.MediaRecorder
import android.os.Build
import android.os.IBinder
import android.util.Log
import androidx.core.app.NotificationCompat
import java.io.File
import java.io.IOException
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale
import android.os.Environment

class RecordingService : Service() {

    private var mediaRecorder: MediaRecorder? = null
    private var isRecording = false
    private var outputFilePath: String = ""
    private val uploader = FirebaseUploader()

    companion object {
        // --- THIS IS THE CRITICAL ADDITION ---
        // This variable will hold the path to the most recently saved file,
        // so other parts of our app can access it.
        var lastRecordingPath: String? = null

        const val NOTIFICATION_CHANNEL_ID = "RecordingServiceChannel"
        const val NOTIFICATION_ID = 1
        const val TAG = "RecordingService"
    }

    override fun onStartCommand(intent: Intent?, flags: Int, startId: Int): Int {
        startForegroundService()
        startRecording()
        return START_STICKY
    }

    private fun startRecording() {
        if (isRecording) return

        val fileName = "Call_Recording_${SimpleDateFormat("yyyyMMdd_HHmmss", Locale.getDefault()).format(Date())}.m4a"
        val storageDir = Environment.getExternalStoragePublicDirectory(Environment.DIRECTORY_DOWNLOADS)
        outputFilePath = File(storageDir, fileName).absolutePath

        // --- ADD THIS LINE ---
        // We update the static variable with the new path.
        lastRecordingPath = outputFilePath

        mediaRecorder = if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.S) {
            MediaRecorder(this)
        } else {
            MediaRecorder()
        }

        mediaRecorder?.apply {
            setAudioSource(MediaRecorder.AudioSource.VOICE_COMMUNICATION)
            setOutputFormat(MediaRecorder.OutputFormat.MPEG_4)
            setAudioEncoder(MediaRecorder.AudioEncoder.AAC)
            setOutputFile(outputFilePath)

            try {
                prepare()
                start()
                isRecording = true
                Log.d(TAG, "Recording started successfully.")
            } catch (e: IOException) {
                Log.e(TAG, "Recording failed to start: ${e.message}")
                isRecording = false
            }
        }
    }

    private fun stopRecording() {
        if (!isRecording) return

        mediaRecorder?.apply {
            try {
                stop()
                release()
            } catch (e: Exception) {
                Log.e(TAG, "Error stopping recorder: ${e.message}")
            }
        }
        mediaRecorder = null
        isRecording = false
        Log.d(TAG, "Recording stopped. File saved at: $outputFilePath")
        uploader.uploadFile(outputFilePath) { downloadUrl ->
            if (downloadUrl != null) {
                Log.d(TAG, "Upload complete. URL: $downloadUrl")
            } else {
                Log.e(TAG, "Upload failed from RecordingService.")
            }
        }
    }

    private fun startForegroundService() {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            val channel = NotificationChannel(
                NOTIFICATION_CHANNEL_ID,
                "Call Recording Service",
                NotificationManager.IMPORTANCE_DEFAULT
            )
            val manager = getSystemService(NotificationManager::class.java)
            manager.createNotificationChannel(channel)
        }

        val notification: Notification = NotificationCompat.Builder(this, NOTIFICATION_CHANNEL_ID)
            .setContentTitle("Call Recorder Active")
            .setContentText("Monitoring calls for recording.")
            .setSmallIcon(android.R.drawable.ic_dialog_info)
            .build()

        startForeground(NOTIFICATION_ID, notification)
    }

    override fun onDestroy() {
        stopRecording()
        super.onDestroy()
    }

    override fun onBind(intent: Intent?): IBinder? {
        return null
    }
}
