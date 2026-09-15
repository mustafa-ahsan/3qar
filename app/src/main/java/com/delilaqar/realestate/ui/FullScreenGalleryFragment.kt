package com.delilaqar.realestate.ui

import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.view.WindowManager
import android.widget.ImageView
import android.widget.TextView
import androidx.fragment.app.DialogFragment
import androidx.viewpager2.widget.ViewPager2
import com.delilaqar.realestate.R

class FullScreenGalleryFragment : DialogFragment() {

    companion object {
        private const val ARG_IMAGES = "images"
        private const val ARG_START_POSITION = "start_position"

        fun newInstance(images: ArrayList<String>, startPosition: Int): FullScreenGalleryFragment {
            val fragment = FullScreenGalleryFragment()
            fragment.arguments = Bundle().apply {
                putStringArrayList(ARG_IMAGES, images)
                putInt(ARG_START_POSITION, startPosition)
            }
            return fragment
        }
    }

    override fun getTheme(): Int = android.R.style.Theme_Black_NoTitleBar_Fullscreen

    override fun onCreateView(
        inflater: LayoutInflater, container: ViewGroup?, savedInstanceState: Bundle?
    ): View {
        return inflater.inflate(R.layout.fragment_fullscreen_gallery, container, false)
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)

        val images = arguments?.getStringArrayList(ARG_IMAGES) ?: arrayListOf()
        val startPosition = (arguments?.getInt(ARG_START_POSITION) ?: 0).coerceIn(0, maxOf(0, images.size - 1))

        val pager = view.findViewById<ViewPager2>(R.id.fullscreenPager)
        val counterText = view.findViewById<TextView>(R.id.fullscreenCounterText)
        val closeButton = view.findViewById<ImageView>(R.id.fullscreenCloseButton)

        pager.adapter = GalleryImageAdapter(images, fullscreen = true)
        pager.setCurrentItem(startPosition, false)
        counterText.text = "${startPosition + 1} / ${images.size}"

        pager.registerOnPageChangeCallback(object : ViewPager2.OnPageChangeCallback() {
            override fun onPageSelected(position: Int) {
                counterText.text = "${position + 1} / ${images.size}"
            }
        })

        closeButton.setOnClickListener { dismiss() }
    }

    override fun onStart() {
        super.onStart()
        dialog?.window?.setLayout(
            WindowManager.LayoutParams.MATCH_PARENT,
            WindowManager.LayoutParams.MATCH_PARENT
        )
    }
}
