package com.delilaqar.realestate.ui

import android.app.Activity
import android.content.Intent
import android.os.Bundle
import android.widget.Toast
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import androidx.activity.result.contract.ActivityResultContracts
import androidx.fragment.app.Fragment
import androidx.navigation.fragment.findNavController
import com.delilaqar.realestate.R
import com.delilaqar.realestate.databinding.FragmentLoginBinding
import com.delilaqar.realestate.util.GoogleAuthHelper
import com.delilaqar.realestate.util.navigateSafe
import com.delilaqar.realestate.util.setOnSingleClickListener
import com.google.android.gms.auth.api.signin.GoogleSignIn
import com.google.android.gms.common.api.ApiException
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.auth.GoogleAuthProvider

class LoginFragment : Fragment() {
    private var _binding: FragmentLoginBinding? = null
    private val binding get() = _binding!!
    private val auth = FirebaseAuth.getInstance()

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
        _binding = FragmentLoginBinding.inflate(inflater, container, false)
        return binding.root
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)
        binding.loginButton.setOnSingleClickListener { attemptLogin() }
        binding.goToRegisterText.setOnSingleClickListener {
            findNavController().navigateSafe(R.id.action_login_to_register)
        }
        binding.googleSignInButton.setOnSingleClickListener { startGoogleSignIn() }
        binding.forgotPasswordText.setOnSingleClickListener { showForgotPasswordDialog() }
    }

    private fun attemptLogin() {
        val email = binding.emailInput.text?.toString()?.trim().orEmpty()
        val password = binding.passwordInput.text?.toString().orEmpty()

        if (email.isEmpty() || password.isEmpty()) {
            showError("يرجى تعبئة جميع الحقول")
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
                                        findNavController().navigateSafe(R.id.action_login_to_home)
                                    }
                                }
                            } else {
                                findNavController().navigateSafe(R.id.action_login_to_home)
                            }
                        },
                        onError = { e ->
                            if (_binding != null) showError("فشل حفظ بيانات الحساب: ${e.message}")
                        }
                    )
                }
                .addOnFailureListener { e ->
                    if (_binding != null) showError("فشل تسجيل الدخول عبر Google: ${e.message}")
                }
        } catch (e: ApiException) {
            if (_binding != null) showError("فشل تسجيل الدخول عبر Google: ${e.message}")
        }
    }

    private fun showForgotPasswordDialog() {
        val prefill = binding.emailInput.text?.toString()?.trim().orEmpty()
        val input = com.google.android.material.textfield.TextInputEditText(requireContext()).apply {
            hint = "البريد الإلكتروني"
            setText(prefill)
            inputType = android.text.InputType.TYPE_TEXT_VARIATION_EMAIL_ADDRESS or android.text.InputType.TYPE_CLASS_TEXT
        }
        val density = resources.displayMetrics.density
        val padding = (20 * density).toInt()
        val container = android.widget.FrameLayout(requireContext()).apply {
            setPadding(padding, padding / 2, padding, 0)
            addView(input)
        }

        androidx.appcompat.app.AlertDialog.Builder(requireContext())
            .setTitle("استرجاع كلمة المرور")
            .setMessage("أدخل بريدك الإلكتروني وسنرسل لك رابط إعادة تعيين كلمة المرور")
            .setView(container)
            .setPositiveButton("إرسال") { _, _ ->
                val email = input.text?.toString()?.trim().orEmpty()
                if (email.isEmpty()) {
                    if (isAdded) Toast.makeText(requireContext(), "يرجى إدخال بريد إلكتروني صحيح", Toast.LENGTH_SHORT).show()
                    return@setPositiveButton
                }
                auth.sendPasswordResetEmail(email)
                    .addOnSuccessListener {
                        if (isAdded) Toast.makeText(requireContext(), "تم إرسال رابط إعادة التعيين إلى بريدك الإلكتروني", Toast.LENGTH_LONG).show()
                    }
                    .addOnFailureListener { e ->
                        if (isAdded) Toast.makeText(requireContext(), "فشل الإرسال: ${e.message}", Toast.LENGTH_LONG).show()
                    }
            }
            .setNegativeButton("إلغاء", null)
            .show()
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
