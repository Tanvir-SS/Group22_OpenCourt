package com.example.group22_opencourt


import android.os.Bundle
import androidx.activity.enableEdgeToEdge
import androidx.appcompat.app.AppCompatActivity
import androidx.fragment.app.Fragment
import androidx.viewpager2.widget.ViewPager2
import com.example.group22_opencourt.databinding.ActivityMainBinding
import com.example.group22_opencourt.ui.main.MainPagerAdapter
import com.example.group22_opencourt.ui.main.SimpleTextFragment

class MainActivity : AppCompatActivity() {

    private lateinit var binding: ActivityMainBinding

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)

        binding = ActivityMainBinding.inflate(layoutInflater)
        setContentView(binding.root)
        enableEdgeToEdge()

        val fragments = ArrayList<Fragment>()
        fragments.add(SimpleTextFragment.newInstance("Home"))
        fragments.add( SimpleTextFragment.newInstance("Search"))
        fragments.add( SimpleTextFragment.newInstance("Create"))
        fragments.add( SimpleTextFragment.newInstance("Profile"))



        val pagerAdapter = MainPagerAdapter(this, fragments)
        binding.viewPager.adapter = pagerAdapter
        binding.viewPager.isUserInputEnabled = true



        // Bottom nav buttons to switch to correct fragment
        binding.bottomNav.setOnItemSelectedListener { item ->
            when (item.itemId) {
                R.id.nav_home -> binding.viewPager.setCurrentItem(0, true)
                R.id.nav_map -> binding.viewPager.setCurrentItem(1, true)
                R.id.nav_add_court -> binding.viewPager.setCurrentItem(2, true)
                R.id.nav_settings -> binding.viewPager.setCurrentItem(3, true)
            }
            true //handled change
        }

        // sync bottom nav button selected to current fragment
        //method requires an object instance
        binding.viewPager.registerOnPageChangeCallback(object : ViewPager2.OnPageChangeCallback() {
            override fun onPageSelected(position: Int) {
                super.onPageSelected(position)
                val itemId = when (position) {
                    0 -> R.id.nav_home
                    1 -> R.id.nav_map
                    2 -> R.id.nav_add_court
                    3 -> R.id.nav_settings
                    else -> R.id.nav_home
                }
                binding.bottomNav.selectedItemId = itemId
            }
        })
    }
}