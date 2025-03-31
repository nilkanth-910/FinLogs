package com.example.finlogs

import android.app.DatePickerDialog
import android.app.Dialog
import android.os.Bundle
import android.view.LayoutInflater
import android.widget.*
import android.widget.TextView
import androidx.appcompat.app.AlertDialog
import androidx.appcompat.app.AppCompatActivity
import androidx.cardview.widget.CardView
import com.google.android.material.floatingactionbutton.FloatingActionButton
import com.google.firebase.database.DataSnapshot
import com.google.firebase.database.DatabaseError
import com.google.firebase.database.DatabaseReference
import com.google.firebase.database.FirebaseDatabase
import com.google.firebase.database.ValueEventListener
import java.text.SimpleDateFormat
import java.util.*

class Expense : AppCompatActivity() {

    private lateinit var itemsContainer: LinearLayout
    private lateinit var databaseReference: DatabaseReference
    private lateinit var addButton: com.google.android.material.floatingactionbutton.FloatingActionButton


    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_expense)

        var topBarTitle = findViewById<TextView>(R.id.topBarTitle)
        topBarTitle.text = topBarTitle.text.toString() + " - Expenses"

        databaseReference = FirebaseDatabase.getInstance().getReference("expenses")
        addButton = findViewById<FloatingActionButton>(R.id.btnAdd)
        itemsContainer = findViewById(R.id.container)


        addButton.setOnClickListener {
            showAddExpenseDialog()
        }

        loadExpenses()
    }

    private fun showAddExpenseDialog() {
        val dialog = Dialog(this)
        dialog.setContentView(R.layout.dialog_add_expense)

        val edtExpDate = dialog.findViewById<EditText>(R.id.edtExpDate)
        val edtExpAmt = dialog.findViewById<EditText>(R.id.edtExpAmt)
        val edtExpDesc = dialog.findViewById<EditText>(R.id.edtExpDesc)
        val edtExpCate = dialog.findViewById<EditText>(R.id.edtExpCate)
        val edtExpPayee = dialog.findViewById<EditText>(R.id.edtExpPayee)
        val btnSaveItem = dialog.findViewById<Button>(R.id.btnSaveItem)

        val calendar = Calendar.getInstance()

        edtExpDate.setOnClickListener {
            val datePicker = DatePickerDialog(
                this,
                { _, year, month, dayOfMonth ->
                    calendar.set(year, month, dayOfMonth)
                    val sdf = SimpleDateFormat("yyyy-MM-dd", Locale.getDefault())
                    edtExpDate.setText(sdf.format(calendar.time))
                },
                calendar.get(Calendar.YEAR),
                calendar.get(Calendar.MONTH),
                calendar.get(Calendar.DAY_OF_MONTH)
            )
            datePicker.show()
        }

        btnSaveItem.setOnClickListener {
            val date = edtExpDate.text.toString()
            val amount = edtExpAmt.text.toString().toDoubleOrNull() ?: 0.0
            val description = edtExpDesc.text.toString()
            val category = edtExpCate.text.toString()
            val payee = edtExpPayee.text.toString()

            if (date.isNotEmpty() && amount > 0 && description.isNotEmpty() && category.isNotEmpty() && payee.isNotEmpty()) {
                saveExpense(date, amount, description, category, payee)
                dialog.dismiss()
            } else {
                Toast.makeText(this, "Please fill all fields!", Toast.LENGTH_SHORT).show()
            }
        }

        dialog.show()
    }

    private fun saveExpense(date: String, amount: Double, description: String, category: String, payee: String) {
        val expenseId = databaseReference.push().key ?: return
        val expense = ExpenseModel(expenseId, date, amount, description, category, payee)

        databaseReference.child(expenseId).setValue(expense).addOnSuccessListener {
            Toast.makeText(this, "Expense saved", Toast.LENGTH_SHORT).show()
        }.addOnFailureListener {
            Toast.makeText(this, "Failed to save expense", Toast.LENGTH_SHORT).show()
        }
    }

    private fun loadExpenses() {
        databaseReference.orderByChild("date").addValueEventListener(object : ValueEventListener {
            override fun onDataChange(snapshot: DataSnapshot) {
                itemsContainer.removeAllViews() // Clear existing views

                val reversedSnapshot = snapshot.children.reversed()
                for (expenseSnapshot in reversedSnapshot) {
                    val expense = expenseSnapshot.getValue(ExpenseModel::class.java)
                    if (expense != null) {
                        addExpenseCard(expense)
                    }
                }
            }

            override fun onCancelled(error: DatabaseError) {
                Toast.makeText(this@Expense, "Failed to load expenses!", Toast.LENGTH_SHORT).show()
            }
        })
    }

    private fun addExpenseCard(expense: ExpenseModel) {
        val cardView = LayoutInflater.from(this).inflate(R.layout.list_card_layout, itemsContainer, false) as CardView
        val dateTextView = cardView.findViewById<TextView>(R.id.dateTextView)
        val amountTextView = cardView.findViewById<TextView>(R.id.amtTextView)
        val descriptionTextView = cardView.findViewById<TextView>(R.id.invTextView)
        val deleteExpenseIcon = cardView.findViewById<ImageView>(R.id.deleteIcon)
        val payee = cardView.findViewById<TextView>(R.id.cstTextView)

        dateTextView.text = expense.date
        amountTextView.text = "₹${expense.amount}"
        descriptionTextView.text = expense.category
        payee.text = expense.payee


        deleteExpenseIcon.setOnClickListener {
            showDeleteConfirmationDialog(expense)
        }
        itemsContainer.addView(cardView)
    }

    private fun showDeleteConfirmationDialog(expense: ExpenseModel) {
        AlertDialog.Builder(this)
            .setTitle("Delete Expense")
            .setMessage("Are you sure you want to delete this sale?")
            .setPositiveButton("Yes") { _, _ -> deleteExpense(expense) }
            .setNegativeButton("No", null)
            .show()
    }

    private fun deleteExpense(expense: ExpenseModel) {
        databaseReference.child(expense.expenseId).removeValue()
            .addOnSuccessListener {
                Toast.makeText(this, "Product deleted successfully!", Toast.LENGTH_SHORT).show()
                loadExpenses() // Refresh the product list
            }
            .addOnFailureListener {
                Toast.makeText(this, "Failed to delete product!", Toast.LENGTH_SHORT).show()
            }
    }
}