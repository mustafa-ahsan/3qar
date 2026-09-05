package com.delilaqar.realestate.ui

import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import androidx.fragment.app.Fragment
import androidx.navigation.fragment.findNavController
import com.delilaqar.realestate.R
import com.delilaqar.realestate.databinding.FragmentRegisterBinding
import com.delilaqar.realestate.util.navigateSafe
import com.delilaqar.realestate.util.setOnSingleClickListener
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.firestore.FirebaseFirestore

class RegisterFragment : Fragment() {
    private var _binding: FragmentRegisterBinding? = null
    private val binding get() = _binding!!
    private val auth = FirebaseAuth.getInstance()
    private val db = FirebaseFirestore.getInstance()

    override fun onCreateView(
        inflater: LayoutInflater, container: ViewGroup?, savedInstanceState: Bundle?
    ): View {
        _binding = FragmentRegisterBinding.inflate(inflater, container, false)
        return binding.root
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)
        binding.registerButton.setOnSingleClickListener { attemptRegister() }
        binding.goToLoginText.setOnSingleClickListener { findNavController().popBackStack() }
    }

    private fun attemptRegister() {
        val name = binding.nameInput.text?.toString()?.trim().orEmpty()
        val phone = binding.phoneInput.text?.toString()?.trim().orEmpty()
        val email = binding.emailInput.text?.toString()?.trim().orEmpty()
        val password = binding.passwordInput.text?.toString().orEmpty()

        if (name.isEmpty() || email.isEmpty() || password.isEmpty()) {
            showError("الرجاء تعبئة الحقول المطلوبة")
            return
        }
        if (password.length < 6) {
            showError("كلمة المرور يجب أن تكون 6 أحرف على الأقل")
            return
        }

        binding.registerButton.isEnabled = false

        auth.createUserWithEmailAndPassword(email, password)
            .addOnSuccessListener { result ->
                val uid = result.user?.uid
                if (uid == null) {
                    if (_binding != null) {
                        showError("حدث خطأ غير متوقع، حاول مرة أخرى")
                        binding.registerButton.isEnabled = true
                    }
                    return@addOnSuccessListener
                }

                val userData = hashMapOf(
                    "uid" to uid,
                    "name" to name,
                    "email" to email,
                    "phone" to phone,
                    "isVerified" to false,
                    "accountType" to "individual"
                )
                db.collection("users").document(uid).set(userData)
                    .addOnSuccessListener {
                        if (isAdded && _binding != null) {
                            findNavController().navigateSafe(R.id.action_register_to_home)
                        }
                    }
                    .addOnFailureListener { e ->
                        if (_binding != null) {
                            showError("تم إنشاء الحساب لكن فشل حفظ البيانات: ${e.message}")
                            binding.registerButton.isEnabled = true
                        }
                    }
            }
            .addOnFailureListener { e ->
                if (_binding != null) {
                    showError("فشل إنشاء الحساب: ${e.message}")
                    binding.registerButton.isEnabled = true
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
