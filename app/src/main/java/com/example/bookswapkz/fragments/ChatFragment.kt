package com.example.bookswapkz.fragments

import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.Toast
import androidx.fragment.app.Fragment
import androidx.fragment.app.viewModels
import androidx.lifecycle.Lifecycle
import androidx.lifecycle.lifecycleScope
import androidx.lifecycle.repeatOnLifecycle
import androidx.navigation.fragment.findNavController
import androidx.navigation.fragment.navArgs
import androidx.recyclerview.widget.LinearLayoutManager
import com.example.bookswapkz.adapters.MessagesAdapter
import com.example.bookswapkz.databinding.FragmentChatDetailBinding
import com.example.bookswapkz.viewmodels.ChatViewModel
import com.google.firebase.auth.FirebaseAuth
import dagger.hilt.android.AndroidEntryPoint
import kotlinx.coroutines.launch

@AndroidEntryPoint
class ChatFragment : Fragment() {

    private var _binding: FragmentChatDetailBinding? = null
    private val binding get() = _binding!!

    private val viewModel: ChatViewModel by viewModels()
    private val args: ChatFragmentArgs by navArgs()
    private val messagesAdapter = MessagesAdapter()
    
    // Current user ID from Firebase Auth
    private val currentUserId: String
        get() = FirebaseAuth.getInstance().currentUser?.uid ?: ""

    override fun onCreateView(
        inflater: LayoutInflater,
        container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View {
        _binding = FragmentChatDetailBinding.inflate(inflater, container, false)
        return binding.root
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)
        setupRecyclerView()
        setupClickListeners()
        observeViewModel()
        loadMessages()
    }

    private fun setupRecyclerView() {
        binding.messagesRecyclerView.apply {
            layoutManager = LinearLayoutManager(context)
            adapter = messagesAdapter
        }
    }

    private fun setupClickListeners() {
        binding.sendButton.setOnClickListener {
            val messageText = binding.messageEditText.text?.toString()?.trim() ?: ""
            if (messageText.isNotEmpty()) {
                // Use currentUserId from Firebase Auth instead of args.userId
                viewModel.sendMessage(messageText, currentUserId)
                binding.messageEditText.text?.clear()
            }
        }
    }

    private fun observeViewModel() {
        viewLifecycleOwner.lifecycleScope.launch {
            viewLifecycleOwner.repeatOnLifecycle(Lifecycle.State.STARTED) {
                viewModel.messages.collect { messages ->
                    messagesAdapter.submitList(messages)
                    if (messages.isNotEmpty()) {
                        binding.messagesRecyclerView.smoothScrollToPosition(messages.size - 1)
                    }
                }
            }
        }

        viewLifecycleOwner.lifecycleScope.launch {
            viewLifecycleOwner.repeatOnLifecycle(Lifecycle.State.STARTED) {
                viewModel.errorMessage.collect { error ->
                    error?.let {
                        // Show error message
                    }
                }
            }
        }
    }

    private fun loadMessages() {
        try {
            // This assumes args.chatId will be available
            viewModel.loadMessages(args.chatId)
        } catch (e: Exception) {
            Toast.makeText(context, "Error loading chat: Invalid chat ID", Toast.LENGTH_SHORT).show()
        }
    }

    override fun onDestroyView() {
        super.onDestroyView()
        _binding = null
    }
}