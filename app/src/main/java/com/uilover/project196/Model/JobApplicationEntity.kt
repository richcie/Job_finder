package com.uilover.project196.Model

import androidx.room.Entity
import androidx.room.ForeignKey
import androidx.room.PrimaryKey

@Entity(
    tableName = "job_applications",
    foreignKeys = [
        ForeignKey(
            entity = JobEntity::class,
            parentColumns = ["id"],
            childColumns = ["jobId"],
            onDelete = ForeignKey.CASCADE
        ),
        ForeignKey(
            entity = UserEntity::class,
            parentColumns = ["userId"],
            childColumns = ["applicantUserId"],
            onDelete = ForeignKey.CASCADE
        )
    ],
    indices = [
        androidx.room.Index(value = ["jobId"]),
        androidx.room.Index(value = ["applicantUserId"]),
        androidx.room.Index(value = ["jobId", "applicantUserId"], unique = true)
    ]
)
data class JobApplicationEntity(
    @PrimaryKey(autoGenerate = true)
    val id: Int = 0,
    val jobId: Int,
    val applicantUserId: String,
    val status: String = "pending",
    val appliedAt: Long = System.currentTimeMillis(),
    val coverLetter: String = "",
    val proposedRate: String = "",
    val skills: String = "",
    val description: String = ""
)