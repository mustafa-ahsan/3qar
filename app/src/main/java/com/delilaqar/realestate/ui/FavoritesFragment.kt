package com.delilaqar.realestate.ui

import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.Toast
import androidx.fragment.app.Fragment
import androidx.navigation.NavOptions
import androidx.navigation.fragment.findNavController
import androidx.recyclerview.widget.LinearLayoutManager
import com.delilaqar.realestate.R
import com.delilaqar.realestate.data.Property
import com.delilaqar.realestate.databinding.FragmentFavoritesBinding
import com.delilaqar.realestate.util.navigateSafe
import com.google.android.gms.tasks.Tasks
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.firestore.DocumentSnapshot
import com.google.firebase.firestore.FirebaseFirestore

class FavoritesFragment : Fragment() {
    private var _binding: FragmentFavoritesBinding? = null
    private val binding get() = _binding!!
    private val db = FirebaseFirestore.getInstance()
    private lateinit var adapter: PropertyAdapter

    override fun onCreateView(
        inflater: LayoutInflater, container: ViewGroup?, savedInstanceState: Bundle?
    ): View {
        _binding = FragmentFavoritesBinding.inflate(inflater, container, false)
        return binding.root
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)

        val uid = FirebaseAuth.getInstance().currentUser?.uid
        if (uid == null) {
            findNavController().navigateSafe(
                R.id.loginFragment,
                null,
                NavOptions.Builder().setPopUpTo(R.id.favoritesFragment, true).build()
            )
            return
        }

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
            onFavoriteClick = { property -> toggleFavorite(uid, property.id) }
        )
        binding.favoritesRecyclerView.layoutDirection = View.LAYOUT_DIRECTION_RTL
        binding.favoritesRecyclerView.layoutManager = LinearLayoutManager(requireContext())
        binding.favoritesRecyclerView.adapter = adapter

        loadFavorites(uid)
    }

    private fun loadFavorites(uid: String) {
        db.collection("users").document(uid).collection("favorites").get()
            .addOnSuccessListener { favSnapshot ->
                if (_binding == null) return@addOnSuccessListener
                val propertyIds = favSnapshot.documents.map { it.id }

                if (propertyIds.isEmpty()) {
                    adapter.updateFavorites(emptySet())
                    adapter.updateData(emptyList())
                    binding.emptyText.visibility = View.VISIBLE
                    return@addOnSuccessListener
                }

                val tasks = propertyIds.map { id -> db.collection("properties").document(id).get() }
                Tasks.whenAllSuccess<DocumentSnapshot>(tasks)
                    .addOnSuccessListener { docs ->
                        if (_binding == null) return@addOnSuccessListener
                        val properties = docs.mapNotNull { doc ->
                            doc.toObject(Property::class.java)?.apply { id = doc.id }
                        }
                        adapter.updateFavorites(propertyIds.toSet())
                        adapter.updateData(properties)
                        binding.emptyText.visibility = if (properties.isEmpty()) View.VISIBLE else View.GONE
                    }
            }
            .addOnFailureListener {
                if (_binding == null) return@addOnFailureListener
                binding.emptyText.visibility = View.VISIBLE
                binding.emptyText.text = "فشل تحميل المفضلة: ${it.message}"
            }
    }

    private fun toggleFavorite(uid: String, propertyId: String) {
        db.collection("users").document(uid).collection("favorites").document(propertyId)
            .delete()
            .addOnSuccessListener {
                if (isAdded) Toast.makeText(requireContext(), "أُزيل من المفضلة", Toast.LENGTH_SHORT).show()
                loadFavorites(uid)
            }
    }

    override fun onDestroyView() {
        super.onDestroyView()
        _binding = null
    }
}
