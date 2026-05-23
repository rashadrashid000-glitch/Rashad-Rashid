package com.example.data

import androidx.room.Dao
import androidx.room.Insert
import androidx.room.OnConflictStrategy
import androidx.room.Query
import kotlinx.coroutines.flow.Flow

@Dao
interface RecentCallDao {
    @Query("SELECT * FROM recent_calls ORDER BY timestamp DESC")
    fun getAllRecentCalls(): Flow<List<RecentCall>>

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertRecentCall(recentCall: RecentCall): Long

    @Query("DELETE FROM recent_calls WHERE id = :id")
    suspend fun deleteRecentCallById(id: Long)

    @Query("DELETE FROM recent_calls")
    suspend fun clearAllRecentCalls()
}
