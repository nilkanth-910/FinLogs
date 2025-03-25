package com.example.finlogs

import androidx.cardview.widget.CardView
import android.app.Dialog
import android.os.Bundle
import android.util.Log
import android.view.LayoutInflater
import android.widget.*
import androidx.appcompat.app.AppCompatActivity
import com.google.firebase.database.*
import java.util.Calendar
import java.util.Locale
import android.app.DatePickerDialog
import androidx.appcompat.app.AlertDialog
import java.text.SimpleDateFormat
import java.util.*

class Sale : AppCompatActivity() {

    private lateinit var btnAddSale: Button
    private lateinit var salesContainer: LinearLayout
    private lateinit var saleList: MutableList<SaleModel>
    private lateinit var databaseReference: DatabaseReference
    private lateinit var itemsReference: DatabaseReference
    private val itemNameMap = mutableMapOf<String, Product>()
    private lateinit var itemAdapter: ArrayAdapter<String>

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_sale)
        var topBarTitle = findViewById<TextView>(R.id.topBarTitle)

        topBarTitle.text = topBarTitle.text.toString() + " - Sale"

        btnAddSale = findViewById(R.id.btnAdd)
        salesContainer = findViewById(R.id.container)

        saleList = mutableListOf()
        databaseReference = FirebaseDatabase.getInstance().getReference("sales")
        itemsReference = FirebaseDatabase.getInstance().getReference("products")

        btnAddSale.setOnClickListener {
            showAddSaleDialog()
        }

        itemAdapter = ArrayAdapter(this, android.R.layout.simple_dropdown_item_1line, mutableListOf())
        loadItems()
        loadSales()
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
        }.addOnFailureListener {
            Log.e("FirebaseData", "Error fetching items: ${it.message}")
        }
    }

    private fun updateAutoCompleteAdapter(itemList: List<String>) {
        itemAdapter.clear()
        itemAdapter.addAll(itemList)
        itemAdapter.notifyDataSetChanged()
    }

    private fun loadSales() {
        databaseReference.orderByChild("date").addValueEventListener(object : ValueEventListener {
            override fun onDataChange(snapshot: DataSnapshot) {
                val tempList = mutableListOf<SaleModel>()
                for (saleSnapshot in snapshot.children) {
                    val sale = saleSnapshot.getValue(SaleModel::class.java)
                    sale?.let { tempList.add(it) }
                }
                val reversedList = tempList.reversed()

                salesContainer.removeAllViews() // Clear existing cards

                for (sale in reversedList) {
                    addSaleCard(sale) // Add card for each sale
                }
            }

            override fun onCancelled(error: DatabaseError) {
                Toast.makeText(this@Sale, "Failed to load sales!", Toast.LENGTH_SHORT).show()
            }
        })
    }

    private fun addSaleCard(sale: SaleModel) {
        val cardView = LayoutInflater.from(this).inflate(R.layout.list_card_layout, salesContainer, false) as CardView
        val invoiceTextView = cardView.findViewById<TextView>(R.id.invTextView)
        val saleAmountTextView = cardView.findViewById<TextView>(R.id.amtTextView)
        val saleDateTextView = cardView.findViewById<TextView>(R.id.dateTextView)
        val deleteSaleIcon = cardView.findViewById<ImageView>(R.id.deleteIcon)
        val cstTextView = cardView.findViewById<TextView>(R.id.cstTextView)

        invoiceTextView.text = "${sale.invoiceNo}"
        saleAmountTextView.text = "Amount: ₹${sale.totalAmount}"
        cstTextView.text = "${sale.customerName}"
        saleDateTextView.text = "${sale.date}"

        deleteSaleIcon.setOnClickListener {
            showDeleteConfirmationDialog(sale)
        }

        salesContainer.addView(cardView)
    }

    private fun showDeleteConfirmationDialog(sale: SaleModel) {
        AlertDialog.Builder(this)
            .setTitle("Delete Sale")
            .setMessage("Are you sure you want to delete this sale?")
            .setPositiveButton("Yes") { _, _ -> deleteSale(sale) }
            .setNegativeButton("No", null)
            .show()
    }

    private fun deleteSale(sale: SaleModel) {
        val databaseReference = FirebaseDatabase.getInstance().getReference("sales")

        databaseReference.child(sale.saleId).removeValue()
            .addOnSuccessListener {
                Toast.makeText(this, "Sale deleted successfully!", Toast.LENGTH_SHORT).show()

                if (sale.saleItems.isEmpty()) {
                    Toast.makeText(this, "No items in Sale Bill.", Toast.LENGTH_SHORT).show()
                } else {
                    this.restoreStock(sale.saleItems)
                }
            }
            .addOnFailureListener {
                Toast.makeText(this, "Failed to delete sale!", Toast.LENGTH_SHORT).show()
            }
    }

    fun restoreStock(saleItems: List<SaleItemModel>) {

        for (item in saleItems) {
            val productRef = itemsReference.child(item.productId).child("stock")

            productRef.runTransaction(object : Transaction.Handler {
                override fun doTransaction(mutableData: MutableData): Transaction.Result {
                    val currentStock = mutableData.getValue(Int::class.java) ?: 0
                    val restoredStock = currentStock + item.qty
                    mutableData.value = restoredStock
                    return Transaction.success(mutableData)
                }

                override fun onComplete(error: DatabaseError?, committed: Boolean, currentData: DataSnapshot?) {
                    if (error != null) {
                        Log.e("StockRestore", "Failed to restore stock for ${item.productName}: ${error.message}")
                    } else if (committed) {
                        val updatedStock = currentData?.getValue(Int::class.java) ?: -1
                        Log.d("StockRestore", "Stock restored successfully for ${item.productName}. Updated stock: $updatedStock")
                    } else {
                        Log.e("StockRestore", "Transaction not committed for ${item.productName}.")
                    }
                }
            })
        }
    }

    private fun showAddSaleDialog() {
        val dialog = Dialog(this)
        dialog.setContentView(R.layout.dialog_add_sale)

        val edtDate = dialog.findViewById<EditText>(R.id.edtSaleDate)
        val edtCustomerName = dialog.findViewById<EditText>(R.id.edtCustomerName)
        val btnAddSaleItem = dialog.findViewById<Button>(R.id.btnAddSaleItem)
        val btnSaveSale = dialog.findViewById<Button>(R.id.btnSaveSale)
        val txtTotalAmount = dialog.findViewById<TextView>(R.id.txtTotalAmount)
        val listViewSaleItems = dialog.findViewById<ListView>(R.id.listViewSaleItems)

        val calendar = Calendar.getInstance()

        // Disable manual input and open DatePicker on click
        edtDate.setOnClickListener {
            val datePicker = DatePickerDialog(
                this,
                { _, year, month, dayOfMonth ->
                    calendar.set(year, month, dayOfMonth)
                    val sdf = SimpleDateFormat("yyyy-MM-dd", Locale.getDefault())
                    edtDate.setText(sdf.format(calendar.time))
                },
                calendar.get(Calendar.YEAR),
                calendar.get(Calendar.MONTH),
                calendar.get(Calendar.DAY_OF_MONTH)
            )
            datePicker.show()
        }

        val saleItems = mutableListOf<SaleItemModel>()

        btnAddSaleItem.setOnClickListener {
            showAddSaleItemDialog(saleItems, listViewSaleItems, txtTotalAmount)
        }

        btnSaveSale.setOnClickListener {
            val date = edtDate.text.toString()
            val customerName = edtCustomerName.text.toString()
            val totalAmount = saleItems.sumOf { it.amount }

            if (date.isEmpty() || customerName.isEmpty()) {
                Toast.makeText(this, "Fill all fields!", Toast.LENGTH_SHORT).show()
                return@setOnClickListener
            }

            fetchLastInvoiceNo { lastInvoiceNo ->
                val newInvoiceNo = generateNextInvoiceNo(lastInvoiceNo)
                val saleId = databaseReference.push().key!!

                val sale = SaleModel(saleId, date, customerName, newInvoiceNo, totalAmount, saleItems)

                databaseReference.child(saleId).setValue(sale)
                    .addOnSuccessListener {
                        Toast.makeText(this, "Sale Added!", Toast.LENGTH_SHORT).show()
                        dialog.dismiss()
                        updateStock(saleItems)
                    }
                    .addOnFailureListener {
                        Toast.makeText(this, "Failed to Add Sale!", Toast.LENGTH_SHORT).show()
                    }
            }
        }

        dialog.show()
    }


    private fun showAddSaleItemDialog(
        saleItems: MutableList<SaleItemModel>,
        listView: ListView,
        txtTotalAmount: TextView
    ) {
        val dialog = Dialog(this)
        dialog.setContentView(R.layout.dialog_add_sale_item)

        val edtItemName = dialog.findViewById<AutoCompleteTextView>(R.id.edtSaleItemName)
        val edtSalePrice = dialog.findViewById<EditText>(R.id.edtSalePrice)
        val edtQty = dialog.findViewById<EditText>(R.id.edtSaleQty)
        val btnAddToSale = dialog.findViewById<Button>(R.id.btnAddToSale)

        edtItemName.setAdapter(itemAdapter)
        edtItemName.threshold = 1

        edtItemName.setOnItemClickListener { _, _, position, _ ->
            val selectedName = itemAdapter.getItem(position) ?: return@setOnItemClickListener
            val product = itemNameMap[selectedName]
            if (product != null) {
                edtSalePrice.setText(String.format("%.2f", product.salePrice))
            }
        }

        btnAddToSale.setOnClickListener {
            val productName = edtItemName.text.toString()
            val salePrice = edtSalePrice.text.toString().toDoubleOrNull() ?: 0.0
            val qty = edtQty.text.toString().toIntOrNull() ?: 0
            val amount = salePrice * qty



            val product = itemNameMap[productName] ?: return@setOnClickListener
            val productId = product.productId

            if (productName.isEmpty() || salePrice < 0) {
                Toast.makeText(this, "Invalid item details!", Toast.LENGTH_SHORT).show()
                return@setOnClickListener
            }

            if (product.stock <=0) {
                Toast.makeText(this, "No Stock for item", Toast.LENGTH_SHORT).show()
                return@setOnClickListener
            }

            saleItems.add(SaleItemModel(productId, productName, salePrice, qty, amount))

            val adapter = ArrayAdapter(
                this,
                android.R.layout.simple_list_item_1,
                saleItems.map { "${it.productName} - ₹${String.format("%.2f", it.amount)}" }
            )

            listView.adapter = adapter
            txtTotalAmount.text = "Total: ₹${saleItems.sumOf { it.amount }}"

            dialog.dismiss()
        }

        dialog.show()
    }


    private fun fetchLastInvoiceNo(callback: (String) -> Unit) {
        databaseReference.orderByChild("invoiceNo").limitToLast(1)
            .addListenerForSingleValueEvent(object : ValueEventListener {
                override fun onDataChange(snapshot: DataSnapshot) {
                    var lastInvoiceNo = "INV1000"
                    for (saleSnapshot in snapshot.children) {
                        val lastSale = saleSnapshot.getValue(SaleModel::class.java)
                        lastSale?.let {
                            lastInvoiceNo = it.invoiceNo
                        }
                    }
                    callback(lastInvoiceNo)
                }

                override fun onCancelled(error: DatabaseError) {
                    callback("INV1000")
                }
            })
    }

    private fun generateNextInvoiceNo(lastInvoiceNo: String): String {
        val numberPart = lastInvoiceNo.removePrefix("INV").toIntOrNull() ?: 1000
        return "INV${numberPart + 1}"
    }

    private fun updateStock(saleItems: List<SaleItemModel>) {
        Log.d("StockUpdate", "Updating stock for ${saleItems.size} items")

        for (item in saleItems) {
            val product = itemNameMap[item.productName]
            if (product != null) {
                val newStock = product.stock - item.qty

                if (newStock >= 0) {
                    Log.d("StockUpdate", "Updating stock for ${item.productName} (ID: ${product.productId}): $newStock")

                    itemsReference.child(product.productId).child("stock").setValue(newStock)
                        .addOnSuccessListener {
                            Log.d("StockUpdate", "Stock updated successfully for ${item.productName}")
                        }
                        .addOnFailureListener { error ->
                            Log.e("StockUpdate", "Error updating stock for ${item.productName}: ${error.message}")
                        }
                } else {
                    Log.e("StockUpdate", "Not enough stock for item: ${item.productName} (Current: ${product.stock}, Tried to remove: ${item.qty})")
                }
            } else {
                Log.e("StockUpdate", "Product not found for item: ${item.productName}")
            }
        }
    }

}
