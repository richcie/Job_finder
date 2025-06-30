package com.uilover.project196.Fragment

import android.content.Intent
import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.Toast
import androidx.databinding.DataBindingUtil
import androidx.fragment.app.Fragment
import androidx.lifecycle.ViewModelProvider
import androidx.recyclerview.widget.LinearLayoutManager
import com.uilover.project196.Activity.DetailActivity
import com.uilover.project196.Activity.LoginActivity
import com.uilover.project196.Activity.MainActivity
import com.uilover.project196.Activity.NotificationsActivity
import com.uilover.project196.Adapter.jobAdapter
import com.uilover.project196.Model.JobModel
import com.uilover.project196.R
import com.uilover.project196.Repository.NotificationRepository
import com.uilover.project196.Utils.UserSession
import com.uilover.project196.ViewModel.BookmarkViewModel
import com.uilover.project196.ViewModel.MainViewModel
import com.uilover.project196.databinding.FragmentBookmarkBinding

// KRITERIA WAJIB: Multiple Fragment (5/16) - Fragment bookmark pekerjaan
class BookmarkFragment : BaseFragment() {
    private var _binding: FragmentBookmarkBinding? = null
    private val binding get() = _binding!!
    private lateinit var mainViewModel: MainViewModel
    private lateinit var bookmarkViewModel: BookmarkViewModel
    private lateinit var bookmarkAdapter: jobAdapter

    override fun onCreateView(
        inflater: LayoutInflater,
        container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View {

        _binding = FragmentBookmarkBinding.inflate(inflater, container, false)
        return binding.root
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)


        mainViewModel = ViewModelProvider(requireActivity())[MainViewModel::class.java]
        mainViewModel.initializeDatabase(requireContext())


        bookmarkViewModel = ViewModelProvider(
            requireActivity(),
            ViewModelProvider.AndroidViewModelFactory.getInstance(requireActivity().application)
        )[BookmarkViewModel::class.java]


        setupReactiveBinding(bookmarkViewModel)


        setupObservers()
        setupGlobalBookmarkObserver()
        setupClickListeners()
        setupRecyclerView()
        setupNotificationButton(binding.imageView3, binding.textView4)
    }

    private fun setupReactiveBinding(viewModel: BookmarkViewModel) {



        viewModel.bookmarkCount.addOnPropertyChangedCallback(
            object : androidx.databinding.Observable.OnPropertyChangedCallback() {
                override fun onPropertyChanged(sender: androidx.databinding.Observable?, propertyId: Int) {
                    val count = viewModel.bookmarkCount.get() ?: "0"
                    binding.subtitleText.text = when {
                        count == "0" -> "Jobs you want to apply to"
                        count == "1" -> "1 saved job"
                        else -> "$count saved jobs"
                    }
                }
            }
        )


        viewModel.showLoadingState.addOnPropertyChangedCallback(
            object : androidx.databinding.Observable.OnPropertyChangedCallback() {
                override fun onPropertyChanged(sender: androidx.databinding.Observable?, propertyId: Int) {
                    binding.loadingIndicator.visibility =
                        if (viewModel.showLoadingState.get() == true) View.VISIBLE else View.GONE
                }
            }
        )


        viewModel.showBookmarksList.addOnPropertyChangedCallback(
            object : androidx.databinding.Observable.OnPropertyChangedCallback() {
                override fun onPropertyChanged(sender: androidx.databinding.Observable?, propertyId: Int) {
                    binding.recyclerViewBookmarks.visibility =
                        if (viewModel.showBookmarksList.get() == true) View.VISIBLE else View.GONE
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
                    binding.emptyStateTitle.text = viewModel.emptyStateTitle.get() ?: "No Bookmarks Yet"
                }
            }
        )


        viewModel.emptyStateSubtitle.addOnPropertyChangedCallback(
            object : androidx.databinding.Observable.OnPropertyChangedCallback() {
                override fun onPropertyChanged(sender: androidx.databinding.Observable?, propertyId: Int) {
                    binding.emptyStateSubtitle.text = viewModel.emptyStateSubtitle.get() ?: "Start bookmarking jobs you're interested in"
                }
            }
        )


        viewModel.showGuestState.addOnPropertyChangedCallback(
            object : androidx.databinding.Observable.OnPropertyChangedCallback() {
                override fun onPropertyChanged(sender: androidx.databinding.Observable?, propertyId: Int) {
                    binding.guestLoginLayout.visibility =
                        if (viewModel.showGuestState.get() == true) View.VISIBLE else View.GONE
                }
            }
        )
    }





    private fun setupGlobalBookmarkObserver() {

        bookmarkViewModel.globalBookmarkStateChanged.observe(viewLifecycleOwner) { stateChange ->
            stateChange?.let {
                android.util.Log.d("BookmarkFragment", "🔄 Global bookmark state changed: ${it.jobTitle} = ${it.isBookmarked}")


                if (it.isBookmarked) {
                    android.util.Log.d("BookmarkFragment", "Job bookmarked - refreshing bookmark list to add it")
                    bookmarkViewModel.loadBookmarkedJobs()
                } else {
                    android.util.Log.d("BookmarkFragment", "Job unbookmarked - refreshing bookmark list to remove it")
                    bookmarkViewModel.loadBookmarkedJobs()
                }


                bookmarkViewModel.onGlobalBookmarkStateChangeHandled("BookmarkFragment")
            }
        }
    }

    private fun setupObservers() {

        bookmarkViewModel.isLoggedIn.observe(viewLifecycleOwner) { isLoggedIn ->
            updateUIBasedOnLoginState(isLoggedIn)
        }


        bookmarkViewModel.bookmarkedJobs.observe(viewLifecycleOwner) { jobs ->
            android.util.Log.d("BookmarkFragment", "📊 LiveData observer triggered with ${jobs?.size ?: 0} jobs")
            jobs?.let {
                android.util.Log.d("BookmarkFragment", "Processing bookmarked jobs LiveData update")
                updateBookmarksList(it)
            } ?: run {
                android.util.Log.d("BookmarkFragment", "Received null jobs list from LiveData")
            }
        }


        bookmarkViewModel.isLoading.observe(viewLifecycleOwner) { isLoading ->
            updateLoadingState(isLoading)
        }


        bookmarkViewModel.isEmpty.observe(viewLifecycleOwner) { isEmpty ->
            updateEmptyState(isEmpty)
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


        bookmarkViewModel.bookmarkToggled.observe(viewLifecycleOwner) { job ->
            job?.let {
                onBookmarkToggled(it)
                bookmarkViewModel.onBookmarkToggledHandled()
            }
        }


        bookmarkViewModel.jobClicked.observe(viewLifecycleOwner) { job ->
            job?.let {
                navigateToJobDetails(it)
                bookmarkViewModel.onJobClickHandled()
            }
        }
    }

    private fun setupClickListeners() {

        binding.loginPromptButton.setOnClickListener {
            navigateToLogin()
        }
    }

    private fun setupRecyclerView() {
        binding.recyclerViewBookmarks.layoutManager = LinearLayoutManager(requireContext())
        bookmarkAdapter = jobAdapter(
            items = emptyList(),
            onBookmarkClick = { job ->

                bookmarkViewModel.removeBookmark(job)
            }
        )
        binding.recyclerViewBookmarks.adapter = bookmarkAdapter
    }

    private fun updateUIBasedOnLoginState(isLoggedIn: Boolean) {
        if (isLoggedIn) {

            binding.guestLoginLayout.visibility = View.GONE
            showLoggedInState()
        } else {

            binding.recyclerViewBookmarks.visibility = View.GONE
            binding.emptyStateLayout.visibility = View.GONE
            binding.guestLoginLayout.visibility = View.VISIBLE
        }
    }

    private fun showLoggedInState() {
        binding.guestLoginLayout.visibility = View.GONE

    }

    private fun updateBookmarksList(jobs: List<JobModel>) {
        android.util.Log.d("BookmarkFragment", "=== UPDATING BOOKMARKS LIST ===")
        android.util.Log.d("BookmarkFragment", "Received ${jobs.size} bookmarked jobs to display")

        if (jobs.isNotEmpty()) {
            android.util.Log.d("BookmarkFragment", "Showing bookmarks list with ${jobs.size} jobs")
            jobs.forEachIndexed { index, job ->
                android.util.Log.d("BookmarkFragment", "UI Job $index: ${job.title} at ${job.company} (bookmarked: ${job.isBookmarked})")
            }


            binding.recyclerViewBookmarks.visibility = View.VISIBLE
            binding.emptyStateLayout.visibility = View.GONE


            bookmarkAdapter.updateData(jobs)
            android.util.Log.d("BookmarkFragment", "Updated RecyclerView adapter with ${jobs.size} jobs")


            updateBookmarkCountDisplay(jobs.size)
        } else {
            android.util.Log.d("BookmarkFragment", "No bookmarked jobs - showing empty state")

            binding.recyclerViewBookmarks.visibility = View.GONE
            binding.emptyStateLayout.visibility = View.VISIBLE
            updateEmptyStateForLoggedInUser()
        }

        android.util.Log.d("BookmarkFragment", "=== BOOKMARKS LIST UPDATE COMPLETED ===")
    }

    private fun updateLoadingState(isLoading: Boolean) {


        android.util.Log.d("BookmarkFragment", "Loading state: $isLoading")
    }

    private fun updateEmptyState(isEmpty: Boolean) {
        if (UserSession.isLoggedIn() && isEmpty) {
            updateEmptyStateForLoggedInUser()
        }
    }

    private fun updateEmptyStateForLoggedInUser() {
        binding.emptyStateLayout.visibility = View.VISIBLE
        binding.guestLoginLayout.visibility = View.GONE
        binding.recyclerViewBookmarks.visibility = View.GONE


        binding.subtitleText.text = "No saved jobs yet"
    }

    private fun updateBookmarkCountDisplay(count: Int) {
        val subtitle = when (count) {
            0 -> "No saved jobs yet"
            1 -> "1 saved job"
            else -> "$count saved jobs"
        }
        binding.subtitleText.text = subtitle
    }

    private fun onBookmarkToggled(job: JobModel) {


        android.util.Log.d("BookmarkFragment", "Bookmark toggled for: ${job.title} - isBookmarked: ${job.isBookmarked}")





        (requireActivity() as? MainActivity)?.let { _ ->


            android.util.Log.d("BookmarkFragment", "MainActivity context available for future use")
        }
    }

    private fun navigateToJobDetails(job: JobModel) {
        if (UserSession.isLoggedIn()) {
            val intent = Intent(requireContext(), DetailActivity::class.java)
            intent.putExtra("object", job)
            startActivity(intent)
        } else {
            navigateToLogin()
        }
    }

    private fun navigateToLogin() {

        (requireActivity() as? MainActivity)?.let { mainActivity ->

            mainActivity.binding.viewPager.currentItem = 4
        }
    }

    override fun onResume() {
        super.onResume()

        android.util.Log.d("BookmarkFragment", "onResume - Fragment becoming visible")


        if (_binding != null && isAdded) {
            android.util.Log.d("BookmarkFragment", "Refreshing login state and bookmarks list")
            bookmarkViewModel.refreshLoginState()


            if (UserSession.isLoggedIn()) {
                android.util.Log.d("BookmarkFragment", "User is logged in - loading latest bookmarks")
                bookmarkViewModel.loadBookmarkedJobs()
            }
        }
    }

    override fun onLoginStateRefresh(isLoggedIn: Boolean) {

        if (_binding != null && isAdded) {
            bookmarkViewModel.refreshLoginState()
        }
    }

    override fun onDestroyView() {
        super.onDestroyView()
        _binding = null
    }
}