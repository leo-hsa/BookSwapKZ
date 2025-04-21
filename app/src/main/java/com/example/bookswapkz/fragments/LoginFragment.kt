package com.example.bookswapkz.fragments

import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.Toast
import androidx.fragment.app.Fragment
import androidx.fragment.app.viewModels
import androidx.navigation.fragment.findNavController
import com.example.bookswapkz.R
import com.example.bookswapkz.databinding.FragmentLoginBinding
import com.example.bookswapkz.viewmodels.BookViewModel

class LoginFragment : Fragment() {
    private var _binding: FragmentLoginBinding? = null
    private val binding get() = _binding!!
    private val viewModel: BookViewModel by viewModels()

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
            val nickname = binding.nicknameEditText.text.toString().trim()
            val password = binding.passwordEditText.text.toString().trim()
            val email = "$nickname@bookswapkz.com"

            if (nickname.isEmpty() || password.isEmpty()) {
                Toast.makeText(context, "Заполните все поля", Toast.LENGTH_SHORT).show()
                return@setOnClickListener
            }

            binding.progressBar.visibility = View.VISIBLE
            binding.loginButton.isEnabled = false

            viewModel.loginUser(email, password).observe(viewLifecycleOwner) { success ->
                binding.progressBar.visibility = View.GONE
                binding.loginButton.isEnabled = true
                if (success) {
                    Toast.makeText(context, "Вход успешен", Toast.LENGTH_SHORT).show()
                    findNavController().navigate(R.id.action_login_to_home)
                } else {
                    Toast.makeText(context, "Ошибка входа", Toast.LENGTH_SHORT).show()
                }
            }
        }

        binding.registerLink.setOnClickListener {
            findNavController().navigate(R.id.action_login_to_register)
        }
    }

    override fun onDestroyView() {
        super.onDestroyView()
        _binding = null
    }
}