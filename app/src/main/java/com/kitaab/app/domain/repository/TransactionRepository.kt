package com.kitaab.app.domain.repository

import com.kitaab.app.domain.model.Transaction

interface TransactionRepository {
    suspend fun getTransactionForConversation(conversationId: String): Result<Transaction?>

    suspend fun createTransaction(
        conversationId: String,
        listingId: String,
        sellerId: String,
        buyerId: String,
        type: String,
        handoffMethod: String,
    ): Result<Transaction>

    suspend fun confirmByCurrentUser(
        transactionId: String,
        isSeller: Boolean,
    ): Result<Transaction>

    suspend fun uploadPackedPhoto(
        transactionId: String,
        photoBytes: ByteArray,
    ): Result<String>

    suspend fun raiseDispute(transactionId: String): Result<Unit>
}
