package com.delilaqar.realestate.ui

import android.view.LayoutInflater
import android.view.ViewGroup
import androidx.recyclerview.widget.RecyclerView
import com.delilaqar.realestate.databinding.ItemUpgradeRequestBinding
import com.delilaqar.realestate.util.CurrencyFormatter
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale

data class SubscriptionRequest(
    val ownerId: String = "",
    val ownerName: String = "",
    val ownerPhone: String = "",
    val planLabel: String = "",
    val durationDays: Int = 0,
    val priceIqd: Double = 0.0,
    val requestedAt: Long = 0
)

class AdminSubscriptionAdapter(
    private val items: MutableList<SubscriptionRequest>,
    private val onApprove: (SubscriptionRequest) -> Unit,
    private val onDismiss: (SubscriptionRequest) -> Unit
) : RecyclerView.Adapter<AdminSubscriptionAdapter.ViewHolder>() {

    private val dateFormat = SimpleDateFormat("dd/MM HH:mm", Locale("ar"))

    inner class ViewHolder(val binding: ItemUpgradeRequestBinding) : RecyclerView.ViewHolder(binding.root)

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): ViewHolder {
        val binding = ItemUpgradeRequestBinding.inflate(LayoutInflater.from(parent.context), parent, false)
        return ViewHolder(binding)
    }

    override fun onBindViewHolder(holder: ViewHolder, position: Int) {
        val request = items[position]
        holder.binding.requestTitle.text = request.ownerName.ifEmpty { "مكتب عقاري" }
        holder.binding.requestMeta.text =
            "${request.planLabel} • ${CurrencyFormatter.format(request.priceIqd)} • ${request.ownerPhone} • ${dateFormat.format(Date(request.requestedAt))}"
        holder.binding.approveButton.setOnClickListener { onApprove(request) }
        holder.binding.dismissButton.setOnClickListener { onDismiss(request) }
    }

    override fun getItemCount() = items.size

    fun submitList(newItems: List<SubscriptionRequest>) {
        items.clear()
        items.addAll(newItems)
        notifyDataSetChanged()
    }
}
