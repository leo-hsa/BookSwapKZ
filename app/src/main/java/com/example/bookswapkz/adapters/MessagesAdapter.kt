package com.example.bookswapkz.adapters

import android.view.LayoutInflater
import android.view.ViewGroup
import androidx.recyclerview.widget.DiffUtil
import androidx.recyclerview.widget.ListAdapter
import androidx.recyclerview.widget.RecyclerView
import com.example.bookswapkz.R
import com.example.bookswapkz.databinding.ItemMessageIncomingBinding
import com.example.bookswapkz.databinding.ItemMessageOutgoingBinding
import com.example.bookswapkz.models.Message
import com.google.firebase.auth.FirebaseAuth
import java.text.SimpleDateFormat
import java.util.*

class MessagesAdapter : ListAdapter<Message, RecyclerView.ViewHolder>(MessageDiffCallback()) {

    private val currentUserId: String? = FirebaseAuth.getInstance().currentUser?.uid

    companion object {
        private const val VIEW_TYPE_SENT = 1
        private const val VIEW_TYPE_RECEIVED = 2
        private val timeFormat = SimpleDateFormat("HH:mm", Locale.getDefault())
    }

    override fun getItemViewType(position: Int): Int {
        val message = getItem(position)
        return if (message.senderId == currentUserId) {
            VIEW_TYPE_SENT
        } else {
            VIEW_TYPE_RECEIVED
        }
    }

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): RecyclerView.ViewHolder {
        val inflater = LayoutInflater.from(parent.context)
        return when (viewType) {
            VIEW_TYPE_SENT -> {
                val binding = ItemMessageOutgoingBinding.inflate(inflater, parent, false)
                SentMessageViewHolder(binding)
            }
            VIEW_TYPE_RECEIVED -> {
                val binding = ItemMessageIncomingBinding.inflate(inflater, parent, false)
                ReceivedMessageViewHolder(binding)
            }
            else -> throw IllegalArgumentException("Invalid view type")
        }
    }

    override fun onBindViewHolder(holder: RecyclerView.ViewHolder, position: Int) {
        val message = getItem(position)
        when (holder) {
            is SentMessageViewHolder -> holder.bind(message)
            is ReceivedMessageViewHolder -> holder.bind(message)
        }
    }

    inner class SentMessageViewHolder(private val binding: ItemMessageOutgoingBinding) :
        RecyclerView.ViewHolder(binding.root) {
        fun bind(message: Message) {
            // Убедись, что ID в item_message_outgoing.xml правильные
            binding.textMessageBody.text = message.text
            binding.textMessageTime.text = message.timestamp?.let { timeFormat.format(it) } ?: ""
        }
    }

    inner class ReceivedMessageViewHolder(private val binding: ItemMessageIncomingBinding) :
        RecyclerView.ViewHolder(binding.root) {
        fun bind(message: Message) {
            // Убедись, что ID в item_message_incoming.xml правильные
            binding.textMessageBody.text = message.text
            // binding.textMessageName.text = "Собеседник" // Можно добавить имя отправителя из participantInfo
            binding.textMessageTime.text = message.timestamp?.let { timeFormat.format(it) } ?: ""
            // Тут можно загружать аватар, если он есть в participantInfo для senderId
            // Glide.with(binding.root.context).load(senderAvatarUrl).placeholder(R.drawable.placeholder_avatar).into(binding.imageMessageProfile)
        }
    }

    class MessageDiffCallback : DiffUtil.ItemCallback<Message>() {
        override fun areItemsTheSame(oldItem: Message, newItem: Message): Boolean {
            return oldItem.id == newItem.id && oldItem.id.isNotBlank() // Убедимся, что ID не пустой
        }

        override fun areContentsTheSame(oldItem: Message, newItem: Message): Boolean {
            return oldItem == newItem
        }
    }
}