package com.delilaqar.realestate.ui

import android.os.Bundle
import android.text.Editable
import android.text.TextWatcher
import android.view.ContextThemeWrapper
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.Toast
import androidx.fragment.app.Fragment
import androidx.navigation.fragment.findNavController
import androidx.recyclerview.widget.LinearLayoutManager
import com.delilaqar.realestate.R
import com.delilaqar.realestate.data.Property
import com.delilaqar.realestate.databinding.FragmentSearchBinding
import com.delilaqar.realestate.util.navigateSafe
import com.google.android.material.chip.Chip
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.firestore.FirebaseFirestore

class SearchFragment : Fragment() {
    private var _binding: FragmentSearchBinding? = null
    private val binding get() = _binding!!
    private val db = FirebaseFirestore.getInstance()
    private lateinit var adapter: PropertyAdapter

    private var allProperties: List<Property> = emptyList()
    private val currentFavoriteIds = mutableSetOf<String>()
    private var selectedCityId: String? = null

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
        _binding = FragmentSearchBinding.inflate(inflater, container, false)
        return binding.root
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)

        adapter = PropertyAdapter(
            items = emptyList(),
            onDetailsClick = { property ->
                if (isAdded) {
                    val bundle = Bundle().apply { putString("propertyId", property.id) }
                    findNavController().navigateSafe(R.id.propertyDetailFragment, bundle)
                }
            },
            onWhatsappClick = {
                if (isAdded) Toast.makeText(requireContext(), "التواصل عبر واتساب قريباً", Toast.LENGTH_SHORT).show()
            },
            onFavoriteClick = { property -> toggleFavorite(property) }
        )
        binding.searchRecyclerView.layoutDirection = View.LAYOUT_DIRECTION_RTL
        binding.searchRecyclerView.layoutManager = LinearLayoutManager(requireContext())
        binding.searchRecyclerView.adapter = adapter

        setupCityChips()
        setupListeners()
        loadFavoriteIdsThenProperties()
    }

    private fun setupCityChips() {
        cities.forEach { (_, name) ->
            val chip = Chip(
                ContextThemeWrapper(
                    requireContext(),
                    com.google.android.material.R.style.Widget_MaterialComponents_Chip_Choice
                )
            ).apply {
                text = name
                isCheckable = true
                id = View.generateViewId()
            }
            binding.cityFilter.addView(chip)
        }

        binding.cityFilter.setOnCheckedStateChangeListener { group, checkedIds ->
            val checkedId = checkedIds.firstOrNull()
            selectedCityId = if (checkedId == null || checkedId == binding.filterAllCities.id) {
                null
            } else {
                val checkedChip = group.findViewById<Chip>(checkedId)
                cities.entries.firstOrNull { it.value == checkedChip?.text }?.key
            }
            applyFilters()
        }
    }

    private fun setupListeners() {
        binding.searchInput.addTextChangedListener(object : TextWatcher {
            override fun beforeTextChanged(s: CharSequence?, start: Int, count: Int, after: Int) {}
            override fun onTextChanged(s: CharSequence?, start: Int, before: Int, count: Int) {
                applyFilters()
            }
            override fun afterTextChanged(s: Editable?) {}
        })

        binding.listingTypeFilter.setOnCheckedStateChangeListener { _, _ -> applyFilters() }
        binding.propertyTypeFilter.setOnCheckedStateChangeListener { _, _ -> applyFilters() }
        binding.bedroomsFilter.setOnCheckedStateChangeListener { _, _ -> applyFilters() }
    }

    private fun loadFavoriteIdsThenProperties() {
        val uid = FirebaseAuth.getInstance().currentUser?.uid
        if (uid == null) {
            loadProperties()
            return
        }
        db.collection("users").document(uid).collection("favorites").get()
            .addOnSuccessListener { snapshot ->
                currentFavoriteIds.clear()
                currentFavoriteIds.addAll(snapshot.documents.map { it.id })
                loadProperties()
            }
            .addOnFailureListener { loadProperties() }
    }

    private fun loadProperties() {
        db.collection("properties")
            .whereEqualTo("status", "active")
            .get()
            .addOnSuccessListener { snapshot ->
                if (_binding == null) return@addOnSuccessListener
                allProperties = snapshot.documents.mapNotNull { doc ->
                    doc.toObject(Property::class.java)?.apply { id = doc.id }
                }
                applyFilters()
            }
    }

    private fun applyFilters() {
        if (_binding == null) return

        val query = binding.searchInput.text?.toString()?.trim().orEmpty()

        val listingType = when (binding.listingTypeFilter.checkedChipId) {
            binding.filterSale.id -> "sale"
            binding.filterRent.id -> "rent"
            else -> null
        }

        val propertyType = when (binding.propertyTypeFilter.checkedChipId) {
            binding.filterApartment.id -> "apartment"
            binding.filterVilla.id -> "villa"
            binding.filterLand.id -> "land"
            binding.filterCommercial.id -> "commercial"
            binding.filterDuplex.id -> "duplex"
            binding.filterChalet.id -> "chalet"
            binding.filterBuilding.id -> "full_building"
            else -> null
        }

        val minBedrooms = when (binding.bedroomsFilter.checkedChipId) {
            binding.filterBed1.id -> 1
            binding.filterBed2.id -> 2
            binding.filterBed3.id -> 3
            binding.filterBed4.id -> 4
            else -> null
        }

        val filtered = allProperties.filter { property ->
            (query.isEmpty() ||
                property.title.contains(query, ignoreCase = true) ||
                property.district.contains(query, ignoreCase = true)) &&
                (listingType == null || property.listingType == listingType) &&
                (propertyType == null || property.propertyType == propertyType) &&
                (selectedCityId == null || property.cityId == selectedCityId) &&
                (minBedrooms == null || property.bedrooms >= minBedrooms)
        }

        adapter.updateFavorites(currentFavoriteIds)
        adapter.updateData(filtered)
        binding.emptyText.visibility = if (filtered.isEmpty()) View.VISIBLE else View.GONE
    }

    private fun toggleFavorite(property: Property) {
        val uid = FirebaseAuth.getInstance().currentUser?.uid
        if (uid == null) {
            if (isAdded) {
                Toast.makeText(requireContext(), "سجل الدخول أولاً لحفظ العقار بالمفضلة", Toast.LENGTH_SHORT).show()
            }
            return
        }

        val favRef = db.collection("users").document(uid).collection("favorites").document(property.id)

        if (currentFavoriteIds.contains(property.id)) {
            favRef.delete().addOnSuccessListener {
                currentFavoriteIds.remove(property.id)
                if (_binding != null) adapter.updateFavorites(currentFavoriteIds)
            }
        } else {
            val data = mapOf("propertyId" to property.id, "addedAt" to System.currentTimeMillis())
            favRef.set(data).addOnSuccessListener {
                currentFavoriteIds.add(property.id)
                if (_binding != null) adapter.updateFavorites(currentFavoriteIds)
            }
        }
    }

    override fun onDestroyView() {
        super.onDestroyView()
        _binding = null
    }
}
