package com.uilover.project196.Model

import java.text.SimpleDateFormat
import java.util.*

data class MessageModel(
    val id: String,
    val chatId: String,
    val text: String,
    val timestamp: Long,
    val isFromUser: Boolean,
    val senderName: String = "",
    val senderId: String = "",
    val messageType: String = "text",
    val jobRequestStatus: String? = null,
    val jobId: String? = null,
    val verificationStatus: String? = null,
    val freelancerData: String? = null
) {
    fun getFormattedTime(): String {
        val sdf = SimpleDateFormat("HH:mm", Locale.getDefault())
        return sdf.format(Date(timestamp))
    }

    fun isJobRequest(): Boolean {
        return messageType == "job_request"
    }

    fun isJobRequestPending(): Boolean {
        return isJobRequest() && jobRequestStatus == "pending"
    }

    fun isJobRequestAccepted(): Boolean {
        return isJobRequest() && jobRequestStatus == "accepted"
    }

    fun isJobRequestRejected(): Boolean {
        return isJobRequest() && jobRequestStatus == "rejected"
    }

    fun isVerificationRequest(): Boolean {
        return messageType == "verification_request"
    }

    fun isVerificationPending(): Boolean {
        return isVerificationRequest() && verificationStatus == "pending"
    }

    fun isVerificationAccepted(): Boolean {
        return isVerificationRequest() && verificationStatus == "accepted"
    }

    fun isVerificationRejected(): Boolean {
        return isVerificationRequest() && verificationStatus == "rejected"
    }

    companion object {
        const val TYPE_TEXT = "text"
        const val TYPE_JOB_REQUEST = "job_request"
        const val TYPE_VERIFICATION_REQUEST = "verification_request"

        const val STATUS_PENDING = "pending"
        const val STATUS_ACCEPTED = "accepted"
        const val STATUS_REJECTED = "rejected"
    }
}