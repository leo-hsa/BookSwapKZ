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

class HomeFragment : Fragment() {

    // Используем nullable тип для _binding и безопасное разыменование через get()
    // Это стандартный и безопасный способ работы с ViewBinding во фрагментах
    private var _binding: FragmentHomeBinding? = null
    private val binding get() = _binding!!

    private val viewModel: BookViewModel by viewModels()
    private lateinit var bookAdapter: BookAdapter

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
        observeViewModel()

        updateUiState(isLoading = true)
        animateFabIn()
    }

    // Настройка RecyclerView
    private fun setupRecyclerView() {
        bookAdapter = BookAdapter { book ->
            handleBookClick(book)
        }
        // Используем безопасный вызов ?.apply для доступа к элементам binding
        binding.recyclerView?.apply {
            layoutManager = LinearLayoutManager(requireContext())
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
        binding.fab?.setOnClickListener {
            val action = HomeFragmentDirections.actionHomeToAddBook()
            findNavController().navigate(action)
        }
    }

    // Наблюдение за данными из ViewModel
    private fun observeViewModel() {
        viewModel.allBooks.observe(viewLifecycleOwner) { books ->
            updateUiState(isLoading = false, books = books)
        }
        // viewModel.errorState.observe ...
    }

    // Обновление UI в зависимости от состояния
    private fun updateUiState(isLoading: Boolean, books: List<Book>? = null, error: String? = null) {
        // Используем ?. при доступе ко всем View из binding
        binding.progressBar?.isVisible = isLoading
        if (!isLoading) {
            if (error != null) {
                binding.emptyListTextView?.isVisible = true
                binding.emptyListTextView?.text = error
                binding.recyclerView?.isVisible = false
            } else if (books.isNullOrEmpty()) {
                binding.emptyListTextView?.isVisible = true
                binding.emptyListTextView?.text = getString(R.string.no_books_found)
                binding.recyclerView?.isVisible = false
            } else {
                binding.emptyListTextView?.isVisible = false
                binding.recyclerView?.isVisible = true
                // submitList можно вызвать безопасно, так как bookAdapter - lateinit var
                // и если код дошел сюда без падения, адаптер уже инициализирован
                bookAdapter.submitList(books)
            }
        } else {
            binding.recyclerView?.isVisible = false
            binding.emptyListTextView?.isVisible = false
        }
    }


    // Анимация появления FAB
    private fun animateFabIn() {
        // Используем ?.apply
        binding.fab?.apply {
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
        binding.recyclerView?.adapter = null // Обнуляем адаптер RecyclerView
        _binding = null // ОЧЕНЬ ВАЖНО обнулить _binding для предотвращения утечек памяти
    }
}