package com.uilover.project196.Adapter

import androidx.fragment.app.Fragment
import androidx.fragment.app.FragmentActivity
import androidx.viewpager2.adapter.FragmentStateAdapter
import com.uilover.project196.Fragment.*
import com.uilover.project196.Utils.UserSession

class MainViewPagerAdapter(fragmentActivity: FragmentActivity) : FragmentStateAdapter(fragmentActivity) {

    override fun getItemCount(): Int {
        return if (UserSession.getUserRole() == UserSession.ROLE_FREELANCER) {
            6
        } else {
            5
        }
    }

    override fun createFragment(position: Int): Fragment {
        val userRole = UserSession.getUserRole()

        return if (userRole == UserSession.ROLE_FREELANCER) {

            when (position) {
                0 -> HomeFragment()
                1 -> ExplorerFragment()
                2 -> JobsFragment()
                3 -> BookmarkFragment()
                4 -> ChatFragment()
                5 -> ProfileFragment()
                else -> HomeFragment()
            }
        } else {

            when (position) {
                0 -> HomeFragment()
                1 -> ExplorerFragment()
                2 -> BookmarkFragment()
                3 -> ChatFragment()
                4 -> ProfileFragment()
                else -> HomeFragment()
            }
        }
    }
}