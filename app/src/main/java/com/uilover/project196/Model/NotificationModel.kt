package com.uilover.project196.Model

import java.text.SimpleDateFormat
import java.util.*

data class NotificationModel(
    val id: String,
    val userId: String,
    val title: String,
    val message: String,
    val timestamp: Long,
    val type: NotificationType,
    val isRead: Boolean = false,
    val jobId: String? = null,
    val companyName: String? = null
) {
    fun getFormattedTime(): String {
        val sdf = SimpleDateFormat("MMM dd, HH:mm", Locale.getDefault())
        return sdf.format(Date(timestamp))
    }

    fun getRelativeTime(): String {
        val now = System.currentTimeMillis()
        val diff = now - timestamp

        return when {
            diff < 60 * 1000 -> "Just now"
            diff < 60 * 60 * 1000 -> "${diff / (60 * 1000)}m ago"
            diff < 24 * 60 * 60 * 1000 -> "${diff / (60 * 60 * 1000)}h ago"
            diff < 7 * 24 * 60 * 60 * 1000 -> "${diff / (24 * 60 * 60 * 1000)}d ago"
            else -> getFormattedTime()
        }
    }
}

enum class NotificationType {
    JOB_APPLICATION,
    JOB_MATCH,
    INTERVIEW_SCHEDULED,
    APPLICATION_STATUS,
    NEW_JOB_ALERT,
    SYSTEM,
    PROMOTION
}