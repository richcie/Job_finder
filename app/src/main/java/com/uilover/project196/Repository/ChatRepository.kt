package com.uilover.project196.Repository

import android.content.Context
import com.uilover.project196.Database.AppDatabase
import com.uilover.project196.Model.ApprovalStatus
import com.uilover.project196.Model.ChatModel
import com.uilover.project196.Utils.UserSession
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext

// KRITERIA: Repository Pattern untuk chat dan messaging
// KRITERIA KOMPLEKSITAS: Repository Pattern untuk chat dan messaging (2/6)
class ChatRepository private constructor(private val context: Context) {

    private val database = AppDatabase.getDatabase(context)
    private val jobApplicationDao = database.jobApplicationDao()
    private val userDao = database.userDao()
    private val jobDao = database.jobDao()
    private val messageDao = database.messageDao()

    private var listeners = mutableListOf<ChatUpdateListener>()

    interface ChatUpdateListener {
        fun onChatsUpdated(chats: List<ChatModel>)
    }

    companion object {
        @Volatile
        private var INSTANCE: ChatRepository? = null

        fun getInstance(context: Context): ChatRepository {
            return INSTANCE ?: synchronized(this) {
                val instance = ChatRepository(context.applicationContext)
                INSTANCE = instance
                instance
            }
        }


        fun getAllChats(): List<ChatModel> {

            return emptyList()
        }

        fun getApprovedChats(): List<ChatModel> {
            return emptyList()
        }

        fun getPendingCount(): Int {
            return 0
        }

        fun approveChat(@Suppress("UNUSED_PARAMETER") chatId: String) {

        }

        fun rejectChat(@Suppress("UNUSED_PARAMETER") chatId: String) {

        }

        fun markChatAsRead(@Suppress("UNUSED_PARAMETER") chatId: String) {

        }
    }

    fun addListener(listener: ChatUpdateListener) {
        listeners.add(listener)
    }

    fun removeListener(listener: ChatUpdateListener) {
        listeners.remove(listener)
    }

    private fun notifyListeners(chats: List<ChatModel>) {
        listeners.forEach { it.onChatsUpdated(chats) }
    }


    private fun clearCache() {
        chatCache.clear()
        android.util.Log.d("ChatRepository", "Chat cache cleared")
    }


    private fun clearCacheForUser(userId: String) {
        chatCache.remove(userId)
        android.util.Log.d("ChatRepository", "Chat cache cleared for user: $userId")
    }


    fun clearCacheOnProfileUpdate(userId: String) {

        clearCacheForUser(userId)



        chatCache.clear()
        android.util.Log.d("ChatRepository", "Chat cache cleared due to profile update for user: $userId")
    }


    suspend fun refreshChatsAfterProfileUpdate() {
        val currentUserId = UserSession.getUserId() ?: return
        clearCacheOnProfileUpdate(currentUserId)


        val freshChats = getChatsForCurrentUser()
        notifyListeners(freshChats)
        android.util.Log.d("ChatRepository", "Chats refreshed after profile update")
    }


    suspend fun getChatsForCurrentUser(): List<ChatModel> {
        return withContext(Dispatchers.IO) {
            val currentUserId = UserSession.getUserId() ?: return@withContext emptyList()
            val userRole = UserSession.getUserRole()

            android.util.Log.d("ChatRepository", "=== GETTING CHATS FOR USER ===")
            android.util.Log.d("ChatRepository", "Current User ID: $currentUserId")
            android.util.Log.d("ChatRepository", "User Role: $userRole")

            when (userRole) {
                UserSession.ROLE_FREELANCER -> {

                    getFreelancerChats(currentUserId)
                }
                UserSession.ROLE_BUSINESS_OWNER -> {

                    getBusinessOwnerChats(currentUserId)
                }
                else -> emptyList()
            }
        }
    }


    private val chatCache = mutableMapOf<String, Pair<List<ChatModel>, Long>>()
    private val cacheValidityMs = 30_000L

    private suspend fun getFreelancerChats(freelancerId: String): List<ChatModel> {

        val cachedData = chatCache[freelancerId]
        val currentTime = System.currentTimeMillis()
        if (cachedData != null && (currentTime - cachedData.second) < cacheValidityMs) {
            android.util.Log.d("ChatRepository", "Returning cached freelancer chats for $freelancerId")
            return cachedData.first
        }

        android.util.Log.d("ChatRepository", "Loading fresh freelancer chats for $freelancerId")


        val applications = jobApplicationDao.getApplicationsByUserId(freelancerId)
        android.util.Log.d("ChatRepository", "Found ${applications.size} applications for freelancer $freelancerId")


        applications.forEachIndexed { index, app ->
            android.util.Log.d("ChatRepository", "Application $index: ID=${app.id}, JobID=${app.jobId}, Status='${app.status}', ApplicantID=${app.applicantUserId}")
        }

        if (applications.isEmpty()) {
            android.util.Log.d("ChatRepository", "No applications found for freelancer $freelancerId")
            return emptyList()
        }

        val jobIds = applications.map { it.jobId }.distinct()
        val jobs = jobDao.getJobsByIds(jobIds).associateBy { it.id }
        val ownerIds = jobs.values.mapNotNull { it.ownerId }.distinct()
        android.util.Log.d("ChatRepository", "Owner IDs found: $ownerIds")

        val businessOwners = if (ownerIds.isNotEmpty()) {
            val loadedOwners = userDao.getUsersByIds(ownerIds)
            android.util.Log.d("ChatRepository", "Loaded ${loadedOwners.size} business owners from database")
            loadedOwners.forEach { owner ->
                android.util.Log.d("ChatRepository", "Loaded business owner: ${owner.userId} (${owner.name})")
            }
            loadedOwners.associateBy { it.userId }
        } else {
            emptyMap()
        }

        val processedChatIds = mutableSetOf<String>()
        val chats = mutableListOf<ChatModel>()


        for (application in applications) {
            android.util.Log.d("ChatRepository", "Processing application ${application.id} for freelancer $freelancerId")

            val job = jobs[application.jobId]
            if (job == null) {
                android.util.Log.d("ChatRepository", "No job found for application ${application.id}, jobId=${application.jobId}")
                continue
            }

            val businessOwner = businessOwners[job.ownerId]
            if (businessOwner == null) {
                android.util.Log.e("ChatRepository", "❌ CRITICAL: No business owner found for job ${job.id}, ownerId=${job.ownerId}")
                android.util.Log.d("ChatRepository", "Available business owners in map: ${businessOwners.keys}")
                android.util.Log.d("ChatRepository", "Job details: title='${job.title}', company='${job.company}', ownerId='${job.ownerId}'")


                val directOwnerLookup = userDao.getUserById(job.ownerId ?: "")
                android.util.Log.d("ChatRepository", "Direct owner lookup result: ${if (directOwnerLookup != null) "${directOwnerLookup.userId} (${directOwnerLookup.name})" else "NULL"}")


                if (directOwnerLookup == null && job.ownerId == "user_002") {
                    android.util.Log.e("ChatRepository", "🚨 EMERGENCY: Business owner user_002 missing! Creating immediately...")

                    try {
                        val emergencyBusinessOwner = com.uilover.project196.Model.UserEntity(
                            userId = "user_002",
                            name = "Sarah Johnson",
                            email = "sarah.johnson@chaboksoft.com",
                            role = UserSession.ROLE_BUSINESS_OWNER,
                            skills = "",
                            title = "Founder & CEO",
                            experience = "10+ years",
                            rating = 0f,
                            totalReviews = 0,
                            completedProjects = 0,
                            hourlyRate = "",
                            availability = "",
                            location = "San Francisco, USA",
                            bio = "Visionary leader building innovative software solutions at ChabokSoft. Passionate about connecting talented freelancers with cutting-edge projects.",
                            companyName = "ChabokSoft",
                            isActive = true
                        )

                        userDao.insertUser(emergencyBusinessOwner)
                        android.util.Log.d("ChatRepository", "🚨 EMERGENCY: Business owner user_002 created successfully!")


                        kotlinx.coroutines.delay(100)
                        val verifyEmergencyOwner = userDao.getUserById("user_002")
                        if (verifyEmergencyOwner != null) {
                            android.util.Log.d("ChatRepository", "🚨 EMERGENCY: Business owner verified - proceeding with fallback logic")
                            val emergencyFallbackOwner = verifyEmergencyOwner

                            val chatId = "chat_${application.id}"
                            val (lastMessage, lastMessageTime) = getLastMessageForChat(chatId)

                            android.util.Log.d("ChatRepository", "Chat $chatId: lastMessage='${lastMessage.take(50)}${if(lastMessage.length > 50) "..." else ""}', status='${application.status}'")


                            val shouldShowChat = (application.status == "shortlisted" || application.status == "hired") || lastMessage.isNotEmpty()

                            android.util.Log.d("ChatRepository", "Chat $chatId shouldShowChat: $shouldShowChat (status check: ${application.status == "shortlisted" || application.status == "hired"}, message check: ${lastMessage.isNotEmpty()})")

                            if (shouldShowChat) {

                                val freelancerProfession = userDao.getUserById(freelancerId)?.title


                                val companyName = emergencyFallbackOwner.companyName.ifEmpty { "Company" }
                                val recruiterName = emergencyFallbackOwner.name.ifEmpty { "Recruiter" }


                                val hasUnreadMessages = messageDao.getUnreadMessageCount(chatId, freelancerId) > 0

                                val chat = ChatModel(
                                    id = chatId,
                                    userId = freelancerId,
                                    companyName = companyName,
                                    companyLogo = "logo1",
                                    recruiterName = recruiterName,
                                    jobTitle = job.title,
                                    lastMessage = lastMessage.ifEmpty { "You've been shortlisted! Start a conversation." },
                                    timestamp = if (lastMessage.isNotEmpty()) lastMessageTime else application.appliedAt + (24 * 60 * 60 * 1000),
                                    isUnread = hasUnreadMessages,

                                    approvalStatus = ApprovalStatus.APPROVED,
                                    messageCount = 0,
                                    freelancerProfession = freelancerProfession
                                )
                                chats.add(chat)
                                processedChatIds.add(chatId)

                                android.util.Log.d("ChatRepository", "✅ EMERGENCY SUCCESS: Added chat $chatId for freelancer $freelancerId with emergency business owner ${emergencyFallbackOwner.name}")
                            } else {
                                android.util.Log.d("ChatRepository", "❌ EMERGENCY: Skipped chat $chatId for freelancer $freelancerId (shouldShowChat=false)")
                            }
                            continue
                        } else {
                            android.util.Log.e("ChatRepository", "🚨 EMERGENCY FAILED: Business owner still not found after creation!")
                        }
                    } catch (e: Exception) {
                        android.util.Log.e("ChatRepository", "🚨 EMERGENCY FAILED: Error creating business owner", e)
                    }
                }


                if (directOwnerLookup != null) {
                    android.util.Log.d("ChatRepository", "🚨 USING FALLBACK: Direct lookup succeeded, using this business owner")
                    val fallbackBusinessOwner = directOwnerLookup

                    val chatId = "chat_${application.id}"
                    val (lastMessage, lastMessageTime) = getLastMessageForChat(chatId)

                    android.util.Log.d("ChatRepository", "Chat $chatId: lastMessage='${lastMessage.take(50)}${if(lastMessage.length > 50) "..." else ""}', status='${application.status}'")


                    val shouldShowChat = (application.status == "shortlisted" || application.status == "hired") || lastMessage.isNotEmpty()

                    android.util.Log.d("ChatRepository", "Chat $chatId shouldShowChat: $shouldShowChat (status check: ${application.status == "shortlisted" || application.status == "hired"}, message check: ${lastMessage.isNotEmpty()})")

                    if (shouldShowChat) {

                        val freelancerProfession = userDao.getUserById(freelancerId)?.title


                        val companyName = fallbackBusinessOwner.companyName.ifEmpty { "Company" }
                        val recruiterName = fallbackBusinessOwner.name.ifEmpty { "Recruiter" }


                        val hasUnreadMessages = messageDao.getUnreadMessageCount(chatId, freelancerId) > 0

                        val chat = ChatModel(
                            id = chatId,
                            userId = freelancerId,
                            companyName = companyName,
                            companyLogo = "logo1",
                            recruiterName = recruiterName,
                            jobTitle = job.title,
                            lastMessage = lastMessage.ifEmpty { "You've been shortlisted! Start a conversation." },
                            timestamp = if (lastMessage.isNotEmpty()) lastMessageTime else application.appliedAt + (24 * 60 * 60 * 1000),
                            isUnread = hasUnreadMessages,

                            approvalStatus = ApprovalStatus.APPROVED,
                            messageCount = 0,
                            freelancerProfession = freelancerProfession
                        )
                        chats.add(chat)
                        processedChatIds.add(chatId)

                        android.util.Log.d("ChatRepository", "✅ FALLBACK SUCCESS: Added chat $chatId for freelancer $freelancerId with fallback business owner ${fallbackBusinessOwner.name}")
                    } else {
                        android.util.Log.d("ChatRepository", "❌ FALLBACK: Skipped chat $chatId for freelancer $freelancerId (shouldShowChat=false)")
                    }
                } else {
                    android.util.Log.e("ChatRepository", "❌ COMPLETE FAILURE: Both batch and direct business owner lookup failed")
                }
                continue
            }

            val chatId = "chat_${application.id}"
            val (lastMessage, lastMessageTime) = getLastMessageForChat(chatId)

            android.util.Log.d("ChatRepository", "Chat $chatId: lastMessage='${lastMessage.take(50)}${if(lastMessage.length > 50) "..." else ""}', status='${application.status}'")


            val shouldShowChat = (application.status == "shortlisted" || application.status == "hired") || lastMessage.isNotEmpty()

            android.util.Log.d("ChatRepository", "Chat $chatId shouldShowChat: $shouldShowChat (status check: ${application.status == "shortlisted" || application.status == "hired"}, message check: ${lastMessage.isNotEmpty()})")

            if (shouldShowChat) {

                val freelancerProfession = userDao.getUserById(freelancerId)?.title


                val companyName = businessOwner.companyName.ifEmpty { "Company" }
                val recruiterName = businessOwner.name.ifEmpty { "Recruiter" }


                val hasUnreadMessages = messageDao.getUnreadMessageCount(chatId, freelancerId) > 0

                val chat = ChatModel(
                    id = chatId,
                    userId = freelancerId,
                    companyName = companyName,
                    companyLogo = "logo1",
                    recruiterName = recruiterName,
                    jobTitle = job.title,
                    lastMessage = lastMessage.ifEmpty { "You've been shortlisted! Start a conversation." },
                    timestamp = if (lastMessage.isNotEmpty()) lastMessageTime else application.appliedAt + (24 * 60 * 60 * 1000),
                    isUnread = hasUnreadMessages,

                    approvalStatus = ApprovalStatus.APPROVED,
                    messageCount = 0,
                    freelancerProfession = freelancerProfession
                )
                chats.add(chat)
                processedChatIds.add(chatId)

                android.util.Log.d("ChatRepository", "✅ Added chat $chatId for freelancer $freelancerId with business owner ${businessOwner.name}")
            } else {
                android.util.Log.d("ChatRepository", "❌ Skipped chat $chatId for freelancer $freelancerId (shouldShowChat=false)")
            }
        }



        try {

            val allMessages = messageDao.getAllMessages()
                .filter { it.senderId == freelancerId }
            val chatIdsWithMessages = allMessages.map { it.chatId }.distinct()

            android.util.Log.d("ChatRepository", "FALLBACK: Checking for orphaned chats for freelancer $freelancerId")
            android.util.Log.d("ChatRepository", "Found ${allMessages.size} messages sent by freelancer, ${chatIdsWithMessages.size} distinct chat IDs")


            val chat19Messages = messageDao.getMessagesForChat("chat_19")
            android.util.Log.d("ChatRepository", "Chat_19 has ${chat19Messages.size} messages")
            chat19Messages.forEach { msg ->
                android.util.Log.d("ChatRepository", "Chat_19 message: sender=${msg.senderId}, text=${msg.text.take(50)}...")
            }

            for (chatId in chatIdsWithMessages) {
                if (!processedChatIds.contains(chatId)) {

                    val (lastMessage, lastMessageTime) = getLastMessageForChat(chatId)

                    if (lastMessage.isNotEmpty()) {

                        val messagesInChat = messageDao.getMessagesForChat(chatId)
                        val otherUserId = messagesInChat.find { it.senderId != freelancerId }?.senderId
                        val businessOwner = otherUserId?.let { userDao.getUserById(it) }

                        if (businessOwner != null) {
                            android.util.Log.d("ChatRepository", "Found orphaned chat $chatId for freelancer $freelancerId - preserving with business owner ${businessOwner.name}")

                            val currentFreelancer = userDao.getUserById(freelancerId)
                            val freelancerProfession = currentFreelancer?.title


                            val hasUnreadMessages = messageDao.getUnreadMessageCount(chatId, freelancerId) > 0

                            val chat = ChatModel(
                                id = chatId,
                                userId = freelancerId,
                                companyName = businessOwner.companyName.ifEmpty { "Company" },
                                companyLogo = "logo1",
                                recruiterName = businessOwner.name.ifEmpty { "Recruiter" },
                                jobTitle = "Previous Conversation",
                                lastMessage = lastMessage,
                                timestamp = lastMessageTime,
                                isUnread = hasUnreadMessages,
                                approvalStatus = ApprovalStatus.APPROVED,
                                messageCount = 0,
                                freelancerProfession = freelancerProfession
                            )
                            chats.add(chat)
                        }
                    }
                }
            }
        } catch (e: Exception) {
            android.util.Log.w("ChatRepository", "Error checking for orphaned chats", e)
        }



        val sortedChats = chats.sortedByDescending { it.timestamp }


        chatCache[freelancerId] = Pair(sortedChats, currentTime)

        android.util.Log.d("ChatRepository", "Final chat count for freelancer $freelancerId: ${sortedChats.size}")


        try {
            val allMessagesInDb = messageDao.getAllMessages()
            val messagesInvolvingFreelancer = allMessagesInDb.filter {
                it.senderId == freelancerId || it.chatId.contains("chat_")
            }
            android.util.Log.d("ChatRepository", "🔍 EMERGENCY DEBUG: Found ${messagesInvolvingFreelancer.size} messages involving freelancer $freelancerId")
            messagesInvolvingFreelancer.forEach { msg ->
                android.util.Log.d("ChatRepository", "🔍 Message: chatId=${msg.chatId}, sender=${msg.senderId}, text=${msg.text.take(30)}...")
            }


            val allChatMessages = allMessagesInDb.filter { it.chatId.startsWith("chat_") }
            android.util.Log.d("ChatRepository", "🔍 ALL CHAT MESSAGES IN DATABASE (${allChatMessages.size} total):")
            allChatMessages.forEach { msg ->
                android.util.Log.d("ChatRepository", "🔍 ChatMsg: chatId=${msg.chatId}, sender=${msg.senderId}, text=${msg.text.take(30)}...")
            }


            val welcomeMessages = allMessagesInDb.filter { it.text.contains("Welcome to the team") }
            android.util.Log.d("ChatRepository", "🔍 EMERGENCY DEBUG: Found ${welcomeMessages.size} welcome messages in database")
            welcomeMessages.forEach { msg ->
                android.util.Log.d("ChatRepository", "🔍 Welcome message: chatId=${msg.chatId}, sender=${msg.senderId}")
            }
        } catch (e: Exception) {
            android.util.Log.e("ChatRepository", "Error in emergency debug", e)
        }

        return sortedChats
    }

    private suspend fun getBusinessOwnerChats(businessOwnerId: String): List<ChatModel> {

        val cachedData = chatCache[businessOwnerId]
        val currentTime = System.currentTimeMillis()
        if (cachedData != null && (currentTime - cachedData.second) < cacheValidityMs) {
            android.util.Log.d("ChatRepository", "Returning cached business owner chats for $businessOwnerId")
            return cachedData.first
        }

        android.util.Log.d("ChatRepository", "Loading fresh business owner chats for $businessOwnerId")


        val ownedJobs = jobDao.getJobsByOwnerId(businessOwnerId)
        if (ownedJobs.isEmpty()) {
            android.util.Log.d("ChatRepository", "No jobs found for business owner $businessOwnerId")
            return emptyList()
        }

        val jobIds = ownedJobs.map { it.id }
        val allApplications = jobApplicationDao.getApplicationsByJobIds(jobIds)
        val freelancerIds = allApplications.map { it.applicantUserId }.distinct()
        val freelancers = if (freelancerIds.isNotEmpty()) {
            userDao.getUsersByIds(freelancerIds).associateBy { it.userId }
        } else {
            emptyMap()
        }

        val chats = mutableListOf<ChatModel>()
        val processedChatIds = mutableSetOf<String>()


        for (application in allApplications) {
            val job = ownedJobs.find { it.id == application.jobId } ?: continue
            val chatId = "chat_${application.id}"
            val (lastMessage, lastMessageTime) = getLastMessageForChat(chatId)

            val shouldShowChat = (application.status == "shortlisted" || application.status == "hired") || lastMessage.isNotEmpty()

            if (shouldShowChat) {
                val freelancer = freelancers[application.applicantUserId]
                val recruiterName = freelancer?.name?.ifEmpty { "Freelancer" } ?: "Freelancer (${application.applicantUserId})"
                val freelancerProfession = freelancer?.title ?: "Unknown"


                    val hasUnreadMessages = messageDao.getUnreadMessageCount(chatId, businessOwnerId) > 0

                    // Get business owner's company name from profile data
                    val businessOwnerEntity = userDao.getUserById(businessOwnerId)
                    val companyName = businessOwnerEntity?.companyName?.ifEmpty { "Your Company" } ?: "Your Company"

                    val chat = ChatModel(
                        id = chatId,
                        userId = businessOwnerId,
                        companyName = companyName,
                        companyLogo = "logo1",
                        recruiterName = recruiterName,
                        jobTitle = job.title,
                        lastMessage = lastMessage,
                        timestamp = if (lastMessage.isNotEmpty()) lastMessageTime else application.appliedAt + (24 * 60 * 60 * 1000),
                        isUnread = hasUnreadMessages,
                        approvalStatus = ApprovalStatus.APPROVED,
                        messageCount = 0,
                        freelancerProfession = freelancerProfession
                    )
                chats.add(chat)
                processedChatIds.add(chatId)
            }
        }



        try {

            val allMessages = messageDao.getAllMessages()
                .filter { it.senderId == businessOwnerId }
            val chatIdsWithMessages = allMessages.map { it.chatId }.distinct()

            for (chatId in chatIdsWithMessages) {
                if (!processedChatIds.contains(chatId)) {

                    val (lastMessage, lastMessageTime) = getLastMessageForChat(chatId)

                    if (lastMessage.isNotEmpty()) {

                        val messagesInChat = messageDao.getMessagesForChat(chatId)
                        val otherUserId = messagesInChat.find { it.senderId != businessOwnerId }?.senderId
                        val freelancer = otherUserId?.let { userDao.getUserById(it) }

                        if (freelancer != null) {
                            android.util.Log.d("ChatRepository", "Found orphaned chat $chatId for business owner $businessOwnerId - preserving with freelancer ${freelancer.name}")


                            val hasUnreadMessages = messageDao.getUnreadMessageCount(chatId, businessOwnerId) > 0

                            // Get business owner's company name from profile data
                            val businessOwnerEntity = userDao.getUserById(businessOwnerId)
                            val companyName = businessOwnerEntity?.companyName?.ifEmpty { "Your Company" } ?: "Your Company"

                            val chat = ChatModel(
                                id = chatId,
                                userId = businessOwnerId,
                                companyName = companyName,
                                companyLogo = "logo1",
                                recruiterName = freelancer.name.ifEmpty { "Freelancer" },
                                jobTitle = "Previous Conversation",
                                lastMessage = lastMessage,
                                timestamp = lastMessageTime,
                                isUnread = hasUnreadMessages,
                                approvalStatus = ApprovalStatus.APPROVED,
                                messageCount = 0,
                                freelancerProfession = freelancer.title
                            )
                            chats.add(chat)
                        }
                    }
                }
            }
        } catch (e: Exception) {
            android.util.Log.w("ChatRepository", "Error checking for orphaned chats", e)
        }

        val sortedChats = chats.sortedByDescending { it.timestamp }


        chatCache[businessOwnerId] = Pair(sortedChats, currentTime)

        android.util.Log.d("ChatRepository", "Final chat count for business owner $businessOwnerId: ${sortedChats.size}")
        return sortedChats
    }


    suspend fun getChatById(chatId: String): ChatModel? {
        return withContext(Dispatchers.IO) {
            val chats = getChatsForCurrentUser()
            chats.find { it.id == chatId }
        }
    }


    suspend fun markChatAsReadById(chatId: String) {
        withContext(Dispatchers.IO) {
            try {
                val currentUserId = UserSession.getUserId() ?: return@withContext

                messageDao.markMessagesAsRead(chatId, currentUserId)
                android.util.Log.d("ChatRepository", "Marked messages as read for chat $chatId and user $currentUserId")
            } catch (e: Exception) {
                android.util.Log.e("ChatRepository", "Error marking chat as read", e)
            }
        }


        withContext(Dispatchers.IO) {
            clearCache()
        }
        val updatedChats = withContext(Dispatchers.IO) { getChatsForCurrentUser() }
        withContext(Dispatchers.Main) {
            notifyListeners(updatedChats)
        }
    }


    suspend fun forceRefreshChats() {
        withContext(Dispatchers.IO) {

            clearCache()
        }
        val updatedChats = withContext(Dispatchers.IO) { getChatsForCurrentUser() }
        withContext(Dispatchers.Main) {
            notifyListeners(updatedChats)
        }
    }


    suspend fun createWelcomeMessageForNewlyShortlistedFreelancer(applicationId: Int, freelancerId: String, businessOwnerId: String): Boolean {
        return withContext(Dispatchers.IO) {
            try {
                val chatId = "chat_$applicationId"


                val existingMessages = messageDao.getMessagesForChat(chatId)
                if (existingMessages.isNotEmpty()) {
                    android.util.Log.d("ChatRepository", "Chat $chatId already has messages, skipping welcome message")
                    return@withContext true
                }


                val businessOwner = userDao.getUserById(businessOwnerId)
                val freelancer = userDao.getUserById(freelancerId)

                if (businessOwner == null || freelancer == null) {
                    android.util.Log.e("ChatRepository", "Could not find business owner ($businessOwnerId) or freelancer ($freelancerId)")
                    return@withContext false
                }


                val welcomeMessageId = "welcome_${System.currentTimeMillis()}_${businessOwnerId}"
                val welcomeMessage = com.uilover.project196.Model.MessageEntity(
                    id = welcomeMessageId,
                    chatId = chatId,
                    senderId = businessOwnerId,
                    text = "Welcome to the team, ${freelancer.name}! 🎉\n\nCongratulations! You've been shortlisted for this position. I'm excited to work with you.\n\nFeel free to ask any questions about the project or send a verification request when you're ready to start working.\n\nLooking forward to our collaboration!",
                    timestamp = System.currentTimeMillis(),
                    isRead = false
                )

                messageDao.insertMessage(welcomeMessage)

                android.util.Log.d("ChatRepository", "✅ Created welcome message for newly shortlisted freelancer $freelancerId in chat $chatId")


                withContext(Dispatchers.Main) {

                    clearCache()
                    chatCache.remove(freelancerId)
                    chatCache.remove(businessOwnerId)

                    android.util.Log.d("ChatRepository", "🚀 CACHE FIX: Explicitly cleared cache for freelancer ($freelancerId) and business owner ($businessOwnerId)")

                    val updatedChats = withContext(Dispatchers.IO) { getChatsForCurrentUser() }
                    notifyListeners(updatedChats)


                    val intent = android.content.Intent("com.uilover.project196.CHAT_CREATED")
                    intent.putExtra("chat_id", chatId)
                    intent.putExtra("freelancer_id", freelancerId)
                    intent.putExtra("business_owner_id", businessOwnerId)
                    intent.putExtra("application_id", applicationId)
                    context.sendBroadcast(intent)

                    android.util.Log.d("ChatRepository", "✅ Broadcast sent to notify UI components about new chat creation")


                    val jobRefreshIntent = android.content.Intent("com.uilover.project196.FREELANCER_SHORTLISTED")
                    jobRefreshIntent.putExtra("freelancer_id", freelancerId)
                    jobRefreshIntent.putExtra("business_owner_id", businessOwnerId)
                    context.sendBroadcast(jobRefreshIntent)

                    android.util.Log.d("ChatRepository", "✅ Broadcast sent to notify freelancer about shortlisting - jobs should now appear")
                }

                true
            } catch (e: Exception) {
                android.util.Log.e("ChatRepository", "Error creating welcome message for shortlisted freelancer", e)
                false
            }
        }
    }


    suspend fun createCongratulationsMessageForVerifiedFreelancer(
        chatId: String,
        freelancerId: String,
        businessOwnerId: String
    ): Boolean {
        return withContext(Dispatchers.IO) {
            try {
                android.util.Log.d("ChatRepository", "=== CREATING VERIFICATION CONGRATULATIONS MESSAGE ===")
                android.util.Log.d("ChatRepository", "Chat ID: $chatId")
                android.util.Log.d("ChatRepository", "Freelancer ID: $freelancerId")
                android.util.Log.d("ChatRepository", "Business Owner ID: $businessOwnerId")


                val freelancer = userDao.getUserById(freelancerId)
                if (freelancer == null) {
                    android.util.Log.e("ChatRepository", "Cannot create congratulations message - freelancer not found: $freelancerId")
                    return@withContext false
                }


                val businessOwner = userDao.getUserById(businessOwnerId)
                val companyName = businessOwner?.companyName?.ifEmpty { "our company" } ?: "our company"


                val congratulationsMessageId = "verification_congrats_${System.currentTimeMillis()}_${freelancerId}_${businessOwnerId}"


                val congratulationsText = buildString {
                    appendLine("🎉 Congratulations ${freelancer.name}!")
                    appendLine()
                    appendLine("Your verification request has been APPROVED! ✅")
                    appendLine()
                    appendLine("You are now a verified freelancer for $companyName. This means:")
                    appendLine("• You can check in/out for work")
                    appendLine("• Your attendance will be tracked")
                    appendLine("• You have access to all job opportunities")
                    appendLine("• You can submit daily progress reports")
                    appendLine()
                    appendLine("Welcome to the verified team! I'm excited to work with you and see what amazing results we can achieve together.")
                    appendLine()
                    append("Ready to start making an impact! 🚀")
                }

                val congratulationsMessage = com.uilover.project196.Model.MessageEntity(
                    id = congratulationsMessageId,
                    chatId = chatId,
                    senderId = businessOwnerId,
                    text = congratulationsText,
                    timestamp = System.currentTimeMillis(),
                    isRead = false
                )

                messageDao.insertMessage(congratulationsMessage)

                android.util.Log.d("ChatRepository", "✅ Created verification congratulations message for freelancer $freelancerId in chat $chatId")


                withContext(Dispatchers.Main) {

                    clearCache()
                    val updatedChats = withContext(Dispatchers.IO) { getChatsForCurrentUser() }
                    notifyListeners(updatedChats)


                    val intent = android.content.Intent("com.uilover.project196.VERIFICATION_CONGRATULATIONS_SENT")
                    intent.putExtra("chat_id", chatId)
                    intent.putExtra("freelancer_id", freelancerId)
                    intent.putExtra("business_owner_id", businessOwnerId)
                    context.sendBroadcast(intent)

                    android.util.Log.d("ChatRepository", "✅ Broadcast sent to notify UI components about verification congratulations message")
                }

                true
            } catch (e: Exception) {
                android.util.Log.e("ChatRepository", "Error creating verification congratulations message", e)
                false
            }
        }
    }


    suspend fun saveMessage(chatId: String, senderId: String, text: String): Boolean {
        return withContext(Dispatchers.IO) {
            try {

                val now = System.currentTimeMillis()
                val recentMessages = messageDao.getMessagesForChat(chatId)
                    .filter { it.senderId == senderId }
                    .filter { (now - it.timestamp) < 5000 }
                    .filter { it.text == text }

                if (recentMessages.isNotEmpty()) {
                    android.util.Log.w("ChatRepository", "Preventing duplicate message: $text")
                    return@withContext false
                }

                val messageId = "msg_${System.currentTimeMillis()}_${senderId}"
                val message = com.uilover.project196.Model.MessageEntity(
                    id = messageId,
                    chatId = chatId,
                    senderId = senderId,
                    text = text,
                    timestamp = System.currentTimeMillis(),
                    isRead = false
                )
                messageDao.insertMessage(message)

                android.util.Log.d("ChatRepository", "✅ Message saved successfully - chatId: $chatId, sender: $senderId, text: $text")


                withContext(Dispatchers.Main) {

                    clearCache()
                    val updatedChats = withContext(Dispatchers.IO) { getChatsForCurrentUser() }
                    notifyListeners(updatedChats)


                    val intent = android.content.Intent("com.uilover.project196.MESSAGE_SENT")
                    intent.putExtra("chat_id", chatId)
                    intent.putExtra("sender_id", senderId)
                    intent.putExtra("message_text", text)
                    context.sendBroadcast(intent)

                    android.util.Log.d("ChatRepository", "✅ Broadcast sent to notify UI components about new message")
                }

                true
            } catch (e: Exception) {
                android.util.Log.e("ChatRepository", "Error saving message", e)
                false
            }
        }
    }

    suspend fun getMessagesForChat(chatId: String): List<com.uilover.project196.Model.MessageModel> {
        return withContext(Dispatchers.IO) {
            try {
                val currentUserId = UserSession.getUserId() ?: return@withContext emptyList()
                val messageEntities = messageDao.getMessagesForChat(chatId)

                messageEntities.map { entity ->

                    val sender = userDao.getUserById(entity.senderId)
                    val senderName = sender?.name ?: "Unknown"

                    com.uilover.project196.Model.MessageModel(
                        id = entity.id,
                        chatId = entity.chatId,
                        text = entity.text,
                        timestamp = entity.timestamp,
                        isFromUser = entity.senderId == currentUserId,
                        senderName = senderName,
                        senderId = entity.senderId,
                        messageType = entity.messageType,
                        jobRequestStatus = entity.jobRequestStatus,
                        jobId = entity.jobId,
                        verificationStatus = entity.verificationStatus,
                        freelancerData = entity.freelancerData
                    )
                }
            } catch (e: Exception) {
                android.util.Log.e("ChatRepository", "Error getting messages", e)
                emptyList()
            }
        }
    }

    suspend fun markMessagesAsRead(chatId: String) {
        withContext(Dispatchers.IO) {
            try {
                val currentUserId = UserSession.getUserId() ?: return@withContext
                messageDao.markMessagesAsRead(chatId, currentUserId)
            } catch (e: Exception) {
                android.util.Log.e("ChatRepository", "Error marking messages as read", e)
            }
        }
    }

    private suspend fun getLastMessageForChat(chatId: String): Pair<String, Long> {
        return withContext(Dispatchers.IO) {
            try {
                val lastMessage = messageDao.getLastMessageForChat(chatId)
                if (lastMessage != null) {
                    Pair(lastMessage.text, lastMessage.timestamp)
                } else {
                    Pair("", System.currentTimeMillis())
                }
            } catch (e: Exception) {
                Pair("", System.currentTimeMillis())
            }
        }
    }


    suspend fun saveJobRequestMessage(chatId: String, senderId: String, jobId: String, message: String): Boolean {
        return withContext(Dispatchers.IO) {
            try {

                val existingRequests = messageDao.getMessagesForChat(chatId)
                    .filter { it.messageType == "job_request" }
                    .filter { it.senderId == senderId }
                    .filter { it.jobId == jobId }
                    .filter { it.jobRequestStatus == "pending" }

                if (existingRequests.isNotEmpty()) {
                    android.util.Log.w("ChatRepository", "Job request already pending for this freelancer")
                    return@withContext false
                }

                val messageId = "job_req_${System.currentTimeMillis()}_${senderId}"
                val jobRequestMessage = com.uilover.project196.Model.MessageEntity(
                    id = messageId,
                    chatId = chatId,
                    senderId = senderId,
                    text = message,
                    timestamp = System.currentTimeMillis(),
                    isRead = false,
                    messageType = "job_request",
                    jobRequestStatus = "pending",
                    jobId = jobId
                )

                messageDao.insertMessage(jobRequestMessage)


                withContext(Dispatchers.Main) {

                    val updatedChats = withContext(Dispatchers.IO) { getChatsForCurrentUser() }
                    notifyListeners(updatedChats)
                }

                true
            } catch (e: Exception) {
                android.util.Log.e("ChatRepository", "Error saving job request message", e)
                false
            }
        }
    }

    suspend fun updateJobRequestStatus(messageId: String, newStatus: String): Boolean {
        return withContext(Dispatchers.IO) {
            try {
                messageDao.updateJobRequestStatus(messageId, newStatus)


                withContext(Dispatchers.Main) {

                    val updatedChats = withContext(Dispatchers.IO) { getChatsForCurrentUser() }
                    notifyListeners(updatedChats)
                }

                true
            } catch (e: Exception) {
                android.util.Log.e("ChatRepository", "Error updating job request status", e)
                false
            }
        }
    }

    suspend fun hasAcceptedJobRequest(chatId: String, jobId: String): Boolean {
        return withContext(Dispatchers.IO) {
            try {
                val messages = messageDao.getMessagesForChat(chatId)
                messages.any {
                    it.messageType == "job_request" &&
                    it.jobId == jobId &&
                    it.jobRequestStatus == "accepted"
                }
            } catch (e: Exception) {
                android.util.Log.e("ChatRepository", "Error checking accepted job request", e)
                false
            }
        }
    }


    suspend fun saveVerificationRequestMessage(chatId: String, senderId: String, freelancerData: String, message: String): Boolean {
        return withContext(Dispatchers.IO) {
            try {

                val existingRequests = messageDao.getMessagesForChat(chatId)
                    .filter { it.messageType == "verification_request" }
                    .filter { it.senderId == senderId }
                    .filter { it.verificationStatus == "pending" }

                if (existingRequests.isNotEmpty()) {
                    android.util.Log.w("ChatRepository", "Verification request already pending for this freelancer")
                    return@withContext false
                }

                val messageId = "verify_req_${System.currentTimeMillis()}_${senderId}"
                val verificationMessage = com.uilover.project196.Model.MessageEntity(
                    id = messageId,
                    chatId = chatId,
                    senderId = senderId,
                    text = message,
                    timestamp = System.currentTimeMillis(),
                    isRead = false,
                    messageType = "verification_request",
                    verificationStatus = "pending",
                    freelancerData = freelancerData
                )

                messageDao.insertMessage(verificationMessage)


                withContext(Dispatchers.Main) {

                    val updatedChats = withContext(Dispatchers.IO) { getChatsForCurrentUser() }
                    notifyListeners(updatedChats)
                }

                true
            } catch (e: Exception) {
                android.util.Log.e("ChatRepository", "Error saving verification request message", e)
                false
            }
        }
    }

    suspend fun updateVerificationStatus(messageId: String, newStatus: String): Boolean {
        return withContext(Dispatchers.IO) {
            try {
                messageDao.updateVerificationStatus(messageId, newStatus)


                withContext(Dispatchers.Main) {

                    val updatedChats = withContext(Dispatchers.IO) { getChatsForCurrentUser() }
                    notifyListeners(updatedChats)
                }

                true
            } catch (e: Exception) {
                android.util.Log.e("ChatRepository", "Error updating verification status", e)
                false
            }
        }
    }

    suspend fun resetVerificationStatusForFreelancer(freelancerId: String): Boolean {
        return withContext(Dispatchers.IO) {
            try {
                android.util.Log.d("ChatRepository", "Resetting verification status for freelancer $freelancerId due to business owner acceptance")
                messageDao.resetVerificationStatusForFreelancer(freelancerId)


                withContext(Dispatchers.Main) {

                    val updatedChats = withContext(Dispatchers.IO) { getChatsForCurrentUser() }
                    notifyListeners(updatedChats)


                    val intent = android.content.Intent("com.uilover.project196.VERIFICATION_STATUS_RESET")
                    intent.putExtra("freelancer_id", freelancerId)
                    context.sendBroadcast(intent)


                    val jobsRefreshIntent = android.content.Intent("com.uilover.project196.VERIFICATION_RESET_JOBS")
                    jobsRefreshIntent.putExtra("freelancer_id", freelancerId)
                    context.sendBroadcast(jobsRefreshIntent)
                }

                android.util.Log.d("ChatRepository", "✅ Verification status reset successfully for freelancer $freelancerId")
                true
            } catch (e: Exception) {
                android.util.Log.e("ChatRepository", "Error resetting verification status for freelancer $freelancerId", e)
                false
            }
        }
    }

    suspend fun hasAcceptedVerification(freelancerId: String): Boolean {
        return withContext(Dispatchers.IO) {
            try {


                val acceptedVerification = messageDao.getAcceptedVerificationForFreelancer(freelancerId)
                val hasAcceptedChat = acceptedVerification != null


                val shortlistedApplications = jobApplicationDao.getApplicationsByUserId(freelancerId)
                    .filter { it.status == "shortlisted" }

                android.util.Log.d("ChatRepository", "=== VERIFICATION CHECK FOR FREELANCER $freelancerId ===")
                android.util.Log.d("ChatRepository", "Shortlisted applications: ${shortlistedApplications.size} (does NOT auto-verify)")
                android.util.Log.d("ChatRepository", "Chat-based verification accepted: $hasAcceptedChat")
                android.util.Log.d("ChatRepository", "Final verification status: $hasAcceptedChat")

                return@withContext hasAcceptedChat

            } catch (e: Exception) {
                android.util.Log.e("ChatRepository", "Error checking verification status for $freelancerId", e)
                false
            }
        }
    }
}