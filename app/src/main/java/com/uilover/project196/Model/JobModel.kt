package com.uilover.project196.Model

import android.os.Parcel
import android.os.Parcelable

data class JobModel(
    val title:String="",
    val company:String="",
    val picUrl:String="",
    val time:String="",
    val model:String="",
    val level:String="",
    val location:String="",
    val salary:String="",
    val category:String="",
    val about:String="",
    val description:String="",
    var isBookmarked: Boolean = false,
    val ownerId: String? = null,
    val status: String = "open",
    var viewCount: Int = 0

):Parcelable {
    constructor(parcel: Parcel) : this(
        parcel.readString().toString(),
        parcel.readString().toString(),
        parcel.readString().toString(),
        parcel.readString().toString(),
        parcel.readString().toString(),
        parcel.readString().toString(),
        parcel.readString().toString(),
        parcel.readString().toString(),
        parcel.readString().toString(),
        parcel.readString().toString(),
        parcel.readString().toString(),
        parcel.readByte() != 0.toByte(),
        parcel.readString(),
        parcel.readString() ?: "open",
        parcel.readInt()
    ) {
    }

    override fun writeToParcel(parcel: Parcel, flags: Int) {
        parcel.writeString(title)
        parcel.writeString(company)
        parcel.writeString(picUrl)
        parcel.writeString(time)
        parcel.writeString(model)
        parcel.writeString(level)
        parcel.writeString(location)
        parcel.writeString(salary)
        parcel.writeString(category)
        parcel.writeString(about)
        parcel.writeString(description)
        parcel.writeByte(if (isBookmarked) 1 else 0)
        parcel.writeString(ownerId)
        parcel.writeString(status)
        parcel.writeInt(viewCount)
    }

    override fun describeContents(): Int {
        return 0
    }

    companion object CREATOR : Parcelable.Creator<JobModel> {
        override fun createFromParcel(parcel: Parcel): JobModel {
            return JobModel(parcel)
        }

        override fun newArray(size: Int): Array<JobModel?> {
            return arrayOfNulls(size)
        }
    }

    fun isOwnedByCurrentUser(): Boolean {
        val currentUserId = com.uilover.project196.Utils.UserSession.getUserId()
        val isOwned = ownerId != null && ownerId == currentUserId
        android.util.Log.d("JobModel", "🏢 isOwnedByCurrentUser() for '${this.title}' - currentUserId: $currentUserId, jobOwnerId: $ownerId, result: $isOwned")
        return isOwned
    }

    fun isOpen(): Boolean {
        val isOpenStatus = status == "open"
        android.util.Log.d("JobModel", "🔓 isOpen() for '${this.title}' - status: '$status', result: $isOpenStatus")
        return isOpenStatus
    }

    fun isClosed(): Boolean {
        val isClosedStatus = status == "closed"
        android.util.Log.d("JobModel", "🔒 isClosed() for '${this.title}' - status: '$status', result: $isClosedStatus")
        return isClosedStatus
    }
}


fun JobModel.toJobEntity(): JobEntity {
    return JobEntity(
        title = this.title,
        company = this.company,
        location = this.location,
        time = this.time,
        model = this.model,
        level = this.level,
        salary = this.salary,
        category = this.category,
        picUrl = this.picUrl,
        isBookmarked = this.isBookmarked,
        ownerId = this.ownerId,
        status = this.status,
        about = this.about,
        description = this.description
    )
}
