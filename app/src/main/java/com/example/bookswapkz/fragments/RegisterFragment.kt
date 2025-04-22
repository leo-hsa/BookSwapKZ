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
import com.example.bookswapkz.viewmodels.BookViewModel

class RegisterFragment : Fragment() {
    private var _binding: FragmentRegisterBinding? = null
    private val binding get() = _binding!!
    private val viewModel: BookViewModel by viewModels({ requireActivity() })

    override fun onCreateView(
        inflater: LayoutInflater, container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View {
        _binding = FragmentRegisterBinding.inflate(inflater, container, false)
        return binding.root
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)

        binding.registerButton.setOnClickListener {
            registerUser()
        }

        binding.loginLink.setOnClickListener {
            findNavController().navigate(R.id.action_register_to_login)
        }

        viewModel.errorMessage.observe(viewLifecycleOwner) { error ->
            if (error != null) {
                binding.progressBar.isVisible = false
                binding.registerButton.isEnabled = true
                Toast.makeText(requireContext(), error, Toast.LENGTH_LONG).show()
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
        val ageStr = binding.ageEditText.text.toString().trim()
        val phone = binding.phoneEditText.text.toString().trim()
        val city = binding.cityEditText.text.toString().trim()
        val street = binding.streetEditText.text.toString().trim()
        val houseNumber = binding.houseNumberEditText.text.toString().trim()

        // Валидация ввода
        if (name.isEmpty() || nickname.isEmpty() || email.isEmpty() || password.isEmpty() ||
            confirmPassword.isEmpty() || ageStr.isEmpty() || phone.isEmpty() || city.isEmpty() ||
            street.isEmpty() || houseNumber.isEmpty()) {
            Toast.makeText(requireContext(), "Пожалуйста, заполните все поля", Toast.LENGTH_SHORT).show()
            return
        }

        if (!Patterns.EMAIL_ADDRESS.matcher(email).matches()) {
            Toast.makeText(requireContext(), "Введите корректный Email", Toast.LENGTH_SHORT).show()
            return
        }

        if (password.length < 6) {
            Toast.makeText(requireContext(), "Пароль должен быть не менее 6 символов", Toast.LENGTH_SHORT).show()
            return
        }

        if (password != confirmPassword) {
            Toast.makeText(requireContext(), "Пароли не совпадают", Toast.LENGTH_SHORT).show()
            return
        }

        val age = ageStr.toIntOrNull()
        if (age == null || age <= 0) {
            Toast.makeText(requireContext(), "Неверный формат возраста", Toast.LENGTH_SHORT).show()
            return
        }

        binding.progressBar.isVisible = true
        binding.registerButton.isEnabled = false

        Log.d("RegisterFragment", "Attempting to register with email: [$email]")

        viewModel.registerUser(nickname, name, city, street, houseNumber, age, phone, email, password)
            .observe(viewLifecycleOwner) { success ->
                binding.progressBar.isVisible = false
                binding.registerButton.isEnabled = true

                if (success) {
                    Toast.makeText(requireContext(), "Регистрация успешна!", Toast.LENGTH_SHORT).show()
                    findNavController().navigate(R.id.action_register_to_home)
                }

            }
    }

    override fun onDestroyView() {
        super.onDestroyView()
        _binding = null
    }
}