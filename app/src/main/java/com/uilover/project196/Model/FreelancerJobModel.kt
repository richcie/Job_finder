package com.uilover.project196.Model

import java.text.SimpleDateFormat
import java.util.*

data class FreelancerJobModel(
    val id: Int,
    val title: String,
    val companyName: String,
    val companyLogo: String,
    val location: String,
    val startDate: Long,
    val endDate: Long? = null,
    val isActive: Boolean = true,
    val lastCheckIn: Long? = null,
    val lastCheckOut: Long? = null,
    val applicationId: Int,
    val businessOwnerId: String,
    val isFrozenByCompany: Boolean = false,
    val frozenByCompanyName: String? = null,
    val jobStatus: String = "open"
) {

    fun getFormattedStartDate(): String {
        val sdf = SimpleDateFormat("dd MMM yyyy", Locale.getDefault())
        return sdf.format(Date(startDate))
    }

    fun getFormattedEndDate(): String {
        return if (endDate != null) {
            val sdf = SimpleDateFormat("dd MMM yyyy", Locale.getDefault())
            sdf.format(Date(endDate))
        } else {
            "Ongoing"
        }
    }

    fun getFormattedLastCheckIn(): String {
        return if (lastCheckIn != null) {
            val sdf = SimpleDateFormat("HH:mm", Locale.getDefault())
            sdf.format(Date(lastCheckIn))
        } else {
            "Not checked in"
        }
    }

    fun getFormattedLastCheckOut(): String {
        return if (lastCheckOut != null) {
            val sdf = SimpleDateFormat("HH:mm", Locale.getDefault())
            sdf.format(Date(lastCheckOut))
        } else {
            "Not checked out"
        }
    }

    fun isCheckedInToday(): Boolean {
        if (lastCheckIn == null) return false

        val today = Calendar.getInstance()
        val checkInDate = Calendar.getInstance().apply { timeInMillis = lastCheckIn }

        return today.get(Calendar.YEAR) == checkInDate.get(Calendar.YEAR) &&
               today.get(Calendar.DAY_OF_YEAR) == checkInDate.get(Calendar.DAY_OF_YEAR)
    }

    fun isCheckedOutToday(): Boolean {
        if (lastCheckOut == null) return false

        val today = Calendar.getInstance()
        val checkOutDate = Calendar.getInstance().apply { timeInMillis = lastCheckOut }

        return today.get(Calendar.YEAR) == checkOutDate.get(Calendar.YEAR) &&
               today.get(Calendar.DAY_OF_YEAR) == checkOutDate.get(Calendar.DAY_OF_YEAR)
    }
}