package com.uilover.project196.Adapter

import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.Toast
import androidx.recyclerview.widget.RecyclerView
import com.uilover.project196.Model.ApprovalStatus
import com.uilover.project196.Model.ChatModel
import com.uilover.project196.R
import com.uilover.project196.Utils.UserSession
import com.uilover.project196.databinding.ViewholderChatBinding

// KRITERIA: RecyclerView Adapter untuk daftar chat
// KRITERIA WAJIB: RecyclerView + Adapter (2/9) - Adapter untuk daftar chat
class ChatAdapter(
    private var chats: List<ChatModel>,
    private val clickListener: ClickListener
) : RecyclerView.Adapter<ChatAdapter.ChatViewHolder>() {

    interface ClickListener {
        fun onChatClick(chat: ChatModel)
        fun onApproveChat(chat: ChatModel)
        fun onRejectChat(chat: ChatModel)
    }

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): ChatViewHolder {
        val binding = ViewholderChatBinding.inflate(
            LayoutInflater.from(parent.context),
            parent,
            false
        )
        return ChatViewHolder(binding)
    }

    override fun onBindViewHolder(holder: ChatViewHolder, position: Int) {
        holder.bind(chats[position])
    }

    override fun getItemCount(): Int = chats.size

    fun updateData(newChats: List<ChatModel>) {
        chats = newChats
        notifyDataSetChanged()
    }

    inner class ChatViewHolder(private val binding: ViewholderChatBinding) :
        RecyclerView.ViewHolder(binding.root) {

        fun bind(chat: ChatModel) {
            binding.apply {
                companyName.text = chat.companyName


                if (UserSession.getUserRole() == UserSession.ROLE_FREELANCER) {

                    recruiterName.text = chat.recruiterName
                    jobTitle.text = chat.jobTitle
                } else {

                    recruiterName.text = chat.recruiterName
                    jobTitle.text = chat.freelancerProfession ?: chat.jobTitle
                }

                lastMessage.text = if (chat.lastMessage.isEmpty()) {
                    "Start a conversation..."
                } else {
                    chat.lastMessage
                }


                val now = System.currentTimeMillis()
                val hoursSinceCreated = (now - chat.timestamp) / (1000 * 60 * 60)

                timestamp.text = if (chat.lastMessage.isEmpty() && hoursSinceCreated < 1) {
                    "Just shortlisted"
                } else {
                    chat.getRelativeTime()
                }


                if (chat.lastMessage.isEmpty()) {
                    if (hoursSinceCreated < 1) {

                        timestamp.setTextColor(root.context.getColor(R.color.purple))
                        timestamp.setTypeface(timestamp.typeface, android.graphics.Typeface.BOLD)
                    } else {

                        timestamp.setTextColor(root.context.getColor(R.color.darkGrey))
                        timestamp.setTypeface(timestamp.typeface, android.graphics.Typeface.NORMAL)
                    }
                } else {

                    timestamp.setTextColor(root.context.getColor(R.color.darkGrey))
                    timestamp.setTypeface(timestamp.typeface, android.graphics.Typeface.NORMAL)
                }


                val logoResource = when (chat.companyLogo) {
                    "logo1" -> R.drawable.logo1
                    "logo2" -> R.drawable.logo2
                    "logo3" -> R.drawable.logo3
                    "logo4" -> R.drawable.logo4
                    else -> R.drawable.logo1
                }
                companyLogo.setImageResource(logoResource)


                unreadIndicator.visibility = if (chat.isUnread) View.VISIBLE else View.GONE


                val userRole = UserSession.getUserRole()

                when (chat.approvalStatus) {
                    ApprovalStatus.PENDING -> {


                        if (userRole == UserSession.ROLE_BUSINESS_OWNER) {
                            approvalButtons.visibility = View.VISIBLE
                            root.isClickable = false
                            root.isFocusable = false

                            approveButton.setOnClickListener {
                                clickListener.onApproveChat(chat)
                                Toast.makeText(root.context, "Chat approved with ${chat.companyName}", Toast.LENGTH_SHORT).show()
                            }

                            rejectButton.setOnClickListener {
                                clickListener.onRejectChat(chat)
                                Toast.makeText(root.context, "Chat rejected and removed", Toast.LENGTH_SHORT).show()
                            }
                        } else {

                            approvalButtons.visibility = View.GONE
                            root.isClickable = true
                            root.isFocusable = true

                            root.setOnClickListener {
                                clickListener.onChatClick(chat)
                            }
                        }
                    }
                    ApprovalStatus.APPROVED -> {

                        approvalButtons.visibility = View.GONE
                        root.isClickable = true
                        root.isFocusable = true


                        root.setOnClickListener {
                            clickListener.onChatClick(chat)
                        }
                    }
                    ApprovalStatus.REJECTED -> {

                        approvalButtons.visibility = View.GONE
                        root.isClickable = false
                        root.isFocusable = false
                    }
                }


                if (chat.approvalStatus == ApprovalStatus.PENDING && userRole == UserSession.ROLE_BUSINESS_OWNER) {

                    root.alpha = 0.8f
                    root.setBackgroundColor(root.context.getColor(R.color.lightGrey))
                } else {

                    root.alpha = 1.0f
                    root.setBackgroundColor(root.context.getColor(R.color.white))
                }
            }
        }
    }
}