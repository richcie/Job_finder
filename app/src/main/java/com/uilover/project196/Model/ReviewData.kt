package com.uilover.project196.Model

data class ReviewData(
    val overallRating: Double = 0.0,
    val totalReviews: Int = 0,
    val fiveStarCount: Int = 0,
    val fourStarCount: Int = 0,
    val threeStarCount: Int = 0,
    val twoStarCount: Int = 0,
    val oneStarCount: Int = 0,
    val recentReviews: List<ReviewEntity> = emptyList()
) {

    fun getFormattedRating(): String {
        return if (overallRating > 0) {
            String.format("%.1f", overallRating)
        } else {
            "0.0"
        }
    }

    fun getReviewCountText(): String {
        return when {
            totalReviews == 0 -> "No reviews yet"
            totalReviews == 1 -> "Based on 1 review"
            else -> "Based on $totalReviews reviews"
        }
    }

    fun getStarPercentage(starCount: Int): Float {
        return if (totalReviews > 0) {
            (starCount.toFloat() / totalReviews.toFloat())
        } else {
            0f
        }
    }
}