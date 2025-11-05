package com.example.group22_opencourt.ui.main

import android.location.Location
import android.os.Bundle
import androidx.fragment.app.Fragment
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import com.example.group22_opencourt.MainActivity
import com.example.group22_opencourt.R

import com.example.group22_opencourt.databinding.FragmentMapBinding
import com.google.android.gms.maps.CameraUpdateFactory
import com.google.android.gms.maps.GoogleMap
import com.google.android.gms.maps.GoogleMapOptions
import com.google.android.gms.maps.OnMapReadyCallback
import com.google.android.gms.maps.SupportMapFragment
import com.google.android.gms.maps.model.LatLng
import com.google.android.gms.maps.model.MarkerOptions
import kotlin.text.replace


class MapFragment : Fragment(), OnMapReadyCallback {

    private lateinit var binding: FragmentMapBinding
    private lateinit var map: GoogleMap
    private var mapCentered = false


    override fun onCreateView(
        inflater: LayoutInflater, container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View? {
        binding = FragmentMapBinding.inflate(inflater, container, false)
        return binding.root
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        var mapFragment = childFragmentManager.findFragmentById(R.id.map_container) as? SupportMapFragment
        if (mapFragment == null) {
            mapFragment = SupportMapFragment.newInstance()
            childFragmentManager.beginTransaction()
                .replace(R.id.map_container, mapFragment)
                .commit()
        }
        mapFragment.getMapAsync(this)
    }

    override fun onMapReady(googleMap: GoogleMap) {
        map = googleMap
        val options = GoogleMapOptions()
        options.mapType(GoogleMap.MAP_TYPE_NORMAL)
        map.uiSettings.isZoomControlsEnabled = true
        map.uiSettings.isCompassEnabled = true
        // get user location from MainActivity and update map
        val location = (activity as? MainActivity)?.currentLocation
        if (location != null) {
            updateUserLocation(location)
        } else {
            // use sfu as default location
            val sfuLatLng = Location("").apply {
                latitude = 49.27984399307886
                longitude = -122.92159771848385
            }
            updateUserLocation(sfuLatLng)
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