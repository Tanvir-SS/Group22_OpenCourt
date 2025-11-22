package com.example.group22_opencourt.ui.main

import android.location.Location
import android.view.LayoutInflater
import android.view.ViewGroup
import android.widget.ImageView
import android.widget.TextView
import androidx.recyclerview.widget.RecyclerView
import com.example.group22_opencourt.R
import com.example.group22_opencourt.databinding.HomeFragmentItemBinding
import com.example.group22_opencourt.model.BasketballCourt
import com.example.group22_opencourt.model.Court
import com.example.group22_opencourt.model.TennisCourt
import java.util.Locale

class HomeRecyclerViewAdapter(
    private var fullList: List<Court>
) : RecyclerView.Adapter<HomeRecyclerViewAdapter.ViewHolder>() {

    private var displayList: List<Court> = fullList
    private var showTennis = true
    private var showBasketball = true

    var location : Location? = null

    class ViewHolder(binding: HomeFragmentItemBinding) : RecyclerView.ViewHolder(binding.root) {
        val nameView: TextView = binding.courtNameText
        val cityView: TextView = binding.courtCityText
        val addressView : TextView = binding.courtAddressText
        val imageView: ImageView = binding.courtImageView
        val distanceView : TextView = binding.courtDistanceText
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
        val court = displayList[position]
        holder.nameView.text = court.base.name
//        holder.cityView.text = court.base.city
        holder.addressView.text = court.base.address
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
            holder.distanceView.text = "-"
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
        return displayList.size
    }

//    fun sort() {
//        fullList = fullList.sortedBy { it.base.name.lowercase() }
//    }
    fun setItems(newList: List<Court>) {
        fullList = newList
        applyFilter(showTennis, showBasketball)
    }

    fun applyFilter(showTennis: Boolean, showBasketball: Boolean) {
        this.showTennis = showTennis
        this.showBasketball = showBasketball
        displayList = fullList.filter {
            (showTennis && it.type == "tennis") || (showBasketball && it.type == "basketball")
        }
        notifyDataSetChanged()
    }

    fun getFullList(): List<Court> {
        return fullList
    }

}