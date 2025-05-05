package com.example.bookswapkz.fragments

import android.os.Bundle
import android.util.Log // Добавлен импорт Log
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
import com.example.bookswapkz.viewmodels.AuthViewModel
import com.google.firebase.auth.FirebaseUser // Добавлен импорт FirebaseUser
import dagger.hilt.android.AndroidEntryPoint
import kotlin.Result // Добавлен импорт Result

@AndroidEntryPoint
class LoginFragment : Fragment() {
    private var _binding: FragmentLoginBinding? = null
    private val binding get() = _binding!!
    private val viewModel: AuthViewModel by viewModels()

    override fun onCreateView(
        inflater: LayoutInflater,
        container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View {
        _binding = FragmentLoginBinding.inflate(inflater, container, false)
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
        binding.loginButton.setOnClickListener {
            val email = binding.emailEditText.text.toString().trim()
            val password = binding.passwordEditText.text.toString().trim()

            if (email.isEmpty() || password.isEmpty()) { /* ... */ return@setOnClickListener }

            binding.progressBar.isVisible = true
            binding.loginButton.isEnabled = false

            // --- ИСПРАВЛЕНО: Обработка Result<FirebaseUser> ---
            viewModel.loginUser(email, password).observe(viewLifecycleOwner) { result: Result<FirebaseUser>? -> // Добавляем тип Result
                binding.progressBar.isVisible = false
                binding.loginButton.isEnabled = true

                result?.onSuccess { firebaseUser -> // Используем ?.onSuccess
                    Toast.makeText(requireContext(), "Вход успешен", Toast.LENGTH_SHORT).show()
                    Log.d("LoginFragment", "Login successful for user: ${firebaseUser.uid}") // Логируем uid
                    findNavController().navigate(R.id.action_login_to_home)
                }
                result?.onFailure { error -> // Используем ?.onFailure
                    Log.e("LoginFragment", "Login failed", error)
                    // Ошибка покажется через errorMessage observer
                }
            }
            // --- КОНЕЦ ---
        }

        binding.registerLink.setOnClickListener {
            findNavController().navigate(R.id.action_login_to_register)
        }
    }

    private fun observeViewModel() {
        viewModel.errorMessage.observe(viewLifecycleOwner) { error -> // Обработка ошибок
            if (error != null) {
                binding.progressBar.isVisible = false
                binding.loginButton.isEnabled = true
                Toast.makeText(requireContext(), error, Toast.LENGTH_LONG).show() // Показываем ошибку из ViewModel
                viewModel.clearErrorMessage() // Сбрасываем
            }
        }
    }
}