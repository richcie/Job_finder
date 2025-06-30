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

class SignupViewModel(application: Application) : AndroidViewModel(application) {
    
    private val authRepository = AuthRepository.getInstance(application)
    
    // =============================================
    // LIVE DATA: Lifecycle-aware reactive state
    // =============================================
    private val _isLoading = MutableLiveData<Boolean>()
    val isLoading: LiveData<Boolean> = _isLoading
    
    private val _signupSuccess = MutableLiveData<Boolean>()
    val signupSuccess: LiveData<Boolean> = _signupSuccess
    
    private val _showSuccessMessage = MutableLiveData<String?>()
    val showSuccessMessage: LiveData<String?> = _showSuccessMessage
    
    private val _showErrorMessage = MutableLiveData<String?>()
    val showErrorMessage: LiveData<String?> = _showErrorMessage
    
    private val _navigateToMain = MutableLiveData<Boolean>()
    val navigateToMain: LiveData<Boolean> = _navigateToMain
    
    // =============================================
    // OBSERVABLE FIELDS: True 2-way reactive UI
    // =============================================
    val fullNameInput = ObservableField<String>("")
    val emailInput = ObservableField<String>("")
    val passwordInput = ObservableField<String>("")
    val confirmPasswordInput = ObservableField<String>("")
    val flexibleFieldInput = ObservableField<String>("")
    val termsAccepted = ObservableField<Boolean>(false)
    val isBusinessOwner = ObservableField<Boolean>(false)
    
    // UI state observables for reactive visibility
    val showLoadingState = ObservableField<Boolean>(false)
    val isSignUpButtonEnabled = ObservableField<Boolean>(false)
    
    // Form validation observables
    val fullNameError = ObservableField<String>("")
    val emailError = ObservableField<String>("")
    val passwordError = ObservableField<String>("")
    val confirmPasswordError = ObservableField<String>("")
    val flexibleFieldError = ObservableField<String>("")
    
    val showFullNameError = ObservableField<Boolean>(false)
    val showEmailError = ObservableField<Boolean>(false)
    val showPasswordError = ObservableField<Boolean>(false)
    val showConfirmPasswordError = ObservableField<Boolean>(false)
    val showFlexibleFieldError = ObservableField<Boolean>(false)
    val showTermsError = ObservableField<Boolean>(false)
    
    // Dynamic form labels
    val flexibleFieldLabel = ObservableField<String>("Professional Role")
    val flexibleFieldHint = ObservableField<String>("Enter your professional role")
    
    init {
        setupReactiveValidation()
    }
    
    // =============================================
    // REACTIVE FORM VALIDATION
    // =============================================
    private fun setupReactiveValidation() {
        fullNameInput.addOnPropertyChangedCallback(object : androidx.databinding.Observable.OnPropertyChangedCallback() {
            override fun onPropertyChanged(sender: androidx.databinding.Observable?, propertyId: Int) {
                validateFullName()
                updateSignUpButtonState()
            }
        })
        
        emailInput.addOnPropertyChangedCallback(object : androidx.databinding.Observable.OnPropertyChangedCallback() {
            override fun onPropertyChanged(sender: androidx.databinding.Observable?, propertyId: Int) {
                validateEmail()
                updateSignUpButtonState()
            }
        })
        
        passwordInput.addOnPropertyChangedCallback(object : androidx.databinding.Observable.OnPropertyChangedCallback() {
            override fun onPropertyChanged(sender: androidx.databinding.Observable?, propertyId: Int) {
                validatePassword()
                validateConfirmPassword()
                updateSignUpButtonState()
            }
        })
        
        confirmPasswordInput.addOnPropertyChangedCallback(object : androidx.databinding.Observable.OnPropertyChangedCallback() {
            override fun onPropertyChanged(sender: androidx.databinding.Observable?, propertyId: Int) {
                validateConfirmPassword()
                updateSignUpButtonState()
            }
        })
        
        flexibleFieldInput.addOnPropertyChangedCallback(object : androidx.databinding.Observable.OnPropertyChangedCallback() {
            override fun onPropertyChanged(sender: androidx.databinding.Observable?, propertyId: Int) {
                validateFlexibleField()
                updateSignUpButtonState()
            }
        })
        
        termsAccepted.addOnPropertyChangedCallback(object : androidx.databinding.Observable.OnPropertyChangedCallback() {
            override fun onPropertyChanged(sender: androidx.databinding.Observable?, propertyId: Int) {
                updateSignUpButtonState()
            }
        })
        
        isBusinessOwner.addOnPropertyChangedCallback(object : androidx.databinding.Observable.OnPropertyChangedCallback() {
            override fun onPropertyChanged(sender: androidx.databinding.Observable?, propertyId: Int) {
                updateFormFieldsBasedOnRole()
                validateFlexibleField()
                updateSignUpButtonState()
            }
        })
    }
    
    private fun validateFullName(): Boolean {
        val name = fullNameInput.get() ?: ""
        val isValid = when {
            name.trim().isEmpty() -> {
                fullNameError.set("Full name is required")
                showFullNameError.set(true)
                false
            }
            name.trim().length < 2 -> {
                fullNameError.set("Full name must be at least 2 characters")
                showFullNameError.set(true)
                false
            }
            else -> {
                fullNameError.set("")
                showFullNameError.set(false)
                true
            }
        }
        return isValid
    }
    
    private fun validateEmail(): Boolean {
        val email = emailInput.get() ?: ""
        val isValid = when {
            email.trim().isEmpty() -> {
                emailError.set("Email is required")
                showEmailError.set(true)
                false
            }
            !android.util.Patterns.EMAIL_ADDRESS.matcher(email).matches() -> {
                emailError.set("Please enter a valid email address")
                showEmailError.set(true)
                false
            }
            else -> {
                emailError.set("")
                showEmailError.set(false)
                true
            }
        }
        return isValid
    }
    
    private fun validatePassword(): Boolean {
        val password = passwordInput.get() ?: ""
        
        // Debug logging to see what's happening
        android.util.Log.d("SignupValidation", "validatePassword called")
        android.util.Log.d("SignupValidation", "password: '$password'")
        android.util.Log.d("SignupValidation", "password length: ${password.length}")
        
        val isValid = when {
            password.isEmpty() -> {
                passwordError.set("Password is required")
                showPasswordError.set(true)
                android.util.Log.d("SignupValidation", "Setting password error: Password is required")
                false
            }
            password.length < 8 -> {
                passwordError.set("Password must be at least 8 characters")
                showPasswordError.set(true)
                android.util.Log.d("SignupValidation", "Setting password error: Password must be at least 8 characters")
                false
            }
            else -> {
                passwordError.set("")
                showPasswordError.set(false)
                android.util.Log.d("SignupValidation", "Password valid - clearing error")
                true
            }
        }
        
        android.util.Log.d("SignupValidation", "showPasswordError.get(): ${showPasswordError.get()}")
        return isValid
    }
    
    private fun validateConfirmPassword(): Boolean {
        val password = passwordInput.get() ?: ""
        val confirmPassword = confirmPasswordInput.get() ?: ""
        
        // Debug logging to see what's happening
        android.util.Log.d("SignupValidation", "validateConfirmPassword called")
        android.util.Log.d("SignupValidation", "password: '$password'")
        android.util.Log.d("SignupValidation", "confirmPassword: '$confirmPassword'")
        android.util.Log.d("SignupValidation", "passwords match: ${password == confirmPassword}")
        
        val isValid = when {
            confirmPassword.isEmpty() -> {
                confirmPasswordError.set("Please confirm your password")
                showConfirmPasswordError.set(true)
                android.util.Log.d("SignupValidation", "Setting error: Please confirm your password")
                false
            }
            password != confirmPassword -> {
                confirmPasswordError.set("Passwords do not match")
                showConfirmPasswordError.set(true)
                android.util.Log.d("SignupValidation", "Setting error: Passwords do not match")
                false
            }
            else -> {
                confirmPasswordError.set("")
                showConfirmPasswordError.set(false)
                android.util.Log.d("SignupValidation", "Passwords match - clearing error")
                true
            }
        }
        
        android.util.Log.d("SignupValidation", "showConfirmPasswordError.get(): ${showConfirmPasswordError.get()}")
        return isValid
    }
    
    private fun validateFlexibleField(): Boolean {
        val value = flexibleFieldInput.get() ?: ""
        val fieldName = if (isBusinessOwner.get() == true) "company name" else "professional role"
        val isValid = when {
            value.trim().isEmpty() -> {
                flexibleFieldError.set("Please enter your $fieldName")
                showFlexibleFieldError.set(true)
                false
            }
            value.trim().length < 2 -> {
                flexibleFieldError.set("${fieldName.replaceFirstChar { it.uppercase() }} must be at least 2 characters")
                showFlexibleFieldError.set(true)
                false
            }
            else -> {
                flexibleFieldError.set("")
                showFlexibleFieldError.set(false)
                true
            }
        }
        return isValid
    }
    
    private fun updateFormFieldsBasedOnRole() {
        if (isBusinessOwner.get() == true) {
            flexibleFieldLabel.set("Company Name")
            flexibleFieldHint.set("Enter your company name")
        } else {
            flexibleFieldLabel.set("Professional Role")
            flexibleFieldHint.set("Enter your professional role")
        }
    }
    
    private fun updateSignUpButtonState() {
        val allFieldsFilled = !fullNameInput.get().isNullOrEmpty() &&
                !emailInput.get().isNullOrEmpty() &&
                !passwordInput.get().isNullOrEmpty() &&
                !confirmPasswordInput.get().isNullOrEmpty() &&
                !flexibleFieldInput.get().isNullOrEmpty() &&
                termsAccepted.get() == true
        
        val isEnabled = allFieldsFilled && !showLoadingState.get()!!
        isSignUpButtonEnabled.set(isEnabled)
    }
    
    // =============================================
    // REACTIVE BUSINESS LOGIC
    // =============================================
    fun performSignUp() {
        // Trigger validation and show any remaining errors when user submits
        showFullNameError.set(!validateFullName())
        showEmailError.set(!validateEmail())
        showPasswordError.set(!validatePassword())
        showConfirmPasswordError.set(!validateConfirmPassword())
        showFlexibleFieldError.set(!validateFlexibleField())
        
        val termsAcceptedValue = termsAccepted.get() ?: false
        val shouldShowTermsError = !termsAcceptedValue
        android.util.Log.d("SignupValidation", "Terms validation - termsAccepted: $termsAcceptedValue, shouldShowTermsError: $shouldShowTermsError")
        
        showTermsError.set(shouldShowTermsError)
        android.util.Log.d("SignupValidation", "After setting showTermsError - showTermsError.get(): ${showTermsError.get()}")
        
        if (!validateFullName() || !validateEmail() || !validatePassword() || 
            !validateConfirmPassword() || !validateFlexibleField() || termsAccepted.get() != true) {
            android.util.Log.d("SignupValidation", "Form validation failed - stopping submission")
            return
        }
        
        _isLoading.value = true
        showLoadingState.set(true)
        isSignUpButtonEnabled.set(false)
        
        viewModelScope.launch {
            try {
                val fullName = fullNameInput.get() ?: ""
                val email = emailInput.get() ?: ""
                val password = passwordInput.get() ?: ""
                val businessOwner = isBusinessOwner.get() ?: false
                
                // Create username from email
                val username = email.substringBefore("@").replace(".", "_")
                val userRole = if (businessOwner) UserSession.ROLE_BUSINESS_OWNER else UserSession.ROLE_FREELANCER
                
                // Debug logging for role selection
                android.util.Log.d("SignupViewModel", "📝 Sign-up form data:")
                android.util.Log.d("SignupViewModel", "   Switch position (isBusinessOwner): $businessOwner")
                android.util.Log.d("SignupViewModel", "   Selected app role: $userRole")
                android.util.Log.d("SignupViewModel", "   Display name: ${if (businessOwner) "Business Owner" else "Freelancer"}")
                
                // Split full name into first and last name
                val nameParts = fullName.trim().split(" ", limit = 2)
                val firstName = nameParts.getOrNull(0)
                val lastName = nameParts.getOrNull(1)
                
                // Get the flexible field value (either professional role or company name)
                val flexibleFieldValue = flexibleFieldInput.get()?.trim()
                
                android.util.Log.d("SignupViewModel", "📤 Sending registration request:")
                android.util.Log.d("SignupViewModel", "   firstName: $firstName")
                android.util.Log.d("SignupViewModel", "   lastName: $lastName")
                android.util.Log.d("SignupViewModel", "   role: $userRole")
                android.util.Log.d("SignupViewModel", "   flexibleField: $flexibleFieldValue")
                
                // Call backend API for registration with role-specific fields
                when (val result = authRepository.register(
                    username = username,
                    email = email,
                    password = password,
                    firstName = firstName,
                    lastName = lastName,
                    role = userRole,
                    professionalRole = if (businessOwner) null else flexibleFieldValue,
                    companyName = if (businessOwner) flexibleFieldValue else null
                )) {
                    is AuthResult.Success -> {
                        _signupSuccess.value = true
                        _showSuccessMessage.value = result.message.ifEmpty { "Account created successfully!" }
                        _navigateToMain.value = true
                    }
                    is AuthResult.Error -> {
                        _showErrorMessage.value = result.message
                    }
                }
                
            } catch (e: Exception) {
                _showErrorMessage.value = "Error creating account: ${e.message}"
            } finally {
                _isLoading.value = false
                showLoadingState.set(false)
                isSignUpButtonEnabled.set(true)
            }
        }
    }
    
    fun toggleRole(isBusinessOwner: Boolean) {
        this.isBusinessOwner.set(isBusinessOwner)
    }
    
    // For debugging - manually trigger confirm password validation
    fun testConfirmPasswordValidation() {
        android.util.Log.d("SignupValidation", "Manual validation trigger")
        android.util.Log.d("SignupValidation", "Before validation - showConfirmPasswordError: ${showConfirmPasswordError.get()}")
        validateConfirmPassword()
        android.util.Log.d("SignupValidation", "After validation - showConfirmPasswordError: ${showConfirmPasswordError.get()}")
        android.util.Log.d("SignupValidation", "After validation - confirmPasswordError: '${confirmPasswordError.get()}'")
    }
    
    fun onSuccessMessageShown() { _showSuccessMessage.value = null }
    fun onErrorMessageShown() { _showErrorMessage.value = null }
    fun onNavigationHandled() { _navigateToMain.value = false }
} 