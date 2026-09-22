package com.delilaqar.realestate.ui

import android.graphics.Bitmap
import android.graphics.ImageDecoder
import android.net.Uri
import android.os.Build
import android.os.Bundle
import android.provider.MediaStore
import android.view.Gravity
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.ArrayAdapter
import android.widget.FrameLayout
import android.widget.ImageView
import android.widget.LinearLayout
import android.widget.Toast
import androidx.activity.result.PickVisualMediaRequest
import androidx.activity.result.contract.ActivityResultContracts
import androidx.fragment.app.Fragment
import androidx.lifecycle.lifecycleScope
import androidx.navigation.NavOptions
import androidx.navigation.fragment.findNavController
import com.bumptech.glide.Glide
import com.delilaqar.realestate.R
import com.delilaqar.realestate.data.Property
import com.delilaqar.realestate.databinding.FragmentPostAdBinding
import com.delilaqar.realestate.util.ImgbbHelper
import com.delilaqar.realestate.util.NsfwDetector
import com.delilaqar.realestate.util.NsfwModelManager
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

    private val existingImageUrls = mutableListOf<String>()
    private val newImageUris = mutableListOf<Uri>()
    private var editingPropertyId: String? = null

    companion object {
        private const val MAX_IMAGES = 8
    }

    private val imagePickerLauncher = registerForActivityResult(
        ActivityResultContracts.PickMultipleVisualMedia(MAX_IMAGES)
    ) { uris: List<Uri> ->
        if (uris.isNotEmpty()) {
            newImageUris.clear()
            newImageUris.addAll(uris)
            refreshImagePreviews()
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

        binding.selectImageButton.setOnClickListener {
            imagePickerLauncher.launch(
                PickVisualMediaRequest(ActivityResultContracts.PickVisualMedia.ImageOnly)
            )
        }
        binding.submitButton.setOnSingleClickListener { submitAd() }

        NsfwModelManager.startBackgroundDownload(requireContext())

        val propertyId = arguments?.getString("propertyId")
        if (propertyId != null) {
            editingPropertyId = propertyId
            loadExistingProperty(propertyId)
        }
    }

    private fun loadExistingProperty(propertyId: String) {
        binding.submitButton.isEnabled = false
        db.collection("properties").document(propertyId).get()
            .addOnSuccessListener { doc ->
                if (_binding == null) return@addOnSuccessListener
                val property = doc.toObject(Property::class.java)
                if (property == null) {
                    showError("تعذّر تحميل بيانات الإعلان")
                    binding.submitButton.isEnabled = true
                    return@addOnSuccessListener
                }

                binding.screenTitle.text = "تعديل الإعلان"
                binding.screenSubtitle.text = "عدّل بيانات عقارك ثم احفظ التغييرات"
                binding.submitButton.text = "تحديث الإعلان"

                binding.titleInput.setText(property.title)
                binding.descriptionInput.setText(property.description)
                binding.districtInput.setText(property.district)
                if (property.price > 0) binding.priceInput.setText(property.price.toLong().toString())
                if (property.bedrooms > 0) binding.bedroomsInput.setText(property.bedrooms.toString())
                if (property.bathrooms > 0) binding.bathroomsInput.setText(property.bathrooms.toString())
                if (property.area > 0) binding.areaInput.setText(property.area.toInt().toString())

                cities[property.cityId]?.let { cityName -> binding.cityInput.setText(cityName, false) }

                binding.listingTypeGroup.check(when (property.listingType) { "rent" -> binding.chipRent.id; "wanted" -> binding.chipWanted.id; else -> binding.chipSale.id })
                val typeChipId = when (property.propertyType) {
                    "villa" -> binding.chipVilla.id
                    "land" -> binding.chipLand.id
                    "commercial" -> binding.chipCommercial.id
                    "duplex" -> binding.chipDuplex.id
                    "chalet" -> binding.chipChalet.id
                    "full_building" -> binding.chipBuilding.id
                    else -> binding.chipApartment.id
                }
                binding.propertyTypeGroup.check(typeChipId)

                existingImageUrls.clear()
                existingImageUrls.addAll(property.images)
                refreshImagePreviews()

                binding.submitButton.isEnabled = true
            }
            .addOnFailureListener {
                if (_binding == null) return@addOnFailureListener
                showError("فشل تحميل بيانات الإعلان: ${it.message}")
                binding.submitButton.isEnabled = true
            }
    }

    private fun refreshImagePreviews() {
        if (_binding == null) return
        binding.imagesPreviewContainer.removeAllViews()

        val totalCount = existingImageUrls.size + newImageUris.size
        if (totalCount == 0) {
            binding.imagesPreviewScroll.visibility = View.GONE
            binding.selectedImagesCountText.visibility = View.GONE
            binding.selectImageButtonText.text = "اختيار صور العقار (صورة واحدة على الأقل)"
            return
        }

        binding.imagesPreviewScroll.visibility = View.VISIBLE
        binding.selectedImagesCountText.visibility = View.VISIBLE
        binding.selectedImagesCountText.text = "$totalCount صورة مختارة"
        binding.selectImageButtonText.text = "تعديل الصور المختارة"

        val density = resources.displayMetrics.density
        val thumbSize = (84 * density).toInt()
        val margin = (6 * density).toInt()
        val removeSize = (22 * density).toInt()

        for (url in existingImageUrls.toList()) {
            addImageThumbnail(url, thumbSize, margin, removeSize, density) {
                existingImageUrls.remove(url)
                refreshImagePreviews()
            }
        }
        for (uri in newImageUris.toList()) {
            addImageThumbnail(uri, thumbSize, margin, removeSize, density) {
                newImageUris.remove(uri)
                refreshImagePreviews()
            }
        }
    }

    private fun addImageThumbnail(
        model: Any, thumbSize: Int, margin: Int, removeSize: Int, density: Float, onRemove: () -> Unit
    ) {
        val frame = FrameLayout(requireContext())
        val frameParams = LinearLayout.LayoutParams(thumbSize, thumbSize)
        frameParams.marginEnd = margin
        frame.layoutParams = frameParams

        val imageView = ImageView(requireContext())
        imageView.layoutParams = FrameLayout.LayoutParams(
            FrameLayout.LayoutParams.MATCH_PARENT,
            FrameLayout.LayoutParams.MATCH_PARENT
        )
        imageView.scaleType = ImageView.ScaleType.CENTER_CROP
        Glide.with(this).load(model).centerCrop().into(imageView)
        frame.addView(imageView)

        val removeButton = ImageView(requireContext())
        val removeParams = FrameLayout.LayoutParams(removeSize, removeSize)
        removeParams.gravity = Gravity.TOP or Gravity.END
        removeParams.topMargin = (4 * density).toInt()
        removeParams.marginEnd = (4 * density).toInt()
        removeButton.layoutParams = removeParams
        removeButton.setBackgroundResource(R.drawable.bg_delete_circle)
        removeButton.setImageResource(R.drawable.ic_delete)
        val pad = (4 * density).toInt()
        removeButton.setPadding(pad, pad, pad, pad)
        removeButton.setOnClickListener { onRemove() }
        frame.addView(removeButton)

        binding.imagesPreviewContainer.addView(frame)
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
            showError("الرجاء تعبئة الحقول الأساسية")
            return
        }

        val listingType = when (binding.listingTypeGroup.checkedChipId) {
            binding.chipRent.id -> "rent"
            binding.chipWanted.id -> "wanted"
            else -> "sale"
        }

        if (listingType != "wanted" && existingImageUrls.isEmpty() && newImageUris.isEmpty()) {
            showError("الرجاء اختيار صورة واحدة على الأقل")
            return
        }

        if (newImageUris.isNotEmpty()) {
            when (NsfwModelManager.state) {
                NsfwModelManager.State.DOWNLOADING -> {
                    showError("جاري تحميل ملفات مهمة تخص رفع الصور والعقار، يمكنك متابعة نسبة التحميل من صفحة حسابي.")
                    return
                }
                NsfwModelManager.State.FAILED -> {
                    showError("تعذّر تحميل الملفات المطلوبة بسبب مشكلة اتصال. يمكنك متابعة الحالة وإعادة المحاولة من صفحة حسابي.")
                    NsfwModelManager.retry(requireContext())
                    return
                }
                NsfwModelManager.State.IDLE -> {
                    if (!NsfwModelManager.isReady(requireContext())) {
                        showError("جاري تحميل ملفات مهمة تخص رفع الصور والعقار، يمكنك متابعة نسبة التحميل من صفحة حسابي.")
                        NsfwModelManager.startBackgroundDownload(requireContext())
                        return
                    }
                }
                NsfwModelManager.State.READY -> { /* تكمل عادي */ }
            }
        }

        val cityId = cities.entries.firstOrNull { it.value == cityName }?.key ?: return
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
        val urisToCheck = newImageUris.toList()

        db.collection("users").document(uid).get().addOnSuccessListener { userDoc ->
            val userPhone = userDoc.getString("phone") ?: "+9647000000000"

            lifecycleScope.launch {
                for ((index, uri) in urisToCheck.withIndex()) {
                    binding.submitButton.text = "جاري فحص الصورة ${index + 1} من ${urisToCheck.size}..."

                    val (isNsfw, debugStr) = withContext(Dispatchers.IO) {
                        try {
                            val bitmap = decodeBitmap(uri)
                            val detector = NsfwDetector(requireContext())
                            val result = detector.isNsfw(bitmap)
                            detector.close()
                            result
                        } catch (e: Exception) {
                            e.printStackTrace()
                            Pair(true, "Exception: ${e.message}")
                        }
                    }

                    if (debugStr == "MODEL_NOT_READY") {
                        showError("جاري تحميل ملفات مهمة تخص رفع الصور والعقار، يمكنك متابعة نسبة التحميل من صفحة حسابي.")
                        binding.submitButton.isEnabled = true
                        binding.submitButton.text = originalButtonText
                        return@launch
                    }
                    if (isNsfw) {
                        showError("عذراً! تم حظر النشر لأن الصورة رقم ${index + 1} تحتوي على مشاهد غير لائقة.")
                        binding.submitButton.isEnabled = true
                        binding.submitButton.text = originalButtonText
                        return@launch
                    }
                }

                val uploadedUrls = mutableListOf<String>()
                for ((index, uri) in urisToCheck.withIndex()) {
                    binding.submitButton.text = "جاري رفع الصورة ${index + 1} من ${urisToCheck.size}..."
                    val url = ImgbbHelper.uploadImage(requireContext(), uri)
                    if (url == null) {
                        showError("فشل رفع الصورة رقم ${index + 1}. تأكد من اتصالك بالإنترنت وحاول مرة أخرى.")
                        binding.submitButton.isEnabled = true
                        binding.submitButton.text = originalButtonText
                        return@launch
                    }
                    uploadedUrls.add(url)
                }

                val finalImages = existingImageUrls + uploadedUrls
                val currentEditingId = editingPropertyId

                if (currentEditingId != null) {
                    binding.submitButton.text = "جاري حفظ التعديلات..."
                    val updateData = hashMapOf<String, Any>(
                        "title" to title, "description" to description, "listingType" to listingType,
                        "propertyType" to propertyType, "price" to (priceText.toDoubleOrNull() ?: 0.0),
                        "cityId" to cityId, "district" to district,
                        "bedrooms" to (bedroomsText.toIntOrNull() ?: 0),
                        "bathrooms" to (bathroomsText.toIntOrNull() ?: 0),
                        "area" to (areaText.toDoubleOrNull() ?: 0.0),
                        "images" to finalImages
                    )
                    db.collection("properties").document(currentEditingId).update(updateData)
                        .addOnSuccessListener {
                            if (_binding == null) return@addOnSuccessListener
                            Toast.makeText(requireContext(), "✅ تم تحديث الإعلان بنجاح", Toast.LENGTH_LONG).show()
                            findNavController().navigateSafe(R.id.profileFragment, null, NavOptions.Builder().setPopUpTo(R.id.postAdFragment, true).build())
                        }
                        .addOnFailureListener { e ->
                            if (_binding == null) return@addOnFailureListener
                            showError("فشل تحديث الإعلان: ${e.message}")
                            binding.submitButton.isEnabled = true
                            binding.submitButton.text = originalButtonText
                        }
                } else {
                    binding.submitButton.text = "جاري النشر..."
                    val property = hashMapOf(
                        "title" to title, "description" to description, "listingType" to listingType,
                        "propertyType" to propertyType, "price" to (priceText.toDoubleOrNull() ?: 0.0),
                        "cityId" to cityId, "district" to district, "status" to "active",
                        "images" to finalImages, "ownerId" to uid, "phoneNumber" to userPhone,
                        "createdAt" to System.currentTimeMillis(),
                        "bedrooms" to (bedroomsText.toIntOrNull() ?: 0),
                        "bathrooms" to (bathroomsText.toIntOrNull() ?: 0),
                        "area" to (areaText.toDoubleOrNull() ?: 0.0),
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
            }
        }.addOnFailureListener {
            showError("فشل الوصول لبيانات حسابك.")
            binding.submitButton.isEnabled = true
            binding.submitButton.text = originalButtonText
        }
    }

    private fun decodeBitmap(uri: Uri): Bitmap {
        val bitmap = if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.P) {
            val source = ImageDecoder.createSource(requireContext().contentResolver, uri)
            ImageDecoder.decodeBitmap(source) { decoder, _, _ ->
                decoder.allocator = ImageDecoder.ALLOCATOR_SOFTWARE
                decoder.isMutableRequired = true
            }
        } else {
            MediaStore.Images.Media.getBitmap(requireContext().contentResolver, uri)
        }
        return bitmap.copy(Bitmap.Config.ARGB_8888, true) ?: bitmap
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
