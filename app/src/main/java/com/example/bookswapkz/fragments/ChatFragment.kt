package com.example.bookswapkz.fragments

import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import androidx.fragment.app.Fragment
import androidx.fragment.app.viewModels
import androidx.navigation.fragment.findNavController
import androidx.navigation.fragment.navArgs
import androidx.recyclerview.widget.LinearLayoutManager
import com.example.bookswapkz.adapters.MessagesAdapter
import com.example.bookswapkz.databinding.FragmentChatBinding
import com.example.bookswapkz.models.Chat
import com.example.bookswapkz.viewmodels.ChatViewModel
import dagger.hilt.android.AndroidEntryPoint

@AndroidEntryPoint
class ChatFragment : Fragment() {

    private var _binding: FragmentChatBinding? = null
    private val binding get() = _binding!!
    private val viewModel: ChatViewModel by viewModels()
    private val args: ChatFragmentArgs by navArgs()
    private lateinit var messagesAdapter: MessagesAdapter

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
        setupUI()
        setupObservers()
        loadMessages()
    }

    private fun setupUI() {
        // Setup toolbar
        binding.backButton.setOnClickListener {
            findNavController().navigateUp()
        }
        binding.userName.text = args.userName

        // Setup RecyclerView
        messagesAdapter = MessagesAdapter()
        binding.messagesRecyclerView.apply {
            adapter = messagesAdapter
            layoutManager = LinearLayoutManager(context).apply {
                stackFromEnd = true
            }
        }

        // Setup send button
        binding.sendButton.setOnClickListener {
            val message = binding.messageInput.text.toString().trim()
            if (message.isNotEmpty()) {
                viewModel.sendMessage(args.userId, message)
                binding.messageInput.text.clear()
            }
        }
    }

    private fun setupObservers() {
        viewModel.messages.observe(viewLifecycleOwner) { messages ->
            messagesAdapter.submitList(messages)
            binding.messagesRecyclerView.scrollToPosition(messages.size - 1)
        }
    }

    private fun loadMessages() {
        viewModel.loadMessages(args.userId)
    }

    override fun onDestroyView() {
        super.onDestroyView()
        _binding = null
    }
}