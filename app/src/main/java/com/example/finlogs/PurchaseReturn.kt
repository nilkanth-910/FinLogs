package com.example.finlogs

import android.app.DatePickerDialog
import android.app.Dialog
import android.os.Bundle
import android.util.Log
import android.view.LayoutInflater
import android.widget.*
import androidx.appcompat.app.AlertDialog
import androidx.appcompat.app.AppCompatActivity
import androidx.cardview.widget.CardView
import com.google.firebase.database.*
import java.text.SimpleDateFormat
import java.util.Calendar
import java.util.Locale

class PurchaseReturn : AppCompatActivity() {

    private lateinit var btnAddPurchaseReturn: com.google.android.material.floatingactionbutton.FloatingActionButton
    private lateinit var purchaseReturnContainer: LinearLayout
    private lateinit var purchaseReturnList: MutableList<PurchaseReturnModel>
    private lateinit var databaseReference: DatabaseReference
    private lateinit var itemsReference: DatabaseReference
    private val itemNameMap = mutableMapOf<String, Product>()
    private lateinit var itemAdapter: ArrayAdapter<String>

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_purchase_return)
        var topBarTitle = findViewById<TextView>(R.id.topBarTitle)

        topBarTitle.text = topBarTitle.text.toString() + " - Purchase Return"

        btnAddPurchaseReturn = findViewById(R.id.btnAdd)
        purchaseReturnContainer = findViewById(R.id.container)

        purchaseReturnList = mutableListOf()
        databaseReference = FirebaseDatabase.getInstance().getReference("purchasesReturn")
        itemsReference = FirebaseDatabase.getInstance().getReference("products")

        btnAddPurchaseReturn.setOnClickListener {
            showAddPurchaseReturnDialog()
        }

        itemAdapter = ArrayAdapter(this, android.R.layout.simple_dropdown_item_1line, mutableListOf())

        loadItems()
        loadPurchasesReturn()


    }

    private fun loadItems() {
        itemsReference.get().addOnSuccessListener { snapshot ->
            itemNameMap.clear()
            val itemList = mutableListOf<String>()

            for (itemSnapshot in snapshot.children) {
                val item = itemSnapshot.getValue(Product::class.java)
                if (item != null && !item.productName.isNullOrEmpty()) {
                    itemNameMap[item.productName] = item
                    itemList.add(item.productName)
                }
            }

            updateAutoCompleteAdapter(itemList)
        }.addOnFailureListener { error ->
            Log.e("FirebaseData", "Error fetching items: ${error.message}")
        }
    }

    private fun updateAutoCompleteAdapter(itemList: List<String>) {
        itemAdapter.clear()
        itemAdapter.addAll(itemList)
        itemAdapter.notifyDataSetChanged()
    }

    private fun loadPurchasesReturn() {
        databaseReference.orderByChild("date").addValueEventListener(object : ValueEventListener {
            override fun onDataChange(snapshot: DataSnapshot) {
                val tempList = mutableListOf<PurchaseReturnModel>()
                for (purchaseReturnSnapshot in snapshot.children) {
                    val purchaseReturn = purchaseReturnSnapshot.getValue(PurchaseReturnModel::class.java)
                    purchaseReturn?.let { tempList.add(it) }
                }

                val reversedList = tempList.reversed()

                purchaseReturnContainer.removeAllViews() // Clear existing cards

                for (purchaseReturn in reversedList) {
                    addPurchaseReturnCard(purchaseReturn) // Add card for each purchase
                }
            }

            override fun onCancelled(error: DatabaseError) {
                Toast.makeText(this@PurchaseReturn, "Failed to load purchasesReturn!", Toast.LENGTH_SHORT).show()
                Log.d("Crash","Error-5")
            }
        })
    }

    private fun addPurchaseReturnCard(purchaseReturn: PurchaseReturnModel) {
        val cardView = LayoutInflater.from(this).inflate(R.layout.list_card_layout, purchaseReturnContainer, false) as CardView
        val invoiceTextView = cardView.findViewById<TextView>(R.id.invTextView)
        val purchaseReturnAmountTextView = cardView.findViewById<TextView>(R.id.amtTextView)
        val purchaseReturnDateTextView = cardView.findViewById<TextView>(R.id.dateTextView)
        val deletePurchaseReturnIcon = cardView.findViewById<ImageView>(R.id.deleteIcon)
        val supplierTextView = cardView.findViewById<TextView>(R.id.cstTextView)

        invoiceTextView.text = "${purchaseReturn.returninvoiceNo}"
        purchaseReturnAmountTextView.text = "₹${purchaseReturn.totalAmount}"
        purchaseReturnDateTextView.text = "${purchaseReturn.date}"
        supplierTextView.text = "${purchaseReturn.supplierName}"

        deletePurchaseReturnIcon.setOnClickListener {
            showDeleteConfirmationDialog(purchaseReturn.purchaseReturnId)
        }

        purchaseReturnContainer.addView(cardView)
    }

    private fun showDeleteConfirmationDialog(purchaseReturnId: String) {
        AlertDialog.Builder(this)
            .setTitle("Delete Purchase Return")
            .setMessage("Are you sure you want to delete this purchase Return?")
            .setPositiveButton("Yes") { _, _ -> deletePurchaseReturn(purchaseReturnId) }
            .setNegativeButton("No", null)
            .show()
    }

    private fun deletePurchaseReturn(purchaseReturnId: String) {
        val databaseReference = FirebaseDatabase.getInstance().getReference("purchasesReturn")

        databaseReference.child(purchaseReturnId).get().addOnSuccessListener { snapshot ->
            val purchaseReturn = snapshot.getValue(PurchaseReturnModel::class.java)
            if (purchaseReturn != null && purchaseReturn.purchaseReturnItems.isNotEmpty()) {
                restoreStock(purchaseReturn.purchaseReturnItems) // Restore stock before deletion
            }

            // Delete the purchase after restoring stock
            databaseReference.child(purchaseReturnId).removeValue()
                .addOnSuccessListener {
                    Toast.makeText(this, "Purchase Return deleted!", Toast.LENGTH_SHORT).show()
                }
                .addOnFailureListener {
                    Toast.makeText(this, "Failed to delete purchase Return!", Toast.LENGTH_SHORT).show()
                }
        }.addOnFailureListener {
            Toast.makeText(this, "Failed to fetch purchase Return details!", Toast.LENGTH_SHORT).show()
        }
    }

    private fun restoreStock(purchaseReturnItems: List<PurchaseItemModel>) {
        val itemsReference = FirebaseDatabase.getInstance().getReference("products")

        for (item in purchaseReturnItems) {
            val productId = item.productId

            itemsReference.child(productId).get()
                .addOnSuccessListener { snapshot ->
                    val product = snapshot.getValue(Product::class.java)

                    if (product != null) {
                        val newStock = (product.stock + item.qty) // Restore stock
                        itemsReference.child(productId).child("stock").setValue(newStock)
                            .addOnFailureListener {
                                Log.d("Stock Restore", "Error updating stock for ${item.itemName}")
                            }
                    }
                }.addOnFailureListener {
                    Toast.makeText(
                        this,
                        "Failed to restore stock for ${item.itemName}",
                        Toast.LENGTH_SHORT
                    ).show()

                }
        }
    }

    private fun showAddPurchaseReturnDialog() {
        val dialog = Dialog(this)
        dialog.setContentView(R.layout.dialog_add_purchase)

        val edtDate = dialog.findViewById<EditText>(R.id.edtPurchaseDate)
        val edtSupplierName = dialog.findViewById<EditText>(R.id.edtSupplierName)
        val edtInvoiceNo = dialog.findViewById<EditText>(R.id.edtInvoiceNo)
        val btnAddPurchaseReturnItem = dialog.findViewById<Button>(R.id.btnAddPurchaseItem)
        val btnSavePurchaseReturn = dialog.findViewById<Button>(R.id.btnSavePurchase)
        val txtTotalAmount = dialog.findViewById<TextView>(R.id.txtTotalAmount)
        val listViewPurchaseReturnItems = dialog.findViewById<ListView>(R.id.listViewPurchaseItems)

        val calendar = Calendar.getInstance()
        edtDate.setOnClickListener {
            val datePicker = DatePickerDialog(
                this,
                { _, year, month, dayOfMonth ->
                    calendar.set(year, month, dayOfMonth)
                    val sdf = SimpleDateFormat("dd/MM/yyyy", Locale.getDefault())
                    edtDate.setText(sdf.format(calendar.time))
                },
                calendar.get(Calendar.YEAR),
                calendar.get(Calendar.MONTH),
                calendar.get(Calendar.DAY_OF_MONTH)
            )
            datePicker.show()
        }

        val purchaseReturnItems = mutableListOf<PurchaseItemModel>()

        btnAddPurchaseReturnItem.setOnClickListener {
            showAddPurchaseReturnItemDialog(purchaseReturnItems, listViewPurchaseReturnItems, txtTotalAmount)
        }

        btnSavePurchaseReturn.setOnClickListener {
            val date = edtDate.text.toString()
            val supplierName = edtSupplierName.text.toString()
            val invoiceNo = edtInvoiceNo.text.toString()
            val totalAmount = purchaseReturnItems.sumOf { it.amount }

            if (date.isEmpty() || supplierName.isEmpty() || invoiceNo.isEmpty()) {
                Toast.makeText(this, "Fill all fields!", Toast.LENGTH_SHORT).show()
                return@setOnClickListener
            }

            val purchaseReturnId = databaseReference.push().key!!
            val purchaseReturn = PurchaseReturnModel(purchaseReturnId, date, supplierName, invoiceNo, totalAmount, purchaseReturnItems)

            databaseReference.child(purchaseReturnId).setValue(purchaseReturn)
                .addOnSuccessListener {
                    Toast.makeText(this, "PurchaseReturn Added!", Toast.LENGTH_SHORT).show()
                    dialog.dismiss()
                    updateStock(purchaseReturnItems)
                }
                .addOnFailureListener {
                    Toast.makeText(this, "Failed to Add Purchase!", Toast.LENGTH_SHORT).show()
                }
        }

        dialog.show()
    }

    private fun updateStock(purchaseReturnItems: List<PurchaseItemModel>) {
        for (item in purchaseReturnItems) {
            val product = itemNameMap[item.itemName]
            if (product != null) {
                Log.d("Data","Stock for item: ${item.qty}")
                val newStock = product.stock - item.qty
                itemsReference.child(product.productId).child("stock").setValue(newStock)
                    .addOnSuccessListener {
                        Log.d("FirebaseData", "Stock updated for item: ${item.itemName}")
                    }
                    .addOnFailureListener { error ->
                        Log.e("FirebaseData", "Error updating stock for item: ${item.itemName} - ${error.message}")
                    }
            }
        }
    }

    private fun showAddPurchaseReturnItemDialog(
        purchaseReturnItems: MutableList<PurchaseItemModel>,
        listView: ListView,
        txtTotalAmount: TextView
    ) {
        val dialog = Dialog(this)
        dialog.setContentView(R.layout.dialog_add_purchase_item)

        val edtItemName = dialog.findViewById<AutoCompleteTextView>(R.id.edtPurchaseItemName)
        val edtGrossPrice = dialog.findViewById<EditText>(R.id.edtPurchaseGrossPrice)
        val edtQty = dialog.findViewById<EditText>(R.id.edtPurchaseQty)
        val btnAddToPurchaseReturn = dialog.findViewById<Button>(R.id.btnAddToPurchase)

        edtItemName.setAdapter(itemAdapter)
        edtItemName.threshold = 1

        edtItemName.setOnItemClickListener { _, _, position, _ ->
            val selectedName = itemAdapter.getItem(position) ?: return@setOnItemClickListener
            val product = itemNameMap[selectedName]
            if (product != null) {
                edtGrossPrice.setText(product.grossPrice.toString())
            }
        }

        btnAddToPurchaseReturn.setOnClickListener {
            val itemName = edtItemName.text.toString()
            val product = itemNameMap[itemName] ?: return@setOnClickListener
            val grossPrice = edtGrossPrice.text.toString().toDoubleOrNull() ?: 0.0
            val qty = edtQty.text.toString().toIntOrNull() ?: 0
            val amount = grossPrice * qty

            val purchaseReturnItem = PurchaseItemModel(
                productId = product.productId,  // Store productId
                itemName = product.productName, // Store name for UI
                grossPrice = grossPrice,
                qty = qty,
                amount = amount
            )

            purchaseReturnItems.add(purchaseReturnItem)

            val adapter = ArrayAdapter(
                this,
                android.R.layout.simple_list_item_1,
                purchaseReturnItems.map { "${it.itemName} - ${it.qty} x ₹${it.grossPrice} = ₹${it.amount}" }
            )
            listView.adapter = adapter
            txtTotalAmount.text = "Total: ₹${purchaseReturnItems.sumOf { it.amount }}"

            dialog.dismiss()
        }

        dialog.show()
    }



}
