package com.delilaqar.realestate

import android.os.Bundle
import androidx.appcompat.app.AppCompatActivity
import com.delilaqar.realestate.databinding.ActivityMainBinding
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.firestore.FirebaseFirestore

class MainActivity : AppCompatActivity() {
    private lateinit var binding: ActivityMainBinding
    private val auth = FirebaseAuth.getInstance()
    private val db = FirebaseFirestore.getInstance()

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        binding = ActivityMainBinding.inflate(layoutInflater)
        setContentView(binding.root)
        binding.welcomeText.text = getString(R.string.welcome_message)
        binding.testButton.setOnClickListener { testFirebaseConnection() }
    }

    private fun testFirebaseConnection() {
        binding.welcomeText.text = "جاري الاختبار..."
        val email = "test@aqar.com"
        val password = "Test123456"

        auth.signInWithEmailAndPassword(email, password)
            .addOnSuccessListener { result -> writeTestData(result.user!!.uid) }
            .addOnFailureListener {
                auth.createUserWithEmailAndPassword(email, password)
                    .addOnSuccessListener { result -> writeTestData(result.user!!.uid) }
                    .addOnFailureListener { e -> binding.welcomeText.text = "فشل تسجيل الدخول: ${e.message}" }
            }
    }

    private fun writeTestData(uid: String) {
        val data = hashMapOf("uid" to uid, "name" to "Test User", "checkedAt" to System.currentTimeMillis())
        db.collection("users").document(uid).set(data)
            .addOnSuccessListener {
                db.collection("users").document(uid).get()
                    .addOnSuccessListener { doc ->
                        binding.welcomeText.text = if (doc.exists())
                            "✅ الاتصال ناجح! Auth + Firestore يشتغلون صح"
                        else "⚠️ الكتابة نجحت بس القراءة فشلت"
                    }
            }
            .addOnFailureListener { e -> binding.welcomeText.text = "❌ فشلت الكتابة: ${e.message}" }
    }
}