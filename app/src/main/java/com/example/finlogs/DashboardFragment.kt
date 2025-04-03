package com.example.finlogs

import android.content.Intent
import android.os.Bundle
import android.util.Log
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.ArrayAdapter
import android.widget.LinearLayout
import android.widget.ListView
import android.widget.TextView
import android.widget.Toast
import androidx.cardview.widget.CardView
import androidx.fragment.app.Fragment
import com.google.android.material.tabs.TabLayout
import com.google.firebase.database.*
import java.text.NumberFormat
import java.util.*

class DashboardFragment : Fragment() {

    private lateinit var tabLayout: TabLayout
    private lateinit var latestListContainer: LinearLayout
    private lateinit var latestListView: ListView
    private lateinit var salesReference: DatabaseReference
    private lateinit var purchasesReference: DatabaseReference
    private lateinit var expensesReference: DatabaseReference
    private lateinit var itemsReference: DatabaseReference
    private lateinit var saleReturnReference: DatabaseReference
    private lateinit var purchaseReturnReference: DatabaseReference
    private lateinit var ttlSales: TextView
    private lateinit var ttlPurchase: TextView
    private lateinit var ttlExpenses: TextView
    private lateinit var ttlProducts: TextView
    private lateinit var ttlSalesReturn: TextView
    private lateinit var ttlPurchaseReturn: TextView

    override fun onCreateView(
        inflater: LayoutInflater, container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View? {
        val view = inflater.inflate(R.layout.fragment_dashboard, container, false)

        var salesCard = view.findViewById<CardView>(R.id.cardSales)
        var purchaseCard = view.findViewById<CardView>(R.id.cardPurchase)
        var expensesCard = view.findViewById<CardView>(R.id.cardExpenses)
        var productsCard = view.findViewById<CardView>(R.id.cardProducts)
        var salesReturnCard = view.findViewById<CardView>(R.id.cardSalesReturn)
        var purchaseReturnCard = view.findViewById<CardView>(R.id.cardPurchaseReturn)

        ttlSales = view.findViewById<TextView>(R.id.ttlSale)
        ttlPurchase = view.findViewById<TextView>(R.id.ttlPurchase)
        ttlExpenses = view.findViewById<TextView>(R.id.ttlExpense)
        ttlProducts = view.findViewById<TextView>(R.id.ttlItems)
        ttlSalesReturn = view.findViewById<TextView>(R.id.ttlSalesReturn)
        ttlPurchaseReturn = view.findViewById<TextView>(R.id.ttlPurchaseReturn)

        salesCard.setOnClickListener {
            startActivity(Intent(requireActivity(), Sale::class.java))
        }
        purchaseCard.setOnClickListener {
            startActivity(Intent(requireActivity(), Purchase::class.java))
        }
        expensesCard.setOnClickListener {
            startActivity(Intent(requireActivity(), Expense::class.java))
        }
        productsCard.setOnClickListener {
            startActivity(Intent(requireActivity(), Items::class.java))
        }
        salesReturnCard.setOnClickListener {
            startActivity(Intent(requireActivity(), SaleReturn::class.java))
        }
        purchaseReturnCard.setOnClickListener {
            startActivity(Intent(requireActivity(), PurchaseReturn::class.java))
        }

        tabLayout = view.findViewById(R.id.tabLayout)
        latestListContainer = view.findViewById(R.id.latestListContainer)
        latestListView = latestListContainer.findViewById(R.id.latestListView) // Initialize the ListView

        salesReference = FirebaseDatabase.getInstance().getReference("sales")
        purchasesReference = FirebaseDatabase.getInstance().getReference("purchases")
        expensesReference = FirebaseDatabase.getInstance().getReference("expenses")
        itemsReference = FirebaseDatabase.getInstance().getReference("products")
        saleReturnReference = FirebaseDatabase.getInstance().getReference("salesReturn")
        purchaseReturnReference = FirebaseDatabase.getInstance().getReference("purchasesReturn")

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
        calculateTotalItems()
        calculateTotalSalesReturn()
        calculateTotalPurchaseReturn()

        return view
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
                Toast.makeText(requireContext(), "Error loading sales data.", Toast.LENGTH_SHORT).show()
            }
        })
    }

    private fun calculateTotalSalesReturn() {
        saleReturnReference.addValueEventListener(object : ValueEventListener {
            override fun onDataChange(snapshot: DataSnapshot) {
                var totalSalesReturn = 0.0
                for (saleReturnSnapshot in snapshot.children) {
                    val saleReturn = saleReturnSnapshot.getValue(SaleReturnModel::class.java)
                    saleReturn?.let {
                        totalSalesReturn += it.totalAmount ?: 0.0
                    }
                }
                val numberFormat = NumberFormat.getNumberInstance(Locale.getDefault())
                ttlSalesReturn.text = "₹${numberFormat.format(totalSalesReturn)}"
            }

            override fun onCancelled(error: DatabaseError) {
                Log.e("Firebase", "Error loading sales return for total calculation: ${error.message}")
                Toast.makeText(requireContext(), "Error loading sales return data.", Toast.LENGTH_SHORT).show()
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
                Toast.makeText(requireContext(), "Error loading purchase data.", Toast.LENGTH_SHORT).show()
            }
        })
    }

    private fun calculateTotalPurchaseReturn() {
        purchaseReturnReference.addValueEventListener(object : ValueEventListener {
            override fun onDataChange(snapshot: DataSnapshot) {
                var totalPurchaseReturn = 0.0
                for (purchaseReturnSnapshot in snapshot.children) {
                    val purchaseReturn = purchaseReturnSnapshot.getValue(PurchaseReturnModel::class.java)
                    purchaseReturn?.let {
                        totalPurchaseReturn += it.totalAmount ?: 0.0
                    }
                }
                val numberFormat = NumberFormat.getNumberInstance(Locale.getDefault())
                ttlPurchaseReturn.text = "₹${numberFormat.format(totalPurchaseReturn)}"
            }

            override fun onCancelled(error: DatabaseError) {
                Log.e("Firebase", "Error loading purchase return for total calculation: ${error.message}")
                Toast.makeText(requireContext(), "Error loading purchase return data.", Toast.LENGTH_SHORT).show()
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
                Toast.makeText(requireContext(), "Error loading expense data.", Toast.LENGTH_SHORT).show()
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
                Toast.makeText(requireContext(), "Error loading item data.", Toast.LENGTH_SHORT).show()
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
                        requireContext(),
                        android.R.layout.simple_list_item_1,
                        salesList.map { "${it.customerName} | ${it.invoiceNo} | ₹${it.totalAmount}" }
                    )
                    latestListView.adapter = adapter
                }

                override fun onCancelled(error: DatabaseError) {
                    Log.e("Firebase", "Error loading latest sales: ${error.message}")
                    Toast.makeText(requireContext(), "Error loading latest sales.", Toast.LENGTH_SHORT).show()
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
                        requireContext(),
                        android.R.layout.simple_list_item_1,
                        purchasesList
                    )
                    latestListView.adapter = adapter
                }

                override fun onCancelled(error: DatabaseError) {
                    Log.e("Firebase", "Error loading latest purchases: ${error.message}")
                    Toast.makeText(requireContext(), "Error loading latest purchases.", Toast.LENGTH_SHORT).show()
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
                        requireContext(),
                        android.R.layout.simple_list_item_1,
                        expensesList
                    )
                    latestListView.adapter = adapter
                }

                override fun onCancelled(error: DatabaseError) {
                    Log.e("Firebase", "Error loading latest expenses: ${error.message}")
                    Toast.makeText(requireContext(), "Error loading latest expenses.", Toast.LENGTH_SHORT).show()
                }
            })
    }
}