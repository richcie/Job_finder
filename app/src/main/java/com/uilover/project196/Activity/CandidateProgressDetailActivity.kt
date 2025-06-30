package com.uilover.project196.Activity

import android.os.Bundle
import android.view.View
import android.widget.Toast
import androidx.appcompat.app.AppCompatActivity
import androidx.lifecycle.lifecycleScope
import androidx.recyclerview.widget.LinearLayoutManager
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import com.uilover.project196.Adapter.AttendanceDetailAdapter
import com.uilover.project196.Database.AppDatabase
import com.uilover.project196.Model.JobAttendanceEntity
import com.uilover.project196.R
import com.uilover.project196.Utils.UserSession
import com.uilover.project196.databinding.ActivityCandidateProgressDetailBinding
import java.text.SimpleDateFormat
import java.util.*

// KRITERIA: Multiple Activity (7/8) - Activity untuk detail progress kandidat
// KRITERIA WAJIB: Multiple Activity (7/8) - Activity untuk detail progress kandidat
// KRITERIA KOMPLEKSITAS: Analytics dan progress tracking
class CandidateProgressDetailActivity : AppCompatActivity() {
    private lateinit var binding: ActivityCandidateProgressDetailBinding
    private lateinit var attendanceAdapter: AttendanceDetailAdapter
    private var attendanceList = mutableListOf<JobAttendanceEntity>()

    private var candidateId: String = ""
    private var candidateName: String = ""
    private var jobId: Int = 0
    private var jobTitle: String = ""
    private var applicationId: Int = 0
    private var candidateRole: String = ""

    override fun onCreate(savedInstanceState: Bundle?) {
        android.util.Log.d("CandidateProgressDetailActivity", "=== ACTIVITY CREATION START ===")
        super.onCreate(savedInstanceState)

        try {
            binding = ActivityCandidateProgressDetailBinding.inflate(layoutInflater)
            setContentView(binding.root)
            android.util.Log.d("CandidateProgressDetailActivity", "✅ Layout inflated and set successfully")


            candidateId = intent.getStringExtra("CANDIDATE_ID") ?: ""
            candidateName = intent.getStringExtra("CANDIDATE_NAME") ?: ""
            jobId = intent.getIntExtra("JOB_ID", 0)
            jobTitle = intent.getStringExtra("JOB_TITLE") ?: ""
            applicationId = intent.getIntExtra("APPLICATION_ID", 0)
            candidateRole = intent.getStringExtra("CANDIDATE_ROLE") ?: ""

            android.util.Log.d("CandidateProgressDetailActivity", "✅ Intent data retrieved: Candidate=$candidateName, Job ID=$jobId, Title='$jobTitle', Role='$candidateRole'")

            setupUI()
            android.util.Log.d("CandidateProgressDetailActivity", "✅ UI setup completed")

            setupRecyclerView()
            android.util.Log.d("CandidateProgressDetailActivity", "✅ RecyclerView setup completed")

            loadAttendanceData()
            android.util.Log.d("CandidateProgressDetailActivity", "✅ Loading attendance records started")

        } catch (e: Exception) {
            android.util.Log.e("CandidateProgressDetailActivity", "❌ Error during onCreate", e)
            Toast.makeText(this, "Error initializing activity: ${e.message}", Toast.LENGTH_LONG).show()
            finish()
        }
    }

    private fun setupUI() {


        binding.candidateName.text = candidateName


        if (candidateRole.isNotEmpty()) {
            android.util.Log.d("CandidateProgressDetailActivity", "Using passed candidate role: '$candidateRole'")
            binding.jobTitle.text = candidateRole
        } else {
            android.util.Log.d("CandidateProgressDetailActivity", "No candidate role passed, fetching from database...")

            lifecycleScope.launch {
                try {
                    val database = AppDatabase.getDatabase(this@CandidateProgressDetailActivity)
                    val userDao = database.userDao()
                    val candidate = userDao.getUserById(candidateId)

                    val roleFromDb = candidate?.title ?: "Full Stack Developer"
                    android.util.Log.d("CandidateProgressDetailActivity", "Candidate role from database: '$roleFromDb'")


                    binding.jobTitle.text = roleFromDb
                } catch (e: Exception) {
                    android.util.Log.e("CandidateProgressDetailActivity", "Error fetching candidate from database", e)

                    binding.jobTitle.text = "Full Stack Developer"
                }
            }
        }

        binding.currentDate.text = getCurrentDateString()


        binding.backButton.setOnClickListener {
            finish()
        }


        updateFreezeButtonState()
        binding.blockFreelancerButton.setOnClickListener {
            val currentBusinessOwnerId = UserSession.getUserId() ?: return@setOnClickListener
            val prefs = getSharedPreferences("user_status", MODE_PRIVATE)
            val isFrozen = prefs.getBoolean("is_frozen_by_${currentBusinessOwnerId}_$candidateId", false)

            if (isFrozen) {
                showUnfreezeFreelancerDialog()
            } else {
                showFreezeFreelancerDialog()
            }
        }
    }

    private fun setupRecyclerView() {
        binding.recyclerViewAttendance.layoutManager = LinearLayoutManager(this)

        attendanceAdapter = AttendanceDetailAdapter(attendanceList) { _, _ ->

            Toast.makeText(this, "Only freelancers can check in/out", Toast.LENGTH_SHORT).show()
        }
        binding.recyclerViewAttendance.adapter = attendanceAdapter
    }

    private fun loadAttendanceData() {
        android.util.Log.d("CandidateProgressDetailActivity", "=== LOADING ATTENDANCE RECORDS ===")
        android.util.Log.d("CandidateProgressDetailActivity", "Job ID: $jobId, Candidate ID: $candidateId")

        binding.loadingIndicator.visibility = View.VISIBLE

        lifecycleScope.launch {
            try {

                val filteredRecords = withContext(Dispatchers.IO) {
                    val database = AppDatabase.getDatabase(this@CandidateProgressDetailActivity)


                    val attendanceRecords = database.jobAttendanceDao().getAttendanceByJobAndFreelancer(jobId, candidateId)


                    val filtered = filterTodayAndTomorrowRecords(attendanceRecords)

                    android.util.Log.d("CandidateProgressDetailActivity", "Found ${attendanceRecords.size} total, filtered to ${filtered.size}")

                    filtered
                }


                updateAttendanceUI(filteredRecords)

            } catch (e: Exception) {
                android.util.Log.e("CandidateProgressDetailActivity", "Error loading attendance records", e)
                Toast.makeText(this@CandidateProgressDetailActivity, "Error loading attendance: ${e.message}", Toast.LENGTH_SHORT).show()
                showEmptyState()
            } finally {
                binding.loadingIndicator.visibility = View.GONE
            }
        }
    }

    private fun filterTodayAndTomorrowRecords(records: List<JobAttendanceEntity>): List<JobAttendanceEntity> {
        val dateFormat = SimpleDateFormat("yyyy-MM-dd", Locale.getDefault())
        val calendar = Calendar.getInstance()

        val today = dateFormat.format(calendar.time)
        calendar.add(Calendar.DAY_OF_MONTH, 1)
        val tomorrow = dateFormat.format(calendar.time)

        android.util.Log.d("CandidateProgressDetailActivity", "Filtering records for today ($today) and tomorrow ($tomorrow)")

        val filtered = records.filter { record ->
            record.attendanceDate == today || record.attendanceDate == tomorrow
        }

        android.util.Log.d("CandidateProgressDetailActivity", "Filtered ${records.size} records down to ${filtered.size} (today + tomorrow only)")
        return filtered
    }


    private fun updateAttendanceUI(filteredRecords: List<JobAttendanceEntity>) {

        attendanceList.clear()
        attendanceList.addAll(filteredRecords.sortedBy { it.attendanceDate })


        attendanceAdapter.notifyDataSetChanged()

        android.util.Log.d("CandidateProgressDetailActivity", "✅ UI updated with ${attendanceList.size} records")

        if (attendanceList.isEmpty()) {
            showEmptyState()
            Toast.makeText(this, "No attendance records available for today/tomorrow", Toast.LENGTH_LONG).show()
        } else {
            showAttendanceData()
            Toast.makeText(this, "Loaded ${attendanceList.size} attendance records", Toast.LENGTH_SHORT).show()
        }
    }

    private fun showAttendanceData() {
        binding.recyclerViewAttendance.visibility = View.VISIBLE
        binding.emptyStateLayout.visibility = View.GONE
    }

    private fun showEmptyState() {
        binding.recyclerViewAttendance.visibility = View.GONE
        binding.emptyStateLayout.visibility = View.VISIBLE
    }

    private fun getCurrentDateString(): String {
        val sdf = SimpleDateFormat("EEEE, dd MMMM yyyy", Locale.getDefault())
        return sdf.format(Date())
    }

    private fun updateFreezeButtonState() {
        val currentBusinessOwnerId = UserSession.getUserId() ?: return
        val prefs = getSharedPreferences("user_status", MODE_PRIVATE)
        val isFrozen = prefs.getBoolean("is_frozen_by_${currentBusinessOwnerId}_$candidateId", false)

        if (isFrozen) {
            binding.blockFreelancerButton.text = "UNFREEZE FREELANCER"
            binding.blockFreelancerButton.setBackgroundColor(getColor(R.color.green))
            binding.blockFreelancerButton.icon = getDrawable(R.drawable.ic_bookmark_filled)
        } else {
            binding.blockFreelancerButton.text = "FREEZE FREELANCER"
            binding.blockFreelancerButton.setBackgroundColor(getColor(R.color.red))
            binding.blockFreelancerButton.icon = getDrawable(R.drawable.ic_block)
        }
    }

    private fun showFreezeFreelancerDialog() {
        val builder = androidx.appcompat.app.AlertDialog.Builder(this)
        builder.setTitle("Freeze Freelancer")
        builder.setMessage("Are you sure you want to freeze $candidateName? This will:\n\n• Disable their check-in/check-out actions for YOUR company's jobs\n• Keep all jobs visible in their Jobs tab\n• Show clear message that YOUR company froze them\n• Preserve all historical attendance data\n• Allow you to unfreeze them later\n\nThey can still work for other companies but not yours.")
        builder.setIcon(R.drawable.ic_block)

        builder.setPositiveButton("FREEZE") { _, _ ->
            freezeFreelancer()
        }

        builder.setNegativeButton("CANCEL") { dialog, _ ->
            dialog.dismiss()
        }

        val dialog = builder.create()
        dialog.show()


        dialog.getButton(androidx.appcompat.app.AlertDialog.BUTTON_POSITIVE)?.setTextColor(getColor(R.color.red))
        dialog.getButton(androidx.appcompat.app.AlertDialog.BUTTON_NEGATIVE)?.setTextColor(getColor(R.color.darkGrey))
    }

    private fun showUnfreezeFreelancerDialog() {
        val builder = androidx.appcompat.app.AlertDialog.Builder(this)
        builder.setTitle("Unfreeze Freelancer")
        builder.setMessage("Are you sure you want to unfreeze $candidateName? This will:\n\n• Re-enable their check-in/check-out actions for YOUR company's jobs\n• Allow them to work on your projects again\n• Restore full access to attendance functionality\n• Keep all historical data intact\n\nThey will be able to clock in/out for your jobs again.")
        builder.setIcon(R.drawable.ic_bookmark_filled)

        builder.setPositiveButton("UNFREEZE") { _, _ ->
            unfreezeFreelancer()
        }

        builder.setNegativeButton("CANCEL") { dialog, _ ->
            dialog.dismiss()
        }

        val dialog = builder.create()
        dialog.show()


        dialog.getButton(androidx.appcompat.app.AlertDialog.BUTTON_POSITIVE)?.setTextColor(getColor(R.color.green))
        dialog.getButton(androidx.appcompat.app.AlertDialog.BUTTON_NEGATIVE)?.setTextColor(getColor(R.color.darkGrey))
    }

    private fun freezeFreelancer() {
        android.util.Log.d("CandidateProgressDetailActivity", "=== FREEZING FREELANCER ===")
        android.util.Log.d("CandidateProgressDetailActivity", "Freezing freelancer: $candidateName (ID: $candidateId)")

        lifecycleScope.launch {
            try {
                val database = AppDatabase.getDatabase(this@CandidateProgressDetailActivity)
                val jobApplicationDao = database.jobApplicationDao()
                val currentBusinessOwnerId = UserSession.getUserId() ?: return@launch
                val currentCompanyName = UserSession.getCompanyName() ?: "Unknown Company"


                android.util.Log.d("CandidateProgressDetailActivity", "❌ NOT marking user as globally inactive - using company-specific freeze")


                val allApplications = jobApplicationDao.getAllApplicationsByUserId(candidateId)
                val businessOwnerJobs = database.jobDao().getJobsByOwnerId(currentBusinessOwnerId)
                val businessOwnerJobIds = businessOwnerJobs.map { it.id }.toSet()

                var frozenApplications = 0
                allApplications.forEach { application ->
                    if (businessOwnerJobIds.contains(application.jobId)) {
                        val updatedApplication = application.copy(status = "frozen")
                        jobApplicationDao.updateJobApplication(updatedApplication)
                        frozenApplications++
                        android.util.Log.d("CandidateProgressDetailActivity", "  ✅ Froze application ${application.id} for job ${application.jobId}")
                    }
                }
                android.util.Log.d("CandidateProgressDetailActivity", "✅ Froze $frozenApplications applications for company: $currentCompanyName")


                android.util.Log.d("CandidateProgressDetailActivity", "✅ Historical attendance records preserved")


                val prefs = getSharedPreferences("user_status", MODE_PRIVATE)
                prefs.edit()
                    .putBoolean("is_frozen_by_${currentBusinessOwnerId}_$candidateId", true)
                    .putString("frozen_by_company_$candidateId", currentCompanyName)
                    .putLong("frozen_at_$candidateId", System.currentTimeMillis())
                    .putBoolean("is_blocked_$candidateId", true)
                    .apply()
                android.util.Log.d("CandidateProgressDetailActivity", "✅ Stored company-specific freeze info: $currentCompanyName froze $candidateName")


                val intent = android.content.Intent("com.uilover.project196.FREELANCER_BLOCKED")
                intent.putExtra("freelancer_id", candidateId)
                intent.putExtra("freelancer_name", candidateName)
                intent.putExtra("action_type", "frozen")
                sendBroadcast(intent)


                Toast.makeText(this@CandidateProgressDetailActivity, "$candidateName has been frozen successfully. They will remain visible but cannot check in/out.", Toast.LENGTH_LONG).show()

                android.util.Log.d("CandidateProgressDetailActivity", "✅ Freelancer freezing completed successfully")


                finish()

            } catch (e: Exception) {
                android.util.Log.e("CandidateProgressDetailActivity", "❌ Error freezing freelancer", e)
                Toast.makeText(this@CandidateProgressDetailActivity, "Error freezing freelancer: ${e.message}", Toast.LENGTH_LONG).show()
            }
        }
    }

    private fun unfreezeFreelancer() {
        android.util.Log.d("CandidateProgressDetailActivity", "=== UNFREEZING FREELANCER ===")
        android.util.Log.d("CandidateProgressDetailActivity", "Unfreezing freelancer: $candidateName (ID: $candidateId)")

        lifecycleScope.launch {
            try {
                val database = AppDatabase.getDatabase(this@CandidateProgressDetailActivity)
                val jobApplicationDao = database.jobApplicationDao()
                val currentBusinessOwnerId = UserSession.getUserId() ?: return@launch
                val currentCompanyName = UserSession.getCompanyName() ?: "Unknown Company"


                val allApplications = jobApplicationDao.getAllApplicationsByUserId(candidateId)
                val businessOwnerJobs = database.jobDao().getJobsByOwnerId(currentBusinessOwnerId)
                val businessOwnerJobIds = businessOwnerJobs.map { it.id }.toSet()

                var unfrozenApplications = 0
                allApplications.forEach { application ->
                    if (businessOwnerJobIds.contains(application.jobId) && application.status == "frozen") {
                        val updatedApplication = application.copy(status = "shortlisted")
                        jobApplicationDao.updateJobApplication(updatedApplication)
                        unfrozenApplications++
                        android.util.Log.d("CandidateProgressDetailActivity", "  ✅ Unfroze application ${application.id} for job ${application.jobId}")
                    }
                }
                android.util.Log.d("CandidateProgressDetailActivity", "✅ Unfroze $unfrozenApplications applications for company: $currentCompanyName")


                val prefs = getSharedPreferences("user_status", MODE_PRIVATE)
                prefs.edit()
                    .remove("is_frozen_by_${currentBusinessOwnerId}_$candidateId")
                    .remove("frozen_by_company_$candidateId")
                    .remove("frozen_at_$candidateId")
                    .putBoolean("is_blocked_$candidateId", false)
                    .apply()
                android.util.Log.d("CandidateProgressDetailActivity", "✅ Removed company-specific freeze info for $currentCompanyName")


                val intent = android.content.Intent("com.uilover.project196.FREELANCER_BLOCKED")
                intent.putExtra("freelancer_id", candidateId)
                intent.putExtra("freelancer_name", candidateName)
                intent.putExtra("action_type", "unfrozen")
                sendBroadcast(intent)


                updateFreezeButtonState()


                Toast.makeText(this@CandidateProgressDetailActivity, "$candidateName has been unfrozen successfully. They can now check in/out for your jobs.", Toast.LENGTH_LONG).show()

                android.util.Log.d("CandidateProgressDetailActivity", "✅ Freelancer unfreezing completed successfully")

            } catch (e: Exception) {
                android.util.Log.e("CandidateProgressDetailActivity", "❌ Error unfreezing freelancer", e)
                Toast.makeText(this@CandidateProgressDetailActivity, "Error unfreezing freelancer: ${e.message}", Toast.LENGTH_LONG).show()
            }
        }
    }
}