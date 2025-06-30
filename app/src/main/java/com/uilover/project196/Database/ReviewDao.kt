package com.uilover.project196.Database

import androidx.room.*
import com.uilover.project196.Model.ReviewEntity

@Dao
// KRITERIA WAJIB: Room DAO untuk operasi database Review
interface ReviewDao {

    @Query("SELECT * FROM reviews WHERE businessOwnerId = :businessOwnerId ORDER BY timestamp DESC")
    suspend fun getReviewsForBusiness(businessOwnerId: String): List<ReviewEntity>

    @Query("SELECT * FROM reviews WHERE businessOwnerId = :businessOwnerId ORDER BY timestamp DESC LIMIT :limit")
    suspend fun getRecentReviewsForBusiness(businessOwnerId: String, limit: Int = 3): List<ReviewEntity>

    @Query("SELECT AVG(rating) FROM reviews WHERE businessOwnerId = :businessOwnerId")
    suspend fun getAverageRating(businessOwnerId: String): Double?

    @Query("SELECT COUNT(*) FROM reviews WHERE businessOwnerId = :businessOwnerId")
    suspend fun getTotalReviewCount(businessOwnerId: String): Int

    @Query("SELECT COUNT(*) FROM reviews WHERE businessOwnerId = :businessOwnerId AND rating = :starRating")
    suspend fun getCountByRating(businessOwnerId: String, starRating: Int): Int

    @Insert
    suspend fun insertReview(review: ReviewEntity): Long

    @Update
    suspend fun updateReview(review: ReviewEntity)

    @Delete
    suspend fun deleteReview(review: ReviewEntity)

    @Query("DELETE FROM reviews")
    suspend fun deleteAllReviews()

    @Query("SELECT * FROM reviews WHERE reviewerId = :reviewerId AND businessOwnerId = :businessOwnerId LIMIT 1")
    suspend fun getUserReviewForBusiness(reviewerId: String, businessOwnerId: String): ReviewEntity?
}