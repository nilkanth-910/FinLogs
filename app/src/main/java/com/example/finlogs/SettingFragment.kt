package com.example.finlogs

import android.content.Context
import android.content.Intent
import android.os.Bundle
import android.util.Log
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.TextView
import android.widget.Toast
import androidx.fragment.app.Fragment
import androidx.lifecycle.lifecycleScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext

class SettingFragment : Fragment() {
    override fun onCreateView(
        inflater: LayoutInflater, container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View? {
        val view = inflater.inflate(R.layout.fragment_setting, container, false)

        val btnAdd = view.findViewById<TextView>(R.id.btnAddProduct) // Assuming TextView, adjust if needed

        btnAdd.setOnClickListener {
            viewLifecycleOwner.lifecycleScope.launch {
                try {
                    // Specify the CSV file name
                    val csvFileName = "products.csv"  // Replace with your actual file name

                    // Pass the context and file name to the import function
                    withContext(Dispatchers.IO) {
                        FirebaseUtils.importProductsFromCSV(requireContext(), csvFileName)
                    }

                    // Show success message
                    withContext(Dispatchers.Main) { // Switch to Main thread for UI update
                        Toast.makeText(requireContext(), "Products imported successfully!", Toast.LENGTH_SHORT).show()
                    }

                } catch (e: Exception) {
                    // Handle errors
                    Log.e("SettingFragment", "Error importing products", e)
                    withContext(Dispatchers.Main) { // Switch to Main thread for UI update
                        Toast.makeText(requireContext(), "Error importing products: ${e.message}", Toast.LENGTH_LONG).show()
                    }
                }
            }
        }

        return view
    }
}