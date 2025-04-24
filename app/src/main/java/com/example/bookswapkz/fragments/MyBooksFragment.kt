package com.example.bookswapkz.fragments

import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import androidx.core.view.isVisible
import androidx.fragment.app.Fragment
import androidx.fragment.app.viewModels
import androidx.navigation.fragment.findNavController
import androidx.recyclerview.widget.GridLayoutManager
import com.example.bookswapkz.adapters.BookAdapter
import com.example.bookswapkz.databinding.FragmentMyBooksBinding
import com.example.bookswapkz.models.Book
import com.example.bookswapkz.viewmodels.BookViewModel
import dagger.hilt.android.AndroidEntryPoint

@AndroidEntryPoint
class MyBooksFragment : Fragment() {
    private var _binding: FragmentMyBooksBinding? = null
    private val binding get() = _binding!!
    private val viewModel: BookViewModel by viewModels({ requireActivity() })
    
    private lateinit var exchangeBooksAdapter: BookAdapter
    private lateinit var rentBooksAdapter: BookAdapter

    override fun onCreateView(
        inflater: LayoutInflater, container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View {
        _binding = FragmentMyBooksBinding.inflate(inflater, container, false)
        return binding.root
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)

        setupAdapters()
        observeViewModel()
    }
    
    private fun setupAdapters() {
        // Setup exchange books adapter
        exchangeBooksAdapter = BookAdapter { book ->
            val action = MyBooksFragmentDirections.actionMyBooksToBookDetail(book)
            findNavController().navigate(action)
        }
        binding.exchangeBooksRecyclerView.layoutManager = GridLayoutManager(context, 2)
        binding.exchangeBooksRecyclerView.adapter = exchangeBooksAdapter
        
        // Setup rent books adapter
        rentBooksAdapter = BookAdapter { book ->
            val action = MyBooksFragmentDirections.actionMyBooksToBookDetail(book)
            findNavController().navigate(action)
        }
        binding.rentBooksRecyclerView.layoutManager = GridLayoutManager(context, 2)
        binding.rentBooksRecyclerView.adapter = rentBooksAdapter
    }
    
    private fun observeViewModel() {
        // Show loading state
        binding.progressBar.isVisible = true
        
        viewModel.myBooks.observe(viewLifecycleOwner) { allBooks ->
            binding.progressBar.isVisible = false
            
            // Split books by type
            val exchangeBooks = allBooks.filter { !it.isForRent }
            val rentBooks = allBooks.filter { it.isForRent }
            
            // Update exchange books section
            exchangeBooksAdapter.submitList(exchangeBooks)
            binding.emptyExchangeBooksTextView.isVisible = exchangeBooks.isEmpty()
            binding.exchangeBooksRecyclerView.isVisible = exchangeBooks.isNotEmpty()
            
            // Update rent books section
            rentBooksAdapter.submitList(rentBooks)
            binding.emptyRentBooksTextView.isVisible = rentBooks.isEmpty()
            binding.rentBooksRecyclerView.isVisible = rentBooks.isNotEmpty()
            
            // Show no books message if both lists are empty
            binding.noBooksTextView.isVisible = allBooks.isEmpty()
            
            // Show/hide section headers based on content
            binding.exchangeHeaderTextView.isVisible = !allBooks.isEmpty()
            binding.rentHeaderTextView.isVisible = !allBooks.isEmpty()
        }
    }

    override fun onDestroyView() {
        super.onDestroyView()
        _binding = null
    }
}