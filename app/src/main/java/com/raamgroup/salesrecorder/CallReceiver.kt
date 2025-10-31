package com.raamgroup.salesrecorder

import android.content.BroadcastReceiver
import android.content.Context
import android.content.Intent
import android.telephony.TelephonyManager
import android.util.Log

class CallReceiver : BroadcastReceiver() {

    companion object {
        const val ACTION_CALL_ENDED = "com.raamgroup.salesrecorder.CALL_ENDED"
        private const val TAG = "CallReceiver"
        private var callStartTime: Long = 0
        private var savedNumber: String? = null
        private var isIncoming: Boolean = false
        private var wasOffHook = false
    }

    override fun onReceive(context: Context, intent: Intent) {
        when (intent.action) {
            Intent.ACTION_NEW_OUTGOING_CALL -> {
                savedNumber = intent.getStringExtra(Intent.EXTRA_PHONE_NUMBER)
                isIncoming = false
                callStartTime = System.currentTimeMillis()
                wasOffHook = false
                Log.d(TAG, "Outgoing call initiated to: $savedNumber")
            }
            TelephonyManager.ACTION_PHONE_STATE_CHANGED -> {
                val stateStr = intent.getStringExtra(TelephonyManager.EXTRA_STATE)
                when (stateStr) {
                    TelephonyManager.EXTRA_STATE_RINGING -> {
                        isIncoming = true
                        savedNumber = intent.getStringExtra(TelephonyManager.EXTRA_INCOMING_NUMBER)
                        callStartTime = System.currentTimeMillis()
                        wasOffHook = false
                        Log.d(TAG, "Incoming call ringing from: $savedNumber")
                    }
                    TelephonyManager.EXTRA_STATE_OFFHOOK -> {
                        if (callStartTime == 0L && !isIncoming) {
                            callStartTime = System.currentTimeMillis()
                        }
                        wasOffHook = true
                        Log.d(TAG, "Call is active (Off-hook)")
                    }
                    TelephonyManager.EXTRA_STATE_IDLE -> {
                        if (callStartTime > 0) {
                            val callEndTime = System.currentTimeMillis()
                            val durationSeconds = (callEndTime - callStartTime) / 1000
                            val callType = when {
                                !wasOffHook && !isIncoming -> "MISSED_OUTGOING"
                                !wasOffHook && isIncoming -> "MISSED_INCOMING"
                                isIncoming -> "INCOMING"
                                else -> "OUTGOING"
                            }
                            val finalNumber = savedNumber ?: "Unknown"

                            // --- THIS IS THE NEW LOGIC ---
                            // Get the user's name from SharedPreferences
                            val sharedPrefs = context.getSharedPreferences("AppPrefs", Context.MODE_PRIVATE)
                            val userName = sharedPrefs.getString("USER_NAME", "Unknown User") ?: "Unknown User"

                            Log.d(TAG, "Call ended. User: $userName, Type: $callType, Number: $finalNumber, Duration: $durationSeconds seconds.")

                            val broadcastIntent = Intent(ACTION_CALL_ENDED).apply {
                                putExtra("userName", userName) // Add username to broadcast
                                putExtra("phoneNumber", finalNumber)
                                putExtra("type", callType)
                                putExtra("duration", durationSeconds)
                            }
                            context.sendBroadcast(broadcastIntent)
                        }
                        // Reset state for the next call
                        callStartTime = 0
                        savedNumber = null
                        isIncoming = false
                        wasOffHook = false
                    }
                }
            }
        }
    }
}