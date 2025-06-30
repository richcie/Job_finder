package com.uilover.project196.Fragment

import android.app.AlertDialog
import android.content.Intent
import android.os.Bundle
import androidx.fragment.app.Fragment
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.Toast
import androidx.lifecycle.ViewModelProvider
import androidx.lifecycle.lifecycleScope
import androidx.recyclerview.widget.LinearLayoutManager
import com.uilover.project196.R
import com.uilover.project196.Utils.UserSession
import com.uilover.project196.ViewModel.MainViewModel
import com.uilover.project196.Repository.UserRepository
import com.uilover.project196.Model.CandidateModel
import com.uilover.project196.databinding.FragmentCandidatesBinding
import kotlinx.coroutines.launch

// KRITERIA WAJIB: Multiple Fragment (13/16) - Fragment daftar kandidat
class CandidatesFragment : Fragment() {

    private var _binding: FragmentCandidatesBinding? = null
    private val binding get() = _binding!!
    private lateinit var mainViewModel: MainViewModel
    private lateinit var userRepository: UserRepository

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
    }

    override fun onCreateView(
        inflater: LayoutInflater, container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View {
        _binding = FragmentCandidatesBinding.inflate(inflater, container, false)
        return binding.root
    }


    private val newCandidateReceiver = object : android.content.BroadcastReceiver() {
        override fun onReceive(context: android.content.Context?, intent: android.content.Intent?) {
            android.util.Log.d("CandidatesFragment", "=== NEW CANDIDATE REGISTERED BROADCAST RECEIVED ===")
            when (intent?.action) {
                "com.uilover.project196.NEW_CANDIDATE_REGISTERED" -> {
                    val freelancerId = intent.getStringExtra("freelancer_id")
                    val jobId = intent.getIntExtra("job_id", 0)
                    val applicationId = intent.getIntExtra("application_id", 0)
                    android.util.Log.d("CandidatesFragment", "New candidate registered: freelancer=$freelancerId, job=$jobId, application=$applicationId")


                    if (UserSession.getUserRole() == UserSession.ROLE_BUSINESS_OWNER) {
                        android.util.Log.d("CandidatesFragment", "Refreshing candidates list for business owner...")
                        loadRealCandidates()
                    }
                }
            }
        }
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)


        UserSession.init(requireContext())


        mainViewModel = ViewModelProvider(requireActivity())[MainViewModel::class.java]
        mainViewModel.initializeDatabase(requireContext())
        userRepository = UserRepository.getInstance(requireContext())


        lifecycleScope.launch {
            userRepository.initializeSampleData()
        }


        val intentFilter = android.content.IntentFilter().apply {
            addAction("com.uilover.project196.NEW_CANDIDATE_REGISTERED")
        }
        if (android.os.Build.VERSION.SDK_INT >= android.os.Build.VERSION_CODES.TIRAMISU) {
            requireContext().registerReceiver(newCandidateReceiver, intentFilter, android.content.Context.RECEIVER_NOT_EXPORTED)
        } else {
            requireContext().registerReceiver(newCandidateReceiver, intentFilter)
        }


        if (UserSession.getUserRole() == UserSession.ROLE_BUSINESS_OWNER) {
            setupRealCandidatesView()
        } else {
            showAccessDeniedMessage()
        }
    }

    private fun setupRealCandidatesView() {

        showCandidatesContent()
        loadRealCandidates()
    }

    private fun showCandidatesContent() {
        binding.businessOwnerContent.visibility = View.VISIBLE
        binding.accessDeniedMessage.visibility = View.GONE


        binding.welcomeMessage.text = "Job Candidates"
        binding.userRoleText.text = "Review freelancers who applied to your job postings"


        binding.welcomeMessage.setOnLongClickListener {
            showClearCandidatesConfirmation()
            true
        }


        binding.userRoleText.setOnClickListener {
            checkAndOfferAttendanceReactivation()
        }


        binding.freelancerCandidatesSection.visibility = View.VISIBLE
    }

    private fun loadRealCandidates() {
        val currentUserId = UserSession.getUserId()
        if (currentUserId == null) {
            showEmptyState("Please log in to view candidates")
            return
        }


        lifecycleScope.launch {
            try {
                android.util.Log.d("CandidatesFragment", "=== LOADING REAL CANDIDATES IN UI ===")
                android.util.Log.d("CandidatesFragment", "loadRealCandidates: Business Owner ID: $currentUserId")

                val candidates = userRepository.getCandidatesForOwner(currentUserId)

                if (candidates.isNotEmpty()) {
                    android.util.Log.d("CandidatesFragment", "loadRealCandidates: ✅ RECEIVED ${candidates.size} REAL CANDIDATES")


                    candidates.forEach { candidate ->
                        android.util.Log.d("CandidatesFragment", "loadRealCandidates: ✅ UI CANDIDATE VERIFICATION:")
                        android.util.Log.d("CandidatesFragment", "  - Real Freelancer ID: ${candidate.userId}")
                        android.util.Log.d("CandidatesFragment", "  - Real Name (from UserEntity): ${candidate.name}")
                        android.util.Log.d("CandidatesFragment", "  - Real Email (from UserEntity): ${candidate.email}")
                        android.util.Log.d("CandidatesFragment", "  - Application Skills (job-specific): ${candidate.skills}")
                        android.util.Log.d("CandidatesFragment", "  - Real Application Date: ${candidate.getFormattedAppliedDate()}")
                        android.util.Log.d("CandidatesFragment", "  - Real Application Message: ${candidate.coverLetter.take(50)}...")
                        android.util.Log.d("CandidatesFragment", "  - Database Application ID: ${candidate.applicationId}")
                        android.util.Log.d("CandidatesFragment", "  - Database Job ID: ${candidate.jobId}")
                    }

                    binding.freelancerCandidatesSection.visibility = View.VISIBLE
                    binding.emptyFreelancersMessage.visibility = View.GONE
                    binding.freelancerCandidatesList.visibility = View.VISIBLE


                    displayRealCandidates(candidates)
                    binding.freelancersCount.text = "${candidates.size} Candidate${if (candidates.size != 1) "s" else ""}"

                    android.util.Log.d("CandidatesFragment", "loadRealCandidates: ✅ ALL CANDIDATES ARE REAL DATA - NOT DUMMY!")
                } else {
                    android.util.Log.d("CandidatesFragment", "loadRealCandidates: No real candidates found in database")
                    showEmptyState("No candidates have applied to your jobs yet.")
                }
            } catch (e: Exception) {
                android.util.Log.e("CandidatesFragment", "loadRealCandidates: Error loading real candidates", e)
                showEmptyState("Error loading candidates: ${e.message}")
            }
        }
    }

    private fun showEmptyState(message: String) {
        binding.freelancerCandidatesSection.visibility = View.VISIBLE
        binding.emptyFreelancersMessage.visibility = View.VISIBLE
        binding.freelancerCandidatesList.visibility = View.GONE
        binding.freelancersCount.text = "0 Candidates"
        binding.emptyFreelancersMessage.text = message
    }

    private fun displayRealCandidates(candidates: List<CandidateModel>) {
        android.util.Log.d("CandidatesFragment", "displayRealCandidates: ✅ DISPLAYING ${candidates.size} REAL CANDIDATES IN UI")


        binding.freelancerCandidatesList.removeAllViews()

        candidates.forEach { candidate ->
            android.util.Log.d("CandidatesFragment", "displayRealCandidates: Creating UI card for REAL candidate:")
            android.util.Log.d("CandidatesFragment", "  - Name from real UserEntity: ${candidate.name}")
            android.util.Log.d("CandidatesFragment", "  - Skills from real application: ${candidate.skills}")

            val cardView = LayoutInflater.from(requireContext()).inflate(R.layout.viewholder_freelancer_candidate, binding.freelancerCandidatesList, false)


            cardView.findViewById<android.widget.TextView>(R.id.freelancerName).text = candidate.name


            val roleTextView = cardView.findViewById<android.widget.TextView>(R.id.freelancerRole)
            val profession = candidate.title.ifEmpty { null }

            if (!profession.isNullOrEmpty()) {
                roleTextView.text = profession
                roleTextView.visibility = View.VISIBLE
            } else {
                roleTextView.visibility = View.GONE
            }

            cardView.findViewById<android.widget.TextView>(R.id.freelancerSkills).text = candidate.skills.joinToString(", ").ifEmpty { "No skills specified" }
            cardView.findViewById<android.widget.TextView>(R.id.freelancerDescription).text = candidate.description.ifEmpty { "No description provided" }
            cardView.findViewById<android.widget.TextView>(R.id.appliedDate).text = "Applied ${candidate.getRelativeAppliedDate()}"


            val statusText = cardView.findViewById<android.widget.TextView>(R.id.statusText)
            val actionButtonsContainer = cardView.findViewById<android.widget.LinearLayout>(R.id.actionButtonsContainer)

            when (candidate.status) {
                "shortlisted", "hired" -> {

                    statusText.text = "Candidates Accepted"
                    statusText.setTextColor(android.graphics.Color.parseColor("#4CAF50"))
                    statusText.visibility = View.VISIBLE
                    actionButtonsContainer.visibility = View.GONE
                }
                "frozen" -> {

                    statusText.text = "Candidates Accepted (Frozen)"
                    statusText.setTextColor(android.graphics.Color.parseColor("#FF9500"))
                    statusText.visibility = View.VISIBLE
                    actionButtonsContainer.visibility = View.GONE
                }
                "rejected" -> {

                    statusText.text = "Rejected Candidate"
                    statusText.setTextColor(android.graphics.Color.parseColor("#F44336"))
                    statusText.visibility = View.VISIBLE
                    actionButtonsContainer.visibility = View.GONE
                }
                else -> {

                    statusText.visibility = View.GONE
                    actionButtonsContainer.visibility = View.VISIBLE


                    cardView.findViewById<android.widget.Button>(R.id.removeFreelancerButton).apply {
                        text = "REJECT"
                        setOnClickListener {
                            updateCandidateStatus(candidate, "rejected")
                        }
                    }

                    cardView.findViewById<android.widget.Button>(R.id.contactFreelancerButton).apply {
                        text = "VIEW DETAILS"
                        setOnClickListener {
                            showAcceptConfirmationWithDetails(candidate)
                        }
                    }
                }
            }

            binding.freelancerCandidatesList.addView(cardView)
        }
    }

    private fun updateCandidateStatus(candidate: CandidateModel, newStatus: String) {
        when(newStatus) {
            "rejected" -> showRejectConfirmation(candidate)
            "shortlisted" -> showAcceptConfirmationWithDetails(candidate)
            else -> showGenericConfirmation(candidate, newStatus)
        }
    }

    private fun showRejectConfirmation(candidate: CandidateModel) {
        val builder = AlertDialog.Builder(requireContext())
        builder.setTitle("Reject Candidate")
        builder.setMessage("Are you sure you want to reject ${candidate.name}?\n\nThis action cannot be undone.")

        builder.setPositiveButton("Reject") { _, _ ->
            performStatusUpdate(candidate, "rejected", "rejected")
        }
        builder.setNegativeButton("Cancel") { dialog, _ ->
            dialog.dismiss()
        }
        builder.show()
    }

    private fun showAcceptConfirmationWithDetails(candidate: CandidateModel) {
        val builder = AlertDialog.Builder(requireContext())
        builder.setTitle("Accept Candidate - ${candidate.name}")


        val cleanCoverLetter = candidate.description
            .ifEmpty { "No cover letter provided" }
            .trim()
            .replace("\t", "    ")
            .lines()
            .joinToString("\n") { it.trim() }


        val detailsInfo = buildString {
            appendLine("📋 CANDIDATE DETAILS")
            appendLine()
            appendLine("👤 Name: ${candidate.name}")
            appendLine("📧 Email: ${candidate.email}")
            appendLine()
            appendLine("🛠️ Applied Skills: ${candidate.skills.joinToString(", ").ifEmpty { "No skills specified" }}")
            appendLine()
            appendLine("📅 Date Applied: ${candidate.getFormattedAppliedDate()}")
            appendLine()
            appendLine("💬 Cover Letter:")
            appendLine(cleanCoverLetter)
            appendLine()
            append("Are you sure you want to shortlist this candidate?")
        }

        builder.setMessage(detailsInfo)

        builder.setPositiveButton("Accept & Shortlist") { _, _ ->
            performStatusUpdate(candidate, "shortlisted", "shortlisted")
        }
        builder.setNegativeButton("Cancel") { dialog, _ ->
            dialog.dismiss()
        }
        builder.show()
    }

    private fun showGenericConfirmation(candidate: CandidateModel, newStatus: String) {
        val statusName = when(newStatus) {
            "hired" -> "hire"
            else -> "update"
        }

        val builder = AlertDialog.Builder(requireContext())
        builder.setTitle("${statusName.replaceFirstChar { it.uppercase() }} Candidate")
        builder.setMessage("Are you sure you want to ${statusName} ${candidate.name}?")

        builder.setPositiveButton(statusName.replaceFirstChar { it.uppercase() }) { _, _ ->
            performStatusUpdate(candidate, newStatus, statusName)
        }
        builder.setNegativeButton("Cancel") { dialog, _ ->
            dialog.dismiss()
        }
        builder.show()
    }

    private fun performStatusUpdate(candidate: CandidateModel, newStatus: String, actionName: String) {
        lifecycleScope.launch {
            val success = userRepository.updateApplicationStatus(candidate.applicationId, newStatus)
            if (success) {
                Toast.makeText(requireContext(), "${candidate.name} has been ${actionName}ed", Toast.LENGTH_SHORT).show()

                if (newStatus == "shortlisted") {

                    val attendanceManager = com.uilover.project196.Utils.AttendanceManager.getInstance(requireContext())
                    attendanceManager.clearAllCandidateProgressOnFirstAcceptance(candidate.userId, candidate.jobId)
                    android.util.Log.d("CandidatesFragment", "✅ Cleared all existing progress data for newly accepted candidate ${candidate.name}")


                    val chatRepository = com.uilover.project196.Repository.ChatRepository.getInstance(requireContext())
                    val verificationResetSuccess = chatRepository.resetVerificationStatusForFreelancer(candidate.userId)

                    if (verificationResetSuccess) {
                        android.util.Log.d("CandidatesFragment", "✅ Verification status reset for freelancer ${candidate.name} (${candidate.userId}) after business owner acceptance")
                    } else {
                        android.util.Log.w("CandidatesFragment", "⚠️ Failed to reset verification status for freelancer ${candidate.name} (${candidate.userId})")
                    }


                    attendanceManager.createInitialAttendanceForNewFreelancer(candidate.jobId, candidate.userId)


                    kotlinx.coroutines.delay(500)


                    val firstAcceptanceIntent = Intent("com.uilover.project196.FREELANCER_FIRST_ACCEPTANCE")
                    firstAcceptanceIntent.putExtra("freelancer_id", candidate.userId)
                    firstAcceptanceIntent.putExtra("job_id", candidate.jobId)
                    firstAcceptanceIntent.putExtra("freelancer_name", candidate.name)
                    requireContext().sendBroadcast(firstAcceptanceIntent)

                    android.util.Log.d("CandidatesFragment", "✅ Sent first acceptance broadcast to clear progress data for ${candidate.name}")


                    val candidatesProgressIntent = Intent("com.uilover.project196.CANDIDATE_HIRED")
                    candidatesProgressIntent.putExtra("freelancer_id", candidate.userId)
                    candidatesProgressIntent.putExtra("job_id", candidate.jobId)
                    candidatesProgressIntent.putExtra("candidate_name", candidate.name)
                    requireContext().sendBroadcast(candidatesProgressIntent)

                    android.util.Log.d("CandidatesFragment", "✅ Sent broadcast to refresh candidates progress for newly hired freelancer")


                    val welcomeMessageSuccess = chatRepository.createWelcomeMessageForNewlyShortlistedFreelancer(
                        applicationId = candidate.applicationId,
                        freelancerId = candidate.userId,
                        businessOwnerId = UserSession.getUserId() ?: ""
                    )

                    if (welcomeMessageSuccess) {
                        android.util.Log.d("CandidatesFragment", "✅ Created welcome message for ${candidate.name} - chat will now appear for both parties")
                    } else {
                        android.util.Log.w("CandidatesFragment", "⚠️ Failed to create welcome message for ${candidate.name}")
                    }


                    kotlinx.coroutines.delay(500)


                    chatRepository.forceRefreshChats()


                    kotlinx.coroutines.delay(200)

                    navigateToChatTab()
                }


                loadRealCandidates()
            } else {
                Toast.makeText(requireContext(), "Failed to update candidate status", Toast.LENGTH_SHORT).show()
            }
        }
    }

    private suspend fun createInitialAttendanceRecords(jobId: Int, freelancerId: String) {
        try {
            val database = com.uilover.project196.Database.AppDatabase.getDatabase(requireContext())
            val jobAttendanceDao = database.jobAttendanceDao()
            val dateFormat = java.text.SimpleDateFormat("yyyy-MM-dd", java.util.Locale.getDefault())
            val calendar = java.util.Calendar.getInstance()


            val existingRecords = jobAttendanceDao.getAttendanceByJobAndFreelancer(jobId, freelancerId)
            if (existingRecords.isNotEmpty()) {
                android.util.Log.d("CandidatesFragment", "Attendance records already exist for job $jobId, freelancer $freelancerId")
                return
            }

            val today = dateFormat.format(calendar.time)


            val todayAttendance = com.uilover.project196.Model.JobAttendanceEntity(
                jobId = jobId,
                freelancerId = freelancerId,
                attendanceDate = today,
                checkInTime = null,
                checkOutTime = null,
                progressReport = null
            )
            val todayId = jobAttendanceDao.upsertAttendance(todayAttendance)
            android.util.Log.d("CandidatesFragment", "✅ Created TODAY's attendance: $today (ID: $todayId) - ACTIONS ENABLED")


            calendar.add(java.util.Calendar.DAY_OF_MONTH, 1)
            val tomorrow = dateFormat.format(calendar.time)

            val tomorrowAttendance = com.uilover.project196.Model.JobAttendanceEntity(
                jobId = jobId,
                freelancerId = freelancerId,
                attendanceDate = tomorrow,
                checkInTime = null,
                checkOutTime = null,
                progressReport = null
            )
            val tomorrowId = jobAttendanceDao.upsertAttendance(tomorrowAttendance)
            android.util.Log.d("CandidatesFragment", "✅ Created TOMORROW's attendance: $tomorrow (ID: $tomorrowId) - ACTIONS DISABLED")

            android.util.Log.d("CandidatesFragment", "=== INITIAL ATTENDANCE SETUP COMPLETE ===")
            android.util.Log.d("CandidatesFragment", "Job: $jobId | Freelancer: $freelancerId")
            android.util.Log.d("CandidatesFragment", "Records created: TODAY ($today) + TOMORROW ($tomorrow)")
            android.util.Log.d("CandidatesFragment", "This creates the rolling 2-day window as requested")

        } catch (e: Exception) {
            android.util.Log.e("CandidatesFragment", "Error creating initial attendance records", e)
        }
    }

    private fun navigateToChatTab() {

        if (requireActivity() is com.uilover.project196.Activity.DetailActivity) {

            val intent = android.content.Intent(requireContext(), com.uilover.project196.Activity.MainActivity::class.java).apply {
                flags = android.content.Intent.FLAG_ACTIVITY_CLEAR_TOP or android.content.Intent.FLAG_ACTIVITY_SINGLE_TOP
                putExtra("NAVIGATE_TO_CHAT", true)
            }
            startActivity(intent)
            requireActivity().finish()
        } else {

            (requireActivity() as? com.uilover.project196.Activity.MainActivity)?.let { mainActivity ->
                mainActivity.binding.viewPager.currentItem = 3
            }
        }
    }

    private fun showClearCandidatesConfirmation() {
        val builder = AlertDialog.Builder(requireContext())
        builder.setTitle("Clear All Candidates")
        builder.setMessage("⚠️ This will permanently delete ALL job applications from ALL jobs.\n\nThis action cannot be undone. Are you sure?")

        builder.setPositiveButton("Clear All") { _, _ ->
            clearAllCandidates()
        }
        builder.setNegativeButton("Cancel") { dialog, _ ->
            dialog.dismiss()
        }
        builder.show()
    }

    private fun clearAllCandidates() {
        lifecycleScope.launch {
            try {
                android.util.Log.d("CandidatesFragment", "clearAllCandidates: User requested to clear all candidates")

                val success = userRepository.clearAllCandidates()
                if (success) {
                    Toast.makeText(requireContext(), "✅ All candidates cleared successfully", Toast.LENGTH_LONG).show()
                    android.util.Log.d("CandidatesFragment", "clearAllCandidates: ✅ All candidates cleared - refreshing UI")


                    loadRealCandidates()
                } else {
                    Toast.makeText(requireContext(), "❌ Failed to clear candidates", Toast.LENGTH_SHORT).show()
                    android.util.Log.e("CandidatesFragment", "clearAllCandidates: ❌ Failed to clear candidates")
                }
            } catch (e: Exception) {
                Toast.makeText(requireContext(), "❌ Error: ${e.message}", Toast.LENGTH_SHORT).show()
                android.util.Log.e("CandidatesFragment", "clearAllCandidates: Exception", e)
            }
        }
    }

    private fun checkAndOfferAttendanceReactivation() {

        val prefs = requireContext().getSharedPreferences("app_state", android.content.Context.MODE_PRIVATE)
        val isAttendanceDisabled = prefs.getBoolean("attendance_disabled", false)

        if (isAttendanceDisabled) {
            val builder = android.app.AlertDialog.Builder(requireContext())
            builder.setTitle("Re-enable Attendance")
            builder.setMessage("Attendance is currently disabled for all your shortlisted candidates.\n\nWould you like to re-enable attendance tracking for all verified freelancers?")

            builder.setPositiveButton("Re-enable") { _, _ ->
                reEnableAttendanceForAllCandidates()
            }
            builder.setNegativeButton("Cancel") { dialog, _ ->
                dialog.dismiss()
            }
            builder.show()
        } else {
            Toast.makeText(requireContext(), "✅ Attendance is already enabled for your candidates", Toast.LENGTH_SHORT).show()
        }
    }

    private fun reEnableAttendanceForAllCandidates() {
        lifecycleScope.launch {
            try {

                val prefs = requireContext().getSharedPreferences("app_state", android.content.Context.MODE_PRIVATE)
                prefs.edit().putBoolean("attendance_disabled", false).apply()


                val intent = android.content.Intent("com.uilover.project196.ATTENDANCE_REACTIVATED")
                requireContext().sendBroadcast(intent)

                Toast.makeText(requireContext(), "✅ Attendance re-enabled for all shortlisted candidates!", Toast.LENGTH_LONG).show()
                android.util.Log.d("CandidatesFragment", "Attendance re-enabled by business owner")

            } catch (e: Exception) {
                Toast.makeText(requireContext(), "❌ Error re-enabling attendance: ${e.message}", Toast.LENGTH_SHORT).show()
                android.util.Log.e("CandidatesFragment", "Error re-enabling attendance", e)
            }
        }
    }

    private fun showAccessDeniedMessage() {
        binding.businessOwnerContent.visibility = View.GONE
        binding.accessDeniedMessage.visibility = View.VISIBLE

        val userRole = UserSession.getRoleDisplayName()
        binding.accessDeniedText.text = "This feature is only available for Business Owners.\n\nYou are currently logged in as: $userRole\n\nBusiness owners can browse and contact freelancer candidates here."

        binding.switchToBusinessOwnerButton.setOnClickListener {

            UserSession.simulateBusinessOwnerLogin()
            setupRealCandidatesView()
        }
    }

    override fun onDestroyView() {
        super.onDestroyView()


        try {
            requireContext().unregisterReceiver(newCandidateReceiver)
            android.util.Log.d("CandidatesFragment", "✅ Unregistered new candidate broadcast receiver")
        } catch (e: Exception) {
            android.util.Log.w("CandidatesFragment", "Error unregistering broadcast receiver", e)
        }

        _binding = null
    }
}