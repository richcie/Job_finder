package com.uilover.project196.Model

import androidx.room.Entity
import androidx.room.PrimaryKey

@Entity(tableName = "reviews")
data class ReviewEntity(
    @PrimaryKey(autoGenerate = true)
    val id: Int = 0,
    val businessOwnerId: String,
    val reviewerId: String,
    val reviewerName: String,
    val reviewerTitle: String,
    val reviewerExperience: String,
    val rating: Int,
    val reviewText: String,
    val timestamp: Long,
    val isVerified: Boolean = false,
    val companyName: String = ""
) {
    companion object {

        const val CHABOKSOFT_BUSINESS_OWNER_ID = "user_002"
        const val CHABOKSOFT_COMPANY_NAME = "Chaboksoft"
    }
}