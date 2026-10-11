package com.kitaab.app.data.local.db

import androidx.room.Dao
import androidx.room.Insert
import androidx.room.OnConflictStrategy
import androidx.room.Query
import androidx.room.Update
import kotlinx.coroutines.flow.Flow

@Dao
interface StagedBookDao {
    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun upsert(book: CachedStagedBook)

    @Update
    suspend fun update(book: CachedStagedBook)

    // Ordered by sortOrder so tray and organise screen stay consistent
    @Query("SELECT * FROM staged_books WHERE sessionId = :sessionId ORDER BY sortOrder ASC")
    fun observeForSession(sessionId: String): Flow<List<CachedStagedBook>>

    @Query("SELECT * FROM staged_books WHERE sessionId = :sessionId ORDER BY sortOrder ASC")
    suspend fun getForSession(sessionId: String): List<CachedStagedBook>

    @Query("SELECT * FROM staged_books WHERE id = :bookId")
    suspend fun getById(bookId: String): CachedStagedBook?

    @Query("SELECT * FROM staged_books WHERE bundleId = :bundleId ORDER BY sortOrder ASC")
    suspend fun getForBundle(bundleId: String): List<CachedStagedBook>

    @Query("SELECT COUNT(*) FROM staged_books WHERE sessionId = :sessionId")
    suspend fun countForSession(sessionId: String): Int

    // Assign / remove bundle membership
    @Query("UPDATE staged_books SET bundleId = :bundleId WHERE id IN (:bookIds)")
    suspend fun assignBundle(
        bundleId: String,
        bookIds: List<String>,
    )

    @Query("UPDATE staged_books SET bundleId = NULL WHERE bundleId = :bundleId")
    suspend fun clearBundle(bundleId: String)

    @Query("UPDATE staged_books SET bundleId = NULL WHERE id IN (:bookIds)")
    suspend fun removeFromBundle(bookIds: List<String>)

    @Query("DELETE FROM staged_books WHERE id = :bookId")
    suspend fun delete(bookId: String)

    @Query("DELETE FROM staged_books WHERE sessionId = :sessionId")
    suspend fun deleteForSession(sessionId: String)
}
