package com.example.data

import androidx.room.Entity
import androidx.room.PrimaryKey

@Entity(tableName = "recent_calls")
data class RecentCall(
    @PrimaryKey(autoGenerate = true) val id: Long = 0,
    val contactId: Long? = null,
    val name: String,
    val phoneNumber: String,
    val timestamp: Long = System.currentTimeMillis(),
    val callType: String, // "INCOMING", "OUTGOING", "MISSED"
    val durationSeconds: Int = 0
)
