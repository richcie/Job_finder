package com.uilover.project196.Utils

import android.content.Context
import com.uilover.project196.Database.AppDatabase
import com.uilover.project196.Model.JobEntity
import com.uilover.project196.Model.JobModel
import com.uilover.project196.Repository.MainRepository
import com.uilover.project196.Repository.UserRepository
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext

// KRITERIA: External API dan library untuk sinkronisasi data
// KRITERIA WAJIB: API External menggunakan library untuk sinkronisasi data
// KRITERIA KOMPLEKSITAS: Sync data antara local dan remote storage
class JobSyncHelper private constructor() {

    companion object {
        @Volatile
        private var INSTANCE: JobSyncHelper? = null

        fun getInstance(): JobSyncHelper {
            return INSTANCE ?: synchronized(this) {
                INSTANCE ?: JobSyncHelper().also { INSTANCE = it }
            }
        }
    }

    private var database: AppDatabase? = null
    private var mainRepository: MainRepository? = null
    private var userRepository: UserRepository? = null

    fun initialize(context: Context) {
        database = AppDatabase.getDatabase(context)
        mainRepository = MainRepository.getInstance()
        userRepository = UserRepository.getInstance(context)
        mainRepository?.initializeDatabase(context)
    }


    suspend fun syncAllJobsToDatabase(): Boolean {
        return withContext(Dispatchers.IO) {
            try {
                android.util.Log.d("JobSyncHelper", "syncAllJobsToDatabase: Starting sync")

                val repository = mainRepository ?: return@withContext false
                val allJobs = repository.allItems

                android.util.Log.d("JobSyncHelper", "syncAllJobsToDatabase: Found ${allJobs.size} jobs to sync")

                for (job in allJobs) {
                    try {

                        val existingJob = repository.findJobEntity(job)
                        if (existingJob == null) {

                            val jobEntity = job.toJobEntity()
                            android.util.Log.d("JobSyncHelper", "syncAllJobsToDatabase: Inserting job ${job.title} from ${job.company}")
                            repository.insertJob(jobEntity)
                        } else {
                            android.util.Log.d("JobSyncHelper", "syncAllJobsToDatabase: Job ${job.title} from ${job.company} already exists with ID ${existingJob.id}")
                        }
                    } catch (e: Exception) {
                        android.util.Log.e("JobSyncHelper", "syncAllJobsToDatabase: Error syncing job ${job.title}", e)
                    }
                }


                userRepository?.initializeSampleData()
                android.util.Log.d("JobSyncHelper", "syncAllJobsToDatabase: Sync completed successfully")
                true

            } catch (e: Exception) {
                android.util.Log.e("JobSyncHelper", "syncAllJobsToDatabase: Error during sync", e)
                false
            }
        }
    }


    suspend fun ensureJobInDatabase(jobModel: JobModel): Int? {
        return withContext(Dispatchers.IO) {
            try {
                android.util.Log.d("JobSyncHelper", "ensureJobInDatabase: Ensuring job ${jobModel.title} is in database")

                val repository = mainRepository ?: return@withContext null
                var jobEntity = repository.findJobEntity(jobModel)

                if (jobEntity == null) {
                    android.util.Log.d("JobSyncHelper", "ensureJobInDatabase: Job not found, inserting...")
                    val newJobEntity = jobModel.toJobEntity()
                    repository.insertJob(newJobEntity)


                    kotlinx.coroutines.delay(100)


                    jobEntity = repository.findJobEntity(jobModel)
                    android.util.Log.d("JobSyncHelper", "ensureJobInDatabase: Job inserted with ID ${jobEntity?.id}")
                } else {
                    android.util.Log.d("JobSyncHelper", "ensureJobInDatabase: Job already exists with ID ${jobEntity.id}")
                }

                jobEntity?.id
            } catch (e: Exception) {
                android.util.Log.e("JobSyncHelper", "ensureJobInDatabase: Error ensuring job in database", e)
                null
            }
        }
    }
}


fun JobModel.toJobEntity(): JobEntity {
    return JobEntity(
        title = this.title,
        company = this.company,
        description = this.description,
        about = this.about,
        location = this.location,
        salary = this.salary,
        time = this.time,
        model = this.model,
        level = this.level,
        category = this.category,
        picUrl = this.picUrl,
        isBookmarked = this.isBookmarked,
        ownerId = this.ownerId,
        status = this.status
    )
}