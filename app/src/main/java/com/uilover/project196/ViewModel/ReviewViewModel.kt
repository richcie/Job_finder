package com.uilover.project196.ViewModel

import android.app.Application
import androidx.databinding.ObservableField
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.LiveData
import androidx.lifecycle.MutableLiveData
import androidx.lifecycle.viewModelScope
import com.uilover.project196.Model.ReviewData
import com.uilover.project196.Model.ReviewEntity
import com.uilover.project196.Model.UserEntity
import com.uilover.project196.Repository.ReviewRepository
import com.uilover.project196.Repository.FreelancerJobRepository
import com.uilover.project196.Utils.UserSession
import com.uilover.project196.Database.AppDatabase
import kotlinx.coroutines.launch
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext

// KRITERIA WAJIB: ViewModel (10/12) - ViewModel untuk ReviewFragment
class ReviewViewModel(application: Application) : AndroidViewModel(application) {

    private val reviewRepository = ReviewRepository.getInstance(application)
    private val freelancerJobRepository = FreelancerJobRepository.getInstance(application)




    private val _reviewData = MutableLiveData<ReviewData>()
    val reviewData: LiveData<ReviewData> = _reviewData

    private val _recentReviews = MutableLiveData<List<ReviewEntity>>()
    val recentReviews: LiveData<List<ReviewEntity>> = _recentReviews

    private val _allReviews = MutableLiveData<List<ReviewEntity>>()
    val allReviews: LiveData<List<ReviewEntity>> = _allReviews

    private val _isLoading = MutableLiveData<Boolean>()
    val isLoading: LiveData<Boolean> = _isLoading

    private val _canWriteReview = MutableLiveData<Pair<Boolean, String>>()
    val canWriteReview: LiveData<Pair<Boolean, String>> = _canWriteReview

    private val _showSuccessMessage = MutableLiveData<String?>()
    val showSuccessMessage: LiveData<String?> = _showSuccessMessage

    private val _showErrorMessage = MutableLiveData<String?>()
    val showErrorMessage: LiveData<String?> = _showErrorMessage

    private val _businessOwnerInfo = MutableLiveData<UserEntity?>()
    val businessOwnerInfo: LiveData<UserEntity?> = _businessOwnerInfo






    val overallRating = ObservableField<String>("0.0")
    val totalReviewsText = ObservableField<String>("No reviews yet")
    val fiveStarCount = ObservableField<String>("0")
    val fourStarCount = ObservableField<String>("0")
    val threeStarCount = ObservableField<String>("0")
    val twoStarCount = ObservableField<String>("0")
    val oneStarCount = ObservableField<String>("0")


    val showLoadingState = ObservableField<Boolean>(false)
    val showContentState = ObservableField<Boolean>(false)
    val showEmptyState = ObservableField<Boolean>(false)
    val showWriteReviewButton = ObservableField<Boolean>(false)
    val writeReviewButtonEnabled = ObservableField<Boolean>(false)
    val writeReviewButtonAlpha = ObservableField<Float>(0.5f)


    val emptyStateTitle = ObservableField<String>("No Reviews Yet")
    val emptyStateSubtitle = ObservableField<String>("Be the first to share your experience working with this company!")


    val selectedRating = ObservableField<Int>(0)
    val reviewText = ObservableField<String>("")
    val ratingDescription = ObservableField<String>("Tap to rate")
    val submitButtonEnabled = ObservableField<Boolean>(false)
    val companyName = ObservableField<String>("")
    val companyLogo = ObservableField<String>("logo1")


    val reviewTextError = ObservableField<String>("")
    val wordCount = ObservableField<String>("0 words")
    val isReviewTextValid = ObservableField<Boolean>(false)


    private var currentBusinessOwnerId: String? = null





    fun loadReviewData(businessOwnerId: String) {
        if (businessOwnerId.isEmpty()) {
            _showErrorMessage.value = "Invalid business owner ID"
            showEmptyStateForError()
            return
        }

        currentBusinessOwnerId = businessOwnerId
        _isLoading.value = true
        showLoadingState.set(true)

        viewModelScope.launch {
            try {

                withContext(Dispatchers.IO) {
                    reviewRepository.initializeDummyReviews(businessOwnerId)
                }

                val reviewDataResult = withContext(Dispatchers.IO) {
                    reviewRepository.getReviewDataForBusiness(businessOwnerId)
                }

                _reviewData.value = reviewDataResult
                updateReactiveStats(reviewDataResult)


                loadBusinessOwnerInfo(businessOwnerId)


                checkWriteReviewPermissions(businessOwnerId)

                _isLoading.value = false
                showLoadingState.set(false)

            } catch (e: Exception) {
                _isLoading.value = false
                showLoadingState.set(false)
                _showErrorMessage.value = "Error loading reviews: ${e.message}"
                showEmptyStateForError()
            }
        }
    }

    private fun updateReactiveStats(reviewData: ReviewData) {

        overallRating.set(reviewData.getFormattedRating())
        totalReviewsText.set(reviewData.getReviewCountText())

        fiveStarCount.set(reviewData.fiveStarCount.toString())
        fourStarCount.set(reviewData.fourStarCount.toString())
        threeStarCount.set(reviewData.threeStarCount.toString())
        twoStarCount.set(reviewData.twoStarCount.toString())
        oneStarCount.set(reviewData.oneStarCount.toString())

        _recentReviews.value = reviewData.recentReviews


        when {
            reviewData.recentReviews.isEmpty() -> showEmptyState()
            else -> showContentState()
        }
    }

    private fun loadBusinessOwnerInfo(businessOwnerId: String) {
        viewModelScope.launch {
            try {
                val businessOwner = withContext(Dispatchers.IO) {
                    val database = AppDatabase.getDatabase(getApplication())
                    database.userDao().getUserById(businessOwnerId)
                }

                _businessOwnerInfo.value = businessOwner

                businessOwner?.let {
                    companyName.set(it.companyName.ifEmpty { it.name })
                    companyLogo.set(getCompanyLogo(it.companyName))
                }

            } catch (e: Exception) {
                _showErrorMessage.value = "Error loading company information"
            }
        }
    }

    private fun checkWriteReviewPermissions(businessOwnerId: String) {
        viewModelScope.launch {
            try {

                if (UserSession.getUserRole() == UserSession.ROLE_BUSINESS_OWNER) {
                    showWriteReviewButton.set(false)
                    return@launch
                }


                if (!UserSession.isLoggedIn()) {
                    showWriteReviewButton.set(true)
                    writeReviewButtonEnabled.set(false)
                    writeReviewButtonAlpha.set(0.5f)
                    return@launch
                }

                val currentUserId = UserSession.getUserId()
                if (currentUserId == null) {
                    showWriteReviewButton.set(false)
                    return@launch
                }

                val canWriteResult = canUserWriteReview(currentUserId, businessOwnerId)
                _canWriteReview.value = canWriteResult

                showWriteReviewButton.set(true)
                writeReviewButtonEnabled.set(canWriteResult.first)
                writeReviewButtonAlpha.set(if (canWriteResult.first) 1.0f else 0.5f)

            } catch (e: Exception) {
                showWriteReviewButton.set(false)
                _showErrorMessage.value = "Error checking review permissions"
            }
        }
    }





    fun onStarRatingSelected(rating: Int) {
        selectedRating.set(rating)

        val descriptions = arrayOf(
            "Terrible experience",
            "Poor experience",
            "Average experience",
            "Good experience",
            "Excellent experience"
        )

        ratingDescription.set(descriptions[rating - 1])
        updateSubmitButtonState()
    }

    fun onReviewTextChanged(text: String) {
        reviewText.set(text)
        validateReviewText(text)
        updateSubmitButtonState()
    }

    private fun validateReviewText(text: String) {
        val trimmedText = text.trim()


        val words = if (trimmedText.isEmpty()) {
            emptyList()
        } else {
            trimmedText.split("\\s+".toRegex()).filter { it.isNotBlank() }
        }

        val wordCountValue = words.size


        val wordCountText = when (wordCountValue) {
            0 -> "0 words"
            1 -> "1 word"
            else -> "$wordCountValue words"
        }
        wordCount.set(wordCountText)


        when {
            trimmedText.isEmpty() -> {
                reviewTextError.set("Review description is required")
                isReviewTextValid.set(false)
            }
            wordCountValue < 10 -> {
                val wordsNeeded = 10 - wordCountValue
                val wordText = if (wordsNeeded == 1) "word" else "words"
                reviewTextError.set("Please add $wordsNeeded more $wordText (minimum 10 words required)")
                isReviewTextValid.set(false)
            }
            trimmedText.length > 500 -> {
                reviewTextError.set("Review description cannot exceed 500 characters")
                isReviewTextValid.set(false)
            }
            else -> {
                reviewTextError.set("")
                isReviewTextValid.set(true)
            }
        }
    }

    private fun updateSubmitButtonState() {
        val hasRating = (selectedRating.get() ?: 0) > 0
        val hasValidText = isReviewTextValid.get() == true
        submitButtonEnabled.set(hasRating && hasValidText)
    }

    fun submitReview() {
        val rating = selectedRating.get() ?: 0
        val text = reviewText.get()?.trim()
        val businessOwnerId = currentBusinessOwnerId
        val currentUserId = UserSession.getUserId()

        if (rating == 0) {
            _showErrorMessage.value = "Please select a rating"
            return
        }

        if (text.isNullOrBlank()) {
            _showErrorMessage.value = "Please write a review description"
            return
        }

        if (businessOwnerId == null || currentUserId == null) {
            _showErrorMessage.value = "Error: Invalid user or business information"
            return
        }

        viewModelScope.launch {
            try {
                val success = submitReviewToDatabase(currentUserId, businessOwnerId, rating, text)

                if (success) {
                    _showSuccessMessage.value = "Review submitted successfully!"
                    resetReviewDialog()

                    loadReviewData(businessOwnerId)
                } else {
                    _showErrorMessage.value = "Failed to submit review. Please try again."
                }

            } catch (e: Exception) {
                _showErrorMessage.value = "Error submitting review: ${e.message}"
            }
        }
    }

    private suspend fun submitReviewToDatabase(
        freelancerId: String,
        businessOwnerId: String,
        rating: Int,
        reviewText: String
    ): Boolean {
        return withContext(Dispatchers.IO) {
            try {
                val database = AppDatabase.getDatabase(getApplication())


                val freelancer = database.userDao().getUserById(freelancerId)
                val businessOwner = database.userDao().getUserById(businessOwnerId)

                if (freelancer == null || businessOwner == null) {
                    return@withContext false
                }


                val review = ReviewEntity(
                    businessOwnerId = businessOwnerId,
                    reviewerId = freelancerId,
                    reviewerName = freelancer.name,
                    reviewerTitle = freelancer.title.ifEmpty { "Freelancer" },
                    reviewerExperience = freelancer.experience.ifEmpty { "Not specified" },
                    rating = rating,
                    reviewText = reviewText,
                    timestamp = System.currentTimeMillis(),
                    isVerified = true,
                    companyName = businessOwner.companyName.ifEmpty { businessOwner.name }
                )

                reviewRepository.addReview(review)

            } catch (e: Exception) {
                false
            }
        }
    }

    private fun resetReviewDialog() {
        selectedRating.set(0)
        reviewText.set("")
        ratingDescription.set("Tap to rate")
        submitButtonEnabled.set(false)
        reviewTextError.set("")
        wordCount.set("0 words")
        isReviewTextValid.set(false)
    }





    private fun showLoadingState() {
        showLoadingState.set(true)
        showContentState.set(false)
        showEmptyState.set(false)
    }

    private fun showContentState() {
        showLoadingState.set(false)
        showContentState.set(true)
        showEmptyState.set(false)
    }

    private fun showEmptyState() {
        showLoadingState.set(false)
        showContentState.set(false)
        showEmptyState.set(true)
        emptyStateTitle.set("No Reviews Yet")
        emptyStateSubtitle.set("Be the first to share your experience working with this company!")
    }

    private fun showEmptyStateForError() {
        showLoadingState.set(false)
        showContentState.set(false)
        showEmptyState.set(true)
        emptyStateTitle.set("Unable to Load Reviews")
        emptyStateSubtitle.set("Please try again later")
    }





    fun loadAllReviews() {
        val businessOwnerId = currentBusinessOwnerId ?: return

        viewModelScope.launch {
            try {
                val allReviewsList = withContext(Dispatchers.IO) {
                    reviewRepository.getAllReviewsForBusiness(businessOwnerId)
                }

                _allReviews.value = allReviewsList.sortedWith(
                    compareByDescending<ReviewEntity> { it.rating }
                        .thenByDescending { it.timestamp }
                )

                if (allReviewsList.isEmpty()) {
                    _showErrorMessage.value = "No reviews available"
                }

            } catch (e: Exception) {
                _showErrorMessage.value = "Error loading reviews"
            }
        }
    }





    private fun getCompanyLogo(companyName: String): String {
        return when (companyName) {
            "ChabokSoft", "Chaboksoft" -> "logo1"
            "KianSoft" -> "logo2"
            "MakanSoft" -> "logo3"
            "TestSoft" -> "logo4"
            else -> "logo1"
        }
    }

    private suspend fun canUserWriteReview(freelancerId: String, businessOwnerId: String): Pair<Boolean, String> {
        return withContext(Dispatchers.IO) {
            try {
                val database = AppDatabase.getDatabase(getApplication())
                val jobApplicationDao = database.jobApplicationDao()
                val jobAttendanceDao = database.jobAttendanceDao()
                val jobDao = database.jobDao()
                val reviewDao = database.reviewDao()


                val existingReview = reviewDao.getUserReviewForBusiness(freelancerId, businessOwnerId)
                if (existingReview != null) {
                    return@withContext Pair(false, "You have already written a review for this company.")
                }


                val businessOwnerJobs = jobDao.getJobsByOwnerId(businessOwnerId)
                if (businessOwnerJobs.isEmpty()) {
                    return@withContext Pair(false, "No jobs found for this company.")
                }

                val freelancerApplications = jobApplicationDao.getApplicationsByUserId(freelancerId)
                val shortlistedApplicationsWithThisOwner = freelancerApplications.filter { application ->
                    val job = businessOwnerJobs.find { it.id == application.jobId }
                    job != null && application.status == "shortlisted"
                }

                if (shortlistedApplicationsWithThisOwner.isEmpty()) {
                    return@withContext Pair(false, "You must be hired by this company to write a review.")
                }


                val jobIds = shortlistedApplicationsWithThisOwner.map { it.jobId }
                var hasCompletedAttendance = false

                for (jobId in jobIds) {
                    val attendanceRecords = jobAttendanceDao.getAttendanceByJobAndFreelancer(jobId, freelancerId)
                    val completedDays = attendanceRecords.filter {
                        it.checkInTime != null && it.checkOutTime != null
                    }
                    if (completedDays.isNotEmpty()) {
                        hasCompletedAttendance = true
                        break
                    }
                }

                if (!hasCompletedAttendance) {
                    return@withContext Pair(false, "You must complete at least one check-in and check-out cycle before writing a review.")
                }

                return@withContext Pair(true, "")

            } catch (e: Exception) {
                return@withContext Pair(false, "Unable to verify review eligibility. Please try again.")
            }
        }
    }


    fun onSuccessMessageShown() { _showSuccessMessage.value = null }
    fun onErrorMessageShown() { _showErrorMessage.value = null }


    fun onWriteReviewButtonClicked() {
        val canWrite = _canWriteReview.value
        if (canWrite?.first != true) {
            val message = if (!UserSession.isLoggedIn()) {
                "Please login to write a review"
            } else {
                canWrite?.second ?: "Cannot write review at this time"
            }
            _showErrorMessage.value = message
        }

    }
}