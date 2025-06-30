package com.uilover.project196.Database

import androidx.room.Dao
import androidx.room.Insert
import androidx.room.Query
import androidx.room.Update
import com.uilover.project196.Model.MessageEntity

@Dao
// KRITERIA WAJIB: Room DAO untuk operasi database Message
interface MessageDao {

    @Insert
    suspend fun insertMessage(message: MessageEntity)

    @Query("SELECT * FROM chat_messages WHERE chatId = :chatId ORDER BY timestamp ASC")
    suspend fun getMessagesForChat(chatId: String): List<MessageEntity>

    @Query("SELECT * FROM chat_messages WHERE chatId = :chatId ORDER BY timestamp DESC LIMIT 1")
    suspend fun getLastMessageForChat(chatId: String): MessageEntity?

    @Query("UPDATE chat_messages SET isRead = 1 WHERE chatId = :chatId AND senderId != :currentUserId")
    suspend fun markMessagesAsRead(chatId: String, currentUserId: String)

    @Query("SELECT COUNT(*) FROM chat_messages WHERE chatId = :chatId AND senderId != :currentUserId AND isRead = 0")
    suspend fun getUnreadMessageCount(chatId: String, currentUserId: String): Int

    @Query("DELETE FROM chat_messages WHERE chatId = :chatId")
    suspend fun deleteMessagesForChat(chatId: String)

    @Query("UPDATE chat_messages SET jobRequestStatus = :newStatus WHERE id = :messageId")
    suspend fun updateJobRequestStatus(messageId: String, newStatus: String)

    @Query("UPDATE chat_messages SET verificationStatus = :newStatus WHERE id = :messageId")
    suspend fun updateVerificationStatus(messageId: String, newStatus: String)

    @Query("SELECT * FROM chat_messages WHERE senderId = :freelancerId AND messageType = 'verification_request' AND verificationStatus = 'accepted' LIMIT 1")
    suspend fun getAcceptedVerificationForFreelancer(freelancerId: String): MessageEntity?

    @Query("SELECT * FROM chat_messages WHERE messageType = 'verification_request' AND verificationStatus = 'accepted'")
    suspend fun getAllAcceptedVerifications(): List<MessageEntity>

    @Query("SELECT DISTINCT senderId FROM chat_messages WHERE messageType = 'verification_request' AND verificationStatus = 'accepted'")
    suspend fun getAllVerifiedFreelancerIds(): List<String>

    @Query("UPDATE chat_messages SET verificationStatus = 'pending' WHERE messageType = 'verification_request' AND verificationStatus IN ('accepted', 'rejected')")
    suspend fun resetAllVerificationStatuses()

    @Query("UPDATE chat_messages SET verificationStatus = 'pending' WHERE messageType = 'verification_request' AND senderId = :freelancerId AND verificationStatus IN ('accepted', 'rejected')")
    suspend fun resetVerificationStatusForFreelancer(freelancerId: String)

    @Query("DELETE FROM chat_messages WHERE messageType = 'verification_request'")
    suspend fun deleteAllVerificationRequests()

    @Query("DELETE FROM chat_messages")
    suspend fun deleteAllMessages()

    @Query("SELECT * FROM chat_messages ORDER BY timestamp DESC")
    suspend fun getAllMessages(): List<MessageEntity>
}