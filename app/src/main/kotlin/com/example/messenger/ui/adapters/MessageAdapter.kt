package com.example.messenger.ui.adapters

import android.view.LayoutInflater
import android.view.ViewGroup
import androidx.recyclerview.widget.RecyclerView
import com.bumptech.glide.Glide
import com.example.messenger.R
import com.example.messenger.databinding.ItemMessageBinding
import com.example.messenger.models.Message

class MessageAdapter(private val currentUserId: String) :
    RecyclerView.Adapter<MessageAdapter.MessageViewHolder>() {

    private var messages = emptyList<Message>()

    fun setMessages(newMessages: List<Message>) {
        messages = newMessages
        notifyDataSetChanged()
    }

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): MessageViewHolder {
        val binding = ItemMessageBinding.inflate(
            LayoutInflater.from(parent.context),
            parent,
            false
        )
        return MessageViewHolder(binding)
    }

    override fun onBindViewHolder(holder: MessageViewHolder, position: Int) {
        holder.bind(messages[position])
    }

    override fun getItemCount() = messages.size

    inner class MessageViewHolder(private val binding: ItemMessageBinding) :
        RecyclerView.ViewHolder(binding.root) {

        fun bind(message: Message) {
            val isCurrentUser = message.senderId == currentUserId

            binding.apply {
                senderName.text = message.senderName

                when (message.mediaType) {
                    "photo" -> {
                        textContent.visibility = android.view.View.GONE
                        mediaView.visibility = android.view.View.VISIBLE
                        Glide.with(root.context)
                            .load(message.mediaUrl)
                            .into(mediaView)
                    }
                    "video" -> {
                        textContent.visibility = android.view.View.GONE
                        mediaView.visibility = android.view.View.VISIBLE
                        Glide.with(root.context)
                            .load(message.mediaUrl)
                            .placeholder(R.drawable.ic_video_placeholder)
                            .into(mediaView)
                    }
                    else -> {
                        textContent.visibility = android.view.View.VISIBLE
                        mediaView.visibility = android.view.View.GONE
                        textContent.text = message.content
                    }
                }

                // Set background color based on sender
                if (isCurrentUser) {
                    messageContainer.setBackgroundColor(
                        binding.root.context.getColor(R.color.primary_gold)
                    )
                    textContent.setTextColor(
                        binding.root.context.getColor(R.color.primary_dark)
                    )
                    senderName.setTextColor(
                        binding.root.context.getColor(R.color.primary_dark)
                    )
                } else {
                    messageContainer.setBackgroundColor(
                        binding.root.context.getColor(R.color.surface_light)
                    )
                    textContent.setTextColor(
                        binding.root.context.getColor(R.color.text_primary)
                    )
                    senderName.setTextColor(
                        binding.root.context.getColor(R.color.primary_gold)
                    )
                }
            }
        }
    }
}
