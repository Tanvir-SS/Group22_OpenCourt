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
import com.example.group22_opencourt.model.TennisCourt
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.GlobalScope
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import java.util.Locale
import kotlin.collections.get

class HomeRecyclerViewAdapter(
    private var courtList: List<Court>, private var lifecycleScope: CoroutineScope
) : RecyclerView.Adapter<HomeRecyclerViewAdapter.ViewHolder>() {

    var location : Location? = null

    class ViewHolder(binding: HomeFragmentItemBinding) : RecyclerView.ViewHolder(binding.root) {
        val nameView: TextView = binding.courtNameText
        val cityView: TextView = binding.courtCityText
        val verticalBar : View = binding.verticalColorBar
        val addressView : TextView = binding.courtAddressText
        val imageView: ImageView = binding.courtImageView
        val distanceView : TextView = binding.courtDistanceText

        val availabilityView : TextView = binding.availableCourtsText
    }

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): ViewHolder {
        return ViewHolder(
            HomeFragmentItemBinding.inflate(
                LayoutInflater.from(parent.context),
                parent,
                false
            )
        )
    }

    override fun onBindViewHolder(holder: ViewHolder, position: Int) {
        val court = courtList[position]
        var nameStr = ""
        holder.nameView.text = court.base.name
//        holder.cityView.text = court.base.city
        holder.addressView.text = court.base.address
        when (court) {
            is BasketballCourt -> nameStr = "Basketball - "
            is TennisCourt -> nameStr = "Tennis - "
        }
        if (court.base.courtsAvailable > 0){
            holder.verticalBar.setBackgroundColor(ContextCompat.getColor(holder.itemView.context, R.color.oc_available))
        } else {
            holder.verticalBar.setBackgroundColor(ContextCompat.getColor(holder.itemView.context, R.color.oc_unavailable))
        }
        holder.nameView.text = nameStr + court.base.name
        holder.availabilityView.text = holder.itemView.context.getString(
            R.string.court_availability,
  court.base.courtsAvailable,
                court.base.totalCourts)
        val geoPoint = court.base.geoPoint
        val userLocation = location // fix smart cast issue
        if (geoPoint != null && userLocation != null) {
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


        when (court) {
            is BasketballCourt -> {
                if (court.base.photoURL.isNotEmpty()){
                    CourtRepository.loadPhoto(court, holder.imageView)
                } else {
                    holder.imageView.setImageResource(R.drawable.basketballcourtexample)
                }

            }
            is TennisCourt ->  if (court.base.photoURL.isNotEmpty()){
                CourtRepository.loadPhoto(court, holder.imageView)
            } else {
                holder.imageView.setImageResource(R.drawable.example_tennis_court)
            }
        }
    }

    override fun getItemCount(): Int {
        return courtList.size
    }

    fun setItems(newList: List<Court>, onSuccess: (() -> Unit)? = null) {
        val oldList = courtList // Make a copy for thread safety
        lifecycleScope.launch(Dispatchers.Default) {
            val diffCallback = object : DiffUtil.Callback() {
                override fun getOldListSize() = oldList.size
                override fun getNewListSize() = newList.size
                override fun areItemsTheSame(oldItemPosition: Int, newItemPosition: Int): Boolean {
                    return oldList[oldItemPosition].base.name == newList[newItemPosition].base.name
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
                courtList = newList
                diffResult.dispatchUpdatesTo(this@HomeRecyclerViewAdapter)
                onSuccess?.invoke()
            }
        }
    }

    fun getFullList(): List<Court> {
        return courtList
    }

}