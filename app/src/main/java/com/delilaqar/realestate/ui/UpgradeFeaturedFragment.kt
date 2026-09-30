package com.delilaqar.realestate.ui

import android.content.Intent
import android.net.Uri
import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.TextView
import android.widget.Toast
import androidx.core.content.ContextCompat
import androidx.fragment.app.Fragment
import com.delilaqar.realestate.R
import com.delilaqar.realestate.data.UpgradePlan
import com.delilaqar.realestate.data.UpgradePlans
import com.delilaqar.realestate.databinding.FragmentUpgradeFeaturedBinding
import com.delilaqar.realestate.util.CurrencyFormatter
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.firestore.FirebaseFirestore

class UpgradeFeaturedFragment : Fragment() {
    private var _binding: FragmentUpgradeFeaturedBinding? = null
    private val binding get() = _binding!!

    private var propertyId: String = ""
    private var propertyTitle: String = ""
    private var selectedPlan: UpgradePlan = UpgradePlans.LISTING.first()

    private data class PlanRow(val row: View, val label: TextView, val price: TextView)
    private lateinit var planRows: Map<String, PlanRow>

    companion object {
        const val WALLET_NUMBER = "07858055717"
        const val WHATSAPP_NUMBER = "+9647824553729"
    }

    override fun onCreateView(
        inflater: LayoutInflater, container: ViewGroup?, savedInstanceState: Bundle?
    ): View {
        _binding = FragmentUpgradeFeaturedBinding.inflate(inflater, container, false)
        return binding.root
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)

        propertyId = arguments?.getString("propertyId").orEmpty()

        planRows = mapOf(
            "3d" to PlanRow(binding.planRow3d, binding.planLabel3d, binding.planPrice3d),
            "7d" to PlanRow(binding.planRow7d, binding.planLabel7d, binding.planPrice7d),
            "30d" to PlanRow(binding.planRow30d, binding.planLabel30d, binding.planPrice30d),
            "365d" to PlanRow(binding.planRow365d, binding.planLabel365d, binding.planPrice365d),
            "bump" to PlanRow(binding.planRowBump, binding.planLabelBump, binding.planPriceBump)
        )

        (UpgradePlans.LISTING + UpgradePlans.BUMP).forEach { plan ->
            val rowViews = planRows[plan.id] ?: return@forEach
            rowViews.label.text = plan.label
            rowViews.price.text = CurrencyFormatter.format(plan.priceIqd)
            rowViews.row.setOnClickListener { selectPlan(plan) }
        }
        selectPlan(selectedPlan)

        binding.upgradeWalletText.text = getString(R.string.upgrade_featured_wallet_number, WALLET_NUMBER)

        if (propertyId.isNotEmpty()) {
            FirebaseFirestore.getInstance().collection("properties").document(propertyId).get()
                .addOnSuccessListener { doc ->
                    if (_binding == null) return@addOnSuccessListener
                    propertyTitle = doc.getString("title").orEmpty()
                    binding.upgradePropertyTitle.text = propertyTitle
                }
        }

        binding.upgradeWhatsappButton.setOnClickListener { openWhatsapp() }
    }

    private fun selectPlan(plan: UpgradePlan) {
        selectedPlan = plan
        val context = context ?: return
        planRows.forEach { (id, rowViews) ->
            val isSelected = id == plan.id
            rowViews.row.setBackgroundResource(if (isSelected) R.drawable.bg_pill_selected else R.drawable.bg_pill_unselected)
            val color = ContextCompat.getColor(context, if (isSelected) android.R.color.white else R.color.text_primary)
            rowViews.label.setTextColor(color)
            rowViews.price.setTextColor(color)
        }
    }

    private fun createUpgradeRequest() {
        if (propertyId.isEmpty()) return
        val request = hashMapOf(
            "propertyId" to propertyId,
            "propertyTitle" to propertyTitle,
            "ownerId" to (FirebaseAuth.getInstance().currentUser?.uid ?: ""),
            "planId" to selectedPlan.id,
            "planLabel" to selectedPlan.label,
            "durationDays" to selectedPlan.durationDays,
            "priceIqd" to selectedPlan.priceIqd,
            "requestedAt" to System.currentTimeMillis(),
            "status" to "pending"
        )
        FirebaseFirestore.getInstance().collection("upgradeRequests").document(propertyId).set(request)
    }

    private fun openWhatsapp() {
        createUpgradeRequest()
        val message = if (selectedPlan.durationDays > 0) {
            "مرحباً، حوّلت ${CurrencyFormatter.format(selectedPlan.priceIqd)} لترقية إعلاني إلى مميز " +
                "لمدة ${selectedPlan.label}.\nالإعلان: $propertyTitle\nرقم الإعلان: $propertyId\n(مرفق صورة إشعار التحويل)"
        } else {
            "مرحباً، حوّلت ${CurrencyFormatter.format(selectedPlan.priceIqd)} مقابل \"${selectedPlan.label}\".\n" +
                "الإعلان: $propertyTitle\nرقم الإعلان: $propertyId\n(مرفق صورة إشعار التحويل)"
        }
        try {
            val uri = Uri.parse("https://api.whatsapp.com/send?phone=$WHATSAPP_NUMBER&text=${Uri.encode(message)}")
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
