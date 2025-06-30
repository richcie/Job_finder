package com.uilover.project196.Adapter

import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import androidx.recyclerview.widget.RecyclerView
import com.uilover.project196.Model.NotificationModel
import com.uilover.project196.Model.NotificationType
import com.uilover.project196.R
import com.uilover.project196.databinding.ViewholderNotificationBinding

// KRITERIA WAJIB: RecyclerView + Adapter (6/9) - Adapter untuk notifikasi
class NotificationAdapter(
    private var notifications: List<NotificationModel>,
    private val clickListener: ClickListener
) : RecyclerView.Adapter<NotificationAdapter.NotificationViewHolder>() {

    interface ClickListener {
        fun onNotificationClick(notification: NotificationModel)
        fun onNotificationRead(notification: NotificationModel)
    }

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): NotificationViewHolder {
        val binding = ViewholderNotificationBinding.inflate(
            LayoutInflater.from(parent.context),
            parent,
            false
        )
        return NotificationViewHolder(binding)
    }

    override fun onBindViewHolder(holder: NotificationViewHolder, position: Int) {
        holder.bind(notifications[position])
    }

    override fun getItemCount(): Int = notifications.size

    fun updateData(newNotifications: List<NotificationModel>) {
        notifications = newNotifications
        notifyDataSetChanged()
    }

    inner class NotificationViewHolder(private val binding: ViewholderNotificationBinding) :
        RecyclerView.ViewHolder(binding.root) {

        fun bind(notification: NotificationModel) {
            binding.apply {
                notificationTitle.text = notification.title
                notificationMessage.text = notification.message
                notificationTime.text = notification.getRelativeTime()


                if (notification.companyName != null) {
                    companyName.text = notification.companyName
                    companyName.visibility = View.VISIBLE
                } else {
                    companyName.visibility = View.GONE
                }


                val iconResource = when (notification.type) {
                    NotificationType.JOB_APPLICATION -> R.drawable.job_type
                    NotificationType.JOB_MATCH -> R.drawable.search_icon
                    NotificationType.INTERVIEW_SCHEDULED -> R.drawable.bell
                    NotificationType.APPLICATION_STATUS -> R.drawable.settings
                    NotificationType.NEW_JOB_ALERT -> R.drawable.bell
                    NotificationType.SYSTEM -> R.drawable.settings
                    NotificationType.PROMOTION -> R.drawable.fav
                }
                notificationIcon.setImageResource(iconResource)


                unreadIndicator.visibility = if (!notification.isRead) View.VISIBLE else View.GONE


                if (!notification.isRead) {
                    root.setBackgroundColor(root.context.getColor(R.color.white))
                } else {
                    root.setBackgroundColor(root.context.getColor(android.R.color.transparent))
                }


                root.setOnClickListener {
                    clickListener.onNotificationClick(notification)
                    if (!notification.isRead) {
                        clickListener.onNotificationRead(notification)
                    }
                }
            }
        }
    }
}