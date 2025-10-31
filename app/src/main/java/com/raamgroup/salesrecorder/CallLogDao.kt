package com.raamgroup.salesrecorder

import androidx.room.Dao
import androidx.room.Insert
import androidx.room.Update
import androidx.room.Query

@Dao
interface CallLogDao {

    @Insert
    suspend fun insertCallLog(callLog: CallLogEntity)

    @Update
    suspend fun updateCallLog(callLog: CallLogEntity)

    @Query("SELECT * FROM call_logs ORDER BY createdAt DESC")
    suspend fun getAllCallLogs(): List<CallLogEntity>

    @Query("SELECT * FROM call_logs WHERE syncStatus = :status ORDER BY createdAt DESC")
    suspend fun getCallLogsByStatus(status: String): List<CallLogEntity>

    @Query("UPDATE call_logs SET syncStatus = :status, attempts = :attempts WHERE id = :id")
    suspend fun updateSyncStatus(id: String, status: String, attempts: Int)

}
