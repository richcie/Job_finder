package com.uilover.project196.Model

import java.text.SimpleDateFormat
import java.util.*

data class ChatModel(
    val id: String,
    val userId: String,
    val companyName: String,
    val companyLogo: String,
    val recruiterName: String,
    val jobTitle: String,
    val lastMessage: String,
    val timestamp: Long,
    val isUnread: Boolean = true,
    val approvalStatus: ApprovalStatus = ApprovalStatus.PENDING,
    val messageCount: Int = 1,
    val freelancerProfession: String? = null
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

enum class ApprovalStatus {
    PENDING,
    APPROVED,
    REJECTED
}