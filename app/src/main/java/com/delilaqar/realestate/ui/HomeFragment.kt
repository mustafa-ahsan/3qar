package com.delilaqar.realestate.ui

import android.content.Intent
import android.content.res.ColorStateList
import android.net.Uri
import android.os.Bundle
import android.text.Editable
import android.text.TextWatcher
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.LinearLayout
import android.widget.TextView
import android.widget.Toast
import androidx.core.content.ContextCompat
import androidx.fragment.app.Fragment
import androidx.navigation.fragment.findNavController
import androidx.recyclerview.widget.LinearLayoutManager
import androidx.recyclerview.widget.RecyclerView
import com.delilaqar.realestate.R
import com.delilaqar.realestate.data.Property
import com.delilaqar.realestate.databinding.FragmentHomeBinding
import com.delilaqar.realestate.util.navigateSafe
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.firestore.FirebaseFirestore
import java.util.Locale

class HomeFragment : Fragment() {
    private var _binding: FragmentHomeBinding? = null
    private val binding get() = _binding!!
    private val db = FirebaseFirestore.getInstance()

    private lateinit var adapter: PropertyAdapter
    private lateinit var featuredAdapter: FeaturedPropertyAdapter
    private val currentFavoriteIds = mutableSetOf<String>()
    private var allProperties: List<Property> = emptyList()

    // null = الكل / "sale" / "rent" / "wanted"
    private var selectedListingFilter: String? = null
    // null = الكل / "apartment" / "villa" / "land"
    private var selectedTypeFilter: String? = null

    override fun onCreateView(
        inflater: LayoutInflater, container: ViewGroup?, savedInstanceState: Bundle?
    ): View {
        _binding = FragmentHomeBinding.inflate(inflater, container, false)
        return binding.root
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)

        adapter = PropertyAdapter(
            items = emptyList(),
            onDetailsClick = { property -> openDetails(property) },
            onWhatsappClick = { property -> openWhatsapp(property) },
            onFavoriteClick = { property -> toggleFavorite(property) }
        )
        binding.propertiesRecyclerView.layoutManager = LinearLayoutManager(requireContext())
        binding.propertiesRecyclerView.adapter = adapter

        featuredAdapter = FeaturedPropertyAdapter(
            items = emptyList(),
            onClick = { property -> openDetails(property) }
        )
        binding.featuredRecyclerView.layoutManager =
            LinearLayoutManager(requireContext(), RecyclerView.HORIZONTAL, false)
        binding.featuredRecyclerView.adapter = featuredAdapter

        setupFilterChips()
        setupSearch()
        loadFavoriteIdsThenProperties()
    }

    private val searchHandler = android.os.Handler(android.os.Looper.getMainLooper())
    private var searchRunnable: Runnable? = null

    private fun setupSearch() {
        binding.searchInput.addTextChangedListener(object : TextWatcher {
            override fun beforeTextChanged(s: CharSequence?, start: Int, count: Int, after: Int) {}
            override fun onTextChanged(s: CharSequence?, start: Int, before: Int, count: Int) {
                searchRunnable?.let { searchHandler.removeCallbacks(it) }
                searchRunnable = Runnable { applyFilters() }
                searchHandler.postDelayed(searchRunnable!!, 300)
            }
            override fun afterTextChanged(s: Editable?) {}
        })
    }

    private fun setupFilterChips() {
        val listingChips = listOf(
            binding.chipAllOffers to null,
            binding.chipForSale to "sale",
            binding.chipForRent to "rent",
            binding.chipWanted to "wanted"
        )
        listingChips.forEach { (chipView, value) ->
            chipView.setOnClickListener {
                selectedListingFilter = value
                listingChips.forEach { (v, _) -> setPillSelected(v, v == chipView) }
                applyFilters()
            }
        }

        val typeChips = listOf(
            binding.typeAllContainer to null,
            binding.typeApartmentContainer to "apartment",
            binding.typeVillaContainer to "villa",
            binding.typeLandContainer to "land"
        )
        typeChips.forEach { (container, value) ->
            container.setOnClickListener {
                selectedTypeFilter = value
                typeChips.forEach { (v, _) -> setTypeContainerSelected(v, v == container) }
                applyFilters()
            }
        }
    }

    private fun setPillSelected(view: TextView, selected: Boolean) {
        view.setBackgroundResource(if (selected) R.drawable.bg_pill_selected else R.drawable.bg_pill_unselected)
        view.setTextColor(
            ContextCompat.getColor(requireContext(), if (selected) R.color.text_primary else R.color.text_secondary)
        )
    }

    private fun setTypeContainerSelected(container: LinearLayout, selected: Boolean) {
        container.setBackgroundResource(if (selected) R.drawable.bg_pill_selected else R.drawable.bg_pill_unselected)
        val label = container.getChildAt(1) as? TextView
        label?.setTextColor(
            ContextCompat.getColor(requireContext(), if (selected) R.color.text_primary else R.color.text_secondary)
        )
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
            .addOnFailureListener {
                if (_binding == null) return@addOnFailureListener
                binding.emptyText.visibility = View.VISIBLE
                binding.emptyText.text = "فشل تحميل العقارات: ${it.message}"
            }
    }

    private fun applyFilters() {
        if (_binding == null) return
        val query = binding.searchInput.text?.toString()?.trim()?.lowercase(Locale.getDefault()).orEmpty()

        val filtered = allProperties.filter { p ->
            val matchesListing = when (selectedListingFilter) {
                "sale" -> p.listingType == "sale"
                "rent" -> p.listingType == "rent"
                // ملاحظة: "مطلوب عقار" غير مدعوم بقاعدة البيانات حالياً، فما راح يطلع فيه نتائج لين نضيفه لاحقاً
                "wanted" -> false
                else -> true
            }
            val matchesType = selectedTypeFilter == null || p.propertyType == selectedTypeFilter
            val matchesSearch = query.isEmpty() ||
                p.title.lowercase(Locale.getDefault()).contains(query) ||
                p.district.lowercase(Locale.getDefault()).contains(query)
            matchesListing && matchesType && matchesSearch
        }

        val featured = filtered.filter { it.featured }
        featuredAdapter.updateData(featured)
        binding.featuredCountText.text = getString(R.string.properties_available_format, featured.size)
        binding.featuredSectionContainer.visibility = if (featured.isEmpty()) View.GONE else View.VISIBLE

        adapter.updateFavorites(currentFavoriteIds)
        adapter.updateData(filtered)
        binding.latestCountText.text = getString(R.string.properties_available_format, filtered.size)
        binding.emptyText.visibility = if (filtered.isEmpty()) View.VISIBLE else View.GONE
    }

    private fun openDetails(property: Property) {
        if (isAdded) {
            val bundle = Bundle().apply { putString("propertyId", property.id) }
            findNavController().navigateSafe(R.id.propertyDetailFragment, bundle)
        }
    }

    private fun openWhatsapp(property: Property) {
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
            startActivity(Intent(Intent.ACTION_VIEW, uri))
        } catch (e: Exception) {
            if (isAdded) Toast.makeText(requireContext(), "تطبيق واتساب غير مثبت على جهازك", Toast.LENGTH_SHORT).show()
        }
    }

    private fun toggleFavorite(property: Property) {
        val uid = FirebaseAuth.getInstance().currentUser?.uid
        if (uid == null) {
            if (isAdded) Toast.makeText(requireContext(), "سجل الدخول أولاً لحفظ العقار بالمفضلة", Toast.LENGTH_SHORT).show()
            return
        }
        val favRef = db.collection("users").document(uid).collection("favorites").document(property.id)
        if (currentFavoriteIds.contains(property.id)) {
            favRef.delete().addOnSuccessListener {
                currentFavoriteIds.remove(property.id)
                if (_binding != null) applyFilters()
            }
        } else {
            val data = mapOf("propertyId" to property.id, "addedAt" to System.currentTimeMillis())
            favRef.set(data).addOnSuccessListener {
                currentFavoriteIds.add(property.id)
                if (_binding != null) applyFilters()
            }
        }
    }

    override fun onDestroyView() {
        super.onDestroyView()
        _binding = null
    }
}
