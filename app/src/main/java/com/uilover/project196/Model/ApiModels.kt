package com.uilover.project196.Model

import com.google.gson.annotations.SerializedName

// =============================================
// REQUEST MODELS
// =============================================

data class LoginRequest(
    @SerializedName("username_or_email")
    val usernameOrEmail: String,
    
    @SerializedName("password")
    val password: String
)

data class RegisterRequest(
    @SerializedName("username")
    val username: String,
    
    @SerializedName("email")
    val email: String,
    
    @SerializedName("password")
    val password: String,
    
    @SerializedName("first_name")
    val firstName: String?,
    
    @SerializedName("last_name")
    val lastName: String?,
    
    @SerializedName("phone")
    val phone: String?,
    
    @SerializedName("role")
    val role: String?,
    
    @SerializedName("professional_role")
    val professionalRole: String?,
    
    @SerializedName("company_name")
    val companyName: String?
)

// =============================================
// RESPONSE MODELS
// =============================================

data class ApiResponse<T>(
    @SerializedName("success")
    val success: Boolean,
    
    @SerializedName("message")
    val message: String,
    
    @SerializedName("data")
    val data: T?
)

data class UserPublic(
    @SerializedName("id")
    val id: Int,
    
    @SerializedName("username")
    val username: String,
    
    @SerializedName("email")
    val email: String,
    
    @SerializedName("first_name")
    val firstName: String?,
    
    @SerializedName("last_name")
    val lastName: String?,
    
    @SerializedName("phone")
    val phone: String?,
    
    @SerializedName("role")
    val role: String,
    
    @SerializedName("professional_role")
    val professionalRole: String?,
    
    @SerializedName("company_name")
    val companyName: String?,
    
    @SerializedName("is_active")
    val isActive: Boolean,
    
    @SerializedName("email_verified")
    val emailVerified: Boolean,
    
    @SerializedName("created_at")
    val createdAt: String
)

data class LoginResponse(
    @SerializedName("access_token")
    val accessToken: String,
    
    @SerializedName("token_type")
    val tokenType: String,
    
    @SerializedName("expires_in")
    val expiresIn: Long,
    
    @SerializedName("user")
    val user: UserPublic
)

// =============================================
// ERROR MODELS
// =============================================

data class ApiError(
    val message: String,
    val code: Int
) 