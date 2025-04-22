package com.example.bookswapkz.fragments

import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import androidx.fragment.app.Fragment
import androidx.fragment.app.viewModels
import androidx.navigation.fragment.findNavController
import androidx.recyclerview.widget.LinearLayoutManager
import com.example.bookswapkz.R
import com.example.bookswapkz.adapters.ExchangeHistoryAdapter
import com.example.bookswapkz.databinding.FragmentProfileBinding
import com.example.bookswapkz.viewmodels.ProfileViewModel
import com.google.firebase.auth.FirebaseAuth
import dagger.hilt.android.AndroidEntryPoint

@AndroidEntryPoint
class ProfileFragment : Fragment() {
    private var _binding: FragmentProfileBinding? = null
    private val binding get() = _binding!!
    private val viewModel: ProfileViewModel by viewModels()
    private lateinit var exchangeHistoryAdapter: ExchangeHistoryAdapter
    private val auth = FirebaseAuth.getInstance()

    override fun onCreateView(
        inflater: LayoutInflater,
        container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View {
        _binding = FragmentProfileBinding.inflate(inflater, container, false)
        return binding.root
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)

        // Check authentication state
        if (auth.currentUser == null) {
            findNavController().navigate(R.id.action_profile_to_login)
            return
        }

        setupRecyclerView()
        setupClickListeners()
        observeUserData()
        loadExchangeHistory()
    }

    private fun setupRecyclerView() {
        exchangeHistoryAdapter = ExchangeHistoryAdapter()
        binding.exchangeHistoryRecyclerView.apply {
            layoutManager = LinearLayoutManager(context)
            adapter = exchangeHistoryAdapter
        }
    }

    private fun setupClickListeners() {
        binding.myBooksButton.setOnClickListener {
            findNavController().navigate(R.id.action_profileFragment_to_myBooksFragment)
        }

        binding.editProfileButton.setOnClickListener {
            findNavController().navigate(R.id.action_profileFragment_to_editProfileFragment)
        }

        binding.logoutButton.setOnClickListener {
            auth.signOut()
            findNavController().navigate(R.id.action_profile_to_login)
        }
    }

    private fun observeUserData() {
        auth.currentUser?.let { user ->
            viewModel.getUserData(user.uid)
            viewModel.userData.observe(viewLifecycleOwner) { userData ->
                binding.apply {
                    nicknameTextView.text = userData?.nickname ?: "No nickname"
                    emailTextView.text = user.email
                    // Add other user data fields as needed
                }
            }
        }
    }

    private fun loadExchangeHistory() {
        auth.currentUser?.let { user ->
            viewModel.loadExchangeHistory(user.uid)
            viewModel.exchangeHistory.observe(viewLifecycleOwner) { exchanges ->
                exchangeHistoryAdapter.submitList(exchanges)
                binding.exchangeHistoryTitle.visibility = if (exchanges.isEmpty()) View.GONE else View.VISIBLE
            }
        }
    }

    override fun onDestroyView() {
        super.onDestroyView()
        _binding = null
    }
}