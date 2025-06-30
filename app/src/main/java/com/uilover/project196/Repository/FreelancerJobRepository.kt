package com.uilover.project196.Repository

import android.content.Context
import com.uilover.project196.Database.AppDatabase
import com.uilover.project196.Model.FreelancerJobModel
import com.uilover.project196.Model.JobAttendanceEntity
import com.uilover.project196.Utils.UserSession
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import java.text.SimpleDateFormat
import java.util.*

// KRITERIA: Repository Pattern untuk manajemen pekerjaan freelancer
// KRITERIA KOMPLEKSITAS: Repository Pattern untuk manajemen pekerjaan freelancer (3/6)
class FreelancerJobRepository private constructor(context: Context) {

    private val database = AppDatabase.getDatabase(context)
    private val jobApplicationDao = database.jobApplicationDao()
    private val jobDao = database.jobDao()
    private val userDao = database.userDao()
    private val jobAttendanceDao = database.jobAttendanceDao()
    private val context = context

    companion object {
        @Volatile
        private var INSTANCE: FreelancerJobRepository? = null

        fun getInstance(context: Context): FreelancerJobRepository {
            return INSTANCE ?: synchronized(this) {
                val instance = FreelancerJobRepository(context.applicationContext)
                INSTANCE = instance
                instance
            }
        }
    }



    suspend fun getVerifiedJobsForFreelancer(freelancerId: String): List<FreelancerJobModel> {
        return withContext(Dispatchers.IO) {
            try {

                val chatRepository = com.uilover.project196.Repository.ChatRepository.getInstance(context)
                val isChatVerified = chatRepository.hasAcceptedVerification(freelancerId)


                val applications = jobApplicationDao.getShortlistedApplicationsByUserId(freelancerId)


                if (applications.isEmpty() && !isChatVerified) {
                    android.util.Log.d("FreelancerJobRepository", "Freelancer $freelancerId has no shortlisted applications AND no chat verification - showing no jobs")
                    return@withContext emptyList()
                }

                android.util.Log.d("FreelancerJobRepository", "Freelancer $freelancerId - Chat verified: $isChatVerified, Shortlisted applications: ${applications.size}")


                if (isChatVerified && applications.isEmpty()) {
                    android.util.Log.d("FreelancerJobRepository", "Freelancer $freelancerId is chat-verified but has no shortlisted applications - showing jobs from verifying business owner")
                    return@withContext getJobsFromVerifyingBusinessOwner(freelancerId)
                }

                val verifiedJobs = mutableListOf<FreelancerJobModel>()


                val jobIds = applications.map { it.jobId }
                android.util.Log.d("FreelancerJobRepository", "Batch loading data for ${jobIds.size} jobs")


                val jobs = jobDao.getJobsByIds(jobIds).associateBy { it.id }


                val ownerIds = jobs.values.mapNotNull { it.ownerId }.distinct()
                val businessOwners = if (ownerIds.isNotEmpty()) {
                    userDao.getUsersByIds(ownerIds).associateBy { it.userId }
                } else {
                    emptyMap()
                }


                val attendanceByJob = jobAttendanceDao.getAttendanceByFreelancer(freelancerId).groupBy { it.jobId }


                val today = SimpleDateFormat("yyyy-MM-dd", Locale.getDefault()).format(Date())
                val currentTime = System.currentTimeMillis()
                val isAttendanceDisabled = context.getSharedPreferences("app_state", android.content.Context.MODE_PRIVATE)
                    .getBoolean("attendance_disabled", false)

                android.util.Log.d("FreelancerJobRepository", "Loaded: ${jobs.size} jobs, ${businessOwners.size} owners, ${attendanceByJob.size} attendance groups")


                for (application in applications) {
                    val job = jobs[application.jobId] ?: continue
                    val ownerId = job.ownerId ?: continue
                    val businessOwner = businessOwners[ownerId] ?: continue


                    val jobAttendanceRecords = attendanceByJob[job.id] ?: emptyList()
                    val todayAttendance = jobAttendanceRecords.find { it.attendanceDate == today }


                    val isFrozenByThisCompany = isFreelancerFrozenByCompany(freelancerId, ownerId)
                    val frozenByCompanyName = if (isFrozenByThisCompany) {
                        businessOwner.companyName.ifEmpty { "Company" }
                    } else null


                    val daysSinceApplication = (currentTime - application.appliedAt) / (1000 * 60 * 60 * 24)
                    val hasAttendanceRecords = jobAttendanceRecords.isNotEmpty()

                    val isJobActive = when {
                        job.status == "closed" -> false
                        isFrozenByThisCompany -> false
                        application.status == "frozen" -> false
                        isAttendanceDisabled -> false
                        daysSinceApplication >= 7 && hasAttendanceRecords -> true
                        todayAttendance != null -> true
                        daysSinceApplication < 3 -> false
                        else -> (job.id % 3) != 0
                    }

                    val freelancerJob = FreelancerJobModel(
                        id = job.id,
                        title = job.title,
                        companyName = businessOwner.companyName.ifEmpty { "Company" },
                        companyLogo = "logo1",
                        location = job.location,
                        startDate = application.appliedAt,
                        endDate = null,
                        isActive = isJobActive,
                        lastCheckIn = todayAttendance?.checkInTime,
                        lastCheckOut = todayAttendance?.checkOutTime,
                        applicationId = application.id,
                        businessOwnerId = ownerId,
                        isFrozenByCompany = isFrozenByThisCompany,
                        frozenByCompanyName = frozenByCompanyName,
                        jobStatus = job.status
                    )

                    verifiedJobs.add(freelancerJob)
                    android.util.Log.d("FreelancerJobRepository", "✅ Added job ${job.title} to verified jobs list")
                }

                android.util.Log.d("FreelancerJobRepository", "=== FINAL RESULT ===")
                android.util.Log.d("FreelancerJobRepository", "Total verified jobs for freelancer $freelancerId: ${verifiedJobs.size}")
                verifiedJobs.forEachIndexed { index, job ->
                    android.util.Log.d("FreelancerJobRepository", "Job ${index + 1}: ${job.title} (${job.companyName})")
                }

                verifiedJobs.sortedByDescending { it.startDate }

            } catch (e: Exception) {
                android.util.Log.e("FreelancerJobRepository", "Error getting verified jobs", e)
                emptyList()
            }
        }
    }


    private suspend fun getJobsFromVerifyingBusinessOwner(freelancerId: String): List<FreelancerJobModel> {
        return withContext(Dispatchers.IO) {
            try {
                android.util.Log.d("FreelancerJobRepository", "Getting jobs from verifying business owner for freelancer $freelancerId")


                val messageDao = database.messageDao()
                val acceptedVerification = messageDao.getAcceptedVerificationForFreelancer(freelancerId)

                if (acceptedVerification == null) {
                    android.util.Log.w("FreelancerJobRepository", "No accepted verification found for freelancer $freelancerId")
                    return@withContext emptyList()
                }


                val chatId = acceptedVerification.chatId
                val messagesInChat = messageDao.getMessagesForChat(chatId)


                val businessOwnerId = messagesInChat
                    .map { it.senderId }
                    .distinct()
                    .find { it != freelancerId }

                if (businessOwnerId == null) {
                    android.util.Log.w("FreelancerJobRepository", "Could not find business owner in verification chat $chatId")
                    return@withContext emptyList()
                }

                android.util.Log.d("FreelancerJobRepository", "Found verifying business owner: $businessOwnerId")


                val businessOwner = userDao.getUserById(businessOwnerId)
                if (businessOwner == null) {
                    android.util.Log.w("FreelancerJobRepository", "Business owner $businessOwnerId not found in database")
                    return@withContext emptyList()
                }


                val allJobsFromOwner = jobDao.getJobsByOwnerId(businessOwnerId)
                    .filter { it.status == "open" }

                if (allJobsFromOwner.isEmpty()) {
                    android.util.Log.d("FreelancerJobRepository", "No open jobs found for verifying business owner")
                    return@withContext emptyList()
                }

                android.util.Log.d("FreelancerJobRepository", "Found ${allJobsFromOwner.size} open jobs from verifying business owner ${businessOwner.name}")


                val today = SimpleDateFormat("yyyy-MM-dd", Locale.getDefault()).format(Date())
                val verificationTime = acceptedVerification.timestamp
                val currentTime = System.currentTimeMillis()
                val attendanceByJob = jobAttendanceDao.getAttendanceByFreelancer(freelancerId).groupBy { it.jobId }
                val isFrozenByThisCompany = isFreelancerFrozenByCompany(freelancerId, businessOwnerId)
                val frozenByCompanyName = if (isFrozenByThisCompany) {
                    businessOwner.companyName.ifEmpty { "Company" }
                } else null
                val isAttendanceDisabled = context.getSharedPreferences("app_state", android.content.Context.MODE_PRIVATE)
                    .getBoolean("attendance_disabled", false)


                val verifiedJobs = allJobsFromOwner.asSequence().map { job ->
                    val jobAttendanceRecords = attendanceByJob[job.id] ?: emptyList()
                    val todayAttendance = jobAttendanceRecords.find { it.attendanceDate == today }
                    val daysSinceVerification = (currentTime - verificationTime) / (1000 * 60 * 60 * 24)
                    val hasAttendanceRecords = jobAttendanceRecords.isNotEmpty()

                    val isJobActive = when {
                        job.status == "closed" -> false
                        isFrozenByThisCompany -> false
                        isAttendanceDisabled -> false
                        daysSinceVerification >= 7 && hasAttendanceRecords -> true
                        todayAttendance != null -> true
                        daysSinceVerification < 1 -> true
                        else -> (job.id % 3) != 0
                    }

                    FreelancerJobModel(
                        id = job.id,
                        title = job.title,
                        companyName = businessOwner.companyName.ifEmpty { "Company" },
                        companyLogo = "logo1",
                        location = job.location,
                        startDate = verificationTime,
                        endDate = null,
                        isActive = isJobActive,
                        lastCheckIn = todayAttendance?.checkInTime,
                        lastCheckOut = todayAttendance?.checkOutTime,
                        applicationId = 0,
                        businessOwnerId = businessOwnerId,
                        isFrozenByCompany = isFrozenByThisCompany,
                        frozenByCompanyName = frozenByCompanyName,
                        jobStatus = job.status
                    )
                }.sortedByDescending { it.startDate }.toList()

                android.util.Log.d("FreelancerJobRepository", "=== VERIFICATION-BASED JOBS RESULT ===")
                android.util.Log.d("FreelancerJobRepository", "Total jobs from verifying business owner: ${verifiedJobs.size}")

                verifiedJobs

            } catch (e: Exception) {
                android.util.Log.e("FreelancerJobRepository", "Error getting jobs from verifying business owner", e)
                emptyList()
            }
        }
    }


    private fun isAttendanceDisabled(): Boolean {
        val prefs = context.getSharedPreferences("app_state", Context.MODE_PRIVATE)
        return prefs.getBoolean("attendance_disabled", false)
    }


    private fun isFreelancerFrozenByCompany(freelancerId: String, businessOwnerId: String): Boolean {
        return try {
            val prefs = context.getSharedPreferences("user_status", android.content.Context.MODE_PRIVATE)
            val isFrozen = prefs.getBoolean("is_frozen_by_${businessOwnerId}_$freelancerId", false)
            val frozenCompany = prefs.getString("frozen_by_company_$freelancerId", "")

            android.util.Log.d("FreelancerJobRepository", "Checking if freelancer $freelancerId is frozen by company $businessOwnerId: $isFrozen (Company: $frozenCompany)")
            isFrozen
        } catch (e: Exception) {
            android.util.Log.e("FreelancerJobRepository", "Error checking if freelancer is frozen by company", e)
            false
        }
    }


    private fun getCompanyThatFrozeFreelancer(freelancerId: String): String? {
        return try {
            val prefs = context.getSharedPreferences("user_status", android.content.Context.MODE_PRIVATE)
            prefs.getString("frozen_by_company_$freelancerId", null)
        } catch (e: Exception) {
            null
        }
    }


    private suspend fun isFreelancerBlocked(freelancerId: String): Boolean {
        return withContext(Dispatchers.IO) {
            try {

                val user = userDao.getUserById(freelancerId)
                val isGloballyInactive = user?.isActive == false


                val prefs = context.getSharedPreferences("user_status", android.content.Context.MODE_PRIVATE)
                val isCacheBlocked = prefs.getBoolean("is_blocked_$freelancerId", false)

                val isBlocked = isGloballyInactive || isCacheBlocked
                android.util.Log.d("FreelancerJobRepository", "Global block check for $freelancerId: globally_inactive=$isGloballyInactive, cache_blocked=$isCacheBlocked, final=$isBlocked")
                isBlocked
            } catch (e: Exception) {
                android.util.Log.e("FreelancerJobRepository", "Error checking if freelancer is blocked", e)
                false
            }
        }
    }


    suspend fun checkInToJob(jobId: Int, freelancerId: String): Boolean {
        android.util.Log.d("FreelancerJobRepository", "=== CHECK-IN VALIDATION START ===")
        android.util.Log.d("FreelancerJobRepository", "JobId: $jobId, FreelancerId: $freelancerId")


        val chatRepository = com.uilover.project196.Repository.ChatRepository.getInstance(context)
        val isVerified = chatRepository.hasAcceptedVerification(freelancerId)

        if (!isVerified) {
            android.util.Log.w("FreelancerJobRepository", "❌ CHECK-IN BLOCKED: Freelancer $freelancerId has no accepted verification")
            android.util.Log.w("FreelancerJobRepository", "Freelancer must send verification request and get it accepted by business owner first")
            return false
        }

        android.util.Log.d("FreelancerJobRepository", "✅ Verification check passed for freelancer $freelancerId")

        if (isAttendanceDisabled()) {
            android.util.Log.w("FreelancerJobRepository", "❌ CHECK-IN BLOCKED: Attendance functionality is disabled after app restart")
            return false
        }


        val job = jobDao.getJobById(jobId)
        if (job == null) {
            android.util.Log.w("FreelancerJobRepository", "❌ CHECK-IN BLOCKED: Job not found")
            return false
        }


        if (job.status == "closed") {
            android.util.Log.w("FreelancerJobRepository", "❌ CHECK-IN BLOCKED: Job has been closed by business owner")
            return false
        }


        val businessOwnerId = job.ownerId ?: ""
        if (isFreelancerFrozenByCompany(freelancerId, businessOwnerId)) {
            val frozenCompany = getCompanyThatFrozeFreelancer(freelancerId) ?: "this company"
            android.util.Log.w("FreelancerJobRepository", "❌ CHECK-IN BLOCKED: Freelancer is frozen by $frozenCompany")
            return false
        }


        if (isFreelancerBlocked(freelancerId)) {
            android.util.Log.w("FreelancerJobRepository", "❌ CHECK-IN BLOCKED: Freelancer is globally blocked/inactive")
            return false
        }

        android.util.Log.d("FreelancerJobRepository", "✅ All validation checks passed, proceeding with check-in")

        return withContext(Dispatchers.IO) {
            try {
                val today = SimpleDateFormat("yyyy-MM-dd", Locale.getDefault()).format(Date())
                val currentTime = System.currentTimeMillis()


                val existingAttendance = jobAttendanceDao.getTodayAttendance(jobId, freelancerId, today)

                if (existingAttendance != null) {

                    val updatedAttendance = existingAttendance.copy(
                        checkInTime = currentTime,
                        updatedAt = currentTime
                    )
                    jobAttendanceDao.upsertAttendance(updatedAttendance)
                } else {

                    val attendance = JobAttendanceEntity(
                        jobId = jobId,
                        freelancerId = freelancerId,
                        attendanceDate = today,
                        checkInTime = currentTime
                    )
                    jobAttendanceDao.upsertAttendance(attendance)
                }

                android.util.Log.d("FreelancerJobRepository", "✅ Check in successful for job $jobId, freelancer $freelancerId")
                true

            } catch (e: Exception) {
                android.util.Log.e("FreelancerJobRepository", "❌ Error checking in", e)
                false
            }
        }
    }


    suspend fun checkOutFromJob(jobId: Int, freelancerId: String, progressReport: String): Boolean {
        android.util.Log.d("FreelancerJobRepository", "=== CHECK-OUT VALIDATION START ===")
        android.util.Log.d("FreelancerJobRepository", "JobId: $jobId, FreelancerId: $freelancerId")


        val chatRepository = com.uilover.project196.Repository.ChatRepository.getInstance(context)
        val isVerified = chatRepository.hasAcceptedVerification(freelancerId)

        if (!isVerified) {
            android.util.Log.w("FreelancerJobRepository", "❌ CHECK-OUT BLOCKED: Freelancer $freelancerId has no accepted verification")
            android.util.Log.w("FreelancerJobRepository", "Freelancer must send verification request and get it accepted by business owner first")
            return false
        }

        android.util.Log.d("FreelancerJobRepository", "✅ Verification check passed for freelancer $freelancerId")

        if (isAttendanceDisabled()) {
            android.util.Log.w("FreelancerJobRepository", "❌ CHECK-OUT BLOCKED: Attendance functionality is disabled after app restart")
            return false
        }


        val job = jobDao.getJobById(jobId)
        if (job == null) {
            android.util.Log.w("FreelancerJobRepository", "❌ CHECK-OUT BLOCKED: Job not found")
            return false
        }


        if (job.status == "closed") {
            android.util.Log.w("FreelancerJobRepository", "❌ CHECK-OUT BLOCKED: Job has been closed by business owner")
            return false
        }


        val businessOwnerId = job.ownerId ?: ""
        if (isFreelancerFrozenByCompany(freelancerId, businessOwnerId)) {
            val frozenCompany = getCompanyThatFrozeFreelancer(freelancerId) ?: "this company"
            android.util.Log.w("FreelancerJobRepository", "❌ CHECK-OUT BLOCKED: Freelancer is frozen by $frozenCompany")
            return false
        }


        if (isFreelancerBlocked(freelancerId)) {
            android.util.Log.w("FreelancerJobRepository", "❌ CHECK-OUT BLOCKED: Freelancer is globally blocked/inactive")
            return false
        }

        android.util.Log.d("FreelancerJobRepository", "✅ All validation checks passed, proceeding with check-out")

        return withContext(Dispatchers.IO) {
            try {
                val today = SimpleDateFormat("yyyy-MM-dd", Locale.getDefault()).format(Date())
                val currentTime = System.currentTimeMillis()


                val existingAttendance = jobAttendanceDao.getTodayAttendance(jobId, freelancerId, today)

                if (existingAttendance != null) {

                    val updatedAttendance = existingAttendance.copy(
                        checkOutTime = currentTime,
                        progressReport = progressReport,
                        updatedAt = currentTime
                    )
                    jobAttendanceDao.upsertAttendance(updatedAttendance)

                    android.util.Log.d("FreelancerJobRepository", "✅ Check out successful for job $jobId, freelancer $freelancerId")
                    true
                } else {
                    android.util.Log.w("FreelancerJobRepository", "❌ No check in record found for today")
                    false
                }

            } catch (e: Exception) {
                android.util.Log.e("FreelancerJobRepository", "❌ Error checking out", e)
                false
            }
        }
    }


    suspend fun getTodayAttendanceStatus(jobId: Int, freelancerId: String): Pair<Boolean, Boolean> {
        return withContext(Dispatchers.IO) {
            try {
                val today = SimpleDateFormat("yyyy-MM-dd", Locale.getDefault()).format(Date())
                val attendance = jobAttendanceDao.getTodayAttendance(jobId, freelancerId, today)

                val isCheckedIn = attendance?.checkInTime != null
                val isCheckedOut = attendance?.checkOutTime != null

                Pair(isCheckedIn, isCheckedOut)

            } catch (e: Exception) {
                android.util.Log.e("FreelancerJobRepository", "Error getting attendance status", e)
                Pair(false, false)
            }
        }
    }


    suspend fun getAttendanceHistory(jobId: Int, freelancerId: String): List<JobAttendanceEntity> {
        return withContext(Dispatchers.IO) {
            try {
                jobAttendanceDao.getAttendanceByJobAndFreelancer(jobId, freelancerId)
            } catch (e: Exception) {
                android.util.Log.e("FreelancerJobRepository", "Error getting attendance history", e)
                emptyList()
            }
        }
    }
}