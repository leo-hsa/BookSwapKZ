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
import androidx.lifecycle.Lifecycle
import androidx.lifecycle.lifecycleScope
import androidx.lifecycle.repeatOnLifecycle
import androidx.navigation.fragment.findNavController
import androidx.recyclerview.widget.LinearLayoutManager
// import com.bumptech.glide.Glide // Glide больше не нужен здесь
import com.example.bookswapkz.R
import com.example.bookswapkz.adapters.MessagesAdapter
import com.example.bookswapkz.databinding.FragmentChatBinding
import com.example.bookswapkz.viewmodels.ChatViewModel
import com.google.firebase.auth.FirebaseAuth
import dagger.hilt.android.AndroidEntryPoint
import kotlinx.coroutines.launch

@AndroidEntryPoint
class ChatFragment : Fragment() {

    private var _binding: FragmentChatBinding? = null
    private val binding get() = _binding!!

    private val viewModel: ChatViewModel by viewModels()
    private lateinit var messagesAdapter: MessagesAdapter

    private val currentUserId: String?
        get() = FirebaseAuth.getInstance().currentUser?.uid

    override fun onCreateView(
        inflater: LayoutInflater,
        container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View {
        _binding = FragmentChatBinding.inflate(inflater, container, false)
        return binding.root
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)
        messagesAdapter = MessagesAdapter()

        Log.d("ChatFragment", "ViewCreated. ChatID from ViewModel: ${viewModel.chatId}, OtherUser: ${viewModel.otherUserIdFromNav}")

        if (viewModel.chatId.isBlank()) {
            Toast.makeText(context, "Ошибка: ID чата не найден.", Toast.LENGTH_LONG).show()
            Log.e("ChatFragment", "Chat ID is blank. Cannot proceed.")
            findNavController().popBackStack()
            return
        }

        setupToolbar()
        setupRecyclerView()
        setupSendButton()
        observeViewModel()
    }

    private fun setupToolbar() {
        binding.textViewUsernameToolbar.text = viewModel.otherUserNameFromNav // Имя из аргументов
        // Всегда показываем плейсхолдер для аватара
        binding.imageViewAvatarToolbar.setImageResource(R.drawable.placeholder_avatar)

        binding.buttonBackChat.setOnClickListener {
            findNavController().popBackStack()
        }
    }

    private fun setupRecyclerView() {
        binding.recyclerViewMessages.apply {
            layoutManager = LinearLayoutManager(context).apply {
                stackFromEnd = true
            }
            adapter = messagesAdapter
            itemAnimator = null
        }
    }

    private fun setupSendButton() {
        binding.buttonSendMessage.setOnClickListener {
            val messageText = binding.editTextMessageInput.text?.toString()?.trim() ?: ""
            if (messageText.isNotEmpty()) {
                if (currentUserId != null) {
                    viewModel.sendMessage(messageText)
                    binding.editTextMessageInput.text?.clear()
                } else {
                    Toast.makeText(context, "Вы не авторизованы.", Toast.LENGTH_SHORT).show()
                }
            }
        }
    }

    private fun observeViewModel() {
        viewLifecycleOwner.lifecycleScope.launch {
            viewLifecycleOwner.repeatOnLifecycle(Lifecycle.State.STARTED) {
                launch {
                    viewModel.messages.collect { messages ->
                        Log.d("ChatFragment", "Updating adapter with ${messages.size} messages.")
                        val oldSize = messagesAdapter.itemCount
                        messagesAdapter.submitList(messages) {
                            if (messages.isNotEmpty() && messages.size > oldSize) {
                                binding.recyclerViewMessages.smoothScrollToPosition(messages.size - 1)
                            } else if (messages.isNotEmpty() && oldSize == 0) {
                                binding.recyclerViewMessages.scrollToPosition(messages.size -1)
                            }
                        }
                    }
                }

                // Наблюдение за otherUserInfo (можно упростить, если аватар не нужен)
                launch {
                    viewModel.otherUserInfo.collect { user ->
                        user?.let {
                            // Обновляем имя, если оно отличается от того, что пришло в аргументах
                            if (binding.textViewUsernameToolbar.text.toString() != (it.nickname ?: viewModel.otherUserNameFromNav)) {
                                binding.textViewUsernameToolbar.text = it.nickname ?: viewModel.otherUserNameFromNav
                            }
                        }
                        // Аватар всегда плейсхолдер
                        binding.imageViewAvatarToolbar.setImageResource(R.drawable.placeholder_avatar)
                    }
                }


                launch {
                    viewModel.errorMessage.collect { error ->
                        error?.let {
                            Toast.makeText(context, it, Toast.LENGTH_LONG).show()
                            viewModel.clearErrorMessage()
                        }
                    }
                }

                launch {
                    viewModel.sendMessageResult.collect { result ->
                        result?.onFailure {
                            Log.e("ChatFragment", "Send message failed: ${it.localizedMessage}")
                        }
                        result?.onSuccess {
                            Log.d("ChatFragment", "Message send success observed in fragment.")
                        }
                    }
                }

                launch {
                    viewModel.isLoading.collect { isLoading ->
                        binding.progressBarChat.isVisible = isLoading
                    }
                }
            }
        }
    }

    override fun onDestroyView() {
        super.onDestroyView()
        _binding = null
    }
}