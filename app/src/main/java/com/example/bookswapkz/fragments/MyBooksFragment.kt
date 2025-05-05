package com.example.bookswapkz.fragments

import android.os.Bundle
import android.util.Log
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.Toast
import androidx.core.content.ContextCompat
import androidx.core.view.isVisible
import androidx.fragment.app.Fragment
import androidx.fragment.app.viewModels
import androidx.navigation.fragment.findNavController
import androidx.recyclerview.widget.GridLayoutManager
import com.example.bookswapkz.R
import com.example.bookswapkz.adapters.BookAdapter
import com.example.bookswapkz.databinding.FragmentMyBooksBinding
import com.example.bookswapkz.models.Book
import com.example.bookswapkz.models.BookType
import com.example.bookswapkz.utils.GridSpacingItemDecoration
import com.example.bookswapkz.viewmodels.BookViewModel
import dagger.hilt.android.AndroidEntryPoint
import com.google.firebase.auth.FirebaseAuth

@AndroidEntryPoint
class MyBooksFragment : Fragment() {
    private var _binding: FragmentMyBooksBinding? = null
    private val binding get() = _binding!!
    private val viewModel: BookViewModel by viewModels({ requireActivity() })
    private val auth = FirebaseAuth.getInstance()

    private lateinit var availableBooksAdapter: BookAdapter
    private lateinit var exchangedBooksAdapter: BookAdapter
    private lateinit var rentedBooksAdapter: BookAdapter

    private enum class BookTab { AVAILABLE, EXCHANGED, RENTED }
    private var currentTab = BookTab.AVAILABLE

    override fun onCreateView(
        inflater: LayoutInflater, container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View {
        _binding = FragmentMyBooksBinding.inflate(inflater, container, false)
        return binding.root
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)

        // Check if user is logged in
        val currentUser = auth.currentUser
        Log.d("MyBooksFragment", "onViewCreated - Current user: ${currentUser?.uid}")

        if (currentUser == null) {
            Log.d("MyBooksFragment", "No user logged in, navigating to login")
            findNavController().navigate(R.id.action_myBooks_to_login)
            return
        }

        setupTabs()
        setupAdapters()
        setupNavigation()
        observeViewModel()

        // Явно запрашиваем загрузку книг пользователя
        viewModel.loadMyBooks()
    }

    private fun setupTabs() {
        // Настраиваем обработчики нажатий на вкладки
        binding.tabAvailable.setOnClickListener { switchToTab(BookTab.AVAILABLE) }
        binding.tabExchanged.setOnClickListener { switchToTab(BookTab.EXCHANGED) }
        binding.tabRented.setOnClickListener { switchToTab(BookTab.RENTED) }

        // По умолчанию открываем вкладку "Доступные"
        switchToTab(BookTab.AVAILABLE)
    }

    private fun switchToTab(tab: BookTab) {
        currentTab = tab
        updateTabAppearance()
        updateVisibleContent()
    }

    private fun updateTabAppearance() {
        // Сброс всех вкладок в неактивное состояние
        val inactiveColor = ContextCompat.getColor(requireContext(), android.R.color.darker_gray)
        val activeColor = ContextCompat.getColor(requireContext(), android.R.color.white)

        // Вкладка "Доступные"
        binding.tabAvailableIcon.setColorFilter(if (currentTab == BookTab.AVAILABLE) activeColor else inactiveColor)
        binding.tabAvailableText.setTextColor(if (currentTab == BookTab.AVAILABLE) activeColor else inactiveColor)
        binding.tabAvailableIndicator.visibility = if (currentTab == BookTab.AVAILABLE) View.VISIBLE else View.INVISIBLE

        // Вкладка "Обмененные"
        binding.tabExchangedIcon.setColorFilter(if (currentTab == BookTab.EXCHANGED) activeColor else inactiveColor)
        binding.tabExchangedText.setTextColor(if (currentTab == BookTab.EXCHANGED) activeColor else inactiveColor)
        binding.tabExchangedIndicator.visibility = if (currentTab == BookTab.EXCHANGED) View.VISIBLE else View.INVISIBLE

        // Вкладка "В аренде"
        binding.tabRentedIcon.setColorFilter(if (currentTab == BookTab.RENTED) activeColor else inactiveColor)
        binding.tabRentedText.setTextColor(if (currentTab == BookTab.RENTED) activeColor else inactiveColor)
        binding.tabRentedIndicator.visibility = if (currentTab == BookTab.RENTED) View.VISIBLE else View.INVISIBLE
    }

    private fun updateVisibleContent() {
        // Отображаем только контейнер активной вкладки
        binding.availableBooksContainer.visibility = if (currentTab == BookTab.AVAILABLE) View.VISIBLE else View.GONE
        binding.exchangedBooksContainer.visibility = if (currentTab == BookTab.EXCHANGED) View.VISIBLE else View.GONE
        binding.rentedBooksContainer.visibility = if (currentTab == BookTab.RENTED) View.VISIBLE else View.GONE
    }

    private fun setupAdapters() {
        // Setup available books adapter
        availableBooksAdapter = BookAdapter { book ->
            val action = MyBooksFragmentDirections.actionMyBooksToBookDetail(book)
            findNavController().navigate(action)
        }
        binding.availableBooksRecyclerView.layoutManager = GridLayoutManager(context, 2)
        binding.availableBooksRecyclerView.addItemDecoration(GridSpacingItemDecoration(2, resources.getDimensionPixelSize(R.dimen.grid_spacing), true))
        binding.availableBooksRecyclerView.adapter = availableBooksAdapter

        // Setup exchanged books adapter
        exchangedBooksAdapter = BookAdapter { book ->
            val action = MyBooksFragmentDirections.actionMyBooksToBookDetail(book)
            findNavController().navigate(action)
        }
        binding.exchangedBooksRecyclerView.layoutManager = GridLayoutManager(context, 2)
        binding.exchangedBooksRecyclerView.addItemDecoration(GridSpacingItemDecoration(2, resources.getDimensionPixelSize(R.dimen.grid_spacing), true))
        binding.exchangedBooksRecyclerView.adapter = exchangedBooksAdapter

        // Setup rented books adapter
        rentedBooksAdapter = BookAdapter { book ->
            val action = MyBooksFragmentDirections.actionMyBooksToBookDetail(book)
            findNavController().navigate(action)
        }
        binding.rentedBooksRecyclerView.layoutManager = GridLayoutManager(context, 2)
        binding.rentedBooksRecyclerView.addItemDecoration(GridSpacingItemDecoration(2, resources.getDimensionPixelSize(R.dimen.grid_spacing), true))
        binding.rentedBooksRecyclerView.adapter = rentedBooksAdapter
    }

    private fun setupNavigation() {
        // Настройка кнопки добавления книги
        binding.addBookFab.setOnClickListener {
            findNavController().navigate(R.id.addBookFragment)
        }
    }

    private fun observeViewModel() {
        // Show loading state
        binding.progressBar.isVisible = true
        Log.d("MyBooksFragment", "Starting to observe myBooks with viewLifecycleOwner")

        viewModel.user.observe(viewLifecycleOwner) { user ->
            Log.d("MyBooksFragment", "User data updated: ${user?.nickname}")
        }

        viewModel.myBooks.observe(viewLifecycleOwner) { allBooks ->
            Log.d("MyBooksFragment", "Received books update. Total books: ${allBooks.size}")
            binding.progressBar.isVisible = false

            // Разделяем книги по категориям с учетом bookType
            val availableBooks = allBooks.filter { book ->
                !book.isRented && !book.isExchanged && (book.bookType == BookType.EXCHANGE || book.bookType == BookType.BOTH)
            }
            val exchangedBooks = allBooks.filter { book ->
                book.isExchanged || (book.bookType == BookType.EXCHANGE && !book.isRented)
            }
            val rentedBooks = allBooks.filter { book ->
                book.isRented || (book.bookType == BookType.RENT && !book.isExchanged) || (book.bookType == BookType.BOTH && book.isRented)
            }

            Log.d("MyBooksFragment", "Split books - Available: ${availableBooks.size}, " +
                    "Exchanged: ${exchangedBooks.size}, Rented: ${rentedBooks.size}")

            // Update available books section
            availableBooksAdapter.submitList(availableBooks)
            binding.emptyAvailableBooksTextView.isVisible = availableBooks.isEmpty()
            binding.availableBooksRecyclerView.isVisible = availableBooks.isNotEmpty()

            // Update exchanged books section
            exchangedBooksAdapter.submitList(exchangedBooks)
            binding.emptyExchangedBooksTextView.isVisible = exchangedBooks.isEmpty()
            binding.exchangedBooksRecyclerView.isVisible = exchangedBooks.isNotEmpty()

            // Update rented books section
            rentedBooksAdapter.submitList(rentedBooks)
            binding.emptyRentedBooksTextView.isVisible = rentedBooks.isEmpty()
            binding.rentedBooksRecyclerView.isVisible = rentedBooks.isNotEmpty()

            Log.d("MyBooksFragment", "UI updated with books data")
        }

        // Observe loading state
        viewModel.isLoading.observe(viewLifecycleOwner) { isLoading ->
            Log.d("MyBooksFragment", "Loading state changed: $isLoading")
            binding.progressBar.isVisible = isLoading
        }

        // Observe errors
        viewModel.errorMessage.observe(viewLifecycleOwner) { error ->
            if (error != null) {
                Log.e("MyBooksFragment", "Error received: $error")
                binding.progressBar.isVisible = false
                if (viewModel.myBooks.value.isNullOrEmpty()) {
                    binding.emptyAvailableBooksTextView.isVisible = true
                    binding.emptyAvailableBooksTextView.text = error
                    binding.availableBooksContainer.visibility = View.VISIBLE
                    binding.exchangedBooksContainer.visibility = View.GONE
                    binding.rentedBooksContainer.visibility = View.GONE
                } else {
                    Toast.makeText(context, error, Toast.LENGTH_LONG).show()
                }
                viewModel.clearErrorMessage()
            }
        }
    }

    override fun onDestroyView() {
        super.onDestroyView()
        _binding = null
    }
}