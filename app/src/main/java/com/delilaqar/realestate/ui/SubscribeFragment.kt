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
import com.delilaqar.realestate.databinding.FragmentSubscribeBinding
import com.delilaqar.realestate.util.CurrencyFormatter
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.firestore.FirebaseFirestore

class SubscribeFragment : Fragment() {
    private var _binding: FragmentSubscribeBinding? = null
    private val binding get() = _binding!!

    private var selectedPlan: UpgradePlan = UpgradePlans.ACCOUNT.first()

    private data class PlanRow(val row: View, val label: TextView, val price: TextView)
    private lateinit var planRows: Map<String, PlanRow>

    companion object {
        const val WALLET_NUMBER = "07858055717"
        const val WHATSAPP_NUMBER = "+9647824553729"
    }

    override fun onCreateView(
        inflater: LayoutInflater, container: ViewGroup?, savedInstanceState: Bundle?
    ): View {
        _binding = FragmentSubscribeBinding.inflate(inflater, container, false)
        return binding.root
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)

        planRows = mapOf(
            "acc_30d" to PlanRow(binding.planRowMonthly, binding.planLabelMonthly, binding.planPriceMonthly),
            "acc_365d" to PlanRow(binding.planRowYearly, binding.planLabelYearly, binding.planPriceYearly)
        )

        UpgradePlans.ACCOUNT.forEach { plan ->
            val rowViews = planRows[plan.id] ?: return@forEach
            rowViews.label.text = plan.label
            rowViews.price.text = CurrencyFormatter.format(plan.priceIqd)
            rowViews.row.setOnClickListener { selectPlan(plan) }
        }
        selectPlan(selectedPlan)

        binding.subscribeWalletText.text = getString(R.string.upgrade_featured_wallet_number, WALLET_NUMBER)
        binding.subscribeWhatsappButton.setOnClickListener { openWhatsapp() }
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

    private fun createSubscriptionRequest() {
        val uid = FirebaseAuth.getInstance().currentUser?.uid ?: return
        val db = FirebaseFirestore.getInstance()
        db.collection("users").document(uid).get()
            .addOnSuccessListener { doc ->
                val request = hashMapOf(
                    "ownerId" to uid,
                    "ownerName" to (doc.getString("name") ?: ""),
                    "ownerPhone" to (doc.getString("phone") ?: ""),
                    "planId" to selectedPlan.id,
                    "planLabel" to selectedPlan.label,
                    "durationDays" to selectedPlan.durationDays,
                    "priceIqd" to selectedPlan.priceIqd,
                    "requestedAt" to System.currentTimeMillis(),
                    "status" to "pending"
                )
                db.collection("subscriptionRequests").document(uid).set(request)
            }
    }

    private fun openWhatsapp() {
        createSubscriptionRequest()
        val message = "مرحباً، حوّلت ${CurrencyFormatter.format(selectedPlan.priceIqd)} لـ ${selectedPlan.label} " +
            "(اشتراك يخلي كل إعلانات حسابي مميزة).\n(مرفق صورة إشعار التحويل)"
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
