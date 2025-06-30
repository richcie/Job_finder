package com.uilover.project196.Adapter

import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import androidx.recyclerview.widget.RecyclerView
import com.uilover.project196.Model.MessageModel
import com.uilover.project196.Utils.UserSession
import com.uilover.project196.databinding.ViewholderMessageReceivedBinding
import com.uilover.project196.databinding.ViewholderMessageSentBinding
import com.uilover.project196.databinding.ViewholderJobRequestBinding
import com.uilover.project196.databinding.ViewholderVerificationRequestBinding
import com.uilover.project196.R
import org.json.JSONObject

// KRITERIA WAJIB: RecyclerView + Adapter (3/9) - Adapter untuk pesan chat
class MessageAdapter(
    private var messages: List<MessageModel>,
    private val onJobRequestAction: ((MessageModel, Boolean) -> Unit)? = null,
    private val onVerificationAction: ((MessageModel, Boolean) -> Unit)? = null
) : RecyclerView.Adapter<RecyclerView.ViewHolder>() {

    companion object {
        private const val TYPE_SENT = 1
        private const val TYPE_RECEIVED = 2
        private const val TYPE_JOB_REQUEST = 3
        private const val TYPE_VERIFICATION_REQUEST = 4
    }

    override fun getItemViewType(position: Int): Int {
        val message = messages[position]
        return when {
            message.isVerificationRequest() -> TYPE_VERIFICATION_REQUEST
            message.isJobRequest() -> TYPE_JOB_REQUEST
            message.isFromUser -> TYPE_SENT
            else -> TYPE_RECEIVED
        }
    }

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): RecyclerView.ViewHolder {
        return when (viewType) {
            TYPE_SENT -> {
                val binding = ViewholderMessageSentBinding.inflate(
                    LayoutInflater.from(parent.context),
                    parent,
                    false
                )
                SentMessageViewHolder(binding)
            }
            TYPE_RECEIVED -> {
                val binding = ViewholderMessageReceivedBinding.inflate(
                    LayoutInflater.from(parent.context),
                    parent,
                    false
                )
                ReceivedMessageViewHolder(binding)
            }
            TYPE_JOB_REQUEST -> {
                val binding = ViewholderJobRequestBinding.inflate(
                    LayoutInflater.from(parent.context),
                    parent,
                    false
                )
                JobRequestViewHolder(binding)
            }
            TYPE_VERIFICATION_REQUEST -> {
                val binding = ViewholderVerificationRequestBinding.inflate(
                    LayoutInflater.from(parent.context),
                    parent,
                    false
                )
                VerificationRequestViewHolder(binding)
            }
            else -> throw IllegalArgumentException("Invalid view type")
        }
    }

    override fun onBindViewHolder(holder: RecyclerView.ViewHolder, position: Int) {
        when (holder) {
            is SentMessageViewHolder -> holder.bind(messages[position])
            is ReceivedMessageViewHolder -> holder.bind(messages[position])
            is JobRequestViewHolder -> holder.bind(messages[position])
            is VerificationRequestViewHolder -> holder.bind(messages[position])
        }
    }

    override fun getItemCount(): Int = messages.size

    fun updateMessages(newMessages: List<MessageModel>) {
        messages = newMessages
        notifyDataSetChanged()
    }

    fun addMessage(message: MessageModel) {
        val newMessages = messages.toMutableList()
        newMessages.add(message)
        messages = newMessages
        notifyItemInserted(messages.size - 1)
    }

    inner class SentMessageViewHolder(private val binding: ViewholderMessageSentBinding) :
        RecyclerView.ViewHolder(binding.root) {

        fun bind(message: MessageModel) {
            binding.messageText.text = message.text
            binding.messageTime.text = message.getFormattedTime()
        }
    }

    inner class ReceivedMessageViewHolder(private val binding: ViewholderMessageReceivedBinding) :
        RecyclerView.ViewHolder(binding.root) {

        fun bind(message: MessageModel) {
            binding.messageText.text = message.text
            binding.messageTime.text = message.getFormattedTime()
        }
    }

    inner class JobRequestViewHolder(private val binding: ViewholderJobRequestBinding) :
        RecyclerView.ViewHolder(binding.root) {

        fun bind(message: MessageModel) {
            binding.requestMessage.text = message.text
            binding.messageTime.text = message.getFormattedTime()


            when (message.jobRequestStatus) {
                MessageModel.STATUS_PENDING -> {
                    binding.statusBadge.text = "PENDING"
                    binding.statusBadge.setBackgroundResource(R.drawable.orange_badge_bg)
                }
                MessageModel.STATUS_ACCEPTED -> {
                    binding.statusBadge.text = "ACCEPTED"
                    binding.statusBadge.setBackgroundResource(R.drawable.purple_bg)
                }
                MessageModel.STATUS_REJECTED -> {
                    binding.statusBadge.text = "REJECTED"
                    binding.statusBadge.setBackgroundResource(R.drawable.red_badge_bg)
                }
            }


            val currentUserRole = UserSession.getUserRole()
            val isBusinessOwner = currentUserRole == UserSession.ROLE_BUSINESS_OWNER
            val isPending = message.isJobRequestPending()

            if (isBusinessOwner && isPending) {
                binding.actionButtons.visibility = View.VISIBLE

                binding.acceptButton.setOnClickListener {
                    onJobRequestAction?.invoke(message, true)
                }

                binding.rejectButton.setOnClickListener {
                    onJobRequestAction?.invoke(message, false)
                }
            } else {
                binding.actionButtons.visibility = View.GONE
            }
        }
    }

    inner class VerificationRequestViewHolder(private val binding: ViewholderVerificationRequestBinding) :
        RecyclerView.ViewHolder(binding.root) {

        fun bind(message: MessageModel) {
            binding.verificationMessage.text = message.text
            binding.verificationTime.text = message.getFormattedTime()


            try {
                message.freelancerData?.let { jsonData ->
                    val freelancerInfo = JSONObject(jsonData)
                    binding.freelancerName.text = freelancerInfo.optString("name", "Unknown Freelancer")
                    binding.freelancerEmail.text = freelancerInfo.optString("email", "No email provided")
                }
            } catch (e: Exception) {
                binding.freelancerName.text = "Unknown Freelancer"
                binding.freelancerEmail.text = "No email provided"
            }


            when (message.verificationStatus) {
                MessageModel.STATUS_PENDING -> {
                    binding.verificationStatusBadge.text = "PENDING"
                    binding.verificationStatusBadge.setBackgroundResource(R.drawable.orange_badge_bg)
                }
                MessageModel.STATUS_ACCEPTED -> {
                    binding.verificationStatusBadge.text = "VERIFIED"
                    binding.verificationStatusBadge.setBackgroundResource(R.drawable.green_bg)
                }
                MessageModel.STATUS_REJECTED -> {
                    binding.verificationStatusBadge.text = "REJECTED"
                    binding.verificationStatusBadge.setBackgroundResource(R.drawable.red_badge_bg)
                }
            }


            val currentUserRole = UserSession.getUserRole()
            val isBusinessOwner = currentUserRole == UserSession.ROLE_BUSINESS_OWNER
            val isPending = message.isVerificationPending()

            if (isBusinessOwner && isPending) {
                binding.verificationActionButtons.visibility = View.VISIBLE

                binding.acceptVerificationButton.setOnClickListener {
                    onVerificationAction?.invoke(message, true)
                }

                binding.rejectVerificationButton.setOnClickListener {
                    onVerificationAction?.invoke(message, false)
                }
            } else {
                binding.verificationActionButtons.visibility = View.GONE
            }
        }
    }
}