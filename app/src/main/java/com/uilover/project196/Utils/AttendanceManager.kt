package com.uilover.project196.Utils

import android.content.Context
import com.uilover.project196.Database.AppDatabase
import com.uilover.project196.Model.JobAttendanceEntity
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import java.text.SimpleDateFormat
import java.util.*

class AttendanceManager private constructor(private val context: Context) {

    private val database = AppDatabase.getDatabase(context)
    private val jobAttendanceDao = database.jobAttendanceDao()
    private val jobApplicationDao = database.jobApplicationDao()
    private val dateFormat = SimpleDateFormat("yyyy-MM-dd", Locale.getDefault())

    companion object {
        @Volatile
        private var INSTANCE: AttendanceManager? = null

        fun getInstance(context: Context): AttendanceManager {
            return INSTANCE ?: synchronized(this) {
                val instance = AttendanceManager(context.applicationContext)
                INSTANCE = instance
                instance
            }
        }
    }


    suspend fun processDailyRollingAttendance() = withContext(Dispatchers.IO) {
        try {
            val prefs = context.getSharedPreferences("attendance_manager", Context.MODE_PRIVATE)
            val lastProcessedDate = prefs.getString("last_processed_date", "")
            val today = dateFormat.format(Date())

            if (lastProcessedDate == today) {
                return@withContext
            }

            val calendar = Calendar.getInstance()
            val todayDate = dateFormat.format(calendar.time)
            calendar.add(Calendar.DAY_OF_MONTH, 1)
            val tomorrow = dateFormat.format(calendar.time)


            val shortlistedApplications = jobApplicationDao.getAllApplications()
                .filter { it.status == "shortlisted" }
                .take(1000)

            android.util.Log.d("AttendanceManager", "Found ${shortlistedApplications.size} shortlisted applications")

            var processedFreelancers = 0
            var createdTodayRecords = 0
            var createdTomorrowRecords = 0


            val batchSize = 50
            for (batch in shortlistedApplications.chunked(batchSize)) {
                for (application in batch) {
                    val jobId = application.jobId
                    val freelancerId = application.applicantUserId


                    val (todayCreated, tomorrowCreated) = ensureRollingAttendanceRecords(
                        jobId, freelancerId, todayDate, tomorrow
                    )

                    if (todayCreated) createdTodayRecords++
                    if (tomorrowCreated) createdTomorrowRecords++
                    processedFreelancers++
                }


                kotlinx.coroutines.delay(10)
            }


            prefs.edit().putString("last_processed_date", today).apply()

            android.util.Log.d("AttendanceManager", "=== DAILY ROLLING ATTENDANCE COMPLETE ===")
            android.util.Log.d("AttendanceManager", "Processed: $processedFreelancers freelancers")
            android.util.Log.d("AttendanceManager", "Created: $createdTodayRecords today records, $createdTomorrowRecords tomorrow records")


            if (createdTodayRecords + createdTomorrowRecords > 10) {
                cleanupOldAttendanceRecords()
            }

        } catch (e: Exception) {
            android.util.Log.e("AttendanceManager", "Error processing daily rolling attendance", e)
        }
    }


    suspend fun ensureFreelancerAttendance(jobId: Int, freelancerId: String) = withContext(Dispatchers.IO) {
        try {
            val calendar = Calendar.getInstance()
            val today = dateFormat.format(calendar.time)
            calendar.add(Calendar.DAY_OF_MONTH, 1)
            val tomorrow = dateFormat.format(calendar.time)

            android.util.Log.d("AttendanceManager", "Ensuring attendance for freelancer $freelancerId, job $jobId")

            ensureRollingAttendanceRecords(jobId, freelancerId, today, tomorrow)

        } catch (e: Exception) {
            android.util.Log.e("AttendanceManager", "Error ensuring freelancer attendance", e)
        }
    }


    private suspend fun ensureRollingAttendanceRecords(
        jobId: Int,
        freelancerId: String,
        today: String,
        tomorrow: String
    ): Pair<Boolean, Boolean> {
        var todayCreated = false
        var tomorrowCreated = false

        try {
            val existingRecords = jobAttendanceDao.getAttendanceByJobAndFreelancer(jobId, freelancerId)
            val existingDates = existingRecords.map { it.attendanceDate }.toSet()


            if (!existingDates.contains(today)) {
                val todayRecord = JobAttendanceEntity(
                    jobId = jobId,
                    freelancerId = freelancerId,
                    attendanceDate = today,
                    checkInTime = null,
                    checkOutTime = null,
                    progressReport = null
                )
                jobAttendanceDao.upsertAttendance(todayRecord)
                android.util.Log.d("AttendanceManager", "✅ Created TODAY record: Job $jobId, Freelancer $freelancerId, Date $today")
                todayCreated = true
            }


            if (!existingDates.contains(tomorrow)) {
                val tomorrowRecord = JobAttendanceEntity(
                    jobId = jobId,
                    freelancerId = freelancerId,
                    attendanceDate = tomorrow,
                    checkInTime = null,
                    checkOutTime = null,
                    progressReport = null
                )
                jobAttendanceDao.upsertAttendance(tomorrowRecord)
                android.util.Log.d("AttendanceManager", "✅ Created TOMORROW record: Job $jobId, Freelancer $freelancerId, Date $tomorrow")
                tomorrowCreated = true
            }

        } catch (e: Exception) {
            android.util.Log.e("AttendanceManager", "Error ensuring rolling attendance for job $jobId, freelancer $freelancerId", e)
        }

        return Pair(todayCreated, tomorrowCreated)
    }


    suspend fun createInitialAttendanceForNewFreelancer(jobId: Int, freelancerId: String) = withContext(Dispatchers.IO) {
        try {
            android.util.Log.d("AttendanceManager", "=== CREATING INITIAL ATTENDANCE FOR NEW FREELANCER ===")
            android.util.Log.d("AttendanceManager", "Job: $jobId, Freelancer: $freelancerId")

            val calendar = Calendar.getInstance()
            val today = dateFormat.format(calendar.time)
            calendar.add(Calendar.DAY_OF_MONTH, 1)
            val tomorrow = dateFormat.format(calendar.time)


            val existingRecords = jobAttendanceDao.getAttendanceByJobAndFreelancer(jobId, freelancerId)
            if (existingRecords.isNotEmpty()) {
                android.util.Log.d("AttendanceManager", "Attendance records already exist - skipping initial creation")
                return@withContext
            }


            val todayRecord = JobAttendanceEntity(
                jobId = jobId,
                freelancerId = freelancerId,
                attendanceDate = today,
                checkInTime = null,
                checkOutTime = null,
                progressReport = null
            )
            val todayId = jobAttendanceDao.upsertAttendance(todayRecord)


            val tomorrowRecord = JobAttendanceEntity(
                jobId = jobId,
                freelancerId = freelancerId,
                attendanceDate = tomorrow,
                checkInTime = null,
                checkOutTime = null,
                progressReport = null
            )
            val tomorrowId = jobAttendanceDao.upsertAttendance(tomorrowRecord)

            android.util.Log.d("AttendanceManager", "✅ INITIAL ATTENDANCE CREATED SUCCESSFULLY")
            android.util.Log.d("AttendanceManager", "TODAY: $today (ID: $todayId) - Actions ENABLED")
            android.util.Log.d("AttendanceManager", "TOMORROW: $tomorrow (ID: $tomorrowId) - Actions DISABLED")
            android.util.Log.d("AttendanceManager", "Freelancer can now access attendance tracking!")

        } catch (e: Exception) {
            android.util.Log.e("AttendanceManager", "Error creating initial attendance for new freelancer", e)
        }
    }


    private suspend fun cleanupOldAttendanceRecords() {
        try {
            val calendar = Calendar.getInstance()
            calendar.add(Calendar.DAY_OF_MONTH, -30)
            val cutoffDate = dateFormat.format(calendar.time)


            val oldRecords = jobAttendanceDao.getAttendanceByDateRange("2020-01-01", cutoffDate)

            if (oldRecords.isNotEmpty()) {

                jobAttendanceDao.deleteOldAttendanceRecords(cutoffDate)
                android.util.Log.d("AttendanceManager", "🧹 Cleaned up ${oldRecords.size} old attendance records (older than $cutoffDate)")
            } else {
                android.util.Log.d("AttendanceManager", "No old attendance records to clean up")
            }

        } catch (e: Exception) {
            android.util.Log.e("AttendanceManager", "Error cleaning up old attendance records", e)
        }
    }


    suspend fun getAttendanceSummary(jobId: Int, freelancerId: String): AttendanceSummary = withContext(Dispatchers.IO) {
        try {
            val records = jobAttendanceDao.getAttendanceByJobAndFreelancer(jobId, freelancerId)
            val calendar = Calendar.getInstance()
            val today = dateFormat.format(calendar.time)

            val todayRecord = records.find { it.attendanceDate == today }
            val totalDays = records.count { it.checkInTime != null || it.checkOutTime != null }
            val completedDays = records.count { it.checkInTime != null && it.checkOutTime != null }

            val totalHours = records.sumOf { record ->
                if (record.checkInTime != null && record.checkOutTime != null) {
                    (record.checkOutTime - record.checkInTime) / (1000.0 * 60 * 60)
                } else 0.0
            }

            AttendanceSummary(
                todayCheckedIn = todayRecord?.checkInTime != null,
                todayCheckedOut = todayRecord?.checkOutTime != null,
                totalDays = totalDays,
                completedDays = completedDays,
                totalHours = totalHours,
                averageHours = if (completedDays > 0) totalHours / completedDays else 0.0
            )

        } catch (e: Exception) {
            android.util.Log.e("AttendanceManager", "Error getting attendance summary", e)
            AttendanceSummary()
        }
    }


    suspend fun resetAttendanceDataOnVerificationAccepted(freelancerId: String) = withContext(Dispatchers.IO) {
        try {
            android.util.Log.d("AttendanceManager", "=== RESETTING ATTENDANCE DATA ON VERIFICATION ACCEPTED ===")
            android.util.Log.d("AttendanceManager", "Freelancer ID: $freelancerId")


            val shortlistedApplications = jobApplicationDao.getAllApplications()
                .filter { it.applicantUserId == freelancerId && it.status == "shortlisted" }

            android.util.Log.d("AttendanceManager", "Found ${shortlistedApplications.size} shortlisted jobs for freelancer")

            val calendar = Calendar.getInstance()
            val today = dateFormat.format(calendar.time)
            calendar.add(Calendar.DAY_OF_MONTH, 1)
            val tomorrow = dateFormat.format(calendar.time)

            var totalRecordsReset = 0
            var totalJobsProcessed = 0

            for (application in shortlistedApplications) {
                val jobId = application.jobId


                val existingRecords = jobAttendanceDao.getAttendanceByJobAndFreelancer(jobId, freelancerId)

                if (existingRecords.isNotEmpty()) {

                    jobAttendanceDao.deleteAttendanceByFreelancerAndJob(jobId, freelancerId)

                    android.util.Log.d("AttendanceManager", "🗑️ Deleted ${existingRecords.size} existing attendance records for freelancer $freelancerId, job $jobId")
                    totalRecordsReset += existingRecords.size
                }


                val todayRecord = JobAttendanceEntity(
                    jobId = jobId,
                    freelancerId = freelancerId,
                    attendanceDate = today,
                    checkInTime = null,
                    checkOutTime = null,
                    progressReport = null
                )

                val tomorrowRecord = JobAttendanceEntity(
                    jobId = jobId,
                    freelancerId = freelancerId,
                    attendanceDate = tomorrow,
                    checkInTime = null,
                    checkOutTime = null,
                    progressReport = null
                )

                jobAttendanceDao.upsertAttendance(todayRecord)
                jobAttendanceDao.upsertAttendance(tomorrowRecord)

                android.util.Log.d("AttendanceManager", "✅ Created fresh attendance records for job $jobId - today: $today, tomorrow: $tomorrow")
                totalJobsProcessed++
            }

            android.util.Log.d("AttendanceManager", "=== ATTENDANCE RESET COMPLETE ===")
            android.util.Log.d("AttendanceManager", "Jobs processed: $totalJobsProcessed")
            android.util.Log.d("AttendanceManager", "Records reset: $totalRecordsReset")
            android.util.Log.d("AttendanceManager", "Fresh rolling system established for all jobs")

        } catch (e: Exception) {
            android.util.Log.e("AttendanceManager", "Error resetting attendance data on verification accepted", e)
        }
    }


    suspend fun clearAllCandidateProgressOnFirstAcceptance(candidateId: String, jobId: Int) = withContext(Dispatchers.IO) {
        try {
            android.util.Log.d("AttendanceManager", "=== CLEARING ALL CANDIDATE PROGRESS ON FIRST ACCEPTANCE ===")
            android.util.Log.d("AttendanceManager", "Candidate ID: $candidateId, Job ID: $jobId")


            val allAttendanceRecords = jobAttendanceDao.getAttendanceByFreelancer(candidateId)
            if (allAttendanceRecords.isNotEmpty()) {

                for (record in allAttendanceRecords) {
                    jobAttendanceDao.deleteAttendanceByFreelancerAndJob(record.jobId, candidateId)
                }
                android.util.Log.d("AttendanceManager", "🗑️ Deleted ${allAttendanceRecords.size} existing attendance records for candidate $candidateId across all jobs")
            }


            android.util.Log.d("AttendanceManager", "✅ Cleared all progress reports and historical attendance data")


            android.util.Log.d("AttendanceManager", "✅ Reset all progress metrics - will be recalculated from scratch")

            android.util.Log.d("AttendanceManager", "=== CANDIDATE PROGRESS CLEANUP COMPLETE ===")
            android.util.Log.d("AttendanceManager", "Candidate $candidateId now has a completely clean progress slate")
            android.util.Log.d("AttendanceManager", "New attendance records will be created when they are verified")

        } catch (e: Exception) {
            android.util.Log.e("AttendanceManager", "Error clearing candidate progress on first acceptance", e)
        }
    }


    suspend fun debugVerificationStatus(freelancerId: String, debugContext: String = "") = withContext(Dispatchers.IO) {
        try {
            android.util.Log.d("AttendanceManager", "=== VERIFICATION STATUS DEBUG ===")
            android.util.Log.d("AttendanceManager", "Context: $debugContext")
            android.util.Log.d("AttendanceManager", "Freelancer ID: $freelancerId")


             val database = com.uilover.project196.Database.AppDatabase.getDatabase(context)
             val messageDao = database.messageDao()


            val chatRepository = com.uilover.project196.Repository.ChatRepository.getInstance(context)
            val isVerified = chatRepository.hasAcceptedVerification(freelancerId)
            android.util.Log.d("AttendanceManager", "Has accepted verification: $isVerified")


            val shortlistedApps = jobApplicationDao.getApplicationsByUserId(freelancerId)
                .filter { it.status == "shortlisted" }
            android.util.Log.d("AttendanceManager", "Shortlisted applications: ${shortlistedApps.size}")


            val allMessages = messageDao.getAllMessages()
                .filter { message -> message.senderId == freelancerId && message.messageType == "verification_request" }
            android.util.Log.d("AttendanceManager", "Verification requests sent: ${allMessages.size}")

            allMessages.forEach { message ->
                android.util.Log.d("AttendanceManager", "  - Status: ${message.verificationStatus}, Chat: ${message.chatId}")
            }


            val acceptedVerification = messageDao.getAcceptedVerificationForFreelancer(freelancerId)
            if (acceptedVerification != null) {
                android.util.Log.d("AttendanceManager", "✅ Accepted verification found:")
                android.util.Log.d("AttendanceManager", "  - Chat ID: ${acceptedVerification.chatId}")
                android.util.Log.d("AttendanceManager", "  - Timestamp: ${acceptedVerification.timestamp}")
                android.util.Log.d("AttendanceManager", "  - Status: ${acceptedVerification.verificationStatus}")
            } else {
                android.util.Log.w("AttendanceManager", "❌ No accepted verification found")
            }

            android.util.Log.d("AttendanceManager", "=== END VERIFICATION DEBUG ===")

        } catch (e: Exception) {
            android.util.Log.e("AttendanceManager", "Error in verification status debug", e)
        }
    }

    data class AttendanceSummary(
        val todayCheckedIn: Boolean = false,
        val todayCheckedOut: Boolean = false,
        val totalDays: Int = 0,
        val completedDays: Int = 0,
        val totalHours: Double = 0.0,
        val averageHours: Double = 0.0
    )
}