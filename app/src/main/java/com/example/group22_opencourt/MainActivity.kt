package com.example.group22_opencourt

import android.Manifest
import android.content.pm.PackageManager
import android.location.Location
import android.location.LocationManager
import android.location.LocationListener
import android.os.Build
import android.os.Bundle
import androidx.activity.OnBackPressedCallback
import androidx.activity.enableEdgeToEdge
import androidx.appcompat.app.AppCompatActivity
import androidx.core.app.ActivityCompat
import androidx.core.content.ContextCompat
import androidx.fragment.app.Fragment
import androidx.navigation.NavController
import androidx.navigation.fragment.NavHostFragment
import androidx.navigation.ui.setupWithNavController
import com.example.group22_opencourt.databinding.ActivityMainBinding


class MainActivity : AppCompatActivity(), LocationListener {

    private lateinit var binding: ActivityMainBinding
    private val PERMISSION_REQUEST_CODE = 0
    private lateinit var locationManager: LocationManager
    var currentLocation: Location? = null
    private lateinit var navController: NavController

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        binding = ActivityMainBinding.inflate(layoutInflater)
        setContentView(binding.root)
        enableEdgeToEdge()

        checkPermissions()

//        // Set up NavController from NavHostFragment
        val navHostFragment = supportFragmentManager.findFragmentById(R.id.nav_host_fragment) as NavHostFragment
        navController = navHostFragment.navController
//
        // Set up bottom navigation with NavController
        binding.bottomNav.setupWithNavController(navController)

        // Update toolbar title on destination change
        navController.addOnDestinationChangedListener { _, destination, _ ->
            binding.toolbar.title = when (destination.id) {
                R.id.homeFragment -> "Home"
                R.id.mapFragment -> "Map"
                R.id.addCourtFragment -> "Add Court"
                R.id.settingsFragment -> "Settings"
                R.id.courtDetailFragment -> "Court Detail"
                else -> "OpenCourt"
            }
        }
    }

    // Location Stuff

    // Initialize the location manager and request location updates
    private fun initLocationManager() {
        // Get the location manager
        try {
            locationManager = getSystemService(LOCATION_SERVICE) as LocationManager
            // Check if GPS provider is enabled
            if(!locationManager.isProviderEnabled(LocationManager.GPS_PROVIDER)) return
            // Get the last known location and update the UI
            val location = locationManager.getLastKnownLocation(
                LocationManager.GPS_PROVIDER)
            if (location != null) onLocationChanged(location)
            // Request location updates
            locationManager.requestLocationUpdates(
                LocationManager.GPS_PROVIDER, 0, 0f, this)
            // Catch security exception if permission is not granted
        } catch (e: SecurityException) {
        }
    }

    override fun onLocationChanged(location: Location) {
        currentLocation = location
        // Get current fragment from NavHostFragment and update location if applicable
        val navHostFragment = supportFragmentManager.findFragmentById(R.id.nav_host_fragment) as? NavHostFragment
        val currentFragment = navHostFragment?.childFragmentManager?.primaryNavigationFragment
        when (currentFragment) {
            is com.example.group22_opencourt.ui.main.MapFragment -> currentFragment.updateUserLocation(location)
            is com.example.group22_opencourt.ui.main.HomeFragment -> currentFragment.updateUserLocation(location)
        }
    }

    // Remove location updates when the activity is destroyed
    override fun onDestroy() {
        super.onDestroy()
        // Remove location updates to prevent memory leaks
        if (locationManager != null)
            locationManager.removeUpdates(this)
    }

    fun checkPermissions() {
        if (Build.VERSION.SDK_INT < 23) return
        // Check if we have location permission
        if (ContextCompat.checkSelfPermission(this, Manifest.permission.ACCESS_FINE_LOCATION) !=
            PackageManager.PERMISSION_GRANTED) ActivityCompat.requestPermissions(this, arrayOf(
            Manifest.permission.ACCESS_FINE_LOCATION), 0)
        else
            initLocationManager()
    }

    override fun onRequestPermissionsResult(requestCode: Int, permissions: Array<String>, grantResults: IntArray) {
        super.onRequestPermissionsResult(requestCode, permissions, grantResults)
        // check if permission is granted
        if (requestCode == PERMISSION_REQUEST_CODE) {
            if (grantResults[0] == PackageManager.PERMISSION_GRANTED) initLocationManager()
        }
    }
    fun showCourtDetail(documentId: String) {
        // Example navigation to CourtDetailFragment with argument
        val bundle = Bundle().apply { putString("documentId", documentId) }
        navController.navigate(R.id.courtDetailFragment, bundle)
    }
}