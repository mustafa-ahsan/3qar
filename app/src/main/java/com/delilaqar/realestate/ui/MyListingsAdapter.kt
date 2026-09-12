package com.delilaqar.realestate.ui

import android.view.LayoutInflater
import android.view.ViewGroup
import androidx.recyclerview.widget.RecyclerView
import com.bumptech.glide.Glide
import com.delilaqar.realestate.data.Property
import com.delilaqar.realestate.databinding.ItemMyListingBinding
import java.util.Locale

class MyListingsAdapter(
    private var items: List<Property>,
    private val onDeleteClick: (Property) -> Unit
) : RecyclerView.Adapter<MyListingsAdapter.ViewHolder>() {

    inner class ViewHolder(val binding: ItemMyListingBinding) : RecyclerView.ViewHolder(binding.root)

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): ViewHolder {
        val binding = ItemMyListingBinding.inflate(LayoutInflater.from(parent.context), parent, false)
        return ViewHolder(binding)
    }

    override fun onBindViewHolder(holder: ViewHolder, position: Int) {
        val property = items[position]
        val binding = holder.binding

        binding.listingTitle.text = property.title
        binding.listingPrice.text = "$${String.format(Locale.US, "%,.0f", property.price)}"
        binding.listingStatus.text = if (property.status == "active") "نشط" else "غير نشط"

        val imageUrl = property.images.firstOrNull()
        if (imageUrl != null) {
            Glide.with(binding.root.context).load(imageUrl).centerCrop().into(binding.listingImage)
        }

        binding.deleteButton.setOnClickListener { onDeleteClick(property) }
    }

    override fun getItemCount(): Int = items.size

    fun updateData(newItems: List<Property>) {
        items = newItems
        notifyDataSetChanged()
    }
}
