package com.example.finlogs

import android.app.DatePickerDialog
import android.app.Dialog
import android.content.Context
import android.widget.Button
import android.widget.EditText
import android.widget.Toast
import java.text.SimpleDateFormat
import java.util.Calendar
import java.util.Locale
import kotlin.toString

object ItemsDialogUtils {
    fun filterDialog(context: Context, onFilterApplied: (ItemsFilterCriteria) -> Unit) {
        val dialog = Dialog(context)
        dialog.setContentView(R.layout.dialog_filter_items)

        val edtName = dialog.findViewById<EditText>(R.id.edtName)
        val edtMinMRP = dialog.findViewById<EditText>(R.id.edtMRPMinPrice)
        val edtMaxMRP = dialog.findViewById<EditText>(R.id.edtMRPMaxPrice)
        val edtMinStock = dialog.findViewById<EditText>(R.id.edtSMin)
        val edtMaxStock = dialog.findViewById<EditText>(R.id.edtSMax)
        val edtMinRate = dialog.findViewById<EditText>(R.id.edtRMinPrice)
        val edtMaxRate = dialog.findViewById<EditText>(R.id.edtRMaxPrice)
        val btnFilter = dialog.findViewById<Button>(R.id.btnFilter)
        val btnClearFilter = dialog.findViewById<Button>(R.id.btnClearFilter)
        val btnCancel = dialog.findViewById<Button>(R.id.btnCancelItem)

        btnFilter.setOnClickListener {
            val name = edtName.text.toString().trim()
            val minPrice = edtMinMRP.text.toString().toDoubleOrNull()
            val maxPrice = edtMaxMRP.text.toString().toDoubleOrNull()
            val minStock = edtMinStock.text.toString().toIntOrNull()
            val maxStock = edtMaxStock.text.toString().toIntOrNull()
            val minRate = edtMinRate.text.toString().toDoubleOrNull()
            val maxRate = edtMaxRate.text.toString().toDoubleOrNull()

            // Validate price range
            if (minPrice != null && maxPrice != null && minPrice > maxPrice) {
                Toast.makeText(context, "Min price cannot be greater than max price!", Toast.LENGTH_SHORT).show()
                return@setOnClickListener
            }

            // Validate stock range
            if (minStock != null && maxStock != null && minStock > maxStock) {
                Toast.makeText(context, "Min stock cannot be greater than max stock!", Toast.LENGTH_SHORT).show()
                return@setOnClickListener
            }

            // Validate rate range
            if (minRate != null && maxRate != null && minRate > maxRate) {
                Toast.makeText(context, "Min rate cannot be greater than max rate!", Toast.LENGTH_SHORT).show()
                return@setOnClickListener
            }

            // Pass filter criteria to the callback
            val filterCriteria = ItemsFilterCriteria(
                name = name,
                minPrice = minPrice,
                maxPrice = maxPrice,
                minStock = minStock,
                maxStock = maxStock,
                minRate = minRate,
                maxRate = maxRate
            )
            onFilterApplied(filterCriteria)
            dialog.dismiss()
        }

        btnClearFilter.setOnClickListener {
            onFilterApplied(ItemsFilterCriteria("", null, null, null, null, null, null))
            dialog.dismiss()
        }
        btnCancel.setOnClickListener {
            dialog.dismiss()
        }

        dialog.show()
    }

}