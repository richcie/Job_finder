package com.uilover.project196.Repository

import android.content.Context
import com.uilover.project196.Database.AppDatabase
import com.uilover.project196.Model.JobModel
import com.uilover.project196.Model.JobEntity
import com.uilover.project196.Utils.UserSession
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.GlobalScope
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext

class MainRepository private constructor() {
    private var database: AppDatabase? = null

    private var cachedJobsWithViewCounts: List<JobModel>? = null
    private var lastCacheTime: Long = 0
    private val cacheValidityMs = 30000

    companion object {
        @Volatile
        private var INSTANCE: MainRepository? = null

        fun getInstance(): MainRepository {
            return INSTANCE ?: synchronized(this) {
                val newInstance = INSTANCE ?: MainRepository().also {
                    INSTANCE = it
                }
                newInstance
            }
        }
    }

    init {
    }

    fun initializeDatabase(context: Context) {
        if (database == null) {
            database = AppDatabase.getDatabase(context)
        }
    }



    val location = listOf("LosAngles, USA", "NewYork,Usa")
    val category= listOf("All","Accountant","Programmer","Writer")

    val exampleText:String=  "We are searching for a talented and motivated this job to join our growing team. In this role, " +
            "you will be responsible for this job and will be responsible for this job."

    private val _items = mutableListOf(
        JobModel(
            "UI Designer",
            "ChabokSoft",
            "logo1",
            "Full-Time",
            "Remote",
            "Internship",
            "NewYork, USA",
            "\$38k - \$46K",
            "2",
            exampleText,
            exampleText,
            false,
            "user_002",
            "open"
        ),
        JobModel(
            "Accountants",
            "KianSoft",
            "logo2",
            "Part-Time",
            "In Person",
            "In Person",
            "LosAngles, USA",
            "\$26k - \$36K",
            "1",
            exampleText,
            exampleText,
            false,
            "user_003",
            "open"
        ),
        JobModel(
            "The author of the news",
            "MakanSoft",
            "logo3",
            "Part-Time",
            "Remote",
            "Senior Level",
            "NewYork, USA",
            "\$20k - \$23K",
            "3",
            exampleText,
            exampleText,
            false,
            "user_004",
            "open"
        ),
        JobModel(
            "Kotlin Programmer",
            "TestSoft",
            "logo4",
            "Full-Time",
            "Remote",
            "Internship",
            "LosAngles, USA",
            "\$38k - \$40K",
            "2",
            exampleText,
            exampleText,
            false,
            "user_005",
            "open"
        )
    )


    private val _userBookmarks = mutableMapOf<String, MutableSet<String>>()


    private class LoggingMutableSet(private val userId: String) : MutableSet<String> {
        private val internalSet = mutableSetOf<String>()

        override fun add(element: String): Boolean {
            val result = internalSet.add(element)
            android.util.Log.d("MainRepository", "🔍 BOOKMARK ADD: User $userId added '$element' (result: $result)")
            return result
        }

        override fun remove(element: String): Boolean {
            val result = internalSet.remove(element)
            if (result) {
                android.util.Log.w("MainRepository", "🚨 BOOKMARK REMOVED: User $userId removed '$element'")
                android.util.Log.w("MainRepository", "🚨 REMOVAL STACK TRACE:", Exception("Bookmark removal trace"))
            }
            return result
        }

        override fun clear() {
            if (internalSet.isNotEmpty()) {
                android.util.Log.w("MainRepository", "🚨 BOOKMARKS CLEARED: User $userId had ${internalSet.size} bookmarks cleared!")
                android.util.Log.w("MainRepository", "🚨 Cleared bookmarks: ${internalSet.toList()}")
                android.util.Log.w("MainRepository", "🚨 CLEAR STACK TRACE:", Exception("Bookmark clear trace"))
            }
            internalSet.clear()
        }

        override fun removeAll(elements: Collection<String>): Boolean {
            val result = internalSet.removeAll(elements)
            if (result) {
                android.util.Log.w("MainRepository", "🚨 BULK BOOKMARK REMOVAL: User $userId removed ${elements.size} bookmarks")
                android.util.Log.w("MainRepository", "🚨 BULK REMOVAL STACK TRACE:", Exception("Bulk removal trace"))
            }
            return result
        }


        override val size: Int get() = internalSet.size
        override fun contains(element: String): Boolean = internalSet.contains(element)
        override fun containsAll(elements: Collection<String>): Boolean = internalSet.containsAll(elements)
        override fun isEmpty(): Boolean = internalSet.isEmpty()
        override fun iterator(): MutableIterator<String> = internalSet.iterator()
        override fun retainAll(elements: Collection<String>): Boolean = internalSet.retainAll(elements)
        override fun addAll(elements: Collection<String>): Boolean = internalSet.addAll(elements)

        fun toList(): List<String> = internalSet.toList()
    }


    private fun getOrCreateUserBookmarkSet(userId: String): MutableSet<String> {
        if (_userBookmarks[userId] == null) {
            android.util.Log.d("MainRepository", "🔍 Creating new bookmark set for user: $userId")
            _userBookmarks[userId] = LoggingMutableSet(userId)
        }
        return _userBookmarks[userId]!!
    }

    val items: List<JobModel>
        get() {
            return _items.toList()
                .filter { job -> job.isOpen() }
                .map { job ->

                    val currentUserId = UserSession.getUserId()
                    val isBookmarked = if (currentUserId != null && !job.isOwnedByCurrentUser()) {
                        _userBookmarks[currentUserId]?.contains(getJobId(job)) == true
                    } else {
                        false
                    }
                    job.copy(isBookmarked = isBookmarked)
                }
        }


    val allItems: List<JobModel>
        get() = _items.toList().map { job ->
            val currentUserId = UserSession.getUserId()
            val isBookmarked = if (currentUserId != null && !job.isOwnedByCurrentUser()) {
                _userBookmarks[currentUserId]?.contains(getJobId(job)) == true
            } else {
                false
            }
            job.copy(isBookmarked = isBookmarked)
        }

    private fun getJobId(job: JobModel): String {

        return "${job.title}_${job.company}"
    }

    fun toggleBookmark(job: JobModel) {
        android.util.Log.d("MainRepository", "toggleBookmark called for job: ${job.title}")

        val currentUserId = UserSession.getUserId()
        android.util.Log.d("MainRepository", "Current user ID: $currentUserId")
        android.util.Log.d("MainRepository", "User logged in: ${UserSession.isLoggedIn()}")

        if (currentUserId == null) {
            android.util.Log.w("MainRepository", "Cannot bookmark - user not logged in")
            return
        }


        if (job.isOwnedByCurrentUser() || job.isClosed()) {
            android.util.Log.w("MainRepository", "Cannot bookmark - job is owned by user or closed. Owned: ${job.isOwnedByCurrentUser()}, Closed: ${job.isClosed()}")
            return
        }

        val jobId = getJobId(job)
        android.util.Log.d("MainRepository", "Job ID: $jobId")


        android.util.Log.d("MainRepository", "=== BOOKMARK MAP STATE BEFORE TOGGLE ===")
        android.util.Log.d("MainRepository", "Total users in bookmark map: ${_userBookmarks.size}")
        _userBookmarks.forEach { (userId, bookmarks) ->
            android.util.Log.d("MainRepository", "User $userId has ${bookmarks.size} bookmarks: ${bookmarks.toList()}")
        }


        val userBookmarks = getOrCreateUserBookmarkSet(currentUserId)
        val wasBookmarked = userBookmarks.contains(jobId)

        android.util.Log.d("MainRepository", "Bookmark state before toggle: $wasBookmarked")


        if (userBookmarks.contains(jobId)) {
            userBookmarks.remove(jobId)
            android.util.Log.d("MainRepository", "Removed bookmark for job: ${job.title}")
        } else {
            userBookmarks.add(jobId)
            android.util.Log.d("MainRepository", "Added bookmark for job: ${job.title}")
        }


        val isBookmarked = userBookmarks.contains(jobId)

        android.util.Log.d("MainRepository", "Bookmark state change: $wasBookmarked -> $isBookmarked")
        android.util.Log.d("MainRepository", "User bookmarks count: ${userBookmarks.size}")
        android.util.Log.d("MainRepository", "Final bookmark state for '${job.title}': $isBookmarked")
        android.util.Log.d("MainRepository", "All user bookmarks: ${userBookmarks.toList()}")


        android.util.Log.d("MainRepository", "=== BOOKMARK MAP STATE AFTER TOGGLE ===")
        android.util.Log.d("MainRepository", "Total users in bookmark map: ${_userBookmarks.size}")
        _userBookmarks.forEach { (userId, bookmarks) ->
            android.util.Log.d("MainRepository", "User $userId has ${bookmarks.size} bookmarks: ${bookmarks.toList()}")
        }


        clearCache()


        val finalBookmarkState = isBookmarked
        android.util.Log.d("MainRepository", "Captured final bookmark state for database: $finalBookmarkState")


        @kotlin.OptIn(kotlinx.coroutines.DelicateCoroutinesApi::class)
        kotlinx.coroutines.GlobalScope.launch {
            try {
                android.util.Log.d("MainRepository", "Database operation starting with bookmark state: $finalBookmarkState")
                val db = database
                if (db != null) {

                    var jobEntity = db.jobDao().getJobByDetails(job.title, job.company, job.ownerId)

                    if (jobEntity == null) {

                        val newJobEntity = JobEntity(
                            title = job.title,
                            company = job.company,
                            location = job.location,
                            time = job.time,
                            model = job.model,
                            level = job.level,
                            salary = job.salary,
                            category = job.category,
                            picUrl = job.picUrl,
                            about = job.about,
                            description = job.description,
                            isBookmarked = finalBookmarkState,
                            ownerId = job.ownerId,
                            status = job.status
                        )
                        db.jobDao().insertJob(newJobEntity)
                        android.util.Log.d("MainRepository", "Created job in database with bookmark state: ${job.title} = $finalBookmarkState")
                    } else {

                        val updatedJobEntity = jobEntity.copy(isBookmarked = finalBookmarkState)
                        db.jobDao().updateJob(updatedJobEntity)
                        android.util.Log.d("MainRepository", "Updated job bookmark in database: ${job.title} = $finalBookmarkState")
                    }
                } else {
                    android.util.Log.w("MainRepository", "Database is null, cannot persist bookmark to database")
                }
            } catch (e: Exception) {
                android.util.Log.e("MainRepository", "Error persisting bookmark to database", e)
            }
        }

        android.util.Log.d("MainRepository", "toggleBookmark completed successfully")
    }

    fun getBookmarkedJobs(): List<JobModel> {
        android.util.Log.d("MainRepository", "=== GET BOOKMARKED JOBS ===")
        val currentUserId = UserSession.getUserId()
        android.util.Log.d("MainRepository", "Current user ID: $currentUserId")

        return if (currentUserId != null) {
            val userBookmarks = _userBookmarks[currentUserId] ?: emptySet()
            android.util.Log.d("MainRepository", "User has ${userBookmarks.size} bookmarks: ${userBookmarks.toList()}")

            val allJobs = _items
            android.util.Log.d("MainRepository", "Total jobs available: ${allJobs.size}")

            val filteredJobs = allJobs.filter { job ->
                val jobId = getJobId(job)
                val isOwnedByUser = job.isOwnedByCurrentUser()
                val isOpen = job.isOpen()
                val isBookmarked = userBookmarks.contains(jobId)

                !isOwnedByUser && isOpen && isBookmarked
            }.map { job -> job.copy(isBookmarked = true) }

            android.util.Log.d("MainRepository", "Returning ${filteredJobs.size} bookmarked jobs")

            filteredJobs
        } else {
            android.util.Log.d("MainRepository", "User not logged in, returning empty list")
            emptyList()
        }
    }

    /**
     * Get bookmarked jobs with view counts (async version)
     */
    suspend fun getBookmarkedJobsWithViewCounts(): List<JobModel> {
        return withContext(Dispatchers.IO) {
            android.util.Log.d("MainRepository", "=== GET BOOKMARKED JOBS WITH VIEW COUNTS ===")
            val currentUserId = UserSession.getUserId()
            android.util.Log.d("MainRepository", "Current user ID: $currentUserId")

            if (currentUserId != null) {
                val userBookmarks = _userBookmarks[currentUserId] ?: emptySet()
                android.util.Log.d("MainRepository", "User has ${userBookmarks.size} bookmarks: ${userBookmarks.toList()}")

                val allJobs = _items
                android.util.Log.d("MainRepository", "Total jobs available: ${allJobs.size}")

                val filteredJobs = allJobs.filter { job ->
                    val jobId = getJobId(job)
                    val isOwnedByUser = job.isOwnedByCurrentUser()
                    val isOpen = job.isOpen()
                    val isBookmarked = userBookmarks.contains(jobId)

                    !isOwnedByUser && isOpen && isBookmarked
                }

                android.util.Log.d("MainRepository", "Found ${filteredJobs.size} bookmarked jobs, enriching with view counts...")

                // Enrich with view counts
                val enrichedJobs = filteredJobs.map { job ->
                    val viewCount = try {
                        val jobEntity = findJobEntity(job)
                        if (jobEntity != null && database != null) {
                            database!!.jobViewDao().getUniqueViewersForJob(jobEntity.id)
                        } else {
                            0
                        }
                    } catch (e: Exception) {
                        android.util.Log.w("MainRepository", "Error getting view count for bookmarked job ${job.title}: ${e.message}")
                        0
                    }
                    
                    job.copy(isBookmarked = true, viewCount = viewCount)
                }

                android.util.Log.d("MainRepository", "Returning ${enrichedJobs.size} bookmarked jobs with view counts")
                enrichedJobs
            } else {
                android.util.Log.d("MainRepository", "User not logged in, returning empty list")
                emptyList()
            }
        }
    }

    fun isJobBookmarked(job: JobModel): Boolean {
        val currentUserId = UserSession.getUserId()

        return if (currentUserId != null && !job.isOwnedByCurrentUser() && job.isOpen()) {
            val userBookmarks = _userBookmarks[currentUserId] ?: emptySet()
            val jobId = getJobId(job)
            val isBookmarked = userBookmarks.contains(jobId)

            android.util.Log.d("MainRepository", "isJobBookmarked for '${job.title}': $isBookmarked")

            isBookmarked
        } else {
            android.util.Log.d("MainRepository", "Cannot bookmark '${job.title}' - user null, job owned, or job closed")
            false
        }
    }


    fun canBookmarkJob(job: JobModel): Boolean {
        return UserSession.isLoggedIn() && !job.isOwnedByCurrentUser() && job.isOpen()
    }


    fun getUserBookmarkIds(userId: String): Set<String> {
        return _userBookmarks[userId]?.toSet() ?: emptySet()
    }


    fun clearUserBookmarks(userId: String) {
        android.util.Log.d("MainRepository", "=== CLEAR USER BOOKMARKS CALLED ===")
        android.util.Log.d("MainRepository", "Clearing bookmarks for user: $userId")

        val existingBookmarks = _userBookmarks[userId]?.toList() ?: emptyList()
        android.util.Log.d("MainRepository", "User had ${existingBookmarks.size} bookmarks: $existingBookmarks")


        android.util.Log.w("MainRepository", "🚨 CLEAR USER BOOKMARKS STACK TRACE:", Exception("Clear user bookmarks trace"))

        _userBookmarks.remove(userId)
        android.util.Log.d("MainRepository", "Bookmarks cleared for user: $userId")
    }


    fun getOwnedJobs(): List<JobModel> {
        val currentUserId = UserSession.getUserId()
        return if (currentUserId != null) {
            _items.filter { job -> job.ownerId == currentUserId }
        } else {
            emptyList()
        }
    }


    fun getOpenOwnedJobs(): List<JobModel> {
        val currentUserId = UserSession.getUserId()
        return if (currentUserId != null) {
            _items.filter { job -> job.ownerId == currentUserId && job.isOpen() }
        } else {
            emptyList()
        }
    }


    fun getClosedOwnedJobs(): List<JobModel> {
        val currentUserId = UserSession.getUserId()
        return if (currentUserId != null) {
            _items.filter { job -> job.ownerId == currentUserId && job.isClosed() }
        } else {
            emptyList()
        }
    }


    fun closeJob(job: JobModel): Boolean {
        val currentUserId = UserSession.getUserId()
        if (currentUserId != null && job.ownerId == currentUserId && job.isOpen()) {
            val index = _items.indexOfFirst {
                it.ownerId == job.ownerId &&
                it.title == job.title &&
                it.company == job.company
            }
            if (index >= 0) {
                _items[index] = _items[index].copy(status = "closed")
                clearCache()


                @kotlin.OptIn(kotlinx.coroutines.DelicateCoroutinesApi::class)
                kotlinx.coroutines.GlobalScope.launch {
                    try {
                        val db = database
                        if (db != null) {
                            val jobEntity = db.jobDao().getJobByDetails(job.title, job.company, job.ownerId)
                            if (jobEntity != null) {
                                val updatedJob = jobEntity.copy(status = "closed")
                                db.jobDao().updateJob(updatedJob)
                                android.util.Log.d("MainRepository", "Job closed in database: ${job.title}")
                            }
                        }
                    } catch (e: Exception) {
                        android.util.Log.e("MainRepository", "Error updating job status in database", e)
                    }
                }

                return true
            }
        }
        return false
    }


    fun reopenJob(job: JobModel): Boolean {
        val currentUserId = UserSession.getUserId()
        if (currentUserId != null && job.ownerId == currentUserId && job.isClosed()) {
            val index = _items.indexOfFirst {
                it.ownerId == job.ownerId &&
                it.title == job.title &&
                it.company == job.company
            }
            if (index >= 0) {
                _items[index] = _items[index].copy(status = "open")
                clearCache()


                @kotlin.OptIn(kotlinx.coroutines.DelicateCoroutinesApi::class)
                kotlinx.coroutines.GlobalScope.launch {
                    try {
                        val db = database
                        if (db != null) {
                            val jobEntity = db.jobDao().getJobByDetails(job.title, job.company, job.ownerId)
                            if (jobEntity != null) {
                                val updatedJob = jobEntity.copy(status = "open")
                                db.jobDao().updateJob(updatedJob)
                                android.util.Log.d("MainRepository", "Job reopened in database: ${job.title}")
                            }
                        }
                    } catch (e: Exception) {
                        android.util.Log.e("MainRepository", "Error updating job status in database", e)
                    }
                }

                return true
            }
        }
        return false
    }


    fun updateJob(updatedJob: JobModel): Boolean {
        android.util.Log.d("MainRepository", "🔧 === JOB UPDATE REPOSITORY TRACE ===")
        android.util.Log.d("MainRepository", "🔧 Attempting to update job: '${updatedJob.title}'")
        android.util.Log.d("MainRepository", "🔧 Updated job owner: '${updatedJob.ownerId}'")

        val currentUserId = UserSession.getUserId()
        android.util.Log.d("MainRepository", "🔧 Current user ID: '$currentUserId'")

        if (currentUserId == null || updatedJob.ownerId != currentUserId) {
            android.util.Log.e("MainRepository", "🔧 ❌ Update denied: user mismatch or not logged in")
            return false
        }


        val index = _items.indexOfFirst {
            it.ownerId == updatedJob.ownerId &&
            it.company == updatedJob.company &&
            it.ownerId == currentUserId
        }

        android.util.Log.d("MainRepository", "🔧 Found job at index: $index")

        if (index >= 0) {

            val originalJob = _items[index]
            android.util.Log.d("MainRepository", "🔧 Original job title: '${originalJob.title}'")
            android.util.Log.d("MainRepository", "🔧 New job title: '${updatedJob.title}'")

            _items[index] = updatedJob
            clearCache()

            android.util.Log.d("MainRepository", "🔧 ✅ Updated in-memory job successfully")


            @kotlin.OptIn(kotlinx.coroutines.DelicateCoroutinesApi::class)
            kotlinx.coroutines.GlobalScope.launch {
                try {
                    android.util.Log.d("MainRepository", "🔧 Updating job in database...")
                    val db = database
                    if (db != null) {

                        val jobEntity = db.jobDao().getJobByDetails(
                            originalJob.title,
                            originalJob.company,
                            originalJob.ownerId
                        )

                        if (jobEntity != null) {
                            android.util.Log.d("MainRepository", "🔧 Found job entity with ID: ${jobEntity.id}")


                            val updatedJobEntity = jobEntity.copy(
                                title = updatedJob.title,
                                salary = updatedJob.salary,
                                time = updatedJob.time,
                                model = updatedJob.model,
                                level = updatedJob.level,
                                description = updatedJob.description,
                                about = updatedJob.about,
                                location = updatedJob.location
                            )

                            db.jobDao().updateJob(updatedJobEntity)
                            android.util.Log.d("MainRepository", "🔧 ✅ Job updated in database successfully!")
                            android.util.Log.d("MainRepository", "🔧 Database updated - Title: '${updatedJobEntity.title}'")
                        } else {
                            android.util.Log.e("MainRepository", "🔧 ❌ Job entity not found in database!")
                        }
                    } else {
                        android.util.Log.e("MainRepository", "🔧 ❌ Database is null!")
                    }
                } catch (e: Exception) {
                    android.util.Log.e("MainRepository", "🔧 ❌ Error updating job in database", e)
                }
            }

            return true
        } else {
            android.util.Log.e("MainRepository", "🔧 ❌ Job not found in _items list")
            return false
        }
    }


    fun createJob(jobModel: JobModel): Boolean {
        val currentUserId = UserSession.getUserId()
        val userRole = UserSession.getUserRole()


        if (currentUserId != null && userRole == UserSession.ROLE_BUSINESS_OWNER) {

            val newJob = jobModel.copy(
                ownerId = currentUserId,
                status = "open"
            )
            _items.add(newJob)


            clearCache()

            return true
        }
        return false
    }


    private fun clearCache() {
        cachedJobsWithViewCounts = null
        lastCacheTime = 0
    }


    fun getFilterOptions(): FilterOptions {
        return FilterOptions(
            salaryRanges = _items.map { it.salary }.distinct().sorted(),
            jobTypes = _items.map { it.time }.distinct().filter { it.isNotEmpty() },
            workingModels = _items.map { it.model }.distinct().filter { it.isNotEmpty() },
            levels = _items.map { it.level }.distinct().filter { it.isNotEmpty() }
        )
    }

    data class FilterOptions(
        val salaryRanges: List<String>,
        val jobTypes: List<String>,
        val workingModels: List<String>,
        val levels: List<String>
    )


    suspend fun findJobEntity(jobModel: JobModel): JobEntity? {
        return withContext(Dispatchers.IO) {
            database?.jobDao()?.getJobByDetails(
                title = jobModel.title,
                company = jobModel.company,
                ownerId = jobModel.ownerId
            )
        }
    }

    suspend fun insertJob(jobEntity: JobEntity) {
        withContext(Dispatchers.IO) {
            database?.jobDao()?.insertJob(jobEntity)
        }
    }


    suspend fun getJobsWithViewCounts(): List<JobModel> {
        return withContext(Dispatchers.IO) {
            try {

                val currentTime = System.currentTimeMillis()
                if (cachedJobsWithViewCounts != null && (currentTime - lastCacheTime) < cacheValidityMs) {
                    android.util.Log.d("MainRepository", "getJobsWithViewCounts: Returning cached data")
                    return@withContext cachedJobsWithViewCounts!!
                }

                val baseJobs = items

                android.util.Log.d("MainRepository", "getJobsWithViewCounts: Loading ${baseJobs.size} jobs (optimized)")


                if (database == null) {
                    android.util.Log.w("MainRepository", "getJobsWithViewCounts: Database not available, returning jobs with 0 view counts")
                    val fallbackJobs = baseJobs.map { it.copy(viewCount = 0) }

                    cachedJobsWithViewCounts = fallbackJobs
                    lastCacheTime = currentTime
                    return@withContext fallbackJobs
                }


                val enrichedJobs = try {

                    val jobEntityMap = mutableMapOf<String, JobEntity>()
                    for (job in baseJobs) {
                        try {
                            val jobEntity = findJobEntity(job)
                            if (jobEntity != null) {
                                val jobKey = "${job.title}_${job.company}_${job.ownerId}"
                                jobEntityMap[jobKey] = jobEntity
                            }
                        } catch (e: Exception) {
                            android.util.Log.w("MainRepository", "Error finding entity for job ${job.title}: ${e.message}")
                        }
                    }


                    val viewCountMap = mutableMapOf<Int, Int>()
                    for (jobEntity in jobEntityMap.values) {
                        try {
                            val viewCount = database!!.jobViewDao().getUniqueViewersForJob(jobEntity.id)
                            viewCountMap[jobEntity.id] = viewCount
                        } catch (e: Exception) {
                            android.util.Log.w("MainRepository", "Error getting view count for job ${jobEntity.id}: ${e.message}")
                            viewCountMap[jobEntity.id] = 0
                        }
                    }


                    baseJobs.map { job ->
                        val jobKey = "${job.title}_${job.company}_${job.ownerId}"
                        val jobEntity = jobEntityMap[jobKey]
                        val viewCount = if (jobEntity != null) {
                            viewCountMap[jobEntity.id] ?: 0
                        } else {
                            0
                        }
                        job.copy(viewCount = viewCount)
                    }
                } catch (e: Exception) {
                    android.util.Log.e("MainRepository", "getJobsWithViewCounts: Database error, using fallback", e)

                    baseJobs.map { it.copy(viewCount = 0) }
                }

                android.util.Log.d("MainRepository", "getJobsWithViewCounts: Completed loading ${enrichedJobs.size} jobs")


                cachedJobsWithViewCounts = enrichedJobs
                lastCacheTime = currentTime

                enrichedJobs

            } catch (e: Exception) {
                android.util.Log.e("MainRepository", "getJobsWithViewCounts: Critical error, using safe fallback", e)

                val fallbackJobs = _items.filter { it.isOpen() }.map { job ->
                    val currentUserId = UserSession.getUserId()
                    val isBookmarked = if (currentUserId != null && !job.isOwnedByCurrentUser()) {
                        _userBookmarks[currentUserId]?.contains(getJobId(job)) == true
                    } else {
                        false
                    }
                    job.copy(isBookmarked = isBookmarked, viewCount = 0)
                }
                fallbackJobs
            }
        }
    }


    suspend fun getAllJobsWithViewCounts(): List<JobModel> {
        return withContext(Dispatchers.IO) {
            val baseJobs = allItems
            val enrichedJobs = mutableListOf<JobModel>()

            android.util.Log.d("MainRepository", "getAllJobsWithViewCounts: Loading all jobs with view counts for all user types")

            for (job in baseJobs) {
                try {

                    val jobEntity = findJobEntity(job)
                    val viewCount = if (jobEntity != null && database != null) {

                        database!!.jobViewDao().getUniqueViewersForJob(jobEntity.id)
                    } else {
                        0
                    }


                    val enrichedJob = job.copy(viewCount = viewCount)
                    enrichedJobs.add(enrichedJob)

                } catch (e: Exception) {
                    android.util.Log.e("MainRepository", "getAllJobsWithViewCounts: Error getting view count for job '${job.title}'", e)

                    enrichedJobs.add(job.copy(viewCount = 0))
                }
            }

            android.util.Log.d("MainRepository", "getAllJobsWithViewCounts: Loaded ${enrichedJobs.size} jobs with view counts for all user types")
            enrichedJobs
        }
    }


    private fun logBookmarkMapState(caller: String) {
        android.util.Log.d("MainRepository", "=== BOOKMARK MAP STATE FROM: $caller ===")
        android.util.Log.d("MainRepository", "Total users in bookmark map: ${_userBookmarks.size}")
        if (_userBookmarks.isEmpty()) {
            android.util.Log.w("MainRepository", "🚨 BOOKMARK MAP IS EMPTY! Called from: $caller")
        } else {
            _userBookmarks.forEach { (userId, bookmarks) ->
                android.util.Log.d("MainRepository", "User $userId has ${bookmarks.size} bookmarks: ${bookmarks.toList()}")
            }
        }
    }
}