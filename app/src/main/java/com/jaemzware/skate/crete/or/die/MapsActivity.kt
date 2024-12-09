package com.jaemzware.skate.crete.or.die
import android.app.AlertDialog
import android.content.ComponentName
import android.content.Context
import android.content.Intent
import android.content.ServiceConnection
import android.content.pm.PackageManager
import androidx.appcompat.app.AppCompatActivity
import android.os.Bundle
import androidx.core.app.ActivityCompat
import com.google.android.gms.maps.CameraUpdateFactory
import com.google.android.gms.maps.GoogleMap
import com.google.android.gms.maps.OnMapReadyCallback
import com.google.android.gms.maps.SupportMapFragment
import com.google.android.gms.maps.model.LatLng
import com.google.android.gms.maps.model.MarkerOptions
import android.graphics.Color
import android.location.Location
import android.os.Handler
import android.os.IBinder
import android.os.Looper
import android.util.Log
import android.view.Gravity
import android.view.View
import android.widget.Button
import android.widget.FrameLayout
import android.widget.LinearLayout
import android.widget.RelativeLayout
import android.widget.TextView
import android.widget.Toast
import com.google.android.gms.maps.model.BitmapDescriptorFactory
import com.google.android.gms.maps.model.Marker
import com.google.android.gms.maps.model.Polyline
import com.google.android.gms.maps.model.PolylineOptions
import com.jaemzware.skatecreteordie.databinding.ActivityMapsBinding
import org.json.JSONException
import org.json.JSONObject
import java.io.BufferedReader
import java.io.InputStreamReader
import java.net.HttpURLConnection
import java.net.URL
import androidx.appcompat.widget.SwitchCompat
import com.google.android.material.snackbar.Snackbar
import com.google.gson.Gson
import com.jaemzware.skatecreteordie.BuildConfig
import com.jaemzware.skatecreteordie.R
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import java.io.IOException

class MapsActivity : AppCompatActivity(), OnMapReadyCallback {
    private lateinit var mMap: GoogleMap
    private lateinit var binding: ActivityMapsBinding
    private lateinit var locationTrackingService: LocationTrackingService
    private lateinit var loadingScreen: FrameLayout
    private var isBound = false
    private var pathPoints = mutableListOf<LatLng>()
    private var polyline: Polyline? = null
    private var currentLocationMarker: Marker? = null
    private val LOCATION_PERMISSION_REQUEST_CODE = 1
    private val BACKGROUND_LOCATION_PERMISSION_REQUEST_CODE = 2
    private var isLocationUpdatesStarted = false
    private var areTrackingControlsEnabled = false
    private lateinit var tvMileage: TextView
    private lateinit var tvElapsedTime: TextView
    private var mileage = 0.0
    private var startTime: Long = 0
    private var elapsedTime: Long = 0
    private var accumulatedElapsedTime: Long = 0
    private var timerHandler = Handler(Looper.getMainLooper())
    private var timerRunnable: Runnable? = null

    val pinImageMap = mapOf(
        "artisanpin" to R.drawable.artisanpin,
        "artisanlightspin" to R.drawable.artisanlightspin,
        "diyparkpin" to R.drawable.diyparkpin,
        "diyparklightspin" to R.drawable.diyparklightspin,
        "dreamlandpin" to R.drawable.dreamlandpin,
        "dreamlandlightspin" to R.drawable.dreamlandlightspin,
        "evergreenpin" to R.drawable.evergreenpin,
        "evergreenlightspin" to R.drawable.evergreenlightspin,
        "fsrbetonpin" to R.drawable.fsrbetonpin,
        "fsrbetonlightspin" to R.drawable.fsrbetonlightspin,
        "grindlinepin" to R.drawable.grindlinepin,
        "grindlinelightspin" to R.drawable.grindlinelightspin,
        "newlinepin" to R.drawable.newlinepin,
        "newlinelightspin" to R.drawable.newlinelightspin,
        "othergoodparkpin" to R.drawable.othergoodparkpin,
        "othergoodparklightspin" to R.drawable.othergoodparklightspin,
        "spohnranchpin" to R.drawable.spohnranchpin,
        "spohnranchlightspin" to R.drawable.spohnranchlightspin,
        "skateparkpin" to R.drawable.skateparkpin,
        "skateparklightspin" to R.drawable.skateparklightspin,
        "spotpin" to R.drawable.spotpin,
        "spotlightspin" to R.drawable.spotlightspin,
        "teampainpin" to R.drawable.teampainpin,
        "teampainlightspin" to R.drawable.teampainlightspin,
        "woodparkpin" to R.drawable.woodparkpin,
        "woodparklightspin" to R.drawable.woodparklightspin
    )
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        binding = ActivityMapsBinding.inflate(layoutInflater)
        setContentView(binding.root)

        // Obtain the SupportMapFragment and get notified when the map is ready to be used.
        val mapFragment = supportFragmentManager
            .findFragmentById(R.id.map) as SupportMapFragment
        mapFragment.getMapAsync(this)

        loadingScreen = findViewById(R.id.loading_screen)

        tvMileage = findViewById(R.id.tvMileage)
        tvElapsedTime = findViewById(R.id.tvElapsedTime)

        val centerButton = findViewById<Button>(R.id.btnCenterOnLocation)
        centerButton.setOnClickListener {
            centerMapOnUserLocation()
        }

        val resetButton = findViewById<Button>(R.id.btnResetPolyline)
        resetButton.setOnClickListener {
            resetPolyline()
        }

        val switchLocationUpdates = findViewById<SwitchCompat>(R.id.switchLocationUpdates)
        switchLocationUpdates.setOnCheckedChangeListener { _, isChecked ->
            if (isChecked) {
                if (ActivityCompat.checkSelfPermission(this, android.Manifest.permission.ACCESS_FINE_LOCATION) == PackageManager.PERMISSION_GRANTED) {
                    if (ActivityCompat.checkSelfPermission(this, android.Manifest.permission.ACCESS_BACKGROUND_LOCATION) == PackageManager.PERMISSION_GRANTED) {
                        // Both permissions are granted, start location updates
                        startLocationUpdates()
                    } else {
                        // ACCESS_BACKGROUND_LOCATION is not granted, show the background location permission dialog
                        showBackgroundLocationPermissionDialog()
                    }
                } else {
                    // ACCESS_FINE_LOCATION is not granted, show the location disclosure dialog
                    showLocationDisclosureDialog()
                }
            } else {
                stopLocationUpdates()
            }
        }

        disableTrackingControls()
    }

    override fun onMapReady(googleMap: GoogleMap) {
        loadingScreen.visibility = View.VISIBLE

        mMap = googleMap
        mMap.isIndoorEnabled = false
        mMap.mapType = GoogleMap.MAP_TYPE_HYBRID

        // Move camera to Seattle
        val seattle = LatLng(47.61123658156927, -122.33720987967835)
        val zoomLevel = 12f // Adjust the zoom level as needed
        mMap.moveCamera(CameraUpdateFactory.newLatLngZoom(seattle, zoomLevel))

        // Load local skatepark data first
        loadLocalSkateparkData()
        // Fetch remote skatepark data
        fetchSkateparkData()

        //USE CUSTOM WINDOW ON PIN TAP
        mMap.setInfoWindowAdapter(CustomInfoWindowAdapter(this))

        //SETUP HANDLER TO GO TO SKATEPARKDETAILS WHEN PIN WINDOW CLICKED
        mMap.setOnInfoWindowClickListener { marker ->
            val skateparkJson = Gson().toJson(marker.tag)

            // Create a new instance of LocationDetailsFragment
            val locationDetailsFragment = LocationDetailsFragment.newInstance(skateparkJson)

            // Replace the current fragment with the LocationDetailsFragment
            supportFragmentManager.beginTransaction()
                .replace(R.id.locationDetailsContainer, locationDetailsFragment)
                .addToBackStack(null)
                .commit()

            // Show the locationDetailsContainer
            findViewById<FrameLayout>(R.id.locationDetailsContainer).visibility = View.VISIBLE
        }

        // Use a GoogleMap.OnMapLoadedCallback to determine when the map is fully loaded
        mMap.setOnMapLoadedCallback {
            loadingScreen.visibility = View.GONE // Hide the loading screen
        }
    }

    override fun onDestroy() {
        super.onDestroy()
        // Stop the foreground service when the activity is destroyed
        stopLocationForegroundService()
    }

    override fun onResume() {
        super.onResume()
        val switchLocationUpdates = findViewById<SwitchCompat>(R.id.switchLocationUpdates)
        if (switchLocationUpdates.isChecked) {
            if (ActivityCompat.checkSelfPermission(this, android.Manifest.permission.ACCESS_FINE_LOCATION) != PackageManager.PERMISSION_GRANTED ||
                ActivityCompat.checkSelfPermission(this, android.Manifest.permission.ACCESS_BACKGROUND_LOCATION) != PackageManager.PERMISSION_GRANTED) {
                // Either ACCESS_FINE_LOCATION or ACCESS_BACKGROUND_LOCATION is not granted, disable tracking controls
                if (isLocationUpdatesStarted) {
                    stopLocationForegroundService()
                }
                switchLocationUpdates.isChecked = false
                disableTrackingControls()
            } else if (ActivityCompat.checkSelfPermission(this, android.Manifest.permission.ACCESS_FINE_LOCATION) == PackageManager.PERMISSION_GRANTED &&
                ActivityCompat.checkSelfPermission(this, android.Manifest.permission.ACCESS_BACKGROUND_LOCATION) == PackageManager.PERMISSION_GRANTED) {
                if (!isLocationUpdatesStarted) {
                    startLocationForegroundService()
                }
            }
        }
    }

    private fun loadLocalSkateparkData() {
        try {
            val inputStream = assets.open("skateparkdata20240303.js")
            val size = inputStream.available()
            val buffer = ByteArray(size)
            inputStream.read(buffer)
            inputStream.close()
            val skateparkData = String(buffer)
            parseAndMapData(skateparkData)
        } catch (e: IOException) {
            e.printStackTrace()
        }
    }

    private fun fetchSkateparkData(retryCount: Int = 15, currentAttempt: Int = 1) {
        Log.d("SKATE.CRETE.OR.DIE", "FETCHBEGIN")

        // Use Kotlin Coroutines for better thread management
        CoroutineScope(Dispatchers.IO).launch {
            Log.d("SKATE.CRETE.OR.DIE", "FETCHTHREADBEGIN")

            try {
                val url = URL(BuildConfig.PARK_DATABASE_URL)
                val connection = withContext(Dispatchers.IO) {
                    (url.openConnection() as HttpURLConnection).apply {
                        // Set timeouts to prevent hanging
                        connectTimeout = 10000
                        readTimeout = 10000
                        requestMethod = "GET"

                        // Add caching support
                        useCaches = true
                        setRequestProperty("Cache-Control", "max-age=3600")
                    }
                }

                val skateparkData = withContext(Dispatchers.IO) {
                    connection.inputStream.bufferedReader().use { it.readText() }
                }

                // Switch to Main thread for UI updates
                withContext(Dispatchers.Main) {
                    parseAndMapData(skateparkData, true)
                }

            } catch (e: Exception) {
                Log.e("SKATE.CRETE.OR.DIE", "FETCHTHREADEXCEPTION", e)

                if (currentAttempt < retryCount) {
                    delay(2000) // Coroutine-friendly delay
                    fetchSkateparkData(retryCount, currentAttempt + 1)
                } else {
                    withContext(Dispatchers.Main) {
                        showSnackbar("Failed to fetch skatepark data after $currentAttempt attempts. Check connection and try restarting.", Toast.LENGTH_LONG)
                    }
                }
            }

            Log.d("SKATE.CRETE.OR.DIE", "FETCHTHREADEND")
        }

        Log.d("SKATE.CRETE.OR.DIE", "FETCHEND")
    }

    private fun parseAndMapData(data: String, isFromNetwork: Boolean = false) {
        // Move to a background thread for parsing
        CoroutineScope(Dispatchers.Default).launch {
            try {
                // Create Gson instance once
                val gson = Gson()

                // Parse JSON more efficiently
                val skateparks = withContext(Dispatchers.Default) {
                    val jsonArray = JSONObject(data).getJSONArray("skateparks")
                    List(jsonArray.length()) { i ->
                        gson.fromJson(jsonArray.getJSONObject(i).toString(), Skatepark::class.java)
                    }
                }

                // Pre-process markers in background
                val markers = skateparks.map { skatepark ->
                    val pinImageResId = pinImageMap[skatepark.pinimage] ?: R.drawable.othergoodparkpin
                    val skateparkLocation = LatLng(skatepark.latitude, skatepark.longitude)
                    val icon = BitmapDescriptorFactory.fromResource(pinImageResId)

                    MarkerOptions()
                        .position(skateparkLocation)
                        .title(skatepark.name)
                        .icon(icon) to skatepark
                }

                // Switch to main thread for map operations
                withContext(Dispatchers.Main) {
                    // Clear map once
                    mMap.clear()

                    // Process in larger batches for better performance
                    val BATCH_SIZE = 100
                    markers.chunked(BATCH_SIZE).forEach { batch ->
                        batch.forEach { (markerOptions, skatepark) ->
                            mMap.addMarker(markerOptions)?.apply {
                                tag = skatepark
                            }
                        }
                        // Small delay between batches to prevent UI freezing
                        delay(10)
                    }

                    if (isFromNetwork) {
                        showSnackbar("Latest skatepark data loaded")
                    }
                }

            } catch (e: JSONException) {
                Log.e("SKATE.CRETE.OR.DIE", "JSON parsing error", e)
                withContext(Dispatchers.Main) {
                    showSnackbar("Error loading skatepark data", Snackbar.LENGTH_LONG)
                }
            }
        }
    }

    private fun showLocationDisclosureDialog() {
        val builder = AlertDialog.Builder(this)
        builder.setTitle("Location Disclosure")
        builder.setMessage("skate.crete.or.die collects location information to track your path through skateparks.\n\n" +
                "It needs location permission set to \"Allow all the time\" in order to track while your phone is locked.\n\n" +
                "skate.crete.or.die does not collect location information once the app is shut down, nor does save or share collected information with 3rd parties.\n\n" +
                "Tracking can be turned off any time using the \"Track\" switch on the bottom of the screen.")
        builder.setPositiveButton("Accept") { _, _ ->
            // User accepted the location disclosure
            requestFineLocationPermissions()
        }
        builder.setNegativeButton("Deny") { _, _ ->
            // User denied the location disclosure
            resetLocationPermissions()
        }
        builder.setCancelable(false)
        builder.show()
    }

    private fun showBackgroundLocationPermissionDialog() {
        val builder = AlertDialog.Builder(this)
        builder.setTitle("Background Location Permission")
        builder.setMessage("To enable tracking, please follow these steps:\n\n" +
                "1. Tap 'Settings' to open the app settings.\n" +
                "2. Tap 'Permissions'.\n" +
                "3. Tap 'Location'.\n" +
                "4. Select 'Allow all the time'.")
        builder.setPositiveButton("Settings") { _, _ ->
            // Open the app settings screen
            val intent = Intent(android.provider.Settings.ACTION_APPLICATION_DETAILS_SETTINGS)
            val uri = android.net.Uri.fromParts("package", packageName, null)
            intent.data = uri
            startActivity(intent)
        }
        builder.setNegativeButton("Cancel") { _, _ ->
            // User canceled, disable tracking controls
            val trackingSwitch = findViewById<SwitchCompat>(R.id.switchLocationUpdates)
            trackingSwitch.isChecked = false
            disableTrackingControls()
        }
        builder.setCancelable(false)
        builder.show()
    }

    private fun requestFineLocationPermissions() {
        if (ActivityCompat.checkSelfPermission(this, android.Manifest.permission.ACCESS_FINE_LOCATION) != PackageManager.PERMISSION_GRANTED) {
            ActivityCompat.requestPermissions(this, arrayOf(android.Manifest.permission.ACCESS_FINE_LOCATION), LOCATION_PERMISSION_REQUEST_CODE)
        } else {
            // Fine location permission is already granted, request background location permission
            requestBackgroundLocationPermission()
        }
    }

    private fun requestBackgroundLocationPermission() {
        if (ActivityCompat.checkSelfPermission(this, android.Manifest.permission.ACCESS_BACKGROUND_LOCATION) != PackageManager.PERMISSION_GRANTED) {
            ActivityCompat.requestPermissions(this, arrayOf(android.Manifest.permission.ACCESS_BACKGROUND_LOCATION), BACKGROUND_LOCATION_PERMISSION_REQUEST_CODE)
        } else {
            startLocationUpdates()
        }
    }

    override fun onRequestPermissionsResult(requestCode: Int, permissions: Array<out String>, grantResults: IntArray) {
        super.onRequestPermissionsResult(requestCode, permissions, grantResults)
        when (requestCode) {
            LOCATION_PERMISSION_REQUEST_CODE, BACKGROUND_LOCATION_PERMISSION_REQUEST_CODE -> {
                if (grantResults.isNotEmpty() && grantResults[0] == PackageManager.PERMISSION_GRANTED) {
                    startLocationUpdates()
                }
            }
        }
    }
    private fun startLocationUpdates() {
        // Check for fine location permission
        if (ActivityCompat.checkSelfPermission(this, android.Manifest.permission.ACCESS_FINE_LOCATION) != PackageManager.PERMISSION_GRANTED) {
            ActivityCompat.requestPermissions(this, arrayOf(android.Manifest.permission.ACCESS_FINE_LOCATION), LOCATION_PERMISSION_REQUEST_CODE)
            return
        }

        // Check for background location permission
        if (ActivityCompat.checkSelfPermission(this, android.Manifest.permission.ACCESS_BACKGROUND_LOCATION) != PackageManager.PERMISSION_GRANTED) {
            ActivityCompat.requestPermissions(this, arrayOf(android.Manifest.permission.ACCESS_BACKGROUND_LOCATION), BACKGROUND_LOCATION_PERMISSION_REQUEST_CODE)
            return
        }

        // Call method to start the location foreground service if not already started
        if (!isLocationUpdatesStarted) {
            startLocationForegroundService()
        }

        if (currentLocationMarker != null) {
            currentLocationMarker?.isVisible = true
        }
    }

    private fun stopLocationUpdates() {
        // Call method to stop the location foreground service
        stopLocationForegroundService()

        if (currentLocationMarker != null) {
            currentLocationMarker?.isVisible = false
        }

        disableTrackingControls()
    }

    private fun startLocationForegroundService() {
        val serviceIntent = Intent(this, LocationTrackingService::class.java)
        startForegroundService(serviceIntent)
        bindService(serviceIntent, serviceConnection, Context.BIND_AUTO_CREATE)
        isLocationUpdatesStarted = true
        showSnackbar("Locking location ...")

        startTime = System.currentTimeMillis() - accumulatedElapsedTime
        if (timerRunnable == null) {
            timerRunnable = object : Runnable {
                override fun run() {
                    elapsedTime = System.currentTimeMillis() - startTime
                    updateMileageAndElapsedTime()
                    timerHandler.postDelayed(this, 1000)
                }
            }
        }
        timerHandler.post(timerRunnable!!)

        val switchLocationUpdates = findViewById<SwitchCompat>(R.id.switchLocationUpdates)
        switchLocationUpdates.isChecked = true
    }

    private fun stopLocationForegroundService() {
        if (isBound) {
            unbindService(serviceConnection)
            isBound = false
        }
        stopService(Intent(this, LocationTrackingService::class.java))
        isLocationUpdatesStarted = false
        showSnackbar("Location services stopped.")

        accumulatedElapsedTime = elapsedTime
        if (timerRunnable != null) {
            timerHandler.removeCallbacks(timerRunnable!!)
        }

        val switchLocationUpdates = findViewById<SwitchCompat>(R.id.switchLocationUpdates)
        switchLocationUpdates.isChecked = false
    }

    private fun resetLocationPermissions() {
        // Stop the location foreground service if it's running
        if (isLocationUpdatesStarted) {
            stopLocationForegroundService()
        }

        // Hide the current location marker if it's visible
        if (currentLocationMarker != null) {
            currentLocationMarker?.isVisible = false
        }

        disableTrackingControls()
    }

    //UPDATEPOLYLINE BINDING TO LOCATIONTRACKINGSERVICE
    private val serviceConnection = object : ServiceConnection {
        override fun onServiceConnected(name: ComponentName?, service: IBinder?) {
            val binder = service as LocationTrackingService.LocalBinder
            locationTrackingService = binder.getService()
            isBound = true
            locationTrackingService.setLocationUpdateListener(object : LocationUpdateListener {
                override fun onLocationUpdated(newLocations: List<LatLng>) {
                    updatePolylineAndMarker(newLocations)
                }
            })
        }

        override fun onServiceDisconnected(name: ComponentName?) {
            isBound = false
        }
    }

    private fun updatePolylineAndMarker(newLocations: List<LatLng>) {
        pathPoints.addAll(newLocations)

        if (polyline != null) {
            polyline?.points = pathPoints
        } else {
            polyline = mMap.addPolyline(PolylineOptions().addAll(pathPoints).width(7f).color(Color.GREEN))
        }

        if(!areTrackingControlsEnabled) {
            enableTrackingControls()
        }

        newLocations.lastOrNull()?.let { updateCurrentLocationMarker(it) }

        if (pathPoints.size >= 2) {
            var totalDistance = 0.0
            for (i in 0 until pathPoints.size - 1) {
                val startPoint = pathPoints[i]
                val endPoint = pathPoints[i + 1]
                totalDistance += calculateDistance(startPoint, endPoint)
            }
            mileage = totalDistance
            updateMileageAndElapsedTime()
        }
    }

    private fun calculateDistance(point1: LatLng, point2: LatLng): Double {
        val results = FloatArray(1)
        Location.distanceBetween(point1.latitude, point1.longitude, point2.latitude, point2.longitude, results)
        return results[0].toDouble()
    }

    private fun updateMileageAndElapsedTime() {
        val miles = mileage * 0.000621371
        val km = mileage * 0.001
        tvMileage.text = String.format("%.4f mi / %.4f km", miles, km)

        val hours = elapsedTime / 3600000
        val minutes = (elapsedTime % 3600000) / 60000
        val seconds = (elapsedTime % 60000) / 1000
        tvElapsedTime.text = String.format("%02d:%02d:%02d", hours, minutes, seconds)
    }

    private fun updateCurrentLocationMarker(newLocation: LatLng) {
        if (currentLocationMarker == null) {
            currentLocationMarker = mMap.addMarker(MarkerOptions()
                .position(newLocation)
                .title("Current Location")
                .icon(BitmapDescriptorFactory.defaultMarker(BitmapDescriptorFactory.HUE_CYAN)))
        } else {
            currentLocationMarker?.position = newLocation
        }
    }

    private fun centerMapOnUserLocation() {
        currentLocationMarker?.position?.let {
            mMap.moveCamera(CameraUpdateFactory.newLatLngZoom(it, 20f))
        }
    }

    private fun resetPolyline() {
        polyline?.remove()
        pathPoints.clear()
        polyline = null // Now we explicitly set it to null to indicate it should be recreated
        mileage = 0.0
        accumulatedElapsedTime = 0
        elapsedTime = 0
        startTime = System.currentTimeMillis()
        updateMileageAndElapsedTime()
    }

    private fun enableTrackingControls() {
        val centerButton = findViewById<Button>(R.id.btnCenterOnLocation)
        centerButton.setTextColor(Color.WHITE)
        centerButton.isEnabled = true
        val resetButton = findViewById<Button>(R.id.btnResetPolyline)
        resetButton.setTextColor(Color.WHITE)
        resetButton.isEnabled = true
        areTrackingControlsEnabled = true
    }

    private fun disableTrackingControls() {
        val centerButton = findViewById<Button>(R.id.btnCenterOnLocation)
        centerButton.isEnabled = false
        centerButton.setTextColor(Color.DKGRAY)
        val resetButton = findViewById<Button>(R.id.btnResetPolyline)
        resetButton.isEnabled = false
        resetButton.setTextColor(Color.DKGRAY)
        areTrackingControlsEnabled = false
        // Stop the location foreground service if it's running
        if (isLocationUpdatesStarted) {
            stopLocationForegroundService()
        }
    }

    private fun showSnackbar(message: String, duration: Int = Snackbar.LENGTH_SHORT) {
        val rootView = findViewById<View>(android.R.id.content)
        val snackbar = Snackbar.make(rootView, message, duration)

        // Find the mapcontrols LinearLayout in your layout
        val mapControls = findViewById<LinearLayout>(R.id.mapcontrols)

        // Set the anchor view to the mapcontrols LinearLayout
        snackbar.setAnchorView(mapControls)

        // Adjust the position of the Snackbar
        val params = snackbar.view.layoutParams as FrameLayout.LayoutParams
        params.gravity = Gravity.BOTTOM or Gravity.CENTER_HORIZONTAL
        val marginBottom = 16 // Set the bottom margin to 16 pixels (adjust as needed)
        params.bottomMargin = marginBottom
        snackbar.view.layoutParams = params

        snackbar.show()
    }
}