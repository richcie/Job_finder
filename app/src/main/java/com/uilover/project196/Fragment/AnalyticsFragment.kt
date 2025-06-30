package com.uilover.project196.Fragment

import android.os.Bundle
import androidx.fragment.app.Fragment
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.*
import androidx.appcompat.app.AlertDialog
import androidx.cardview.widget.CardView
import androidx.lifecycle.ViewModelProvider
import androidx.lifecycle.lifecycleScope
import com.bumptech.glide.Glide
import com.uilover.project196.Database.AppDatabase
import com.uilover.project196.Model.ReviewEntity
import com.uilover.project196.R
import com.uilover.project196.Repository.UserRepository
import com.uilover.project196.Repository.ReviewRepository
import com.uilover.project196.Utils.UserSession
import com.uilover.project196.ViewModel.MainViewModel
import com.uilover.project196.databinding.FragmentAnalyticsBinding
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext

// KRITERIA WAJIB: Multiple Fragment (10/16) - Fragment analitik
// KRITERIA KOMPLEKSITAS: Advanced analytics dan reporting
class AnalyticsFragment : Fragment() {

    private var _binding: FragmentAnalyticsBinding? = null
    private val binding get() = _binding!!
    private lateinit var userRepository: UserRepository
    private lateinit var reviewRepository: ReviewRepository
    private lateinit var mainViewModel: MainViewModel
    private var currentBusinessOwnerId: String? = null

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
    }

    override fun onCreateView(
        inflater: LayoutInflater, container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View {
        _binding = FragmentAnalyticsBinding.inflate(inflater, container, false)
        return binding.root
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)


        UserSession.init(requireContext())
        userRepository = UserRepository.getInstance(requireContext())
        reviewRepository = ReviewRepository.getInstance(requireContext())
        mainViewModel = ViewModelProvider(requireActivity())[MainViewModel::class.java]
        mainViewModel.initializeDatabase(requireContext())


        setupSeeAllRatingButton()


        loadJobAnalytics()
    }

    private fun setupSeeAllRatingButton() {
        binding.seeAllRatingBtn.setOnClickListener {
            showAllRatingsDialog()
        }
    }

    private fun loadJobAnalytics() {

        activity?.let { activity ->
            if (activity is com.uilover.project196.Activity.DetailActivity) {
                val currentJob = activity.item

                lifecycleScope.launch {
                    try {

                        val analytics = userRepository.getJobSpecificAnalytics(currentJob)
                        updateJobAnalyticsUI(analytics.uniqueViews, analytics.acceptedFreelancers)


                        currentBusinessOwnerId = currentJob.ownerId


                        loadBusinessOwnerRatings(currentJob.ownerId)

                    } catch (e: Exception) {
                        android.util.Log.e("AnalyticsFragment", "Error loading analytics", e)
                        showDefaultValues()
                    }
                }
            } else {
                showDefaultValues()
            }
        }
    }

    private fun updateJobAnalyticsUI(uniqueViews: Int, acceptedFreelancers: Int) {

        binding.jobViewsCount.text = uniqueViews.toString()
        binding.applicationsCount.text = acceptedFreelancers.toString()
    }

    private suspend fun loadBusinessOwnerRatings(businessOwnerId: String?) {
        if (businessOwnerId.isNullOrEmpty()) {
            android.util.Log.w("AnalyticsFragment", "No business owner ID provided for rating analytics")
            showDefaultRatingValues()
            return
        }

        try {
            android.util.Log.d("AnalyticsFragment", "Loading rating analytics for business owner: $businessOwnerId")


            reviewRepository.initializeDummyReviews(businessOwnerId)


            val reviewData = reviewRepository.getReviewDataForBusiness(businessOwnerId)

            android.util.Log.d("AnalyticsFragment", "Rating data loaded - Rating: ${reviewData.overallRating}, Reviews: ${reviewData.totalReviews}")


            updateRatingAnalyticsUI(reviewData)

        } catch (e: Exception) {
            android.util.Log.e("AnalyticsFragment", "Error loading business owner ratings", e)
            showDefaultRatingValues()
        }
    }

    private fun updateRatingAnalyticsUI(reviewData: com.uilover.project196.Model.ReviewData) {

        binding.overallRatingTxt.text = reviewData.getFormattedRating()
        binding.totalReviewsTxt.text = reviewData.getReviewCountText()


        binding.fiveStarCount.text = reviewData.fiveStarCount.toString()
        binding.fourStarCount.text = reviewData.fourStarCount.toString()
        binding.threeStarCount.text = reviewData.threeStarCount.toString()
        binding.twoStarCount.text = reviewData.twoStarCount.toString()
        binding.oneStarCount.text = reviewData.oneStarCount.toString()


        binding.fiveStarProgress.progress = (reviewData.getStarPercentage(reviewData.fiveStarCount) * 100).toInt()
        binding.fourStarProgress.progress = (reviewData.getStarPercentage(reviewData.fourStarCount) * 100).toInt()
        binding.threeStarProgress.progress = (reviewData.getStarPercentage(reviewData.threeStarCount) * 100).toInt()
        binding.twoStarProgress.progress = (reviewData.getStarPercentage(reviewData.twoStarCount) * 100).toInt()
        binding.oneStarProgress.progress = (reviewData.getStarPercentage(reviewData.oneStarCount) * 100).toInt()

        android.util.Log.d("AnalyticsFragment", "Rating UI updated successfully")
    }

    private fun showAllRatingsDialog() {
        val businessId = currentBusinessOwnerId ?: return

        lifecycleScope.launch {
            try {
                val allReviews = withContext(Dispatchers.IO) {
                    reviewRepository.getAllReviewsForBusiness(businessId)
                }

                if (allReviews.isEmpty()) {
                    Toast.makeText(requireContext(), "No reviews available", Toast.LENGTH_SHORT).show()
                    return@launch
                }


                val dialogMainContainer = LinearLayout(requireContext()).apply {
                    orientation = LinearLayout.VERTICAL
                    layoutParams = ViewGroup.LayoutParams(
                        ViewGroup.LayoutParams.MATCH_PARENT,
                        ViewGroup.LayoutParams.WRAP_CONTENT
                    )
                    setBackgroundResource(R.drawable.white_bg)
                }


                val headerContainer = LinearLayout(requireContext()).apply {
                    orientation = LinearLayout.VERTICAL
                    layoutParams = LinearLayout.LayoutParams(
                        LinearLayout.LayoutParams.MATCH_PARENT,
                        LinearLayout.LayoutParams.WRAP_CONTENT
                    )
                    setPadding(48, 48, 48, 24)
                    setBackgroundResource(R.drawable.purple_bg)
                }


                val headerTopRow = LinearLayout(requireContext()).apply {
                    orientation = LinearLayout.HORIZONTAL
                    layoutParams = LinearLayout.LayoutParams(
                        LinearLayout.LayoutParams.MATCH_PARENT,
                        LinearLayout.LayoutParams.WRAP_CONTENT
                    ).apply {
                        setMargins(0, 0, 0, 8)
                    }
                    gravity = android.view.Gravity.CENTER_VERTICAL
                }

                val headerTitle = TextView(requireContext()).apply {
                    text = "All Company Reviews"
                    setTextColor(resources.getColor(R.color.white, null))
                    textSize = 20f
                    setTypeface(null, android.graphics.Typeface.BOLD)
                    layoutParams = LinearLayout.LayoutParams(
                        0,
                        LinearLayout.LayoutParams.WRAP_CONTENT,
                        1f
                    )
                }

                val closeButton = TextView(requireContext()).apply {
                    layoutParams = LinearLayout.LayoutParams(64, 64)
                    text = "✕"
                    setTextColor(resources.getColor(R.color.white, null))
                    textSize = 16f
                    setTypeface(null, android.graphics.Typeface.BOLD)
                    gravity = android.view.Gravity.CENTER
                    setPadding(8, 8, 8, 8)
                    background = android.graphics.drawable.RippleDrawable(
                        android.content.res.ColorStateList.valueOf(resources.getColor(R.color.white, null)),
                        null,
                        null
                    )
                    isClickable = true
                    isFocusable = true
                }

                headerTopRow.addView(headerTitle)
                headerTopRow.addView(closeButton)

                val headerSubtitle = TextView(requireContext()).apply {
                    text = "${allReviews.size} reviews • ${getAverageRatingText(allReviews)} average rating"
                    setTextColor(resources.getColor(R.color.white, null))
                    textSize = 14f
                    alpha = 0.9f
                    layoutParams = LinearLayout.LayoutParams(
                        LinearLayout.LayoutParams.MATCH_PARENT,
                        LinearLayout.LayoutParams.WRAP_CONTENT
                    )
                }

                headerContainer.addView(headerTopRow)
                headerContainer.addView(headerSubtitle)


                val scrollView = ScrollView(requireContext()).apply {
                    layoutParams = LinearLayout.LayoutParams(
                        LinearLayout.LayoutParams.MATCH_PARENT,
                        resources.displayMetrics.heightPixels * 2 / 3
                    )
                    setBackgroundResource(R.drawable.grey_bg)
                }


                val reviewsContainer = LinearLayout(requireContext()).apply {
                    orientation = LinearLayout.VERTICAL
                    setPadding(24, 24, 24, 24)
                }


                val sortedAllReviews = allReviews.sortedWith(
                    compareByDescending<ReviewEntity> { it.rating }
                        .thenByDescending { it.timestamp }
                )


                val companyLogos = mutableMapOf<String, String>()
                val uniqueBusinessOwnerIds = sortedAllReviews.map { it.businessOwnerId }.distinct()
                for (businessOwnerId in uniqueBusinessOwnerIds) {
                    companyLogos[businessOwnerId] = getBusinessOwnerCompanyLogo(businessOwnerId)
                }


                for (review in sortedAllReviews) {
                    val companyLogo = companyLogos[review.businessOwnerId] ?: "logo1"
                    val reviewView = createReviewView(review, companyLogo)
                    reviewsContainer.addView(reviewView)
                }

                scrollView.addView(reviewsContainer)


                dialogMainContainer.addView(headerContainer)
                dialogMainContainer.addView(scrollView)


                val dialog = AlertDialog.Builder(requireContext())
                    .setView(dialogMainContainer)
                    .setPositiveButton("Close") { dialog, _ ->
                        dialog.dismiss()
                    }
                    .create()
                    .apply {
                        window?.setBackgroundDrawableResource(android.R.color.transparent)
                    }


                closeButton.setOnClickListener {
                    dialog.dismiss()
                }

                dialog.show()

            } catch (e: Exception) {
                android.util.Log.e("AnalyticsFragment", "Error loading all reviews", e)
                Toast.makeText(requireContext(), "Error loading reviews", Toast.LENGTH_SHORT).show()
            }
        }
    }

    private fun createReviewView(review: ReviewEntity, companyLogo: String = "logo1"): View {

        val cardView = CardView(requireContext()).apply {
            layoutParams = LinearLayout.LayoutParams(
                LinearLayout.LayoutParams.MATCH_PARENT,
                LinearLayout.LayoutParams.WRAP_CONTENT
            ).apply {
                setMargins(0, 0, 0, 24)
            }
            radius = 24f
            cardElevation = 8f
            setCardBackgroundColor(resources.getColor(R.color.white, null))
        }

        val reviewView = LinearLayout(requireContext()).apply {
            orientation = LinearLayout.VERTICAL
            layoutParams = LinearLayout.LayoutParams(
                LinearLayout.LayoutParams.MATCH_PARENT,
                LinearLayout.LayoutParams.WRAP_CONTENT
            )
            setPadding(48, 48, 48, 48)
        }


        val headerLayout = LinearLayout(requireContext()).apply {
            orientation = LinearLayout.HORIZONTAL
            layoutParams = LinearLayout.LayoutParams(
                LinearLayout.LayoutParams.MATCH_PARENT,
                LinearLayout.LayoutParams.WRAP_CONTENT
            ).apply {
                setMargins(0, 0, 0, 32)
            }
        }


        val profileImage = ImageView(requireContext()).apply {
            layoutParams = LinearLayout.LayoutParams(120, 120).apply {
                setMargins(0, 0, 48, 0)
            }
            setBackgroundResource(R.drawable.grey_full_corner_bg)
            setPadding(0, 0, 0, 0)
            scaleType = ImageView.ScaleType.CENTER_CROP


            val drawableResourceId = resources.getIdentifier(companyLogo, "drawable", requireContext().packageName)
            Glide.with(requireContext())
                .load(drawableResourceId)
                .centerCrop()
                .into(this)
        }


        val userInfoLayout = LinearLayout(requireContext()).apply {
            orientation = LinearLayout.VERTICAL
            layoutParams = LinearLayout.LayoutParams(
                0,
                LinearLayout.LayoutParams.WRAP_CONTENT,
                1f
            )
        }


        val nameTextView = TextView(requireContext()).apply {
            text = review.reviewerName
            setTextColor(resources.getColor(R.color.black, null))
            textSize = 16f
            setTypeface(null, android.graphics.Typeface.BOLD)
            layoutParams = LinearLayout.LayoutParams(
                LinearLayout.LayoutParams.MATCH_PARENT,
                LinearLayout.LayoutParams.WRAP_CONTENT
            ).apply {
                setMargins(0, 0, 0, 8)
            }
        }

        val titleTextView = TextView(requireContext()).apply {
            text = review.reviewerTitle
            setTextColor(resources.getColor(R.color.darkGrey, null))
            textSize = 14f
            layoutParams = LinearLayout.LayoutParams(
                LinearLayout.LayoutParams.MATCH_PARENT,
                LinearLayout.LayoutParams.WRAP_CONTENT
            ).apply {
                setMargins(0, 8, 0, 0)
            }
        }

        userInfoLayout.addView(nameTextView)
        userInfoLayout.addView(titleTextView)


        val ratingLayout = LinearLayout(requireContext()).apply {
            orientation = LinearLayout.VERTICAL
            layoutParams = LinearLayout.LayoutParams(
                LinearLayout.LayoutParams.WRAP_CONTENT,
                LinearLayout.LayoutParams.WRAP_CONTENT
            )
            gravity = android.view.Gravity.END
        }


        val ratingContainer = LinearLayout(requireContext()).apply {
            orientation = LinearLayout.HORIZONTAL
            layoutParams = LinearLayout.LayoutParams(
                LinearLayout.LayoutParams.WRAP_CONTENT,
                LinearLayout.LayoutParams.WRAP_CONTENT
            ).apply {
                setMargins(0, 0, 0, 8)
            }
            setBackgroundResource(getRatingBackgroundDrawable(review.rating))
            setPadding(12, 10, 12, 10)
            gravity = android.view.Gravity.CENTER_VERTICAL
        }

        val ratingTextView = TextView(requireContext()).apply {
            text = review.rating.toString()
            setTextColor(resources.getColor(R.color.white, null))
            textSize = 14f
            setTypeface(null, android.graphics.Typeface.BOLD)
            layoutParams = LinearLayout.LayoutParams(
                LinearLayout.LayoutParams.WRAP_CONTENT,
                LinearLayout.LayoutParams.WRAP_CONTENT
            ).apply {
                setMargins(0, 0, 8, 0)
            }
        }

        val starTextView = TextView(requireContext()).apply {
            text = "★"
            setTextColor(resources.getColor(R.color.white, null))
            textSize = 14f
        }

        ratingContainer.addView(ratingTextView)
        ratingContainer.addView(starTextView)

        val dateTextView = TextView(requireContext()).apply {
            text = formatDate(review.timestamp)
            setTextColor(resources.getColor(R.color.black, null))
            textSize = 12f
            setTypeface(null, android.graphics.Typeface.BOLD)
            alpha = 0.8f
            layoutParams = LinearLayout.LayoutParams(
                LinearLayout.LayoutParams.WRAP_CONTENT,
                LinearLayout.LayoutParams.WRAP_CONTENT
            )
        }

        ratingLayout.addView(ratingContainer)
        ratingLayout.addView(dateTextView)

        headerLayout.addView(profileImage)
        headerLayout.addView(userInfoLayout)
        headerLayout.addView(ratingLayout)


        val reviewTextView = TextView(requireContext()).apply {
            text = "\"${review.reviewText}\""
            setTextColor(resources.getColor(R.color.black, null))
            textSize = 15f
            setLineSpacing(8f, 1f)
            layoutParams = LinearLayout.LayoutParams(
                LinearLayout.LayoutParams.MATCH_PARENT,
                LinearLayout.LayoutParams.WRAP_CONTENT
            ).apply {
                setMargins(0, 16, 0, 0)
            }
            setBackgroundResource(R.drawable.grey_bg)
            setPadding(32, 24, 32, 24)
        }

        reviewView.addView(headerLayout)
        reviewView.addView(reviewTextView)

        cardView.addView(reviewView)
        return cardView
    }

    private fun formatDate(timestamp: Long): String {
        val now = System.currentTimeMillis()
        val diff = now - timestamp

        val minutes = diff / (60 * 1000)
        val hours = diff / (60 * 60 * 1000)
        val days = diff / (24 * 60 * 60 * 1000)
        val weeks = days / 7
        val months = days / 30
        val years = days / 365

        return when {
            minutes < 1 -> "Just now"
            hours < 1 -> "${minutes}m ago"
            days < 1 -> "${hours}h ago"
            days < 7 -> "${days} day${if (days != 1L) "s" else ""} ago"
            weeks < 4 -> "${weeks} week${if (weeks != 1L) "s" else ""} ago"
            months < 1 -> "${weeks} week${if (weeks != 1L) "s" else ""} ago"
            months < 12 -> "${months} month${if (months != 1L) "s" else ""} ago"
            else -> "${years} year${if (years != 1L) "s" else ""} ago"
        }
    }

    private fun getAverageRatingText(reviews: List<ReviewEntity>): String {
        if (reviews.isEmpty()) return "0.0"
        val average = reviews.map { it.rating }.average()
        return String.format("%.1f", average)
    }

    private fun getRatingBackgroundDrawable(rating: Int): Int {
        return when (rating) {
            5 -> R.drawable.green_badge_bg
            4 -> R.drawable.orange_badge_bg
            3 -> R.drawable.yellow_badge_bg
            2 -> R.drawable.orange_badge_bg
            1 -> R.drawable.red_badge_bg
            else -> R.drawable.grey_badge_bg
        }
    }

    private fun getCompanyLogo(companyName: String): String {
        return when (companyName) {
            "ChabokSoft", "Chaboksoft" -> "logo1"
            "KianSoft" -> "logo2"
            "MakanSoft" -> "logo3"
            "TestSoft" -> "logo4"
            else -> "logo1"
        }
    }

    private suspend fun getBusinessOwnerCompanyLogo(businessOwnerId: String): String {
        return withContext(Dispatchers.IO) {
            try {
                val database = AppDatabase.getDatabase(requireContext())
                val userDao = database.userDao()
                val businessOwner = userDao.getUserById(businessOwnerId)
                val companyName = businessOwner?.companyName ?: "ChabokSoft"
                getCompanyLogo(companyName)
            } catch (e: Exception) {
                android.util.Log.e("AnalyticsFragment", "Error getting business owner company logo", e)
                "logo1"
            }
        }
    }

    private fun showDefaultValues() {
        updateJobAnalyticsUI(0, 0)
        showDefaultRatingValues()
    }

    private fun showDefaultRatingValues() {

        binding.overallRatingTxt.text = "0.0"
        binding.totalReviewsTxt.text = "No reviews yet"


        binding.fiveStarCount.text = "0"
        binding.fourStarCount.text = "0"
        binding.threeStarCount.text = "0"
        binding.twoStarCount.text = "0"
        binding.oneStarCount.text = "0"


        binding.fiveStarProgress.progress = 0
        binding.fourStarProgress.progress = 0
        binding.threeStarProgress.progress = 0
        binding.twoStarProgress.progress = 0
        binding.oneStarProgress.progress = 0
    }

    override fun onDestroyView() {
        super.onDestroyView()
        _binding = null
    }
}