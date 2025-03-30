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
import java.text.SimpleDateFormat
import java.util.*

class MainActivity : AppCompatActivity() {

    private val firebaseHelper = FirebaseHelper()
    private lateinit var tabLayout: TabLayout
    private lateinit var latestListContainer: LinearLayout

    private lateinit var latestListView: ListView

    private lateinit var salesReference: DatabaseReference
    private lateinit var purchasesReference: DatabaseReference
    private lateinit var expensesReference: DatabaseReference // Assuming you have an expenses reference

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        enableEdgeToEdge()
        setContentView(R.layout.activity_main)

        findViewById<CardView>(R.id.cardSales).setOnClickListener {
            startActivity(Intent(this, Sale::class.java))
        }
        findViewById<CardView>(R.id.cardPurchase).setOnClickListener {
            startActivity(Intent(this, Purchase::class.java))
        }
        findViewById<CardView>(R.id.cardExpenses).setOnClickListener {
            startActivity(Intent(this, Expense::class.java))
        }
        findViewById<CardView>(R.id.cardProducts).setOnClickListener {
            startActivity(Intent(this, Items::class.java))
        }
        tabLayout = findViewById(R.id.tabLayout)
        latestListContainer = findViewById(R.id.latestListContainer)
        latestListView = latestListContainer.findViewById(R.id.latestListView) // Initialize the ListView

        salesReference = FirebaseDatabase.getInstance().getReference("sales")
        purchasesReference = FirebaseDatabase.getInstance().getReference("purchases")
        expensesReference = FirebaseDatabase.getInstance().getReference("expenses")

        tabLayout.addOnTabSelectedListener(object : TabLayout.OnTabSelectedListener {
            override fun onTabSelected(tab: TabLayout.Tab?) {
                when (tab?.position) {
                    0 -> loadLatestSales()
                    1 -> loadLatestPurchases()
                    //2 -> loadLatestExpenses()
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
                        salesList.map { "${it.customerName} | ${it.invoiceNo} | ₹${it.totalAmount} | ${it.date}" }
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
                            purchasesList.add("${it.supplierName} | ${it.invoiceNo} | ₹${it.totalAmount} | ${it.date}")
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

//    private fun loadLatestExpenses() {
//        expensesReference.orderByChild("date").limitToLast(5) // Assuming 'date' field for sorting and ExpenseModel
//            .get().addOnSuccessListener { snapshot ->
//                val expensesList = mutableListOf<String>()
//                val dateFormatter = SimpleDateFormat("dd/MM/yyyy", Locale.getDefault())
//                for (expenseSnapshot in snapshot.children) {
//                    val expense = expenseSnapshot.getValue(ExpenseModel::class.java) // Replace with your ExpenseModel
//                    expense?.let {
//                        expensesList.add("Expense: ${it.description}, Amt-₹${it.amount}, Date: ${it.date}") // Adjust based on your ExpenseModel fields
//                    }
//                }
//                latestTextView.text = expensesList.joinToString("\n")
//            }.addOnFailureListener { error ->
//                Log.e("Firebase", "Error loading latest expenses: ${error.message}")
//                latestTextView.text = "Error loading expenses."
//            }
//    }
}