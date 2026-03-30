package com.kitaab.app.data.local.db

import androidx.room.Database
import androidx.room.RoomDatabase
import androidx.room.TypeConverters

@Database(
    entities = [CachedUserProfile::class, CachedListing::class],
    version = 1,
    exportSchema = false,
)
@TypeConverters(Converters::class)
abstract class KitaabDatabase : RoomDatabase() {
    abstract fun userProfileDao(): UserProfileDao

    abstract fun listingDao(): ListingDao
}
