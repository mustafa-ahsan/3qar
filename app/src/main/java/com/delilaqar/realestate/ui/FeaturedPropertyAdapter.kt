package com.delilaqar.realestate.ui

import android.view.LayoutInflater
import android.view.ViewGroup
import androidx.recyclerview.widget.RecyclerView
import android.widget.ImageView
import androidx.core.content.ContextCompat
import com.bumptech.glide.Glide
import com.delilaqar.realestate.data.Property
import com.delilaqar.realestate.databinding.ItemPropertyFeaturedBinding
import com.delilaqar.realestate.util.CurrencyFormatter
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
        binding.priceText.text = CurrencyFormatter.format(property.price)
        binding.locationText.text = property.district

        val context = binding.root.context
        val imageUrl = property.images.firstOrNull()
        if (imageUrl != null) {
            binding.propertyImage.background = null
            binding.propertyImage.scaleType = ImageView.ScaleType.CENTER_CROP
            Glide.with(context).load(imageUrl).centerCrop().into(binding.propertyImage)
        } else {
            Glide.with(context).clear(binding.propertyImage)
            binding.propertyImage.scaleType = ImageView.ScaleType.CENTER
            binding.propertyImage.setBackgroundColor(ContextCompat.getColor(context, com.delilaqar.realestate.R.color.surface_card_light))
            binding.propertyImage.setImageResource(com.delilaqar.realestate.R.drawable.ic_no_image)
        }

        binding.root.setOnClickListener { onClick(property) }
    }

    override fun getItemCount(): Int = items.size

    fun updateData(newItems: List<Property>) {
        items = newItems
        notifyDataSetChanged()
    }
}
