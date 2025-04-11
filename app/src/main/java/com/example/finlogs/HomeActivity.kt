package com.example.finlogs

import android.os.Bundle
import androidx.appcompat.app.AppCompatActivity
import androidx.fragment.app.Fragment
import com.google.android.material.bottomnavigation.BottomNavigationView // Ensure this import is present

class HomeActivity : AppCompatActivity() {

    // Make bottomNavigationView accessible within the class
    private lateinit var bottomNavigationView: BottomNavigationView

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_home)

        bottomNavigationView = findViewById(R.id.bottom_navigation) // Initialize here
        bottomNavigationView.setOnNavigationItemSelectedListener { item ->
            when (item.itemId) {
                R.id.nav_ledger -> {
                    loadFragment(LedgerFragment())
                    true
                }
                R.id.nav_dashboard -> {
                    loadFragment(DashboardFragment())
                    true
                }
                R.id.nav_setting -> {
                    loadFragment(SettingFragment())
                    true
                }
                else -> false
            }
        }

        // Load initial fragment only if state is not restored
        if (savedInstanceState == null) {
            loadFragment(DashboardFragment())
            bottomNavigationView.menu.findItem(R.id.nav_dashboard)?.isChecked = true // Ensure item is checked
        }
    }

    private fun loadFragment(fragment: Fragment) {
        supportFragmentManager.beginTransaction()
            .replace(R.id.fragment_container, fragment) // R.id.fragment_container is assumed ID for your Fragment container view
            .commit()
    }

    // ---- START: Override onBackPressed ----
    override fun onBackPressed() {
        val currentFragment = supportFragmentManager.findFragmentById(R.id.fragment_container) // Get current fragment

        if (currentFragment is LedgerFragment || currentFragment is SettingFragment) {
            // If on Ledger or Setting, navigate to Dashboard
            loadFragment(DashboardFragment())
            // Update the bottom navigation view selection
            bottomNavigationView.menu.findItem(R.id.nav_dashboard)?.isChecked = true
        } else {
            // Otherwise (e.g., if on Dashboard), perform default back action (usually exit)
            super.onBackPressed()
        }
    }
    // ---- END: Override onBackPressed ----
}