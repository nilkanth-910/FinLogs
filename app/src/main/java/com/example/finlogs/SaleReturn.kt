package com.example.finlogs

import androidx.cardview.widget.CardView
import android.app.Dialog
import android.os.Bundle
import android.util.Log
import android.view.LayoutInflater
import android.widget.*
import androidx.appcompat.app.AppCompatActivity
import com.google.firebase.database.*
import android.app.DatePickerDialog
import android.content.Intent
import android.graphics.Paint
import android.graphics.pdf.PdfDocument
import android.view.View
import androidx.appcompat.app.AlertDialog
import androidx.core.content.FileProvider
import java.io.File
import java.io.FileOutputStream
import java.io.IOException
import java.text.SimpleDateFormat
import java.util.*
import kotlin.text.format

class SaleReturn : AppCompatActivity() {

    private lateinit var btnAddSaleReturn: com.google.android.material.floatingactionbutton.FloatingActionButton
    private lateinit var salesReturnContainer: LinearLayout
    private lateinit var saleReturnList: MutableList<SaleReturnModel>
    private lateinit var reversedList: List<SaleReturnModel>
    private lateinit var databaseReference: DatabaseReference
    private lateinit var itemsReference: DatabaseReference
    private val itemNameMap = mutableMapOf<String, Product>()
    private lateinit var itemAdapter: ArrayAdapter<String>

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_sale_return)
        var topBarTitle = findViewById<TextView>(R.id.topBarTitle)

        var filter = findViewById<ImageView>(R.id.filter)
        filter.setOnClickListener {
            applyFilter()
        }

        var back = findViewById<ImageView>(R.id.back)
        back.setOnClickListener {
            onBackPressed()
        }

        topBarTitle.text = "Sale Return"

        btnAddSaleReturn = findViewById(R.id.btnAdd)
        salesReturnContainer = findViewById(R.id.container)

        saleReturnList = mutableListOf()
        databaseReference = FirebaseDatabase.getInstance().getReference("salesReturn")
        itemsReference = FirebaseDatabase.getInstance().getReference("products")

        btnAddSaleReturn.setOnClickListener {
            showAddSaleReturnDialog()
        }

        itemAdapter = ArrayAdapter(this, android.R.layout.simple_dropdown_item_1line, mutableListOf())
        loadItems()
        loadSalesReturn()
    }

    private fun applyFilter(){
        DialogUtils.filterDialog(this) { filterCriteria ->
            // Apply the filter criteria to the sales list

            val filteredSalesReturn = reversedList.filter { sale ->
                val matchesName = filterCriteria.name.isEmpty() || sale.customerName.contains(filterCriteria.name, ignoreCase = true)
                val matchesInvoice = filterCriteria.invoiceNo.isEmpty() || sale.returnInvoiceNo.contains(filterCriteria.invoiceNo, ignoreCase = true)
                val matchesPrice = (filterCriteria.minPrice == null || sale.totalAmount >= filterCriteria.minPrice) &&
                        (filterCriteria.maxPrice == null || sale.totalAmount <= filterCriteria.maxPrice)
                val matchesDate = (filterCriteria.startDate.isEmpty() || sale.date >= filterCriteria.startDate) &&
                        (filterCriteria.endDate.isEmpty() || sale.date <= filterCriteria.endDate)

                matchesName && matchesInvoice && matchesPrice && matchesDate
            }

            salesReturnContainer.removeAllViews()
            for (sale in filteredSalesReturn) {
                addSaleReturnCard(sale)
            }

            if (filteredSalesReturn.isEmpty()) {
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
        }.addOnFailureListener {
            Log.e("FirebaseData", "Error fetching items: ${it.message}")
        }
    }

    private fun updateAutoCompleteAdapter(itemList: List<String>) {
        itemAdapter.clear()
        itemAdapter.addAll(itemList)
        itemAdapter.notifyDataSetChanged()
    }

    private fun loadSalesReturn() {
        databaseReference.orderByChild("date").addValueEventListener(object : ValueEventListener {
            override fun onDataChange(snapshot: DataSnapshot) {
                val tempList = mutableListOf<SaleReturnModel>()
                for (saleReturnSnapshot in snapshot.children) {
                    val saleReturn = saleReturnSnapshot.getValue(SaleReturnModel::class.java)
                    saleReturn?.let { tempList.add(it) }
                }
                reversedList = tempList.reversed()

                salesReturnContainer.removeAllViews() // Clear existing cards

                for (sale in reversedList) {
                    addSaleReturnCard(sale) // Add card for each sale
                }
            }

            override fun onCancelled(error: DatabaseError) {
                Toast.makeText(this@SaleReturn, "Failed to load sales!", Toast.LENGTH_SHORT).show()
            }
        })
    }

    private fun addSaleReturnCard(saleReturn: SaleReturnModel) {
        val cardView = LayoutInflater.from(this).inflate(R.layout.list_card_layout, salesReturnContainer, false) as CardView
        val invoiceTextView = cardView.findViewById<TextView>(R.id.invTextView)
        val saleAmountTextView = cardView.findViewById<TextView>(R.id.amtTextView)
        val saleDateTextView = cardView.findViewById<TextView>(R.id.dateTextView)
        val deleteSaleIcon = cardView.findViewById<ImageView>(R.id.deleteIcon)
        val cstTextView = cardView.findViewById<TextView>(R.id.cstTextView)
        val shareIcon = cardView.findViewById<ImageView>(R.id.shareIcon)

        invoiceTextView.text = "${saleReturn.returnInvoiceNo}"
        saleAmountTextView.text = "₹${saleReturn.totalAmount}"
        cstTextView.text = "${saleReturn.customerName}"
        saleDateTextView.text = "${saleReturn.date}"

        shareIcon.setOnClickListener {
            sharePDF(saleReturn)
        }


        deleteSaleIcon.setOnClickListener {
            showDeleteConfirmationDialog(saleReturn)
        }

        cardView.setOnClickListener {
            showSaleReturnDialog(saleReturn)
        }

        salesReturnContainer.addView(cardView)
    }

    private fun sharePDF(saleReturn: SaleReturnModel) {
        val pdfDocument = PdfDocument()
        val pageInfo = PdfDocument.PageInfo.Builder(300, 600, 1).create()
        val page = pdfDocument.startPage(pageInfo)
        val canvas = page.canvas
        val paint = Paint()

        // Draw sale return details on the PDF
        paint.textSize = 16f
        canvas.drawText("Return Invoice No: ${saleReturn.returnInvoiceNo}", 10f, 25f, paint)
        canvas.drawText("Customer Name: ${saleReturn.customerName}", 10f, 50f, paint)
        canvas.drawText("Date: ${saleReturn.date}", 10f, 75f, paint)
        canvas.drawText("Total Amount: ₹${saleReturn.totalAmount}", 10f, 100f, paint)

        // Draw sale return items
        var yPosition = 125f
        paint.textSize = 14f
        for (item in saleReturn.saleReturnItems) {
            canvas.drawText("${item.productName} - ₹${item.amount} (Qty: ${item.qty})", 10f, yPosition, paint)
            yPosition += 20f
        }

        pdfDocument.finishPage(page)

        // Save the PDF to external storage
        val fileName = "SaleReturn_${saleReturn.returnInvoiceNo}.pdf"
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
            startActivity(Intent.createChooser(shareIntent, "Share Sale Return PDF"))
        } catch (e: IOException) {
            e.printStackTrace()
            Toast.makeText(this, "Failed to generate PDF!", Toast.LENGTH_SHORT).show()
        }
    }

    private fun showDeleteConfirmationDialog(saleReturn: SaleReturnModel) {
        AlertDialog.Builder(this)
            .setTitle("Delete SaleReturn")
            .setMessage("Are you sure you want to delete this sale?")
            .setPositiveButton("Yes") { _, _ -> deleteSaleReturn(saleReturn) }
            .setNegativeButton("No", null)
            .show()
    }

    private fun deleteSaleReturn(saleReturn: SaleReturnModel) {
        val databaseReference = FirebaseDatabase.getInstance().getReference("salesReturn")

        databaseReference.child(saleReturn.saleReturnId).removeValue()
            .addOnSuccessListener {
                Toast.makeText(this, "SaleReturn deleted successfully!", Toast.LENGTH_SHORT).show()

                if (saleReturn.saleReturnItems.isEmpty()) {
                    Toast.makeText(this, "No items in SaleReturn Bill.", Toast.LENGTH_SHORT).show()
                } else {
                    this.restoreStock(saleReturn.saleReturnItems)
                }
            }
            .addOnFailureListener {
                Toast.makeText(this, "Failed to delete saleReturn!", Toast.LENGTH_SHORT).show()
            }
    }

    fun restoreStock(saleReturnItems: List<SaleItemModel>) {

        for (item in saleReturnItems) {
            val productRef = itemsReference.child(item.productId).child("stock")

            productRef.runTransaction(object : Transaction.Handler {
                override fun doTransaction(mutableData: MutableData): Transaction.Result {
                    val currentStock = mutableData.getValue(Int::class.java) ?: 0
                    val restoredStock = currentStock - item.qty
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

    private fun showAddSaleReturnDialog() {
        val dialog = Dialog(this)
        dialog.setContentView(R.layout.dialog_add_sale)

        val dialogTitle = dialog.findViewById<TextView>(R.id.edtSaleTitle)
        val edtDate = dialog.findViewById<EditText>(R.id.edtSaleDate)
        val edtCustomerName = dialog.findViewById<EditText>(R.id.edtCustomerName)
        val btnAddSaleReturnItem = dialog.findViewById<Button>(R.id.btnAddSaleItem)
        val btnSaveSaleReturn = dialog.findViewById<Button>(R.id.btnSaveSale)
        val txtTotalAmount = dialog.findViewById<TextView>(R.id.txtTotalAmount)
        val listViewSaleReturnItems = dialog.findViewById<ListView>(R.id.listViewSaleItems)
        val btnCancel = dialog.findViewById<Button>(R.id.btnCancelSale)

        val calendar = Calendar.getInstance()


        dialogTitle.text = "Add Sale Return"
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

        val saleReturnItems = mutableListOf<SaleItemModel>()

        btnAddSaleReturnItem.setOnClickListener {
            showAddSaleReturnItemDialog(saleReturnItems, listViewSaleReturnItems, txtTotalAmount)
        }

        btnSaveSaleReturn.setOnClickListener {
            val date = edtDate.text.toString()
            val customerName = edtCustomerName.text.toString()
            val totalAmount = saleReturnItems.sumOf { it.amount }

            if (date.isEmpty() || customerName.isEmpty()) {
                Toast.makeText(this, "Fill all fields!", Toast.LENGTH_SHORT).show()
                return@setOnClickListener
            }

            fetchLastInvoiceNo { lastInvoiceNo ->
                val newInvoiceNo = generateNextInvoiceNo(lastInvoiceNo)
                val saleId = databaseReference.push().key!!

                val sale = SaleReturnModel(saleId, date, customerName, newInvoiceNo, totalAmount, saleReturnItems)

                databaseReference.child(saleId).setValue(sale)
                    .addOnSuccessListener {
                        Toast.makeText(this, "SaleReturn Added!", Toast.LENGTH_SHORT).show()
                        dialog.dismiss()
                        updateStock(saleReturnItems)
                    }
                    .addOnFailureListener {
                        Toast.makeText(this, "Failed to Add SaleReturn!", Toast.LENGTH_SHORT).show()
                    }
            }
        }

        btnCancel.setOnClickListener {
            dialog.dismiss()
        }

        dialog.show()
    }


    private fun showAddSaleReturnItemDialog(
        saleReturnItems: MutableList<SaleItemModel>,
        listView: ListView,
        txtTotalAmount: TextView
    ) {
        val dialog = Dialog(this)
        dialog.setContentView(R.layout.dialog_add_sale_item)

        val edtItemName = dialog.findViewById<AutoCompleteTextView>(R.id.edtSaleItemName)
        val edtSaleReturnPrice = dialog.findViewById<EditText>(R.id.edtSalePrice)
        val edtQty = dialog.findViewById<EditText>(R.id.edtSaleQty)
        val btnAddToSaleReturn = dialog.findViewById<Button>(R.id.btnAddToSale)

        edtItemName.setAdapter(itemAdapter)
        edtItemName.threshold = 1

        edtItemName.setOnItemClickListener { _, _, position, _ ->
            val selectedName = itemAdapter.getItem(position) ?: return@setOnItemClickListener
            val product = itemNameMap[selectedName]
            if (product != null) {
                edtSaleReturnPrice.setText(String.format("%.2f", product.salePrice))
            }
        }

        btnAddToSaleReturn.setOnClickListener {
            val productName = edtItemName.text.toString()
            val saleReturnPrice = edtSaleReturnPrice.text.toString().toDoubleOrNull() ?: 0.0
            val qty = edtQty.text.toString().toIntOrNull() ?: 0
            val amount = saleReturnPrice * qty



            val product = itemNameMap[productName] ?: return@setOnClickListener
            val productId = product.productId

            if (productName.isEmpty() || saleReturnPrice < 0) {
                Toast.makeText(this, "Invalid item details!", Toast.LENGTH_SHORT).show()
                return@setOnClickListener
            }

            if (product.stock <=0) {
                Toast.makeText(this, "No Stock for item", Toast.LENGTH_SHORT).show()
                return@setOnClickListener
            }

            saleReturnItems.add(SaleItemModel(productId, productName, saleReturnPrice, qty, amount))

            val adapter = ArrayAdapter(
                this,
                android.R.layout.simple_list_item_1,
                saleReturnItems.map { "${it.productName} - ₹${String.format("%.2f", it.amount)}" }
            )

            listView.adapter = adapter
            txtTotalAmount.text = "Total: ₹${saleReturnItems.sumOf { it.amount }}"

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
                        val lastSale = saleSnapshot.getValue(SaleReturnModel::class.java)
                        lastSale?.let {
                            lastInvoiceNo = it.returnInvoiceNo
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

    private fun updateStock(saleReturnItems: List<SaleItemModel>) {
        Log.d("StockUpdate", "Updating stock for ${saleReturnItems.size} items")

        for (item in saleReturnItems) {
            val product = itemNameMap[item.productName]
            if (product != null) {
                val newStock = product.stock + item.qty

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


    private fun showSaleReturnDialog(saleReturn: SaleReturnModel) {
        val dialog = Dialog(this)
        dialog.setContentView(R.layout.dialog_add_sale)

        val dialogTitle = dialog.findViewById<TextView>(R.id.edtSaleTitle)
        val edtDate = dialog.findViewById<EditText>(R.id.edtSaleDate)
        val edtCustomerName = dialog.findViewById<EditText>(R.id.edtCustomerName)
        val listViewSaleItems = dialog.findViewById<ListView>(R.id.listViewSaleItems)
        val txtTotalAmount = dialog.findViewById<TextView>(R.id.txtTotalAmount)
        val btnSaveSale = dialog.findViewById<Button>(R.id.btnSaveSale)
        val btnCancel = dialog.findViewById<Button>(R.id.btnCancelSale)
        val btnAddSaleItem = dialog.findViewById<Button>(R.id.btnAddSaleItem)

        dialogTitle.text = "Sale Return Details"
        edtDate.setText(saleReturn.date)
        edtCustomerName.setText(saleReturn.customerName)
        txtTotalAmount.text = "Total: ₹${saleReturn.totalAmount}"

        // Disable editing for all fields
        edtDate.isEnabled = false
        edtCustomerName.isEnabled = false

        // Populate sale return items in the list
        val adapter = ArrayAdapter(
            this,
            android.R.layout.simple_list_item_1,
            saleReturn.saleReturnItems.map { "${it.productName} - ₹${String.format("%.2f", it.amount)}" }
        )
        listViewSaleItems.adapter = adapter

        btnAddSaleItem.visibility = View.GONE

        btnSaveSale.text = "Edit"
        btnCancel.text = "Close"

        btnSaveSale.setOnClickListener {
            updateSaleReturn(saleReturn)
            dialog.dismiss()
        }

        btnCancel.setOnClickListener {
            dialog.dismiss()
        }

        dialog.show()
    }

    private fun updateSaleReturn(saleReturn: SaleReturnModel) {
        val dialog = Dialog(this)
        dialog.setContentView(R.layout.dialog_add_sale)

        val dialogTitle = dialog.findViewById<TextView>(R.id.edtSaleTitle)
        val edtDate = dialog.findViewById<EditText>(R.id.edtSaleDate)
        val edtCustomerName = dialog.findViewById<EditText>(R.id.edtCustomerName)
        val btnAddSaleItem = dialog.findViewById<Button>(R.id.btnAddSaleItem)
        val btnSaveSale = dialog.findViewById<Button>(R.id.btnSaveSale)
        val txtTotalAmount = dialog.findViewById<TextView>(R.id.txtTotalAmount)
        val listViewSaleItems = dialog.findViewById<ListView>(R.id.listViewSaleItems)
        val btnCancel = dialog.findViewById<Button>(R.id.btnCancelSale)

        val calendar = Calendar.getInstance()
        val saleReturnItems = saleReturn.saleReturnItems.toMutableList()

        dialogTitle.text = "Edit Sale Return"
        edtDate.setText(saleReturn.date)
        edtCustomerName.setText(saleReturn.customerName)
        txtTotalAmount.text = "Total: ₹${saleReturn.totalAmount}"

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

        // Populate sale return items in the list
        val adapter = ArrayAdapter(
            this,
            android.R.layout.simple_list_item_1,
            saleReturnItems.map { "${it.productName} - ₹${String.format("%.2f", it.amount)}" }
        )
        listViewSaleItems.adapter = adapter

        btnAddSaleItem.setOnClickListener {
            showAddSaleReturnItemDialog(saleReturnItems, listViewSaleItems, txtTotalAmount)
        }

        btnSaveSale.setOnClickListener {
            val updatedDate = edtDate.text.toString()
            val updatedCustomerName = edtCustomerName.text.toString()
            val updatedTotalAmount = saleReturnItems.sumOf { it.amount }

            if (updatedDate.isEmpty() || updatedCustomerName.isEmpty()) {
                Toast.makeText(this, "Fill all fields!", Toast.LENGTH_SHORT).show()
                return@setOnClickListener
            }

            val updatedSaleReturn = SaleReturnModel(
                saleReturnId = saleReturn.saleReturnId,
                date = updatedDate,
                customerName = updatedCustomerName,
                returnInvoiceNo = saleReturn.returnInvoiceNo,
                totalAmount = updatedTotalAmount,
                saleReturnItems = saleReturnItems
            )

            databaseReference.child(saleReturn.saleReturnId).setValue(updatedSaleReturn)
                .addOnSuccessListener {
                    Toast.makeText(this, "Sale Return updated successfully!", Toast.LENGTH_SHORT).show()
                    dialog.dismiss()
                }
                .addOnFailureListener {
                    Toast.makeText(this, "Failed to update Sale Return!", Toast.LENGTH_SHORT).show()
                }
        }

        btnCancel.setOnClickListener {
            dialog.dismiss()
        }

        dialog.show()
    }
}
