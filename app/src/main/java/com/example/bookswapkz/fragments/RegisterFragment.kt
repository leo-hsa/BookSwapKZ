package com.example.bookswapkz.fragments

import android.os.Bundle
import android.util.Log
import android.util.Patterns
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.Toast
import androidx.core.view.isVisible
import androidx.fragment.app.Fragment
import androidx.fragment.app.viewModels
import androidx.navigation.fragment.findNavController
import com.example.bookswapkz.R
import com.example.bookswapkz.databinding.FragmentRegisterBinding
import com.example.bookswapkz.viewmodels.AuthViewModel
import com.google.firebase.auth.FirebaseUser
import dagger.hilt.android.AndroidEntryPoint

@AndroidEntryPoint
class RegisterFragment : Fragment() {
    private var _binding: FragmentRegisterBinding? = null
    private val binding get() = _binding!!
    private val viewModel: AuthViewModel by viewModels()

    override fun onCreateView(
        inflater: LayoutInflater,
        container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View {
        _binding = FragmentRegisterBinding.inflate(inflater, container, false)
        return binding.root
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)
        setupViews()
        observeViewModel()
    }

    override fun onDestroyView() {
        super.onDestroyView()
        _binding = null
    }

    private fun setupViews() {
        binding.registerButton.setOnClickListener {
            registerUser()
        }

        binding.loginLink.setOnClickListener {
            findNavController().navigate(R.id.action_register_to_login)
        }
    }

    private fun observeViewModel() {
        viewModel.registrationResult.observe(viewLifecycleOwner) { result ->
            binding.progressBar.isVisible = false
            binding.registerButton.isEnabled = true

            result?.let {
                if (result.isSuccess) {
                    val firebaseUser = result.getOrNull()
                    Toast.makeText(requireContext(), "Регистрация успешна!", Toast.LENGTH_SHORT).show()
                    Log.d("RegisterFragment", "Registration successful for user: ${firebaseUser?.uid}")
                    findNavController().navigate(R.id.action_register_to_home)
                    viewModel.clearRegistrationResult()
                }
            }
        }

        viewModel.errorMessage.observe(viewLifecycleOwner) { errorMessage ->
            if (errorMessage != null) {
                binding.progressBar.isVisible = false
                binding.registerButton.isEnabled = true
                Toast.makeText(requireContext(), errorMessage, Toast.LENGTH_LONG).show()
                viewModel.clearErrorMessage()
            }
        }
    }

    private fun registerUser() {
        val name = binding.nameEditText.text.toString().trim()
        val nickname = binding.nicknameEditText.text.toString().trim()
        val email = binding.emailEditText.text.toString().trim()
        val password = binding.passwordEditText.text.toString().trim()
        val confirmPassword = binding.confirmPasswordEditText.text.toString().trim()
        val age = binding.ageEditText.text.toString().trim()
        val phone = binding.phoneEditText.text.toString().trim()
        val city = binding.cityEditText.text.toString().trim()
        val street = binding.streetEditText.text.toString().trim()
        val houseNumber = binding.houseNumberEditText.text.toString().trim()

        // Проверка заполнения всех полей
        if (name.isEmpty() || nickname.isEmpty() || email.isEmpty() ||
            password.isEmpty() || confirmPassword.isEmpty() || age.isEmpty() ||
            phone.isEmpty() || city.isEmpty() || street.isEmpty() || houseNumber.isEmpty()) {
            Toast.makeText(requireContext(), "Пожалуйста, заполните все поля", Toast.LENGTH_SHORT).show()
            return
        }

        // Проверка формата email
        if (!Patterns.EMAIL_ADDRESS.matcher(email).matches()) {
            Toast.makeText(requireContext(), "Пожалуйста, введите корректный email", Toast.LENGTH_SHORT).show()
            return
        }

        // Проверка совпадения паролей
        if (password != confirmPassword) {
            Toast.makeText(requireContext(), "Пароли не совпадают", Toast.LENGTH_SHORT).show()
            return
        }

        // Проверка длины пароля
        if (password.length < 6) {
            Toast.makeText(requireContext(), "Пароль должен содержать минимум 6 символов", Toast.LENGTH_SHORT).show()
            return
        }

        // Проверка возраста
        val ageInt = age.toIntOrNull()
        if (ageInt == null || ageInt < 13 || ageInt > 120) {
            Toast.makeText(requireContext(), "Возраст должен быть от 13 до 120 лет", Toast.LENGTH_SHORT).show()
            return
        }

        // Проверка формата телефона
        if (!phone.matches(Regex("^\\+?[1-9]\\d{1,14}\$"))) {
            Toast.makeText(requireContext(), "Введите корректный номер телефона", Toast.LENGTH_SHORT).show()
            return
        }

        binding.progressBar.isVisible = true
        binding.registerButton.isEnabled = false
        Log.d("RegisterFragment", "Attempting to register with email: [$email]")

        viewModel.registerUser(email, password, name, nickname, city, street, houseNumber, ageInt, phone)
    }
}