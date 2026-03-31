package com.kitaab.app.data.local.db

import androidx.room.Dao
import androidx.room.Insert
import androidx.room.OnConflictStrategy
import androidx.room.Query
import androidx.room.Update
import kotlinx.coroutines.flow.Flow

@Dao
interface StagedBundleDao {

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun upsert(bundle: CachedStagedBundle)

    @Update
    suspend fun update(bundle: CachedStagedBundle)

    @Query("SELECT * FROM staged_bundles WHERE sessionId = :sessionId")
    fun observeForSession(sessionId: String): Flow<List<CachedStagedBundle>>

    @Query("SELECT * FROM staged_bundles WHERE sessionId = :sessionId")
    suspend fun getForSession(sessionId: String): List<CachedStagedBundle>

    @Query("SELECT * FROM staged_bundles WHERE id = :bundleId")
    suspend fun getById(bundleId: String): CachedStagedBundle?

    @Query("DELETE FROM staged_bundles WHERE id = :bundleId")
    suspend fun delete(bundleId: String)

    @Query("DELETE FROM staged_bundles WHERE sessionId = :sessionId")
    suspend fun deleteForSession(sessionId: String)
}