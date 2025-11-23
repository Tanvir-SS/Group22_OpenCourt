package com.example.group22_opencourt.ui.main

import android.location.Location
import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import androidx.fragment.app.Fragment
import androidx.fragment.app.activityViewModels
import androidx.lifecycle.Observer
import androidx.lifecycle.lifecycleScope
import androidx.recyclerview.widget.DividerItemDecoration
import androidx.recyclerview.widget.LinearLayoutManager
import com.example.group22_opencourt.R
import com.example.group22_opencourt.databinding.FragmentHomeBinding
import com.example.group22_opencourt.model.BasketballCourt
import com.example.group22_opencourt.model.Court
import com.example.group22_opencourt.model.TennisCourt
import com.google.android.gms.maps.CameraUpdateFactory
import com.google.android.gms.maps.model.LatLng
import com.google.android.gms.maps.model.MarkerOptions

class HomeFragment : Fragment() {

    private lateinit var binding: FragmentHomeBinding
    private lateinit var adapter: HomeRecyclerViewAdapter
    private val viewModel: HomeViewModel by activityViewModels()

    // Filter state
    private var showTennis = true
    private var showBasketball = true
    private var lastUserLocation: Location? = null
    private val locationUpdateThresholdMeters = 50f
    private var selectedDistanceKm = 5 // Default distance
    private val distanceIntervals = listOf(1, 2, 5, 10, 25)

    private var courts = emptyList<Court>()

    override fun onCreateView(
        inflater: LayoutInflater, container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View? {
        binding = FragmentHomeBinding.inflate(inflater, container, false)
        return binding.root
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        binding.recyclerView.layoutManager = LinearLayoutManager(requireContext())
        val dividerItemDecoration = DividerItemDecoration(
            binding.recyclerView.context,
            DividerItemDecoration.VERTICAL
        )
        binding.recyclerView.addItemDecoration(dividerItemDecoration)

        adapter = HomeRecyclerViewAdapter(courts, viewLifecycleOwner.lifecycleScope)
        binding.recyclerView.adapter = adapter

        // Observe courts from ViewModel and update adapter
        viewModel.courts.observe(viewLifecycleOwner) { courts ->
            adapter.setItems(courts)
            this@HomeFragment.courts = courts
            applyAllFilters()
        }

        // Filter button logic
        binding.filterButton.setOnClickListener {
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
            tennisCheck.isChecked = showTennis
            basketballCheck.isChecked = showBasketball
            // Checkbox listeners
            tennisCheck.setOnCheckedChangeListener { _, isChecked ->
                showTennis = isChecked
                applyAllFilters()
            }
            basketballCheck.setOnCheckedChangeListener { _, isChecked ->
                showBasketball = isChecked
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
            popupWindow.showAsDropDown(binding.filterButton, 0, 0)
        }
    }

    private fun applyAllFilters() {
        val filtered = courts.filter { court ->
            var typeMatch = false
            when (court) {
                is BasketballCourt -> typeMatch = showBasketball
                is TennisCourt -> typeMatch = showTennis
            }
            val location = lastUserLocation
            val geoPoint = court.base.geoPoint
            val distanceMatch = if (location != null && geoPoint != null) {
                val results = FloatArray(1)
                Location.distanceBetween(
                    location.latitude, location.longitude,
                    geoPoint.latitude, geoPoint.longitude,
                    results
                )
                results[0] <= selectedDistanceKm * 1000
            } else {
                true // If no location, show all
            }
            typeMatch && distanceMatch
        }
        val location = lastUserLocation
        var sorted = filtered
        if (location != null) {
            //sort by shortest distance
            sorted = filtered.sortedBy { court ->
                val geoPoint = court.base.geoPoint
                if (geoPoint != null) {
                    val results = FloatArray(1)
                    Location.distanceBetween(
                        location.latitude, location.longitude,
                        geoPoint.latitude, geoPoint.longitude,
                        results
                    )
                    //return distance
                    results[0]
                } else {
                    //return max distance because no location data
                    Float.MAX_VALUE
                }
            }
        }
        adapter.setItems(sorted)
    }

    fun updateUserLocation(location: Location) {
        val lastLocation = lastUserLocation
        val shouldUpdate = lastLocation == null || location.distanceTo(lastLocation) > locationUpdateThresholdMeters
        if (shouldUpdate) {
            adapter.location = location
            lastUserLocation = location
            applyAllFilters()
        }
    }
}