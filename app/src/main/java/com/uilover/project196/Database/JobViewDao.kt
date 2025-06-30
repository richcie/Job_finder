package com.uilover.project196.Database

import androidx.room.*
import com.uilover.project196.Model.JobViewEntity

@Dao
interface JobViewDao {

    @Query("SELECT COUNT(*) FROM job_views WHERE jobId = :jobId")
    suspend fun getViewCountForJob(jobId: Int): Int

    @Query("SELECT COUNT(DISTINCT viewerUserId) FROM job_views WHERE jobId = :jobId")
    suspend fun getUniqueViewersForJob(jobId: Int): Int

    @Query("SELECT * FROM job_views WHERE jobId = :jobId ORDER BY viewedAt DESC")
    suspend fun getViewsForJob(jobId: Int): List<JobViewEntity>

    @Query("SELECT * FROM job_views WHERE viewerUserId = :userId ORDER BY viewedAt DESC")
    suspend fun getViewsByUser(userId: String): List<JobViewEntity>

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertView(view: JobViewEntity)

    @Query("SELECT COUNT(*) FROM job_views WHERE jobId = :jobId AND viewerUserId = :userId AND viewedAt > :timestamp")
    suspend fun hasUserViewedJobRecently(jobId: Int, userId: String, timestamp: Long): Int

    @Query("SELECT COUNT(*) FROM job_views WHERE jobId = :jobId AND viewerUserId = :userId")
    suspend fun hasUserEverViewedJob(jobId: Int, userId: String): Int

    @Query("DELETE FROM job_views WHERE viewedAt < :cutoffTime")
    suspend fun deleteOldViews(cutoffTime: Long)


    @Query("SELECT COUNT(*) FROM job_views WHERE jobId IN (SELECT id FROM jobs WHERE ownerId = :ownerId)")
    suspend fun getTotalViewsForOwner(ownerId: String): Int

    @Query("SELECT jobId, COUNT(*) as viewCount FROM job_views WHERE jobId IN (SELECT id FROM jobs WHERE ownerId = :ownerId) GROUP BY jobId ORDER BY viewCount DESC")
    suspend fun getViewStatsForOwner(ownerId: String): List<JobViewStats>

    @Query("DELETE FROM job_views")
    suspend fun deleteAllViews()
}

data class JobViewStats(
    val jobId: Int,
    val viewCount: Int
)