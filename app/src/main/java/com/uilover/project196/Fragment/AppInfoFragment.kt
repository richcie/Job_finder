package com.uilover.project196.Fragment

import android.os.Bundle
import androidx.fragment.app.Fragment
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import com.uilover.project196.R

// Fragment untuk menampilkan informasi tentang aplikasi Connecting Opportunities
class AppInfoFragment : Fragment() {

    override fun onCreateView(
        inflater: LayoutInflater, container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View? {
        // Inflate the layout for this fragment
        return inflater.inflate(R.layout.fragment_app_info, container, false)
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)
        
        // Set title for the activity/toolbar if needed
        requireActivity().title = "About Connecting Opportunities"
        
        // Setup back button functionality
        val backButton = view.findViewById<android.widget.ImageView>(com.uilover.project196.R.id.backButton)
        backButton?.setOnClickListener {
            // Navigate back to previous fragment/activity
            if (parentFragmentManager.backStackEntryCount > 0) {
                parentFragmentManager.popBackStack()
            } else {
                requireActivity().finish()
            }
        }
    }
} 