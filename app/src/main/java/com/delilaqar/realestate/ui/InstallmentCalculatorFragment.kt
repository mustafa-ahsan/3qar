package com.delilaqar.realestate.ui

import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import androidx.fragment.app.Fragment
import com.delilaqar.realestate.databinding.FragmentInstallmentCalculatorBinding
import java.util.Locale

class InstallmentCalculatorFragment : Fragment() {
    private var _binding: FragmentInstallmentCalculatorBinding? = null
    private val binding get() = _binding!!

    override fun onCreateView(
        inflater: LayoutInflater, container: ViewGroup?, savedInstanceState: Bundle?
    ): View {
        _binding = FragmentInstallmentCalculatorBinding.inflate(inflater, container, false)
        return binding.root
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)

        binding.calculationModeGroup.setOnCheckedStateChangeListener { _, checkedIds ->
            val withInterest = checkedIds.contains(binding.chipWithInterest.id)
            binding.profitRateLayout.visibility = if (withInterest) View.VISIBLE else View.GONE
        }

        binding.calculateButton.setOnClickListener { calculate() }
    }

    private fun calculate() {
        hideError()

        val totalPrice = binding.totalPriceInput.text?.toString()?.trim()?.toDoubleOrNull()
        if (totalPrice == null || totalPrice <= 0) {
            showError("يرجى إدخال سعر صحيح للعقار")
            return
        }

        val downPayment = binding.downPaymentInput.text?.toString()?.trim()?.toDoubleOrNull() ?: 0.0
        if (downPayment < 0 || downPayment >= totalPrice) {
            showError("الدفعة الأولى يجب أن تكون أقل من السعر الكلي")
            return
        }

        val months = binding.monthsInput.text?.toString()?.trim()?.toIntOrNull()
        if (months == null || months <= 0) {
            showError("يرجى إدخال مدة سداد صحيحة بالأشهر")
            return
        }

        val withInterest = binding.calculationModeGroup.checkedChipId == binding.chipWithInterest.id
        val principal = totalPrice - downPayment

        val totalProfit: Double
        val monthlyInstallment: Double

        if (withInterest) {
            val annualRate = binding.profitRateInput.text?.toString()?.trim()?.toDoubleOrNull()
            if (annualRate == null || annualRate < 0) {
                showError("يرجى إدخال نسبة ربح صحيحة")
                return
            }
            totalProfit = principal * (annualRate / 100.0) * (months / 12.0)
            monthlyInstallment = (principal + totalProfit) / months
        } else {
            totalProfit = 0.0
            monthlyInstallment = principal / months
        }

        val totalPayable = principal + totalProfit

        binding.monthlyInstallmentText.text = formatCurrency(monthlyInstallment)
        binding.principalText.text = formatCurrency(principal)
        binding.profitText.text = formatCurrency(totalProfit)
        binding.totalPayableText.text = formatCurrency(totalPayable)
        binding.resultCard.visibility = View.VISIBLE
    }

    private fun formatCurrency(value: Double): String {
        return "$${String.format(Locale.US, "%,.2f", value)}"
    }

    private fun showError(message: String) {
        binding.errorText.text = message
        binding.errorText.visibility = View.VISIBLE
        binding.resultCard.visibility = View.GONE
    }

    private fun hideError() {
        binding.errorText.visibility = View.GONE
    }

    override fun onDestroyView() {
        super.onDestroyView()
        _binding = null
    }
}
