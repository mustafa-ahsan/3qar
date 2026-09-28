package com.delilaqar.realestate.ui

import android.content.Intent
import android.net.Uri
import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.Toast
import androidx.fragment.app.Fragment
import com.delilaqar.realestate.R
import com.delilaqar.realestate.databinding.FragmentUpgradeFeaturedBinding
import com.delilaqar.realestate.util.CurrencyFormatter
import com.google.firebase.firestore.FirebaseFirestore

class UpgradeFeaturedFragment : Fragment() {
    private var _binding: FragmentUpgradeFeaturedBinding? = null
    private val binding get() = _binding!!

    private var propertyId: String = ""
    private var propertyTitle: String = ""

    companion object {
        const val PRICE_IQD = 2000.0
        const val DURATION_DAYS = 3
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

        binding.upgradePriceText.text = getString(
            R.string.upgrade_featured_price_format,
            CurrencyFormatter.format(PRICE_IQD),
            DURATION_DAYS
        )
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

    private fun openWhatsapp() {
        val message = "مرحباً، حوّلت ${CurrencyFormatter.format(PRICE_IQD)} لترقية إعلاني إلى مميز " +
            "لمدة $DURATION_DAYS أيام.\nالإعلان: $propertyTitle\nرقم الإعلان: $propertyId\n(مرفق صورة إشعار التحويل)"
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
