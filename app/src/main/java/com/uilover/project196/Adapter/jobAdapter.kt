package com.uilover.project196.Adapter

import android.content.Context
import android.content.Intent
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup

import androidx.recyclerview.widget.RecyclerView
import com.bumptech.glide.Glide
import com.uilover.project196.Activity.DetailActivity
import com.uilover.project196.Activity.LoginActivity
import com.uilover.project196.Model.JobModel
import com.uilover.project196.R
import com.uilover.project196.Utils.UserSession
import com.uilover.project196.databinding.ViewholderJobBinding
import androidx.recyclerview.widget.DiffUtil

// KRITERIA: RecyclerView Adapter untuk daftar pekerjaan
// KRITERIA WAJIB: RecyclerView + Adapter (1/9) - Adapter untuk daftar pekerjaan
// KRITERIA WAJIB: Custom list item dengan ViewHolder
class jobAdapter(
    private var items: List<JobModel>,
    private val onBookmarkClick: ((JobModel) -> Unit)? = null,
    private val onJobClick: ((JobModel) -> Unit)? = null
) : RecyclerView.Adapter<jobAdapter.Viewholder>() {
    private lateinit var context: Context

    inner class Viewholder(val binding: ViewholderJobBinding) :
        RecyclerView.ViewHolder(binding.root)

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): jobAdapter.Viewholder {
        context = parent.context

        val binding = ViewholderJobBinding.inflate(
            LayoutInflater.from(parent.context),
            parent,
            false
        )
        return Viewholder(binding)
    }

    override fun onBindViewHolder(holder: jobAdapter.Viewholder, position: Int) {
        val item = items[position]





        bindJobData(holder, item)
        setupClickListeners(holder, item)


        loadJobImage(holder, item)


    }

    private fun loadJobImage(holder: Viewholder, item: JobModel) {
        val drawableResourceId = holder.itemView.resources
            .getIdentifier(item.picUrl, "drawable", holder.itemView.context.packageName)

        if (drawableResourceId != 0) {
            Glide.with(holder.itemView.context)
                .load(drawableResourceId)
                .into(holder.binding.pic)
        } else {

            holder.binding.pic.setImageResource(R.drawable.logo1)
        }
    }

    private fun handleJobClick(job: JobModel) {
        if (UserSession.isLoggedIn()) {

            val intent = Intent(context, DetailActivity::class.java)
            intent.putExtra("object", job)
            context.startActivity(intent)
        } else {

            val intent = Intent(context, LoginActivity::class.java)
            intent.putExtra("SOURCE_SCREEN", "other")
            context.startActivity(intent)
        }
    }


    private fun bindJobData(holder: Viewholder, item: JobModel) {
        holder.binding.apply {
            titleTxt.text = item.title
            companyTxt.text = item.company
            timeTxt.text = item.time
            modelTxt.text = item.model
            levelTxt.text = item.level
            salaryTxt.text = item.salary
            viewCountText.text = if (item.viewCount == 1) "1 view" else "${item.viewCount} views"


            val bookmarkIcon = if (item.isBookmarked) R.drawable.ic_bookmark_filled else R.drawable.ic_bookmark_outline
            imageView7.setImageResource(bookmarkIcon)

            android.util.Log.d("jobAdapter", "📋 bindJobData for job: ${item.title}")
            android.util.Log.d("jobAdapter", "📋 Job bookmark state: ${item.isBookmarked}")
            android.util.Log.d("jobAdapter", "📋 Bookmark icon resource: $bookmarkIcon")
            android.util.Log.d("jobAdapter", "📋 Icon drawable name: ${if (item.isBookmarked) "ic_bookmark_filled" else "ic_bookmark_outline"}")
            android.util.Log.d("jobAdapter", "📋 Job owned by current user: ${item.isOwnedByCurrentUser()}")
            android.util.Log.d("jobAdapter", "📋 Job is closed: ${item.isClosed()}")


            val bookmarkVisibility = if (item.isOwnedByCurrentUser() || item.isClosed()) View.GONE else View.VISIBLE
            imageView7.visibility = bookmarkVisibility

            android.util.Log.d("jobAdapter", "📋 Bookmark icon visibility: ${if (bookmarkVisibility == View.VISIBLE) "VISIBLE" else "GONE"}")

            ownedJobBadge.visibility = if (item.isOwnedByCurrentUser()) View.VISIBLE else View.GONE
            closedJobBadge.visibility = if (item.isOwnedByCurrentUser() && item.isClosed()) View.VISIBLE else View.GONE
            viewCountContainer.visibility = if (item.isOpen()) View.VISIBLE else View.GONE
        }
    }

    private fun setupClickListeners(holder: Viewholder, item: JobModel) {
        holder.binding.apply {

            imageView7.setOnClickListener {
                android.util.Log.d("jobAdapter", "🔖 ============ BOOKMARK CLICK DEBUG ============")
                android.util.Log.d("jobAdapter", "🔖 Job: ${item.title} at ${item.company}")
                android.util.Log.d("jobAdapter", "🔖 ORIGINAL bookmark state: ${item.isBookmarked}")
                android.util.Log.d("jobAdapter", "🔖 Expected action: ${if (!item.isBookmarked) "ADD bookmark" else "REMOVE bookmark"}")
                android.util.Log.d("jobAdapter", "🔖 Job owner ID: ${item.ownerId}")
                android.util.Log.d("jobAdapter", "🔖 Job status: ${item.status}")
                android.util.Log.d("jobAdapter", "🔖 onBookmarkClick callback exists: ${onBookmarkClick != null}")


                if (item.isOwnedByCurrentUser()) {
                    android.util.Log.w("jobAdapter", "🔖 Cannot bookmark owned job: ${item.title}")
                    return@setOnClickListener
                }

                if (item.isClosed()) {
                    android.util.Log.w("jobAdapter", "🔖 Cannot bookmark closed job: ${item.title}")
                    return@setOnClickListener
                }



                android.util.Log.d("jobAdapter", "🔖 Processing bookmark click through centralized state management")


                imageView7.animate()
                    .scaleX(0.85f)
                    .scaleY(0.85f)
                    .setDuration(80)
                    .withEndAction {
                        imageView7.animate()
                            .scaleX(1.0f)
                            .scaleY(1.0f)
                            .setDuration(120)
                            .setInterpolator(android.view.animation.OvershootInterpolator(1.2f))
                            .start()
                    }
                    .start()

                if (onBookmarkClick != null) {
                    android.util.Log.d("jobAdapter", "🔖 Calling onBookmarkClick callback with original item...")

                    onBookmarkClick.invoke(item)
                    android.util.Log.d("jobAdapter", "🔖 onBookmarkClick callback completed - state management will handle UI updates")
                } else {
                    android.util.Log.e("jobAdapter", "🔖 ERROR: onBookmarkClick callback is NULL!")
                }
            }


            root.setOnClickListener {
                android.util.Log.d("jobAdapter", "📱 Job card clicked for: ${item.title}")
                if (onJobClick != null) {
                    onJobClick.invoke(item)
                } else {
                    handleJobClick(item)
                }
            }
        }
    }

    override fun getItemCount(): Int = items.size

    fun updateData(newJobs: List<JobModel>) {

        val diffCallback = JobDiffCallback(items, newJobs)
        val diffResult = DiffUtil.calculateDiff(diffCallback)

        items = newJobs
        diffResult.dispatchUpdatesTo(this)
    }

    fun updateBookmarkState(position: Int) {
        if (position >= 0 && position < items.size) {
            notifyItemChanged(position)
        }
    }

    fun getJob(position: Int): JobModel? {
        return if (position >= 0 && position < items.size) {
            items[position]
        } else {
            null
        }
    }

    fun findJobPosition(job: JobModel): Int {
        return items.indexOfFirst { it.title == job.title && it.company == job.company }
    }


    fun updateJob(updatedJob: JobModel) {
        val position = findJobPosition(updatedJob)
        if (position >= 0) {
            val currentJob = items[position]



            if (currentJob.isBookmarked != updatedJob.isBookmarked ||
                currentJob.viewCount != updatedJob.viewCount ||
                currentJob.status != updatedJob.status) {

                android.util.Log.d("jobAdapter", "🔄 Updating job ${updatedJob.title}: bookmark=${currentJob.isBookmarked}->${updatedJob.isBookmarked}")

                items = items.toMutableList().apply {
                    set(position, updatedJob)
                }


                // Update the item in the list
                notifyItemChanged(position)
            } else {
                android.util.Log.d("jobAdapter", "🔄 No update needed for job ${updatedJob.title} - states are identical")
            }
        }
    }


    fun updateJobBookmarkState(jobTitle: String, jobCompany: String, isBookmarked: Boolean) {
        val position = items.indexOfFirst { it.title == jobTitle && it.company == jobCompany }
        if (position >= 0) {
            val currentJob = items[position]
            if (currentJob.isBookmarked != isBookmarked) {
                android.util.Log.d("jobAdapter", "🔖 Updating bookmark state for ${jobTitle}: ${currentJob.isBookmarked} -> ${isBookmarked}")

                val updatedJob = currentJob.copy(isBookmarked = isBookmarked)
                items = items.toMutableList().apply {
                    set(position, updatedJob)
                }


                notifyItemChanged(position, mapOf("bookmark" to isBookmarked))

                android.util.Log.d("jobAdapter", "✅ Bookmark state updated and item refreshed at position $position")
            } else {
                android.util.Log.d("jobAdapter", "🔖 Bookmark state for ${jobTitle} already matches: ${isBookmarked}")
            }
        } else {
            android.util.Log.w("jobAdapter", "🔖 Job not found for bookmark update: ${jobTitle} at ${jobCompany}")
        }
    }


    fun removeJob(job: JobModel) {
        val position = findJobPosition(job)
        if (position >= 0) {
            items = items.toMutableList().apply {
                removeAt(position)
            }
            notifyItemRemoved(position)
        }
    }


    private class JobDiffCallback(
        private val oldList: List<JobModel>,
        private val newList: List<JobModel>
    ) : DiffUtil.Callback() {

        override fun getOldListSize(): Int = oldList.size
        override fun getNewListSize(): Int = newList.size

        override fun areItemsTheSame(oldItemPosition: Int, newItemPosition: Int): Boolean {
            val oldItem = oldList[oldItemPosition]
            val newItem = newList[newItemPosition]
            return oldItem.title == newItem.title &&
                   oldItem.company == newItem.company &&
                   oldItem.ownerId == newItem.ownerId
        }

        override fun areContentsTheSame(oldItemPosition: Int, newItemPosition: Int): Boolean {
            val oldItem = oldList[oldItemPosition]
            val newItem = newList[newItemPosition]
            return oldItem.isBookmarked == newItem.isBookmarked &&
                   oldItem.viewCount == newItem.viewCount &&
                   oldItem.salary == newItem.salary &&
                   oldItem.status == newItem.status &&
                   oldItem.time == newItem.time &&
                   oldItem.model == newItem.model &&
                   oldItem.level == newItem.level
        }

        override fun getChangePayload(oldItemPosition: Int, newItemPosition: Int): Any? {
            val oldItem = oldList[oldItemPosition]
            val newItem = newList[newItemPosition]


            val changes = mutableMapOf<String, Any>()

            if (oldItem.isBookmarked != newItem.isBookmarked) {
                changes["bookmark"] = newItem.isBookmarked
            }

            if (oldItem.viewCount != newItem.viewCount) {
                changes["viewCount"] = newItem.viewCount
            }

            if (oldItem.status != newItem.status) {
                changes["status"] = newItem.status
            }

            return if (changes.isNotEmpty()) changes else null
        }
    }

    override fun onBindViewHolder(holder: Viewholder, position: Int, payloads: MutableList<Any>) {
        if (payloads.isEmpty()) {

            onBindViewHolder(holder, position)
        } else {

            val item = items[position]

            for (payload in payloads) {
                if (payload is Map<*, *>) {
                    @Suppress("UNCHECKED_CAST")
                    val changes = payload as Map<String, Any>






                    bindJobData(holder, item)
                }
            }
        }
    }
}