package com.uilover.project196.Model

import androidx.room.Entity
import androidx.room.PrimaryKey
import androidx.room.Index

@Entity(
    tableName = "job_attendance",
    indices = [
        Index(value = ["jobId"]),
        Index(value = ["freelancerId"]),
        Index(value = ["attendanceDate"]),
        Index(value = ["jobId", "freelancerId", "attendanceDate"], unique = true)
    ]
)
data class JobAttendanceEntity(
    @PrimaryKey(autoGenerate = true)
    val id: Int = 0,
    val jobId: Int,
    val freelancerId: String,
    val attendanceDate: String,
    val checkInTime: Long? = null,
    val checkOutTime: Long? = null,
    val progressReport: String? = null,
    val createdAt: Long = System.currentTimeMillis(),
    val updatedAt: Long = System.currentTimeMillis()
)