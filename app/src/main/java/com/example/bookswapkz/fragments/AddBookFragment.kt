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
import com.example.bookswapkz.R
import com.example.bookswapkz.databinding.FragmentAddBookBinding
import com.example.bookswapkz.models.Book
import com.example.bookswapkz.viewmodels.BookViewModel
import dagger.hilt.android.AndroidEntryPoint

@AndroidEntryPoint
class AddBookFragment : Fragment() {
    private var _binding: FragmentAddBookBinding? = null
    private val binding get() = _binding!!
    private val viewModel: BookViewModel by viewModels({ requireActivity() })
    private var isForRent: Boolean = false

    override fun onCreateView(
        inflater: LayoutInflater, container: ViewGroup?, savedInstanceState: Bundle?
    ): View {
        _binding = FragmentAddBookBinding.inflate(inflater, container, false)
        return binding.root
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)
        setupSpinners()
        setupRentalSection()
        observeViewModel()
        binding.addBookButton.setOnClickListener { addBook() }
        
        // Настройка тулбара для возврата назад
        binding.toolbar.setNavigationOnClickListener {
            findNavController().popBackStack()
        }
    }

    private fun setupSpinners() {
        // Setup condition spinner
        val conditions = arrayOf("Отличное", "Хорошее", "Удовлетворительное", "Требует ремонта")
        val conditionAdapter = ArrayAdapter(requireContext(), android.R.layout.simple_spinner_item, conditions)
        conditionAdapter.setDropDownViewResource(android.R.layout.simple_spinner_dropdown_item)
        binding.conditionSpinner.adapter = conditionAdapter

        // Setup city spinner
        val cities = resources.getStringArray(R.array.cities)
        val cityAdapter = ArrayAdapter(requireContext(), android.R.layout.simple_spinner_item, cities)
        cityAdapter.setDropDownViewResource(android.R.layout.simple_spinner_dropdown_item)
        binding.citySpinner.adapter = cityAdapter
    }

    private fun setupRentalSection() {
        // По умолчанию "Обмен" активен
        binding.exchangeTypeButton.setOnClickListener {
            setExchangeMode()
        }
        
        binding.rentTypeButton.setOnClickListener {
            setRentMode()
        }
        
        // Устанавливаем изначальный режим "Обмен"
        setExchangeMode()

        binding.rentPriceEditText.addTextChangedListener(object : TextWatcher {
            override fun beforeTextChanged(s: CharSequence?, start: Int, count: Int, after: Int) {}
            override fun onTextChanged(s: CharSequence?, start: Int, before: Int, count: Int) {}
            override fun afterTextChanged(s: Editable?) { updateTotalPrice() }
        })

        binding.rentHoursEditText.addTextChangedListener(object : TextWatcher {
            override fun beforeTextChanged(s: CharSequence?, start: Int, count: Int, after: Int) {}
            override fun onTextChanged(s: CharSequence?, start: Int, before: Int, count: Int) {}
            override fun afterTextChanged(s: Editable?) { updateTotalPrice() }
        })
    }
    
    private fun setExchangeMode() {
        isForRent = false
        binding.exchangeTypeButton.backgroundTintList = resources.getColorStateList(R.color.teal, null)
        binding.exchangeTypeButton.setTextColor(resources.getColor(android.R.color.white, null))
        binding.rentTypeButton.backgroundTintList = resources.getColorStateList(android.R.color.white, null)
        binding.rentTypeButton.setTextColor(resources.getColor(android.R.color.black, null))
        binding.rentOptionsLayout.isVisible = false
        binding.rentPriceEditText.text?.clear()
        binding.rentHoursEditText.text?.clear()
        updateTotalPrice()
    }
    
    private fun setRentMode() {
        isForRent = true
        binding.rentTypeButton.backgroundTintList = resources.getColorStateList(R.color.teal, null)
        binding.rentTypeButton.setTextColor(resources.getColor(android.R.color.white, null))
        binding.exchangeTypeButton.backgroundTintList = resources.getColorStateList(android.R.color.white, null)
        binding.exchangeTypeButton.setTextColor(resources.getColor(android.R.color.black, null))
        binding.rentOptionsLayout.isVisible = true
    }

    private fun updateTotalPrice() {
        val price = binding.rentPriceEditText.text.toString().toDoubleOrNull() ?: 0.0
        val hours = binding.rentHoursEditText.text.toString().toIntOrNull() ?: 0
        val total = price * hours
        binding.totalPriceTextView.text = "Итого: $total ₸"
    }

    private fun observeViewModel() {
        viewModel.user.observe(viewLifecycleOwner) { user ->
            val isReadyToAdd = user != null && !user.phone.isNullOrBlank()
            binding.addBookButton.isEnabled = isReadyToAdd
        }

        viewModel.errorMessage.observe(viewLifecycleOwner) { error ->
            if (error != null) {
                binding.progressBar?.isVisible = false
                binding.addBookButton?.isEnabled = viewModel.user.value?.phone?.isNotBlank() == true
                Toast.makeText(requireContext(), "Ошибка: $error", Toast.LENGTH_LONG).show()
                viewModel.clearErrorMessage()
            }
        }
    }

    private fun addBook() {
        val title = binding.titleEditText.text.toString().trim()
        val author = binding.authorEditText.text.toString().trim()
        val condition = binding.conditionSpinner.selectedItem?.toString() ?: ""
        val city = binding.citySpinner.selectedItem?.toString() ?: ""

        if (title.isEmpty() || author.isEmpty() || condition.isEmpty() || city.isEmpty()) {
            Toast.makeText(requireContext(), "Заполните все поля", Toast.LENGTH_SHORT).show()
            return
        }

        val currentUser = viewModel.user.value
        val currentUserPhone = currentUser?.phone
        val currentUserNickname = currentUser?.nickname

        if (currentUser == null || currentUserPhone.isNullOrBlank() || currentUserNickname.isNullOrBlank()) {
            Toast.makeText(requireContext(), "Сначала заполните профиль", Toast.LENGTH_SHORT).show()
            return
        }

        val rentPrice = if (isForRent) binding.rentPriceEditText.text.toString().toDoubleOrNull() else null
        val rentHours = if (isForRent) binding.rentHoursEditText.text.toString().toIntOrNull() else null

        if (isForRent && (rentPrice == null || rentHours == null)) {
            Toast.makeText(requireContext(), "Заполните данные об аренде", Toast.LENGTH_SHORT).show()
            return
        }

        val book = Book(
            title = title,
            author = author,
            condition = condition,
            city = city,
            phone = currentUserPhone,
            ownerNickname = currentUserNickname,
            timestamp = System.currentTimeMillis(),
            isForRent = isForRent,
            rentPrice = rentPrice,
            rentPeriod = if (rentHours != null) "$rentHours ч." else null
        )

        binding.progressBar?.isVisible = true
        binding.addBookButton.isEnabled = false

        viewModel.addBook(book, null).observe(viewLifecycleOwner) { bookIdResult ->
            binding.progressBar?.isVisible = false
            
            bookIdResult.onSuccess { bookId ->
                Toast.makeText(requireContext(), "Книга успешно добавлена!", Toast.LENGTH_SHORT).show()
                Log.d("AddBookFragment", "Book added with ID: $bookId")
                findNavController().popBackStack()
            }
            
            bookIdResult.onFailure { error ->
                Log.e("AddBookFragment", "Failed to add book", error)
                binding.addBookButton.isEnabled = viewModel.user.value?.phone?.isNotBlank() == true
            }
        }
    }

    override fun onDestroyView() {
        super.onDestroyView()
        _binding = null
    }
}