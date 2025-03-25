package com.example.finlogs

import android.content.Context
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.ImageView
import android.widget.TextView
import android.widget.Toast
import androidx.appcompat.app.AlertDialog
import android.widget.BaseAdapter
import com.google.firebase.database.FirebaseDatabase

class PurchaseAdapter(private val context: Context, private val purchases: List<PurchaseModel>) : BaseAdapter() {

    override fun getCount(): Int = purchases.size

    override fun getItem(position: Int): Any = purchases[position]

    override fun getItemId(position: Int): Long = position.toLong()

    override fun getView(position: Int, convertView: View?, parent: ViewGroup?): View {
        val view = convertView ?: LayoutInflater.from(context)
            .inflate(R.layout.purchase_list_item, parent, false)

        val txtPurchaseDetails = view.findViewById<TextView>(R.id.txtPurchaseDetails)
        val btnDeletePurchase = view.findViewById<ImageView>(R.id.btnDeletePurchase)

        val purchase = purchases[position]
        txtPurchaseDetails.text =
            "${purchase.date} - ${purchase.supplierName} - ₹${purchase.totalAmount}"

        return view
    }





}