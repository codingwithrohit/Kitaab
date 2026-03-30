package com.kitaab.app.di

import android.content.Context
import androidx.room.Room
import com.kitaab.app.data.local.db.KitaabDatabase
import com.kitaab.app.data.local.db.ListingDao
import com.kitaab.app.data.local.db.UserProfileDao
import dagger.Module
import dagger.Provides
import dagger.hilt.InstallIn
import dagger.hilt.android.qualifiers.ApplicationContext
import dagger.hilt.components.SingletonComponent
import javax.inject.Singleton

@Module
@InstallIn(SingletonComponent::class)
object DatabaseModule {

    @Provides
    @Singleton
    fun provideDatabase(@ApplicationContext context: Context): KitaabDatabase =
        Room.databaseBuilder(
            context,
            KitaabDatabase::class.java,
            "kitaab.db",
        ).build()

    @Provides
    fun provideUserProfileDao(db: KitaabDatabase): UserProfileDao = db.userProfileDao()

    @Provides
    fun provideListingDao(db: KitaabDatabase): ListingDao = db.listingDao()
}