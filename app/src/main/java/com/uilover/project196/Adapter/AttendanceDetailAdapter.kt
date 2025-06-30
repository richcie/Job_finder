package com.uilover.project196.Adapter

import android.graphics.Color
import android.view.LayoutInflater
import android.view.ViewGroup
import androidx.recyclerview.widget.DiffUtil
import androidx.recyclerview.widget.RecyclerView
import com.uilover.project196.Model.JobAttendanceEntity
import com.uilover.project196.databinding.ViewholderAttendanceDetailBinding
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import java.text.SimpleDateFormat
import java.util.*

// KRITERIA WAJIB: RecyclerView + Adapter (9/9) - Adapter untuk detail absensi
class AttendanceDetailAdapter(
    private var attendanceList: MutableList<JobAttendanceEntity>,
    private val onActionClick: ((JobAttendanceEntity, String) -> Unit)? = null
) : RecyclerView.Adapter<AttendanceDetailAdapter.AttendanceDetailViewHolder>() {


    private val dateDisplayFormat = SimpleDateFormat("EEEE, MMM dd, yyyy", Locale.getDefault())
    private val timeFormat = SimpleDateFormat("HH:mm", Locale.getDefault())
    private val dateKeyFormat = SimpleDateFormat("yyyy-MM-dd", Locale.getDefault())
    private val todayString = dateKeyFormat.format(Date())

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): AttendanceDetailViewHolder {
        val binding = ViewholderAttendanceDetailBinding.inflate(
            LayoutInflater.from(parent.context),
            parent,
            false
        )
        return AttendanceDetailViewHolder(binding)
    }

    override fun onBindViewHolder(holder: AttendanceDetailViewHolder, position: Int) {
        holder.bind(attendanceList[position])
    }

    override fun getItemCount(): Int = attendanceList.size

    fun updateData(newAttendanceList: List<JobAttendanceEntity>) {
        android.util.Log.d("AttendanceDetailAdapter", "Updating adapter with ${newAttendanceList.size} records")


        newAttendanceList.forEachIndexed { index, record ->
            android.util.Log.d("AttendanceDetailAdapter", "Record $index: ${record.attendanceDate}, checkIn=${record.checkInTime != null}")
        }


        attendanceList.clear()
        attendanceList.addAll(newAttendanceList)


        notifyDataSetChanged()

        android.util.Log.d("AttendanceDetailAdapter", "Adapter updated. Current size: ${attendanceList.size}")
    }

    inner class AttendanceDetailViewHolder(
        private val binding: ViewholderAttendanceDetailBinding
    ) : RecyclerView.ViewHolder(binding.root) {

        fun bind(attendance: JobAttendanceEntity) {
            val isToday = attendance.attendanceDate == todayString
            val isPast = attendance.attendanceDate < todayString
            val isFuture = attendance.attendanceDate > todayString


            val isAttendanceDisabled = binding.root.context.getSharedPreferences("app_state", android.content.Context.MODE_PRIVATE)
                .getBoolean("attendance_disabled", false)


            val isUserBlocked = isCurrentUserBlocked(binding.root.context)

            binding.apply {

                val date = dateKeyFormat.parse(attendance.attendanceDate)
                attendanceDate.text = date?.let { dateDisplayFormat.format(it) } ?: attendance.attendanceDate


                when {
                    isToday -> {
                        dateBadge.text = "TODAY"
                        dateBadge.setBackgroundColor(Color.parseColor("#6235b9"))
                        dateBadge.visibility = android.view.View.VISIBLE
                    }
                    isPast -> {
                        dateBadge.text = "PAST"
                        dateBadge.setBackgroundColor(Color.parseColor("#858585"))
                        dateBadge.visibility = android.view.View.VISIBLE
                    }
                    isFuture -> {
                        dateBadge.text = "UPCOMING"
                        dateBadge.setBackgroundColor(Color.parseColor("#fed442"))
                        dateBadge.visibility = android.view.View.VISIBLE
                    }
                    else -> {
                        dateBadge.visibility = android.view.View.GONE
                    }
                }


                setupCheckInButton(attendance, isToday, isFuture, isAttendanceDisabled, isUserBlocked)


                setupCheckOutButton(attendance, isToday, isFuture, isAttendanceDisabled, isUserBlocked)


                calculateWorkHours(attendance, isToday)


                setupProgressReport(attendance)


                setupDayStatus(attendance, isFuture)


                root.alpha = if (isFuture) 0.7f else 1.0f
            }
        }

        private fun ViewholderAttendanceDetailBinding.setupCheckInButton(
            attendance: JobAttendanceEntity,
            isToday: Boolean,
            isFuture: Boolean,
            isAttendanceDisabled: Boolean,
            isUserBlocked: Boolean
        ) {
            if (attendance.checkInTime != null) {
                checkInTime.text = timeFormat.format(Date(attendance.checkInTime))
                checkInStatus.text = "Checked In"
                checkInStatus.setTextColor(Color.parseColor("#4CAF50"))
                checkInButton.visibility = android.view.View.GONE
            } else {
                checkInTime.text = "--:--"
                checkInButton.visibility = android.view.View.VISIBLE

                when {
                    isUserBlocked -> {
                        checkInStatus.text = "Account frozen"
                        checkInStatus.setTextColor(Color.parseColor("#F44336"))
                        checkInButton.isEnabled = false
                        checkInButton.text = "FROZEN"
                        checkInButton.alpha = 0.6f
                        checkInButton.setOnClickListener(null)
                    }
                    isToday && !isAttendanceDisabled -> {
                        checkInStatus.text = "Ready to check in"
                        checkInStatus.setTextColor(Color.parseColor("#6235b9"))
                        checkInButton.isEnabled = true
                        checkInButton.text = "Check In"
                        checkInButton.alpha = 1.0f
                        checkInButton.setOnClickListener { onActionClick?.invoke(attendance, "check_in") }
                    }
                    isFuture && !isAttendanceDisabled -> {
                        checkInStatus.text = "Tomorrow's shift"
                        checkInStatus.setTextColor(Color.parseColor("#fed442"))
                        checkInButton.isEnabled = false
                        checkInButton.text = "Tomorrow"
                        checkInButton.alpha = 0.6f
                        checkInButton.setOnClickListener(null)
                    }
                    isAttendanceDisabled -> {
                        checkInStatus.text = "Attendance disabled"
                        checkInStatus.setTextColor(Color.parseColor("#F44336"))
                        checkInButton.isEnabled = false
                        checkInButton.text = "Disabled"
                        checkInButton.alpha = 0.6f
                        checkInButton.setOnClickListener(null)
                    }
                    else -> {
                        checkInStatus.text = if (isFuture) "Locked until date" else "Not checked in"
                        checkInStatus.setTextColor(Color.parseColor("#858585"))
                        checkInButton.isEnabled = false
                        checkInButton.text = "Disabled"
                        checkInButton.alpha = 0.6f
                        checkInButton.setOnClickListener(null)
                    }
                }
            }
        }

        private fun ViewholderAttendanceDetailBinding.setupCheckOutButton(
            attendance: JobAttendanceEntity,
            isToday: Boolean,
            isFuture: Boolean,
            isAttendanceDisabled: Boolean,
            isUserBlocked: Boolean
        ) {
            if (attendance.checkOutTime != null) {
                checkOutTime.text = timeFormat.format(Date(attendance.checkOutTime))
                checkOutStatus.text = "Checked Out"
                checkOutStatus.setTextColor(Color.parseColor("#6235b9"))
                checkOutButton.visibility = android.view.View.GONE
            } else {
                checkOutTime.text = "--:--"
                checkOutButton.visibility = android.view.View.VISIBLE

                when {
                    isUserBlocked -> {
                        checkOutStatus.text = "Account frozen"
                        checkOutStatus.setTextColor(Color.parseColor("#F44336"))
                        checkOutButton.isEnabled = false
                        checkOutButton.text = "FROZEN"
                        checkOutButton.alpha = 0.6f
                        checkOutButton.setOnClickListener(null)
                    }
                    isToday && attendance.checkInTime != null && !isAttendanceDisabled -> {
                        checkOutStatus.text = "Ready to check out"
                        checkOutStatus.setTextColor(Color.parseColor("#4CAF50"))
                        checkOutButton.isEnabled = true
                        checkOutButton.text = "Check Out"
                        checkOutButton.alpha = 1.0f
                        checkOutButton.setOnClickListener { onActionClick?.invoke(attendance, "check_out") }
                    }
                    isToday && attendance.checkInTime == null && !isAttendanceDisabled -> {
                        checkOutStatus.text = "Check in first"
                        checkOutStatus.setTextColor(Color.parseColor("#858585"))
                        checkOutButton.isEnabled = false
                        checkOutButton.text = "Disabled"
                        checkOutButton.alpha = 0.6f
                        checkOutButton.setOnClickListener(null)
                    }
                    isAttendanceDisabled -> {
                        checkOutStatus.text = "Attendance disabled"
                        checkOutStatus.setTextColor(Color.parseColor("#F44336"))
                        checkOutButton.isEnabled = false
                        checkOutButton.text = "Disabled"
                        checkOutButton.alpha = 0.6f
                        checkOutButton.setOnClickListener(null)
                    }
                    else -> {
                        checkOutStatus.text = if (isFuture) "Locked until date" else "Not checked out"
                        checkOutStatus.setTextColor(Color.parseColor("#858585"))
                        checkOutButton.isEnabled = false
                        checkOutButton.text = "Disabled"
                        checkOutButton.alpha = 0.6f
                        checkOutButton.setOnClickListener(null)
                    }
                }
            }
        }

        private fun ViewholderAttendanceDetailBinding.calculateWorkHours(attendance: JobAttendanceEntity, isToday: Boolean) {
            when {
                attendance.checkInTime != null && attendance.checkOutTime != null -> {
                    val workHours = (attendance.checkOutTime - attendance.checkInTime) / (1000.0 * 60 * 60)
                    workHoursText.text = String.format("%.2f hours", workHours)
                    workHoursText.setTextColor(Color.parseColor("#4CAF50"))


                    android.util.Log.d("AttendanceDetailAdapter", "=== WORK HOURS CALCULATION ===")
                    android.util.Log.d("AttendanceDetailAdapter", "Check-in: ${attendance.checkInTime}")
                    android.util.Log.d("AttendanceDetailAdapter", "Check-out: ${attendance.checkOutTime}")
                    android.util.Log.d("AttendanceDetailAdapter", "Time diff (ms): ${attendance.checkOutTime - attendance.checkInTime}")
                    android.util.Log.d("AttendanceDetailAdapter", "Work hours: $workHours")
                    android.util.Log.d("AttendanceDetailAdapter", "Formatted: ${String.format("%.2f hours", workHours)}")
                }
                attendance.checkInTime != null && isToday -> {
                    val currentTime = System.currentTimeMillis()
                    val workHours = (currentTime - attendance.checkInTime) / (1000.0 * 60 * 60)
                    workHoursText.text = String.format("%.2f hours (ongoing)", workHours)
                    workHoursText.setTextColor(Color.parseColor("#fed442"))
                }
                attendance.checkInTime != null -> {
                    workHoursText.text = "Incomplete"
                    workHoursText.setTextColor(Color.parseColor("#fed442"))
                }
                else -> {
                    workHoursText.text = if (attendance.attendanceDate > todayString) "Scheduled" else "0.00 hours"
                    workHoursText.setTextColor(Color.parseColor("#858585"))
                }
            }
        }

        private fun ViewholderAttendanceDetailBinding.setupProgressReport(attendance: JobAttendanceEntity) {
            if (!attendance.progressReport.isNullOrEmpty()) {
                progressReport.text = attendance.progressReport
                progressReportLayout.visibility = android.view.View.VISIBLE
            } else {
                progressReportLayout.visibility = android.view.View.GONE
            }
        }

        private fun ViewholderAttendanceDetailBinding.setupDayStatus(attendance: JobAttendanceEntity, isFuture: Boolean) {
            val dayStatus = when {
                isFuture -> "Scheduled"
                attendance.checkInTime != null && attendance.checkOutTime != null -> "Completed"
                attendance.checkInTime != null -> "In Progress"
                else -> "Absent"
            }

            dayStatusIndicator.text = dayStatus


            dayStatusIndicator.setBackgroundColor(Color.TRANSPARENT)


            dayStatusIndicator.setTextColor(
                when (dayStatus) {
                    "Completed" -> Color.parseColor("#4CAF50")
                    "In Progress" -> Color.parseColor("#fed442")
                    "Scheduled" -> Color.parseColor("#6235b9")
                    "Absent" -> Color.parseColor("#F44336")
                    else -> Color.parseColor("#F44336")
                }
            )
        }
    }


    private fun isCurrentUserBlocked(context: android.content.Context): Boolean {
        return try {
            val userId = com.uilover.project196.Utils.UserSession.getUserId()
            if (userId == null) {
                android.util.Log.w("AttendanceDetailAdapter", "No user ID available")
                return true
            }


            val prefs = context.getSharedPreferences("user_status", android.content.Context.MODE_PRIVATE)
            val lastChecked = prefs.getLong("last_checked_$userId", 0)
            val currentTime = System.currentTimeMillis()


            if (currentTime - lastChecked > 30000) {

                android.os.Handler(android.os.Looper.getMainLooper()).post {
                    updateUserBlockedStatusInBackground(context, userId)
                }
            }


            val isFrozen = prefs.getBoolean("is_blocked_$userId", false)
            android.util.Log.d("AttendanceDetailAdapter", "User $userId frozen status: $isFrozen (cached)")
            isFrozen

        } catch (e: Exception) {
            android.util.Log.e("AttendanceDetailAdapter", "Error checking user frozen status", e)
            true
        }
    }

    private fun updateUserBlockedStatusInBackground(context: android.content.Context, userId: String) {
        kotlinx.coroutines.CoroutineScope(kotlinx.coroutines.Dispatchers.IO).launch {
            try {
                val database = com.uilover.project196.Database.AppDatabase.getDatabase(context)
                val userDao = database.userDao()
                val user = userDao.getUserById(userId)

                val isFrozen = user?.isActive == false
                android.util.Log.d("AttendanceDetailAdapter", "Updated user $userId frozen status from database: $isFrozen")


                val prefs = context.getSharedPreferences("user_status", android.content.Context.MODE_PRIVATE)
                prefs.edit()
                    .putBoolean("is_blocked_$userId", isFrozen)
                    .putLong("last_checked_$userId", System.currentTimeMillis())
                    .apply()

            } catch (e: Exception) {
                android.util.Log.e("AttendanceDetailAdapter", "Error updating user frozen status", e)
            }
        }
    }


    private class AttendanceDiffCallback(
        private val oldList: List<JobAttendanceEntity>,
        private val newList: List<JobAttendanceEntity>
    ) : DiffUtil.Callback() {

        override fun getOldListSize(): Int = oldList.size
        override fun getNewListSize(): Int = newList.size

        override fun areItemsTheSame(oldItemPosition: Int, newItemPosition: Int): Boolean {
            return oldList[oldItemPosition].id == newList[newItemPosition].id
        }

        override fun areContentsTheSame(oldItemPosition: Int, newItemPosition: Int): Boolean {
            val oldItem = oldList[oldItemPosition]
            val newItem = newList[newItemPosition]
            return oldItem.checkInTime == newItem.checkInTime &&
                   oldItem.checkOutTime == newItem.checkOutTime &&
                   oldItem.progressReport == newItem.progressReport
        }
    }
}