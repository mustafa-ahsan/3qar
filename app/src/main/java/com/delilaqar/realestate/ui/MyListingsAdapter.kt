package com.delilaqar.realestate.ui

import android.view.LayoutInflater
import android.view.ViewGroup
import androidx.recyclerview.widget.RecyclerView
import android.widget.ImageView
import androidx.core.content.ContextCompat
import com.bumptech.glide.Glide
import com.delilaqar.realestate.data.Property
import com.delilaqar.realestate.databinding.ItemMyListingBinding
import com.delilaqar.realestate.util.CurrencyFormatter

class MyListingsAdapter(
    private var items: List<Property>,
    private val onDeleteClick: (Property) -> Unit,
    private val onEditClick: (Property) -> Unit
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
        binding.listingPrice.text = CurrencyFormatter.format(property.price)
        binding.listingStatus.text = if (property.status == "active") "نشط" else "غير نشط"

        val context = binding.root.context
        val imageUrl = property.images.firstOrNull()
        if (imageUrl != null) {
            binding.listingImage.visibility = android.view.View.VISIBLE
            binding.listingImage.scaleType = ImageView.ScaleType.CENTER_CROP
            Glide.with(context).load(imageUrl).centerCrop().into(binding.listingImage)
        } else {
            Glide.with(context).clear(binding.listingImage)
            binding.listingImage.visibility = android.view.View.GONE
        }

        binding.deleteButton.setOnClickListener { onDeleteClick(property) }
        binding.editButton.setOnClickListener { onEditClick(property) }
    }

    override fun getItemCount(): Int = items.size

    fun updateData(newItems: List<Property>) {
        items = newItems
        notifyDataSetChanged()
    }
}
