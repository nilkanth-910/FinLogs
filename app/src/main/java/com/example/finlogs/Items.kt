package com.example.finlogs

import android.app.Dialog
import android.os.Bundle
import android.text.Editable
import android.text.TextWatcher
import android.view.LayoutInflater
import android.view.View
import android.widget.*
import androidx.appcompat.app.AlertDialog
import androidx.appcompat.app.AppCompatActivity
import androidx.cardview.widget.CardView
import com.google.firebase.database.DataSnapshot
import com.google.firebase.database.DatabaseError
import com.google.firebase.database.DatabaseReference
import com.google.firebase.database.FirebaseDatabase
import com.google.firebase.database.ValueEventListener
import kotlin.text.compareTo
import kotlin.toString

class Items : AppCompatActivity() {

    private lateinit var itemsContainer: LinearLayout
    private lateinit var addButton: com.google.android.material.floatingactionbutton.FloatingActionButton
    private lateinit var productList: MutableList<Product>
    private lateinit var databaseReference: DatabaseReference
    private lateinit var itemsList: Iterable<DataSnapshot>
    private val database = FirebaseDatabase.getInstance().reference.child("products")

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_items)

        var topBarTitle = findViewById<TextView>(R.id.topBarTitle)
        topBarTitle.text = "Items"

        var filter = findViewById<ImageView>(R.id.filter)
        filter.setOnClickListener {
            applyFilter()
        }


        var back = findViewById<ImageView>(R.id.back)
        back.setOnClickListener {
            onBackPressed()
        }

        databaseReference = FirebaseDatabase.getInstance().getReference("products")
        addButton = findViewById(R.id.btnAdd)
        itemsContainer = findViewById(R.id.container)
        productList = mutableListOf()

        addButton.setOnClickListener {
            showAddItemPopup()
        }

        loadProducts()
    }

    private fun applyFilter() {
        ItemsDialogUtils.filterDialog(this) { filterCriteria ->
            // Apply the filter criteria to the items list
            val filteredItems = itemsList.filter { itemSnapshot ->
                val item = itemSnapshot.getValue(Product::class.java)
                if (item != null) {
                    val matchesName = filterCriteria.name.isEmpty() || item.productName.contains(filterCriteria.name, ignoreCase = true)
                    val matchesPrice = (filterCriteria.minPrice == null || item.mrp >= filterCriteria.minPrice) &&
                            (filterCriteria.maxPrice == null || item.mrp <= filterCriteria.maxPrice)
                    val matchesStock = (filterCriteria.minStock == null || item.stock >= filterCriteria.minStock) &&
                            (filterCriteria.maxStock == null || item.stock <= filterCriteria.maxStock)
                    val matchesRate = (filterCriteria.minRate == null || item.grossPrice >= filterCriteria.minRate) &&
                            (filterCriteria.maxRate == null || item.grossPrice <= filterCriteria.maxRate)

                    matchesName && matchesPrice && matchesStock && matchesRate
                } else {
                    false
                }
            }

            itemsContainer.removeAllViews()
            for (itemSnapshot in filteredItems) {
                val item = itemSnapshot.getValue(Product::class.java)
                if (item != null) {
                    addItemCard(item)
                }
            }

            if (filteredItems.isEmpty()) {
                Toast.makeText(this, "No Record found", Toast.LENGTH_SHORT).show()
            }
        }
    }

    private fun showAddItemPopup() {
        val dialog = Dialog(this)
        val view = LayoutInflater.from(this).inflate(R.layout.dialog_add_item, null)
        dialog.setContentView(view)

        val edtBarcode = view.findViewById<EditText>(R.id.edtBarcode)
        val edtProductName = view.findViewById<EditText>(R.id.edtProductName)
        val edtRate = view.findViewById<EditText>(R.id.edtRate)
        val edtHSN = view.findViewById<EditText>(R.id.edtHSN)
        val edtTax = view.findViewById<EditText>(R.id.edtTax)
        val edtGrossPrice = view.findViewById<EditText>(R.id.edtGrossPrice)
        val edtSalePrice = view.findViewById<EditText>(R.id.edtSalePrice)
        val edtMRP = view.findViewById<EditText>(R.id.edtMRP)
        val edtStock = view.findViewById<EditText>(R.id.edtStock)
        val btnSave = view.findViewById<Button>(R.id.btnSaveItem)
        val btnCancel = view.findViewById<Button>(R.id.btnCancelItem)

        val textWatcher = object : TextWatcher {
            override fun afterTextChanged(s: Editable?) {
                val rate = edtRate.text.toString().toDoubleOrNull() ?: 0.0
                val tax = edtTax.text.toString().toDoubleOrNull() ?: 0.0
                val grossPrice = rate * (1 + tax / 100)
                edtGrossPrice.setText(grossPrice.toString())
            }

            override fun beforeTextChanged(s: CharSequence?, start: Int, count: Int, after: Int) {}
            override fun onTextChanged(s: CharSequence?, start: Int, before: Int, count: Int) {}
        }

        edtRate.addTextChangedListener(textWatcher)
        edtTax.addTextChangedListener(textWatcher)

        btnSave.setOnClickListener {
            val barcode = edtBarcode.text.toString()
            val name = edtProductName.text.toString()
            val rate = edtRate.text.toString().toDoubleOrNull() ?: 0.0
            val hsn = edtHSN.text.toString()
            val tax = edtTax.text.toString().toDoubleOrNull() ?: 0.0
            val grossPrice = edtGrossPrice.text.toString().toDoubleOrNull() ?: 0.0
            val salePrice = edtSalePrice.text.toString().toDoubleOrNull() ?: 0.0
            val mrp = edtMRP.text.toString().toDoubleOrNull() ?: 0.0
            val stock = edtStock.text.toString().toIntOrNull() ?: 0

            if (name.isNotEmpty() && barcode.isNotEmpty()) {
                val productId = database.push().key ?: return@setOnClickListener
                val product = Product(productId, barcode, name, hsn, rate, tax, grossPrice, salePrice, mrp, stock)

                database.child(productId).setValue(product).addOnSuccessListener {
                    Toast.makeText(this, "Product Added!", Toast.LENGTH_SHORT).show()
                    dialog.dismiss()
                    loadProducts()
                }.addOnFailureListener {
                    Toast.makeText(this, "Failed to Add Product!", Toast.LENGTH_SHORT).show()
                }
            } else {
                Toast.makeText(this, "Please fill all fields!", Toast.LENGTH_SHORT).show()
            }
        }

        btnCancel.setOnClickListener {
            dialog.dismiss()
        }
        dialog.show()
    }


    private fun loadProducts() {
        databaseReference.addValueEventListener(object : ValueEventListener {
            override fun onDataChange(snapshot: DataSnapshot) {

                itemsContainer.removeAllViews() // Clear existing cards

                itemsList = snapshot.children
                for (productSnapshot in snapshot.children) {
                    val product = productSnapshot.getValue(Product::class.java)
                    if (product != null) {
                        addItemCard(product) // Add card for each sale
                    }
                }
            }

            override fun onCancelled(error: DatabaseError) {
                Toast.makeText(this@Items, "Failed to load sales!", Toast.LENGTH_SHORT).show()
            }
        })
    }

    private fun addItemCard(product: Product) {
        val cardView = LayoutInflater.from(this).inflate(R.layout.list_card_layout, itemsContainer, false) as CardView
        val itemNameTextView = cardView.findViewById<TextView>(R.id.invTextView)
        val productSalePriceTextView = cardView.findViewById<TextView>(R.id.amtTextView)
        val productMRPTextView = cardView.findViewById<TextView>(R.id.dateTextView)
        val stockTextView = cardView.findViewById<TextView>(R.id.cstTextView)
        val deleteProductIcon = cardView.findViewById<ImageView>(R.id.deleteIcon)
        val shareIcon = cardView.findViewById<ImageView>(R.id.shareIcon)

        shareIcon.visibility = View.GONE

        itemNameTextView.text = "${product.productName}"
        productSalePriceTextView.text = "GP: ₹${product.grossPrice}"
        productMRPTextView.text = "MRP: ₹${product.mrp}"
        stockTextView.text = "Stock: ${product.stock}"

        cardView.setOnClickListener {
            showItemDialog(product)
        }

        deleteProductIcon.setOnClickListener {
            showDeleteConfirmationDialog(product)
        }

        itemsContainer.addView(cardView)
    }

    private fun showDeleteConfirmationDialog(product: Product) {
        AlertDialog.Builder(this)
            .setTitle("Delete Item")
            .setMessage("Are you sure you want to delete this sale?")
            .setPositiveButton("Yes") { _, _ -> deleteItem(product) }
            .setNegativeButton("No", null)
            .show()
    }

    private fun deleteItem(product: Product) {
        database.child(product.productId).removeValue()
            .addOnSuccessListener {
                Toast.makeText(this, "Product deleted successfully!", Toast.LENGTH_SHORT).show()
                loadProducts() // Refresh the product list
            }
            .addOnFailureListener {
                Toast.makeText(this, "Failed to delete product!", Toast.LENGTH_SHORT).show()
            }
    }

    private fun showItemDialog(item: Product) {
        val dialog = Dialog(this)
        dialog.setContentView(R.layout.dialog_add_item)

        val dialogTitle = dialog.findViewById<TextView>(R.id.edtItemTitle)
        val edtBarcode = dialog.findViewById<EditText>(R.id.edtBarcode)
        val edtProductName = dialog.findViewById<EditText>(R.id.edtProductName)
        val edtHSN = dialog.findViewById<EditText>(R.id.edtHSN)
        val edtRate = dialog.findViewById<EditText>(R.id.edtRate)
        val edtTax = dialog.findViewById<EditText>(R.id.edtTax)
        val edtGrossPrice = dialog.findViewById<EditText>(R.id.edtGrossPrice)
        val edtSalePrice = dialog.findViewById<EditText>(R.id.edtSalePrice)
        val edtMRP = dialog.findViewById<EditText>(R.id.edtMRP)
        val edtStock = dialog.findViewById<EditText>(R.id.edtStock)
        val btnSaveItem = dialog.findViewById<Button>(R.id.btnSaveItem)
        val btnCancel = dialog.findViewById<Button>(R.id.btnCancelItem)

        dialogTitle.text = "Item Details"
        edtBarcode.setText(item.barcode)
        edtProductName.setText(item.productName)
        edtHSN.setText(item.hsn)
        edtRate.setText(item.rate.toString())
        edtTax.setText(item.tax.toString())
        edtGrossPrice.setText(item.grossPrice.toString())
        edtSalePrice.setText(item.salePrice.toString())
        edtMRP.setText(item.mrp.toString())
        edtStock.setText(item.stock.toString())

        // Disable editing for all fields
        edtBarcode.isEnabled = false
        edtProductName.isEnabled = false
        edtHSN.isEnabled = false
        edtRate.isEnabled = false
        edtTax.isEnabled = false
        edtGrossPrice.isEnabled = false
        edtSalePrice.isEnabled = false
        edtMRP.isEnabled = false
        edtStock.isEnabled = false

        btnSaveItem.text = "Edit"
        btnCancel.text = "Close"

        btnSaveItem.setOnClickListener {
            updateItem(item)
            dialog.dismiss()
        }

        btnCancel.setOnClickListener {
            dialog.dismiss()
        }

        dialog.show()
    }

    private fun updateItem(item: Product) {
        val dialog = Dialog(this)
        dialog.setContentView(R.layout.dialog_add_item)

        val dialogTitle = dialog.findViewById<TextView>(R.id.edtItemTitle)
        val edtBarcode = dialog.findViewById<EditText>(R.id.edtBarcode)
        val edtProductName = dialog.findViewById<EditText>(R.id.edtProductName)
        val edtHSN = dialog.findViewById<EditText>(R.id.edtHSN)
        val edtRate = dialog.findViewById<EditText>(R.id.edtRate)
        val edtTax = dialog.findViewById<EditText>(R.id.edtTax)
        val edtGrossPrice = dialog.findViewById<EditText>(R.id.edtGrossPrice)
        val edtSalePrice = dialog.findViewById<EditText>(R.id.edtSalePrice)
        val edtMRP = dialog.findViewById<EditText>(R.id.edtMRP)
        val edtStock = dialog.findViewById<EditText>(R.id.edtStock)
        val btnSaveItem = dialog.findViewById<Button>(R.id.btnSaveItem)

        dialogTitle.text = "Update Item"
        edtBarcode.setText(item.barcode)
        edtProductName.setText(item.productName)
        edtHSN.setText(item.hsn)
        edtRate.setText(item.rate.toString())
        edtTax.setText(item.tax.toString())
        edtGrossPrice.setText(item.grossPrice.toString())
        edtSalePrice.setText(item.salePrice.toString())
        edtMRP.setText(item.mrp.toString())
        edtStock.setText(item.stock.toString())

        btnSaveItem.setOnClickListener {
            val updatedItem = Product(
                productId = item.productId,
                barcode = edtBarcode.text.toString(),
                productName = edtProductName.text.toString(),
                hsn = edtHSN.text.toString(),
                rate = edtRate.text.toString().toDoubleOrNull() ?: 0.0,
                tax = edtTax.text.toString().toDoubleOrNull() ?: 0.0,
                grossPrice = edtGrossPrice.text.toString().toDoubleOrNull() ?: 0.0,
                salePrice = edtSalePrice.text.toString().toDoubleOrNull() ?: 0.0,
                mrp = edtMRP.text.toString().toDoubleOrNull() ?: 0.0,
                stock = edtStock.text.toString().toIntOrNull() ?: 0
            )

            database.child(item.productId).setValue(updatedItem)
                .addOnSuccessListener {
                    Toast.makeText(this, "Item updated successfully!", Toast.LENGTH_SHORT).show()
                    dialog.dismiss()
                }
                .addOnFailureListener {
                    Toast.makeText(this, "Failed to update item!", Toast.LENGTH_SHORT).show()
                }
        }

        dialog.show()
    }

}
