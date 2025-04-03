package com.example.finlogs

import android.app.Dialog
import android.os.Bundle
import android.text.Editable
import android.text.TextWatcher
import android.view.LayoutInflater
import android.widget.*
import androidx.appcompat.app.AlertDialog
import androidx.appcompat.app.AppCompatActivity
import androidx.cardview.widget.CardView
import com.google.firebase.database.DataSnapshot
import com.google.firebase.database.DatabaseError
import com.google.firebase.database.DatabaseReference
import com.google.firebase.database.FirebaseDatabase
import com.google.firebase.database.ValueEventListener
import kotlin.toString

class Items : AppCompatActivity() {

    private lateinit var itemsContainer: LinearLayout
    private lateinit var addButton: com.google.android.material.floatingactionbutton.FloatingActionButton
    private lateinit var productList: MutableList<Product>
    private lateinit var databaseReference: DatabaseReference
    private val database = FirebaseDatabase.getInstance().reference.child("products")

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_items)

        var topBarTitle = findViewById<TextView>(R.id.topBarTitle)
        topBarTitle.text = topBarTitle.text.toString() + " - Items"

        databaseReference = FirebaseDatabase.getInstance().getReference("products")
        addButton = findViewById(R.id.btnAdd)
        itemsContainer = findViewById(R.id.container)
        productList = mutableListOf()

        addButton.setOnClickListener {
            showAddItemPopup()
        }

        loadProducts()
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

        dialog.show()
    }


    private fun loadProducts() {
        databaseReference.addValueEventListener(object : ValueEventListener {
            override fun onDataChange(snapshot: DataSnapshot) {

                itemsContainer.removeAllViews() // Clear existing cards

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

        itemNameTextView.text = "${product.productName}"
        productSalePriceTextView.text = "GP: ₹${product.grossPrice}"
        productMRPTextView.text = "MRP: ₹${product.mrp}"
        stockTextView.text = "Stock: ${product.stock}"


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

}
