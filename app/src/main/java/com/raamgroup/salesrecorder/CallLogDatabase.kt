package com.raamgroup.salesrecorder

import androidx.room.Database
import androidx.room.RoomDatabase

@Database(entities = [CallLogEntity::class], version = 1)
abstract class CallLogDatabase : RoomDatabase() {
    abstract fun callLogDao(): CallLogDao
}