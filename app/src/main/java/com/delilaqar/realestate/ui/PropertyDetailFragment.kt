package com.delilaqar.realestate.ui

import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.Toast
import androidx.fragment.app.Fragment
import com.bumptech.glide.Glide
import com.delilaqar.realestate.data.Property
import com.delilaqar.realestate.databinding.FragmentPropertyDetailBinding
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
                bindProperty(property)
            }
            .addOnFailureListener {
                if (isAdded) Toast.makeText(requireContext(), "فشل تحميل العقار: ${it.message}", Toast.LENGTH_LONG).show()
            }
    }

    private fun bindProperty(property: Property) {
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
        if (imageUrl != null) {
            Glide.with(requireContext()).load(imageUrl).centerCrop().into(binding.detailImage)
        }

        binding.whatsappDetailButton.setOnClickListener {
            if (isAdded) Toast.makeText(requireContext(), "سيتوفر التواصل قريباً", Toast.LENGTH_SHORT).show()
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
