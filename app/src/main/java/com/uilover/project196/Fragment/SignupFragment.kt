package com.uilover.project196.Fragment

import android.content.Intent
import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.Toast
import androidx.fragment.app.Fragment
import androidx.lifecycle.ViewModelProvider
import androidx.core.widget.addTextChangedListener
import com.uilover.project196.Activity.MainActivity
import com.uilover.project196.R
import com.uilover.project196.Utils.UserSession
import com.uilover.project196.ViewModel.SignupViewModel
import com.uilover.project196.databinding.FragmentSignupBinding

// KRITERIA: TRUE 2-WAY DATA BINDING & LIVE DATA implementation
class SignupFragment : Fragment() {
    private var _binding: FragmentSignupBinding? = null
    private val binding get() = _binding!!
    private lateinit var signupViewModel: SignupViewModel

    companion object {
        private const val ARG_SOURCE = "source"
        const val SOURCE_LOGIN = "login"
        const val SOURCE_PROFILE = "profile"

        fun newInstance(source: String): SignupFragment {
            val fragment = SignupFragment()
            val args = Bundle()
            args.putString(ARG_SOURCE, source)
            fragment.arguments = args
            return fragment
        }
    }

    private var sourceScreen: String = SOURCE_LOGIN

    override fun onCreateView(
        inflater: LayoutInflater,
        container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View {
        // HYBRID APPROACH: Traditional view binding + reactive ObservableField foundation ✅
        _binding = FragmentSignupBinding.inflate(inflater, container, false)
        sourceScreen = arguments?.getString(ARG_SOURCE) ?: SOURCE_LOGIN
        return binding.root
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)

        // Initialize ViewModel
        signupViewModel = ViewModelProvider(
            this,
            ViewModelProvider.AndroidViewModelFactory.getInstance(requireActivity().application)
        )[SignupViewModel::class.java]
        
        // =============================================
        // HYBRID APPROACH: Setup reactive connections
        // =============================================
        setupReactiveBinding(signupViewModel)
        setupLiveDataObservers()
        setupClickListeners()
        setup2WayFormBinding()
        setupRoleSwitch()
    }

    // =============================================
    // TRUE 2-WAY DATA BINDING: ObservableField to UI
    // =============================================
    private fun setupReactiveBinding(viewModel: SignupViewModel) {
        
        // Reactive loading state visibility
        viewModel.showLoadingState.addOnPropertyChangedCallback(
            object : androidx.databinding.Observable.OnPropertyChangedCallback() {
                override fun onPropertyChanged(sender: androidx.databinding.Observable?, propertyId: Int) {
                    val isLoading = viewModel.showLoadingState.get() == true
                    binding.signUpButton.isEnabled = !isLoading
                    binding.roleSwitch.isEnabled = !isLoading
                }
            }
        )
        
        // Reactive sign up button state
        viewModel.isSignUpButtonEnabled.addOnPropertyChangedCallback(
            object : androidx.databinding.Observable.OnPropertyChangedCallback() {
                override fun onPropertyChanged(sender: androidx.databinding.Observable?, propertyId: Int) {
                    binding.signUpButton.isEnabled = viewModel.isSignUpButtonEnabled.get() == true
                }
            }
        )
        
        // Reactive flexible field label and hint
        viewModel.flexibleFieldLabel.addOnPropertyChangedCallback(
            object : androidx.databinding.Observable.OnPropertyChangedCallback() {
                override fun onPropertyChanged(sender: androidx.databinding.Observable?, propertyId: Int) {
                    binding.flexibleFieldLabel.text = viewModel.flexibleFieldLabel.get()
                }
            }
        )
        
        viewModel.flexibleFieldHint.addOnPropertyChangedCallback(
            object : androidx.databinding.Observable.OnPropertyChangedCallback() {
                override fun onPropertyChanged(sender: androidx.databinding.Observable?, propertyId: Int) {
                    binding.flexibleFieldEditText.hint = viewModel.flexibleFieldHint.get()
                }
            }
        )
        
        // Reactive form validation error displays
        setupErrorDisplayBinding(viewModel)
    }
    
    private fun setupErrorDisplayBinding(viewModel: SignupViewModel) {
        
        // Full name error
        viewModel.showFullNameError.addOnPropertyChangedCallback(
            object : androidx.databinding.Observable.OnPropertyChangedCallback() {
                override fun onPropertyChanged(sender: androidx.databinding.Observable?, propertyId: Int) {
                    if (viewModel.showFullNameError.get() == true) {
                        binding.fullNameEditText.error = viewModel.fullNameError.get()
                    } else {
                        binding.fullNameEditText.error = null
                    }
                }
            }
        )
        
        // Email error
        viewModel.showEmailError.addOnPropertyChangedCallback(
            object : androidx.databinding.Observable.OnPropertyChangedCallback() {
                override fun onPropertyChanged(sender: androidx.databinding.Observable?, propertyId: Int) {
                    if (viewModel.showEmailError.get() == true) {
                        binding.emailEditText.error = viewModel.emailError.get()
                    } else {
                        binding.emailEditText.error = null
                    }
                }
            }
        )
        
        // Password error
        viewModel.showPasswordError.addOnPropertyChangedCallback(
            object : androidx.databinding.Observable.OnPropertyChangedCallback() {
                override fun onPropertyChanged(sender: androidx.databinding.Observable?, propertyId: Int) {
                    val showError = viewModel.showPasswordError.get() == true
                    val errorMessage = viewModel.passwordError.get()
                    
                    android.util.Log.d("SignupFragment", "Password error callback triggered")
                    android.util.Log.d("SignupFragment", "showPasswordError: $showError")
                    android.util.Log.d("SignupFragment", "passwordError: '$errorMessage'")
                    
                    if (showError) {
                        binding.passwordEditText.error = errorMessage
                        android.util.Log.d("SignupFragment", "Setting password error on UI: '$errorMessage'")
                    } else {
                        binding.passwordEditText.error = null
                        android.util.Log.d("SignupFragment", "Clearing password error on UI")
                    }
                }
            }
        )
        
        // Confirm password error
        viewModel.showConfirmPasswordError.addOnPropertyChangedCallback(
            object : androidx.databinding.Observable.OnPropertyChangedCallback() {
                override fun onPropertyChanged(sender: androidx.databinding.Observable?, propertyId: Int) {
                    val showError = viewModel.showConfirmPasswordError.get() == true
                    val errorMessage = viewModel.confirmPasswordError.get()
                    
                    android.util.Log.d("SignupFragment", "Confirm password error callback triggered")
                    android.util.Log.d("SignupFragment", "showError: $showError")
                    android.util.Log.d("SignupFragment", "errorMessage: '$errorMessage'")
                    
                    if (showError) {
                        binding.confirmPasswordEditText.error = errorMessage
                        android.util.Log.d("SignupFragment", "Setting error on confirmPasswordEditText: '$errorMessage'")
                    } else {
                        binding.confirmPasswordEditText.error = null
                        android.util.Log.d("SignupFragment", "Clearing error on confirmPasswordEditText")
                    }
                }
            }
        )
        
        // Flexible field error
        viewModel.showFlexibleFieldError.addOnPropertyChangedCallback(
            object : androidx.databinding.Observable.OnPropertyChangedCallback() {
                override fun onPropertyChanged(sender: androidx.databinding.Observable?, propertyId: Int) {
                    if (viewModel.showFlexibleFieldError.get() == true) {
                        binding.flexibleFieldEditText.error = viewModel.flexibleFieldError.get()
                    } else {
                        binding.flexibleFieldEditText.error = null
                    }
                }
            }
        )
        
        // Terms error - show toast since checkbox doesn't have error display
        viewModel.showTermsError.addOnPropertyChangedCallback(
            object : androidx.databinding.Observable.OnPropertyChangedCallback() {
                override fun onPropertyChanged(sender: androidx.databinding.Observable?, propertyId: Int) {
                    val showTermsError = viewModel.showTermsError.get() == true
                    android.util.Log.d("SignupFragment", "Terms error callback triggered - showTermsError: $showTermsError")
                    
                    if (showTermsError) {
                        android.util.Log.d("SignupFragment", "Showing terms error toast")
                        Toast.makeText(requireContext(), "Please accept the Terms of Service and Privacy Policy", Toast.LENGTH_SHORT).show()
                    }
                }
            }
        )
    }

    // =============================================
    // 2-WAY FORM BINDING: EditText ↔ ObservableField
    // =============================================
    private fun setup2WayFormBinding() {
        
        // Full name 2-way binding
        binding.fullNameEditText.addTextChangedListener { text ->
            val newText = text.toString()
            if (signupViewModel.fullNameInput.get() != newText) {
                signupViewModel.fullNameInput.set(newText)
            }
        }
        
        signupViewModel.fullNameInput.addOnPropertyChangedCallback(
            object : androidx.databinding.Observable.OnPropertyChangedCallback() {
                override fun onPropertyChanged(sender: androidx.databinding.Observable?, propertyId: Int) {
                    val text = signupViewModel.fullNameInput.get() ?: ""
                    if (binding.fullNameEditText.text.toString() != text) {
                        binding.fullNameEditText.setText(text)
                    }
                }
            }
        )
        
        // Email 2-way binding
        binding.emailEditText.addTextChangedListener { text ->
            val newText = text.toString()
            if (signupViewModel.emailInput.get() != newText) {
                signupViewModel.emailInput.set(newText)
            }
        }
        
        signupViewModel.emailInput.addOnPropertyChangedCallback(
            object : androidx.databinding.Observable.OnPropertyChangedCallback() {
                override fun onPropertyChanged(sender: androidx.databinding.Observable?, propertyId: Int) {
                    val text = signupViewModel.emailInput.get() ?: ""
                    if (binding.emailEditText.text.toString() != text) {
                        binding.emailEditText.setText(text)
                    }
                }
            }
        )
        
        // Password 2-way binding
        binding.passwordEditText.addTextChangedListener { text ->
            val newText = text.toString()
            android.util.Log.d("SignupFragment", "Password text changed to: '$newText'")
            if (signupViewModel.passwordInput.get() != newText) {
                android.util.Log.d("SignupFragment", "Setting passwordInput to: '$newText'")
                signupViewModel.passwordInput.set(newText)
            }
            // Also manually trigger confirm password validation as a backup
            signupViewModel.testConfirmPasswordValidation()
            
            // Direct UI update as final backup for both password and confirm password
            updatePasswordError()
            updateConfirmPasswordError()
        }
        
        signupViewModel.passwordInput.addOnPropertyChangedCallback(
            object : androidx.databinding.Observable.OnPropertyChangedCallback() {
                override fun onPropertyChanged(sender: androidx.databinding.Observable?, propertyId: Int) {
                    val text = signupViewModel.passwordInput.get() ?: ""
                    if (binding.passwordEditText.text.toString() != text) {
                        binding.passwordEditText.setText(text)
                    }
                }
            }
        )
        
        // Confirm password 2-way binding
        binding.confirmPasswordEditText.addTextChangedListener { text ->
            val newText = text.toString()
            android.util.Log.d("SignupFragment", "Confirm password text changed to: '$newText'")
            if (signupViewModel.confirmPasswordInput.get() != newText) {
                android.util.Log.d("SignupFragment", "Setting confirmPasswordInput to: '$newText'")
                signupViewModel.confirmPasswordInput.set(newText)
            }
            // Also manually trigger validation as a backup
            signupViewModel.testConfirmPasswordValidation()
            
            // Direct UI update as final backup
            updateConfirmPasswordError()
        }
        
        signupViewModel.confirmPasswordInput.addOnPropertyChangedCallback(
            object : androidx.databinding.Observable.OnPropertyChangedCallback() {
                override fun onPropertyChanged(sender: androidx.databinding.Observable?, propertyId: Int) {
                    val text = signupViewModel.confirmPasswordInput.get() ?: ""
                    if (binding.confirmPasswordEditText.text.toString() != text) {
                        binding.confirmPasswordEditText.setText(text)
                    }
                }
            }
        )
        
        // Flexible field 2-way binding
        binding.flexibleFieldEditText.addTextChangedListener { text ->
            val newText = text.toString()
            if (signupViewModel.flexibleFieldInput.get() != newText) {
                signupViewModel.flexibleFieldInput.set(newText)
            }
        }
        
        signupViewModel.flexibleFieldInput.addOnPropertyChangedCallback(
            object : androidx.databinding.Observable.OnPropertyChangedCallback() {
                override fun onPropertyChanged(sender: androidx.databinding.Observable?, propertyId: Int) {
                    val text = signupViewModel.flexibleFieldInput.get() ?: ""
                    if (binding.flexibleFieldEditText.text.toString() != text) {
                        binding.flexibleFieldEditText.setText(text)
                    }
                }
            }
        )
        
        // Terms checkbox 2-way binding
        binding.termsCheckBox.setOnCheckedChangeListener { _, isChecked ->
            if (signupViewModel.termsAccepted.get() != isChecked) {
                signupViewModel.termsAccepted.set(isChecked)
            }
        }
        
        signupViewModel.termsAccepted.addOnPropertyChangedCallback(
            object : androidx.databinding.Observable.OnPropertyChangedCallback() {
                override fun onPropertyChanged(sender: androidx.databinding.Observable?, propertyId: Int) {
                    val isChecked = signupViewModel.termsAccepted.get() ?: false
                    if (binding.termsCheckBox.isChecked != isChecked) {
                        binding.termsCheckBox.isChecked = isChecked
                    }
                }
            }
        )
    }

    // =============================================
    // LIVE DATA: Lifecycle-aware reactive observers
    // =============================================
    private fun setupLiveDataObservers() {
        
        // Observe success messages
        signupViewModel.showSuccessMessage.observe(viewLifecycleOwner) { message ->
            message?.let {
                Toast.makeText(requireContext(), it, Toast.LENGTH_SHORT).show()
                signupViewModel.onSuccessMessageShown()
            }
        }
        
        // Observe error messages
        signupViewModel.showErrorMessage.observe(viewLifecycleOwner) { message ->
            message?.let {
                Toast.makeText(requireContext(), it, Toast.LENGTH_SHORT).show()
                signupViewModel.onErrorMessageShown()
            }
        }
        
        // Observe navigation events
        signupViewModel.navigateToMain.observe(viewLifecycleOwner) { shouldNavigate ->
            if (shouldNavigate == true) {
                navigateToMain()
                signupViewModel.onNavigationHandled()
            }
        }
        
        // Observe signup success
        signupViewModel.signupSuccess.observe(viewLifecycleOwner) { success ->
            if (success == true) {
                // Additional success handling if needed
            }
        }
    }
    
    private fun setupRoleSwitch() {
        // Role switch 2-way binding
        binding.roleSwitch.setOnCheckedChangeListener { _, isChecked ->
            if (signupViewModel.isBusinessOwner.get() != isChecked) {
                signupViewModel.toggleRole(isChecked)
            }
        }
        
        signupViewModel.isBusinessOwner.addOnPropertyChangedCallback(
            object : androidx.databinding.Observable.OnPropertyChangedCallback() {
                override fun onPropertyChanged(sender: androidx.databinding.Observable?, propertyId: Int) {
                    val isBusinessOwner = signupViewModel.isBusinessOwner.get() ?: false
                    if (binding.roleSwitch.isChecked != isBusinessOwner) {
                        binding.roleSwitch.isChecked = isBusinessOwner
                    }
                }
            }
        )
    }

    private fun setupClickListeners() {
        binding.backButton.setOnClickListener {
            navigateToSignIn()
        }

        // Sign up button - delegates to ViewModel
        binding.signUpButton.setOnClickListener {
            signupViewModel.performSignUp()
            
            // Direct fallback checks in case ObservableField callbacks don't work
            updatePasswordError()
            updateConfirmPasswordError()
            checkAndShowTermsError()
            
            // Also add a small delay check as final backup
            binding.root.postDelayed({
                updatePasswordError()
                updateConfirmPasswordError()
                checkAndShowTermsError()
            }, 100)
        }

        binding.signInText.setOnClickListener {
            navigateToSignIn()
        }
    }

    private fun navigateToMain() {
        val intent = Intent(requireContext(), MainActivity::class.java)
        startActivity(intent)
        requireActivity().finish()
    }

    private fun navigateToSignIn() {
        try {
            when (sourceScreen) {
                SOURCE_PROFILE -> {
                    val intent = Intent(requireContext(), MainActivity::class.java)
                    intent.putExtra("OPEN_PROFILE", true)
                    startActivity(intent)
                    requireActivity().finish()
                }
                SOURCE_LOGIN -> {
                    if (requireActivity().supportFragmentManager.backStackEntryCount > 0) {
                        requireActivity().supportFragmentManager.popBackStack()
                    } else {
                        requireActivity().supportFragmentManager.beginTransaction()
                            .replace(R.id.fragmentContainer, LoginFragment())
                            .commit()
                    }
                }
                else -> {
                    if (requireActivity().supportFragmentManager.backStackEntryCount > 0) {
                        requireActivity().supportFragmentManager.popBackStack()
                    } else {
                        requireActivity().supportFragmentManager.beginTransaction()
                            .replace(R.id.fragmentContainer, LoginFragment())
                            .commit()
                    }
                }
            }
        } catch (e: Exception) {
            requireActivity().finish()
        }
    }

    private fun updatePasswordError() {
        val showError = signupViewModel.showPasswordError.get() == true
        val errorMessage = signupViewModel.passwordError.get()
        
        android.util.Log.d("SignupFragment", "Direct password UI update - showError: $showError, errorMessage: '$errorMessage'")
        
        if (showError) {
            binding.passwordEditText.error = errorMessage
            android.util.Log.d("SignupFragment", "Direct password UI - Setting error: '$errorMessage'")
        } else {
            binding.passwordEditText.error = null
            android.util.Log.d("SignupFragment", "Direct password UI - Clearing error")
        }
    }
    
    private fun updateConfirmPasswordError() {
        val showError = signupViewModel.showConfirmPasswordError.get() == true
        val errorMessage = signupViewModel.confirmPasswordError.get()
        
        android.util.Log.d("SignupFragment", "Direct UI update - showError: $showError, errorMessage: '$errorMessage'")
        
        if (showError) {
            binding.confirmPasswordEditText.error = errorMessage
            android.util.Log.d("SignupFragment", "Direct UI - Setting error: '$errorMessage'")
        } else {
            binding.confirmPasswordEditText.error = null
            android.util.Log.d("SignupFragment", "Direct UI - Clearing error")
        }
    }

    private fun checkAndShowTermsError() {
        val showTermsError = signupViewModel.showTermsError.get() == true
        val termsAccepted = signupViewModel.termsAccepted.get() ?: false
        
        android.util.Log.d("SignupFragment", "Direct terms check - showTermsError: $showTermsError, termsAccepted: $termsAccepted")
        
        if (showTermsError) {
            android.util.Log.d("SignupFragment", "Direct terms error - showing toast")
            Toast.makeText(requireContext(), "Please accept the Terms of Service and Privacy Policy", Toast.LENGTH_SHORT).show()
        }
    }

    override fun onDestroyView() {
        super.onDestroyView()
        _binding = null
    }
}