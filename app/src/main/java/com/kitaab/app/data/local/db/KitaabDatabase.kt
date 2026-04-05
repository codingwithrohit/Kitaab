package com.kitaab.app.data.local.db

import androidx.room.Database
import androidx.room.RoomDatabase
import androidx.room.TypeConverters
import androidx.room.migration.Migration
import androidx.sqlite.db.SupportSQLiteDatabase

@Database(
    entities = [
        CachedUserProfile::class,
        CachedListing::class,
        CachedPostingSession::class,
        CachedStagedBook::class,
        CachedStagedBundle::class,
    ],
    version = 2,
    exportSchema = false,
)
@TypeConverters(Converters::class)
abstract class KitaabDatabase : RoomDatabase() {
    abstract fun userProfileDao(): UserProfileDao

    abstract fun listingDao(): ListingDao

    abstract fun postingSessionDao(): PostingSessionDao

    abstract fun stagedBookDao(): StagedBookDao

    abstract fun stagedBundleDao(): StagedBundleDao

    companion object {
        val MIGRATION_1_2 =
            object : Migration(1, 2) {
                override fun migrate(db: SupportSQLiteDatabase) {
                    db.execSQL(
                        """
                        CREATE TABLE IF NOT EXISTS posting_sessions (
                            id TEXT NOT NULL PRIMARY KEY,
                            defaultType TEXT NOT NULL,
                            defaultCity TEXT NOT NULL,
                            defaultPincode TEXT NOT NULL,
                            defaultLocality TEXT NOT NULL,
                            createdAt INTEGER NOT NULL
                        )
                        """.trimIndent(),
                    )
                    db.execSQL(
                        """
                        CREATE TABLE IF NOT EXISTS staged_books (
                            id TEXT NOT NULL PRIMARY KEY,
                            sessionId TEXT NOT NULL,
                            bundleId TEXT,
                            title TEXT NOT NULL,
                            author TEXT NOT NULL,
                            publisher TEXT NOT NULL,
                            edition TEXT NOT NULL,
                            isbn TEXT NOT NULL,
                            subject TEXT NOT NULL,
                            examTags TEXT NOT NULL,
                            hasSolutions INTEGER NOT NULL,
                            hasNotes INTEGER NOT NULL,
                            condition TEXT NOT NULL,
                            individualPrice TEXT NOT NULL,
                            photoPaths TEXT NOT NULL,
                            typeOverride TEXT,
                            sortOrder INTEGER NOT NULL,
                            FOREIGN KEY (sessionId) REFERENCES posting_sessions(id) ON DELETE CASCADE
                        )
                        """.trimIndent(),
                    )
                    db.execSQL("CREATE INDEX IF NOT EXISTS index_staged_books_sessionId ON staged_books(sessionId)")
                    db.execSQL("CREATE INDEX IF NOT EXISTS index_staged_books_bundleId ON staged_books(bundleId)")
                    db.execSQL(
                        """
                        CREATE TABLE IF NOT EXISTS staged_bundles (
                            id TEXT NOT NULL PRIMARY KEY,
                            sessionId TEXT NOT NULL,
                            name TEXT NOT NULL,
                            bundlePrice TEXT NOT NULL,
                            typeOverride TEXT,
                            FOREIGN KEY (sessionId) REFERENCES posting_sessions(id) ON DELETE CASCADE
                        )
                        """.trimIndent(),
                    )
                    db.execSQL("CREATE INDEX IF NOT EXISTS index_staged_bundles_sessionId ON staged_bundles(sessionId)")
                }
            }
    }
}
