package com.kitaab.app.domain.repository


interface AuthRepository {
    suspend fun signIn(email: String, password: String): Result<Unit>
    suspend fun signUp(email: String, password: String): Result<Unit>
    suspend fun signOut(): Result<Unit>
    suspend fun deleteAccount(): Result<Unit>
}