package com.delilaqar.realestate.ui

import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.ArrayAdapter
import android.widget.Toast
import androidx.fragment.app.Fragment
import androidx.navigation.NavOptions
import androidx.navigation.fragment.findNavController
import com.delilaqar.realestate.R
import com.delilaqar.realestate.databinding.FragmentPostAdBinding
import com.delilaqar.realestate.util.navigateSafe
import com.delilaqar.realestate.util.setOnSingleClickListener
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.firestore.FirebaseFirestore

class PostAdFragment : Fragment() {
    private var _binding: FragmentPostAdBinding? = null
    private val binding get() = _binding!!
    private val db = FirebaseFirestore.getInstance()

    private val cities = linkedMapOf(
        "baghdad" to "بغداد",
        "basra" to "البصرة",
        "mosul" to "الموصل",
        "erbil" to "أربيل",
        "najaf" to "النجف",
        "karbala" to "كربلاء",
        "sulaymaniyah" to "السليمانية",
        "kirkuk" to "كركوك",
        "nasiriyah" to "الناصرية",
        "hillah" to "الحلة",
        "ramadi" to "الرمادي",
        "diwaniyah" to "الديوانية",
        "amarah" to "العمارة",
        "kut" to "الكوت",
        "dohuk" to "دهوك",
        "tikrit" to "تكريت",
        "samawah" to "السماوة",
        "baqubah" to "بعقوبة"
    )

    override fun onCreateView(
        inflater: LayoutInflater, container: ViewGroup?, savedInstanceState: Bundle?
    ): View {
        _binding = FragmentPostAdBinding.inflate(inflater, container, false)
        return binding.root
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)

        if (FirebaseAuth.getInstance().currentUser == null) {
            findNavController().navigateSafe(
                R.id.loginFragment,
                null,
                NavOptions.Builder().setPopUpTo(R.id.postAdFragment, true).build()
            )
            return
        }

        val cityAdapter = ArrayAdapter(
            requireContext(),
            android.R.layout.simple_list_item_1,
            cities.values.toList()
        )
        binding.cityInput.setAdapter(cityAdapter)

        binding.submitButton.setOnSingleClickListener { submitAd() }
    }

    private fun submitAd() {
        val uid = FirebaseAuth.getInstance().currentUser?.uid
        if (uid == null) {
            showError("الرجاء تسجيل الدخول أولاً")
            return
        }

        val title = binding.titleInput.text?.toString()?.trim().orEmpty()
        val description = binding.descriptionInput.text?.toString()?.trim().orEmpty()
        val cityName = binding.cityInput.text?.toString()?.trim().orEmpty()
        val district = binding.districtInput.text?.toString()?.trim().orEmpty()
        val priceText = binding.priceInput.text?.toString()?.trim().orEmpty()
        val bedroomsText = binding.bedroomsInput.text?.toString()?.trim().orEmpty()
        val bathroomsText = binding.bathroomsInput.text?.toString()?.trim().orEmpty()
        val areaText = binding.areaInput.text?.toString()?.trim().orEmpty()

        if (title.isEmpty() || cityName.isEmpty() || district.isEmpty() || priceText.isEmpty()) {
            showError("الرجاء تعبئة الحقول الأساسية (العنوان، المدينة، الحي، السعر)")
            return
        }

        val cityId = cities.entries.firstOrNull { it.value == cityName }?.key
        if (cityId == null) {
            showError("الرجاء اختيار مدينة من القائمة")
            return
        }

        val listingType = if (binding.listingTypeGroup.checkedChipId == binding.chipRent.id) "rent" else "sale"

        val propertyType = when (binding.propertyTypeGroup.checkedChipId) {
            binding.chipVilla.id -> "villa"
            binding.chipLand.id -> "land"
            binding.chipCommercial.id -> "commercial"
            binding.chipDuplex.id -> "duplex"
            binding.chipChalet.id -> "chalet"
            binding.chipBuilding.id -> "full_building"
            else -> "apartment"
        }

        binding.submitButton.isEnabled = false

        val property = hashMapOf(
            "title" to title,
            "description" to description,
            "listingType" to listingType,
            "propertyType" to propertyType,
            "price" to (priceText.toDoubleOrNull() ?: 0.0),
            "cityId" to cityId,
            "district" to district,
            "bedrooms" to (bedroomsText.toIntOrNull() ?: 0),
            "bathrooms" to (bathroomsText.toIntOrNull() ?: 0),
            "area" to (areaText.toDoubleOrNull() ?: 0.0),
            "featured" to false,
            "status" to "active",
            "images" to listOf("https://picsum.photos/seed/${System.currentTimeMillis()}/800/600"),
            "ownerId" to uid
        )

        db.collection("properties").add(property)
            .addOnSuccessListener {
                if (_binding == null) return@addOnSuccessListener
                Toast.makeText(requireContext(), "✅ تم نشر الإعلان بنجاح", Toast.LENGTH_LONG).show()
                findNavController().navigateSafe(
                    R.id.homeFragment,
                    null,
                    NavOptions.Builder().setPopUpTo(R.id.postAdFragment, true).build()
                )
            }
            .addOnFailureListener { e ->
                if (_binding == null) return@addOnFailureListener
                showError("فشل نشر الإعلان: ${e.message}")
                binding.submitButton.isEnabled = true
            }
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
