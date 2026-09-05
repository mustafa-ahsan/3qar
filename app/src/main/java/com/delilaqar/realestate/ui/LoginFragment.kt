package com.delilaqar.realestate.ui

import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import androidx.fragment.app.Fragment
import androidx.navigation.fragment.findNavController
import com.delilaqar.realestate.R
import com.delilaqar.realestate.databinding.FragmentLoginBinding
import com.delilaqar.realestate.util.navigateSafe
import com.delilaqar.realestate.util.setOnSingleClickListener
import com.google.firebase.auth.FirebaseAuth

class LoginFragment : Fragment() {
    private var _binding: FragmentLoginBinding? = null
    private val binding get() = _binding!!
    private val auth = FirebaseAuth.getInstance()

    override fun onCreateView(
        inflater: LayoutInflater, container: ViewGroup?, savedInstanceState: Bundle?
    ): View {
        _binding = FragmentLoginBinding.inflate(inflater, container, false)
        return binding.root
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)
        binding.loginButton.setOnSingleClickListener { attemptLogin() }
        binding.goToRegisterText.setOnSingleClickListener {
            findNavController().navigateSafe(R.id.action_login_to_register)
        }
    }

    private fun attemptLogin() {
        val email = binding.emailInput.text?.toString()?.trim().orEmpty()
        val password = binding.passwordInput.text?.toString().orEmpty()

        if (email.isEmpty() || password.isEmpty()) {
            showError("الرجاء تعبئة جميع الحقول")
            return
        }

        binding.loginButton.isEnabled = false

        auth.signInWithEmailAndPassword(email, password)
            .addOnSuccessListener {
                if (isAdded && _binding != null) {
                    findNavController().navigateSafe(R.id.action_login_to_home)
                }
            }
            .addOnFailureListener { e ->
                if (_binding != null) {
                    showError("فشل تسجيل الدخول: ${e.message}")
                    binding.loginButton.isEnabled = true
                }
            }
    }

    private fun showError(message: String) {
        binding.errorText.text = message
        binding.errorText.visibility = View.VISIBLE
    }

    override fun onDestroyView() {
        super.onDestroyView()
        _binding = null
    }
}
