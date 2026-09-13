package com.delilaqar.realestate.ui

import android.app.Activity
import android.content.Intent
import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import androidx.activity.result.contract.ActivityResultContracts
import androidx.fragment.app.Fragment
import androidx.navigation.fragment.findNavController
import com.delilaqar.realestate.R
import com.delilaqar.realestate.databinding.FragmentRegisterBinding
import com.delilaqar.realestate.util.GoogleAuthHelper
import com.delilaqar.realestate.util.navigateSafe
import com.delilaqar.realestate.util.setOnSingleClickListener
import com.google.android.gms.auth.api.signin.GoogleSignIn
import com.google.android.gms.common.api.ApiException
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.auth.GoogleAuthProvider
import com.google.firebase.firestore.FirebaseFirestore

class RegisterFragment : Fragment() {
    private var _binding: FragmentRegisterBinding? = null
    private val binding get() = _binding!!
    private val auth = FirebaseAuth.getInstance()
    private val db = FirebaseFirestore.getInstance()

    private val googleSignInLauncher = registerForActivityResult(
        ActivityResultContracts.StartActivityForResult()
    ) { result ->
        if (result.resultCode == Activity.RESULT_OK) {
            handleGoogleSignInResult(result.data)
        }
    }

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
        binding.googleSignUpButton.setOnSingleClickListener { startGoogleSignIn() }
    }

    private fun attemptRegister() {
        val name = binding.nameInput.text?.toString()?.trim().orEmpty()
        val phone = binding.phoneInput.text?.toString()?.trim().orEmpty()
        val email = binding.emailInput.text?.toString()?.trim().orEmpty()
        val password = binding.passwordInput.text?.toString().orEmpty()

        if (name.isEmpty() || email.isEmpty() || password.isEmpty()) {
            showError("يرجى تعبئة الحقول المطلوبة")
            return
        }
        if (password.length < 6) {
            showError("يجب أن تتكون كلمة المرور من 6 أحرف على الأقل")
            return
        }

        binding.registerButton.isEnabled = false

        auth.createUserWithEmailAndPassword(email, password)
            .addOnSuccessListener { result ->
                val uid = result.user?.uid
                if (uid == null) {
                    if (_binding != null) {
                        showError("حدث خطأ غير متوقع، يرجى المحاولة مرة أخرى")
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
                            showError("تم إنشاء الحساب، لكن فشل حفظ البيانات: ${e.message}")
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

    private fun startGoogleSignIn() {
        val signInClient = GoogleAuthHelper.getSignInClient(requireContext())
        googleSignInLauncher.launch(signInClient.signInIntent)
    }

    private fun handleGoogleSignInResult(data: Intent?) {
        try {
            val task = GoogleSignIn.getSignedInAccountFromIntent(data)
            val account = task.getResult(ApiException::class.java)
            val idToken = account.idToken

            if (idToken == null) {
                showError("تعذّر الحصول على بيانات حساب Google")
                return
            }

            val credential = GoogleAuthProvider.getCredential(idToken, null)
            auth.signInWithCredential(credential)
                .addOnSuccessListener { result ->
                    val user = result.user
                    if (user == null || _binding == null) return@addOnSuccessListener

                    GoogleAuthHelper.ensureUserDocument(
                        uid = user.uid,
                        name = user.displayName ?: "مستخدم",
                        email = user.email ?: "",
                        onComplete = { isNewUser ->
                            if (!isAdded || _binding == null) return@ensureUserDocument
                            if (isNewUser) {
                                GoogleAuthHelper.promptForPhoneNumber(requireContext(), user.uid) {
                                    if (isAdded && _binding != null) {
                                        findNavController().navigateSafe(R.id.action_register_to_home)
                                    }
                                }
                            } else {
                                findNavController().navigateSafe(R.id.action_register_to_home)
                            }
                        },
                        onError = { e ->
                            if (_binding != null) showError("فشل حفظ بيانات الحساب: ${e.message}")
                        }
                    )
                }
                .addOnFailureListener { e ->
                    if (_binding != null) showError("فشل التسجيل عبر Google: ${e.message}")
                }
        } catch (e: ApiException) {
            if (_binding != null) showError("فشل التسجيل عبر Google: ${e.message}")
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
