package com.example.bookswapkz.fragments

import android.os.Bundle
import androidx.fragment.app.Fragment
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import androidx.core.view.isVisible
import androidx.fragment.app.viewModels
import com.example.bookswapkz.adapters.RentBookAdapter // Адаптер для аренды
import com.example.bookswapkz.databinding.FragmentRentBinding // Используем Binding
import com.example.bookswapkz.viewmodels.BookViewModel // Используем общую ViewModel пока
import dagger.hilt.android.AndroidEntryPoint

@AndroidEntryPoint
class RentFragment : Fragment() {

    private var _binding: FragmentRentBinding? = null
    private val binding get() = _binding!!

    // Используем BookViewModel, пока нет отдельной RentViewModel
    private val viewModel: BookViewModel by viewModels({ requireActivity() })
    // private lateinit var rentBookAdapter: RentBookAdapter // Адаптер пока не создаем

    override fun onCreateView(
        inflater: LayoutInflater, container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View {
        _binding = FragmentRentBinding.inflate(inflater, container, false)
        return binding.root
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)

        // setupRecyclerView() // Пока не вызываем
        // observeViewModel() // Пока не вызываем

        // --- Используем ID из FragmentRentBinding ---
        binding.textEmptyRent?.text = "Раздел аренды в разработке"
        binding.textEmptyRent?.isVisible = true
        binding.rentBooksRecyclerView?.isVisible = false // Updated to correct ID
        binding.progressBar?.isVisible = false // ID ProgressBar
        // ---
    }

    /*
    private fun setupRecyclerView() {
        rentBookAdapter = RentBookAdapter { book -> /* ... */ }
        binding.rentBooksRecyclerView?.apply { /* ... */ } // Updated to correct ID
    }

    private fun observeViewModel() {
         // TODO: Заменить viewModel.rentableBooks на реальную LiveData
         viewModel.rentableBooks.observe(viewLifecycleOwner) { books ->
             rentBookAdapter.submitList(books)
             binding.textEmptyRent?.isVisible = books.isNullOrEmpty()
             binding.rentBooksRecyclerView?.isVisible = !books.isNullOrEmpty() // Updated to correct ID
         }
          viewModel.isLoading.observe(...) { isLoading -> ... }
          viewModel.errorMessage.observe(...) { error -> ... }
     }
    */

    override fun onDestroyView() {
        super.onDestroyView()
        // --- Используем ID из Binding ---
        binding.rentBooksRecyclerView?.adapter = null // Updated to correct ID
        _binding = null
    }
}