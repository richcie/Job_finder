package com.uilover.project196.ViewModel

import android.app.Application
import androidx.databinding.ObservableField
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.LiveData
import androidx.lifecycle.MutableLiveData
import androidx.lifecycle.viewModelScope
import kotlinx.coroutines.launch
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import com.uilover.project196.Model.MessageModel
import com.uilover.project196.Repository.ChatRepository
import com.uilover.project196.Utils.UserSession
import org.json.JSONObject

// KRITERIA WAJIB: ViewModel (4/12) - ViewModel untuk ChatConversationActivity
class ChatConversationViewModel(application: Application) : AndroidViewModel(application) {

    private val chatRepository = ChatRepository.getInstance(application)


    private val _messages = MutableLiveData<List<MessageModel>>()
    val messages: LiveData<List<MessageModel>> = _messages

    private val _isLoading = MutableLiveData<Boolean>()
    val isLoading: LiveData<Boolean> = _isLoading

    private val _isSendingMessage = MutableLiveData<Boolean>()
    val isSendingMessage: LiveData<Boolean> = _isSendingMessage

    private val _showSuccessMessage = MutableLiveData<String?>()
    val showSuccessMessage: LiveData<String?> = _showSuccessMessage

    private val _showErrorMessage = MutableLiveData<String?>()
    val showErrorMessage: LiveData<String?> = _showErrorMessage

    private val _scrollToBottom = MutableLiveData<Boolean>()
    val scrollToBottom: LiveData<Boolean> = _scrollToBottom

    private val _isLoggedIn = MutableLiveData<Boolean>()
    val isLoggedIn: LiveData<Boolean> = _isLoggedIn


    val messageInput = ObservableField<String>("")
    val currentChatId = ObservableField<String>("")


    private val _chatTitle = MutableLiveData<String>()
    val chatTitle: LiveData<String> = _chatTitle

    private val _chatSubtitle = MutableLiveData<String>()
    val chatSubtitle: LiveData<String> = _chatSubtitle


    private var lastSentTime = 0L
    private val minSendInterval = 1000L

    init {
        refreshLoginState()
    }

    fun refreshLoginState() {
        _isLoggedIn.value = UserSession.isLoggedIn()
    }

    fun setChatDetails(chatId: String, companyName: String, recruiterName: String, jobTitle: String) {
        currentChatId.set(chatId)
        _chatTitle.value = companyName
        _chatSubtitle.value = "$recruiterName • $jobTitle"
    }

    fun loadMessages(chatId: String) {
        if (!UserSession.isLoggedIn()) {
            _showErrorMessage.value = "Please log in to view messages"
            return
        }

        _isLoading.value = true
        viewModelScope.launch {
            try {
                val loadedMessages = withContext(Dispatchers.IO) {
                    chatRepository.getMessagesForChat(chatId)
                }

                _messages.value = loadedMessages
                _scrollToBottom.value = true


                withContext(Dispatchers.IO) {
                    chatRepository.markMessagesAsRead(chatId)
                }

            } catch (e: Exception) {
                _showErrorMessage.value = "Failed to load messages: ${e.message}"
            } finally {
                _isLoading.value = false
            }
        }
    }

    fun sendMessage() {
        val messageText = messageInput.get()?.trim() ?: ""
        val chatId = currentChatId.get() ?: ""

        if (messageText.isEmpty()) {
            _showErrorMessage.value = "Please enter a message"
            return
        }

        if (chatId.isEmpty()) {
            _showErrorMessage.value = "Chat not initialized"
            return
        }

        if (!UserSession.isLoggedIn()) {
            _showErrorMessage.value = "Please log in to send messages"
            return
        }


        val now = System.currentTimeMillis()
        if (now - lastSentTime < minSendInterval) {
            _showErrorMessage.value = "Please wait before sending another message"
            return
        }


        if (_isSendingMessage.value == true) {
            return
        }

        val currentUserId = UserSession.getUserId()
        if (currentUserId == null) {
            _showErrorMessage.value = "User session expired"
            return
        }


        if (handleSpecialCommands(messageText, chatId, currentUserId)) {
            return
        }

        lastSentTime = now
        _isSendingMessage.value = true

        viewModelScope.launch {
            try {
                val success = withContext(Dispatchers.IO) {
                    chatRepository.saveMessage(chatId, currentUserId, messageText)
                }

                if (success) {

                    messageInput.set("")


                    loadMessages(chatId)
                    _showSuccessMessage.value = "Message sent!"
                } else {
                    _showErrorMessage.value = "Failed to send message"
                    lastSentTime = 0L
                }
            } catch (e: Exception) {
                _showErrorMessage.value = "Error sending message: ${e.message}"
                lastSentTime = 0L
            } finally {
                _isSendingMessage.value = false
            }
        }
    }

    private fun handleSpecialCommands(messageText: String, chatId: String, currentUserId: String): Boolean {
        return when {
            messageText.contains("/verify", ignoreCase = true) -> {
                sendVerificationRequest(chatId, currentUserId)
                true
            }
            messageText.contains("/request", ignoreCase = true) -> {
                sendJobRequest(chatId, currentUserId)
                true
            }
            else -> false
        }
    }

    private fun sendVerificationRequest(chatId: String, currentUserId: String) {
        if (_isSendingMessage.value == true) return

        _isSendingMessage.value = true
        viewModelScope.launch {
            try {

                val userName = UserSession.getUserName() ?: "Unknown"
                val userEmail = UserSession.getUserEmail() ?: "No email"

                val freelancerData = JSONObject().apply {
                    put("id", currentUserId)
                    put("name", userName)
                    put("email", userEmail)
                }.toString()

                val message = "I would like to request verification for this job. Please verify my profile and credentials."

                val success = withContext(Dispatchers.IO) {
                    chatRepository.saveVerificationRequestMessage(chatId, currentUserId, freelancerData, message)
                }

                if (success) {
                    messageInput.set("")
                    loadMessages(chatId)
                    _showSuccessMessage.value = "Verification request sent!"
                } else {
                    _showErrorMessage.value = "Verification request already pending or failed to send"
                }
            } catch (e: Exception) {
                _showErrorMessage.value = "Error sending verification request: ${e.message}"
            } finally {
                _isSendingMessage.value = false
            }
        }
    }

    private fun sendJobRequest(chatId: String, currentUserId: String) {
        if (_isSendingMessage.value == true) return

        _isSendingMessage.value = true
        viewModelScope.launch {
            try {

                val jobId = "job_default"
                val message = "I would like to request to work on this job. I believe my skills and experience make me a great fit for this position."

                val success = withContext(Dispatchers.IO) {
                    chatRepository.saveJobRequestMessage(chatId, currentUserId, jobId, message)
                }

                if (success) {
                    messageInput.set("")
                    loadMessages(chatId)
                    _showSuccessMessage.value = "Job request sent!"
                } else {
                    _showErrorMessage.value = "Job request already pending or failed to send"
                }
            } catch (e: Exception) {
                _showErrorMessage.value = "Error sending job request: ${e.message}"
            } finally {
                _isSendingMessage.value = false
            }
        }
    }

    fun handleJobRequestAction(message: MessageModel, isAccept: Boolean) {
        if (!UserSession.isLoggedIn() || UserSession.getUserRole() != UserSession.ROLE_BUSINESS_OWNER) {
            _showErrorMessage.value = "Only business owners can handle job requests"
            return
        }

        _isLoading.value = true
        viewModelScope.launch {
            try {
                val newStatus = if (isAccept) "accepted" else "rejected"
                val success = withContext(Dispatchers.IO) {
                    chatRepository.updateJobRequestStatus(message.id, newStatus)
                }

                if (success) {
                    loadMessages(currentChatId.get() ?: "")
                    _showSuccessMessage.value = "Job request ${if (isAccept) "accepted" else "rejected"}"
                } else {
                    _showErrorMessage.value = "Failed to update job request"
                }
            } catch (e: Exception) {
                _showErrorMessage.value = "Error updating job request: ${e.message}"
            } finally {
                _isLoading.value = false
            }
        }
    }

    fun handleVerificationAction(message: MessageModel, isAccept: Boolean) {
        if (!UserSession.isLoggedIn() || UserSession.getUserRole() != UserSession.ROLE_BUSINESS_OWNER) {
            _showErrorMessage.value = "Only business owners can handle verification requests"
            return
        }

        _isLoading.value = true
        viewModelScope.launch {
            try {
                val newStatus = if (isAccept) "accepted" else "rejected"
                val success = withContext(Dispatchers.IO) {
                    chatRepository.updateVerificationStatus(message.id, newStatus)
                }

                if (success) {
                    loadMessages(currentChatId.get() ?: "")
                    _showSuccessMessage.value = "Verification request ${if (isAccept) "accepted" else "rejected"}"
                } else {
                    _showErrorMessage.value = "Failed to update verification request"
                }
            } catch (e: Exception) {
                _showErrorMessage.value = "Error updating verification request: ${e.message}"
            } finally {
                _isLoading.value = false
            }
        }
    }


    fun onSuccessMessageShown() {
        _showSuccessMessage.value = null
    }

    fun onErrorMessageShown() {
        _showErrorMessage.value = null
    }

    fun onScrolledToBottom() {
        _scrollToBottom.value = false
    }
}