package com.example.group22_opencourt.ui.main

import androidx.recyclerview.widget.RecyclerView
import android.view.LayoutInflater
import android.view.ViewGroup
import android.widget.ImageView
import android.widget.TextView

import com.example.group22_opencourt.ui.main.placeholder.PlaceholderContent.PlaceholderItem
import com.example.group22_opencourt.databinding.HomeFragmentItemBinding


class HomeRecyclerViewAdapter(
    private val list: List<PlaceholderItem>
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

    }

    override fun getItemCount(): Int {
        return list.size
    } 



}