package com.example.finlogs

import com.google.firebase.database.FirebaseDatabase
import com.google.firebase.database.DatabaseReference
import android.util.Log

class FirebaseHelper {
    private val database: DatabaseReference = FirebaseDatabase.getInstance().reference.child("products")
    private val salesDatabase: DatabaseReference = FirebaseDatabase.getInstance().reference.child("sales")

    // Add a new product to Firebase
    fun addProduct(product: Product, onSuccess: () -> Unit, onFailure: (Exception) -> Unit) {
        val productId = database.push().key
        if (productId == null) {
            onFailure(Exception("Failed to generate product ID"))
            return
        }

        val newProduct = product.copy(productId = productId)
        database.child(productId).setValue(newProduct)
            .addOnSuccessListener { onSuccess() }
            .addOnFailureListener { onFailure(it) }
    }

    // Retrieve all products from Firebase
    fun getProducts(onSuccess: (List<Product>) -> Unit, onFailure: (Exception) -> Unit) {
        database.get().addOnSuccessListener { snapshot ->
            val productList = snapshot.children.mapNotNull { it.getValue(Product::class.java) }
            onSuccess(productList)
        }.addOnFailureListener {
            onFailure(it)
        }
    }

    // Update an existing product in Firebase
    fun updateProduct(product: Product, onSuccess: () -> Unit, onFailure: (Exception) -> Unit) {
        if (product.productId.isEmpty()) {
            onFailure(Exception("Product ID is missing"))
            return
        }

        database.child(product.productId).setValue(product)
            .addOnSuccessListener { onSuccess() }
            .addOnFailureListener { onFailure(it) }
    }

    // Delete a product from Firebase
    fun deleteProduct(productId: String, onSuccess: () -> Unit, onFailure: (Exception) -> Unit) {
        if (productId.isEmpty()) {
            onFailure(Exception("Invalid Product ID"))
            return
        }

        database.child(productId).removeValue()
            .addOnSuccessListener { onSuccess() }
            .addOnFailureListener { onFailure(it) }
    }

    // Add a new sale entry to Firebase
    fun addSale(sale: SaleModel, onSuccess: () -> Unit, onFailure: (Exception) -> Unit) {
        val saleId = salesDatabase.push().key
        if (saleId == null) {
            onFailure(Exception("Failed to generate sale ID"))
            return
        }

        val newSale = sale.copy(saleId = saleId)
        salesDatabase.child(saleId).setValue(newSale)
            .addOnSuccessListener { onSuccess() }
            .addOnFailureListener { onFailure(it) }
    }

    // Retrieve all sales from Firebase
    fun getSales(onSuccess: (List<SaleModel>) -> Unit, onFailure: (Exception) -> Unit) {
        salesDatabase.get().addOnSuccessListener { snapshot ->
            val saleList = snapshot.children.mapNotNull { it.getValue(SaleModel::class.java) }
            onSuccess(saleList)
        }.addOnFailureListener {
            onFailure(it)
        }
    }
}
