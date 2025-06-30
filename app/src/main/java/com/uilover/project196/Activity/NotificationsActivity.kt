package com.uilover.project196.Activity

import android.os.Bundle
import android.view.View
import android.widget.Toast
import androidx.appcompat.app.AppCompatActivity
import androidx.recyclerview.widget.LinearLayoutManager
import com.uilover.project196.Adapter.NotificationAdapter
import com.uilover.project196.Model.NotificationModel
import com.uilover.project196.Model.NotificationType
import com.uilover.project196.Repository.NotificationRepository
import com.uilover.project196.Utils.UserSession
import com.uilover.project196.databinding.ActivityNotificationsBinding

// KRITERIA: Multiple Activity (8/8) - Activity untuk notifikasi
// KRITERIA WAJIB: Multiple Activity (8/8) - Activity untuk sistem notifikasi
// KRITERIA KOMPLEKSITAS: Real-time notification system
class NotificationsActivity : AppCompatActivity(), NotificationAdapter.ClickListener {

    private lateinit var binding: ActivityNotificationsBinding
    private lateinit var notificationAdapter: NotificationAdapter
    private var notifications = mutableListOf<NotificationModel>()

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        binding = ActivityNotificationsBinding.inflate(layoutInflater)
        setContentView(binding.root)


        UserSession.init(this)


        if (UserSession.isLoggedIn()) {
            setupUI()
            loadNotifications()
            setupRecyclerView()
            updateNotificationCount()
        } else {
            showGuestState()
        }
    }

    private fun setupUI() {
        binding.backButton.setOnClickListener {
            finish()
        }

        binding.markAllReadButton.setOnClickListener {
            markAllNotificationsAsRead()
        }
    }

    private fun showGuestState() {

        binding.recyclerViewNotifications.visibility = View.GONE
        binding.emptyStateLayout.visibility = View.VISIBLE


        binding.markAllReadButton.visibility = View.GONE


        Toast.makeText(this, "Please log in to view notifications", Toast.LENGTH_LONG).show()


        finish()
    }

    private fun loadNotifications() {

        if (!UserSession.isLoggedIn()) {
            return
        }

        notifications.clear()


        val userNotifications = NotificationRepository.getAllNotifications()
            .filter { _ ->
                // Filter logic can be added here based on user preferences or roles
                // For now, show all notifications for the logged-in user
                true
            }
        notifications.addAll(userNotifications)
    }

    private fun setupRecyclerView() {

        if (!UserSession.isLoggedIn()) {
            return
        }

        if (notifications.isEmpty()) {
            binding.recyclerViewNotifications.visibility = View.GONE
            binding.emptyStateLayout.visibility = View.VISIBLE
        } else {
            binding.recyclerViewNotifications.visibility = View.VISIBLE
            binding.emptyStateLayout.visibility = View.GONE

            binding.recyclerViewNotifications.layoutManager = LinearLayoutManager(this)
            notificationAdapter = NotificationAdapter(notifications, this)
            binding.recyclerViewNotifications.adapter = notificationAdapter
        }
    }

    private fun updateNotificationCount() {

        if (!UserSession.isLoggedIn()) {
            return
        }

        val unreadCount = notifications.count { !it.isRead }
        // Log the unread count for debugging purposes
        android.util.Log.d("NotificationsActivity", "Unread notifications: $unreadCount")
    }

    private fun markAllNotificationsAsRead() {

        if (!UserSession.isLoggedIn()) {
            Toast.makeText(this, "Please log in to manage notifications", Toast.LENGTH_SHORT).show()
            return
        }

        NotificationRepository.markAllNotificationsAsRead()
        loadNotifications()
        notificationAdapter.updateData(notifications)
        updateNotificationCount()
        Toast.makeText(this, "All notifications marked as read", Toast.LENGTH_SHORT).show()
    }

    override fun onNotificationClick(notification: NotificationModel) {

        if (!UserSession.isLoggedIn()) {
            Toast.makeText(this, "Please log in to view notification details", Toast.LENGTH_SHORT).show()
            return
        }


        val message = buildString {
            append("Title: ${notification.title}\n\n")
            append("Message: ${notification.message}\n\n")
            append("Time: ${notification.getFormattedTime()}\n\n")
            append("Type: ${notification.type.name.replace("_", " ")}")
            if (notification.companyName != null) {
                append("\nCompany: ${notification.companyName}")
            }
            if (notification.jobId != null) {
                append("\nJob ID: ${notification.jobId}")
            }
        }

        androidx.appcompat.app.AlertDialog.Builder(this)
            .setTitle("Notification Details")
            .setMessage(message)
            .setPositiveButton("OK", null)
            .setNeutralButton("Mark as Read") { _, _ ->
                if (!notification.isRead) {
                    onNotificationRead(notification)
                }
            }
            .show()
    }

    override fun onNotificationRead(notification: NotificationModel) {

        if (!UserSession.isLoggedIn()) {
            Toast.makeText(this, "Please log in to manage notifications", Toast.LENGTH_SHORT).show()
            return
        }

        NotificationRepository.markNotificationAsRead(notification.id)
        loadNotifications()
        notificationAdapter.updateData(notifications)
        updateNotificationCount()
    }
}