package com.uilover.project196.Activity

import android.os.Build
import android.os.Bundle
import android.view.View
import android.view.WindowManager
import androidx.appcompat.app.AppCompatActivity
import androidx.fragment.app.Fragment
import androidx.fragment.app.FragmentActivity
import androidx.viewpager2.adapter.FragmentStateAdapter
import androidx.lifecycle.ViewModelProvider
import com.bumptech.glide.Glide
import com.uilover.project196.Fragment.AboutFragment
import com.uilover.project196.Fragment.AnalyticsFragment
import com.uilover.project196.Fragment.CandidatesFragment
import com.uilover.project196.Fragment.CompanyFragment
import com.uilover.project196.Fragment.PostingFragment
import com.uilover.project196.Fragment.ReviewFragment
import com.uilover.project196.Fragment.CandidatesProgressFragment
import com.uilover.project196.Model.JobModel
import com.uilover.project196.R
import com.uilover.project196.Utils.UserSession
import com.uilover.project196.Utils.JobSyncHelper
import com.uilover.project196.Utils.toJobEntity
import com.uilover.project196.ViewModel.MainViewModel
import com.uilover.project196.ViewModel.JobApplicationViewModel
import com.uilover.project196.Repository.UserRepository
import com.uilover.project196.databinding.ActivityDetailBinding
import com.uilover.project196.databinding.DialogApplyJobBinding
import android.widget.Toast
import androidx.core.content.ContextCompat
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import com.google.android.material.tabs.TabLayoutMediator
import kotlinx.coroutines.withContext

// KRITERIA: Multiple Activity (4/8) - Activity untuk detail pekerjaan
// KRITERIA WAJIB: Multiple Activity (4/8) - Activity untuk detail pekerjaan
// KRITERIA KOMPLEKSITAS: 2-way data binding dengan reactive UI
class DetailActivity : AppCompatActivity() {
    lateinit var binding: ActivityDetailBinding
    lateinit var item: JobModel
    private lateinit var mainViewModel: MainViewModel
    private lateinit var jobApplicationViewModel: JobApplicationViewModel
    private lateinit var userRepository: UserRepository

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        binding = ActivityDetailBinding.inflate(layoutInflater)
        setContentView(binding.root)

        window.setFlags(
            WindowManager.LayoutParams.FLAG_LAYOUT_NO_LIMITS,
            WindowManager.LayoutParams.FLAG_LAYOUT_NO_LIMITS
        )


        UserSession.init(this)


        if (!UserSession.isLoggedIn()) {
            android.util.Log.d("DetailActivity", "User not logged in, simulating freelancer login for demo purposes")
            UserSession.simulateFreelancerLogin()
        }


        mainViewModel = ViewModelProvider(this)[MainViewModel::class.java]
        mainViewModel.initializeDatabase(this)
        jobApplicationViewModel = ViewModelProvider(this, ViewModelProvider.AndroidViewModelFactory.getInstance(getApplication()))[JobApplicationViewModel::class.java]
        userRepository = UserRepository.getInstance(this)

        getBundle()
    }

    private fun getBundle() {

        item = if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
            intent.getParcelableExtra("object", JobModel::class.java)!!
        } else {
            @Suppress("DEPRECATION")
            intent.getParcelableExtra("object")!!
        }


        updateJobDetailsUI()


        android.util.Log.d("DetailActivity", "Current user info: ${UserSession.getCurrentUserInfo()}")
        android.util.Log.d("DetailActivity", "Job details: title=${item.title}, company=${item.company}, ownerId=${item.ownerId}")



        CoroutineScope(Dispatchers.Main).launch {

            android.util.Log.d("DetailActivity", "Forcing sync of all jobs to database...")
            try {

                withContext(Dispatchers.IO) {
                    val allJobs = mainViewModel.loadAllData()
                    android.util.Log.d("DetailActivity", "Found ${allJobs.size} jobs to sync")

                    for (job in allJobs) {
                        try {
                            val existingJob = mainViewModel.findJobEntity(job)
                            if (existingJob == null) {
                                val jobEntity = job.toJobEntity()
                                android.util.Log.d("DetailActivity", "Inserting job ${job.title} from ${job.company}")
                                mainViewModel.insertJob(jobEntity)
                            } else {
                                android.util.Log.d("DetailActivity", "Job ${job.title} already exists with ID ${existingJob.id}")
                            }
                        } catch (e: Exception) {
                            android.util.Log.e("DetailActivity", "Error syncing job ${job.title}", e)
                        }
                    }


                    userRepository.initializeSampleData()
                }

                android.util.Log.d("DetailActivity", "Database sync completed")

                kotlinx.coroutines.delay(500)
            } catch (e: Exception) {
                android.util.Log.e("DetailActivity", "Error during database sync", e)
            }


            val viewTracked = trackJobViewAndWait()
            android.util.Log.d("DetailActivity", "View tracking completed: $viewTracked")


            setupViewPager()
            setupBookmarkFunctionality()
            setupApplyButton()


            kotlinx.coroutines.delay(500)
            refreshAboutFragmentStats()
        }

        binding.backBtn.setOnClickListener {
            finish()
        }
    }

    private fun setupBookmarkFunctionality() {

        if (item.isOwnedByCurrentUser()) {
            binding.bookmarkBtn.visibility = View.GONE
            return
        }


        binding.bookmarkBtn.visibility = View.VISIBLE

        binding.bookmarkBtn.setOnClickListener {

            if (mainViewModel.canBookmarkJob(item)) {

                mainViewModel.toggleBookmark(item)


                item.isBookmarked = mainViewModel.isJobBookmarked(item)


                updateBookmarkIcon()
            }
        }
    }

    private fun updateBookmarkIcon() {

        if (item.isOwnedByCurrentUser()) {
            binding.bookmarkBtn.visibility = View.GONE
            return
        }


        val isBookmarked = mainViewModel.isJobBookmarked(item)
        item.isBookmarked = isBookmarked

        if (isBookmarked) {
            binding.bookmarkBtn.setImageResource(R.drawable.ic_bookmark_filled_white)
        } else {
            binding.bookmarkBtn.setImageResource(R.drawable.ic_bookmark_outline_white)
        }
    }

    private fun setupViewPager() {

        val isJobOwner = item.isOwnedByCurrentUser()
        val isBusinessOwner = UserSession.getUserRole() == UserSession.ROLE_BUSINESS_OWNER

        val fragments = mutableListOf<Fragment>()
        val tabTitles = mutableListOf<String>()

        if (isJobOwner && isBusinessOwner) {

            val tab1 = PostingFragment()
            val tab2 = CandidatesFragment()

            val tab3 = CandidatesProgressFragment.newInstanceForJob(item.title, item.company, item.ownerId)
            val tab4 = AnalyticsFragment()

            tab1.arguments = Bundle()
            tab2.arguments = Bundle()
            tab4.arguments = Bundle()

            fragments.addAll(listOf(tab1, tab2, tab3, tab4))
            tabTitles.addAll(listOf("Posting", "Candidates", "Candidates Progress", "Analytics"))
        } else {

            val tab1 = AboutFragment()
            val tab2 = CompanyFragment()
            val tab3 = ReviewFragment()

            val bundle1 = Bundle()
            bundle1.putString("description", item.description)
            bundle1.putString("about", item.about)

            val bundle2 = Bundle()
            bundle2.putString("company", item.company)
            bundle2.putString("picUrl", item.picUrl)

            val bundle3 = Bundle()
            bundle3.putString("businessOwnerId", item.ownerId)

            tab1.arguments = bundle1
            tab2.arguments = bundle2
            tab3.arguments = bundle3

            fragments.addAll(listOf(tab1, tab2, tab3))
            tabTitles.addAll(listOf("About", "Company", "Review"))
        }

        val adapter = ViewPagerAdapter(this, fragments)
        binding.viewpager.adapter = adapter

        TabLayoutMediator(binding.tabLayout, binding.viewpager) { tab, position ->
            tab.text = tabTitles[position]
        }.attach()


        binding.tabLayout.addOnTabSelectedListener(object : com.google.android.material.tabs.TabLayout.OnTabSelectedListener {
            override fun onTabSelected(tab: com.google.android.material.tabs.TabLayout.Tab?) {
                if (tab?.text == "About") {

                    refreshAboutFragmentStats()
                }
            }

            override fun onTabUnselected(tab: com.google.android.material.tabs.TabLayout.Tab?) {}

            override fun onTabReselected(tab: com.google.android.material.tabs.TabLayout.Tab?) {
                if (tab?.text == "About") {

                    refreshAboutFragmentStats()
                }
            }
        })
    }

    private fun setupApplyButton() {

        if (UserSession.getUserRole() == UserSession.ROLE_BUSINESS_OWNER || item.isOwnedByCurrentUser()) {
            binding.applyJobBtn.visibility = View.GONE

            val layoutParams = binding.scrollView.layoutParams as androidx.constraintlayout.widget.ConstraintLayout.LayoutParams
            layoutParams.bottomToBottom = androidx.constraintlayout.widget.ConstraintLayout.LayoutParams.PARENT_ID
            layoutParams.bottomToTop = androidx.constraintlayout.widget.ConstraintLayout.LayoutParams.UNSET
            binding.scrollView.layoutParams = layoutParams
        } else {

            binding.applyJobBtn.visibility = View.VISIBLE


            val layoutParams = binding.scrollView.layoutParams as androidx.constraintlayout.widget.ConstraintLayout.LayoutParams
            layoutParams.bottomToTop = binding.applyJobBtn.id
            layoutParams.bottomToBottom = androidx.constraintlayout.widget.ConstraintLayout.LayoutParams.UNSET
            layoutParams.bottomMargin = 8
            binding.scrollView.layoutParams = layoutParams

            binding.applyJobBtn.setOnClickListener {

                applyToJob()
            }


            binding.applyJobBtn.isEnabled = false
            binding.applyJobBtn.text = "Checking..."
            binding.applyJobBtn.alpha = 0.7f


            checkApplicationStatus()
        }
    }

    private fun applyToJob() {
        val currentUserId = UserSession.getUserId()
        if (currentUserId == null) {
            android.widget.Toast.makeText(this, "Silakan login terlebih dahulu untuk melamar pekerjaan", android.widget.Toast.LENGTH_SHORT).show()
            return
        }


        showApplicationFormDialog()
    }

    private fun showApplicationFormDialog() {

        jobApplicationViewModel.initializeForm(item)


        val dialogBinding = DialogApplyJobBinding.inflate(layoutInflater)
        val dialog = android.app.AlertDialog.Builder(this)
            .setView(dialogBinding.root)
            .setCancelable(false)
            .create()


        dialog.window?.setBackgroundDrawableResource(android.R.color.transparent)


        setupApplicationDialogReactiveBinding(dialogBinding, jobApplicationViewModel)
        setupApplicationDialogLiveDataObservers(dialog)


        dialogBinding.cancelButton.setOnClickListener {
            dialog.dismiss()
        }

        dialogBinding.applyButton.setOnClickListener {
            jobApplicationViewModel.submitApplication(item, mainViewModel)
        }

        dialog.show()
    }




    private fun setupApplicationDialogReactiveBinding(dialogBinding: DialogApplyJobBinding, viewModel: JobApplicationViewModel) {


        var skillsValidationJob: kotlinx.coroutines.Job? = null
        var descriptionValidationJob: kotlinx.coroutines.Job? = null


        dialogBinding.skillsEditText.addTextChangedListener(object : android.text.TextWatcher {
            override fun beforeTextChanged(s: CharSequence?, start: Int, count: Int, after: Int) {}
            override fun onTextChanged(s: CharSequence?, start: Int, before: Int, count: Int) {

                skillsValidationJob?.cancel()
                skillsValidationJob = CoroutineScope(Dispatchers.Main).launch {
                    kotlinx.coroutines.delay(150)
                    viewModel.validateSkills(s.toString())
                }
            }
            override fun afterTextChanged(s: android.text.Editable?) {}
        })


        dialogBinding.descriptionEditText.addTextChangedListener(object : android.text.TextWatcher {
            override fun beforeTextChanged(s: CharSequence?, start: Int, count: Int, after: Int) {}
            override fun onTextChanged(s: CharSequence?, start: Int, before: Int, count: Int) {

                descriptionValidationJob?.cancel()
                descriptionValidationJob = CoroutineScope(Dispatchers.Main).launch {
                    kotlinx.coroutines.delay(150)
                    viewModel.validateDescription(s.toString())
                }
            }
            override fun afterTextChanged(s: android.text.Editable?) {}
        })


        viewModel.skillsError.addOnPropertyChangedCallback(object : androidx.databinding.Observable.OnPropertyChangedCallback() {
            override fun onPropertyChanged(sender: androidx.databinding.Observable?, propertyId: Int) {
                val errorMessage = viewModel.skillsError.get()
                dialogBinding.skillsEditText.error = if (errorMessage.isNullOrEmpty()) null else errorMessage
            }
        })

        viewModel.descriptionError.addOnPropertyChangedCallback(object : androidx.databinding.Observable.OnPropertyChangedCallback() {
            override fun onPropertyChanged(sender: androidx.databinding.Observable?, propertyId: Int) {
                val errorMessage = viewModel.descriptionError.get()
                dialogBinding.descriptionEditText.error = if (errorMessage.isNullOrEmpty()) null else errorMessage
            }
        })

        viewModel.submitButtonEnabled.addOnPropertyChangedCallback(object : androidx.databinding.Observable.OnPropertyChangedCallback() {
            override fun onPropertyChanged(sender: androidx.databinding.Observable?, propertyId: Int) {
                dialogBinding.applyButton.isEnabled = viewModel.submitButtonEnabled.get() ?: false
            }
        })

        viewModel.submitButtonText.addOnPropertyChangedCallback(object : androidx.databinding.Observable.OnPropertyChangedCallback() {
            override fun onPropertyChanged(sender: androidx.databinding.Observable?, propertyId: Int) {
                dialogBinding.applyButton.text = viewModel.submitButtonText.get()
            }
        })

        viewModel.characterCount.addOnPropertyChangedCallback(object : androidx.databinding.Observable.OnPropertyChangedCallback() {
            override fun onPropertyChanged(sender: androidx.databinding.Observable?, propertyId: Int) {


            }
        })
    }




    private fun setupApplicationDialogLiveDataObservers(dialog: android.app.AlertDialog) {

        jobApplicationViewModel.applicationResult.observe(this) { success ->
            success?.let {
                if (it) {
                    // Refresh application status instead of hardcoding the button state
                    checkApplicationStatus()
                }
                dialog.dismiss()
                jobApplicationViewModel.onApplicationResultHandled()
            }
        }

        jobApplicationViewModel.showSuccessMessage.observe(this) { message ->
            message?.let {
                Toast.makeText(this, it, Toast.LENGTH_SHORT).show()
                jobApplicationViewModel.onSuccessMessageShown()
            }
        }

        jobApplicationViewModel.showErrorMessage.observe(this) { message ->
            message?.let {
                Toast.makeText(this, it, Toast.LENGTH_SHORT).show()
                jobApplicationViewModel.onErrorMessageShown()
            }
        }
    }



    private fun checkApplicationStatus() {
        val currentUserId = UserSession.getUserId() ?: return


        CoroutineScope(Dispatchers.IO).launch {
            try {

                val jobEntity = mainViewModel.findJobEntity(item)
                if (jobEntity != null) {
                    // Get the specific application status instead of just checking if applied
                    val applicationStatus = userRepository.getUserApplicationStatus(jobEntity.id, currentUserId)


                    withContext(Dispatchers.Main) {
                        when (applicationStatus) {
                            "rejected" -> {
                                // Show rejected status with dark red disabled background
                                binding.applyJobBtn.isEnabled = false
                                binding.applyJobBtn.text = "Lamaran Ditolak"
                                binding.applyJobBtn.alpha = 0.7f
                                binding.applyJobBtn.setBackgroundResource(R.drawable.red_bg_disabled)
                                binding.applyJobBtn.setTextColor(ContextCompat.getColor(this@DetailActivity, R.color.white))
                            }
                            "pending", "shortlisted", "hired" -> {
                                // Show applied status with default purple background
                                binding.applyJobBtn.isEnabled = false
                                binding.applyJobBtn.text = "Sudah Melamar"
                                binding.applyJobBtn.alpha = 0.6f
                                binding.applyJobBtn.setBackgroundResource(R.drawable.purple_bg)
                                binding.applyJobBtn.setTextColor(ContextCompat.getColor(this@DetailActivity, R.color.white))
                            }
                            null -> {
                                // No application found, user can apply
                                binding.applyJobBtn.isEnabled = true
                                binding.applyJobBtn.text = "Apply for Job"
                                binding.applyJobBtn.alpha = 1.0f
                                binding.applyJobBtn.setBackgroundResource(R.drawable.purple_bg)
                                binding.applyJobBtn.setTextColor(ContextCompat.getColor(this@DetailActivity, R.color.white))
                            }
                        }
                    }
                } else {

                    withContext(Dispatchers.Main) {
                        binding.applyJobBtn.isEnabled = true
                        binding.applyJobBtn.text = "Apply for Job"
                        binding.applyJobBtn.alpha = 1.0f
                        binding.applyJobBtn.setBackgroundResource(R.drawable.purple_bg)
                        binding.applyJobBtn.setTextColor(ContextCompat.getColor(this@DetailActivity, R.color.white))
                    }
                }
            } catch (e: Exception) {
                android.util.Log.e("DetailActivity", "Error checking application status", e)

                withContext(Dispatchers.Main) {
                    binding.applyJobBtn.isEnabled = true
                    binding.applyJobBtn.text = "Apply for Job"
                    binding.applyJobBtn.alpha = 1.0f
                    binding.applyJobBtn.setBackgroundResource(R.drawable.purple_bg)
                    binding.applyJobBtn.setTextColor(ContextCompat.getColor(this@DetailActivity, R.color.white))
                }
            }
        }
    }




    private suspend fun trackJobViewAndWait(): Boolean {
        val currentUserId = UserSession.getUserId()
        val userRole = UserSession.getUserRole()

        android.util.Log.d("DetailActivity", "=== STARTING VIEW TRACKING ===")
        android.util.Log.d("DetailActivity", "trackJobViewAndWait: userId=$currentUserId, role=$userRole, jobTitle=${item.title}")
        android.util.Log.d("DetailActivity", "trackJobViewAndWait: Job details - company=${item.company}, ownerId=${item.ownerId}, category=${item.category}")

        if (currentUserId == null) {
            android.util.Log.e("DetailActivity", "trackJobViewAndWait: User not logged in")
            return false
        }


        if (userRole != UserSession.ROLE_FREELANCER) {
            android.util.Log.e("DetailActivity", "trackJobViewAndWait: User is not a freelancer, role=$userRole, expected=${UserSession.ROLE_FREELANCER}")
            return false
        }

        return withContext(Dispatchers.IO) {
            try {
                android.util.Log.d("DetailActivity", "=== STEP 1: ENSURE USER EXISTS ===")

                val userName = UserSession.getUserName() ?: "Demo User"
                val userEmail = UserSession.getUserEmail() ?: "demo@example.com"
                android.util.Log.d("DetailActivity", "trackJobViewAndWait: Ensuring user in database - userId=$currentUserId, name=$userName, email=$userEmail, role=$userRole")

                val userEnsured = userRepository.ensureUserInDatabase(currentUserId, userName, userEmail, userRole)
                android.util.Log.d("DetailActivity", "trackJobViewAndWait: User ensured in database: $userEnsured")

                if (!userEnsured) {
                    android.util.Log.e("DetailActivity", "trackJobViewAndWait: FAILED to ensure user exists in database")
                    return@withContext false
                }

                android.util.Log.d("DetailActivity", "=== STEP 2: ENSURE JOB EXISTS ===")

                val jobSyncHelper = com.uilover.project196.Utils.JobSyncHelper.getInstance()
                jobSyncHelper.initialize(this@DetailActivity)

                android.util.Log.d("DetailActivity", "trackJobViewAndWait: Ensuring job is in database: title=${item.title}, company=${item.company}, ownerId=${item.ownerId}")


                val jobId = jobSyncHelper.ensureJobInDatabase(item)

                android.util.Log.d("DetailActivity", "trackJobViewAndWait: Job ID from database=${jobId}")

                if (jobId != null) {
                    android.util.Log.d("DetailActivity", "=== STEP 3: TRACK THE VIEW ===")

                    val existingViews = userRepository.hasUserEverViewedJob(jobId, currentUserId)
                    android.util.Log.d("DetailActivity", "trackJobViewAndWait: Job ID=${jobId}, Existing views for user $currentUserId = $existingViews")


                    android.util.Log.d("DetailActivity", "trackJobViewAndWait: Calling userRepository.trackJobView with jobId=$jobId, viewerUserId=$currentUserId")
                    userRepository.trackJobView(
                        jobId = jobId,
                        viewerUserId = currentUserId,
                        sessionId = System.currentTimeMillis().toString()
                    )


                    kotlinx.coroutines.delay(200)
                    val viewsAfterTracking = userRepository.hasUserEverViewedJob(jobId, currentUserId)
                    android.util.Log.d("DetailActivity", "trackJobViewAndWait: Views after tracking = $viewsAfterTracking")

                    if (viewsAfterTracking > existingViews) {
                        android.util.Log.d("DetailActivity", "=== ✅ VIEW TRACKING SUCCESS ===")
                        true
                    } else {
                        android.util.Log.e("DetailActivity", "=== ❌ VIEW TRACKING FAILED - no increase in view count ===")
                        false
                    }
                } else {
                    android.util.Log.e("DetailActivity", "trackJobViewAndWait: FAILED to ensure job exists in database for job: ${item.title}")
                    false
                }
            } catch (e: Exception) {
                android.util.Log.e("DetailActivity", "trackJobViewAndWait: EXCEPTION during view tracking", e)
                false
            }
        }
    }

    private fun refreshAboutFragmentStats() {

        val fragments = supportFragmentManager.fragments
        android.util.Log.d("DetailActivity", "refreshAboutFragmentStats: Total fragments=${fragments.size}")

        var aboutFragmentFound = false
        fragments.forEach { fragment ->
            android.util.Log.d("DetailActivity", "refreshAboutFragmentStats: Fragment type=${fragment::class.simpleName}, isVisible=${fragment.isVisible}")
            if (fragment is AboutFragment && fragment.isVisible) {
                android.util.Log.d("DetailActivity", "refreshAboutFragmentStats: Found visible AboutFragment, calling refresh")
                aboutFragmentFound = true
                fragment.refreshJobStatistics()
            }
        }

        if (!aboutFragmentFound) {
            android.util.Log.w("DetailActivity", "refreshAboutFragmentStats: No visible AboutFragment found")
        }
    }

    override fun onResume() {
        super.onResume()

        refreshJobDataFromRepository()
    }

    private fun refreshJobDataFromRepository() {

        val allJobs = mainViewModel.loadData()
        val updatedJob = allJobs.find { job ->
            job.title == item.title &&
            job.company == item.company &&
            job.ownerId == item.ownerId
        }

        if (updatedJob != null) {

            item = updatedJob

            updateJobDetailsUI()
        }
    }


    fun refreshJobData(updatedJob: JobModel) {

        item = updatedJob

        updateJobDetailsUI()
    }


    suspend fun trackViewManually() {
        android.util.Log.d("DetailActivity", "trackViewManually: Called from AboutFragment")


        val currentUserId = UserSession.getUserId()
        val userRole = UserSession.getUserRole()

        if (currentUserId == null || userRole != UserSession.ROLE_FREELANCER) {
            android.util.Log.d("DetailActivity", "trackViewManually: Skipping manual tracking - user not freelancer or not logged in")
            return
        }

        if (item.isOwnedByCurrentUser()) {
            android.util.Log.d("DetailActivity", "trackViewManually: Skipping manual tracking - user owns this job")
            return
        }

        val viewTracked = trackJobViewAndWait()
        android.util.Log.d("DetailActivity", "trackViewManually: View tracking completed: $viewTracked")
    }

    private fun updateJobDetailsUI() {
        binding.titleTxt.text = item.title
        binding.companyTxt.text = item.company




        binding.salaryTxt.text = item.salary
        binding.jobTypeTxt.text = item.time
        binding.workingModelTxt.text = item.model
        binding.levelTxt.text = item.level

        binding.jobTypeTxt.text = item.time
        binding.workingModelTxt.text = item.model
        binding.levelTxt.text = item.level
        binding.salaryTxt.text = item.salary

        val drawableResourceId = resources.getIdentifier(item.picUrl, "drawable", packageName)

        Glide.with(this)
            .load(drawableResourceId)
            .into(binding.picDetail)


        updateBookmarkIcon()


        refreshViewPagerFragments()
    }

    private fun refreshViewPagerFragments() {

        val adapter = binding.viewpager.adapter as? ViewPagerAdapter
        adapter?.let {

            if (item.isOwnedByCurrentUser() && UserSession.getUserRole() == UserSession.ROLE_BUSINESS_OWNER) {
                val fragments = supportFragmentManager.fragments
                fragments.forEach { fragment ->
                    if (fragment is PostingFragment && fragment.isVisible) {

                    }
                }
            }
        }
    }

    private class ViewPagerAdapter(
        fragmentActivity: FragmentActivity,
        private val fragments: List<Fragment>
    ) : FragmentStateAdapter(fragmentActivity) {

        override fun getItemCount(): Int = fragments.size

        override fun createFragment(position: Int): Fragment = fragments[position]
    }
}