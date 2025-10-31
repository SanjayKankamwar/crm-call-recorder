package com.raamgroup.salesrecorder

import android.net.Uri
import android.util.Log
import com.google.firebase.storage.FirebaseStorage
import java.io.File

class FirebaseUploader {

    companion object {
        const val TAG = "FirebaseUploader"
    }

    // --- THIS IS THE FIX: The function is updated to take a callback ---
    fun uploadFile(filePath: String, onComplete: (downloadUrl: String?) -> Unit) {
        val storageRef = FirebaseStorage.getInstance().reference
        val fileToUpload = Uri.fromFile(File(filePath))
        val recordingRef = storageRef.child("recordings/${fileToUpload.lastPathSegment}")

        Log.d(TAG, "Starting upload for: $filePath")
        val uploadTask = recordingRef.putFile(fileToUpload)

        uploadTask.addOnFailureListener { exception ->
            Log.e(TAG, "Upload failed: ${exception.message}")
            onComplete(null) // Return null on failure
        }.addOnSuccessListener {
            Log.d(TAG, "Upload successful! Getting download URL...")
            // Get the URL and return it via the callback
            recordingRef.downloadUrl.addOnSuccessListener { uri ->
                val downloadUrl = uri.toString()
                Log.d(TAG, "Download URL: $downloadUrl")
                onComplete(downloadUrl)
            }.addOnFailureListener {
                Log.e(TAG, "Failed to get download URL.")
                onComplete(null)
            }
        }.addOnProgressListener { snapshot ->
            val progress = (100.0 * snapshot.bytesTransferred) / snapshot.totalByteCount
            Log.d(TAG, "Upload is ${progress.toInt()}% done")
        }
    }
}