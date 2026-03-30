package com.kitaab.app.data.local.db

import androidx.room.Dao
import androidx.room.Insert
import androidx.room.OnConflictStrategy
import androidx.room.Query

@Dao
interface ListingDao {
    @Query("SELECT * FROM own_listings ORDER BY createdAt DESC")
    suspend fun getAll(): List<CachedListing>

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun upsertAll(listings: List<CachedListing>)

    @Query("UPDATE own_listings SET status = :status WHERE id = :id")
    suspend fun updateStatus(
        id: String,
        status: String,
    )

    @Query("DELETE FROM own_listings")
    suspend fun clear()
}
