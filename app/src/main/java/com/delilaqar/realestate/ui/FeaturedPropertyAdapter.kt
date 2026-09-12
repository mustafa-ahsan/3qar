package com.delilaqar.realestate.ui

import android.view.LayoutInflater
import android.view.ViewGroup
import androidx.recyclerview.widget.RecyclerView
import com.bumptech.glide.Glide
import com.delilaqar.realestate.data.Property
import com.delilaqar.realestate.databinding.ItemPropertyFeaturedBinding
import java.util.Locale

class FeaturedPropertyAdapter(
    private var items: List<Property>,
    private val onClick: (Property) -> Unit
) : RecyclerView.Adapter<FeaturedPropertyAdapter.FeaturedViewHolder>() {

    inner class FeaturedViewHolder(val binding: ItemPropertyFeaturedBinding) :
        RecyclerView.ViewHolder(binding.root)

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): FeaturedViewHolder {
        val binding = ItemPropertyFeaturedBinding.inflate(
            LayoutInflater.from(parent.context), parent, false
        )
        return FeaturedViewHolder(binding)
    }

    override fun onBindViewHolder(holder: FeaturedViewHolder, position: Int) {
        val property = items[position]
        val binding = holder.binding

        binding.titleText.text = property.title
        binding.priceText.text = "$${String.format(Locale.US, "%,.0f", property.price)}"
        binding.locationText.text = property.district

        val imageUrl = property.images.firstOrNull()
        if (imageUrl != null) {
            Glide.with(binding.root.context).load(imageUrl).centerCrop().into(binding.propertyImage)
        }

        binding.root.setOnClickListener { onClick(property) }
    }

    override fun getItemCount(): Int = items.size

    fun updateData(newItems: List<Property>) {
        items = newItems
        notifyDataSetChanged()
    }
}
