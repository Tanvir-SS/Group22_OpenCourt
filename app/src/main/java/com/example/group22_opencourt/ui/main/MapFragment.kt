package com.example.group22_opencourt.ui.main

import android.location.Location
import android.os.Bundle
import androidx.fragment.app.Fragment
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import androidx.lifecycle.lifecycleScope
import com.example.group22_opencourt.MainActivity
import com.example.group22_opencourt.R
import com.example.group22_opencourt.databinding.FragmentMapBinding
import com.google.android.gms.maps.CameraUpdateFactory
import com.google.android.gms.maps.GoogleMap
import com.google.android.gms.maps.OnMapReadyCallback
import com.google.android.gms.maps.SupportMapFragment
import com.google.android.gms.maps.model.LatLng
import com.google.android.gms.maps.model.MarkerOptions
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext


class MapFragment : Fragment(), OnMapReadyCallback {

    private lateinit var binding: FragmentMapBinding
    private lateinit var map: GoogleMap
    private var mapCentered = false
    private var mapFragment: SupportMapFragment? = null

    override fun onCreateView(
        inflater: LayoutInflater, container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View? {
        binding = FragmentMapBinding.inflate(inflater, container, false)
        return binding.root
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        if (mapFragment == null) {
            mapFragment = childFragmentManager.findFragmentById(R.id.map_container) as? SupportMapFragment
            if (mapFragment == null) {
                mapFragment = SupportMapFragment.newInstance()
                childFragmentManager.beginTransaction()
                    .replace(R.id.map_container, mapFragment!!)
                    .commitNow()
            }
            mapFragment!!.getMapAsync(this)
        }
    }

    override fun onMapReady(googleMap: GoogleMap) {
        // setup the map
        map = googleMap
        map.mapType = GoogleMap.MAP_TYPE_NORMAL
        map.uiSettings.isZoomControlsEnabled = true
        map.uiSettings.isCompassEnabled = true

        // update user location asynchronously
        viewLifecycleOwner.lifecycleScope.launch(Dispatchers.IO) {
            val location = getUserLocation()
            withContext(Dispatchers.Main) {
                updateUserLocation(location)
            }
        }
    }

    private fun getUserLocation(): Location {
        // get user location from MainActivity
        val activity = activity as? MainActivity
        // if location is null, return a default location (sfu)
        return activity?.currentLocation ?: Location("").apply {
            latitude = 49.27984399307886
            longitude = -122.92159771848385
        }
    }

    fun updateUserLocation(location: Location) {
        val latLng = LatLng(location.latitude, location.longitude)
        map.clear()
        map.addMarker(MarkerOptions().position(latLng).title("You are here"))
        // center map on user location
        if (!mapCentered) {
            val cameraUpdate = CameraUpdateFactory.newLatLngZoom(latLng, 16f)
            map.animateCamera(cameraUpdate)
            mapCentered = true
        }
    }

}