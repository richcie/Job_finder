package com.uilover.project196.Fragment

import android.app.Dialog
import android.content.Intent
import android.os.Bundle
import android.view.LayoutInflater
import android.view.MotionEvent
import android.view.View
import android.view.ViewGroup
import android.view.Window
import android.widget.AdapterView
import android.widget.ArrayAdapter
import android.widget.Toast
import androidx.appcompat.app.AlertDialog
import androidx.fragment.app.Fragment
import androidx.lifecycle.ViewModelProvider
import androidx.lifecycle.lifecycleScope
import androidx.recyclerview.widget.LinearLayoutManager
import androidx.recyclerview.widget.RecyclerView
import androidx.viewpager2.widget.ViewPager2
import com.uilover.project196.Activity.NotificationsActivity
import com.uilover.project196.Adapter.CategoryAdapter
import com.uilover.project196.Adapter.jobAdapter
import com.uilover.project196.Model.JobModel
import com.uilover.project196.R
import com.uilover.project196.Repository.NotificationRepository
import com.uilover.project196.Utils.UserSession
import com.uilover.project196.ViewModel.MainViewModel
import com.uilover.project196.ViewModel.JobManagementViewModel
import com.uilover.project196.ViewModel.BookmarkViewModel
import com.uilover.project196.databinding.DialogCreateJobBinding
import com.uilover.project196.databinding.FragmentHomeBinding
import kotlinx.coroutines.launch

class HomeFragment : BaseFragment() {
    private var _binding: FragmentHomeBinding? = null
    private val binding get() = _binding!!
    private lateinit var mainViewModel: MainViewModel
    private lateinit var jobManagementViewModel: JobManagementViewModel
    private lateinit var bookmarkViewModel: BookmarkViewModel
    private lateinit var openJobsAdapter: jobAdapter
    private lateinit var closedJobsAdapter: jobAdapter
    private var allJobsCache: List<JobModel> = emptyList()

    override fun onCreateView(
        inflater: LayoutInflater,
        container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View {
        _binding = FragmentHomeBinding.inflate(inflater, container, false)
        return binding.root
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)


        UserSession.init(requireContext())
        mainViewModel = ViewModelProvider(requireActivity())[MainViewModel::class.java]
        mainViewModel.initializeDatabase(requireContext())

        jobManagementViewModel = ViewModelProvider(
            this,
            ViewModelProvider.AndroidViewModelFactory.getInstance(requireActivity().application)
        )[JobManagementViewModel::class.java]

        bookmarkViewModel = ViewModelProvider(
            requireActivity(),
            ViewModelProvider.AndroidViewModelFactory.getInstance(requireActivity().application)
        )[BookmarkViewModel::class.java]

        setupJobManagementObservers()
        setupGlobalBookmarkObserver()

        if (UserSession.getUserRole() == UserSession.ROLE_BUSINESS_OWNER) {
            hideJobSectionsForBusinessOwner()
            initJobManagement()
        } else {
            showJobSectionsForUsers()
            initCategory()
            initSuggest()
            initRecent("0")
        }

        setupTouchHandling()
        setupSeeAllClickListeners()
        setupNotificationButton(binding.imageView3, binding.textView4)
    }

    private fun setupJobManagementObservers() {

        jobManagementViewModel.jobCreated.observe(viewLifecycleOwner) { created ->
            if (created) {

                loadOwnedJobs()
                jobManagementViewModel.onJobCreatedHandled()
            }
        }


        jobManagementViewModel.showSuccessMessage.observe(viewLifecycleOwner) { message ->
            message?.let {
                Toast.makeText(requireContext(), it, Toast.LENGTH_LONG).show()
                jobManagementViewModel.onSuccessMessageShown()
            }
        }


        jobManagementViewModel.showErrorMessage.observe(viewLifecycleOwner) { message ->
            message?.let {
                Toast.makeText(requireContext(), it, Toast.LENGTH_LONG).show()
                jobManagementViewModel.onErrorMessageShown()
            }
        }
    }





    private fun setupGlobalBookmarkObserver() {

        bookmarkViewModel.globalBookmarkStateChanged.observe(viewLifecycleOwner) { stateChange ->
            stateChange?.let {
                android.util.Log.d("HomeFragment", "🔄 Global bookmark state changed: ${it.jobTitle} = ${it.isBookmarked}")


                updateAdapterBookmarkStates(it.jobTitle, extractCompanyFromJobId(it.jobId), it.isBookmarked)


                bookmarkViewModel.onGlobalBookmarkStateChangeHandled("HomeFragment")
            }
        }


        bookmarkViewModel.bookmarkStatesMap.observe(viewLifecycleOwner) { bookmarkStates ->
            bookmarkStates?.let {

                if (allJobsCache.isNotEmpty()) {
                    allJobsCache = bookmarkViewModel.applyBookmarkStatesToJobs(allJobsCache)
                }
            }
        }


        bookmarkViewModel.showSuccessMessage.observe(viewLifecycleOwner) { message ->
            message?.let {
                Toast.makeText(requireContext(), it, Toast.LENGTH_SHORT).show()
                bookmarkViewModel.onSuccessMessageShown()
            }
        }


        bookmarkViewModel.showErrorMessage.observe(viewLifecycleOwner) { message ->
            message?.let {
                Toast.makeText(requireContext(), it, Toast.LENGTH_SHORT).show()
                bookmarkViewModel.onErrorMessageShown()
            }
        }
    }


    private fun updateAdapterBookmarkStates(jobTitle: String, jobCompany: String, isBookmarked: Boolean) {
        android.util.Log.d("HomeFragment", "🏠 ========= HOME BOOKMARK UPDATE =========")
        android.util.Log.d("HomeFragment", "🏠 Job: $jobTitle at $jobCompany")
        android.util.Log.d("HomeFragment", "🏠 New bookmark state: $isBookmarked")
        android.util.Log.d("HomeFragment", "🏠 User role: ${UserSession.getUserRole()}")


        if (UserSession.getUserRole() != UserSession.ROLE_BUSINESS_OWNER) {

            val recentAdapter = binding.recyclerViewRecent.adapter as? jobAdapter
            if (recentAdapter != null) {
                android.util.Log.d("HomeFragment", "🏠 Updating recent adapter (${recentAdapter.itemCount} items)")
                recentAdapter.updateJobBookmarkState(jobTitle, jobCompany, isBookmarked)
                android.util.Log.d("HomeFragment", "✅ Recent adapter updated")
            } else {
                android.util.Log.w("HomeFragment", "⚠️ Recent adapter is null")
            }


            val suggestAdapter = binding.recyclerViewSuggest.adapter as? jobAdapter
            if (suggestAdapter != null) {
                android.util.Log.d("HomeFragment", "🏠 Updating suggest adapter (${suggestAdapter.itemCount} items)")
                suggestAdapter.updateJobBookmarkState(jobTitle, jobCompany, isBookmarked)
                android.util.Log.d("HomeFragment", "✅ Suggest adapter updated")
            } else {
                android.util.Log.w("HomeFragment", "⚠️ Suggest adapter is null")
            }

            android.util.Log.d("HomeFragment", "✅ All bookmark states updated in adapters")
            android.util.Log.d("HomeFragment", "🏠 ========= HOME UPDATE COMPLETE =========")
        } else {
            android.util.Log.d("HomeFragment", "📋 Business owner view - no adapters to update")
        }
    }


    private fun extractCompanyFromJobId(jobId: String): String {

        val parts = jobId.split("_")
        return if (parts.size >= 2) parts[1] else ""
    }


    private fun refreshAllJobLists() {
        android.util.Log.d("HomeFragment", "🔄 Refreshing all job lists to sync bookmark states")


        if (UserSession.getUserRole() != UserSession.ROLE_BUSINESS_OWNER) {

            val currentCategoryFilter = getCurrentCategoryFilter()
            initRecent(currentCategoryFilter)


            initSuggest()

            android.util.Log.d("HomeFragment", "✅ All job lists refreshed successfully")
        } else {
            android.util.Log.d("HomeFragment", "📋 Business owner view - no job lists to refresh")
        }
    }


    private fun getCurrentCategoryFilter(): String {

        return "0"
    }

    private fun initRecent(cat: String) {
        lifecycleScope.launch {
            try {

                val cachedJobs = if (allJobsCache.isNotEmpty()) {
                    allJobsCache
                } else {
                    val jobs = mainViewModel.loadDataWithViewCounts()
                    allJobsCache = jobs
                    jobs
                }

                val data: List<JobModel> = if (cat == "0") {
                    cachedJobs.sortedBy { it.category }
                } else {
                    cachedJobs.filter { it.category == cat }
                }

                if (binding.recyclerViewRecent.adapter == null) {
                    binding.recyclerViewRecent.layoutManager = LinearLayoutManager(requireContext(), LinearLayoutManager.HORIZONTAL, false)
                    val recentAdapter = jobAdapter(
                        items = bookmarkViewModel.applyBookmarkStatesToJobs(data),
                        onBookmarkClick = { job ->
                            android.util.Log.d("HomeFragment", "Recent job bookmark clicked: ${job.title}")


                            bookmarkViewModel.toggleBookmark(job)


                            android.util.Log.d("HomeFragment", "Bookmark toggle delegated to centralized state management")
                        }
                    )
                    binding.recyclerViewRecent.adapter = recentAdapter
                } else {

                    (binding.recyclerViewRecent.adapter as jobAdapter).updateData(
                        bookmarkViewModel.applyBookmarkStatesToJobs(data)
                    )
                }
            } catch (e: Exception) {
                android.util.Log.e("HomeFragment", "initRecent: Error loading jobs with view counts", e)

                val data: List<JobModel> = if (cat == "0") {
                    mainViewModel.loadData().map { it.copy(viewCount = 0) }.sortedBy { it.category }
                } else {
                    mainViewModel.loadData().map { it.copy(viewCount = 0) }.filter { it.category == cat }
                }

                if (binding.recyclerViewRecent.adapter == null) {
                    binding.recyclerViewRecent.layoutManager = LinearLayoutManager(requireContext(), LinearLayoutManager.HORIZONTAL, false)
                    val recentAdapter = jobAdapter(
                        items = bookmarkViewModel.applyBookmarkStatesToJobs(data),
                        onBookmarkClick = { job ->
                            android.util.Log.d("HomeFragment", "Recent job bookmark clicked (fallback): ${job.title}")


                            bookmarkViewModel.toggleBookmark(job)


                            android.util.Log.d("HomeFragment", "Bookmark toggle delegated to centralized state management (fallback)")
                        }
                    )
                    binding.recyclerViewRecent.adapter = recentAdapter
                } else {

                    (binding.recyclerViewRecent.adapter as jobAdapter).updateData(
                        bookmarkViewModel.applyBookmarkStatesToJobs(data)
                    )
                }
            }
        }
    }

    private fun initSuggest() {
        binding.progressBarSuggest.visibility = View.VISIBLE

        lifecycleScope.launch {
            try {

                val jobsWithViews = if (allJobsCache.isNotEmpty()) {
                    allJobsCache
                } else {
                    val jobs = mainViewModel.loadDataWithViewCounts()
                    allJobsCache = jobs
                    jobs
                }

                if (binding.recyclerViewSuggest.adapter == null) {
                    binding.recyclerViewSuggest.layoutManager = LinearLayoutManager(requireContext(), LinearLayoutManager.HORIZONTAL, false)
                    val suggestAdapter = jobAdapter(
                        items = bookmarkViewModel.applyBookmarkStatesToJobs(jobsWithViews),
                        onBookmarkClick = { job ->
                            android.util.Log.d("HomeFragment", "Suggested job bookmark clicked: ${job.title}")


                            bookmarkViewModel.toggleBookmark(job)


                            android.util.Log.d("HomeFragment", "Bookmark toggle delegated to centralized state management")
                        }
                    )
                    binding.recyclerViewSuggest.adapter = suggestAdapter
                } else {

                    (binding.recyclerViewSuggest.adapter as jobAdapter).updateData(
                        bookmarkViewModel.applyBookmarkStatesToJobs(jobsWithViews)
                    )
                }
                binding.progressBarSuggest.visibility = View.GONE
            } catch (e: Exception) {
                android.util.Log.e("HomeFragment", "initSuggest: Error loading jobs with view counts", e)

                val jobs = mainViewModel.loadData().map { it.copy(viewCount = 0) }

                if (binding.recyclerViewSuggest.adapter == null) {
                    val suggestAdapter = jobAdapter(
                        items = bookmarkViewModel.applyBookmarkStatesToJobs(jobs),
                        onBookmarkClick = { job ->

                            bookmarkViewModel.toggleBookmark(job)
                        }
                    )
                    binding.recyclerViewSuggest.adapter = suggestAdapter
                } else {

                    (binding.recyclerViewSuggest.adapter as jobAdapter).updateData(
                        bookmarkViewModel.applyBookmarkStatesToJobs(jobs)
                    )
                }
                binding.progressBarSuggest.visibility = View.GONE
            }
        }
    }

    private fun hideJobSectionsForBusinessOwner() {

        binding.suggestedJobsHeader.visibility = View.GONE
        binding.suggestedJobsContainer.visibility = View.GONE


        binding.recentJobsHeader.visibility = View.GONE
        binding.categoryContainer.visibility = View.GONE
        binding.recyclerViewRecent.visibility = View.GONE
    }

    private fun showJobSectionsForUsers() {

        binding.suggestedJobsHeader.visibility = View.VISIBLE
        binding.suggestedJobsContainer.visibility = View.VISIBLE


        binding.recentJobsHeader.visibility = View.VISIBLE
        binding.categoryContainer.visibility = View.VISIBLE
        binding.recyclerViewRecent.visibility = View.VISIBLE


        binding.jobManagementSection.visibility = View.GONE
    }

    private fun initJobManagement() {

        if (UserSession.getUserRole() == UserSession.ROLE_BUSINESS_OWNER) {
            showJobManagementContent()
            setupJobManagementRecyclerViews()
            setupCreateJobButton()
            loadOwnedJobs()
        } else {

            binding.jobManagementSection.visibility = View.GONE
        }
    }

    private fun showJobManagementContent() {
        binding.jobManagementSection.visibility = View.VISIBLE


        val userName = UserSession.getUserName() ?: "Business Owner"
        binding.welcomeMessage.text = "Welcome back, $userName!"
        binding.userRoleText.text = "Manage your job postings and view candidates"
    }

    private fun setupJobManagementRecyclerViews() {

        binding.recyclerViewOpenJobs.layoutManager = LinearLayoutManager(requireContext(), LinearLayoutManager.HORIZONTAL, false)
        openJobsAdapter = jobAdapter(
            items = emptyList(),
            onBookmarkClick = null,
            onJobClick = { job ->

                if (job.isOpen() && job.isOwnedByCurrentUser()) {
                    navigateToJobDetails(job)
                } else {
                    showJobActionDialog(job, true)
                }
            }
        )
        binding.recyclerViewOpenJobs.adapter = openJobsAdapter


        binding.recyclerViewClosedJobs.layoutManager = LinearLayoutManager(requireContext(), LinearLayoutManager.HORIZONTAL, false)
        closedJobsAdapter = jobAdapter(
            items = emptyList(),
            onBookmarkClick = null,
            onJobClick = { job ->
                showJobActionDialog(job, false)
            }
        )
        binding.recyclerViewClosedJobs.adapter = closedJobsAdapter
    }


    private fun navigateToJobDetails(job: JobModel) {
        android.util.Log.d("HomeFragment", "🎯 Direct navigation to job details: ${job.title}")
        android.util.Log.d("HomeFragment", "🎯 Job owner: ${job.ownerId}, Current user: ${UserSession.getUserId()}")
        android.util.Log.d("HomeFragment", "🎯 Job status: ${job.status}")

        try {
            val intent = Intent(requireContext(), com.uilover.project196.Activity.DetailActivity::class.java)
            intent.putExtra("object", job)
            startActivity(intent)
            android.util.Log.d("HomeFragment", "🎯 Navigation successful")
        } catch (e: Exception) {
            android.util.Log.e("HomeFragment", "🎯 Navigation failed", e)
            Toast.makeText(requireContext(), "Error opening job details: ${e.message}", Toast.LENGTH_SHORT).show()
        }
    }

    private fun loadOwnedJobs() {
        val openJobs = mainViewModel.getOpenOwnedJobs()
        val closedJobs = mainViewModel.getClosedOwnedJobs()


        if (openJobs.isNotEmpty()) {
            binding.openJobsSection.visibility = View.VISIBLE
            binding.emptyOpenJobsMessage.visibility = View.GONE
            binding.recyclerViewOpenJobs.visibility = View.VISIBLE
            openJobsAdapter.updateData(openJobs)
            binding.openJobsCount.text = "${openJobs.size} Active Job${if (openJobs.size != 1) "s" else ""}"
        } else {
            binding.openJobsSection.visibility = View.VISIBLE
            binding.emptyOpenJobsMessage.visibility = View.VISIBLE
            binding.recyclerViewOpenJobs.visibility = View.GONE
            binding.openJobsCount.text = "0 Active Jobs"
        }


        if (closedJobs.isNotEmpty()) {
            binding.closedJobsSection.visibility = View.VISIBLE
            binding.emptyClosedJobsMessage.visibility = View.GONE
            binding.recyclerViewClosedJobs.visibility = View.VISIBLE
            closedJobsAdapter.updateData(closedJobs)
            binding.closedJobsCount.text = "${closedJobs.size} Closed Job${if (closedJobs.size != 1) "s" else ""}"
        } else {
            binding.closedJobsSection.visibility = View.VISIBLE
            binding.emptyClosedJobsMessage.visibility = View.VISIBLE
            binding.recyclerViewClosedJobs.visibility = View.GONE
            binding.closedJobsCount.text = "0 Closed Jobs"
        }
    }

    private fun showJobActionDialog(job: JobModel, isOpen: Boolean) {
        val builder = AlertDialog.Builder(requireContext())
        builder.setTitle(job.title)

        val options = if (isOpen) {
            arrayOf("View Details", "Close Job", "View Candidates", "Cancel")
        } else {
            arrayOf("View Details", "Reopen Job", "View Candidates", "Cancel")
        }

        builder.setItems(options) { dialog, which ->
            when (which) {
                0 -> {

                    navigateToJobDetails(job)
                    dialog.dismiss()
                }
                1 -> {
                    if (isOpen) {
                        closeJob(job)
                    } else {
                        reopenJob(job)
                    }
                }
                2 -> {
                    showCandidatesForJob(job)
                }
                3 -> {

                    dialog.dismiss()
                }
            }
        }

        builder.show()
    }

    private fun closeJob(job: JobModel) {
        val builder = AlertDialog.Builder(requireContext())
        builder.setTitle("Close Job")
        builder.setMessage("Are you sure you want to close this job posting?\n\n\"${job.title}\" at ${job.company}\n\nThis will remove it from public listings and candidates won't be able to apply anymore.")

        builder.setPositiveButton("Close Job") { _, _ ->
            if (mainViewModel.closeJob(job)) {
                Toast.makeText(requireContext(), "Job closed successfully", Toast.LENGTH_SHORT).show()
                loadOwnedJobs()
            } else {
                Toast.makeText(requireContext(), "Failed to close job", Toast.LENGTH_SHORT).show()
            }
        }

        builder.setNegativeButton("Cancel") { dialog, _ ->
            dialog.dismiss()
        }

        builder.show()
    }

    private fun reopenJob(job: JobModel) {
        val builder = AlertDialog.Builder(requireContext())
        builder.setTitle("Reopen Job")
        builder.setMessage("Are you sure you want to reopen this job posting?\n\n\"${job.title}\" at ${job.company}\n\nThis will make it visible in public listings again and candidates will be able to apply.")

        builder.setPositiveButton("Reopen Job") { _, _ ->
            if (mainViewModel.reopenJob(job)) {
                Toast.makeText(requireContext(), "Job reopened successfully", Toast.LENGTH_SHORT).show()
                loadOwnedJobs()
            } else {
                Toast.makeText(requireContext(), "Failed to reopen job", Toast.LENGTH_SHORT).show()
            }
        }

        builder.setNegativeButton("Cancel") { dialog, _ ->
            dialog.dismiss()
        }

        builder.show()
    }

    private fun showCandidatesForJob(job: JobModel) {

        val candidates = getCandidatesForJob(job)

        if (candidates.isNotEmpty()) {
            val candidateNames = candidates.joinToString("\n") { "• ${it.name} - ${it.title}" }

            val builder = AlertDialog.Builder(requireContext())
            builder.setTitle("Candidates for: ${job.title}")
            builder.setMessage("The following candidates have applied:\n\n$candidateNames")
            builder.setPositiveButton("OK") { dialog, _ ->
                dialog.dismiss()
            }
            builder.show()
        } else {
            Toast.makeText(requireContext(), "No candidates have applied to this job yet", Toast.LENGTH_SHORT).show()
        }
    }

    private fun getCandidatesForJob(job: JobModel): List<CandidateModel> {


        return when (job.category) {
            "2" -> listOf(
                CandidateModel(
                    id = "1",
                    name = "Alex Johnson",
                    title = "Senior UI/UX Designer",
                    experience = "5+ years",
                    skills = listOf("Figma", "Adobe XD", "Sketch", "UI Design"),
                    rating = 4.8f,
                    completedProjects = 47,
                    appliedDate = "2 days ago",
                    status = "Pending Review"
                ),
                CandidateModel(
                    id = "4",
                    name = "Sarah Wilson",
                    title = "UX/UI Designer",
                    experience = "6+ years",
                    skills = listOf("Figma", "Adobe XD", "Sketch", "Prototyping"),
                    rating = 4.9f,
                    completedProjects = 51,
                    appliedDate = "1 week ago",
                    status = "Shortlisted"
                )
            )
            "1" -> listOf(
                CandidateModel(
                    id = "6",
                    name = "Michael Brown",
                    title = "Senior Accountant",
                    experience = "8+ years",
                    skills = listOf("QuickBooks", "Excel", "Financial Analysis", "Tax Preparation"),
                    rating = 4.7f,
                    completedProjects = 35,
                    appliedDate = "3 days ago",
                    status = "Under Review"
                )
            )
            "3" -> listOf(
                CandidateModel(
                    id = "7",
                    name = "Emily Davis",
                    title = "Content Writer & Editor",
                    experience = "4+ years",
                    skills = listOf("Content Writing", "SEO", "Copywriting", "Editing"),
                    rating = 4.8f,
                    completedProjects = 42,
                    appliedDate = "1 day ago",
                    status = "New Application"
                )
            )
            else -> emptyList()
        }
    }


    data class CandidateModel(
        val id: String,
        val name: String,
        val title: String,
        val experience: String,
        val skills: List<String>,
        val rating: Float,
        val completedProjects: Int,
        val appliedDate: String,
        val status: String
    )

    private fun initCategory() {
        binding.progressBarCategory.visibility = View.VISIBLE
        binding.recyclerViewCategory.layoutManager = LinearLayoutManager(requireContext(), LinearLayoutManager.HORIZONTAL, false)
        binding.recyclerViewCategory.adapter = CategoryAdapter(mainViewModel.loadCategory(), object : CategoryAdapter.ClickListener {
            override fun onClick(category: String) {
                initRecent(category)
            }
        })
        binding.progressBarCategory.visibility = View.GONE
    }



    private fun setupTouchHandling() {

        val scrollListener = object : RecyclerView.OnScrollListener() {
            override fun onScrollStateChanged(recyclerView: RecyclerView, newState: Int) {
                super.onScrollStateChanged(recyclerView, newState)
                val viewPager = requireActivity().findViewById<ViewPager2>(com.uilover.project196.R.id.viewPager)

                when (newState) {
                    RecyclerView.SCROLL_STATE_DRAGGING -> {

                        viewPager.isUserInputEnabled = false
                    }
                    RecyclerView.SCROLL_STATE_SETTLING -> {

                        viewPager.isUserInputEnabled = false
                    }
                    RecyclerView.SCROLL_STATE_IDLE -> {

                        viewPager.isUserInputEnabled = true
                    }
                }
            }
        }


        binding.recyclerViewSuggest.addOnScrollListener(scrollListener)
        binding.recyclerViewOpenJobs.addOnScrollListener(scrollListener)
        binding.recyclerViewClosedJobs.addOnScrollListener(scrollListener)
        binding.recyclerViewCategory.addOnScrollListener(scrollListener)
        binding.recyclerViewRecent.addOnScrollListener(scrollListener)


        val touchListener = View.OnTouchListener { view, event ->
            when (event.action) {
                MotionEvent.ACTION_DOWN -> {

                    view.parent.requestDisallowInterceptTouchEvent(true)

                    val viewPager = requireActivity().findViewById<ViewPager2>(com.uilover.project196.R.id.viewPager)
                    viewPager.isUserInputEnabled = false
                }
                MotionEvent.ACTION_UP, MotionEvent.ACTION_CANCEL -> {

                    val viewPager = requireActivity().findViewById<ViewPager2>(com.uilover.project196.R.id.viewPager)
                    viewPager.isUserInputEnabled = true
                }
            }
            false
        }


        binding.recyclerViewSuggest.setOnTouchListener(touchListener)
        binding.recyclerViewOpenJobs.setOnTouchListener(touchListener)
        binding.recyclerViewClosedJobs.setOnTouchListener(touchListener)
        binding.recyclerViewCategory.setOnTouchListener(touchListener)
        binding.recyclerViewRecent.setOnTouchListener(touchListener)
    }

    private fun setupSeeAllClickListeners() {

        binding.textView6.setOnClickListener {
            val viewPager = requireActivity().findViewById<androidx.viewpager2.widget.ViewPager2>(com.uilover.project196.R.id.viewPager)
            viewPager.currentItem = 1
        }


        binding.textView7.setOnClickListener {
            val viewPager = requireActivity().findViewById<androidx.viewpager2.widget.ViewPager2>(com.uilover.project196.R.id.viewPager)
            viewPager.currentItem = 1
        }
    }

    override fun onResume() {
        super.onResume()

        refreshBasedOnLoginState()
    }

    override fun onLoginStateRefresh(isLoggedIn: Boolean) {

        refreshBasedOnLoginState()
    }

    private fun refreshBasedOnLoginState() {

        if (UserSession.getUserRole() == UserSession.ROLE_BUSINESS_OWNER) {
            hideJobSectionsForBusinessOwner()
            initJobManagement()
        } else {
            showJobSectionsForUsers()
            refreshAllData()
        }
    }

    private fun refreshAllData() {

        if (_binding != null && binding.recyclerViewSuggest.adapter != null) {
            lifecycleScope.launch {
                try {
                    val jobsWithViews = mainViewModel.loadDataWithViewCounts()

                    (binding.recyclerViewSuggest.adapter as jobAdapter).updateData(
                        bookmarkViewModel.applyBookmarkStatesToJobs(jobsWithViews)
                    )
                } catch (e: Exception) {
                    android.util.Log.e("HomeFragment", "refreshAllData: Error refreshing jobs with view counts", e)

                    val fallbackJobs = mainViewModel.loadData().map { it.copy(viewCount = 0) }
                    (binding.recyclerViewSuggest.adapter as jobAdapter).updateData(
                        bookmarkViewModel.applyBookmarkStatesToJobs(fallbackJobs)
                    )
                }
            }
        }


        if (_binding != null && binding.recyclerViewRecent.adapter != null) {


            val updatedData = mainViewModel.loadData().sortedBy { it.category }

            (binding.recyclerViewRecent.adapter as jobAdapter).updateData(
                bookmarkViewModel.applyBookmarkStatesToJobs(updatedData)
            )
        }
    }

    private fun setupCreateJobButton() {
        binding.createJobCard.setOnClickListener {
            showCreateJobDialog()
        }
    }

    private fun showCreateJobDialog() {
        val dialog = Dialog(requireContext())
        dialog.requestWindowFeature(Window.FEATURE_NO_TITLE)
        val dialogBinding = DialogCreateJobBinding.inflate(layoutInflater)

        dialog.setContentView(dialogBinding.root)


        val window = dialog.window
        window?.setLayout(ViewGroup.LayoutParams.MATCH_PARENT, ViewGroup.LayoutParams.WRAP_CONTENT)
        window?.setBackgroundDrawableResource(android.R.color.transparent)

        setupCreateJobDialog(dialogBinding, dialog)

        dialog.show()
    }

    private fun setupCreateJobDialog(dialogBinding: DialogCreateJobBinding, dialog: Dialog) {

        jobManagementViewModel.initializeFormForCreate()
        jobManagementViewModel.refreshCompanyInfo()


        // Set company name from user session or database
        lifecycleScope.launch {
            try {
                val userRepository = com.uilover.project196.Repository.UserRepository.getInstance(requireContext())
                val userId = UserSession.getUserId()
                val userEntity = if (userId != null) userRepository.getUserById(userId) else null
                val userCompanyName = userEntity?.companyName ?: "Your Company"
                
                dialogBinding.textViewCompany.text = userCompanyName
                // Also set it in the JobManagementViewModel so it's used during actual job creation
                jobManagementViewModel.setCompanyName(userCompanyName)
                android.util.Log.d("HomeFragment", "Setting create job dialog company name to: '$userCompanyName'")
            } catch (e: Exception) {
                android.util.Log.e("HomeFragment", "Error getting user company name", e)
                val fallbackCompany = "Your Company"
                dialogBinding.textViewCompany.text = fallbackCompany
                jobManagementViewModel.setCompanyName(fallbackCompany)
            }
        }


        setupCreateJobDialogReactiveBinding(dialogBinding, jobManagementViewModel)
        setupCreateJobDialogLiveDataObservers(dialog)


        val filterOptions = mainViewModel.getFilterOptions()


        val salaryAdapter = ArrayAdapter(requireContext(), R.layout.spinner_item, filterOptions.salaryRanges)
        salaryAdapter.setDropDownViewResource(android.R.layout.simple_spinner_dropdown_item)
        dialogBinding.spinnerSalary.adapter = salaryAdapter


        val jobTypeAdapter = ArrayAdapter(requireContext(), R.layout.spinner_item, filterOptions.jobTypes)
        jobTypeAdapter.setDropDownViewResource(android.R.layout.simple_spinner_dropdown_item)
        dialogBinding.spinnerJobType.adapter = jobTypeAdapter


        val workingModelAdapter = ArrayAdapter(requireContext(), R.layout.spinner_item, filterOptions.workingModels)
        workingModelAdapter.setDropDownViewResource(android.R.layout.simple_spinner_dropdown_item)
        dialogBinding.spinnerWorkingModel.adapter = workingModelAdapter


        val levelAdapter = ArrayAdapter(requireContext(), R.layout.spinner_item, filterOptions.levels)
        levelAdapter.setDropDownViewResource(android.R.layout.simple_spinner_dropdown_item)
        dialogBinding.spinnerLevel.adapter = levelAdapter


        dialogBinding.spinnerSalary.onItemSelectedListener = object : AdapterView.OnItemSelectedListener {
            override fun onItemSelected(parent: AdapterView<*>?, view: View?, position: Int, id: Long) {
                jobManagementViewModel.validateSalary(filterOptions.salaryRanges[position])
            }
            override fun onNothingSelected(parent: AdapterView<*>?) {}
        }

        dialogBinding.spinnerJobType.onItemSelectedListener = object : AdapterView.OnItemSelectedListener {
            override fun onItemSelected(parent: AdapterView<*>?, view: View?, position: Int, id: Long) {
                jobManagementViewModel.validateJobType(filterOptions.jobTypes[position])
            }
            override fun onNothingSelected(parent: AdapterView<*>?) {}
        }

        dialogBinding.spinnerWorkingModel.onItemSelectedListener = object : AdapterView.OnItemSelectedListener {
            override fun onItemSelected(parent: AdapterView<*>?, view: View?, position: Int, id: Long) {
                jobManagementViewModel.validateWorkingModel(filterOptions.workingModels[position])
            }
            override fun onNothingSelected(parent: AdapterView<*>?) {}
        }

        dialogBinding.spinnerLevel.onItemSelectedListener = object : AdapterView.OnItemSelectedListener {
            override fun onItemSelected(parent: AdapterView<*>?, view: View?, position: Int, id: Long) {
                jobManagementViewModel.validateLevel(filterOptions.levels[position])
            }
            override fun onNothingSelected(parent: AdapterView<*>?) {}
        }



        if (filterOptions.salaryRanges.isNotEmpty()) {
            dialogBinding.spinnerSalary.setSelection(0)
        }
        if (filterOptions.jobTypes.isNotEmpty()) {
            dialogBinding.spinnerJobType.setSelection(0)
        }
        if (filterOptions.workingModels.isNotEmpty()) {
            dialogBinding.spinnerWorkingModel.setSelection(0)
        }
        if (filterOptions.levels.isNotEmpty()) {
            dialogBinding.spinnerLevel.setSelection(0)
        }


        dialogBinding.cancelCreateJob.setOnClickListener {
            dialog.dismiss()
        }

        dialogBinding.closeCreateJob.setOnClickListener {
            dialog.dismiss()
        }


        dialogBinding.createJobButton.setOnClickListener {
            jobManagementViewModel.createJob { success ->
                if (success) {

                    try {
                        if (dialog.isShowing) {
                            dialog.dismiss()
                        }
                    } catch (e: Exception) {
                        android.util.Log.e("HomeFragment", "Error closing dialog", e)
                    }
                    loadOwnedJobs()
                } else {
                    android.util.Log.e("HomeFragment", "Job creation failed - dialog stays open for user to fix errors")
                }
            }
        }
    }




    private fun setupCreateJobDialogReactiveBinding(dialogBinding: DialogCreateJobBinding, viewModel: JobManagementViewModel) {


        var titleValidationJob: kotlinx.coroutines.Job? = null
        var descriptionValidationJob: kotlinx.coroutines.Job? = null


        dialogBinding.editTextJobTitle.addTextChangedListener(object : android.text.TextWatcher {
            override fun beforeTextChanged(s: CharSequence?, start: Int, count: Int, after: Int) {}
            override fun onTextChanged(s: CharSequence?, start: Int, before: Int, count: Int) {

                titleValidationJob?.cancel()
                titleValidationJob = kotlinx.coroutines.CoroutineScope(kotlinx.coroutines.Dispatchers.Main).launch {
                    kotlinx.coroutines.delay(150)
                    viewModel.validateJobTitle(s.toString())
                }
            }
            override fun afterTextChanged(s: android.text.Editable?) {}
        })


        dialogBinding.editTextJobDescription.addTextChangedListener(object : android.text.TextWatcher {
            override fun beforeTextChanged(s: CharSequence?, start: Int, count: Int, after: Int) {}
            override fun onTextChanged(s: CharSequence?, start: Int, before: Int, count: Int) {

                descriptionValidationJob?.cancel()
                descriptionValidationJob = kotlinx.coroutines.CoroutineScope(kotlinx.coroutines.Dispatchers.Main).launch {
                    kotlinx.coroutines.delay(150)
                    viewModel.validateJobDescription(s.toString())
                }
            }
            override fun afterTextChanged(s: android.text.Editable?) {}
        })


        viewModel.jobTitleError.addOnPropertyChangedCallback(object : androidx.databinding.Observable.OnPropertyChangedCallback() {
            override fun onPropertyChanged(sender: androidx.databinding.Observable?, propertyId: Int) {
                dialogBinding.editTextJobTitle.error = if (viewModel.jobTitleError.get()?.isNotEmpty() == true)
                    viewModel.jobTitleError.get() else null
            }
        })

        viewModel.jobDescriptionError.addOnPropertyChangedCallback(object : androidx.databinding.Observable.OnPropertyChangedCallback() {
            override fun onPropertyChanged(sender: androidx.databinding.Observable?, propertyId: Int) {
                dialogBinding.editTextJobDescription.error = if (viewModel.jobDescriptionError.get()?.isNotEmpty() == true)
                    viewModel.jobDescriptionError.get() else null
            }
        })


        viewModel.submitButtonEnabled.addOnPropertyChangedCallback(object : androidx.databinding.Observable.OnPropertyChangedCallback() {
            override fun onPropertyChanged(sender: androidx.databinding.Observable?, propertyId: Int) {
                dialogBinding.createJobButton.isEnabled = viewModel.submitButtonEnabled.get() ?: false
                dialogBinding.createJobButton.alpha = if (viewModel.submitButtonEnabled.get() ?: false) 1.0f else 0.6f
            }
        })


        viewModel.submitButtonText.addOnPropertyChangedCallback(object : androidx.databinding.Observable.OnPropertyChangedCallback() {
            override fun onPropertyChanged(sender: androidx.databinding.Observable?, propertyId: Int) {
                dialogBinding.createJobButton.text = viewModel.submitButtonText.get() ?: "Create Job"
            }
        })
    }


    private fun setupCreateJobDialogLiveDataObservers(dialog: Dialog) {

        jobManagementViewModel.showSuccessMessage.observe(viewLifecycleOwner) { message ->
            message?.let {
                android.widget.Toast.makeText(requireContext(), it, android.widget.Toast.LENGTH_SHORT).show()
                jobManagementViewModel.onSuccessMessageShown()


                try {
                    if (dialog.isShowing) {
                        dialog.dismiss()
                    }
                } catch (e: Exception) {
                    android.util.Log.e("HomeFragment", "Error closing dialog on success", e)
                }
            }
        }


        jobManagementViewModel.showErrorMessage.observe(viewLifecycleOwner) { message ->
            message?.let {
                android.widget.Toast.makeText(requireContext(), it, android.widget.Toast.LENGTH_SHORT).show()
                jobManagementViewModel.onErrorMessageShown()

            }
        }
    }






    private fun getCompanyLogo(companyName: String): String {
        return when (companyName) {
            "ChabokSoft" -> "logo1"
            "KianSoft" -> "logo2"
            "MakanSoft" -> "logo3"
            "TestSoft" -> "logo4"
            else -> "logo1"
        }
    }

    override fun onDestroyView() {
        super.onDestroyView()
        _binding = null
    }
}