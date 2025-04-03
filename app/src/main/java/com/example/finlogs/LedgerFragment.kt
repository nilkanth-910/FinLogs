package com.example.finlogs

import android.os.Bundle
import android.util.Log
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.ArrayAdapter
import android.widget.AutoCompleteTextView
import androidx.fragment.app.Fragment
import android.widget.ListView
import android.widget.TextView
import com.google.firebase.database.*
import kotlin.toString

class LedgerFragment : Fragment() {

    private lateinit var autoCompleteItem: AutoCompleteTextView
    private lateinit var saleDatabaseReference: DatabaseReference
    private lateinit var purchaseDatabaseReference: DatabaseReference
    private lateinit var itemsReference: DatabaseReference
    private lateinit var itemAdapter: ArrayAdapter<String>
    private lateinit var listOutwardsView: ListView
    private lateinit var listInwardsView: ListView


    override fun onCreateView(
        inflater: LayoutInflater, container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View? {
        val view = inflater.inflate(R.layout.fragment_ledger, container, false)


        val topBarTitle = view.findViewById<TextView>(R.id.topBarTitle)
        topBarTitle.text = topBarTitle.text.toString() + " - Ledger"

        autoCompleteItem = view.findViewById(R.id.autoCompleteItem)
        listOutwardsView = view.findViewById(R.id.listViewOutwards)
        listInwardsView = view.findViewById(R.id.listViewInwards)

        saleDatabaseReference = FirebaseDatabase.getInstance().getReference("sales")
        purchaseDatabaseReference = FirebaseDatabase.getInstance().getReference("purchases")
        itemsReference = FirebaseDatabase.getInstance().getReference("products")
        itemAdapter = ArrayAdapter(requireContext(), android.R.layout.simple_dropdown_item_1line, mutableListOf())

        loadProductNames()

        autoCompleteItem.setOnItemClickListener { parent, view, position, id ->
            val selectedItem = parent.getItemAtPosition(position) as String
            loadProductData(selectedItem)
        }

        return view
    }

    private fun loadProductNames() {
        itemsReference.get().addOnSuccessListener { snapshot ->
            val itemList = mutableListOf<String>()
            for (itemSnapshot in snapshot.children) {
                val item = itemSnapshot.getValue(Product::class.java)
                if (item != null && !item.productName.isNullOrEmpty()) {
                    itemList.add(item.productName)
                }
            }
            updateAutoCompleteAdapter(itemList)
            autoCompleteItem.setAdapter(itemAdapter)
        }.addOnFailureListener {
            Log.e("FirebaseData", "Error fetching items: ${it.message}")
        }
    }

    private fun updateAutoCompleteAdapter(itemList: List<String>) {
        itemAdapter.clear()
        itemAdapter.addAll(itemList)
        itemAdapter.notifyDataSetChanged()
    }

    private fun loadProductData(productName: String) {
        purchaseDatabaseReference.orderByChild("date").addValueEventListener(object : ValueEventListener {
            override fun onDataChange(snapshot: DataSnapshot) {
                val tempList = mutableListOf<PurchaseModel>()
                for (purchaseSnapshot in snapshot.children) {
                    val purchase = purchaseSnapshot.getValue(PurchaseModel::class.java)
                    purchase?.let { tempList.add(it) }
                }
                for (purchase in tempList) {
                    purchase.items?.let { items ->
                        for (item in items) {
                            if (item.itemName == productName) {
                                Log.d("LedgerFragment", "Data for $item")
                                val adapter = ArrayAdapter(
                                    requireContext(), // Corrected context
                                    android.R.layout.simple_list_item_1,
                                    listOf(
                                        "${purchase.invoiceNo} | ${purchase.date} | ${item.qty} | ₹${String.format("%.2f",item.amount)}"
                                    ) // Corrected list creation
                                )
                                listInwardsView.adapter = adapter
                                break
                            }
                        }
                    }
                    Log.d("LedgerFragment", "Failed to load outwards data: ${purchase.items}")
                }
            }
            override fun onCancelled(error: DatabaseError) {}
        })

        saleDatabaseReference.orderByChild("date").addValueEventListener(object : ValueEventListener {
            override fun onDataChange(snapshot: DataSnapshot) {
                val tempList = mutableListOf<SaleModel>()
                for (saleSnapshot in snapshot.children) {
                    val sale = saleSnapshot.getValue(SaleModel::class.java)
                    sale?.let { tempList.add(it) }
                }
                for (sale in tempList) {
                    sale.saleItems?.let { items ->
                        for (item in items) {
                            if (item.productName == productName) {
                                Log.d("LedgerFragment", "Data for $item")
                                val adapter = ArrayAdapter(
                                    requireContext(), // Corrected context
                                    android.R.layout.simple_list_item_1,
                                    listOf(
                                        "${sale.invoiceNo} | ${sale.date} | ${item.qty} | ₹${String.format("%.2f",item.amount)}"
                                    ) // Corrected list creation
                                )
                                listOutwardsView.adapter = adapter
                                break
                            }
                        }
                    }
                    Log.d("LedgerFragment", "Failed to load outwards data: ${sale.saleItems}")
                }
            }
            override fun onCancelled(error: DatabaseError) {}
        })
    }
}

