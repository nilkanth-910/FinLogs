package com.example.finlogs

import android.content.Context
import android.content.Intent
import android.content.SharedPreferences
import android.os.Bundle
import android.view.View
import android.widget.Button
import android.widget.EditText
import android.widget.ProgressBar
import android.widget.TextView
import android.widget.Toast
import androidx.appcompat.app.AppCompatActivity
import com.google.firebase.database.FirebaseDatabase

class LoginActivity : AppCompatActivity() {

    private lateinit var database: FirebaseDatabase
    private lateinit var progressBar: ProgressBar

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_login)

        var topBarTitle = findViewById<TextView>(R.id.topBarTitle)

        topBarTitle.text = "Login"

        database = FirebaseDatabase.getInstance()
        progressBar = findViewById(R.id.progressBar)

        val usernameEditText = findViewById<EditText>(R.id.usernameEditText)
        val passwordEditText = findViewById<EditText>(R.id.passwordEditText)
        val loginButton = findViewById<Button>(R.id.loginButton)

        loginButton.setOnClickListener {
            val username = usernameEditText.text.toString().trim()
            val password = passwordEditText.text.toString().trim()

            if (username.isEmpty() || password.isEmpty()) {
                Toast.makeText(this, "Please enter both username and password", Toast.LENGTH_SHORT).show()
                return@setOnClickListener
            }

            progressBar.visibility = View.VISIBLE

            val usersRef = database.reference.child("users").child(username)

            usersRef.get().addOnSuccessListener { snapshot ->
                progressBar.visibility = View.GONE
                if (snapshot.exists()) {
                    val storedPassword = snapshot.child("password").value.toString()
                    if (storedPassword == password) {

                        // ---- START: Add this code ----
                        // Save login state
                        val loginPrefs: SharedPreferences = getSharedPreferences("MyPrefs", Context.MODE_PRIVATE)
                        val editor = loginPrefs.edit()
                        editor.putBoolean("isLoggedIn", true)
                        editor.putString("loginId", username) // Optionally store username
                        editor.apply()
                        // ---- END: Add this code ----

                        Toast.makeText(this, "Login successful", Toast.LENGTH_SHORT).show()
                        startActivity(Intent(this, HomeActivity::class.java))
                        finish() // Finish LoginActivity
                    } else {
                        Toast.makeText(this, "Incorrect password", Toast.LENGTH_SHORT).show()
                    }
                } else {
                    Toast.makeText(this, "Username not found", Toast.LENGTH_SHORT).show()
                }
            }.addOnFailureListener {
                progressBar.visibility = View.GONE
                Toast.makeText(this, "Database error: ${it.message}", Toast.LENGTH_SHORT).show()
            }
        }
    }
}