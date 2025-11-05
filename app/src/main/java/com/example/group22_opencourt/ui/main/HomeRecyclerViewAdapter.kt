package com.example.group22_opencourt.ui.main

import androidx.recyclerview.widget.RecyclerView
import android.view.LayoutInflater
import android.view.ViewGroup
import android.widget.ImageView
import android.widget.TextView
import com.example.group22_opencourt.R

import com.example.group22_opencourt.ui.main.placeholder.PlaceholderContent.PlaceholderItem
import com.example.group22_opencourt.databinding.HomeFragmentItemBinding


class HomeRecyclerViewAdapter(
    private var list: List<PlaceholderItem>
) : RecyclerView.Adapter<HomeRecyclerViewAdapter.ViewHolder>() {

    class ViewHolder(binding: HomeFragmentItemBinding) : RecyclerView.ViewHolder(binding.root) {
        val nameView: TextView = binding.courtNameText
        val cityView: TextView = binding.courtCityText
        val imageView : ImageView = binding.courtImageView
        val addressView : TextView = binding.courtAddressText
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
        val item = list[position]
        holder.nameView.text = item.content
//        holder.addressView.text = item.details
        // Set image based on sport
        val context = holder.imageView.context
        val imageRes = when (item.sport) {
            "tennis" -> R.drawable.example_tennis_court
            "basketball" -> R.drawable.basketballcourtexample
            else -> R.drawable.example_tennis_court
        }
        holder.imageView.setImageResource(imageRes)
    }

    override fun getItemCount(): Int {
        return list.size
    }

    fun setItems(newList: List<PlaceholderItem>) {
        list = newList
        notifyDataSetChanged()
    }



}