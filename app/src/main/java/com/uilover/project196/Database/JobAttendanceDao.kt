package com.uilover.project196.Database

import androidx.room.Dao
import androidx.room.Insert
import androidx.room.OnConflictStrategy
import androidx.room.Query
import androidx.room.Update
import com.uilover.project196.Model.JobAttendanceEntity

@Dao
// KRITERIA WAJIB: Room DAO untuk operasi database JobAttendance
interface JobAttendanceDao {

    @Insert(onConflict = OnConflictStrategy.IGNORE)
    suspend fun insertAttendance(attendance: JobAttendanceEntity): Long

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun upsertAttendance(attendance: JobAttendanceEntity): Long

    @Update
    suspend fun updateAttendance(attendance: JobAttendanceEntity)

    @Query("SELECT * FROM job_attendance WHERE freelancerId = :freelancerId ORDER BY attendanceDate DESC")
    suspend fun getAttendanceByFreelancer(freelancerId: String): List<JobAttendanceEntity>

    @Query("SELECT * FROM job_attendance WHERE jobId = :jobId AND freelancerId = :freelancerId ORDER BY attendanceDate DESC")
    suspend fun getAttendanceByJobAndFreelancer(jobId: Int, freelancerId: String): List<JobAttendanceEntity>

    @Query("SELECT * FROM job_attendance WHERE jobId = :jobId AND freelancerId = :freelancerId AND attendanceDate = :date LIMIT 1")
    suspend fun getTodayAttendance(jobId: Int, freelancerId: String, date: String): JobAttendanceEntity?

    @Query("SELECT * FROM job_attendance WHERE jobId = :jobId ORDER BY attendanceDate DESC")
    suspend fun getAttendanceByJob(jobId: Int): List<JobAttendanceEntity>

    @Query("SELECT * FROM job_attendance WHERE attendanceDate = :date")
    suspend fun getAttendanceByDate(date: String): List<JobAttendanceEntity>

    @Query("SELECT * FROM job_attendance WHERE attendanceDate >= :startDate AND attendanceDate <= :endDate ORDER BY attendanceDate ASC")
    suspend fun getAttendanceByDateRange(startDate: String, endDate: String): List<JobAttendanceEntity>

    @Query("SELECT DISTINCT freelancerId FROM job_attendance WHERE jobId = :jobId")
    suspend fun getFreelancersByJob(jobId: Int): List<String>

    @Query("SELECT DISTINCT jobId FROM job_attendance WHERE freelancerId = :freelancerId")
    suspend fun getJobsByFreelancer(freelancerId: String): List<Int>

    @Query("DELETE FROM job_attendance WHERE jobId = :jobId")
    suspend fun deleteAttendanceByJob(jobId: Int)

    @Query("DELETE FROM job_attendance WHERE jobId = :jobId AND freelancerId = :freelancerId")
    suspend fun deleteAttendanceByFreelancerAndJob(jobId: Int, freelancerId: String)

    @Query("DELETE FROM job_attendance WHERE attendanceDate < :cutoffDate")
    suspend fun deleteOldAttendanceRecords(cutoffDate: String)

    @Query("DELETE FROM job_attendance")
    suspend fun deleteAllAttendance()
}