package com.uilover.project196.ViewModel

import android.app.Application
import androidx.databinding.ObservableField
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.LiveData
import androidx.lifecycle.MutableLiveData
import androidx.lifecycle.viewModelScope
import com.uilover.project196.Model.JobModel
import com.uilover.project196.Repository.MainRepository
import com.uilover.project196.Utils.UserSession
import kotlinx.coroutines.launch

class ExplorerViewModel(application: Application) : AndroidViewModel(application) {

    private val repository = MainRepository.getInstance()

    private val _allJobs = MutableLiveData<List<JobModel>>()
    val allJobs: LiveData<List<JobModel>> = _allJobs

    private val _filteredJobs = MutableLiveData<List<JobModel>>()
    val filteredJobs: LiveData<List<JobModel>> = _filteredJobs

    private val _isLoading = MutableLiveData<Boolean>()
    val isLoading: LiveData<Boolean> = _isLoading

    private val _showSuccessMessage = MutableLiveData<String?>()
    val showSuccessMessage: LiveData<String?> = _showSuccessMessage

    private val _showErrorMessage = MutableLiveData<String?>()
    val showErrorMessage: LiveData<String?> = _showErrorMessage

    private val _availableSalaryRanges = MutableLiveData<List<String>>()
    val availableSalaryRanges: LiveData<List<String>> = _availableSalaryRanges

    private val _availableLevels = MutableLiveData<List<String>>()
    val availableLevels: LiveData<List<String>> = _availableLevels

    val searchQuery = ObservableField<String>("")
    val categoryFilter = ObservableField<String>("0")


    val selectedSalaryRanges = ObservableField<List<String>>(listOf())
    val selectedLevels = ObservableField<List<String>>(listOf())
    val selectedWorkTime = ObservableField<String>("")
    val selectedWorkModel = ObservableField<String>("")


    val resultCount = ObservableField<String>("0")
    val emptyStateTitle = ObservableField<String>("No Jobs Found")
    val emptyStateSubtitle = ObservableField<String>("Try adjusting your search or filters")


    val showEmptyState = ObservableField<Boolean>(false)
    val showLoadingState = ObservableField<Boolean>(false)
    val showContentState = ObservableField<Boolean>(false)
    val showGuestState = ObservableField<Boolean>(false)


    val isFullTimeSelected = ObservableField<Boolean>(false)
    val isPartTimeSelected = ObservableField<Boolean>(false)
    val isRemoteSelected = ObservableField<Boolean>(false)
    val isInPersonSelected = ObservableField<Boolean>(false)


    val activeFilterCount = ObservableField<String>("0")
    val hasActiveFilters = ObservableField<Boolean>(false)

    init {

        setupReactiveFiltering()
    }





    private fun setupReactiveFiltering() {

        searchQuery.addOnPropertyChangedCallback(
            object : androidx.databinding.Observable.OnPropertyChangedCallback() {
                override fun onPropertyChanged(sender: androidx.databinding.Observable?, propertyId: Int) {
                    applyFilters()
                }
            }
        )


        categoryFilter.addOnPropertyChangedCallback(
            object : androidx.databinding.Observable.OnPropertyChangedCallback() {
                override fun onPropertyChanged(sender: androidx.databinding.Observable?, propertyId: Int) {
                    applyFilters()
                }
            }
        )


        selectedSalaryRanges.addOnPropertyChangedCallback(
            object : androidx.databinding.Observable.OnPropertyChangedCallback() {
                override fun onPropertyChanged(sender: androidx.databinding.Observable?, propertyId: Int) {
                    applyFilters()
                    updateActiveFilterCount()
                }
            }
        )


        selectedLevels.addOnPropertyChangedCallback(
            object : androidx.databinding.Observable.OnPropertyChangedCallback() {
                override fun onPropertyChanged(sender: androidx.databinding.Observable?, propertyId: Int) {
                    applyFilters()
                    updateActiveFilterCount()
                }
            }
        )


        selectedWorkTime.addOnPropertyChangedCallback(
            object : androidx.databinding.Observable.OnPropertyChangedCallback() {
                override fun onPropertyChanged(sender: androidx.databinding.Observable?, propertyId: Int) {
                    applyFilters()
                    updateActiveFilterCount()
                    updateWorkTimeSelections()
                }
            }
        )


        selectedWorkModel.addOnPropertyChangedCallback(
            object : androidx.databinding.Observable.OnPropertyChangedCallback() {
                override fun onPropertyChanged(sender: androidx.databinding.Observable?, propertyId: Int) {
                    applyFilters()
                    updateActiveFilterCount()
                    updateWorkModelSelections()
                }
            }
        )
    }

    fun loadJobs(mainViewModel: com.uilover.project196.ViewModel.MainViewModel) {
        _isLoading.value = true
        showLoadingState.set(true)

        viewModelScope.launch {
            try {
                android.util.Log.d("ExplorerViewModel", "Loading jobs with view counts")
                val rawJobs = mainViewModel.loadDataWithViewCounts()

                val jobs = if (rawJobs.isEmpty()) {
                    android.util.Log.w("ExplorerViewModel", "No jobs from database, using basic data")
                    mainViewModel.loadData().map { it.copy(viewCount = 0) }
                } else {
                    rawJobs
                }

                _allJobs.value = jobs


                _availableSalaryRanges.value = jobs.map { it.salary }.distinct()
                _availableLevels.value = jobs.map { it.level }.distinct()

                android.util.Log.d("ExplorerViewModel", "Loaded ${jobs.size} jobs")

                _isLoading.value = false
                showLoadingState.set(false)


                applyFilters()

            } catch (e: Exception) {
                android.util.Log.e("ExplorerViewModel", "Error loading jobs", e)
                _isLoading.value = false
                showLoadingState.set(false)
                _showErrorMessage.value = "Failed to load jobs: ${e.message}"
                handleLoadError()
            }
        }
    }

    private fun applyFilters() {
        val jobs = _allJobs.value ?: return

        var filtered = jobs


        val categoryValue = categoryFilter.get() ?: "0"
        if (categoryValue != "0") {
            filtered = filtered.filter { it.category == categoryValue }
        }


        val query = searchQuery.get()?.trim() ?: ""
        if (query.isNotEmpty()) {
            filtered = filtered.filter { job ->
                job.title.contains(query, ignoreCase = true) ||
                job.company.contains(query, ignoreCase = true) ||
                job.category.contains(query, ignoreCase = true)
            }
        }


        val salaryRanges = selectedSalaryRanges.get() ?: listOf()
        if (salaryRanges.isNotEmpty()) {
            filtered = filtered.filter { job ->
                salaryRanges.contains(job.salary)
            }
        }


        val levels = selectedLevels.get() ?: listOf()
        if (levels.isNotEmpty()) {
            filtered = filtered.filter { job ->
                levels.contains(job.level)
            }
        }


        val workTime = selectedWorkTime.get() ?: ""
        if (workTime.isNotEmpty()) {
            filtered = filtered.filter { job ->
                job.time.equals(workTime, ignoreCase = true)
            }
        }


        val workModel = selectedWorkModel.get() ?: ""
        if (workModel.isNotEmpty()) {
            filtered = filtered.filter { job ->
                job.model.equals(workModel, ignoreCase = true)
            }
        }


        val currentUserId = UserSession.getUserId()
        if (currentUserId != null) {
            val ownedJobs = filtered.filter { it.ownerId == currentUserId }
            val otherJobs = filtered.filter { it.ownerId != currentUserId }
            filtered = ownedJobs + otherJobs
        }

        _filteredJobs.value = filtered
        updateUIState(filtered)

        android.util.Log.d("ExplorerViewModel", "Applied filters: ${filtered.size} jobs found")
    }

    private fun updateUIState(filteredJobs: List<JobModel>) {
        val count = filteredJobs.size
        resultCount.set(count.toString())

        when {
            !UserSession.isLoggedIn() -> showGuestState()
            count == 0 -> showEmptyStateForUser()
            else -> showContentState()
        }
    }

    private fun showGuestState() {
        showGuestState.set(true)
        showEmptyState.set(false)
        showContentState.set(false)
        emptyStateTitle.set("Login Required")
        emptyStateSubtitle.set("Please log in to search and filter jobs")
    }

    private fun showEmptyStateForUser() {
        showGuestState.set(false)
        showEmptyState.set(true)
        showContentState.set(false)

        val hasFilters = hasActiveFilters.get() == true
        if (hasFilters) {
            emptyStateTitle.set("No Jobs Match Your Filters")
            emptyStateSubtitle.set("Try adjusting your search or filter criteria")
        } else {
            emptyStateTitle.set("No Jobs Available")
            emptyStateSubtitle.set("Check back later for new opportunities")
        }
    }

    private fun showContentState() {
        showGuestState.set(false)
        showEmptyState.set(false)
        showContentState.set(true)
    }

    private fun handleLoadError() {
        showGuestState.set(false)
        showEmptyState.set(true)
        showContentState.set(false)
        emptyStateTitle.set("Error Loading Jobs")
        emptyStateSubtitle.set("Please try again later")
    }





    fun updateSalaryRanges(ranges: List<String>) {
        selectedSalaryRanges.set(ranges)
    }

    fun updateLevels(levels: List<String>) {
        selectedLevels.set(levels)
    }

    fun setWorkTime(workTime: String) {
        selectedWorkTime.set(workTime)
    }

    fun setWorkModel(workModel: String) {
        selectedWorkModel.set(workModel)
    }

    fun clearAllFilters() {
        selectedSalaryRanges.set(listOf())
        selectedLevels.set(listOf())
        selectedWorkTime.set("")
        selectedWorkModel.set("")

    }

    private fun updateActiveFilterCount() {
        val salaryCount = selectedSalaryRanges.get()?.size ?: 0
        val levelCount = selectedLevels.get()?.size ?: 0
        val workTimeCount = if (selectedWorkTime.get()?.isNotEmpty() == true) 1 else 0
        val workModelCount = if (selectedWorkModel.get()?.isNotEmpty() == true) 1 else 0

        val totalCount = salaryCount + levelCount + workTimeCount + workModelCount
        activeFilterCount.set(totalCount.toString())
        hasActiveFilters.set(totalCount > 0)
    }

    private fun updateWorkTimeSelections() {
        val workTime = selectedWorkTime.get() ?: ""
        isFullTimeSelected.set(workTime == "Full-Time")
        isPartTimeSelected.set(workTime == "Part-Time")
    }

    private fun updateWorkModelSelections() {
        val workModel = selectedWorkModel.get() ?: ""
        isRemoteSelected.set(workModel == "Remote")
        isInPersonSelected.set(workModel == "In Person")
    }





    fun onSearchQueryChanged(query: String) {
        searchQuery.set(query)
    }

    fun onCategorySelected(category: String) {
        categoryFilter.set(category)
    }

    fun toggleFullTime() {
        val current = selectedWorkTime.get() ?: ""
        if (current == "Full-Time") {
            selectedWorkTime.set("")
        } else {
            selectedWorkTime.set("Full-Time")
        }
    }

    fun togglePartTime() {
        val current = selectedWorkTime.get() ?: ""
        if (current == "Part-Time") {
            selectedWorkTime.set("")
        } else {
            selectedWorkTime.set("Part-Time")
        }
    }

    fun toggleRemote() {
        val current = selectedWorkModel.get() ?: ""
        if (current == "Remote") {
            selectedWorkModel.set("")
        } else {
            selectedWorkModel.set("Remote")
        }
    }

    fun toggleInPerson() {
        val current = selectedWorkModel.get() ?: ""
        if (current == "In Person") {
            selectedWorkModel.set("")
        } else {
            selectedWorkModel.set("In Person")
        }
    }


    fun onSuccessMessageShown() { _showSuccessMessage.value = null }
    fun onErrorMessageShown() { _showErrorMessage.value = null }
}