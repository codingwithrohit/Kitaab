package com.kitaab.app.data.local.db

import androidx.room.Dao
import androidx.room.Insert
import androidx.room.OnConflictStrategy
import androidx.room.Query
import kotlinx.coroutines.flow.Flow

@Dao
interface PostingSessionDao {
    @Query("SELECT * FROM posting_sessions")
    suspend fun getAllSessions(): List<CachedPostingSession>

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

    @Query(
        """
    UPDATE posting_sessions 
    SET defaultType = :type, 
        defaultCity = :city, 
        defaultPincode = :pincode, 
        defaultLocality = :locality 
    WHERE id = :sessionId
""",
    )
    suspend fun updateDefaults(
        sessionId: String,
        type: String,
        city: String,
        pincode: String,
        locality: String,
    )

    @Query("DELETE FROM posting_sessions")
    suspend fun deleteAll()
}
