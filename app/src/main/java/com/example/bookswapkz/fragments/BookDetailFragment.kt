package com.example.bookswapkz.fragments

import android.content.Intent
import android.content.pm.PackageManager
import android.net.Uri
import android.os.Bundle
import android.util.Log
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.Toast
import androidx.appcompat.app.AlertDialog
import androidx.core.app.ActivityCompat
import androidx.core.content.ContextCompat
import androidx.core.view.isVisible
import androidx.fragment.app.Fragment
import androidx.fragment.app.viewModels
import androidx.lifecycle.Observer
import androidx.navigation.fragment.findNavController
import androidx.navigation.fragment.navArgs
import com.bumptech.glide.Glide
import com.example.bookswapkz.R
import com.example.bookswapkz.databinding.FragmentBookDetailBinding
import com.example.bookswapkz.models.Book
import com.example.bookswapkz.models.BookType
import com.example.bookswapkz.models.User
import com.example.bookswapkz.viewmodels.BookViewModel
import com.google.firebase.auth.FirebaseAuth
import dagger.hilt.android.AndroidEntryPoint
import kotlin.Result
import java.text.SimpleDateFormat
import java.util.*

@AndroidEntryPoint
class BookDetailFragment : Fragment() {

    private var _binding: FragmentBookDetailBinding? = null
    private val binding get() = _binding!!
    private val args: BookDetailFragmentArgs by navArgs()
    private val viewModel: BookViewModel by viewModels({ requireActivity() })

    private val CALL_PHONE_REQUEST_CODE = 101
    private lateinit var currentBook: Book

    override fun onCreateView(
        inflater: LayoutInflater, container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View {
        _binding = FragmentBookDetailBinding.inflate(inflater, container, false)
        currentBook = args.book
        return binding.root
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)
        displayBookDetails(currentBook)
        setupButtonClickListeners(currentBook)
        observeViewModel()

        // Настройка тулбара
        binding.toolbar.setNavigationOnClickListener {
            findNavController().popBackStack()
        }
    }

    private fun displayBookDetails(book: Book) {
        binding.titleDetailTextView.text = book.title
        binding.authorDetailTextView.text = book.author
        binding.conditionDetailTextView.text = book.condition
        binding.cityDetailTextView.text = book.city
        binding.ownerDetailTextView.text = book.ownerNickname ?: "Неизвестно"

        binding.phoneDetailTextView.text = book.phone ?: "Не указан"
        binding.phoneDetailTextView.isVisible = !book.phone.isNullOrBlank()

        // Отображение информации об аренде
        binding.rentInfoContainer.isVisible = book.bookType == BookType.RENT || book.bookType == BookType.BOTH
        if (book.bookType == BookType.RENT || book.bookType == BookType.BOTH) {
            val rentInfo = if (book.rentPrice != null) {
                "${book.rentPrice} ₸/${book.rentPeriod ?: "час"}"
            } else {
                "Бесплатно"
            }
            binding.rentInfoTextView.text = rentInfo
        }

        // Загрузка изображения книги
        if (!book.imageUrl.isNullOrEmpty()) {
            Glide.with(binding.root.context)
                .load(book.imageUrl)
                .placeholder(R.drawable.book_cover_placeholder)
                .error(R.drawable.ic_book_placeholder_error)
                .centerCrop()
                .into(binding.bookImageDetail)
        } else {
            binding.bookImageDetail.setImageResource(R.drawable.book_cover_placeholder)
        }

        val currentUserId = FirebaseAuth.getInstance().currentUser?.uid
        val isOwner = currentUserId == book.userId
        val canExchange = currentUserId != null && !isOwner && !book.isRented

        binding.exchangeButton.isVisible = canExchange

        // Настройка кнопок действий внизу
        binding.messageButton.isVisible = currentUserId != null && !isOwner
        binding.shareButton.isVisible = true
    }

    private fun setupButtonClickListeners(book: Book) {
        // Кнопка телефона - теперь в карточке информации
        binding.phoneDetailTextView.setOnClickListener {
            book.phone?.let { phone ->
                if (phone.isNotBlank()) {
                    checkCallPermissionAndDial(phone)
                }
            }
        }

        binding.exchangeButton.setOnClickListener {
            showExchangeConfirmationDialog(book)
        }

        // Новые кнопки в нижней части экрана
        binding.messageButton.setOnClickListener {
            val ownerId = book.userId
            val ownerNickname = book.ownerNickname ?: "Собеседник"
            if (ownerId.isNotBlank()) {
                handleChatButtonClick(ownerId, ownerNickname)
            } else {
                Toast.makeText(requireContext(), "Невозможно начать чат", Toast.LENGTH_SHORT).show()
            }
        }

        binding.shareButton.setOnClickListener {
            shareBook(book)
        }

        // Кнопка меню в тулбаре
        binding.menuButton.setOnClickListener {
            showBookOptionsMenu(book)
        }
    }

    private fun shareBook(book: Book) {
        val shareIntent = Intent(Intent.ACTION_SEND)
        shareIntent.type = "text/plain"
        val shareText = "Посмотрите книгу \"${book.title}\" автора ${book.author} на BookSwapKZ!"
        shareIntent.putExtra(Intent.EXTRA_TEXT, shareText)
        startActivity(Intent.createChooser(shareIntent, "Поделиться книгой"))
    }

    private fun showBookOptionsMenu(book: Book) {
        val currentUserId = FirebaseAuth.getInstance().currentUser?.uid
        val isOwner = currentUserId == book.userId

        val options = ArrayList<String>()
        if (isOwner) {
            options.add("Редактировать")
            options.add("Удалить")
        } else {
            options.add("Пожаловаться")
        }

        AlertDialog.Builder(requireContext())
            .setItems(options.toTypedArray()) { _, which ->
                when {
                    isOwner && which == 0 -> {} // TODO: Редактирование книги
                    isOwner && which == 1 -> {} // TODO: Удаление книги
                    !isOwner && which == 0 -> {} // TODO: Жалоба на книгу
                }
            }
            .show()
    }

    private fun handleChatButtonClick(ownerId: String, ownerNickname: String) {
        Log.d("BookDetailFragment", "Chat button clicked for owner ID: $ownerId")
        binding.progressBarDetail.isVisible = true

        viewModel.getOrCreateChatForNavigation(ownerId).observe(viewLifecycleOwner, Observer { chatIdResult ->
            binding.progressBarDetail.isVisible = false

            if (chatIdResult != null) {
                chatIdResult.onSuccess { chatId ->
                    if (chatId.isNotBlank()) {
                        Log.i("BookDetailFragment", "Chat ID $chatId obtained. Navigating...")
                        try {
                            val action = BookDetailFragmentDirections.actionBookDetailFragmentToChatFragment(
                                userId = FirebaseAuth.getInstance().currentUser?.uid ?: "",
                                userName = FirebaseAuth.getInstance().currentUser?.displayName ?: ""
                            )
                            findNavController().navigate(action)
                        } catch (e: Exception) {
                            Log.e("BookDetailFragment", "Navigation failed", e)
                            Toast.makeText(context, "Ошибка перехода: ${e.localizedMessage}", Toast.LENGTH_SHORT).show()
                        }
                    } else {
                        Toast.makeText(context, "Не удалось создать чат", Toast.LENGTH_SHORT).show()
                    }
                }
                chatIdResult.onFailure { error ->
                    Log.e("BookDetailFragment", "Failed to get or create chat", error)
                }
                viewModel.getOrCreateChatForNavigation(ownerId).removeObservers(viewLifecycleOwner)
            }
        })
    }

    private fun observeViewModel() {
        // --- ИСПРАВЛЕНО: Обработка Result<Unit> ---
        viewModel.exchangeResult.observe(viewLifecycleOwner) { result ->
            if (result != null) {
                binding.progressBarDetail?.isVisible = false
                val isOwner = FirebaseAuth.getInstance().currentUser?.uid == currentBook.userId // userId
                val canExchange = !isOwner && !currentBook.isRented
                binding.exchangeButton?.isEnabled = canExchange

                result.onSuccess { // onSuccess
                    Toast.makeText(requireContext(), "Обмен зарегистрирован!", Toast.LENGTH_SHORT).show()
                    findNavController().popBackStack()
                }
                result.onFailure { error -> // onFailure
                    Log.e("BookDetailFragment", "Exchange failed", error)
                }
                viewModel.clearExchangeResult() // Используем clearExchangeResult
            }
        }

        viewModel.errorMessage.observe(viewLifecycleOwner) { error ->
            if (error != null) {
                binding.progressBarDetail?.isVisible = false
                // --- ИСПРАВЛЕНО: Импорт и вызов Toast ---
                Toast.makeText(requireContext(), error, Toast.LENGTH_LONG).show()
                viewModel.clearErrorMessage() // Используем clearErrorMessage
            }
        }
    }

    private fun showExchangeConfirmationDialog(book: Book) {
        AlertDialog.Builder(requireContext())
            .setTitle("Подтверждение обмена")
            .setMessage("Вы уверены, что хотите обменяться книгой \"${book.title}\"? Владелец получит уведомление о вашем запросе.")
            .setPositiveButton("Обменять") { dialog, _ ->
                binding.progressBarDetail.isVisible = true
                binding.exchangeButton.isEnabled = false
                viewModel.triggerExchange(book)
                dialog.dismiss()
            }
            .setNegativeButton("Отмена", null)
            .show()
    }

    private fun checkCallPermissionAndDial(phoneNumber: String) {
        if (ContextCompat.checkSelfPermission(requireContext(), android.Manifest.permission.CALL_PHONE) != PackageManager.PERMISSION_GRANTED) {
            ActivityCompat.requestPermissions(requireActivity(), arrayOf(android.Manifest.permission.CALL_PHONE), CALL_PHONE_REQUEST_CODE)
        } else {
            dialPhone(phoneNumber)
        }
    }

    private fun dialPhone(phoneNumber: String) {
        val intent = Intent(Intent.ACTION_DIAL)
        intent.data = Uri.parse("tel:$phoneNumber")
        startActivity(intent)
    }

    override fun onDestroyView() {
        super.onDestroyView()
        _binding = null
    }

    fun updateBook(newBook: Book) {
        currentBook = newBook
        if (_binding != null) {
            displayBookDetails(newBook)
        }
    }

    private fun formatTimestamp(timestamp: Date): String {
        val now = Date()
        val diff = now.time - timestamp.time
        val seconds = diff / 1000
        val minutes = seconds / 60
        val hours = minutes / 60
        val days = hours / 24

        return when {
            seconds < 60 -> "только что"
            minutes < 60 -> "$minutes мин назад"
            hours < 24 -> "$hours ч назад"
            days < 7 -> "$days д назад"
            else -> SimpleDateFormat("dd.MM.yyyy", Locale.getDefault()).format(timestamp)
        }
    }
}