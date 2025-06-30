package com.uilover.project196.Network

import com.uilover.project196.Model.ApiResponse
import com.uilover.project196.Model.LoginRequest
import com.uilover.project196.Model.LoginResponse
import com.uilover.project196.Model.RegisterRequest
import com.uilover.project196.Model.UserPublic
import retrofit2.Response
import retrofit2.http.Body
import retrofit2.http.POST

interface AuthApiService {
    
    @POST("api/v1/auth/login")
    suspend fun login(@Body request: LoginRequest): Response<ApiResponse<LoginResponse>>
    
    @POST("api/v1/auth/register")
    suspend fun register(@Body request: RegisterRequest): Response<ApiResponse<LoginResponse>>
    
    @POST("api/v1/auth/logout")
    suspend fun logout(): Response<ApiResponse<Unit>>
} 