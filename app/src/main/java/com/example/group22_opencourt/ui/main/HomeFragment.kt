package com.example.group22_opencourt.ui.main

import android.Manifest
import android.content.Context
import android.content.pm.PackageManager
import android.location.Location
import android.os.Bundle
import android.util.Log
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.ArrayAdapter
import android.widget.AdapterView
import android.widget.LinearLayout
import android.widget.TextView
import androidx.core.content.ContextCompat
import androidx.fragment.app.Fragment
import androidx.fragment.app.activityViewModels
import androidx.lifecycle.Observer
import androidx.lifecycle.lifecycleScope
import androidx.navigation.fragment.findNavController
import androidx.recyclerview.widget.DividerItemDecoration
import androidx.recyclerview.widget.LinearLayoutManager
import com.example.group22_opencourt.R
import com.example.group22_opencourt.databinding.FragmentHomeBinding
import com.example.group22_opencourt.model.BasketballCourt
import com.example.group22_opencourt.model.Court
import com.example.group22_opencourt.model.ImagesRepository
import com.example.group22_opencourt.model.TennisCourt
import com.example.group22_opencourt.model.User
import com.example.group22_opencourt.model.UserRepository
import com.example.group22_opencourt.ui.main.HomeRecyclerViewAdapter.ViewHolder
import com.google.android.material.checkbox.MaterialCheckBox
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.Job
import kotlinx.coroutines.delay
import kotlinx.coroutines.isActive
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import java.util.Locale

class HomeFragment : Fragment() {
    // View binding
    private lateinit var binding: FragmentHomeBinding
    private lateinit var adapter: HomeRecyclerViewAdapter
    private val viewModel: HomeViewModel by activityViewModels()

    // Filter state
    private var showTennis = true
    private var showBasketball = true

    // New sorting state
    private var showRecent = false
    private var lastUserLocation: Location? = null
    private val locationUpdateThresholdMeters = 50f
    private var selectedDistanceKm = 5 // Default distance
    private val distanceIntervals = listOf(1, 2, 5, 10, 25)

    // All courts list
    private var courts = emptyList<Court>()

    // New filter state variable
    private var filterAvailableOnly = false // Track available courts only filter

    // Current mode
    private enum class Mode { NEARBY, FAVOURITES }
    private var currentMode: Mode = Mode.NEARBY

    // Current user data
    var currentUser : User? = null

    // Permission monitoring job
    private var permissionJob : Job? = null

    override fun onCreateView(
        inflater: LayoutInflater, container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View? {
        // Inflate the layout for this fragment using view binding
        binding = FragmentHomeBinding.inflate(inflater, container, false)
        Log.d("debug", "onCreateViewCalled")
        return binding.root
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        // divide items in recycler view
        val dividerItemDecoration = DividerItemDecoration(
            binding.recyclerView.context,
            DividerItemDecoration.VERTICAL
        )
        binding.recyclerView.addItemDecoration(dividerItemDecoration)
        val latitude1 = arguments?.getDouble("latitude") ?: 0.0
        val longitude1 = arguments?.getDouble("longitude") ?: 0.0
        if (latitude1 != 0.0) {
            lastUserLocation = Location("manual").apply {
                latitude = latitude1
                longitude = longitude1
            }
        }
        // Setup RecyclerView and its adapter
        adapter = HomeRecyclerViewAdapter(
            courtList = emptyList(),
            currentUser = currentUser,
            onItemClick = {
                onCourtSelected(it.base.id)
            }
        )
        binding.recyclerView.adapter = adapter
        binding.recyclerView.layoutManager = LinearLayoutManager(requireContext())

        // Observe current user data and update adapter
        UserRepository.instance.currentUser.observe(viewLifecycleOwner) {
            currentUser = it
            adapter.updateCurrentUser(it)
            adapter.setItems(courts) {
                Log.d("HomeFragment", "Adapter updated with new user favorites")
            }
        }

        // Observe courts from ViewModel and update adapter
        viewModel.courts.observe(viewLifecycleOwner) { courts ->
            this@HomeFragment.courts = courts
            lifecycleScope.launch {
                withContext(Dispatchers.IO) {
                    val ctx = context ?: return@withContext
                    courts.forEach {
                        ImagesRepository.instance.ensurePhotoForPlace(ctx, it)
                    }
                }

            }
            Log.d("debug", "filter called from observer $lastUserLocation")
            applyAllFilters() {
                Log.d("debug", "onsuccess")
            }
        }

        // Filter button logic
        binding.filterButton.setOnClickListener {
            // Show popup window with filter options
            val inflater = LayoutInflater.from(requireContext())
            val popupView = inflater.inflate(R.layout.filter_popup, null)
            val minWidthPx = (180 * resources.displayMetrics.density).toInt()
            val buttonWidth = binding.filterButton.width
            val popupWidth = if (buttonWidth > minWidthPx) buttonWidth else minWidthPx
            val popupWindow = android.widget.PopupWindow(
                popupView,
                popupWidth,
                ViewGroup.LayoutParams.WRAP_CONTENT,
                true
            )
            popupWindow.setBackgroundDrawable(android.graphics.drawable.ColorDrawable(android.graphics.Color.TRANSPARENT))
            popupWindow.isOutsideTouchable = true

            // Set initial checkbox states
            val tennisCheck = popupView.findViewById<com.google.android.material.checkbox.MaterialCheckBox>(
                R.id.checkbox_tennis)
            val basketballCheck = popupView.findViewById<com.google.android.material.checkbox.MaterialCheckBox>(R.id.checkbox_basketball)
            val availableCheck = popupView.findViewById<com.google.android.material.checkbox.MaterialCheckBox>(R.id.checkbox_available)
            val recentContainer : LinearLayout = popupView.findViewById(R.id.sort_recent_container)
            val nearbyContainer : LinearLayout = popupView.findViewById(R.id.distance_container)
            if (currentMode == Mode.NEARBY) {
                recentContainer.visibility = View.GONE
                nearbyContainer.visibility = View.VISIBLE
            } else if (currentMode == Mode.FAVOURITES) {
                recentContainer.visibility = View.VISIBLE
                nearbyContainer.visibility = View.GONE
            }
            tennisCheck.isChecked = showTennis
            basketballCheck.isChecked = showBasketball
            availableCheck.isChecked = filterAvailableOnly

            // Checkbox listeners
            tennisCheck.setOnCheckedChangeListener { _, isChecked ->
                showTennis = isChecked
                applyAllFilters()
            }
            basketballCheck.setOnCheckedChangeListener { _, isChecked ->
                showBasketball = isChecked
                applyAllFilters()
            }
            availableCheck.setOnCheckedChangeListener { _, isChecked ->
                filterAvailableOnly = isChecked
                applyAllFilters()
            }

            // Recent sorting checkbox logic
            val recentCheck = popupView.findViewById<MaterialCheckBox>(R.id.checkbox_recent)
            recentCheck.isChecked = showRecent
            recentCheck.setOnCheckedChangeListener { _, isChecked ->
                showRecent = isChecked
                applyAllFilters()
            }

            //  Distance slider logic
            val distanceSlider = popupView.findViewById<com.google.android.material.slider.Slider>(R.id.distance_slider)
            val distanceValue = popupView.findViewById<android.widget.TextView>(R.id.distance_value)
            // Set initial slider position
            val initialIndex = distanceIntervals.indexOf(selectedDistanceKm)
            distanceSlider.value = if (initialIndex >= 0) initialIndex.toFloat() else 0f
            distanceValue.text = "${distanceIntervals[distanceSlider.value.toInt()]} km"
            distanceSlider.addOnChangeListener { _, value, _ ->
                selectedDistanceKm = distanceIntervals[value.toInt()]
                distanceValue.text = "${selectedDistanceKm} km"
                applyAllFilters()
            }
            val xOffset = binding.filterButton.width - popupWidth
            popupWindow.showAsDropDown(binding.filterButton, xOffset, 0)
        }
        // Setup mode spinner
        setupModeSpinner()
    }

    private fun setupModeSpinner() {
        // initialize spinner with modes for nearby and favourites
        val spinner = binding.homeModeSpinner
        val adapter = ArrayAdapter.createFromResource(
            requireContext(),
            R.array.home_modes,
            R.layout.item_home_mode_spinner
        ).also { arrAdapter ->
            arrAdapter.setDropDownViewResource(R.layout.item_home_mode_spinner_dropdown)
        }
        spinner.adapter = adapter

        // Spinner selection listener
        spinner.onItemSelectedListener = object : AdapterView.OnItemSelectedListener {
            override fun onItemSelected(
                parent: AdapterView<*>,
                view: View?,
                position: Int,
                id: Long
            ) {
                // Update current mode based on selection
                when (position) {
                    0 -> {
                        currentMode = Mode.NEARBY
                        applyAllFilters()
                    }
                    1 -> {
                        currentMode = Mode.FAVOURITES
                        applyAllFilters()
                    }
                }
            }

            override fun onNothingSelected(parent: AdapterView<*>) {
                // no-op
            }
        }
    }

    private fun applyAllFilters(onSuccess: ((List<Court>) -> Unit)? = null) {
        // apply all filters and update adapter
        permissionJob?.cancel()
        binding.locationInfoText.visibility = View.GONE
        val location = lastUserLocation
        if (location == null) {
            // filter based on mode
            val user = currentUser
            if (currentMode == Mode.FAVOURITES && user != null) {
                // filter favourites
                val filter = courts.filter { court ->
                    user.favourites.contains(court.base.id)
                }
                // sort favourites
                val sorted = filter.sortedBy { court ->
                    user.favourites.indexOf(court.base.id)
                }
                // update adapter
                adapter.setItems(sorted)
            } else if (currentMode ==  Mode.NEARBY) {
                binding.locationInfoText.visibility = View.VISIBLE
                monitorLocationPermissionStatus()
                adapter.setItems(emptyList())
            }
            return
        }
        // proceed with filtering and sorting using location
        adapter.location = location
        val filtered = filterCourts(location)
        var sorted = filtered

        // sort by shortest distance
        sorted = sortList(filtered, location)
        if (sorted.isEmpty()) {
            binding.locationInfoText.visibility = View.VISIBLE
            if (currentMode == Mode.FAVOURITES) {
                binding.locationInfoText.text = "No Courts Favourited"
            } else if (currentMode == Mode.NEARBY) {
                binding.locationInfoText.text = "No Courts Nearby"
            }
        }

        // update adapter
        adapter.setItems(sorted) {
            onSuccess?.invoke(sorted)
        }
    }

    fun updateUserLocation(location: Location) {
        // called from MainActivity when location is updated
        Log.d("location", "recieved in home fragment")
        // set location only if significant change
        val lastLocation = lastUserLocation
        var shouldUpdate = lastLocation == null || location.distanceTo(lastLocation) > locationUpdateThresholdMeters
        if (!this::binding.isInitialized) {
            return
        }
        if (shouldUpdate) {
            // update location
            lastUserLocation = location
            Log.d("debug", "filter called from update userLocation")
            applyAllFilters { sorted ->
                val childCount = binding.recyclerView.childCount
                val max = minOf(childCount, sorted.size)
                // update visible distance labels
                for (i in 0 until max) {
                    val child = binding.recyclerView.getChildAt(i) ?: continue
                    val holder = binding.recyclerView.getChildViewHolder(child) as? ViewHolder ?: continue
                    val court = sorted.getOrNull(i) ?: continue
                    // calculate distance
                    val geoPoint = court.base.geoPoint
                    val location = lastUserLocation

                    if (geoPoint != null && location != null) {
                        val results = FloatArray(1)
                        Location.distanceBetween(
                            location.latitude, location.longitude,
                            geoPoint.latitude, geoPoint.longitude,
                            results
                        )
                        // update distance label
                        val distanceKm = results[0] / 1000f
                        holder.distanceView.text =
                            String.format(Locale.getDefault(), "%.1f km", distanceKm)
                    } else {
                        holder.distanceView.text = "-.- km"
                    }
                }
            }
        }
    }

    private fun filterCourts(location : Location) : List<Court> {
        // filter courts based on current filter settings
        return courts.filter { court ->
            var typeMatch = false
            when (court) {
                // check type filter
                is BasketballCourt -> typeMatch = showBasketball
                is TennisCourt -> typeMatch = showTennis
            }
            var distanceMatch = false
            // check distance or favourites filter
            if (currentMode == Mode.NEARBY) {
                val geoPoint = court.base.geoPoint
                distanceMatch = if (geoPoint != null) {
                    val results = FloatArray(1)
                    Location.distanceBetween(
                        location.latitude, location.longitude,
                        geoPoint.latitude, geoPoint.longitude,
                        results
                    )
                    results[0] <= selectedDistanceKm * 1000
                } else {
                   true
                }
            } else if (currentMode == Mode.FAVOURITES){
                // check if in favourites
                val user = currentUser
                if (user == null) {
                    distanceMatch = false
                } else {
                    distanceMatch = user.favourites.contains(court.base.id)
                }
            }
            var availableMatch = true
            if (filterAvailableOnly && court.base.courtsAvailable == 0) {
                availableMatch = false
            }
            typeMatch && distanceMatch && availableMatch
        }
    }

    private fun sortList(list : List<Court>, location : Location) : List<Court> {
        // sort list based on current mode and settings
        val user = currentUser
        if (currentMode == Mode.NEARBY) {
            // sort by distance
            return sortByLocation(list, location)
        } else if (currentMode == Mode.FAVOURITES && user != null) {
            if (showRecent) {
                // sort by favourites order
                return list.sortedBy { court ->
                    user.favourites.indexOf(court.base.id)
                }
            } else {
                return sortByLocation(list, location)
            }
        }
        return list
    }

    private fun sortByLocation(list : List<Court>, location : Location ) : List<Court> {
        // sort courts by distance from location
        return list.sortedBy { court ->
            val geoPoint = court.base.geoPoint
            if (geoPoint != null) {
                val results = FloatArray(1)
                // calculate distance from location to court
                Location.distanceBetween(
                    location.latitude, location.longitude,
                    geoPoint.latitude, geoPoint.longitude,
                    results
                )
                // return distance in array
                results[0]
            } else {
                // return max distance because no location data
                Float.MAX_VALUE
            }
        }
    }

    private fun onCourtSelected(documentId: String) {
        // navigate to court detail fragment with document ID
        val args = Bundle().apply {
            putString("document_id", documentId)
        }
        findNavController().navigate(R.id.action_homeFragment_to_courtDetailFragment, args)
    }

    private fun monitorLocationPermissionStatus() {
        // monitor location permission status and update UI
        permissionJob = lifecycleScope.launch {
            while (isActive) {
                 if (!isLocationPermissionGranted(requireContext())) {
                     binding.locationInfoText.text = "Location not Enabled"
                 } else {
                    binding.locationInfoText.text = "Fetching Location"
                 }
                 delay(1000)
            }

        }

    }

    fun isLocationPermissionGranted(context: Context): Boolean {
        // check if either fine or coarse location permission is granted
        val fine = ContextCompat.checkSelfPermission(
            context,
            Manifest.permission.ACCESS_FINE_LOCATION
        ) == PackageManager.PERMISSION_GRANTED

        val coarse = ContextCompat.checkSelfPermission(
            context,
            Manifest.permission.ACCESS_COARSE_LOCATION
        ) == PackageManager.PERMISSION_GRANTED

        return fine || coarse
    }

}