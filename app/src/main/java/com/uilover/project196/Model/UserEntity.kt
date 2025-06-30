package com.uilover.project196.Model

import androidx.room.Entity
import androidx.room.PrimaryKey

@Entity(tableName = "users")
data class UserEntity(
    @PrimaryKey
    val userId: String,
    val name: String,
    val email: String,
    val role: String,
    val skills: String = "",
    val title: String = "",
    val experience: String = "",
    val rating: Float = 0.0f,
    val totalReviews: Int = 0,
    val completedProjects: Int = 0,
    val hourlyRate: String = "",
    val availability: String = "",
    val location: String = "",
    val bio: String = "",
    val companyName: String = "",
    val isActive: Boolean = true,
    val createdAt: Long = System.currentTimeMillis()
)