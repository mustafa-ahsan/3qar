package com.delilaqar.realestate.ui

import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.Toast
import androidx.fragment.app.Fragment
import androidx.navigation.fragment.findNavController
import androidx.recyclerview.widget.LinearLayoutManager
import com.delilaqar.realestate.R
import com.delilaqar.realestate.data.Property
import com.delilaqar.realestate.databinding.FragmentHomeBinding
import com.delilaqar.realestate.util.navigateSafe
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.firestore.FirebaseFirestore

class HomeFragment : Fragment() {
    private var _binding: FragmentHomeBinding? = null
    private val binding get() = _binding!!
    private val db = FirebaseFirestore.getInstance()
    private lateinit var adapter: PropertyAdapter
    private val currentFavoriteIds = mutableSetOf<String>()

    override fun onCreateView(
        inflater: LayoutInflater, container: ViewGroup?, savedInstanceState: Bundle?
    ): View {
        _binding = FragmentHomeBinding.inflate(inflater, container, false)
        return binding.root
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)

        binding.propertiesRecyclerView.layoutDirection = View.LAYOUT_DIRECTION_RTL

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
        binding.propertiesRecyclerView.layoutManager = LinearLayoutManager(requireContext())
        binding.propertiesRecyclerView.adapter = adapter

        loadFavoriteIdsThenProperties()
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
                val properties = snapshot.documents.mapNotNull { doc ->
                    doc.toObject(Property::class.java)?.apply { id = doc.id }
                }
                adapter.updateFavorites(currentFavoriteIds)
                adapter.updateData(properties)
                binding.emptyText.visibility = if (properties.isEmpty()) View.VISIBLE else View.GONE
            }
            .addOnFailureListener {
                if (_binding == null) return@addOnFailureListener
                binding.emptyText.visibility = View.VISIBLE
                binding.emptyText.text = "فشل تحميل العقارات: ${it.message}"
            }
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
