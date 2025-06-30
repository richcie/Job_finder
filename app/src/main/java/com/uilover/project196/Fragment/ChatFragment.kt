package com.uilover.project196.Fragment

import android.content.Intent
import android.content.BroadcastReceiver
import android.content.Context
import android.content.IntentFilter
import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.Toast
import androidx.fragment.app.Fragment
import androidx.lifecycle.lifecycleScope
import androidx.lifecycle.ViewModelProvider
import androidx.recyclerview.widget.LinearLayoutManager
import kotlinx.coroutines.launch
import com.uilover.project196.Activity.ChatConversationActivity
import com.uilover.project196.Activity.MainActivity
import com.uilover.project196.Adapter.ChatAdapter
import com.uilover.project196.Model.ChatModel
import com.uilover.project196.Repository.ChatRepository
import com.uilover.project196.Utils.UserSession
import com.uilover.project196.ViewModel.ChatViewModel
import com.uilover.project196.databinding.FragmentChatBinding

// KRITERIA WAJIB: Multiple Fragment (9/16) - Fragment daftar chat
class ChatFragment : BaseFragment(), ChatRepository.ChatUpdateListener, ChatAdapter.ClickListener {
    private var _binding: FragmentChatBinding? = null
    private val binding get() = _binding!!
    private lateinit var chatAdapter: ChatAdapter
    private var allChats = mutableListOf<ChatModel>()
    private lateinit var chatRepository: ChatRepository
    private lateinit var chatViewModel: ChatViewModel


    private val messageReceiver = object : BroadcastReceiver() {
        override fun onReceive(context: Context?, intent: Intent?) {
            when (intent?.action) {
                "com.uilover.project196.MESSAGE_SENT" -> {
                    val chatId = intent.getStringExtra("chat_id")
                    val senderId = intent.getStringExtra("sender_id")

                    android.util.Log.d("ChatFragment", "Received MESSAGE_SENT broadcast - chatId: $chatId, sender: $senderId")


                    if (UserSession.isLoggedIn() && ::chatRepository.isInitialized) {
                        android.util.Log.d("ChatFragment", "Force refreshing chat list due to new message")
                        lifecycleScope.launch {

                            kotlinx.coroutines.delay(100)
                            chatRepository.forceRefreshChats()
                            loadAllChats()
                        }
                    }
                }
                "com.uilover.project196.CHAT_CREATED" -> {
                    val chatId = intent.getStringExtra("chat_id")
                    val freelancerId = intent.getStringExtra("freelancer_id")
                    val businessOwnerId = intent.getStringExtra("business_owner_id")

                    android.util.Log.d("ChatFragment", "Received CHAT_CREATED broadcast - chatId: $chatId, freelancer: $freelancerId, businessOwner: $businessOwnerId")


                    if (UserSession.isLoggedIn() && ::chatRepository.isInitialized) {
                        val currentUserId = UserSession.getUserId()


                        if (currentUserId == freelancerId || currentUserId == businessOwnerId) {
                            android.util.Log.d("ChatFragment", "Force refreshing chat list due to new chat creation involving current user")
                            lifecycleScope.launch {

                                kotlinx.coroutines.delay(200)

                                chatRepository.forceRefreshChats()
                                loadAllChats()
                            }
                        }
                    }
                }
                "com.uilover.project196.FREELANCER_SHORTLISTED" -> {
                    val freelancerId = intent.getStringExtra("freelancer_id")
                    val businessOwnerId = intent.getStringExtra("business_owner_id")

                    android.util.Log.d("ChatFragment", "Received FREELANCER_SHORTLISTED broadcast - freelancer: $freelancerId, businessOwner: $businessOwnerId")


                    if (UserSession.isLoggedIn() && ::chatRepository.isInitialized) {
                        val currentUserId = UserSession.getUserId()


                        if (currentUserId == freelancerId && UserSession.getUserRole() == UserSession.ROLE_FREELANCER) {
                            android.util.Log.d("ChatFragment", "Current freelancer was shortlisted - force refreshing chat list")
                            lifecycleScope.launch {

                                kotlinx.coroutines.delay(300)

                                chatRepository.forceRefreshChats()
                                loadAllChats()
                            }
                        }
                    }
                }
                "com.uilover.project196.PROFILE_UPDATED" -> {
                    val userId = intent.getStringExtra("user_id")
                    val updatedField = intent.getStringExtra("updated_field")

                    android.util.Log.d("ChatFragment", "Received PROFILE_UPDATED broadcast - userId: $userId, field: $updatedField")


                    if (UserSession.isLoggedIn() && ::chatRepository.isInitialized && updatedField == "profession") {
                        android.util.Log.d("ChatFragment", "Force refreshing chat list due to profession update")
                        lifecycleScope.launch {

                            kotlinx.coroutines.delay(150)

                            chatRepository.clearCacheOnProfileUpdate(userId ?: "")
                            loadAllChats()
                        }
                    }
                }
            }
        }
    }

    override fun onCreateView(
        inflater: LayoutInflater,
        container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View {
        _binding = FragmentChatBinding.inflate(inflater, container, false)
        return binding.root
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)


        chatRepository = ChatRepository.getInstance(requireContext())


        chatViewModel = ViewModelProvider(
            this,
            ViewModelProvider.AndroidViewModelFactory.getInstance(requireActivity().application)
        )[ChatViewModel::class.java]


        setupObservers()


        setupNotificationButton(binding.imageView3, binding.textView4)


        chatViewModel.refreshLoginState()
    }

    private fun setupObservers() {

        chatViewModel.allChats.observe(viewLifecycleOwner) { chatList ->
            allChats.clear()
            allChats.addAll(chatList)
            if (::chatAdapter.isInitialized) {
                chatAdapter.updateData(allChats)
            } else {
                setupRecyclerView()
            }
            updateUI()
        }


        chatViewModel.isLoading.observe(viewLifecycleOwner) { _ ->


        }


        chatViewModel.isLoggedIn.observe(viewLifecycleOwner) { isLoggedIn ->
            if (isLoggedIn) {
                initializeChat()
                if (::chatRepository.isInitialized) {
                    chatRepository.addListener(this)
                }
            } else {
                showGuestState()
                if (::chatRepository.isInitialized) {
                    chatRepository.removeListener(this)
                }
            }
        }


        chatViewModel.shouldShowGuestState.observe(viewLifecycleOwner) { shouldShow ->
            if (shouldShow) {
                showGuestState()
            }
        }


        chatViewModel.showSuccessMessage.observe(viewLifecycleOwner) { message ->
            message?.let {
                Toast.makeText(requireContext(), it, Toast.LENGTH_SHORT).show()
                chatViewModel.onSuccessMessageShown()
            }
        }


        chatViewModel.showErrorMessage.observe(viewLifecycleOwner) { message ->
            message?.let {
                Toast.makeText(requireContext(), it, Toast.LENGTH_SHORT).show()
                chatViewModel.onErrorMessageShown()
            }
        }


        chatViewModel.chatCount.observe(viewLifecycleOwner) { count ->
            updateChatCountDisplay(count)
        }


        chatViewModel.unreadCount.observe(viewLifecycleOwner) { count ->
            updateUnreadCountDisplay(count)
        }
    }

    private fun initializeChat() {

        loadAllChats()
        setupRecyclerView()
        updateUI()
    }

    private fun showGuestState() {

        binding.recyclerViewChats.visibility = View.GONE
        binding.emptyStateLayout.visibility = View.GONE
        binding.guestLoginLayout.visibility = View.VISIBLE


        binding.pendingCount.visibility = View.GONE
        binding.subtitleText.text = "Please log in to access chat features"


        binding.loginPromptButton.setOnClickListener {

            (requireActivity() as? MainActivity)?.let { mainActivity ->

                mainActivity.binding.viewPager.currentItem = 4
            }
        }
    }

    private fun loadAllChats() {

        chatViewModel.loadChats()
    }

    private fun updateChatCountDisplay(@Suppress("UNUSED_PARAMETER") count: Int) {

        binding.subtitleText.text = chatViewModel.getChatCountText()
    }

    private fun updateUnreadCountDisplay(count: Int) {

        if (count > 0) {
            binding.pendingCount.visibility = View.VISIBLE
            binding.pendingCount.text = count.toString()
        } else {
            binding.pendingCount.visibility = View.GONE
        }
    }

    private fun setupRecyclerView() {

        if (!UserSession.isLoggedIn()) {
            return
        }

        binding.recyclerViewChats.layoutManager = LinearLayoutManager(requireContext())
        chatAdapter = ChatAdapter(allChats, this)
        binding.recyclerViewChats.adapter = chatAdapter
    }

    private fun updateUI() {

        if (!UserSession.isLoggedIn()) {
            showGuestState()
            return
        }

        val totalCount = allChats.size
        val userRole = UserSession.getUserRole()





        if (totalCount == 0) {
            binding.recyclerViewChats.visibility = View.GONE
            binding.emptyStateLayout.visibility = View.VISIBLE
            binding.guestLoginLayout.visibility = View.GONE
        } else {
            binding.recyclerViewChats.visibility = View.VISIBLE
            binding.emptyStateLayout.visibility = View.GONE
            binding.guestLoginLayout.visibility = View.GONE
        }


        when (userRole) {
            UserSession.ROLE_FREELANCER -> {
                binding.subtitleText.text = if (totalCount > 0) {
                    "$totalCount active conversation${if (totalCount != 1) "s" else ""}"
                } else {
                    "Apply to jobs to start conversations with employers"
                }
            }
            UserSession.ROLE_BUSINESS_OWNER -> {
                binding.subtitleText.text = if (totalCount > 0) {
                    "$totalCount chat${if (totalCount != 1) "s" else ""} with selected candidates"
                } else {
                    "Shortlist candidates to start conversations"
                }
            }
            else -> {
                binding.subtitleText.text = "Connect with others"
            }
        }
    }


    override fun onChatClick(chat: ChatModel) {

        if (chatViewModel.navigateToLogin()) {
            return
        }


        chatViewModel.markChatAsRead(chat.id)


        val intent = Intent(requireContext(), ChatConversationActivity::class.java).apply {
            putExtra("CHAT_ID", chat.id)
            putExtra("COMPANY_NAME", chat.companyName)
            putExtra("COMPANY_LOGO", chat.companyLogo)
            putExtra("RECRUITER_NAME", chat.recruiterName)
            putExtra("JOB_TITLE", chat.jobTitle)
            putExtra("INITIAL_MESSAGE", chat.lastMessage)
        }
        startActivity(intent)
    }

    override fun onApproveChat(chat: ChatModel) {

    }

    override fun onRejectChat(chat: ChatModel) {

    }


    override fun onChatsUpdated(chats: List<ChatModel>) {

        if (_binding != null && UserSession.isLoggedIn()) {
            loadAllChats()

            if (::chatAdapter.isInitialized) {
                chatAdapter.updateData(allChats)
            }
            updateUI()
        }
    }

    override fun onResume() {
        super.onResume()


        val intentFilter = IntentFilter().apply {
            addAction("com.uilover.project196.MESSAGE_SENT")
            addAction("com.uilover.project196.CHAT_CREATED")
            addAction("com.uilover.project196.FREELANCER_SHORTLISTED")
            addAction("com.uilover.project196.PROFILE_UPDATED")
        }
        if (android.os.Build.VERSION.SDK_INT >= android.os.Build.VERSION_CODES.TIRAMISU) {
            requireContext().registerReceiver(messageReceiver, intentFilter, Context.RECEIVER_NOT_EXPORTED)
        } else {
            requireContext().registerReceiver(messageReceiver, intentFilter)
        }
        android.util.Log.d("ChatFragment", "Registered message, chat creation, and profile update broadcast receiver")


        refreshBasedOnLoginState()


        if (UserSession.isLoggedIn() && ::chatRepository.isInitialized) {
            android.util.Log.d("ChatFragment", "Fragment resumed - refreshing chat list")
            lifecycleScope.launch {

                kotlinx.coroutines.delay(100)
                loadAllChats()
            }
        }
    }

    override fun onLoginStateRefresh(isLoggedIn: Boolean) {

        chatViewModel.refreshLoginState()
    }

    private fun refreshBasedOnLoginState() {


        chatViewModel.refreshLoginState()
    }

    override fun onPause() {
        super.onPause()


        try {
            requireContext().unregisterReceiver(messageReceiver)
            android.util.Log.d("ChatFragment", "Unregistered message, chat creation, and profile update broadcast receiver")
        } catch (e: Exception) {
            android.util.Log.w("ChatFragment", "Error unregistering message receiver: ${e.message}")
        }


        if (::chatRepository.isInitialized) {
            chatRepository.removeListener(this)
        }
    }

    override fun onDestroyView() {
        super.onDestroyView()
        if (::chatRepository.isInitialized) {
            chatRepository.removeListener(this)
        }
        _binding = null
    }
}