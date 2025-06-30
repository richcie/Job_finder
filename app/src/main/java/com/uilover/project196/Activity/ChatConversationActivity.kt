package com.uilover.project196.Activity

import android.os.Bundle
import android.widget.Toast
import androidx.appcompat.app.AppCompatActivity
import androidx.recyclerview.widget.LinearLayoutManager
import androidx.lifecycle.lifecycleScope
import androidx.lifecycle.ViewModelProvider
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import kotlinx.coroutines.Dispatchers
import com.uilover.project196.Adapter.MessageAdapter
import com.uilover.project196.Model.MessageModel
import com.uilover.project196.R
import com.uilover.project196.Repository.ChatRepository
import com.uilover.project196.Utils.UserSession
import com.uilover.project196.ViewModel.ChatConversationViewModel
import com.uilover.project196.databinding.ActivityChatConversationBinding
import com.uilover.project196.Database.AppDatabase
import android.view.View
import androidx.core.content.ContextCompat
import org.json.JSONObject
import java.util.*
// KRITERIA: Komunikasi antar activity menggunakan Intent
import android.content.Intent
import android.content.BroadcastReceiver
import android.content.Context
import android.content.IntentFilter



// KRITERIA: Multiple Activity (5/8) - Activity untuk percakapan chat
// KRITERIA WAJIB: Multiple Activity (5/8) - Activity untuk percakapan chat
// KRITERIA WAJIB: Komunikasi antar Activity menggunakan Intent
class ChatConversationActivity : AppCompatActivity(), UserSession.ProfessionChangeListener {

    private lateinit var binding: ActivityChatConversationBinding
    private lateinit var messageAdapter: MessageAdapter
    private var messages = mutableListOf<MessageModel>()
    private lateinit var chatRepository: ChatRepository
    private lateinit var chatConversationViewModel: ChatConversationViewModel


    private var chatId: String = ""
    private var companyName: String = ""
    private var companyLogo: String = ""
    private var recruiterName: String = ""
    private var jobTitle: String = ""
    private var initialMessage: String = ""
    private var jobId: String = ""

    private val verificationResetReceiver = object : BroadcastReceiver() {
        // KRITERIA: Komunikasi antar activity menggunakan Intent
        override fun onReceive(context: Context?, intent: Intent?) {
            if (intent?.action == "com.uilover.project196.VERIFICATION_STATUS_RESET") {
                val resetFreelancerId = intent.getStringExtra("freelancer_id")
                val currentUserId = UserSession.getUserId()


                if (resetFreelancerId == currentUserId) {
                    android.util.Log.d("ChatConversationActivity", "Received verification reset broadcast for current user - refreshing button state")
                    setupVerificationButton()
                }
            }
        }
    }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        binding = ActivityChatConversationBinding.inflate(layoutInflater)
        setContentView(binding.root)


        // KRITERIA: Komunikasi antar activity menggunakan Intent
        val intentFilter = IntentFilter("com.uilover.project196.VERIFICATION_STATUS_RESET")
        if (android.os.Build.VERSION.SDK_INT >= android.os.Build.VERSION_CODES.TIRAMISU) {
            registerReceiver(verificationResetReceiver, intentFilter, Context.RECEIVER_NOT_EXPORTED)
        } else {
            registerReceiver(verificationResetReceiver, intentFilter)
        }


        UserSession.addProfessionChangeListener(this)


        chatRepository = ChatRepository.getInstance(this)


        chatConversationViewModel = ViewModelProvider(
            this,
            ViewModelProvider.AndroidViewModelFactory.getInstance(application)
        )[ChatConversationViewModel::class.java]

        getChatDetails()
        setupObservers()
        setupUI()
        setupRecyclerView()
        setupMessageInput()
        setupVerificationButton()


        chatConversationViewModel.loadMessages(chatId)


        lifecycleScope.launch {
            try {
                withContext(Dispatchers.IO) {
                    chatRepository.markMessagesAsRead(chatId)
                }
                android.util.Log.d("ChatConversationActivity", "Messages marked as read for chat $chatId")
            } catch (e: Exception) {
                android.util.Log.e("ChatConversationActivity", "Error marking messages as read", e)
            }
        }
    }

    private fun getChatDetails() {
        chatId = intent.getStringExtra("CHAT_ID") ?: ""
        companyName = intent.getStringExtra("COMPANY_NAME") ?: ""
        companyLogo = intent.getStringExtra("COMPANY_LOGO") ?: ""
        recruiterName = intent.getStringExtra("RECRUITER_NAME") ?: ""
        jobTitle = intent.getStringExtra("JOB_TITLE") ?: ""
        initialMessage = intent.getStringExtra("INITIAL_MESSAGE") ?: ""
        jobId = intent.getStringExtra("JOB_ID") ?: ""


        chatConversationViewModel.setChatDetails(chatId, companyName, recruiterName, jobTitle)
    }

    private fun setupObservers() {

        chatConversationViewModel.messages.observe(this) { messageList ->
            messages.clear()
            messages.addAll(messageList)
            messageAdapter.updateMessages(messages)
        }


        chatConversationViewModel.isLoading.observe(this) { _ ->


        }


        chatConversationViewModel.isSendingMessage.observe(this) { isSending ->
            binding.sendButton.isEnabled = !isSending
            binding.editTextMessage.isEnabled = !isSending
            binding.editTextMessage.alpha = if (isSending) 0.6f else 1.0f
        }


        chatConversationViewModel.showSuccessMessage.observe(this) { message ->
            message?.let {
                Toast.makeText(this, it, Toast.LENGTH_SHORT).show()
                chatConversationViewModel.onSuccessMessageShown()
            }
        }


        chatConversationViewModel.showErrorMessage.observe(this) { message ->
            message?.let {
                Toast.makeText(this, it, Toast.LENGTH_SHORT).show()
                chatConversationViewModel.onErrorMessageShown()
            }
        }


        chatConversationViewModel.scrollToBottom.observe(this) { shouldScroll ->
            if (shouldScroll && messages.isNotEmpty()) {
                binding.recyclerViewMessages.scrollToPosition(messages.size - 1)
                chatConversationViewModel.onScrolledToBottom()
            }
        }


        chatConversationViewModel.chatTitle.observe(this) { title ->

            supportActionBar?.title = title
        }


        chatConversationViewModel.isLoggedIn.observe(this) { isLoggedIn ->
            if (!isLoggedIn) {
                Toast.makeText(this, "Please log in to continue", Toast.LENGTH_SHORT).show()
                finish()
            }
        }


        chatConversationViewModel.messageInput.addOnPropertyChangedCallback(
            object : androidx.databinding.Observable.OnPropertyChangedCallback() {
                override fun onPropertyChanged(sender: androidx.databinding.Observable?, propertyId: Int) {
                    val viewModelText = chatConversationViewModel.messageInput.get() ?: ""
                    val editTextText = binding.editTextMessage.text.toString()


                    if (viewModelText != editTextText) {
                        binding.editTextMessage.setText(viewModelText)

                        binding.editTextMessage.setSelection(viewModelText.length)
                    }
                }
            }
        )
    }

    private fun setupUI() {

        if (UserSession.getUserRole() == UserSession.ROLE_FREELANCER) {

            binding.companyName.text = companyName
            binding.recruiterName.text = "$recruiterName • $jobTitle"
        } else {

            binding.companyName.text = companyName


            lifecycleScope.launch {
                val freelancerProfession = withContext(Dispatchers.IO) {
                    try {

                        val applicationId = chatId.removePrefix("chat_").toIntOrNull()
                        if (applicationId != null) {
                            val jobApplicationDao = com.uilover.project196.Database.AppDatabase.getDatabase(this@ChatConversationActivity).jobApplicationDao()
                            val userDao = com.uilover.project196.Database.AppDatabase.getDatabase(this@ChatConversationActivity).userDao()

                            val application = jobApplicationDao.getApplicationById(applicationId)
                            val freelancer = application?.let { userDao.getUserById(it.applicantUserId) }
                            freelancer?.title
                        } else null
                    } catch (e: Exception) {
                        android.util.Log.e("ChatConversationActivity", "Error getting freelancer profession", e)
                        null
                    }
                }

                if (!freelancerProfession.isNullOrEmpty()) {
                    binding.recruiterName.text = "$recruiterName • $freelancerProfession"
                } else {
                    binding.recruiterName.text = "$recruiterName • $jobTitle"
                }
            }
        }


        val logoResource = when (companyLogo) {
            "logo1" -> R.drawable.logo1
            "logo2" -> R.drawable.logo2
            "logo3" -> R.drawable.logo3
            "logo4" -> R.drawable.logo4
            else -> R.drawable.logo1
        }
        binding.companyLogo.setImageResource(logoResource)


        binding.backButton.setOnClickListener {
            finish()
        }
    }

    private fun setupVerificationButton() {
        val currentUserRole = UserSession.getUserRole()
        val currentUserId = UserSession.getUserId()

        android.util.Log.d("ChatConversationActivity", "=== VERIFICATION BUTTON SETUP ===")
        android.util.Log.d("ChatConversationActivity", "User role: $currentUserRole, User ID: $currentUserId")

        if (currentUserRole == UserSession.ROLE_FREELANCER && currentUserId != null) {
            binding.verificationRequestButton.visibility = View.VISIBLE
            binding.verificationText.visibility = View.VISIBLE


            lifecycleScope.launch {
                val isVerified = withContext(Dispatchers.IO) {
                    val verified = chatRepository.hasAcceptedVerification(currentUserId)
                    android.util.Log.d("ChatConversationActivity", "Comprehensive verification check result: $verified")
                    android.util.Log.d("ChatConversationActivity", "Note: Verification includes both shortlisted applications AND chat-based verification")
                    verified
                }

                android.util.Log.d("ChatConversationActivity", "Is freelancer verified (shortlisted OR chat): $isVerified")

                if (isVerified) {
                    binding.verificationRequestButton.isEnabled = false
                    binding.verificationRequestButton.alpha = 0.6f
                    binding.verificationText.text = "Already Verified"
                    binding.verificationText.setTextColor(ContextCompat.getColor(this@ChatConversationActivity, R.color.darkGrey))
                    android.util.Log.d("ChatConversationActivity", "✅ Verification button disabled - freelancer is verified through job acceptance")
                } else {

                    binding.verificationRequestButton.isEnabled = true
                    binding.verificationRequestButton.alpha = 1.0f
                    binding.verificationText.text = "Request Verified Status"
                    binding.verificationText.setTextColor(ContextCompat.getColor(this@ChatConversationActivity, R.color.green))
                    android.util.Log.d("ChatConversationActivity", "❌ Verification button enabled - freelancer needs verification")
                }

                binding.verificationRequestButton.setOnClickListener {
                    showFreelancerVerificationConfirmationDialog()
                }
            }
        } else {
            binding.verificationRequestButton.visibility = View.GONE
            binding.verificationText.visibility = View.GONE
            android.util.Log.d("ChatConversationActivity", "Verification button hidden - not a freelancer or not logged in")
        }
    }


    @Suppress("UNUSED_PARAMETER")
    private fun checkForVerificationRequestCommand(messageText: String): Boolean {

        return false
    }

    private fun showFreelancerVerificationConfirmationDialog() {
        val builder = androidx.appcompat.app.AlertDialog.Builder(this)
        builder.setTitle("Request Verification")
        builder.setMessage("Are you sure you want to request verified freelancer status? This will:\n\n• Send a verification request to the business owner\n• Allow them to review your profile\n• If approved, grant you access to work on their jobs\n• Enable attendance tracking and check-in/out features\n\nThe business owner will be able to accept or reject your request.")
        builder.setIcon(R.drawable.fav)

        builder.setPositiveButton("SEND REQUEST") { _, _ ->
            sendVerificationRequest()
        }

        builder.setNegativeButton("CANCEL") { dialog, _ ->
            dialog.dismiss()
        }

        val dialog = builder.create()
        dialog.show()


        dialog.getButton(androidx.appcompat.app.AlertDialog.BUTTON_POSITIVE)?.setTextColor(getColor(R.color.green))
        dialog.getButton(androidx.appcompat.app.AlertDialog.BUTTON_NEGATIVE)?.setTextColor(getColor(R.color.darkGrey))
    }

    private fun sendVerificationRequest() {

        binding.verificationRequestButton.isEnabled = false
        binding.verificationText.text = "Sending request..."


        chatConversationViewModel.messageInput.set("/verify")
        chatConversationViewModel.sendMessage()


        binding.verificationText.text = "Request Pending"
        binding.verificationText.setTextColor(ContextCompat.getColor(this, R.color.orange))
    }



    private fun checkForJobRequestCommand(messageText: String): Boolean {
        if (messageText.uppercase() == "REQUEST JOB") {
            val currentUserId = UserSession.getUserId()
            val currentUserRole = UserSession.getUserRole()

            if (currentUserId == null) {
                Toast.makeText(this, "Please log in to send job request", Toast.LENGTH_SHORT).show()
                return true
            }

            if (currentUserRole != UserSession.ROLE_FREELANCER) {
                Toast.makeText(this, "Only freelancers can request jobs", Toast.LENGTH_SHORT).show()
                return true
            }

            sendJobRequest()
            return true
        }
        return false
    }

    private fun setupJobRequestButton() {


    }

    private fun sendJobRequest() {


        chatConversationViewModel.messageInput.set("/request")
        chatConversationViewModel.sendMessage()
    }

    private fun handleJobRequestAction(message: MessageModel, isAccept: Boolean) {
        lifecycleScope.launch {
            try {
                val newStatus = if (isAccept) MessageModel.STATUS_ACCEPTED else MessageModel.STATUS_REJECTED

                val success = withContext(Dispatchers.IO) {
                    chatRepository.updateJobRequestStatus(message.id, newStatus)
                }

                if (success) {
                    val actionText = if (isAccept) "accepted" else "rejected"
                    Toast.makeText(this@ChatConversationActivity, "Job request $actionText", Toast.LENGTH_SHORT).show()
                    loadMessagesFromDatabase()


                } else {
                    Toast.makeText(this@ChatConversationActivity, "Failed to update job request", Toast.LENGTH_SHORT).show()
                }
            } catch (e: Exception) {
                Toast.makeText(this@ChatConversationActivity, "Error updating job request: ${e.message}", Toast.LENGTH_SHORT).show()
            }
        }
    }

    private fun handleVerificationAction(message: MessageModel, isAccept: Boolean) {

        showVerificationConfirmationDialog(message, isAccept)
    }

    private fun showVerificationConfirmationDialog(message: MessageModel, isAccept: Boolean) {
        val builder = androidx.appcompat.app.AlertDialog.Builder(this)


        val freelancerName = try {
            val freelancerInfo = org.json.JSONObject(message.freelancerData ?: "{}")
            freelancerInfo.optString("name", "this freelancer")
        } catch (e: Exception) {
            "this freelancer"
        }

        if (isAccept) {
            builder.setTitle("Accept Verification Request")
            builder.setMessage("Are you sure you want to verify $freelancerName? This will:\n\n• Grant them verified freelancer status\n• Allow them to check in/out for work\n• Enable attendance tracking\n• Add them to your candidates progress\n\nThis action will give them access to work on your jobs.")
            builder.setIcon(R.drawable.fav)
        } else {
            builder.setTitle("Reject Verification Request")
            builder.setMessage("Are you sure you want to reject $freelancerName's verification request? This will:\n\n• Deny them verified status\n• Prevent them from working on your jobs\n• Block their attendance access\n\nThey can submit a new verification request later.")
            builder.setIcon(R.drawable.ic_block)
        }

        val positiveButtonText = if (isAccept) "VERIFY" else "REJECT"
        val positiveButtonColor = if (isAccept) R.color.green else R.color.red

        builder.setPositiveButton(positiveButtonText) { _, _ ->
            performVerificationAction(message, isAccept)
        }

        builder.setNegativeButton("CANCEL") { dialog, _ ->
            dialog.dismiss()
        }

        val dialog = builder.create()
        dialog.show()


        dialog.getButton(androidx.appcompat.app.AlertDialog.BUTTON_POSITIVE)?.setTextColor(getColor(positiveButtonColor))
        dialog.getButton(androidx.appcompat.app.AlertDialog.BUTTON_NEGATIVE)?.setTextColor(getColor(R.color.darkGrey))
    }

    private fun performVerificationAction(message: MessageModel, isAccept: Boolean) {
        lifecycleScope.launch {
            try {
                val newStatus = if (isAccept) MessageModel.STATUS_ACCEPTED else MessageModel.STATUS_REJECTED

                val success = withContext(Dispatchers.IO) {
                    val updateSuccess = chatRepository.updateVerificationStatus(message.id, newStatus)


                    if (isAccept && updateSuccess) {
                        try {
                            val freelancerId = message.senderId
                            val currentBusinessOwnerId = UserSession.getUserId()

                            android.util.Log.d("ChatConversationActivity", "=== VERIFICATION ACCEPTANCE DEBUG ===")
                            android.util.Log.d("ChatConversationActivity", "Freelancer ID: $freelancerId")
                            android.util.Log.d("ChatConversationActivity", "Business Owner ID: $currentBusinessOwnerId")
                            android.util.Log.d("ChatConversationActivity", "Message ID: ${message.id}")
                            android.util.Log.d("ChatConversationActivity", "Chat ID: ${message.chatId}")


                            val prefs = getSharedPreferences("app_state", MODE_PRIVATE)
                            prefs.edit().putBoolean("attendance_disabled", false).apply()


                            val freelancerName = try {
                                val freelancerInfo = org.json.JSONObject(message.freelancerData ?: "{}")
                                freelancerInfo.optString("name", "Unknown Freelancer")
                            } catch (e: Exception) {
                                "Unknown Freelancer"
                            }


                            val attendanceManager = com.uilover.project196.Utils.AttendanceManager.getInstance(this@ChatConversationActivity)
                            attendanceManager.resetAttendanceDataOnVerificationAccepted(freelancerId)



                            val database = com.uilover.project196.Database.AppDatabase.getDatabase(this@ChatConversationActivity)
                            val jobDao = database.jobDao()
                            val ownedJobs = jobDao.getJobsByOwnerId(currentBusinessOwnerId ?: "")

                            android.util.Log.d("ChatConversationActivity", "Creating attendance records for ${ownedJobs.size} jobs owned by $currentBusinessOwnerId")

                            var attendanceCreated = 0
                            for (job in ownedJobs) {
                                attendanceManager.createInitialAttendanceForNewFreelancer(job.id, freelancerId)
                                attendanceCreated++
                                android.util.Log.d("ChatConversationActivity", "Created attendance for job ${job.id} - ${job.title}")
                            }

                            android.util.Log.d("ChatConversationActivity", "✅ Total attendance records created: $attendanceCreated")


                            val verifiedMessage = database.messageDao().getAcceptedVerificationForFreelancer(freelancerId)
                            android.util.Log.d("ChatConversationActivity", "Verification saved check: ${if (verifiedMessage != null) "YES" else "NO"}")

                            android.util.Log.d("ChatConversationActivity", "Verification accepted for freelancer $freelancerId ($freelancerName) - attendance re-enabled and data reset")


                            val congratulationsSuccess = chatRepository.createCongratulationsMessageForVerifiedFreelancer(
                                chatId = message.chatId,
                                freelancerId = freelancerId,
                                businessOwnerId = currentBusinessOwnerId ?: ""
                            )

                            if (congratulationsSuccess) {
                                android.util.Log.d("ChatConversationActivity", "✅ Created automatic congratulations message for verified freelancer $freelancerName")
                            } else {
                                android.util.Log.w("ChatConversationActivity", "⚠️ Failed to create congratulations message for verified freelancer $freelancerName")
                            }


                            // KRITERIA: Komunikasi antar activity menggunakan Intent
                            val intent = Intent("com.uilover.project196.VERIFICATION_ACCEPTED")
                            intent.putExtra("freelancer_id", freelancerId)
                            intent.putExtra("freelancer_name", freelancerName)
                            intent.putExtra("attendance_reset", true)
                            sendBroadcast(intent)


                            // KRITERIA: Komunikasi antar activity menggunakan Intent
                            val candidatesProgressIntent = Intent("com.uilover.project196.CANDIDATE_HIRED")
                            candidatesProgressIntent.putExtra("freelancer_id", freelancerId)
                            candidatesProgressIntent.putExtra("candidate_name", freelancerName)
                            sendBroadcast(candidatesProgressIntent)

                            android.util.Log.d("ChatConversationActivity", "✅ Broadcasts sent for UI refresh")

                        } catch (e: Exception) {
                            android.util.Log.e("ChatConversationActivity", "Error re-enabling attendance after verification", e)
                        }
                    }

                    updateSuccess
                }

                if (success) {
                    val actionText = if (isAccept) "verified" else "rejected"

                    if (isAccept) {
                        Toast.makeText(this@ChatConversationActivity, "Freelancer verification approved! 🎉 Congratulations message sent.", Toast.LENGTH_LONG).show()
                    } else {
                        Toast.makeText(this@ChatConversationActivity, "Freelancer verification $actionText", Toast.LENGTH_SHORT).show()
                    }


                    loadMessagesFromDatabase()


                    if (isAccept) {
                        binding.verificationRequestButton.isEnabled = false
                        binding.verificationRequestButton.alpha = 0.6f
                        binding.verificationText.text = "Already Verified"
                        binding.verificationText.setTextColor(ContextCompat.getColor(this@ChatConversationActivity, R.color.darkGrey))
                    } else {

                        binding.verificationRequestButton.isEnabled = true
                        binding.verificationRequestButton.alpha = 1.0f
                        binding.verificationText.text = "Request Verified Status"
                        binding.verificationText.setTextColor(ContextCompat.getColor(this@ChatConversationActivity, R.color.green))
                    }
                } else {
                    Toast.makeText(this@ChatConversationActivity, "Failed to update verification request", Toast.LENGTH_SHORT).show()
                }
            } catch (e: Exception) {
                Toast.makeText(this@ChatConversationActivity, "Error updating verification request: ${e.message}", Toast.LENGTH_SHORT).show()
            }
        }
    }



    private fun loadMessagesFromDatabase() {
        lifecycleScope.launch {
            try {

                val loadedMessages = withContext(Dispatchers.IO) {
                    chatRepository.getMessagesForChat(chatId)
                }


                messages.clear()
                messages.addAll(loadedMessages)
                messageAdapter.updateMessages(messages)


                if (messages.isNotEmpty()) {
                    binding.recyclerViewMessages.scrollToPosition(messages.size - 1)
                }


                withContext(Dispatchers.IO) {
                    chatRepository.markMessagesAsRead(chatId)
                }
            } catch (e: Exception) {
                android.util.Log.e("ChatConversationActivity", "Error loading messages", e)
            }
        }
    }

    private fun getRandomTechnology(): String {
        val technologies = listOf("React", "Kotlin", "Python", "Java", "Swift", "Node.js", "Angular", "Vue.js")
        return technologies.random()
    }

    private fun setupRecyclerView() {
        val layoutManager = LinearLayoutManager(this)
        binding.recyclerViewMessages.layoutManager = layoutManager
        messageAdapter = MessageAdapter(
            messages = messages,
            onJobRequestAction = { message, isAccept ->

                chatConversationViewModel.handleJobRequestAction(message, isAccept)
            },
            onVerificationAction = { message, isAccept ->

                handleVerificationAction(message, isAccept)
            }
        )
        binding.recyclerViewMessages.adapter = messageAdapter


        if (messages.isNotEmpty()) {
            binding.recyclerViewMessages.scrollToPosition(messages.size - 1)
        }
    }

    private fun setupMessageInput() {
        binding.sendButton.setOnClickListener {

            chatConversationViewModel.messageInput.set(binding.editTextMessage.text.toString())

            chatConversationViewModel.sendMessage()
        }


        binding.editTextMessage.setOnEditorActionListener { _, actionId, _ ->
            if (actionId == android.view.inputmethod.EditorInfo.IME_ACTION_SEND ||
                actionId == android.view.inputmethod.EditorInfo.IME_ACTION_DONE) {
                chatConversationViewModel.messageInput.set(binding.editTextMessage.text.toString())
                chatConversationViewModel.sendMessage()

                binding.editTextMessage.clearFocus()
                true
            } else {
                false
            }
        }



        binding.editTextMessage.addTextChangedListener(object : android.text.TextWatcher {
            override fun beforeTextChanged(s: CharSequence?, start: Int, count: Int, after: Int) {}
            override fun onTextChanged(s: CharSequence?, start: Int, before: Int, count: Int) {}
            override fun afterTextChanged(s: android.text.Editable?) {

                val currentText = s?.toString() ?: ""
                if (chatConversationViewModel.messageInput.get() != currentText) {
                    chatConversationViewModel.messageInput.set(currentText)
                }
            }
        })
    }





    override fun onResume() {
        super.onResume()


    }

    override fun onDestroy() {
        super.onDestroy()


        try {
            unregisterReceiver(verificationResetReceiver)
        } catch (e: Exception) {

            android.util.Log.w("ChatConversationActivity", "Error unregistering verification reset receiver: ${e.message}")
        }


        UserSession.removeProfessionChangeListener(this)
    }


    override fun onProfessionChanged(newProfession: String) {

        runOnUiThread {
            setupUI()
        }
    }
}