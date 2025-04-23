package com.example.finlogs

import android.app.DatePickerDialog
import android.app.Dialog
import android.content.Intent
import android.graphics.Paint
import android.graphics.pdf.PdfDocument
import android.os.Bundle
import android.util.Log
import android.view.LayoutInflater
import android.view.View
import android.widget.*
import androidx.appcompat.app.AlertDialog
import androidx.appcompat.app.AppCompatActivity
import androidx.cardview.widget.CardView
import androidx.core.content.FileProvider
import com.google.firebase.database.*
import java.io.File
import java.io.FileOutputStream
import java.io.IOException
import java.text.SimpleDateFormat
import java.util.Calendar
import java.util.Locale

class Purchase : AppCompatActivity() {

    private lateinit var btnAddPurchase: com.google.android.material.floatingactionbutton.FloatingActionButton
    private lateinit var purchaseContainer: LinearLayout
    private lateinit var reversedList : List<PurchaseModel>
    private lateinit var purchaseList: MutableList<PurchaseModel>
    private lateinit var databaseReference: DatabaseReference
    private lateinit var itemsReference: DatabaseReference
    private val itemNameMap = mutableMapOf<String, Product>()
    private lateinit var itemAdapter: ArrayAdapter<String>

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_purchase)
        var topBarTitle = findViewById<TextView>(R.id.topBarTitle)

        var filter = findViewById<ImageView>(R.id.filter)
        filter.setOnClickListener {
            applyFilter()
        }

        var back = findViewById<ImageView>(R.id.back)
        back.setOnClickListener {
            onBackPressed()
        }

        topBarTitle.text = "Purchase"

        btnAddPurchase = findViewById(R.id.btnAdd)
        purchaseContainer = findViewById(R.id.container)

        purchaseList = mutableListOf()
        databaseReference = FirebaseDatabase.getInstance().getReference("purchases")
        itemsReference = FirebaseDatabase.getInstance().getReference("products")

        btnAddPurchase.setOnClickListener {
            showAddPurchaseDialog()
        }

        itemAdapter = ArrayAdapter(this, android.R.layout.simple_dropdown_item_1line, mutableListOf())

        loadItems()
        loadPurchases()
    }

    private fun applyFilter(){
        DialogUtils.filterDialog(this) { filterCriteria ->

            val filteredPurchases = reversedList.filter { purchase ->
                val matchesName = filterCriteria.name.isEmpty() || purchase.supplierName.contains(filterCriteria.name, ignoreCase = true)
                val matchesInvoice = filterCriteria.invoiceNo.isEmpty() || purchase.invoiceNo.contains(filterCriteria.invoiceNo, ignoreCase = true)
                val matchesPrice = (filterCriteria.minPrice == null || purchase.totalAmount >= filterCriteria.minPrice) &&
                        (filterCriteria.maxPrice == null || purchase.totalAmount <= filterCriteria.maxPrice)
                val matchesDate = (filterCriteria.startDate.isEmpty() || purchase.date >= filterCriteria.startDate) &&
                        (filterCriteria.endDate.isEmpty() || purchase.date <= filterCriteria.endDate)

                matchesName && matchesInvoice && matchesPrice && matchesDate
            }

            purchaseContainer.removeAllViews()
            for (purchase in filteredPurchases) {
                addPurchaseCard(purchase)
            }

            if (filteredPurchases.isEmpty()) {
                Toast.makeText(this, "No Record found", Toast.LENGTH_SHORT).show()
            }
        }
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

    private fun loadPurchases() {
        databaseReference.orderByChild("date").addValueEventListener(object : ValueEventListener {
            override fun onDataChange(snapshot: DataSnapshot) {
                val tempList = mutableListOf<PurchaseModel>()
                for (purchaseSnapshot in snapshot.children) {
                    val purchase = purchaseSnapshot.getValue(PurchaseModel::class.java)
                    purchase?.let { tempList.add(it) }
                }

                // Reverse the list to get the latest purchases first
                reversedList = tempList.reversed()

                purchaseContainer.removeAllViews() // Clear existing cards

                for (purchase in reversedList) {
                    addPurchaseCard(purchase) // Add card for each purchase
                }
            }

            override fun onCancelled(error: DatabaseError) {
                Toast.makeText(this@Purchase, "Failed to load purchases!", Toast.LENGTH_SHORT).show()
                Log.d("Crash","Error-5")
            }
        })
    }

    private fun addPurchaseCard(purchase: PurchaseModel) {
        val cardView = LayoutInflater.from(this).inflate(R.layout.list_card_layout, purchaseContainer, false) as CardView
        val invoiceTextView = cardView.findViewById<TextView>(R.id.invTextView)
        val purchaseAmountTextView = cardView.findViewById<TextView>(R.id.amtTextView)
        val purchaseDateTextView = cardView.findViewById<TextView>(R.id.dateTextView)
        val deletePurchaseIcon = cardView.findViewById<ImageView>(R.id.deleteIcon)
        val supplierTextView = cardView.findViewById<TextView>(R.id.cstTextView)
        val shareIcon = cardView.findViewById<ImageView>(R.id.shareIcon)

        invoiceTextView.text = "${purchase.invoiceNo}"
        purchaseAmountTextView.text = "₹${purchase.totalAmount}"
        purchaseDateTextView.text = "${purchase.date}"
        supplierTextView.text = "${purchase.supplierName}"

        deletePurchaseIcon.setOnClickListener {
            showDeleteConfirmationDialog(purchase.purchaseId)
        }


        shareIcon.setOnClickListener {
            sharePDF(purchase)
        }

        cardView.setOnClickListener {
            showPurchaseDialog(purchase)
        }

        purchaseContainer.addView(cardView)
    }

    private fun sharePDF(purchase: PurchaseModel) {
        val pdfDocument = PdfDocument()
        val pageInfo = PdfDocument.PageInfo.Builder(300, 600, 1).create()
        val page = pdfDocument.startPage(pageInfo)
        val canvas = page.canvas
        val paint = Paint()

        // Draw purchase details on the PDF
        paint.textSize = 16f
        canvas.drawText("Invoice No: ${purchase.invoiceNo}", 10f, 25f, paint)
        canvas.drawText("Supplier Name: ${purchase.supplierName}", 10f, 50f, paint)
        canvas.drawText("Date: ${purchase.date}", 10f, 75f, paint)
        canvas.drawText("Total Amount: ₹${purchase.totalAmount}", 10f, 100f, paint)

        // Draw purchase items
        var yPosition = 125f
        paint.textSize = 14f
        for (item in purchase.items) {
            canvas.drawText("${item.itemName} - ₹${item.amount} (Qty: ${item.qty})", 10f, yPosition, paint)
            yPosition += 20f
        }

        pdfDocument.finishPage(page)

        // Save the PDF to external storage
        val fileName = "Purchase_${purchase.invoiceNo}.pdf"
        val file = File(getExternalFilesDir(null), fileName)
        try {
            pdfDocument.writeTo(FileOutputStream(file))
            pdfDocument.close()

            // Share the PDF
            val uri = FileProvider.getUriForFile(this, "${packageName}.provider", file)
            val shareIntent = Intent(Intent.ACTION_SEND).apply {
                type = "application/pdf"
                putExtra(Intent.EXTRA_STREAM, uri)
                addFlags(Intent.FLAG_GRANT_READ_URI_PERMISSION)
            }
            startActivity(Intent.createChooser(shareIntent, "Share Purchase PDF"))
        } catch (e: IOException) {
            e.printStackTrace()
            Toast.makeText(this, "Failed to generate PDF!", Toast.LENGTH_SHORT).show()
        }
    }

    private fun showDeleteConfirmationDialog(purchaseId: String) {
        AlertDialog.Builder(this)
            .setTitle("Delete Purchase")
            .setMessage("Are you sure you want to delete this purchase?")
            .setPositiveButton("Yes") { _, _ -> deletePurchase(purchaseId) }
            .setNegativeButton("No", null)
            .show()
    }

    private fun deletePurchase(purchaseId: String) {
        val databaseReference = FirebaseDatabase.getInstance().getReference("purchases")

        databaseReference.child(purchaseId).get().addOnSuccessListener { snapshot ->
            val purchase = snapshot.getValue(PurchaseModel::class.java)
            if (purchase != null && purchase.items.isNotEmpty()) {
                restoreStock(purchase.items) // Restore stock before deletion
            }

            // Delete the purchase after restoring stock
            databaseReference.child(purchaseId).removeValue()
                .addOnSuccessListener {
                    Toast.makeText(this, "Purchase deleted!", Toast.LENGTH_SHORT).show()
                }
                .addOnFailureListener {
                    Toast.makeText(this, "Failed to delete purchase!", Toast.LENGTH_SHORT).show()
                }
        }.addOnFailureListener {
            Toast.makeText(this, "Failed to fetch purchase details!", Toast.LENGTH_SHORT).show()
        }
    }

    private fun restoreStock(purchaseItems: List<PurchaseItemModel>) {
        val itemsReference = FirebaseDatabase.getInstance().getReference("products")

        for (item in purchaseItems) {
            val productId = item.productId

            itemsReference.child(productId).get()
                .addOnSuccessListener { snapshot ->
                    val product = snapshot.getValue(Product::class.java)

                    if (product != null) {
                        val newStock = (product.stock - item.qty) // Restore stock
                        itemsReference.child(productId).child("stock").setValue(newStock)
                            .addOnFailureListener {
                                Log.d("Stock Restore","Error updating stock for ${item.itemName}")
                            }
                    }
                }.addOnFailureListener {
                    Toast.makeText(this, "Failed to restore stock for ${item.itemName}", Toast.LENGTH_SHORT).show()
                }
        }
    }

    private fun showAddPurchaseDialog() {
        val dialog = Dialog(this)
        dialog.setContentView(R.layout.dialog_add_purchase)

        val edtDate = dialog.findViewById<EditText>(R.id.edtPurchaseDate)
        val edtSupplierName = dialog.findViewById<EditText>(R.id.edtSupplierName)
        val edtInvoiceNo = dialog.findViewById<EditText>(R.id.edtInvoiceNo)
        val btnAddPurchaseItem = dialog.findViewById<Button>(R.id.btnAddPurchaseItem)
        val btnSavePurchase = dialog.findViewById<Button>(R.id.btnSavePurchase)
        val txtTotalAmount = dialog.findViewById<TextView>(R.id.txtTotalAmount)
        val listViewPurchaseItems = dialog.findViewById<ListView>(R.id.listViewPurchaseItems)
        val btnCancel = dialog.findViewById<Button>(R.id.btnCancelPurchase)

        val calendar = Calendar.getInstance()
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

        val purchaseItems = mutableListOf<PurchaseItemModel>()

        btnAddPurchaseItem.setOnClickListener {
            showAddPurchaseItemDialog(purchaseItems, listViewPurchaseItems, txtTotalAmount)
        }

        btnSavePurchase.setOnClickListener {
            val date = edtDate.text.toString()
            val supplierName = edtSupplierName.text.toString()
            val invoiceNo = edtInvoiceNo.text.toString()
            val totalAmount = purchaseItems.sumOf { it.amount }

            if (date.isEmpty() || supplierName.isEmpty() || invoiceNo.isEmpty()) {
                Toast.makeText(this, "Fill all fields!", Toast.LENGTH_SHORT).show()
                return@setOnClickListener
            }

            val purchaseId = databaseReference.push().key!!
            val purchase = PurchaseModel(purchaseId, date, supplierName, invoiceNo, totalAmount, purchaseItems)

            databaseReference.child(purchaseId).setValue(purchase)
                .addOnSuccessListener {
                    Toast.makeText(this, "Purchase Added!", Toast.LENGTH_SHORT).show()
                    dialog.dismiss()
                    updateStock(purchaseItems)
                }
                .addOnFailureListener {
                    Toast.makeText(this, "Failed to Add Purchase!", Toast.LENGTH_SHORT).show()
                }
        }

        btnCancel.setOnClickListener {
            dialog.dismiss()
        }

        dialog.show()
    }

    private fun updateStock(purchaseItems: List<PurchaseItemModel>) {
        for (item in purchaseItems) {
            val product = itemNameMap[item.itemName]
            if (product != null) {
                Log.d("Data","Stock for item: ${item.qty}")
                val newStock = product.stock + item.qty
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

    private fun showAddPurchaseItemDialog(
        purchaseItems: MutableList<PurchaseItemModel>,
        listView: ListView,
        txtTotalAmount: TextView
    ) {
        val dialog = Dialog(this)
        dialog.setContentView(R.layout.dialog_add_purchase_item)

        val edtItemName = dialog.findViewById<AutoCompleteTextView>(R.id.edtPurchaseItemName)
        val edtGrossPrice = dialog.findViewById<EditText>(R.id.edtPurchaseGrossPrice)
        val edtQty = dialog.findViewById<EditText>(R.id.edtPurchaseQty)
        val btnAddToPurchase = dialog.findViewById<Button>(R.id.btnAddToPurchase)

        edtItemName.setAdapter(itemAdapter)
        edtItemName.threshold = 1

        edtItemName.setOnItemClickListener { _, _, position, _ ->
            val selectedName = itemAdapter.getItem(position) ?: return@setOnItemClickListener
            val product = itemNameMap[selectedName]
            if (product != null) {
                edtGrossPrice.setText(product.grossPrice.toString())
            }
        }

        btnAddToPurchase.setOnClickListener {
            val itemName = edtItemName.text.toString()
            val product = itemNameMap[itemName] ?: return@setOnClickListener
            val grossPrice = edtGrossPrice.text.toString().toDoubleOrNull() ?: 0.0
            val qty = edtQty.text.toString().toIntOrNull() ?: 0
            val amount = grossPrice * qty

            val purchaseItem = PurchaseItemModel(
                productId = product.productId,  // Store productId
                itemName = product.productName, // Store name for UI
                grossPrice = grossPrice,
                qty = qty,
                amount = amount
            )

            purchaseItems.add(purchaseItem)

            val adapter = ArrayAdapter(
                this,
                android.R.layout.simple_list_item_1,
                purchaseItems.map { "${it.itemName} - ${it.qty} x ₹${it.grossPrice} = ₹${it.amount}" }
            )
            listView.adapter = adapter
            txtTotalAmount.text = "Total: ₹${purchaseItems.sumOf { it.amount }}"

            dialog.dismiss()
        }

        dialog.show()
    }

    private fun showPurchaseDialog(purchase: PurchaseModel) {
        val dialog = Dialog(this)
        dialog.setContentView(R.layout.dialog_add_purchase)

        val dialogTitle = dialog.findViewById<TextView>(R.id.edtPurchaseTitle)
        val edtDate = dialog.findViewById<EditText>(R.id.edtPurchaseDate)
        val edtSupplierName = dialog.findViewById<EditText>(R.id.edtSupplierName)
        val edtInvoiceNo = dialog.findViewById<EditText>(R.id.edtInvoiceNo)
        val listViewPurchaseItems = dialog.findViewById<ListView>(R.id.listViewPurchaseItems)
        val txtTotalAmount = dialog.findViewById<TextView>(R.id.txtTotalAmount)
        val btnSavePurchase = dialog.findViewById<Button>(R.id.btnSavePurchase)
        val btnCancelPurchase = dialog.findViewById<Button>(R.id.btnCancelPurchase)
        val btnAddPurchaseItem = dialog.findViewById<Button>(R.id.btnAddPurchaseItem)

        dialogTitle.text = "Purchase Details"
        edtDate.setText(purchase.date)
        edtSupplierName.setText(purchase.supplierName)
        edtInvoiceNo.setText(purchase.invoiceNo)
        txtTotalAmount.text = "Total: ₹${purchase.totalAmount}"

        // Disable editing for all fields
        edtDate.isEnabled = false
        edtSupplierName.isEnabled = false
        edtInvoiceNo.isEnabled = false

        // Populate purchase items in the list
        val adapter = ArrayAdapter(
            this,
            android.R.layout.simple_list_item_1,
            purchase.items.map { "${it.itemName} - ₹${String.format("%.2f", it.amount)}" }
        )
        listViewPurchaseItems.adapter = adapter

        btnAddPurchaseItem.visibility = View.GONE

        btnSavePurchase.text = "Edit"
        btnCancelPurchase.text = "Close"

        btnSavePurchase.setOnClickListener {
            updatePurchase(purchase)
            dialog.dismiss()
        }

        btnCancelPurchase.setOnClickListener {
            dialog.dismiss()
        }

        dialog.show()
    }

    private fun updatePurchase(purchase: PurchaseModel) {
        val dialog = Dialog(this)
        dialog.setContentView(R.layout.dialog_add_purchase)

        val dialogTitle = dialog.findViewById<TextView>(R.id.edtPurchaseTitle)
        val edtDate = dialog.findViewById<EditText>(R.id.edtPurchaseDate)
        val edtSupplierName = dialog.findViewById<EditText>(R.id.edtSupplierName)
        val edtInvoiceNo = dialog.findViewById<EditText>(R.id.edtInvoiceNo)
        val btnAddPurchaseItem = dialog.findViewById<Button>(R.id.btnAddPurchaseItem)
        val btnSavePurchase = dialog.findViewById<Button>(R.id.btnSavePurchase)
        val txtTotalAmount = dialog.findViewById<TextView>(R.id.txtTotalAmount)
        val listViewPurchaseItems = dialog.findViewById<ListView>(R.id.listViewPurchaseItems)
        val btnCancelPurchase = dialog.findViewById<Button>(R.id.btnCancelPurchase)

        val calendar = Calendar.getInstance()
        val purchaseItems = purchase.items.toMutableList()

        dialogTitle.text = "Edit Purchase"
        edtDate.setText(purchase.date)
        edtSupplierName.setText(purchase.supplierName)
        edtInvoiceNo.setText(purchase.invoiceNo)
        txtTotalAmount.text = "Total: ₹${purchase.totalAmount}"

        // Enable DatePicker for date field
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

        // Populate purchase items in the list
        val adapter = ArrayAdapter(
            this,
            android.R.layout.simple_list_item_1,
            purchaseItems.map { "${it.itemName} - ₹${String.format("%.2f", it.amount)}" }
        )
        listViewPurchaseItems.adapter = adapter

        btnAddPurchaseItem.setOnClickListener {
            showAddPurchaseItemDialog(purchaseItems, listViewPurchaseItems, txtTotalAmount)
        }

        btnSavePurchase.setOnClickListener {
            val updatedDate = edtDate.text.toString()
            val updatedSupplierName = edtSupplierName.text.toString()
            val updatedInvoiceNo = edtInvoiceNo.text.toString()
            val updatedTotalAmount = purchaseItems.sumOf { it.amount }

            if (updatedDate.isEmpty() || updatedSupplierName.isEmpty() || updatedInvoiceNo.isEmpty()) {
                Toast.makeText(this, "Fill all fields!", Toast.LENGTH_SHORT).show()
                return@setOnClickListener
            }

            val updatedPurchase = PurchaseModel(
                purchaseId = purchase.purchaseId,
                date = updatedDate,
                supplierName = updatedSupplierName,
                invoiceNo = updatedInvoiceNo,
                totalAmount = updatedTotalAmount,
                items = purchaseItems
            )

            databaseReference.child(purchase.purchaseId).setValue(updatedPurchase)
                .addOnSuccessListener {
                    Toast.makeText(this, "Purchase updated successfully!", Toast.LENGTH_SHORT).show()
                    dialog.dismiss()
                }
                .addOnFailureListener {
                    Toast.makeText(this, "Failed to update purchase!", Toast.LENGTH_SHORT).show()
                }
        }

        btnCancelPurchase.setOnClickListener {
            dialog.dismiss()
        }

        dialog.show()
    }

}
