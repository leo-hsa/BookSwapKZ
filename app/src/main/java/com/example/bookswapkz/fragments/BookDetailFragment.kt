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
import androidx.fragment.app.activityViewModels // Используем activityViewModels для BookViewModel
import androidx.lifecycle.Observer // Убедись, что этот импорт есть
import androidx.navigation.fragment.findNavController
import androidx.navigation.fragment.navArgs
import com.bumptech.glide.Glide
import com.example.bookswapkz.R
import com.example.bookswapkz.databinding.FragmentBookDetailBinding
import com.example.bookswapkz.models.Book
import com.example.bookswapkz.models.BookType
// import com.example.bookswapkz.models.User // Не используется напрямую здесь
import com.example.bookswapkz.viewmodels.BookViewModel
import com.google.firebase.auth.FirebaseAuth
import dagger.hilt.android.AndroidEntryPoint
// import kotlin.Result // Уже импортирован или доступен
import java.text.SimpleDateFormat
import java.util.* // Для Date и Locale

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
            val rentInfo = if (book.rentPrice != null && (book.rentPrice > 0 || book.rentPrice == 0.0)) { // Исправлено условие для "Бесплатно"
                if (book.rentPrice == 0.0) "Бесплатно" else "${book.rentPrice} ₸/${book.rentPeriod ?: "час"}"
            } else {
                "Цена не указана" // Или другое значение по умолчанию
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
        // val canExchange = currentUserId != null && !isOwner && !book.isRented // Если isRented есть в модели Book
        val canExchange = currentUserId != null && !isOwner // Упрощенный вариант

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
            showExchangeConfirmationDialog(book)
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
            options.add("Редактировать") // TODO: Implement edit
            options.add("Удалить")     // TODO: Implement delete
        } else {
            options.add("Пожаловаться") // TODO: Implement report
        }

        AlertDialog.Builder(requireContext())
            .setItems(options.toTypedArray()) { _, which ->
                when {
                    isOwner && which == 0 -> {
                        Toast.makeText(context, "Редактирование пока не доступно", Toast.LENGTH_SHORT).show()
                    }
                    isOwner && which == 1 -> {
                        Toast.makeText(context, "Удаление пока не доступно", Toast.LENGTH_SHORT).show()
                    }
                    !isOwner && which == 0 -> {
                        Toast.makeText(context, "Жалоба пока не доступна", Toast.LENGTH_SHORT).show()
                    }
                }
            }
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
            // Опционально: findNavController().navigate(R.id.action_global_loginFragment)
            return
        }

        val currentUserId = firebaseAuthUser.uid
        val currentUserName = currentAppUser.nickname?.takeIf { it.isNotBlank() }
            ?: firebaseAuthUser.displayName?.takeIf { it.isNotBlank() }
            ?: "Вы"

        val chatLiveData = bookViewModel.getOrCreateChatForNavigation(ownerId)

        chatLiveData.observe(viewLifecycleOwner) { chatIdResult -> // chatIdResult здесь Result<String>?
            // Этот LiveData наблюдатель будет автоматически удален, когда viewLifecycleOwner уничтожится.
            // Если getOrCreateChatForNavigation возвращает новый LiveData каждый раз,
            // то ручное удаление наблюдателя не так критично для этого случая, так как
            // мы обычно переходим на другой экран после первого результата.
            // Но если вы хотите быть уверены, что он сработает только один раз на один клик,
            // и BookViewModel может переиспользовать тот же LiveData, тогда нужно было бы удалять наблюдателя.

            chatIdResult?.let { result ->
                binding.progressBarDetail.isVisible = false // Скрываем прогресс после получения ответа

                result.onSuccess { chatId ->
                    if (chatId.isNotBlank()) {
                        Log.i("BookDetailFragment", "Chat ID $chatId получен. Переход в ChatFragment...")
                        try {
                            val action = BookDetailFragmentDirections.actionBookDetailFragmentToChatFragment(
                                chatId = chatId,
                                userId = currentUserId,
                                userName = currentUserName,
                                otherUserId = ownerId,
                                otherUserName = ownerNickname
                            )
                            findNavController().navigate(action)
                        } catch (e: Exception) {
                            Log.e("BookDetailFragment", "Ошибка навигации в ChatFragment", e)
                            Toast.makeText(context, "Ошибка перехода в чат: ${e.localizedMessage}", Toast.LENGTH_LONG).show()
                        }
                    } else {
                        Log.e("BookDetailFragment", "Полученный Chat ID пустой.")
                        Toast.makeText(context, "Не удалось создать или найти чат (пустой ID).", Toast.LENGTH_SHORT).show()
                    }
                }
                result.onFailure { error ->
                    Log.e("BookDetailFragment", "Не удалось получить или создать чат", error)
                    Toast.makeText(context, "Ошибка при создании чата: ${error.localizedMessage}", Toast.LENGTH_LONG).show()
                }
            }
        }
    }


    private fun observeBookViewModel() {
        bookViewModel.exchangeResult.observe(viewLifecycleOwner) { result ->
            // Не скрываем progressBarDetail здесь, так как им управляет triggerExchange
            result?.onSuccess {
                Toast.makeText(requireContext(), "Обмен зарегистрирован!", Toast.LENGTH_SHORT).show()
                findNavController().popBackStack() // Возвращаемся назад после успешного обмена
            }
            result?.onFailure { error ->
                Log.e("BookDetailFragment", "Exchange failed", error)
                Toast.makeText(requireContext(), "Ошибка обмена: ${error.localizedMessage}", Toast.LENGTH_LONG).show()
            }
            // Сбрасываем результат, чтобы не срабатывать повторно при пересоздании View
            if (result != null) bookViewModel.clearExchangeResult()
        }

        bookViewModel.errorMessage.observe(viewLifecycleOwner) { error ->
            error?.let {
                // progressBarDetail.isVisible = false // Управляется в специфичных операциях
                Toast.makeText(requireContext(), it, Toast.LENGTH_LONG).show()
                bookViewModel.clearErrorMessage() // Сбрасываем ошибку после показа
            }
        }

        bookViewModel.isLoading.observe(viewLifecycleOwner) { isLoading ->
            if (!isAdded) return@observe // Предотвращение креша если фрагмент отсоединен
            // Эта общая isLoading может конфликтовать с progressBarDetail, управляемым в handleChatButtonClick
            // Лучше, чтобы каждая операция управляла своим индикатором или использовала общий с осторожностью.
            // Пока что, если progressBarDetail уже видим из-за другой операции, не трогаем его.
            if (binding.progressBarDetail.isVisible && !isLoading) {
                // Если progressBar видим и ViewModel говорит, что загрузка закончилась,
                // но это не операция чата, то скрываем.
                // Это допущение, что isLoading из ViewModel не относится к операции чата.
            } else if (!binding.progressBarDetail.isVisible && isLoading) {
                //binding.progressBarDetail.isVisible = isLoading // Можно включить, если это основной индикатор
            }
            // Более надежно: каждая асинхронная операция во ViewModel должна сама управлять своим isLoading флагом,
            // а фрагмент подписывается на конкретные флаги.
            // Либо, если isLoading общий, то он должен быть true только во время одной операции.
        }
    }

    private fun showExchangeConfirmationDialog(book: Book) {
        AlertDialog.Builder(requireContext())
            .setTitle("Подтверждение обмена")
            .setMessage("Вы уверены, что хотите обменяться книгой \"${book.title}\"? Владелец получит уведомление о вашем запросе.")
            .setPositiveButton("Обменять") { dialog, _ ->
                // progressBarDetail будет управляться в bookViewModel.triggerExchange через isLoading
                binding.exchangeButton.isEnabled = false // Блокируем кнопку на время операции
                bookViewModel.triggerExchange(book)
                dialog.dismiss()
            }
            .setNegativeButton("Отмена") { dialog, _ ->
                binding.exchangeButton.isEnabled = true // Разблокируем кнопку, если отмена
                dialog.dismiss()
            }
            .show()
    }

    private fun checkCallPermissionAndDial(phoneNumber: String) {
        if (ContextCompat.checkSelfPermission(requireContext(), android.Manifest.permission.CALL_PHONE) != PackageManager.PERMISSION_GRANTED) {
            // Запрашиваем разрешение
            ActivityCompat.requestPermissions(requireActivity(), arrayOf(android.Manifest.permission.CALL_PHONE), CALL_PHONE_REQUEST_CODE)
        } else {
            // Разрешение уже есть
            dialPhone(phoneNumber)
        }
    }

    // Обработка результата запроса разрешений
    override fun onRequestPermissionsResult(requestCode: Int, permissions: Array<out String>, grantResults: IntArray) {
        super.onRequestPermissionsResult(requestCode, permissions, grantResults)
        if (requestCode == CALL_PHONE_REQUEST_CODE) {
            if ((grantResults.isNotEmpty() && grantResults[0] == PackageManager.PERMISSION_GRANTED)) {
                // Разрешение получено
                currentBook.phone?.takeIf { it.isNotBlank() }?.let { dialPhone(it) }
            } else {
                // В разрешении отказано
                Toast.makeText(context, "Разрешение на звонок отклонено", Toast.LENGTH_SHORT).show()
            }
        }
    }

    private fun dialPhone(phoneNumber: String) {
        val intent = Intent(Intent.ACTION_DIAL).apply {
            data = Uri.parse("tel:$phoneNumber")
        }
        try {
            startActivity(intent)
        } catch (e: Exception) {
            Toast.makeText(context, "Не удалось найти приложение для звонка", Toast.LENGTH_SHORT).show()
        }
    }

    override fun onDestroyView() {
        super.onDestroyView()
        _binding = null
    }

    // Если нужно обновлять детали книги извне (например, после редактирования)
    // fun updateBookDisplay(newBook: Book) {
    //     currentBook = newBook
    //     if (_binding != null) { // Проверяем, что binding еще существует
    //         displayBookDetails(newBook)
    //     }
    // }

    // Форматирование времени (если нужно)
    // private fun formatTimestamp(timestamp: Date): String {
    //     val now = Date()
    //     val diff = now.time - timestamp.time
    //     val seconds = diff / 1000
    //     val minutes = seconds / 60
    //     val hours = minutes / 60
    //     val days = hours / 24
    //
    //     return when {
    //         seconds < 60 -> "только что"
    //         minutes < 60 -> "$minutes мин назад"
    //         hours < 24 -> "$hours ч назад"
    //         days < 7 -> "$days д назад"
    //         else -> SimpleDateFormat("dd.MM.yyyy", Locale.getDefault()).format(timestamp)
    //     }
    // }
}