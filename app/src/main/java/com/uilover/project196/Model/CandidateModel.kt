package com.uilover.project196.Model

import java.text.SimpleDateFormat
import java.util.*

data class CandidateModel(
    val userId: String,
    val jobId: Int,
    val applicationId: Int,
    val name: String,
    val email: String,
    val title: String,
    val experience: String,
    val skills: List<String>,
    val rating: Float,
    val totalReviews: Int,
    val completedProjects: Int,
    val hourlyRate: String,
    val availability: String,
    val location: String,
    val bio: String,
    val appliedDate: Long,
    val status: String,
    val coverLetter: String = "",
    val proposedRate: String = "",
    val description: String = ""
) {
    fun getFormattedAppliedDate(): String {
        val sdf = SimpleDateFormat("MMM dd, yyyy", Locale.getDefault())
        return sdf.format(Date(appliedDate))
    }

    fun getRelativeAppliedDate(): String {
        val now = System.currentTimeMillis()
        val diff = now - appliedDate

        return when {
            diff < 60 * 1000 -> "Just now"
            diff < 60 * 60 * 1000 -> "${diff / (60 * 1000)} minutes ago"
            diff < 24 * 60 * 60 * 1000 -> "${diff / (60 * 60 * 1000)} hours ago"
            diff < 7 * 24 * 60 * 60 * 1000 -> "${diff / (24 * 60 * 60 * 1000)} days ago"
            else -> getFormattedAppliedDate()
        }
    }

    fun getStatusDisplayName(): String {
        return when (status) {
            "pending" -> "Pending Review"
            "shortlisted" -> "Shortlisted"
            "rejected" -> "Rejected"
            "hired" -> "Hired"
            else -> "Unknown"
        }
    }

    fun getStatusColor(): String {
        return when (status) {
            "pending" -> "#FFA500"
            "shortlisted" -> "#4CAF50"
            "rejected" -> "#F44336"
            "hired" -> "#2196F3"
            else -> "#757575"
        }
    }

    companion object {
        fun fromUserAndApplication(user: UserEntity, application: JobApplicationEntity): CandidateModel {
            return CandidateModel(
                userId = user.userId,
                jobId = application.jobId,
                applicationId = application.id,
                name = user.name,
                email = user.email,
                title = user.title,
                experience = user.experience,
                skills = if (application.skills.isNotEmpty()) {

                    application.skills.split(",").map { it.trim() }
                } else {
                    emptyList()
                },
                rating = user.rating,
                totalReviews = user.totalReviews,
                completedProjects = user.completedProjects,
                hourlyRate = user.hourlyRate,
                availability = user.availability,
                location = user.location,
                bio = user.bio,
                appliedDate = application.appliedAt,
                status = application.status,
                coverLetter = application.coverLetter,
                proposedRate = application.proposedRate,
                description = application.description
            )
        }
    }
}