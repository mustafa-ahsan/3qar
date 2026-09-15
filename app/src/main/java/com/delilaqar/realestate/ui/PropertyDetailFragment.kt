package com.delilaqar.realestate.ui

import android.content.Intent
import android.net.Uri
import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.ImageView
import android.widget.Toast
import androidx.fragment.app.Fragment
import androidx.viewpager2.widget.ViewPager2
import com.delilaqar.realestate.data.Property
import com.delilaqar.realestate.databinding.FragmentPropertyDetailBinding
import com.delilaqar.realestate.util.CurrencyFormatter
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.firestore.FieldValue
import com.google.firebase.firestore.FirebaseFirestore
import com.google.firebase.firestore.SetOptions

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
        binding.detailPrice.text = CurrencyFormatter.format(property.price)
        binding.detailLocation.text = property.district
        binding.detailBedrooms.text = "🛏 ${property.bedrooms}\nغرف نوم"
        binding.detailBathrooms.text = "🚿 ${property.bathrooms}\nحمامات"
        binding.detailArea.text = "📐 ${property.area.toInt()}\nم²"
        binding.detailDescription.text = property.description.ifEmpty { "لا يوجد وصف" }

        binding.detailPropertyTypeBadge.text = propertyTypeLabel(property.propertyType)
        binding.detailListingTypeBadge.text = if (property.listingType == "rent") "للإيجار" else "للبيع"

        setupImageGallery(property.images)

        binding.shareButton.setOnClickListener {
            val shareText = "${property.title}\n" +
                "السعر: ${CurrencyFormatter.format(property.price)}\n" +
                "الموقع: ${property.district}\n\n" +
                "شاهد هذا العقار على تطبيق دليلك للعقار"
            val sendIntent = Intent(Intent.ACTION_SEND).apply {
                type = "text/plain"
                putExtra(Intent.EXTRA_TEXT, shareText)
            }
            startActivity(Intent.createChooser(sendIntent, "مشاركة الإعلان"))
        }

        binding.whatsappDetailButton.setOnClickListener {
            var phone = property.phoneNumber.trim().ifEmpty { "+9647000000000" }
            if (phone.startsWith("07")) {
                phone = "+964" + phone.substring(1)
            } else if (phone.startsWith("00964")) {
                phone = "+964" + phone.substring(5)
            } else if (!phone.startsWith("+")) {
                phone = "+964$phone"
            }

            val message = "مرحباً، أنا مهتم بعقارك (${property.title}) المعروض في تطبيق عقار."

            try {
                val uri = Uri.parse("https://api.whatsapp.com/send?phone=$phone&text=${Uri.encode(message)}")
                val intent = Intent(Intent.ACTION_VIEW, uri)
                startActivity(intent)
            } catch (e: Exception) {
                if (isAdded) Toast.makeText(requireContext(), "تطبيق واتساب غير مثبت على جهازك", Toast.LENGTH_SHORT).show()
            }
        }

        binding.reportButton.setOnClickListener {
            val uid = FirebaseAuth.getInstance().currentUser?.uid
            if (uid == null) {
                Toast.makeText(requireContext(), "يجب تسجيل الدخول أولاً للإبلاغ", Toast.LENGTH_SHORT).show()
                return@setOnClickListener
            }

            binding.reportButton.isEnabled = false
            binding.reportButton.text = "جاري الإبلاغ..."

            val docRef = db.collection("properties").document(propertyId)

            docRef.get().addOnSuccessListener { document ->
                val currentReports = document.get("reportedBy") as? List<*>
                val reportsCount = currentReports?.size ?: 0

                val updates = hashMapOf<String, Any>(
                    "reportedBy" to FieldValue.arrayUnion(uid)
                )

                if (reportsCount >= 3) {
                    updates["status"] = "hidden"
                }

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

    private fun setupImageGallery(images: List<String>) {
        val displayImages = images.ifEmpty { listOf("") }

        binding.detailImagePager.adapter = GalleryImageAdapter(displayImages)

        binding.dotsIndicatorContainer.removeAllViews()

        if (displayImages.size <= 1) {
            binding.dotsIndicatorContainer.visibility = View.GONE
            binding.imageCounterText.visibility = View.GONE
            return
        }

        binding.dotsIndicatorContainer.visibility = View.VISIBLE
        binding.imageCounterText.visibility = View.VISIBLE
        binding.imageCounterText.text = "1 / ${displayImages.size}"

        val dots = mutableListOf<ImageView>()
        val density = resources.displayMetrics.density
        val margin = (4 * density).toInt()

        for (i in displayImages.indices) {
            val dot = ImageView(requireContext())
            val params = android.widget.LinearLayout.LayoutParams(
                android.widget.LinearLayout.LayoutParams.WRAP_CONTENT,
                android.widget.LinearLayout.LayoutParams.WRAP_CONTENT
            )
            params.marginStart = margin
            params.marginEnd = margin
            dot.layoutParams = params
            dot.setImageResource(
                if (i == 0) com.delilaqar.realestate.R.drawable.dot_selected
                else com.delilaqar.realestate.R.drawable.dot_unselected
            )
            binding.dotsIndicatorContainer.addView(dot)
            dots.add(dot)
        }

        binding.detailImagePager.registerOnPageChangeCallback(object : ViewPager2.OnPageChangeCallback() {
            override fun onPageSelected(position: Int) {
                super.onPageSelected(position)
                if (_binding == null) return
                for (i in dots.indices) {
                    dots[i].setImageResource(
                        if (i == position) com.delilaqar.realestate.R.drawable.dot_selected
                        else com.delilaqar.realestate.R.drawable.dot_unselected
                    )
                }
                binding.imageCounterText.text = "${position + 1} / ${displayImages.size}"
            }
        })
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
