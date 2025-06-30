package com.uilover.project196.ViewModel

import android.app.Application
import androidx.databinding.ObservableField
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.LiveData
import androidx.lifecycle.MutableLiveData
import androidx.lifecycle.viewModelScope
import com.uilover.project196.Model.JobModel
import com.uilover.project196.Repository.UserRepository
import com.uilover.project196.Utils.UserSession
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext

// KRITERIA WAJIB: ViewModel (8/12) - ViewModel untuk aplikasi pekerjaan
class JobApplicationViewModel(application: Application) : AndroidViewModel(application) {

    private var userRepository: UserRepository? = null




    private fun getRepository(): UserRepository? {
        if (userRepository == null) {
            try {
                userRepository = UserRepository.getInstance(getApplication())
                android.util.Log.d("JobApplicationViewModel", "UserRepository initialized successfully")
            } catch (e: Exception) {
                android.util.Log.e("JobApplicationViewModel", "Error getting UserRepository", e)
                return null
            }
        }
        return userRepository
    }




    private val _applicationResult = MutableLiveData<Boolean?>()
    val applicationResult: LiveData<Boolean?> = _applicationResult

    private val _showSuccessMessage = MutableLiveData<String?>()
    val showSuccessMessage: LiveData<String?> = _showSuccessMessage

    private val _showErrorMessage = MutableLiveData<String?>()
    val showErrorMessage: LiveData<String?> = _showErrorMessage

    private val _isSubmitting = MutableLiveData<Boolean>()
    val isSubmitting: LiveData<Boolean> = _isSubmitting






    val skillsInput = ObservableField<String>("")
    val descriptionInput = ObservableField<String>("")


    val skillsError = ObservableField<String>("")
    val descriptionError = ObservableField<String>("")
    val isFormValid = ObservableField<Boolean>(false)


    val showLoadingState = ObservableField<Boolean>(false)
    val submitButtonEnabled = ObservableField<Boolean>(false)
    val submitButtonText = ObservableField<String>("Submit Application")


    val jobTitle = ObservableField<String>("")
    val companyName = ObservableField<String>("")
    val characterCount = ObservableField<String>("0")


    private var skillsInputValue = ""
    private var descriptionInputValue = ""





    fun initializeForm(job: JobModel) {
        jobTitle.set(job.title)
        companyName.set(job.company)
        resetForm()
    }

    fun validateSkills(skills: String) {
        skillsInputValue = skills
        skillsInput.set(skills)


        when {
            skills.trim().isEmpty() -> {
                skillsError.set("Skills is required")
            }
            skills.trim().length < 3 -> {
                skillsError.set("Skills must be at least 3 characters")
            }
            else -> {
                skillsError.set("")
            }
        }

        updateFormValidation()
    }

    fun validateDescription(description: String) {
        descriptionInputValue = description
        descriptionInput.set(description)


        characterCount.set("${description.length}/500")


        when {
            description.trim().isEmpty() -> {
                descriptionError.set("Cover letter is required")
            }
            description.trim().length < 10 -> {
                descriptionError.set("Cover letter must be at least 10 characters")
            }
            description.trim().length > 500 -> {
                descriptionError.set("Cover letter cannot exceed 500 characters")
            }
            else -> {
                descriptionError.set("")
            }
        }

        updateFormValidation()
    }

    private fun updateFormValidation() {

        val skillsValid = skillsInputValue.trim().isNotEmpty() && skillsInputValue.trim().length >= 3
        val descriptionValid = descriptionInputValue.trim().isNotEmpty() &&
                             descriptionInputValue.trim().length >= 10 &&
                             descriptionInputValue.trim().length <= 500

        val formValid = skillsValid && descriptionValid
        isFormValid.set(formValid)


        val buttonEnabled = formValid && !(showLoadingState.get() ?: false)
        submitButtonEnabled.set(buttonEnabled)
    }

    fun submitApplication(job: JobModel, mainViewModel: com.uilover.project196.ViewModel.MainViewModel) {
        val currentUserId = UserSession.getUserId()
        if (currentUserId == null) {
            _showErrorMessage.value = "Please login to apply for jobs"
            return
        }

        val skills = skillsInputValue.trim()
        val description = descriptionInputValue.trim()


        if (skills.isEmpty() || description.isEmpty()) {
            _showErrorMessage.value = "Please fill all required fields"
            return
        }


        _isSubmitting.value = true
        showLoadingState.set(true)
        submitButtonEnabled.set(false)
        submitButtonText.set("Submitting...")

        viewModelScope.launch {
            try {
                val success = withContext(Dispatchers.IO) {
                    val repository = getRepository()
                    if (repository == null) {
                        android.util.Log.e("JobApplicationViewModel", "UserRepository not available")
                        return@withContext false
                    }


                    val jobEntity = mainViewModel.findJobEntity(job)
                    if (jobEntity != null) {
                        repository.applyForJob(
                            jobId = jobEntity.id,
                            userId = currentUserId,
                            coverLetter = "Applied through the app",
                            proposedRate = "",
                            skills = skills,
                            description = description
                        )
                    } else {
                        false
                    }
                }

                _applicationResult.value = success

                if (success) {
                    _showSuccessMessage.value = "Application submitted successfully!"
                } else {
                    _showErrorMessage.value = "You have already applied to this job"
                }

            } catch (e: Exception) {
                _showErrorMessage.value = "Error submitting application: ${e.message}"
                _applicationResult.value = false
            } finally {
                _isSubmitting.value = false
                showLoadingState.set(false)
                resetSubmissionState()
            }
        }
    }

    private fun resetSubmissionState() {
        submitButtonText.set("Submit Application")
        updateFormValidation()
    }

    private fun resetForm() {
        skillsInputValue = ""
        descriptionInputValue = ""
        skillsInput.set("")
        descriptionInput.set("")
        skillsError.set("")
        descriptionError.set("")
        characterCount.set("0/500")
        isFormValid.set(false)
        submitButtonEnabled.set(false)
        showLoadingState.set(false)
        submitButtonText.set("Submit Application")
    }





    fun onSuccessMessageShown() {
        _showSuccessMessage.value = null
    }

    fun onErrorMessageShown() {
        _showErrorMessage.value = null
    }

    fun onApplicationResultHandled() {
        _applicationResult.value = null
    }
}