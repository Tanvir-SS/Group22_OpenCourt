package com.example.group22_opencourt.ui.main

import android.location.Location
import android.util.Log
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.ImageView
import android.widget.TextView
import androidx.core.content.ContextCompat
import androidx.recyclerview.widget.DiffUtil
import androidx.recyclerview.widget.RecyclerView
import com.example.group22_opencourt.R
import com.example.group22_opencourt.databinding.HomeFragmentItemBinding
import com.example.group22_opencourt.model.BasketballCourt
import com.example.group22_opencourt.model.Court
import com.example.group22_opencourt.model.ImagesRepository
import com.example.group22_opencourt.model.TennisCourt
import com.example.group22_opencourt.model.User
import java.util.Locale

class HomeRecyclerViewAdapter(
    private var courtList: List<Court>,
    private val onItemClick: ((Court) -> Unit)? = null,
    private var currentUser: User? = null
) : RecyclerView.Adapter<HomeRecyclerViewAdapter.ViewHolder>() {

    var location : Location? = null
    // update current user data and notify adapter
    fun updateCurrentUser(user: User?) {
        currentUser = user
        notifyDataSetChanged()
    }

    class ViewHolder(val binding: HomeFragmentItemBinding) : RecyclerView.ViewHolder(binding.root) {
        // initialize all views
        val nameView: TextView = binding.courtNameText
        val cityView: TextView = binding.courtCityText
        val verticalBar : View = binding.verticalColorBar
        val addressView : TextView = binding.courtAddressText
        val imageView: ImageView = binding.courtImageView
        val distanceView : TextView = binding.courtDistanceText
        val availabilityView : TextView = binding.availableCourtsText

    }

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): ViewHolder {
        // Inflate the item layout using View Binding
        return ViewHolder(
            HomeFragmentItemBinding.inflate(
                LayoutInflater.from(parent.context),
                parent,
                false
            )
        )
    }

    override fun onBindViewHolder(holder: ViewHolder, position: Int) {
        // get the court at the given position
        val court = courtList[position]
        holder.binding.root.setOnClickListener {
            // invoke the click listener with the court
            onItemClick?.invoke(court)
        }

        // set up court name, address, availability, distance, and image
        holder.nameView.text = court.base.name
        holder.addressView.text = court.base.address
        // show green bar if available, red if not
        if (court.base.courtsAvailable != 0){
            holder.verticalBar.setBackgroundColor(ContextCompat.getColor(holder.itemView.context, R.color.oc_available))
        } else {
            holder.verticalBar.setBackgroundColor(ContextCompat.getColor(holder.itemView.context, R.color.oc_unavailable))
        }
        holder.nameView.text = court.base.name
        holder.availabilityView.text = holder.itemView.context.getString(
            R.string.court_availability,
  court.base.courtsAvailable,
                court.base.totalCourts)
        val geoPoint = court.base.geoPoint
        val userLocation = location // fix smart cast issue
        if (geoPoint != null && userLocation != null) {
            // calculate distance between user and court
            val results = FloatArray(1)
            Location.distanceBetween(
                userLocation.latitude, userLocation.longitude,
                geoPoint.latitude, geoPoint.longitude,
                results
            )
            val distanceKm = results[0] / 1000f
            holder.distanceView.text = String.format(Locale.getDefault(), "%.1f km", distanceKm)
        } else {
            holder.distanceView.text = "-.- km"
        }

        // load court image for basketball and tennis courts
        when (court) {
            // use default image if no photoUri
            is BasketballCourt -> {
                if (court.base.photoUri.isNotEmpty() && court.base.photoUri != ImagesRepository.URI_NON_EXIST){
                    ImagesRepository.instance.loadCourtPhoto(
                        holder.imageView.context, court.base.photoUri, holder.imageView)
                } else {
                    holder.imageView.setImageResource(R.drawable.basketballcourtexample)
                }

            }
            // use default image if no photoUri
            is TennisCourt -> {
                if (court.base.photoUri.isNotEmpty() && court.base.photoUri != ImagesRepository.URI_NON_EXIST) {
                    ImagesRepository.instance.loadCourtPhoto(
                        holder.imageView.context, court.base.photoUri, holder.imageView
                    )
                } else {
                    holder.imageView.setImageResource(R.drawable.example_tennis_court)
                }
            }
        }

        // set up icon type from court type
        val iconRes = when (court.type) {
            "tennis" -> R.drawable.ic_tennis_ball
            "basketball" -> R.drawable.ic_basketball_ball
            else -> R.drawable.ic_launcher_foreground
        }
        holder.binding.courtTypeIcon.setImageResource(iconRes)

        // if court is favorite, show the favorite icon
        val favourites = currentUser?.favourites
        Log.d("HomeRecycler", "favourites: $favourites")
        val isFavourite = favourites?.contains(court.base.id)
        Log.d("HomeRecycler", "isFavourite: $isFavourite")
        if (isFavourite == true) {
            holder.binding.favouriteIcon.visibility = View.VISIBLE
        } else {
            holder.binding.favouriteIcon.visibility = View.INVISIBLE
        }
    }

    override fun getItemCount(): Int {
        // return the size of the court list
        return courtList.size
    }

    fun setItems(newList: List<Court>, onSuccess: (() -> Unit)? = null) {
        val oldList = courtList.toList() // Make a copy for thread safety

        // Use DiffUtil to calculate the differences between old and new lists
        val diffCallback = object : DiffUtil.Callback() {
            override fun getOldListSize() = oldList.size
            override fun getNewListSize() = newList.size
            override fun areItemsTheSame(oldItemPosition: Int, newItemPosition: Int): Boolean {
                return oldList[oldItemPosition].base.id == newList[newItemPosition].base.id
            }

            override fun areContentsTheSame(
                oldItemPosition: Int,
                newItemPosition: Int
            ): Boolean {
                return oldList[oldItemPosition] == newList[newItemPosition]
            }
        }
        val diffResult = DiffUtil.calculateDiff(diffCallback)

        courtList = newList
        Log.d("court", "happen here")
        diffResult.dispatchUpdatesTo(this@HomeRecyclerViewAdapter)
        onSuccess?.invoke()
    }

    fun getFullList(): List<Court> {
        return courtList
    }

}