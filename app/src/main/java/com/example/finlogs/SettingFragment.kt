package com.example.finlogs

import android.content.Context
import android.content.Intent
import android.content.SharedPreferences // Import SharedPreferences
import android.os.Bundle
import android.util.Log
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.Button // Import Button
import android.widget.TextView
import android.widget.Toast
import androidx.appcompat.app.AppCompatDelegate // Import AppCompatDelegate
import androidx.fragment.app.Fragment
import androidx.lifecycle.lifecycleScope
import com.google.android.material.switchmaterial.SwitchMaterial // Import SwitchMaterial
import com.google.firebase.auth.FirebaseAuth // Import FirebaseAuth
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext

class SettingFragment : Fragment() {

    private lateinit var sharedPreferences: SharedPreferences
    private val themeKey = "isDarkMode" // Key to store theme preference

    override fun onCreateView(
        inflater: LayoutInflater, container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View? {
        // Initialize SharedPreferences
        sharedPreferences = requireActivity().getSharedPreferences("ThemePrefs", Context.MODE_PRIVATE)

        // Inflate the layout for this fragment
        return inflater.inflate(R.layout.fragment_setting, container, false)
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)

        // --- Existing Button Logic ---
        val btnAdd = view.findViewById<TextView>(R.id.btnAddProduct)
        val btnLogout = view.findViewById<Button>(R.id.logoutButton)

        btnAdd.setOnClickListener {
            // ... (your existing add product logic) ...
            viewLifecycleOwner.lifecycleScope.launch {
                try {
                    val csvFileName = "products.csv"
                    withContext(Dispatchers.IO) {
                        FirebaseUtils.importProductsFromCSV(requireContext(), csvFileName)
                    }
                    withContext(Dispatchers.Main) {
                        Toast.makeText(requireContext(), "Products imported successfully!", Toast.LENGTH_SHORT).show()
                    }
                } catch (e: Exception) {
                    Log.e("SettingFragment", "Error importing products", e)
                    withContext(Dispatchers.Main) {
                        Toast.makeText(requireContext(), "Error importing products: ${e.message}", Toast.LENGTH_LONG).show()
                    }
                }
            }
        }

        btnLogout.setOnClickListener {

            FirebaseAuth.getInstance().signOut()

            val loginPrefs: SharedPreferences =
                requireActivity().getSharedPreferences("MyPrefs", Context.MODE_PRIVATE)
            val editor = loginPrefs.edit()
            editor.putBoolean("isLoggedIn", false)
            editor.remove("loginId") // Remove loginId as well
            editor.apply()

            // 3. Navigate back to MainActivity (which will redirect to LoginActivity)
            val intent = Intent(activity, MainActivity::class.java)
            intent.flags = Intent.FLAG_ACTIVITY_NEW_TASK or Intent.FLAG_ACTIVITY_CLEAR_TASK // Clear back stack
            startActivity(intent)
            activity?.finish() // Finish HomeActivity
        }

        // --- Theme Switch Logic ---
//        val themeSwitch = view.findViewById<SwitchMaterial>(R.id.themeSwitch)
//
//        // Set the switch's initial state based on saved preference
//        themeSwitch.isChecked = sharedPreferences.getBoolean(themeKey, false) // Default to light mode (false)
//
//        // Set listener for theme changes
//        themeSwitch.setOnCheckedChangeListener { _, isChecked ->
//            if (isChecked) {
//                // Apply Dark Mode
//                AppCompatDelegate.setDefaultNightMode(AppCompatDelegate.MODE_NIGHT_YES)
//                // Save preference
//                sharedPreferences.edit().putBoolean(themeKey, true).apply()
//            } else {
//                // Apply Light Mode
//                AppCompatDelegate.setDefaultNightMode(AppCompatDelegate.MODE_NIGHT_NO)
//                // Save preference
//                sharedPreferences.edit().putBoolean(themeKey, false).apply()
//            }
//
//        }
    }
}