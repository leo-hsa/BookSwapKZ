package com.example.bookswapkz.fragments

import android.os.Bundle
import android.text.Editable
import android.text.TextWatcher
import android.util.Log
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.ArrayAdapter
import android.widget.Toast
import androidx.core.view.isVisible
import androidx.fragment.app.Fragment
import androidx.fragment.app.viewModels
import androidx.navigation.fragment.findNavController
import androidx.recyclerview.widget.GridLayoutManager
import com.example.bookswapkz.R
import com.example.bookswapkz.adapters.BookAdapter
import com.example.bookswapkz.databinding.DialogFiltersBinding
import com.example.bookswapkz.databinding.FragmentHomeBinding
import com.example.bookswapkz.models.Book
import com.example.bookswapkz.utils.GridSpacingItemDecoration
import com.example.bookswapkz.viewmodels.BookViewModel
import com.google.android.material.dialog.MaterialAlertDialogBuilder
import dagger.hilt.android.AndroidEntryPoint

@AndroidEntryPoint
class HomeFragment : Fragment() {

    private var _binding: FragmentHomeBinding? = null
    private val binding get() = _binding!!

    private val viewModel: BookViewModel by viewModels({ requireActivity() })
    private lateinit var bookAdapter: BookAdapter
    private var fullBookList: List<Book> = emptyList()

    private var selectedRentType: String = "Все книги"
    private var selectedPriceType: String = "Все цены"
    private var selectedCity: String? = null
    private var currentSearchQuery: String = ""

    override fun onCreateView(
        inflater: LayoutInflater,
        container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View {
        Log.d("HomeFragment", "onCreateView called")
        _binding = FragmentHomeBinding.inflate(inflater, container, false)
        return binding.root
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)
        Log.d("HomeFragment", "onViewCreated called")
        setupRecyclerView()
        setupSearch()
        setupFilterButton()
        setupNavigation()
        observeViewModel()
        
        // Показываем индикатор загрузки сразу
        binding.progressBar?.isVisible = true
    }
    
    private fun setupNavigation() {
        // Настраиваем кнопку добавления книги
        binding.addBookFab?.setOnClickListener {
            findNavController().navigate(R.id.addBookFragment)
        }
    }

    private fun setupRecyclerView() {
        bookAdapter = BookAdapter { book -> handleBookClick(book) }
        binding.recentBooksRecyclerView?.apply {
            layoutManager = GridLayoutManager(requireContext(), 2)
            adapter = bookAdapter
            addItemDecoration(GridSpacingItemDecoration(2, resources.getDimensionPixelSize(R.dimen.grid_spacing), true))
        }
    }

    private fun handleBookClick(book: Book) {
        try {
            // Убедитесь, что HomeFragmentDirections сгенерирован и действие существует
            val action = HomeFragmentDirections.actionHomeToBookDetail(book)
            findNavController().navigate(action)
        } catch (e: Exception) {
            Log.e("HomeFragment", "Navigation to detail failed for book ${book.id}", e)
            Toast.makeText(context, "Не удалось открыть детали книги", Toast.LENGTH_SHORT).show()
        }
    }

    private fun setupSearch() {
        binding.searchEditText?.addTextChangedListener(object : TextWatcher {
            override fun beforeTextChanged(s: CharSequence?, start: Int, count: Int, after: Int) {}
            override fun onTextChanged(s: CharSequence?, start: Int, before: Int, count: Int) {}
            override fun afterTextChanged(s: Editable?) {
                val newQuery = s?.toString()?.trim() ?: ""
                if (newQuery != currentSearchQuery) {
                    currentSearchQuery = newQuery
                    applyFilters()
                }
            }
        })
    }

    private fun setupFilterButton() {
        binding.filterButton?.setOnClickListener {
            showFilterDialog()
        }
    }

    private fun showFilterDialog() {
        val dialogBinding = DialogFiltersBinding.inflate(layoutInflater)
        val dialogView = dialogBinding.root

        // Setup filter dropdowns
        setupFilterDropdowns(dialogBinding)

        MaterialAlertDialogBuilder(requireContext())
            .setView(dialogView)
            .setPositiveButton(R.string.apply) { _, _ ->
                applyFilters()
            }
            .setNegativeButton(R.string.cancel, null)
            .setNeutralButton(R.string.reset) { _, _ ->
                selectedRentType = "Все книги"
                selectedPriceType = "Все цены"
                selectedCity = null
                applyFilters()
            }
            .show()
    }

    private fun setupFilterDropdowns(dialogBinding: DialogFiltersBinding) {
        // Book types
        val rentTypes = arrayOf("Все книги", "Для аренды")
        val rentAdapter = ArrayAdapter(requireContext(), android.R.layout.simple_dropdown_item_1line, rentTypes)
        dialogBinding.rentTypeAutoComplete?.setAdapter(rentAdapter)
        dialogBinding.rentTypeAutoComplete?.setText(selectedRentType, false)

        // Price types
        val priceTypes = arrayOf("Все цены", "Бесплатно", "Платно")
        val priceAdapter = ArrayAdapter(requireContext(), android.R.layout.simple_dropdown_item_1line, priceTypes)
        dialogBinding.priceTypeAutoComplete?.setAdapter(priceAdapter)
        dialogBinding.priceTypeAutoComplete?.setText(selectedPriceType, false)

        // Cities
        val cities = resources.getStringArray(R.array.cities)
        val cityAdapter = ArrayAdapter(requireContext(), android.R.layout.simple_dropdown_item_1line, cities)
        dialogBinding.cityAutoComplete?.setAdapter(cityAdapter)
        dialogBinding.cityAutoComplete?.setText(selectedCity ?: "", false)

        // Handle selections
        dialogBinding.rentTypeAutoComplete?.setOnItemClickListener { _, _, _, _ ->
            selectedRentType = dialogBinding.rentTypeAutoComplete?.text?.toString() ?: "Все книги"
        }

        dialogBinding.priceTypeAutoComplete?.setOnItemClickListener { _, _, _, _ ->
            selectedPriceType = dialogBinding.priceTypeAutoComplete?.text?.toString() ?: "Все цены"
        }

        dialogBinding.cityAutoComplete?.setOnItemClickListener { _, _, _, _ ->
            selectedCity = dialogBinding.cityAutoComplete?.text?.toString()
        }
    }

    private fun applyFilters() {
        val sourceList = fullBookList.toList()
        
        val filteredList = sourceList.filter { book ->
            // Search filter
            val searchQuery = currentSearchQuery.trim().lowercase()
            val matchesSearch = searchQuery.isEmpty() || listOf(
                book.title.lowercase(),
                book.author.lowercase(),
                book.city.lowercase(),
                book.ownerNickname?.lowercase() ?: ""
            ).any { it.contains(searchQuery) }

            // Book type filter
            val matchesRentType = when (selectedRentType) {
                "Для аренды" -> book.isForRent
                else -> true
            }

            // Price type filter
            val matchesPriceType = when (selectedPriceType) {
                "Бесплатно" -> !book.isForRent
                "Платно" -> book.isForRent
                else -> true
            }

            // City filter
            val matchesCity = selectedCity.isNullOrBlank() || book.city == selectedCity

            matchesSearch && matchesRentType && matchesPriceType && matchesCity
        }

        bookAdapter.submitList(filteredList)
        updateEmptyState(filteredList)
    }

    private fun updateEmptyState(books: List<Book>) {
        binding.emptyListTextView?.isVisible = books.isEmpty()
        binding.recentBooksRecyclerView?.isVisible = books.isNotEmpty()
        
        if (books.isEmpty()) {
            binding.emptyListTextView?.text = if (currentSearchQuery.isNotBlank() || 
                selectedRentType != "Все книги" || 
                selectedPriceType != "Все цены" || 
                selectedCity != null) {
                getString(R.string.no_books_found_filters)
            } else {
                getString(R.string.no_books_added_yet)
            }
        }
    }

    private fun observeViewModel() {
        viewModel.allBooks.observe(viewLifecycleOwner) { books ->
            Log.d("HomeFragment", "Observed allBooks update: ${books?.size ?: "null"} books")
            fullBookList = books ?: emptyList()
            applyFilters()
        }

        viewModel.isLoading.observe(viewLifecycleOwner) { isLoading ->
            updateUiState(isLoading = isLoading, isInitialLoad = fullBookList.isEmpty())
        }

        viewModel.errorMessage.observe(viewLifecycleOwner) { error ->
            if (error != null) {
                Log.e("HomeFragment", "Observed error: $error")
                if (fullBookList.isEmpty()) {
                    updateUiState(isLoading = false, error = error, isInitialLoad = true)
                } else {
                    Toast.makeText(context, error, Toast.LENGTH_LONG).show()
                }
                viewModel.clearErrorMessage()
            }
        }
    }


    private fun updateUiState(
        isLoading: Boolean,
        books: List<Book>? = null,
        error: String? = null,
        isInitialLoad: Boolean = false,
        isFilteredResult: Boolean = false
    ) {
        Log.d("HomeFragment", "updateUiState - isLoading: $isLoading, books.isNullOrEmpty: ${books.isNullOrEmpty()}, error: $error, isInitialLoad: $isInitialLoad, isFilteredResult: $isFilteredResult")
        binding.progressBar?.isVisible = isLoading

        if (!isLoading) {
            val isEmpty = books.isNullOrEmpty() // Результат (полный или отфильтрованный)
            if (isInitialLoad) {
                // Состояние после первой загрузки
                binding.recentBooksRecyclerView?.isVisible = !isEmpty || error != null
                binding.emptyListTextView?.isVisible = isEmpty || error != null
                binding.emptyListTextView?.text = when {
                    error != null -> error
                    isEmpty -> getString(R.string.no_books_added_yet)
                    else -> ""
                }
            } else if (isFilteredResult) {
                // Состояние после применения фильтра
                binding.recentBooksRecyclerView?.isVisible = !isEmpty
                binding.emptyListTextView?.isVisible = isEmpty && fullBookList.isNotEmpty()
                if(isEmpty && fullBookList.isNotEmpty()) {
                    binding.emptyListTextView?.text = getString(R.string.no_books_found_filters)
                }
            }
            // Если просто обновление списка без изменения фильтров, submitList обновит RecyclerView
        } else {
            // Пока грузится
            binding.recentBooksRecyclerView?.isVisible = false
            binding.emptyListTextView?.isVisible = false
        }
    }

    override fun onDestroyView() {
        super.onDestroyView()
        binding.recentBooksRecyclerView?.adapter = null
        _binding = null
        Log.d("HomeFragment", "onDestroyView called, binding set to null")
    }
}