package com.delilaqar.realestate.ui

import android.view.LayoutInflater
import android.view.ViewGroup
import android.widget.ImageView
import androidx.recyclerview.widget.RecyclerView
import com.bumptech.glide.Glide
import com.delilaqar.realestate.R

class GalleryImageAdapter(
    private val imageUrls: List<String>,
    private val onImageClick: ((Int) -> Unit)? = null
) : RecyclerView.Adapter<GalleryImageAdapter.ImageViewHolder>() {

    inner class ImageViewHolder(val imageView: ImageView) : RecyclerView.ViewHolder(imageView)

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): ImageViewHolder {
        val view = LayoutInflater.from(parent.context)
            .inflate(R.layout.item_gallery_image, parent, false) as ImageView
        return ImageViewHolder(view)
    }

    override fun onBindViewHolder(holder: ImageViewHolder, position: Int) {
        Glide.with(holder.imageView.context)
            .load(imageUrls[position])
            .centerCrop()
            .into(holder.imageView)

        holder.imageView.setOnClickListener { onImageClick?.invoke(position) }
    }

    override fun getItemCount(): Int = imageUrls.size
}
