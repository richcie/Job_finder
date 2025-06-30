package com.uilover.project196.Adapter

import android.graphics.drawable.AnimationDrawable
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import androidx.recyclerview.widget.RecyclerView
import com.uilover.project196.Model.FreelancerJobModel
import com.uilover.project196.databinding.ViewholderFreelancerJobBinding
import com.uilover.project196.R
import android.content.Context

// KRITERIA WAJIB: RecyclerView + Adapter (4/9) - Adapter untuk pekerjaan freelancer
class FreelancerJobAdapter(
    private val jobs: List<FreelancerJobModel>,
    private val onJobClick: (FreelancerJobModel) -> Unit,
    private val onViewProgress: ((FreelancerJobModel) -> Unit)? = null,
    private val onWriteReview: ((FreelancerJobModel) -> Unit)? = null
) : RecyclerView.Adapter<FreelancerJobAdapter.JobViewHolder>() {

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): JobViewHolder {
        val binding = ViewholderFreelancerJobBinding.inflate(
            LayoutInflater.from(parent.context), parent, false
        )
        return JobViewHolder(binding)
    }

    override fun onBindViewHolder(holder: JobViewHolder, position: Int) {
        holder.bind(jobs[position])
    }

    override fun getItemCount(): Int = jobs.size

    inner class JobViewHolder(private val binding: ViewholderFreelancerJobBinding) :
        RecyclerView.ViewHolder(binding.root) {

        fun bind(job: FreelancerJobModel) {
            binding.apply {
                jobTitle.text = job.title
                companyName.text = job.companyName

                jobStartDate.text = "Started: ${job.getFormattedStartDate()}"


                val prefs = itemView.context.getSharedPreferences("app_state", android.content.Context.MODE_PRIVATE)
                val isAttendanceDisabled = prefs.getBoolean("attendance_disabled", false)


                statusText.setBackgroundColor(android.graphics.Color.TRANSPARENT)


                when {
                    job.jobStatus == "closed" -> {
                        statusText.text = "JOBS CLOSED"
                        statusText.setTextColor(android.graphics.Color.parseColor("#757575"))
                        jobStatusDescription.text = "This job has been closed by the business owner. No further check-in/out actions are allowed."
                    }
                    job.isFrozenByCompany -> {
                        statusText.text = "FROZEN"
                        statusText.setTextColor(android.graphics.Color.parseColor("#F44336"))
                        jobStatusDescription.text = "You have been frozen by ${job.frozenByCompanyName ?: "this company"}. Check-in/out actions are disabled."
                    }
                    job.isActive -> {
                        statusText.text = "ACTIVE"
                        statusText.setTextColor(android.graphics.Color.parseColor("#4CAF50"))
                        jobStatusDescription.text = "This job is currently active. You can manage attendance and view progress."
                    }
                    else -> {
                        statusText.text = "SHORTLISTED"
                        statusText.setTextColor(android.graphics.Color.parseColor("#FF9500"))
                        jobStatusDescription.text = "You have been shortlisted for this job. Waiting for activation."
                    }
                }


                when {
                    job.jobStatus == "closed" -> {
                        checkInStatus.text = "❌ Job closed by business owner"
                        checkInStatus.setTextColor(itemView.context.getColor(R.color.darkGrey))
                        checkOutStatus.text = "❌ Check-in/out no longer available"
                        checkOutStatus.setTextColor(itemView.context.getColor(R.color.darkGrey))
                    }
                    job.isFrozenByCompany -> {
                        checkInStatus.text = "🔒 Frozen by ${job.frozenByCompanyName ?: "company"}"
                        checkInStatus.setTextColor(itemView.context.getColor(R.color.red))
                        checkOutStatus.text = "🔒 Contact company to unfreeze your account"
                        checkOutStatus.setTextColor(itemView.context.getColor(R.color.red))
                    }
                    isAttendanceDisabled -> {
                        checkInStatus.text = "⚠ Attendance disabled - awaiting re-verification"
                        checkInStatus.setTextColor(itemView.context.getColor(R.color.red))
                        checkOutStatus.text = "⚠ Please wait for business owner approval"
                        checkOutStatus.setTextColor(itemView.context.getColor(R.color.red))
                    }
                    else -> {

                        if (job.isCheckedInToday()) {
                            checkInStatus.text = "✓ Checked in: ${job.getFormattedLastCheckIn()}"
                            checkInStatus.setTextColor(itemView.context.getColor(R.color.green))
                        } else {
                            checkInStatus.text = "⚬ Not checked in today"
                            checkInStatus.setTextColor(itemView.context.getColor(R.color.darkGrey))
                        }

                        if (job.isCheckedOutToday()) {
                            checkOutStatus.text = "✓ Checked out: ${job.getFormattedLastCheckOut()}"
                            checkOutStatus.setTextColor(itemView.context.getColor(R.color.green))
                        } else {
                            checkOutStatus.text = "⚬ Not checked out today"
                            checkOutStatus.setTextColor(itemView.context.getColor(R.color.darkGrey))
                        }
                    }
                }


                var isExpanded = false
                statusIndicator.setOnClickListener {
                    isExpanded = !isExpanded
                    jobStatusActions.visibility = if (isExpanded) View.VISIBLE else View.GONE
                }


                if (job.jobStatus == "closed") {
                    manageAttendanceButton.isEnabled = false
                    manageAttendanceButton.alpha = 0.5f
                    manageAttendanceButton.text = "JOB CLOSED"
                    manageAttendanceButton.setOnClickListener {
                        android.widget.Toast.makeText(
                            itemView.context,
                            "This job has been closed by the business owner. Attendance is no longer available.",
                            android.widget.Toast.LENGTH_LONG
                        ).show()
                    }
                } else if (job.isFrozenByCompany) {
                    manageAttendanceButton.isEnabled = false
                    manageAttendanceButton.alpha = 0.5f
                    manageAttendanceButton.text = "FROZEN"
                    manageAttendanceButton.setOnClickListener {
                        android.widget.Toast.makeText(
                            itemView.context,
                            "You have been frozen by ${job.frozenByCompanyName ?: "this company"}. Contact them to unfreeze your account.",
                            android.widget.Toast.LENGTH_LONG
                        ).show()
                    }
                } else {
                    manageAttendanceButton.isEnabled = true
                    manageAttendanceButton.alpha = 1.0f
                    manageAttendanceButton.text = "MANAGE ATTENDANCE"
                    manageAttendanceButton.setOnClickListener {
                        onJobClick(job)
                    }
                }

                viewProgressButton.setOnClickListener {
                    onViewProgress?.invoke(job) ?: run {

                        android.widget.Toast.makeText(
                            itemView.context,
                            "Progress tracking for ${job.title}",
                            android.widget.Toast.LENGTH_SHORT
                        ).show()
                    }
                }


                writeReviewButton.setOnClickListener {
                    when {
                        job.isFrozenByCompany -> {
                            android.widget.Toast.makeText(
                                itemView.context,
                                "You are frozen by ${job.frozenByCompanyName ?: "this company"}. You cannot write reviews.",
                                android.widget.Toast.LENGTH_LONG
                            ).show()
                        }
                        job.jobStatus == "closed" -> {
                            android.widget.Toast.makeText(
                                itemView.context,
                                "This job has been closed. Review functionality is not available.",
                                android.widget.Toast.LENGTH_LONG
                            ).show()
                        }
                        else -> {
                            onWriteReview?.invoke(job) ?: run {

                                android.widget.Toast.makeText(
                                    itemView.context,
                                    "Write review for ${job.companyName}",
                                    android.widget.Toast.LENGTH_SHORT
                                ).show()
                            }
                        }
                    }
                }


                when {
                    job.isFrozenByCompany -> {

                        manageAttendanceButton.isEnabled = false
                        manageAttendanceButton.text = "FROZEN BY COMPANY"
                        manageAttendanceButton.alpha = 0.6f
                        manageAttendanceButton.setBackgroundColor(android.graphics.Color.parseColor("#F44336"))


                        writeReviewButton.isEnabled = false
                        writeReviewButton.alpha = 0.6f
                        writeReviewButton.text = "FROZEN"
                    }
                    isAttendanceDisabled -> {

                        manageAttendanceButton.isEnabled = false
                        manageAttendanceButton.text = "DISABLED"
                        manageAttendanceButton.alpha = 0.6f


                        writeReviewButton.isEnabled = true
                        writeReviewButton.alpha = 1.0f
                        writeReviewButton.text = "REVIEW"
                    }
                    else -> {

                        manageAttendanceButton.isEnabled = true
                        manageAttendanceButton.text = "ATTENDANCE"
                        manageAttendanceButton.alpha = 1.0f
                        manageAttendanceButton.setBackgroundColor(android.graphics.Color.parseColor("#4CAF50"))


                        writeReviewButton.isEnabled = true
                        writeReviewButton.alpha = 1.0f
                        writeReviewButton.text = "REVIEW"
                    }
                }


                val animationDrawable = blueCircleIndicator.background as? AnimationDrawable
                animationDrawable?.start()


                root.setOnClickListener {
                    onJobClick(job)
                }
            }
        }
    }
}