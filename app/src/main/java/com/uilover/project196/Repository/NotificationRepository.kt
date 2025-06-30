package com.uilover.project196.Repository

import com.uilover.project196.Model.NotificationModel
import com.uilover.project196.Model.NotificationType
import com.uilover.project196.Utils.UserSession

object NotificationRepository {
    private var notifications = mutableListOf<NotificationModel>()
    private var listeners = mutableListOf<NotificationUpdateListener>()

    interface NotificationUpdateListener {
        fun onNotificationCountChanged(unreadCount: Int)
    }

    init {
        generateDummyNotifications()
    }

    fun addListener(listener: NotificationUpdateListener) {
        listeners.add(listener)
    }

    fun removeListener(listener: NotificationUpdateListener) {
        listeners.remove(listener)
    }

    private fun notifyListeners() {
        val unreadCount = getUnreadCount()
        listeners.forEach { it.onNotificationCountChanged(unreadCount) }
    }

    private fun generateDummyNotifications() {
        val currentTime = System.currentTimeMillis()

        notifications.clear()

        notifications.addAll(listOf(
            NotificationModel(
                id = "1",
                userId = "user_001",
                title = "New Job Match!",
                message = "A Software Engineer position at Google matches your profile perfectly",
                timestamp = currentTime - (30 * 60 * 1000),
                type = NotificationType.JOB_MATCH,
                isRead = false,
                jobId = "job_001",
                companyName = "Google Inc."
            ),
            NotificationModel(
                id = "2",
                userId = "user_001",
                title = "Application Status Update",
                message = "Your application for Frontend Developer at Meta has been reviewed",
                timestamp = currentTime - (2 * 60 * 60 * 1000),
                type = NotificationType.APPLICATION_STATUS,
                isRead = false,
                jobId = "job_002",
                companyName = "Meta"
            ),
            NotificationModel(
                id = "3",
                userId = "user_001",
                title = "Interview Scheduled",
                message = "Interview scheduled for Senior Android Developer position",
                timestamp = currentTime - (4 * 60 * 60 * 1000),
                type = NotificationType.INTERVIEW_SCHEDULED,
                isRead = true,
                jobId = "job_003",
                companyName = "Netflix"
            ),
            NotificationModel(
                id = "4",
                userId = "user_001",
                title = "New Job Alert",
                message = "5 new Product Manager jobs posted in your area",
                timestamp = currentTime - (6 * 60 * 60 * 1000),
                type = NotificationType.NEW_JOB_ALERT,
                isRead = false
            ),
            NotificationModel(
                id = "5",
                userId = "user_001",
                title = "Application Submitted",
                message = "Your application for Data Scientist at Amazon has been submitted successfully",
                timestamp = currentTime - (8 * 60 * 60 * 1000),
                type = NotificationType.JOB_APPLICATION,
                isRead = true,
                jobId = "job_004",
                companyName = "Amazon"
            ),
            NotificationModel(
                id = "6",
                userId = "user_002",
                title = "Profile View Alert",
                message = "Microsoft recruiter viewed your profile",
                timestamp = currentTime - (12 * 60 * 60 * 1000),
                type = NotificationType.SYSTEM,
                isRead = true,
                companyName = "Microsoft"
            ),
            NotificationModel(
                id = "7",
                userId = "user_001",
                title = "Job Match Found",
                message = "UI/UX Designer position at Apple matches your skills",
                timestamp = currentTime - (1 * 24 * 60 * 60 * 1000),
                type = NotificationType.JOB_MATCH,
                isRead = false,
                jobId = "job_005",
                companyName = "Apple Inc."
            ),
            NotificationModel(
                id = "8",
                userId = "user_001",
                title = "Premium Feature Available",
                message = "Upgrade to Premium to unlock advanced job search features",
                timestamp = currentTime - (2 * 24 * 60 * 60 * 1000),
                type = NotificationType.PROMOTION,
                isRead = true
            ),
            NotificationModel(
                id = "9",
                userId = "user_001",
                title = "Application Deadline",
                message = "Reminder: Backend Engineer application at Spotify closes tomorrow",
                timestamp = currentTime - (3 * 24 * 60 * 60 * 1000),
                type = NotificationType.SYSTEM,
                isRead = false,
                jobId = "job_006",
                companyName = "Spotify"
            ),
            NotificationModel(
                id = "10",
                userId = "user_002",
                title = "Congratulations!",
                message = "You've been shortlisted for the DevOps Engineer position at Tesla",
                timestamp = currentTime - (4 * 24 * 60 * 60 * 1000),
                type = NotificationType.APPLICATION_STATUS,
                isRead = false,
                jobId = "job_007",
                companyName = "Tesla Inc."
            )
        ))
    }

    fun getAllNotifications(): List<NotificationModel> {
        val currentUserId = UserSession.getUserId()
        return if (currentUserId != null) {
            notifications.filter { it.userId == currentUserId }
        } else {
            emptyList()
        }
    }

    fun getUnreadCount(): Int {
        val currentUserId = UserSession.getUserId()
        return if (currentUserId != null) {
            notifications.count { !it.isRead && it.userId == currentUserId }
        } else {
            0
        }
    }

    fun markNotificationAsRead(notificationId: String) {
        val currentUserId = UserSession.getUserId() ?: return
        val index = notifications.indexOfFirst { it.id == notificationId && it.userId == currentUserId }
        if (index != -1 && !notifications[index].isRead) {
            notifications[index] = notifications[index].copy(isRead = true)
            notifyListeners()
        }
    }

    fun markAllNotificationsAsRead() {
        val currentUserId = UserSession.getUserId() ?: return
        var hasChanges = false
        notifications.forEachIndexed { index, notification ->
            if (!notification.isRead && notification.userId == currentUserId) {
                notifications[index] = notification.copy(isRead = true)
                hasChanges = true
            }
        }
        if (hasChanges) {
            notifyListeners()
        }
    }

    fun updateNotifications(updatedNotifications: List<NotificationModel>) {
        val currentUserId = UserSession.getUserId() ?: return

        notifications.removeAll { it.userId == currentUserId }
        notifications.addAll(updatedNotifications.filter { it.userId == currentUserId })
        notifyListeners()
    }


    fun addNotification(notification: NotificationModel) {
        notifications.add(notification)
        notifyListeners()
    }
}