package com.uilover.project196.Fragment

import android.app.AlertDialog
import android.app.Dialog
import android.os.Bundle
import androidx.fragment.app.Fragment
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.view.Window
import android.widget.ArrayAdapter
import android.widget.Toast
import androidx.lifecycle.ViewModelProvider
import com.uilover.project196.R
import com.uilover.project196.Utils.UserSession
import com.uilover.project196.ViewModel.MainViewModel
import com.uilover.project196.databinding.FragmentPostingBinding
import com.uilover.project196.databinding.DialogEditJobBinding
import com.uilover.project196.Model.JobModel
import com.uilover.project196.ViewModel.JobManagementViewModel
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.Job
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch

// KRITERIA WAJIB: Multiple Fragment (7/16) - Fragment posting pekerjaan baru
class PostingFragment : Fragment() {

    private var _binding: FragmentPostingBinding? = null
    private val binding get() = _binding!!
    private lateinit var mainViewModel: MainViewModel
    private lateinit var jobManagementViewModel: JobManagementViewModel
    private var currentJob: JobModel? = null


    private val observableCallbacks = mutableMapOf<androidx.databinding.ObservableField<*>, androidx.databinding.Observable.OnPropertyChangedCallback>()

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
    }

    override fun onCreateView(
        inflater: LayoutInflater, container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View {
        _binding = FragmentPostingBinding.inflate(inflater, container, false)
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


        setupReactiveBinding(jobManagementViewModel)
        setupObservers()


        setupJobDisplay()
        setupJobManagement()
    }




    private fun setupReactiveBinding(viewModel: JobManagementViewModel) {


        val companyNameCallback = object : androidx.databinding.Observable.OnPropertyChangedCallback() {
            override fun onPropertyChanged(sender: androidx.databinding.Observable?, propertyId: Int) {



            }
        }
        viewModel.companyName.addOnPropertyChangedCallback(companyNameCallback)
        observableCallbacks[viewModel.companyName] = companyNameCallback


        val formValidCallback = object : androidx.databinding.Observable.OnPropertyChangedCallback() {
            override fun onPropertyChanged(sender: androidx.databinding.Observable?, propertyId: Int) {



            }
        }
        viewModel.isFormValid.addOnPropertyChangedCallback(formValidCallback)
        observableCallbacks[viewModel.isFormValid] = formValidCallback


        val jobTitleCallback = object : androidx.databinding.Observable.OnPropertyChangedCallback() {
            override fun onPropertyChanged(sender: androidx.databinding.Observable?, propertyId: Int) {



            }
        }
        viewModel.jobTitle.addOnPropertyChangedCallback(jobTitleCallback)
        observableCallbacks[viewModel.jobTitle] = jobTitleCallback


        val jobDescriptionCallback = object : androidx.databinding.Observable.OnPropertyChangedCallback() {
            override fun onPropertyChanged(sender: androidx.databinding.Observable?, propertyId: Int) {



            }
        }
        viewModel.jobDescription.addOnPropertyChangedCallback(jobDescriptionCallback)
        observableCallbacks[viewModel.jobDescription] = jobDescriptionCallback


        val salaryCallback = object : androidx.databinding.Observable.OnPropertyChangedCallback() {
            override fun onPropertyChanged(sender: androidx.databinding.Observable?, propertyId: Int) {



            }
        }
        viewModel.selectedSalary.addOnPropertyChangedCallback(salaryCallback)
        observableCallbacks[viewModel.selectedSalary] = salaryCallback


        val jobTypeCallback = object : androidx.databinding.Observable.OnPropertyChangedCallback() {
            override fun onPropertyChanged(sender: androidx.databinding.Observable?, propertyId: Int) {


            }
        }
        viewModel.selectedJobType.addOnPropertyChangedCallback(jobTypeCallback)
        observableCallbacks[viewModel.selectedJobType] = jobTypeCallback


        val workingModelCallback = object : androidx.databinding.Observable.OnPropertyChangedCallback() {
            override fun onPropertyChanged(sender: androidx.databinding.Observable?, propertyId: Int) {


            }
        }
        viewModel.selectedWorkingModel.addOnPropertyChangedCallback(workingModelCallback)
        observableCallbacks[viewModel.selectedWorkingModel] = workingModelCallback


        val levelCallback = object : androidx.databinding.Observable.OnPropertyChangedCallback() {
            override fun onPropertyChanged(sender: androidx.databinding.Observable?, propertyId: Int) {


            }
        }
        viewModel.selectedLevel.addOnPropertyChangedCallback(levelCallback)
        observableCallbacks[viewModel.selectedLevel] = levelCallback
    }

    private fun setupObservers() {

        jobManagementViewModel.currentJob.observe(viewLifecycleOwner) { job ->
            job?.let {
                currentJob = it
                updateJobStatusUI(it)


                activity?.let { activity ->
                    if (activity is com.uilover.project196.Activity.DetailActivity) {
                        activity.refreshJobData(it)
                    }
                }
            }
        }


        jobManagementViewModel.showSuccessMessage.observe(viewLifecycleOwner) { message ->
            message?.let {
                Toast.makeText(requireContext(), it, Toast.LENGTH_SHORT).show()
                jobManagementViewModel.onSuccessMessageShown()
            }
        }


        jobManagementViewModel.showErrorMessage.observe(viewLifecycleOwner) { message ->
            message?.let {
                Toast.makeText(requireContext(), it, Toast.LENGTH_SHORT).show()
                jobManagementViewModel.onErrorMessageShown()
            }
        }


        jobManagementViewModel.isLoading.observe(viewLifecycleOwner) { isLoading ->

            if (isLoading) {

            } else {

            }
        }


        jobManagementViewModel.jobUpdated.observe(viewLifecycleOwner) { updated ->
            if (updated) {

                jobManagementViewModel.onJobUpdatedHandled()
            }
        }
    }

    private fun setupJobDisplay() {

        activity?.let { activity ->
            if (activity is com.uilover.project196.Activity.DetailActivity) {
                currentJob = activity.item
                currentJob?.let { job ->
                    displayJobDetails(job)
                }
            }
        }
    }

    private fun displayJobDetails(job: JobModel) {

        updateJobStatusUI(job)


        // For now, show "Just posted" for user's own jobs
        // TODO: Add proper creation timestamp to JobModel for accurate posting time
        binding.postedDateText.text = "Just posted"
    }

    private fun updateJobStatusUI(job: JobModel) {

        binding.jobStatusText.text = jobManagementViewModel.getJobStatusDisplay(job)
        binding.jobStatusDescription.text = jobManagementViewModel.getJobStatusDescription(job)

        if (job.isOpen()) {

            binding.jobStatusText.setBackgroundResource(R.drawable.purple_full_corner)
            binding.closeJobButton.visibility = View.VISIBLE
            binding.reopenJobButton.visibility = View.GONE
        } else {

            binding.jobStatusText.setBackgroundResource(R.drawable.grey_bg)
            binding.closeJobButton.visibility = View.GONE
            binding.reopenJobButton.visibility = View.VISIBLE
        }
    }

    private fun setupJobManagement() {

        currentJob?.let { job ->
            if (jobManagementViewModel.canManageJob(job)) {
                binding.jobManagementCard.visibility = View.VISIBLE


                binding.editJobButton.setOnClickListener {
                    showEditJobDialog(job)
                }


                binding.closeJobButton.setOnClickListener {
                    showCloseJobDialog(job)
                }


                binding.reopenJobButton.setOnClickListener {
                    showReopenJobDialog(job)
                }
            } else {

                binding.jobManagementCard.visibility = View.GONE
            }
        }
    }

    private fun showCloseJobDialog(job: JobModel) {
        val builder = AlertDialog.Builder(requireContext())
        builder.setTitle("Close Job Posting")
        builder.setMessage("Are you sure you want to close this job posting?\n\n\"${job.title}\" at ${job.company}\n\nThis will:\n• Remove it from public listings\n• Stop new applications\n• Move it to your Closed Jobs section")

        builder.setPositiveButton("Close Job") { _, _ ->
            jobManagementViewModel.closeJob(job)
        }

        builder.setNegativeButton("Cancel") { dialog, _ ->
            dialog.dismiss()
        }

        builder.show()
    }

    private fun showReopenJobDialog(job: JobModel) {
        val builder = AlertDialog.Builder(requireContext())
        builder.setTitle("Reopen Job Posting")
        builder.setMessage("Are you sure you want to reopen this job posting?\n\n\"${job.title}\" at ${job.company}\n\nThis will make it visible in public listings again and candidates will be able to apply.")

        builder.setPositiveButton("Reopen Job") { _, _ ->
            jobManagementViewModel.reopenJob(job)
        }

        builder.setNegativeButton("Cancel") { dialog, _ ->
            dialog.dismiss()
        }

        builder.show()
    }

    private fun showEditJobDialog(job: JobModel) {
        val dialog = Dialog(requireContext())
        dialog.requestWindowFeature(Window.FEATURE_NO_TITLE)
        val dialogBinding = DialogEditJobBinding.inflate(layoutInflater)

        dialog.setContentView(dialogBinding.root)


        val window = dialog.window
        window?.setLayout(ViewGroup.LayoutParams.MATCH_PARENT, ViewGroup.LayoutParams.WRAP_CONTENT)
        window?.setBackgroundDrawableResource(android.R.color.transparent)

        setupEditJobDialog(dialogBinding, dialog, job)

        dialog.show()
    }

    private fun setupEditJobDialog(dialogBinding: DialogEditJobBinding, dialog: Dialog, job: JobModel) {
        android.util.Log.d("PostingFragment", "🔧 === JOB EDITING TRACE START ===")
        android.util.Log.d("PostingFragment", "🔧 Original job data:")
        android.util.Log.d("PostingFragment", "🔧   Title: '${job.title}'")
        android.util.Log.d("PostingFragment", "🔧   Company: '${job.company}'")
        android.util.Log.d("PostingFragment", "🔧   Salary: '${job.salary}'")
        android.util.Log.d("PostingFragment", "🔧   Job Type: '${job.time}'")
        android.util.Log.d("PostingFragment", "🔧   Working Model: '${job.model}'")
        android.util.Log.d("PostingFragment", "🔧   Level: '${job.level}'")
        android.util.Log.d("PostingFragment", "🔧   Description: '${job.description}'")


        jobManagementViewModel.initializeFormForEdit(job)

        android.util.Log.d("PostingFragment", "🔧 ViewModel populated. Current ViewModel values:")
        android.util.Log.d("PostingFragment", "🔧   jobTitle: '${jobManagementViewModel.jobTitle.get()}'")
        android.util.Log.d("PostingFragment", "🔧   selectedSalary: '${jobManagementViewModel.selectedSalary.get()}'")
        android.util.Log.d("PostingFragment", "🔧   selectedJobType: '${jobManagementViewModel.selectedJobType.get()}'")
        android.util.Log.d("PostingFragment", "🔧   selectedWorkingModel: '${jobManagementViewModel.selectedWorkingModel.get()}'")
        android.util.Log.d("PostingFragment", "🔧   selectedLevel: '${jobManagementViewModel.selectedLevel.get()}'")
        android.util.Log.d("PostingFragment", "🔧   jobDescription: '${jobManagementViewModel.jobDescription.get()}'")


        val filterOptions = mainViewModel.getFilterOptions()


        setupEditJobDialogReactiveBinding(dialogBinding, jobManagementViewModel)
        setupEditJobDialogLiveDataObservers(dialog)


        dialogBinding.editJobTitle.setText(jobManagementViewModel.jobTitle.get() ?: "")
        dialogBinding.editJobCompany.setText(job.company)
        dialogBinding.editJobDescription.setText(jobManagementViewModel.jobDescription.get() ?: "")


        val salaryList = filterOptions.salaryRanges.toMutableList()
        val salaryAdapter = ArrayAdapter(requireContext(), android.R.layout.simple_dropdown_item_1line, salaryList)
        dialogBinding.editJobSalary.setAdapter(salaryAdapter)
        dialogBinding.editJobSalary.setText(jobManagementViewModel.selectedSalary.get() ?: "", false)


        val jobTypeList = filterOptions.jobTypes.toMutableList()
        val jobTypeAdapter = ArrayAdapter(requireContext(), android.R.layout.simple_dropdown_item_1line, jobTypeList)
        dialogBinding.editJobType.setAdapter(jobTypeAdapter)
        dialogBinding.editJobType.setText(jobManagementViewModel.selectedJobType.get() ?: "", false)


        val workingModelList = filterOptions.workingModels.toMutableList()
        val workingModelAdapter = ArrayAdapter(requireContext(), android.R.layout.simple_dropdown_item_1line, workingModelList)
        dialogBinding.editJobWorkingModel.setAdapter(workingModelAdapter)
        dialogBinding.editJobWorkingModel.setText(jobManagementViewModel.selectedWorkingModel.get() ?: "", false)


        val levelList = filterOptions.levels.toMutableList()
        val levelAdapter = ArrayAdapter(requireContext(), android.R.layout.simple_dropdown_item_1line, levelList)
        dialogBinding.editJobLevel.setAdapter(levelAdapter)
        dialogBinding.editJobLevel.setText(jobManagementViewModel.selectedLevel.get() ?: "", false)

        android.util.Log.d("PostingFragment", "🔧 Dialog fields populated from ViewModel")


        dialogBinding.editJobSaveButton.setOnClickListener {
            android.util.Log.d("PostingFragment", "🔧 Save button clicked. Current ViewModel values:")
            android.util.Log.d("PostingFragment", "🔧   jobTitle: '${jobManagementViewModel.jobTitle.get()}'")
            android.util.Log.d("PostingFragment", "🔧   selectedSalary: '${jobManagementViewModel.selectedSalary.get()}'")
            android.util.Log.d("PostingFragment", "🔧   selectedJobType: '${jobManagementViewModel.selectedJobType.get()}'")
            android.util.Log.d("PostingFragment", "🔧   selectedWorkingModel: '${jobManagementViewModel.selectedWorkingModel.get()}'")
            android.util.Log.d("PostingFragment", "🔧   selectedLevel: '${jobManagementViewModel.selectedLevel.get()}'")
            android.util.Log.d("PostingFragment", "🔧   jobDescription: '${jobManagementViewModel.jobDescription.get()}'")


            android.util.Log.d("PostingFragment", "🔧 Calling updateJob...")


            jobManagementViewModel.updateJob { success ->
                android.util.Log.d("PostingFragment", "🔧 updateJob callback: success = $success")
                if (success) {

                    currentJob = jobManagementViewModel.currentJob.value
                    android.util.Log.d("PostingFragment", "🔧 Updated job title: ${currentJob?.title}")
                    currentJob?.let {
                        displayJobDetails(it)

                        activity?.let { activity ->
                            if (activity is com.uilover.project196.Activity.DetailActivity) {
                                activity.refreshJobData(it)
                                android.util.Log.d("PostingFragment", "🔧 Notified parent activity to refresh")
                            }
                        }
                    }

                    try {
                        if (dialog.isShowing) {
                            dialog.dismiss()
                        }
                    } catch (e: Exception) {
                        android.util.Log.e("PostingFragment", "Error closing dialog", e)
                    }
                    android.util.Log.d("PostingFragment", "🔧 Dialog dismissed")
                } else {
                    android.util.Log.e("PostingFragment", "🔧 Job update failed - dialog stays open for user to fix errors")
                }
            }
        }

        dialogBinding.editJobCancelButton.setOnClickListener {
            android.util.Log.d("PostingFragment", "🔧 Cancel button clicked")
            dialog.dismiss()
        }

        android.util.Log.d("PostingFragment", "🔧 === JOB EDITING TRACE END ===")
    }




    private fun setupEditJobDialogReactiveBinding(dialogBinding: DialogEditJobBinding, viewModel: JobManagementViewModel) {
        android.util.Log.d("PostingFragment", "🔗 Setting up reactive validation for edit dialog")


        var titleValidationJob: kotlinx.coroutines.Job? = null
        var descriptionValidationJob: kotlinx.coroutines.Job? = null


        dialogBinding.editJobTitle.addTextChangedListener(object : android.text.TextWatcher {
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


        dialogBinding.editJobDescription.addTextChangedListener(object : android.text.TextWatcher {
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


        dialogBinding.editJobSalary.addTextChangedListener(object : android.text.TextWatcher {
            override fun beforeTextChanged(s: CharSequence?, start: Int, count: Int, after: Int) {}
            override fun onTextChanged(s: CharSequence?, start: Int, before: Int, count: Int) {}
            override fun afterTextChanged(s: android.text.Editable?) {
                viewModel.validateSalary(s?.toString() ?: "")
            }
        })


        dialogBinding.editJobType.addTextChangedListener(object : android.text.TextWatcher {
            override fun beforeTextChanged(s: CharSequence?, start: Int, count: Int, after: Int) {}
            override fun onTextChanged(s: CharSequence?, start: Int, before: Int, count: Int) {}
            override fun afterTextChanged(s: android.text.Editable?) {
                viewModel.validateJobType(s?.toString() ?: "")
            }
        })


        dialogBinding.editJobWorkingModel.addTextChangedListener(object : android.text.TextWatcher {
            override fun beforeTextChanged(s: CharSequence?, start: Int, count: Int, after: Int) {}
            override fun onTextChanged(s: CharSequence?, start: Int, before: Int, count: Int) {}
            override fun afterTextChanged(s: android.text.Editable?) {
                viewModel.validateWorkingModel(s?.toString() ?: "")
            }
        })


        dialogBinding.editJobLevel.addTextChangedListener(object : android.text.TextWatcher {
            override fun beforeTextChanged(s: CharSequence?, start: Int, count: Int, after: Int) {}
            override fun onTextChanged(s: CharSequence?, start: Int, before: Int, count: Int) {}
            override fun afterTextChanged(s: android.text.Editable?) {
                viewModel.validateLevel(s?.toString() ?: "")
            }
        })


        viewModel.jobTitleError.addOnPropertyChangedCallback(object : androidx.databinding.Observable.OnPropertyChangedCallback() {
            override fun onPropertyChanged(sender: androidx.databinding.Observable?, propertyId: Int) {
                dialogBinding.editJobTitle.error = if (viewModel.jobTitleError.get()?.isNotEmpty() == true)
                    viewModel.jobTitleError.get() else null
            }
        })

        viewModel.jobDescriptionError.addOnPropertyChangedCallback(object : androidx.databinding.Observable.OnPropertyChangedCallback() {
            override fun onPropertyChanged(sender: androidx.databinding.Observable?, propertyId: Int) {
                dialogBinding.editJobDescription.error = if (viewModel.jobDescriptionError.get()?.isNotEmpty() == true)
                    viewModel.jobDescriptionError.get() else null
            }
        })


        viewModel.submitButtonEnabled.addOnPropertyChangedCallback(object : androidx.databinding.Observable.OnPropertyChangedCallback() {
            override fun onPropertyChanged(sender: androidx.databinding.Observable?, propertyId: Int) {
                dialogBinding.editJobSaveButton.isEnabled = viewModel.submitButtonEnabled.get() ?: false
                dialogBinding.editJobSaveButton.alpha = if (viewModel.submitButtonEnabled.get() ?: false) 1.0f else 0.6f
            }
        })


        viewModel.submitButtonText.addOnPropertyChangedCallback(object : androidx.databinding.Observable.OnPropertyChangedCallback() {
            override fun onPropertyChanged(sender: androidx.databinding.Observable?, propertyId: Int) {
                dialogBinding.editJobSaveButton.text = viewModel.submitButtonText.get() ?: "Update Job"
            }
        })

        android.util.Log.d("PostingFragment", "🔗 Reactive validation setup complete")
    }


    private fun setupEditJobDialogLiveDataObservers(dialog: Dialog) {

        jobManagementViewModel.showSuccessMessage.observe(viewLifecycleOwner) { message ->
            message?.let {
                android.widget.Toast.makeText(requireContext(), it, android.widget.Toast.LENGTH_SHORT).show()
                jobManagementViewModel.onSuccessMessageShown()


                try {
                    if (dialog.isShowing) {
                        dialog.dismiss()
                    }
                } catch (e: Exception) {
                    android.util.Log.e("PostingFragment", "Error closing dialog on success", e)
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




    private fun showJobClosedSuccessDialog() {
        val builder = AlertDialog.Builder(requireContext())
        builder.setTitle("Job Closed Successfully")
        builder.setMessage("Your job posting has been closed and moved to the Closed Jobs section.\n\nIt's no longer visible to candidates, but you can reopen it anytime from the Home tab or here.")

        builder.setPositiveButton("OK") { dialog, _ ->
            dialog.dismiss()
        }

        builder.setNeutralButton("Go to Home") { _, _ ->

            activity?.finish()
        }

        builder.show()
    }

    override fun onDestroyView() {
        super.onDestroyView()


        if (::jobManagementViewModel.isInitialized) {

            observableCallbacks.forEach { (observableField, callback) ->
                observableField.removeOnPropertyChangedCallback(callback)
            }
            observableCallbacks.clear()
        }

        _binding = null
    }
}