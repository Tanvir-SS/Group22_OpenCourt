package com.example.group22_opencourt.ui.main

import android.annotation.SuppressLint
import android.location.Geocoder
import android.location.Location
import android.os.Bundle
import android.util.Log
import androidx.fragment.app.Fragment
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.AdapterView
import android.widget.ArrayAdapter
import android.widget.Toast
import android.view.inputmethod.EditorInfo
import androidx.lifecycle.lifecycleScope
import com.example.group22_opencourt.MainActivity
import com.example.group22_opencourt.R
import com.example.group22_opencourt.databinding.FragmentMapBinding
import com.google.android.gms.maps.CameraUpdateFactory
import com.google.android.gms.maps.GoogleMap
import com.google.android.gms.maps.OnMapReadyCallback
import com.google.android.gms.maps.SupportMapFragment
import com.google.android.gms.maps.model.BitmapDescriptorFactory
import com.google.android.gms.maps.model.LatLng
import com.google.android.gms.maps.model.MarkerOptions
import com.google.android.material.navigation.NavigationBarView
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import kotlin.text.clear
import com.google.android.libraries.places.api.Places
import com.google.android.libraries.places.api.model.Place
import com.google.android.libraries.places.api.net.PlacesClient
import com.google.android.libraries.places.api.net.FetchPlaceRequest
import com.google.android.libraries.places.api.net.FindAutocompletePredictionsRequest
import com.example.group22_opencourt.BuildConfig



class MapFragment : Fragment(), OnMapReadyCallback, GoogleMap.OnMyLocationButtonClickListener {
    // initialize variables
    private lateinit var binding: FragmentMapBinding
    private lateinit var map: GoogleMap
    private var mapFragment: SupportMapFragment? = null
    private var mapMovedByUser = false
    private lateinit var placesClient: PlacesClient
    private var hasCenteredOnUser = false

    // binding setup
    override fun onCreateView(
        inflater: LayoutInflater, container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View? {
        binding = FragmentMapBinding.inflate(inflater, container, false)
        return binding.root
    }

    // map setup
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
        if (!Places.isInitialized()) {
            Places.initialize(requireContext(), BuildConfig.MAPS_API_KEY)
        }
        placesClient= Places.createClient(requireContext())
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

    private fun setupSearchBar() {
        binding.searchEditText.setOnEditorActionListener { v, actionId, event ->
            val address = v.text.toString()
            Log.d("MapFragment", "Search bar input: $address")

            val isEnterKey = event != null &&
                    event.keyCode == android.view.KeyEvent.KEYCODE_ENTER &&
                    event.action == android.view.KeyEvent.ACTION_DOWN

            if (actionId == EditorInfo.IME_ACTION_SEARCH ||
                actionId == EditorInfo.IME_ACTION_DONE ||
                isEnterKey) {

                hideKeyboard()

                if (address.isNotEmpty()) {
                    Toast.makeText(requireContext(), "Searching...", Toast.LENGTH_LONG).show()
                    searchAndMarkLocation(address)
                    binding.searchEditText.text.clear()
                    binding.searchEditText.clearFocus()
                } else {
                    Log.d("MapFragment", "Search bar is empty")
                    Toast.makeText(requireContext(), "Enter an address", Toast.LENGTH_SHORT).show()
                }
                true
            } else {
                false
            }
        }
    }


    private fun searchAndMarkLocation(query: String) {
        Log.d("MapFragment", "Places search for: $query")

        if (query.isBlank()) {
            Toast.makeText(requireContext(), "Enter a location", Toast.LENGTH_SHORT).show()
            Log.e("MapFragment", "Empty query")
            return
        }

        val request = FindAutocompletePredictionsRequest.builder()
            .setQuery(query)
            .build()

        placesClient.findAutocompletePredictions(request)
            .addOnSuccessListener { response ->
                val predictions = response.autocompletePredictions

                if (predictions.isEmpty()) {
                    Toast.makeText(requireContext(), "No results found", Toast.LENGTH_SHORT).show()
                    Log.e("MapFragment", "No predictions for '$query'")
                    return@addOnSuccessListener
                }

                val prediction = predictions[0]
                Log.d(
                    "MapFragment",
                    "Prediction selected: ${prediction.placeId} - ${prediction.getFullText(null)}"
                )

                val placeRequest = FetchPlaceRequest.builder(
                    prediction.placeId,
                    listOf(
                        Place.Field.ID,
                        Place.Field.NAME,
                        Place.Field.ADDRESS,
                        Place.Field.LAT_LNG
                    )
                ).build()

                placesClient.fetchPlace(placeRequest)
                    .addOnSuccessListener { placeResponse ->
                        val place = placeResponse.place
                        val latLng = place.latLng

                        if (latLng == null) {
                            Toast.makeText(requireContext(), "Location has no coordinates", Toast.LENGTH_SHORT).show()
                            Log.e("MapFragment", "No latLng for place: ${place.name}")
                            return@addOnSuccessListener
                        }

                        Log.d("MapFragment", "Resolved place: ${place.name} at $latLng")

                        map.clear()
                        map.addMarker(
                            MarkerOptions()
                                .position(latLng)
                                .title(place.name)
                                .icon(BitmapDescriptorFactory.defaultMarker(BitmapDescriptorFactory.HUE_BLUE))
                        )

                        map.animateCamera(CameraUpdateFactory.newLatLngZoom(latLng, 16f))

                        Toast.makeText(requireContext(), "Found: ${place.name}", Toast.LENGTH_SHORT).show()
                    }
                    .addOnFailureListener { e ->
                        Log.e("MapFragment", "FetchPlace failed", e)
                        Toast.makeText(requireContext(), "Failed to fetch place", Toast.LENGTH_SHORT).show()
                    }
            }
            .addOnFailureListener { e ->
                Log.e("MapFragment", "Autocomplete failed", e)
                Toast.makeText(requireContext(), "Search failed", Toast.LENGTH_SHORT).show()
            }
    }


    @SuppressLint("MissingPermission")
    override fun onMapReady(googleMap: GoogleMap) {
        // setup the map
        map = googleMap
        map.setOnMyLocationButtonClickListener(this)

        map.uiSettings.isZoomControlsEnabled = true
        map.uiSettings.isCompassEnabled = true
        map.isMyLocationEnabled = true

        // detect if user moved the map
        map.setOnCameraMoveStartedListener { reason ->
            if (reason == GoogleMap.OnCameraMoveStartedListener.REASON_GESTURE) {
                mapMovedByUser = true
            }
        }

        // observe location updates from MainActivity
        (activity as? MainActivity)?.currentLocationLiveData?.observe(viewLifecycleOwner) { location ->
            if (!hasCenteredOnUser) {
                hasCenteredOnUser = true
                centerMapOnUser(location)
            }
        }

        // update user location asynchronously
        viewLifecycleOwner.lifecycleScope.launch(Dispatchers.IO) {
            val location = getUserLocation()
            withContext(Dispatchers.Main) {
                if (!hasCenteredOnUser) {
                    hasCenteredOnUser = true
                    centerMapOnUser(location)
                }
            }
        }
        setupMapTypeSpinner()
        setupSearchBar()
    }

    override fun onMyLocationButtonClick(): Boolean {
        // reset the flag so camera recenters
        mapMovedByUser = false

        viewLifecycleOwner.lifecycleScope.launch(Dispatchers.IO) {
            val location = getUserLocation()
            withContext(Dispatchers.Main) {
                centerMapOnUser(location)
            }
        }
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

    fun centerMapOnUser(location: Location) {
        val latLng = LatLng(location.latitude, location.longitude)
        map.clear()
        map.animateCamera(CameraUpdateFactory.newLatLngZoom(latLng, 16f))
    }

    private fun hideKeyboard() {
        val inputMethodManager =
            requireContext().getSystemService(android.content.Context.INPUT_METHOD_SERVICE)
                    as android.view.inputmethod.InputMethodManager

        val view = requireActivity().currentFocus ?: View(requireContext())
        inputMethodManager.hideSoftInputFromWindow(view.windowToken, 0)
    }
}