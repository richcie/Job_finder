package com.uilover.project196.Repository

import android.content.Context
import com.uilover.project196.Database.AppDatabase
import com.uilover.project196.Database.ReviewDao
import com.uilover.project196.Model.ReviewEntity
import com.uilover.project196.Model.ReviewData
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext

// KRITERIA KOMPLEKSITAS: Repository Pattern untuk sistem review (6/6)
class ReviewRepository private constructor(private val reviewDao: ReviewDao) {

    companion object {
        @Volatile
        private var INSTANCE: ReviewRepository? = null

        fun getInstance(context: Context): ReviewRepository {
            return INSTANCE ?: synchronized(this) {
                val database = AppDatabase.getDatabase(context)
                val instance = ReviewRepository(database.reviewDao())
                INSTANCE = instance
                instance
            }
        }
    }

    suspend fun getReviewDataForBusiness(businessOwnerId: String): ReviewData {
        return withContext(Dispatchers.IO) {
            try {
                val averageRating = reviewDao.getAverageRating(businessOwnerId) ?: 0.0
                val totalReviews = reviewDao.getTotalReviewCount(businessOwnerId)

                val fiveStarCount = reviewDao.getCountByRating(businessOwnerId, 5)
                val fourStarCount = reviewDao.getCountByRating(businessOwnerId, 4)
                val threeStarCount = reviewDao.getCountByRating(businessOwnerId, 3)
                val twoStarCount = reviewDao.getCountByRating(businessOwnerId, 2)
                val oneStarCount = reviewDao.getCountByRating(businessOwnerId, 1)

                val recentReviews = reviewDao.getRecentReviewsForBusiness(businessOwnerId, 3)

                ReviewData(
                    overallRating = averageRating,
                    totalReviews = totalReviews,
                    fiveStarCount = fiveStarCount,
                    fourStarCount = fourStarCount,
                    threeStarCount = threeStarCount,
                    twoStarCount = twoStarCount,
                    oneStarCount = oneStarCount,
                    recentReviews = recentReviews
                )
            } catch (e: Exception) {
                android.util.Log.e("ReviewRepository", "Error getting review data for business $businessOwnerId", e)
                ReviewData()
            }
        }
    }

    suspend fun addReview(review: ReviewEntity): Boolean {
        return withContext(Dispatchers.IO) {
            try {
                reviewDao.insertReview(review)
                true
            } catch (e: Exception) {
                android.util.Log.e("ReviewRepository", "Error adding review", e)
                false
            }
        }
    }

    suspend fun getUserReviewForBusiness(reviewerId: String, businessOwnerId: String): ReviewEntity? {
        return withContext(Dispatchers.IO) {
            try {
                reviewDao.getUserReviewForBusiness(reviewerId, businessOwnerId)
            } catch (e: Exception) {
                android.util.Log.e("ReviewRepository", "Error getting user review", e)
                null
            }
        }
    }

    suspend fun getAllReviewsForBusiness(businessOwnerId: String): List<ReviewEntity> {
        return withContext(Dispatchers.IO) {
            try {
                reviewDao.getReviewsForBusiness(businessOwnerId)
            } catch (e: Exception) {
                android.util.Log.e("ReviewRepository", "Error getting all reviews for business $businessOwnerId", e)
                emptyList()
            }
        }
    }

    suspend fun initializeDummyReviews(businessOwnerId: String) {
        withContext(Dispatchers.IO) {
            try {

                val existingReviews = reviewDao.getTotalReviewCount(businessOwnerId)
                if (existingReviews > 0) {
                    android.util.Log.d("ReviewRepository", "Reviews already exist for business $businessOwnerId, skipping initialization")
                    return@withContext
                }


                if (businessOwnerId != ReviewEntity.CHABOKSOFT_BUSINESS_OWNER_ID) {
                    android.util.Log.d("ReviewRepository", "Dummy reviews are only created for Chaboksoft (businessOwnerId: ${ReviewEntity.CHABOKSOFT_BUSINESS_OWNER_ID}), skipping for $businessOwnerId")
                    return@withContext
                }

                android.util.Log.d("ReviewRepository", "Initializing dummy reviews for Chaboksoft (businessOwnerId: $businessOwnerId)")

                val dummyReviews = listOf(
                    ReviewEntity(
                        businessOwnerId = ReviewEntity.CHABOKSOFT_BUSINESS_OWNER_ID,
                        reviewerId = "reviewer_001",
                        reviewerName = "Sarah M.",
                        reviewerTitle = "Software Engineer",
                        reviewerExperience = "2 years",
                        rating = 5,
                        reviewText = "Great company culture and excellent work-life balance. Management is supportive and there are plenty of opportunities for growth.",
                        timestamp = System.currentTimeMillis() - (14L * 24L * 60L * 60L * 1000L),
                        isVerified = true,
                        companyName = ReviewEntity.CHABOKSOFT_COMPANY_NAME
                    ),
                    ReviewEntity(
                        businessOwnerId = ReviewEntity.CHABOKSOFT_BUSINESS_OWNER_ID,
                        reviewerId = "reviewer_002",
                        reviewerName = "Mike T.",
                        reviewerTitle = "UI Designer",
                        reviewerExperience = "1.5 years",
                        rating = 4,
                        reviewText = "Really enjoy working here. The team is collaborative and the projects are interesting. Benefits package is competitive.",
                        timestamp = System.currentTimeMillis() - (30L * 24L * 60L * 60L * 1000L),
                        isVerified = true,
                        companyName = ReviewEntity.CHABOKSOFT_COMPANY_NAME
                    ),
                    ReviewEntity(
                        businessOwnerId = ReviewEntity.CHABOKSOFT_BUSINESS_OWNER_ID,
                        reviewerId = "reviewer_003",
                        reviewerName = "Jennifer L.",
                        reviewerTitle = "Product Manager",
                        reviewerExperience = "3 years",
                        rating = 5,
                        reviewText = "Amazing place to grow your career. Leadership invests in employee development and the remote work policy is very flexible.",
                        timestamp = System.currentTimeMillis() - (21L * 24L * 60L * 60L * 1000L),
                        isVerified = true,
                        companyName = ReviewEntity.CHABOKSOFT_COMPANY_NAME
                    ),
                    ReviewEntity(
                        businessOwnerId = ReviewEntity.CHABOKSOFT_BUSINESS_OWNER_ID,
                        reviewerId = "reviewer_004",
                        reviewerName = "David R.",
                        reviewerTitle = "Backend Developer",
                        reviewerExperience = "4 years",
                        rating = 5,
                        reviewText = "Outstanding technical culture and cutting-edge projects. Great mentorship opportunities and fair compensation.",
                        timestamp = System.currentTimeMillis() - (7L * 24L * 60L * 60L * 1000L),
                        isVerified = true,
                        companyName = ReviewEntity.CHABOKSOFT_COMPANY_NAME
                    ),
                    ReviewEntity(
                        businessOwnerId = ReviewEntity.CHABOKSOFT_BUSINESS_OWNER_ID,
                        reviewerId = "reviewer_005",
                        reviewerName = "Lisa K.",
                        reviewerTitle = "QA Engineer",
                        reviewerExperience = "2.5 years",
                        rating = 4,
                        reviewText = "Good work environment with solid processes. Room for improvement in some areas but overall positive experience.",
                        timestamp = System.currentTimeMillis() - (45L * 24L * 60L * 60L * 1000L),
                        isVerified = true,
                        companyName = ReviewEntity.CHABOKSOFT_COMPANY_NAME
                    ),
                    ReviewEntity(
                        businessOwnerId = ReviewEntity.CHABOKSOFT_BUSINESS_OWNER_ID,
                        reviewerId = "reviewer_006",
                        reviewerName = "Alex W.",
                        reviewerTitle = "DevOps Engineer",
                        reviewerExperience = "3.5 years",
                        rating = 5,
                        reviewText = "Innovative company with modern technology stack. Great team collaboration and challenging projects that help you grow.",
                        timestamp = System.currentTimeMillis() - (60L * 24L * 60L * 60L * 1000L),
                        isVerified = true,
                        companyName = ReviewEntity.CHABOKSOFT_COMPANY_NAME
                    ),
                    ReviewEntity(
                        businessOwnerId = ReviewEntity.CHABOKSOFT_BUSINESS_OWNER_ID,
                        reviewerId = "reviewer_007",
                        reviewerName = "Emma S.",
                        reviewerTitle = "Data Analyst",
                        reviewerExperience = "1 year",
                        rating = 4,
                        reviewText = "Learning a lot here. Good onboarding process and supportive team members. Looking forward to more challenging projects.",
                        timestamp = System.currentTimeMillis() - (10L * 24L * 60L * 60L * 1000L),
                        isVerified = true,
                        companyName = ReviewEntity.CHABOKSOFT_COMPANY_NAME
                    ),
                    ReviewEntity(
                        businessOwnerId = ReviewEntity.CHABOKSOFT_BUSINESS_OWNER_ID,
                        reviewerId = "reviewer_008",
                        reviewerName = "James H.",
                        reviewerTitle = "Frontend Developer",
                        reviewerExperience = "2.5 years",
                        rating = 5,
                        reviewText = "Excellent company with modern development practices. Work-life balance is respected and the team is very collaborative.",
                        timestamp = System.currentTimeMillis() - (35L * 24L * 60L * 60L * 1000L),
                        isVerified = true,
                        companyName = ReviewEntity.CHABOKSOFT_COMPANY_NAME
                    ),
                    ReviewEntity(
                        businessOwnerId = ReviewEntity.CHABOKSOFT_BUSINESS_OWNER_ID,
                        reviewerId = "reviewer_009",
                        reviewerName = "Maria G.",
                        reviewerTitle = "Business Analyst",
                        reviewerExperience = "3 years",
                        rating = 4,
                        reviewText = "Great place to work with interesting projects. Management is transparent and communication is excellent.",
                        timestamp = System.currentTimeMillis() - (28L * 24L * 60L * 60L * 1000L),
                        isVerified = true,
                        companyName = ReviewEntity.CHABOKSOFT_COMPANY_NAME
                    ),
                    ReviewEntity(
                        businessOwnerId = ReviewEntity.CHABOKSOFT_BUSINESS_OWNER_ID,
                        reviewerId = "reviewer_010",
                        reviewerName = "Kevin L.",
                        reviewerTitle = "Mobile Developer",
                        reviewerExperience = "1.5 years",
                        rating = 5,
                        reviewText = "Amazing company culture! Great opportunities to learn new technologies and work on impactful projects.",
                        timestamp = System.currentTimeMillis() - (42L * 24L * 60L * 60L * 1000L),
                        isVerified = true,
                        companyName = ReviewEntity.CHABOKSOFT_COMPANY_NAME
                    ),

                    ReviewEntity(
                        businessOwnerId = ReviewEntity.CHABOKSOFT_BUSINESS_OWNER_ID,
                        reviewerId = "reviewer_011",
                        reviewerName = "Rachel P.",
                        reviewerTitle = "UX Designer",
                        reviewerExperience = "2 years",
                        rating = 3,
                        reviewText = "Decent place to work but could improve communication between departments. Some processes need streamlining.",
                        timestamp = System.currentTimeMillis() - (50L * 24L * 60L * 60L * 1000L),
                        isVerified = true,
                        companyName = ReviewEntity.CHABOKSOFT_COMPANY_NAME
                    ),
                    ReviewEntity(
                        businessOwnerId = ReviewEntity.CHABOKSOFT_BUSINESS_OWNER_ID,
                        reviewerId = "reviewer_012",
                        reviewerName = "Tom B.",
                        reviewerTitle = "Security Engineer",
                        reviewerExperience = "4 years",
                        rating = 4,
                        reviewText = "Strong technical team and good security practices. Competitive salary and benefits package.",
                        timestamp = System.currentTimeMillis() - (55L * 24L * 60L * 60L * 1000L),
                        isVerified = true,
                        companyName = ReviewEntity.CHABOKSOFT_COMPANY_NAME
                    ),
                    ReviewEntity(
                        businessOwnerId = ReviewEntity.CHABOKSOFT_BUSINESS_OWNER_ID,
                        reviewerId = "reviewer_013",
                        reviewerName = "Anna K.",
                        reviewerTitle = "Project Manager",
                        reviewerExperience = "5 years",
                        rating = 5,
                        reviewText = "Excellent leadership and clear project vision. Great place for experienced professionals to make an impact.",
                        timestamp = System.currentTimeMillis() - (63L * 24L * 60L * 60L * 1000L),
                        isVerified = true,
                        companyName = ReviewEntity.CHABOKSOFT_COMPANY_NAME
                    )
                )


                for (review in dummyReviews) {
                    reviewDao.insertReview(review)
                }

                android.util.Log.d("ReviewRepository", "Successfully initialized ${dummyReviews.size} dummy reviews for Chaboksoft (businessOwnerId: $businessOwnerId)")

            } catch (e: Exception) {
                android.util.Log.e("ReviewRepository", "Error initializing dummy reviews", e)
            }
        }
    }
}