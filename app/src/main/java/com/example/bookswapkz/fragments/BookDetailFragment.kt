package com.example.bookswapkz.fragments

import android.content.Intent
import android.net.Uri
import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import androidx.fragment.app.Fragment
import com.bumptech.glide.Glide
import com.example.bookswapkz.databinding.FragmentBookDetailBinding
import com.example.bookswapkz.models.Book

class BookDetailFragment : Fragment() {
    private var _binding: FragmentBookDetailBinding? = null
    private val binding get() = _binding!!

    override fun onCreateView(
        inflater: LayoutInflater, container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View {
        _binding = FragmentBookDetailBinding.inflate(inflater, container, false)
        return binding.root
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)

        // Load book only if arguments are present
        arguments?.let {
            val book = BookDetailFragmentArgs.fromBundle(it).book
            updateBook(book)
        }

        binding.callButton.setOnClickListener {
            binding.titleTextView.tag?.let { phone ->
                val intent = Intent(Intent.ACTION_DIAL, Uri.parse("tel:$phone"))
                startActivity(intent)
            }
        }
    }

    fun updateBook(book: Book?) {
        if (book == null) {
            binding.titleTextView.text = ""
            binding.authorTextView.text = ""
            binding.conditionTextView.text = ""
            binding.cityTextView.text = ""
            binding.bookImage.visibility = View.GONE
            binding.titleTextView.tag = null
            return
        }

        binding.titleTextView.text = book.title
        binding.authorTextView.text = book.author
        binding.conditionTextView.text = book.condition
        binding.cityTextView.text = book.city

        if (!book.imageUrl.isNullOrEmpty()) {
            Glide.with(binding.bookImage.context)
                .load(book.imageUrl)
                .into(binding.bookImage)
            binding.bookImage.visibility = View.VISIBLE
        } else {
            binding.bookImage.visibility = View.GONE
        }

        binding.titleTextView.tag = book.phone
    }

    override fun onDestroyView() {
        super.onDestroyView()
        _binding = null
    }
}