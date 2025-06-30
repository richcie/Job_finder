package com.uilover.project196.Database

import androidx.room.*
import com.uilover.project196.Model.JobApplicationEntity

@Dao
interface JobApplicationDao {

    @Query("SELECT * FROM job_applications WHERE jobId = :jobId ORDER BY appliedAt DESC")
    suspend fun getApplicationsForJob(jobId: Int): List<JobApplicationEntity>

    @Query("SELECT * FROM job_applications WHERE applicantUserId = :userId ORDER BY appliedAt DESC")
    suspend fun getApplicationsByUser(userId: String): List<JobApplicationEntity>

    @Query("SELECT * FROM job_applications WHERE applicantUserId = :userId ORDER BY appliedAt DESC")
    suspend fun getApplicationsByUserId(userId: String): List<JobApplicationEntity>

    @Query("SELECT * FROM job_applications WHERE applicantUserId = :userId AND status = 'shortlisted' ORDER BY appliedAt DESC")
    suspend fun getShortlistedApplicationsByUserId(userId: String): List<JobApplicationEntity>

    @Query("SELECT * FROM job_applications WHERE jobId = :jobId ORDER BY appliedAt DESC")
    suspend fun getApplicationsByJobId(jobId: Int): List<JobApplicationEntity>

    @Query("SELECT * FROM job_applications WHERE jobId IN (:jobIds) ORDER BY appliedAt DESC")
    suspend fun getApplicationsByJobIds(jobIds: List<Int>): List<JobApplicationEntity>

    @Query("SELECT * FROM job_applications WHERE jobId = :jobId AND applicantUserId = :userId")
    suspend fun getApplicationByJobAndUser(jobId: Int, userId: String): JobApplicationEntity?

    @Query("SELECT COUNT(*) FROM job_applications WHERE jobId = :jobId")
    suspend fun getApplicationCountForJob(jobId: Int): Int

    @Query("SELECT COUNT(*) FROM job_applications WHERE applicantUserId = :userId")
    suspend fun getApplicationCountByUser(userId: String): Int

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertApplication(application: JobApplicationEntity)

    @Update
    suspend fun updateApplication(application: JobApplicationEntity)

    @Delete
    suspend fun deleteApplication(application: JobApplicationEntity)

    @Query("UPDATE job_applications SET status = :status WHERE id = :applicationId")
    suspend fun updateApplicationStatus(applicationId: Int, status: String)

    @Query("SELECT * FROM job_applications WHERE status = :status ORDER BY appliedAt DESC")
    suspend fun getApplicationsByStatus(status: String): List<JobApplicationEntity>

    @Query("DELETE FROM job_applications")
    suspend fun deleteAllApplications()

    @Query("DELETE FROM job_applications WHERE jobId IN (SELECT id FROM jobs WHERE ownerId = :ownerId)")
    suspend fun deleteApplicationsForOwner(ownerId: String)

    @Query("UPDATE job_applications SET status = 'pending' WHERE status = 'shortlisted'")
    suspend fun resetShortlistedApplicationsToPending()

    @Query("SELECT * FROM job_applications ORDER BY appliedAt DESC")
    suspend fun getAllApplications(): List<JobApplicationEntity>

    @Query("SELECT * FROM job_applications WHERE id = :applicationId")
    suspend fun getApplicationById(applicationId: Int): JobApplicationEntity?

    @Query("SELECT * FROM job_applications WHERE applicantUserId = :userId")
    suspend fun getAllApplicationsByUserId(userId: String): List<JobApplicationEntity>

    @Update
    suspend fun updateJobApplication(application: JobApplicationEntity)
}