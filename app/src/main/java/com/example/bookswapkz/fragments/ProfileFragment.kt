package com.example.bookswapkz.fragments

import android.os.Bundle
import android.util.Log
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.Toast // <<<--- ДОБАВЬ ИМПОРТ
import androidx.core.view.isVisible
import androidx.fragment.app.Fragment
import androidx.fragment.app.viewModels
import androidx.navigation.fragment.findNavController
import androidx.recyclerview.widget.LinearLayoutManager
// import com.bumptech.glide.Glide // Не используется, так как нет аватара
import com.example.bookswapkz.R
// import com.example.bookswapkz.adapters.ExchangeHistoryAdapter // Закомментируй, если не используется
import com.example.bookswapkz.databinding.FragmentProfileBinding
import com.example.bookswapkz.models.User
import com.example.bookswapkz.viewmodels.ProfileViewModel // Твоя ViewModel
import com.google.firebase.auth.FirebaseAuth
import dagger.hilt.android.AndroidEntryPoint

@AndroidEntryPoint
class ProfileFragment : Fragment() {

    private var _binding: FragmentProfileBinding? = null
    private val binding get() = _binding!!

    private val viewModel: ProfileViewModel by viewModels()
    // private lateinit var exchangeHistoryAdapter: ExchangeHistoryAdapter // Закомментируй, если не используется
    private val auth = FirebaseAuth.getInstance()
    private val TAG = "ProfileFragment"

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

        val currentUser = auth.currentUser
        if (currentUser == null) {
            Log.w(TAG, "User not authenticated, navigating to login.")
            navigateToLogin()
            return
        }

        Log.d(TAG, "User authenticated: ${currentUser.uid}. Setting up profile.")
        setupToolbar()
        // setupRecyclerView() // Закомментируй, если история обменов не используется
        setupClickListeners()
        observeViewModel()

        // Загружаем данные пользователя
        viewModel.getUserData(currentUser.uid)
        // loadExchangeHistory() // Закомментируй, если история обменов не используется
    }

    private fun setupToolbar() {
        binding.toolbarProfile.setNavigationOnClickListener {
            findNavController().popBackStack()
        }
        // Можно добавить меню, если нужно
    }

    /* // Закомментируй, если история обменов не используется
    private fun setupRecyclerView() {
        exchangeHistoryAdapter = ExchangeHistoryAdapter()
        binding.exchangeHistoryRecyclerView.apply {
            layoutManager = LinearLayoutManager(context)
            adapter = exchangeHistoryAdapter
            isNestedScrollingEnabled = false
        }
    }
    */

    private fun setupClickListeners() {
        binding.myBooksButton.setOnClickListener {
            try {
                findNavController().navigate(R.id.action_profileFragment_to_myBooksFragment)
            } catch (e: Exception) {
                Log.e(TAG, "Navigation to myBooks failed", e)
                Toast.makeText(context, "Не удалось перейти к книгам", Toast.LENGTH_SHORT).show()
            }
        }

        binding.editProfileButton.setOnClickListener {
            try {
                findNavController().navigate(R.id.action_profileFragment_to_editProfileFragment)
            } catch (e: Exception) {
                Log.e(TAG, "Navigation to editProfile failed", e)
                Toast.makeText(context, "Не удалось перейти к редактированию", Toast.LENGTH_SHORT).show()
            }
        }

        binding.logoutButton.setOnClickListener {
            // Упрощенный выход без диалога
            Log.d(TAG, "Logout button clicked.")
            auth.signOut()
            navigateToLogin() // Переходим на логин после выхода
        }
    }

    private fun observeViewModel() {
        viewModel.userData.observe(viewLifecycleOwner) { user ->
            user?.let { bindUserData(it) } ?: run {
                Log.w(TAG, "Received null user data from ViewModel.")
                // Показываем заглушки, если данные пользователя null
                binding.nameTextView.text = "Пользователь"
                binding.subtitleTextView.text = auth.currentUser?.email ?: "Нет данных"
                binding.cityValueTextView.text = "Неизвестно"
                binding.ageValueTextView.text = "Неизвестно"
                binding.profileImageView.setImageResource(R.drawable.placeholder_avatar)
            }
        }

        /* // Закомментируй, если история обменов не используется
        viewModel.exchangeHistory.observe(viewLifecycleOwner) { exchanges ->
            Log.d(TAG, "Received ${exchanges?.size ?: 0} exchanges from ViewModel.")
            exchangeHistoryAdapter.submitList(exchanges ?: emptyList())
            val hasExchanges = !exchanges.isNullOrEmpty()
            binding.exchangeHistoryTitle.isVisible = hasExchanges
            binding.exchangeHistoryRecyclerView.isVisible = hasExchanges
        }
        */

        // Убрана обработка errorMessage и isLoading для упрощения
    }

    /* // Закомментируй, если история обменов не используется
    private fun loadExchangeHistory() {
        val userId = auth.currentUser?.uid
        if (userId == null) {
            Log.w(TAG, "Cannot load exchange history, user not logged in.")
            binding.exchangeHistoryTitle.isVisible = false
            binding.exchangeHistoryRecyclerView.isVisible = false
            return
        }
        Log.d(TAG, "Loading exchange history for user: $userId")
        viewModel.loadExchangeHistory(userId)
    }
     */

    private fun bindUserData(user: User) {
        Log.d(TAG, "Binding user data: $user")
        binding.nameTextView.text = user.name.takeIf { it.isNotBlank() } ?: user.nickname
        binding.subtitleTextView.text = user.email
        binding.cityValueTextView.text = user.city.takeIf { it.isNotBlank() } ?: "Не указан"
        binding.ageValueTextView.text = user.age.takeIf { it > 0 }?.toString() ?: "Не указан"
        binding.profileImageView.setImageResource(R.drawable.placeholder_avatar) // Плейсхолдер
    }

    private fun navigateToLogin() {
        try {
            // Используй ID твоего action для перехода на экран логина
            findNavController().navigate(R.id.action_profile_to_login)
        } catch (e: Exception) {
            Log.e(TAG,"Failed to navigate to login screen", e)
            // Можно попытаться вернуться назад или показать ошибку
            if (findNavController().previousBackStackEntry != null) {
                findNavController().popBackStack()
            } else {
                Toast.makeText(context, "Требуется вход", Toast.LENGTH_LONG).show()
            }
        }
    }

    override fun onDestroyView() {
        super.onDestroyView()
        Log.d(TAG, "onDestroyView called.")
        _binding = null
    }
}