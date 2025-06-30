package com.uilover.project196.Model

import androidx.room.Entity
import androidx.room.PrimaryKey

@Entity(
    tableName = "chat_messages",
    indices = [
        androidx.room.Index(value = ["chatId"]),
        androidx.room.Index(value = ["senderId"]),
        androidx.room.Index(value = ["timestamp"])
    ]
)
data class MessageEntity(
    @PrimaryKey
    val id: String,
    val chatId: String,
    val senderId: String,
    val text: String,
    val timestamp: Long = System.currentTimeMillis(),
    val isRead: Boolean = false,
    val messageType: String = "text",
    val jobRequestStatus: String? = null,
    val jobId: String? = null,
    val verificationStatus: String? = null,
    val freelancerData: String? = null
)