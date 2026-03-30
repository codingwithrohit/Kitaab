package com.kitaab.app.feature.chat

import androidx.lifecycle.SavedStateHandle
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.kitaab.app.domain.repository.ConversationRepository
import com.kitaab.app.domain.repository.TransactionRepository
import dagger.hilt.android.lifecycle.HiltViewModel
import io.github.jan.supabase.SupabaseClient
import io.github.jan.supabase.auth.auth
import io.github.jan.supabase.postgrest.postgrest
import kotlinx.coroutines.channels.Channel
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.receiveAsFlow
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch
import javax.inject.Inject

@HiltViewModel
class ChatViewModel
    @Inject
    constructor(
        private val conversationRepository: ConversationRepository,
        private val transactionRepository: TransactionRepository,
        private val supabase: SupabaseClient,
        savedStateHandle: SavedStateHandle,
    ) : ViewModel() {
        private val conversationId: String = checkNotNull(savedStateHandle["conversationId"])

        private val _uiState = MutableStateFlow(ChatUiState())
        val uiState: StateFlow<ChatUiState> = _uiState.asStateFlow()

        private val _events = Channel<ChatEvent>(Channel.BUFFERED)
        val events = _events.receiveAsFlow()

        private var sendJob: kotlinx.coroutines.Job? = null
        private var confirmJob: kotlinx.coroutines.Job? = null

        init {
            val userId = supabase.auth.currentUserOrNull()?.id ?: ""
            _uiState.update { it.copy(currentUserId = userId) }
            loadConversationDetails()
            loadMessages()
            subscribeToMessages()
            markRead()
        }

        private fun loadConversationDetails() {
            viewModelScope.launch {
                runCatching {
                    val conversation =
                        supabase.postgrest["conversations"]
                            .select { filter { eq("id", conversationId) } }
                            .decodeList<ConversationRow>()
                            .firstOrNull() ?: return@runCatching

                    val currentUserId = _uiState.value.currentUserId
                    val isSeller = conversation.sellerId == currentUserId
                    val otherUserId = if (isSeller) conversation.buyerId else conversation.sellerId

                    val listing =
                        supabase.postgrest["listings"]
                            .select { filter { eq("id", conversation.listingId) } }
                            .decodeList<ListingRow>()
                            .firstOrNull()

                    _uiState.update {
                        it.copy(
                            isSeller = isSeller,
                            otherUserId = otherUserId,
                            listingId = conversation.listingId,
                            listingType = listing?.type ?: "SELL",
                        )
                    }
                }
                loadTransaction()
            }
        }

        fun loadTransaction() {
            viewModelScope.launch {
                transactionRepository.getTransactionForConversation(conversationId)
                    .onSuccess { transaction ->
                        _uiState.update { it.copy(transaction = transaction) }
                    }
            }
        }

        fun onInputChanged(text: String) {
            _uiState.update { it.copy(inputText = text) }
        }

        fun sendMessage() {
            val text = _uiState.value.inputText.trim()
            if (text.isBlank()) return
            if (sendJob?.isActive == true) return

            sendJob =
                viewModelScope.launch {
                    _uiState.update { it.copy(isSending = true, inputText = "") }
                    conversationRepository.sendMessage(conversationId, text)
                        .onSuccess { message ->
                            _uiState.update { state ->
                                state.copy(
                                    isSending = false,
                                    messages = state.messages + message,
                                )
                            }
                            _events.send(ChatEvent.ScrollToBottom)
                        }
                        .onFailure { throwable ->
                            _uiState.update {
                                it.copy(
                                    isSending = false,
                                    inputText = text,
                                    error = throwable.message ?: "Failed to send message",
                                )
                            }
                        }
                }
        }

        fun onMarkCompleteClick() {
            val state = _uiState.value
            // Seller must pick handoff method first if no transaction exists
            if (state.transaction == null && state.isSeller) {
                _uiState.update { it.copy(showHandoffSheet = true) }
                return
            }
            // Buyer can confirm directly once transaction exists
            if (state.transaction != null) {
                confirmTransaction()
            }
        }

        fun onHandoffMethodSelected(method: String) {
            _uiState.update { it.copy(showHandoffSheet = false, isCreatingTransaction = true) }
            viewModelScope.launch {
                val state = _uiState.value
                transactionRepository.createTransaction(
                    conversationId = conversationId,
                    listingId = state.listingId,
                    sellerId = state.currentUserId,
                    buyerId = state.otherUserId,
                    type = state.listingType,
                    handoffMethod = method,
                ).onSuccess { transaction ->
                    _uiState.update { it.copy(transaction = transaction, isCreatingTransaction = false) }
                }.onFailure { throwable ->
                    _uiState.update {
                        it.copy(
                            isCreatingTransaction = false,
                            error = throwable.message ?: "Failed to initiate transaction",
                        )
                    }
                }
            }
        }

        fun dismissHandoffSheet() {
            _uiState.update { it.copy(showHandoffSheet = false) }
        }

        fun confirmTransaction() {
            if (confirmJob?.isActive == true) return
            val transaction = _uiState.value.transaction ?: return

            confirmJob =
                viewModelScope.launch {
                    _uiState.update { it.copy(isConfirming = true) }
                    transactionRepository.confirmByCurrentUser(
                        transactionId = transaction.id,
                        isSeller = _uiState.value.isSeller,
                    ).onSuccess { updated ->
                        _uiState.update { it.copy(transaction = updated, isConfirming = false) }
                        if (updated.confirmedBySeller && updated.confirmedByBuyer) {
                            _events.send(ChatEvent.TransactionComplete)
                        }
                    }.onFailure { throwable ->
                        _uiState.update {
                            it.copy(
                                isConfirming = false,
                                error = throwable.message ?: "Failed to confirm transaction",
                            )
                        }
                    }
                }
        }

        fun raiseDispute() {
            val transaction = _uiState.value.transaction ?: return
            viewModelScope.launch {
                _uiState.update { it.copy(isDisputing = true) }
                transactionRepository.raiseDispute(transaction.id)
                    .onSuccess {
                        _uiState.update { state ->
                            state.copy(
                                isDisputing = false,
                                transaction = state.transaction?.copy(disputed = true),
                            )
                        }
                    }
                    .onFailure { throwable ->
                        _uiState.update {
                            it.copy(
                                isDisputing = false,
                                error = throwable.message ?: "Failed to raise dispute",
                            )
                        }
                    }
            }
        }

        fun clearError() {
            _uiState.update { it.copy(error = null) }
        }

        private fun loadMessages() {
            viewModelScope.launch {
                _uiState.update { it.copy(isLoading = true) }
                conversationRepository.getMessages(conversationId)
                    .onSuccess { messages ->
                        _uiState.update { it.copy(messages = messages, isLoading = false) }
                        if (messages.isNotEmpty()) _events.send(ChatEvent.ScrollToBottom)
                    }
                    .onFailure { throwable ->
                        _uiState.update {
                            it.copy(
                                isLoading = false,
                                error = throwable.message ?: "Failed to load messages",
                            )
                        }
                    }
            }
        }

        private fun subscribeToMessages() {
            viewModelScope.launch {
                conversationRepository.subscribeToMessages(conversationId)
                    .collect { newMessage ->
                        _uiState.update { state ->
                            val alreadyExists = state.messages.any { it.id == newMessage.id }
                            if (alreadyExists) {
                                state
                            } else {
                                state.copy(messages = state.messages + newMessage)
                            }
                        }
                        _events.send(ChatEvent.ScrollToBottom)
                        markRead()
                    }
            }
        }

        private fun markRead() {
            val userId = _uiState.value.currentUserId
            if (userId.isBlank()) return
            viewModelScope.launch {
                conversationRepository.markMessagesRead(conversationId, userId)
            }
        }
    }

@kotlinx.serialization.Serializable
private data class ConversationRow(
    val id: String,
    @kotlinx.serialization.SerialName("listing_id") val listingId: String,
    @kotlinx.serialization.SerialName("buyer_id") val buyerId: String,
    @kotlinx.serialization.SerialName("seller_id") val sellerId: String,
)

@kotlinx.serialization.Serializable
private data class ListingRow(
    val id: String,
    val type: String,
)
