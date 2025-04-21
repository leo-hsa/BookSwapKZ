package com.example.bookswapkz.fragments

import android.os.Bundle
import android.util.Log
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import androidx.fragment.app.Fragment
import androidx.fragment.app.viewModels
import androidx.navigation.fragment.findNavController
import com.example.bookswapkz.R
import com.example.bookswapkz.databinding.FragmentProfileBinding
import com.example.bookswapkz.viewmodels.BookViewModel // Убедитесь, что импорт ViewModel правильный
import com.google.firebase.auth.FirebaseAuth

class ProfileFragment : Fragment() {
    private var _binding: FragmentProfileBinding? = null
    private val binding get() = _binding!!
    // Привязка ViewModel к Activity для общего доступа к состоянию пользователя
    private val viewModel: BookViewModel by viewModels({ requireActivity() })

    override fun onCreateView(
        inflater: LayoutInflater, container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View {
        _binding = FragmentProfileBinding.inflate(inflater, container, false)
        return binding.root
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)

        Log.d("ProfileFragment", "onViewCreated called")

        // Наблюдаем за данными пользователя из ViewModel
        viewModel.user.observe(viewLifecycleOwner) { user ->
            if (user != null) {
                Log.d("ProfileFragment", "User data received: ${user.nickname}")
                binding.nameTextView.text = user.name.ifEmpty { "-" } // Показываем прочерк, если пусто
                binding.nicknameTextView.text = user.nickname.ifEmpty { "-" }
                binding.cityTextView.text = user.city.ifEmpty { "-" }
                binding.streetTextView.text = user.street.ifEmpty { "-" } // <-- Отображаем улицу
                binding.houseNumberTextView.text = user.houseNumber.ifEmpty { "-" } // <-- Отображаем номер дома
                binding.ageTextView.text = if (user.age > 0) user.age.toString() else "-" // Показываем возраст или прочерк
                binding.phoneTextView.text = user.phone.ifEmpty { "-" }
            } else {
                // Если пользователь null (не вошел или вышел), переходим на экран логина
                Log.d("ProfileFragment", "User is null, navigating to login")
                // Проверяем, что мы еще не на экране логина, чтобы избежать цикла
                if (findNavController().currentDestination?.id != R.id.loginFragment) {
                    // Ищем глобальное действие для перехода на логин (создайте его в nav_graph)
                    findNavController().navigate(R.id.action_global_loginFragment)
                }
            }
        }

        // Кнопка выхода
        binding.logoutButton.setOnClickListener {
            Log.d("ProfileFragment", "Logout button clicked")
            FirebaseAuth.getInstance().signOut()
            // После выхода пользователь в viewModel.user станет null,
            // и observe выше автоматически перенаправит на логин.
            // Дополнительная навигация здесь может быть избыточной,
            // но можно оставить для немедленного перехода.
            if (findNavController().currentDestination?.id != R.id.loginFragment) {
                findNavController().navigate(R.id.action_global_loginFragment)
            }
        }

        // Опционально: если мы зашли в профиль, а данных еще нет, запросим их
        if (viewModel.user.value == null && FirebaseAuth.getInstance().currentUser != null) {
            Log.d("ProfileFragment", "User data is null but user is logged in, calling viewModel.getUser()")
            viewModel.getUser()
        }
    }

    override fun onDestroyView() {
        super.onDestroyView()
        Log.d("ProfileFragment", "onDestroyView called")
        _binding = null // Важно для предотвращения утечек памяти
    }
}