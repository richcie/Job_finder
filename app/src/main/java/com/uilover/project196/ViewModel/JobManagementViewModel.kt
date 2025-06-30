package com.uilover.project196.ViewModel

import android.app.Application
import androidx.databinding.ObservableField
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.LiveData
import androidx.lifecycle.MutableLiveData
import androidx.lifecycle.viewModelScope

import com.uilover.project196.Database.AppDatabase
import com.uilover.project196.Model.JobModel
import com.uilover.project196.Repository.MainRepository
import com.uilover.project196.Repository.UserRepository
import com.uilover.project196.Utils.UserSession
import kotlinx.coroutines.launch

// KRITERIA WAJIB: ViewModel (7/12) - ViewModel untuk manajemen pekerjaan
class JobManagementViewModel(application: Application) : AndroidViewModel(application) {

    private val mainRepository = MainRepository.getInstance()


    private val _isLoading = MutableLiveData<Boolean>()
    val isLoading: LiveData<Boolean> = _isLoading

    private val _showSuccessMessage = MutableLiveData<String?>()
    val showSuccessMessage: LiveData<String?> = _showSuccessMessage

    private val _showErrorMessage = MutableLiveData<String?>()
    val showErrorMessage: LiveData<String?> = _showErrorMessage

    private val _currentJob = MutableLiveData<JobModel?>()
    val currentJob: LiveData<JobModel?> = _currentJob

    private val _jobCreated = MutableLiveData<Boolean>()
    val jobCreated: LiveData<Boolean> = _jobCreated

    private val _jobUpdated = MutableLiveData<Boolean>()
    val jobUpdated: LiveData<Boolean> = _jobUpdated

    private val _validationErrors = MutableLiveData<Map<String, String>>()
    val validationErrors: LiveData<Map<String, String>> = _validationErrors


    val jobTitle = ObservableField<String>("")
    val jobDescription = ObservableField<String>("")
    val selectedSalary = ObservableField<String>("")
    val selectedJobType = ObservableField<String>("")
    val selectedWorkingModel = ObservableField<String>("")
    val selectedLevel = ObservableField<String>("")


    val jobTitleError = ObservableField<String>("")
    val jobDescriptionError = ObservableField<String>("")
    val salaryError = ObservableField<String>("")
    val jobTypeError = ObservableField<String>("")
    val workingModelError = ObservableField<String>("")
    val levelError = ObservableField<String>("")


    val isFormValid = ObservableField<Boolean>(false)
    val submitButtonEnabled = ObservableField<Boolean>(false)
    val submitButtonText = ObservableField<String>("Create Job")


    val descriptionCharacterCount = ObservableField<String>("0/500")


    val companyName = ObservableField<String>("")
    val companyLogo = ObservableField<String>("")


    private var jobTitleValue = ""
    private var jobDescriptionValue = ""

    init {
        refreshCompanyInfo()
    }

    fun refreshCompanyInfo() {
        val userRole = UserSession.getUserRole()
        
        if (userRole == UserSession.ROLE_BUSINESS_OWNER) {
            // For business owners, get company name from user profile data
            val userId = UserSession.getUserId()
            if (userId != null) {
                viewModelScope.launch {
                    try {
                        val userRepository = com.uilover.project196.Repository.UserRepository.getInstance(getApplication())
                        val userEntity = userRepository.getUserById(userId)
                        val company = userEntity?.companyName ?: "Your Company"
                        companyName.set(company)
                        companyLogo.set(getCompanyLogo(company))
                        android.util.Log.d("JobManagementViewModel", "✅ Set company name for business owner: '$company'")
                    } catch (e: Exception) {
                        android.util.Log.e("JobManagementViewModel", "❌ Error getting company name from database", e)
                        companyName.set("Unknown Company")
                        companyLogo.set(getCompanyLogo("Unknown Company"))
                    }
                }
            } else {
                companyName.set("Unknown Company")
                companyLogo.set(getCompanyLogo("Unknown Company"))
            }
        } else {
            // For freelancers, use UserSession logic
            val company = UserSession.getCompanyName() ?: "Unknown Company"
            companyName.set(company)
            companyLogo.set(getCompanyLogo(company))
        }
    }

    /**
     * Explicitly set company name - used by fragments when they have the correct company name
     */
    fun setCompanyName(company: String) {
        companyName.set(company)
        companyLogo.set(getCompanyLogo(company))
        android.util.Log.d("JobManagementViewModel", "✅ Company name explicitly set to: '$company'")
    }

    fun validateJobTitle(title: String) {
        jobTitleValue = title
        jobTitle.set(title)


        when {
            title.trim().isEmpty() -> {
                jobTitleError.set("Job title is required")
            }
            title.trim().length < 3 -> {
                jobTitleError.set("Job title must be at least 3 characters")
            }
            title.trim().length > 100 -> {
                jobTitleError.set("Job title cannot exceed 100 characters")
            }
            else -> {
                jobTitleError.set("")
            }
        }

        updateFormValidation()
    }

    fun validateJobDescription(description: String) {
        jobDescriptionValue = description
        jobDescription.set(description)


        descriptionCharacterCount.set("${description.length}/500")


        when {
            description.trim().isEmpty() -> {
                jobDescriptionError.set("Job description is required")
            }
            description.trim().length < 10 -> {
                jobDescriptionError.set("Job description must be at least 10 characters")
            }
            description.trim().length > 500 -> {
                jobDescriptionError.set("Job description cannot exceed 500 characters")
            }
            else -> {
                jobDescriptionError.set("")
            }
        }

        updateFormValidation()
    }

    fun validateSalary(salary: String) {
        selectedSalary.set(salary)


        if (salary.trim().isEmpty()) {
            salaryError.set("Please select a salary range")
        } else {
            salaryError.set("")
        }

        updateFormValidation()
    }

    fun validateJobType(jobType: String) {
        selectedJobType.set(jobType)


        if (jobType.trim().isEmpty()) {
            jobTypeError.set("Please select a job type")
        } else {
            jobTypeError.set("")
        }

        updateFormValidation()
    }

    fun validateWorkingModel(workingModel: String) {
        selectedWorkingModel.set(workingModel)


        if (workingModel.trim().isEmpty()) {
            workingModelError.set("Please select a working model")
        } else {
            workingModelError.set("")
        }

        updateFormValidation()
    }

    fun validateLevel(level: String) {
        selectedLevel.set(level)


        if (level.trim().isEmpty()) {
            levelError.set("Please select an experience level")
        } else {
            levelError.set("")
        }

        updateFormValidation()
    }

    private fun updateFormValidation() {

        val titleValid = jobTitleValue.trim().isNotEmpty() &&
                        jobTitleValue.trim().length >= 3 &&
                        jobTitleValue.trim().length <= 100

        val descriptionValid = jobDescriptionValue.trim().isNotEmpty() &&
                             jobDescriptionValue.trim().length >= 10 &&
                             jobDescriptionValue.trim().length <= 500

        val salaryValid = selectedSalary.get()?.trim()?.isNotEmpty() ?: false
        val jobTypeValid = selectedJobType.get()?.trim()?.isNotEmpty() ?: false
        val workingModelValid = selectedWorkingModel.get()?.trim()?.isNotEmpty() ?: false
        val levelValid = selectedLevel.get()?.trim()?.isNotEmpty() ?: false

        val formValid = titleValid && descriptionValid && salaryValid && jobTypeValid && workingModelValid && levelValid
        isFormValid.set(formValid)


        val buttonEnabled = formValid && !(_isLoading.value ?: false)
        submitButtonEnabled.set(buttonEnabled)
    }


    fun clearValidationErrors() {
        jobTitleError.set("")
        jobDescriptionError.set("")
        salaryError.set("")
        jobTypeError.set("")
        workingModelError.set("")
        levelError.set("")
    }


    fun validateAllFieldsOnSubmit() {

        validateJobTitle(jobTitleValue)
        validateJobDescription(jobDescriptionValue)
        validateSalary(selectedSalary.get() ?: "")
        validateJobType(selectedJobType.get() ?: "")
        validateWorkingModel(selectedWorkingModel.get() ?: "")
        validateLevel(selectedLevel.get() ?: "")

        android.util.Log.d("JobManagementViewModel", "✅ Validated all fields on submit attempt")
        android.util.Log.d("JobManagementViewModel", "   Title error: '${jobTitleError.get()}'")
        android.util.Log.d("JobManagementViewModel", "   Description error: '${jobDescriptionError.get()}'")
        android.util.Log.d("JobManagementViewModel", "   Salary error: '${salaryError.get()}'")
        android.util.Log.d("JobManagementViewModel", "   JobType error: '${jobTypeError.get()}'")
        android.util.Log.d("JobManagementViewModel", "   WorkingModel error: '${workingModelError.get()}'")
        android.util.Log.d("JobManagementViewModel", "   Level error: '${levelError.get()}'")
        android.util.Log.d("JobManagementViewModel", "   Form valid: ${isFormValid.get()}")
    }


    fun initializeFormForEdit(job: JobModel) {
        submitButtonText.set("Update Job")
        loadJobForEditing(job)

        validateJobTitle(job.title)
        validateJobDescription(job.description)
        validateSalary(job.salary)
        validateJobType(job.time)
        validateWorkingModel(job.model)
        validateLevel(job.level)
    }


    fun initializeFormForCreate() {
        submitButtonText.set("Create Job")
        clearForm()
        clearValidationErrors()
    }

    fun validateForm(): Boolean {
        val errors = mutableMapOf<String, String>()

        val title = jobTitle.get()?.trim() ?: ""
        val description = jobDescription.get()?.trim() ?: ""
        val salary = selectedSalary.get() ?: ""
        val jobType = selectedJobType.get() ?: ""
        val workingModel = selectedWorkingModel.get() ?: ""
        val level = selectedLevel.get() ?: ""


        if (title.isEmpty()) {
            errors["title"] = "Job title is required"
        } else if (title.length < 3) {
            errors["title"] = "Job title must be at least 3 characters"
        }

        if (description.isEmpty()) {
            errors["description"] = "Job description is required"
        } else if (description.length < 10) {
            errors["description"] = "Job description must be at least 10 characters"
        }

        if (salary.isEmpty()) {
            errors["salary"] = "Please select a salary range"
        }

        if (jobType.isEmpty()) {
            errors["jobType"] = "Please select a job type"
        }

        if (workingModel.isEmpty()) {
            errors["workingModel"] = "Please select a working model"
        }

        if (level.isEmpty()) {
            errors["level"] = "Please select an experience level"
        }

        _validationErrors.value = errors
        val isValid = errors.isEmpty()
        isFormValid.set(isValid)

        return isValid
    }

    fun createJob(onComplete: (Boolean) -> Unit) {
        android.util.Log.d("JobManagementViewModel", "🚀 createJob() called")


        validateAllFieldsOnSubmit()

        if (!(isFormValid.get() ?: false)) {
            android.util.Log.e("JobManagementViewModel", "❌ Form validation failed - calling onComplete(false)")
            _showErrorMessage.value = "Please fix the form errors before submitting"
            onComplete(false)
            return
        }

        if (!UserSession.isLoggedIn() || UserSession.getUserRole() != UserSession.ROLE_BUSINESS_OWNER) {
            _showErrorMessage.value = "Only business owners can create jobs"
            onComplete(false)
            return
        }


        _isLoading.value = true
        submitButtonEnabled.set(false)
        submitButtonText.set("Creating...")

        viewModelScope.launch {
            try {
                val jobCompanyName = companyName.get() ?: ""
                android.util.Log.d("JobManagementViewModel", "📝 Creating job with company name: '$jobCompanyName'")
                
                val newJob = JobModel(
                    title = jobTitle.get()?.trim() ?: "",
                    company = jobCompanyName,
                    picUrl = companyLogo.get() ?: "logo1",
                    time = selectedJobType.get() ?: "",
                    model = selectedWorkingModel.get() ?: "",
                    level = selectedLevel.get() ?: "",
                    location = "",
                    salary = selectedSalary.get() ?: "",
                    category = "2",
                    about = jobDescription.get()?.trim() ?: "",
                    description = jobDescription.get()?.trim() ?: "",
                    isBookmarked = false,
                    ownerId = UserSession.getUserId(),
                    status = "open"
                )

                val success = mainRepository.createJob(newJob)

                if (success) {
                    android.util.Log.d("JobManagementViewModel", "✅ Job created successfully - calling onComplete(true)")
                    _showSuccessMessage.value = "Job '${newJob.title}' created successfully!"
                    clearForm()
                    clearValidationErrors()
                    submitButtonText.set("Create Job")
                } else {
                    android.util.Log.e("JobManagementViewModel", "❌ Repository failed to create job - calling onComplete(false)")
                    _showErrorMessage.value = "Failed to create job. Please try again."
                    submitButtonText.set("Create Job")
                }
                onComplete(success)
            } catch (e: Exception) {
                android.util.Log.e("JobManagementViewModel", "❌ Exception creating job - calling onComplete(false)", e)
                _showErrorMessage.value = "Error creating job: ${e.message}"
                submitButtonText.set("Create Job")
                onComplete(false)
            } finally {
                _isLoading.value = false
                submitButtonEnabled.set(true)
            }
        }
    }

    fun loadJobForEditing(job: JobModel) {
        _currentJob.value = job


        jobTitle.set(job.title)
        jobDescription.set(job.description)
        selectedSalary.set(job.salary)
        selectedJobType.set(job.time)
        selectedWorkingModel.set(job.model)
        selectedLevel.set(job.level)

        validateForm()
    }

    fun updateJob(onComplete: (Boolean) -> Unit) {
        val currentJobValue = _currentJob.value
        if (currentJobValue == null) {
            _showErrorMessage.value = "No job selected for editing"
            onComplete(false)
            return
        }


        validateAllFieldsOnSubmit()

        if (!(isFormValid.get() ?: false)) {
            _showErrorMessage.value = "Please fix the form errors before submitting"
            onComplete(false)
            return
        }


        _isLoading.value = true
        submitButtonEnabled.set(false)
        submitButtonText.set("Updating...")

        viewModelScope.launch {
            try {
                val updatedJob = currentJobValue.copy(
                    title = jobTitle.get()?.trim() ?: "",
                    location = "",
                    salary = selectedSalary.get() ?: "",
                    time = selectedJobType.get() ?: "",
                    model = selectedWorkingModel.get() ?: "",
                    level = selectedLevel.get() ?: "",
                    description = jobDescription.get()?.trim() ?: "",
                    about = jobDescription.get()?.trim() ?: ""
                )

                android.util.Log.d("JobManagementViewModel", "🔧 Calling repository.updateJob...")
                val success = mainRepository.updateJob(updatedJob)
                android.util.Log.d("JobManagementViewModel", "🔧 Repository update result: $success")

                if (success) {
                    android.util.Log.d("JobManagementViewModel", "✅ Job updated successfully - calling onComplete(true)")
                    _currentJob.value = updatedJob
                    _showSuccessMessage.value = "Job updated successfully!"
                    submitButtonText.set("Update Job")
                    onComplete(true)
                } else {
                    android.util.Log.e("JobManagementViewModel", "❌ Repository failed to update job - calling onComplete(false)")
                    _showErrorMessage.value = "Failed to update job. Please try again."
                    submitButtonText.set("Update Job")
                    onComplete(false)
                }

            } catch (e: Exception) {
                android.util.Log.e("JobManagementViewModel", "❌ Exception updating job - calling onComplete(false)", e)
                _showErrorMessage.value = "Error updating job: ${e.message}"
                submitButtonText.set("Update Job")
                onComplete(false)
            } finally {
                _isLoading.value = false
                submitButtonEnabled.set(true)
            }
        }
    }

    fun closeJob(job: JobModel) {
        _isLoading.value = true

        viewModelScope.launch {
            try {
                val success = mainRepository.closeJob(job)

                if (success) {
                    val closedJob = job.copy(status = "closed")
                    _currentJob.value = closedJob
                    _showSuccessMessage.value = "Job closed successfully"
                    _jobUpdated.value = true
                } else {
                    _showErrorMessage.value = "Failed to close job"
                }
            } catch (e: Exception) {
                _showErrorMessage.value = "Error closing job: ${e.message}"
            } finally {
                _isLoading.value = false
            }
        }
    }

    fun reopenJob(job: JobModel) {
        _isLoading.value = true

        viewModelScope.launch {
            try {
                val success = mainRepository.reopenJob(job)

                if (success) {
                    val reopenedJob = job.copy(status = "open")
                    _currentJob.value = reopenedJob
                    _showSuccessMessage.value = "Job reopened successfully"
                    _jobUpdated.value = true
                } else {
                    _showErrorMessage.value = "Failed to reopen job"
                }
            } catch (e: Exception) {
                _showErrorMessage.value = "Error reopening job: ${e.message}"
            } finally {
                _isLoading.value = false
            }
        }
    }

    fun clearForm() {

        jobTitle.set("")
        jobDescription.set("")
        selectedSalary.set("")
        selectedJobType.set("")
        selectedWorkingModel.set("")
        selectedLevel.set("")


        jobTitleValue = ""
        jobDescriptionValue = ""


        isFormValid.set(false)
        submitButtonEnabled.set(false)
        submitButtonText.set("Create Job")
        descriptionCharacterCount.set("0/500")


        clearValidationErrors()
        _validationErrors.value = emptyMap()
    }

    fun getFilterOptions() = mainRepository.getFilterOptions()

    private fun getCompanyLogo(companyName: String): String {
        return when (companyName) {
            "ChabokSoft", "Chaboksoft" -> "logo1"
            "KianSoft" -> "logo2"
            "MakanSoft" -> "logo3"
            "TestSoft" -> "logo4"
            else -> "logo1"
        }
    }


    fun getJobStatusDisplay(job: JobModel): String {
        return if (job.isOpen()) "Active" else "Closed"
    }

    fun getJobStatusDescription(job: JobModel): String {
        return if (job.isOpen()) {
            "Closing this job will remove it from public listings and stop new applications."
        } else {
            "This job is closed. Reopening will make it visible in public listings again."
        }
    }

    fun canManageJob(job: JobModel): Boolean {
        return job.isOwnedByCurrentUser() && UserSession.getUserRole() == UserSession.ROLE_BUSINESS_OWNER
    }


    fun onSuccessMessageShown() {
        _showSuccessMessage.value = null
    }

    fun onErrorMessageShown() {
        _showErrorMessage.value = null
    }

    fun onJobCreatedHandled() {
        _jobCreated.value = false
    }

    fun onJobUpdatedHandled() {
        _jobUpdated.value = false
    }
}