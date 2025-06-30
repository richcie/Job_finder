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
import com.uilover.project196.Utils.UserSession
import kotlinx.coroutines.launch

// KRITERIA WAJIB: ViewModel (6/12) - ViewModel untuk BookmarkFragment
class BookmarkViewModel(application: Application) : AndroidViewModel(application) {

    private val database = AppDatabase.getDatabase(application)


    private val mainRepository = MainRepository.getInstance()






    private val _globalBookmarkStateChanged = MutableLiveData<BookmarkStateChange?>()
    val globalBookmarkStateChanged: LiveData<BookmarkStateChange?> = _globalBookmarkStateChanged


    private val _bookmarkStatesMap = MutableLiveData<Map<String, Boolean>>()
    val bookmarkStatesMap: LiveData<Map<String, Boolean>> = _bookmarkStatesMap


    private val lastBookmarkUpdateTime = mutableMapOf<String, Long>()


    private val _bookmarkedJobs = MutableLiveData<List<JobModel>>()
    val bookmarkedJobs: LiveData<List<JobModel>> = _bookmarkedJobs

    private val _isLoading = MutableLiveData<Boolean>()
    val isLoading: LiveData<Boolean> = _isLoading

    private val _isLoggedIn = MutableLiveData<Boolean>()
    val isLoggedIn: LiveData<Boolean> = _isLoggedIn

    private val _isEmpty = MutableLiveData<Boolean>()
    val isEmpty: LiveData<Boolean> = _isEmpty

    private val _showSuccessMessage = MutableLiveData<String?>()
    val showSuccessMessage: LiveData<String?> = _showSuccessMessage

    private val _showErrorMessage = MutableLiveData<String?>()
    val showErrorMessage: LiveData<String?> = _showErrorMessage

    private val _bookmarkToggled = MutableLiveData<JobModel?>()
    val bookmarkToggled: LiveData<JobModel?> = _bookmarkToggled

    private val _jobClicked = MutableLiveData<JobModel?>()
    val jobClicked: LiveData<JobModel?> = _jobClicked


    val searchQuery = ObservableField<String>("")
    val bookmarkCount = ObservableField<String>("0")
    val emptyStateTitle = ObservableField<String>("No saved jobs yet")
    val emptyStateSubtitle = ObservableField<String>("Start saving jobs you're interested in")


    val showEmptyState = ObservableField<Boolean>(false)
    val showGuestState = ObservableField<Boolean>(false)
    val showBookmarksList = ObservableField<Boolean>(false)
    val showLoadingState = ObservableField<Boolean>(false)





    data class BookmarkStateChange(
        val jobId: String,
        val isBookmarked: Boolean,
        val jobTitle: String,
        val timestamp: Long = System.currentTimeMillis()
    )

    init {

        mainRepository.initializeDatabase(application)
        refreshLoginState()
        initializeBookmarkStates()
    }





    private fun initializeBookmarkStates() {
        viewModelScope.launch {
            try {
                updateGlobalBookmarkStates()
            } catch (e: Exception) {
                android.util.Log.e("BookmarkViewModel", "Error initializing bookmark states", e)
            }
        }
    }

    private fun updateGlobalBookmarkStates() {

        val existingStates = _bookmarkStatesMap.value?.toMutableMap() ?: mutableMapOf()
        val bookmarkStates = mutableMapOf<String, Boolean>()


        val allJobs = mainRepository.allItems
        android.util.Log.d("BookmarkViewModel", "updateGlobalBookmarkStates: Processing ${allJobs.size} jobs")

        allJobs.forEach { job ->
            val jobId = getJobId(job)
            val isBookmarked = mainRepository.isJobBookmarked(job)


            val recentlyUpdated = existingStates.containsKey(jobId) &&
                                (System.currentTimeMillis() - (lastBookmarkUpdateTime[jobId] ?: 0)) < 2000

            if (recentlyUpdated) {

                bookmarkStates[jobId] = existingStates[jobId] ?: isBookmarked
                android.util.Log.d("BookmarkViewModel", "Preserving recent state for $jobId: ${existingStates[jobId]}")
            } else {

                bookmarkStates[jobId] = isBookmarked
            }

            if (bookmarkStates[jobId] == true) {
                android.util.Log.d("BookmarkViewModel", "Found bookmarked job: ${job.title} ($jobId)")
            }
        }

        _bookmarkStatesMap.value = bookmarkStates
        android.util.Log.d("BookmarkViewModel", "Updated global bookmark states: ${bookmarkStates.size} total jobs, ${bookmarkStates.values.count { it }} bookmarked")
        android.util.Log.d("BookmarkViewModel", "Bookmarked job IDs: ${bookmarkStates.filter { it.value }.keys}")
    }

    private fun getJobId(job: JobModel): String {

        return "${job.title}_${job.company}"
    }


    fun isJobBookmarked(job: JobModel): Boolean {
        val jobId = getJobId(job)
        val isBookmarked = _bookmarkStatesMap.value?.get(jobId) ?: false
        android.util.Log.d("BookmarkViewModel", "isJobBookmarked check for '${job.title}' ($jobId): $isBookmarked")
        android.util.Log.d("BookmarkViewModel", "Available bookmark states: ${_bookmarkStatesMap.value?.keys?.toList()}")
        return isBookmarked
    }


    fun getJobWithUpdatedBookmarkState(job: JobModel): JobModel {
        return job.copy(isBookmarked = isJobBookmarked(job))
    }


    fun applyBookmarkStatesToJobs(jobs: List<JobModel>): List<JobModel> {
        return jobs.map { job ->
            job.copy(isBookmarked = isJobBookmarked(job))
        }
    }





    fun toggleBookmark(job: JobModel) {
        android.util.Log.d("BookmarkViewModel", "🎯 toggleBookmark called on instance: ${this.hashCode()}")
        android.util.Log.d("BookmarkViewModel", "🎯 globalBookmarkStateChanged has ${globalBookmarkStateChanged.hasObservers()} observers")

        if (!UserSession.isLoggedIn()) {
            _showErrorMessage.value = "Please log in to bookmark jobs"
            return
        }

        if (job.isOwnedByCurrentUser()) {
            _showErrorMessage.value = "You cannot bookmark your own jobs"
            return
        }

        if (job.isClosed()) {
            _showErrorMessage.value = "Cannot bookmark closed jobs"
            return
        }

        viewModelScope.launch {
            try {
                val jobId = getJobId(job)



                val repositoryState = mainRepository.isJobBookmarked(job)
                val jobUIState = job.isBookmarked
                val wasBookmarked = repositoryState
                val newBookmarkState = !wasBookmarked

                android.util.Log.d("BookmarkViewModel", "=== BOOKMARK TOGGLE DEBUG ===")
                android.util.Log.d("BookmarkViewModel", "Toggling bookmark for job: ${job.title}")
                android.util.Log.d("BookmarkViewModel", "Job ID: $jobId")
                android.util.Log.d("BookmarkViewModel", "Repository state: $wasBookmarked -> $newBookmarkState")
                android.util.Log.d("BookmarkViewModel", "UI state: $jobUIState (for comparison)")


                if (wasBookmarked != jobUIState) {
                    android.util.Log.i("BookmarkViewModel", "Repository state differs from UI state - using repository as source of truth")
                }


                mainRepository.toggleBookmark(job)
                android.util.Log.d("BookmarkViewModel", "Toggled bookmark in repository")


                val actualNewState = mainRepository.isJobBookmarked(job)
                android.util.Log.d("BookmarkViewModel", "Repository state after toggle: $actualNewState")


                if (actualNewState != newBookmarkState) {
                    android.util.Log.e("BookmarkViewModel", "ERROR: Repository state ($actualNewState) doesn't match expected state ($newBookmarkState)")
                    android.util.Log.e("BookmarkViewModel", "This indicates a repository toggle issue - using actual state for UI")
                }


                val currentStates = _bookmarkStatesMap.value?.toMutableMap() ?: mutableMapOf()
                currentStates[jobId] = actualNewState
                _bookmarkStatesMap.postValue(currentStates)


                lastBookmarkUpdateTime[jobId] = System.currentTimeMillis()

                android.util.Log.d("BookmarkViewModel", "Updated global state map: $jobId = $actualNewState")





                val stateChange = BookmarkStateChange(
                    jobId = jobId,
                    isBookmarked = actualNewState,
                    jobTitle = job.title
                )
                android.util.Log.d("BookmarkViewModel", "🔥 ========= BROADCASTING GLOBAL STATE CHANGE =========")
                android.util.Log.d("BookmarkViewModel", "🔥 Job ID: $jobId")
                android.util.Log.d("BookmarkViewModel", "🔥 Job Title: ${job.title}")
                android.util.Log.d("BookmarkViewModel", "🔥 Company: ${job.company}")
                android.util.Log.d("BookmarkViewModel", "🔥 New State: $actualNewState")
                android.util.Log.d("BookmarkViewModel", "🔥 Observer count: ${globalBookmarkStateChanged.hasObservers()}")
                android.util.Log.d("BookmarkViewModel", "🔥 Current handlers: ${stateChangeHandlers.joinToString(", ")}")

                _globalBookmarkStateChanged.postValue(stateChange)

                android.util.Log.d("BookmarkViewModel", "✅ Global bookmark state change POSTED to LiveData")
                android.util.Log.d("BookmarkViewModel", "🔥 ========= BROADCAST COMPLETE =========")


                val updatedJob = job.copy(isBookmarked = actualNewState)
                _bookmarkToggled.postValue(updatedJob)


                val message = if (actualNewState) {
                    "Job '${job.title}' added to bookmarks"
                } else {
                    "Job '${job.title}' removed from bookmarks"
                }
                _showSuccessMessage.postValue(message)

                android.util.Log.d("BookmarkViewModel", "Success message: $message")



                if (!actualNewState) {

                    android.util.Log.d("BookmarkViewModel", "Refreshing bookmark list for removal")
                    loadBookmarkedJobs()
                } else {

                    android.util.Log.d("BookmarkViewModel", "Bookmark added - updating count only")
                    val currentBookmarks = _bookmarkedJobs.value ?: emptyList()
                    if (!currentBookmarks.any { getJobId(it) == jobId }) {

                        val updatedBookmarks = currentBookmarks + updatedJob
                        _bookmarkedJobs.postValue(updatedBookmarks)
                        updateUIState(updatedBookmarks)
                    }
                }

                android.util.Log.d("BookmarkViewModel", "Bookmark toggle completed successfully")

            } catch (e: Exception) {
                android.util.Log.e("BookmarkViewModel", "Error toggling bookmark", e)
                _showErrorMessage.value = "Failed to update bookmark: ${e.message}"
            }
        }
    }

    fun removeBookmark(job: JobModel) {
        android.util.Log.d("BookmarkViewModel", "removeBookmark called for: ${job.title}")
        android.util.Log.d("BookmarkViewModel", "Current bookmark state before removal: ${job.isBookmarked}")


        toggleBookmark(job)
    }






    private val stateChangeHandlers = mutableSetOf<String>()

    fun onGlobalBookmarkStateChangeHandled(fragmentName: String = "unknown") {
        android.util.Log.d("BookmarkViewModel", "🔄 Fragment '$fragmentName' handled global bookmark state change")
        stateChangeHandlers.add(fragmentName)


        viewModelScope.launch {
            kotlinx.coroutines.delay(500)


            val currentState = _globalBookmarkStateChanged.value
            if (currentState != null) {
                android.util.Log.d("BookmarkViewModel", "📤 Clearing global bookmark state for: ${currentState.jobTitle}")
                android.util.Log.d("BookmarkViewModel", "📤 Handlers that processed this change: ${stateChangeHandlers.joinToString(", ")}")
            }

            _globalBookmarkStateChanged.value = null
            stateChangeHandlers.clear()
            android.util.Log.d("BookmarkViewModel", "✅ Global bookmark state change cleared after delay")
        }
    }


    fun clearGlobalBookmarkState() {
        _globalBookmarkStateChanged.value = null
        stateChangeHandlers.clear()
        android.util.Log.d("BookmarkViewModel", "🔄 Manual clear of global bookmark state")
    }

    fun refreshLoginState() {
        android.util.Log.d("BookmarkViewModel", "=== REFRESH LOGIN STATE CALLED ===")
        val isLoggedIn = UserSession.isLoggedIn()
        android.util.Log.d("BookmarkViewModel", "User logged in: $isLoggedIn")
        android.util.Log.d("BookmarkViewModel", "User ID: ${UserSession.getUserId()}")

        _isLoggedIn.value = isLoggedIn

        if (isLoggedIn) {
            android.util.Log.d("BookmarkViewModel", "User is logged in - loading bookmarks and updating states")


            val currentStates = _bookmarkStatesMap.value
            android.util.Log.d("BookmarkViewModel", "Current bookmark states before refresh: ${currentStates?.size ?: 0} total")
            currentStates?.filter { it.value }?.forEach { (jobId, isBookmarked) ->
                android.util.Log.d("BookmarkViewModel", "Current bookmarked job: $jobId = $isBookmarked")
            }

            loadBookmarkedJobs()



            if (_bookmarkStatesMap.value.isNullOrEmpty()) {
                android.util.Log.d("BookmarkViewModel", "Global bookmark states are empty - initializing them")
                updateGlobalBookmarkStates()
            } else {
                android.util.Log.d("BookmarkViewModel", "Global bookmark states already exist - preserving them")
            }


            val newStates = _bookmarkStatesMap.value
            android.util.Log.d("BookmarkViewModel", "New bookmark states after refresh: ${newStates?.size ?: 0} total")
            newStates?.filter { it.value }?.forEach { (jobId, isBookmarked) ->
                android.util.Log.d("BookmarkViewModel", "New bookmarked job: $jobId = $isBookmarked")
            }
        } else {
            android.util.Log.d("BookmarkViewModel", "User not logged in - showing guest state")
            showGuestState()
        }
        android.util.Log.d("BookmarkViewModel", "=== REFRESH LOGIN STATE COMPLETED ===")
    }

    fun loadBookmarkedJobs() {
        android.util.Log.d("BookmarkViewModel", "=== LOADING BOOKMARKED JOBS ===")
        _isLoading.value = true
        showLoadingState.set(true)

        viewModelScope.launch {
            try {
                android.util.Log.d("BookmarkViewModel", "Current user: ${UserSession.getUserId()}")
                android.util.Log.d("BookmarkViewModel", "User logged in: ${UserSession.isLoggedIn()}")


                val bookmarkedJobModels = mainRepository.getBookmarkedJobsWithViewCounts()

                android.util.Log.d("BookmarkViewModel", "Repository returned ${bookmarkedJobModels.size} bookmarked jobs")
                bookmarkedJobModels.forEachIndexed { index, job ->
                    android.util.Log.d("BookmarkViewModel", "Bookmarked job $index: ${job.title} at ${job.company}")
                }

                _bookmarkedJobs.value = bookmarkedJobModels
                updateUIState(bookmarkedJobModels)

                android.util.Log.d("BookmarkViewModel", "Updated UI state with ${bookmarkedJobModels.size} bookmarked jobs")


                updateGlobalBookmarkStates()

                _isLoading.value = false
                showLoadingState.set(false)

                android.util.Log.d("BookmarkViewModel", "=== BOOKMARKED JOBS LOADING COMPLETED ===")
            } catch (e: Exception) {
                _isLoading.value = false
                showLoadingState.set(false)
                _showErrorMessage.value = "Failed to load bookmarked jobs: ${e.message}"
                android.util.Log.e("BookmarkViewModel", "Error loading bookmarked jobs", e)
                handleLoadError()
            }
        }
    }

    fun onJobClicked(job: JobModel) {
        _jobClicked.value = job
    }

    fun onJobClickHandled() {
        _jobClicked.value = null
    }

    fun onBookmarkToggledHandled() {
        _bookmarkToggled.value = null
    }

    fun searchBookmarks(query: String) {
        searchQuery.set(query)

        val currentJobs = _bookmarkedJobs.value ?: emptyList()
        val filteredJobs = if (query.isEmpty()) {
            currentJobs
        } else {
            currentJobs.filter { job ->
                job.title.contains(query, ignoreCase = true) ||
                job.company.contains(query, ignoreCase = true) ||
                job.category.contains(query, ignoreCase = true)
            }
        }

        _bookmarkedJobs.value = filteredJobs
        updateUIState(filteredJobs)
    }

    fun clearSearch() {
        searchQuery.set("")
        loadBookmarkedJobs()
    }


    private fun updateUIState(jobs: List<JobModel>) {
        val jobCount = jobs.size
        android.util.Log.d("BookmarkViewModel", "=== UPDATING UI STATE ===")
        android.util.Log.d("BookmarkViewModel", "Job count: $jobCount")

        bookmarkCount.set(jobCount.toString())

        val isLoggedIn = UserSession.isLoggedIn()
        android.util.Log.d("BookmarkViewModel", "User logged in: $isLoggedIn")
        _isEmpty.value = jobCount == 0

        when {
            !isLoggedIn -> {
                android.util.Log.d("BookmarkViewModel", "Showing guest state")
                showGuestState()
            }
            jobCount == 0 -> {
                android.util.Log.d("BookmarkViewModel", "Showing empty state for logged user")
                showEmptyStateForLoggedUser()
            }
            else -> {
                android.util.Log.d("BookmarkViewModel", "Showing bookmarks list state")
                showBookmarksListState()
            }
        }

        android.util.Log.d("BookmarkViewModel", "=== UI STATE UPDATE COMPLETED ===")
    }

    private fun showGuestState() {

        showGuestState.set(true)
        showEmptyState.set(false)
        showBookmarksList.set(false)
        emptyStateTitle.set("Login Required")
        emptyStateSubtitle.set("Please log in to access your saved jobs")
    }

    private fun showEmptyStateForLoggedUser() {

        showGuestState.set(false)
        showEmptyState.set(true)
        showBookmarksList.set(false)
        emptyStateTitle.set("No saved jobs yet")
        emptyStateSubtitle.set("Start saving jobs you're interested in")
    }

    private fun showBookmarksListState() {

        showGuestState.set(false)
        showEmptyState.set(false)
        showBookmarksList.set(true)
    }

    private fun handleLoadError() {

        showGuestState.set(false)
        showEmptyState.set(true)
        showBookmarksList.set(false)
        emptyStateTitle.set("Failed to load bookmarks")
        emptyStateSubtitle.set("Please try again later")
    }


    fun onSuccessMessageShown() {
        _showSuccessMessage.value = null
    }

    fun onErrorMessageShown() {
        _showErrorMessage.value = null
    }


    fun getBookmarkCountDisplay(): String {
        val count = _bookmarkedJobs.value?.size ?: 0
        return when (count) {
            0 -> "No saved jobs"
            1 -> "1 saved job"
            else -> "$count saved jobs"
        }
    }

    fun canBookmarkJob(job: JobModel): Boolean {

        return mainRepository.canBookmarkJob(job)
    }

    fun shouldShowBookmarkIcon(job: JobModel): Boolean {

        return mainRepository.canBookmarkJob(job)
    }
}