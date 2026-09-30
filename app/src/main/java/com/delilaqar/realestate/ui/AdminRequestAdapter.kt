package com.delilaqar.realestate.ui

import android.view.LayoutInflater
import android.view.ViewGroup
import androidx.recyclerview.widget.RecyclerView
import com.delilaqar.realestate.databinding.ItemUpgradeRequestBinding
import com.delilaqar.realestate.util.CurrencyFormatter
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale

data class UpgradeRequest(
    val propertyId: String = "",
    val propertyTitle: String = "",
    val planId: String = "",
    val planLabel: String = "",
    val durationDays: Int = 0,
    val priceIqd: Double = 0.0,
    val requestedAt: Long = 0
)

class AdminRequestAdapter(
    private val items: MutableList<UpgradeRequest>,
    private val onApprove: (UpgradeRequest) -> Unit,
    private val onDismiss: (UpgradeRequest) -> Unit
) : RecyclerView.Adapter<AdminRequestAdapter.ViewHolder>() {

    private val dateFormat = SimpleDateFormat("dd/MM HH:mm", Locale("ar"))

    inner class ViewHolder(val binding: ItemUpgradeRequestBinding) : RecyclerView.ViewHolder(binding.root)

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): ViewHolder {
        val binding = ItemUpgradeRequestBinding.inflate(LayoutInflater.from(parent.context), parent, false)
        return ViewHolder(binding)
    }

    override fun onBindViewHolder(holder: ViewHolder, position: Int) {
        val request = items[position]
        holder.binding.requestTitle.text = request.propertyTitle.ifEmpty { request.propertyId }
        holder.binding.requestMeta.text =
            "${request.planLabel} • ${CurrencyFormatter.format(request.priceIqd)} • ${dateFormat.format(Date(request.requestedAt))}"
        holder.binding.approveButton.setOnClickListener { onApprove(request) }
        holder.binding.dismissButton.setOnClickListener { onDismiss(request) }
    }

    override fun getItemCount() = items.size

    fun submitList(newItems: List<UpgradeRequest>) {
        items.clear()
        items.addAll(newItems)
        notifyDataSetChanged()
    }
}
