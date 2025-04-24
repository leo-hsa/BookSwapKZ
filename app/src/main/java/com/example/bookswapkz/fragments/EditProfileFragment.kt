package com.example.bookswapkz.fragments

import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import androidx.fragment.app.Fragment
import androidx.fragment.app.viewModels
import androidx.navigation.fragment.findNavController
import com.example.bookswapkz.R
import com.example.bookswapkz.databinding.FragmentEditProfileBinding
import com.example.bookswapkz.viewmodels.ProfileViewModel
import com.google.android.material.snackbar.Snackbar
import com.google.firebase.auth.FirebaseAuth
import dagger.hilt.android.AndroidEntryPoint

@AndroidEntryPoint
class EditProfileFragment : Fragment() {
    private var _binding: FragmentEditProfileBinding? = null
    private val binding get() = _binding!!
    private val viewModel: ProfileViewModel by viewModels()
    private val auth = FirebaseAuth.getInstance()

    override fun onCreateView(
        inflater: LayoutInflater,
        container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View {
        _binding = FragmentEditProfileBinding.inflate(inflater, container, false)
        return binding.root
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)

        setupClickListeners()
        loadUserData()
        observeViewModel()
    }

    private fun setupClickListeners() {
        binding.saveButton.setOnClickListener {
            if (validateInputs()) {
                saveProfile()
            }
        }

        binding.cancelButton.setOnClickListener {
            findNavController().navigateUp()
        }
    }

    private fun validateInputs(): Boolean {
        var isValid = true

        // Validate nickname
        val nickname = binding.nicknameEditText.text.toString().trim()
        if (nickname.isEmpty()) {
            binding.nicknameInputLayout.error = getString(R.string.field_required)
            isValid = false
        } else {
            binding.nicknameInputLayout.error = null
        }

        // Validate phone
        val phone = binding.phoneEditText.text.toString().trim()
        if (phone.isEmpty()) {
            binding.phoneInputLayout.error = getString(R.string.field_required)
            isValid = false
        } else {
            binding.phoneInputLayout.error = null
        }

        // Validate city
        val city = binding.cityEditText.text.toString().trim()
        if (city.isEmpty()) {
            binding.cityInputLayout.error = getString(R.string.field_required)
            isValid = false
        } else {
            binding.cityInputLayout.error = null
        }

        // Validate street
        val street = binding.streetEditText.text.toString().trim()
        if (street.isEmpty()) {
            binding.streetInputLayout.error = getString(R.string.field_required)
            isValid = false
        } else {
            binding.streetInputLayout.error = null
        }

        // Validate house number
        val houseNumber = binding.houseNumberEditText.text.toString().trim()
        if (houseNumber.isEmpty()) {
            binding.houseNumberInputLayout.error = getString(R.string.field_required)
            isValid = false
        } else {
            binding.houseNumberInputLayout.error = null
        }

        return isValid
    }

    private fun loadUserData() {
        auth.currentUser?.let { user ->
            viewModel.getUserData(user.uid)
        }
    }

    private fun observeViewModel() {
        viewModel.userData.observe(viewLifecycleOwner) { userData ->
            userData?.let { user ->
                binding.apply {
                    nicknameEditText.setText(user.nickname)
                    phoneEditText.setText(user.phone)
                    cityEditText.setText(user.city)
                    streetEditText.setText(user.street)
                    houseNumberEditText.setText(user.houseNumber)
                }
            }
        }

        viewModel.nicknameExists.observe(viewLifecycleOwner) { exists ->
            if (exists) {
                binding.nicknameInputLayout.error = getString(R.string.nickname_exists)
            }
        }

        viewModel.saveSuccess.observe(viewLifecycleOwner) { success ->
            if (success) {
                findNavController().navigateUp()
            }
        }

        viewModel.errorMessage.observe(viewLifecycleOwner) { error ->
            error?.let {
                Snackbar.make(binding.root, it, Snackbar.LENGTH_LONG).show()
            }
        }
    }

    private fun saveProfile() {
        val nickname = binding.nicknameEditText.text.toString().trim()
        val phone = binding.phoneEditText.text.toString().trim()
        val city = binding.cityEditText.text.toString().trim()
        val street = binding.streetEditText.text.toString().trim()
        val houseNumber = binding.houseNumberEditText.text.toString().trim()

        auth.currentUser?.let { user ->
            viewModel.updateUserProfile(
                userId = user.uid,
                nickname = nickname,
                phone = phone,
                city = city,
                street = street,
                houseNumber = houseNumber
            )
        }
    }

    override fun onDestroyView() {
        super.onDestroyView()
        _binding = null
    }
} 