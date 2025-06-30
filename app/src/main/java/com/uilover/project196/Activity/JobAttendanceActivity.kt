package com.uilover.project196.Activity

import android.app.Dialog
import android.content.Intent
import android.os.Bundle
import android.view.View
import android.widget.Toast
import androidx.appcompat.app.AppCompatActivity
import androidx.lifecycle.lifecycleScope
import androidx.lifecycle.ViewModelProvider
import androidx.recyclerview.widget.LinearLayoutManager
import com.uilover.project196.Repository.FreelancerJobRepository
import com.uilover.project196.Utils.UserSession
import com.uilover.project196.databinding.ActivityJobAttendanceBinding
import com.uilover.project196.databinding.DialogProgressReportBinding
import com.uilover.project196.Adapter.AttendanceDetailAdapter
import com.uilover.project196.Database.AppDatabase
import com.uilover.project196.Model.JobAttendanceEntity
import com.uilover.project196.ViewModel.ProgressReportViewModel
import com.uilover.project196.R
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import java.text.SimpleDateFormat
import java.util.*

// KRITERIA: Multiple Activity (6/8) - Activity untuk absensi pekerjaan
// KRITERIA WAJIB: Multiple Activity (6/8) - Activity untuk absensi dan tracking pekerjaan
// KRITERIA KOMPLEKSITAS: Fitur kompleks beyond CRUD - attendance tracking
class JobAttendanceActivity : AppCompatActivity() {

    private lateinit var binding: ActivityJobAttendanceBinding
    private lateinit var freelancerJobRepository: FreelancerJobRepository
    private lateinit var progressReportViewModel: ProgressReportViewModel
    private lateinit var attendanceAdapter: AttendanceDetailAdapter
    private var attendanceList = mutableListOf<JobAttendanceEntity>()

    private var jobId: Int = 0
    private var jobTitle: String = ""
    private var companyName: String = ""

    override fun onCreate(savedInstanceState: Bundle?) {
        android.util.Log.d("JobAttendanceActivity", "=== ACTIVITY CREATION START ===")
        super.onCreate(savedInstanceState)

        try {
            binding = ActivityJobAttendanceBinding.inflate(layoutInflater)
            setContentView(binding.root)
            android.util.Log.d("JobAttendanceActivity", "✅ Layout inflated and set successfully")

            setupRepository()
            android.util.Log.d("JobAttendanceActivity", "✅ Repository setup completed")

            getIntentData()
            android.util.Log.d("JobAttendanceActivity", "✅ Intent data retrieved: Job ID=$jobId, Title='$jobTitle', Company='$companyName'")

            setupUI()
            android.util.Log.d("JobAttendanceActivity", "✅ UI setup completed")

            setupRecyclerView()
            android.util.Log.d("JobAttendanceActivity", "✅ RecyclerView setup completed")

            loadAttendanceRecords()
            android.util.Log.d("JobAttendanceActivity", "✅ Loading attendance records started")

        } catch (e: Exception) {
            android.util.Log.e("JobAttendanceActivity", "❌ Error during onCreate", e)
            android.widget.Toast.makeText(this, "Error initializing activity: ${e.message}", android.widget.Toast.LENGTH_LONG).show()
            finish()
        }
    }

    private fun setupRepository() {
        freelancerJobRepository = FreelancerJobRepository.getInstance(this)
        progressReportViewModel = ViewModelProvider(this, ViewModelProvider.AndroidViewModelFactory.getInstance(getApplication()))[ProgressReportViewModel::class.java]
    }

    private fun getIntentData() {
        jobId = intent.getIntExtra("JOB_ID", 0)
        jobTitle = intent.getStringExtra("JOB_TITLE") ?: ""
        companyName = intent.getStringExtra("COMPANY_NAME") ?: ""
    }

    private fun setupUI() {

        binding.jobTitle.text = jobTitle
        binding.companyName.text = companyName
        binding.currentDate.text = getCurrentDateString()


        binding.backButton.setOnClickListener {
            finish()
        }
    }

    private fun setupRecyclerView() {
        binding.attendanceRecyclerView.layoutManager = LinearLayoutManager(this)
        attendanceAdapter = AttendanceDetailAdapter(attendanceList) { attendance, action ->
            when (action) {
                "check_in" -> handleCheckIn(attendance)
                "check_out" -> handleCheckOut(attendance)
            }
        }
        binding.attendanceRecyclerView.adapter = attendanceAdapter
    }

    private fun loadAttendanceRecords() {
        val freelancerId = UserSession.getUserId()
        if (freelancerId == null) {
            Toast.makeText(this, "User not logged in", Toast.LENGTH_SHORT).show()
            finish()
            return
        }

        android.util.Log.d("JobAttendanceActivity", "=== LOADING ATTENDANCE RECORDS ===")
        android.util.Log.d("JobAttendanceActivity", "Job ID: $jobId, Freelancer ID: $freelancerId")

        binding.loadingIndicator.visibility = View.VISIBLE

        lifecycleScope.launch {
            try {

                val chatRepository = com.uilover.project196.Repository.ChatRepository.getInstance(this@JobAttendanceActivity)
                val isVerified = chatRepository.hasAcceptedVerification(freelancerId)

                if (!isVerified) {
                    android.util.Log.w("JobAttendanceActivity", "❌ ACCESS DENIED: Freelancer $freelancerId has no accepted verification")


                    val attendanceManager = com.uilover.project196.Utils.AttendanceManager.getInstance(this@JobAttendanceActivity)
                    attendanceManager.debugVerificationStatus(freelancerId, "JobAttendanceActivity.loadAttendanceRecords")

                    withContext(Dispatchers.Main) {
                        binding.loadingIndicator.visibility = View.GONE
                        Toast.makeText(
                            this@JobAttendanceActivity,
                            "Verification required. Please send a verification request to the business owner first.",
                            Toast.LENGTH_LONG
                        ).show()
                        finish()
                    }
                    return@launch
                }

                android.util.Log.d("JobAttendanceActivity", "✅ Verification check passed for freelancer $freelancerId")


                val appDatabase = com.uilover.project196.Database.AppDatabase.getDatabase(this@JobAttendanceActivity)
                val jobDataAccess = appDatabase.jobDao()
                val jobEntity = jobDataAccess.getJobById(jobId)

                if (jobEntity?.status == "closed") {
                    android.util.Log.w("JobAttendanceActivity", "❌ ACCESS DENIED: Job has been closed by business owner")

                    withContext(Dispatchers.Main) {
                        binding.loadingIndicator.visibility = View.GONE
                        Toast.makeText(
                            this@JobAttendanceActivity,
                            "This job has been closed by the business owner. Attendance is no longer available.",
                            Toast.LENGTH_LONG
                        ).show()
                        finish()
                    }
                    return@launch
                }

                android.util.Log.d("JobAttendanceActivity", "✅ Job is open and available for attendance")


                val (isValid, filteredRecords, frozenCompany) = withContext(Dispatchers.IO) {
                    val database = AppDatabase.getDatabase(this@JobAttendanceActivity)
                    val jobAttendanceDao = database.jobAttendanceDao()
                    val jobApplicationDao = database.jobApplicationDao()
                    val jobDao = database.jobDao()


                    val frozenCompanyName = if (isCurrentUserFrozenByCompany()) {
                        getFrozenByCompanyName()
                    } else {
                        ""
                    }


                    val job = jobDao.getJobById(jobId)
                    if (job == null) {
                        android.util.Log.e("JobAttendanceActivity", "Job $jobId not found in database")
                        return@withContext Triple(false, emptyList<JobAttendanceEntity>(), frozenCompanyName)
                    }

                    val application = jobApplicationDao.getApplicationByJobAndUser(jobId, freelancerId)
                    if (application == null || application.status != "shortlisted") {
                        android.util.Log.e("JobAttendanceActivity", "Invalid access: ${application?.status ?: "no application"}")
                        return@withContext Triple(false, emptyList<JobAttendanceEntity>(), frozenCompanyName)
                    }

                    android.util.Log.d("JobAttendanceActivity", "✅ Job and application validation passed")


                    val records = jobAttendanceDao.getAttendanceByJobAndFreelancer(jobId, freelancerId)
                    android.util.Log.d("JobAttendanceActivity", "Found ${records.size} attendance records")


                    val attendanceManager = com.uilover.project196.Utils.AttendanceManager.getInstance(this@JobAttendanceActivity)
                    attendanceManager.ensureFreelancerAttendance(jobId, freelancerId)


                    val filtered = filterTodayAndTomorrowRecords(records)

                    android.util.Log.d("JobAttendanceActivity", "Found ${records.size} total, filtered to ${filtered.size}")

                    Triple(true, filtered, frozenCompanyName)
                }


                if (!isValid) {

                    if (frozenCompany.isNotEmpty()) {
                        Toast.makeText(this@JobAttendanceActivity, "Your job was frozen by company $frozenCompany", Toast.LENGTH_LONG).show()
                    } else {
                        when {
                            filteredRecords.isEmpty() -> {
                                Toast.makeText(this@JobAttendanceActivity, "You haven't applied to this job", Toast.LENGTH_SHORT).show()
                            }
                            else -> {
                                Toast.makeText(this@JobAttendanceActivity, "You need to be shortlisted by the business owner first", Toast.LENGTH_SHORT).show()
                            }
                        }
                    }
                    finish()
                    return@launch
                }


                updateAttendanceUI(filteredRecords)

            } catch (e: Exception) {
                android.util.Log.e("JobAttendanceActivity", "Error loading attendance records", e)
                Toast.makeText(this@JobAttendanceActivity, "Error loading attendance: ${e.message}", Toast.LENGTH_SHORT).show()
            } finally {
                binding.loadingIndicator.visibility = View.GONE
            }
        }
    }


    private fun updateAttendanceUI(filteredRecords: List<JobAttendanceEntity>) {

        attendanceList.clear()
        attendanceList.addAll(filteredRecords.sortedBy { it.attendanceDate })


        attendanceAdapter.notifyDataSetChanged()

        android.util.Log.d("JobAttendanceActivity", "✅ UI updated with ${attendanceList.size} records")


        val message = if (attendanceList.isEmpty()) {
            "No attendance records available for today/tomorrow"
        } else {
            "Loaded ${attendanceList.size} attendance records"
        }

        Toast.makeText(this, message,
            if (attendanceList.isEmpty()) Toast.LENGTH_LONG else Toast.LENGTH_SHORT).show()
    }

    private fun filterTodayAndTomorrowRecords(records: List<JobAttendanceEntity>): List<JobAttendanceEntity> {
        val dateFormat = SimpleDateFormat("yyyy-MM-dd", Locale.getDefault())
        val calendar = Calendar.getInstance()

        val today = dateFormat.format(calendar.time)
        calendar.add(Calendar.DAY_OF_MONTH, 1)
        val tomorrow = dateFormat.format(calendar.time)

        android.util.Log.d("JobAttendanceActivity", "Filtering records for today ($today) and tomorrow ($tomorrow)")

        val filtered = records.filter { record ->
            record.attendanceDate == today || record.attendanceDate == tomorrow
        }

        android.util.Log.d("JobAttendanceActivity", "Filtered ${records.size} records down to ${filtered.size} (today + tomorrow only)")
        return filtered
    }

    private fun isDateToday(dateString: String): Boolean {
        val dateFormat = SimpleDateFormat("yyyy-MM-dd", Locale.getDefault())
        val today = dateFormat.format(Date())
        return dateString == today
    }

    private fun handleCheckIn(attendance: JobAttendanceEntity) {
        val freelancerId = UserSession.getUserId()
        if (freelancerId == null) {
            Toast.makeText(this, "User not logged in", Toast.LENGTH_SHORT).show()
            return
        }

        lifecycleScope.launch {
            try {

                val chatRepository = com.uilover.project196.Repository.ChatRepository.getInstance(this@JobAttendanceActivity)
                val isVerified = chatRepository.hasAcceptedVerification(freelancerId)

                if (!isVerified) {
                    android.util.Log.w("JobAttendanceActivity", "❌ CHECK-IN BLOCKED: No accepted verification")


                    val attendanceManager = com.uilover.project196.Utils.AttendanceManager.getInstance(this@JobAttendanceActivity)
                    attendanceManager.debugVerificationStatus(freelancerId, "JobAttendanceActivity.handleCheckIn")

                    Toast.makeText(this@JobAttendanceActivity, "Verification required. Please send a verification request to the business owner and wait for approval before checking in.", Toast.LENGTH_LONG).show()
                    return@launch
                }

                android.util.Log.d("JobAttendanceActivity", "✅ Verification check passed for check-in")


                if (isCurrentUserFrozenByCompany()) {
                    val frozenCompany = getFrozenByCompanyName()
                    Toast.makeText(this@JobAttendanceActivity, "You have been frozen by $frozenCompany. You cannot perform attendance actions for their jobs.", Toast.LENGTH_LONG).show()
                    return@launch
                }


                if (isCurrentUserBlocked()) {
                    Toast.makeText(this@JobAttendanceActivity, "Your account has been blocked. You cannot perform attendance actions.", Toast.LENGTH_LONG).show()
                    return@launch
                }


                val prefs = getSharedPreferences("app_state", MODE_PRIVATE)
                if (prefs.getBoolean("attendance_disabled", false)) {
                    Toast.makeText(this@JobAttendanceActivity, "Attendance functionality is currently disabled. Please wait for re-verification.", Toast.LENGTH_LONG).show()
                    return@launch
                }


                if (!isDateToday(attendance.attendanceDate)) {
                    Toast.makeText(this@JobAttendanceActivity, "You can only check in on the current day (${attendance.attendanceDate})", Toast.LENGTH_SHORT).show()
                    return@launch
                }


                val checkInButton = findViewById<android.widget.Button>(R.id.checkInButton)
                checkInButton?.isEnabled = false

                try {
                    val success = withContext(Dispatchers.IO) {
                        freelancerJobRepository.checkInToJob(jobId, freelancerId)
                    }

                    if (success) {

                        updateAttendanceItemCheckIn(attendance)
                        Toast.makeText(this@JobAttendanceActivity, "Successfully checked in!", Toast.LENGTH_SHORT).show()


                        val intent = Intent("com.uilover.project196.ATTENDANCE_UPDATED")
                        intent.putExtra("freelancer_id", freelancerId)
                        intent.putExtra("job_id", jobId)
                        intent.putExtra("action", "check_in")
                        sendBroadcast(intent)
                        android.util.Log.d("JobAttendanceActivity", "✅ Sent attendance update broadcast for check-in")
                    } else {
                        Toast.makeText(this@JobAttendanceActivity, "Failed to check in. Please try again.", Toast.LENGTH_SHORT).show()
                    }

                } catch (e: Exception) {
                    android.util.Log.e("JobAttendanceActivity", "Error checking in", e)
                    Toast.makeText(this@JobAttendanceActivity, "Error checking in: ${e.message}", Toast.LENGTH_SHORT).show()
                } finally {

                    checkInButton?.isEnabled = true
                }
            } catch (e: Exception) {
                android.util.Log.e("JobAttendanceActivity", "Error in handleCheckIn", e)
                Toast.makeText(this@JobAttendanceActivity, "Error: ${e.message}", Toast.LENGTH_SHORT).show()
            }
        }
    }

    private fun handleCheckOut(attendance: JobAttendanceEntity) {
        lifecycleScope.launch {
            try {
                val freelancerId = UserSession.getUserId()
                if (freelancerId == null) {
                    Toast.makeText(this@JobAttendanceActivity, "User not logged in", Toast.LENGTH_SHORT).show()
                    return@launch
                }


                val chatRepository = com.uilover.project196.Repository.ChatRepository.getInstance(this@JobAttendanceActivity)
                val isVerified = chatRepository.hasAcceptedVerification(freelancerId)

                if (!isVerified) {
                    android.util.Log.w("JobAttendanceActivity", "❌ CHECK-OUT BLOCKED: No accepted verification")


                    val attendanceManager = com.uilover.project196.Utils.AttendanceManager.getInstance(this@JobAttendanceActivity)
                    attendanceManager.debugVerificationStatus(freelancerId, "JobAttendanceActivity.handleCheckOut")

                    Toast.makeText(this@JobAttendanceActivity, "Verification required. Please send a verification request to the business owner and wait for approval before checking out.", Toast.LENGTH_LONG).show()
                    return@launch
                }

                android.util.Log.d("JobAttendanceActivity", "✅ Verification check passed for check-out")


                if (isCurrentUserFrozenByCompany()) {
                    val frozenCompany = getFrozenByCompanyName()
                    Toast.makeText(this@JobAttendanceActivity, "You have been frozen by $frozenCompany. You cannot perform attendance actions for their jobs.", Toast.LENGTH_LONG).show()
                    return@launch
                }


                if (isCurrentUserBlocked()) {
                    Toast.makeText(this@JobAttendanceActivity, "Your account has been blocked. You cannot perform attendance actions.", Toast.LENGTH_LONG).show()
                    return@launch
                }


                val prefs = getSharedPreferences("app_state", MODE_PRIVATE)
                if (prefs.getBoolean("attendance_disabled", false)) {
                    Toast.makeText(this@JobAttendanceActivity, "Attendance functionality is currently disabled. Please wait for re-verification.", Toast.LENGTH_LONG).show()
                    return@launch
                }

                if (!isDateToday(attendance.attendanceDate)) {
                    Toast.makeText(this@JobAttendanceActivity, "You can only check out on the current day", Toast.LENGTH_SHORT).show()
                    return@launch
                }

                if (attendance.checkInTime == null) {
                    Toast.makeText(this@JobAttendanceActivity, "You must check in first", Toast.LENGTH_SHORT).show()
                    return@launch
                }

                showProgressReportDialog(attendance)
            } catch (e: Exception) {
                android.util.Log.e("JobAttendanceActivity", "Error in handleCheckOut", e)
                Toast.makeText(this@JobAttendanceActivity, "Error: ${e.message}", Toast.LENGTH_SHORT).show()
            }
        }
    }

    private fun showProgressReportDialog(attendance: JobAttendanceEntity) {

        progressReportViewModel.initializeForm(jobTitle, companyName)


        val dialog = Dialog(this)
        val dialogBinding = DialogProgressReportBinding.inflate(layoutInflater)
        dialog.setContentView(dialogBinding.root)


        setupProgressDialogReactiveBinding(dialogBinding, progressReportViewModel)
        setupProgressDialogLiveDataObservers(dialog, attendance)


        dialogBinding.jobTitle.text = jobTitle
        dialogBinding.companyName.text = companyName


        dialogBinding.cancelButton.setOnClickListener {
            dialog.dismiss()
        }

        dialogBinding.submitButton.setOnClickListener {
            progressReportViewModel.submitProgressReport(jobId, attendance)
        }

        dialog.show()
    }




    private fun setupProgressDialogReactiveBinding(dialogBinding: DialogProgressReportBinding, viewModel: ProgressReportViewModel) {


        dialogBinding.progressInput.addTextChangedListener(object : android.text.TextWatcher {
            override fun beforeTextChanged(s: CharSequence?, start: Int, count: Int, after: Int) {}
            override fun onTextChanged(s: CharSequence?, start: Int, before: Int, count: Int) {
                viewModel.validateProgress(s.toString())

                val text = s.toString().trim()
                when {
                    text.isEmpty() -> {
                        dialogBinding.progressInput.error = "Progress report is required"
                    }
                    text.length < 10 -> {
                        dialogBinding.progressInput.error = "Progress report must be at least 10 characters"
                    }
                    text.length > 1000 -> {
                        dialogBinding.progressInput.error = "Progress report cannot exceed 1000 characters"
                    }
                    else -> {
                        dialogBinding.progressInput.error = null
                    }
                }


                val isValid = text.length >= 10 && text.length <= 1000
                dialogBinding.submitButton.isEnabled = isValid
            }
            override fun afterTextChanged(s: android.text.Editable?) {}
        })



    }




    private fun setupProgressDialogLiveDataObservers(dialog: Dialog, attendance: JobAttendanceEntity) {

        progressReportViewModel.checkOutResult.observe(this) { success ->
            success?.let {
                if (it) {


                    val progressText = "Progress report submitted successfully"
                    updateAttendanceItemCheckOut(attendance, progressText)
                }
                dialog.dismiss()
                progressReportViewModel.onCheckOutResultHandled()
            }
        }

        progressReportViewModel.showSuccessMessage.observe(this) { message ->
            message?.let {
                Toast.makeText(this, it, Toast.LENGTH_LONG).show()


                val freelancerId = UserSession.getUserId()
                if (freelancerId != null && it.contains("checked out")) {
                    val intent = Intent("com.uilover.project196.ATTENDANCE_UPDATED")
                    intent.putExtra("freelancer_id", freelancerId)
                    intent.putExtra("job_id", jobId)
                    intent.putExtra("action", "check_out")
                    sendBroadcast(intent)
                    android.util.Log.d("JobAttendanceActivity", "✅ Sent attendance update broadcast for check-out")
                }

                progressReportViewModel.onSuccessMessageShown()
            }
        }

        progressReportViewModel.showErrorMessage.observe(this) { message ->
            message?.let {
                Toast.makeText(this, it, Toast.LENGTH_SHORT).show()
                progressReportViewModel.onErrorMessageShown()
            }
        }
    }




    private fun updateAttendanceItemCheckIn(attendance: JobAttendanceEntity) {
        val index = attendanceList.indexOfFirst {
            it.jobId == attendance.jobId &&
            it.freelancerId == attendance.freelancerId &&
            it.attendanceDate == attendance.attendanceDate
        }

        if (index != -1) {
            attendanceList[index] = attendanceList[index].copy(
                checkInTime = System.currentTimeMillis()
            )
            attendanceAdapter.notifyItemChanged(index)
            android.util.Log.d("JobAttendanceActivity", "✅ Updated check-in for item at index $index")
        } else {

            android.util.Log.w("JobAttendanceActivity", "Attendance item not found, performing full reload")
            loadAttendanceRecords()
        }
    }


    private fun updateAttendanceItemCheckOut(attendance: JobAttendanceEntity, progressReport: String) {
        val index = attendanceList.indexOfFirst {
            it.jobId == attendance.jobId &&
            it.freelancerId == attendance.freelancerId &&
            it.attendanceDate == attendance.attendanceDate
        }

        if (index != -1) {
            attendanceList[index] = attendanceList[index].copy(
                checkOutTime = System.currentTimeMillis(),
                progressReport = progressReport
            )
            attendanceAdapter.notifyItemChanged(index)
            android.util.Log.d("JobAttendanceActivity", "✅ Updated check-out for item at index $index")
        } else {

            android.util.Log.w("JobAttendanceActivity", "Attendance item not found, performing full reload")
            loadAttendanceRecords()
        }
    }

    private fun getCurrentDateString(): String {
        val sdf = SimpleDateFormat("EEEE, dd MMMM yyyy", Locale.getDefault())
        return sdf.format(Date())
    }

    private suspend fun isCurrentUserFrozenByCompany(): Boolean {
        return withContext(Dispatchers.IO) {
            try {
                val userId = UserSession.getUserId() ?: return@withContext false


                val database = AppDatabase.getDatabase(this@JobAttendanceActivity)
                val job = database.jobDao().getJobById(jobId)
                val businessOwnerId = job?.ownerId ?: return@withContext false


                val prefs = getSharedPreferences("user_status", MODE_PRIVATE)
                val isFrozen = prefs.getBoolean("is_frozen_by_${businessOwnerId}_$userId", false)

                android.util.Log.d("JobAttendanceActivity", "Checking if user $userId is frozen by company $businessOwnerId: $isFrozen")
                isFrozen
            } catch (e: Exception) {
                android.util.Log.e("JobAttendanceActivity", "Error checking company freeze status", e)
                false
            }
        }
    }

    private fun getFrozenByCompanyName(): String {
        val userId = UserSession.getUserId() ?: return "Unknown Company"
        val prefs = getSharedPreferences("user_status", MODE_PRIVATE)
        return prefs.getString("frozen_by_company_$userId", "Unknown Company") ?: "Unknown Company"
    }

    private fun isCurrentUserBlocked(): Boolean {
        return try {
            val userId = UserSession.getUserId()
            if (userId == null) {
                android.util.Log.w("JobAttendanceActivity", "No user ID available")
                return true
            }


            val prefs = getSharedPreferences("user_status", MODE_PRIVATE)
            val lastChecked = prefs.getLong("last_checked_$userId", 0)
            val currentTime = System.currentTimeMillis()


            if (currentTime - lastChecked > 30000) {

                lifecycleScope.launch {
                    updateUserBlockedStatusInBackground(userId)
                }
            }


            val isBlocked = prefs.getBoolean("is_blocked_$userId", false)
            android.util.Log.d("JobAttendanceActivity", "User $userId blocked status: $isBlocked (cached)")
            isBlocked

        } catch (e: Exception) {
            android.util.Log.e("JobAttendanceActivity", "Error checking user blocked status", e)
            true
        }
    }

    private suspend fun updateUserBlockedStatusInBackground(userId: String) {
        try {
            val database = com.uilover.project196.Database.AppDatabase.getDatabase(this)
            val userDao = database.userDao()
            val user = userDao.getUserById(userId)

            val isBlocked = user?.isActive == false
            android.util.Log.d("JobAttendanceActivity", "Updated user $userId blocked status from database: $isBlocked")


            val prefs = getSharedPreferences("user_status", MODE_PRIVATE)
            prefs.edit()
                .putBoolean("is_blocked_$userId", isBlocked)
                .putLong("last_checked_$userId", System.currentTimeMillis())
                .apply()


            if (isBlocked) {
                runOnUiThread {
                    Toast.makeText(this@JobAttendanceActivity, "Your account has been blocked by the business owner.", Toast.LENGTH_LONG).show()

                    loadAttendanceRecords()
                }
            }

        } catch (e: Exception) {
            android.util.Log.e("JobAttendanceActivity", "Error updating user blocked status", e)
        }
    }
}