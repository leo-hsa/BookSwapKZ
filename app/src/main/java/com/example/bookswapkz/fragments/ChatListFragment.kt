package com.example.bookswapkz.fragments

import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.Toast
import androidx.core.view.isVisible
import androidx.fragment.app.Fragment
import androidx.fragment.app.viewModels
import androidx.lifecycle.lifecycleScope
import androidx.navigation.fragment.findNavController
import com.example.bookswapkz.R
import com.example.bookswapkz.adapters.ChatsAdapter
import com.example.bookswapkz.databinding.FragmentChatListBinding
import com.example.bookswapkz.models.Chat
import com.example.bookswapkz.viewmodels.ChatListViewModel
import com.google.firebase.auth.FirebaseAuth
import dagger.hilt.android.AndroidEntryPoint
import kotlinx.coroutines.launch

@AndroidEntryPoint
class ChatListFragment : Fragment() {
    private var _binding: FragmentChatListBinding? = null
    private val binding get() = _binding!!
    private val viewModel: ChatListViewModel by viewModels()
    
    private val chatsAdapter = ChatsAdapter { chat ->
        val currentUserId = FirebaseAuth.getInstance().currentUser?.uid ?: ""
        // Get the other participant's ID (the one that's not the current user)
        val otherUserId = chat.participantIds.firstOrNull { it != currentUserId } ?: ""
        val otherUserInfo = chat.participantInfo[otherUserId] ?: mapOf()
        val otherUserName = otherUserInfo["nickname"] ?: "Unknown User"
        
        // Navigate to the chat detail screen
        findNavController().navigate(
            ChatListFragmentDirections.actionChatListToChatFragment(
                userId = currentUserId,
                userName = FirebaseAuth.getInstance().currentUser?.displayName ?: "",
                chatId = chat.chatId,
                otherUserId = otherUserId,
                otherUserName = otherUserName
            )
        )
    }

    override fun onCreateView(
        inflater: LayoutInflater,
        container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View {
        _binding = FragmentChatListBinding.inflate(inflater, container, false)
        return binding.root
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)
        setupRecyclerView()
        observeViewModel()
        loadChats()
    }

    private fun setupRecyclerView() {
        binding.chatsRecyclerView.apply {
            adapter = chatsAdapter
        }
    }

    private fun observeViewModel() {
        viewLifecycleOwner.lifecycleScope.launch {
            viewModel.chats.collect { chats ->
                chatsAdapter.submitList(chats)
                binding.emptyStateTextView.isVisible = chats.isEmpty()
            }
        }

        viewLifecycleOwner.lifecycleScope.launch {
            viewModel.errorMessage.collect { error -> 
                error?.let {
                    Toast.makeText(context, it, Toast.LENGTH_SHORT).show()
                }
            }
        }
    }

    private fun loadChats() {
        viewModel.loadUserChats()
    }

    override fun onDestroyView() {
        super.onDestroyView()
        _binding = null
    }
}