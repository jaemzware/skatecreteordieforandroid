package com.jaemzware.skate.crete.or.die

import android.content.ClipData
import android.content.ClipboardManager
import android.content.Context.CLIPBOARD_SERVICE
import android.content.Intent
import android.net.Uri
import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.Button
import android.widget.FrameLayout
import android.widget.ImageView
import android.widget.LinearLayout
import android.widget.TextView
import android.widget.Toast
import androidx.fragment.app.Fragment
import com.bumptech.glide.Glide
import com.google.gson.Gson
import com.jaemzware.skatecreteordie.BuildConfig
import com.jaemzware.skatecreteordie.R

class LocationDetailsFragment : Fragment() {
    private lateinit var skateparkAddressTextView: TextView
    private lateinit var clipboardManager: ClipboardManager
    companion object {
        fun newInstance(skateparkJson: String): LocationDetailsFragment {
            val fragment = LocationDetailsFragment()
            val args = Bundle()
            args.putString("skateparkData", skateparkJson)
            fragment.arguments = args
            return fragment
        }
    }

    override fun onCreateView(inflater: LayoutInflater, container: ViewGroup?, savedInstanceState: Bundle?): View? {
        val view = inflater.inflate(R.layout.fragment_location_details, container, false)

        // Retrieve the skateparkData from the fragment arguments
        val skateparkJson = arguments?.getString("skateparkData")

        // Parse the skateparkJson and populate the UI with the data
        val skatepark = Gson().fromJson(skateparkJson, Skatepark::class.java)
        skateparkAddressTextView = view.findViewById<TextView>(R.id.skateparkAddress)
        // Update the UI elements with the skatepark data
        view.findViewById<TextView>(R.id.skateparkName).text = skatepark.name
        skateparkAddressTextView.text = skatepark.address
        view.findViewById<TextView>(R.id.skateparkBuilder).text = getString(R.string.builder_with_placeholder, skatepark.builder)
        view.findViewById<TextView>(R.id.skateparkSize).text = getString(R.string.size_with_placeholder, skatepark.sqft)
        view.findViewById<TextView>(R.id.skateparkLights).text = getString(R.string.lights_with_placeholder, skatepark.lights)
        view.findViewById<TextView>(R.id.skateparkCovered).text = getString(R.string.covered_with_placeholder, skatepark.covered)

        // Set click listener for the close button
        view.findViewById<Button>(R.id.closeButton).setOnClickListener {
            closeFragment()
        }

        // Set click listener for the directions button
        view.findViewById<Button>(R.id.directionsButton).setOnClickListener {
            showDirectionsToSkatepark(skatepark.latitude, skatepark.longitude)
        }

        // Set click listener for the website button
        view.findViewById<Button>(R.id.websiteButton).setOnClickListener {
            val urls = skatepark.url.split("\\n".toRegex())
            if (urls.isNotEmpty()) {
                openSkateparkWebsite(urls.first())
            }
        }

        // Set click listener for the map button
        view.findViewById<Button>(R.id.mapButton).setOnClickListener {
            val mapurl = "https://www.google.com/search?q="+skatepark.latitude+"%2C"+skatepark.longitude
            openSkateparkWebsite(mapurl)
        }

        // Load and display skatepark images
        val skateparkImage: ImageView = view.findViewById(R.id.skateparkImage)
        loadSkateparkImages(skatepark.photos, skateparkImage)

        //make address copy-able
        clipboardManager = requireContext().getSystemService(CLIPBOARD_SERVICE) as ClipboardManager

        skateparkAddressTextView.setOnLongClickListener {
            val clipData = ClipData.newPlainText("Skatepark Address", skateparkAddressTextView.text)
            clipboardManager.setPrimaryClip(clipData)
            Toast.makeText(requireContext(), "Address copied to clipboard", Toast.LENGTH_SHORT).show()
            true
        }

        //close this fragment when user taps outside any of its views
        val fragmentContainer = view.findViewById<FrameLayout>(R.id.fragmentContainer)
        val detailsContainer = view.findViewById<LinearLayout>(R.id.detailsContainer)

        fragmentContainer.setOnClickListener { clickedView ->
            if (!isViewClicked(detailsContainer, clickedView)) {
                // Close the fragment
                closeFragment()
            }
        }

        return view
    }

    private fun isViewClicked(view: View, clickedView: View): Boolean {
        val location = IntArray(2)
        view.getLocationOnScreen(location)
        val left = location[0]
        val top = location[1]
        val right = left + view.width
        val bottom = top + view.height

        val clickedLocation = IntArray(2)
        clickedView.getLocationOnScreen(clickedLocation)
        val clickedX = clickedLocation[0]
        val clickedY = clickedLocation[1]

        return clickedX < left || clickedX > right || clickedY < top || clickedY > bottom
    }

    private fun closeFragment() {
        // Remove the LocationDetailsFragment from the back stack
        requireActivity().supportFragmentManager.popBackStack()
    }

    private fun showDirectionsToSkatepark(lat: Double, lng: Double) {
        val gmmIntentUri = Uri.parse("google.navigation:q=$lat,$lng")
        val mapIntent = Intent(Intent.ACTION_VIEW, gmmIntentUri)
        mapIntent.setPackage("com.google.android.apps.maps")

        if (mapIntent.resolveActivity(requireActivity().packageManager) != null) {
            startActivity(mapIntent)
        } else {
            // Google Maps app is not installed, inform the user or provide alternatives
            Toast.makeText(requireContext(), "Google Maps app is not installed", Toast.LENGTH_SHORT).show()
        }
    }

    private fun openSkateparkWebsite(url: String) {

        // Check if there are any URLs available
        if (url.isNotEmpty()) {
            // Create an intent to open the URL in the default web browser
            val intent = Intent(Intent.ACTION_VIEW)
            intent.setData(Uri.parse(url))
            startActivity(intent)
        } else {
            // No URLs available, inform the user or handle accordingly
            Toast.makeText(requireContext(), "No website information available", Toast.LENGTH_SHORT).show()
        }
    }

    private fun loadSkateparkImages(photos: String, imageView: ImageView) {
        // Split the photos string into an array of photo filenames
        val photoFilenames = photos.split(" ")

        // Check if there are any photo filenames
        if (photoFilenames.isNotEmpty()) {
            // Initialize the current photo index
            var currentPhotoIndex = 0

            // Function to load the photo at the current index
            fun loadPhoto() {
                val photoUrl = BuildConfig.IMAGES_BASE_URL + photoFilenames[currentPhotoIndex]

                // Load the photo using Glide or Picasso library
                Glide.with(requireContext())
                    .load(photoUrl)
                    .into(imageView)
            }

            // Load the first photo initially
            loadPhoto()

            // Set up a click listener to cycle through the photos
            imageView.setOnClickListener {
                // Increment the current photo index
                currentPhotoIndex++

                // Check if the current photo index exceeds the number of photos
                if (currentPhotoIndex >= (photoFilenames.size-1)) {
                    // Reset the current photo index to 0 to start from the beginning
                    currentPhotoIndex = 0
                }

                // Load the photo at the updated index
                loadPhoto()
            }
        } else {
            Glide.with(requireContext())
                .load(R.drawable.othergoodparklightspin)
                .into(imageView)
        }
    }
}