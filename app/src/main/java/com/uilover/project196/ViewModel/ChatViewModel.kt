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
import com.uilover.project196.Model.ChatModel
import com.uilover.project196.Repository.ChatRepository
import com.uilover.project196.Utils.UserSession

// KRITERIA WAJIB: ViewModel (3/12) - ViewModel untuk ChatFragment
class ChatViewModel(application: Application) : AndroidViewModel(application), ChatRepository.ChatUpdateListener {

    private val chatRepository = ChatRepository.getInstance(application)


    private val _allChats = MutableLiveData<List<ChatModel>>()
    val allChats: LiveData<List<ChatModel>> = _allChats

    private val _isLoading = MutableLiveData<Boolean>()
    val isLoading: LiveData<Boolean> = _isLoading

    private val _isLoggedIn = MutableLiveData<Boolean>()
    val isLoggedIn: LiveData<Boolean> = _isLoggedIn

    private val _showErrorMessage = MutableLiveData<String?>()
    val showErrorMessage: LiveData<String?> = _showErrorMessage

    private val _showSuccessMessage = MutableLiveData<String?>()
    val showSuccessMessage: LiveData<String?> = _showSuccessMessage

    private val _chatCount = MutableLiveData<Int>()
    val chatCount: LiveData<Int> = _chatCount

    private val _unreadCount = MutableLiveData<Int>()
    val unreadCount: LiveData<Int> = _unreadCount

    private val _userRole = MutableLiveData<String>()
    val userRole: LiveData<String> = _userRole

    private val _shouldShowGuestState = MutableLiveData<Boolean>()
    val shouldShowGuestState: LiveData<Boolean> = _shouldShowGuestState


    val searchQuery = ObservableField<String>("")

    init {
        refreshLoginState()
        chatRepository.addListener(this)
    }

    override fun onCleared() {
        super.onCleared()
        chatRepository.removeListener(this)
    }

    fun refreshLoginState() {
        val isLoggedIn = UserSession.isLoggedIn()
        _isLoggedIn.value = isLoggedIn
        _userRole.value = UserSession.getUserRole()
        _shouldShowGuestState.value = !isLoggedIn

        if (isLoggedIn) {
            loadChats()
        } else {
            clearChats()
        }
    }

    fun loadChats() {
        if (!UserSession.isLoggedIn()) {
            _shouldShowGuestState.value = true
            clearChats()
            return
        }

        _isLoading.value = true
        _shouldShowGuestState.value = false

        viewModelScope.launch {
            try {
                val chats = withContext(Dispatchers.IO) {
                    chatRepository.getChatsForCurrentUser()
                }

                _allChats.value = chats
                _chatCount.value = chats.size
                _unreadCount.value = chats.count { it.isUnread }

            } catch (e: Exception) {
                _showErrorMessage.value = "Failed to load chats: ${e.message}"
                clearChats()
            } finally {
                _isLoading.value = false
            }
        }
    }

    fun refreshChats() {
        if (!UserSession.isLoggedIn()) return

        viewModelScope.launch {
            try {
                withContext(Dispatchers.IO) {
                    chatRepository.forceRefreshChats()
                }
                loadChats()
            } catch (e: Exception) {
                _showErrorMessage.value = "Failed to refresh chats: ${e.message}"
            }
        }
    }

    private fun clearChats() {
        _allChats.value = emptyList()
        _chatCount.value = 0
        _unreadCount.value = 0
    }

    fun markChatAsRead(chatId: String) {
        viewModelScope.launch {
            try {
                withContext(Dispatchers.IO) {
                    chatRepository.markChatAsReadById(chatId)
                }

                loadChats()
            } catch (e: Exception) {
                _showErrorMessage.value = "Failed to mark chat as read: ${e.message}"
            }
        }
    }

    fun filterChats(query: String) {
        searchQuery.set(query)
        val currentChats = _allChats.value ?: return

        if (query.isEmpty()) {

            return
        }

        val filteredChats = currentChats.filter { chat ->
            chat.companyName.contains(query, ignoreCase = true) ||
            chat.recruiterName.contains(query, ignoreCase = true) ||
            chat.jobTitle.contains(query, ignoreCase = true) ||
            chat.lastMessage.contains(query, ignoreCase = true)
        }

        _allChats.value = filteredChats
    }

    fun getUserRoleDisplayText(): String {
        return when (UserSession.getUserRole()) {
            UserSession.ROLE_FREELANCER -> "Freelancer"
            UserSession.ROLE_BUSINESS_OWNER -> "Business Owner"
            else -> "Guest"
        }
    }

    fun getChatCountText(): String {
        val count = _chatCount.value ?: 0
        return when (count) {
            0 -> "No active chats"
            1 -> "1 active chat"
            else -> "$count active chats"
        }
    }

    fun getUnreadCountText(): String {
        val count = _unreadCount.value ?: 0
        return when (count) {
            0 -> "All caught up!"
            1 -> "1 unread message"
            else -> "$count unread messages"
        }
    }

    fun shouldShowEmptyState(): Boolean {
        val chats = _allChats.value
        return chats.isNullOrEmpty() && _isLoggedIn.value == true && _isLoading.value != true
    }


    override fun onChatsUpdated(chats: List<ChatModel>) {

        if (_isLoggedIn.value == true) {
            viewModelScope.launch {
                loadChats()
            }
        }
    }


    fun navigateToLogin(): Boolean {
        if (!UserSession.isLoggedIn()) {
            _showSuccessMessage.value = "Please log in to access chat features"
            return true
        }
        return false
    }


    fun onSuccessMessageShown() {
        _showSuccessMessage.value = null
    }

    fun onErrorMessageShown() {
        _showErrorMessage.value = null
    }


    fun handleChatApproval(@Suppress("UNUSED_PARAMETER") chat: ChatModel, isApproved: Boolean) {
        if (UserSession.getUserRole() != UserSession.ROLE_BUSINESS_OWNER) {
            _showErrorMessage.value = "Only business owners can approve chats"
            return
        }

        viewModelScope.launch {
            try {


                val action = if (isApproved) "approved" else "rejected"
                _showSuccessMessage.value = "Chat $action successfully"
                refreshChats()
            } catch (e: Exception) {
                _showErrorMessage.value = "Failed to update chat: ${e.message}"
            }
        }
    }
}