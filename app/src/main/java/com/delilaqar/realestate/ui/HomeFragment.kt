package com.delilaqar.realestate.ui

import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.Toast
import androidx.fragment.app.Fragment
import androidx.recyclerview.widget.LinearLayoutManager
import com.delilaqar.realestate.data.Property
import com.delilaqar.realestate.databinding.FragmentHomeBinding
import com.google.firebase.firestore.FirebaseFirestore

class HomeFragment : Fragment() {
    private var _binding: FragmentHomeBinding? = null
    private val binding get() = _binding!!
    private val db = FirebaseFirestore.getInstance()
    private lateinit var adapter: PropertyAdapter

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
            onDetailsClick = {
                Toast.makeText(requireContext(), "شاشة التفاصيل قريباً", Toast.LENGTH_SHORT).show()
            },
            onWhatsappClick = {
                Toast.makeText(requireContext(), "التواصل عبر واتساب قريباً", Toast.LENGTH_SHORT).show()
            }
        )
        binding.propertiesRecyclerView.layoutManager = LinearLayoutManager(requireContext())
        binding.propertiesRecyclerView.adapter = adapter

        loadProperties()
    }

    private fun loadProperties() {
        db.collection("properties")
            .whereEqualTo("status", "active")
            .get()
            .addOnSuccessListener { snapshot ->
                val properties = snapshot.documents.mapNotNull { doc ->
                    doc.toObject(Property::class.java)?.apply { id = doc.id }
                }
                adapter.updateData(properties)
                binding.emptyText.visibility = if (properties.isEmpty()) View.VISIBLE else View.GONE
            }
            .addOnFailureListener {
                binding.emptyText.visibility = View.VISIBLE
                binding.emptyText.text = "فشل تحميل العقارات: ${it.message}"
            }
    }

    override fun onDestroyView() {
        super.onDestroyView()
        _binding = null
    }
}
