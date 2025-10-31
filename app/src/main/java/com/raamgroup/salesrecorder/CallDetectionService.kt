package com.raamgroup.salesrecorder

import android.accessibilityservice.AccessibilityService
import android.content.Intent
import android.util.Log
import android.view.accessibility.AccessibilityEvent

class CallDetectionService : AccessibilityService() {

    companion object {
        const val TAG = "CallDetectionService"
    }

    private var isCallActive = false

    override fun onAccessibilityEvent(event: AccessibilityEvent?) {
        // We only care about events where the window on the screen changes.
        if (event?.eventType == AccessibilityEvent.TYPE_WINDOW_STATE_CHANGED) {

            val className = event.className?.toString() ?: ""
            // Log every event to make debugging on different phones easier.
            Log.d(TAG, "Window state changed: $className")

            // --- IMPROVEMENT #1: Make the check more generic and not case-sensitive ---
            val isInCall = className.lowercase().contains("incall")

            if (isInCall && !isCallActive) {
                // The call screen has appeared, and we weren't previously in a call.
                isCallActive = true
                Log.d(TAG, "CALL_STATE_ACTIVE: Starting RecordingService.")
                startService(Intent(this, RecordingService::class.java))
            } else if (!isInCall && isCallActive) {
                // The call screen has disappeared, and we WERE previously in a call.
                // This is a simple but effective way to detect the end of a call for our prototype.
                isCallActive = false
                Log.d(TAG, "CALL_STATE_IDLE: Stopping RecordingService because '$className' appeared.")
                stopService(Intent(this, RecordingService::class.java))
            }
        }
    }

    override fun onInterrupt() {
        Log.d(TAG, "Service was interrupted.")
        // If the service is interrupted, make sure we stop any active recording.
        if (isCallActive) {
            stopService(Intent(this, RecordingService::class.java))
            isCallActive = false
        }
    }
}