package com.example.bookswapkz.fragments

import android.os.Bundle
import android.util.Log
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.ArrayAdapter
import android.widget.Toast
import androidx.core.view.isVisible
import androidx.fragment.app.Fragment
import androidx.fragment.app.viewModels
import androidx.lifecycle.Observer
import androidx.navigation.fragment.findNavController
import com.example.bookswapkz.R
import com.example.bookswapkz.databinding.FragmentAddBookBinding
import com.example.bookswapkz.models.Book
import com.example.bookswapkz.viewmodels.BookViewModel

class AddBookFragment : Fragment() {
    private var _binding: FragmentAddBookBinding? = null
    private val binding get() = _binding!!
    private val viewModel: BookViewModel by viewModels({ requireActivity() })

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

        binding.addBookButton.isEnabled = false
        binding.addBookButton.setOnClickListener {
            addBook()
        }

        viewModel.user.observe(viewLifecycleOwner, Observer { user ->
            val isReadyToAdd = user != null && !user.phone.isNullOrBlank()
            binding.addBookButton.isEnabled = isReadyToAdd
            Log.d("AddBookFragment", "User observed: ${user?.nickname}, Phone: ${user?.phone}, Button enabled: $isReadyToAdd")
            if (user != null && user.phone.isNullOrBlank() && !_binding!!.addBookButton.isEnabled) {
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
        val conditions = resources.getStringArray(R.array.book_conditions)
        val conditionAdapter = ArrayAdapter(requireContext(), android.R.layout.simple_spinner_item, conditions)
        conditionAdapter.setDropDownViewResource(android.R.layout.simple_spinner_dropdown_item)
        binding.conditionSpinner.adapter = conditionAdapter
        binding.conditionSpinner.onFocusChangeListener = View.OnFocusChangeListener { v, hasFocus -> if(hasFocus) hideKeyboard(v) }

        val cities = resources.getStringArray(R.array.cities)
        val cityAdapter = ArrayAdapter(requireContext(), android.R.layout.simple_spinner_item, cities)
        cityAdapter.setDropDownViewResource(android.R.layout.simple_spinner_dropdown_item)
        binding.citySpinner.adapter = cityAdapter
        binding.citySpinner.onFocusChangeListener = View.OnFocusChangeListener { v, hasFocus -> if(hasFocus) hideKeyboard(v) }

        val rentPeriods = resources.getStringArray(R.array.rent_periods)
        val rentPeriodAdapter = ArrayAdapter(requireContext(), android.R.layout.simple_spinner_item, rentPeriods)
        rentPeriodAdapter.setDropDownViewResource(android.R.layout.simple_spinner_dropdown_item)
        binding.rentPeriodSpinner.adapter = rentPeriodAdapter
    }

    private fun setupRentalOptions() {
        binding.rentSwitch.setOnCheckedChangeListener { _, isChecked ->
            binding.rentOptionsLayout.visibility = if (isChecked) View.VISIBLE else View.GONE
        }
    }

    private fun addBook() {
        val title = binding.titleEditText.text.toString().trim()
        val author = binding.authorEditText.text.toString().trim()
        val condition = binding.conditionSpinner.selectedItem?.toString() ?: ""
        val city = binding.citySpinner.selectedItem?.toString() ?: ""

        if (title.isEmpty() || author.isEmpty() || condition.isEmpty() || city.isEmpty()) {
            Toast.makeText(requireContext(), "Пожалуйста, заполните все поля", Toast.LENGTH_SHORT).show()
            return
        }

        val currentUser = viewModel.user.value
        val currentUserPhone = currentUser?.phone

        if (currentUser == null || currentUserPhone.isNullOrBlank()) {
            Toast.makeText(requireContext(), "Ошибка: Данные пользователя недоступны.", Toast.LENGTH_LONG).show()
            return
        }

        val isForRent = binding.rentSwitch.isChecked
        val rentPrice = if (isForRent) {
            binding.rentPriceEditText.text.toString().toDoubleOrNull()
        } else {
            null
        }
        val rentPeriod = if (isForRent) {
            binding.rentPeriodSpinner.selectedItem?.toString()
        } else {
            null
        }

        val book = Book(
            title = title,
            author = author,
            condition = condition,
            city = city,
            userId = currentUser.userId,
            phone = currentUserPhone,
            isForRent = isForRent,
            rentPrice = rentPrice,
            rentPeriod = rentPeriod
        )

        binding.progressBar.isVisible = true
        binding.addBookButton.isEnabled = false

        viewModel.addBook(book, null).observe(viewLifecycleOwner) { success ->
            if (success) {
                Toast.makeText(requireContext(), "Книга успешно добавлена", Toast.LENGTH_SHORT).show()
                findNavController().navigateUp()
            }
            binding.progressBar.isVisible = false
            binding.addBookButton.isEnabled = true
        }
    }

    private fun hideKeyboard(view: View) {
        val imm = requireActivity().getSystemService(android.content.Context.INPUT_METHOD_SERVICE) as android.view.inputmethod.InputMethodManager
        imm.hideSoftInputFromWindow(view.windowToken, 0)
    }

    override fun onDestroyView() {
        super.onDestroyView()
        _binding = null
    }
}