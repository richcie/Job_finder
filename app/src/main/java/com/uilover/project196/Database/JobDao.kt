package com.uilover.project196.Database

import androidx.room.*
import com.uilover.project196.Model.JobEntity

@Dao
// KRITERIA WAJIB: Room DAO untuk operasi database Job
interface JobDao {

    @Query("SELECT * FROM jobs")
    suspend fun getAllJobs(): List<JobEntity>

    @Query("SELECT * FROM jobs WHERE isBookmarked = 1")
    suspend fun getBookmarkedJobs(): List<JobEntity>

    @Query("SELECT * FROM jobs WHERE category = :category")
    suspend fun getJobsByCategory(category: String): List<JobEntity>

    @Query("SELECT * FROM jobs WHERE ownerId = :ownerId")
    suspend fun getJobsByOwner(ownerId: String): List<JobEntity>

    @Query("SELECT * FROM jobs WHERE ownerId = :ownerId")
    suspend fun getJobsByOwnerId(ownerId: String): List<JobEntity>

    @Query("SELECT * FROM jobs WHERE id = :jobId")
    suspend fun getJobById(jobId: Int): JobEntity?

    @Query("SELECT * FROM jobs WHERE id IN (:jobIds)")
    suspend fun getJobsByIds(jobIds: List<Int>): List<JobEntity>

    @Query("SELECT * FROM jobs WHERE status = 'open'")
    suspend fun getOpenJobs(): List<JobEntity>

    @Query("SELECT * FROM jobs WHERE ownerId = :ownerId AND status = 'open'")
    suspend fun getOpenJobsByOwner(ownerId: String): List<JobEntity>

    @Query("SELECT * FROM jobs WHERE ownerId = :ownerId AND status = 'closed'")
    suspend fun getClosedJobsByOwner(ownerId: String): List<JobEntity>

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertJob(job: JobEntity): Long

    @Query("SELECT * FROM jobs WHERE title = :title AND company = :company AND (:ownerId IS NULL AND ownerId IS NULL OR ownerId = :ownerId)")
    suspend fun getJobByDetails(title: String, company: String, ownerId: String?): JobEntity?

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertJobs(jobs: List<JobEntity>)

    @Update
    suspend fun updateJob(job: JobEntity)

    @Delete
    suspend fun deleteJob(job: JobEntity)

    @Query("DELETE FROM jobs")
    suspend fun deleteAllJobs()

    @Query("UPDATE jobs SET isBookmarked = :isBookmarked WHERE id = :jobId")
    suspend fun updateBookmarkStatus(jobId: Int, isBookmarked: Boolean)

    @Query("UPDATE jobs SET status = :status WHERE id = :jobId AND ownerId = :ownerId")
    suspend fun updateJobStatus(jobId: Int, ownerId: String, status: String)
}