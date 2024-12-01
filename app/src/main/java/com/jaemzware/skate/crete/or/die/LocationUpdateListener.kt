package com.jaemzware.skate.crete.or.die

import com.google.android.gms.maps.model.LatLng

interface LocationUpdateListener {
    fun onLocationUpdated(newLocations: List<LatLng>)
}