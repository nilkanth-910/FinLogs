package com.example.finlogs

import android.content.Context // Import Context
import android.content.Intent
import android.content.SharedPreferences // Import SharedPreferences
import android.os.Bundle
import android.widget.Button
import androidx.appcompat.app.AppCompatActivity

class MainActivity : AppCompatActivity() {

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)

        // ---- START: Check Login State ----
        // Check login state FIRST
        val loginPrefs: SharedPreferences = getSharedPreferences("MyPrefs", Context.MODE_PRIVATE) // Use Context.MODE_PRIVATE
        val isLoggedIn = loginPrefs.getBoolean("isLoggedIn", false)

        if (isLoggedIn) {
            // User is logged in, go directly to HomeActivity
            startActivity(Intent(this, HomeActivity::class.java))
            finish() // Finish MainActivity so user can't go back to it using the back button
            return   // Important: Stop executing the rest of onCreate for MainActivity
        }
        // ---- END: Check Login State ----

        // If not logged in, proceed to show the normal MainActivity layout
        setContentView(R.layout.activity_main)

        // --- Original Button Setup --- (Keep this part)
        val loginButton = findViewById<Button>(R.id.loginButton)
        val registerButton = findViewById<Button>(R.id.registerButton)

        loginButton.setOnClickListener {
            startActivity(Intent(this, LoginActivity::class.java))
        }

        registerButton.setOnClickListener {
            // Ensure you have a RegisterActivity if you keep this button
            startActivity(Intent(this, RegisterActivity::class.java))
        }
    }
}