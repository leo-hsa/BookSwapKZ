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
import com.example.bookswapkz.models.BookType
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
    private var selectedCondition: String? = null
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
                selectedRentType = getString(R.string.all_books)
                selectedPriceType = getString(R.string.all_prices)
                selectedCondition = null
                selectedCity = null
                applyFilters()
            }
            .show()
    }

    private fun setupFilterDropdowns(dialogBinding: DialogFiltersBinding) {
        // Book types
        val rentTypes = arrayOf(
            getString(R.string.all_books),
            getString(R.string.for_rent),
            getString(R.string.for_exchange)
        )
        val rentAdapter = ArrayAdapter(requireContext(), android.R.layout.simple_dropdown_item_1line, rentTypes)
        dialogBinding.rentTypeAutoComplete?.setAdapter(rentAdapter)
        dialogBinding.rentTypeAutoComplete?.setText(selectedRentType, false)

        // Price types
        val priceTypes = arrayOf(
            getString(R.string.all_prices),
            getString(R.string.free),
            getString(R.string.price_range_low),  // До 500 ₸
            getString(R.string.price_range_medium),  // 500-1000 ₸
            getString(R.string.price_range_high)  // Свыше 1000 ₸
        )
        val priceAdapter = ArrayAdapter(requireContext(), android.R.layout.simple_dropdown_item_1line, priceTypes)
        dialogBinding.priceTypeAutoComplete?.setAdapter(priceAdapter)
        dialogBinding.priceTypeAutoComplete?.setText(selectedPriceType, false)

        // Conditions
        val conditions = arrayOf(getString(R.string.all_conditions)) + resources.getStringArray(R.array.book_conditions)
        val conditionAdapter = ArrayAdapter(requireContext(), android.R.layout.simple_dropdown_item_1line, conditions)
        dialogBinding.conditionAutoComplete?.setAdapter(conditionAdapter)
        dialogBinding.conditionAutoComplete?.setText(selectedCondition ?: getString(R.string.all_conditions), false)

        // Cities
        val cities = arrayOf(getString(R.string.all_cities)) + resources.getStringArray(R.array.cities)
        val cityAdapter = ArrayAdapter(requireContext(), android.R.layout.simple_dropdown_item_1line, cities)
        dialogBinding.cityAutoComplete?.setAdapter(cityAdapter)
        dialogBinding.cityAutoComplete?.setText(selectedCity ?: getString(R.string.all_cities), false)

        // Handle selections
        dialogBinding.rentTypeAutoComplete?.setOnItemClickListener { _, _, _, _ ->
            selectedRentType = dialogBinding.rentTypeAutoComplete?.text?.toString() ?: getString(R.string.all_books)
        }

        dialogBinding.priceTypeAutoComplete?.setOnItemClickListener { _, _, _, _ ->
            selectedPriceType = dialogBinding.priceTypeAutoComplete?.text?.toString() ?: getString(R.string.all_prices)
        }

        dialogBinding.conditionAutoComplete?.setOnItemClickListener { _, _, _, _ ->
            val selected = dialogBinding.conditionAutoComplete?.text?.toString()
            selectedCondition = if (selected == getString(R.string.all_conditions)) null else selected
        }

        dialogBinding.cityAutoComplete?.setOnItemClickListener { _, _, _, _ ->
            val selected = dialogBinding.cityAutoComplete?.text?.toString()
            selectedCity = if (selected == getString(R.string.all_cities)) null else selected
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
                getString(R.string.for_rent) -> book.bookType == BookType.RENT || (book.bookType == BookType.BOTH && (book.rentPrice ?: 0.0) > 0)
                getString(R.string.for_exchange) -> book.bookType == BookType.EXCHANGE || (book.bookType == BookType.BOTH && (book.rentPrice ?: 0.0) == 0.0)
                else -> true
            }

            // Price type filter
            val matchesPriceType = when (selectedPriceType) {
                getString(R.string.free) -> {
                    if (book.bookType == BookType.RENT || book.bookType == BookType.BOTH) {
                        (book.rentPrice ?: 0.0) == 0.0
                    } else {
                        true // Для EXCHANGE всегда бесплатно
                    }
                }
                getString(R.string.price_range_low) -> {
                    if (book.bookType == BookType.RENT || book.bookType == BookType.BOTH) {
                        val price = book.rentPrice ?: 0.0
                        price in 0.0..500.0
                    } else {
                        false // EXCHANGE не подпадает под ценовые диапазоны
                    }
                }
                getString(R.string.price_range_medium) -> {
                    if (book.bookType == BookType.RENT || book.bookType == BookType.BOTH) {
                        val price = book.rentPrice ?: 0.0
                        price in 500.0..1000.0
                    } else {
                        false
                    }
                }
                getString(R.string.price_range_high) -> {
                    if (book.bookType == BookType.RENT || book.bookType == BookType.BOTH) {
                        val price = book.rentPrice ?: 0.0
                        price > 1000.0
                    } else {
                        false
                    }
                }
                else -> true
            }

            // Condition filter
            val matchesCondition = selectedCondition.isNullOrBlank() || book.condition == selectedCondition

            // City filter
            val matchesCity = selectedCity.isNullOrBlank() || book.city == selectedCity

            matchesSearch && matchesRentType && matchesPriceType && matchesCondition && matchesCity
        }

        bookAdapter.submitList(filteredList)
        updateEmptyState(filteredList)
    }

    private fun updateEmptyState(books: List<Book>) {
        binding.emptyListTextView?.isVisible = books.isEmpty()
        binding.recentBooksRecyclerView?.isVisible = books.isNotEmpty()

        if (books.isEmpty()) {
            binding.emptyListTextView?.text = if (currentSearchQuery.isNotBlank() ||
                selectedRentType != getString(R.string.all_books) ||
                selectedPriceType != getString(R.string.all_prices) ||
                selectedCondition != null ||
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
            val isEmpty = books.isNullOrEmpty()
            if (isInitialLoad) {
                binding.recentBooksRecyclerView?.isVisible = !isEmpty || error != null
                binding.emptyListTextView?.isVisible = isEmpty || error != null
                binding.emptyListTextView?.text = when {
                    error != null -> error
                    isEmpty -> getString(R.string.no_books_added_yet)
                    else -> ""
                }
            } else if (isFilteredResult) {
                binding.recentBooksRecyclerView?.isVisible = !isEmpty
                binding.emptyListTextView?.isVisible = isEmpty && fullBookList.isNotEmpty()
                if (isEmpty && fullBookList.isNotEmpty()) {
                    binding.emptyListTextView?.text = getString(R.string.no_books_found_filters)
                }
            }
        } else {
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