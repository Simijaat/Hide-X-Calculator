package com.example.vaultcalc.data.download

import androidx.room.*
import kotlinx.coroutines.flow.Flow

@Dao
interface DownloadDao {
    @Query("SELECT * FROM downloads ORDER BY createdAt DESC")
    fun getAllDownloads(): Flow<List<DownloadTask>>

    @Query("SELECT * FROM downloads WHERE id = :id")
    suspend fun getDownloadById(id: String): DownloadTask?

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insert(task: DownloadTask)

    @Update
    suspend fun update(task: DownloadTask)

    @Delete
    suspend fun delete(task: DownloadTask)

    @Query("UPDATE downloads SET state = :newState WHERE id = :id")
    suspend fun updateState(id: String, newState: DownloadState)

    @Query("UPDATE downloads SET downloadedBytes = :bytes WHERE id = :id")
    suspend fun updateProgress(id: String, bytes: Long)

    @Query("UPDATE downloads SET state = :newState, errorReason = :reason WHERE id = :id")
    suspend fun updateStateWithError(id: String, newState: DownloadState, reason: String?)
}
