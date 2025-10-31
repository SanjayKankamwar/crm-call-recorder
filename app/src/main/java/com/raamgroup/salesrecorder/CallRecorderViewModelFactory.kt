package com.raamgroup.salesrecorder

import android.content.Context
import androidx.lifecycle.ViewModel
import androidx.lifecycle.ViewModelProvider

class CallRecorderViewModelFactory(private val context: Context) : ViewModelProvider.Factory {
    override fun <T : ViewModel> create(modelClass: Class<T>): T {
        if (modelClass.isAssignableFrom(CallRecorderViewModel::class.java)) {
            @Suppress("UNCHECKED_CAST")
            return CallRecorderViewModel(context) as T
        }
        throw IllegalArgumentException("Unknown ViewModel class")
    }
}