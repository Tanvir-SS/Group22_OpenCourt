package com.example.group22_opencourt.ui.detail

import CourtRepository
import android.graphics.Color
import android.os.Bundle
import android.util.Log
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.Button
import androidx.core.content.ContextCompat
import androidx.fragment.app.Fragment
import androidx.lifecycle.ViewModelProvider
import androidx.lifecycle.lifecycleScope
import androidx.navigation.fragment.findNavController
import androidx.navigation.fragment.navArgs
import androidx.recyclerview.widget.DiffUtil
import androidx.recyclerview.widget.LinearLayoutManager
import androidx.recyclerview.widget.RecyclerView
import com.example.group22_opencourt.MainActivity
import com.example.group22_opencourt.R
import com.example.group22_opencourt.model.TennisCourt
import com.example.group22_opencourt.model.BasketballCourt
import com.example.group22_opencourt.model.Court
import com.example.group22_opencourt.model.CourtStatus
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext


class CourtDetailFragment : Fragment() {
    private lateinit var viewModel: CourtDetailViewModel
    private lateinit var courtsRecyclerView : RecyclerView
//    private val args: CourtDetailFragmentArgs by navArgs()
    private lateinit var adapter: CourtStatusAdapter

    private var documentId = ""

    override fun onCreateView(
        inflater: LayoutInflater,
        container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View? {
        val view = inflater.inflate(R.layout.fragment_court_detail, container, false)
        // Get document ID from arguments
        documentId = arguments?.getString("document_id") ?: "1EYahQs7n7ZUF6qPPAjT"
        // Set up ViewModel
        viewModel = ViewModelProvider(this, CourtDetailViewModelFactory(documentId)).get(CourtDetailViewModel::class.java)
        courtsRecyclerView = view.findViewById<RecyclerView>(R.id.courts_recycler_view)
        return view
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)
        courtsRecyclerView.layoutManager = LinearLayoutManager(requireContext())
        adapter = CourtStatusAdapter(emptyList(), lifecycleScope)
        courtsRecyclerView.adapter = adapter
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
                adapter.setItems(court.base.courtStatus)


                // Update map image
                 CourtRepository.loadMapPhoto(court, view.findViewById(R.id.map_image))
            }
        }
        val editCourtButton : Button = view.findViewById(R.id.edit_court_button)
        editCourtButton.setOnClickListener {
            val args = Bundle().apply {
                putString("document_id", documentId)
            }
            findNavController().navigate(R.id.action_courtDetailFragment_to_editCourtFragment, args)
        }

        val checkInButton : Button = view.findViewById(R.id.check_in_button)
        checkInButton.setOnClickListener {
            val args = Bundle().apply {
                putString("document_id", documentId)
            }
            findNavController().navigate(R.id.action_courtDetailFragment_to_checkInFragment, args)
        }
        val activity = requireActivity()
        if (activity is MainActivity) {
            activity.showBackButton()
        }
    }

    companion object {
        fun newInstance(documentId: String): CourtDetailFragment {
            val fragment = CourtDetailFragment()
            fragment.arguments = Bundle().apply { putString("document_id", documentId) }
            return fragment
        }
    }
}


class CourtStatusAdapter(private var items: List<CourtStatus>, private val lifecycleScope: CoroutineScope) : RecyclerView.Adapter<CourtStatusAdapter.ViewHolder>() {
    class ViewHolder(val view: android.view.View) : RecyclerView.ViewHolder(view) {
        val colorSquare = view.findViewById<View>(R.id.status_color_square)
        val nameView = view.findViewById<android.widget.TextView>(R.id.court_name)
        val statusView = view.findViewById<android.widget.TextView>(R.id.court_status)
    }

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): ViewHolder {
        val inflater = LayoutInflater.from(parent.context)
        val view = inflater.inflate(R.layout.item_court_status, parent, false)
        return ViewHolder(view)
    }

    override fun onBindViewHolder(holder: ViewHolder, position: Int) {
        val courtStatus = items[position]
        holder.nameView.text = "Court ${position + 1}"
        holder.statusView.text = if (courtStatus.courtAvailable) "Available" else "In Play"
        // Set color bar based on status
        if (courtStatus.courtAvailable)  {
            holder.colorSquare.setBackgroundColor(ContextCompat.getColor(holder.itemView.context, R.color.oc_available))
        } else {
            holder.colorSquare.setBackgroundColor(ContextCompat.getColor(holder.itemView.context, R.color.oc_unavailable))
        }

    }

    override fun getItemCount(): Int = items.size

    fun setItems(newList: List<CourtStatus>, onSuccess: (() -> Unit)? = null) {
        val oldList = items // Make a copy for thread safety
        lifecycleScope.launch(Dispatchers.Default) {
            val diffCallback = object : DiffUtil.Callback() {
                override fun getOldListSize() = oldList.size
                override fun getNewListSize() = newList.size
                override fun areItemsTheSame(oldItemPosition: Int, newItemPosition: Int): Boolean {
                    return oldList[oldItemPosition] == newList[newItemPosition]
                }

                override fun areContentsTheSame(
                    oldItemPosition: Int,
                    newItemPosition: Int
                ): Boolean {
                    return oldList[oldItemPosition] == newList[newItemPosition]
                }
            }
            val diffResult = DiffUtil.calculateDiff(diffCallback)
            withContext(Dispatchers.Main) {
                items = newList
                diffResult.dispatchUpdatesTo(this@CourtStatusAdapter)
                onSuccess?.invoke()
            }
        }
    }
}
