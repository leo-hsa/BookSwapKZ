package com.example.bookswapkz.adapters

import android.view.LayoutInflater
import android.view.ViewGroup
import androidx.recyclerview.widget.DiffUtil
import androidx.recyclerview.widget.ListAdapter
import androidx.recyclerview.widget.RecyclerView
import com.example.bookswapkz.databinding.ItemExchangeHistoryBinding
import com.example.bookswapkz.models.Exchange
import java.text.SimpleDateFormat
import java.util.Locale

class ExchangeHistoryAdapter : ListAdapter<Exchange, ExchangeHistoryAdapter.ExchangeViewHolder>(ExchangeDiffCallback()) {

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): ExchangeViewHolder {
        val binding = ItemExchangeHistoryBinding.inflate(
            LayoutInflater.from(parent.context),
            parent,
            false
        )
        return ExchangeViewHolder(binding)
    }

    override fun onBindViewHolder(holder: ExchangeViewHolder, position: Int) {
        holder.bind(getItem(position))
    }

    class ExchangeViewHolder(
        private val binding: ItemExchangeHistoryBinding
    ) : RecyclerView.ViewHolder(binding.root) {

        private val dateFormat = SimpleDateFormat("dd MMM yyyy, HH:mm", Locale.getDefault())

        fun bind(exchange: Exchange) {
            binding.apply {
                bookTitleTextView.text = exchange.bookTitle
                exchangeDateTextView.text = dateFormat.format(exchange.exchangeDate)
                newOwnerTextView.text = "To: ${exchange.newOwnerName}"
            }
        }
    }

    private class ExchangeDiffCallback : DiffUtil.ItemCallback<Exchange>() {
        override fun areItemsTheSame(oldItem: Exchange, newItem: Exchange): Boolean {
            return oldItem.id == newItem.id
        }

        override fun areContentsTheSame(oldItem: Exchange, newItem: Exchange): Boolean {
            return oldItem == newItem
        }
    }
} 