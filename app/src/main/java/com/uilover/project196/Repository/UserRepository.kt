package com.uilover.project196.Repository

import android.content.Context
import androidx.lifecycle.LiveData
import androidx.lifecycle.MutableLiveData
import com.uilover.project196.Database.AppDatabase
import com.uilover.project196.Model.*
import com.uilover.project196.Utils.UserSession
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.GlobalScope
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext

// KRITERIA KOMPLEKSITAS: Repository Pattern untuk manajemen pengguna (4/6)
class UserRepository private constructor(private val context: Context) {

    private val database = AppDatabase.getDatabase(context)
    private val userDao = database.userDao()
    private val jobApplicationDao = database.jobApplicationDao()
    private val jobViewDao = database.jobViewDao()
    private val jobDao = database.jobDao()

    private val _candidates = MutableLiveData<List<CandidateModel>>()
    val candidates: LiveData<List<CandidateModel>> = _candidates

    companion object {
        @Volatile
        private var INSTANCE: UserRepository? = null

        fun getInstance(context: Context): UserRepository {
            return INSTANCE ?: synchronized(this) {
                val instance = UserRepository(context.applicationContext)
                INSTANCE = instance
                instance
            }
        }
    }

    private var isInitialized = false


    suspend fun initializeSampleData() {
        withContext(Dispatchers.IO) {
            val hasData = userDao.getUserCount() > 0
            if (hasData) {
                android.util.Log.d("UserRepository", "initializeSampleData: Already initialized, skipping")

                ensureAllBusinessOwnersExist()
                return@withContext
            }

            android.util.Log.d("UserRepository", "initializeSampleData: Initializing sample data...")
            android.util.Log.d("UserRepository", "initializeSampleData: All DAO tables have been reset")
            android.util.Log.d("UserRepository", "initializeSampleData: No sample users will be created")
            android.util.Log.d("UserRepository", "initializeSampleData: No sample applications will be created")
            android.util.Log.d("UserRepository", "initializeSampleData: Database is ready for real user registrations and applications")


            ensureAllBusinessOwnersExist()

            android.util.Log.d("UserRepository", "initializeSampleData: ✅ Clean state initialization complete")
            isInitialized = true
        }
    }


    private suspend fun ensureAllBusinessOwnersExist() {
        android.util.Log.d("UserRepository", "Ensuring all business owner accounts exist...")

        try {

            ensureUserInDatabase("user_002", "Sarah Johnson", "sarah.johnson@chaboksoft.com", UserSession.ROLE_BUSINESS_OWNER)


            ensureUserInDatabase("user_003", "Alex Kim", "alex.kim@kiansoft.com", UserSession.ROLE_BUSINESS_OWNER)


            ensureUserInDatabase("user_004", "Maria Rodriguez", "maria.rodriguez@makansoft.com", UserSession.ROLE_BUSINESS_OWNER)


            ensureUserInDatabase("user_005", "David Chen", "david.chen@testsoft.com", UserSession.ROLE_BUSINESS_OWNER)

            android.util.Log.d("UserRepository", "✅ All business owner accounts ensured")

        } catch (e: Exception) {
            android.util.Log.e("UserRepository", "Error ensuring business owner accounts", e)
        }
    }





    suspend fun getCandidatesForJob(jobId: Int): List<CandidateModel> {
        return withContext(Dispatchers.IO) {
            android.util.Log.d("UserRepository", "getCandidatesForJob: Loading applications for job ID: $jobId")
            val applications = jobApplicationDao.getApplicationsForJob(jobId)
            android.util.Log.d("UserRepository", "getCandidatesForJob: Found ${applications.size} applications")

            val candidates = mutableListOf<CandidateModel>()

            for (application in applications) {
                android.util.Log.d("UserRepository", "getCandidatesForJob: Processing application ID ${application.id} from user ${application.applicantUserId}")

                val user = userDao.getUserById(application.applicantUserId)
                if (user != null && user.isActive) {
                    android.util.Log.d("UserRepository", "getCandidatesForJob: ✅ REAL USER FOUND:")
                    android.util.Log.d("UserRepository", "  - Database User ID: ${user.userId}")
                    android.util.Log.d("UserRepository", "  - Database User Name: ${user.name}")
                    android.util.Log.d("UserRepository", "  - Database User Email: ${user.email}")
                    android.util.Log.d("UserRepository", "  - Database User Role: ${user.role}")
                    android.util.Log.d("UserRepository", "  - Application Skills: ${application.skills}")
                    android.util.Log.d("UserRepository", "  - Application Cover Letter: ${application.coverLetter.take(50)}...")
                    android.util.Log.d("UserRepository", "  - Application Date: ${java.util.Date(application.appliedAt)}")

                    val candidateModel = CandidateModel.fromUserAndApplication(user, application)
                    candidates.add(candidateModel)

                    android.util.Log.d("UserRepository", "getCandidatesForJob: ✅ CandidateModel created with REAL data binding")
                } else {
                    android.util.Log.w("UserRepository", "getCandidatesForJob: ❌ User ${application.applicantUserId} not found or inactive")
                }
            }

            candidates
        }
    }


    suspend fun getCandidatesForOwner(ownerId: String): List<CandidateModel> {
        return withContext(Dispatchers.IO) {
            android.util.Log.d("UserRepository", "getCandidatesForOwner: Loading candidates for business owner: $ownerId")


            val ownerJobs = jobDao.getJobsByOwner(ownerId)
            if (ownerJobs.isEmpty()) {
                android.util.Log.d("UserRepository", "No jobs found for owner $ownerId")
                return@withContext emptyList()
            }


            val jobIds = ownerJobs.map { it.id }
            val allApplications = jobApplicationDao.getApplicationsByJobIds(jobIds)

            if (allApplications.isEmpty()) {
                android.util.Log.d("UserRepository", "No applications found for owner $ownerId jobs")
                return@withContext emptyList()
            }


            val userIds = allApplications.map { it.applicantUserId }.distinct()
            val users = userDao.getUsersByIds(userIds).associateBy { it.userId }


            val allCandidates = allApplications.mapNotNull { application ->
                val user = users[application.applicantUserId]
                if (user != null && user.isActive) {
                    CandidateModel.fromUserAndApplication(user, application)
                } else {
                    null
                }
            }

            android.util.Log.d("UserRepository", "getCandidatesForOwner: Found ${allCandidates.size} candidates across ${ownerJobs.size} jobs")
            allCandidates.sortedByDescending { it.appliedDate }
        }
    }


    suspend fun applyForJob(jobId: Int, userId: String, coverLetter: String = "", proposedRate: String = "", skills: String = "", description: String = ""): Boolean {
        return withContext(Dispatchers.IO) {
            try {
                android.util.Log.d("UserRepository", "=== FREELANCER JOB APPLICATION ===")
                android.util.Log.d("UserRepository", "Freelancer ID: $userId applying to Job ID: $jobId")


                val existingApplication = jobApplicationDao.getApplicationByJobAndUser(jobId, userId)
                if (existingApplication != null) {
                    android.util.Log.d("UserRepository", "❌ User already applied to this job")
                    return@withContext false
                }


                android.util.Log.d("UserRepository", "🗑️🔥 === COMPLETE PROGRESS RESET FOR FREELANCER $userId ===")
                android.util.Log.d("UserRepository", "Deleting ALL existing progress references and candidate data for fresh start")


                val jobAttendanceDao = AppDatabase.getDatabase(context).jobAttendanceDao()
                val allAttendanceRecords = jobAttendanceDao.getAttendanceByFreelancer(userId)

                if (allAttendanceRecords.isNotEmpty()) {
                    android.util.Log.d("UserRepository", "🗑️ Found ${allAttendanceRecords.size} existing attendance records to DELETE")


                    var deletedRecords = 0
                    for (record in allAttendanceRecords) {
                        jobAttendanceDao.deleteAttendanceByFreelancerAndJob(record.jobId, userId)
                        deletedRecords++
                        android.util.Log.d("UserRepository", "  ✅ Deleted attendance record for Job ${record.jobId} on ${record.attendanceDate}")
                    }
                    android.util.Log.d("UserRepository", "🗑️ DELETED ${deletedRecords}/${allAttendanceRecords.size} attendance records for freelancer $userId")
                } else {
                    android.util.Log.d("UserRepository", "✅ No existing attendance records found - freelancer has clean slate")
                }


                android.util.Log.d("UserRepository", "🗑️ Clearing ALL cached progress data and status flags...")
                val userStatusPrefs = context.getSharedPreferences("user_status", android.content.Context.MODE_PRIVATE)
                val progressPrefs = context.getSharedPreferences("candidates_progress", android.content.Context.MODE_PRIVATE)


                userStatusPrefs.edit()
                    .remove("is_blocked_$userId")
                    .remove("block_reason_$userId")
                    .remove("frozen_status_$userId")
                    .remove("last_checked_$userId")
                    .remove("verification_status_$userId")
                    .apply()


                progressPrefs.edit()
                    .remove("progress_$userId")
                    .remove("last_checkin_$userId")
                    .remove("last_checkout_$userId")
                    .remove("total_hours_$userId")
                    .remove("attendance_summary_$userId")
                    .apply()

                android.util.Log.d("UserRepository", "✅ ALL cached progress data cleared for freelancer $userId")


                try {
                    val chatRepository = com.uilover.project196.Repository.ChatRepository.getInstance(context)
                    chatRepository.resetVerificationStatusForFreelancer(userId)
                    android.util.Log.d("UserRepository", "✅ Verification status reset - freelancer must verify again")
                } catch (e: Exception) {
                    android.util.Log.w("UserRepository", "Could not reset verification status: ${e.message}")
                }

                android.util.Log.d("UserRepository", "🔥✅ === COMPLETE PROGRESS RESET FINISHED ===")
                android.util.Log.d("UserRepository", "Freelancer $userId now has 100% clean slate - no previous progress references")


                try {
                    val intent = android.content.Intent("com.uilover.project196.FREELANCER_PROGRESS_RESET")
                    intent.putExtra("freelancer_id", userId)
                    intent.putExtra("job_id", jobId)
                    intent.putExtra("reset_type", "COMPLETE_RESET_ON_APPLICATION")
                    context.sendBroadcast(intent)
                    android.util.Log.d("UserRepository", "📡 Sent broadcast to refresh UI after complete progress reset")
                } catch (e: Exception) {
                    android.util.Log.w("UserRepository", "Failed to send progress reset broadcast: ${e.message}")
                }


                val application = JobApplicationEntity(
                    jobId = jobId,
                    applicantUserId = userId,
                    status = "pending",
                    appliedAt = System.currentTimeMillis(),
                    coverLetter = coverLetter,
                    proposedRate = proposedRate,
                    skills = skills,
                    description = description
                )

                jobApplicationDao.insertApplication(application)
                android.util.Log.d("UserRepository", "✅ Created new job application for freelancer $userId to job $jobId")


                try {
                    val intent = android.content.Intent("com.uilover.project196.NEW_CANDIDATE_REGISTERED")
                    intent.putExtra("freelancer_id", userId)
                    intent.putExtra("job_id", jobId)
                    intent.putExtra("application_id", application.id)
                    context.sendBroadcast(intent)
                    android.util.Log.d("UserRepository", "✅ Sent broadcast to refresh candidates list for business owners")
                } catch (e: Exception) {
                    android.util.Log.w("UserRepository", "Failed to send broadcast, but application was successful", e)
                }


                android.util.Log.d("UserRepository", "✅ Freelancer $userId is now registered in candidates list for job $jobId")
                android.util.Log.d("UserRepository", "Business owner can now see this application in their candidates section")

                android.util.Log.d("UserRepository", "=== JOB APPLICATION COMPLETE ===")
                android.util.Log.d("UserRepository", "✅ Clean slate established for freelancer $userId")
                android.util.Log.d("UserRepository", "✅ Freelancer registered in business owner's candidates list")

                true
            } catch (e: Exception) {
                android.util.Log.e("UserRepository", "❌ Error in job application process", e)
                false
            }
        }
    }


    suspend fun trackJobView(jobId: Int, viewerUserId: String, sessionId: String = "") {
        withContext(Dispatchers.IO) {
            try {
                android.util.Log.d("UserRepository", "=== UserRepository.trackJobView CALLED ===")
                android.util.Log.d("UserRepository", "trackJobView: jobId=$jobId, viewerUserId=$viewerUserId, sessionId=$sessionId")
                android.util.Log.d("UserRepository", "trackJobView: Expected freelancer role constant=${UserSession.ROLE_FREELANCER}")


                android.util.Log.d("UserRepository", "trackJobView: Looking up user in database...")
                val user = userDao.getUserById(viewerUserId)
                android.util.Log.d("UserRepository", "trackJobView: user found=${user != null}")

                if (user != null) {
                    android.util.Log.d("UserRepository", "trackJobView: User details - userId=${user.userId}, name=${user.name}, email=${user.email}, role=${user.role}, isActive=${user.isActive}")
                } else {
                    android.util.Log.e("UserRepository", "trackJobView: ❌ User not found in database, userId=$viewerUserId")
                    android.util.Log.e("UserRepository", "trackJobView: This means ensureUserInDatabase failed or user wasn't created properly")
                    return@withContext
                }

                if (user.role != UserSession.ROLE_FREELANCER) {
                    android.util.Log.e("UserRepository", "trackJobView: ❌ User is not a freelancer, role=${user.role}, expected=${UserSession.ROLE_FREELANCER}")
                    return@withContext
                }

                if (!user.isActive) {
                    android.util.Log.e("UserRepository", "trackJobView: ❌ User is not active, isActive=${user.isActive}")
                    return@withContext
                }

                android.util.Log.d("UserRepository", "trackJobView: ✅ User validation passed - proceeding with view tracking")


                val oneDayAgo = System.currentTimeMillis() - (24 * 60 * 60 * 1000)
                android.util.Log.d("UserRepository", "trackJobView: Checking for recent views within 24h...")
                val recentViews = jobViewDao.hasUserViewedJobRecently(jobId, viewerUserId, oneDayAgo)
                android.util.Log.d("UserRepository", "trackJobView: Recent views (24h) for user $viewerUserId on job $jobId = $recentViews")

                if (recentViews > 0) {
                    android.util.Log.d("UserRepository", "trackJobView: ⚠️ User has viewed this job recently (within 24h), skipping duplicate tracking")
                    return@withContext
                }

                android.util.Log.d("UserRepository", "trackJobView: ✅ No recent views found, proceeding to track new view")


                val view = JobViewEntity(
                    jobId = jobId,
                    viewerUserId = viewerUserId,
                    viewedAt = System.currentTimeMillis(),
                    sessionId = sessionId,
                    source = "detail_view"
                )

                android.util.Log.d("UserRepository", "trackJobView: Inserting view entity into database...")
                jobViewDao.insertView(view)
                android.util.Log.d("UserRepository", "trackJobView: ✅ View entity inserted successfully")


                android.util.Log.d("UserRepository", "trackJobView: Verifying view was inserted...")
                val totalViewsAfter = jobViewDao.hasUserEverViewedJob(jobId, viewerUserId)
                android.util.Log.d("UserRepository", "trackJobView: Total views for user $viewerUserId on job $jobId after tracking = $totalViewsAfter")

                android.util.Log.d("UserRepository", "=== ✅ VIEW TRACKING COMPLETED SUCCESSFULLY ===")

            } catch (e: Exception) {
                android.util.Log.e("UserRepository", "=== ❌ EXCEPTION in trackJobView ===", e)

            }
        }
    }


    suspend fun getJobAnalytics(ownerId: String): JobAnalytics {
        return withContext(Dispatchers.IO) {
            val totalViews = jobViewDao.getTotalViewsForOwner(ownerId)


            val ownerJobs = jobDao.getJobsByOwner(ownerId)
            var totalJobApplications = 0
            for (job in ownerJobs) {
                totalJobApplications += jobApplicationDao.getApplicationCountForJob(job.id)
            }

            JobAnalytics(
                totalViews = totalViews,
                totalApplications = totalJobApplications,
                uniqueViewers = 0,
                conversionRate = if (totalViews > 0) (totalJobApplications.toFloat() / totalViews) * 100 else 0f
            )
        }
    }


    suspend fun getJobSpecificAnalytics(jobModel: com.uilover.project196.Model.JobModel): JobSpecificAnalytics {
        return withContext(Dispatchers.IO) {
            try {

                val jobEntity = findJobEntity(jobModel)

                if (jobEntity != null) {

                    val uniqueViews = jobViewDao.getUniqueViewersForJob(jobEntity.id)


                    val applications = jobApplicationDao.getApplicationsByJobId(jobEntity.id)
                    val acceptedFreelancers = applications.count {
                        it.status == "shortlisted" || it.status == "hired"
                    }

                    JobSpecificAnalytics(
                        uniqueViews = uniqueViews,
                        acceptedFreelancers = acceptedFreelancers
                    )
                } else {
                    JobSpecificAnalytics(0, 0)
                }
            } catch (e: Exception) {
                JobSpecificAnalytics(0, 0)
            }
        }
    }


    suspend fun getFreelancerViewsForJob(jobModel: com.uilover.project196.Model.JobModel): Int {
        return withContext(Dispatchers.IO) {
            try {
                android.util.Log.d("UserRepository", "getFreelancerViewsForJob: Looking for job: ${jobModel.title} at ${jobModel.company}")


                val jobEntity = findJobEntity(jobModel)
                android.util.Log.d("UserRepository", "getFreelancerViewsForJob: jobEntity found=${jobEntity != null}, id=${jobEntity?.id}")

                if (jobEntity != null) {

                    val jobViews = jobViewDao.getViewsForJob(jobEntity.id)
                    android.util.Log.d("UserRepository", "getFreelancerViewsForJob: Total views found=${jobViews.size}")


                    var totalFreelancerViews = 0

                    for (view in jobViews) {
                        val user = userDao.getUserById(view.viewerUserId)
                        android.util.Log.d("UserRepository", "getFreelancerViewsForJob: Checking viewer ${view.viewerUserId}, user found=${user != null}, role=${user?.role}")
                        if (user != null && user.role == "freelancer" && user.isActive) {
                            totalFreelancerViews++
                        }
                    }

                    android.util.Log.d("UserRepository", "getFreelancerViewsForJob: Total freelancer views=$totalFreelancerViews")
                    totalFreelancerViews
                } else {
                    android.util.Log.e("UserRepository", "getFreelancerViewsForJob: Job entity not found in database")
                    0
                }
            } catch (e: Exception) {
                android.util.Log.e("UserRepository", "getFreelancerViewsForJob: Error", e)
                0
            }
        }
    }


    suspend fun hasUserEverViewedJob(jobId: Int, userId: String): Int {
        return withContext(Dispatchers.IO) {
            try {
                jobViewDao.hasUserEverViewedJob(jobId, userId)
            } catch (e: Exception) {
                0
            }
        }
    }


    suspend fun hasUserAppliedToJob(jobId: Int, userId: String): Boolean {
        return withContext(Dispatchers.IO) {
            try {
                val existingApplication = jobApplicationDao.getApplicationByJobAndUser(jobId, userId)
                existingApplication != null
            } catch (e: Exception) {
                false
            }
        }
    }

    // New method to get application status
    suspend fun getUserApplicationStatus(jobId: Int, userId: String): String? {
        return withContext(Dispatchers.IO) {
            try {
                val existingApplication = jobApplicationDao.getApplicationByJobAndUser(jobId, userId)
                existingApplication?.status
            } catch (e: Exception) {
                null
            }
        }
    }


    private suspend fun findJobEntity(jobModel: com.uilover.project196.Model.JobModel): com.uilover.project196.Model.JobEntity? {
        return withContext(Dispatchers.IO) {
            try {
                android.util.Log.d("UserRepository", "findJobEntity: Searching for job: title='${jobModel.title}', company='${jobModel.company}', ownerId='${jobModel.ownerId}'")


                val jobEntity = jobDao.getJobByDetails(jobModel.title, jobModel.company, jobModel.ownerId)

                android.util.Log.d("UserRepository", "findJobEntity: Result - jobEntity found=${jobEntity != null}, id=${jobEntity?.id}")

                jobEntity
            } catch (e: Exception) {
                android.util.Log.e("UserRepository", "findJobEntity: Error finding job", e)
                null
            }
        }
    }


    suspend fun getActiveFreelancers(): List<UserEntity> {
        return withContext(Dispatchers.IO) {
            userDao.getActiveFreelancers()
        }
    }


    suspend fun getUserById(userId: String): UserEntity? {
        return withContext(Dispatchers.IO) {
            try {
                userDao.getUserById(userId)
            } catch (e: Exception) {
                android.util.Log.e("UserRepository", "getUserById: Error getting user $userId", e)
                null
            }
        }
    }


    suspend fun updateApplicationStatus(applicationId: Int, status: String): Boolean {
        return withContext(Dispatchers.IO) {
            try {
                jobApplicationDao.updateApplicationStatus(applicationId, status)
                true
            } catch (e: Exception) {
                false
            }
        }
    }


    suspend fun clearAllCandidates(): Boolean {
        return withContext(Dispatchers.IO) {
            try {
                android.util.Log.d("UserRepository", "=== CLEARING ALL CANDIDATES ===")
                android.util.Log.d("UserRepository", "clearAllCandidates: Deleting all job applications from database...")

                jobApplicationDao.deleteAllApplications()

                android.util.Log.d("UserRepository", "clearAllCandidates: ✅ All job applications deleted successfully")
                android.util.Log.d("UserRepository", "clearAllCandidates: All candidates have been removed from all jobs")
                true
            } catch (e: Exception) {
                android.util.Log.e("UserRepository", "clearAllCandidates: ❌ Error clearing candidates", e)
                false
            }
        }
    }


    suspend fun clearCandidatesForOwner(ownerId: String): Boolean {
        return withContext(Dispatchers.IO) {
            try {
                android.util.Log.d("UserRepository", "=== CLEARING CANDIDATES FOR OWNER ===")
                android.util.Log.d("UserRepository", "clearCandidatesForOwner: Deleting applications for owner: $ownerId")

                jobApplicationDao.deleteApplicationsForOwner(ownerId)

                android.util.Log.d("UserRepository", "clearCandidatesForOwner: ✅ Applications for owner $ownerId deleted successfully")
                true
            } catch (e: Exception) {
                android.util.Log.e("UserRepository", "clearCandidatesForOwner: ❌ Error clearing candidates for owner", e)
                false
            }
        }
    }


    suspend fun ensureUserInDatabase(userId: String, name: String, email: String, role: String, professionalRole: String? = null, companyName: String? = null): Boolean {
        return withContext(Dispatchers.IO) {
            try {
                        android.util.Log.d("UserRepository", "=== ensureUserInDatabase CALLED ===")
        android.util.Log.d("UserRepository", "ensureUserInDatabase: Checking user userId=$userId, name=$name, email=$email, role=$role, professionalRole=$professionalRole, companyName=$companyName")

                val existingUser = userDao.getUserById(userId)
                android.util.Log.d("UserRepository", "ensureUserInDatabase: Existing user lookup result = ${existingUser != null}")

                if (existingUser == null) {
                    android.util.Log.d("UserRepository", "ensureUserInDatabase: User not found, creating user $userId")

                    val newUser = if (userId == "john_doe_freelancer") {
                        android.util.Log.d("UserRepository", "ensureUserInDatabase: Creating special John Doe freelancer account")

                        UserEntity(
                            userId = userId,
                            name = name,
                            email = email,
                            role = role,
                            skills = "React,Node.js,Python,AWS,Docker,TypeScript",
                            title = "Senior Full Stack Developer",
                            experience = "6+ years",
                            rating = 4.9f,
                            totalReviews = 150,
                            completedProjects = 55,
                            hourlyRate = "$80-90/hour",
                            availability = "Available now",
                            location = "San Francisco, USA",
                            bio = "Experienced full-stack developer with a passion for creating robust, scalable applications. Specializes in modern web technologies and agile development practices.",
                            isActive = true
                        )
                    } else if (userId == "user_002") {
                        android.util.Log.d("UserRepository", "ensureUserInDatabase: Creating special Sarah Johnson business owner account")

                        UserEntity(
                            userId = userId,
                            name = name,
                            email = email,
                            role = role,
                            skills = "",
                            title = "Founder & CEO",
                            experience = "10+ years",
                            rating = 0f,
                            totalReviews = 0,
                            completedProjects = 0,
                            hourlyRate = "",
                            availability = "",
                            location = "San Francisco, USA",
                            bio = "Visionary leader building innovative software solutions at ChabokSoft. Passionate about connecting talented freelancers with cutting-edge projects.",
                            companyName = "ChabokSoft",
                            isActive = true
                        )
                    } else if (userId == "user_003") {
                        android.util.Log.d("UserRepository", "ensureUserInDatabase: Creating KianSoft business owner account")

                        UserEntity(
                            userId = userId,
                            name = name,
                            email = email,
                            role = role,
                            skills = "",
                            title = "Founder & CEO",
                            experience = "8+ years",
                            rating = 0f,
                            totalReviews = 0,
                            completedProjects = 0,
                            hourlyRate = "",
                            availability = "",
                            location = "Los Angeles, USA",
                            bio = "Innovative leader at KianSoft, focused on delivering exceptional technology solutions and building strong partnerships with talented professionals.",
                            companyName = "KianSoft",
                            isActive = true
                        )
                    } else if (userId == "user_004") {
                        android.util.Log.d("UserRepository", "ensureUserInDatabase: Creating MakanSoft business owner account")

                        UserEntity(
                            userId = userId,
                            name = name,
                            email = email,
                            role = role,
                            skills = "",
                            title = "Founder & CEO",
                            experience = "12+ years",
                            rating = 0f,
                            totalReviews = 0,
                            completedProjects = 0,
                            hourlyRate = "",
                            availability = "",
                            location = "New York, USA",
                            bio = "Experienced entrepreneur at MakanSoft, dedicated to creating innovative solutions and fostering collaborative relationships with freelance professionals.",
                            companyName = "MakanSoft",
                            isActive = true
                        )
                    } else if (userId == "user_005") {
                        android.util.Log.d("UserRepository", "ensureUserInDatabase: Creating TestSoft business owner account")

                        UserEntity(
                            userId = userId,
                            name = name,
                            email = email,
                            role = role,
                            skills = "",
                            title = "Founder & CEO",
                            experience = "15+ years",
                            rating = 0f,
                            totalReviews = 0,
                            completedProjects = 0,
                            hourlyRate = "",
                            availability = "",
                            location = "Seattle, USA",
                            bio = "Seasoned technology leader at TestSoft, committed to excellence in software development and building meaningful connections with freelance talent.",
                            companyName = "TestSoft",
                            isActive = true
                        )
                    } else {
                        android.util.Log.d("UserRepository", "ensureUserInDatabase: Creating default user account with professionalRole='$professionalRole'")

                        UserEntity(
                            userId = userId,
                            name = name,
                            email = email,
                            role = role,
                            skills = if (role == UserSession.ROLE_FREELANCER) "React,Node.js,Python,AWS,Docker" else "",
                            title = if (role == UserSession.ROLE_FREELANCER) professionalRole ?: "" else "",
                            experience = if (role == UserSession.ROLE_FREELANCER) "5+ years" else "",
                            rating = if (role == UserSession.ROLE_FREELANCER) 4.8f else 0f,
                            totalReviews = if (role == UserSession.ROLE_FREELANCER) 127 else 0,
                            completedProjects = if (role == UserSession.ROLE_FREELANCER) 47 else 0,
                            hourlyRate = if (role == UserSession.ROLE_FREELANCER) "$75-85/hour" else "",
                            availability = if (role == UserSession.ROLE_FREELANCER) "Available now" else "",
                            location = "New York, USA",
                            bio = if (role == UserSession.ROLE_FREELANCER)
                                  "Passionate full-stack developer with expertise in modern web technologies."
                                  else "Business professional focused on innovative solutions.",
                            isActive = true,
                            companyName = if (role == UserSession.ROLE_BUSINESS_OWNER) companyName ?: "" else ""
                        )
                    }

                    android.util.Log.d("UserRepository", "ensureUserInDatabase: About to insert user with details - userId=${newUser.userId}, role=${newUser.role}, isActive=${newUser.isActive}")
                    userDao.insertUser(newUser)
                    android.util.Log.d("UserRepository", "ensureUserInDatabase: User insert completed")


                    kotlinx.coroutines.delay(100)
                    val verifyUser = userDao.getUserById(userId)
                    if (verifyUser != null) {
                        android.util.Log.d("UserRepository", "ensureUserInDatabase: ✅ User $userId created and verified successfully")
                        android.util.Log.d("UserRepository", "ensureUserInDatabase: Verified user details - userId=${verifyUser.userId}, role=${verifyUser.role}, isActive=${verifyUser.isActive}")
                        true
                    } else {
                        android.util.Log.e("UserRepository", "ensureUserInDatabase: ❌ User $userId was NOT found after insert - database issue!")
                        false
                    }
                } else {
                    android.util.Log.d("UserRepository", "ensureUserInDatabase: ✅ User $userId already exists")
                    android.util.Log.d("UserRepository", "ensureUserInDatabase: Existing user details - userId=${existingUser.userId}, role=${existingUser.role}, company=${existingUser.companyName}")
                    
                    // Update existing user with latest data from backend (especially company name)
                    var needsUpdate = false
                    var updatedUser = existingUser
                    
                    // Check if company name needs updating
                    if (companyName != null && existingUser.companyName != companyName) {
                        android.util.Log.d("UserRepository", "ensureUserInDatabase: Updating company name from '${existingUser.companyName}' to '$companyName'")
                        updatedUser = updatedUser.copy(companyName = companyName)
                        needsUpdate = true
                    }
                    
                    // Check if professional role needs updating
                    if (professionalRole != null && existingUser.title != professionalRole) {
                        android.util.Log.d("UserRepository", "ensureUserInDatabase: Updating professional role from '${existingUser.title}' to '$professionalRole'")
                        updatedUser = updatedUser.copy(title = professionalRole)
                        needsUpdate = true
                    }
                    
                    // Check if role needs updating
                    if (existingUser.role != role) {
                        android.util.Log.d("UserRepository", "ensureUserInDatabase: Updating role from '${existingUser.role}' to '$role'")
                        updatedUser = updatedUser.copy(role = role)
                        needsUpdate = true
                    }
                    
                    // Check if name needs updating
                    if (existingUser.name != name) {
                        android.util.Log.d("UserRepository", "ensureUserInDatabase: Updating name from '${existingUser.name}' to '$name'")
                        updatedUser = updatedUser.copy(name = name)
                        needsUpdate = true
                    }
                    
                    // Check if email needs updating
                    if (existingUser.email != email) {
                        android.util.Log.d("UserRepository", "ensureUserInDatabase: Updating email from '${existingUser.email}' to '$email'")
                        updatedUser = updatedUser.copy(email = email)
                        needsUpdate = true
                    }
                    
                    if (needsUpdate) {
                        android.util.Log.d("UserRepository", "ensureUserInDatabase: Updating existing user with new data")
                        userDao.updateUser(updatedUser)
                        android.util.Log.d("UserRepository", "ensureUserInDatabase: ✅ User $userId updated successfully")
                    } else {
                        android.util.Log.d("UserRepository", "ensureUserInDatabase: No updates needed for user $userId")
                    }
                    
                    true
                }
            } catch (e: Exception) {
                android.util.Log.e("UserRepository", "=== ❌ EXCEPTION in ensureUserInDatabase ===", e)
                false
            }
        }
    }


    suspend fun updateUserProfession(userId: String, newProfession: String): Boolean {
        return withContext(Dispatchers.IO) {
            try {
                android.util.Log.d("UserRepository", "updateUserProfession: Updating profession for user $userId to '$newProfession'")

                val rowsUpdated = userDao.updateUserTitle(userId, newProfession)
                val success = rowsUpdated > 0

                if (success) {
                    android.util.Log.d("UserRepository", "updateUserProfession: ✅ Successfully updated profession in database")
                } else {
                    android.util.Log.w("UserRepository", "updateUserProfession: ⚠️ No rows updated - user may not exist")
                }

                success
            } catch (e: Exception) {
                android.util.Log.e("UserRepository", "updateUserProfession: ❌ Error updating profession in database", e)
                false
            }
        }
    }


    suspend fun updateUserRole(userId: String, newRole: String): Boolean {
        return withContext(Dispatchers.IO) {
            try {
                android.util.Log.d("UserRepository", "updateUserRole: Updating role for user $userId to '$newRole'")

                val rowsUpdated = userDao.updateUserRole(userId, newRole)
                val success = rowsUpdated > 0

                if (success) {
                    android.util.Log.d("UserRepository", "updateUserRole: ✅ Successfully updated role in database")
                } else {
                    android.util.Log.w("UserRepository", "updateUserRole: ⚠️ No rows updated - user may not exist")
                }

                success
            } catch (e: Exception) {
                android.util.Log.e("UserRepository", "updateUserRole: ❌ Error updating role in database", e)
                false
            }
        }
    }


    suspend fun updateUserData(userEntity: UserEntity): Boolean {
        return withContext(Dispatchers.IO) {
            try {
                android.util.Log.d("UserRepository", "updateUserData: Updating user data for user ${userEntity.userId}")

                userDao.updateUser(userEntity)
                android.util.Log.d("UserRepository", "updateUserData: ✅ Successfully updated user data in database")
                true
            } catch (e: Exception) {
                android.util.Log.e("UserRepository", "updateUserData: ❌ Error updating user data in database", e)
                false
            }
        }
    }
}

data class JobAnalytics(
    val totalViews: Int,
    val totalApplications: Int,
    val uniqueViewers: Int,
    val conversionRate: Float
)

data class JobSpecificAnalytics(
    val uniqueViews: Int,
    val acceptedFreelancers: Int
)