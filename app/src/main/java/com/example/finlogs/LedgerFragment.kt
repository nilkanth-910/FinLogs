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

class LedgerFragment : Fragment() {

    private lateinit var autoCompleteItem: AutoCompleteTextView
    private lateinit var saleDatabaseReference: DatabaseReference
    private lateinit var purchaseDatabaseReference: DatabaseReference
    private lateinit var itemsReference: DatabaseReference
    private lateinit var itemAdapter: ArrayAdapter<String>
    private lateinit var listOutwardsView: ListView
    private lateinit var listInwardsView: ListView
    private lateinit var ttlInwards: TextView
    private lateinit var ttlOutwards: TextView

    override fun onCreateView(
        inflater: LayoutInflater, container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View? {
        val view = inflater.inflate(R.layout.fragment_ledger, container, false)


        val topBarTitle = view.findViewById<TextView>(R.id.topBarTitle)
        topBarTitle.text = topBarTitle.text.toString()

        autoCompleteItem = view.findViewById(R.id.autoCompleteItem)
        listOutwardsView = view.findViewById(R.id.listViewOutwards)
        listInwardsView = view.findViewById(R.id.listViewInwards)
        ttlInwards = view.findViewById(R.id.ttlInwards)
        ttlOutwards = view.findViewById(R.id.ttlOutwards)

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
        // Fetch purchases (inwards) data
        var ttlInward = 0.0
        var ttlOutward = 0.0
        purchaseDatabaseReference.orderByChild("date").addValueEventListener(object : ValueEventListener {
            override fun onDataChange(snapshot: DataSnapshot) {
                val inwardsData = mutableListOf<String>()
                for (purchaseSnapshot in snapshot.children) {
                    val purchase = purchaseSnapshot.getValue(PurchaseModel::class.java)
                    purchase?.items?.forEach { item ->
                        if (item.itemName == productName) {
                            inwardsData.add("${purchase.invoiceNo} | ${purchase.date} | ${item.qty} | ₹${String.format("%.0f", item.amount)}")
                            ttlInward += item.qty
                        }
                    }
                }
                val inwardsAdapter = ArrayAdapter(requireContext(), android.R.layout.simple_list_item_1, inwardsData)
                listInwardsView.adapter = inwardsAdapter
                ttlInwards.text = "Total Inwards: ${String.format("%.2f", ttlInward)}"

            }
            override fun onCancelled(error: DatabaseError) {
                Log.e("LedgerFragment", "Failed to load inwards data: ${error.message}")
            }
        })

        // Fetch sales (outwards) data
        saleDatabaseReference.orderByChild("date").addValueEventListener(object : ValueEventListener {
            override fun onDataChange(snapshot: DataSnapshot) {
                val outwardsData = mutableListOf<String>()
                for (saleSnapshot in snapshot.children) {
                    val sale = saleSnapshot.getValue(SaleModel::class.java)
                    sale?.saleItems?.forEach { item ->
                        if (item.productName == productName) {
                            outwardsData.add("${sale.invoiceNo} | ${sale.date} | ${item.qty} | ₹${String.format("%.2f", item.amount)}")
                            ttlOutward += item.qty
                        }
                    }
                }
                val outwardsAdapter = ArrayAdapter(requireContext(), android.R.layout.simple_list_item_1, outwardsData)
                listOutwardsView.adapter = outwardsAdapter
                ttlOutwards.text = "Total Outwards: ${String.format("%.2f", ttlOutward)}"
            }

            override fun onCancelled(error: DatabaseError) {
                Log.e("LedgerFragment", "Failed to load outwards data: ${error.message}")
            }
        })
    }
}

