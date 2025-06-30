package com.uilover.project196.Fragment

import android.app.Dialog
import android.content.Intent
import android.os.Bundle
import android.text.Editable
import android.text.TextWatcher
import android.view.LayoutInflater
import android.view.MotionEvent
import android.view.View
import android.view.ViewGroup
import android.view.Window
import android.widget.Toast
import androidx.core.content.ContextCompat
import androidx.fragment.app.Fragment
import androidx.lifecycle.ViewModelProvider
import androidx.lifecycle.lifecycleScope
import androidx.recyclerview.widget.GridLayoutManager
import androidx.recyclerview.widget.LinearLayoutManager
import androidx.recyclerview.widget.RecyclerView
import androidx.viewpager2.widget.ViewPager2
import com.uilover.project196.Activity.NotificationsActivity
import com.uilover.project196.Adapter.CategoryAdapter
import com.uilover.project196.Adapter.FilterOptionAdapter
import com.uilover.project196.Adapter.jobAdapter
import com.uilover.project196.Model.JobModel
import com.uilover.project196.R
import com.uilover.project196.Repository.NotificationRepository
import com.uilover.project196.Utils.UserSession
import com.uilover.project196.ViewModel.MainViewModel
import com.uilover.project196.ViewModel.BookmarkViewModel
import com.uilover.project196.ViewModel.ExplorerViewModel
import com.uilover.project196.databinding.FragmentExplorerBinding
import com.uilover.project196.databinding.DialogFilterBinding
import kotlinx.coroutines.launch

// KRITERIA: Multiple Fragment (2/16) - Fragment eksplorasi pekerjaan
// KRITERIA WAJIB: Multiple Fragment (2/16) - Fragment eksplorasi pekerjaan
// KRITERIA KOMPLEKSITAS: Advanced filtering dan search
class ExplorerFragment : BaseFragment() {
    private var _binding: FragmentExplorerBinding? = null
    private val binding get() = _binding!!




    private lateinit var explorerViewModel: ExplorerViewModel
    private lateinit var mainViewModel: MainViewModel
    private lateinit var bookmarkViewModel: BookmarkViewModel


    private lateinit var searchAdapter: jobAdapter

    override fun onCreateView(
        inflater: LayoutInflater,
        container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View {

        _binding = FragmentExplorerBinding.inflate(inflater, container, false)
        return binding.root
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)


        UserSession.init(requireContext())




        explorerViewModel = ViewModelProvider(
            this,
            ViewModelProvider.AndroidViewModelFactory.getInstance(requireActivity().application)
        )[ExplorerViewModel::class.java]

        mainViewModel = ViewModelProvider(requireActivity())[MainViewModel::class.java]
        mainViewModel.initializeDatabase(requireContext())

        bookmarkViewModel = ViewModelProvider(
            requireActivity(),
            ViewModelProvider.AndroidViewModelFactory.getInstance(requireActivity().application)
        )[BookmarkViewModel::class.java]




        setupReactiveBinding(explorerViewModel)
        setupLiveDataObservers()
        setupGlobalBookmarkObserver()
        setupSearchFunctionality()
        setupCategoryRecyclerView()
        setupRecyclerView()
        setupTouchHandling()
        setupNotificationButton(binding.imageView3, binding.textView4)


        explorerViewModel.loadJobs(mainViewModel)
    }




    private fun setupReactiveBinding(viewModel: ExplorerViewModel) {


        binding.editTextSearch.addTextChangedListener(object : TextWatcher {
            override fun beforeTextChanged(s: CharSequence?, start: Int, count: Int, after: Int) {}
            override fun onTextChanged(s: CharSequence?, start: Int, before: Int, count: Int) {
                val query = s.toString()
                if (viewModel.searchQuery.get() != query) {
                    viewModel.onSearchQueryChanged(query)
                }
            }
            override fun afterTextChanged(s: Editable?) {}
        })


        viewModel.resultCount.addOnPropertyChangedCallback(
            object : androidx.databinding.Observable.OnPropertyChangedCallback() {
                override fun onPropertyChanged(sender: androidx.databinding.Observable?, propertyId: Int) {
                    val count = viewModel.resultCount.get() ?: "0"

                    android.util.Log.d("ExplorerFragment", "🔄 Result count updated: $count jobs")
                }
            }
        )


        viewModel.showLoadingState.addOnPropertyChangedCallback(
            object : androidx.databinding.Observable.OnPropertyChangedCallback() {
                override fun onPropertyChanged(sender: androidx.databinding.Observable?, propertyId: Int) {
                    binding.progressBarSearchCategory.visibility =
                        if (viewModel.showLoadingState.get() == true) View.VISIBLE else View.GONE
                }
            }
        )


        viewModel.showContentState.addOnPropertyChangedCallback(
            object : androidx.databinding.Observable.OnPropertyChangedCallback() {
                override fun onPropertyChanged(sender: androidx.databinding.Observable?, propertyId: Int) {
                    binding.recyclerViewSearchResults.visibility =
                        if (viewModel.showContentState.get() == true) View.VISIBLE else View.GONE
                }
            }
        )


        viewModel.emptyStateTitle.addOnPropertyChangedCallback(
            object : androidx.databinding.Observable.OnPropertyChangedCallback() {
                override fun onPropertyChanged(sender: androidx.databinding.Observable?, propertyId: Int) {

                    android.util.Log.d("ExplorerFragment", "🔄 Empty state: ${viewModel.emptyStateTitle.get()}")
                }
            }
        )


        viewModel.activeFilterCount.addOnPropertyChangedCallback(
            object : androidx.databinding.Observable.OnPropertyChangedCallback() {
                override fun onPropertyChanged(sender: androidx.databinding.Observable?, propertyId: Int) {
                    val count = viewModel.activeFilterCount.get()?.toIntOrNull() ?: 0

                    android.util.Log.d("ExplorerFragment", "🔄 Active filters: $count")
                }
            }
        )
    }




    private fun setupLiveDataObservers() {


        explorerViewModel.filteredJobs.observe(viewLifecycleOwner) { jobs ->
            jobs?.let {
                updateSearchResults(it)
            }
        }


        explorerViewModel.availableSalaryRanges.observe(viewLifecycleOwner) { _ ->

        }

        explorerViewModel.availableLevels.observe(viewLifecycleOwner) { _ ->

        }


        explorerViewModel.showSuccessMessage.observe(viewLifecycleOwner) { message ->
            message?.let {
                Toast.makeText(requireContext(), it, Toast.LENGTH_SHORT).show()
                explorerViewModel.onSuccessMessageShown()
            }
        }


        explorerViewModel.showErrorMessage.observe(viewLifecycleOwner) { message ->
            message?.let {
                Toast.makeText(requireContext(), it, Toast.LENGTH_SHORT).show()
                explorerViewModel.onErrorMessageShown()
            }
        }
    }




    private fun setupGlobalBookmarkObserver() {
        android.util.Log.d("ExplorerFragment", "🎯 Setting up global bookmark observer")
        android.util.Log.d("ExplorerFragment", "🎯 BookmarkViewModel instance: $bookmarkViewModel")
        android.util.Log.d("ExplorerFragment", "🎯 BookmarkViewModel hashCode: ${bookmarkViewModel.hashCode()}")


        val hasObservers = bookmarkViewModel.globalBookmarkStateChanged.hasObservers()
        android.util.Log.d("ExplorerFragment", "🎯 globalBookmarkStateChanged has observers before: $hasObservers")

        bookmarkViewModel.globalBookmarkStateChanged.observe(viewLifecycleOwner) { stateChange ->
            android.util.Log.d("ExplorerFragment", "📢 Global bookmark observer triggered with: $stateChange")

            stateChange?.let {
                android.util.Log.d("ExplorerFragment", "🔄 Global bookmark state changed: ${it.jobTitle} = ${it.isBookmarked}")
                android.util.Log.d("ExplorerFragment", "🔄 Job ID being updated: ${it.jobId}")


                updateSearchAdapterBookmarkState(it.jobTitle, extractCompanyFromJobId(it.jobId), it.isBookmarked)


                bookmarkViewModel.onGlobalBookmarkStateChangeHandled("ExplorerFragment")
            } ?: run {
                android.util.Log.d("ExplorerFragment", "📢 Global bookmark observer triggered with null stateChange")
            }
        }

        val hasObserversAfter = bookmarkViewModel.globalBookmarkStateChanged.hasObservers()
        android.util.Log.d("ExplorerFragment", "🎯 globalBookmarkStateChanged has observers after: $hasObserversAfter")


        bookmarkViewModel.bookmarkStatesMap.observe(viewLifecycleOwner) { bookmarkStates ->
            bookmarkStates?.let {

                android.util.Log.d("ExplorerFragment", "🔄 Bookmark states map updated with ${bookmarkStates.size} jobs")
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


    private fun updateSearchAdapterBookmarkState(jobTitle: String, jobCompany: String, isBookmarked: Boolean) {
        android.util.Log.d("ExplorerFragment", "🎯 ========= EXPLORER BOOKMARK UPDATE =========")
        android.util.Log.d("ExplorerFragment", "🎯 Job: $jobTitle at $jobCompany")
        android.util.Log.d("ExplorerFragment", "🎯 New bookmark state: $isBookmarked")
        android.util.Log.d("ExplorerFragment", "🎯 Search adapter initialized: ${::searchAdapter.isInitialized}")

        if (::searchAdapter.isInitialized) {
            val adapterItemsBefore = searchAdapter.itemCount
            android.util.Log.d("ExplorerFragment", "🎯 Adapter has $adapterItemsBefore items before update")

            searchAdapter.updateJobBookmarkState(jobTitle, jobCompany, isBookmarked)

            val adapterItemsAfter = searchAdapter.itemCount
            android.util.Log.d("ExplorerFragment", "🎯 Adapter has $adapterItemsAfter items after update")
            android.util.Log.d("ExplorerFragment", "✅ Bookmark state updated in search adapter")
            android.util.Log.d("ExplorerFragment", "🎯 ========= EXPLORER UPDATE COMPLETE =========")
        } else {
            android.util.Log.w("ExplorerFragment", "⚠️ Search adapter not initialized, falling back to refresh")
            refreshJobList()
        }
    }


    private fun extractCompanyFromJobId(jobId: String): String {

        val parts = jobId.split("_")
        return if (parts.size >= 2) parts[1] else ""
    }


    private fun refreshJobList() {
        android.util.Log.d("ExplorerFragment", "🔄 Refreshing job list to sync bookmark states")


        val currentJobs = explorerViewModel.filteredJobs.value
        if (currentJobs != null && currentJobs.isNotEmpty()) {
            android.util.Log.d("ExplorerFragment", "📋 Refreshing ${currentJobs.size} filtered jobs with latest bookmark states")


            updateSearchResults(currentJobs)

            android.util.Log.d("ExplorerFragment", "✅ Job list refreshed successfully")
        } else {
            android.util.Log.d("ExplorerFragment", "📋 No filtered jobs available, reloading from ViewModel")


            explorerViewModel.loadJobs(mainViewModel)
        }
    }

    private fun setupSearchFunctionality() {

        binding.imageViewFilter.setOnClickListener {
            showReactiveFilterDialog()
        }
    }

    private fun setupCategoryRecyclerView() {
        binding.recyclerViewSearchCategory.layoutManager =
            LinearLayoutManager(requireContext(), LinearLayoutManager.HORIZONTAL, false)

        val categories = mainViewModel.loadCategory()

        binding.recyclerViewSearchCategory.adapter = CategoryAdapter(categories, object : CategoryAdapter.ClickListener {
            override fun onClick(category: String) {

                explorerViewModel.onCategorySelected(category)
            }
        })
    }

    private fun setupRecyclerView() {
        binding.recyclerViewSearchResults.layoutManager =
            LinearLayoutManager(requireContext(), LinearLayoutManager.VERTICAL, false)
    }

    private fun updateSearchResults(jobs: List<JobModel>) {
        android.util.Log.d("ExplorerFragment", "📋 updateSearchResults called with ${jobs.size} jobs")


        val jobsWithBookmarks = bookmarkViewModel.applyBookmarkStatesToJobs(jobs)
        android.util.Log.d("ExplorerFragment", "📋 Applied bookmark states to ${jobsWithBookmarks.size} jobs")


        jobsWithBookmarks.take(3).forEachIndexed { index, job ->
            val jobId = "${job.title}_${job.company}"
            android.util.Log.d("ExplorerFragment", "📋 Sample job $index: $jobId (bookmarked: ${job.isBookmarked})")
        }

        if (!::searchAdapter.isInitialized) {
            android.util.Log.d("ExplorerFragment", "🆕 Initializing search adapter with ${jobsWithBookmarks.size} jobs")

            searchAdapter = jobAdapter(
                items = jobsWithBookmarks,
                onBookmarkClick = { job ->
                    android.util.Log.d("ExplorerFragment", "🔖 Search result bookmark clicked: ${job.title}")
                    val jobId = "${job.title}_${job.company}"
                    android.util.Log.d("ExplorerFragment", "🔖 Job ID for bookmark toggle: $jobId")


                    bookmarkViewModel.toggleBookmark(job)


                    android.util.Log.d("ExplorerFragment", "🔖 Bookmark toggle delegated to centralized state management")
                }
            )
            binding.recyclerViewSearchResults.adapter = searchAdapter
            android.util.Log.d("ExplorerFragment", "✅ Search adapter initialized and set to RecyclerView")
        } else {

            android.util.Log.d("ExplorerFragment", "🔄 Updating existing search adapter with ${jobsWithBookmarks.size} jobs")
            searchAdapter.updateData(jobsWithBookmarks)
            android.util.Log.d("ExplorerFragment", "✅ Search adapter updated with new data")
        }
    }




    private fun showReactiveFilterDialog() {
        val dialog = Dialog(requireContext())
        dialog.requestWindowFeature(Window.FEATURE_NO_TITLE)
        val dialogBinding = DialogFilterBinding.inflate(layoutInflater)
        dialog.setContentView(dialogBinding.root)

        val window = dialog.window
        window?.setLayout(ViewGroup.LayoutParams.MATCH_PARENT, ViewGroup.LayoutParams.WRAP_CONTENT)
        window?.setBackgroundDrawableResource(android.R.color.transparent)

        setupReactiveFilterDialog(dialogBinding, dialog)
        dialog.show()
    }

    private fun setupReactiveFilterDialog(dialogBinding: DialogFilterBinding, dialog: Dialog) {
        val salaryRanges = explorerViewModel.availableSalaryRanges.value ?: listOf()
        val levels = explorerViewModel.availableLevels.value ?: listOf()






        dialogBinding.recyclerViewLevel.layoutManager = GridLayoutManager(requireContext(), 2)
        val levelAdapter = FilterOptionAdapter(
            levels,
            object : FilterOptionAdapter.ClickListener {
                override fun onSelectionChanged(selectedItems: List<String>) {
                    explorerViewModel.updateLevels(selectedItems)
                }
            },
            explorerViewModel.selectedLevels.get() ?: listOf()
        )
        dialogBinding.recyclerViewLevel.adapter = levelAdapter


        dialogBinding.recyclerViewSalary.layoutManager = GridLayoutManager(requireContext(), 2)
        val salaryAdapter = FilterOptionAdapter(
            salaryRanges,
            object : FilterOptionAdapter.ClickListener {
                override fun onSelectionChanged(selectedItems: List<String>) {
                    explorerViewModel.updateSalaryRanges(selectedItems)
                }
            },
            explorerViewModel.selectedSalaryRanges.get() ?: listOf()
        )
        dialogBinding.recyclerViewSalary.adapter = salaryAdapter




        setupReactiveWorkTimeButton(dialogBinding.fullTimeFilter, explorerViewModel.isFullTimeSelected)
        setupReactiveWorkTimeButton(dialogBinding.partTimeFilter, explorerViewModel.isPartTimeSelected)

        dialogBinding.fullTimeFilter.setOnClickListener {
            explorerViewModel.toggleFullTime()
        }

        dialogBinding.partTimeFilter.setOnClickListener {
            explorerViewModel.togglePartTime()
        }




        setupReactiveWorkModelButton(dialogBinding.remoteFilter, explorerViewModel.isRemoteSelected)
        setupReactiveWorkModelButton(dialogBinding.inPersonFilter, explorerViewModel.isInPersonSelected)

        dialogBinding.remoteFilter.setOnClickListener {
            explorerViewModel.toggleRemote()
        }

        dialogBinding.inPersonFilter.setOnClickListener {
            explorerViewModel.toggleInPerson()
        }




        dialogBinding.clearAllFilters.setOnClickListener {
            explorerViewModel.clearAllFilters()
            levelAdapter.clearSelections()
            salaryAdapter.clearSelections()
        }

        dialogBinding.applyFilters.setOnClickListener {
            dialog.dismiss()

        }

        dialogBinding.closeFilter.setOnClickListener {
            dialog.dismiss()
        }
    }

    private fun setupReactiveWorkTimeButton(button: android.widget.TextView, selectedObservable: androidx.databinding.ObservableField<Boolean>) {

        updateButtonAppearance(button, selectedObservable.get() ?: false)


        selectedObservable.addOnPropertyChangedCallback(
            object : androidx.databinding.Observable.OnPropertyChangedCallback() {
                override fun onPropertyChanged(sender: androidx.databinding.Observable?, propertyId: Int) {
                    updateButtonAppearance(button, selectedObservable.get() ?: false)
                }
            }
        )
    }

    private fun setupReactiveWorkModelButton(button: android.widget.TextView, selectedObservable: androidx.databinding.ObservableField<Boolean>) {

        updateButtonAppearance(button, selectedObservable.get() ?: false)


        selectedObservable.addOnPropertyChangedCallback(
            object : androidx.databinding.Observable.OnPropertyChangedCallback() {
                override fun onPropertyChanged(sender: androidx.databinding.Observable?, propertyId: Int) {
                    updateButtonAppearance(button, selectedObservable.get() ?: false)
                }
            }
        )
    }

    private fun updateButtonAppearance(button: android.widget.TextView, selected: Boolean) {
        if (selected) {
            button.setBackgroundResource(R.drawable.purple_full_corner)
            button.setTextColor(ContextCompat.getColor(requireContext(), R.color.white))
        } else {
            button.setBackgroundResource(R.drawable.grey_full_corner_bg)
            button.setTextColor(ContextCompat.getColor(requireContext(), R.color.black))
        }
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

        binding.recyclerViewSearchCategory.addOnScrollListener(scrollListener)
        binding.recyclerViewSearchResults.addOnScrollListener(scrollListener)

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

        binding.recyclerViewSearchCategory.setOnTouchListener(touchListener)
        binding.recyclerViewSearchResults.setOnTouchListener(touchListener)
    }

    override fun onResume() {
        super.onResume()
        refreshBasedOnLoginState()
    }

    override fun onLoginStateRefresh(isLoggedIn: Boolean) {
        refreshBasedOnLoginState()
    }

    private fun refreshBasedOnLoginState() {
        if (_binding != null) {

            explorerViewModel.loadJobs(mainViewModel)
        }
    }

    override fun onDestroyView() {
        super.onDestroyView()
        _binding = null
    }
}