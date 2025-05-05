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
import androidx.lifecycle.Observer
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
        viewModel.errorMessage.observe(viewLifecycleOwner) { errorMessage ->
            if (errorMessage != null) {
                Toast.makeText(requireContext(), errorMessage, Toast.LENGTH_LONG).show()
                viewModel.clearErrorMessage()
            }
        }
        
        viewModel.registrationResult.observe(viewLifecycleOwner) { result ->
            result?.let {
                binding.progressBar.isVisible = false
                binding.registerButton.isEnabled = true
                
                if (result.isSuccess) {
                    findNavController().navigate(R.id.action_register_to_home)
                } else {
                    val exception = result.exceptionOrNull()
                    Toast.makeText(context, exception?.message ?: "Registration failed", Toast.LENGTH_SHORT).show()
                }
            }
        }
    }

    private fun registerUser() {
        // Считываем значения всех полей
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

        // Преобразование возраста в число
        val ageInt = age.toIntOrNull()
        if (ageInt == null) {
            Toast.makeText(requireContext(), "Пожалуйста, введите корректный возраст", Toast.LENGTH_SHORT).show()
            return
        }

        // Отображение прогресса и блокировка кнопки
        binding.progressBar.isVisible = true
        binding.registerButton.isEnabled = false
        Log.d("RegisterFragment", "Attempting to register with email: [$email]")

        // Вызов метода регистрации из ViewModel
        viewModel.registerUser(email, password, name, nickname, city, street, houseNumber, ageInt, phone)
            .observe(viewLifecycleOwner) { result ->
                binding.progressBar.isVisible = false
                binding.registerButton.isEnabled = true

                if (result.isSuccess) {
                    val firebaseUser = result.getOrNull()
                    Toast.makeText(requireContext(), "Регистрация успешна!", Toast.LENGTH_SHORT).show()
                    Log.d("RegisterFragment", "Registration successful for user: ${firebaseUser?.uid}")
                    findNavController().navigate(R.id.action_register_to_home)
                } else {
                    val error = result.exceptionOrNull()
                    Log.e("RegisterFragment", "Registration failed", error)
                    Toast.makeText(requireContext(), error?.message ?: "Ошибка регистрации", Toast.LENGTH_LONG).show()
                }
            }
    }
}