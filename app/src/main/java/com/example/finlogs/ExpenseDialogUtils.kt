package com.example.finlogs

import android.app.DatePickerDialog
import android.app.Dialog
import android.content.Context
import android.util.Log
import android.widget.Button
import android.widget.EditText
import android.widget.Toast
import java.text.SimpleDateFormat
import java.util.Calendar
import java.util.Locale

object ExpenseDialogUtils {
    fun filterDialog(context: Context, onFilterApplied: (FilterCriteria) -> Unit) {
        val dialog = Dialog(context)
        Log.d("DialogUtils", "$context")
        dialog.setContentView(R.layout.dialog_filter_expense)

        val edtName = dialog.findViewById<EditText>(R.id.edtName)
        val edtInvoiceNumber = dialog.findViewById<EditText>(R.id.edtInvoiceNo)
        val edtMinPrice = dialog.findViewById<EditText>(R.id.edtMinPrice)
        val edtMaxPrice = dialog.findViewById<EditText>(R.id.edtMaxPrice)
        val edtStartDate = dialog.findViewById<EditText>(R.id.edtStartDate)
        val edtEndDate = dialog.findViewById<EditText>(R.id.edtEndDate)
        val btnFilter = dialog.findViewById<Button>(R.id.btnFilter)
        val btnClearFilter = dialog.findViewById<Button>(R.id.btnClearFilter)
        val btnCancel = dialog.findViewById<Button>(R.id.btnCancelItem)
        var filterCriteria = FilterCriteria("", "", null, null, "", "")
        val calendar = Calendar.getInstance()

        // Date picker for start date
        edtStartDate.setOnClickListener {
            val datePicker = DatePickerDialog(
                context,
                { _, year, month, dayOfMonth ->
                    val sdf = SimpleDateFormat("yyyy-MM-dd", Locale.getDefault())
                    calendar.set(year, month, dayOfMonth)
                    edtStartDate.setText(sdf.format(calendar.time))
                },
                calendar.get(Calendar.YEAR),
                calendar.get(Calendar.MONTH),
                calendar.get(Calendar.DAY_OF_MONTH)
            )
            datePicker.show()
        }

        // Date picker for end date
        edtEndDate.setOnClickListener {
            val datePicker = DatePickerDialog(
                context,
                { _, year, month, dayOfMonth ->
                    val sdf = SimpleDateFormat("yyyy-MM-dd", Locale.getDefault())
                    calendar.set(year, month, dayOfMonth)
                    edtEndDate.setText(sdf.format(calendar.time))
                },
                calendar.get(Calendar.YEAR),
                calendar.get(Calendar.MONTH),
                calendar.get(Calendar.DAY_OF_MONTH)
            )
            datePicker.show()
        }

        btnFilter.setOnClickListener {
            val name = edtName.text.toString().trim()
            val invoiceNo = edtInvoiceNumber.text.toString().trim()
            val minPrice = edtMinPrice.text.toString().toDoubleOrNull()
            val maxPrice = edtMaxPrice.text.toString().toDoubleOrNull()
            val startDate = edtStartDate.text.toString().trim()
            val endDate = edtEndDate.text.toString().trim()

            // Validate price range
            if (minPrice != null && maxPrice != null && minPrice > maxPrice) {
                Toast.makeText(context, "Min price cannot be greater than max price!", Toast.LENGTH_SHORT).show()
                return@setOnClickListener
            }

            // Validate date range
            if (startDate.isNotEmpty() && endDate.isNotEmpty()) {
                val sdf = SimpleDateFormat("yyyy-MM-dd", Locale.getDefault())
                val start = sdf.parse(startDate)
                val end = sdf.parse(endDate)
                if (start != null && end != null && start.after(end)) {
                    Toast.makeText(context, "Start date cannot be after end date!", Toast.LENGTH_SHORT).show()
                    return@setOnClickListener
                }
            }

            // Pass filter criteria to the callback
            filterCriteria = FilterCriteria(name, invoiceNo, minPrice, maxPrice, startDate, endDate)
            onFilterApplied(filterCriteria)
            dialog.dismiss()
        }

        btnClearFilter.setOnClickListener {
            onFilterApplied(filterCriteria)
            dialog.dismiss()
        }

        btnCancel.setOnClickListener {
            dialog.dismiss()
        }

        dialog.show()
    }
}