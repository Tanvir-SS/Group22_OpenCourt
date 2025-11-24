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
import androidx.navigation.fragment.findNavController
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
import androidx.fragment.app.activityViewModels
import com.google.android.gms.maps.model.Marker
import android.widget.ImageView
import android.widget.TextView
import com.example.group22_opencourt.model.BasketballCourt
import com.example.group22_opencourt.model.Court
import com.example.group22_opencourt.model.TennisCourt


class MapFragment : Fragment(), OnMapReadyCallback, GoogleMap.OnMyLocationButtonClickListener {
    // initialize variables
    private lateinit var binding: FragmentMapBinding
    private lateinit var map: GoogleMap
    private var mapFragment: SupportMapFragment? = null
    private var mapMovedByUser = false
    private lateinit var placesClient: PlacesClient
    private var hasCenteredOnUser = false
    private val viewModel: HomeViewModel by activityViewModels()

    // Filter state
    private var showTennis = true
    private var showBasketball = true

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

        // Set initial checkbox states
        val tennisCheck = binding.mapCheckboxTennis
        val basketballCheck = binding.mapCheckboxBasketball
        tennisCheck.isChecked = showTennis
        basketballCheck.isChecked = showBasketball

        // Checkbox listeners
        tennisCheck.setOnCheckedChangeListener { _, isChecked ->
            showTennis = isChecked
            observeCourts()
        }
        basketballCheck.setOnCheckedChangeListener { _, isChecked ->
            showBasketball = isChecked
            observeCourts()
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

    private fun setupSearchBar() {
        // handle search bar input
        binding.searchEditText.setOnEditorActionListener { v, actionId, event ->
            val address = v.text.toString()
            Log.d("MapFragment", "Search bar input: $address")
            // check for search action or enter key
            val isEnterKey = event != null &&
                    event.keyCode == android.view.KeyEvent.KEYCODE_ENTER &&
                    event.action == android.view.KeyEvent.ACTION_DOWN

            if (actionId == EditorInfo.IME_ACTION_SEARCH ||
                actionId == EditorInfo.IME_ACTION_DONE ||
                isEnterKey) {
                // hide keyboard if search initiated
                hideKeyboard()
                // perform search if address is not empty
                if (address.isNotEmpty()) {
                    Toast.makeText(requireContext(), "Searching...", Toast.LENGTH_LONG).show()
                    searchAndMarkLocation(address)
                    // clear search bar
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
        // validate input
        if (query.isBlank()) {
            Toast.makeText(requireContext(), "Enter a location", Toast.LENGTH_SHORT).show()
            Log.e("MapFragment", "Empty query")
            return
        }
        // build autocomplete request
        val request = FindAutocompletePredictionsRequest.builder()
            .setQuery(query)
            .build()
        // execute autocomplete request
        placesClient.findAutocompletePredictions(request)
            // handle successful response
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
                // build place details request
                val placeRequest = FetchPlaceRequest.builder(
                    prediction.placeId,
                    listOf(
                        Place.Field.ID,
                        Place.Field.NAME,
                        Place.Field.ADDRESS,
                        Place.Field.LAT_LNG
                    )
                ).build()
                // execute place details request
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
                        // add marker to map
                        map.addMarker(
                            MarkerOptions()
                                .position(latLng)
                                .title(place.name)
                                .icon(BitmapDescriptorFactory.defaultMarker(BitmapDescriptorFactory.HUE_BLUE))
                        )
                        // move camera to location
                        map.animateCamera(CameraUpdateFactory.newLatLngZoom(latLng, 15f))
                        Toast.makeText(requireContext(), "Found: ${place.name}", Toast.LENGTH_SHORT).show()
                    }
                    // handle place details failure
                    .addOnFailureListener { e ->
                        Log.e("MapFragment", "FetchPlace failed", e)
                        Toast.makeText(requireContext(), "Failed to fetch place", Toast.LENGTH_SHORT).show()
                    }
            }
            // handle autocomplete failure
            .addOnFailureListener { e ->
                Log.e("MapFragment", "Autocomplete failed", e)
                Toast.makeText(requireContext(), "Search failed", Toast.LENGTH_SHORT).show()
            }
    }


    private fun observeCourts() {
        // observe courts from ViewModel and add markers
        viewModel.courts.observe(viewLifecycleOwner) { courts ->
            map.clear()
            for (court in courts) {
                val geoPoint = court.base.geoPoint
                if (geoPoint != null) {
                    // get court location and add marker
                    val latLng = LatLng(geoPoint.latitude, geoPoint.longitude)

                    // Filter based on court type
                    if ((court is TennisCourt && showTennis) || (court is BasketballCourt && showBasketball)) {
                        val marker = map.addMarker(
                            MarkerOptions()
                                .position(latLng)
                                .title(court.base.name)
                                .icon(BitmapDescriptorFactory.defaultMarker(BitmapDescriptorFactory.HUE_RED))
                        )
                        // set the court as marker tag for info window
                        marker?.tag = court
                    }
                } else {
                    Log.w("MapFragment", "Court ${court.base.name} does not have a valid GeoPoint")
                }
            }
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

        map.setPadding(0,250,0,0)

        // detect if user moved the map
        map.setOnCameraMoveStartedListener { reason ->
            if (reason == GoogleMap.OnCameraMoveStartedListener.REASON_GESTURE) {
                mapMovedByUser = true
            }
        }

        map.setInfoWindowAdapter(object : GoogleMap.InfoWindowAdapter {
            override fun getInfoWindow(marker: Marker): View? {
                return null
            }
            // custom info window content
            override fun getInfoContents(marker: Marker): View? {
                val court = marker.tag as? Court ?: return null
                val view = LayoutInflater.from(requireContext()).inflate(R.layout.custom_info_window, null)

                val title = view.findViewById<TextView>(R.id.info_title)
                val snippet = view.findViewById<TextView>(R.id.info_text)
                val icon = view.findViewById<ImageView>(R.id.info_icon)

                // set court name and details
                title.text = court.base.name
                snippet.text = "See Details"

                // set the icon based on court type
                val iconRes = when (court.type) {
                    "tennis" -> R.drawable.ic_tennis
                    "basketball" -> R.drawable.ic_basketball
                    else -> R.drawable.ic_launcher_foreground
                }
                icon.setImageResource(iconRes)

                return view
            }
        })

        map.setOnInfoWindowClickListener { marker ->
            val court = marker.tag as? Court ?: return@setOnInfoWindowClickListener
            Log.d("MapFragment", "Info window clicked: ${court.base.name}")

            // LAUNCH COURT DETAIL FRAGMENT HERE
            onCourtSelected(court.base.id)
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
        observeCourts()
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
        // center map on user location
        val latLng = LatLng(location.latitude, location.longitude)
        map.moveCamera(CameraUpdateFactory.newLatLngZoom(latLng, 15f))
    }

    private fun hideKeyboard() {
        // hide keyboard
        val inputMethodManager =
            requireContext().getSystemService(android.content.Context.INPUT_METHOD_SERVICE)
                    as android.view.inputmethod.InputMethodManager

        val view = requireActivity().currentFocus ?: View(requireContext())
        inputMethodManager.hideSoftInputFromWindow(view.windowToken, 0)
    }

    private fun onCourtSelected(documentId: String) {
        val args = Bundle().apply {
            putString("document_id", documentId)
        }
        findNavController().navigate(R.id.action_mapFragment_to_courtDetailFragment, args)
    }
}