    package com.example.bookswapkz.fragments

    import android.os.Bundle
    import android.view.LayoutInflater
    import android.view.View
    import android.view.ViewGroup
    import androidx.fragment.app.Fragment
    import androidx.fragment.app.viewModels
    import androidx.navigation.fragment.findNavController
    import com.example.bookswapkz.R
    import com.example.bookswapkz.databinding.FragmentProfileBinding
    import com.example.bookswapkz.viewmodels.BookViewModel
    import com.google.firebase.auth.FirebaseAuth

    class ProfileFragment : Fragment() {
        private var _binding: FragmentProfileBinding? = null
        private val binding get() = _binding!!
        private val viewModel: BookViewModel by viewModels()

        override fun onCreateView(
            inflater: LayoutInflater, container: ViewGroup?,
            savedInstanceState: Bundle?
        ): View {
            _binding = FragmentProfileBinding.inflate(inflater, container, false)
            return binding.root
        }

        override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
            super.onViewCreated(view, savedInstanceState)

            viewModel.user.observe(viewLifecycleOwner) { user ->
                if (user != null) {
                    binding.nameTextView.text = user.name
                    binding.nicknameTextView.text = user.nickname
                    binding.cityTextView.text = user.city
                    binding.ageTextView.text = user.age.toString()
                    binding.phoneTextView.text = user.phone
                } else {
                    findNavController().navigate(R.id.action_profile_to_login)
                }
            }

            binding.logoutButton.setOnClickListener {
                FirebaseAuth.getInstance().signOut()
                findNavController().navigate(R.id.action_profile_to_login)
            }
        }

        override fun onDestroyView() {
            super.onDestroyView()
            _binding = null
        }
    }