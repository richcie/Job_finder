package com.uilover.project196.ViewModel

import android.app.Application
import androidx.databinding.ObservableField
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.LiveData
import androidx.lifecycle.MutableLiveData
import androidx.lifecycle.viewModelScope
import com.uilover.project196.Database.AppDatabase
import com.uilover.project196.Model.CandidateProgressModel
import com.uilover.project196.Utils.UserSession
import com.uilover.project196.Repository.ChatRepository
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext

// KRITERIA WAJIB: ViewModel (11/12) - ViewModel untuk CandidatesProgressFragment
class CandidatesProgressViewModel(application: Application) : AndroidViewModel(application) {

    private val database = AppDatabase.getDatabase(application)
    private val chatRepository = ChatRepository.getInstance(application)




    private val _candidatesProgressList = MutableLiveData<List<CandidateProgressModel>>()
    val candidatesProgressList: LiveData<List<CandidateProgressModel>> = _candidatesProgressList

    private val _isLoading = MutableLiveData<Boolean>()
    val isLoading: LiveData<Boolean> = _isLoading

    private val _isAuthorized = MutableLiveData<Boolean>()
    val isAuthorized: LiveData<Boolean> = _isAuthorized

    private val _showSuccessMessage = MutableLiveData<String?>()
    val showSuccessMessage: LiveData<String?> = _showSuccessMessage

    private val _showErrorMessage = MutableLiveData<String?>()
    val showErrorMessage: LiveData<String?> = _showErrorMessage




    val candidateCount = ObservableField<String>("0")
    val emptyStateTitle = ObservableField<String>("No Candidates Yet")
    val emptyStateSubtitle = ObservableField<String>("Candidates will appear here once they apply and get verified")
    val headerTitle = ObservableField<String>("Candidates Progress")
    val headerSubtitle = ObservableField<String>("Track your team's progress")


    val showEmptyState = ObservableField<Boolean>(false)
    val showLoadingState = ObservableField<Boolean>(false)
    val showContentState = ObservableField<Boolean>(false)
    val showUnauthorizedState = ObservableField<Boolean>(false)


    val searchQuery = ObservableField<String>("")
    val filterStatus = ObservableField<String>("all")
    val sortOption = ObservableField<String>("recent")


    val totalCandidates = ObservableField<String>("0")
    val activeCandidates = ObservableField<String>("0")
    val averageCompletionRate = ObservableField<String>("0%")
    val totalHoursWorked = ObservableField<String>("0h")


    private var specificJobId: String? = null
    private var jobTitle: String? = null
    private var jobCompany: String? = null
    private var jobOwnerId: String? = null





    fun initializeForAllJobs() {
        android.util.Log.d("CandidatesProgressViewModel", "Initializing for all jobs")
        specificJobId = null
        jobTitle = null
        jobCompany = null
        jobOwnerId = null
        checkAuthorizationAndLoad()
    }

    fun initializeForSpecificJob(jobId: String) {
        android.util.Log.d("CandidatesProgressViewModel", "Initializing for specific job: $jobId")
        specificJobId = jobId
        checkAuthorizationAndLoad()
    }

    fun initializeForJobDetails(title: String, company: String, ownerId: String) {
        android.util.Log.d("CandidatesProgressViewModel", "Initializing for job: $title at $company")
        jobTitle = title
        jobCompany = company
        jobOwnerId = ownerId
        checkAuthorizationAndLoad()
    }

    private fun checkAuthorizationAndLoad() {
        val isBusinessOwner = UserSession.isLoggedIn() && UserSession.getUserRole() == UserSession.ROLE_BUSINESS_OWNER
        _isAuthorized.value = isBusinessOwner

        if (isBusinessOwner) {
            headerTitle.set("Candidates Progress")
            headerSubtitle.set("Track your team's progress and performance")
            loadCandidatesProgress()
        } else {
            showUnauthorizedState()
        }
    }

    fun loadCandidatesProgress() {
        if (!UserSession.isLoggedIn() || UserSession.getUserRole() != UserSession.ROLE_BUSINESS_OWNER) {
            android.util.Log.w("CandidatesProgressViewModel", "Unauthorized access attempt")
            showUnauthorizedState()
            return
        }

        _isLoading.value = true
        showLoadingState.set(true)

        viewModelScope.launch {
            try {
                android.util.Log.d("CandidatesProgressViewModel", "=== LOADING CANDIDATES PROGRESS ===")
                val businessOwnerId = UserSession.getUserId() ?: return@launch

                val jobDao = database.jobDao()
                val jobApplicationDao = database.jobApplicationDao()
                val userDao = database.userDao()
                val jobAttendanceDao = database.jobAttendanceDao()
                val messageDao = database.messageDao()


                val ownedJobs = when {
                    specificJobId != null -> {
                        val specificJob = jobDao.getJobById(specificJobId!!.toInt())
                        if (specificJob != null && specificJob.ownerId == businessOwnerId) {
                            listOf(specificJob)
                        } else {
                            emptyList()
                        }
                    }
                    jobTitle != null && jobCompany != null && jobOwnerId != null -> {
                        val allJobs = jobDao.getJobsByOwnerId(businessOwnerId)
                        allJobs.filter { job ->
                            job.title == jobTitle &&
                            job.company == jobCompany &&
                            job.ownerId == jobOwnerId
                        }
                    }
                    else -> {
                        jobDao.getJobsByOwnerId(businessOwnerId)
                    }
                }

                android.util.Log.d("CandidatesProgressViewModel", "Found ${ownedJobs.size} owned jobs")


                val uniqueCandidatesMap = mutableMapOf<String, CandidateProgressModel>()
                val allVerifiedFreelancerIds = messageDao.getAllVerifiedFreelancerIds()
                val allFreelancerIds = mutableSetOf<String>()


                for (job in ownedJobs) {
                    val shortlistedApplications = withContext(Dispatchers.IO) {
                        val allApplications = jobApplicationDao.getApplicationsByJobId(job.id)
                        allApplications.filter { it.status == "shortlisted" }
                    }

                    shortlistedApplications.forEach { application ->
                        allFreelancerIds.add(application.applicantUserId)
                    }
                }


                allFreelancerIds.addAll(allVerifiedFreelancerIds)

                android.util.Log.d("CandidatesProgressViewModel", "Processing ${allFreelancerIds.size} unique freelancers")


                for (freelancerId in allFreelancerIds) {

                    val isVerified = withContext(Dispatchers.IO) {
                        try {
                            chatRepository.hasAcceptedVerification(freelancerId)
                        } catch (e: Exception) {
                            android.util.Log.e("CandidatesProgressViewModel", "Error checking verification for $freelancerId", e)
                            false
                        }
                    }

                    if (!isVerified) {
                        android.util.Log.w("CandidatesProgressViewModel", "❌ SKIPPING freelancer $freelancerId - NO ACCEPTED VERIFICATION")
                        continue
                    }

                    val freelancer = withContext(Dispatchers.IO) { userDao.getUserById(freelancerId) }
                    if (freelancer != null) {

                        val application = withContext(Dispatchers.IO) {
                            var foundApplication: com.uilover.project196.Model.JobApplicationEntity? = null
                            for (job in ownedJobs) {
                                val applications = jobApplicationDao.getApplicationsByJobId(job.id)
                                val shortlistedApp = applications.find { it.applicantUserId == freelancerId && it.status == "shortlisted" }
                                if (shortlistedApp != null) {
                                    foundApplication = shortlistedApp
                                    break
                                }
                            }
                            foundApplication
                        }


                        var primaryJob = application?.let { app ->
                            ownedJobs.find { it.id == app.jobId }
                        }

                        if (primaryJob == null && ownedJobs.isNotEmpty()) {
                            primaryJob = ownedJobs.first()
                        }

                        if (primaryJob != null) {
                            val hiredDate = application?.appliedAt ?: System.currentTimeMillis()


                            val attendanceRecords = withContext(Dispatchers.IO) {
                                jobAttendanceDao.getAttendanceByJobAndFreelancer(primaryJob.id, freelancer.userId)
                            }


                            val baseExpectedWorkDays = calculateExpectedWorkDays(hiredDate)
                            val attendedDays = attendanceRecords.count {
                                it.checkInTime != null || it.checkOutTime != null
                            }

                            val totalWorkDays = maxOf(baseExpectedWorkDays, attendedDays)
                            val completionRate = when {
                                totalWorkDays <= 0 -> 0.0
                                attendedDays > totalWorkDays -> 1.0
                                else -> attendedDays.toDouble() / totalWorkDays
                            }


                            val completedAttendanceRecords = attendanceRecords.filter {
                                it.checkInTime != null && it.checkOutTime != null
                            }
                            val totalHours = completedAttendanceRecords.sumOf { attendance ->
                                (attendance.checkOutTime!! - attendance.checkInTime!!) / (1000.0 * 60 * 60)
                            }
                            val averageHoursPerDay = if (completedAttendanceRecords.isNotEmpty()) {
                                totalHours / completedAttendanceRecords.size
                            } else 0.0


                            val today = java.text.SimpleDateFormat("yyyy-MM-dd", java.util.Locale.getDefault())
                                .format(java.util.Date())
                            val todayAttendance = attendanceRecords.find { it.attendanceDate == today }


                            val wasFrozenByBusinessOwner = try {
                                val prefs = getApplication<Application>().getSharedPreferences("user_status", android.content.Context.MODE_PRIVATE)
                                prefs.getBoolean("is_blocked_${freelancer.userId}", false)
                            } catch (e: Exception) {
                                false
                            }

                            val currentStatus = when {
                                !freelancer.isActive && wasFrozenByBusinessOwner -> "frozen"
                                todayAttendance?.checkInTime != null && todayAttendance.checkOutTime == null -> "checked_in"
                                todayAttendance?.checkOutTime != null -> "checked_out"
                                else -> "not_started"
                            }

                            val candidateRole = if (!freelancer.title.isNullOrEmpty()) {
                                freelancer.title
                            } else {
                                "Full Stack Developer"
                            }

                            val candidateProgress = CandidateProgressModel(
                                candidateId = freelancer.userId,
                                candidateName = freelancer.name,
                                candidateEmail = freelancer.email,
                                candidateRole = candidateRole,
                                jobId = primaryJob.id,
                                jobTitle = primaryJob.title,
                                applicationId = application?.id ?: 0,
                                hiredDate = hiredDate,
                                status = if (application != null) "shortlisted" else "verified",
                                totalWorkDays = totalWorkDays,
                                attendedDays = attendedDays,
                                lastCheckIn = todayAttendance?.checkInTime,
                                lastCheckOut = todayAttendance?.checkOutTime,
                                currentStatus = currentStatus,
                                averageHoursPerDay = averageHoursPerDay,
                                totalHours = totalHours,
                                completionRate = completionRate,
                                lastProgressReport = todayAttendance?.progressReport
                            )

                            uniqueCandidatesMap[freelancerId] = candidateProgress
                        }
                    }
                }


                val candidatesProgressList = uniqueCandidatesMap.values.toMutableList()
                candidatesProgressList.sortByDescending {
                    maxOf(it.lastCheckIn ?: 0L, it.lastCheckOut ?: 0L, it.hiredDate)
                }


                _candidatesProgressList.value = candidatesProgressList
                updateUIState(candidatesProgressList)

                _isLoading.value = false
                showLoadingState.set(false)

                android.util.Log.d("CandidatesProgressViewModel", "=== PROCESSING COMPLETE ===")
                android.util.Log.d("CandidatesProgressViewModel", "Final candidates list size: ${candidatesProgressList.size}")

            } catch (e: Exception) {
                android.util.Log.e("CandidatesProgressViewModel", "Error loading candidates progress", e)
                _isLoading.value = false
                showLoadingState.set(false)
                _showErrorMessage.value = "Failed to load candidates: ${e.message}"
                updateUIState(emptyList())
            }
        }
    }

    private fun calculateExpectedWorkDays(hiredDate: Long): Int {
        val startCalendar = java.util.Calendar.getInstance().apply {
            timeInMillis = hiredDate
            set(java.util.Calendar.HOUR_OF_DAY, 0)
            set(java.util.Calendar.MINUTE, 0)
            set(java.util.Calendar.SECOND, 0)
            set(java.util.Calendar.MILLISECOND, 0)
        }

        val currentCalendar = java.util.Calendar.getInstance().apply {
            set(java.util.Calendar.HOUR_OF_DAY, 0)
            set(java.util.Calendar.MINUTE, 0)
            set(java.util.Calendar.SECOND, 0)
            set(java.util.Calendar.MILLISECOND, 0)
        }

        var workDays = 0
        val calendar = startCalendar.clone() as java.util.Calendar

        while (calendar.timeInMillis <= currentCalendar.timeInMillis) {
            val dayOfWeek = calendar.get(java.util.Calendar.DAY_OF_WEEK)
            if (dayOfWeek != java.util.Calendar.SATURDAY && dayOfWeek != java.util.Calendar.SUNDAY) {
                workDays++
            }
            calendar.add(java.util.Calendar.DAY_OF_MONTH, 1)
        }

        return workDays
    }

    private fun updateUIState(candidatesList: List<CandidateProgressModel>) {
        val count = candidatesList.size
        candidateCount.set(count.toString())


        updateAnalytics(candidatesList)

        when {
            !UserSession.isLoggedIn() || UserSession.getUserRole() != UserSession.ROLE_BUSINESS_OWNER -> showUnauthorizedState()
            count == 0 -> showEmptyState()
            else -> showContentState()
        }
    }

    private fun updateAnalytics(candidatesList: List<CandidateProgressModel>) {
        totalCandidates.set(candidatesList.size.toString())

        val activeCandidatesCount = candidatesList.count {
            it.currentStatus in listOf("checked_in", "checked_out")
        }
        activeCandidates.set(activeCandidatesCount.toString())

        if (candidatesList.isNotEmpty()) {
            val avgCompletion = candidatesList.map { it.completionRate }.average() * 100
            averageCompletionRate.set("${avgCompletion.toInt()}%")

            val totalHours = candidatesList.sumOf { it.totalHours }
            totalHoursWorked.set("${totalHours.toInt()}h")
        } else {
            averageCompletionRate.set("0%")
            totalHoursWorked.set("0h")
        }
    }

    private fun showUnauthorizedState() {
        showUnauthorizedState.set(true)
        showEmptyState.set(false)
        showContentState.set(false)
        showLoadingState.set(false)
        emptyStateTitle.set("Access Denied")
        emptyStateSubtitle.set("Only business owners can view candidate progress")
    }

    private fun showEmptyState() {
        showUnauthorizedState.set(false)
        showEmptyState.set(true)
        showContentState.set(false)
        showLoadingState.set(false)
        emptyStateTitle.set("No Candidates Yet")
        emptyStateSubtitle.set("Candidates will appear here once they apply and get verified")
    }

    private fun showContentState() {
        showUnauthorizedState.set(false)
        showEmptyState.set(false)
        showContentState.set(true)
        showLoadingState.set(false)
    }


    fun onSearchQueryChanged(query: String) {
        searchQuery.set(query)

        applyFilters()
    }

    fun onFilterStatusChanged(status: String) {
        filterStatus.set(status)
        applyFilters()
    }

    fun onSortOptionChanged(option: String) {
        sortOption.set(option)
        applyFilters()
    }

    private fun applyFilters() {


        val currentList = _candidatesProgressList.value ?: return

        var filteredList = currentList


        val query = searchQuery.get()?.lowercase() ?: ""
        if (query.isNotEmpty()) {
            filteredList = filteredList.filter { candidate ->
                candidate.candidateName.lowercase().contains(query) ||
                candidate.candidateRole.lowercase().contains(query) ||
                candidate.jobTitle.lowercase().contains(query)
            }
        }


        val status = filterStatus.get() ?: "all"
        if (status != "all") {
            filteredList = filteredList.filter { candidate ->
                candidate.currentStatus == status
            }
        }


        when (sortOption.get()) {
            "name" -> filteredList = filteredList.sortedBy { it.candidateName }
            "completion" -> filteredList = filteredList.sortedByDescending { it.completionRate }
            "hours" -> filteredList = filteredList.sortedByDescending { it.totalHours }
            else -> filteredList = filteredList.sortedByDescending {
                maxOf(it.lastCheckIn ?: 0L, it.lastCheckOut ?: 0L, it.hiredDate)
            }
        }


        _candidatesProgressList.value = filteredList
        updateUIState(filteredList)
    }


    fun refreshData() {
        android.util.Log.d("CandidatesProgressViewModel", "Manual refresh triggered")
        loadCandidatesProgress()
    }


    fun onSuccessMessageShown() { _showSuccessMessage.value = null }
    fun onErrorMessageShown() { _showErrorMessage.value = null }
}