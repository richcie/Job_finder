package com.uilover.project196.Utils

import android.content.Context
import android.content.SharedPreferences
import kotlinx.coroutines.*
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow

object UserSession {
    private const val PREF_NAME = "user_session"
    private const val KEY_IS_LOGGED_IN = "is_logged_in"
    private const val KEY_USER_ID = "user_id"
    private const val KEY_USER_NAME = "user_name"
    private const val KEY_USER_EMAIL = "user_email"
    private const val KEY_USER_ROLE = "user_role"
    private const val KEY_USER_PROFESSION = "user_profession"


    const val ROLE_FREELANCER = "freelancer"
    const val ROLE_BUSINESS_OWNER = "business_owner"

    private lateinit var prefs: SharedPreferences


    interface LoginStateListener {
        fun onLoginStateChanged(isLoggedIn: Boolean)
        fun onLoginStateRefresh(isLoggedIn: Boolean) {

            onLoginStateChanged(isLoggedIn)
        }
    }


    interface ProfessionChangeListener {
        fun onProfessionChanged(newProfession: String)
    }

    private val loginStateListeners = mutableListOf<LoginStateListener>()
    private val professionChangeListeners = mutableListOf<ProfessionChangeListener>()


    private var isSyncing = false
    private var lastSyncTime = 0L
    private val syncCacheTime = 300_000L


    private val _professionFlow = MutableStateFlow<String?>(null)
    val professionFlow: StateFlow<String?> = _professionFlow.asStateFlow()

    fun init(context: Context) {
        android.util.Log.d("UserSession", "🔐 UserSession.init() called with context: ${context.javaClass.simpleName}")
        prefs = context.getSharedPreferences(PREF_NAME, Context.MODE_PRIVATE)
        android.util.Log.d("UserSession", "🔐 UserSession.init() completed - prefs initialized: ${::prefs.isInitialized}")


        if (::prefs.isInitialized) {
            val isLoggedIn = prefs.getBoolean(KEY_IS_LOGGED_IN, false)
            val userId = prefs.getString(KEY_USER_ID, null)
            val userName = prefs.getString(KEY_USER_NAME, null)
            val userRole = prefs.getString(KEY_USER_ROLE, null)
            android.util.Log.d("UserSession", "🔐 Current session state - logged in: $isLoggedIn, userId: $userId, userName: $userName, role: $userRole")
        }
    }

    fun isLoggedIn(): Boolean {
        val loggedIn = ::prefs.isInitialized && prefs.getBoolean(KEY_IS_LOGGED_IN, false)
        android.util.Log.d("UserSession", "🔐 isLoggedIn() called - result: $loggedIn, prefs initialized: ${::prefs.isInitialized}")
        return loggedIn
    }

    fun getUserId(): String? {
        val userId = if (::prefs.isInitialized) prefs.getString(KEY_USER_ID, null) else null
        android.util.Log.d("UserSession", "🔐 getUserId() called - result: $userId, prefs initialized: ${::prefs.isInitialized}")
        return userId
    }

    fun getUserName(): String? {
        val userName = if (::prefs.isInitialized) prefs.getString(KEY_USER_NAME, null) else null
        android.util.Log.d("UserSession", "🔐 getUserName() called - result: $userName")
        return userName
    }

    fun getUserEmail(): String? {
        return if (::prefs.isInitialized) prefs.getString(KEY_USER_EMAIL, null) else null
    }

    fun getUserRole(): String? {
        val userRole = if (::prefs.isInitialized) prefs.getString(KEY_USER_ROLE, null) else null
        android.util.Log.d("UserSession", "🔐 getUserRole() called - result: $userRole, prefs initialized: ${::prefs.isInitialized}")
        return userRole
    }

    fun getUserProfession(): String? {
        return if (::prefs.isInitialized) prefs.getString(KEY_USER_PROFESSION, null) else null
    }


    fun getUserProfessionForDisplay(userId: String, databaseTitle: String? = null): String? {
        return if (userId == getUserId()) {

            getUserProfession() ?: databaseTitle
        } else {

            databaseTitle
        }
    }


    fun syncUserDataFromDatabase(context: android.content.Context) {
        if (!isLoggedIn() || isSyncing) return

        val userId = getUserId() ?: return


        isSyncing = true


        CoroutineScope(Dispatchers.IO + SupervisorJob()).launch {
            try {
                val userRepository = com.uilover.project196.Repository.UserRepository.getInstance(context)
                val userEntity = userRepository.getUserById(userId)

                if (userEntity != null) {
                    android.util.Log.d("UserSession", "syncUserDataFromDatabase: Found user data - role: ${userEntity.role}, title: ${userEntity.title}")

                    val currentRole = getUserRole()


                    prefs.edit().apply {

                        putString(KEY_USER_ROLE, userEntity.role)


                        if (!userEntity.title.isNullOrEmpty()) {
                            putString(KEY_USER_PROFESSION, userEntity.title)
                        }

                        apply()
                    }

                    android.util.Log.d("UserSession", "syncUserDataFromDatabase: ✅ Synced user data - role: '${userEntity.role}', profession: '${userEntity.title}' from database")


                    if (currentRole != userEntity.role) {
                        withContext(Dispatchers.Main) {
                            notifyLoginStateChanged(true)
                        }
                    }
                } else {
                    android.util.Log.w("UserSession", "syncUserDataFromDatabase: ⚠️ User not found in database for ID: $userId")
                }
            } catch (e: Exception) {
                android.util.Log.e("UserSession", "syncUserDataFromDatabase: ❌ Error syncing user data", e)
            } finally {

                isSyncing = false
            }
        }
    }

    fun updateUserProfession(profession: String, context: android.content.Context? = null): Boolean {
        return if (::prefs.isInitialized && isLoggedIn()) {
            val userId = getUserId()
            if (userId == null) {
                android.util.Log.e("UserSession", "updateUserProfession: No user ID available")
                return false
            }


            prefs.edit().apply {
                putString(KEY_USER_PROFESSION, profession)
                apply()
            }


            context?.let { ctx ->

                CoroutineScope(Dispatchers.IO + SupervisorJob()).launch {
                    try {
                        val userRepository = com.uilover.project196.Repository.UserRepository.getInstance(ctx)
                        val databaseUpdated = userRepository.updateUserProfession(userId, profession)

                        if (databaseUpdated) {
                            android.util.Log.d("UserSession", "updateUserProfession: ✅ Both session and database updated successfully")
                        } else {
                            android.util.Log.w("UserSession", "updateUserProfession: ⚠️ Session updated but database update failed")
                        }
                    } catch (e: Exception) {
                        android.util.Log.e("UserSession", "updateUserProfession: ❌ Error updating database", e)
                    }
                }
            }


            notifyProfessionChanged(profession)


            context?.let {
                try {
                    val intent = android.content.Intent("com.uilover.project196.PROFESSION_CHANGED")
                    intent.putExtra("user_id", userId)
                    intent.putExtra("new_profession", profession)
                    it.sendBroadcast(intent)
                } catch (e: Exception) {
                    android.util.Log.e("UserSession", "updateUserProfession: Error sending broadcast", e)
                }
            }

            true
        } else {
            false
        }
    }

    fun login(userId: String, userName: String, userEmail: String, userRole: String = ROLE_FREELANCER, profession: String? = null, context: android.content.Context? = null) {
        if (::prefs.isInitialized) {
            val wasLoggedIn = isLoggedIn()
            prefs.edit().apply {
                putBoolean(KEY_IS_LOGGED_IN, true)
                putString(KEY_USER_ID, userId)
                putString(KEY_USER_NAME, userName)
                putString(KEY_USER_EMAIL, userEmail)
                putString(KEY_USER_ROLE, userRole)
                if (profession != null) {
                    putString(KEY_USER_PROFESSION, profession)
                }
                apply()
            }

            android.util.Log.d("UserSession", "=== USER LOGIN COMPLETE ===")
            android.util.Log.d("UserSession", "User ID: $userId")
            android.util.Log.d("UserSession", "User Name (for display): $userName")
            android.util.Log.d("UserSession", "User Email: $userEmail")
            android.util.Log.d("UserSession", "User Role (app role): $userRole")
            android.util.Log.d("UserSession", "Role Display Name: ${getRoleDisplayName()}")
            android.util.Log.d("UserSession", "Initial Profession: $profession")



            context?.let { ctx ->
                CoroutineScope(Dispatchers.IO + SupervisorJob()).launch {
                    try {
                        val userRepository = com.uilover.project196.Repository.UserRepository.getInstance(ctx)


                        android.util.Log.d("UserSession", "=== ENSURING USER EXISTS IN DATABASE ===")
                        val userCreated = userRepository.ensureUserInDatabase(userId, userName, userEmail, userRole)
                        android.util.Log.d("UserSession", "ensureUserInDatabase result: $userCreated")


                        val userEntity = userRepository.getUserById(userId)

                        if (userEntity != null) {
                            android.util.Log.d("UserSession", "=== SYNCING DATA FROM DATABASE ===")
                            android.util.Log.d("UserSession", "Database Role: '${userEntity.role}'")
                            android.util.Log.d("UserSession", "Database Title: '${userEntity.title}'")
                            android.util.Log.d("UserSession", "Database Company: '${userEntity.companyName}'")


                            prefs.edit().apply {
                                putString(KEY_USER_ROLE, userEntity.role)
                                if (!userEntity.title.isNullOrEmpty()) {
                                    putString(KEY_USER_PROFESSION, userEntity.title)
                                    android.util.Log.d("UserSession", "✅ Updated profession to: '${userEntity.title}'")
                                } else {
                                    android.util.Log.w("UserSession", "⚠️ No title/profession found in database")
                                }
                                apply()
                            }
                            android.util.Log.d("UserSession", "✅ Login sync complete - role '${userEntity.role}', profession '${userEntity.title}', company '${userEntity.companyName}' from database for user $userId")
                        } else {
                            android.util.Log.e("UserSession", "❌ User still not found in database after ensureUserInDatabase - this is a serious issue")
                        }
                    } catch (e: Exception) {
                        android.util.Log.e("UserSession", "❌ Error syncing user data during login", e)
                    }
                }
            }


            if (!wasLoggedIn) {
                notifyLoginStateChanged(true)
            }
        }
    }


    fun syncProfessionFromDatabase(context: android.content.Context) {
        if (!isLoggedIn()) return

        val userId = getUserId() ?: return


        CoroutineScope(Dispatchers.IO + SupervisorJob()).launch {
            try {
                val userRepository = com.uilover.project196.Repository.UserRepository.getInstance(context)
                val userEntity = userRepository.getUserById(userId)

                if (userEntity != null && !userEntity.title.isNullOrEmpty()) {

                    val currentProfession = getUserProfession()
                    if (currentProfession.isNullOrEmpty()) {
                        prefs.edit().apply {
                            putString(KEY_USER_PROFESSION, userEntity.title)
                            apply()
                        }
                        android.util.Log.d("UserSession", "syncProfessionFromDatabase: ✅ Synced profession '${userEntity.title}' from database")
                    }
                }
            } catch (e: Exception) {
                android.util.Log.e("UserSession", "syncProfessionFromDatabase: ❌ Error syncing profession", e)
            }
        }
    }

    fun logout() {
        if (::prefs.isInitialized) {
            val wasLoggedIn = isLoggedIn()


            val userId = getUserId()
            if (userId != null) {
                val mainRepository = com.uilover.project196.Repository.MainRepository.getInstance()
                mainRepository.clearUserBookmarks(userId)
            }

            prefs.edit().clear().apply()


            if (wasLoggedIn) {
                notifyLoginStateChanged(false)
            }
        }
    }


    fun isGuestUser(): Boolean {
        return !isLoggedIn()
    }


    fun simulateFreelancerLogin(context: android.content.Context? = null) {
        if (::prefs.isInitialized) {


            login("john_doe_freelancer", "John Doe", "john.doe@email.com", ROLE_FREELANCER, context = context)
        }
    }


    fun simulateAlexJohnsonLogin(context: android.content.Context? = null) {
        if (::prefs.isInitialized) {

            login("freelancer_001", "Alex Johnson", "alex.johnson@email.com", ROLE_FREELANCER, context = context)
        }
    }


    fun simulateBusinessOwnerLogin(context: android.content.Context? = null) {
        if (::prefs.isInitialized) {
            login("user_002", "Sarah Johnson", "sarah.johnson@chaboksoft.com", ROLE_BUSINESS_OWNER, context = context)
        }
    }


    fun getCompanyName(): String? {
        val userRole = getUserRole()


        if (userRole == ROLE_BUSINESS_OWNER) {
            return null // Let ProfileViewModel read from user profile data
        }


        val email = getUserEmail()
        val name = getUserName()

        return when {
            email?.contains("@chaboksoft.com") == true -> "ChabokSoft"
            email?.contains("@kiansoft.com") == true -> "KianSoft"
            email?.contains("@makansoft.com") == true -> "MakanSoft"
            email?.contains("@testsoft.com") == true -> "TestSoft"

            email?.contains("@") == true -> {
                val domain = email.substringAfter("@").substringBefore(".")
                domain.replaceFirstChar { it.uppercase() }
            }

            name != null -> "${name.split(" ").firstOrNull() ?: "User"} Company"
            else -> "Unknown Company"
        }
    }


    fun simulateLogin(context: android.content.Context? = null) {
        simulateFreelancerLogin(context)
    }


    fun getRoleDisplayName(): String {
        return when (getUserRole()) {
            ROLE_FREELANCER -> "Freelancer"
            ROLE_BUSINESS_OWNER -> "Business Owner"
            else -> "User"
        }
    }


    fun getCurrentUserInfo(): String {
        return if (isLoggedIn()) {
            "User: ${getUserName()} (${getRoleDisplayName()}) - ID: ${getUserId()}"
        } else {
            "Guest User"
        }
    }


    fun addLoginStateListener(listener: LoginStateListener) {
        if (!loginStateListeners.contains(listener)) {
            loginStateListeners.add(listener)
        }
    }

    fun removeLoginStateListener(listener: LoginStateListener) {
        loginStateListeners.remove(listener)
    }


    fun addProfessionChangeListener(listener: ProfessionChangeListener) {
        if (!professionChangeListeners.contains(listener)) {
            professionChangeListeners.add(listener)
        }
    }

    fun removeProfessionChangeListener(listener: ProfessionChangeListener) {
        professionChangeListeners.remove(listener)
    }

    private fun notifyLoginStateChanged(isLoggedIn: Boolean) {

        val listenersCopy = loginStateListeners.toList()
        listenersCopy.forEach { listener ->
            try {
                listener.onLoginStateChanged(isLoggedIn)
                listener.onLoginStateRefresh(isLoggedIn)
            } catch (e: Exception) {

                android.util.Log.w("UserSession", "Error notifying login state listener", e)
            }
        }
    }

    private fun notifyProfessionChanged(newProfession: String) {

        val listenersCopy = professionChangeListeners.toList()
        listenersCopy.forEach { listener ->
            try {
                listener.onProfessionChanged(newProfession)
            } catch (e: Exception) {

                android.util.Log.w("UserSession", "Error notifying profession change listener", e)
            }
        }
    }
}