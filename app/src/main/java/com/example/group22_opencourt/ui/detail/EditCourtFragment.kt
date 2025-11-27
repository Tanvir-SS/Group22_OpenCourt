package com.example.group22_opencourt.ui.detail

import android.os.Bundle
import android.util.Log
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.Button
import android.widget.CheckBox
import android.widget.LinearLayout
import android.widget.Toast
import androidx.fragment.app.Fragment
import androidx.lifecycle.ViewModelProvider
import com.example.group22_opencourt.R
import com.example.group22_opencourt.model.BasketballCourt
import com.example.group22_opencourt.model.Court
import com.example.group22_opencourt.model.TennisCourt
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch

class EditCourtFragment : Fragment() {
    private lateinit var viewModel: CourtDetailViewModel
    private var documentId: String = ""
    // initialize all checkboxes and layouts
    private lateinit var checkboxLights: CheckBox
    private lateinit var checkboxIndoor: CheckBox
    private lateinit var checkboxWashroom: CheckBox
    private lateinit var checkboxAccessibility: CheckBox
    private lateinit var checkboxPracticeWall: CheckBox
    private lateinit var checkboxNets: CheckBox
    private lateinit var lightsLayout: LinearLayout
    private lateinit var indoorLayout: LinearLayout
    private lateinit var washroomLayout: LinearLayout
    private lateinit var accessibilityLayout: LinearLayout
    private lateinit var layoutTennisAmenities: LinearLayout
    private lateinit var layoutBasketballAmenities: LinearLayout

    override fun onCreateView(
        inflater: LayoutInflater,
        container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View? {
        val view = inflater.inflate(R.layout.fragment_edit_court, container, false)
        // Get document ID from arguments
        documentId = arguments?.getString("document_id") ?: ""
        viewModel = ViewModelProvider(this, CourtDetailViewModelFactory(documentId)).get(
            CourtDetailViewModel::class.java)
        return view
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)
        viewModel.courtLiveData.observe(viewLifecycleOwner) { court ->
            Log.d("debug", "message")
            if (court != null) {
                Log.d("debug", court.toString())
                // Title: "{name} ({number of courts})"
                val titleView = view.findViewById<android.widget.TextView>(R.id.edit_court_title)
                var titleString = ""
                when (court) {
                    is TennisCourt -> titleString = "Tennis - "
                    is BasketballCourt -> titleString = "Basketball - "
                }
                titleString += "${court.base.name} (${court.base.totalCourts})"
                titleView.text = titleString
                // Address
                val addressView = view.findViewById<android.widget.TextView>(R.id.edit_court_address)
                addressView.text = court.base.address
                // Last Verified (if you want to show it)
                val lastVerifiedView = view.findViewById<android.widget.TextView>(R.id.edit_last_verified)
                // lastVerifiedView.text = "Last Verified: ${court.base.lastVerified}" // Uncomment if available
            }
        }

        // Initialize checkboxes and layouts
        checkboxLights = view.findViewById(R.id.edit_checkboxLights)
        checkboxIndoor = view.findViewById(R.id.edit_checkboxIndoor)
        checkboxWashroom = view.findViewById(R.id.edit_checkboxWashroom)
        checkboxAccessibility = view.findViewById(R.id.edit_checkboxAccessibility)
        checkboxPracticeWall = view.findViewById(R.id.edit_checkboxPracticeWall)
        checkboxNets = view.findViewById(R.id.edit_checkboxNets)
        lightsLayout = view.findViewById(R.id.edit_lights_layout)
        indoorLayout = view.findViewById(R.id.edit_indoor_layout)
        washroomLayout = view.findViewById(R.id.edit_washrooms_layout)
        accessibilityLayout = view.findViewById(R.id.edit_accessibility_layout)
        layoutTennisAmenities = view.findViewById(R.id.edit_layoutTennisAmenities)
        layoutBasketballAmenities = view.findViewById(R.id.edit_layoutBasketballAmenities)
        // Apply Changes button
        val applyChangesButton = view.findViewById<Button>(R.id.edit_buttonApply)

        // Populate amenities based on court data
        viewModel.courtLiveData.observe(viewLifecycleOwner) { court ->
            if (court != null) {
                populateAmenities(court)
                applyChangesButton.setOnClickListener {
                    saveChanges(court)
                }
            }
        }

        setupLayoutClickListeners()
    }

    private fun populateAmenities(court: Court) {
        // Set checkboxes based on court data
        checkboxLights.isChecked = court.base.lights == true
        checkboxIndoor.isChecked = court.base.indoor == true
        checkboxWashroom.isChecked = court.base.washroom == true
        checkboxAccessibility.isChecked = court.base.accessibility == true

        // Show/hide sport-specific amenities
        if (court is TennisCourt) {
            layoutTennisAmenities.visibility = View.VISIBLE
            layoutBasketballAmenities.visibility = View.GONE
            checkboxPracticeWall.isChecked = court.practiceWall == true
        } else if (court is BasketballCourt) {
            layoutTennisAmenities.visibility = View.GONE
            layoutBasketballAmenities.visibility = View.VISIBLE
            checkboxNets.isChecked = court.nets == true
        } else {
            layoutTennisAmenities.visibility = View.GONE
            layoutBasketballAmenities.visibility = View.GONE
        }
    }

    private fun saveChanges(court: Court) {
        // Update court object with checkbox values
        court.base.lights = checkboxLights.isChecked
        court.base.indoor = checkboxIndoor.isChecked
        court.base.washroom = checkboxWashroom.isChecked
        court.base.accessibility = checkboxAccessibility.isChecked

        // Sport-specific amenities
        if (court is TennisCourt) {
            court.practiceWall = checkboxPracticeWall.isChecked
        } else if (court is BasketballCourt) {
            court.nets = checkboxNets.isChecked
        }

        // Save updated court to repository
        CoroutineScope(Dispatchers.IO).launch {
            CourtRepository.instance.updateCourt(court)
            CoroutineScope(Dispatchers.Main).launch {
                Toast.makeText(requireContext(), "Changes Saved", Toast.LENGTH_SHORT).show()
                requireActivity().onBackPressed()
            }
        }
    }

    private fun setupLayoutClickListeners() {
        // Toggle checkboxes when layouts are clicked not just checkboxes
        lightsLayout.setOnClickListener {
            checkboxLights.isChecked = !checkboxLights.isChecked
        }
        indoorLayout.setOnClickListener {
            checkboxIndoor.isChecked = !checkboxIndoor.isChecked
        }
        washroomLayout.setOnClickListener {
            checkboxWashroom.isChecked = !checkboxWashroom.isChecked
        }
        accessibilityLayout.setOnClickListener {
            checkboxAccessibility.isChecked = !checkboxAccessibility.isChecked
        }
        layoutTennisAmenities.setOnClickListener {
            if (::checkboxPracticeWall.isInitialized) {
                checkboxPracticeWall.isChecked = !checkboxPracticeWall.isChecked
            }
        }
        layoutBasketballAmenities.setOnClickListener {
            if (::checkboxNets.isInitialized) {
                checkboxNets.isChecked = !checkboxNets.isChecked
            }
        }
    }
}
