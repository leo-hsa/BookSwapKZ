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

import androidx.navigation.fragment.findNavController
import androidx.navigation.fragment.navArgs

import com.example.bookswapkz.R
import com.example.bookswapkz.databinding.FragmentBookDetailBinding
import com.example.bookswapkz.models.Book
import com.example.bookswapkz.viewmodels.BookViewModel
import com.google.firebase.auth.FirebaseAuth

class BookDetailFragment : Fragment() {

    private var _binding: FragmentBookDetailBinding? = null
    private val binding get() = _binding!!
    private val args: BookDetailFragmentArgs by navArgs()
    private val viewModel: BookViewModel by viewModels({ requireActivity() })

    private val CALL_PHONE_REQUEST_CODE = 101

    override fun onCreateView(
        inflater: LayoutInflater, container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View {
        _binding = FragmentBookDetailBinding.inflate(inflater, container, false)
        return binding.root
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)
        displayBookDetails(args.book)

        binding.callButton.setOnClickListener {
            val phoneNumber = args.book.phone
            if (!phoneNumber.isNullOrBlank()) {
                checkCallPermissionAndDial(phoneNumber)
            } else {
                Toast.makeText(context, "Номер телефона не указан", Toast.LENGTH_SHORT).show()
            }
        }

        binding.exchangeButton.setOnClickListener {
            val bookToExchange = args.book
            showExchangeConfirmationDialog(bookToExchange)
        }

        viewModel.exchangeStatus.observe(viewLifecycleOwner) { success ->
            if (success != null) {
                binding.progressBarDetail.isVisible = false
                binding.exchangeButton.isEnabled = true
                if (success) {
                    Toast.makeText(context, "Обмен зарегистрирован!", Toast.LENGTH_SHORT).show()
                    findNavController().popBackStack()
                } else {
                    Toast.makeText(context, viewModel.errorMessage.value ?: "Ошибка обмена", Toast.LENGTH_LONG).show()
                }
                viewModel.clearExchangeStatus()
                viewModel.clearErrorMessage()
            }
        }
    }

    private fun displayBookDetails(book: Book) {
        binding.titleDetailTextView.text = book.title
        binding.authorDetailTextView.text = book.author
        binding.conditionDetailTextView.text = book.condition
        binding.cityDetailTextView.text = book.city
        binding.phoneDetailTextView.text = book.phone ?: "Не указан"
        binding.phoneDetailTextView.isVisible = !book.phone.isNullOrBlank()
        binding.callButton.isVisible = !book.phone.isNullOrBlank()
        binding.ownerCountDetailTextView.text = getString(R.string.owner_count_format, book.ownerCount)


        binding.bookImageDetail.setImageResource(R.drawable.ic_book_placeholder)
        binding.bookImageDetail.isVisible = true


        val currentUserId = FirebaseAuth.getInstance().currentUser?.uid
        binding.exchangeButton.isVisible = currentUserId != null && book.userId != currentUserId
    }

    private fun showExchangeConfirmationDialog(book: Book) {
        AlertDialog.Builder(requireContext())
            .setTitle("Подтверждение обмена")
            .setMessage("Вы уверены, что хотите получить книгу \"${book.title}\"? Информация о вас будет передана текущему владельцу.")
            .setPositiveButton("Обменять") { dialog, _ ->
                binding.progressBarDetail.isVisible = true
                binding.exchangeButton.isEnabled = false
                viewModel.performExchange(book)
                dialog.dismiss()
            }
            .setNegativeButton("Отмена") { dialog, _ ->
                dialog.dismiss()
            }
            .show()
    }

    private fun checkCallPermissionAndDial(phoneNumber: String) {
        if (ContextCompat.checkSelfPermission(requireContext(), android.Manifest.permission.CALL_PHONE) == PackageManager.PERMISSION_GRANTED) {
            dialPhone(phoneNumber)
        } else {
            ActivityCompat.requestPermissions(requireActivity(), arrayOf(android.Manifest.permission.CALL_PHONE), CALL_PHONE_REQUEST_CODE)
        }
    }

    private fun dialPhone(phoneNumber: String) {
        try {
            val intent = Intent(Intent.ACTION_CALL, Uri.parse("tel:$phoneNumber"))
            startActivity(intent)
        } catch (e: SecurityException) {
            Log.e("BookDetailFragment", "SecurityException on call", e)
            Toast.makeText(context, "Не удалось совершить звонок. Проверьте разрешения.", Toast.LENGTH_SHORT).show()
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

        if (_binding != null) {
            displayBookDetails(newBook)
        }
    }
}