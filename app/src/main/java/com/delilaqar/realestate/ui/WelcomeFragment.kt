package com.delilaqar.realestate.ui

import android.app.Activity
import android.content.Intent
import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.Toast
import androidx.activity.result.contract.ActivityResultContracts
import androidx.fragment.app.Fragment
import androidx.navigation.fragment.findNavController
import com.delilaqar.realestate.R
import com.delilaqar.realestate.databinding.FragmentWelcomeBinding
import com.delilaqar.realestate.util.GoogleAuthHelper
import com.delilaqar.realestate.util.navigateSafe
import com.delilaqar.realestate.util.setOnSingleClickListener
import com.google.android.gms.auth.api.signin.GoogleSignIn
import com.google.android.gms.common.api.ApiException
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.auth.GoogleAuthProvider

class WelcomeFragment : Fragment() {
    private var _binding: FragmentWelcomeBinding? = null
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
        _binding = FragmentWelcomeBinding.inflate(inflater, container, false)
        return binding.root
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)

        binding.createAccountButton.setOnSingleClickListener {
            findNavController().navigateSafe(R.id.action_welcome_to_register)
        }
        binding.goToLoginText.setOnSingleClickListener {
            findNavController().navigateSafe(R.id.action_welcome_to_login)
        }
        binding.guestBrowseButton.setOnSingleClickListener {
            findNavController().navigateSafe(R.id.action_welcome_to_home)
        }
        binding.googleSignInButton.setOnSingleClickListener { startGoogleSignIn() }
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
                                        findNavController().navigateSafe(R.id.action_welcome_to_home)
                                    }
                                }
                            } else {
                                findNavController().navigateSafe(R.id.action_welcome_to_home)
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

    private fun showError(message: String) {
        if (isAdded) Toast.makeText(requireContext(), message, Toast.LENGTH_LONG).show()
    }

    override fun onDestroyView() {
        super.onDestroyView()
        _binding = null
    }
}
