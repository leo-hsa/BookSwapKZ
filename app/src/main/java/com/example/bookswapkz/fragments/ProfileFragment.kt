package com.example.bookswapkz.fragments

import android.app.AlertDialog
import android.os.Bundle
import android.util.Log
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.Toast
import androidx.core.view.isVisible
import androidx.fragment.app.Fragment
import androidx.fragment.app.activityViewModels
import androidx.navigation.fragment.findNavController
import androidx.recyclerview.widget.LinearLayoutManager
import com.example.bookswapkz.R
import com.example.bookswapkz.adapters.ExchangeRequestAdapter // Используем адаптер запросов
import com.example.bookswapkz.databinding.FragmentProfileBinding
import com.example.bookswapkz.models.User
import com.example.bookswapkz.utils.EventObserver
import com.example.bookswapkz.viewmodels.BookViewModel
import com.google.firebase.auth.FirebaseAuth
import dagger.hilt.android.AndroidEntryPoint

@AndroidEntryPoint
class ProfileFragment : Fragment() {

    private var _binding: FragmentProfileBinding? = null
    private val binding get() = _binding!!

    private val bookViewModel: BookViewModel by activityViewModels()
    private lateinit var exchangeRequestAdapter: ExchangeRequestAdapter // Адаптер для запросов
    private val auth = FirebaseAuth.getInstance()
    private val TAG = "ProfileFragment"

    override fun onCreateView(
        inflater: LayoutInflater, container: ViewGroup?, savedInstanceState: Bundle?
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
        setupRecyclerView() // Настраиваем RecyclerView для запросов
        setupClickListeners()
        observeViewModel()

        // НЕ загружаем запросы автоматически, ждем нажатия кнопки
    }

    private fun setupToolbar() {
        binding.toolbarProfile.setNavigationOnClickListener { findNavController().popBackStack() }
    }

    private fun setupRecyclerView() {
        exchangeRequestAdapter = ExchangeRequestAdapter(
            onAcceptClick = { exchange -> bookViewModel.acceptExchange(exchange) },
            onRejectClick = { exchange -> bookViewModel.rejectExchange(exchange) }
        )
        // Используем RecyclerView, который был для истории
        binding.exchangeHistoryRecyclerView.apply {
            layoutManager = LinearLayoutManager(context)
            adapter = exchangeRequestAdapter
            isNestedScrollingEnabled = false
        }
    }

    private fun setupClickListeners() {
        binding.myBooksButton.setOnClickListener {
            try { findNavController().navigate(R.id.action_profileFragment_to_myBooksFragment) }
            catch (e: Exception) { Log.e(TAG, "Nav to myBooks failed", e); showNavErrorToast() }
        }
        binding.editProfileButton.setOnClickListener {
            try { findNavController().navigate(R.id.action_profileFragment_to_editProfileFragment) }
            catch (e: Exception) { Log.e(TAG, "Nav to editProfile failed", e); showNavErrorToast() }
        }
        // КНОПКА ЗАГРУЗКИ ЗАПРОСОВ
        binding.loadRequestsButton.setOnClickListener {
            Log.d(TAG, "Load requests button clicked.")
            bookViewModel.loadPendingReceivedRequests()
            // Сразу показываем заголовок, список будет обновлен через LiveData
            binding.exchangeHistoryTitle.isVisible = true
        }
        binding.logoutButton.setOnClickListener { showLogoutConfirmationDialog() }
    }

    private fun showLogoutConfirmationDialog() {
        AlertDialog.Builder(requireContext())
            .setTitle("Выход").setMessage("Вы уверены?")
            .setPositiveButton("Выйти") { _, _ -> auth.signOut(); navigateToLogin() }
            .setNegativeButton("Отмена", null).show()
    }

    private fun observeViewModel() {
        // Наблюдаем за данными пользователя
        bookViewModel.user.observe(viewLifecycleOwner) { user ->
            user?.let { bindUserData(it) } ?: run {
                bindUserData(User(nickname = "Загрузка...", email = auth.currentUser?.email ?: ""))
            }
        }

        // Наблюдаем за ЗАПРОСАМИ на обмен
        bookViewModel.pendingReceivedRequests.observe(viewLifecycleOwner) { requests ->
            Log.d(TAG, "Received ${requests?.size ?: 0} pending exchange requests.")
            exchangeRequestAdapter.submitList(requests ?: emptyList())
            // Показываем RecyclerView только если есть запросы (после нажатия кнопки)
            binding.exchangeHistoryRecyclerView.isVisible = !requests.isNullOrEmpty()
            // Можно добавить сообщение "Нет входящих запросов", если список пуст ПОСЛЕ загрузки
            if(binding.exchangeHistoryTitle.isVisible && requests.isNullOrEmpty() && bookViewModel.isLoadingRequests.value == false) {
                Toast.makeText(context, "Нет входящих запросов", Toast.LENGTH_SHORT).show()
            }
        }

        // Наблюдаем за результатом принятия/отклонения
        bookViewModel.exchangeActionResult.observe(viewLifecycleOwner, EventObserver { result ->
            result.onFailure { error ->
                Toast.makeText(context, "Ошибка обработки запроса: ${error.localizedMessage}", Toast.LENGTH_SHORT).show()
            }
            // Сообщение об успехе показывается из ViewModel
        })

        // Наблюдаем за статусом загрузки ЗАПРОСОВ
        bookViewModel.isLoadingRequests.observe(viewLifecycleOwner) { isLoading ->
            binding.progressBarRequests.isVisible = isLoading
            // Скрываем RecyclerView на время загрузки, если он был виден
            if (isLoading) binding.exchangeHistoryRecyclerView.isVisible = false
        }

        // Наблюдаем за общими ошибками (если они не связаны с загрузкой запросов)
        bookViewModel.errorMessage.observe(viewLifecycleOwner) { error ->
            error?.let {
                // Показываем ошибку, только если она не связана с загрузкой запросов (т.к. она обрабатывается выше)
                if(bookViewModel.isLoadingRequests.value == false){
                    Toast.makeText(context, it, Toast.LENGTH_LONG).show()
                }
                bookViewModel.clearErrorMessage()
            }
        }
    }

    private fun bindUserData(user: User) {
        binding.nameTextView.text = user.name.takeIf { !it.isNullOrBlank() } ?: user.nickname
        binding.subtitleTextView.text = user.email
        binding.cityValueTextView.text = user.city.takeIf { !it.isNullOrBlank() } ?: "Не указан"
        binding.ageValueTextView.text = user.age?.takeIf { it > 0 }?.toString() ?: "Не указан"
        binding.profileImageView.setImageResource(R.drawable.placeholder_avatar)
    }

    private fun navigateToLogin() {
        if (!isAdded || findNavController().currentDestination?.id == R.id.loginFragment) { return }
        try {
            findNavController().navigate(R.id.action_global_loginFragment)
        } catch (e: Exception) {
            Log.e(TAG,"Failed to navigate to login screen", e)
            showNavErrorToast("Ошибка навигации на экран входа.")
            if (findNavController().previousBackStackEntry != null) {
                try { findNavController().popBackStack() } catch (ignored: Exception) {}
            }
        }
    }

    private fun showNavErrorToast(message: String = "Не удалось выполнить переход") {
        Toast.makeText(context, message, Toast.LENGTH_SHORT).show()
    }

    override fun onDestroyView() {
        super.onDestroyView()
        binding.exchangeHistoryRecyclerView.adapter = null
        _binding = null
    }
}