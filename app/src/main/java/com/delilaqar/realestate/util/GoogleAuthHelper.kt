package com.delilaqar.realestate.util

import android.content.Context
import com.delilaqar.realestate.R
import com.google.android.gms.auth.api.signin.GoogleSignIn
import com.google.android.gms.auth.api.signin.GoogleSignInClient
import com.google.android.gms.auth.api.signin.GoogleSignInOptions
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
     * ينشئ وثيقة المستخدم بقاعدة البيانات إذا كانت أول مرة يسجّل دخول فيها،
     * ولا يفعل شيئاً إذا كانت موجودة مسبقاً (تسجيل دخول عادي).
     */
    fun ensureUserDocument(
        uid: String,
        name: String,
        email: String,
        onComplete: () -> Unit,
        onError: (Exception) -> Unit
    ) {
        val db = FirebaseFirestore.getInstance()
        val userRef = db.collection("users").document(uid)

        userRef.get()
            .addOnSuccessListener { doc ->
                if (doc.exists()) {
                    onComplete()
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
                        .addOnSuccessListener { onComplete() }
                        .addOnFailureListener { onError(it) }
                }
            }
            .addOnFailureListener { onError(it) }
    }
}
