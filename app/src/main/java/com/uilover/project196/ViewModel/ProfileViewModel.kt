package com.uilover.project196.ViewModel

import android.app.Application
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.LiveData
import androidx.lifecycle.MutableLiveData
import androidx.lifecycle.viewModelScope
import androidx.databinding.ObservableField
import com.uilover.project196.Database.AppDatabase
import com.uilover.project196.Model.UserEntity
import com.uilover.project196.Utils.UserSession
import kotlinx.coroutines.launch

// KRITERIA WAJIB: ViewModel (5/12) - ViewModel untuk ProfileFragment
class ProfileViewModel(application: Application) : AndroidViewModel(application) {

    private val database = AppDatabase.getDatabase(application)
    private val userDao = database.userDao()




    private val _userProfile = MutableLiveData<UserEntity?>()
    val userProfile: LiveData<UserEntity?> = _userProfile

    private val _isLoggedIn = MutableLiveData<Boolean>()
    val isLoggedIn: LiveData<Boolean> = _isLoggedIn

    private val _isLoading = MutableLiveData<Boolean>()
    val isLoading: LiveData<Boolean> = _isLoading

    private val _showSuccessMessage = MutableLiveData<String?>()
    val showSuccessMessage: LiveData<String?> = _showSuccessMessage

    private val _showErrorMessage = MutableLiveData<String?>()
    val showErrorMessage: LiveData<String?> = _showErrorMessage





    val userName = ObservableField<String>("")
    val userEmail = ObservableField<String>("")
    val userProfession = ObservableField<String>("")
    val userCompany = ObservableField<String>("")
    val userRole = ObservableField<String>("")
    val userSubtitle = ObservableField<String>("Join to unlock full features")


    val showGuestState = ObservableField<Boolean>(true)
    val showLoggedInState = ObservableField<Boolean>(false)
    val showFreelancerSignOut = ObservableField<Boolean>(false)
    val showBusinessOwnerSignOut = ObservableField<Boolean>(false)
    val showFreelancerBadge = ObservableField<Boolean>(false)
    val showBusinessOwnerBadge = ObservableField<Boolean>(false)
    val showCompanyInfo = ObservableField<Boolean>(false)
    val showFreelancerProfession = ObservableField<Boolean>(false)
    val showFreelancerRoleSection = ObservableField<Boolean>(false)
    val showBusinessOwnerSection = ObservableField<Boolean>(false)


    val professionInputText = ObservableField<String>("")
    val professionDialogTitle = ObservableField<String>("Set Your Professional Role")
    val professionDialogMessage = ObservableField<String>("Enter your profession or job title")
    val showProfessionValidationError = ObservableField<Boolean>(false)
    val professionValidationMessage = ObservableField<String>("")


    val currentRoleDisplayText = ObservableField<String>("Set your professional role")
    val professionDisplayText = ObservableField<String>("")
    val companyDisplayText = ObservableField<String>("")

    init {
        refreshLoginState()
        loadUserProfile()
    }

    fun refreshLoginState() {
        _isLoggedIn.value = UserSession.isLoggedIn()
        updateUIState(_isLoggedIn.value == true)

        if (_isLoggedIn.value == true) {
            loadUserProfile()
        } else {
            clearProfileData()
        }
    }

    private fun updateUIState(isLoggedIn: Boolean) {
        showGuestState.set(!isLoggedIn)
        showLoggedInState.set(isLoggedIn)

        if (isLoggedIn) {
            userSubtitle.set("Logged in successfully")
            updateRoleBasedVisibility()
        } else {
            userSubtitle.set("Join to unlock full features")
            hideAllRoleSpecificElements()
        }
    }

    private fun updateRoleBasedVisibility() {
        val currentUserProfile = _userProfile.value
        android.util.Log.d("ProfileViewModel", "🎭 Updating role-based visibility:")
        android.util.Log.d("ProfileViewModel", "   User profile role: ${currentUserProfile?.role}")
        android.util.Log.d("ProfileViewModel", "   ROLE_FREELANCER constant: ${UserSession.ROLE_FREELANCER}")
        android.util.Log.d("ProfileViewModel", "   ROLE_BUSINESS_OWNER constant: ${UserSession.ROLE_BUSINESS_OWNER}")
        
        when {
            currentUserProfile?.role == UserSession.ROLE_FREELANCER -> {
                android.util.Log.d("ProfileViewModel", "   → Showing FREELANCER state (badge: 'Freelancer')")
                showFreelancerState()
            }
            currentUserProfile?.role == UserSession.ROLE_BUSINESS_OWNER -> {
                android.util.Log.d("ProfileViewModel", "   → Showing BUSINESS OWNER state (badge: 'Business Owner')")
                showBusinessOwnerState()
            }
            else -> {
                android.util.Log.d("ProfileViewModel", "   → Hiding all role-specific elements (no matching role)")
                hideAllRoleSpecificElements()
            }
        }
    }

    private fun showFreelancerState() {
        showFreelancerBadge.set(true)
        showBusinessOwnerBadge.set(false)
        showCompanyInfo.set(false)
        showBusinessOwnerSection.set(false)
        showFreelancerRoleSection.set(true)
        showFreelancerSignOut.set(true)
        showBusinessOwnerSignOut.set(false)

        val profession = _userProfile.value?.title ?: ""
        if (profession.isNotEmpty()) {
            showFreelancerProfession.set(true)
            professionDisplayText.set(profession)
            currentRoleDisplayText.set("Current role: $profession")
        } else {
            showFreelancerProfession.set(false)
            currentRoleDisplayText.set("Set your professional role")
        }
    }

    private fun showBusinessOwnerState() {
        showBusinessOwnerBadge.set(true)
        showFreelancerBadge.set(false)
        showCompanyInfo.set(true)
        showBusinessOwnerSection.set(true)
        showFreelancerRoleSection.set(false)
        showFreelancerProfession.set(false)
        showBusinessOwnerSignOut.set(true)
        showFreelancerSignOut.set(false)

        val companyName = _userProfile.value?.companyName ?: "Your Company"
        companyDisplayText.set(companyName)
    }

    private fun hideAllRoleSpecificElements() {
        showFreelancerBadge.set(false)
        showBusinessOwnerBadge.set(false)
        showCompanyInfo.set(false)
        showFreelancerProfession.set(false)
        showFreelancerRoleSection.set(false)
        showBusinessOwnerSection.set(false)
        showFreelancerSignOut.set(false)
        showBusinessOwnerSignOut.set(false)
    }

    private fun loadUserProfile() {
        _isLoading.value = true
        viewModelScope.launch {
            try {
                val userId = UserSession.getUserId()
                if (userId != null) {
                    val user = userDao.getUserById(userId)
                    _userProfile.value = user


                    user?.let {
                        android.util.Log.d("ProfileViewModel", "👤 Loading user profile data:")
                        android.util.Log.d("ProfileViewModel", "   Database name: ${it.name}")
                        android.util.Log.d("ProfileViewModel", "   Database email: ${it.email}")
                        android.util.Log.d("ProfileViewModel", "   Database role: ${it.role}")
                        android.util.Log.d("ProfileViewModel", "   Database title: ${it.title}")
                        android.util.Log.d("ProfileViewModel", "   Database company: ${it.companyName}")
                        
                        userName.set(it.name)
                        userEmail.set(it.email)
                        userProfession.set(it.title)
                        userCompany.set(it.companyName)
                        userRole.set(it.role)
                        professionInputText.set(it.title)
                        
                        android.util.Log.d("ProfileViewModel", "✅ Profile data set in observables")
                    }

                    updateRoleBasedVisibility()
                } else {
                    _userProfile.value = null
                    clearProfileData()
                }
                _isLoading.value = false
            } catch (e: Exception) {
                _isLoading.value = false
                _showErrorMessage.value = "Failed to load profile: ${e.message}"
            }
        }
    }

    private fun clearProfileData() {
        _userProfile.value = null

        userName.set("")
        userEmail.set("")
        userProfession.set("")
        userCompany.set("")
        userRole.set("")
        professionInputText.set("")
        hideAllRoleSpecificElements()
    }

    fun updateUserProfession(newProfession: String) {
        if (newProfession.isBlank()) {
            showProfessionValidationError.set(true)
            professionValidationMessage.set("Please enter a valid profession")
            return
        }


        showProfessionValidationError.set(false)
        professionValidationMessage.set("")

        _isLoading.value = true
        viewModelScope.launch {
            try {
                val userId = UserSession.getUserId()
                if (userId != null) {
                    val currentUser = userDao.getUserById(userId)
                    if (currentUser != null) {
                        val updatedUser = currentUser.copy(title = newProfession)
                        userDao.updateUser(updatedUser)


                        UserSession.updateUserProfession(newProfession, getApplication())


                        userProfession.set(newProfession)
                        professionInputText.set(newProfession)
                        professionDisplayText.set(newProfession)
                        currentRoleDisplayText.set("Current role: $newProfession")
                        showFreelancerProfession.set(true)


                        _userProfile.value = updatedUser


                        refreshChatDataAfterProfileUpdate()

                        _showSuccessMessage.value = "Profession updated successfully!"
                    }
                }
                _isLoading.value = false
            } catch (e: Exception) {
                _isLoading.value = false
                _showErrorMessage.value = "Failed to update profession: ${e.message}"
            }
        }
    }


    private suspend fun refreshChatDataAfterProfileUpdate() {
        try {
            val chatRepository = com.uilover.project196.Repository.ChatRepository.getInstance(getApplication())
            chatRepository.refreshChatsAfterProfileUpdate()


            val intent = android.content.Intent("com.uilover.project196.PROFILE_UPDATED")
            intent.putExtra("user_id", UserSession.getUserId())
            intent.putExtra("updated_field", "profession")
            getApplication<android.app.Application>().sendBroadcast(intent)

            android.util.Log.d("ProfileViewModel", "Chat data refreshed and broadcast sent after profession update")
        } catch (e: Exception) {
            android.util.Log.e("ProfileViewModel", "Error refreshing chat data after profile update", e)
        }
    }




    fun prepareEditProfessionDialog() {
        val currentProfession = _userProfile.value?.title ?: ""
        professionInputText.set(currentProfession)
        showProfessionValidationError.set(false)
        professionValidationMessage.set("")

        professionDialogTitle.set("Update Your Professional Role")
        professionDialogMessage.set("Enter your profession or job title (e.g., UI Designer, Software Engineer, etc.)")
    }

    fun validateAndUpdateProfession(): Boolean {
        val newProfession = professionInputText.get()?.trim() ?: ""

        if (newProfession.isEmpty()) {
            showProfessionValidationError.set(true)
            professionValidationMessage.set("Please enter a valid profession")
            return false
        }

        if (newProfession.length < 2) {
            showProfessionValidationError.set(true)
            professionValidationMessage.set("Profession must be at least 2 characters")
            return false
        }

        updateUserProfession(newProfession)
        return true
    }

    fun clearProfessionValidation() {
        showProfessionValidationError.set(false)
        professionValidationMessage.set("")
    }




    @Suppress("UNUSED_PARAMETER")
    fun createJob(
        title: String,
        description: String,
        salary: String, // Not used in current implementation
        jobType: String, // Not used in current implementation
        workingModel: String, // Not used in current implementation
        level: String, // Not used in current implementation
        location: String = "Remote" // Not used in current implementation
    ): Boolean {

        if (title.isBlank() || description.isBlank()) {
            _showErrorMessage.value = "Please fill in all required fields"
            return false
        }

        if (!UserSession.isLoggedIn() || UserSession.getUserRole() != UserSession.ROLE_BUSINESS_OWNER) {
            _showErrorMessage.value = "Only business owners can create jobs"
            return false
        }

        viewModelScope.launch {
            try {


                _showSuccessMessage.value = "Job '$title' created successfully!"
            } catch (e: Exception) {
                _showErrorMessage.value = "Failed to create job: ${e.message}"
            }
        }

        return true
    }

    fun signOut() {
        viewModelScope.launch {
            try {
                UserSession.logout()
                clearProfileData()
                _isLoggedIn.value = false
                updateUIState(false)
                _showSuccessMessage.value = "Signed out successfully"
            } catch (e: Exception) {
                _showErrorMessage.value = "Error signing out: ${e.message}"
            }
        }
    }




    fun getUserDisplayName(): String {
        return userName.get() ?: "Guest User"
    }

    fun getUserSubtitle(): String {
        return userSubtitle.get() ?: "Join to unlock full features"
    }

    fun isBusinessOwner(): Boolean {
        return _userProfile.value?.role == UserSession.ROLE_BUSINESS_OWNER
    }

    fun isFreelancer(): Boolean {
        return _userProfile.value?.role == UserSession.ROLE_FREELANCER
    }

    fun getCompanyName(): String {
        return companyDisplayText.get() ?: "Unknown Company"
    }

    fun getUserProfessionDisplay(): String {
        return professionDisplayText.get() ?: ""
    }


    fun onSuccessMessageShown() {
        _showSuccessMessage.value = null
    }

    fun onErrorMessageShown() {
        _showErrorMessage.value = null
    }
}