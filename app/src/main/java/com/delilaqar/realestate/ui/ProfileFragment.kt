package com.delilaqar.realestate.ui

import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.Toast
import androidx.fragment.app.Fragment
import com.delilaqar.realestate.databinding.FragmentProfileBinding
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.firestore.FirebaseFirestore

class ProfileFragment : Fragment() {
    private var _binding: FragmentProfileBinding? = null
    private val binding get() = _binding!!
    private val auth = FirebaseAuth.getInstance()
    private val db = FirebaseFirestore.getInstance()

    override fun onCreateView(
        inflater: LayoutInflater, container: ViewGroup?, savedInstanceState: Bundle?
    ): View {
        _binding = FragmentProfileBinding.inflate(inflater, container, false)
        return binding.root
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)
        binding.seedButton.setOnClickListener { seedSampleData() }
    }

    private fun seedSampleData() {
        val email = "test@aqar.com"
        val password = "Test123456"

        auth.signInWithEmailAndPassword(email, password)
            .addOnSuccessListener { result -> insertSampleProperties(result.user!!.uid) }
            .addOnFailureListener {
                auth.createUserWithEmailAndPassword(email, password)
                    .addOnSuccessListener { result -> insertSampleProperties(result.user!!.uid) }
                    .addOnFailureListener { e ->
                        if (isAdded) {
                            Toast.makeText(requireContext(), "فشل تسجيل الدخول: ${e.message}", Toast.LENGTH_LONG).show()
                        }
                    }
            }
    }

    private fun insertSampleProperties(ownerId: String) {
        val samples = listOf(
            hashMapOf(
                "title" to "شقة حديثة غرفتين وصالة للإيجار في الكرادة",
                "price" to 6000,
                "listingType" to "rent",
                "propertyType" to "apartment",
                "cityId" to "baghdad",
                "district" to "الكرادة، بغداد",
                "bedrooms" to 2,
                "bathrooms" to 2,
                "area" to 120,
                "featured" to true,
                "status" to "active",
                "images" to listOf("https://picsum.photos/seed/aqar2/800/600"),
                "ownerId" to ownerId
            ),
            hashMapOf(
                "title" to "فيلا فاخرة أربع غرف مع حديقة للبيع في حي الجزائر",
                "price" to 180000,
                "listingType" to "sale",
                "propertyType" to "villa",
                "cityId" to "basra",
                "district" to "حي الجزائر، البصرة",
                "bedrooms" to 4,
                "bathrooms" to 4,
                "area" to 350,
                "featured" to false,
                "status" to "active",
                "images" to listOf("https://picsum.photos/seed/aqar3/800/600"),
                "ownerId" to ownerId
            ),
            hashMapOf(
                "title" to "شقة مفروشة بالكامل ثلاث غرف للإيجار في عنكاوا",
                "price" to 8000,
                "listingType" to "rent",
                "propertyType" to "apartment",
                "cityId" to "erbil",
                "district" to "عنكاوا، أربيل",
                "bedrooms" to 3,
                "bathrooms" to 2,
                "area" to 160,
                "featured" to false,
                "status" to "active",
                "images" to listOf("https://picsum.photos/seed/aqar4/800/600"),
                "ownerId" to ownerId
            ),
            hashMapOf(
                "title" to "قطعة أرض سكنية قريبة من حرم الإمام علي للبيع",
                "price" to 95000,
                "listingType" to "sale",
                "propertyType" to "land",
                "cityId" to "najaf",
                "district" to "قرب الحرم، النجف",
                "bedrooms" to 0,
                "bathrooms" to 0,
                "area" to 300,
                "featured" to false,
                "status" to "active",
                "images" to listOf("https://picsum.photos/seed/aqar5/800/600"),
                "ownerId" to ownerId
            ),
            hashMapOf(
                "title" to "شقة غرفتين وصالة للإيجار في حي الأندلس بالناصرية",
                "price" to 4000,
                "listingType" to "rent",
                "propertyType" to "apartment",
                "cityId" to "nasiriyah",
                "district" to "حي الأندلس، الناصرية",
                "bedrooms" to 2,
                "bathrooms" to 1,
                "area" to 100,
                "featured" to true,
                "status" to "active",
                "images" to listOf("https://picsum.photos/seed/aqar6/800/600"),
                "ownerId" to ownerId
            )
        )

        var completed = 0
        samples.forEach { data ->
            db.collection("properties").add(data)
                .addOnSuccessListener {
                    completed++
                    if (completed == samples.size && isAdded) {
                        Toast.makeText(
                            requireContext(),
                            "✅ تمت إضافة ${samples.size} عقارات تجريبية",
                            Toast.LENGTH_LONG
                        ).show()
                    }
                }
                .addOnFailureListener { e ->
                    if (isAdded) {
                        Toast.makeText(requireContext(), "فشلت إضافة عقار: ${e.message}", Toast.LENGTH_LONG).show()
                    }
                }
        }
    }

    override fun onDestroyView() {
        super.onDestroyView()
        _binding = null
    }
}
