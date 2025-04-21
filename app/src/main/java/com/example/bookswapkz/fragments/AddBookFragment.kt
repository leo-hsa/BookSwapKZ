package com.example.bookswapkz.fragments

import android.app.Activity
import android.content.Intent
import android.net.Uri
import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.ArrayAdapter
import android.widget.Toast
import androidx.activity.result.contract.ActivityResultContracts
import androidx.fragment.app.Fragment
import androidx.fragment.app.viewModels
import androidx.navigation.fragment.findNavController
import com.example.bookswapkz.R
import com.example.bookswapkz.databinding.FragmentAddBookBinding
import com.example.bookswapkz.models.Book
import com.example.bookswapkz.viewmodels.BookViewModel

class AddBookFragment : Fragment() {
    private var _binding: FragmentAddBookBinding? = null
    private val binding get() = _binding!!
    private val viewModel: BookViewModel by viewModels()
    private var imageUri: Uri? = null

    private val pickImageLauncher = registerForActivityResult(ActivityResultContracts.StartActivityForResult()) { result ->
        if (result.resultCode == Activity.RESULT_OK) {
            result.data?.data?.let { uri ->
                imageUri = uri
                binding.bookImage.setImageURI(uri)
                binding.bookImage.visibility = View.VISIBLE
            }
        }
    }

    override fun onCreateView(
        inflater: LayoutInflater, container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View {
        _binding = FragmentAddBookBinding.inflate(inflater, container, false)
        return binding.root
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)

        val conditions = arrayOf("Новое", "Хорошее", "Среднее")
        val cities = arrayOf("Алматы", "Астана", "Шымкент")
        binding.conditionSpinner.adapter = ArrayAdapter(requireContext(), android.R.layout.simple_spinner_item, conditions)
        binding.citySpinner.adapter = ArrayAdapter(requireContext(), android.R.layout.simple_spinner_item, cities)

        binding.selectImageButton.setOnClickListener {
            val intent = Intent(Intent.ACTION_PICK)
            intent.type = "image/*"
            pickImageLauncher.launch(intent)
        }

        binding.addBookButton.setOnClickListener {
            val title = binding.titleEditText.text.toString().trim()
            val author = binding.authorEditText.text.toString().trim()
            val condition = binding.conditionSpinner.selectedItem.toString()
            val city = binding.citySpinner.selectedItem.toString()

            if (title.isEmpty() || author.isEmpty()) {
                Toast.makeText(context, "Заполните название и автора", Toast.LENGTH_SHORT).show()
                return@setOnClickListener
            }

            val book = Book(
                id = "",
                title = title,
                author = author,
                condition = condition,
                city = city,
                userId = "",
                imageUrl = null,
                phone = viewModel.user.value?.phone
            )

            binding.progressBar.visibility = View.VISIBLE
            binding.addBookButton.isEnabled = false

            viewModel.addBook(book, imageUri).observe(viewLifecycleOwner) { success ->
                binding.progressBar.visibility = View.GONE
                binding.addBookButton.isEnabled = true
                if (success) {
                    Toast.makeText(context, "Книга добавлена", Toast.LENGTH_SHORT).show()
                    findNavController().navigate(R.id.action_addBook_to_home)
                } else {
                    Toast.makeText(context, "Ошибка добавления", Toast.LENGTH_SHORT).show()
                }
            }
        }
    }

    override fun onDestroyView() {
        super.onDestroyView()
        _binding = null
    }
}