package com.example.bookswapkz.adapters // Убедитесь, что пакет правильный

import android.view.LayoutInflater
import android.view.ViewGroup
// УДАЛИТЕ: import androidx.navigation.findNavController - навигация должна быть во фрагменте
import androidx.recyclerview.widget.DiffUtil
import androidx.recyclerview.widget.ListAdapter
import androidx.recyclerview.widget.RecyclerView
import com.example.bookswapkz.databinding.ItemBookBinding
// УДАЛИТЕ: import com.example.bookswapkz.fragments.HomeFragmentDirections - адаптер не должен знать о фрагментах
import com.example.bookswapkz.models.Book


// ДОБАВЬТЕ ПАРАМЕТР КОНСТРУКТОРА: private val onItemClicked: (Book) -> Unit
class BookAdapter(private val onItemClicked: (Book) -> Unit) : ListAdapter<Book, BookAdapter.BookViewHolder>(BookDiffCallback()) {

    // Передаем лямбду в конструктор ViewHolder
    class BookViewHolder(
        private val binding: ItemBookBinding,
        private val onItemClicked: (Book) -> Unit // Добавляем параметр и сюда
    ) : RecyclerView.ViewHolder(binding.root) {

        fun bind(book: Book) {
            binding.titleTextView.text = book.title
            binding.authorTextView.text = book.author
            binding.conditionTextView.text = book.condition
            // Теперь при клике вызываем лямбду, переданную из адаптера
            binding.root.setOnClickListener {
                onItemClicked(book)
            }
        }
    }

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): BookViewHolder {
        val binding = ItemBookBinding.inflate(LayoutInflater.from(parent.context), parent, false)
        // Передаем лямбду, полученную адаптером, в создаваемый ViewHolder
        return BookViewHolder(binding, onItemClicked)
    }

    override fun onBindViewHolder(holder: BookViewHolder, position: Int) {
        holder.bind(getItem(position))
    }
}

class BookDiffCallback : DiffUtil.ItemCallback<Book>() {
    override fun areItemsTheSame(oldItem: Book, newItem: Book): Boolean {
        return oldItem.id == newItem.id // Используем id, как исправили ранее
    }

    override fun areContentsTheSame(oldItem: Book, newItem: Book): Boolean {
        return oldItem == newItem
    }
}

