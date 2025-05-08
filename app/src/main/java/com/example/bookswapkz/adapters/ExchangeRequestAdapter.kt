package com.example.bookswapkz.adapters

import android.view.LayoutInflater
import android.view.ViewGroup
import androidx.recyclerview.widget.DiffUtil
import androidx.recyclerview.widget.ListAdapter
import androidx.recyclerview.widget.RecyclerView
import com.bumptech.glide.Glide
import com.example.bookswapkz.R
import com.example.bookswapkz.databinding.ItemExchangeRequestBinding // Убедись, что имя правильное
import com.example.bookswapkz.models.Exchange

class ExchangeRequestAdapter(
    private val onAcceptClick: (Exchange) -> Unit,
    private val onRejectClick: (Exchange) -> Unit
) : ListAdapter<Exchange, ExchangeRequestAdapter.ExchangeViewHolder>(ExchangeDiffCallback()) {

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): ExchangeViewHolder {
        val binding = ItemExchangeRequestBinding.inflate(
            LayoutInflater.from(parent.context),
            parent,
            false
        )
        return ExchangeViewHolder(binding, onAcceptClick, onRejectClick)
    }

    override fun onBindViewHolder(holder: ExchangeViewHolder, position: Int) {
        holder.bind(getItem(position))
    }

    inner class ExchangeViewHolder(
        private val binding: ItemExchangeRequestBinding,
        private val onAccept: (Exchange) -> Unit,
        private val onReject: (Exchange) -> Unit
    ) : RecyclerView.ViewHolder(binding.root) {

        fun bind(exchange: Exchange) {
            binding.requesterInfoTextView.text = "Пользователь '${exchange.requesterNickname ?: "..."}' хочет вашу книгу:"
            binding.bookTitleTextView.text = exchange.requestedBookTitle ?: "Название неизвестно"

            // Загрузка обложки книги, которую хотят
            if (!exchange.requestedBookImageUrl.isNullOrBlank()) {
                Glide.with(binding.root.context)
                    .load(exchange.requestedBookImageUrl)
                    .placeholder(R.drawable.book_cover_placeholder)
                    .error(R.drawable.ic_book_placeholder_error)
                    .into(binding.bookImageView)
            } else {
                binding.bookImageView.setImageResource(R.drawable.book_cover_placeholder)
            }

            binding.acceptButton.setOnClickListener { onAccept(exchange) }
            binding.rejectButton.setOnClickListener { onReject(exchange) }
        }
    }

    class ExchangeDiffCallback : DiffUtil.ItemCallback<Exchange>() {
        override fun areItemsTheSame(oldItem: Exchange, newItem: Exchange): Boolean {
            return oldItem.id == newItem.id
        }

        override fun areContentsTheSame(oldItem: Exchange, newItem: Exchange): Boolean {
            return oldItem == newItem
        }
    }
}