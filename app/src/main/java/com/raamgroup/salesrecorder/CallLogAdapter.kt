package com.raamgroup.salesrecorder

import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.TextView
import androidx.recyclerview.widget.RecyclerView

class CallLogAdapter(
    private var callLogs: MutableList<CallLogEntity>,
    private val onPlayRecording: (CallLogEntity) -> Unit
) : RecyclerView.Adapter<CallLogAdapter.ViewHolder>() {

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): ViewHolder {
        val view = LayoutInflater.from(parent.context).inflate(R.layout.item_call_log, parent, false)
        return ViewHolder(view)
    }

    override fun onBindViewHolder(holder: ViewHolder, position: Int) {
        val callLog = callLogs[position]
        holder.bind(callLog, onPlayRecording)
    }

    override fun getItemCount() = callLogs.size

    fun updateLogs(newLogs: List<CallLogEntity>) {
        callLogs.clear()
        callLogs.addAll(newLogs)
        notifyDataSetChanged()
    }

    class ViewHolder(itemView: View) : RecyclerView.ViewHolder(itemView) {
        private val userNameTextView: TextView = itemView.findViewById(R.id.userNameTextView)
        private val phoneNumberTextView: TextView = itemView.findViewById(R.id.phoneNumberTextView)
        private val callInfoTextView: TextView = itemView.findViewById(R.id.callInfoTextView)
        private val syncStatusTextView: TextView = itemView.findViewById(R.id.syncStatusTextView)

        fun bind(callLog: CallLogEntity, onPlayRecording: (CallLogEntity) -> Unit) {
            userNameTextView.text = callLog.userName
            phoneNumberTextView.text = callLog.phoneNumber
            callInfoTextView.text = "${callLog.type} - ${callLog.callDurationSeconds}s"
            syncStatusTextView.text = "Sync Status: ${callLog.syncStatus}"

            itemView.setOnClickListener {
                onPlayRecording(callLog)
            }
        }
    }
}