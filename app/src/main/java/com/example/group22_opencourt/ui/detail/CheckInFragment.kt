package com.example.group22_opencourt.ui.detail

import android.os.Bundle
import android.util.Log
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.ImageView
import android.widget.Toast
import androidx.fragment.app.Fragment
import androidx.lifecycle.ViewModelProvider
import androidx.lifecycle.lifecycleScope
import androidx.recyclerview.widget.LinearLayoutManager
import androidx.recyclerview.widget.RecyclerView
import com.example.group22_opencourt.MainActivity
import com.example.group22_opencourt.R
import com.example.group22_opencourt.model.BasketballCourt
import com.example.group22_opencourt.model.TennisCourt
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch

class CheckInFragment : Fragment() {

    private lateinit var viewModel: CourtDetailViewModel
    private var documentId: String = ""
    private lateinit var adapter: CourtStatusAdapter

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

        viewModel.courtLiveData.observe(viewLifecycleOwner) { court ->
            val checkInOutButton = view.findViewById<View>(R.id.check_in_apply)

            checkInOutButton.setOnClickListener {
                court?.let {
                    // Iterate over courtStatus and toggle availability for all courts based on adapter state
                    CoroutineScope(Dispatchers.IO).launch {
                        // Update the courtsAvailable field based on the courtStatus list
                        val availableCount = court.base.courtStatus.count { it.courtAvailable }
                        court.base.courtsAvailable = availableCount

                        // Save the updated court to the database
                        CourtRepository.instance.updateCourt(court) { success ->
                            if (success) {
                                // Update the LiveData to reflect the change in availability
                                CourtRepository.instance.updateCourtInLiveData(court)
                                CoroutineScope(Dispatchers.Main).launch {
                                    Toast.makeText(requireContext(), "Courts Updated.", Toast.LENGTH_SHORT).show()
                                    requireActivity().onBackPressed()
                                }
                            } else {
                                CoroutineScope(Dispatchers.Main).launch {
                                    Toast.makeText(requireContext(), "Failed to Update Courts.", Toast.LENGTH_SHORT).show()
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
                val lastVerifiedView = view.findViewById<android.widget.TextView>(R.id.check_in_last_verified)
                // lastVerifiedView.text = "Last Verified: ${court.base.lastVerified}" // Uncomment if available

                adapter.setItems(court.base.courtStatus)
            }
        }

        val activity = requireActivity()
        if (activity is MainActivity) {
            activity.showBackButton()
        }

        val recyclerView = view.findViewById<RecyclerView>(R.id.courts_recycler_view)
        recyclerView.layoutManager = LinearLayoutManager(requireContext())
        adapter = CourtStatusAdapter(emptyList(), lifecycleScope, true)
        recyclerView.adapter = adapter
    }
}