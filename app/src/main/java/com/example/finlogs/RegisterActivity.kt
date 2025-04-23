package com.example.finlogs

import android.os.Bundle
import android.view.View
import android.widget.Button
import android.widget.EditText
import android.widget.ImageView
import android.widget.TextView
import android.widget.Toast
import androidx.appcompat.app.AppCompatActivity
import com.google.firebase.database.FirebaseDatabase

class RegisterActivity : AppCompatActivity() {

    private lateinit var database: FirebaseDatabase

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_register)

        var filter = findViewById<ImageView>(R.id.filter)
        var back = findViewById<ImageView>(R.id.back)

        filter.visibility = View.GONE
        back.visibility = View.GONE

        database = FirebaseDatabase.getInstance()

        val storeNameEditText = findViewById<EditText>(R.id.storeNameEditText)
        val usernameEditText = findViewById<EditText>(R.id.usernameEditText)
        val emailEditText = findViewById<EditText>(R.id.emailEditText)
        val passwordEditText = findViewById<EditText>(R.id.passwordEditText)
        val mobileNoEditText = findViewById<EditText>(R.id.mobileNoEditText)
        val registerButton = findViewById<Button>(R.id.registerButton)
        val topBarTitle = findViewById<TextView>(R.id.topBarTitle)
        topBarTitle.text = "Register"

        registerButton.setOnClickListener {
            val storeName = storeNameEditText.text.toString().trim()
            val username = usernameEditText.text.toString().trim()
            val email = emailEditText.text.toString().trim()
            val password = passwordEditText.text.toString().trim()
            val mobileNo = mobileNoEditText.text.toString().trim()

            if (storeName.isEmpty() || username.isEmpty() || email.isEmpty() || password.isEmpty() || mobileNo.isEmpty()) {
                Toast.makeText(this, "Please fill in all fields", Toast.LENGTH_SHORT).show()
                return@setOnClickListener
            }

            val usersRef = database.reference.child("users")

            // Check if username already exists
            usersRef.child(username).get().addOnSuccessListener { snapshot ->
                if (snapshot.exists()) {
                    Toast.makeText(this, "Username already taken", Toast.LENGTH_SHORT).show()
                } else {
                    // Store all user data
                    val user = mapOf(
                        "storeName" to storeName,
                        "username" to username,
                        "email" to email,
                        "password" to password,
                        "mobileNo" to mobileNo
                    )

                    usersRef.child(username).setValue(user)
                        .addOnCompleteListener {
                            Toast.makeText(this, "Registration successful", Toast.LENGTH_SHORT).show()
                            finish()
                        }
                        .addOnFailureListener { e ->
                            Toast.makeText(this, "Failed to save user data: ${e.message}", Toast.LENGTH_SHORT).show()
                        }
                }
            }.addOnFailureListener {
                Toast.makeText(this, "Database error: ${it.message}", Toast.LENGTH_SHORT).show()
            }
        }
    }
}