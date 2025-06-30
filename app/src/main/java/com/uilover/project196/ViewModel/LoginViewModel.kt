package com.uilover.project196.ViewModel

import android.app.Application
import androidx.databinding.ObservableField
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.LiveData
import androidx.lifecycle.MutableLiveData
import androidx.lifecycle.viewModelScope
import com.uilover.project196.Repository.AuthRepository
import com.uilover.project196.Repository.AuthResult
import com.uilover.project196.Utils.UserSession
import kotlinx.coroutines.launch

class LoginViewModel(application: Application) : AndroidViewModel(application) {
    
    private val authRepository = AuthRepository.getInstance(application)
    
    // =============================================
    // LIVE DATA: Lifecycle-aware reactive state
    // =============================================
    private val _isLoading = MutableLiveData<Boolean>()
    val isLoading: LiveData<Boolean> = _isLoading
    
    private val _loginSuccess = MutableLiveData<Boolean>()
    val loginSuccess: LiveData<Boolean> = _loginSuccess
    
    private val _showSuccessMessage = MutableLiveData<String?>()
    val showSuccessMessage: LiveData<String?> = _showSuccessMessage
    
    private val _showErrorMessage = MutableLiveData<String?>()
    val showErrorMessage: LiveData<String?> = _showErrorMessage
    
    private val _navigateToMain = MutableLiveData<String?>() // Contains source screen
    val navigateToMain: LiveData<String?> = _navigateToMain
    
    // =============================================
    // OBSERVABLE FIELDS: True 2-way reactive UI
    // =============================================
    val emailInput = ObservableField<String>("")
    val passwordInput = ObservableField<String>("")
    
    // UI state observables for reactive visibility
    val showLoadingState = ObservableField<Boolean>(false)
    val showFormState = ObservableField<Boolean>(true)
    val isSignInButtonEnabled = ObservableField<Boolean>(false)
    
    // Form validation observables
    val emailError = ObservableField<String>("")
    val passwordError = ObservableField<String>("")
    val showEmailError = ObservableField<Boolean>(false)
    val showPasswordError = ObservableField<Boolean>(false)
    
    // Demo login states
    val isDemoLoginEnabled = ObservableField<Boolean>(true)
    
    init {
        // Setup reactive form validation
        setupReactiveValidation()
    }
    
    // =============================================
    // REACTIVE FORM VALIDATION
    // =============================================
    private fun setupReactiveValidation() {
        // Email validation callback
        emailInput.addOnPropertyChangedCallback(object : androidx.databinding.Observable.OnPropertyChangedCallback() {
            override fun onPropertyChanged(sender: androidx.databinding.Observable?, propertyId: Int) {
                validateEmail()
                updateSignInButtonState()
            }
        })
        
        // Password validation callback
        passwordInput.addOnPropertyChangedCallback(object : androidx.databinding.Observable.OnPropertyChangedCallback() {
            override fun onPropertyChanged(sender: androidx.databinding.Observable?, propertyId: Int) {
                validatePassword()
                updateSignInButtonState()
            }
        })
    }
    
    private fun validateEmail(): Boolean {
        val email = emailInput.get() ?: ""
        
        // Debug logging for development
        android.util.Log.d("LoginValidation", "validateEmail called")
        android.util.Log.d("LoginValidation", "email: '$email'")
        android.util.Log.d("LoginValidation", "email length: ${email.length}")
        
        val isValid = when {
            email.trim().isEmpty() -> {
                emailError.set("Email is required")
                showEmailError.set(true)
                android.util.Log.d("LoginValidation", "Setting email error: Email is required")
                false
            }
            !android.util.Patterns.EMAIL_ADDRESS.matcher(email.trim()).matches() -> {
                emailError.set("Please enter a valid email address")
                showEmailError.set(true)
                android.util.Log.d("LoginValidation", "Setting email error: Invalid email format")
                false
            }
            else -> {
                emailError.set("")
                showEmailError.set(false)
                android.util.Log.d("LoginValidation", "Email valid - clearing error")
                true
            }
        }
        
        android.util.Log.d("LoginValidation", "showEmailError.get(): ${showEmailError.get()}")
        return isValid
    }

    private fun validatePassword(): Boolean {
        val password = passwordInput.get() ?: ""
        
        // Debug logging for development
        android.util.Log.d("LoginValidation", "validatePassword called")
        android.util.Log.d("LoginValidation", "password: '$password'")
        android.util.Log.d("LoginValidation", "password length: ${password.length}")
        
        val isValid = when {
            password.isEmpty() -> {
                passwordError.set("Password is required")
                showPasswordError.set(true)
                android.util.Log.d("LoginValidation", "Setting password error: Password is required")
                false
            }
            password.length < 8 -> {
                passwordError.set("Password must be at least 8 characters")
                showPasswordError.set(true)
                android.util.Log.d("LoginValidation", "Setting password error: Password must be at least 8 characters")
                false
            }
            else -> {
                passwordError.set("")
                showPasswordError.set(false)
                android.util.Log.d("LoginValidation", "Password valid - clearing error")
                true
            }
        }
        
        android.util.Log.d("LoginValidation", "showPasswordError.get(): ${showPasswordError.get()}")
        return isValid
    }
    
    private fun updateSignInButtonState() {
        // More professional validation: check both field content AND validation state
        val email = emailInput.get() ?: ""
        val password = passwordInput.get() ?: ""
        
        val allFieldsFilled = email.trim().isNotEmpty() && password.isNotEmpty()
        val noValidationErrors = !showEmailError.get()!! && !showPasswordError.get()!!
        val notLoading = !showLoadingState.get()!!
        
        val isEnabled = allFieldsFilled && noValidationErrors && notLoading
        
        android.util.Log.d("LoginValidation", "updateSignInButtonState:")
        android.util.Log.d("LoginValidation", "  allFieldsFilled: $allFieldsFilled")
        android.util.Log.d("LoginValidation", "  noValidationErrors: $noValidationErrors")
        android.util.Log.d("LoginValidation", "  notLoading: $notLoading")
        android.util.Log.d("LoginValidation", "  isEnabled: $isEnabled")
        
        isSignInButtonEnabled.set(isEnabled)
    }
    
    // =============================================
    // REACTIVE BUSINESS LOGIC
    // =============================================
    fun performFormLogin(sourceScreen: String) {
        // Trigger validation and show any remaining errors when user submits
        android.util.Log.d("LoginValidation", "performFormLogin called - triggering validation")
        
        val emailValid = validateEmail()
        val passwordValid = validatePassword()
        
        // Force show errors on submission attempt
        showEmailError.set(!emailValid)
        showPasswordError.set(!passwordValid)
        
        android.util.Log.d("LoginValidation", "Form validation results:")
        android.util.Log.d("LoginValidation", "  emailValid: $emailValid")
        android.util.Log.d("LoginValidation", "  passwordValid: $passwordValid")
        
        if (!emailValid || !passwordValid) {
            android.util.Log.d("LoginValidation", "Form validation failed - stopping submission")
            return
        }
        
        _isLoading.value = true
        showLoadingState.set(true)
        isSignInButtonEnabled.set(false)
        isDemoLoginEnabled.set(false)
        
        viewModelScope.launch {
            try {
                val email = emailInput.get() ?: ""
                val password = passwordInput.get() ?: ""
                
                // Call backend API for authentication
                when (val result = authRepository.login(email, password)) {
                    is AuthResult.Success -> {
                        _loginSuccess.value = true
                        _showSuccessMessage.value = result.message.ifEmpty { "Welcome back, ${result.data.user.username}!" }
                        _navigateToMain.value = sourceScreen
                    }
                    is AuthResult.Error -> {
                        _showErrorMessage.value = result.message
                    }
                }
            } catch (e: Exception) {
                _showErrorMessage.value = "Login failed: ${e.message}"
            } finally {
                _isLoading.value = false
                showLoadingState.set(false)
                isSignInButtonEnabled.set(true)
                isDemoLoginEnabled.set(true)
            }
        }
    }
    
    fun performFreelancerDemoLogin(sourceScreen: String) {
        _isLoading.value = true
        showLoadingState.set(true)
        isDemoLoginEnabled.set(false)
        
        viewModelScope.launch {
            try {
                UserSession.simulateFreelancerLogin()
                _loginSuccess.value = true
                _showSuccessMessage.value = "Welcome, ${UserSession.getUserName()}! (${UserSession.getRoleDisplayName()})"
                _navigateToMain.value = sourceScreen
            } catch (e: Exception) {
                _showErrorMessage.value = "Demo login failed: ${e.message}"
            } finally {
                _isLoading.value = false
                showLoadingState.set(false)
                isDemoLoginEnabled.set(true)
            }
        }
    }
    
    fun performBusinessOwnerDemoLogin(sourceScreen: String) {
        _isLoading.value = true
        showLoadingState.set(true)
        isDemoLoginEnabled.set(false)
        
        viewModelScope.launch {
            try {
                UserSession.simulateBusinessOwnerLogin()
                _loginSuccess.value = true
                _showSuccessMessage.value = "Welcome, ${UserSession.getUserName()}! (${UserSession.getRoleDisplayName()})"
                _navigateToMain.value = sourceScreen
            } catch (e: Exception) {
                _showErrorMessage.value = "Demo login failed: ${e.message}"
            } finally {
                _isLoading.value = false
                showLoadingState.set(false)
                isDemoLoginEnabled.set(true)
            }
        }
    }
    
    fun clearForm() {
        emailInput.set("")
        passwordInput.set("")
        emailError.set("")
        passwordError.set("")
        showEmailError.set(false)
        showPasswordError.set(false)
    }
    
    // Message handling
    fun onSuccessMessageShown() { _showSuccessMessage.value = null }
    fun onErrorMessageShown() { _showErrorMessage.value = null }
    fun onNavigationHandled() { _navigateToMain.value = null }
} 