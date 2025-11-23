package com.example.group22_opencourt.ui.main

import android.location.Location
import android.os.Bundle
import android.util.Log
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import androidx.fragment.app.Fragment
import androidx.fragment.app.activityViewModels
import androidx.lifecycle.Observer
import androidx.lifecycle.lifecycleScope
import androidx.navigation.fragment.findNavController
import androidx.recyclerview.widget.DividerItemDecoration
import androidx.recyclerview.widget.LinearLayoutManager
import com.example.group22_opencourt.MainActivity
import com.example.group22_opencourt.R
import com.example.group22_opencourt.databinding.FragmentHomeBinding
import com.example.group22_opencourt.model.BasketballCourt
import com.example.group22_opencourt.model.Court
import com.example.group22_opencourt.model.TennisCourt
import com.example.group22_opencourt.ui.main.HomeRecyclerViewAdapter.ViewHolder
import com.google.android.gms.maps.CameraUpdateFactory
import com.google.android.gms.maps.model.LatLng
import com.google.android.gms.maps.model.MarkerOptions
import java.util.Locale

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

    // New filter state variable
    private var filterAvailableOnly = false // Track available courts only filter



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
        lastUserLocation = null
        adapter = HomeRecyclerViewAdapter(courts, viewLifecycleOwner.lifecycleScope) {
            onCourtSelected(it.base.id)
        }
        binding.recyclerView.adapter = adapter

        // Observe courts from ViewModel and update adapter
        viewModel.courts.observe(viewLifecycleOwner) { courts ->
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
            val availableCheck = popupView.findViewById<com.google.android.material.checkbox.MaterialCheckBox>(R.id.checkbox_available)
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
    }

    private fun applyAllFilters(onSuccess: ((List<Court>) -> Unit)? = null) {
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
            var availableMatch = true
            if (filterAvailableOnly && court.base.courtsAvailable == 0) {
                availableMatch = false
            }
            typeMatch && distanceMatch && availableMatch
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
        adapter.setItems(sorted) {
            binding.recyclerView.scrollToPosition(0)
            onSuccess?.invoke(sorted)
        }
    }

    fun updateUserLocation(location: Location) {
        val lastLocation = lastUserLocation
        var shouldUpdate = lastLocation == null || location.distanceTo(lastLocation) > locationUpdateThresholdMeters
        if (!this::binding.isInitialized || !this::adapter.isInitialized) {
            return
        }
        if (shouldUpdate) {
            adapter.location = location
            lastUserLocation = location
            applyAllFilters() { sorted ->
                //the apply filters only updates position and reuses viewHolders if on screen
                //apply this to update the any detail on the texts in the viewholders
                for (i in 0 until minOf(binding.recyclerView.childCount, sorted.size)) {
                    val court = sorted[i]
                    val child = binding.recyclerView.getChildAt(i)
                    val holder = binding.recyclerView.getChildViewHolder(child) as ViewHolder
                    val geoPoint = court.base.geoPoint
                    val location = lastUserLocation
                    if (geoPoint != null && location != null) {
                        val results = FloatArray(1)
                        Location.distanceBetween(
                            location.latitude, location.longitude,
                            geoPoint.latitude, geoPoint.longitude,
                            results
                        )
                        val distanceKm = results[0] / 1000f
                        holder.distanceView.text = String.format(Locale.getDefault(), "%.1f km", distanceKm)
                    } else {
                        Log.d("debug", court.toString())
                        holder.distanceView.text = "-.- km"
                    }
                }
            }
        }
    }

    private fun onCourtSelected(documentId: String) {
//        val action = HomeFragmentDirections.actionHomeFragmentToCourtDetailFragment(documentId)
//        findNavController().navigate(action)


        (activity as? MainActivity)?.showCourtDetail(documentId)
    }
}