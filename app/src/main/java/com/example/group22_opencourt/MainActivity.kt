package com.example.group22_opencourt

import android.Manifest
import android.content.pm.PackageManager
import android.location.Location
import android.location.LocationManager
import android.location.LocationListener
import android.os.Build
import android.os.Bundle
import androidx.activity.enableEdgeToEdge
import androidx.appcompat.app.AppCompatActivity
import androidx.core.app.ActivityCompat
import androidx.core.content.ContextCompat
import androidx.fragment.app.Fragment
import androidx.lifecycle.MutableLiveData
import androidx.viewpager2.widget.ViewPager2
import com.example.group22_opencourt.databinding.ActivityMainBinding
import com.example.group22_opencourt.ui.main.AddCourtFragment
import com.example.group22_opencourt.ui.main.HomeFragment
import com.example.group22_opencourt.ui.main.MainPagerAdapter
import com.example.group22_opencourt.ui.main.MapFragment
import com.example.group22_opencourt.ui.main.SettingsFragment
import com.example.group22_opencourt.ui.main.SimpleTextFragment


class MainActivity : AppCompatActivity(), LocationListener {

    private lateinit var binding: ActivityMainBinding
    private val PERMISSION_REQUEST_CODE = 0
    private lateinit var locationManager: LocationManager
    private val mapFragment = MapFragment()
    var currentLocation: Location? = null

    // LiveData to expose location updates
    val currentLocationLiveData = MutableLiveData<Location>()

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        binding = ActivityMainBinding.inflate(layoutInflater)
        setContentView(binding.root)
        enableEdgeToEdge()

        checkPermissions()

        val fragments = ArrayList<Fragment>()
        fragments.add(HomeFragment())
        fragments.add(MapFragment())
        fragments.add(AddCourtFragment())
        fragments.add(SettingsFragment())

        val pagerAdapter = MainPagerAdapter(this, fragments)
        binding.viewPager.adapter = pagerAdapter
        binding.viewPager.isUserInputEnabled = false

        // Bottom nav buttons to switch to correct fragment
        binding.bottomNav.setOnItemSelectedListener { item ->
            when (item.itemId) {
                R.id.nav_home -> binding.viewPager.setCurrentItem(0, true)
                R.id.nav_map -> binding.viewPager.setCurrentItem(1, true)
                R.id.nav_add_court -> binding.viewPager.setCurrentItem(2, true)
                R.id.nav_settings -> binding.viewPager.setCurrentItem(3, true)
            }
            true //handled change
        }

        // sync bottom nav button selected to current fragment
        //method requires an object instance
        binding.viewPager.registerOnPageChangeCallback(object : ViewPager2.OnPageChangeCallback() {
            override fun onPageSelected(position: Int) {
                super.onPageSelected(position)
                var itemId = 0
                when (position) {
                    0 -> {itemId = R.id.nav_home; binding.toolbar.title = "Home"}
                    1 -> {itemId = R.id.nav_map; binding.toolbar.title = "Map"}
                    2 -> {itemId = R.id.nav_add_court; binding.toolbar.title = "Add Court"}
                    3 -> {itemId = R.id.nav_settings; binding.toolbar.title = "Settings"}
                    else -> R.id.nav_home
                }
                binding.bottomNav.selectedItemId = itemId
            }
        })
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
        currentLocationLiveData.postValue(location) // Post the updated location
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
}