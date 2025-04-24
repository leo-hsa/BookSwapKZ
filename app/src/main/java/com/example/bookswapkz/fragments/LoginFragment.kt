package com.example.bookswapkz.fragments

import android.os.Bundle
import android.util.Log // Добавлен Log
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
import com.example.bookswapkz.databinding.FragmentLoginBinding
import com.example.bookswapkz.viewmodels.BookViewModel
import com.google.firebase.auth.FirebaseUser // <-- Импорт FirebaseUser
import dagger.hilt.android.AndroidEntryPoint

@AndroidEntryPoint
class LoginFragment : Fragment() {
    private var _binding: FragmentLoginBinding? = null
    private val binding get() = _binding!!
    private val viewModel: BookViewModel by viewModels({ requireActivity() })

    override fun onCreateView(
        inflater: LayoutInflater, container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View {
        _binding = FragmentLoginBinding.inflate(inflater, container, false)
        return binding.root
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)

        binding.loginButton.setOnClickListener {
            val email = binding.emailEditText.text.toString().trim()
            val password = binding.passwordEditText.text.toString().trim()

            if (email.isEmpty() || password.isEmpty()) {
                Toast.makeText(requireContext(), "Заполните Email и пароль", Toast.LENGTH_SHORT).show()
                return@setOnClickListener
            }

            binding.progressBar.isVisible = true
            binding.loginButton.isEnabled = false

            // --- ИСПРАВЛЕНО: Обработка Result<FirebaseUser> ---
            viewModel.loginUser(email, password).observe(viewLifecycleOwner) { result -> // Тип - Result<FirebaseUser>
                binding.progressBar.isVisible = false
                binding.loginButton.isEnabled = true

                result.onSuccess { firebaseUser -> // Если успех
                    Toast.makeText(requireContext(), "Вход успешен", Toast.LENGTH_SHORT).show()
                    Log.d("LoginFragment", "Login successful for user: ${firebaseUser.uid}")
                    // Используем ID из графа навигации
                    findNavController().navigate(R.id.action_login_to_home)
                }
                result.onFailure { error -> // Если ошибка
                    Log.e("LoginFragment", "Login failed", error)
                    // Ошибка будет показана через errorMessage Observer ниже
                }
            }
            // --- КОНЕЦ ИСПРАВЛЕНИЯ ---
        }

        binding.registerLink.setOnClickListener {
            findNavController().navigate(R.id.action_login_to_register)
        }

        viewModel.errorMessage.observe(viewLifecycleOwner) { error ->
            if (error != null) {
                binding.progressBar.isVisible = false
                binding.loginButton.isEnabled = true
                Toast.makeText(requireContext(), error, Toast.LENGTH_LONG).show()
                viewModel.clearErrorMessage()
            }
        }
    }

    override fun onDestroyView() {
        super.onDestroyView()
        _binding = null
    }
}