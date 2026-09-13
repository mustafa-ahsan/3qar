package com.delilaqar.realestate.util

import android.content.Context
import android.text.InputType
import android.widget.FrameLayout
import androidx.appcompat.app.AlertDialog
import com.delilaqar.realestate.R
import com.google.android.gms.auth.api.signin.GoogleSignIn
import com.google.android.gms.auth.api.signin.GoogleSignInClient
import com.google.android.gms.auth.api.signin.GoogleSignInOptions
import com.google.android.material.textfield.TextInputEditText
import com.google.firebase.firestore.FirebaseFirestore

object GoogleAuthHelper {

    fun getSignInClient(context: Context): GoogleSignInClient {
        val options = GoogleSignInOptions.Builder(GoogleSignInOptions.DEFAULT_SIGN_IN)
            .requestIdToken(context.getString(R.string.default_web_client_id))
            .requestEmail()
            .build()
        return GoogleSignIn.getClient(context, options)
    }

    /**
     * ينشئ وثيقة المستخدم إذا كانت أول مرة يسجّل دخول فيها (isNewUser = true)،
     * أو يمرّ مباشرة إذا كانت موجودة مسبقاً (isNewUser = false).
     */
    fun ensureUserDocument(
        uid: String,
        name: String,
        email: String,
        onComplete: (isNewUser: Boolean) -> Unit,
        onError: (Exception) -> Unit
    ) {
        val db = FirebaseFirestore.getInstance()
        val userRef = db.collection("users").document(uid)

        userRef.get()
            .addOnSuccessListener { doc ->
                if (doc.exists()) {
                    onComplete(false)
                } else {
                    val userData = hashMapOf(
                        "uid" to uid,
                        "name" to name,
                        "email" to email,
                        "phone" to "",
                        "isVerified" to false,
                        "accountType" to "individual"
                    )
                    userRef.set(userData)
                        .addOnSuccessListener { onComplete(true) }
                        .addOnFailureListener { onError(it) }
                }
            }
            .addOnFailureListener { onError(it) }
    }

    /**
     * يطلب من المستخدم رقم هاتفه (لاستخدامه بزر واتساب بإعلاناته لاحقاً) ويحفظه.
     * يظهر خيار "لاحقاً" لعدم إجبار المستخدم، بس التطبيق يبقى يذكّره من شاشة حسابي.
     */
    fun promptForPhoneNumber(context: Context, uid: String, onDone: () -> Unit) {
        val input = TextInputEditText(context).apply {
            hint = "رقم الهاتف"
            inputType = InputType.TYPE_CLASS_PHONE
        }
        val density = context.resources.displayMetrics.density
        val padding = (20 * density).toInt()
        val container = FrameLayout(context).apply {
            setPadding(padding, padding / 2, padding, 0)
            addView(input)
        }

        AlertDialog.Builder(context)
            .setTitle("رقم الهاتف")
            .setMessage("يرجى إدخال رقم هاتفك ليتمكن المهتمون من التواصل معك عبر واتساب عند نشر إعلاناتك")
            .setView(container)
            .setCancelable(false)
            .setPositiveButton("حفظ") { _, _ ->
                val phone = input.text?.toString()?.trim().orEmpty()
                if (phone.isNotEmpty()) {
                    FirebaseFirestore.getInstance().collection("users").document(uid)
                        .update("phone", phone)
                }
                onDone()
            }
            .setNegativeButton("لاحقاً") { _, _ -> onDone() }
            .show()
    }
}
