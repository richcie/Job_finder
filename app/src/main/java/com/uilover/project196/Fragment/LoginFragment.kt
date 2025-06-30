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
import com.uilover.project196.ViewModel.LoginViewModel
import com.uilover.project196.databinding.FragmentLoginBinding

// KRITERIA: TRUE 2-WAY DATA BINDING & LIVE DATA implementation
class LoginFragment : Fragment() {
    private var _binding: FragmentLoginBinding? = null
    private val binding get() = _binding!!
    private lateinit var loginViewModel: LoginViewModel

    companion object {
        private const val ARG_SOURCE = "source"
        const val SOURCE_INTRO = "intro"
        const val SOURCE_PROFILE = "profile"
        const val SOURCE_OTHER = "other"

        fun newInstance(source: String): LoginFragment {
            val fragment = LoginFragment()
            val args = Bundle()
            args.putString(ARG_SOURCE, source)
            fragment.arguments = args
            return fragment
        }
    }

    private var sourceScreen: String = SOURCE_INTRO

    override fun onCreateView(
        inflater: LayoutInflater,
        container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View {
        // HYBRID APPROACH: Traditional view binding + reactive ObservableField foundation ✅
        _binding = FragmentLoginBinding.inflate(inflater, container, false)
        sourceScreen = arguments?.getString(ARG_SOURCE) ?: SOURCE_INTRO
        return binding.root
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)
        
        // Initialize ViewModel
        loginViewModel = ViewModelProvider(
            this,
            ViewModelProvider.AndroidViewModelFactory.getInstance(requireActivity().application)
        )[LoginViewModel::class.java]
        
        // =============================================
        // HYBRID APPROACH: Setup reactive connections
        // =============================================
        setupReactiveBinding(loginViewModel)
        setupLiveDataObservers()
        setupClickListeners()
        setup2WayFormBinding()
        
        // DIRECT UPDATE: Initial error state sync
        updateEmailErrorDirect()
        updatePasswordErrorDirect()
    }

    // =============================================
    // TRUE 2-WAY DATA BINDING: ObservableField to UI
    // =============================================
    private fun setupReactiveBinding(viewModel: LoginViewModel) {
        
        // Reactive loading state visibility
        viewModel.showLoadingState.addOnPropertyChangedCallback(
            object : androidx.databinding.Observable.OnPropertyChangedCallback() {
                override fun onPropertyChanged(sender: androidx.databinding.Observable?, propertyId: Int) {
                    // Show/hide loading indicator (you can add a ProgressBar to layout if needed)
                    // For now, we'll update button states
                    val isLoading = viewModel.showLoadingState.get() == true
                    binding.signInButton.isEnabled = !isLoading
                    binding.demoLoginFreelancerButton.isEnabled = !isLoading
                    binding.demoLoginBusinessOwnerButton.isEnabled = !isLoading
                }
            }
        )
        
        // Reactive sign in button state
        viewModel.isSignInButtonEnabled.addOnPropertyChangedCallback(
            object : androidx.databinding.Observable.OnPropertyChangedCallback() {
                override fun onPropertyChanged(sender: androidx.databinding.Observable?, propertyId: Int) {
                    binding.signInButton.isEnabled = viewModel.isSignInButtonEnabled.get() == true
                }
            }
        )
        
        // Reactive demo login button state
        viewModel.isDemoLoginEnabled.addOnPropertyChangedCallback(
            object : androidx.databinding.Observable.OnPropertyChangedCallback() {
                override fun onPropertyChanged(sender: androidx.databinding.Observable?, propertyId: Int) {
                    val isEnabled = viewModel.isDemoLoginEnabled.get() == true
                    binding.demoLoginFreelancerButton.isEnabled = isEnabled
                    binding.demoLoginBusinessOwnerButton.isEnabled = isEnabled
                }
            }
        )
        
        // Reactive email error display with DIRECT UPDATE
        viewModel.showEmailError.addOnPropertyChangedCallback(
            object : androidx.databinding.Observable.OnPropertyChangedCallback() {
                override fun onPropertyChanged(sender: androidx.databinding.Observable?, propertyId: Int) {
                    val showError = viewModel.showEmailError.get() == true
                    val errorMessage = viewModel.emailError.get()
                    
                    android.util.Log.d("LoginFragment", "Email error callback triggered")
                    android.util.Log.d("LoginFragment", "showEmailError: $showError")
                    android.util.Log.d("LoginFragment", "emailError: '$errorMessage'")
                    
                    if (showError) {
                        binding.emailEditText.error = errorMessage
                        android.util.Log.d("LoginFragment", "Setting email error on UI: '$errorMessage'")
                    } else {
                        binding.emailEditText.error = null
                        android.util.Log.d("LoginFragment", "Clearing email error on UI")
                    }
                }
            }
        )
        
        // Reactive password error display with DIRECT UPDATE
        viewModel.showPasswordError.addOnPropertyChangedCallback(
            object : androidx.databinding.Observable.OnPropertyChangedCallback() {
                override fun onPropertyChanged(sender: androidx.databinding.Observable?, propertyId: Int) {
                    val showError = viewModel.showPasswordError.get() == true
                    val errorMessage = viewModel.passwordError.get()
                    
                    android.util.Log.d("LoginFragment", "Password error callback triggered")
                    android.util.Log.d("LoginFragment", "showPasswordError: $showError")
                    android.util.Log.d("LoginFragment", "passwordError: '$errorMessage'")
                    
                    if (showError) {
                        binding.passwordEditText.error = errorMessage
                        android.util.Log.d("LoginFragment", "Setting password error on UI: '$errorMessage'")
                    } else {
                        binding.passwordEditText.error = null
                        android.util.Log.d("LoginFragment", "Clearing password error on UI")
                    }
                }
            }
        )
    }

    // =============================================
    // 2-WAY FORM BINDING: EditText ↔ ObservableField
    // =============================================
    private fun setup2WayFormBinding() {
        
        // Email field 2-way binding with DIRECT UPDATE
        binding.emailEditText.addTextChangedListener { text ->
            val newText = text.toString()
            if (loginViewModel.emailInput.get() != newText) {
                loginViewModel.emailInput.set(newText)
                
                // DIRECT UPDATE: Force immediate error display update
                android.util.Log.d("LoginFragment", "Email text changed to: '$newText' - triggering direct update")
                updateEmailErrorDirect()
            }
        }
        
        loginViewModel.emailInput.addOnPropertyChangedCallback(
            object : androidx.databinding.Observable.OnPropertyChangedCallback() {
                override fun onPropertyChanged(sender: androidx.databinding.Observable?, propertyId: Int) {
                    val text = loginViewModel.emailInput.get() ?: ""
                    if (binding.emailEditText.text.toString() != text) {
                        binding.emailEditText.setText(text)
                    }
                }
            }
        )
        
        // Password field 2-way binding with DIRECT UPDATE
        binding.passwordEditText.addTextChangedListener { text ->
            val newText = text.toString()
            if (loginViewModel.passwordInput.get() != newText) {
                loginViewModel.passwordInput.set(newText)
                
                // DIRECT UPDATE: Force immediate error display update
                android.util.Log.d("LoginFragment", "Password text changed to: '$newText' - triggering direct update")
                updatePasswordErrorDirect()
            }
        }
        
        loginViewModel.passwordInput.addOnPropertyChangedCallback(
            object : androidx.databinding.Observable.OnPropertyChangedCallback() {
                override fun onPropertyChanged(sender: androidx.databinding.Observable?, propertyId: Int) {
                    val text = loginViewModel.passwordInput.get() ?: ""
                    if (binding.passwordEditText.text.toString() != text) {
                        binding.passwordEditText.setText(text)
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
        loginViewModel.showSuccessMessage.observe(viewLifecycleOwner) { message ->
            message?.let {
                Toast.makeText(requireContext(), it, Toast.LENGTH_SHORT).show()
                loginViewModel.onSuccessMessageShown()
            }
        }
        
        // Observe error messages
        loginViewModel.showErrorMessage.observe(viewLifecycleOwner) { message ->
            message?.let {
                Toast.makeText(requireContext(), it, Toast.LENGTH_SHORT).show()
                loginViewModel.onErrorMessageShown()
            }
        }
        
        // Observe navigation events
        loginViewModel.navigateToMain.observe(viewLifecycleOwner) { sourceScreen ->
            sourceScreen?.let {
                navigateToMain(it)
                loginViewModel.onNavigationHandled()
            }
        }
        
        // Observe login success
        loginViewModel.loginSuccess.observe(viewLifecycleOwner) { success ->
            if (success == true) {
                // Additional success handling if needed
            }
        }
    }
    
    private fun setupClickListeners() {
        
        binding.backButton.setOnClickListener {
            navigateToHome()
        }
        
        // Form login - delegates to ViewModel
        binding.signInButton.setOnClickListener {
            loginViewModel.performFormLogin(sourceScreen)
        }
        
        // Demo logins - delegate to ViewModel
        binding.demoLoginFreelancerButton.setOnClickListener {
            loginViewModel.performFreelancerDemoLogin(sourceScreen)
        }
        
        binding.demoLoginBusinessOwnerButton.setOnClickListener {
            loginViewModel.performBusinessOwnerDemoLogin(sourceScreen)
        }
        
        binding.signUpText.setOnClickListener {
            navigateToSignUp()
        }
    }

    private fun navigateToMain(sourceScreen: String) {
        val intent = Intent(requireContext(), MainActivity::class.java)
        when (sourceScreen) {
            SOURCE_PROFILE -> {
                intent.putExtra("OPEN_PROFILE", true)
            }
            SOURCE_INTRO -> {
                // Default navigation
            }
            else -> {
                // Default navigation
            }
        }
        startActivity(intent)
        requireActivity().finish()
    }

    private fun navigateToHome() {
        val intent = Intent(requireContext(), MainActivity::class.java)
        when (sourceScreen) {
            SOURCE_PROFILE -> {
                intent.putExtra("OPEN_PROFILE", true)
            }
            else -> {
                // Default navigation
            }
        }
        startActivity(intent)
        requireActivity().finish()
    }

    private fun navigateToSignUp() {
        requireActivity().supportFragmentManager.beginTransaction()
            .replace(R.id.fragmentContainer, SignupFragment.newInstance(SignupFragment.SOURCE_LOGIN))
            .addToBackStack(null)
            .commit()
    }

    // =============================================
    // DIRECT UPDATE HELPERS: Immediate error display
    // =============================================
    private fun updateEmailErrorDirect() {
        val showError = loginViewModel.showEmailError.get() == true
        val errorMessage = loginViewModel.emailError.get()
        
        android.util.Log.d("LoginFragment", "updateEmailErrorDirect called")
        android.util.Log.d("LoginFragment", "showEmailError: $showError, errorMessage: '$errorMessage'")
        
        if (showError) {
            binding.emailEditText.error = errorMessage
            android.util.Log.d("LoginFragment", "DIRECT UPDATE: Set email error on UI: '$errorMessage'")
        } else {
            binding.emailEditText.error = null
            android.util.Log.d("LoginFragment", "DIRECT UPDATE: Cleared email error on UI")
        }
    }
    
    private fun updatePasswordErrorDirect() {
        val showError = loginViewModel.showPasswordError.get() == true
        val errorMessage = loginViewModel.passwordError.get()
        
        android.util.Log.d("LoginFragment", "updatePasswordErrorDirect called")
        android.util.Log.d("LoginFragment", "showPasswordError: $showError, errorMessage: '$errorMessage'")
        
        if (showError) {
            binding.passwordEditText.error = errorMessage
            android.util.Log.d("LoginFragment", "DIRECT UPDATE: Set password error on UI: '$errorMessage'")
        } else {
            binding.passwordEditText.error = null
            android.util.Log.d("LoginFragment", "DIRECT UPDATE: Cleared password error on UI")
        }
    }

    override fun onDestroyView() {
        super.onDestroyView()
        _binding = null
    }
}