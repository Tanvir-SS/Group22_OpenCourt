package com.example.group22_opencourt.ui.main

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

class HomeRecyclerViewAdapter(
    private var list: List<Court>
) : RecyclerView.Adapter<HomeRecyclerViewAdapter.ViewHolder>() {

    class ViewHolder(binding: HomeFragmentItemBinding) : RecyclerView.ViewHolder(binding.root) {
        val nameView: TextView = binding.courtNameText
        val cityView: TextView = binding.courtCityText
        val imageView: ImageView = binding.courtImageView
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
        val court = list[position]
        holder.nameView.text = court.base.name
        holder.cityView.text = court.base.city
        holder.imageView
        when (court) {
            is BasketballCourt -> holder.imageView.setImageResource(R.drawable.basketballcourtexample)
            is TennisCourt -> holder.imageView.setImageResource(R.drawable.example_tennis_court)
        }
    }

    override fun getItemCount(): Int {
        return list.size
    }

    fun setItems(newList: List<Court>) {
        list = newList
        notifyDataSetChanged()
    }

}