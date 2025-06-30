package com.uilover.project196.ViewModel

import android.app.Application
import androidx.databinding.ObservableField
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.LiveData
import androidx.lifecycle.MutableLiveData
import androidx.lifecycle.viewModelScope
import com.uilover.project196.Database.AppDatabase
import com.uilover.project196.Repository.ReviewRepository
import com.uilover.project196.Repository.UserRepository
import com.uilover.project196.Utils.UserSession
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import kotlinx.coroutines.Dispatchers

// KRITERIA WAJIB: ViewModel (9/12) - ViewModel untuk AnalyticsFragment
class AnalyticsViewModel(application: Application) : AndroidViewModel(application) {

    private val database = AppDatabase.getDatabase(application)
    private lateinit var userRepository: UserRepository
    private lateinit var reviewRepository: ReviewRepository


    private var currentBusinessOwnerId: String? = null




    private val _isLoading = MutableLiveData<Boolean>()
    val isLoading: LiveData<Boolean> = _isLoading

    private val _isAuthorized = MutableLiveData<Boolean>()
    val isAuthorized: LiveData<Boolean> = _isAuthorized

    private val _showSuccessMessage = MutableLiveData<String?>()
    val showSuccessMessage: LiveData<String?> = _showSuccessMessage

    private val _showErrorMessage = MutableLiveData<String?>()
    val showErrorMessage: LiveData<String?> = _showErrorMessage






    val headerTitle = ObservableField<String>("Business Analytics")
    val headerSubtitle = ObservableField<String>("Track your business performance")


    val companyName = ObservableField<String>("Your Company")
    val companyDescription = ObservableField<String>("Growing business")
    val companyLogo = ObservableField<String>("logo1")


    val totalJobs = ObservableField<String>("0")
    val totalApplications = ObservableField<String>("0")
    val totalFreelancers = ObservableField<String>("0")
    val totalViews = ObservableField<String>("0")
    val averageRating = ObservableField<String>("0.0")
    val totalReviews = ObservableField<String>("0")


    val activeJobs = ObservableField<String>("0")
    val closedJobs = ObservableField<String>("0")
    val shortlistedCandidates = ObservableField<String>("0")
    val hiredCandidates = ObservableField<String>("0")
    val responseRate = ObservableField<String>("0%")
    val averageTimeToHire = ObservableField<String>("0 days")


    val totalBudget = ObservableField<String>("$0")
    val averageSalary = ObservableField<String>("$0")
    val costPerHire = ObservableField<String>("$0")


    val showLoadingState = ObservableField<Boolean>(false)
    val showContentState = ObservableField<Boolean>(false)
    val showEmptyState = ObservableField<Boolean>(false)
    val showUnauthorizedState = ObservableField<Boolean>(false)


    val emptyStateTitle = ObservableField<String>("No Analytics Data")
    val emptyStateSubtitle = ObservableField<String>("Start posting jobs to see analytics")





    fun initializeForBusinessOwner(businessOwnerId: String? = null) {
        android.util.Log.d("AnalyticsViewModel", "Initializing analytics for business owner: $businessOwnerId")


        currentBusinessOwnerId = businessOwnerId ?: UserSession.getUserId()


        val isBusinessOwner = UserSession.isLoggedIn() && UserSession.getUserRole() == UserSession.ROLE_BUSINESS_OWNER
        _isAuthorized.value = isBusinessOwner

        if (isBusinessOwner && currentBusinessOwnerId != null) {

            try {
                userRepository = UserRepository.getInstance(getApplication())
                reviewRepository = ReviewRepository.getInstance(getApplication())

                headerTitle.set("Business Analytics")
                headerSubtitle.set("Track your business performance and growth")
                loadAnalyticsData()
            } catch (e: Exception) {
                android.util.Log.e("AnalyticsViewModel", "Error initializing repositories", e)
                _showErrorMessage.value = "Failed to initialize analytics: ${e.message}"
                showUnauthorizedState()
            }
        } else {
            android.util.Log.w("AnalyticsViewModel", "Unauthorized access - user is not business owner")
            showUnauthorizedState()
        }
    }

    private fun loadAnalyticsData() {
        val businessOwnerId = currentBusinessOwnerId
        if (businessOwnerId == null) {
            android.util.Log.e("AnalyticsViewModel", "No business owner ID available")
            showEmptyState()
            return
        }

        _isLoading.value = true
        showLoadingState.set(true)

        viewModelScope.launch {
            try {
                android.util.Log.d("AnalyticsViewModel", "=== LOADING ANALYTICS DATA ===")
                android.util.Log.d("AnalyticsViewModel", "Business Owner ID: $businessOwnerId")


                loadCompanyInformation(businessOwnerId)


                loadJobStatistics(businessOwnerId)


                loadApplicationStatistics(businessOwnerId)


                loadReviewStatistics(businessOwnerId)


                loadFinancialStatistics(businessOwnerId)


                updateUIState()

                _isLoading.value = false
                showLoadingState.set(false)

                android.util.Log.d("AnalyticsViewModel", "=== ANALYTICS DATA LOADED SUCCESSFULLY ===")

            } catch (e: Exception) {
                android.util.Log.e("AnalyticsViewModel", "Error loading analytics data", e)
                _isLoading.value = false
                showLoadingState.set(false)
                _showErrorMessage.value = "Failed to load analytics: ${e.message}"
                showEmptyState()
            }
        }
    }

    private suspend fun loadCompanyInformation(businessOwnerId: String) {
        try {
            val user = withContext(Dispatchers.IO) {
                userRepository.getUserById(businessOwnerId)
            }

            if (user != null) {

                companyName.set(user.name + "'s Business")


                val description = when {
                    !user.title.isNullOrEmpty() -> "${user.title} • ${user.name}'s Business"
                    else -> "Growing business"
                }
                companyDescription.set(description)


                val logoIndex = (user.name.hashCode()) % 4 + 1
                companyLogo.set("logo$logoIndex")

                android.util.Log.d("AnalyticsViewModel", "Company info loaded: ${user.name}'s Business")
            } else {
                companyName.set("Your Company")
                companyDescription.set("Growing business")
                companyLogo.set("logo1")
            }
        } catch (e: Exception) {
            android.util.Log.e("AnalyticsViewModel", "Error loading company information", e)
        }
    }

    private suspend fun loadJobStatistics(businessOwnerId: String) {
        try {
            val jobDao = database.jobDao()
            val jobViewDao = database.jobViewDao()


            val allJobs = withContext(Dispatchers.IO) {
                jobDao.getJobsByOwnerId(businessOwnerId)
            }

            totalJobs.set(allJobs.size.toString())


            val activeJobsCount = allJobs.count { true }
            val closedJobsCount = 0

            activeJobs.set(activeJobsCount.toString())
            closedJobs.set(closedJobsCount.toString())


            val totalViewsCount = withContext(Dispatchers.IO) {
                allJobs.sumOf { job ->
                    try {
                        jobViewDao.getViewCountForJob(job.id)
                    } catch (e: Exception) {
                        0
                    }
                }
            }
            totalViews.set(totalViewsCount.toString())

            android.util.Log.d("AnalyticsViewModel", "Job statistics loaded: ${allJobs.size} total jobs, $activeJobsCount active, $totalViewsCount total views")

        } catch (e: Exception) {
            android.util.Log.e("AnalyticsViewModel", "Error loading job statistics", e)
            totalJobs.set("0")
            activeJobs.set("0")
            closedJobs.set("0")
            totalViews.set("0")
        }
    }

    private suspend fun loadApplicationStatistics(businessOwnerId: String) {
        try {
            val jobDao = database.jobDao()
            val jobApplicationDao = database.jobApplicationDao()
            // Note: userDao not currently used in this function


            val allJobs = withContext(Dispatchers.IO) {
                jobDao.getJobsByOwnerId(businessOwnerId)
            }


            var totalApplicationsCount = 0
            var shortlistedCount = 0
            val uniqueFreelancers = mutableSetOf<String>()

            for (job in allJobs) {
                val applications = withContext(Dispatchers.IO) {
                    jobApplicationDao.getApplicationsByJobId(job.id)
                }

                totalApplicationsCount += applications.size
                shortlistedCount += applications.count { it.status == "shortlisted" }



                applications.forEach { application ->
                    uniqueFreelancers.add(application.applicantUserId)
                }
            }

            totalApplications.set(totalApplicationsCount.toString())
            totalFreelancers.set(uniqueFreelancers.size.toString())
            shortlistedCandidates.set(shortlistedCount.toString())


            val jobAttendanceDao = database.jobAttendanceDao()
            val hiredCandidatesCount = withContext(Dispatchers.IO) {
                val allAttendanceRecords = mutableSetOf<String>()
                for (job in allJobs) {
                    val attendanceRecords = jobAttendanceDao.getAttendanceByJob(job.id)
                    attendanceRecords.forEach { record ->
                        allAttendanceRecords.add(record.freelancerId)
                    }
                }
                allAttendanceRecords.size
            }
            hiredCandidates.set(hiredCandidatesCount.toString())


            val responseRateValue = if (totalApplicationsCount > 0) {
                (shortlistedCount * 100) / totalApplicationsCount
            } else 0
            responseRate.set("$responseRateValue%")

            android.util.Log.d("AnalyticsViewModel", "Application statistics loaded: $totalApplicationsCount applications, $shortlistedCount shortlisted, $hiredCandidatesCount hired")

        } catch (e: Exception) {
            android.util.Log.e("AnalyticsViewModel", "Error loading application statistics", e)
            totalApplications.set("0")
            totalFreelancers.set("0")
            shortlistedCandidates.set("0")
            hiredCandidates.set("0")
            responseRate.set("0%")
        }
    }

    private suspend fun loadReviewStatistics(businessOwnerId: String) {
        try {
            val reviewData = withContext(Dispatchers.IO) {
                reviewRepository.getReviewDataForBusiness(businessOwnerId)
            }

            averageRating.set(reviewData.getFormattedRating())
            totalReviews.set(reviewData.totalReviews.toString())

            android.util.Log.d("AnalyticsViewModel", "Review statistics loaded: ${reviewData.getFormattedRating()} rating, ${reviewData.totalReviews} reviews")

        } catch (e: Exception) {
            android.util.Log.e("AnalyticsViewModel", "Error loading review statistics", e)
            averageRating.set("0.0")
            totalReviews.set("0")
        }
    }

    private suspend fun loadFinancialStatistics(businessOwnerId: String) {
        try {
            val jobDao = database.jobDao()


            val allJobs = withContext(Dispatchers.IO) {
                jobDao.getJobsByOwnerId(businessOwnerId)
            }

            if (allJobs.isNotEmpty()) {

                val salaries = allJobs.mapNotNull { job ->
                    parseSalaryRange(job.salary)
                }

                if (salaries.isNotEmpty()) {
                    val totalBudgetValue = salaries.sum()
                    val averageSalaryValue = salaries.average()

                    totalBudget.set("$${totalBudgetValue.toInt()}")
                    averageSalary.set("$${averageSalaryValue.toInt()}")


                    val responseRatePercent = responseRate.get()?.removeSuffix("%")?.toIntOrNull() ?: 1
                    val costPerHireValue = if (responseRatePercent > 0) {
                        averageSalaryValue * (100.0 / responseRatePercent) * 0.1
                    } else {
                        averageSalaryValue
                    }
                    costPerHire.set("$${costPerHireValue.toInt()}")
                } else {
                    totalBudget.set("$0")
                    averageSalary.set("$0")
                    costPerHire.set("$0")
                }
            } else {
                totalBudget.set("$0")
                averageSalary.set("$0")
                costPerHire.set("$0")
            }

            android.util.Log.d("AnalyticsViewModel", "Financial statistics loaded")

        } catch (e: Exception) {
            android.util.Log.e("AnalyticsViewModel", "Error loading financial statistics", e)
            totalBudget.set("$0")
            averageSalary.set("$0")
            costPerHire.set("$0")
        }
    }

    private fun parseSalaryRange(salaryString: String): Double? {
        return try {

            val cleanString = salaryString.replace(Regex("[,$]"), "")
            val parts = cleanString.split("-", "to", "–").map { it.trim() }

            when (parts.size) {
                1 -> parts[0].toDoubleOrNull()
                2 -> {
                    val min = parts[0].toDoubleOrNull() ?: 0.0
                    val max = parts[1].toDoubleOrNull() ?: 0.0
                    (min + max) / 2.0
                }
                else -> null
            }
        } catch (e: Exception) {
            null
        }
    }

    private fun updateUIState() {
        val jobsCount = totalJobs.get()?.toIntOrNull() ?: 0
        val applicationsCount = totalApplications.get()?.toIntOrNull() ?: 0

        when {
            !UserSession.isLoggedIn() || UserSession.getUserRole() != UserSession.ROLE_BUSINESS_OWNER -> showUnauthorizedState()
            jobsCount == 0 && applicationsCount == 0 -> showEmptyState()
            else -> showContentState()
        }


        val subtitle = buildString {
            append("$jobsCount jobs posted")
            if (applicationsCount > 0) {
                append(" • $applicationsCount applications")
            }
            val rating = averageRating.get() ?: "0.0"
            if (rating != "0.0") {
                append(" • $rating rating")
            }
        }
        headerSubtitle.set(subtitle)
    }

    private fun showUnauthorizedState() {
        showUnauthorizedState.set(true)
        showEmptyState.set(false)
        showContentState.set(false)
        showLoadingState.set(false)
        emptyStateTitle.set("Access Denied")
        emptyStateSubtitle.set("Only business owners can view analytics")
    }

    private fun showEmptyState() {
        showUnauthorizedState.set(false)
        showEmptyState.set(true)
        showContentState.set(false)
        showLoadingState.set(false)
        emptyStateTitle.set("No Analytics Data")
        emptyStateSubtitle.set("Start posting jobs to see your business analytics")
    }

    private fun showContentState() {
        showUnauthorizedState.set(false)
        showEmptyState.set(false)
        showContentState.set(true)
        showLoadingState.set(false)
    }


    fun refreshAnalytics() {
        android.util.Log.d("AnalyticsViewModel", "Manual analytics refresh triggered")
        loadAnalyticsData()
    }


    fun onSuccessMessageShown() { _showSuccessMessage.value = null }
    fun onErrorMessageShown() { _showErrorMessage.value = null }
}