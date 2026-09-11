package com.delilaqar.realestate.ui

import android.graphics.Bitmap
import android.graphics.ImageDecoder
import android.net.Uri
import android.os.Build
import android.os.Bundle
import android.provider.MediaStore
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
import com.delilaqar.realestate.util.NsfwDetector
import com.delilaqar.realestate.util.navigateSafe
import com.delilaqar.realestate.util.setOnSingleClickListener
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.firestore.FirebaseFirestore
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext

class PostAdFragment : Fragment() {
    private var _binding: FragmentPostAdBinding? = null
    private val binding get() = _binding!!
    private val db = FirebaseFirestore.getInstance()

    private var selectedImageUri: Uri? = null

    private val imagePickerLauncher = registerForActivityResult(ActivityResultContracts.GetContent()) { uri: Uri? ->
        if (uri != null) {
            selectedImageUri = uri
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
            findNavController().navigateSafe(R.id.loginFragment, null, NavOptions.Builder().setPopUpTo(R.id.postAdFragment, true).build())
            return
        }

        val cityAdapter = ArrayAdapter(requireContext(), android.R.layout.simple_list_item_1, cities.values.toList())
        binding.cityInput.setAdapter(cityAdapter)

        binding.selectImageButton.setOnClickListener { imagePickerLauncher.launch("image/*") }
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
        
        if (title.isEmpty() || cityName.isEmpty() || district.isEmpty() || priceText.isEmpty()) {
            showError("الرجاء تعبئة الحقول الأساسية")
            return
        }

        if (selectedImageUri == null) {
            showError("الرجاء اختيار صورة واحدة على الأقل")
            return
        }

        val cityId = cities.entries.firstOrNull { it.value == cityName }?.key ?: return
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

        try {
            val checkStream = requireContext().contentResolver.openInputStream(selectedImageUri!!)
            val exifCheck = checkStream?.let { androidx.exifinterface.media.ExifInterface(it) }
            val orientationValue = exifCheck?.getAttributeInt(
                androidx.exifinterface.media.ExifInterface.TAG_ORIENTATION, -1
            ) ?: -1
            checkStream?.close()
            Toast.makeText(requireContext(), "قيمة الدوران EXIF: $orientationValue", Toast.LENGTH_LONG).show()
        } catch (e: Exception) {
            Toast.makeText(requireContext(), "خطأ EXIF: ${e.message}", Toast.LENGTH_LONG).show()
        }

        binding.submitButton.isEnabled = false
        val originalButtonText = binding.submitButton.text
        binding.submitButton.text = "جاري الفحص الأمني للصورة..."

        db.collection("users").document(uid).get().addOnSuccessListener { userDoc ->
            val userPhone = userDoc.getString("phone") ?: "+9647000000000"

            lifecycleScope.launch {
                val (isNsfw, debugStr) = withContext(Dispatchers.IO) {
                    try {
                        val bitmap = if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.P) {
                            val source = ImageDecoder.createSource(requireContext().contentResolver, selectedImageUri!!)
                            // إجبار الأندرويد على استخدام Software Bitmap ليتوافق مع الذكاء الاصطناعي
                            ImageDecoder.decodeBitmap(source) { decoder, _, _ ->
                                decoder.allocator = ImageDecoder.ALLOCATOR_SOFTWARE
                                decoder.isMutableRequired = true
                            }
                        } else {
                            MediaStore.Images.Media.getBitmap(requireContext().contentResolver, selectedImageUri!!)
                        }
                        
                        val softwareBitmap = bitmap.copy(Bitmap.Config.ARGB_8888, true) ?: bitmap
                        val detector = NsfwDetector(requireContext())
                        val result = detector.isNsfw(softwareBitmap)
                        detector.close()
                        result
                    } catch (e: Exception) {
                        e.printStackTrace()
                        Pair(true, "Exception: ${e.message}") 
                    }
                }

                // عرض الأرقام على الشاشة لكي نقرأها
                Toast.makeText(requireContext(), "أرقام الفحص: $debugStr", Toast.LENGTH_LONG).show()

                if (isNsfw) {
                    showError("عذراً! تم حظر نشر الإعلان لأن الصورة تحتوي على مشاهد غير لائقة.")
                    binding.submitButton.isEnabled = true
                    binding.submitButton.text = originalButtonText
                    return@launch
                }

                binding.submitButton.text = "جاري الرفع والنشر..."

                val uploadedUrl = ImgbbHelper.uploadImage(requireContext(), selectedImageUri!!)

                if (uploadedUrl == null) {
                    showError("فشل رفع الصورة.")
                    binding.submitButton.isEnabled = true
                    binding.submitButton.text = originalButtonText
                    return@launch
                }

                val property = hashMapOf(
                    "title" to title, "description" to description, "listingType" to listingType,
                    "propertyType" to propertyType, "price" to (priceText.toDoubleOrNull() ?: 0.0),
                    "cityId" to cityId, "district" to district, "status" to "active",
                    "images" to listOf(uploadedUrl), "ownerId" to uid, "phoneNumber" to userPhone
                )

                db.collection("properties").add(property)
                    .addOnSuccessListener {
                        if (_binding == null) return@addOnSuccessListener
                        Toast.makeText(requireContext(), "✅ تم نشر الإعلان بنجاح", Toast.LENGTH_LONG).show()
                        findNavController().navigateSafe(R.id.homeFragment, null, NavOptions.Builder().setPopUpTo(R.id.postAdFragment, true).build())
                    }
                    .addOnFailureListener { e ->
                        if (_binding == null) return@addOnFailureListener
                        showError("فشل نشر الإعلان: ${e.message}")
                        binding.submitButton.isEnabled = true
                        binding.submitButton.text = originalButtonText
                    }
            }
        }.addOnFailureListener {
            showError("فشل الوصول لبيانات حسابك.")
            binding.submitButton.isEnabled = true
            binding.submitButton.text = originalButtonText
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
