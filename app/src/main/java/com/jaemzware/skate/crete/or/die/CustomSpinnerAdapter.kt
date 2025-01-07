package com.jaemzware.skate.crete.or.die

import android.content.Context
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.ArrayAdapter
import android.widget.ImageView
import android.widget.TextView
import com.jaemzware.skatecreteordie.R

class CustomSpinnerAdapter(context: Context, pinTypes: List<String>, private val pinImageMap: Map<String, Int>) :
    ArrayAdapter<String>(context, 0, pinTypes) {

    override fun getView(position: Int, convertView: View?, parent: ViewGroup): View {
        return createItemView(position, convertView, parent)
    }

    override fun getDropDownView(position: Int, convertView: View?, parent: ViewGroup): View {
        return createItemView(position, convertView, parent)
    }

    private fun createItemView(position: Int, convertView: View?, parent: ViewGroup): View {
        val view = convertView ?: LayoutInflater.from(context).inflate(R.layout.custom_spinner_item, parent, false)
        val pinType = getItem(position)

        val pinImage = view.findViewById<ImageView>(R.id.pinImage)
        val pinTypeText = view.findViewById<TextView>(R.id.pinTypeText)

        val imageResource = if (pinType == "All") {
            // Set a default image for the "All" filter
            null
        } else {
            pinImageMap[pinType] ?: R.drawable.othergoodparkpin
        }

        // Set the image resource or make the ImageView invisible
        if (imageResource != null) {
            pinImage.setImageResource(imageResource)
            pinImage.visibility = View.VISIBLE
        } else {
            pinImage.visibility = View.GONE
        }

        pinTypeText.text = pinType

        return view
    }
}