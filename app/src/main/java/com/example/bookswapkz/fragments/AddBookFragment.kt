package com.example.bookswapkz.fragments

// import android.net.Uri // Убран
import android.os.Bundle
import android.text.Editable
import android.text.TextWatcher
import android.util.Log
import android.util.Patterns
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.ArrayAdapter
import android.widget.Toast
// import androidx.activity.result.PickVisualMediaRequest // Убран
// import androidx.activity.result.contract.ActivityResultContracts // Убран
import androidx.core.view.isVisible
import androidx.fragment.app.Fragment
import androidx.fragment.app.viewModels
import androidx.lifecycle.Observer
import androidx.navigation.fragment.findNavController
// import com.bumptech.glide.Glide // Убран
import com.example.bookswapkz.R
import com.example.bookswapkz.databinding.FragmentAddBookBinding
import com.example.bookswapkz.models.Book
import com.example.bookswapkz.viewmodels.BookViewModel
import dagger.hilt.android.AndroidEntryPoint
import java.util.Date

@AndroidEntryPoint
class AddBookFragment : Fragment() {

    private var _binding: FragmentAddBookBinding? = null
    private val binding get() = _binding!!
    private val viewModel: BookViewModel by viewModels({ requireActivity() })

    // private var selectedImageUri: Uri? = null // Убрано
    // private val pickMediaLauncher = ... // Убрано

    override fun onCreateView(
        inflater: LayoutInflater, container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View {
        _binding = FragmentAddBookBinding.inflate(inflater, container, false)
        return binding.root
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)

        setupSpinners()
        setupRentalOptions()

        // --- УБРАНЫ слушатели и View для картинок ---
        // binding.selectImageButton?.setOnClickListener { launchImagePicker() }
        // binding.bookImage?.isVisible = false // Скрываем ImageView, если он еще есть в макете
        // ---

        binding.addBookButton.isEnabled = false
        binding.addBookButton.setOnClickListener {
            addBook()
        }

        viewModel.user.observe(viewLifecycleOwner, Observer { user ->
            val isReadyToAdd = user != null && !user.phone.isNullOrBlank()
            binding.addBookButton.isEnabled = isReadyToAdd
            Log.d("AddBookFragment", "User observed: ${user?.nickname}, Phone: ${user?.phone}, Button enabled: $isReadyToAdd")
            if (user != null && user.phone.isNullOrBlank() && _binding?.addBookButton?.isEnabled == false) {
                Toast.makeText(context, "Добавьте номер телефона в профиле", Toast.LENGTH_SHORT).show()
            }
        })

        viewModel.errorMessage.observe(viewLifecycleOwner) { error ->
            if (error != null) {
                binding.progressBar.isVisible = false
                binding.addBookButton.isEnabled = viewModel.user.value != null && !viewModel.user.value?.phone.isNullOrBlank()
                Toast.makeText(requireContext(), "Ошибка: $error", Toast.LENGTH_LONG).show()
                viewModel.clearErrorMessage()
            }
        }
    }

    private fun setupSpinners() {
        // ... (код настройки спиннеров без изменений) ...
        val conditions = resources.getStringArray(R.array.book_conditions)
        val conditionAdapter = ArrayAdapter(requireContext(), android.R.layout.simple_spinner_item, conditions)
        conditionAdapter.setDropDownViewResource(android.R.layout.simple_spinner_dropdown_item)
        binding.conditionSpinner.adapter = conditionAdapter

        val cities = resources.getStringArray(R.array.cities)
        val cityAdapter = ArrayAdapter(requireContext(), android.R.layout.simple_spinner_item, cities)
        cityAdapter.setDropDownViewResource(android.R.layout.simple_spinner_dropdown_item)
        binding.citySpinner.adapter = cityAdapter
    }

    private fun setupRentalOptions() {
        // Setup the rent switch
        binding.rentSwitch.setOnCheckedChangeListener { _, isChecked ->
            binding.rentOptionsLayout.isVisible = isChecked
            updateTotalPrice()
        }

        // Setup text change listeners for calculating total price
        binding.rentPriceEditText.addTextChangedListener(object : TextWatcher {
            override fun beforeTextChanged(s: CharSequence?, start: Int, count: Int, after: Int) {}
            override fun onTextChanged(s: CharSequence?, start: Int, before: Int, count: Int) {}
            override fun afterTextChanged(s: Editable?) {
                updateTotalPrice()
            }
        })

        binding.rentDurationEditText.addTextChangedListener(object : TextWatcher {
            override fun beforeTextChanged(s: CharSequence?, start: Int, count: Int, after: Int) {}
            override fun onTextChanged(s: CharSequence?, start: Int, before: Int, count: Int) {}
            override fun afterTextChanged(s: Editable?) {
                updateTotalPrice()
            }
        })
    }

    private fun updateTotalPrice() {
        if (binding.rentSwitch.isChecked) {
            val pricePerHour = binding.rentPriceEditText.text.toString().toDoubleOrNull() ?: 0.0
            val durationHours = binding.rentDurationEditText.text.toString().toIntOrNull() ?: 0
            val totalPrice = pricePerHour * durationHours
            binding.totalPriceTextView.text = getString(R.string.rent_total_hint, totalPrice)
        }
    }

    // private fun launchImagePicker() { ... } // Убран

    private fun addBook() {
        val title = binding.titleEditText.text.toString().trim()
        val author = binding.authorEditText.text.toString().trim()
        val condition = binding.conditionSpinner.selectedItem?.toString() ?: ""
        val city = binding.citySpinner.selectedItem?.toString() ?: ""
        // val description = ...

        if (title.isEmpty() || author.isEmpty() || condition.isEmpty() || city.isEmpty()) {
            Toast.makeText(requireContext(), "Пожалуйста, заполните поля: Название, Автор, Состояние, Город", Toast.LENGTH_SHORT).show()
            return
        }

        val currentUser = viewModel.user.value
        val currentUserPhone = currentUser?.phone
        val currentUserNickname = currentUser?.nickname

        if (currentUser == null || currentUserPhone.isNullOrBlank() || currentUserNickname.isNullOrBlank()) {
            Toast.makeText(requireContext(), "Ошибка: Данные пользователя недоступны. Перезайдите.", Toast.LENGTH_LONG).show()
            binding.addBookButton.isEnabled = false
            Log.e("AddBookFragment", "Add attempt failed: currentUser, phone or nickname is blank.")
            return
        }

        // Handle rental options
        val isForRent = binding.rentSwitch.isChecked
        var rentPrice: Double? = null
        var rentDurationHours: Int? = null
        var rentTotalPrice: Double? = null
        
        if (isForRent) {
            rentPrice = binding.rentPriceEditText.text.toString().toDoubleOrNull()
            rentDurationHours = binding.rentDurationEditText.text.toString().toIntOrNull()
            
            if (rentPrice == null || rentDurationHours == null || rentPrice <= 0 || rentDurationHours <= 0) {
                Toast.makeText(requireContext(), 
                    "Укажите корректную стоимость аренды за час и длительность аренды", 
                    Toast.LENGTH_SHORT).show()
                return
            }
            
            rentTotalPrice = rentPrice * rentDurationHours
        }

        val book = Book(
            title = title,
            author = author,
            condition = condition,
            city = city,
            phone = currentUserPhone,
            ownerNickname = currentUserNickname,
            userId = currentUser.id,
            timestamp = System.currentTimeMillis(),
            isForRent = isForRent,
            rentPrice = rentPrice,
            rentDurationHours = rentDurationHours,
            rentTotalPrice = rentTotalPrice
            // description = description,
            // imageUrl = null // Картинки нет
        )

        binding.progressBar.isVisible = true
        binding.addBookButton.isEnabled = false

        // Передаем null для imageUri
        viewModel.addBook(book, null).observe(viewLifecycleOwner) { bookIdResult ->
            binding.progressBar.isVisible = false
            // Кнопку разблокирует наблюдатель user

            bookIdResult.onSuccess { bookId ->
                Toast.makeText(requireContext(), "Книга успешно добавлена!", Toast.LENGTH_SHORT).show()
                Log.d("AddBookFragment", "Book added with ID: $bookId")
                findNavController().popBackStack()
            }
            bookIdResult.onFailure { error ->
                Log.e("AddBookFragment", "Failed to add book", error)
                // Ошибка покажется через viewModel.errorMessage
                binding.addBookButton.isEnabled = viewModel.user.value != null && !viewModel.user.value?.phone.isNullOrBlank()
            }
        }
    }

    private fun hideKeyboard(view: View) {
        // ... (код без изменений) ...
        val imm = requireActivity().getSystemService(android.content.Context.INPUT_METHOD_SERVICE) as android.view.inputmethod.InputMethodManager
        imm.hideSoftInputFromWindow(view.windowToken, 0)
    }

    override fun onDestroyView() {
        super.onDestroyView()
        _binding = null
    }
}