package com.example.bookswapkz.adapters

import android.view.LayoutInflater
import android.view.ViewGroup
import androidx.recyclerview.widget.DiffUtil
import androidx.recyclerview.widget.ListAdapter
import androidx.recyclerview.widget.RecyclerView
import com.example.bookswapkz.databinding.ItemChatBinding
import com.example.bookswapkz.models.Chat
import java.text.SimpleDateFormat
import java.util.*

class ChatsAdapter(
    private val onChatClick: (Chat) -> Unit
) : ListAdapter<Chat, ChatsAdapter.ChatViewHolder>(ChatDiffCallback()) {

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): ChatViewHolder {
        val binding = ItemChatBinding.inflate(
            LayoutInflater.from(parent.context),
            parent,
            false
        )
        return ChatViewHolder(binding)
    }

    override fun onBindViewHolder(holder: ChatViewHolder, position: Int) {
        holder.bind(getItem(position))
    }

    inner class ChatViewHolder(
        private val binding: ItemChatBinding
    ) : RecyclerView.ViewHolder(binding.root) {

        init {
            binding.root.setOnClickListener {
                val position = bindingAdapterPosition
                if (position != RecyclerView.NO_POSITION) {
                    onChatClick(getItem(position))
                }
            }
        }

        fun bind(chat: Chat) {
            val participantName = chat.participantInfo.values.firstOrNull()?.get("nickname") ?: "Unknown"
            binding.userNameTextView.text = participantName
            binding.lastMessageTextView.text = chat.lastMessageText
            val formattedTimestamp = chat.lastMessageTimestamp?.let { formatTimestamp(it) } ?: "Unknown time"
            binding.timestampTextView.text = formattedTimestamp
        }
    }

    private class ChatDiffCallback : DiffUtil.ItemCallback<Chat>() {
        override fun areItemsTheSame(oldItem: Chat, newItem: Chat): Boolean {
            return oldItem.chatId == newItem.chatId
        }

        override fun areContentsTheSame(oldItem: Chat, newItem: Chat): Boolean {
            return oldItem == newItem
        }
    }

    private fun formatTimestamp(timestamp: Date): String {
        val now = Date()
        val diff = now.time - timestamp.time
        val seconds = diff / 1000
        val minutes = seconds / 60
        val hours = minutes / 60
        val days = hours / 24

        return when {
            seconds < 60 -> "только что"
            minutes < 60 -> "$minutes мин назад"
            hours < 24 -> "$hours ч назад"
            days < 7 -> "$days д назад"
            else -> SimpleDateFormat("dd.MM.yyyy", Locale.getDefault()).format(timestamp)
        }
    }
}