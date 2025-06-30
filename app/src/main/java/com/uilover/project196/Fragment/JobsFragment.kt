package com.uilover.project196.Fragment

import android.content.Intent
import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import androidx.lifecycle.lifecycleScope
import androidx.recyclerview.widget.LinearLayoutManager
import com.uilover.project196.Adapter.FreelancerJobAdapter
import com.uilover.project196.Model.FreelancerJobModel
import com.uilover.project196.Repository.FreelancerJobRepository
import com.uilover.project196.Utils.UserSession
import com.uilover.project196.databinding.FragmentJobsBinding
import com.uilover.project196.Activity.JobAttendanceActivity
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import android.content.BroadcastReceiver
import android.content.Context
import android.content.IntentFilter

// KRITERIA WAJIB: Multiple Fragment (6/16) - Fragment daftar pekerjaan freelancer
class JobsFragment : BaseFragment() {

    private var _binding: FragmentJobsBinding? = null
    private val binding get() = _binding!!

    private lateinit var freelancerJobRepository: FreelancerJobRepository
    private lateinit var jobAdapter: FreelancerJobAdapter
    private var jobs = mutableListOf<FreelancerJobModel>()

    private val verificationReceiver = object : BroadcastReceiver() {
        override fun onReceive(context: Context?, intent: Intent?) {
            when (intent?.action) {
                "com.uilover.project196.VERIFICATION_ACCEPTED" -> {
                    val freelancerId = intent.getStringExtra("freelancer_id")
                    val currentUserId = UserSession.getUserId()


                    if (freelancerId == currentUserId) {
                        android.util.Log.d("JobsFragment", "Received verification accepted for current user, refreshing jobs")
                        loadJobs()
                    }
                }
                "com.uilover.project196.VERIFICATION_RESET_JOBS" -> {
                    val freelancerId = intent.getStringExtra("freelancer_id")
                    val currentUserId = UserSession.getUserId()


                    if (freelancerId == currentUserId) {
                        android.util.Log.d("JobsFragment", "Received verification reset for current user, hiding jobs until re-verified")
                        loadJobs()
                    }
                }
                "com.uilover.project196.ATTENDANCE_REACTIVATED" -> {
                    android.util.Log.d("JobsFragment", "Received attendance reactivated broadcast, refreshing jobs")
                    loadJobs()
                }
                "com.uilover.project196.FREELANCER_SHORTLISTED" -> {
                    val freelancerId = intent.getStringExtra("freelancer_id")
                    val currentUserId = UserSession.getUserId()


                    if (freelancerId == currentUserId) {
                        android.util.Log.d("JobsFragment", "Received shortlisted notification for current user, refreshing jobs")
                        loadJobs()
                    }
                }
                "com.uilover.project196.ATTENDANCE_UPDATED" -> {
                    val freelancerId = intent.getStringExtra("freelancer_id")
                    val jobId = intent.getIntExtra("job_id", -1)
                    val currentUserId = UserSession.getUserId()


                    if (freelancerId == currentUserId) {
                        android.util.Log.d("JobsFragment", "Received attendance updated for current user (job: $jobId), refreshing jobs")
                        loadJobs()
                    }
                }
            }
        }
    }

    override fun onCreateView(
        inflater: LayoutInflater,
        container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View {
        _binding = FragmentJobsBinding.inflate(inflater, container, false)
        return binding.root
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)

        setupRepository()
        setupRecyclerView()
        setupUI()
        loadJobs()


        setupNotificationButton(binding.imageView3, binding.textView4)
    }

    private fun setupRepository() {
        freelancerJobRepository = FreelancerJobRepository.getInstance(requireContext())
    }

    private fun setupRecyclerView() {
        jobAdapter = FreelancerJobAdapter(
            jobs = jobs,
            onJobClick = { job ->

                android.util.Log.d("JobsFragment", "=== JOB CLICK DEBUG ===")
                android.util.Log.d("JobsFragment", "Job clicked - ID: ${job.id}, Title: ${job.title}")
                android.util.Log.d("JobsFragment", "Company: ${job.companyName}, Active: ${job.isActive}")

                try {
                    val intent = Intent(requireContext(), JobAttendanceActivity::class.java)
                    intent.putExtra("JOB_ID", job.id)
                    intent.putExtra("JOB_TITLE", job.title)
                    intent.putExtra("COMPANY_NAME", job.companyName)

                    android.util.Log.d("JobsFragment", "Starting JobAttendanceActivity with intent extras:")
                    android.util.Log.d("JobsFragment", "- JOB_ID: ${job.id}")
                    android.util.Log.d("JobsFragment", "- JOB_TITLE: ${job.title}")
                    android.util.Log.d("JobsFragment", "- COMPANY_NAME: ${job.companyName}")

                    startActivity(intent)
                    android.util.Log.d("JobsFragment", "JobAttendanceActivity start attempted")
                } catch (e: Exception) {
                    android.util.Log.e("JobsFragment", "Error starting JobAttendanceActivity", e)
                    android.widget.Toast.makeText(requireContext(), "Error opening job details: ${e.message}", android.widget.Toast.LENGTH_LONG).show()
                }
            },
            onViewProgress = { job ->

                android.util.Log.d("JobsFragment", "View progress clicked for job: ${job.title}")
                android.widget.Toast.makeText(
                    requireContext(),
                    "📊 Progress tracking for ${job.title}\n\nFeature coming soon!",
                    android.widget.Toast.LENGTH_LONG
                ).show()
            },
            onWriteReview = { job ->

                android.util.Log.d("JobsFragment", "Write review clicked for job: ${job.title}, Company: ${job.companyName}")


                try {
                    val intent = Intent(requireContext(), com.uilover.project196.Activity.DetailActivity::class.java)
                    intent.putExtra("businessOwnerId", job.businessOwnerId)
                    intent.putExtra("companyName", job.companyName)
                    intent.putExtra("showReviewTab", true)
                    startActivity(intent)
                } catch (e: Exception) {
                    android.util.Log.e("JobsFragment", "Error navigating to review screen", e)
                    android.widget.Toast.makeText(
                        requireContext(),
                        "Error opening review screen: ${e.message}",
                        android.widget.Toast.LENGTH_LONG
                    ).show()
                }
            }
        )

        binding.recyclerViewJobs.apply {
            layoutManager = LinearLayoutManager(requireContext())
            adapter = jobAdapter
        }
    }

    private fun setupUI() {

        if (!UserSession.isLoggedIn()) {
            showGuestState()
            return
        }


        val userRole = UserSession.getUserRole()
        if (userRole != UserSession.ROLE_FREELANCER) {
            binding.noJobsLayout.visibility = View.VISIBLE
            binding.guestLoginLayout.visibility = View.GONE
            binding.noJobsText.text = "Feature for freelancers only"
            binding.recyclerViewJobs.visibility = View.GONE
            return
        }


        binding.swipeRefreshLayout.setOnRefreshListener {
            loadJobs()
        }


        binding.guestLoginLayout.visibility = View.GONE
    }

    private fun showGuestState() {

        binding.recyclerViewJobs.visibility = View.GONE
        binding.noJobsLayout.visibility = View.GONE
        binding.guestLoginLayout.visibility = View.VISIBLE


        binding.loginPromptButton.setOnClickListener {

            (requireActivity() as? com.uilover.project196.Activity.MainActivity)?.let { mainActivity ->

                mainActivity.binding.viewPager.currentItem = 4
            }
        }
    }

    private fun loadJobs() {
        val currentUserId = UserSession.getUserId()
        android.util.Log.d("JobsFragment", "=== LOADING JOBS DEBUG ===")
        android.util.Log.d("JobsFragment", "Current user ID: $currentUserId")
        android.util.Log.d("JobsFragment", "User role: ${UserSession.getUserRole()}")

        if (currentUserId == null) {
            android.util.Log.w("JobsFragment", "User not logged in")
            showNoJobs("Please log in first")
            return
        }

        binding.swipeRefreshLayout.isRefreshing = true

        lifecycleScope.launch {
            try {
                android.util.Log.d("JobsFragment", "Fetching verified jobs from repository...")
                val verifiedJobs = withContext(Dispatchers.IO) {
                    freelancerJobRepository.getVerifiedJobsForFreelancer(currentUserId)
                }

                android.util.Log.d("JobsFragment", "Repository returned ${verifiedJobs.size} verified jobs")
                verifiedJobs.forEachIndexed { index, job ->
                    android.util.Log.d("JobsFragment", "Job $index: ID=${job.id}, Title='${job.title}', Active=${job.isActive}")
                }

                jobs.clear()
                jobs.addAll(verifiedJobs)

                if (jobs.isEmpty()) {
                    android.util.Log.w("JobsFragment", "No verified jobs found - showing empty state")
                    showNoJobs("No verified jobs yet")
                } else {
                    android.util.Log.d("JobsFragment", "Showing ${jobs.size} jobs to user")
                    showJobs()
                }

                jobAdapter.notifyDataSetChanged()
                android.util.Log.d("JobsFragment", "RecyclerView adapter notified of data change")

            } catch (e: Exception) {
                android.util.Log.e("JobsFragment", "Error loading jobs", e)
                showNoJobs("Error loading jobs: ${e.message}")
            } finally {
                binding.swipeRefreshLayout.isRefreshing = false
            }
        }
    }

    private fun showJobs() {
        binding.recyclerViewJobs.visibility = View.VISIBLE
        binding.noJobsLayout.visibility = View.GONE
    }

    private fun showNoJobs(message: String) {
        binding.recyclerViewJobs.visibility = View.GONE
        binding.noJobsLayout.visibility = View.VISIBLE
        binding.noJobsText.text = message
    }

    override fun onResume() {
        super.onResume()


        if (UserSession.isLoggedIn() && UserSession.getUserRole() == UserSession.ROLE_FREELANCER) {
            android.util.Log.d("JobsFragment", "Fragment resumed - refreshing jobs to sync attendance data")
            loadJobs()
        }


        if (UserSession.isLoggedIn() && UserSession.getUserRole() == UserSession.ROLE_FREELANCER) {
            val filter = IntentFilter().apply {
                addAction("com.uilover.project196.VERIFICATION_ACCEPTED")
                addAction("com.uilover.project196.VERIFICATION_RESET_JOBS")
                addAction("com.uilover.project196.ATTENDANCE_REACTIVATED")
                addAction("com.uilover.project196.FREELANCER_SHORTLISTED")
                addAction("com.uilover.project196.ATTENDANCE_UPDATED")
            }


            if (android.os.Build.VERSION.SDK_INT >= android.os.Build.VERSION_CODES.TIRAMISU) {
                requireContext().registerReceiver(verificationReceiver, filter, Context.RECEIVER_NOT_EXPORTED)
            } else {
                requireContext().registerReceiver(verificationReceiver, filter)
            }
        }
    }

    override fun onPause() {
        super.onPause()

        try {
            if (UserSession.isLoggedIn() && UserSession.getUserRole() == UserSession.ROLE_FREELANCER) {
                requireContext().unregisterReceiver(verificationReceiver)
            }
        } catch (e: IllegalArgumentException) {

        }
    }

    override fun onLoginStateRefresh(isLoggedIn: Boolean) {

        refreshBasedOnLoginState()
    }

    private fun refreshBasedOnLoginState() {
        if (UserSession.isLoggedIn()) {

            if (::freelancerJobRepository.isInitialized) {
                setupUI()
                loadJobs()
            }
        } else {
            showGuestState()
        }
    }

    override fun onDestroyView() {
        super.onDestroyView()


        try {
            requireContext().unregisterReceiver(verificationReceiver)
        } catch (e: Exception) {

        }

        _binding = null
    }
}