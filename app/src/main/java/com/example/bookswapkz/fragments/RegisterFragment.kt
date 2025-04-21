package com.example.bookswapkz.fragments

import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.ArrayAdapter
import android.widget.Toast
import androidx.fragment.app.Fragment
import androidx.fragment.app.viewModels
import androidx.navigation.fragment.findNavController
import com.example.bookswapkz.R
import com.example.bookswapkz.databinding.FragmentRegisterBinding
import com.example.bookswapkz.viewmodels.BookViewModel

class RegisterFragment : Fragment() {
    private var _binding: FragmentRegisterBinding? = null
    private val binding get() = _binding!!
    private val viewModel: BookViewModel by viewModels()

    override fun onCreateView(
        inflater: LayoutInflater, container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View {
        _binding = FragmentRegisterBinding.inflate(inflater, container, false)
        return binding.root
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)

        val cities = arrayOf("Алматы", "Астана", "Шымкент")
        binding.citySpinner.adapter = ArrayAdapter(
            requireContext(),
            android.R.layout.simple_spinner_dropdown_item,
            cities
        )

        binding.registerButton.setOnClickListener {
            val name = binding.nameEditText.text.toString().trim()
            val nickname = binding.nicknameEditText.text.toString().trim()
            val password = binding.passwordEditText.text.toString().trim()
            val confirmPassword = binding.confirmPasswordEditText.text.toString().trim()
            val ageText = binding.ageEditText.text.toString().trim()
            val phone = binding.phoneEditText.text.toString().trim()
            val city = binding.citySpinner.selectedItem.toString()
            val email = "$nickname@bookswapkz.com"

            if (name.isEmpty() || nickname.isEmpty() || password.isEmpty() || confirmPassword.isEmpty() || ageText.isEmpty() || phone.isEmpty()) {
                Toast.makeText(context, "Заполните все поля", Toast.LENGTH_SHORT).show()
                return@setOnClickListener
            }

            if (password != confirmPassword) {
                Toast.makeText(context, "Пароли не совпадают", Toast.LENGTH_SHORT).show()
                return@setOnClickListener
            }

            if (password.length < 6) {
                Toast.makeText(context, "Пароль должен быть минимум 6 символов", Toast.LENGTH_SHORT).show()
                return@setOnClickListener
            }

            val age = ageText.toIntOrNull()
            if (age == null || age < 5 || age > 90) {
                Toast.makeText(context, "Возраст должен быть от 5 до 90", Toast.LENGTH_SHORT).show()
                return@setOnClickListener
            }

            if (!phone.matches(Regex("^\\+?[0-9]{10,12}\$"))) {
                Toast.makeText(context, "Некорректный номер телефона", Toast.LENGTH_SHORT).show()
                return@setOnClickListener
            }

            binding.progressBar.visibility = View.VISIBLE
            binding.registerButton.isEnabled = false

            viewModel.registerUser(nickname, name, city, age, phone, email, password)
                .observe(viewLifecycleOwner) { success ->
                    binding.progressBar.visibility = View.GONE
                    binding.registerButton.isEnabled = true
                    if (success) {
                        Toast.makeText(context, "Регистрация успешна", Toast.LENGTH_SHORT).show()
                        findNavController().navigate(R.id.action_register_to_home)
                    } else {
                        Toast.makeText(context, "Ошибка регистрации", Toast.LENGTH_SHORT).show()
                    }
                }
        }

        binding.loginLink.setOnClickListener {
            findNavController().navigate(R.id.action_register_to_login)
        }
    }

    override fun onDestroyView() {
        super.onDestroyView()
        _binding = null
    }
}