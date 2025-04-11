package com.example.finlogs // Ensure your package name is correct

import android.content.Context
import android.net.Uri // Import Uri
import android.util.Log
import com.google.firebase.database.FirebaseDatabase
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import java.io.BufferedReader
import java.io.InputStreamReader // Keep this

object FirebaseUtils {

    suspend fun importProductsFromCSV(context: Context, uri: Uri) { // Changed signature
        withContext(Dispatchers.IO) {
            val database = FirebaseDatabase.getInstance()
            val productsRef = database.getReference("products")

            Log.d("FirebaseUtils", "Attempting to OVERWRITE products.")

            productsRef.removeValue().addOnSuccessListener {
                Log.d("FirebaseUtils", "Existing products deleted successfully for overwrite.")
            }.addOnFailureListener { e ->
                Log.e("FirebaseUtils", "Error deleting existing products: ${e.message}", e)
                // Consider throwing the exception or handling it so the import doesn't proceed
            }

            try {
                // Get InputStream from Uri
                context.contentResolver.openInputStream(uri)?.use { inputStream -> // Use .use for auto-closing
                    val reader = BufferedReader(InputStreamReader(inputStream))
                    var line: String? = reader.readLine() // Read header (optional: validate header here)
                    if (line == null) {
                        Log.w("FirebaseUtils", "CSV file is empty or header is missing.")
                        return@withContext // Exit if file is empty
                    }
                    Log.d("FirebaseUtils", "CSV Header: $line") // Log header

                    while (reader.readLine().also { line = it } != null) {
                        val currentLine = line ?: continue // Skip if line is null
                        val data = currentLine.split(",")
                        if (data.size == 10) { // Ensure correct column count
                            // --- Same parsing logic as before ---
                            val barcode = data[0].toString()
                            val grossPrice = data[1].replace("�", "").toDoubleOrNull() ?: 0.0
                            val hsn = data[2].toString()
                            val mrp = data[3].replace("�", "").toDoubleOrNull() ?: 0.0
                            val productName = data[5].toString()
                            val rate = data[6].replace("�", "").toDoubleOrNull() ?: 0.0
                            val salePrice = data[7].replace("�", "").toDoubleOrNull() ?: 0.0
                            val stock = data[8].toInt()
                            val tax = data[9].replace("�", "").toDoubleOrNull() ?: 0.0

                            // --- End parsing logic ---

                            val obj = productsRef.push()
                            val key = obj.key ?: continue // Get key, skip if null
                            val productId = key

                            val product = mapOf(
                                "barcode" to barcode,
                                "grossPrice" to grossPrice,
                                "hsn" to hsn,
                                "mrp" to mrp,
                                "productId" to productId, // Use the generated key
                                "productName" to productName,
                                "rate" to rate,
                                "salePrice" to salePrice,
                                "stock" to stock,
                                "tax" to tax
                            )
                            Log.d("FirebaseUtils", "Adding product (Overwrite): $productName")
                            obj.setValue(product)

                        } else {
                            Log.w("FirebaseUtils", "Invalid CSV row (expected 10 columns, got ${data.size}): $currentLine")
                        }
                    }
                    Log.d("FirebaseUtils", "Finished OVERWRITING products from CSV.")
                } ?: Log.e("FirebaseUtils", "Failed to open input stream for URI: $uri")

            } catch (e: Exception) {
                Log.e("FirebaseUtils", "Error importing products (Overwrite): ${e.message}", e)
                throw e // Re-throw exception to be caught in Fragment
            }
        }
    }

    // --- Function to ADD products from CSV (without deleting) ---
    suspend fun addProductsFromCSV(context: Context, uri: Uri) { // Added function
        withContext(Dispatchers.IO) {
            val database = FirebaseDatabase.getInstance()
            val productsRef = database.getReference("products")

            Log.d("FirebaseUtils", "Attempting to ADD products.")
            // DO NOT delete existing products here

            try {
                // Get InputStream from Uri
                context.contentResolver.openInputStream(uri)?.use { inputStream -> // Use .use for auto-closing
                    val reader = BufferedReader(InputStreamReader(inputStream))
                    var line: String? = reader.readLine() // Read header (optional: validate header here)
                    if (line == null) {
                        Log.w("FirebaseUtils", "CSV file is empty or header is missing.")
                        return@withContext // Exit if file is empty
                    }
                    Log.d("FirebaseUtils", "CSV Header: $line") // Log header

                    while (reader.readLine().also { line = it } != null) {
                        val currentLine = line ?: continue // Skip if line is null
                        val data = currentLine.split(",")
                        if (data.size == 10) {

                            val barcode = data[0].toString()
                            val grossPrice = data[1].replace("�", "").toDoubleOrNull() ?: 0.0
                            val hsn = data[2].toString()
                            val mrp = data[3].replace("�", "").toDoubleOrNull() ?: 0.0
                            val productName = data[5].toString()
                            val rate = data[6].replace("�", "").toDoubleOrNull() ?: 0.0
                            val salePrice = data[7].replace("�", "").toDoubleOrNull() ?: 0.0
                            val stock = data[8].toInt()
                            val tax = data[9].replace("�", "").toDoubleOrNull() ?: 0.0


                            val obj = productsRef.push()
                            val key = obj.key ?: continue // Get key, skip if null
                            val productId = key

                            val product = mapOf(
                                "barcode" to barcode,
                                "grossPrice" to grossPrice,
                                "hsn" to hsn,
                                "mrp" to mrp,
                                "productId" to productId,
                                "productName" to productName,
                                "rate" to rate,
                                "salePrice" to salePrice,
                                "stock" to stock,
                                "tax" to tax
                            )
                            Log.d("FirebaseUtils", "Adding product (Add): $productName")
                            obj.setValue(product)

                        } else {
                            Log.w("FirebaseUtils", "Invalid CSV row (expected 10 columns, got ${data.size}): $currentLine")
                        }
                    }
                    Log.d("FirebaseUtils", "Finished ADDING products from CSV.")
                } ?: Log.e("FirebaseUtils", "Failed to open input stream for URI: $uri")

            } catch (e: Exception) {
                Log.e("FirebaseUtils", "Error importing products (Add): ${e.message}", e)
                throw e // Re-throw exception to be caught in Fragment
            }
        }
    }
}