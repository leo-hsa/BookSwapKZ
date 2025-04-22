package com.example.bookswapkz.fragments

import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import androidx.fragment.app.Fragment
import androidx.fragment.app.viewModels
import androidx.navigation.fragment.findNavController
import androidx.recyclerview.widget.LinearLayoutManager
import com.example.bookswapkz.R
import com.example.bookswapkz.adapters.RentBookAdapter
import com.example.bookswapkz.databinding.FragmentRentBinding
import com.example.bookswapkz.viewmodels.BookViewModel
import com.google.android.material.chip.Chip

class RentFragment : Fragment() {
    private var _binding: FragmentRentBinding? = null
    private val binding get() = _binding!!
    private val viewModel: BookViewModel by viewModels()
    private lateinit var adapter: RentBookAdapter

    override fun onCreateView(
        inflater: LayoutInflater,
        container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View {
        _binding = FragmentRentBinding.inflate(inflater, container, false)
        return binding.root
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)

        setupRecyclerView()
        setupFilterChips()
        observeBooks()
    }

    private fun setupRecyclerView() {
        adapter = RentBookAdapter { book ->
            // Handle book click - navigate to book details
            val action = RentFragmentDirections.actionRentFragmentToBookDetailFragment(book)
            findNavController().navigate(action)
        }
        
        binding.rentBooksRecyclerView.apply {
            layoutManager = LinearLayoutManager(context)
            adapter = this@RentFragment.adapter
        }
    }

    private fun setupFilterChips() {
        binding.filterChipGroup.setOnCheckedChangeListener { group, checkedId ->
            when (checkedId) {
                R.id.allChip -> viewModel.loadRentableBooks()
                R.id.paidChip -> viewModel.loadRentableBooks(paidOnly = true)
                R.id.freeChip -> viewModel.loadRentableBooks(paidOnly = false)
            }
        }
    }

    private fun observeBooks() {
        viewModel.rentableBooks.observe(viewLifecycleOwner) { books ->
            adapter.submitList(books)
            binding.progressBar.visibility = View.GONE
        }

        viewModel.isLoading.observe(viewLifecycleOwner) { isLoading ->
            binding.progressBar.visibility = if (isLoading) View.VISIBLE else View.GONE
        }
    }

    override fun onDestroyView() {
        super.onDestroyView()
        _binding = null
    }
} 