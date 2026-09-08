package com.delilaqar.realestate.ui

import android.graphics.Color
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
                // تم التعديل هنا لتمرير الـ propertyId
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

        binding.whatsappDetailButton.setOnClickListener {
            if (isAdded) Toast.makeText(requireContext(), "سيتوفر التواصل قريباً", Toast.LENGTH_SHORT).show()
        }

        // كود زر الإبلاغ الجديد
        // كود زر الإبلاغ مع ميزة الإخفاء التلقائي
        // كود زر الإبلاغ مع الاتصال المباشر بالسيرفر للتحقق من العدد الفعلي
        binding.reportButton.setOnClickListener {
            val uid = FirebaseAuth.getInstance().currentUser?.uid
            if (uid == null) {
                Toast.makeText(requireContext(), "يجب تسجيل الدخول أولاً للإبلاغ", Toast.LENGTH_SHORT).show()
                return@setOnClickListener
            }

            // نوقف الزر فوراً حتى لا يضغط عليه المستخدم مرتين بسرعة
            binding.reportButton.isEnabled = false
            binding.reportButton.text = "جاري الإبلاغ..."

            val docRef = db.collection("properties").document(propertyId)

            // 1. نجلب البيانات الطازجة من السيرفر مباشرة لحظة الضغط
            docRef.get().addOnSuccessListener { document ->
                // نقرأ مصفوفة البلاغات الحالية مباشرة من قاعدة البيانات
                val currentReports = document.get("reportedBy") as? List<*>
                val reportsCount = currentReports?.size ?: 0

                val updates = hashMapOf<String, Any>(
                    "reportedBy" to com.google.firebase.firestore.FieldValue.arrayUnion(uid)
                )

                // 2. إذا كان عدد البلاغات الفعلي في السيرفر 3 أو أكثر (يعني بلاغنا هذا هو الرابع أو أكثر)
                if (reportsCount >= 3) {
                    updates["status"] = "hidden"
                }

                // 3. نرسل التحديث
                docRef.set(updates, com.google.firebase.firestore.SetOptions.merge())
                    .addOnSuccessListener {
                        if (isAdded) Toast.makeText(requireContext(), "🚩 تم استلام البلاغ.", Toast.LENGTH_LONG).show()
                        binding.reportButton.text = "تم الإبلاغ"
                    }
                    .addOnFailureListener { e ->
                        // في حال فشل الإرسال، نعيد تفعيل الزر
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