package com.jaemzware.skate.crete.or.die

import android.content.Context
import android.graphics.drawable.Drawable
import android.os.Handler
import android.os.Looper
import android.view.LayoutInflater
import android.view.View
import android.widget.ImageView
import android.widget.TextView
import com.bumptech.glide.Glide
import com.bumptech.glide.load.DataSource
import com.bumptech.glide.load.engine.GlideException
import com.bumptech.glide.request.RequestListener
import com.bumptech.glide.request.target.Target
import com.google.android.gms.maps.GoogleMap
import com.google.android.gms.maps.model.Marker
import com.jaemzware.skatecreteordie.BuildConfig
import com.jaemzware.skatecreteordie.R

class CustomInfoWindowAdapter(private val context: Context) : GoogleMap.InfoWindowAdapter {
    private val contents: View = LayoutInflater.from(context).inflate(R.layout.custom_info_window, null)
    private var selectedMarker: Marker? = null

    override fun getInfoWindow(marker: Marker): View? {
        // Use default frame
        return null
    }

    override fun getInfoContents(marker: Marker): View {
        // Set the title
        val title = contents.findViewById<TextView>(R.id.title)
        title.text = marker.title

        // Retrieve the Skatepark object from the marker's tag
        val skatepark = marker.tag as? Skatepark
        val imageView = contents.findViewById<ImageView>(R.id.image)

        // Check if the selected marker has changed or if the image is not loaded
        if (selectedMarker != marker || imageView.drawable == null) {
            // Load the image associated with the current marker using Glide
            selectedMarker = marker // Update the selected marker

            val photoFilenames = skatepark?.photos?.split(" ")
            val firstPhotoUrl = photoFilenames?.firstOrNull()

            if (!firstPhotoUrl.isNullOrEmpty()) {
                val fullImageUrl = if (firstPhotoUrl.startsWith("http://") || firstPhotoUrl.startsWith("https://")) {
                    firstPhotoUrl
                } else {
                    BuildConfig.IMAGES_BASE_URL + firstPhotoUrl
                }
                Glide.with(context)
                    .load(fullImageUrl)
                    .listener(object : RequestListener<Drawable> {
                        override fun onLoadFailed(
                            e: GlideException?,
                            model: Any?,
                            target: Target<Drawable>,
                            isFirstResource: Boolean
                        ): Boolean {
                            e?.printStackTrace()
                            return false
                        }

                        override fun onResourceReady(
                            resource: Drawable,  // Changed from Drawable? to Drawable (non-null)
                            model: Any,          // Changed from Any? to Any (non-null)
                            target: Target<Drawable>,  // This stays the same
                            dataSource: DataSource,    // Changed from DataSource? to DataSource (non-null)
                            isFirstResource: Boolean
                        ): Boolean {
                            Handler(Looper.getMainLooper()).post {
                                if (marker.isInfoWindowShown) {
                                    marker.hideInfoWindow()
                                    marker.showInfoWindow()
                                }
                            }
                            return false
                        }
                    })
                    .into(imageView)
            }
        }

        return contents
    }
}