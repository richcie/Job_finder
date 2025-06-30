package com.uilover.project196.Model

import java.text.SimpleDateFormat
import java.util.*

data class CandidateProgressModel(
    val candidateId: String,
    val candidateName: String,
    val candidateEmail: String,
    val candidateRole: String,
    val jobId: Int,
    val jobTitle: String,
    val applicationId: Int,
    val hiredDate: Long,
    val status: String,
    val totalWorkDays: Int,
    val attendedDays: Int,
    val lastCheckIn: Long?,
    val lastCheckOut: Long?,
    val currentStatus: String,
    val averageHoursPerDay: Double,
    val totalHours: Double,
    val completionRate: Double,
    val lastProgressReport: String?
) {
    fun getFormattedHiredDate(): String {
        val sdf = SimpleDateFormat("MMM dd, yyyy", Locale.getDefault())
        return sdf.format(Date(hiredDate))
    }

    fun getFormattedLastCheckIn(): String? {
        return lastCheckIn?.let {
            val sdf = SimpleDateFormat("MMM dd, HH:mm", Locale.getDefault())
            sdf.format(Date(it))
        }
    }

    fun getFormattedLastCheckOut(): String? {
        return lastCheckOut?.let {
            val sdf = SimpleDateFormat("MMM dd, HH:mm", Locale.getDefault())
            sdf.format(Date(it))
        }
    }

    fun getCurrentStatusDisplay(): String {
        return when (currentStatus) {
            "checked_in" -> "Currently Working"
            "checked_out" -> "Work Completed Today"
            "not_started" -> "Not Started Today"
            "frozen" -> "Freelancer Frozen"
            else -> "Unknown"
        }
    }

    fun getCurrentStatusColor(): String {
        return when (currentStatus) {
            "checked_in" -> "#4CAF50"
            "checked_out" -> "#6235b9"
            "not_started" -> "#fed442"
            "frozen" -> "#F44336"
            else -> "#858585"
        }
    }

    fun getCompletionRateDisplay(): String {
        return "${(completionRate * 100).toInt()}%"
    }

    fun getCompletionRateColor(): String {
        return when {
            completionRate >= 0.9 -> "#4CAF50"
            completionRate >= 0.7 -> "#fed442"
            else -> "#F44336"
        }
    }

    fun getWorkingSummary(): String {
        return "$attendedDays/$totalWorkDays days completed"
    }
}