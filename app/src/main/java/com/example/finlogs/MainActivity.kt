package com.example.finlogs

import android.content.Intent
import android.os.Bundle
import android.util.Log
import android.widget.ArrayAdapter
import android.widget.LinearLayout
import android.widget.ListView
import android.widget.TextView
import android.widget.Toast
import androidx.activity.enableEdgeToEdge
import androidx.appcompat.app.AppCompatActivity
import androidx.cardview.widget.CardView
import com.google.android.material.tabs.TabLayout
import com.google.firebase.database.*
import java.text.NumberFormat
import java.text.SimpleDateFormat
import java.util.*

class MainActivity : AppCompatActivity() {

    private val firebaseHelper = FirebaseHelper()
    private lateinit var tabLayout: TabLayout
    private lateinit var latestListContainer: LinearLayout
    private lateinit var latestListView: ListView
    private lateinit var salesReference: DatabaseReference
    private lateinit var purchasesReference: DatabaseReference
    private lateinit var expensesReference: DatabaseReference
    private lateinit var itemsReference: DatabaseReference // Reference for inventory items
    private lateinit var ttlSales: TextView
    private lateinit var ttlPurchase: TextView
    private lateinit var ttlExpenses: TextView
    private lateinit var ttlProducts: TextView

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        enableEdgeToEdge()
        setContentView(R.layout.activity_main)

        var salesCard = findViewById <CardView>(R.id.cardSales)
        var purchaseCard = findViewById <CardView>(R.id.cardPurchase)
        var expensesCard = findViewById <CardView>(R.id.cardExpenses)
        var productsCard = findViewById <CardView>(R.id.cardProducts)

        ttlSales = findViewById<TextView>(R.id.ttlSale)
        ttlPurchase = findViewById<TextView>(R.id.ttlPurchase)
        ttlExpenses = findViewById<TextView>(R.id.ttlExpense)
        ttlProducts = findViewById<TextView>(R.id.ttlItems)

        salesCard.setOnClickListener {
            startActivity(Intent(this, Sale::class.java))
        }
        purchaseCard.setOnClickListener {
            startActivity(Intent(this, Purchase::class.java))
        }
        expensesCard.setOnClickListener {
            startActivity(Intent(this, Expense::class.java))
        }
        productsCard.setOnClickListener {
            startActivity(Intent(this, Items::class.java))
        }
        tabLayout = findViewById(R.id.tabLayout)
        latestListContainer = findViewById(R.id.latestListContainer)
        latestListView = latestListContainer.findViewById(R.id.latestListView) // Initialize the ListView

        salesReference = FirebaseDatabase.getInstance().getReference("sales")
        purchasesReference = FirebaseDatabase.getInstance().getReference("purchases")
        expensesReference = FirebaseDatabase.getInstance().getReference("expenses")
        itemsReference = FirebaseDatabase.getInstance().getReference("products") // Initialize items reference

        tabLayout.addOnTabSelectedListener(object : TabLayout.OnTabSelectedListener {
            override fun onTabSelected(tab: TabLayout.Tab?) {
                when (tab?.position) {
                    0 -> loadLatestSales()
                    1 -> loadLatestPurchases()
                    2 -> loadLatestExpenses()
                }
            }

            override fun onTabUnselected(tab: TabLayout.Tab?) {
                // Optional
            }

            override fun onTabReselected(tab: TabLayout.Tab?) {
                // Optional
            }
        })

        loadLatestSales()
        calculateTotalSales()
        calculateTotalPurchases()
        calculateTotalExpenses()
        calculateTotalItems() // Call the function to calculate and display total items
    }

    private fun calculateTotalSales() {
        salesReference.addValueEventListener(object : ValueEventListener {
            override fun onDataChange(snapshot: DataSnapshot) {
                var totalSales = 0.0
                for (saleSnapshot in snapshot.children) {
                    val sale = saleSnapshot.getValue(SaleModel::class.java)
                    sale?.let {
                        totalSales += it.totalAmount ?: 0.0
                    }
                }
                val numberFormat = NumberFormat.getNumberInstance(Locale.getDefault())
                ttlSales.text = "₹${numberFormat.format(totalSales)}"
            }

            override fun onCancelled(error: DatabaseError) {
                Log.e("Firebase", "Error loading sales for total calculation: ${error.message}")
                Toast.makeText(this@MainActivity, "Error loading sales data.", Toast.LENGTH_SHORT).show()
            }
        })
    }

    private fun calculateTotalPurchases() {
        purchasesReference.addValueEventListener(object : ValueEventListener {
            override fun onDataChange(snapshot: DataSnapshot) {
                var totalPurchase = 0.0
                for (purchaseSnapshot in snapshot.children) {
                    val purchase = purchaseSnapshot.getValue(PurchaseModel::class.java)
                    purchase?.let {
                        totalPurchase += it.totalAmount ?: 0.0
                    }
                }
                val numberFormat = NumberFormat.getNumberInstance(Locale.getDefault())
                ttlPurchase.text = "₹${numberFormat.format(totalPurchase)}"
            }

            override fun onCancelled(error: DatabaseError) {
                Log.e("Firebase", "Error loading purchases for total calculation: ${error.message}")
                Toast.makeText(this@MainActivity, "Error loading purchase data.", Toast.LENGTH_SHORT).show()
            }
        })
    }

    private fun calculateTotalExpenses() {
        expensesReference.addValueEventListener(object : ValueEventListener {
            override fun onDataChange(snapshot: DataSnapshot) {
                var totalExpense = 0.0
                for (expenseSnapshot in snapshot.children) {
                    val expense = expenseSnapshot.getValue(ExpenseModel::class.java)
                    expense?.let {
                        totalExpense += it.amount ?: 0.0
                    }
                }
                val numberFormat = NumberFormat.getNumberInstance(Locale.getDefault())
                ttlExpenses.text = "₹${numberFormat.format(totalExpense)}"
            }

            override fun onCancelled(error: DatabaseError) {
                Log.e("Firebase", "Error loading expenses for total calculation: ${error.message}")
                Toast.makeText(this@MainActivity, "Error loading expense data.", Toast.LENGTH_SHORT).show()
            }
        })
    }

    private fun calculateTotalItems() {
        itemsReference.addValueEventListener(object : ValueEventListener {
            override fun onDataChange(snapshot: DataSnapshot) {
                val totalItems = snapshot.childrenCount // Get the count of child nodes
                val numberFormat = NumberFormat.getNumberInstance(Locale.getDefault()) // Use NumberFormat for items as well if needed
                ttlProducts.text = numberFormat.format(totalItems.toDouble()).toString() // Format as number (no decimals for count)
            }

            override fun onCancelled(error: DatabaseError) {
                Log.e("Firebase", "Error loading items for total count: ${error.message}")
                Toast.makeText(this@MainActivity, "Error loading item data.", Toast.LENGTH_SHORT).show()
            }
        })
    }

    private fun loadLatestSales() {
        salesReference.orderByChild("date").limitToLast(5)
            .addValueEventListener(object : ValueEventListener {
                override fun onDataChange(snapshot: DataSnapshot) {
                    val salesList = mutableListOf<SaleModel>()
                    val reversedSnapshot = snapshot.children.reversed()
                    for (saleSnapshot in reversedSnapshot) {
                        val sale = saleSnapshot.getValue(SaleModel::class.java)
                        sale?.let {
                            salesList.add(it)
                        }
                    }
                    val adapter = ArrayAdapter(
                        this@MainActivity,
                        android.R.layout.simple_list_item_1,
                        salesList.map { "${it.customerName} | ${it.invoiceNo} | ₹${it.totalAmount}" }
                    )
                    latestListView.adapter = adapter
                }

                override fun onCancelled(error: DatabaseError) {
                    Log.e("Firebase", "Error loading latest sales: ${error.message}")
                    Toast.makeText(this@MainActivity, "Error loading latest sales.", Toast.LENGTH_SHORT).show()
                }
            })
    }

    private fun loadLatestPurchases() {
        purchasesReference.orderByChild("date").limitToLast(5)
            .addValueEventListener(object : ValueEventListener {
                override fun onDataChange(snapshot: DataSnapshot) {
                    val purchasesList = mutableListOf<String>()
                    val reversedSnapshot = snapshot.children.reversed()
                    for (purchaseSnapshot in reversedSnapshot) {
                        val purchase = purchaseSnapshot.getValue(PurchaseModel::class.java)
                        purchase?.let {
                            purchasesList.add("${it.supplierName} | ${it.invoiceNo} | ₹${it.totalAmount}")
                        }
                    }
                    val adapter = ArrayAdapter(
                        this@MainActivity,
                        android.R.layout.simple_list_item_1,
                        purchasesList
                    )
                    latestListView.adapter = adapter
                }

                override fun onCancelled(error: DatabaseError) {
                    Log.e("Firebase", "Error loading latest purchases: ${error.message}")
                    Toast.makeText(this@MainActivity, "Error loading latest purchases.", Toast.LENGTH_SHORT).show()
                }
            })
    }

    private fun loadLatestExpenses() {
        expensesReference.orderByChild("date").limitToLast(5)
            .addValueEventListener(object : ValueEventListener {
                override fun onDataChange(snapshot: DataSnapshot) {
                    val expensesList = mutableListOf<String>()
                    val reversedSnapshot = snapshot.children.reversed()
                    for (expenseSnapshot in reversedSnapshot) {
                        val expense = expenseSnapshot.getValue(ExpenseModel::class.java)
                        expense?.let {
                            expensesList.add("${it.payee} | ${it.category} | ₹${it.amount}")
                        }
                    }
                    val adapter = ArrayAdapter(
                        this@MainActivity,
                        android.R.layout.simple_list_item_1,
                        expensesList
                    )
                    latestListView.adapter = adapter
                }

                override fun onCancelled(error: DatabaseError) {
                    Log.e("Firebase", "Error loading latest expenses: ${error.message}")
                    Toast.makeText(this@MainActivity, "Error loading latest expenses.", Toast.LENGTH_SHORT).show()
                }
            })
    }
}