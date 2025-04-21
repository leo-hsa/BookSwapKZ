package com.example.bookswapkz.fragments

import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import androidx.fragment.app.Fragment
import androidx.fragment.app.viewModels
import androidx.navigation.fragment.findNavController
import androidx.recyclerview.widget.LinearLayoutManager
import com.example.bookswapkz.adapters.BookAdapter
import com.example.bookswapkz.databinding.FragmentMyBooksBinding
import com.example.bookswapkz.viewmodels.BookViewModel
// Возможно, понадобятся другие импорты, например, для R или моделей
import com.example.bookswapkz.R
import com.example.bookswapkz.models.Book

class MyBooksFragment : Fragment() {
    private var _binding: FragmentMyBooksBinding? = null
    private val binding get() = _binding!!
    private val viewModel: BookViewModel by viewModels()

    override fun onCreateView(
        inflater: LayoutInflater, container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View {
        _binding = FragmentMyBooksBinding.inflate(inflater, container, false)
        return binding.root
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)

        val adapter = BookAdapter { book ->
            val action = MyBooksFragmentDirections.actionMyBooksToBookDetail(book)
            findNavController().navigate(action)
        }
        binding.recyclerView.layoutManager = LinearLayoutManager(context)
        binding.recyclerView.adapter = adapter

        viewModel.myBooks.observe(viewLifecycleOwner) { books ->
            adapter.submitList(books)
        }
    }

    override fun onDestroyView() {
        super.onDestroyView()
        _binding = null
    }
}