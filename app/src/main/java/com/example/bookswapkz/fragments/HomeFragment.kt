package com.example.bookswapkz.fragments

import android.os.Bundle
import android.text.Editable
import android.text.TextWatcher
import android.util.Log
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.ArrayAdapter
import android.widget.AutoCompleteTextView
import android.widget.Toast
import androidx.core.view.isVisible
import androidx.fragment.app.Fragment
import androidx.fragment.app.viewModels
import androidx.lifecycle.Observer
import androidx.navigation.fragment.findNavController
import androidx.recyclerview.widget.GridLayoutManager
import com.example.bookswapkz.R
import com.example.bookswapkz.adapters.BookAdapter // Убедитесь, что пакет правильный
import com.example.bookswapkz.databinding.DialogFiltersBinding
import com.example.bookswapkz.databinding.FragmentHomeBinding
import com.example.bookswapkz.models.Book // Убедитесь, что модель импортирована
import com.example.bookswapkz.utils.GridSpacingItemDecoration // Убедитесь, что утилита импортирована
import com.example.bookswapkz.viewmodels.BookViewModel // Убедитесь, что ViewModel импортирована
import com.google.android.material.chip.Chip
import com.google.android.material.dialog.MaterialAlertDialogBuilder
import dagger.hilt.android.AndroidEntryPoint

@AndroidEntryPoint
class HomeFragment : Fragment() {

    private var _binding: FragmentHomeBinding? = null
    private val binding get() = _binding!!

    private val viewModel: BookViewModel by viewModels({ requireActivity() })
    private lateinit var bookAdapter: BookAdapter
    private var fullBookList: List<Book> = emptyList()

    private object FilterType {
        const val ALL_BOOKS = "All Books"
        const val FOR_RENT = "For Rent"
        const val ALL_PRICES = "All Prices"
        const val FREE = "Free"
        const val PAID = "Paid"
    }
    private var currentSearchQuery: String = ""
    private var selectedRentType: String = FilterType.ALL_BOOKS
    private var selectedPriceType: String = FilterType.ALL_PRICES
    private var selectedCity: String? = null

    override fun onCreateView(
        inflater: LayoutInflater,
        container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View {
        _binding = FragmentHomeBinding.inflate(inflater, container, false)
        return binding.root
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)

        setupRecyclerView()
        setupSearch()
        setupFilterButton()
        setupFab()
        observeViewModel()

        updateUiState(isLoading = true, isInitialLoad = true)
    }

    private fun setupRecyclerView() {
        bookAdapter = BookAdapter { book ->
            handleBookClick(book)
        }
        binding.recentBooksRecyclerView?.apply {
            layoutManager = GridLayoutManager(requireContext(), 2)
            adapter = bookAdapter
            // Убедитесь, что R.dimen.grid_spacing существует
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

    private fun setupFab() {
        binding.addBookFab?.setOnClickListener {
            findNavController().navigate(HomeFragmentDirections.actionHomeToAddBook())
        }
        // animateFabIn() // Анимация пока убрана
    }

    private fun showFilterDialog() {
        // --- Код showFilterDialog остается таким же, как в предыдущем ответе ---
        // --- Он использует временные переменные tempSelected... ---
        // --- и кнопки AlertDialog "Применить", "Отмена", "Сбросить" ---
        // --- для обновления основных переменных selected... и вызова applyFilters ---
        val dialogBinding = DialogFiltersBinding.inflate(layoutInflater)
        val dialogView = dialogBinding.root

        val cities = resources.getStringArray(R.array.cities)
        val cityAdapter = ArrayAdapter(requireContext(), android.R.layout.simple_dropdown_item_1line, cities)
        dialogBinding.cityAutoComplete?.setAdapter(cityAdapter)

        // Восстановление состояния
        dialogBinding.cityAutoComplete?.setText(selectedCity ?: "", false)
        when (selectedRentType) {
            FilterType.FOR_RENT -> dialogBinding.forRentChip?.isChecked = true
            else -> dialogBinding.allBooksChip?.isChecked = true
        }
        val priceGroupEnabled = selectedRentType == FilterType.FOR_RENT
        dialogBinding.priceTypeChipGroup?.isEnabled = priceGroupEnabled
        dialogBinding.allPricesChip?.isEnabled = priceGroupEnabled
        dialogBinding.freeChip?.isEnabled = priceGroupEnabled
        dialogBinding.paidChip?.isEnabled = priceGroupEnabled
        if (priceGroupEnabled) {
            when (selectedPriceType) {
                FilterType.FREE -> dialogBinding.freeChip?.isChecked = true
                FilterType.PAID -> dialogBinding.paidChip?.isChecked = true
                else -> dialogBinding.allPricesChip?.isChecked = true
            }
        } else {
            dialogBinding.allPricesChip?.isChecked = true
        }

        // Временные переменные для диалога
        var tempSelectedRentType = selectedRentType
        var tempSelectedPriceType = selectedPriceType
        var tempSelectedCity = selectedCity

        // Слушатели
        dialogBinding.rentTypeChipGroup?.setOnCheckedStateChangeListener { group, checkedIds ->
            val checkedId = checkedIds.firstOrNull()
            tempSelectedRentType = if (checkedId == R.id.forRentChip) FilterType.FOR_RENT else FilterType.ALL_BOOKS
            val isRentSelected = tempSelectedRentType == FilterType.FOR_RENT
            dialogBinding.priceTypeChipGroup?.isEnabled = isRentSelected
            dialogBinding.allPricesChip?.isEnabled = isRentSelected
            dialogBinding.freeChip?.isEnabled = isRentSelected
            dialogBinding.paidChip?.isEnabled = isRentSelected
            if (!isRentSelected) {
                tempSelectedPriceType = FilterType.ALL_PRICES
                dialogBinding.allPricesChip?.isChecked = true
            } else if (dialogBinding.priceTypeChipGroup?.checkedChipId == View.NO_ID) {
                dialogBinding.allPricesChip?.isChecked = true
                tempSelectedPriceType = FilterType.ALL_PRICES
            }
        }
        dialogBinding.priceTypeChipGroup?.setOnCheckedStateChangeListener { group, checkedIds ->
            val checkedId = checkedIds.firstOrNull()
            tempSelectedPriceType = when(checkedId) {
                R.id.freeChip -> FilterType.FREE
                R.id.paidChip -> FilterType.PAID
                else -> FilterType.ALL_PRICES
            }
        }
        dialogBinding.cityAutoComplete?.setOnItemClickListener { _, _, position, _ ->
            tempSelectedCity = cityAdapter.getItem(position)
        }
        dialogBinding.cityAutoComplete?.addTextChangedListener(object: TextWatcher {
            override fun beforeTextChanged(s: CharSequence?, start: Int, count: Int, after: Int) {}
            override fun onTextChanged(s: CharSequence?, start: Int, before: Int, count: Int) {}
            override fun afterTextChanged(s: Editable?) {
                val inputText = s?.toString() ?: ""
                if (cities.contains(inputText)) { tempSelectedCity = inputText }
                else if (inputText.isBlank()) { tempSelectedCity = null }
                else { if (!cities.contains(tempSelectedCity)) { tempSelectedCity = null } }
            }
        })

        // Кнопки AlertDialog
        MaterialAlertDialogBuilder(requireContext())
            .setView(dialogView)
            .setTitle("Фильтры")
            .setPositiveButton("Применить") { dialogInterface, _ ->
                selectedRentType = tempSelectedRentType
                selectedPriceType = tempSelectedPriceType
                selectedCity = tempSelectedCity // Сохраняем город из AutoCompleteTextView
                applyFilters()
                dialogInterface.dismiss()
            }
            .setNegativeButton("Отмена", null)
            .setNeutralButton("Сбросить") { dialogInterface, _ ->
                selectedRentType = FilterType.ALL_BOOKS
                selectedPriceType = FilterType.ALL_PRICES
                selectedCity = null
                applyFilters()
                dialogInterface.dismiss()
            }
            .show()
    }

    // --- ИСПРАВЛЕННЫЙ applyFilters (БЕЗ searchable полей) ---
    private fun applyFilters() {
        val sourceList = fullBookList.toList()

        val filteredList = sourceList.filter { book ->
            // 1. Фильтр Поиска (стандартный contains, ignoreCase)
            val matchesSearch = currentSearchQuery.isEmpty() ||
                    book.title.contains(currentSearchQuery, ignoreCase = true) ||
                    book.author.contains(currentSearchQuery, ignoreCase = true) ||
                    book.city.contains(currentSearchQuery, ignoreCase = true) ||
                    (book.ownerNickname?.contains(currentSearchQuery, ignoreCase = true) ?: false) // По нику владельца

            // 2. Фильтр Города
            val matchesCity = selectedCity.isNullOrBlank() ||
                    book.city.equals(selectedCity, ignoreCase = true)

            // 3. Фильтр Типа (Аренда / Все)
            val matchesRentType = when (selectedRentType) {
                FilterType.FOR_RENT -> book.isForRent
                else -> true // ALL_BOOKS или null
            }

            // 4. Фильтр Цены (только если выбрана Аренда)
            var matchesPriceType = true
            if (selectedRentType == FilterType.FOR_RENT) {
                matchesPriceType = when (selectedPriceType) {
                    FilterType.FREE -> book.rentPrice == null || book.rentPrice <= 0.0
                    FilterType.PAID -> book.rentPrice != null && book.rentPrice > 0.0
                    else -> true // ALL_PRICES или null
                }
            }

            matchesSearch && matchesCity && matchesRentType && matchesPriceType
        }

        Log.d("HomeFragment", "Filtering complete. Source: ${sourceList.size}, Filtered: ${filteredList.size}, Query: '$currentSearchQuery', Rent: $selectedRentType, Price: $selectedPriceType, City: $selectedCity")

        bookAdapter.submitList(filteredList)

        // Обновляем UI после фильтрации
        updateUiState(isLoading = false, books = filteredList, isFilteredResult = true)
    }
    // --- КОНЕЦ ИСПРАВЛЕННОГО applyFilters ---


    private fun observeViewModel() {
        viewModel.allBooks.observe(viewLifecycleOwner) { books ->
            Log.d("HomeFragment", "Observed allBooks update: ${books?.size ?: "null"} books")
            fullBookList = books ?: emptyList()
            updateUiState(isLoading = false, books = fullBookList, isInitialLoad = true)
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