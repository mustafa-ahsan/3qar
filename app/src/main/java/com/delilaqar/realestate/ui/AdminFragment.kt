package com.delilaqar.realestate.ui

import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.Toast
import androidx.fragment.app.Fragment
import androidx.navigation.fragment.findNavController
import androidx.recyclerview.widget.LinearLayoutManager
import com.delilaqar.realestate.databinding.FragmentAdminBinding
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.firestore.FirebaseFirestore
import com.google.firebase.firestore.Query

class AdminFragment : Fragment() {
    private var _binding: FragmentAdminBinding? = null
    private val binding get() = _binding!!
    private val db = FirebaseFirestore.getInstance()
    private lateinit var adapter: AdminRequestAdapter

    companion object {
        const val ADMIN_EMAIL = "apk.apk.mustafa@gmail.com"
    }

    override fun onCreateView(
        inflater: LayoutInflater, container: ViewGroup?, savedInstanceState: Bundle?
    ): View {
        _binding = FragmentAdminBinding.inflate(inflater, container, false)
        return binding.root
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)

        if (FirebaseAuth.getInstance().currentUser?.email != ADMIN_EMAIL) {
            findNavController().popBackStack()
            return
        }

        adapter = AdminRequestAdapter(mutableListOf(), ::approveRequest, ::dismissRequest)
        binding.requestsRecyclerView.layoutManager = LinearLayoutManager(requireContext())
        binding.requestsRecyclerView.adapter = adapter

        db.collection("upgradeRequests")
            .orderBy("requestedAt", Query.Direction.DESCENDING)
            .addSnapshotListener { snapshot, _ ->
                if (_binding == null || snapshot == null) return@addSnapshotListener
                val requests = snapshot.documents.mapNotNull { doc ->
                    val propertyTitle = doc.getString("propertyTitle") ?: return@mapNotNull null
                    val requestedAt = doc.getLong("requestedAt") ?: 0
                    val planLabel = doc.getString("planLabel") ?: ""
                    val durationDays = (doc.getLong("durationDays") ?: 3).toInt()
                    val priceIqd = doc.getDouble("priceIqd") ?: 0.0
                    UpgradeRequest(
                        propertyId = doc.id,
                        propertyTitle = propertyTitle,
                        planLabel = planLabel,
                        durationDays = durationDays,
                        priceIqd = priceIqd,
                        requestedAt = requestedAt
                    )
                }
                adapter.submitList(requests)
                binding.emptyStateText.visibility = if (requests.isEmpty()) View.VISIBLE else View.GONE
            }
    }

    private fun approveRequest(request: UpgradeRequest) {
        val featuredUntil = System.currentTimeMillis() + request.durationDays * 24L * 3600L * 1000L
        db.collection("properties").document(request.propertyId)
            .update("featuredUntil", featuredUntil)
            .addOnSuccessListener {
                db.collection("upgradeRequests").document(request.propertyId).delete()
                if (isAdded) Toast.makeText(requireContext(), "تم تفعيل الإعلان المميز", Toast.LENGTH_SHORT).show()
            }
            .addOnFailureListener {
                if (isAdded) Toast.makeText(requireContext(), "فشل التفعيل، تأكد من رقم الإعلان", Toast.LENGTH_SHORT).show()
            }
    }

    private fun dismissRequest(request: UpgradeRequest) {
        db.collection("upgradeRequests").document(request.propertyId).delete()
    }

    override fun onDestroyView() {
        super.onDestroyView()
        _binding = null
    }
}
