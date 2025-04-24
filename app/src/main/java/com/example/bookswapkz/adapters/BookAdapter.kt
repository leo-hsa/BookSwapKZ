package com.example.bookswapkz.adapters

import android.view.LayoutInflater
import android.view.ViewGroup
import androidx.recyclerview.widget.DiffUtil
import androidx.recyclerview.widget.ListAdapter
import androidx.recyclerview.widget.RecyclerView
import com.bumptech.glide.Glide
import com.example.bookswapkz.R
import com.example.bookswapkz.databinding.ItemBookBinding
import com.example.bookswapkz.models.Book

class BookAdapter(private val onItemClick: (Book) -> Unit) : 
    ListAdapter<Book, BookAdapter.BookViewHolder>(BookDiffCallback()) {

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): BookViewHolder {
        val binding = ItemBookBinding.inflate(
            LayoutInflater.from(parent.context), 
            parent, 
            false
        )
        return BookViewHolder(binding)
    }

    override fun onBindViewHolder(holder: BookViewHolder, position: Int) {
        holder.bind(getItem(position))
    }

    inner class BookViewHolder(private val binding: ItemBookBinding) : 
        RecyclerView.ViewHolder(binding.root) {

        init {
            binding.root.setOnClickListener {
                val position = adapterPosition
                if (position != RecyclerView.NO_POSITION) {
                    onItemClick(getItem(position))
                }
            }
        }

        fun bind(book: Book) {
            binding.bookTitle.text = book.title
            binding.bookAuthor.text = book.author
            
            // Set owner name
            book.ownerName?.let { 
                binding.ownerName.text = it
            }
            
            // Set city/distance if available
            book.city?.let {
                binding.distance.text = it
            }

            // Load image with Glide
            if (book.imageUrl?.isNotBlank() == true) {
                Glide.with(binding.bookCover.context)
                    .load(book.imageUrl)
                    .placeholder(R.drawable.book_cover_placeholder)
                    .into(binding.bookCover)
            } else {
                binding.bookCover.setImageResource(R.drawable.book_cover_placeholder)
            }
            
            // Set status chip if book has rent information
            if (book.isForRent) {
                binding.statusChip.text = "Аренда"
                binding.statusChip.setChipBackgroundColorResource(R.color.status_rental)
            } else {
                binding.statusChip.text = "Обмен"
                binding.statusChip.setChipBackgroundColorResource(R.color.status_exchange)
            }
        }
    }

    class BookDiffCallback : DiffUtil.ItemCallback<Book>() {
        override fun areItemsTheSame(oldItem: Book, newItem: Book): Boolean {
            return oldItem.id == newItem.id
        }

        override fun areContentsTheSame(oldItem: Book, newItem: Book): Boolean {
            return oldItem == newItem
        }
    }
}