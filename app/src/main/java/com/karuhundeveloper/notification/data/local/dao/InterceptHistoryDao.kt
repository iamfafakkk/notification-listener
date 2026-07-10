package com.karuhundeveloper.notification.data.local.dao

import androidx.room.Dao
import androidx.room.Insert
import androidx.room.Query
import com.karuhundeveloper.notification.data.local.entity.InterceptHistory
import kotlinx.coroutines.flow.Flow

@Dao
interface InterceptHistoryDao {

    @Insert
    suspend fun insertHistory(history: InterceptHistory)

    @Query("SELECT * FROM intercept_history WHERE ruleId = :ruleId ORDER BY timestamp DESC")
    fun getHistoryForRule(ruleId: Int): Flow<List<InterceptHistory>>

    @Query("DELETE FROM intercept_history WHERE ruleId = :ruleId")
    suspend fun clearHistoryForRule(ruleId: Int)

    @Query("DELETE FROM intercept_history")
    suspend fun deleteAllHistory()
}
