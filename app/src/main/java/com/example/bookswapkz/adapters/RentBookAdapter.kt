package com.example.bookswapkz.adapters

import android.view.LayoutInflater
import android.view.ViewGroup
import androidx.recyclerview.widget.DiffUtil
import androidx.recyclerview.widget.ListAdapter
import androidx.recyclerview.widget.RecyclerView
import com.example.bookswapkz.databinding.ItemRentBookBinding
import com.example.bookswapkz.models.Book

class RentBookAdapter(
    private val onBookClick: (Book) -> Unit
) : ListAdapter<Book, RentBookAdapter.RentBookViewHolder>(BookDiffCallback()) {

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): RentBookViewHolder {
        val binding = ItemRentBookBinding.inflate(
            LayoutInflater.from(parent.context),
            parent,
            false
        )
        return RentBookViewHolder(binding)
    }

    override fun onBindViewHolder(holder: RentBookViewHolder, position: Int) {
        holder.bind(getItem(position))
    }

    inner class RentBookViewHolder(
        private val binding: ItemRentBookBinding
    ) : RecyclerView.ViewHolder(binding.root) {

        init {
            binding.root.setOnClickListener {
                val position = bindingAdapterPosition
                if (position != RecyclerView.NO_POSITION) {
                    onBookClick(getItem(position))
                }
            }
        }

        fun bind(book: Book) {
            binding.apply {
                bookTitleTextView.text = book.title
                authorTextView.text = book.author
                conditionTextView.text = book.condition
                cityTextView.text = book.city

                // Set rent price text
                rentPriceTextView.text = when {
                    book.rentPrice != null -> "${book.rentPrice} ₸/${book.rentPeriod ?: "нед"}"
                    else -> "Бесплатно"
                }

                // Set rent button state
                rentButton.isEnabled = !book.isRented
                rentButton.text = if (book.isRented) "Арендовано" else "Арендовать"
            }
        }
    }

    private class BookDiffCallback : DiffUtil.ItemCallback<Book>() {
        override fun areItemsTheSame(oldItem: Book, newItem: Book): Boolean {
            return oldItem.id == newItem.id
        }

        override fun areContentsTheSame(oldItem: Book, newItem: Book): Boolean {
            return oldItem == newItem
        }
    }
} 