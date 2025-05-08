package com.example.bookswapkz.adapters

import android.view.LayoutInflater
import android.view.ViewGroup
import androidx.core.view.isVisible
import androidx.recyclerview.widget.DiffUtil
import androidx.recyclerview.widget.ListAdapter
import androidx.recyclerview.widget.RecyclerView
// import com.bumptech.glide.Glide // Glide больше не нужен здесь, если нет аватаров
import com.example.bookswapkz.R
import com.example.bookswapkz.databinding.ItemChatBinding
import com.example.bookswapkz.models.Chat
import java.text.SimpleDateFormat
import java.util.Calendar
import java.util.Locale

class ChatAdapter(
    private val currentUserId: String,
    private val onItemClicked: (Chat) -> Unit
) : ListAdapter<Chat, ChatAdapter.ChatViewHolder>(ChatDiffCallback()) {

    private val timeFormat = SimpleDateFormat("HH:mm", Locale.getDefault())
    private val dateFormat = SimpleDateFormat("dd.MM.yy", Locale.getDefault())

    inner class ChatViewHolder(private val binding: ItemChatBinding) : RecyclerView.ViewHolder(binding.root) {
        fun bind(chat: Chat) {
            val otherParticipantEntry = chat.participantInfo.entries.firstOrNull { it.key != currentUserId }
            val otherUserName = otherParticipantEntry?.value?.get("nickname") ?: "Собеседник"
            // val otherUserAvatarUrl = otherParticipantEntry?.value?.get("avatarUrl") // Убрали

            binding.userNameTextView.text = otherUserName
            binding.lastMessageTextView.text = chat.lastMessageText ?: "Нет сообщений"

            chat.lastMessageTimestamp?.let { timestamp ->
                val messageCalendar = Calendar.getInstance()
                messageCalendar.time = timestamp
                val todayCalendar = Calendar.getInstance()
                if (messageCalendar.get(Calendar.YEAR) == todayCalendar.get(Calendar.YEAR) &&
                    messageCalendar.get(Calendar.DAY_OF_YEAR) == todayCalendar.get(Calendar.DAY_OF_YEAR)) {
                    binding.timestampTextView.text = timeFormat.format(timestamp)
                } else {
                    binding.timestampTextView.text = dateFormat.format(timestamp)
                }
            } ?: run {
                binding.timestampTextView.text = ""
            }

            // Всегда показываем плейсхолдер, так как avatarUrl убран
            binding.avatarImageView.setImageResource(R.drawable.placeholder_avatar)

            val unread = chat.unreadCount[currentUserId] ?: 0L
            binding.unreadCountTextView.isVisible = unread > 0
            binding.unreadCountTextView.text = if (unread > 99) "99+" else unread.toString()

            binding.root.setOnClickListener {
                onItemClicked(chat)
            }
        }
    }

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): ChatViewHolder {
        val binding = ItemChatBinding.inflate(LayoutInflater.from(parent.context), parent, false)
        return ChatViewHolder(binding)
    }

    override fun onBindViewHolder(holder: ChatViewHolder, position: Int) {
        holder.bind(getItem(position))
    }

    class ChatDiffCallback : DiffUtil.ItemCallback<Chat>() {
        override fun areItemsTheSame(oldItem: Chat, newItem: Chat): Boolean {
            return oldItem.chatId == newItem.chatId
        }
        override fun areContentsTheSame(oldItem: Chat, newItem: Chat): Boolean {
            return oldItem == newItem
        }
    }
}