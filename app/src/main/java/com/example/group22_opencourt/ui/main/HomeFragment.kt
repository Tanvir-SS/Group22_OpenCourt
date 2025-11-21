package com.example.group22_opencourt.ui.main

import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import androidx.fragment.app.Fragment
import androidx.fragment.app.activityViewModels
import androidx.lifecycle.Observer
import androidx.recyclerview.widget.DividerItemDecoration
import androidx.recyclerview.widget.LinearLayoutManager
import com.example.group22_opencourt.R
import com.example.group22_opencourt.databinding.FragmentHomeBinding
import com.example.group22_opencourt.model.Court

class HomeFragment : Fragment() {

    private lateinit var binding: FragmentHomeBinding
    private lateinit var adapter: HomeRecyclerViewAdapter
    private val viewModel: HomeViewModel by activityViewModels()

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

        adapter = HomeRecyclerViewAdapter(emptyList())
        binding.recyclerView.adapter = adapter

        // Observe courts from ViewModel and update adapter
        viewModel.courts.observe(viewLifecycleOwner) { courts ->
            adapter.setItems(courts)
        }

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
            // Set initial checkbox states (visual only, no filtering)
            val tennisCheck = popupView.findViewById<com.google.android.material.checkbox.MaterialCheckBox>(
                R.id.checkbox_tennis)
            val basketballCheck = popupView.findViewById<com.google.android.material.checkbox.MaterialCheckBox>(R.id.checkbox_basketball)
            tennisCheck.isChecked = false
            basketballCheck.isChecked = false
            // Checkbox listeners (visual only, no filtering)
            tennisCheck.setOnCheckedChangeListener { _, _ -> /* no-op */ }
            basketballCheck.setOnCheckedChangeListener { _, _ -> /* no-op */ }
            popupWindow.showAsDropDown(binding.filterButton, 0, 0)
        }
    }
}