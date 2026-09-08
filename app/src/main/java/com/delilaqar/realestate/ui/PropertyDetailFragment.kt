package com.delilaqar.realestate.ui

import android.content.Intent
import android.graphics.Color
import android.net.Uri
import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.Toast
import androidx.fragment.app.Fragment
import com.bumptech.glide.Glide
import com.delilaqar.realestate.data.Property
import com.delilaqar.realestate.databinding.FragmentPropertyDetailBinding
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.firestore.FieldValue
import com.google.firebase.firestore.FirebaseFirestore
import com.google.firebase.firestore.SetOptions
import java.util.Locale

class PropertyDetailFragment : Fragment() {
    private var _binding: FragmentPropertyDetailBinding? = null
    private val binding get() = _binding!!
    private val db = FirebaseFirestore.getInstance()

    override fun onCreateView(
        inflater: LayoutInflater, container: ViewGroup?, savedInstanceState: Bundle?
    ): View {
        _binding = FragmentPropertyDetailBinding.inflate(inflater, container, false)
        return binding.root
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)
        val propertyId = arguments?.getString("propertyId") ?: return
        loadProperty(propertyId)
    }

    private fun loadProperty(propertyId: String) {
        db.collection("properties").document(propertyId).get()
            .addOnSuccessListener { doc ->
                if (_binding == null) return@addOnSuccessListener
                val property = doc.toObject(Property::class.java) ?: return@addOnSuccessListener
                bindProperty(propertyId, property)
            }
            .addOnFailureListener {
                if (isAdded) Toast.makeText(requireContext(), "فشل تحميل العقار: ${it.message}", Toast.LENGTH_LONG).show()
            }
    }

    private fun bindProperty(propertyId: String, property: Property) {
        binding.detailTitle.text = property.title
        binding.detailPrice.text = "$${String.format(Locale.US, "%,.0f", property.price)}"
        binding.detailLocation.text = property.district
        binding.detailBedrooms.text = "🛏 ${property.bedrooms}\nغرف نوم"
        binding.detailBathrooms.text = "🚿 ${property.bathrooms}\nحمامات"
        binding.detailArea.text = "📐 ${property.area.toInt()}\nم²"
        binding.detailDescription.text = property.description.ifEmpty { "لا يوجد وصف" }

        binding.detailPropertyTypeBadge.text = propertyTypeLabel(property.propertyType)
        binding.detailListingTypeBadge.text = if (property.listingType == "rent") "للإيجار" else "للبيع"

        val imageUrl = property.images.firstOrNull()
        if (!imageUrl.isNullOrEmpty()) {
            Glide.with(this)
                .load(imageUrl)
                .placeholder(android.R.color.darker_gray)
                .error(android.R.drawable.ic_dialog_alert)
                .centerCrop()
                .into(binding.detailImage)
        } else {
            binding.detailImage.setBackgroundColor(Color.DKGRAY)
        }

        // --- كود زر الواتساب ---
        binding.whatsappDetailButton.setOnClickListener {
            // جلب رقم الهاتف من العقار (وإذا كان فارغاً نضع رقم افتراضي)
            val phone = property.phoneNumber.ifEmpty { "+9647000000000" } 
            
            // رسالة جاهزة للبائع
            val message = "مرحباً، أنا مهتم بعقارك (${property.title}) المعروض في تطبيق عقار."
            
            try {
                // فتح تطبيق واتساب
                val uri = Uri.parse("https://api.whatsapp.com/send?phone=$phone&text=${Uri.encode(message)}")
                val intent = Intent(Intent.ACTION_VIEW, uri)
                startActivity(intent)
            } catch (e: Exception) {
                if (isAdded) Toast.makeText(requireContext(), "تطبيق واتساب غير مثبت على جهازك", Toast.LENGTH_SHORT).show()
            }
        }

        // --- كود زر الإبلاغ الذكي ---
        binding.reportButton.setOnClickListener {
            val uid = FirebaseAuth.getInstance().currentUser?.uid
            if (uid == null) {
                Toast.makeText(requireContext(), "يجب تسجيل الدخول أولاً للإبلاغ", Toast.LENGTH_SHORT).show()
                return@setOnClickListener
            }

            // إيقاف الزر لتجنب الضغط المتكرر
            binding.reportButton.isEnabled = false
            binding.reportButton.text = "جاري الإبلاغ..."

            val docRef = db.collection("properties").document(propertyId)

            // قراءة البيانات من السيرفر مباشرة
            docRef.get().addOnSuccessListener { document ->
                val currentReports = document.get("reportedBy") as? List<*>
                val reportsCount = currentReports?.size ?: 0

                val updates = hashMapOf<String, Any>(
                    "reportedBy" to FieldValue.arrayUnion(uid)
                )

                // الإخفاء إذا وصل الحد
                if (reportsCount >= 3) {
                    updates["status"] = "hidden"
                }

                // إرسال التحديث
                docRef.set(updates, SetOptions.merge())
                    .addOnSuccessListener {
                        if (isAdded) Toast.makeText(requireContext(), "🚩 تم استلام البلاغ.", Toast.LENGTH_LONG).show()
                        binding.reportButton.text = "تم الإبلاغ"
                    }
                    .addOnFailureListener { e ->
                        binding.reportButton.isEnabled = true
                        binding.reportButton.text = "🚩 إبلاغ عن محتوى مسيء"
                        if (isAdded) Toast.makeText(requireContext(), "حدث خطأ: ${e.localizedMessage}", Toast.LENGTH_LONG).show()
                    }
            }
        }
    }

    private fun propertyTypeLabel(type: String): String = when (type) {
        "apartment" -> "شقة"
        "villa" -> "فيلا"
        "land" -> "أرض"
        "commercial" -> "تجاري ومكاتب"
        "duplex" -> "دوبلكس"
        "chalet" -> "شاليه"
        "full_building" -> "عمارة كاملة"
        else -> type
    }

    override fun onDestroyView() {
        super.onDestroyView()
        _binding = null
    }
}