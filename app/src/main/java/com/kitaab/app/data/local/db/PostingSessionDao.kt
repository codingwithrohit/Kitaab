package com.kitaab.app.data.local.db

import androidx.room.Dao
import androidx.room.Insert
import androidx.room.OnConflictStrategy
import androidx.room.Query
import kotlinx.coroutines.flow.Flow

@Dao
interface PostingSessionDao {

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun upsert(session: CachedPostingSession)

    @Query("SELECT * FROM posting_sessions ORDER BY createdAt DESC LIMIT 1")
    fun observeLatest(): Flow<CachedPostingSession?>

    @Query("SELECT * FROM posting_sessions ORDER BY createdAt DESC LIMIT 1")
    suspend fun getLatest(): CachedPostingSession?

    @Query("DELETE FROM posting_sessions WHERE id = :sessionId")
    suspend fun delete(sessionId: String)

    // Called on app launch to clean up abandoned sessions older than 7 days
    @Query("DELETE FROM posting_sessions WHERE createdAt < :cutoff")
    suspend fun deleteOlderThan(cutoff: Long)

    @Query("DELETE FROM posting_sessions")
    suspend fun deleteAll()
}