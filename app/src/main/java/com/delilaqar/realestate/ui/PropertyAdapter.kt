package com.delilaqar.realestate.ui

import android.content.res.ColorStateList
import android.view.LayoutInflater
import android.view.ViewGroup
import android.widget.ImageView
import androidx.core.content.ContextCompat
import androidx.recyclerview.widget.RecyclerView
import com.bumptech.glide.Glide
import com.delilaqar.realestate.R
import com.delilaqar.realestate.data.Property
import com.delilaqar.realestate.databinding.ItemPropertyBinding
import com.delilaqar.realestate.util.CurrencyFormatter
import java.util.Locale

class PropertyAdapter(
    private var items: List<Property>,
    private val onDetailsClick: (Property) -> Unit,
    private val onWhatsappClick: (Property) -> Unit,
    private val onFavoriteClick: (Property) -> Unit,
    private var favoriteIds: Set<String> = emptySet()
) : RecyclerView.Adapter<PropertyAdapter.PropertyViewHolder>() {

    inner class PropertyViewHolder(val binding: ItemPropertyBinding) :
        RecyclerView.ViewHolder(binding.root)

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): PropertyViewHolder {
        val binding = ItemPropertyBinding.inflate(
            LayoutInflater.from(parent.context), parent, false
        )
        return PropertyViewHolder(binding)
    }

    override fun onBindViewHolder(holder: PropertyViewHolder, position: Int) {
        val property = items[position]
        val binding = holder.binding
        val context = binding.root.context

        binding.titleText.text = property.title
        binding.priceText.text = CurrencyFormatter.format(property.price)
        binding.locationText.text = property.district
        binding.detailsText.text =
            "${property.bedrooms} غرف · ${property.bathrooms} حمامات · ${property.area.toInt()} م²"

        binding.propertyTypeBadge.text = propertyTypeLabel(property.propertyType)

        when (property.listingType) {
            "rent" -> {
                binding.listingTypeBadge.text = context.getString(R.string.badge_rent)
                binding.listingTypeBadge.backgroundTintList =
                    ColorStateList.valueOf(ContextCompat.getColor(context, R.color.accent_green))
            }
            "wanted" -> {
                binding.listingTypeBadge.text = "مطلوب"
                binding.listingTypeBadge.backgroundTintList =
                    ColorStateList.valueOf(ContextCompat.getColor(context, R.color.accent_orange))
            }
            else -> {
                binding.listingTypeBadge.text = context.getString(R.string.badge_sale)
                binding.listingTypeBadge.backgroundTintList =
                    ColorStateList.valueOf(ContextCompat.getColor(context, R.color.primary_blue))
            }
        }

        val isFavorite = favoriteIds.contains(property.id)
        binding.favoriteIcon.setImageResource(
            if (isFavorite) R.drawable.ic_favorite else R.drawable.ic_favorite_outline
        )

        val imageUrl = property.images.firstOrNull()
        if (imageUrl != null) {
            binding.propertyImage.background = null
            binding.propertyImage.scaleType = ImageView.ScaleType.CENTER_CROP
            Glide.with(context).load(imageUrl).centerCrop().into(binding.propertyImage)
        } else {
            Glide.with(context).clear(binding.propertyImage)
            binding.propertyImage.scaleType = ImageView.ScaleType.CENTER
            binding.propertyImage.setBackgroundColor(ContextCompat.getColor(context, R.color.surface_card_light))
            binding.propertyImage.setImageResource(R.drawable.ic_no_image)
        }

        binding.detailsButton.setOnClickListener { onDetailsClick(property) }
        binding.whatsappButton.setOnClickListener { onWhatsappClick(property) }
        binding.favoriteIcon.setOnClickListener { onFavoriteClick(property) }
    }

    override fun getItemCount(): Int = items.size

    fun updateData(newItems: List<Property>) {
        items = newItems
        notifyDataSetChanged()
    }

    fun updateFavorites(newFavorites: Set<String>) {
        favoriteIds = newFavorites
        notifyDataSetChanged()
    }

    private fun propertyTypeLabel(type: String): String = when (type) {
        "apartment" -> "شقة"
        "villa" -> "فيلا"
        "land" -> "أرض"
        "commercial" -> "تجاري ومكاتب"
        "duplex" -> "دوبلكس"
        "chalet" -> "شاليه"
        "full_building" -> "عمارة كاملة"
        else -> type
    }
}
