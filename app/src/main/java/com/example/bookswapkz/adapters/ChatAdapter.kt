package com.example.bookswapkz.adapters

import android.view.LayoutInflater
import android.view.ViewGroup
import androidx.core.view.isVisible // Импорт isVisible
import androidx.recyclerview.widget.DiffUtil
import androidx.recyclerview.widget.ListAdapter
import androidx.recyclerview.widget.RecyclerView
import com.bumptech.glide.Glide
import com.example.bookswapkz.R // Импорт R
import com.example.bookswapkz.databinding.ItemChatBinding // Используем правильный биндинг
import com.example.bookswapkz.models.Chat
import java.text.SimpleDateFormat // Импорт SimpleDateFormat
import java.util.Locale

class ChatAdapter(
    private val currentUserId: String,
    private val onItemClicked: (Chat) -> Unit
) : ListAdapter<Chat, ChatAdapter.ChatViewHolder>(ChatDiffCallback()) {

    private val timeFormat = SimpleDateFormat("HH:mm", Locale.getDefault())

    inner class ChatViewHolder(private val binding: ItemChatBinding) : RecyclerView.ViewHolder(binding.root) {
        fun bind(chat: Chat) {
            // --- ИСПРАВЛЕНО: Получение данных собеседника ---
            val otherParticipantPair = chat.participantInfo.entries.firstOrNull { it.key != currentUserId }
            // Используем ?.get() для доступа к значениям в Map
            val otherUserName = otherParticipantPair?.value?.get("nickname") ?: "Собеседник"
            val otherUserAvatarUrl = otherParticipantPair?.value?.get("avatarUrl") // Предполагаем, что ключ 'avatarUrl'

            // --- ИСПРАВЛЕНО: Используем правильные ID из item_chat.xml ---
            binding.userNameTextView.text = otherUserName
            binding.lastMessageTextView.text = chat.lastMessageText ?: "" // Добавляем обработку null
            binding.timestampTextView.text = chat.lastMessageTimestamp?.let { timeFormat.format(it) } ?: "" // Обработка null timestamp

            // --- ИСПРАВЛЕНО: Загрузка аватара и обработка ошибок Glide ---
            if (!otherUserAvatarUrl.isNullOrBlank()) {
                Glide.with(binding.root.context)
                    .load(otherUserAvatarUrl)
                    .placeholder(R.drawable.placeholder_avatar) // Плейсхолдер при загрузке
                    .error(R.drawable.placeholder_avatar) // Плейсхолдер при ошибке
                    .circleCrop()
                    .into(binding.avatarImageView)
            } else {
                binding.avatarImageView.setImageResource(R.drawable.placeholder_avatar) // Установка плейсхолдера по умолчанию
            }

            // Отображение счетчика непрочитанных
            val unread = chat.unreadCount[currentUserId] ?: 0
            binding.unreadCountTextView.isVisible = unread > 0
            binding.unreadCountTextView.text = unread.toString()
            // --- КОНЕЦ ИСПРАВЛЕНИЙ ---

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