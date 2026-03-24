package com.kitaab.app.feature.chat

import androidx.lifecycle.SavedStateHandle
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.kitaab.app.domain.repository.ConversationRepository
import dagger.hilt.android.lifecycle.HiltViewModel
import io.github.jan.supabase.SupabaseClient
import io.github.jan.supabase.auth.auth
import kotlinx.coroutines.channels.Channel
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.receiveAsFlow
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch
import javax.inject.Inject


@HiltViewModel
class ChatViewModel @Inject constructor(
    private val conversationRepository: ConversationRepository,
    private val supabase: SupabaseClient,
    savedStateHandle: SavedStateHandle,
) : ViewModel() {

    private val conversationId: String = checkNotNull(savedStateHandle["conversationId"])

    private val _uiState = MutableStateFlow(ChatUiState())
    val uiState: StateFlow<ChatUiState> = _uiState.asStateFlow()

    private val _events = Channel<ChatEvent>(Channel.BUFFERED)
    val events = _events.receiveAsFlow()

    private var sendJob: kotlinx.coroutines.Job? = null

    init {
        val userId = supabase.auth.currentUserOrNull()?.id ?: ""
        _uiState.update { it.copy(currentUserId = userId) }
        loadMessages()
        subscribeToMessages()
        markRead()
    }

    fun onInputChanged(text: String) {
        _uiState.update { it.copy(inputText = text) }
    }

    fun sendMessage() {
        val text = _uiState.value.inputText.trim()
        if (text.isBlank()) return
        if (sendJob?.isActive == true) return

        sendJob = viewModelScope.launch {
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
                        if (alreadyExists) state
                        else state.copy(messages = state.messages + newMessage)
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