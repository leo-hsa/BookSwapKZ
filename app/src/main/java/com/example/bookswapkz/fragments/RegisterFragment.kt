package com.example.bookswapkz.fragments

import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.Toast
import androidx.core.view.isVisible // Убедитесь, что этот импорт есть
import androidx.fragment.app.Fragment
import androidx.fragment.app.viewModels
import androidx.navigation.fragment.findNavController
import com.example.bookswapkz.R
import com.example.bookswapkz.databinding.FragmentRegisterBinding
import com.example.bookswapkz.viewmodels.BookViewModel // Убедитесь, что импорт ViewModel правильный

class RegisterFragment : Fragment() {
    private var _binding: FragmentRegisterBinding? = null
    private val binding get() = _binding!!
    // Используем ViewModel, привязанную к Activity, если состояние пользователя нужно всем фрагментам
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
            registerUser() // Выносим логику в отдельный метод для читаемости
        }

        binding.loginLink.setOnClickListener {
            // Переходим на экран входа
            findNavController().navigate(R.id.action_register_to_login)
        }

        // Наблюдаем за сообщением об ошибке из ViewModel
        viewModel.errorMessage.observe(viewLifecycleOwner) { error ->
            if (error != null) {
                Toast.makeText(context, error, Toast.LENGTH_LONG).show()
                viewModel.clearErrorMessage() // Сбрасываем ошибку после показа
            }
        }
    }

    private fun registerUser() {
        val name = binding.nameEditText.text.toString().trim()
        val nickname = binding.nicknameEditText.text.toString().trim()
        val password = binding.passwordEditText.text.toString().trim()
        val confirmPassword = binding.confirmPasswordEditText.text.toString().trim()
        val ageStr = binding.ageEditText.text.toString().trim()
        val phone = binding.phoneEditText.text.toString().trim()
        val city = binding.cityEditText.text.toString().trim()
        val street = binding.streetEditText.text.toString().trim() // <-- Считываем улицу
        val houseNumber = binding.houseNumberEditText.text.toString().trim() // <-- Считываем номер дома

        // Валидация ввода
        if (name.isEmpty() || nickname.isEmpty() || password.isEmpty() || confirmPassword.isEmpty() ||
            ageStr.isEmpty() || phone.isEmpty() || city.isEmpty() || street.isEmpty() || houseNumber.isEmpty()) {
            Toast.makeText(context, "Пожалуйста, заполните все поля", Toast.LENGTH_SHORT).show()
            return
        }

        if (password.length < 6) { // Пример: минимальная длина пароля
            Toast.makeText(context, "Пароль должен быть не менее 6 символов", Toast.LENGTH_SHORT).show()
            return
        }

        if (password != confirmPassword) {
            Toast.makeText(context, "Пароли не совпадают", Toast.LENGTH_SHORT).show()
            return
        }

        val age = ageStr.toIntOrNull()
        if (age == null || age <= 0) { // Проверка корректности возраста
            Toast.makeText(context, "Неверный формат возраста", Toast.LENGTH_SHORT).show()
            return
        }

        // Генерируем email (можно заменить на реальное поле ввода email)
        val email = "$nickname@bookswapkz.com"

        // Показываем прогресс и блокируем кнопку
        binding.progressBar.isVisible = true
        binding.registerButton.isEnabled = false

        // Вызываем метод ViewModel для регистрации
        viewModel.registerUser(nickname, name, city, street, houseNumber, age, phone, email, password)
            .observe(viewLifecycleOwner) { success ->
                // Скрываем прогресс и разблокируем кнопку в любом случае
                binding.progressBar.isVisible = false
                binding.registerButton.isEnabled = true

                if (success) {
                    Toast.makeText(context, "Регистрация успешна!", Toast.LENGTH_SHORT).show()
                    // Переход на главный экран после успешной регистрации
                    // Предполагаем, что есть глобальное действие для перехода на Home
                    findNavController().navigate(R.id.action_global_homeFragment)
                }
                // Сообщение об ошибке будет показано через observe errorMessage
            }
    }


    override fun onDestroyView() {
        super.onDestroyView()
        _binding = null // Обязательная очистка для избежания утечек
    }
}