package com.example.bookswapkz.adapters

import android.view.LayoutInflater
// import android.view.View // Не используется
import android.view.ViewGroup
import androidx.core.content.ContextCompat
import androidx.recyclerview.widget.DiffUtil
import androidx.recyclerview.widget.ListAdapter
import androidx.recyclerview.widget.RecyclerView
// import com.bumptech.glide.Glide // Glide не нужен, если нет ImageView
import com.example.bookswapkz.R
import com.example.bookswapkz.databinding.ItemExchangeHistoryBinding
import com.example.bookswapkz.models.Exchange
import com.example.bookswapkz.models.ExchangeStatus
import java.text.SimpleDateFormat
// import java.util.Date // Не используется напрямую
import java.util.Locale

class ExchangeHistoryAdapter : ListAdapter<Exchange, ExchangeHistoryAdapter.ExchangeHistoryViewHolder>(ExchangeDiffCallback()) {

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): ExchangeHistoryViewHolder {
        val binding = ItemExchangeHistoryBinding.inflate(
            LayoutInflater.from(parent.context),
            parent,
            false
        )
        return ExchangeHistoryViewHolder(binding)
    }

    override fun onBindViewHolder(holder: ExchangeHistoryViewHolder, position: Int) {
        holder.bind(getItem(position))
    }

    class ExchangeHistoryViewHolder(
        private val binding: ItemExchangeHistoryBinding
    ) : RecyclerView.ViewHolder(binding.root) {

        private val dateFormat = SimpleDateFormat("dd.MM.yyyy HH:mm", Locale.getDefault())

        fun bind(exchange: Exchange) {
            binding.apply {
                bookTitleTextView.text = exchange.requestedBookTitle ?: "Книга не найдена"

                val dateToShow = exchange.processedAt ?: exchange.createdAt
                exchangeDateTextView.text = dateToShow?.let { dateFormat.format(it) } ?: "Дата неизвестна"

                val statusText: String
                val statusColorRes: Int
                when (exchange.status) {
                    ExchangeStatus.ACCEPTED.name -> {
                        statusText = "Обмен с '${exchange.requesterNickname ?: "..."}' принят"
                        statusColorRes = R.color.black // Убедись, что цвет есть
                    }
                    ExchangeStatus.REJECTED.name -> {
                        statusText = "Запрос от '${exchange.requesterNickname ?: "..."}' отклонен"
                        statusColorRes = R.color.black
                    }
                    ExchangeStatus.PENDING.name -> {
                        statusText = "Ожидает ответа от '${exchange.requesterNickname ?: "..."}'"
                        statusColorRes = R.color.black // Убедись, что цвет есть
                    }
                    else -> {
                        statusText = "Статус: ${exchange.status}"
                        statusColorRes = R.color.text_secondary
                    }
                }
                newOwnerTextView.text = statusText
                try {
                    newOwnerTextView.setTextColor(ContextCompat.getColor(binding.root.context, statusColorRes))
                } catch (e: Exception) {
                    // Игнорируем, если цвет не найден
                }

                // --- КОД ДЛЯ ЗАГРУЗКИ КАРТИНКИ УДАЛЕН ---
                /*
                if (!exchange.requestedBookImageUrl.isNullOrBlank()) {
                    Glide.with(binding.root.context)
                        .load(exchange.requestedBookImageUrl)
                        .placeholder(R.drawable.book_cover_placeholder)
                        .error(R.drawable.ic_book_placeholder_error)
                        .into(binding.bookImageView) // ОШИБКА БЫЛА ЗДЕСЬ
                } else {
                    binding.bookImageView.setImageResource(R.drawable.book_cover_placeholder) // И ЗДЕСЬ
                }
                 */
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