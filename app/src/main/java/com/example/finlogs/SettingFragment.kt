package com.example.finlogs // Ensure your package name is correct

import android.app.Activity // Import Activity
import android.content.Context
import android.content.Intent
import android.content.SharedPreferences
import android.net.Uri // Import Uri
import android.os.Bundle
import android.provider.OpenableColumns // To get filename (optional)
import android.util.Log
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.Button
import android.widget.TextView
import android.widget.Toast
import androidx.activity.result.ActivityResultLauncher // Import ActivityResultLauncher
import androidx.activity.result.contract.ActivityResultContracts // Import ActivityResultContracts
import androidx.appcompat.app.AlertDialog // Import AlertDialog
import androidx.appcompat.app.AppCompatDelegate
import androidx.fragment.app.Fragment
import androidx.lifecycle.lifecycleScope
import com.google.android.material.switchmaterial.SwitchMaterial
import com.google.firebase.auth.FirebaseAuth
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import java.io.File // Import File (optional, for displaying filename)

class SettingFragment : Fragment() {

    private lateinit var sharedPreferences: SharedPreferences
    private val themeKey = "isDarkMode"

    // --- START: Declare ActivityResultLauncher ---
    private lateinit var filePickerLauncher: ActivityResultLauncher<Intent>
    // --- END: Declare ActivityResultLauncher ---

    override fun onCreate(savedInstanceState: Bundle?) { // Use onCreate for launcher registration
        super.onCreate(savedInstanceState)

        // --- START: Register ActivityResultLauncher ---
        filePickerLauncher = registerForActivityResult(ActivityResultContracts.StartActivityForResult()) { result ->
            if (result.resultCode == Activity.RESULT_OK) {
                result.data?.data?.let { uri ->
                    // File selected successfully, show confirmation dialog
                    showImportConfirmationDialog(uri)
                    // Optional: Display selected filename
                    val filename = getFileName(requireContext(), uri)
                    Log.d("SettingFragment", "Selected file: $filename, URI: $uri")
                    // You could show the filename in a TextView if needed
                } ?: run {
                    Toast.makeText(requireContext(), "Failed to get file URI", Toast.LENGTH_SHORT).show()
                }
            } else {
                Log.d("SettingFragment", "File selection cancelled or failed.")
                // Optional: Show a message if the user cancelled
                // Toast.makeText(requireContext(), "File selection cancelled", Toast.LENGTH_SHORT).show()
            }
        }
        // --- END: Register ActivityResultLauncher ---
    }

    override fun onCreateView(
        inflater: LayoutInflater, container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View? {
        sharedPreferences = requireActivity().getSharedPreferences("ThemePrefs", Context.MODE_PRIVATE)
        return inflater.inflate(R.layout.fragment_setting, container, false)
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)

        val btnAddProduct = view.findViewById<TextView>(R.id.btnAddProduct) // Corrected ID reference if it's a TextView
        val btnLogout = view.findViewById<Button>(R.id.logoutButton)

        // --- START: Updated btnAddProduct Logic ---
        btnAddProduct.setOnClickListener {
            // Launch the file picker
            val intent = Intent(Intent.ACTION_OPEN_DOCUMENT).apply {
                addCategory(Intent.CATEGORY_OPENABLE)
                // Temporarily allow any file type for testing
                type = "*/*"  // <<-- Change this line
            }
            Log.d("SettingFragment", "Launching file picker for ANY file type (testing).") // Optional: Update log
            filePickerLauncher.launch(intent)
        }
        // --- END: Updated btnAddProduct Logic ---

        // --- Logout Button Logic (Keep as is) ---
        btnLogout.setOnClickListener {
            FirebaseAuth.getInstance().signOut()
            val loginPrefs: SharedPreferences =
                requireActivity().getSharedPreferences("MyPrefs", Context.MODE_PRIVATE)
            val editor = loginPrefs.edit()
            editor.putBoolean("isLoggedIn", false)
            editor.remove("loginId")
            editor.apply()

            val intent = Intent(activity, MainActivity::class.java)
            intent.flags = Intent.FLAG_ACTIVITY_NEW_TASK or Intent.FLAG_ACTIVITY_CLEAR_TASK
            startActivity(intent)
            activity?.finish()
        }

    }

    // --- START: Function to show confirmation dialog ---
    private fun showImportConfirmationDialog(uri: Uri) {
        AlertDialog.Builder(requireContext())
            .setTitle("Import Products from CSV")
            .setMessage("How do you want to import the products?\n\nADD: Adds products to the existing list.\nOVERWRITE: Deletes all existing products first.")
            .setPositiveButton("Overwrite") { dialog, _ ->
                Log.d("SettingFragment", "User chose OVERWRITE.")
                runImportJob(uri, overwrite = true)
                dialog.dismiss()
            }
            .setNegativeButton("Add") { dialog, _ ->
                Log.d("SettingFragment", "User chose ADD.")
                runImportJob(uri, overwrite = false)
                dialog.dismiss()
            }
            .setNeutralButton("Cancel") { dialog, _ ->
                Log.d("SettingFragment", "User chose CANCEL.")
                dialog.dismiss()
            }
            .setCancelable(false) // Prevent dismissing by tapping outside
            .show()
    }
    // --- END: Function to show confirmation dialog ---

    // --- START: Function to run the import coroutine ---
    private fun runImportJob(uri: Uri, overwrite: Boolean) {
        viewLifecycleOwner.lifecycleScope.launch {
            try {
                val operation = if (overwrite) "Overwriting" else "Adding"
                Toast.makeText(requireContext(), "$operation products...", Toast.LENGTH_SHORT).show()
                Log.d("SettingFragment", "Starting coroutine to ${if (overwrite) "overwrite" else "add"} products.")

                if (overwrite) {
                    withContext(Dispatchers.IO) {
                        FirebaseUtils.importProductsFromCSV(requireContext(), uri)
                    }
                } else {
                    withContext(Dispatchers.IO) {
                        FirebaseUtils.addProductsFromCSV(requireContext(), uri)
                    }
                }

                // Success
                withContext(Dispatchers.Main) {
                    val successMessage = if (overwrite) "Products overwritten successfully!" else "Products added successfully!"
                    Log.d("SettingFragment", successMessage)
                    Toast.makeText(requireContext(), successMessage, Toast.LENGTH_SHORT).show()
                }
            } catch (e: Exception) {
                // Failure
                Log.e("SettingFragment", "Error importing products (${if (overwrite) "overwrite" else "add"}): ${e.message}", e)
                withContext(Dispatchers.Main) {
                    Toast.makeText(requireContext(), "Error importing products: ${e.message}", Toast.LENGTH_LONG).show()
                }
            }
        }
    }
    // --- END: Function to run the import coroutine ---

    // --- START: Optional helper function to get filename ---
    private fun getFileName(context: Context, uri: Uri): String? {
        var fileName: String? = null
        if (uri.scheme == "content") {
            val cursor = context.contentResolver.query(uri, null, null, null, null)
            cursor?.use { // Use .use for auto-closing
                if (it.moveToFirst()) {
                    val nameIndex = it.getColumnIndex(OpenableColumns.DISPLAY_NAME)
                    if (nameIndex != -1) {
                        fileName = it.getString(nameIndex)
                    }
                }
            }
        }
        if (fileName == null) {
            fileName = uri.path
            val cut = fileName?.lastIndexOf('/')
            if (cut != -1 && cut != null) {
                fileName = fileName?.substring(cut + 1)
            }
        }
        return fileName
    }
    // --- END: Optional helper function to get filename ---

}