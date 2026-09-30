package com.delilaqar.realestate.ui

import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.TextView
import androidx.fragment.app.Fragment
import androidx.navigation.fragment.findNavController
import com.delilaqar.realestate.R
import com.delilaqar.realestate.data.Property
import com.delilaqar.realestate.databinding.FragmentTopOfficesBinding
import com.delilaqar.realestate.util.navigateSafe
import com.google.firebase.firestore.FirebaseFirestore

class TopOfficesFragment : Fragment() {
    private var _binding: FragmentTopOfficesBinding? = null
    private val binding get() = _binding!!

    override fun onCreateView(
        inflater: LayoutInflater, container: ViewGroup?, savedInstanceState: Bundle?
    ): View {
        _binding = FragmentTopOfficesBinding.inflate(inflater, container, false)
        return binding.root
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)
        val db = FirebaseFirestore.getInstance()

        db.collection("properties").whereEqualTo("status", "active").get()
            .addOnSuccessListener { snapshot ->
                if (_binding == null) return@addOnSuccessListener
                val now = System.currentTimeMillis()
                val properties = snapshot.toObjects(Property::class.java)
                val grouped = properties.filter { it.ownerId.isNotEmpty() }
                    .groupBy { it.ownerId }
                    .map { (ownerId, list) -> Triple(ownerId, list.size, list.any { it.verifiedUntil > now }) }
                    .sortedByDescending { it.second }
                    .take(20)

                binding.officesContainer.removeAllViews()
                if (grouped.isEmpty()) {
                    binding.emptyStateText.visibility = View.VISIBLE
                    return@addOnSuccessListener
                }
                grouped.forEachIndexed { index, triple ->
                    val (ownerId, count, verified) = triple
                    db.collection("users").document(ownerId).get().addOnSuccessListener { userDoc ->
                        if (_binding == null) return@addOnSuccessListener
                        val name = userDoc.getString("name") ?: "مستخدم"
                        addOfficeRow(index + 1, name, count, verified, ownerId)
                    }
                }
            }
    }

    private fun addOfficeRow(rank: Int, name: String, count: Int, verified: Boolean, ownerId: String) {
        val context = context ?: return
        val row = LayoutInflater.from(context).inflate(R.layout.item_top_office, binding.officesContainer, false)
        row.findViewById<TextView>(R.id.rankText).text = "#$rank"
        row.findViewById<TextView>(R.id.officeNameText).text = if (verified) "✓ $name" else name
        row.findViewById<TextView>(R.id.listingCountText).text = "$count إعلان"
        row.setOnClickListener {
            val bundle = Bundle().apply { putString("ownerId", ownerId) }
            findNavController().navigateSafe(R.id.officeProfileFragment, bundle)
        }
        binding.officesContainer.addView(row)
    }

    override fun onDestroyView() {
        super.onDestroyView()
        _binding = null
    }
}
