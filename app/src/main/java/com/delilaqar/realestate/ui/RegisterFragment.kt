package com.delilaqar.realestate.ui

import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import androidx.fragment.app.Fragment
import androidx.navigation.fragment.findNavController
import com.delilaqar.realestate.R
import com.delilaqar.realestate.databinding.FragmentRegisterBinding
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

        binding.registerButton.setOnClickListener { attemptRegister() }
        binding.goToLoginText.setOnClickListener {
            findNavController().popBackStack()
        }
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

        auth.createUserWithEmailAndPassword(email, password)
            .addOnSuccessListener { result ->
                val uid = result.user!!.uid
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
                        findNavController().navigate(R.id.action_register_to_home)
                    }
                    .addOnFailureListener { e ->
                        showError("تم إنشاء الحساب لكن فشل حفظ البيانات: ${e.message}")
                    }
            }
            .addOnFailureListener { e ->
                showError("فشل إنشاء الحساب: ${e.message}")
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
