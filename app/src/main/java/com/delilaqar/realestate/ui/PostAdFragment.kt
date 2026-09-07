package com.delilaqar.realestate.ui

import android.net.Uri
import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.ArrayAdapter
import android.widget.Toast
import androidx.activity.result.contract.ActivityResultContracts
import androidx.fragment.app.Fragment
import androidx.lifecycle.lifecycleScope
import androidx.navigation.NavOptions
import androidx.navigation.fragment.findNavController
import com.delilaqar.realestate.R
import com.delilaqar.realestate.databinding.FragmentPostAdBinding
import com.delilaqar.realestate.util.ImgbbHelper
import com.delilaqar.realestate.util.navigateSafe
import com.delilaqar.realestate.util.setOnSingleClickListener
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.firestore.FirebaseFirestore
import kotlinx.coroutines.launch

class PostAdFragment : Fragment() {
    private var _binding: FragmentPostAdBinding? = null
    private val binding get() = _binding!!
    private val db = FirebaseFirestore.getInstance()

    // 1. متغير لحفظ مسار الصورة التي سيختارها المستخدم
    private var selectedImageUri: Uri? = null

    // 2. أداة لفتح الاستوديو واختيار صورة
    private val imagePickerLauncher = registerForActivityResult(ActivityResultContracts.GetContent()) { uri: Uri? ->
        if (uri != null) {
            selectedImageUri = uri
            // عرض الصورة للمستخدم بعد اختيارها
            binding.imagePreview.setImageURI(uri)
            binding.imagePreview.visibility = View.VISIBLE
        }
    }

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

        // 3. ربط زر اختيار الصورة بالاستوديو
        binding.selectImageButton.setOnClickListener {
            imagePickerLauncher.launch("image/*") // يقبل الصور فقط
        }

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

        // 4. إجبار المستخدم على اختيار صورة للعقار
        if (selectedImageUri == null) {
            showError("الرجاء اختيار صورة واحدة على الأقل للعقار")
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
        val originalButtonText = binding.submitButton.text
        binding.submitButton.text = "جاري رفع الصورة والنشر..." // تحديث واجهة المستخدم

        // 5. استخدام Coroutine لرفع الصورة بدون تجميد التطبيق
        lifecycleScope.launch {
            val uploadedUrl = ImgbbHelper.uploadImage(requireContext(), selectedImageUri!!)

            if (uploadedUrl == null) {
                showError("فشل رفع الصورة، يرجى التأكد من اتصالك بالإنترنت والمحاولة مجدداً.")
                binding.submitButton.isEnabled = true
                binding.submitButton.text = originalButtonText
                return@launch
            }

            // 6. إذا نجح الرفع، ننشئ كائن العقار مع الرابط الحقيقي
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
                "images" to listOf(uploadedUrl), // الرابط الحقيقي هنا!
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
                    binding.submitButton.text = originalButtonText
                }
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