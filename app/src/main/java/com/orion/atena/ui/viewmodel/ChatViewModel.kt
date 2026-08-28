package com.orion.atena.ui.viewmodel

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.orion.atena.data.api.OrionApiService
import com.orion.atena.data.local.ChatDao
import com.orion.atena.data.model.ChatMessage
import com.orion.atena.data.model.ChatSession
import com.orion.atena.data.model.MessageEntity
import kotlinx.coroutines.flow.*
import kotlinx.coroutines.launch
import java.util.UUID

class ChatViewModel(
    private val apiService: OrionApiService,
    private val chatDao: ChatDao,
) : ViewModel() {

    private val _uiState = MutableStateFlow<ChatUiState>(ChatUiState.Loading)
    val uiState: StateFlow<ChatUiState> = _uiState.asStateFlow()

    private val _historyState = MutableStateFlow(ChatHistoryState())
    val historyState: StateFlow<ChatHistoryState> = _historyState.asStateFlow()

    private val _messages = MutableStateFlow<List<ChatMessage>>(emptyList())
    val messages: StateFlow<List<ChatMessage>> = _messages.asStateFlow()

    private val _selectedText = MutableStateFlow<String?>(null)
    val selectedText: StateFlow<String?> = _selectedText.asStateFlow()

    private val _quotingMessage = MutableStateFlow<ChatMessage?>(null)
    val quotingMessage: StateFlow<ChatMessage?> = _quotingMessage.asStateFlow()

    private var currentChatId: String? = null

    init {
        observeChatHistory()
    }

    private fun observeChatHistory() {
        viewModelScope.launch {
            chatDao.getAllChats().collect { chats ->
                _historyState.value = ChatHistoryState(
                    chats = chats,
                    currentChatId = currentChatId,
                )
            }
        }
    }

    fun newChat() {
        val chatId = UUID.randomUUID().toString()
        val chat = ChatSession(id = chatId, title = "Sessão Quântica")

        viewModelScope.launch {
            chatDao.insertChat(chat)
            currentChatId = chatId
            _messages.value = emptyList()
            _uiState.value = ChatUiState.Active(chatId = chatId, messages = emptyList())
            _historyState.value = _historyState.value.copy(currentChatId = chatId)
        }
    }

    fun openChat(chatId: String) {
        currentChatId = chatId
        viewModelScope.launch {
            chatDao.getMessages(chatId).collect { entities ->
                val msgs = entities.map {
                    ChatMessage(
                        id = it.id,
                        role = it.role,
                        content = it.content,
                        timestamp = it.timestamp,
                        quotedFrom = it.quotedFrom,
                        quotedContent = it.quotedContent
                    )
                }
                _messages.value = msgs
                _uiState.value = ChatUiState.Active(chatId = chatId, messages = msgs)
            }
            _historyState.value = _historyState.value.copy(currentChatId = chatId)
        }
    }

    fun executeQuantumBenchmark(
        numQubits: Int = 5,
        shots: Int = 1000,
        useRealHardware: Boolean = false,
        ibmToken: String? = null
    ) {
        val chatId = currentChatId ?: run {
            newChat()
            currentChatId ?: return
        }

        viewModelScope.launch {
            _uiState.value = ChatUiState.Active(
                chatId = chatId, 
                messages = _messages.value, 
                isStreaming = true
            )

            try {
                val result = apiService.runQuantumBenchmark(numQubits, shots, useRealHardware, ibmToken)
                val content = """
                    ⚛️ **Processamento Quântico Orion**
                    • **Engine:** ${result.engine}
                    • **Status:** ${result.status}
                    • **Qubits:** ${result.qubitsProcessados}
                    • **Espaço Hilbert:** ${result.espacoHilbert ?: "N/A"}
                    • **Shots:** ${result.shotsExecutados}
                    • **Tempo Execução:** ${result.tempoExecucaoMs} ms
                    • **Amostras:** ${result.amostraResultado}
                """.trimIndent()

                val quantumMessage = ChatMessage(role = "assistant", content = content)
                _messages.value = _messages.value + quantumMessage

                chatDao.insertMessage(
                    MessageEntity(
                        id = quantumMessage.id, 
                        chatId = chatId, 
                        role = "assistant", 
                        content = content
                    )
                )

                _uiState.value = ChatUiState.Active(chatId = chatId, messages = _messages.value)
            } catch (e: Exception) {
                _uiState.value = ChatUiState.Error(
                    message = "Erro na execução quântica GCP: ${e.localizedMessage}",
                    chatId = chatId
                )
            }
        }
    }

    fun selectText(text: String?) { _selectedText.value = text }
    fun startQuote(message: ChatMessage) { _quotingMessage.value = message; _selectedText.value = null }
    fun cancelQuote() { _quotingMessage.value = null; _selectedText.value = null }
fun sendMessage(userPrompt: String) {
    if (userPrompt.isBlank()) return

    viewModelScope.launch {
        val userMessage = ChatMessage(
            role = "user",
            content = userPrompt
        )
        chatDao.insertMessage(
            MessageEntity(
                id = userMessage.id,
                chatId = currentChatId ?: "1",
                role = userMessage.role,
                content = userMessage.content,
                timestamp = userMessage.timestamp
            )
        )
    }
}
}
