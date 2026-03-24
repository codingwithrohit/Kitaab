package com.kitaab.app.di


import com.kitaab.app.data.repository.AuthRepositoryImpl
import com.kitaab.app.data.repository.ConversationRepositoryImpl
import com.kitaab.app.data.repository.DonationRequestRepositoryImpl
import com.kitaab.app.domain.repository.AuthRepository
import com.kitaab.app.domain.repository.ConversationRepository
import com.kitaab.app.domain.repository.DonationRequestRepository
import dagger.Binds
import dagger.Module
import dagger.hilt.InstallIn
import dagger.hilt.components.SingletonComponent
import javax.inject.Singleton

@Module
@InstallIn(SingletonComponent::class)
abstract class RepositoryModule {

    @Binds
    @Singleton
    abstract fun bindAuthRepository(impl: AuthRepositoryImpl): AuthRepository

    @Binds
    @Singleton
    abstract fun bindConversationRepository(
        impl: ConversationRepositoryImpl,
    ): ConversationRepository

    @Binds
    @Singleton
    abstract fun bindDonationRequestRepository(
        impl: DonationRequestRepositoryImpl,
    ): DonationRequestRepository


}