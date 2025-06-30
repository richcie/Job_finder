package com.uilover.project196.Fragment

import android.os.Bundle
import androidx.fragment.app.Fragment
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.TextView
import androidx.lifecycle.lifecycleScope
import com.uilover.project196.Activity.DetailActivity
import com.uilover.project196.R
import com.uilover.project196.Repository.UserRepository
import com.uilover.project196.Utils.UserSession
import kotlinx.coroutines.launch
import kotlinx.coroutines.delay

// KRITERIA WAJIB: Multiple Fragment (15/16) - Fragment tentang aplikasi
class AboutFragment : Fragment() {
    private lateinit var userRepository: UserRepository
    private lateinit var freelancersCountText: TextView
    private lateinit var viewsCountText: TextView

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        userRepository = UserRepository.getInstance(requireContext())
    }

    override fun onCreateView(
        inflater: LayoutInflater, container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View? {

        return inflater.inflate(R.layout.fragment_about, container, false)
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)


        freelancersCountText = view.findViewById(R.id.freelancersCountText)
        viewsCountText = view.findViewById(R.id.viewsCountText)

        android.util.Log.d("AboutFragment", "onViewCreated: Views initialized")


        view.findViewById<android.widget.Button>(R.id.refreshStatsButton)?.setOnClickListener {
            android.util.Log.d("AboutFragment", "Refresh button clicked")

            android.widget.Toast.makeText(requireContext(), "Refreshing stats...", android.widget.Toast.LENGTH_SHORT).show()

            loadJobStatistics()
        }


        val description = arguments?.getString("description") ?: "No description available"
        val about = arguments?.getString("about") ?: ""


        val fullJobDescription = if (about.isNotEmpty() && about != description) {
            "$description\n\nAbout the role:\n$about"
        } else {
            description
        }


        val jobDescriptionText = view.findViewById<TextView>(R.id.jobDescriptionText)
        jobDescriptionText?.text = fullJobDescription


        loadJobStatistics()
    }

    override fun onResume() {
        super.onResume()

        loadJobStatistics()
    }

    private fun loadJobStatistics() {

        val activity = requireActivity() as? DetailActivity
        if (activity != null) {
            val job = activity.item

            android.util.Log.d("AboutFragment", "loadJobStatistics: Loading stats for job: ${job.title}, company: ${job.company}, ownerId: ${job.ownerId}")
            android.util.Log.d("AboutFragment", "loadJobStatistics: Current user: ${UserSession.getCurrentUserInfo()}")

            lifecycleScope.launch {
                try {
                    android.util.Log.d("AboutFragment", "loadJobStatistics: Getting analytics for job: ${job.title}")


                    val analytics = userRepository.getJobSpecificAnalytics(job)

                    android.util.Log.d("AboutFragment", "loadJobStatistics: Analytics result - uniqueViews=${analytics.uniqueViews}, acceptedFreelancers=${analytics.acceptedFreelancers}")


                    freelancersCountText.text = analytics.acceptedFreelancers.toString()
                    viewsCountText.text = analytics.uniqueViews.toString()

                    android.util.Log.d("AboutFragment", "loadJobStatistics: UI updated successfully - Views shown: ${analytics.uniqueViews}, Freelancers shown: ${analytics.acceptedFreelancers}")


                    android.util.Log.d("AboutFragment", "loadJobStatistics: Job views displayed: ${analytics.uniqueViews} (Current user: ${UserSession.getRoleDisplayName()})")

                } catch (e: Exception) {
                    android.util.Log.e("AboutFragment", "loadJobStatistics: Error loading stats", e)

                    freelancersCountText.text = "0"
                    viewsCountText.text = "0"
                }
            }
        } else {
            android.util.Log.e("AboutFragment", "loadJobStatistics: Activity is null or not DetailActivity")
        }
    }


    fun refreshJobStatistics() {
        android.util.Log.d("AboutFragment", "refreshJobStatistics: Called from DetailActivity")
        loadJobStatistics()
    }
}