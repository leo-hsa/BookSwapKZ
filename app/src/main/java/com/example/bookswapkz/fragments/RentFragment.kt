package com.example.bookswapkz.fragments

import android.os.Bundle
import androidx.fragment.app.Fragment
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import androidx.fragment.app.viewModels // Импорт для viewModels
import androidx.recyclerview.widget.LinearLayoutManager // Импорт для LayoutManager
import com.example.bookswapkz.R
import com.example.bookswapkz.adapters.RentBookAdapter // Импорт вашего адаптера для аренды
import com.example.bookswapkz.databinding.FragmentRentBinding // Импорт Binding
import com.example.bookswapkz.models.Book // Импорт модели Book
import com.example.bookswapkz.viewmodels.BookViewModel // Импорт ViewModel
import dagger.hilt.android.AndroidEntryPoint // Импорт Hilt
import android.widget.Toast
import androidx.navigation.fragment.findNavController

@AndroidEntryPoint
class RentFragment : Fragment() {

    private var _binding: FragmentRentBinding? = null
    private val binding get() = _binding!!

    // Получаем ViewModel
    private val viewModel: BookViewModel by viewModels({ requireActivity() })
    private lateinit var adapter: RentBookAdapter

    override fun onCreateView(
        inflater: LayoutInflater, container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View {
        _binding = FragmentRentBinding.inflate(inflater, container, false)
        return binding.root
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)

        setupUI()
        observeViewModel()
    }

    private fun setupUI() {
        // Update UI based on books available for rent
        if (viewModel.allBooks.value.isNullOrEmpty()) {
            binding.rentBooksRecyclerView.visibility = View.GONE
            binding.textEmptyRent.visibility = View.VISIBLE
            binding.textEmptyRent.text = getString(R.string.no_rentable_books)
        } else {
            binding.rentBooksRecyclerView.visibility = View.VISIBLE
            binding.textEmptyRent.visibility = View.GONE
        }

        // Setup adapter with callback for book click
        adapter = RentBookAdapter { book -> 
            navigateToBookDetail(book)
        }
        
        binding.rentBooksRecyclerView.adapter = adapter
        binding.rentBooksRecyclerView.layoutManager = LinearLayoutManager(requireContext())
        
        // Observe rentable books from ViewModel
        viewModel.allBooks.observe(viewLifecycleOwner) { books ->
            if (books.isNullOrEmpty()) {
                binding.textEmptyRent.visibility = View.VISIBLE
                binding.textEmptyRent.text = getString(R.string.no_rentable_books)
                binding.rentBooksRecyclerView.visibility = View.GONE
            } else {
                val rentableBooks = books.filter { it.isForRent && !it.isRented }
                adapter.submitList(rentableBooks)
            }
        }
    }

    private fun observeViewModel() {
        // TODO: Обработка загрузки и ошибок для аренды
        // viewModel.isRentLoading.observe(...)
    }

    private fun rentBook(book: Book) {
        // TODO: Implement rent logic when ready
        Toast.makeText(requireContext(), "Аренда книги '${book.title}' в разработке", Toast.LENGTH_SHORT).show()
    }
    
    private fun returnBook(book: Book) {
        // TODO: Implement return logic when ready
        Toast.makeText(requireContext(), "Возврат книги '${book.title}' в разработке", Toast.LENGTH_SHORT).show()
    }
    
    private fun navigateToBookDetail(book: Book) {
        val action = RentFragmentDirections.actionRentFragmentToBookDetailFragment(book)
        findNavController().navigate(action)
    }

    override fun onDestroyView() {
        super.onDestroyView()
        adapter.submitList(null)
        _binding = null
    }
}