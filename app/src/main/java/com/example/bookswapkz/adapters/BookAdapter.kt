package com.example.bookswapkz.adapters

import android.util.Log
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import androidx.core.view.isVisible
import androidx.recyclerview.widget.DiffUtil
import androidx.recyclerview.widget.ListAdapter
import androidx.recyclerview.widget.RecyclerView
import com.bumptech.glide.Glide
import com.example.bookswapkz.R
import com.example.bookswapkz.databinding.ItemBookBinding // <- Убедитесь, что имя совпадает с item_book.xml
import com.example.bookswapkz.models.Book
import com.google.firebase.auth.FirebaseAuth

class BookAdapter(
    private val onItemClicked: (Book) -> Unit
    // Убираем вторую лямбду, так как чат открывается из деталей
) : ListAdapter<Book, BookAdapter.BookViewHolder>(BookDiffCallback()) {

    inner class BookViewHolder(
        private val binding: ItemBookBinding, // <- Используем биндинг
        private val onItemClicked: (Book) -> Unit
    ) : RecyclerView.ViewHolder(binding.root) {

        fun bind(book: Book) {
            binding.titleTextView.text = book.title
            binding.authorTextView.text = book.author
            binding.conditionTextView.text = book.condition // TextView для состояния
            binding.ownerCountTextView.text = book.ownerCount.toString() // Chip или TextView
            binding.ownerNicknameTextView?.text = book.ownerNickname ?: "" // TextView для ника (nullable)
            binding.ownerNicknameTextView?.isVisible = !book.ownerNickname.isNullOrBlank()
            binding.cityTextView?.text = book.city // TextView для города (если есть)

            if (!book.imageUrl.isNullOrEmpty()) {
                binding.bookImage.let { imageView -> // Используем ID bookImage
                    Glide.with(binding.root.context)
                        .load(book.imageUrl)
                        .placeholder(R.drawable.ic_book_placeholder)
                        .error(R.drawable.ic_book_placeholder_error)
                        .centerCrop()
                        .into(imageView)
                    imageView.isVisible = true
                }
            } else {
                binding.bookImage.setImageResource(R.drawable.ic_book_placeholder)
                binding.bookImage.isVisible = true
            }

            binding.root.setOnClickListener {
                onItemClicked(book)
            }
        }
    }

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): BookViewHolder {
        // Создаем биндинг из макета
        val binding = ItemBookBinding.inflate(LayoutInflater.from(parent.context), parent, false)
        return BookViewHolder(binding, onItemClicked)
    }

    override fun onBindViewHolder(holder: BookViewHolder, position: Int) {
        holder.bind(getItem(position))
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