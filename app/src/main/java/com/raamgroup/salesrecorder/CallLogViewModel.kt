package com.raamgroup.salesrecorder

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import androidx.media3.common.MediaItem
import androidx.media3.exoplayer.ExoPlayer
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch
import java.time.LocalDateTime

class CallLogViewModel(private val exoPlayer: ExoPlayer) : ViewModel() {

    private val _callLogs = MutableStateFlow<List<CallLogUI>>(emptyList())
    val callLogs = _callLogs.asStateFlow()

    private var currentPlayingId: Long? = null

    fun addCallLog(callLog: CallLog) {
        val uiLog = CallLogUI(
            id = System.currentTimeMillis(), // Use timestamp as a unique ID
            createdAt = LocalDateTime.now(),
            userName = callLog.userName,
            phoneNumber = callLog.phoneNumber,
            type = callLog.type,
            callDurationSeconds = callLog.callDurationSeconds.toLong(),
            uploadUrl = callLog.uploadUrl ?: ""
        )
        _callLogs.value = (_callLogs.value + uiLog).sortedByDescending { it.createdAt }
    }

    fun playRecording(callLog: CallLogUI) {
        if (currentPlayingId == callLog.id) {
            exoPlayer.playWhenReady = !exoPlayer.playWhenReady
            return
        }

        currentPlayingId = callLog.id
        val mediaItem = MediaItem.fromUri(callLog.uploadUrl)
        exoPlayer.setMediaItem(mediaItem)
        exoPlayer.prepare()
        exoPlayer.playWhenReady = true
    }

    override fun onCleared() {
        exoPlayer.release()
        super.onCleared()
    }
}
