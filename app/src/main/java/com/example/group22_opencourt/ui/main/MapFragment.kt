package com.example.group22_opencourt.ui.main

import android.annotation.SuppressLint
import android.location.Location
import android.os.Bundle
import androidx.fragment.app.Fragment
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.AdapterView
import android.widget.ArrayAdapter
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
import com.google.android.material.navigation.NavigationBarView
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext


class MapFragment : Fragment(), OnMapReadyCallback,
    GoogleMap.OnMapLongClickListener, GoogleMap.OnMyLocationButtonClickListener {

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

    private fun setupMapTypeSpinner() {
        // set up a spinner for map types
        val mapTypes = resources.getStringArray(R.array.map_types)
        val adapter = ArrayAdapter(requireContext(), android.R.layout.simple_spinner_item, mapTypes)
        adapter.setDropDownViewResource(android.R.layout.simple_spinner_dropdown_item)
        binding.mapTypeSpinner.adapter = adapter
        // default to normal map type
        binding.mapTypeSpinner.setSelection(0)
        // change map type for spinner selection
        binding.mapTypeSpinner.setOnItemSelectedListener(object : AdapterView.OnItemSelectedListener {
            override fun onItemSelected(parent: AdapterView<*>, view: View?, position: Int, id: Long) {
                when (position) {
                    0 -> map.mapType = GoogleMap.MAP_TYPE_NORMAL
                    1 -> map.mapType = GoogleMap.MAP_TYPE_SATELLITE
                    2 -> map.mapType = GoogleMap.MAP_TYPE_TERRAIN
                    3 -> map.mapType = GoogleMap.MAP_TYPE_HYBRID
                }
            }
            override fun onNothingSelected(parent: AdapterView<*>) {}
        })
    }

    @SuppressLint("MissingPermission")
    override fun onMapReady(googleMap: GoogleMap) {
        // setup the map
        map = googleMap
        map.setOnMapLongClickListener(this)
        map.setOnMyLocationButtonClickListener(this)

        map.uiSettings.isZoomControlsEnabled = true
        map.uiSettings.isCompassEnabled = true
        map.isMyLocationEnabled = true

        // update user location asynchronously
        viewLifecycleOwner.lifecycleScope.launch(Dispatchers.IO) {
            val location = getUserLocation()
            withContext(Dispatchers.Main) {
                updateUserLocation(location)
            }
        }
        setupMapTypeSpinner()
    }

    override fun onMapLongClick(latLng: LatLng) {
        map.clear()
        map.addMarker(MarkerOptions().position(latLng).title("Selected Location"))
        val cameraUpdate = CameraUpdateFactory.newLatLngZoom(latLng, 16f)
        map.animateCamera(cameraUpdate)
    }

    override fun onMyLocationButtonClick(): Boolean {
        viewLifecycleOwner.lifecycleScope.launch(Dispatchers.IO) {
            val location = getUserLocation()
            withContext(Dispatchers.Main) {
                updateUserLocation(location)
            }
        }
        mapCentered = false
        return true
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
        if (!this::map.isInitialized) {
            return
        }
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