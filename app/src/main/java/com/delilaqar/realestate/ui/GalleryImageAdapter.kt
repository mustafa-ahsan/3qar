package com.delilaqar.realestate.ui

import android.view.LayoutInflater
import android.view.ViewGroup
import android.widget.ImageView
import androidx.core.content.ContextCompat
import androidx.recyclerview.widget.RecyclerView
import com.bumptech.glide.Glide
import com.delilaqar.realestate.R

class GalleryImageAdapter(
    private val imageUrls: List<String>,
    private val fullscreen: Boolean = false,
    private val onImageClick: ((Int) -> Unit)? = null
) : RecyclerView.Adapter<GalleryImageAdapter.ImageViewHolder>() {

    inner class ImageViewHolder(val imageView: ImageView) : RecyclerView.ViewHolder(imageView)

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): ImageViewHolder {
        val view = LayoutInflater.from(parent.context)
            .inflate(R.layout.item_gallery_image, parent, false) as ImageView
        return ImageViewHolder(view)
    }

    override fun onBindViewHolder(holder: ImageViewHolder, position: Int) {
        val url = imageUrls[position]
        val imageView = holder.imageView
        val context = imageView.context

        if (url.isBlank()) {
            Glide.with(context).clear(imageView)
            imageView.scaleType = ImageView.ScaleType.CENTER
            imageView.setBackgroundColor(ContextCompat.getColor(context, R.color.surface_card_light))
            imageView.setImageResource(R.drawable.ic_no_image)
        } else {
            imageView.background = null
            imageView.scaleType = if (fullscreen) ImageView.ScaleType.FIT_CENTER else ImageView.ScaleType.CENTER_CROP
            val request = Glide.with(context).load(url)
            if (fullscreen) request.into(imageView) else request.centerCrop().into(imageView)
        }

        imageView.setOnClickListener { onImageClick?.invoke(position) }
    }

    override fun getItemCount(): Int = imageUrls.size
}
