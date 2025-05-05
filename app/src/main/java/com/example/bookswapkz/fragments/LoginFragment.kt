package com.example.bookswapkz.fragments

import android.os.Bundle
import android.util.Log
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.Toast
import androidx.core.view.isVisible
import androidx.fragment.app.Fragment
import androidx.fragment.app.viewModels
import androidx.navigation.fragment.findNavController
import com.example.bookswapkz.R
import com.example.bookswapkz.databinding.FragmentLoginBinding
import com.example.bookswapkz.viewmodels.AuthViewModel
import com.google.firebase.auth.FirebaseUser
import dagger.hilt.android.AndroidEntryPoint

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

            if (email.isEmpty() || password.isEmpty()) {
                Toast.makeText(requireContext(), "Пожалуйста, заполните все поля", Toast.LENGTH_SHORT).show()
                return@setOnClickListener
            }

            binding.progressBar.isVisible = true
            binding.loginButton.isEnabled = false
            viewModel.loginUser(email, password)
        }

        binding.registerLink.setOnClickListener {
            findNavController().navigate(R.id.action_login_to_register)
        }
    }

    private fun observeViewModel() {
        viewModel.loginResult.observe(viewLifecycleOwner) { result ->
            binding.progressBar.isVisible = false
            binding.loginButton.isEnabled = true

            result?.let {
                if (result.isSuccess) {
                    val firebaseUser = result.getOrNull()
                    Toast.makeText(requireContext(), "Вход успешен", Toast.LENGTH_SHORT).show()
                    Log.d("LoginFragment", "Login successful for user: ${firebaseUser?.uid}")
                    findNavController().navigate(R.id.action_login_to_home)
                    viewModel.clearLoginResult()
                }
            }
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
}