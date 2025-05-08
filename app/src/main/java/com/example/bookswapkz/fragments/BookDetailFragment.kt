package com.example.bookswapkz.fragments

import android.app.AlertDialog // Импорт стандартного AlertDialog
import android.content.Intent
import android.content.pm.PackageManager
import android.net.Uri
import android.os.Bundle
import android.util.Log
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.Toast
// import androidx.appcompat.app.AlertDialog // Можно использовать и этот, если есть зависимость appcompat
import androidx.core.app.ActivityCompat
import androidx.core.content.ContextCompat
import androidx.core.view.isVisible
import androidx.fragment.app.Fragment
import androidx.fragment.app.activityViewModels
import androidx.lifecycle.Observer
import androidx.navigation.fragment.findNavController
import androidx.navigation.fragment.navArgs
import com.bumptech.glide.Glide
import com.example.bookswapkz.R
import com.example.bookswapkz.databinding.FragmentBookDetailBinding
import com.example.bookswapkz.models.Book
import com.example.bookswapkz.models.BookType
import com.example.bookswapkz.viewmodels.BookViewModel
import com.example.bookswapkz.utils.EventObserver // Импорт EventObserver
import com.google.firebase.auth.FirebaseAuth
import dagger.hilt.android.AndroidEntryPoint
import java.text.SimpleDateFormat
import java.util.*

@AndroidEntryPoint
class BookDetailFragment : Fragment() {

    private var _binding: FragmentBookDetailBinding? = null
    private val binding get() = _binding!!
    private val args: BookDetailFragmentArgs by navArgs()

    private val bookViewModel: BookViewModel by activityViewModels()

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
        observeBookViewModel()

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

        binding.rentInfoContainer.isVisible = book.bookType == BookType.RENT || book.bookType == BookType.BOTH
        if (book.bookType == BookType.RENT || book.bookType == BookType.BOTH) {
            val rentInfo = if (book.rentPrice != null && (book.rentPrice > 0 || book.rentPrice == 0.0)) {
                if (book.rentPrice == 0.0) "Бесплатно" else "${book.rentPrice} ₸/${book.rentPeriod ?: "час"}"
            } else {
                "Цена не указана"
            }
            binding.rentInfoTextView.text = rentInfo
        }

        if (!book.imageUrl.isNullOrEmpty()) {
            Glide.with(requireContext())
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
        val canExchange = currentUserId != null && !isOwner

        binding.exchangeButton.isVisible = canExchange
        binding.messageButton.isVisible = currentUserId != null && !isOwner
        binding.shareButton.isVisible = true
    }

    private fun setupButtonClickListeners(book: Book) {
        binding.phoneDetailTextView.setOnClickListener {
            book.phone?.takeIf { it.isNotBlank() }?.let { phone ->
                checkCallPermissionAndDial(phone)
            }
        }

        binding.exchangeButton.setOnClickListener {
            // Теперь инициируем запрос на обмен
            showRequestConfirmationDialog(book)
        }

        binding.messageButton.setOnClickListener {
            val ownerId = book.userId
            val ownerNickname = book.ownerNickname ?: "Собеседник"
            if (ownerId.isNotBlank()) {
                handleChatButtonClick(ownerId, ownerNickname)
            } else {
                Toast.makeText(requireContext(), "Невозможно начать чат: владелец не определен.", Toast.LENGTH_SHORT).show()
            }
        }

        binding.shareButton.setOnClickListener {
            shareBook(book)
        }

        binding.menuButton.setOnClickListener {
            showBookOptionsMenu(book)
        }
    }

    private fun shareBook(book: Book) {
        val shareIntent = Intent(Intent.ACTION_SEND).apply {
            type = "text/plain"
            val shareText = "Посмотрите книгу \"${book.title}\" автора ${book.author} на BookSwapKZ!"
            putExtra(Intent.EXTRA_TEXT, shareText)
        }
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
            .setItems(options.toTypedArray()) { _, which -> /* TODO: Implement actions */ }
            .show()
    }

    private fun handleChatButtonClick(ownerId: String, ownerNickname: String) {
        Log.d("BookDetailFragment", "Chat button clicked for owner ID: $ownerId, owner nickname: $ownerNickname")
        binding.progressBarDetail.isVisible = true

        val currentAppUser = bookViewModel.user.value
        val firebaseAuthUser = FirebaseAuth.getInstance().currentUser
        if (firebaseAuthUser == null || currentAppUser == null) {
            Toast.makeText(requireContext(), "Пожалуйста, войдите в систему.", Toast.LENGTH_SHORT).show()
            binding.progressBarDetail.isVisible = false
            return
        }
        val currentUserId = firebaseAuthUser.uid
        val currentUserName = currentAppUser.nickname?.takeIf { it.isNotBlank() }
            ?: firebaseAuthUser.displayName?.takeIf { it.isNotBlank() }
            ?: "Вы"

        val chatLiveData = bookViewModel.getOrCreateChatForNavigation(ownerId)
        chatLiveData.observe(viewLifecycleOwner) { chatIdResult ->
            chatIdResult?.let { result ->
                binding.progressBarDetail.isVisible = false
                result.onSuccess { chatId ->
                    if (chatId.isNotBlank()) {
                        Log.i("BookDetailFragment", "Chat ID $chatId obtained. Navigating...")
                        try {
                            val action = BookDetailFragmentDirections.actionBookDetailFragmentToChatFragment(
                                chatId = chatId, userId = currentUserId, userName = currentUserName,
                                otherUserId = ownerId, otherUserName = ownerNickname
                            )
                            findNavController().navigate(action)
                        } catch (e: Exception) {
                            Log.e("BookDetailFragment", "Navigation failed", e)
                            Toast.makeText(context, "Ошибка перехода в чат: ${e.localizedMessage}", Toast.LENGTH_LONG).show()
                        }
                    } else {
                        Log.e("BookDetailFragment", "Received blank chat ID.")
                        Toast.makeText(context, "Не удалось создать или найти чат (пустой ID).", Toast.LENGTH_SHORT).show()
                    }
                }
                result.onFailure { error ->
                    Log.e("BookDetailFragment", "Failed to get or create chat", error)
                    Toast.makeText(context, "Ошибка при создании чата: ${error.localizedMessage}", Toast.LENGTH_LONG).show()
                }
                // Не удаляем наблюдателя вручную, LiveData сам очистится
            }
        }
    }

    // Диалог подтверждения запроса на обмен
    private fun showRequestConfirmationDialog(book: Book) {
        AlertDialog.Builder(requireContext())
            .setTitle("Запрос на обмен")
            .setMessage("Вы уверены, что хотите запросить книгу \"${book.title}\"? Владелец получит уведомление.")
            .setPositiveButton("Запросить") { dialog, _ ->
                // progressBar будет управляться через isLoading во ViewModel
                binding.exchangeButton.isEnabled = false // Блокируем кнопку
                bookViewModel.requestExchange(book)
                dialog.dismiss()
            }
            .setNegativeButton("Отмена") { dialog, _ ->
                binding.exchangeButton.isEnabled = true // Разблокируем
                dialog.dismiss()
            }
            .show()
    }

    private fun observeBookViewModel() {
        // Наблюдаем за результатом отправки запроса (для разблокировки кнопки)
        bookViewModel.requestSentResult.observe(viewLifecycleOwner, EventObserver { result ->
            binding.exchangeButton.isEnabled = true // Разблокируем кнопку после ответа
            // Toast об успехе/ошибке показывается из ViewModel
        })

        bookViewModel.errorMessage.observe(viewLifecycleOwner) { error ->
            error?.let {
                Toast.makeText(requireContext(), it, Toast.LENGTH_LONG).show()
                bookViewModel.clearErrorMessage()
            }
        }

        bookViewModel.isLoading.observe(viewLifecycleOwner) { isLoading ->
            if (!isAdded) return@observe
            // Управляем прогресс-баром только если кнопка обмена заблокирована (т.е. идет операция обмена)
            if (!binding.exchangeButton.isEnabled) {
                binding.progressBarDetail.isVisible = isLoading
            } else if (!isLoading) {
                // Если загрузка закончилась, а кнопка разблокирована, скрываем прогресс
                binding.progressBarDetail.isVisible = false
            }
        }
    }

    private fun checkCallPermissionAndDial(phoneNumber: String) {
        if (ContextCompat.checkSelfPermission(requireContext(), android.Manifest.permission.CALL_PHONE) != PackageManager.PERMISSION_GRANTED) {
            ActivityCompat.requestPermissions(requireActivity(), arrayOf(android.Manifest.permission.CALL_PHONE), CALL_PHONE_REQUEST_CODE)
        } else {
            dialPhone(phoneNumber)
        }
    }

    override fun onRequestPermissionsResult(requestCode: Int, permissions: Array<out String>, grantResults: IntArray) {
        super.onRequestPermissionsResult(requestCode, permissions, grantResults)
        if (requestCode == CALL_PHONE_REQUEST_CODE) {
            if ((grantResults.isNotEmpty() && grantResults[0] == PackageManager.PERMISSION_GRANTED)) {
                currentBook.phone?.takeIf { it.isNotBlank() }?.let { dialPhone(it) }
            } else {
                Toast.makeText(context, "Разрешение на звонок отклонено", Toast.LENGTH_SHORT).show()
            }
        }
    }

    private fun dialPhone(phoneNumber: String) {
        val intent = Intent(Intent.ACTION_DIAL).apply { data = Uri.parse("tel:$phoneNumber") }
        try { startActivity(intent) }
        catch (e: Exception) { Toast.makeText(context, "Не удалось найти приложение для звонка", Toast.LENGTH_SHORT).show() }
    }

    override fun onDestroyView() {
        super.onDestroyView()
        _binding = null
    }
}