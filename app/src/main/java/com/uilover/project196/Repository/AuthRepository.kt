package com.uilover.project196.Repository

import android.content.Context
import android.util.Log
import com.uilover.project196.Model.*
import com.uilover.project196.Network.ApiClient
import com.uilover.project196.Utils.UserSession
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext

sealed class AuthResult<out T> {
    data class Success<T>(val data: T, val message: String = "") : AuthResult<T>()
    data class Error(val message: String, val code: Int = 0) : AuthResult<Nothing>()
}

class AuthRepository private constructor(private val context: Context) {
    
    companion object {
        @Volatile
        private var INSTANCE: AuthRepository? = null
        
        fun getInstance(context: Context): AuthRepository {
            return INSTANCE ?: synchronized(this) {
                val instance = AuthRepository(context.applicationContext)
                INSTANCE = instance
                instance
            }
        }
    }
    
    private val authService = ApiClient.authService
    private val userRepository = UserRepository.getInstance(context)
    
    suspend fun login(email: String, password: String): AuthResult<LoginResponse> {
        return withContext(Dispatchers.IO) {
            try {
                Log.d("AuthRepository", "Attempting login for email: $email")
                
                val request = LoginRequest(
                    usernameOrEmail = email,
                    password = password
                )
                
                val response = authService.login(request)
                
                if (response.isSuccessful) {
                    val body = response.body()
                    if (body != null && body.success && body.data != null) {
                        Log.d("AuthRepository", "Login successful for user: ${body.data.user.username}")
                        
                        val user = body.data.user
                        val userRole = mapBackendRoleToAppRole(user.role)
                        val displayName = user.firstName?.takeIf { it.isNotBlank() } ?: user.username
                        
                        UserSession.login(
                            userId = user.id.toString(),
                            userName = displayName,
                            userEmail = user.email,
                            userRole = userRole,
                            context = context
                        )
                        
                        // Ensure user exists in local database for compatibility
                        userRepository.ensureUserInDatabase(
                            userId = user.id.toString(),
                            name = displayName,
                            email = user.email,
                            role = userRole,
                            professionalRole = user.professionalRole,
                            companyName = user.companyName
                        )
                        
                        AuthResult.Success(body.data, body.message)
                    } else {
                        Log.e("AuthRepository", "Login failed: ${body?.message ?: "Unknown error"}")
                        AuthResult.Error(body?.message ?: "Login failed")
                    }
                } else {
                    val errorMessage = parseErrorMessage(response)
                    Log.e("AuthRepository", "Login HTTP error: $errorMessage")
                    AuthResult.Error(errorMessage, response.code())
                }
            } catch (e: Exception) {
                Log.e("AuthRepository", "Login exception", e)
                AuthResult.Error("Network error: ${e.message}")
            }
        }
    }
    
    suspend fun register(
        username: String,
        email: String,
        password: String,
        firstName: String?,
        lastName: String?,
        role: String?,
        professionalRole: String? = null,
        companyName: String? = null
    ): AuthResult<LoginResponse> {
        return withContext(Dispatchers.IO) {
            try {
                Log.d("AuthRepository", "Attempting registration for email: $email")
                
                val backendRole = mapAppRoleToBackendRole(role)
                val finalProfessionalRole = if (backendRole == "job_seeker") professionalRole else null
                val finalCompanyName = if (backendRole == "employer") companyName else null
                
                Log.d("AuthRepository", "📋 Profile fields for backend:")
                Log.d("AuthRepository", "   Backend role: $backendRole")
                Log.d("AuthRepository", "   Professional role: $finalProfessionalRole")
                Log.d("AuthRepository", "   Company name: $finalCompanyName")
                
                val request = RegisterRequest(
                    username = username,
                    email = email,
                    password = password,
                    firstName = firstName,
                    lastName = lastName,
                    phone = null,
                    role = backendRole,
                    professionalRole = finalProfessionalRole,
                    companyName = finalCompanyName
                )
                
                val response = authService.register(request)
                
                if (response.isSuccessful) {
                    val body = response.body()
                    if (body != null && body.success && body.data != null) {
                        val gson = com.google.gson.Gson()
                        val responseJson = gson.toJson(body)
                        Log.d("AuthRepository", "📋 Full registration response: $responseJson")
                        
                        val (user, loginResponse) = try {
                            val userData = body.data.user
                            val responseData = body.data
                            Log.d("AuthRepository", "Registration successful for user: ${userData.username} (LoginResponse structure)")
                            Pair(userData, responseData)
                        } catch (e: Exception) {
                            Log.w("AuthRepository", "LoginResponse structure not found, trying UserPublic directly")
                            
                            try {
                                val userJson = gson.toJson(body.data)
                                val userData = gson.fromJson(userJson, UserPublic::class.java)
                                
                                if (userData != null) {
                                    // Create a mock LoginResponse for compatibility
                                    val responseData = LoginResponse(
                                        accessToken = "",
                                        tokenType = "Bearer",
                                        expiresIn = 3600,
                                        user = userData
                                    )
                                    Log.d("AuthRepository", "Registration successful for user: ${userData.username} (UserPublic structure)")
                                    Pair(userData, responseData)
                                } else {
                                    Log.e("AuthRepository", "UserPublic data is null after parsing")
                                    return@withContext AuthResult.Error("Failed to parse user data from registration response")
                                }
                            } catch (parseException: Exception) {
                                Log.e("AuthRepository", "Failed to parse UserPublic from response", parseException)
                                return@withContext AuthResult.Error("Failed to parse registration response: ${parseException.message}")
                            }
                        }
                        
                        val userRole = mapBackendRoleToAppRole(user.role)
                        
                        // Use firstName if available, otherwise fallback to username
                        val displayName = user.firstName?.takeIf { it.isNotBlank() } ?: user.username
                        
                        UserSession.login(
                            userId = user.id.toString(),
                            userName = displayName,
                            userEmail = user.email,
                            userRole = userRole,
                            context = context
                        )
                        
                        // Ensure user exists in local database for compatibility
                        userRepository.ensureUserInDatabase(
                            userId = user.id.toString(),
                            name = displayName,
                            email = user.email,
                            role = userRole,
                            professionalRole = user.professionalRole,
                            companyName = user.companyName
                        )
                        
                        AuthResult.Success(loginResponse, body.message)
                    } else {
                        Log.e("AuthRepository", "Registration failed: ${body?.message ?: "Unknown error"}")
                        AuthResult.Error(body?.message ?: "Registration failed")
                    }
                } else {
                    val errorMessage = parseErrorMessage(response)
                    Log.e("AuthRepository", "Registration HTTP error: $errorMessage")
                    AuthResult.Error(errorMessage, response.code())
                }
            } catch (e: Exception) {
                Log.e("AuthRepository", "Registration exception", e)
                AuthResult.Error("Network error: ${e.message}")
            }
        }
    }
    
    suspend fun logout(): AuthResult<Unit> {
        return withContext(Dispatchers.IO) {
            try {
                Log.d("AuthRepository", "Attempting logout")
                
                // Try to call backend logout (optional, as we might not have valid token)
                try {
                    authService.logout()
                } catch (e: Exception) {
                    Log.w("AuthRepository", "Backend logout failed, proceeding with local logout", e)
                }
                
                // Always clear local session
                UserSession.logout()
                Log.d("AuthRepository", "Local logout completed")
                
                AuthResult.Success(Unit, "Logged out successfully")
            } catch (e: Exception) {
                Log.e("AuthRepository", "Logout exception", e)
                // Even if there's an error, clear local session
                UserSession.logout()
                AuthResult.Error("Logout error: ${e.message}")
            }
        }
    }
    
    private fun mapAppRoleToBackendRole(appRole: String?): String {
        val backendRole = when (appRole) {
            UserSession.ROLE_FREELANCER -> "job_seeker"
            UserSession.ROLE_BUSINESS_OWNER -> "employer"
            else -> "job_seeker" // Default
        }
        Log.d("AuthRepository", "🔄 Role mapping: App role '$appRole' → Backend role '$backendRole'")
        return backendRole
    }
    
    private fun mapBackendRoleToAppRole(backendRole: String?): String {
        val appRole = when (backendRole) {
            "job_seeker" -> UserSession.ROLE_FREELANCER
            "employer" -> UserSession.ROLE_BUSINESS_OWNER
            "admin" -> UserSession.ROLE_BUSINESS_OWNER // Treat admin as business owner for now
            null -> {
                Log.w("AuthRepository", "Backend role is null, defaulting to FREELANCER")
                UserSession.ROLE_FREELANCER
            }
            else -> {
                Log.w("AuthRepository", "Unknown backend role '$backendRole', defaulting to FREELANCER")
                UserSession.ROLE_FREELANCER
            }
        }
        Log.d("AuthRepository", "🔄 Role mapping: Backend role '$backendRole' → App role '$appRole'")
        return appRole
    }
    
    private fun parseErrorMessage(response: retrofit2.Response<*>): String {
        return try {
            val errorBody = response.errorBody()?.string()
            if (errorBody != null && errorBody.isNotEmpty()) {
                // Try to parse error body as JSON
                val gson = com.google.gson.Gson()
                val apiResponse = gson.fromJson(errorBody, ApiResponse::class.java)
                val originalMessage = apiResponse.message
                
                // Convert "Invalid credentials" to "Email/Password is invalid"
                if (originalMessage == "Invalid credentials") {
                    "Email/Password is invalid"
                } else {
                    originalMessage
                }
            } else {
                "HTTP ${response.code()}: ${response.message()}"
            }
        } catch (e: Exception) {
            "HTTP ${response.code()}: ${response.message()}"
        }
    }
} 