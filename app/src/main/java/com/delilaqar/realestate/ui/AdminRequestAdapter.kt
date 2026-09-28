package com.delilaqar.realestate.ui

import android.view.LayoutInflater
import android.view.ViewGroup
import androidx.recyclerview.widget.RecyclerView
import com.delilaqar.realestate.databinding.ItemUpgradeRequestBinding
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale

data class UpgradeRequest(
    val propertyId: String = "",
    val propertyTitle: String = "",
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
        holder.binding.requestMeta.text = "${dateFormat.format(Date(request.requestedAt))}  •  ${request.propertyId}"
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
