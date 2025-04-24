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
import com.example.bookswapkz.models.User
import com.example.bookswapkz.viewmodels.BookViewModel
import com.google.firebase.auth.FirebaseAuth
import dagger.hilt.android.AndroidEntryPoint

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
    }

    private fun displayBookDetails(book: Book) {
        binding.titleDetailTextView.text = book.title
        binding.authorDetailTextView.text = book.author
        binding.conditionDetailTextView.text = book.condition
        binding.cityDetailTextView.text = book.city
        binding.ownerNicknameTextView.text = book.ownerName
        binding.ownerNicknameTextView.visibility = View.VISIBLE

        binding.phoneDetailTextView.text = book.phone ?: "Не указан"
        binding.phoneDetailTextView.isVisible = !book.phone.isNullOrBlank()
        binding.callButton.isVisible = !book.phone.isNullOrBlank()

        binding.ownerCountDetailTextView.text = getString(R.string.owner_count_format, book.ownerCount)

        binding.rentInfoLabelTextView.visibility = if (book.isForRent) View.VISIBLE else View.GONE
        binding.rentInfoTextView.visibility = if (book.isForRent) View.VISIBLE else View.GONE
        if (book.isForRent) {
            binding.rentInfoTextView.text = getString(
                R.string.rent_info_format,
                book.rentPrice ?: 0.0,
                book.rentDurationHours ?: 0,
                book.rentTotalPrice ?: 0.0
            )
        }

        if (!book.imageUrl.isNullOrEmpty()) {
            Glide.with(this)
                .load(book.imageUrl)
                .placeholder(R.drawable.ic_book_placeholder)
                .error(R.drawable.ic_book_placeholder_error)
                .into(binding.bookImageDetail)
            binding.bookImageDetail.isVisible = true
        } else {
            binding.bookImageDetail.setImageResource(R.drawable.ic_book_placeholder)
            binding.bookImageDetail.isVisible = true
        }

        val currentUserId = FirebaseAuth.getInstance().currentUser?.uid
        val canExchange = currentUserId != null && book.userId != currentUserId && !book.isRented
        binding.exchangeButton.isVisible = canExchange
        if(book.isRented) {
            binding.exchangeButton.isEnabled = false
            binding.exchangeButton.text = "Уже в аренде"
        } else if (canExchange) {
            binding.exchangeButton.isEnabled = true
            binding.exchangeButton.text = "Предложить обмен"
        } else {
            binding.exchangeButton.isVisible = false
        }

    }

    private fun setupButtonClickListeners(book: Book) {
        binding.callButton.setOnClickListener {
            val phoneNumber = book.phone
            if (!phoneNumber.isNullOrBlank()) {
                checkCallPermissionAndDial(phoneNumber)
            } else {
                Toast.makeText(requireContext(), "Номер телефона не указан", Toast.LENGTH_SHORT).show()
            }
        }

        binding.exchangeButton.setOnClickListener {
            showExchangeConfirmationDialog(book)
        }
    }

    private fun observeViewModel() {
        viewModel.exchangeResult.observe(viewLifecycleOwner) { result ->
            if (result != null) {
                binding.progressBarDetail?.isVisible = false
                val canExchange = FirebaseAuth.getInstance().currentUser?.uid != null && currentBook.userId != FirebaseAuth.getInstance().currentUser?.uid && !currentBook.isRented
                binding.exchangeButton?.isEnabled = canExchange

                result.onSuccess {
                    Toast.makeText(requireContext(), "Обмен зарегистрирован!", Toast.LENGTH_SHORT).show()
                    findNavController().popBackStack()
                }
                result.onFailure { error ->
                    Log.e("BookDetailFragment", "Exchange failed", error)
                }
                viewModel.clearExchangeResult()
            }
        }

        viewModel.errorMessage.observe(viewLifecycleOwner) { error ->
            if (error != null) {
                Toast.makeText(requireContext(), error, Toast.LENGTH_LONG).show()
                viewModel.clearErrorMessage()
            }
        }
    }


    private fun showExchangeConfirmationDialog(book: Book) {
        AlertDialog.Builder(requireContext())
            .setTitle("Подтверждение обмена")
            .setMessage("Вы уверены, что хотите получить книгу \"${book.title}\"? Информация о вас (${viewModel.user.value?.nickname ?: "Вы"}) будет передана текущему владельцу.")
            .setPositiveButton("Обменять") { dialog, _ ->
                binding.progressBarDetail?.isVisible = true
                binding.exchangeButton?.isEnabled = false
                viewModel.triggerExchange(book)
                dialog.dismiss()
            }
            .setNegativeButton("Отмена", null)
            .show()
    }

    private fun checkCallPermissionAndDial(phoneNumber: String) {
        if (ContextCompat.checkSelfPermission(requireContext(), android.Manifest.permission.CALL_PHONE) == PackageManager.PERMISSION_GRANTED) {
            dialPhone(phoneNumber)
        } else {
            Toast.makeText(requireContext(), "Нет разрешения на звонки", Toast.LENGTH_SHORT).show()
        }
    }

    private fun dialPhone(phoneNumber: String) {
        try {
            val intent = Intent(Intent.ACTION_DIAL, Uri.parse("tel:$phoneNumber"))
            startActivity(intent)
        } catch (e: Exception) {
            Log.e("BookDetailFragment", "Error dialing phone", e)
            Toast.makeText(context, "Не удалось открыть набор номера.", Toast.LENGTH_SHORT).show()
        }
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
}