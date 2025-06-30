package com.uilover.project196.Fragment

import android.content.Intent
import android.view.View
import android.widget.ImageView
import android.widget.TextView
import androidx.fragment.app.Fragment
import com.uilover.project196.Activity.NotificationsActivity
import com.uilover.project196.Repository.NotificationRepository
import com.uilover.project196.Utils.UserSession


// KRITERIA WAJIB: Multiple Fragment (16/16) - Base class untuk semua fragment
// KRITERIA WAJIB: Navigation antar fragment
abstract class BaseFragment : Fragment(), NotificationRepository.NotificationUpdateListener, UserSession.LoginStateListener {


    private var notificationBadge: TextView? = null


    protected fun setupNotificationButton(notificationIcon: ImageView?, notificationBadge: TextView?) {

        this.notificationBadge = notificationBadge

        notificationIcon?.setOnClickListener {
            handleNotificationClick()
        }


        updateNotificationBadge(notificationBadge)
    }


    private fun handleNotificationClick() {
        val intent = if (UserSession.isLoggedIn()) {
            Intent(requireContext(), NotificationsActivity::class.java)
        } else {
            Intent(requireContext(), com.uilover.project196.Activity.LoginActivity::class.java).apply {
                putExtra("SOURCE_SCREEN", "notification")
            }
        }
        startActivity(intent)
    }


    private fun updateNotificationBadge(notificationBadge: TextView?) {
        notificationBadge?.let { badge ->
            if (UserSession.isLoggedIn()) {
                val unreadCount = NotificationRepository.getUnreadCount()
                if (unreadCount > 0) {
                    badge.text = unreadCount.toString()
                    badge.visibility = View.VISIBLE
                } else {
                    badge.visibility = View.GONE
                }
            } else {

                badge.visibility = View.GONE
            }
        }
    }


    override fun onNotificationCountChanged(unreadCount: Int) {

        updateNotificationBadge(notificationBadge)
    }

    override fun onResume() {
        super.onResume()

        NotificationRepository.addListener(this)

        UserSession.addLoginStateListener(this)

        updateNotificationBadge(notificationBadge)
    }

    override fun onPause() {
        super.onPause()

        NotificationRepository.removeListener(this)

        UserSession.removeLoginStateListener(this)
    }


    override fun onLoginStateChanged(isLoggedIn: Boolean) {

        updateNotificationBadge(notificationBadge)


        onLoginStateRefresh(isLoggedIn)
    }


    override fun onLoginStateRefresh(isLoggedIn: Boolean) {


    }

    override fun onDestroyView() {
        super.onDestroyView()

        notificationBadge = null
    }
}