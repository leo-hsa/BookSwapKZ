package com.example.bookswapkz.fragments

// Импорты - убедитесь, что все необходимые присутствуют
import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import androidx.core.view.isVisible
import androidx.fragment.app.Fragment
import androidx.fragment.app.viewModels
import androidx.navigation.fragment.findNavController
import androidx.recyclerview.widget.LinearLayoutManager
import androidx.dynamicanimation.animation.SpringAnimation
import androidx.dynamicanimation.animation.SpringForce
import com.example.bookswapkz.R // Убедитесь, что R импортирован
import com.example.bookswapkz.adapters.BookAdapter // Убедитесь, что пакет адаптера правильный
import com.example.bookswapkz.databinding.FragmentHomeBinding
import com.example.bookswapkz.models.Book
import com.example.bookswapkz.viewmodels.BookViewModel // Убедитесь, что пакет ViewModel правильный
import com.google.android.material.chip.Chip
import com.google.android.material.chip.ChipGroup

class HomeFragment : Fragment() {

    // Используем nullable тип для _binding и безопасное разыменование через get()
    // Это стандартный и безопасный способ работы с ViewBinding во фрагментах
    private var _binding: FragmentHomeBinding? = null
    private val binding get() = _binding!!

    private val viewModel: BookViewModel by viewModels()
    private lateinit var bookAdapter: BookAdapter
    private var currentFilters = mutableSetOf<String>()

    override fun onCreateView(
        inflater: LayoutInflater, container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View {
        _binding = FragmentHomeBinding.inflate(inflater, container, false)
        return binding.root // binding здесь гарантированно не null
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)

        setupRecyclerView()
        setupFab()
        setupFilters()
        observeViewModel()

        updateUiState(isLoading = true)
        animateFabIn()
    }

    // Настройка RecyclerView
    private fun setupRecyclerView() {
        bookAdapter = BookAdapter { book ->
            handleBookClick(book)
        }
        binding.recentBooksRecyclerView?.apply {
            layoutManager = androidx.recyclerview.widget.GridLayoutManager(requireContext(), 2)
            adapter = bookAdapter
        }
    }

    // Обработка клика по элементу списка
    private fun handleBookClick(book: Book) {
        val action = HomeFragmentDirections.actionHomeToBookDetail(book)
        findNavController().navigate(action)
    }


    // Настройка FloatingActionButton
    private fun setupFab() {
        // Используем ?. для setOnClickListener
        binding.addBookFab?.setOnClickListener {
            val action = HomeFragmentDirections.actionHomeToAddBook()
            findNavController().navigate(action)
        }
    }

    private fun setupFilters() {
        // Setup filter button click listener
        binding.filterButton?.setOnClickListener {
            // Show filter dialog or bottom sheet
            showFilterDialog()
        }

        // Setup chip group listener
        binding.filterChipGroup?.setOnCheckedChangeListener { group, checkedId ->
            val chip = group.findViewById<Chip>(checkedId)
            if (chip != null) {
                val filterText = chip.text.toString()
                if (chip.isChecked) {
                    currentFilters.add(filterText)
                } else {
                    currentFilters.remove(filterText)
                }
                applyFilters()
            }
        }
    }

    private fun showFilterDialog() {
        // TODO: Implement filter dialog or bottom sheet
        // This could show additional filter options like price range, distance, etc.
    }

    private fun applyFilters() {
        viewModel.allBooks.value?.let { books ->
            val filteredBooks = if (currentFilters.isEmpty()) {
                books
            } else {
                books.filter { book ->
                    currentFilters.any { filter ->
                        when (filter) {
                            "Available Now" -> !book.isRented
                            "Fiction" -> book.condition.equals("Fiction", ignoreCase = true)
                            "Non-Fiction" -> book.condition.equals("Non-Fiction", ignoreCase = true)
                            "Science" -> book.condition.equals("Science", ignoreCase = true)
                            "History" -> book.condition.equals("History", ignoreCase = true)
                            else -> false
                        }
                    }
                }
            }
            bookAdapter.submitList(filteredBooks)
        }
    }

    // Наблюдение за данными из ViewModel
    private fun observeViewModel() {
        viewModel.allBooks.observe(viewLifecycleOwner) { books ->
            updateUiState(isLoading = false, books = books)
            applyFilters() // Apply filters when new data arrives
        }
        // viewModel.errorState.observe ...
    }

    // Обновление UI в зависимости от состояния
    private fun updateUiState(isLoading: Boolean, books: List<Book>? = null, error: String? = null) {
        binding.recentBooksRecyclerView?.isVisible = when {
            isLoading -> false
            error != null -> false
            books.isNullOrEmpty() -> false
            else -> true
        }

        if (!isLoading && error == null && !books.isNullOrEmpty()) {
            bookAdapter.submitList(books)
        }
    }


    // Анимация появления FAB
    private fun animateFabIn() {
        // Используем ?.apply
        binding.addBookFab?.apply {
            translationY = 300f
            alpha = 0f
            val tyAnimation = SpringAnimation(this, SpringAnimation.TRANSLATION_Y, 0f).apply {
                spring.stiffness = SpringForce.STIFFNESS_LOW
                spring.dampingRatio = SpringForce.DAMPING_RATIO_MEDIUM_BOUNCY
            }
            animate()
                .alpha(1f)
                .setDuration(400)
                .setStartDelay(100)
                .withEndAction { translationY = 0f }
                .start()
            tyAnimation.start()
        }
    }

    override fun onDestroyView() {
        super.onDestroyView()
        // Используем ?. при доступе к адаптеру RecyclerView перед обнулением binding
        binding.recentBooksRecyclerView?.adapter = null // Обнуляем адаптер RecyclerView
        _binding = null // ОЧЕНЬ ВАЖНО обнулить _binding для предотвращения утечек памяти
    }
}