package com.example.group22_opencourt.ui.main

import android.os.Bundle
import androidx.fragment.app.Fragment
import androidx.recyclerview.widget.GridLayoutManager
import androidx.recyclerview.widget.LinearLayoutManager
import androidx.recyclerview.widget.RecyclerView
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import androidx.recyclerview.widget.DividerItemDecoration
import com.example.group22_opencourt.R
import com.example.group22_opencourt.databinding.FragmentHomeBinding
import com.example.group22_opencourt.ui.main.placeholder.PlaceholderContent

class HomeFragment : Fragment() {

    private lateinit var binding: FragmentHomeBinding
    private var selectedSports = mutableSetOf("tennis", "basketball")
    private lateinit var adapter: HomeRecyclerViewAdapter

    override fun onCreateView(
        inflater: LayoutInflater, container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View? {
        binding = FragmentHomeBinding.inflate(inflater, container, false)
        return binding.root
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        binding.recyclerView.layoutManager = LinearLayoutManager(requireContext())
        val dividerItemDecoration = DividerItemDecoration(
            binding.recyclerView.context,
            DividerItemDecoration.VERTICAL
        )
        binding.recyclerView.addItemDecoration(dividerItemDecoration)

        adapter = HomeRecyclerViewAdapter(PlaceholderContent.ITEMS)
        binding.recyclerView.adapter = adapter

        // Filter button logic
        binding.filterButton.setOnClickListener {
            val inflater = LayoutInflater.from(requireContext())
            val popupView = inflater.inflate(R.layout.filter_popup, null)
            val minWidthPx = (180 * resources.displayMetrics.density).toInt()
            val buttonWidth = binding.filterButton.width
            val popupWidth = if (buttonWidth > minWidthPx) buttonWidth else minWidthPx
            val popupWindow = android.widget.PopupWindow(
                popupView,
                popupWidth,
                ViewGroup.LayoutParams.WRAP_CONTENT,
                true
            )
            popupWindow.setBackgroundDrawable(android.graphics.drawable.ColorDrawable(android.graphics.Color.TRANSPARENT))
            popupWindow.isOutsideTouchable = true
            // Set initial checkbox states
            val tennisCheck = popupView.findViewById<com.google.android.material.checkbox.MaterialCheckBox>(R.id.checkbox_tennis)
            val basketballCheck = popupView.findViewById<com.google.android.material.checkbox.MaterialCheckBox>(R.id.checkbox_basketball)
            tennisCheck.isChecked = selectedSports.contains("tennis")
            basketballCheck.isChecked = selectedSports.contains("basketball")
            // Checkbox listeners
            tennisCheck.setOnCheckedChangeListener { _, isChecked ->
                if (isChecked) selectedSports.add("tennis") else selectedSports.remove("tennis")
                filterList()
            }
            basketballCheck.setOnCheckedChangeListener { _, isChecked ->
                if (isChecked) selectedSports.add("basketball") else selectedSports.remove("basketball")
                filterList()
            }
            popupWindow.showAsDropDown(binding.filterButton, 0, 0)
        }
    }

    private fun filterList() {
        val filtered = PlaceholderContent.ITEMS.filter { selectedSports.contains(it.sport) }
        adapter.setItems(filtered)
    }
}