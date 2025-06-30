package com.uilover.project196.ViewModel

import android.app.Application

import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.LiveData
import androidx.lifecycle.MutableLiveData
import androidx.lifecycle.viewModelScope
import com.uilover.project196.Model.JobAttendanceEntity
import com.uilover.project196.Repository.FreelancerJobRepository
import com.uilover.project196.Utils.UserSession
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext

// KRITERIA WAJIB: ViewModel (12/12) - ViewModel untuk laporan progress
class ProgressReportViewModel(application: Application) : AndroidViewModel(application) {

    private var freelancerJobRepository: FreelancerJobRepository? = null




    private fun getRepository(): FreelancerJobRepository? {
        if (freelancerJobRepository == null) {
            try {
                freelancerJobRepository = FreelancerJobRepository.getInstance(getApplication())
                android.util.Log.d("ProgressReportViewModel", "FreelancerJobRepository initialized successfully")
            } catch (e: Exception) {
                android.util.Log.e("ProgressReportViewModel", "Error getting FreelancerJobRepository", e)
                return null
            }
        }
        return freelancerJobRepository
    }




    private val _checkOutResult = MutableLiveData<Boolean?>()
    val checkOutResult: LiveData<Boolean?> = _checkOutResult

    private val _showSuccessMessage = MutableLiveData<String?>()
    val showSuccessMessage: LiveData<String?> = _showSuccessMessage

    private val _showErrorMessage = MutableLiveData<String?>()
    val showErrorMessage: LiveData<String?> = _showErrorMessage

    private val _isSubmitting = MutableLiveData<Boolean>()
    val isSubmitting: LiveData<Boolean> = _isSubmitting




























    private var progressInputValue = ""
    private var jobTitleValue = ""
    private var companyNameValue = ""
    private var currentDateValue = ""





    fun initializeForm(jobTitle: String, companyName: String) {
        this.jobTitleValue = jobTitle
        this.companyNameValue = companyName
        setCurrentDate()
        resetForm()
    }

    private fun setCurrentDate() {
        val dateFormat = java.text.SimpleDateFormat("EEEE, dd MMMM yyyy", java.util.Locale.getDefault())
        currentDateValue = dateFormat.format(java.util.Date())
    }

    fun validateProgress(progressText: String) {
        progressInputValue = progressText





















        updateFormValidation()
    }

    private fun updateFormValidation() {






    }

    fun submitProgressReport(jobId: Int, @Suppress("UNUSED_PARAMETER") attendance: JobAttendanceEntity) {
        val currentUserId = UserSession.getUserId()
        if (currentUserId == null) {
            _showErrorMessage.value = "Please login to submit progress report"
            return
        }

        val progressText = progressInputValue.trim()


        if (progressText.length < 10) {
            _showErrorMessage.value = "Progress report must be at least 10 characters"
            return
        }


        _isSubmitting.value = true




        viewModelScope.launch {
            try {
                val success = withContext(Dispatchers.IO) {
                    val repository = getRepository()
                    if (repository == null) {
                        android.util.Log.e("ProgressReportViewModel", "FreelancerJobRepository not available")
                        return@withContext false
                    }

                    repository.checkOutFromJob(jobId, currentUserId, progressText)
                }

                _checkOutResult.value = success

                if (success) {
                    _showSuccessMessage.value = "Successfully checked out with progress report!"
                } else {
                    _showErrorMessage.value = "Failed to check out. Please try again."
                }

            } catch (e: Exception) {
                _showErrorMessage.value = "Error submitting progress report: ${e.message}"
                _checkOutResult.value = false
            } finally {
                _isSubmitting.value = false

                resetSubmissionState()
            }
        }
    }

    private fun resetSubmissionState() {

        updateFormValidation()
    }

    private fun resetForm() {
        progressInputValue = ""







    }





    fun getProgressReportPreview(): String {
        val progress = progressInputValue.trim()
        val job = jobTitleValue
        val company = companyNameValue
        val date = currentDateValue

        return "Progress Report for $job at $company\nDate: $date\n\n$progress"
    }

    fun isValidForSubmission(): Boolean {
        val progressText = progressInputValue.trim()
        return progressText.length >= 10 && progressText.length <= 1000
    }





    fun onSuccessMessageShown() {
        _showSuccessMessage.value = null
    }

    fun onErrorMessageShown() {
        _showErrorMessage.value = null
    }

    fun onCheckOutResultHandled() {
        _checkOutResult.value = null
    }
}