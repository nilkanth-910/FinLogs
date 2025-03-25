package com.example.finlogs

import android.content.Context
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.BaseAdapter
import android.widget.ImageView
import android.widget.TextView
import android.widget.Toast
import androidx.appcompat.app.AlertDialog
import com.google.firebase.database.FirebaseDatabase

class SaleAdapter(private val context: Sale, private val salesList: List<SaleModel>) : BaseAdapter() {

    override fun getCount(): Int = salesList.size

    override fun getItem(position: Int): Any = salesList[position]

    override fun getItemId(position: Int): Long = position.toLong()

    override fun getView(position: Int, convertView: View?, parent: ViewGroup?): View {
        val view = convertView ?: LayoutInflater.from(context).inflate(R.layout.sale_list_item, parent, false)

        val txtSaleDetails = view.findViewById<TextView>(R.id.txtSaleDetails)
        val btnDeleteSale = view.findViewById<ImageView>(R.id.btnDeleteSale)

        val sale = salesList[position]
        txtSaleDetails.text = "${sale.date} - ${sale.customerName} - ₹${String.format("%.2f", sale.totalAmount)}"

        return view
    }
}

