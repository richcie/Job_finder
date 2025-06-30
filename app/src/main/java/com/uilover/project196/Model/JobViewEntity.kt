package com.uilover.project196.Model

import androidx.room.Entity
import androidx.room.ForeignKey
import androidx.room.PrimaryKey

@Entity(
    tableName = "job_views",
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
            childColumns = ["viewerUserId"],
            onDelete = ForeignKey.CASCADE
        )
    ],
    indices = [
        androidx.room.Index(value = ["jobId"]),
        androidx.room.Index(value = ["viewerUserId"]),
        androidx.room.Index(value = ["viewedAt"])
    ]
)
data class JobViewEntity(
    @PrimaryKey(autoGenerate = true)
    val id: Int = 0,
    val jobId: Int,
    val viewerUserId: String,
    val viewedAt: Long = System.currentTimeMillis(),
    val sessionId: String = "",
    val source: String = "detail_view"
)