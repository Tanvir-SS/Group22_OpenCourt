package com.example.group22_opencourt.ui.detail

import CourtRepository
import android.os.Bundle
import android.util.Log
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import androidx.fragment.app.Fragment
import androidx.lifecycle.ViewModelProvider
import com.example.group22_opencourt.R
import com.example.group22_opencourt.model.TennisCourt
import com.example.group22_opencourt.model.BasketballCourt

class CourtDetailFragment : Fragment() {
    private lateinit var viewModel: CourtDetailViewModel

    override fun onCreateView(
        inflater: LayoutInflater,
        container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View? {
        val view = inflater.inflate(R.layout.fragment_court_detail, container, false)
        // Get document ID from arguments
        val documentId = arguments?.getString("document_id") ?: "1EYahQs7n7ZUF6qPPAjT"
        // Set up ViewModel
        viewModel = ViewModelProvider(this, CourtDetailViewModelFactory(documentId)).get(CourtDetailViewModel::class.java)
        return view
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)
        viewModel.courtLiveData.observe(viewLifecycleOwner) { court ->
            Log.d("debug", "message")
            if (court != null) {
                Log.d("debug", court.toString())
                // Title: "{name} ({number of courts})"
                val titleView = view.findViewById<android.widget.TextView>(R.id.court_title)
                var titleString = ""
                when (court) {
                    is TennisCourt -> titleString = "Tennis - "
                    is BasketballCourt -> titleString = "Basketball - "
                }
                titleString += "${court.base.name} (${court.base.totalCourts})"
                titleView.text = titleString
                // Address
                val addressView = view.findViewById<android.widget.TextView>(R.id.court_address)
                addressView.text = court.base.address
                // Last Verified (if you want to show it)
                val lastVerifiedView = view.findViewById<android.widget.TextView>(R.id.last_verified)
                // lastVerifiedView.text = "Last Verified: ${court.base.lastVerified}" // Uncomment if available

                // Build amenities string
                val amenitiesList = mutableListOf<String>()
                court.base.lights?.let { amenitiesList.add("Lights: ${if (it) "✓" else "✗"}") }
                court.base.indoor?.let { amenitiesList.add("Indoor: ${if (it) "✓" else "✗"}") }
                court.base.washroom?.let { amenitiesList.add("Washroom: ${if (it) "✓" else "✗"}") }
                court.base.accessibility?.let { amenitiesList.add("Accessibility: ${if (it) "✓" else "✗"}") }
                when (court) {
                    is TennisCourt -> court.practiceWall?.let { amenitiesList.add("Practice Wall: ${if (it) "✓" else "✗"}") }
                    is BasketballCourt -> court.nets?.let { amenitiesList.add("Nets: ${if (it) "✓" else "✗"}") }
                }
                val amenitiesView = view.findViewById<android.widget.TextView>(R.id.amenities_list)
                amenitiesView.text = if (amenitiesList.isNotEmpty()) amenitiesList.joinToString(" · ") else "None"

                // Update courts list RecyclerView (uncomment and implement CourtStatusAdapter)
                // val courtsRecyclerView = view.findViewById<androidx.recyclerview.widget.RecyclerView>(R.id.courts_recycler_view)
                // courtsRecyclerView.adapter = CourtStatusAdapter(court.base.courtStatus)

                // Update map image
                 CourtRepository.loadMapPhoto(court, view.findViewById(R.id.map_image))
            }
        }
    }

}
