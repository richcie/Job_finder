package com.uilover.project196.Fragment

import android.app.Dialog
import android.content.Intent
import android.os.Bundle
import android.view.LayoutInflater
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
import com.uilover.project196.Activity.LoginActivity
import com.uilover.project196.Model.JobModel
import com.uilover.project196.R
import com.uilover.project196.Utils.UserSession
import com.uilover.project196.ViewModel.MainViewModel
import com.uilover.project196.ViewModel.ProfileViewModel
import com.uilover.project196.ViewModel.JobManagementViewModel
import com.uilover.project196.Fragment.AppInfoFragment
import com.uilover.project196.databinding.DialogCreateJobBinding
import com.uilover.project196.databinding.FragmentProfileBinding
import com.uilover.project196.Database.AppDatabase
import kotlinx.coroutines.launch

// KRITERIA WAJIB: Multiple Fragment (8/16) - Fragment profil pengguna
class ProfileFragment : BaseFragment() {
    private var _binding: FragmentProfileBinding? = null
    private val binding get() = _binding!!
    private lateinit var mainViewModel: MainViewModel
    private lateinit var profileViewModel: ProfileViewModel
    private lateinit var jobManagementViewModel: JobManagementViewModel


    private var activeDialogs = mutableSetOf<Dialog>()

    override fun onCreateView(
        inflater: LayoutInflater,
        container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View {
        _binding = FragmentProfileBinding.inflate(inflater, container, false)
        return binding.root
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)


        val ctx = context
        if (!isAdded || ctx == null || _binding == null) return


        UserSession.init(ctx)


        mainViewModel = ViewModelProvider(requireActivity())[MainViewModel::class.java]
        mainViewModel.initializeDatabase(ctx)

        profileViewModel = ViewModelProvider(
            this,
            ViewModelProvider.AndroidViewModelFactory.getInstance(requireActivity().application)
        )[ProfileViewModel::class.java]

        jobManagementViewModel = ViewModelProvider(
            this,
            ViewModelProvider.AndroidViewModelFactory.getInstance(requireActivity().application)
        )[JobManagementViewModel::class.java]




        setupReactiveBinding(profileViewModel)
        setupLiveDataObservers()
        setupClickListeners()
        setupNotificationButton(binding.imageView3, binding.textView4)


        applyInitialUIState()
    }




    private fun setupReactiveBinding(viewModel: ProfileViewModel) {


        viewModel.userName.addOnPropertyChangedCallback(
            object : androidx.databinding.Observable.OnPropertyChangedCallback() {
                override fun onPropertyChanged(sender: androidx.databinding.Observable?, propertyId: Int) {
                    if (_binding != null) {
                        binding.userNameText.text = viewModel.userName.get() ?: "Guest User"
                    }
                }
            }
        )


        viewModel.userSubtitle.addOnPropertyChangedCallback(
            object : androidx.databinding.Observable.OnPropertyChangedCallback() {
                override fun onPropertyChanged(sender: androidx.databinding.Observable?, propertyId: Int) {
                    if (_binding != null) {
                        binding.userSubtitleText.text = viewModel.userSubtitle.get() ?: "Join to unlock full features"
                    }
                }
            }
        )


        viewModel.showGuestState.addOnPropertyChangedCallback(
            object : androidx.databinding.Observable.OnPropertyChangedCallback() {
                override fun onPropertyChanged(sender: androidx.databinding.Observable?, propertyId: Int) {
                    if (_binding != null) {
                        val showGuest = viewModel.showGuestState.get() ?: true
                        binding.loginCard.visibility = if (showGuest) View.VISIBLE else View.GONE
                        binding.signupCard.visibility = if (showGuest) View.VISIBLE else View.GONE
                    }
                }
            }
        )


        viewModel.showFreelancerSignOut.addOnPropertyChangedCallback(
            object : androidx.databinding.Observable.OnPropertyChangedCallback() {
                override fun onPropertyChanged(sender: androidx.databinding.Observable?, propertyId: Int) {
                    if (_binding != null) {
                        binding.freelancerSignOutSection.visibility =
                            if (viewModel.showFreelancerSignOut.get() ?: false) View.VISIBLE else View.GONE
                    }
                }
            }
        )


        viewModel.showBusinessOwnerSignOut.addOnPropertyChangedCallback(
            object : androidx.databinding.Observable.OnPropertyChangedCallback() {
                override fun onPropertyChanged(sender: androidx.databinding.Observable?, propertyId: Int) {
                    if (_binding != null) {
                        binding.businessOwnerSignOutSection.visibility =
                            if (viewModel.showBusinessOwnerSignOut.get() ?: false) View.VISIBLE else View.GONE
                    }
                }
            }
        )


        viewModel.showFreelancerBadge.addOnPropertyChangedCallback(
            object : androidx.databinding.Observable.OnPropertyChangedCallback() {
                override fun onPropertyChanged(sender: androidx.databinding.Observable?, propertyId: Int) {
                    if (_binding != null) {
                        val showBadge = viewModel.showFreelancerBadge.get() ?: false
                        if (showBadge) {
                            binding.badgeContainer.visibility = View.VISIBLE
                            binding.userRoleBadge.text = "Freelancer"
                            binding.userRoleBadge.setBackgroundResource(R.drawable.orange_badge_bg)
                        }
                    }
                }
            }
        )


        viewModel.showBusinessOwnerBadge.addOnPropertyChangedCallback(
            object : androidx.databinding.Observable.OnPropertyChangedCallback() {
                override fun onPropertyChanged(sender: androidx.databinding.Observable?, propertyId: Int) {
                    if (_binding != null) {
                        val showBadge = viewModel.showBusinessOwnerBadge.get() ?: false
                        if (showBadge) {
                            binding.badgeContainer.visibility = View.VISIBLE
                            binding.userRoleBadge.text = "Business Owner"
                            binding.userRoleBadge.setBackgroundResource(R.drawable.red_badge_bg)
                        } else if (!(viewModel.showFreelancerBadge.get() ?: false)) {
                            binding.badgeContainer.visibility = View.GONE
                        }
                    }
                }
            }
        )


        viewModel.showCompanyInfo.addOnPropertyChangedCallback(
            object : androidx.databinding.Observable.OnPropertyChangedCallback() {
                override fun onPropertyChanged(sender: androidx.databinding.Observable?, propertyId: Int) {
                    if (_binding != null) {
                        val showCompany = viewModel.showCompanyInfo.get() ?: false
                        binding.companyCircle.visibility = if (showCompany) View.VISIBLE else View.GONE
                        binding.companyNameText.visibility = if (showCompany) View.VISIBLE else View.GONE
                    }
                }
            }
        )


        viewModel.companyDisplayText.addOnPropertyChangedCallback(
            object : androidx.databinding.Observable.OnPropertyChangedCallback() {
                override fun onPropertyChanged(sender: androidx.databinding.Observable?, propertyId: Int) {
                    if (_binding != null) {
                        binding.companyNameText.text = viewModel.companyDisplayText.get() ?: "Your Company"
                    }
                }
            }
        )


        viewModel.showFreelancerProfession.addOnPropertyChangedCallback(
            object : androidx.databinding.Observable.OnPropertyChangedCallback() {
                override fun onPropertyChanged(sender: androidx.databinding.Observable?, propertyId: Int) {
                    if (_binding != null) {
                        val showProfession = viewModel.showFreelancerProfession.get() ?: false
                        binding.freelancerProfessionText.visibility = if (showProfession) View.VISIBLE else View.GONE
                        binding.professionCircle.visibility = if (showProfession) View.VISIBLE else View.GONE
                    }
                }
            }
        )


        viewModel.professionDisplayText.addOnPropertyChangedCallback(
            object : androidx.databinding.Observable.OnPropertyChangedCallback() {
                override fun onPropertyChanged(sender: androidx.databinding.Observable?, propertyId: Int) {
                    if (_binding != null) {
                        binding.freelancerProfessionText.text = viewModel.professionDisplayText.get() ?: ""
                    }
                }
            }
        )


        viewModel.showFreelancerRoleSection.addOnPropertyChangedCallback(
            object : androidx.databinding.Observable.OnPropertyChangedCallback() {
                override fun onPropertyChanged(sender: androidx.databinding.Observable?, propertyId: Int) {
                    if (_binding != null) {
                        binding.freelancerRoleSection.visibility =
                            if (viewModel.showFreelancerRoleSection.get() ?: false) View.VISIBLE else View.GONE
                    }
                }
            }
        )


        viewModel.currentRoleDisplayText.addOnPropertyChangedCallback(
            object : androidx.databinding.Observable.OnPropertyChangedCallback() {
                override fun onPropertyChanged(sender: androidx.databinding.Observable?, propertyId: Int) {
                    if (_binding != null) {
                        binding.currentRoleText.text = viewModel.currentRoleDisplayText.get() ?: "Set your professional role"
                    }
                }
            }
        )


        viewModel.showBusinessOwnerSection.addOnPropertyChangedCallback(
            object : androidx.databinding.Observable.OnPropertyChangedCallback() {
                override fun onPropertyChanged(sender: androidx.databinding.Observable?, propertyId: Int) {
                    if (_binding != null) {
                        binding.businessOwnerActionsSection.visibility =
                            if (viewModel.showBusinessOwnerSection.get() ?: false) View.VISIBLE else View.GONE
                    }
                }
            }
        )
    }




    private fun setupLiveDataObservers() {




        profileViewModel.isLoading.observe(viewLifecycleOwner) { _ ->


        }


        profileViewModel.showSuccessMessage.observe(viewLifecycleOwner) { message ->
            message?.let {
                Toast.makeText(requireContext(), it, Toast.LENGTH_SHORT).show()
                profileViewModel.onSuccessMessageShown()
            }
        }


        profileViewModel.showErrorMessage.observe(viewLifecycleOwner) { message ->
            message?.let {
                Toast.makeText(requireContext(), it, Toast.LENGTH_SHORT).show()
                profileViewModel.onErrorMessageShown()
            }
        }
    }




    private fun applyInitialUIState() {
        if (_binding == null || !isAdded) return


        profileViewModel.showGuestState.notifyChange()
        profileViewModel.showFreelancerSignOut.notifyChange()
        profileViewModel.showBusinessOwnerSignOut.notifyChange()
        profileViewModel.showFreelancerBadge.notifyChange()
        profileViewModel.showBusinessOwnerBadge.notifyChange()
        profileViewModel.showCompanyInfo.notifyChange()
        profileViewModel.showFreelancerProfession.notifyChange()
        profileViewModel.showFreelancerRoleSection.notifyChange()
        profileViewModel.showBusinessOwnerSection.notifyChange()
        profileViewModel.userName.notifyChange()
        profileViewModel.userSubtitle.notifyChange()
        profileViewModel.companyDisplayText.notifyChange()
        profileViewModel.professionDisplayText.notifyChange()
        profileViewModel.currentRoleDisplayText.notifyChange()
    }

    override fun onResume() {
        super.onResume()

        if (_binding != null && isAdded) {
            profileViewModel.refreshLoginState()
        }
    }

    override fun onLoginStateRefresh(isLoggedIn: Boolean) {

        if (_binding != null && isAdded) {
            profileViewModel.refreshLoginState()

            applyInitialUIState()
        }
    }






    private fun showSignOutDialog() {

        val ctx = context
        if (!isAdded || ctx == null) return

        val dialog = AlertDialog.Builder(ctx)
            .setTitle("Sign Out")
            .setMessage("Are you sure you want to sign out? You'll return to guest mode and lose access to your bookmarks and chat history.")
            .setPositiveButton("Sign Out") { _, _ ->
                performSignOut()
            }
            .setNegativeButton("Cancel", null)
            .create()


        activeDialogs.add(dialog)


        dialog.setOnDismissListener {
            activeDialogs.remove(dialog)
        }

        dialog.show()
    }

    private fun performSignOut() {

        profileViewModel.signOut()


        refreshAppState()
    }

    private fun showRoleInputDialog() {

        val ctx = context
        if (!isAdded || ctx == null) return


        profileViewModel.prepareEditProfessionDialog()


        val dialogView = LayoutInflater.from(ctx).inflate(R.layout.dialog_simple_profession, null)


        val titleText = dialogView.findViewById<android.widget.TextView>(R.id.professionDialogTitle)
        val messageText = dialogView.findViewById<android.widget.TextView>(R.id.professionDialogMessage)
        val currentProfessionText = dialogView.findViewById<android.widget.TextView>(R.id.currentProfessionDisplay)
        val inputEditText = dialogView.findViewById<android.widget.EditText>(R.id.professionInputField)
        val errorText = dialogView.findViewById<android.widget.TextView>(R.id.professionErrorMessage)
        val closeButton = dialogView.findViewById<android.widget.TextView>(R.id.closeProfessionDialog)
        val editButton = dialogView.findViewById<android.widget.TextView>(R.id.editProfessionButton)


        val dialog = Dialog(ctx)
        dialog.requestWindowFeature(Window.FEATURE_NO_TITLE)
        dialog.setContentView(dialogView)


        activeDialogs.add(dialog)






        profileViewModel.professionDialogTitle.addOnPropertyChangedCallback(
            object : androidx.databinding.Observable.OnPropertyChangedCallback() {
                override fun onPropertyChanged(sender: androidx.databinding.Observable?, propertyId: Int) {
                    if (dialog.isShowing) {
                        titleText.text = profileViewModel.professionDialogTitle.get() ?: "Set Role"
                    }
                }
            }
        )


        profileViewModel.professionDialogMessage.addOnPropertyChangedCallback(
            object : androidx.databinding.Observable.OnPropertyChangedCallback() {
                override fun onPropertyChanged(sender: androidx.databinding.Observable?, propertyId: Int) {
                    if (dialog.isShowing) {
                        messageText.text = profileViewModel.professionDialogMessage.get() ?: ""
                    }
                }
            }
        )


        profileViewModel.professionDisplayText.addOnPropertyChangedCallback(
            object : androidx.databinding.Observable.OnPropertyChangedCallback() {
                override fun onPropertyChanged(sender: androidx.databinding.Observable?, propertyId: Int) {
                    if (dialog.isShowing) {
                        val profession = profileViewModel.professionDisplayText.get() ?: ""
                        currentProfessionText.text = if (profession.isNotEmpty()) profession else "Not set"
                    }
                }
            }
        )


        profileViewModel.showProfessionValidationError.addOnPropertyChangedCallback(
            object : androidx.databinding.Observable.OnPropertyChangedCallback() {
                override fun onPropertyChanged(sender: androidx.databinding.Observable?, propertyId: Int) {
                    if (dialog.isShowing) {
                        errorText.visibility = if (profileViewModel.showProfessionValidationError.get() ?: false)
                            View.VISIBLE else View.GONE
                    }
                }
            }
        )


        profileViewModel.professionValidationMessage.addOnPropertyChangedCallback(
            object : androidx.databinding.Observable.OnPropertyChangedCallback() {
                override fun onPropertyChanged(sender: androidx.databinding.Observable?, propertyId: Int) {
                    if (dialog.isShowing) {
                        errorText.text = profileViewModel.professionValidationMessage.get() ?: ""
                    }
                }
            }
        )


        inputEditText.addTextChangedListener(object : android.text.TextWatcher {
            override fun beforeTextChanged(s: CharSequence?, start: Int, count: Int, after: Int) {}
            override fun onTextChanged(s: CharSequence?, start: Int, before: Int, count: Int) {
                profileViewModel.clearProfessionValidation()
            }
            override fun afterTextChanged(s: android.text.Editable?) {
                profileViewModel.professionInputText.set(s?.toString() ?: "")
            }
        })


        profileViewModel.professionInputText.addOnPropertyChangedCallback(
            object : androidx.databinding.Observable.OnPropertyChangedCallback() {
                override fun onPropertyChanged(sender: androidx.databinding.Observable?, propertyId: Int) {
                    if (dialog.isShowing) {
                        val text = profileViewModel.professionInputText.get() ?: ""
                        if (inputEditText.text.toString() != text) {
                            inputEditText.setText(text)
                            inputEditText.setSelection(text.length)
                        }
                    }
                }
            }
        )


        titleText.text = profileViewModel.professionDialogTitle.get()
        messageText.text = profileViewModel.professionDialogMessage.get()
        val currentProfession = profileViewModel.professionDisplayText.get() ?: ""
        currentProfessionText.text = if (currentProfession.isNotEmpty()) currentProfession else "Not set"
        inputEditText.setText(profileViewModel.professionInputText.get())


        editButton.setOnClickListener {

            val inputText = inputEditText.text.toString().trim()
            if (inputText.isEmpty()) {
                profileViewModel.showProfessionValidationError.set(true)
                profileViewModel.professionValidationMessage.set("Please enter a valid profession")
                return@setOnClickListener
            }


            profileViewModel.clearProfessionValidation()
            if (profileViewModel.validateAndUpdateProfession()) {
                dialog.dismiss()
            }
        }

        closeButton.setOnClickListener {
            profileViewModel.clearProfessionValidation()
            dialog.dismiss()
        }


        val window = dialog.window
        window?.setLayout(ViewGroup.LayoutParams.MATCH_PARENT, ViewGroup.LayoutParams.WRAP_CONTENT)
        window?.setBackgroundDrawableResource(android.R.color.transparent)


        @Suppress("DEPRECATION")
        window?.setSoftInputMode(
            android.view.WindowManager.LayoutParams.SOFT_INPUT_STATE_VISIBLE or
            android.view.WindowManager.LayoutParams.SOFT_INPUT_ADJUST_RESIZE
        )


        dialog.setOnDismissListener {
            activeDialogs.remove(dialog)
        }

        dialog.show()


        inputEditText.post {
            if (dialog.isShowing && !isDetached && context != null) {
                inputEditText.requestFocus()
            }
        }
    }

    private fun refreshAppState() {

        activity?.let {



        }
    }

    private fun setupClickListeners() {

        if (_binding == null || !isAdded) return


        binding.loginCard.setOnClickListener {

            val ctx = context
            if (!isAdded || ctx == null) return@setOnClickListener

            val intent = Intent(ctx, LoginActivity::class.java)
            intent.putExtra("SOURCE_SCREEN", "profile")
            startActivity(intent)
        }


        binding.signupCard.setOnClickListener {

            val ctx = context
            if (!isAdded || ctx == null) return@setOnClickListener

            val intent = Intent(ctx, LoginActivity::class.java)
            intent.putExtra("START_WITH_SIGNUP", true)
            intent.putExtra("SOURCE_SCREEN", "profile")
            startActivity(intent)
        }


        binding.aboutCard.setOnClickListener {

            parentFragmentManager.beginTransaction()
                .replace(android.R.id.content, AppInfoFragment())
                .addToBackStack(null)
                .commit()
        }


        binding.freelancerSignOutCard.setOnClickListener {
            showSignOutDialog()
        }


        binding.businessOwnerSignOutCard.setOnClickListener {
            showSignOutDialog()
        }


        binding.createJobCard.setOnClickListener {
            showCreateJobDialog()
        }


        binding.settingsButton.setOnClickListener {
            showSettingsDialog()
        }


        binding.roleSettingsCard.setOnClickListener {
            showRoleInputDialog()
        }
    }

    private fun showCreateJobDialog() {

        val ctx = context
        if (!isAdded || ctx == null) return

        val dialog = Dialog(ctx)
        dialog.requestWindowFeature(Window.FEATURE_NO_TITLE)
        val dialogBinding = DialogCreateJobBinding.inflate(layoutInflater)

        dialog.setContentView(dialogBinding.root)


        activeDialogs.add(dialog)


        val window = dialog.window
        window?.setLayout(ViewGroup.LayoutParams.MATCH_PARENT, ViewGroup.LayoutParams.WRAP_CONTENT)
        window?.setBackgroundDrawableResource(android.R.color.transparent)


        @Suppress("DEPRECATION")
        window?.setSoftInputMode(android.view.WindowManager.LayoutParams.SOFT_INPUT_ADJUST_RESIZE)


        dialog.setOnDismissListener {
            activeDialogs.remove(dialog)
        }

        setupCreateJobDialog(dialogBinding, dialog)

        dialog.show()
    }

    private fun setupCreateJobDialog(dialogBinding: DialogCreateJobBinding, dialog: Dialog) {

        val ctx = context
        if (!isAdded || ctx == null) return


        jobManagementViewModel.initializeFormForCreate()
        jobManagementViewModel.refreshCompanyInfo()


        // Set company name from user's profile
        val userCompanyName = profileViewModel.companyDisplayText.get() ?: "Your Company"
        dialogBinding.textViewCompany.text = userCompanyName
        // Also set it in the JobManagementViewModel so it's used during actual job creation
        jobManagementViewModel.setCompanyName(userCompanyName)
        android.util.Log.d("ProfileFragment", "Setting create job dialog company name to: '$userCompanyName'")


        setupCreateJobDialogReactiveBinding(dialogBinding, jobManagementViewModel)
        setupCreateJobDialogLiveDataObservers(dialog)


        val filterOptions = mainViewModel.getFilterOptions()


        val salaryAdapter = ArrayAdapter(ctx, R.layout.spinner_item, filterOptions.salaryRanges)
        salaryAdapter.setDropDownViewResource(android.R.layout.simple_spinner_dropdown_item)
        dialogBinding.spinnerSalary.adapter = salaryAdapter


        val jobTypeAdapter = ArrayAdapter(ctx, R.layout.spinner_item, filterOptions.jobTypes)
        jobTypeAdapter.setDropDownViewResource(android.R.layout.simple_spinner_dropdown_item)
        dialogBinding.spinnerJobType.adapter = jobTypeAdapter


        val workingModelAdapter = ArrayAdapter(ctx, R.layout.spinner_item, filterOptions.workingModels)
        workingModelAdapter.setDropDownViewResource(android.R.layout.simple_spinner_dropdown_item)
        dialogBinding.spinnerWorkingModel.adapter = workingModelAdapter


        val levelAdapter = ArrayAdapter(ctx, R.layout.spinner_item, filterOptions.levels)
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
                        android.util.Log.e("ProfileFragment", "Error closing dialog", e)
                    }
                    loadOwnedJobs()
                } else {
                    android.util.Log.e("ProfileFragment", "Job creation failed - dialog stays open for user to fix errors")
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


        viewModel.descriptionCharacterCount.addOnPropertyChangedCallback(object : androidx.databinding.Observable.OnPropertyChangedCallback() {
            override fun onPropertyChanged(sender: androidx.databinding.Observable?, propertyId: Int) {


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
                    android.util.Log.e("ProfileFragment", "Error closing dialog on success", e)
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




    private fun loadOwnedJobs() {



    }

    private fun showSettingsDialog() {

        val ctx = context
        if (!isAdded || ctx == null) return

        if (UserSession.isLoggedIn()) {

            showUserSettingsDialog(ctx)
        } else {

            showGuestSettingsDialog(ctx)
        }
    }

    private fun showUserSettingsDialog(context: android.content.Context) {
        val options = if (UserSession.getUserRole() == UserSession.ROLE_FREELANCER) {
            arrayOf("Edit Profile", "Change Profession", "Notification Settings", "Privacy Settings", "Help & Support")
        } else {
            arrayOf("Edit Profile", "Company Settings", "Notification Settings", "Privacy Settings", "Help & Support")
        }

        AlertDialog.Builder(context)
            .setTitle("Settings")
            .setItems(options) { _, which ->
                when (which) {
                    0 -> showEditProfileDialog()
                    1 -> if (UserSession.getUserRole() == UserSession.ROLE_FREELANCER) {
                        showChangeProfessionDialog()
                    } else {
                        showCompanySettingsDialog()
                    }
                    2 -> showNotificationSettingsDialog()
                    3 -> showPrivacySettingsDialog()
                    4 -> showHelpSupportDialog()
                }
            }
            .setNegativeButton("Cancel", null)
            .show()
    }

    private fun showGuestSettingsDialog(context: android.content.Context) {
        val options = arrayOf("Help & Support", "About App")

        AlertDialog.Builder(context)
            .setTitle("Settings")
            .setItems(options) { _, which ->
                when (which) {
                    0 -> showHelpSupportDialog()
                    1 -> {

                        parentFragmentManager.beginTransaction()
                            .replace(android.R.id.content, AppInfoFragment())
                            .addToBackStack(null)
                            .commit()
                    }
                }
            }
            .setNegativeButton("Cancel", null)
            .show()
    }

    private fun showEditProfileDialog() {
        val ctx = context ?: return
        Toast.makeText(ctx, "Edit Profile feature coming soon!", Toast.LENGTH_SHORT).show()
    }

    private fun showChangeProfessionDialog() {
        showRoleInputDialog()
    }



    private fun showCompanySettingsDialog() {
        val ctx = context ?: return
        Toast.makeText(ctx, "Company Settings feature coming soon!", Toast.LENGTH_SHORT).show()
    }

    private fun showNotificationSettingsDialog() {
        val ctx = context ?: return
        Toast.makeText(ctx, "Notification Settings feature coming soon!", Toast.LENGTH_SHORT).show()
    }

    private fun showPrivacySettingsDialog() {
        val ctx = context ?: return
        Toast.makeText(ctx, "Privacy Settings feature coming soon!", Toast.LENGTH_SHORT).show()
    }

    private fun showHelpSupportDialog() {
        val ctx = context ?: return

        AlertDialog.Builder(ctx)
            .setTitle("Help & Support")
            .setMessage("Job Finder App v1.0\n\nFor support, please contact:\nsupport@jobfinder.com\n\nOr visit our website:\nwww.jobfinder.com")
            .setPositiveButton("OK", null)
            .show()
    }

    override fun onDestroyView() {

        activeDialogs.forEach { dialog ->
            try {
                if (dialog.isShowing) {
                    dialog.dismiss()
                }
            } catch (e: Exception) {

            }
        }
        activeDialogs.clear()

        super.onDestroyView()
        _binding = null
    }
}