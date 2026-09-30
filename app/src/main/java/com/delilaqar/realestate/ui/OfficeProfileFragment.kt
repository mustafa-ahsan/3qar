package com.delilaqar.realestate.ui

import android.content.Intent
import android.net.Uri
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
import com.delilaqar.realestate.databinding.FragmentOfficeProfileBinding
import com.delilaqar.realestate.util.navigateSafe
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.firestore.FirebaseFirestore

class OfficeProfileFragment : Fragment() {
    private var _binding: FragmentOfficeProfileBinding? = null
    private val binding get() = _binding!!
    private lateinit var adapter: PropertyAdapter

    override fun onCreateView(
        inflater: LayoutInflater, container: ViewGroup?, savedInstanceState: Bundle?
    ): View {
        _binding = FragmentOfficeProfileBinding.inflate(inflater, container, false)
        return binding.root
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)
        val ownerId = arguments?.getString("ownerId").orEmpty()
        if (ownerId.isEmpty()) {
            findNavController().popBackStack()
            return
        }

        val db = FirebaseFirestore.getInstance()

        adapter = PropertyAdapter(
            items = emptyList(),
            onDetailsClick = { property -> openDetails(property) },
            onWhatsappClick = { property -> openWhatsapp(property) },
            onFavoriteClick = { },
            currentUserId = FirebaseAuth.getInstance().currentUser?.uid
        )
        binding.officePropertiesRecyclerView.layoutManager = LinearLayoutManager(requireContext())
        binding.officePropertiesRecyclerView.adapter = adapter

        db.collection("users").document(ownerId).get().addOnSuccessListener { doc ->
            if (_binding == null) return@addOnSuccessListener
            binding.officeNameText.text = doc.getString("name") ?: "مكتب عقاري"
            binding.officePhoneText.text = doc.getString("phone") ?: ""
            val subscriptionUntil = doc.getLong("subscriptionUntil") ?: 0
            binding.officeVerifiedBadge.visibility =
                if (subscriptionUntil > System.currentTimeMillis()) View.VISIBLE else View.GONE
        }

        db.collection("properties")
            .whereEqualTo("ownerId", ownerId)
            .whereEqualTo("status", "active")
            .get()
            .addOnSuccessListener { snapshot ->
                if (_binding == null) return@addOnSuccessListener
                adapter.updateData(snapshot.toObjects(Property::class.java))
            }
    }

    private fun openDetails(property: Property) {
        val bundle = Bundle().apply { putString("propertyId", property.id) }
        findNavController().navigateSafe(R.id.propertyDetailFragment, bundle)
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

    override fun onDestroyView() {
        super.onDestroyView()
        _binding = null
    }
}
