package com.raamgroup.salesrecorder

import android.util.Log
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import org.json.JSONObject
import java.io.OutputStreamWriter
import java.net.HttpURLConnection
import java.net.URL

class NetworkClient {

    companion object {
        private const val TAG = "NetworkClient"
        // THIS IS YOUR LIVE API URL
        private const val API_URL = "https://sales-recorder-backend.vercel.app/api/save-log"
    }

    suspend fun sendCallLog(log: CallLog) {
        withContext(Dispatchers.IO) { // Perform network operation on a background thread
            var connection: HttpURLConnection? = null
            try {
                val url = URL(API_URL)
                connection = url.openConnection() as HttpURLConnection
                connection.requestMethod = "POST"
                connection.setRequestProperty("Content-Type", "application/json; charset=utf-8")
                connection.doOutput = true

                // Create the JSON object to send
                val jsonObject = JSONObject()
                jsonObject.put("userName", log.userName)
                jsonObject.put("phoneNumber", log.phoneNumber)
                jsonObject.put("type", log.type)
                jsonObject.put("callDurationSeconds", log.callDurationSeconds)
                jsonObject.put("uploadUrl", log.uploadUrl)

                val outputStreamWriter = OutputStreamWriter(connection.outputStream)
                outputStreamWriter.write(jsonObject.toString())
                outputStreamWriter.flush()

                val responseCode = connection.responseCode
                if (responseCode == HttpURLConnection.HTTP_OK) {
                    Log.d(TAG, "Successfully sent call log to backend.")
                } else {
                    Log.e(TAG, "Failed to send log. Response code: $responseCode")
                }

            } catch (e: Exception) {
                Log.e(TAG, "Error sending call log: ${e.message}")
            } finally {
                connection?.disconnect()
            }
        }
    }
}
