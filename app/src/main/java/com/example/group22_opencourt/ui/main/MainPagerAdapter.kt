package com.example.group22_opencourt.ui.main

import androidx.fragment.app.Fragment
import androidx.fragment.app.FragmentActivity
import androidx.viewpager2.adapter.FragmentStateAdapter
import com.example.group22_opencourt.ui.main.SimpleTextFragment

class MainPagerAdapter(activity: FragmentActivity, var list : ArrayList<Fragment>) : FragmentStateAdapter(activity) {
    override fun getItemCount(): Int {
        return list.size
    }

    override fun createFragment(position: Int): Fragment {
        return list[position]
    }
}
