package com.example.group22_opencourt.ui.detail

import CourtRepository
import android.os.Bundle
import android.util.Log
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.Button
import android.widget.ImageView
import android.widget.TextView
import androidx.core.content.ContextCompat
import androidx.fragment.app.Fragment
import androidx.lifecycle.ViewModelProvider
import androidx.lifecycle.lifecycleScope
import androidx.navigation.fragment.findNavController
import androidx.recyclerview.widget.DiffUtil
import androidx.recyclerview.widget.LinearLayoutManager
import androidx.recyclerview.widget.RecyclerView
import com.example.group22_opencourt.MainActivity
import com.example.group22_opencourt.R
import com.example.group22_opencourt.model.BasketballCourt
import com.example.group22_opencourt.model.Court
import com.example.group22_opencourt.model.CourtStatus
import com.example.group22_opencourt.model.TennisCourt
import com.google.android.gms.maps.CameraUpdateFactory
import com.google.android.gms.maps.GoogleMap
import com.google.android.gms.maps.OnMapReadyCallback
import com.google.android.gms.maps.SupportMapFragment
import com.google.android.gms.maps.model.LatLng
import com.google.android.gms.maps.model.MarkerOptions
import com.google.firebase.firestore.GeoPoint
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext

//for weather

import android.location.Geocoder
import android.widget.ProgressBar
import java.util.Locale

import android.Manifest
import android.content.Intent
import android.content.pm.PackageManager
import android.os.Build
import android.widget.Toast
import androidx.activity.result.contract.ActivityResultContracts
import com.example.group22_opencourt.model.User
import com.example.group22_opencourt.model.UserRepository


class CourtDetailFragment : Fragment(), OnMapReadyCallback {
    private lateinit var viewModel: CourtDetailViewModel
    private lateinit var courtsRecyclerView : RecyclerView
//    private val args: CourtDetailFragmentArgs by navArgs()
    private lateinit var adapter: CourtStatusAdapter

    private var pendingLatLng: LatLng? = null

    private var map : GoogleMap? = null

    private lateinit var lastVerifiedTextView : TextView
    private var documentId = ""

    private var lastWeatherAddress: String? = null

    private var pendingStart: (() -> Unit)? = null

    private lateinit var favouriteImgView : ImageView

    private var favourited = false

    private var currUser : User? = null

    private var court : Court? = null

    private val requestNotifPermission =
        registerForActivityResult(ActivityResultContracts.RequestPermission()) { granted ->
            if (granted) pendingStart?.invoke()
            pendingStart = null
        }

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
        val mapFragment = childFragmentManager.findFragmentById(R.id.map_fragment_lite) as? SupportMapFragment
        mapFragment?.getMapAsync(this)
        courtsRecyclerView.layoutManager = LinearLayoutManager(requireContext())
        lastVerifiedTextView = view.findViewById(R.id.last_verified)
        adapter = CourtStatusAdapter(emptyList(), lifecycleScope)
        courtsRecyclerView.adapter = adapter
        favouriteImgView = view.findViewById(R.id.heartImage)

        UserRepository.instance.currentUser.observe(viewLifecycleOwner) {user ->
            Log.d("details", user.toString())
            if (user == null) {
                favouriteImgView.setImageResource(R.drawable.heart_outline)
                return@observe
            }
            currUser = user
            if (user.favourites.contains(documentId)) {
                favourited = true
                favouriteImgView.setImageResource(R.drawable.heart_filled)
            } else {
                favourited= false
                 favouriteImgView.setImageResource(R.drawable.heart_outline)
            }
        }

        //Weather UI (NEW)
        val weatherValue = view.findViewById<android.widget.TextView>(R.id.weather_value)
        val weatherProgress = view.findViewById<android.widget.ProgressBar>(R.id.weather_progress)

        // Used to avoid geocoding + refetching repeatedly for the same court/address

        viewModel.weather.observe(viewLifecycleOwner) { state ->
            when (state) {
                is WeatherUiState.Idle -> {
                    weatherProgress.visibility = View.GONE
                    weatherValue.text = "—"
                }
                is WeatherUiState.Loading -> {
                    weatherProgress.visibility = View.VISIBLE
                    weatherValue.text = "Loading…"
                }
                is WeatherUiState.Ready -> {
                    weatherProgress.visibility = View.GONE
                    val w = state.weather
                    val emoji = weatherCodeToEmoji(w.weatherCode)
                    val temp = String.format(Locale.getDefault(), "%.0f", w.tempC)
                    val wind = String.format(Locale.getDefault(), "%.0f", w.windKmh)

                    weatherValue.text = "$emoji $temp°C · ${w.description} · Wind $wind km/h"

                }
                is WeatherUiState.Error -> {
                    weatherProgress.visibility = View.GONE
                    weatherValue.text = state.message
                }
            }
        }


        viewModel.courtLiveData.observe(viewLifecycleOwner) { court ->
            Log.d("map", "court loaded")
            this@CourtDetailFragment.court = court
            if (court != null) {
                val icon : ImageView = view.findViewById(R.id.court_icon_type)
                when (court) {
                    is TennisCourt -> icon.setImageResource(R.drawable.ic_tennis_ball)
                    is BasketballCourt -> icon.setImageResource(R.drawable.ic_basketball_ball)
                }
                setLastVerified(court)
                val geoPoint : GeoPoint? = court.base.geoPoint
                if (map != null && geoPoint != null) {
                    val latLng = LatLng(geoPoint.latitude, geoPoint.longitude)
                    map!!.moveCamera(CameraUpdateFactory.newLatLngZoom(latLng, 15f))
                    map!!.addMarker(MarkerOptions().position(latLng))
                } else {
                    if (geoPoint != null) {
                        pendingLatLng = LatLng(geoPoint.latitude, geoPoint.longitude)
                    }
                }
                Log.d("debug", court.toString())
                if (court.base.courtsAvailable == 0) {
                    val activity = requireActivity()
                    if (activity is MainActivity) {
                        activity.showToolBarButton("Notify When\navailable") {
                            startNotificationService(court)
                        }
                    }
                } else {
                    val activity = requireActivity()
                    if (activity is MainActivity) {
                        activity.hideToolBarButton()
                    }
                }
                // Title: "{name} ({number of courts})"
                val titleView = view.findViewById<android.widget.TextView>(R.id.court_title)
                var titleString = ""
                titleString += "${court.base.name} (${court.base.totalCourts})"
                titleView.text = titleString
                // Address
                val addressView = view.findViewById<android.widget.TextView>(R.id.court_address)
                addressView.text = court.base.address

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

                // Weather fetch trigger
                val addr = court.base.address
                if (addr.isNotBlank() && addr != lastWeatherAddress) {
                    lastWeatherAddress = addr
                    viewModel.setWeatherLoading()

                    lifecycleScope.launch(Dispatchers.IO) {
                        val latLon: Pair<Double, Double>? = try {
                            val geocoder = android.location.Geocoder(requireContext(), java.util.Locale.getDefault())
                            val results = geocoder.getFromLocationName(addr, 1)
                            val loc = results?.firstOrNull()
                            if (loc != null) Pair(loc.latitude, loc.longitude) else null
                        } catch (_: Exception) {
                            null
                        }

                        withContext(Dispatchers.Main) {
                            if (latLon == null) {
                                viewModel.setWeatherError("Weather unavailable")
                            } else {
                                viewModel.loadWeather(latLon.first, latLon.second)
                            }
                        }
                    }
                }
            }
        }

        favouriteImgView.setOnClickListener {
            Log.d("details", "image clicked")
            val currCourt = court
            if (currCourt == null) {
                return@setOnClickListener
            }
            val userInstance = currUser
            if (userInstance == null) {
                return@setOnClickListener
            }
            if (favourited) {
                userInstance.favourites.remove(currCourt.base.id)
                UserRepository.instance.createOrUpdateUser(userInstance) {
                    if (it) {
                        Toast.makeText(requireContext(), "unfavorited court", Toast.LENGTH_SHORT).show()
                    }
                }
            } else {
                userInstance.favourites.add(0, currCourt.base.id)
                UserRepository.instance.createOrUpdateUser(userInstance) {
                    if (it) {
                        Toast.makeText(requireContext(), "court added to favourites", Toast.LENGTH_SHORT).show()
                    }
                }
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

    private fun setLastVerified(court : Court) {
        val millis = court.base.lastUpdate
        var remainingMillis = System.currentTimeMillis() - millis

        val minutesInMilli = 60 * 1000L
        val hoursInMilli = 60 * minutesInMilli
        val daysInMilli = 24 * hoursInMilli
        val yearsInMilli = 365 * daysInMilli  // approximate

        val years = remainingMillis / yearsInMilli
        remainingMillis %= yearsInMilli

        val days = remainingMillis / daysInMilli
        remainingMillis %= daysInMilli

        val hours = remainingMillis / hoursInMilli
        remainingMillis %= hoursInMilli

        val minutes = remainingMillis / minutesInMilli

        val parts = mutableListOf<String>()
        parts.add("Last Verified:")

        if (years > 0) parts.add("$years year${if (years > 1) "s" else ""}")
        if (days > 0) parts.add("$days day${if (days > 1) "s" else ""}")

        // Always show minutes if there are years or days
        if (years > 0 || days > 0 || minutes > 0) {
            val totalMinutes = if (years > 0 || days > 0) {
                // include leftover hours as minutes
                minutes + hours * 60
            } else {
                minutes
            }
            parts.add("$totalMinutes minute${if (totalMinutes > 1) "s" else ""}")
        }
        parts.add("ago")
        Log.d("debug", parts.joinToString(" "))
        lastVerifiedTextView.text = parts.joinToString(" ")
    }
    private fun weatherCodeToEmoji(code: Int): String = when (code) {
        0 -> "☀️"
        1, 2, 3 -> "⛅"
        45, 48 -> "🌫️"
        51, 53, 55 -> "🌦️"
        61, 63, 65 -> "🌧️"
        71, 73, 75 -> "🌨️"
        80, 81, 82 -> "🌦️"
        95, 96, 99 -> "⛈️"
        else -> "🌡️"
    }

    private fun startNotificationService(court: Court) {
        val start = {
            val i = Intent(requireContext(), com.example.group22_opencourt.model.CourtAvailabilityService::class.java).apply {
                putExtra("extra_document_id", documentId)
                putExtra("extra_court_name", court.base.name)
            }
            ContextCompat.startForegroundService(requireContext(), i)
            Toast.makeText(requireContext(), "Monitoring Availability…", Toast.LENGTH_SHORT).show()
        }

        if (Build.VERSION.SDK_INT >= 33 &&
            ContextCompat.checkSelfPermission(requireContext(), Manifest.permission.POST_NOTIFICATIONS)
            != PackageManager.PERMISSION_GRANTED
        ) {
            pendingStart = start
            requestNotifPermission.launch(Manifest.permission.POST_NOTIFICATIONS)

        } else {
            start()
        }
    }

    override fun onMapReady(map: GoogleMap) {
        this@CourtDetailFragment.map = map
        // The Lite Mode map is automatically unresponsive.
        Log.d("map", "map loaded")
        pendingLatLng?.let {
            map.moveCamera(CameraUpdateFactory.newLatLngZoom(it, 15f))
            map.addMarker(MarkerOptions().position(it))
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
        val oldList = items.toList() // Make a copy for thread safety

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

        items = newList
        diffResult.dispatchUpdatesTo(this@CourtStatusAdapter)
        onSuccess?.invoke()


    }
}
