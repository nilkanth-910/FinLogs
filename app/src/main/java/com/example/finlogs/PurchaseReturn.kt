package com.example.finlogs

import android.app.DatePickerDialog
import android.app.Dialog
import android.content.Intent
import android.graphics.pdf.PdfDocument
import android.os.Bundle
import android.util.Log
import android.view.LayoutInflater
import android.view.View
import android.widget.*
import androidx.appcompat.app.AlertDialog
import androidx.appcompat.app.AppCompatActivity
import androidx.cardview.widget.CardView
import com.google.firebase.database.*
import java.io.IOException
import java.text.SimpleDateFormat
import java.util.Calendar
import java.util.Locale
import kotlin.text.format
import androidx.core.content.FileProvider
import android.graphics.Paint
import java.io.File
import java.io.FileOutputStream


class PurchaseReturn : AppCompatActivity() {

    private lateinit var btnAddPurchaseReturn: com.google.android.material.floatingactionbutton.FloatingActionButton
    private lateinit var purchaseReturnContainer: LinearLayout
    private lateinit var purchaseReturnList: MutableList<PurchaseReturnModel>
    private lateinit var reversedList: List<PurchaseReturnModel>
    private lateinit var databaseReference: DatabaseReference
    private lateinit var itemsReference: DatabaseReference
    private val itemNameMap = mutableMapOf<String, Product>()
    private lateinit var itemAdapter: ArrayAdapter<String>

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_purchase_return)
        var topBarTitle = findViewById<TextView>(R.id.topBarTitle)

        var filter = findViewById<ImageView>(R.id.filter)
        filter.setOnClickListener {
            applyFilter()
        }

        var back = findViewById<ImageView>(R.id.back)
        back.setOnClickListener {
            onBackPressed()
        }

        topBarTitle.text = "Purchase Return"

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

    private fun applyFilter(){
        DialogUtils.filterDialog(this) { filterCriteria ->

            val filteredPurchasesReturn = reversedList.filter { purchase ->
                val matchesName = filterCriteria.name.isEmpty() || purchase.supplierName.contains(filterCriteria.name, ignoreCase = true)
                val matchesInvoice = filterCriteria.invoiceNo.isEmpty() || purchase.returninvoiceNo.contains(filterCriteria.invoiceNo, ignoreCase = true)
                val matchesPrice = (filterCriteria.minPrice == null || purchase.totalAmount >= filterCriteria.minPrice) &&
                        (filterCriteria.maxPrice == null || purchase.totalAmount <= filterCriteria.maxPrice)
                val matchesDate = (filterCriteria.startDate.isEmpty() || purchase.date >= filterCriteria.startDate) &&
                        (filterCriteria.endDate.isEmpty() || purchase.date <= filterCriteria.endDate)
                matchesName && matchesInvoice && matchesPrice && matchesDate
            }

            purchaseReturnContainer.removeAllViews()
            for (purchase in filteredPurchasesReturn) {
                addPurchaseReturnCard(purchase)
            }

            if (filteredPurchasesReturn.isEmpty()) {
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

    private fun loadPurchasesReturn() {
        databaseReference.orderByChild("date").addValueEventListener(object : ValueEventListener {
            override fun onDataChange(snapshot: DataSnapshot) {
                val tempList = mutableListOf<PurchaseReturnModel>()
                for (purchaseReturnSnapshot in snapshot.children) {
                    val purchaseReturn = purchaseReturnSnapshot.getValue(PurchaseReturnModel::class.java)
                    purchaseReturn?.let { tempList.add(it) }
                }

                reversedList = tempList.reversed()

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
        val shareIcon = cardView.findViewById<ImageView>(R.id.shareIcon)

        invoiceTextView.text = "${purchaseReturn.returninvoiceNo}"
        purchaseReturnAmountTextView.text = "₹${purchaseReturn.totalAmount}"
        purchaseReturnDateTextView.text = "${purchaseReturn.date}"
        supplierTextView.text = "${purchaseReturn.supplierName}"

        shareIcon.setOnClickListener {
            sharePDF(purchaseReturn)
        }

        deletePurchaseReturnIcon.setOnClickListener {
            showDeleteConfirmationDialog(purchaseReturn.purchaseReturnId)
        }

        cardView.setOnClickListener {
            showPurchaseReturnDialog(purchaseReturn)
        }

        purchaseReturnContainer.addView(cardView)
    }

    private fun sharePDF(purchaseReturn: PurchaseReturnModel) {
        val pdfDocument = PdfDocument()
        val pageInfo = PdfDocument.PageInfo.Builder(300, 600, 1).create()
        val page = pdfDocument.startPage(pageInfo)
        val canvas = page.canvas
        val paint = Paint()

        // Draw purchase return details on the PDF
        paint.textSize = 16f
        canvas.drawText("Return Invoice No: ${purchaseReturn.returninvoiceNo}", 10f, 25f, paint)
        canvas.drawText("Supplier Name: ${purchaseReturn.supplierName}", 10f, 50f, paint)
        canvas.drawText("Date: ${purchaseReturn.date}", 10f, 75f, paint)
        canvas.drawText("Total Amount: ₹${purchaseReturn.totalAmount}", 10f, 100f, paint)

        // Draw purchase return items
        var yPosition = 125f
        paint.textSize = 14f
        for (item in purchaseReturn.purchaseReturnItems) {
            canvas.drawText("${item.itemName} - ₹${item.amount} (Qty: ${item.qty})", 10f, yPosition, paint)
            yPosition += 20f
        }

        pdfDocument.finishPage(page)

        // Save the PDF to external storage
        val fileName = "PurchaseReturn_${purchaseReturn.returninvoiceNo}.pdf"
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
            startActivity(Intent.createChooser(shareIntent, "Share Purchase Return PDF"))
        } catch (e: IOException) {
            e.printStackTrace()
            Toast.makeText(this, "Failed to generate PDF!", Toast.LENGTH_SHORT).show()
        }
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

        val dialogTitle = dialog.findViewById<TextView>(R.id.edtPurchaseTitle)
        val edtDate = dialog.findViewById<EditText>(R.id.edtPurchaseDate)
        val edtSupplierName = dialog.findViewById<EditText>(R.id.edtSupplierName)
        val edtInvoiceNo = dialog.findViewById<EditText>(R.id.edtInvoiceNo)
        val btnAddPurchaseReturnItem = dialog.findViewById<Button>(R.id.btnAddPurchaseItem)
        val btnSavePurchaseReturn = dialog.findViewById<Button>(R.id.btnSavePurchase)
        val txtTotalAmount = dialog.findViewById<TextView>(R.id.txtTotalAmount)
        val listViewPurchaseReturnItems = dialog.findViewById<ListView>(R.id.listViewPurchaseItems)
        val btnCancel = dialog.findViewById<Button>(R.id.btnCancelPurchase)

        dialogTitle.text = "Add Purchase Return"

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

        btnCancel.setOnClickListener {
            dialog.dismiss()
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

    private fun showPurchaseReturnDialog(purchaseReturn: PurchaseReturnModel) {
        val dialog = Dialog(this)
        dialog.setContentView(R.layout.dialog_add_purchase)

        val dialogTitle = dialog.findViewById<TextView>(R.id.edtPurchaseTitle)
        val edtDate = dialog.findViewById<EditText>(R.id.edtPurchaseDate)
        val edtSupplierName = dialog.findViewById<EditText>(R.id.edtSupplierName)
        val edtInvoiceNo = dialog.findViewById<EditText>(R.id.edtInvoiceNo)
        val listViewPurchaseItems = dialog.findViewById<ListView>(R.id.listViewPurchaseItems)
        val txtTotalAmount = dialog.findViewById<TextView>(R.id.txtTotalAmount)
        val btnSavePurchase = dialog.findViewById<Button>(R.id.btnSavePurchase)
        val btnCancel = dialog.findViewById<Button>(R.id.btnCancelPurchase)
        val btnAddPurchaseItem = dialog.findViewById<Button>(R.id.btnAddPurchaseItem)

        dialogTitle.text = "Purchase Return Details"
        edtDate.setText(purchaseReturn.date)
        edtSupplierName.setText(purchaseReturn.supplierName)
        edtInvoiceNo.setText(purchaseReturn.returninvoiceNo)
        txtTotalAmount.text = "Total: ₹${purchaseReturn.totalAmount}"

        // Disable editing for all fields
        edtDate.isEnabled = false
        edtSupplierName.isEnabled = false
        edtInvoiceNo.isEnabled = false

        // Populate purchase return items in the list
        val adapter = ArrayAdapter(
            this,
            android.R.layout.simple_list_item_1,
            purchaseReturn.purchaseReturnItems.map { "${it.itemName} - ₹${String.format("%.2f", it.amount)}" }
        )
        listViewPurchaseItems.adapter = adapter

        btnAddPurchaseItem.visibility = View.GONE

        btnSavePurchase.text = "Edit"
        btnCancel.text = "Close"

        btnSavePurchase.setOnClickListener {
            updatePurchaseReturn(purchaseReturn)
            dialog.dismiss()
        }

        btnCancel.setOnClickListener {
            dialog.dismiss()
        }

        dialog.show()
    }

    private fun updatePurchaseReturn(purchaseReturn: PurchaseReturnModel) {
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
        val btnCancel = dialog.findViewById<Button>(R.id.btnCancelPurchase)

        dialogTitle.text = "Edit Purchase Return"

        val calendar = Calendar.getInstance()
        val purchaseReturnItems = purchaseReturn.purchaseReturnItems.toMutableList()

        // Populate fields with purchase return details
        edtDate.setText(purchaseReturn.date)
        edtSupplierName.setText(purchaseReturn.supplierName)
        edtInvoiceNo.setText(purchaseReturn.returninvoiceNo)
        txtTotalAmount.text = "Total: ₹${purchaseReturn.totalAmount}"

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

        // Populate purchase return items in the list
        val adapter = ArrayAdapter(
            this,
            android.R.layout.simple_list_item_1,
            purchaseReturnItems.map { "${it.itemName} - ₹${String.format("%.2f", it.amount)}" }
        )
        listViewPurchaseItems.adapter = adapter

        btnAddPurchaseItem.setOnClickListener {
            showAddPurchaseReturnItemDialog(purchaseReturnItems, listViewPurchaseItems, txtTotalAmount)
        }

        btnSavePurchase.setOnClickListener {
            val updatedDate = edtDate.text.toString()
            val updatedSupplierName = edtSupplierName.text.toString()
            val updatedInvoiceNo = edtInvoiceNo.text.toString()
            val updatedTotalAmount = purchaseReturnItems.sumOf { it.amount }

            if (updatedDate.isEmpty() || updatedSupplierName.isEmpty() || updatedInvoiceNo.isEmpty()) {
                Toast.makeText(this, "Fill all fields!", Toast.LENGTH_SHORT).show()
                return@setOnClickListener
            }

            val updatedPurchaseReturn = PurchaseReturnModel(
                purchaseReturnId = purchaseReturn.purchaseReturnId,
                date = updatedDate,
                supplierName = updatedSupplierName,
                returninvoiceNo = updatedInvoiceNo,
                totalAmount = updatedTotalAmount,
                purchaseReturnItems = purchaseReturnItems
            )

            databaseReference.child(purchaseReturn.purchaseReturnId).setValue(updatedPurchaseReturn)
                .addOnSuccessListener {
                    Toast.makeText(this, "Purchase Return updated successfully!", Toast.LENGTH_SHORT).show()
                    dialog.dismiss()
                }
                .addOnFailureListener {
                    Toast.makeText(this, "Failed to update Purchase Return!", Toast.LENGTH_SHORT).show()
                }
        }

        btnCancel.setOnClickListener {
            dialog.dismiss()
        }

        dialog.show()
    }

}
