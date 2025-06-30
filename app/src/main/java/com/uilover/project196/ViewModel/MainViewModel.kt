package com.uilover.project196.ViewModel

import android.app.Application
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.ViewModel
import androidx.lifecycle.ViewModelProvider
import androidx.lifecycle.viewModelScope
import com.uilover.project196.Model.JobEntity
import com.uilover.project196.Model.JobModel
import com.uilover.project196.Repository.MainRepository
import com.uilover.project196.Repository.UserRepository
import com.uilover.project196.Utils.toJobEntity
import kotlinx.coroutines.launch

class MainViewModel(val repository: MainRepository) : ViewModel() {
constructor() : this(MainRepository.getInstance())

    private var userRepository: UserRepository? = null

    fun initializeDatabase(context: android.content.Context) {
        repository.initializeDatabase(context)
        userRepository = UserRepository.getInstance(context)

        if (!isInitialized) {
            isInitialized = true
            syncJobsToDatabase()
        }
    }

    private var isInitialized = false

    private fun syncJobsToDatabase() {
        viewModelScope.launch {
            try {
                userRepository?.initializeSampleData()

                val allJobs = repository.allItems
                val jobsToInsert = mutableListOf<com.uilover.project196.Model.JobEntity>()

                for (job in allJobs) {
                    try {
                        val existingJob = repository.findJobEntity(job)
                        if (existingJob == null) {
                            jobsToInsert.add(job.toJobEntity())
                        }
                    } catch (e: Exception) {
                        android.util.Log.e("MainViewModel", "syncJobsToDatabase: Error checking job ${job.title}", e)
                    }
                }

                if (jobsToInsert.isNotEmpty()) {
                    for (jobEntity in jobsToInsert) {
                        repository.insertJob(jobEntity)
                    }
                }

            } catch (e: Exception) {
                android.util.Log.e("MainViewModel", "syncJobsToDatabase: Error during optimized sync", e)
            }
        }
    }

    fun loadLocation() = repository.location

    fun loadCategory() = repository.category

    fun loadData() = repository.items

    fun loadAllData() = repository.allItems


    suspend fun loadDataWithViewCounts(): List<JobModel> {
        return repository.getJobsWithViewCounts()
    }

    suspend fun loadAllDataWithViewCounts(): List<JobModel> {
        return repository.getAllJobsWithViewCounts()
    }

    fun toggleBookmark(job: JobModel) {
        try {
            repository.toggleBookmark(job)
        } catch (e: Exception) {
            android.util.Log.e("MainViewModel", "ERROR in repository.toggleBookmark: ${e.message}", e)
        }
    }

    fun getBookmarkedJobs(): List<JobModel> {
        return repository.getBookmarkedJobs()
    }

    suspend fun getBookmarkedJobsWithViewCounts(): List<JobModel> {
        return repository.getBookmarkedJobsWithViewCounts()
    }

    fun isJobBookmarked(job: JobModel): Boolean {
        return repository.isJobBookmarked(job)
    }

    fun canBookmarkJob(job: JobModel): Boolean {
        return repository.canBookmarkJob(job)
    }

    fun getOwnedJobs(): List<JobModel> {
        return repository.getOwnedJobs()
    }

    fun getOpenOwnedJobs(): List<JobModel> {
        return repository.getOpenOwnedJobs()
    }

    fun getClosedOwnedJobs(): List<JobModel> {
        return repository.getClosedOwnedJobs()
    }

    fun closeJob(job: JobModel): Boolean {
        return repository.closeJob(job)
    }

    fun reopenJob(job: JobModel): Boolean {
        return repository.reopenJob(job)
    }

    fun updateJob(updatedJob: JobModel): Boolean {
        return repository.updateJob(updatedJob)
    }

    fun createJob(jobModel: JobModel): Boolean {
        return repository.createJob(jobModel)
    }

    fun getFilterOptions() = repository.getFilterOptions()


    suspend fun findJobEntity(jobModel: JobModel): com.uilover.project196.Model.JobEntity? {
        return repository.findJobEntity(jobModel)
    }

    fun insertJob(jobEntity: JobEntity) = viewModelScope.launch {
        repository.insertJob(jobEntity)
    }


    suspend fun ensureJobInDatabase(jobModel: JobModel): Int? {
        return try {
            var jobEntity = repository.findJobEntity(jobModel)
            if (jobEntity == null) {
                val newJobEntity = jobModel.toJobEntity()
                repository.insertJob(newJobEntity)
                jobEntity = repository.findJobEntity(jobModel)
            }
            jobEntity?.id
        } catch (e: Exception) {
            android.util.Log.e("MainViewModel", "ensureJobInDatabase: Error ensuring job in database", e)
            null
        }
    }


    suspend fun forceSyncJobsToDatabase() {
        viewModelScope.launch {
            try {
                val allJobs = repository.allItems
                for (job in allJobs) {
                    try {
                        val existingJob = repository.findJobEntity(job)
                        if (existingJob == null) {
                            val jobEntity = job.toJobEntity()
                            repository.insertJob(jobEntity)
                        }
                    } catch (e: Exception) {
                        android.util.Log.e("MainViewModel", "forceSyncJobsToDatabase: Error syncing job ${job.title}", e)
                    }
                }
                userRepository?.initializeSampleData()
            } catch (e: Exception) {
                android.util.Log.e("MainViewModel", "forceSyncJobsToDatabase: Error during manual sync", e)
            }
        }
    }
}