package com.uilover.project196.Fragment

import android.os.Bundle
import androidx.fragment.app.Fragment
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.ImageView
import android.widget.TextView
import com.bumptech.glide.Glide
import com.uilover.project196.R


// KRITERIA WAJIB: Multiple Fragment (14/16) - Fragment profil perusahaan
class CompanyFragment : Fragment() {

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
    }

    override fun onCreateView(
        inflater: LayoutInflater, container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View? {

        return inflater.inflate(R.layout.fragment_company, container, false)
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)


        setupCompanyInfo(view)
    }

    private fun setupCompanyInfo(view: View) {

        val companyName = arguments?.getString("company") ?: "Company Name"
        val picUrl = arguments?.getString("picUrl") ?: "logo1"


        val companyLogo = view.findViewById<ImageView>(R.id.companyLogo)
        companyLogo?.let {
            val drawableResourceId = resources.getIdentifier(picUrl, "drawable", requireContext().packageName)
            Glide.with(this)
                .load(drawableResourceId)
                .into(it)
        }


        val companyNameTxt = view.findViewById<TextView>(R.id.companyNameTxt)
        companyNameTxt?.text = companyName
    }



}