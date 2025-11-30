package com.example.group22_opencourt

import android.Manifest
import android.content.Intent
import android.content.pm.PackageManager
import android.location.Location
import android.location.LocationManager
import android.location.LocationListener
import android.os.Build
import android.os.Bundle
import android.util.Log
import android.view.View.GONE
import android.view.View.VISIBLE
import androidx.activity.OnBackPressedCallback
import androidx.activity.enableEdgeToEdge
import androidx.activity.result.contract.ActivityResultContracts
import androidx.appcompat.app.AppCompatActivity
import androidx.core.app.ActivityCompat
import androidx.core.content.ContextCompat
import androidx.fragment.app.Fragment
import androidx.navigation.NavController
import androidx.navigation.NavOptions
import androidx.navigation.fragment.NavHostFragment
import androidx.navigation.ui.setupWithNavController
import androidx.lifecycle.MutableLiveData
import androidx.navigation.findNavController

import com.example.group22_opencourt.databinding.ActivityMainBinding
import com.example.group22_opencourt.model.UserRepository
import com.google.android.libraries.places.api.Places
import com.google.firebase.auth.FirebaseAuth
import org.w3c.dom.Text


class MainActivity : AppCompatActivity(), LocationListener {

    private lateinit var binding: ActivityMainBinding
    private val PERMISSION_REQUEST_CODE = 0
    private lateinit var locationManager: LocationManager
    var currentLocation: Location? = null
    private lateinit var navController: NavController

    // LiveData to expose location updates
    val currentLocationLiveData = MutableLiveData<Location>()

    private val locationPermissionLauncher =
        registerForActivityResult(ActivityResultContracts.RequestMultiplePermissions()) { permissions ->
            val fineGranted = permissions[Manifest.permission.ACCESS_FINE_LOCATION] ?: false
            val coarseGranted = permissions[Manifest.permission.ACCESS_COARSE_LOCATION] ?: false

            if (fineGranted || coarseGranted) {
                Log.d("location", "called from main")
                initLocationManager()
            }
        }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        binding = ActivityMainBinding.inflate(layoutInflater)
        setContentView(binding.root)
        enableEdgeToEdge()
        val user = FirebaseAuth.getInstance().currentUser
        if (user == null) {
            val loginIntent : Intent = Intent(this, LoginActivity::class.java)
            startActivity(loginIntent)
            finish()
        } else {
            checkPermissions()
            UserRepository.instance.listenCurrentUser()
            CourtRepository.instance.listenCourtsByCity("")
        }
        //        // Set up NavController from NavHostFragment
        val navHostFragment = supportFragmentManager.findFragmentById(R.id.nav_host_fragment) as NavHostFragment
        navController = navHostFragment.navController
//
        // Set up bottom navigation with NavController
        binding.bottomNav.setupWithNavController(navController)

        navController.addOnDestinationChangedListener { controller, destination, arguments ->
            // This runs on every navigation event
            hideToolBarButton()
        }

        binding.bottomNav.setOnItemSelectedListener { item ->
            val currentDestId = navController.currentDestination?.id

            // If the tapped tab is already selected, do nothing
            if (item.itemId == currentDestId) {
                return@setOnItemSelectedListener true
            }
            hideBackButton()
            when (item.itemId) {
                R.id.homeFragment -> {
                    // Always go back to the base firstFragment, clearing anything above it
                    val options = NavOptions.Builder()
                        .setPopUpTo(R.id.homeFragment, inclusive = false)
                        .build()
                    var args1 : Bundle? = null
                    val location = currentLocation
                    if (location != null) {
                        args1 = Bundle().apply {
                            putDouble("latitude", location.latitude)
                            putDouble("longitude", location.longitude)
                        }
                    }
                    navController.navigate(R.id.homeFragment, args1, options)
                    true
                }
                R.id.mapFragment -> {
                    navController.navigate(R.id.mapFragment)
                    true
                }
                R.id.addCourtFragment -> {
                    navController.navigate(R.id.addCourtFragment)
                    true
                }
                R.id.settingsFragment -> {
                    navController.navigate(R.id.settingsFragment)
                    true
                }
                else -> false
            }
        }


        // Update toolbar title on destination change
        navController.addOnDestinationChangedListener { _, destination, _ ->
            binding.toolbar.title = when (destination.id) {
                R.id.homeFragment -> "Home"
                R.id.mapFragment -> "Map"
                R.id.addCourtFragment -> "Add Court"
                R.id.settingsFragment -> "Settings"
                R.id.courtDetailFragment -> "Court Detail"
                R.id.editCourtFragment -> "Edit Court"
                R.id.checkInFragment -> "Check In"
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
            is com.example.group22_opencourt.ui.main.HomeFragment -> currentFragment.updateUserLocation(location)
        }
        currentLocationLiveData.postValue(location) // Post the updated location
    }

    // Remove location updates when the activity is destroyed
    override fun onDestroy() {
        super.onDestroy()
        // Remove location updates to prevent memory leaks
        if (::locationManager.isInitialized)
            locationManager.removeUpdates(this)
    }

    fun checkPermissions() {
        if (Build.VERSION.SDK_INT < 23) return
        // Check if we have location permission
        if (ContextCompat.checkSelfPermission(
                this,
                Manifest.permission.ACCESS_COARSE_LOCATION
            ) == PackageManager.PERMISSION_GRANTED
        ) {
            initLocationManager()  // already granted
        } else {
            locationPermissionLauncher.launch(
                arrayOf(
                    Manifest.permission.ACCESS_FINE_LOCATION,
                    Manifest.permission.ACCESS_COARSE_LOCATION
                )
            )
        }
    }

    fun showBackButton() {
        binding.toolbar.navigationIcon = ContextCompat.getDrawable(this, R.drawable.ic_arrow_back)
        binding.toolbar.setNavigationOnClickListener {
            navController.navigateUp()
            if (navController.currentDestination?.id != R.id.courtDetailFragment) {
                hideBackButton()
            }
        }
    }

    fun hideBackButton() {
        binding.toolbar.navigationIcon = null
    }

    fun showToolBarButton(text : String, onClick : () -> Unit) {
        binding.toolbarButton.visibility = VISIBLE
        binding.toolbarButton.text = text
        binding.toolbarButton.setOnClickListener {
            onClick()
        }
    }

    fun hideToolBarButton() {
        binding.toolbarButton.visibility = GONE
    }
}