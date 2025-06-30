package com.uilover.project196.Model

import androidx.room.Entity
import androidx.room.PrimaryKey

@Entity(tableName = "jobs")
data class JobEntity(
    @PrimaryKey(autoGenerate = true)
    val id: Int = 0,
    val title: String,
    val company: String,
    val location: String,
    val time: String,
    val model: String,
    val level: String,
    val salary: String,
    val category: String,
    val picUrl: String,
    val isBookmarked: Boolean = false,
    val ownerId: String? = null,
    val status: String = "open",
    val about: String = "",
    val description: String = ""
)