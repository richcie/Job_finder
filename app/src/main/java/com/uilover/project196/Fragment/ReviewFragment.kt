package com.uilover.project196.Fragment

import android.os.Bundle
import androidx.fragment.app.Fragment
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.Toast
import android.widget.TextView
import android.widget.LinearLayout
import android.widget.ImageView
import android.widget.ScrollView
import androidx.cardview.widget.CardView
import androidx.lifecycle.lifecycleScope
import androidx.lifecycle.ViewModelProvider
import androidx.appcompat.app.AlertDialog
import com.bumptech.glide.Glide
import com.google.android.material.textfield.TextInputEditText
import com.google.android.material.button.MaterialButton
import com.uilover.project196.R
import com.uilover.project196.Utils.UserSession
import com.uilover.project196.Repository.ReviewRepository
import com.uilover.project196.Repository.FreelancerJobRepository
import com.uilover.project196.Model.ReviewData
import com.uilover.project196.Model.ReviewEntity
import com.uilover.project196.Model.UserEntity
import com.uilover.project196.Database.AppDatabase
import com.uilover.project196.ViewModel.ReviewViewModel
import com.uilover.project196.databinding.FragmentReviewBinding
import kotlinx.coroutines.launch
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import java.text.SimpleDateFormat
import java.util.*
import android.text.TextWatcher


// KRITERIA WAJIB: Multiple Fragment (11/16) - Fragment review dan rating
class ReviewFragment : Fragment() {




    private var _binding: FragmentReviewBinding? = null
    private val binding get() = _binding!!
    private lateinit var viewModel: ReviewViewModel

    private lateinit var reviewRepository: ReviewRepository
    private lateinit var freelancerJobRepository: FreelancerJobRepository
    private var businessOwnerId: String? = null

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        reviewRepository = ReviewRepository.getInstance(requireContext())
        freelancerJobRepository = FreelancerJobRepository.getInstance(requireContext())
    }

    override fun onCreateView(
        inflater: LayoutInflater,
        container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View {

        _binding = FragmentReviewBinding.inflate(inflater, container, false)
        return binding.root
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)


        UserSession.init(requireContext())


        viewModel = ViewModelProvider(
            this,
            ViewModelProvider.AndroidViewModelFactory.getInstance(requireActivity().application)
        )[ReviewViewModel::class.java]


        businessOwnerId = arguments?.getString("businessOwnerId")

        if (businessOwnerId.isNullOrEmpty()) {
            android.util.Log.w("ReviewFragment", "No businessOwnerId provided, cannot load review data")
            viewModel.onErrorMessageShown()
            return
        }

        android.util.Log.d("ReviewFragment", "Loading reviews for businessOwnerId: $businessOwnerId")




        setupReactiveBinding(viewModel)
        setupLiveDataObservers()
        setupClickListeners()


        viewModel.loadReviewData(businessOwnerId!!)
    }




    private fun setupReactiveBinding(viewModel: ReviewViewModel) {


        viewModel.overallRating.addOnPropertyChangedCallback(
            object : androidx.databinding.Observable.OnPropertyChangedCallback() {
                override fun onPropertyChanged(sender: androidx.databinding.Observable?, propertyId: Int) {
                    binding.overallRatingTxt.text = viewModel.overallRating.get() ?: "0.0"
                }
            }
        )


        viewModel.totalReviewsText.addOnPropertyChangedCallback(
            object : androidx.databinding.Observable.OnPropertyChangedCallback() {
                override fun onPropertyChanged(sender: androidx.databinding.Observable?, propertyId: Int) {
                    binding.totalReviewsTxt.text = viewModel.totalReviewsText.get() ?: "No reviews yet"
                }
            }
        )


        viewModel.fiveStarCount.addOnPropertyChangedCallback(
            object : androidx.databinding.Observable.OnPropertyChangedCallback() {
                override fun onPropertyChanged(sender: androidx.databinding.Observable?, propertyId: Int) {
                    binding.fiveStarCountTxt.text = viewModel.fiveStarCount.get() ?: "0"
                }
            }
        )

        viewModel.fourStarCount.addOnPropertyChangedCallback(
            object : androidx.databinding.Observable.OnPropertyChangedCallback() {
                override fun onPropertyChanged(sender: androidx.databinding.Observable?, propertyId: Int) {
                    binding.fourStarCountTxt.text = viewModel.fourStarCount.get() ?: "0"
                }
            }
        )

        viewModel.threeStarCount.addOnPropertyChangedCallback(
            object : androidx.databinding.Observable.OnPropertyChangedCallback() {
                override fun onPropertyChanged(sender: androidx.databinding.Observable?, propertyId: Int) {
                    binding.threeStarCountTxt.text = viewModel.threeStarCount.get() ?: "0"
                }
            }
        )

        viewModel.twoStarCount.addOnPropertyChangedCallback(
            object : androidx.databinding.Observable.OnPropertyChangedCallback() {
                override fun onPropertyChanged(sender: androidx.databinding.Observable?, propertyId: Int) {
                    binding.twoStarCountTxt.text = viewModel.twoStarCount.get() ?: "0"
                }
            }
        )

        viewModel.oneStarCount.addOnPropertyChangedCallback(
            object : androidx.databinding.Observable.OnPropertyChangedCallback() {
                override fun onPropertyChanged(sender: androidx.databinding.Observable?, propertyId: Int) {
                    binding.oneStarCountTxt.text = viewModel.oneStarCount.get() ?: "0"
                }
            }
        )


        viewModel.showLoadingState.addOnPropertyChangedCallback(
            object : androidx.databinding.Observable.OnPropertyChangedCallback() {
                override fun onPropertyChanged(sender: androidx.databinding.Observable?, propertyId: Int) {



                }
            }
        )

        viewModel.showContentState.addOnPropertyChangedCallback(
            object : androidx.databinding.Observable.OnPropertyChangedCallback() {
                override fun onPropertyChanged(sender: androidx.databinding.Observable?, propertyId: Int) {

                }
            }
        )

        viewModel.showEmptyState.addOnPropertyChangedCallback(
            object : androidx.databinding.Observable.OnPropertyChangedCallback() {
                override fun onPropertyChanged(sender: androidx.databinding.Observable?, propertyId: Int) {
                    val showEmpty = viewModel.showEmptyState.get() == true
                    if (showEmpty) {
                        showEmptyStateInContainer()
                    }
                }
            }
        )


        viewModel.showWriteReviewButton.addOnPropertyChangedCallback(
            object : androidx.databinding.Observable.OnPropertyChangedCallback() {
                override fun onPropertyChanged(sender: androidx.databinding.Observable?, propertyId: Int) {

                    view?.findViewById<CardView>(R.id.writeReviewButton)?.visibility =
                        if (viewModel.showWriteReviewButton.get() == true) View.VISIBLE else View.GONE
                }
            }
        )

        viewModel.writeReviewButtonAlpha.addOnPropertyChangedCallback(
            object : androidx.databinding.Observable.OnPropertyChangedCallback() {
                override fun onPropertyChanged(sender: androidx.databinding.Observable?, propertyId: Int) {
                    view?.findViewById<CardView>(R.id.writeReviewButton)?.alpha =
                        viewModel.writeReviewButtonAlpha.get() ?: 0.5f
                }
            }
        )
    }




    private fun setupLiveDataObservers() {


        viewModel.reviewData.observe(viewLifecycleOwner) { reviewData ->
            reviewData?.let {
                updateRatingBars(it)
            }
        }


        viewModel.recentReviews.observe(viewLifecycleOwner) { reviews ->
            reviews?.let {
                updateRecentReviewsContainer(it)
            }
        }


        viewModel.allReviews.observe(viewLifecycleOwner) { allReviews ->
            allReviews?.let {
                if (it.isNotEmpty()) {
                    showAllReviewsDialog(it)
                }
            }
        }


        viewModel.showSuccessMessage.observe(viewLifecycleOwner) { message ->
            message?.let {
                Toast.makeText(requireContext(), it, Toast.LENGTH_SHORT).show()
                viewModel.onSuccessMessageShown()
            }
        }


        viewModel.showErrorMessage.observe(viewLifecycleOwner) { message ->
            message?.let {
                Toast.makeText(requireContext(), it, Toast.LENGTH_SHORT).show()
                viewModel.onErrorMessageShown()
            }
        }


        viewModel.businessOwnerInfo.observe(viewLifecycleOwner) { _ ->
            // Business owner info observer - implement if UI updates needed
        }


        viewModel.canWriteReview.observe(viewLifecycleOwner) { _ ->
            // Can write review observer - implement if UI updates needed
        }
    }

    private fun setupClickListeners() {

        binding.viewAllReviewsBtn.setOnClickListener {
            viewModel.loadAllReviews()
        }


        view?.findViewById<CardView>(R.id.writeReviewButton)?.setOnClickListener {
            val canWrite = viewModel.canWriteReview.value
            if (canWrite?.first == true) {
                showWriteReviewDialog()
            } else {
                viewModel.onWriteReviewButtonClicked()
            }
        }
    }





    private fun showEmptyStateInContainer() {
        binding.recentReviewsContainer.removeAllViews()
        val emptyStateTitle = viewModel.emptyStateTitle.get() ?: "No Reviews Yet"
        val emptyStateSubtitle = viewModel.emptyStateSubtitle.get() ?: "Be the first to share your experience!"
        val noReviewsView = createNoReviewsView(emptyStateTitle, emptyStateSubtitle)
        binding.recentReviewsContainer.addView(noReviewsView)
    }

    private fun updateRecentReviewsContainer(reviews: List<ReviewEntity>) {
        binding.recentReviewsContainer.removeAllViews()

        if (reviews.isEmpty()) {
            showEmptyStateInContainer()
        } else {

            val sortedReviews = reviews.sortedWith(
                compareByDescending<ReviewEntity> { it.rating }
                    .thenByDescending { it.timestamp }
            )


            lifecycleScope.launch {

                val companyLogos = mutableMapOf<String, String>()
                val uniqueBusinessOwnerIds = sortedReviews.map { it.businessOwnerId }.distinct()
                for (businessOwnerId in uniqueBusinessOwnerIds) {
                    companyLogos[businessOwnerId] = getBusinessOwnerCompanyLogo(businessOwnerId)
                }

                for (review in sortedReviews) {
                    val companyLogo = companyLogos[review.businessOwnerId] ?: "logo1"
                    val reviewView = createReviewView(review, companyLogo)
                    binding.recentReviewsContainer.addView(reviewView)
                }
            }
        }
    }

    private fun updateRatingBars(reviewData: ReviewData) {

        val fiveStarPercentage = reviewData.getStarPercentage(reviewData.fiveStarCount)
        val fourStarPercentage = reviewData.getStarPercentage(reviewData.fourStarCount)
        val threeStarPercentage = reviewData.getStarPercentage(reviewData.threeStarCount)
        val twoStarPercentage = reviewData.getStarPercentage(reviewData.twoStarCount)
        val oneStarPercentage = reviewData.getStarPercentage(reviewData.oneStarCount)


        updateBarWeight(binding.fiveStarBar, fiveStarPercentage * 4)
        updateBarWeight(binding.fourStarBar, fourStarPercentage * 4)
        updateBarWeight(binding.threeStarBar, threeStarPercentage * 4)
        updateBarWeight(binding.twoStarBar, twoStarPercentage * 4)
        updateBarWeight(binding.oneStarBar, oneStarPercentage * 4)
    }

    private fun updateBarWeight(view: View?, weight: Float) {
        view?.let {
            val layoutParams = it.layoutParams as LinearLayout.LayoutParams
            layoutParams.weight = if (weight > 0) weight else 0.1f
            it.layoutParams = layoutParams
        }
    }

    private fun createNoReviewsView(
        title: String = "No Reviews Yet",
        subtitle: String = "Be the first to share your experience working with this company!"
    ): View {

        val cardView = androidx.cardview.widget.CardView(requireContext()).apply {
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

        val containerLayout = LinearLayout(requireContext()).apply {
            orientation = LinearLayout.VERTICAL
            layoutParams = LinearLayout.LayoutParams(
                LinearLayout.LayoutParams.MATCH_PARENT,
                LinearLayout.LayoutParams.WRAP_CONTENT
            )
            setPadding(48, 64, 48, 64)
        }


        val iconView = ImageView(requireContext()).apply {
            layoutParams = LinearLayout.LayoutParams(
                120, 120
            ).apply {
                setMargins(0, 0, 0, 32)
            }
            setImageResource(R.drawable.ic_bookmark_outline)
            setBackgroundResource(R.drawable.purple_bg)
            setPadding(24, 24, 24, 24)
            setColorFilter(resources.getColor(R.color.white, null))
            scaleType = ImageView.ScaleType.CENTER_INSIDE
            adjustViewBounds = false
        }


        val iconContainer = LinearLayout(requireContext()).apply {
            orientation = LinearLayout.HORIZONTAL
            layoutParams = LinearLayout.LayoutParams(
                LinearLayout.LayoutParams.MATCH_PARENT,
                LinearLayout.LayoutParams.WRAP_CONTENT
            )
            gravity = android.view.Gravity.CENTER
        }
        iconContainer.addView(iconView)


        val titleTextView = TextView(requireContext()).apply {
            text = title
            setTextColor(resources.getColor(R.color.black, null))
            textSize = 18f
            setTypeface(null, android.graphics.Typeface.BOLD)
            textAlignment = View.TEXT_ALIGNMENT_CENTER
            layoutParams = LinearLayout.LayoutParams(
                LinearLayout.LayoutParams.MATCH_PARENT,
                LinearLayout.LayoutParams.WRAP_CONTENT
            ).apply {
                setMargins(0, 0, 0, 16)
            }
        }


        val descriptionTextView = TextView(requireContext()).apply {
            text = subtitle
            setTextColor(resources.getColor(R.color.darkGrey, null))
            textSize = 14f
            textAlignment = View.TEXT_ALIGNMENT_CENTER
            setLineSpacing(4f, 1f)
            layoutParams = LinearLayout.LayoutParams(
                LinearLayout.LayoutParams.MATCH_PARENT,
                LinearLayout.LayoutParams.WRAP_CONTENT
            )
        }

        containerLayout.addView(iconContainer)
        containerLayout.addView(titleTextView)
        containerLayout.addView(descriptionTextView)

        cardView.addView(containerLayout)
        return cardView
    }

    private fun createReviewView(review: ReviewEntity, companyLogo: String = "logo1"): View {

        val cardView = androidx.cardview.widget.CardView(requireContext()).apply {
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
                android.util.Log.e("ReviewFragment", "Error getting business owner company logo", e)
                "logo1"
            }
        }
    }






    private fun showWriteReviewDialog() {
        val businessOwner = viewModel.businessOwnerInfo.value

        if (businessOwner == null) {
            Toast.makeText(requireContext(), "Error loading company information", Toast.LENGTH_SHORT).show()
            return
        }


        val dialogView = LayoutInflater.from(requireContext()).inflate(R.layout.dialog_write_review, null)


        val dialog = AlertDialog.Builder(requireContext())
            .setView(dialogView)
            .setCancelable(true)
            .create()

        dialog.window?.setBackgroundDrawableResource(android.R.color.transparent)


        setupReactiveReviewDialog(dialogView, businessOwner, dialog)

        dialog.show()


        dialogView.findViewById<ImageView>(R.id.closeButton)?.setOnClickListener {
            dialog.dismiss()
        }

        dialogView.findViewById<MaterialButton>(R.id.cancelButton)?.setOnClickListener {
            dialog.dismiss()
        }
    }


    private fun setupReactiveReviewDialog(dialogView: View, businessOwner: UserEntity, dialog: AlertDialog) {

        viewModel.selectedRating.set(0)
        viewModel.reviewText.set("")
        viewModel.ratingDescription.set("Tap to rate")
        viewModel.submitButtonEnabled.set(false)
        viewModel.reviewTextError.set("")
        viewModel.wordCount.set("0 words")
        viewModel.isReviewTextValid.set(false)


        val companyName = dialogView.findViewById<TextView>(R.id.companyName)
        val companyLogo = dialogView.findViewById<ImageView>(R.id.companyLogo)

        companyName?.text = businessOwner.companyName.ifEmpty { businessOwner.name }


        val logoName = viewModel.companyLogo.get() ?: "logo1"
        val drawableResourceId = resources.getIdentifier(logoName, "drawable", requireContext().packageName)
        if (drawableResourceId != 0) {
            Glide.with(requireContext())
                .load(drawableResourceId)
                .centerCrop()
                .into(companyLogo!!)
        }


        val stars = listOf(
            dialogView.findViewById<TextView>(R.id.star1),
            dialogView.findViewById<TextView>(R.id.star2),
            dialogView.findViewById<TextView>(R.id.star3),
            dialogView.findViewById<TextView>(R.id.star4),
            dialogView.findViewById<TextView>(R.id.star5)
        )

        val ratingDescription = dialogView.findViewById<TextView>(R.id.ratingDescription)
        val reviewDescriptionInput = dialogView.findViewById<TextInputEditText>(R.id.reviewDescription)
        val submitButton = dialogView.findViewById<MaterialButton>(R.id.submitButton)


        stars.forEachIndexed { index, star ->
            star?.setOnClickListener {
                viewModel.onStarRatingSelected(index + 1)
                updateStarDisplayFromViewModel(stars)
            }
        }


        viewModel.ratingDescription.addOnPropertyChangedCallback(
            object : androidx.databinding.Observable.OnPropertyChangedCallback() {
                override fun onPropertyChanged(sender: androidx.databinding.Observable?, propertyId: Int) {
                    ratingDescription?.text = viewModel.ratingDescription.get()
                }
            }
        )


        viewModel.submitButtonEnabled.addOnPropertyChangedCallback(
            object : androidx.databinding.Observable.OnPropertyChangedCallback() {
                override fun onPropertyChanged(sender: androidx.databinding.Observable?, propertyId: Int) {
                    submitButton?.isEnabled = viewModel.submitButtonEnabled.get() == true
                }
            }
        )


        reviewDescriptionInput?.addTextChangedListener(object : TextWatcher {
            override fun beforeTextChanged(s: CharSequence?, start: Int, count: Int, after: Int) {}
            override fun onTextChanged(s: CharSequence?, start: Int, before: Int, count: Int) {}
            override fun afterTextChanged(s: android.text.Editable?) {
                viewModel.onReviewTextChanged(s?.toString() ?: "")
            }
        })


        viewModel.onReviewTextChanged("")


        viewModel.reviewText.addOnPropertyChangedCallback(
            object : androidx.databinding.Observable.OnPropertyChangedCallback() {
                override fun onPropertyChanged(sender: androidx.databinding.Observable?, propertyId: Int) {
                    val text = viewModel.reviewText.get() ?: ""
                    if (reviewDescriptionInput?.text?.toString() != text) {
                        reviewDescriptionInput?.setText(text)
                    }
                }
            }
        )


        viewModel.reviewTextError.addOnPropertyChangedCallback(
            object : androidx.databinding.Observable.OnPropertyChangedCallback() {
                override fun onPropertyChanged(sender: androidx.databinding.Observable?, propertyId: Int) {
                    val errorMessage = viewModel.reviewTextError.get()
                    val textInputLayout = reviewDescriptionInput?.parent?.parent as? com.google.android.material.textfield.TextInputLayout
                    textInputLayout?.error = if (errorMessage.isNullOrEmpty()) null else errorMessage
                }
            }
        )


        viewModel.wordCount.addOnPropertyChangedCallback(
            object : androidx.databinding.Observable.OnPropertyChangedCallback() {
                override fun onPropertyChanged(sender: androidx.databinding.Observable?, propertyId: Int) {
                    val wordCountText = viewModel.wordCount.get() ?: "0 words"

                    val textInputLayout = reviewDescriptionInput?.parent?.parent as? com.google.android.material.textfield.TextInputLayout
                    textInputLayout?.helperText = "$wordCountText (minimum 10 words required)"
                }
            }
        )


        submitButton?.setOnClickListener {
            viewModel.submitReview()
            dialog.dismiss()
        }
    }


    private fun updateStarDisplayFromViewModel(stars: List<TextView?>) {
        val rating = viewModel.selectedRating.get() ?: 0
        stars.forEachIndexed { index, star ->
            if (index < rating) {
                star?.setTextColor(resources.getColor(R.color.yellow, null))
            } else {
                star?.setTextColor(resources.getColor(R.color.darkGrey, null))
            }
        }
    }





    override fun onDestroyView() {
        super.onDestroyView()
        _binding = null
    }

    private fun showAllReviewsDialog(allReviews: List<ReviewEntity>) {
        if (allReviews.isEmpty()) {
            Toast.makeText(requireContext(), "No reviews available", Toast.LENGTH_SHORT).show()
            return
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
                    text = "All Reviews"
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


                lifecycleScope.launch {
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
    }




}