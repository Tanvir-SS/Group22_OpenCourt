package com.example.group22_opencourt.ui.detail

import android.location.Location
import android.os.Bundle
import android.util.Log
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.ImageView
import android.widget.TextView
import android.widget.Toast
import androidx.fragment.app.Fragment
import androidx.lifecycle.ViewModelProvider
import androidx.lifecycle.lifecycleScope
import androidx.recyclerview.widget.LinearLayoutManager
import androidx.recyclerview.widget.RecyclerView
import com.example.group22_opencourt.MainActivity
import com.example.group22_opencourt.R
import com.example.group22_opencourt.model.BasketballCourt
import com.example.group22_opencourt.model.Court
import com.example.group22_opencourt.model.TennisCourt
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch

class CheckInFragment : Fragment() {

    private lateinit var viewModel: CourtDetailViewModel
    private var documentId: String = ""
    private lateinit var adapter: CourtStatusAdapter

    private lateinit var lastVerifiedView : TextView

    override fun onCreateView(
        inflater: LayoutInflater,
        container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View? {
        val view = inflater.inflate(R.layout.fragment_check_in, container, false)
        // Get document ID from arguments
        documentId = arguments?.getString("document_id") ?: "1EYahQs7n7ZUF6qPPAjT"
        viewModel = ViewModelProvider(this, CourtDetailViewModelFactory(documentId)).get(
            CourtDetailViewModel::class.java)
        return view
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)
        val recyclerView = view.findViewById<RecyclerView>(R.id.courts_recycler_view)
        viewModel.courtLiveData.observe(viewLifecycleOwner) { court ->
            val checkInOutButton = view.findViewById<View>(R.id.check_in_apply)

            checkInOutButton.setOnClickListener {
                court?.let {
                    val userLocation = (activity as? MainActivity)?.currentLocation
                    if (userLocation != null) {
                        val courtLocation = Location("").apply {
                            latitude = court.base.geoPoint?.latitude!!
                            longitude = court.base.geoPoint?.longitude!!
                        }
                        val distance = userLocation.distanceTo(courtLocation)

                        if (distance > 200) {
                            Toast.makeText(requireContext(), "Must be Within 200m to Check In or Out.", Toast.LENGTH_SHORT).show()
                            return@setOnClickListener
                        }
                    } else {
                        Toast.makeText(requireContext(), "Unable to Determine your Location.", Toast.LENGTH_SHORT).show()
                        return@setOnClickListener
                    }

                    // Proceed with the existing check-in logic
                    CoroutineScope(Dispatchers.IO).launch {
                        // Update the courtsAvailable field based on the courtStatus list
                        val availableCount = court.base.courtStatus.count { it.courtAvailable }
                        court.base.courtsAvailable = availableCount
                        court.base.lastUpdate = System.currentTimeMillis()

                        // Save the updated court to the database
                        CourtRepository.instance.updateCourt(court) { success ->
                            if (success) {
                                // Update the LiveData to reflect the change in availability
                                CourtRepository.instance.updateCourtInLiveData(court)
                                CoroutineScope(Dispatchers.Main).launch {
                                    Toast.makeText(requireContext(), "Court Availability Updated.", Toast.LENGTH_SHORT).show()
                                    requireActivity().onBackPressed()
                                }
                            } else {
                                CoroutineScope(Dispatchers.Main).launch {
                                    Toast.makeText(requireContext(), "Failed to Update Court Availability.", Toast.LENGTH_SHORT).show()
                                }
                            }
                        }
                    }
                }
            }

            if (court != null) {
                Log.d("debug", court.toString())
                // Title: "{name} ({number of courts})"
                val titleView = view.findViewById<android.widget.TextView>(R.id.check_in_court_title)
                var titleString = ""
                // Set iconType based on court type
                val iconType = when (court) {
                    is TennisCourt -> R.drawable.ic_tennis_ball
                    is BasketballCourt -> R.drawable.ic_basketball_ball
                    else -> R.drawable.ic_launcher_foreground
                }
                val iconView = view.findViewById<ImageView>(R.id.courtcheckin_type_icon)
                iconView.setImageResource(iconType)
                titleString += "${court.base.name} (${court.base.totalCourts})"
                titleView.text = titleString
                // Address
                val addressView = view.findViewById<android.widget.TextView>(R.id.check_in_court_address)
                addressView.text = court.base.address
                // Last Verified (if you want to show it)
                lastVerifiedView = view.findViewById<android.widget.TextView>(R.id.check_in_last_verified)
                setLastVerified(court)

                recyclerView.post {
                    adapter.setItems(court.base.courtStatus)
                }
            }
        }

        val activity = requireActivity()
        if (activity is MainActivity) {
            activity.showBackButton()
        }


        recyclerView.layoutManager = LinearLayoutManager(requireContext())
        adapter = CourtStatusAdapter(emptyList(), lifecycleScope, true)
        recyclerView.adapter = adapter
    }

    private fun setLastVerified(court: Court) {
        val diffMillis = System.currentTimeMillis() - court.base.lastUpdate
        if (diffMillis < 0) {
            lastVerifiedView.text = "Last Verified: just now"
            return
        }

        val minutesTotal = diffMillis / 60_000L
        val minutesInDay = 24 * 60L
        val minutesInYear = 365 * minutesInDay // approximate

        val years = minutesTotal / minutesInYear
        val remainingAfterYears = minutesTotal % minutesInYear

        val days = remainingAfterYears / minutesInDay
        val remainingAfterDays = remainingAfterYears % minutesInDay

        val minutes = remainingAfterDays % 60
        val hours = remainingAfterDays / 60

        val parts = mutableListOf<String>()
        parts.add("Last Verified:")

        if (years > 0) parts.add("$years year${if (years > 1) "s" else ""}")
        if (days > 0) parts.add("$days day${if (days > 1) "s" else ""}")
        if (hours > 0) parts.add("$hours hour${if (hours > 1) "s" else ""}")
        if (minutes > 0 || parts.size == 1) { // always show something
            parts.add("$minutes minute${if (minutes != 1L) "s" else ""}")
        }

        parts.add("ago")
        lastVerifiedView.text = parts.joinToString(" ")
    }
}