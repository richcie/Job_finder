package com.uilover.project196.Fragment

import android.content.Intent
import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.Toast
import androidx.lifecycle.ViewModelProvider
import androidx.recyclerview.widget.LinearLayoutManager
import com.uilover.project196.Activity.CandidateProgressDetailActivity
import com.uilover.project196.Adapter.CandidateProgressAdapter
import com.uilover.project196.Model.CandidateProgressModel
import com.uilover.project196.Utils.UserSession
import com.uilover.project196.ViewModel.CandidatesProgressViewModel
import com.uilover.project196.databinding.FragmentCandidatesProgressBinding
import android.content.BroadcastReceiver
import android.content.Context
import android.content.IntentFilter

// KRITERIA WAJIB: Multiple Fragment (12/16) - Fragment progress kandidat
class CandidatesProgressFragment : BaseFragment(), CandidateProgressAdapter.ClickListener, UserSession.ProfessionChangeListener {
    private var _binding: FragmentCandidatesProgressBinding? = null
    private val binding get() = _binding!!
    private lateinit var viewModel: CandidatesProgressViewModel
    private lateinit var progressAdapter: CandidateProgressAdapter


    private var specificJobId: String? = null
    private var jobTitle: String? = null
    private var jobCompany: String? = null
    private var jobOwnerId: String? = null

    private val verificationReceiver = object : BroadcastReceiver() {
        override fun onReceive(context: Context?, intent: Intent?) {
            android.util.Log.d("CandidatesProgressFragment", "=== BROADCAST RECEIVED ===")
            android.util.Log.d("CandidatesProgressFragment", "Action: ${intent?.action}")
            android.util.Log.d("CandidatesProgressFragment", "Current user role: ${UserSession.getUserRole()}")
            android.util.Log.d("CandidatesProgressFragment", "Is logged in: ${UserSession.isLoggedIn()}")

            when (intent?.action) {
                "com.uilover.project196.VERIFICATION_ACCEPTED" -> {
                    val freelancerId = intent.getStringExtra("freelancer_id")
                    val attendanceReset = intent.getBooleanExtra("attendance_reset", false)
                    android.util.Log.d("CandidatesProgressFragment", "Received verification accepted broadcast for freelancer $freelancerId, attendance reset: $attendanceReset")


                    if (UserSession.isLoggedIn() && UserSession.getUserRole() == UserSession.ROLE_BUSINESS_OWNER) {
                        android.util.Log.d("CandidatesProgressFragment", "Refreshing candidates progress list...")
                        viewModel.refreshData()
                    } else {
                        android.util.Log.d("CandidatesProgressFragment", "Not refreshing - user not business owner or not logged in")
                    }
                }
                "com.uilover.project196.CANDIDATE_HIRED" -> {
                    val freelancerId = intent.getStringExtra("freelancer_id")
                    val candidateName = intent.getStringExtra("candidate_name")
                    val unusedJobId = intent.getIntExtra("job_id", -1) // Job ID not used in this context
                    android.util.Log.d("CandidatesProgressFragment", "Received candidate hired broadcast for $candidateName ($freelancerId)")


                    if (UserSession.isLoggedIn() && UserSession.getUserRole() == UserSession.ROLE_BUSINESS_OWNER) {
                        android.util.Log.d("CandidatesProgressFragment", "Refreshing candidates progress list...")
                        viewModel.refreshData()
                    } else {
                        android.util.Log.d("CandidatesProgressFragment", "Not refreshing - user not business owner or not logged in")
                    }
                }
                "com.uilover.project196.FREELANCER_BLOCKED" -> {
                    val freelancerId = intent.getStringExtra("freelancer_id")
                    val freelancerName = intent.getStringExtra("freelancer_name")
                    val actionType = intent.getStringExtra("action_type") ?: "blocked"
                    android.util.Log.d("CandidatesProgressFragment", "Received freelancer $actionType broadcast for $freelancerName ($freelancerId)")


                    if (UserSession.isLoggedIn() && UserSession.getUserRole() == UserSession.ROLE_BUSINESS_OWNER) {
                        android.util.Log.d("CandidatesProgressFragment", "Refreshing candidates progress list after $actionType...")
                        viewModel.refreshData()
                    } else {
                        android.util.Log.d("CandidatesProgressFragment", "Not refreshing - user not business owner or not logged in")
                    }
                }
                "com.uilover.project196.FREELANCER_FIRST_ACCEPTANCE" -> {
                    val freelancerId = intent.getStringExtra("freelancer_id")
                    val freelancerName = intent.getStringExtra("freelancer_name")
                    val jobId = intent.getIntExtra("job_id", -1)
                    android.util.Log.d("CandidatesProgressFragment", "Received freelancer first acceptance broadcast for $freelancerName ($freelancerId) on job $jobId")


                    if (UserSession.isLoggedIn() && UserSession.getUserRole() == UserSession.ROLE_BUSINESS_OWNER) {
                        viewModel.refreshData()
                    }
                }
                "com.uilover.project196.FREELANCER_PROGRESS_RESET" -> {
                    val freelancerId = intent.getStringExtra("freelancer_id")
                    val jobId = intent.getIntExtra("job_id", -1)
                    val resetType = intent.getStringExtra("reset_type")
                    android.util.Log.d("CandidatesProgressFragment", "🔥 COMPLETE PROGRESS RESET: freelancer $freelancerId applied to job $jobId ($resetType)")


                    if (UserSession.isLoggedIn() && UserSession.getUserRole() == UserSession.ROLE_BUSINESS_OWNER) {
                        android.util.Log.d("CandidatesProgressFragment", "🔄 Refreshing candidates progress after complete reset...")
                        viewModel.refreshData()
                    } else {
                        android.util.Log.d("CandidatesProgressFragment", "Not refreshing - user not business owner or not logged in")
                    }
                }
            }
        }
    }

    companion object {
        fun newInstance(jobId: String): CandidatesProgressFragment {
            val fragment = CandidatesProgressFragment()
            val args = Bundle()
            args.putString("JOB_ID", jobId)
            fragment.arguments = args
            return fragment
        }

        fun newInstanceForJob(jobTitle: String, company: String, ownerId: String?): CandidatesProgressFragment {
            val fragment = CandidatesProgressFragment()
            val args = Bundle()
            args.putString("JOB_TITLE", jobTitle)
            args.putString("JOB_COMPANY", company)
            args.putString("JOB_OWNER_ID", ownerId ?: "")
            fragment.arguments = args
            return fragment
        }
    }

    override fun onCreateView(
        inflater: LayoutInflater,
        container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View {
        _binding = FragmentCandidatesProgressBinding.inflate(inflater, container, false)
        return binding.root
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)


        viewModel = ViewModelProvider(
            this,
            ViewModelProvider.AndroidViewModelFactory.getInstance(requireActivity().application)
        )[CandidatesProgressViewModel::class.java]


        parseArguments()




        setupReactiveBinding(viewModel)
        setupLiveDataObservers()
        setupRecyclerView()
        setupBroadcastReceivers()


        initializeViewModel()


        UserSession.addProfessionChangeListener(this)
    }

    private fun parseArguments() {
        arguments?.let { args ->
            specificJobId = args.getString("JOB_ID")
            jobTitle = args.getString("JOB_TITLE")
            jobCompany = args.getString("JOB_COMPANY")
            jobOwnerId = args.getString("JOB_OWNER_ID")
        }
    }

    private fun initializeViewModel() {
        when {
            specificJobId != null -> {
                android.util.Log.d("CandidatesProgressFragment", "Initializing for specific job ID: $specificJobId")
                viewModel.initializeForSpecificJob(specificJobId!!)
            }
            jobTitle != null && jobCompany != null && jobOwnerId != null -> {
                android.util.Log.d("CandidatesProgressFragment", "Initializing for job: $jobTitle at $jobCompany")
                viewModel.initializeForJobDetails(jobTitle!!, jobCompany!!, jobOwnerId!!)
            }
            else -> {
                android.util.Log.d("CandidatesProgressFragment", "Initializing for all jobs")
                viewModel.initializeForAllJobs()
            }
        }
    }




    private fun setupReactiveBinding(viewModel: CandidatesProgressViewModel) {


        viewModel.candidateCount.addOnPropertyChangedCallback(
            object : androidx.databinding.Observable.OnPropertyChangedCallback() {
                override fun onPropertyChanged(sender: androidx.databinding.Observable?, propertyId: Int) {
                    val count = viewModel.candidateCount.get() ?: "0"
                    val subtitle = when {
                        count == "0" -> "No candidates yet"
                        count == "1" -> "1 candidate"
                        else -> "$count candidates"
                    }
                    viewModel.headerSubtitle.set(subtitle)
                }
            }
        )


        viewModel.showLoadingState.addOnPropertyChangedCallback(
            object : androidx.databinding.Observable.OnPropertyChangedCallback() {
                override fun onPropertyChanged(sender: androidx.databinding.Observable?, propertyId: Int) {
                    val isVisible = viewModel.showLoadingState.get() == true
                    // Loading UI updates can be added here if needed
                    android.util.Log.d("CandidatesProgressFragment", "Loading state changed: $isVisible")
                }
            }
        )


        viewModel.showContentState.addOnPropertyChangedCallback(
            object : androidx.databinding.Observable.OnPropertyChangedCallback() {
                override fun onPropertyChanged(sender: androidx.databinding.Observable?, propertyId: Int) {
                    binding.recyclerViewProgress.visibility =
                        if (viewModel.showContentState.get() == true) View.VISIBLE else View.GONE
                }
            }
        )


        viewModel.showEmptyState.addOnPropertyChangedCallback(
            object : androidx.databinding.Observable.OnPropertyChangedCallback() {
                override fun onPropertyChanged(sender: androidx.databinding.Observable?, propertyId: Int) {
                    binding.emptyStateLayout.visibility =
                        if (viewModel.showEmptyState.get() == true) View.VISIBLE else View.GONE
                }
            }
        )


        viewModel.emptyStateTitle.addOnPropertyChangedCallback(
            object : androidx.databinding.Observable.OnPropertyChangedCallback() {
                override fun onPropertyChanged(sender: androidx.databinding.Observable?, propertyId: Int) {
                    binding.emptyTitle.text = viewModel.emptyStateTitle.get() ?: "No Candidates"
                }
            }
        )


        viewModel.emptyStateSubtitle.addOnPropertyChangedCallback(
            object : androidx.databinding.Observable.OnPropertyChangedCallback() {
                override fun onPropertyChanged(sender: androidx.databinding.Observable?, propertyId: Int) {
                    binding.emptyMessage.text = viewModel.emptyStateSubtitle.get() ?: "Candidates will appear here"
                }
            }
        )


        viewModel.showUnauthorizedState.addOnPropertyChangedCallback(
            object : androidx.databinding.Observable.OnPropertyChangedCallback() {
                override fun onPropertyChanged(sender: androidx.databinding.Observable?, propertyId: Int) {
                    val isUnauthorized = viewModel.showUnauthorizedState.get() == true
                    if (isUnauthorized) {
                        binding.recyclerViewProgress.visibility = View.GONE
                        binding.emptyStateLayout.visibility = View.VISIBLE
                    }
                }
            }
        )


        viewModel.totalCandidates.addOnPropertyChangedCallback(
            object : androidx.databinding.Observable.OnPropertyChangedCallback() {
                override fun onPropertyChanged(sender: androidx.databinding.Observable?, propertyId: Int) {

                    val count = viewModel.totalCandidates.get() ?: "0"
                    // Log total candidates count for analytics
                    android.util.Log.d("CandidatesProgressFragment", "Total candidates count: $count")
                }
            }
        )

        viewModel.averageCompletionRate.addOnPropertyChangedCallback(
            object : androidx.databinding.Observable.OnPropertyChangedCallback() {
                override fun onPropertyChanged(sender: androidx.databinding.Observable?, propertyId: Int) {
                    val rate = viewModel.averageCompletionRate.get() ?: "0%"
                    // Log completion rate for analytics
                    android.util.Log.d("CandidatesProgressFragment", "Average completion rate: $rate")
                }
            }
        )
    }




    private fun setupLiveDataObservers() {


        viewModel.candidatesProgressList.observe(viewLifecycleOwner) { candidatesList ->
            candidatesList?.let {
                android.util.Log.d("CandidatesProgressFragment", "📊 LiveData observer triggered with ${it.size} candidates")
                updateCandidatesList(it)
            }
        }


        viewModel.isAuthorized.observe(viewLifecycleOwner) { isAuthorized ->
            if (!isAuthorized) {
                android.util.Log.w("CandidatesProgressFragment", "User not authorized to view candidates progress")
            }
        }


        viewModel.isLoading.observe(viewLifecycleOwner) { isLoading ->
            android.util.Log.d("CandidatesProgressFragment", "Loading state: $isLoading")

        }


        viewModel.showSuccessMessage.observe(viewLifecycleOwner) { message ->
            message?.let {
                Toast.makeText(requireContext(), it, Toast.LENGTH_SHORT).show()
                viewModel.onSuccessMessageShown()
            }
        }


        viewModel.showErrorMessage.observe(viewLifecycleOwner) { message ->
            message?.let {
                Toast.makeText(requireContext(), it, Toast.LENGTH_SHORT).show()
                viewModel.onErrorMessageShown()
            }
        }
    }

    private fun setupBroadcastReceivers() {

        val intentFilter = IntentFilter().apply {
            addAction("com.uilover.project196.VERIFICATION_ACCEPTED")
            addAction("com.uilover.project196.CANDIDATE_HIRED")
            addAction("com.uilover.project196.FREELANCER_BLOCKED")
            addAction("com.uilover.project196.FREELANCER_FIRST_ACCEPTANCE")
            addAction("com.uilover.project196.FREELANCER_PROGRESS_RESET")
        }
        if (android.os.Build.VERSION.SDK_INT >= android.os.Build.VERSION_CODES.TIRAMISU) {
            requireContext().registerReceiver(verificationReceiver, intentFilter, Context.RECEIVER_NOT_EXPORTED)
        } else {
            requireContext().registerReceiver(verificationReceiver, intentFilter)
        }
    }

    private fun setupRecyclerView() {
        binding.recyclerViewProgress.layoutManager = LinearLayoutManager(requireContext())
        progressAdapter = CandidateProgressAdapter(mutableListOf(), this)
        binding.recyclerViewProgress.adapter = progressAdapter
    }

    private fun updateCandidatesList(candidatesList: List<CandidateProgressModel>) {
        android.util.Log.d("CandidatesProgressFragment", "Updating RecyclerView with ${candidatesList.size} candidates")
        progressAdapter.updateData(candidatesList.toMutableList())
    }






    override fun onCandidateProgressClick(candidateProgress: CandidateProgressModel) {
        android.util.Log.d("CandidatesProgressFragment", "=== CANDIDATE PROGRESS CLICKED ===")
        android.util.Log.d("CandidatesProgressFragment", "Candidate Name: ${candidateProgress.candidateName}")
        android.util.Log.d("CandidatesProgressFragment", "Candidate Role: ${candidateProgress.candidateRole}")
        android.util.Log.d("CandidatesProgressFragment", "Job Title: ${candidateProgress.jobTitle}")
        android.util.Log.d("CandidatesProgressFragment", "Passing actual candidate role data to detail activity")

        val intent = Intent(requireContext(), CandidateProgressDetailActivity::class.java).apply {
            putExtra("CANDIDATE_ID", candidateProgress.candidateId)
            putExtra("CANDIDATE_NAME", candidateProgress.candidateName)
            putExtra("JOB_ID", candidateProgress.jobId)
            putExtra("JOB_TITLE", candidateProgress.jobTitle)
            putExtra("APPLICATION_ID", candidateProgress.applicationId)

            putExtra("CANDIDATE_ROLE", candidateProgress.candidateRole)
        }
        startActivity(intent)
    }

    override fun onResume() {
        super.onResume()

        android.util.Log.d("CandidatesProgressFragment", "=== FRAGMENT RESUMED ===")


        if (UserSession.isLoggedIn() && UserSession.getUserRole() == UserSession.ROLE_BUSINESS_OWNER) {
            android.util.Log.d("CandidatesProgressFragment", "User is business owner - refreshing candidates progress through ViewModel")

            binding.root.postDelayed({
                viewModel.refreshData()
            }, 300)
        } else {
            android.util.Log.d("CandidatesProgressFragment", "User is not business owner or not logged in")
        }
    }

    override fun onLoginStateRefresh(isLoggedIn: Boolean) {

        if (isLoggedIn && UserSession.getUserRole() == UserSession.ROLE_BUSINESS_OWNER) {
            android.util.Log.d("CandidatesProgressFragment", "Login state refreshed - reinitializing ViewModel")
            initializeViewModel()
        } else {
            android.util.Log.d("CandidatesProgressFragment", "Login state refreshed - user not authorized")

        }
    }

    override fun onDestroyView() {
        super.onDestroyView()


        try {
            requireContext().unregisterReceiver(verificationReceiver)
        } catch (e: Exception) {

        }


        UserSession.removeProfessionChangeListener(this)

        _binding = null
    }


    override fun onProfessionChanged(newProfession: String) {

        if (UserSession.isLoggedIn() && UserSession.getUserRole() == UserSession.ROLE_BUSINESS_OWNER) {
            android.util.Log.d("CandidatesProgressFragment", "Profession changed to: $newProfession - refreshing data through ViewModel")
            viewModel.refreshData()
        }
    }


}