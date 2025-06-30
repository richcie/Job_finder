package com.uilover.project196.Adapter

import android.graphics.Color
import android.graphics.drawable.AnimationDrawable
import android.view.LayoutInflater
import android.view.ViewGroup
import androidx.recyclerview.widget.RecyclerView
import com.uilover.project196.Model.CandidateProgressModel
import com.uilover.project196.databinding.ViewholderCandidateProgressBinding

// KRITERIA WAJIB: RecyclerView + Adapter (5/9) - Adapter untuk progress kandidat
class CandidateProgressAdapter(
    private var candidatesProgress: MutableList<CandidateProgressModel>,
    private val clickListener: ClickListener
) : RecyclerView.Adapter<CandidateProgressAdapter.CandidateProgressViewHolder>() {

    interface ClickListener {
        fun onCandidateProgressClick(candidateProgress: CandidateProgressModel)
    }

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): CandidateProgressViewHolder {
        val binding = ViewholderCandidateProgressBinding.inflate(
            LayoutInflater.from(parent.context),
            parent,
            false
        )
        return CandidateProgressViewHolder(binding)
    }

    override fun onBindViewHolder(holder: CandidateProgressViewHolder, position: Int) {
        holder.bind(candidatesProgress[position])
    }

    override fun getItemCount(): Int = candidatesProgress.size

    fun updateData(newCandidatesProgress: List<CandidateProgressModel>) {
        android.util.Log.d("CandidateProgressAdapter", "updateData called with ${newCandidatesProgress.size} candidates")


        newCandidatesProgress.forEach { candidate ->
            if (candidate.totalHours == 0.0 && candidate.attendedDays == 0) {
                android.util.Log.d("CandidateProgressAdapter", "Candidate ${candidate.candidateName} has fresh progress data (recently accepted)")
            }
        }

        candidatesProgress.clear()
        candidatesProgress.addAll(newCandidatesProgress)
        android.util.Log.d("CandidateProgressAdapter", "Adapter now has ${candidatesProgress.size} candidates")
        notifyDataSetChanged()
    }

    inner class CandidateProgressViewHolder(
        private val binding: ViewholderCandidateProgressBinding
    ) : RecyclerView.ViewHolder(binding.root) {

        fun bind(candidateProgress: CandidateProgressModel) {
            binding.apply {

                candidateName.text = candidateProgress.candidateName
                candidateRole.text = candidateProgress.candidateRole
                android.util.Log.d("CandidateProgressAdapter", "=== SETTING CANDIDATE ROLE ===")
                android.util.Log.d("CandidateProgressAdapter", "Candidate: ${candidateProgress.candidateName}")
                android.util.Log.d("CandidateProgressAdapter", "Setting candidateRole.text to: '${candidateProgress.candidateRole}'")
                android.util.Log.d("CandidateProgressAdapter", "This should display as purple 'Senior Full Stack' text")


                hiredDate.text = "Hired: ${candidateProgress.getFormattedHiredDate()}"


                if (candidateProgress.totalHours == 0.0 && candidateProgress.attendedDays == 0) {

                    workingSummary.text = "0/${candidateProgress.totalWorkDays} days (Fresh Start)"
                    completionRate.text = "0% (Just Started)"
                    completionRate.setTextColor(Color.parseColor("#4CAF50"))
                    android.util.Log.d("CandidateProgressAdapter", "Displaying fresh start data for ${candidateProgress.candidateName}")
                } else {

                    workingSummary.text = candidateProgress.getWorkingSummary()
                    completionRate.text = candidateProgress.getCompletionRateDisplay()


                    try {
                        completionRate.setTextColor(Color.parseColor(candidateProgress.getCompletionRateColor()))
                    } catch (e: Exception) {
                        completionRate.setTextColor(Color.parseColor("#858585"))
                    }
                }


                currentStatus.text = candidateProgress.getCurrentStatusDisplay()


                currentStatus.setBackgroundColor(Color.TRANSPARENT)
                try {
                    currentStatus.setTextColor(Color.parseColor(candidateProgress.getCurrentStatusColor()))
                } catch (e: Exception) {
                    currentStatus.setTextColor(Color.parseColor("#858585"))
                }


                if (candidateProgress.currentStatus == "frozen") {
                    frozenStatusBadge.visibility = android.view.View.VISIBLE
                } else {
                    frozenStatusBadge.visibility = android.view.View.GONE
                }


                val lastActivity = when {
                    candidateProgress.totalHours == 0.0 && candidateProgress.attendedDays == 0 ->
                        "Fresh start - Ready to begin work"
                    candidateProgress.lastCheckOut != null ->
                        "Last check-out: ${candidateProgress.getFormattedLastCheckOut()}"
                    candidateProgress.lastCheckIn != null ->
                        "Last check-in: ${candidateProgress.getFormattedLastCheckIn()}"
                    else -> "No activity yet"
                }
                lastActivityText.text = lastActivity


                if (candidateProgress.totalHours == 0.0 && candidateProgress.attendedDays == 0) {
                    totalHours.text = "0.0 hours (Fresh Start)"
                    averageHours.text = "Ready to begin"
                } else {
                    totalHours.text = String.format("%.2f hours total", candidateProgress.totalHours)
                    averageHours.text = String.format("%.2f avg/day", candidateProgress.averageHoursPerDay)
                }


                val animationDrawable = blueCircleIndicator.background as? AnimationDrawable
                animationDrawable?.start()


                root.setOnClickListener {
                    clickListener.onCandidateProgressClick(candidateProgress)
                }
            }
        }
    }
}